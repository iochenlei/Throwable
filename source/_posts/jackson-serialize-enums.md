---
title: 【译】Jackson序列化枚举类型
date: 2020-04-14 21:26:42
tags:
- jackson
- 枚举
categories:
- Java
---

原文：[How To Serialize and Deserialize Enums with Jackson](https://www.baeldung.com/jackson-serialize-enums)

## 1. 前言

在本篇教程中我们将学习如何在Java中使用Jackson 2控制枚举类型的序列化与反序列化。

## 2. 枚举类型转为JSON

下面是我们定义的枚举类型：

```java
public enum Distance {
    KILOMETER("km", 1000), 
    MILE("miles", 1609.34),
    METER("meters", 1), 
    INCH("inches", 0.0254),
    CENTIMETER("cm", 0.01), 
    MILLIMETER("mm", 0.001);
 
    private String unit;
    private final double meters;
 
    private Distance(String unit, double meters) {
        this.unit = unit;
        this.meters = meters;
    }
 
    // standard getters and setters
}
```

### 2.1. 默认行为

默认情况下，Jackson会把Java枚举类型转为String，例如：
```java
new ObjectMapper().writeValueAsString(Distance.MILE);
```

将产生结果：

```text
"MILE"
```

然而，我们希望得到的结果如下：

```json
{"unit":"miles","meters":1609.34}
```

### 2.2. 枚举类型转为JSON对象

从Jackson 2.2.1开始，我们可以使用	`@JsonFormat`配置转换的结果：

```java
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Distance { ... }
```

此时，转换之后的结果是：

```java
{"unit":"miles","meters":1609.34}
```

### 2.3. 枚举类型与@JsonValue

另一个控制转换结果的简单的方法是在Getter函数上使用`@JsonValue`注解：

```java
public enum Distance { 
    ...
  
    @JsonValue
    public String getMeters() {
        return meters;
    }
}
```

此时，`getMeters()`返回的值被用来表示该枚举类型，因此，转换之后的结果是：

```java
1609.34
```

### 2.4. 为枚举类型自定义Serializer

在Jackson 2.1.2之前，我们可以使用一个自定义的Serializer来对枚举类型进行定制的序列化。

首先我们需要定义Serializer:

```java
public class DistanceSerializer extends StdSerializer {
     
    public DistanceSerializer() {
        super(Distance.class);
    }
 
    public DistanceSerializer(Class t) {
        super(t);
    }
 
    public void serialize(
      Distance distance, JsonGenerator generator, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
        generator.writeStartObject();
        generator.writeFieldName("name");
        generator.writeString(distance.name());
        generator.writeFieldName("unit");
        generator.writeString(distance.getUnit());
        generator.writeFieldName("meters");
        generator.writeNumber(distance.getMeters());
        generator.writeEndObject();
    }
}
```

现在，我们把这个Serializer用于到枚举类型之上：

```java
@JsonSerialize(using = DistanceSerializer.class)
public enum TypeEnum { ... }
```

序列化之后的结果：

```json
{"name":"MILE","unit":"miles","meters":1609.34}
```

## 3. 反序列化JSON为Enum

首先，我们来定义一个City类，该类有一个Distance成员：

```java
{"name":"MILE","unit":"miles","meters":1609.34}
```

下面，我们将讨论如何使用不同的方法来反序列一个JSON字符串为枚举类型。

### 3.1. 默认行为

**默认情况下，Jackson将根据枚举类型的名称反序列化枚举类型。**

例如，下面这个JSON字符串：
```json
{"distance":"KILOMETER"}
```

能反序列化为`Distance.KILOMETER`对象：

```java
City city = new ObjectMapper().readValue(json, City.class);
assertEquals(Distance.KILOMETER, city.getDistance());
```

### 3.2. 使用@JsonValue

我们已经学习了如何使用`@JsonValue`来序列化枚举类型，由于本例中枚举类型的值是常量，所以我们也可以使用同样的注解来反序列化。

首先，我们在`getMeters()`方法上使用`@JsonValue`注解：

```java
public enum Distance {
    ...
 
    @JsonValue
    public double getMeters() {
        return meters;
    }
}
```

现在，`getMeters()`方法的返回值可以用于表示一个枚举对象。因此，当反序列化下面这个JSON字符串：

```json
{"distance":"0.0254"}
```

Jackson将查找`getMeters()`方法返回值为0.0254的枚举对象。在本例中，该对象是`Distance.INCH`:

```java
assertEquals(Distance.INCH, city.getDistance());
```

## 3.3. 使用@JsonProperty

`@JsonProperty`注解可以用于一个枚举类型实例上：

```java
public enum Distance {
    @JsonProperty("distance-in-km")
    KILOMETER("km", 1000), 
    @JsonProperty("distance-in-miles")
    MILE("miles", 1609.34);
  
    ...
}
```

通过使用该注解，**我们告知Jackson把@JsonProperty的值映射到带有该注解的对象上面去。**

假设JSON字符串如下：
```json
{"distance": "distance-in-km"}
```

该字符串反序列化之后，会映射到`Distance.KILOMETER`对象：

```java
assertEquals(Distance.KILOMETER, city.getDistance());
```

### 3.4. 使用@JsonCreator

**Jackson会调用被@JsonCreator注解的方法来构建对应的对象。*

假设我们的JSON如下：

```json
{
    "distance": {
        "unit":"miles", 
        "meters":1609.34
    }
}
```

现在，我们来定义一个带有`@JsonCreator`注解的`forValues()`工厂方法：

```java
public enum Distance {
    
    @JsonCreator
    public static Distance forValues(@JsonProperty("unit") String unit,
      @JsonProperty("meters") double meters) {
        for (Distance distance : Distance.values()) {
            if (
              distance.unit.equals(unit) && Double.compare(distance.meters, meters) == 0) {
                return distance;
            }
        }
 
        return null;
    }
 
    ...
}
```

注意，`@JsonProperty`注解会把输入的JSON字段绑定到方法参数上。

然后，我们反序列化该JSON字符串会获得结果：

```java
assertEquals(Distance.MILE, city.getDistance());
```

### 3.5. 使用自定义的Deserializer

在上面给出的方法都不能使用在，则可以使用自定义的Deserializer，例如，我们可能访问不到枚举类型的源码，或者我们可能使用的是旧版本的Jackson。

根据我们以前的文章所述，首先，我们需要创建一个Deserializer类：

```java
public class CustomEnumDeserializer extends StdDeserializer<Distance> {
 
    @Override
    public Distance deserialize(JsonParser jsonParser, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
 
        String unit = node.get("unit").asText();
        double meters = node.get("meters").asDouble();
 
        for (Distance distance : Distance.values()) {
            
            if (distance.getUnit().equals(unit) && Double.compare(
              distance.getMeters(), meters) == 0) {
                return distance;
            }
        }
 
        return null;
    }
}
```

然后，我们通过`@JsonDeserialize`注解使用这个Deserializer：

```java
@JsonDeserialize(using = CustomEnumDeserializer.class)
public enum Distance {
   ...
}
```

我们获得的结果是：

```java
assertEquals(Distance.MILE, city.getDistance());
```

## 4. 总结

本文演示了如何使用更好的方法控制Java枚举类型的序列化与反序列化过程。

所有的实例代码可以在[GitHub](https://github.com/eugenp/tutorials/tree/master/jackson-modules/jackson-conversions#readme "GitHub")找到。

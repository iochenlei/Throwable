---
title: Spring源码分析 —— Controller的注册
date: 2020-11-17 11:23:42
tags: Spring
categories:
    - 源码分析
---

什么是Controller的注册？Controller的注册其实就是对@Controller与@RequestMapping注解的处理。

我们在使用Spring MVC进行开发的过程中往往需要同时搭配使用@Controller与@RequestMapping注解。

@Controller标注在类上面，表示被注解的类是一个Controller，@RequestMapping注解用在类或者方法上面，它的主要功能是定义被注解的方法可以处理的请求。

在Spring MVC的实现中把Controller中每一个把@RequestMapping注解的方法使用HandlerMethod类封装，@RequestMapping注解中的信息使用RequestMappingInfo类进行封装。

HandlerMethod类的位置如下：

```
org.springframework.web.method.HandlerMethod
```

RequestMappingInfo类的位置如下：

```
org.springframework.web.servlet.mvc.method.RequestMappingInfo
```

Controller的注册工作由RequestMappingHandlerMapping类负责完成。下面我们先分析这个类的结构，再分析它的实现。

RequestMappingHandlerMapping类的位置如下：

```
org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
```

<!-- more -->

## 类的结构层次

该类的继承关系如下图所示，它需要实现HandlerMapping接口，该接口的`getHandler()`方法需要返回可以处理传入请求的请求处理器。

{% plantuml %}
@startuml
interface InitializingBean {
    + afterPropertiesSet(): void
}

interface HandlerMapping {
	+ getHandler(HttpServletRequest request) HandlerExecutionChain
}

abstract class AbstractHandlerMapping implements HandlerMapping {
}

abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {
}

abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping {
}

abstract class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping {
}

@enduml
{% endplantuml %}

为了便于大家的理解，下面给出上面类图对应的代码。

AbstractHandlerMapping类：
```java
public abstract class AbstractHandlerMapping implements HandlerMapping {
}
```

AbstractHandlerMethodMapping类，其中存在一个泛型T：
```java
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {
    // 变量mappingRegistry用于存放Controller的注册记录
    private final MappingRegistry mappingRegistry = new MappingRegistry();
}
```

RequestMappingInfoHandlerMapping类：
```java
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {
}
```

根据RequestMappingInfoHandlerMapping的类声明，可见AbstractHandlerMethodMapping中的泛型T是RequestMappingInfo。

RequestMappingHandlerMapping类：
```java
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping {
}
```

综上可得，最终RequestMappingHandlerMapping类实现了接口HandlerMapping与InitializingBean。

熟悉Spring的同学都知道，一旦BeanFactory完成`InitializingBean`接口实现类的的依赖注入就会调用`InitializingBean`接口的`afterPropertiesSet()`方法。
因此这里就是Controller注册工作的起点，我们将从这个方法着手对源码进行分析。

## 源码分析

在进行源码分析之前，我们先来看下方法的调用顺序：

{% plantuml %}
@startuml
participant RequestMappingHandlerMapping as RMHM
participant MappingRegistry as MR

autonumber

RMHM -> RMHM: afterPropertiesSet()
activate RMHM #ff0000

RMHM -> RMHM: 调用initHandlerMethods()方法枚举出所有符合要求的Bean。
activate RMHM

RMHM -> RMHM: 调用processCandidateBean()方法对Bean进行处理。
activate RMHM

RMHM -> RMHM: 调用detectHandlerMethods()方法处理Bean中被@RequestMapping注解的Method
activate RMHM

RMHM -> RMHM: 调用getMappingForMethod()方法为Method生成RequestMappingInfo
activate RMHM

RMHM -> RMHM: 调用createRequestMappingInfo()创建RequestMappingInfo
activate RMHM

RMHM -> RMHM: 调用registerHandlerMethod()方法注册handler与RequestMappingInfo
activate RMHM

RMHM -> MR: 调用register()方法把handler与RequestMappingInfo注册为HandlerMethod
activate MR

return

return
return
return
return
return
return
return
@enduml
{% endplantuml %}

### afterProperties()方法

这个方法没什么好分析的，非常简单，对initHandlerMethods()方法进行了调用。

### initHandlerMethods()方法

该方法由`AbstractHandlerMethodMapping`类实现，位置如下：

```
org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
```

它的主要功能是：

1. 遍历出BeanFactory中所有Bean的名称；
2. 排除那些带有作用域的Bean名称；
3. 再把Bean名称传递给`processCandidateBean()`方法进行进一步的处理。

它的源码如下：

```java
protected void initHandlerMethods() {
    // 遍历BeanFactory中所有Bean的名称
    for (String beanName : getCandidateBeanNames()) {
        // 这个if语句用于排除那些带有作用域的Bean：即Bean的名称以scopedTarget.开始的
        if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
            // 把Bean名称传递给processCandidateBean()方法进行处理。
            processCandidateBean(beanName);
        }
    }
    // handlerMethodsInitialized()函数的默认实现仅仅打印了日志，所以就不贴出来了。
    handlerMethodsInitialized(getHandlerMethods());
}

// 获取BeanFactory中所有Bean的名称。
protected String[] getCandidateBeanNames() {
    // detectHandlerMethodsInAncestorContexts用于判断是加载父BeanFactory中的Bean，默认值是false。
    return (this.detectHandlerMethodsInAncestorContexts ?
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
            obtainApplicationContext().getBeanNamesForType(Object.class));
}

// 返回一个只读的Map，用于存放解析出来的HandlerMethod。
public Map<T, HandlerMethod> getHandlerMethods() {
    // 为MappingRegistry上读锁
    this.mappingRegistry.acquireReadLock();
    try {
        // 返回一个不可变的Map
        return Collections.unmodifiableMap(this.mappingRegistry.getMappings());
    }
    finally {
        // 解锁
        this.mappingRegistry.releaseReadLock();
    }
}
```

### processCandidateBean()方法

该方法由`AbstractHandlerMethodMapping`类实现，主要功能是：

1. 判断Bean是不是一个Handler，判断的依据是Bean的Class上面存在@Controller或者@RequestMapping注解；
2. 如果Bean是一个Handler，则调用detectHandlerMethods()方法对它进行处理。

它的源码如下：

```java
protected void processCandidateBean(String beanName) {
    Class<?> beanType = null;
    try {
        // 根据Bean的名称获取它的类型。
        beanType = obtainApplicationContext().getType(beanName);
    }
    catch (Throwable ex) {
        // An unresolvable bean type, probably from a lazy bean - let's ignore it.
        if (logger.isTraceEnabled()) {
            logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
        }
    }
    // 判断Bean是不是一个Handler。
    if (beanType != null && isHandler(beanType)) {
        // 如果Bean是Handler，则调用detectHandlerMethods()方法进行下一步的处理。
        detectHandlerMethods(beanName);
    }
}
```

上面调用的isHandler()方法类`RequestMappingHandlerMapping`实现，功能如下：

+ 判断Bean是否为Handler，依据是Bean的Class上面是否存在@RequestMapping或@Controller注解。

源码如下：

```java
@Override
protected boolean isHandler(Class<?> beanType) {
    return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
            AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
}
```

### detectHandlerMethods()方法

该方法还是由`AbstractHandlerMethodMapping`类实现，主要功能是：

1. 遍历Handler中用户定义的所有方法；
2. 使用RequestMappingInfo对象封装方法上@RequestMapping注解所给出的信息；
3. 将Handler、Method以及Method对应的RequestMappingInfo对象进行注册。

{% note primary %}
**这里的Handler指的是什么？**
答：根据上面的processCandidateBean()方法可知，这里的Handler实际上就是存在@Controller注解的Bean。
{% endnote %}

该方法的源码如下：

```java
protected void detectHandlerMethods(Object handler) {
    // 如果传入的handler参数是个字符串，意味着handler是Bean的名称。
    // 则调用BeanFactory的getType()方法根据Bean的名称获取Bean的类型。
    Class<?> handlerType = (handler instanceof String ?
            obtainApplicationContext().getType((String) handler) : handler.getClass());

    if (handlerType != null) {
        // 如果handler是由cglib动态生成的代理类，就获取被代理之前的类（即代理类的超类）。
        Class<?> userType = ClassUtils.getUserClass(handlerType);
        // MethodIntrospector.selectMethods()用于枚举出目标类中由用户定义的方法。
        // 它的第一个参数是目标类型，第二个参数是一个回调函数。它的返回值是Map<Method, T>。
        // 第二个参数回调函数的入参是Method，返回值是泛型T或null。
        // 该回调函数如果返回的是null则传入的Method不会出现在返回的Map中。

        // 根据上文可知T是RequestMappingInfo，
        // 所以变量methods的类型实际上就是Map<Method, RequestMappingInfo>。
        Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
                (MethodIntrospector.MetadataLookup<T>) method -> {
                    try {
                        // 调用getMappingForMethod()，解析出方法上的@RequestMapping注解，并将它包装成RequestMappingInfo对象返回。
                        return getMappingForMethod(method, userType);
                    }
                    catch (Throwable ex) {
                        throw new IllegalStateException("Invalid mapping on handler class [" +
                                userType.getName() + "]: " + method, ex);
                    }
                });
        if (logger.isTraceEnabled()) {
            logger.trace(formatMappings(userType, methods));
        }
        // 遍历methods，注册Method及其对应的RequestMappingInfo对象。
        methods.forEach((method, mapping) -> {
            // 在handler或者handler实现的接口中寻找一个可调用的方法。
            Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
            // 调用registerHandlerMethod()完成注册。
            registerHandlerMethod(handler, invocableMethod, mapping);
        });
    }
}
```

### getMappingForMethod()方法

该方法的作用是解析@RequestMapping注解，使用RequestMappingInfo对象封装@RequestMapping上给出的信息，它会对方法和方法所属类上的@RequestMapping注解分别进行解析，然后按照一定的规则将两者合为一体后返回。

该方法的源码如下：

```java
@Override
@Nullable
protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    // 先提取方法上的@RequestMapping注解。
    RequestMappingInfo info = createRequestMappingInfo(method);
    // 如果方法上存在@RequestMapping注解。
    if (info != null) {
        // 接着提取方法所属类上的@RequestMapping注解。
        RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
        if (typeInfo != null) {
            // 如果方法所属类上也可以提取到@RequestMapping注解，则合并这两个RequestMappingInfo对象。
            info = typeInfo.combine(info);
        }
        // 获取为全局Controller配置的请求路径前缀。
        String prefix = getPathPrefix(handlerType);
        if (prefix != null) {
            // 如果配置了请求路径前缀，则把前缀并入RequestMappingInfo对象中。
            info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
        }
    }
    return info;
}

@Nullable
private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
    // 获取合并后的RequestMapping注解。
    RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
    // 获取自定义的RequestCondition。RequestCondition中保存的是请求的匹配条件。
    RequestCondition<?> condition = (element instanceof Class ?
            getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
    // 如果RequestMapping对象不为空则调用重载方法createRequestMappingInfo()创建RequestMappingInfo对象。
    // 如果RequestMapping对象为空则返回null。
    return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
}

protected RequestMappingInfo createRequestMappingInfo(
        RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {
    // 调用Builder创建RequestMappingInfo对象，并使用RequestMapping中的信息对Builder进行初始化。
    RequestMappingInfo.Builder builder = RequestMappingInfo
            .paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
            .methods(requestMapping.method())
            .params(requestMapping.params())
            .headers(requestMapping.headers())
            .consumes(requestMapping.consumes())
            .produces(requestMapping.produces())
            .mappingName(requestMapping.name());
    // 如果存在自定义的RequestCondition，则把自定义的RequestCondition加入RequestMappingInfo中。
    if (customCondition != null) {
        builder.customCondition(customCondition);
    }
    return builder.options(this.config).build();
}
```

{% note primary %}
什么是合并后的RequestMapping注解？
答：@GetMapping、@PostMapping和@PutMapping这些注解，其实可以当做@RequestMapping注解的一个别名。
但是Java中是不支持为注解起别名这种操作。为了解决这个问题，Spring给出了一个解决方法：重新声明一个注解让它具有与原注解同样的属性。
这样一来，把注解的别名转换为原注解只需要把别名中的属性复制到原注解中即可，AnnotatedElementUtils.findMergedAnnotation()方法就是来完成这件事的。
{% endnote %}

### registerHandlerMethod()方法

在detectHandlerMethods()方法中，完成RequestMappingInfo的解析后就会调用registerHandlerMethod()方法完成handler、method和mapping的注册工作。

```java
protected void detectHandlerMethods(Object handler) {
        ...
        methods.forEach((method, mapping) -> {
            Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
            registerHandlerMethod(handler, invocableMethod, mapping);
        });
    }
}
```

registerHandlerMethod()方法的源码如下：

```java
protected void registerHandlerMethod(Object handler, Method method, T mapping) {
    this.mappingRegistry.register(mapping, handler, method);
}
```

可见它调用了变量mappingRegistry的register()方法，变量mappingRegistry用于存放handler、method和mapping的注册记录，它的类型MappingRegistry是在AbstractHandlerMethodMapping类中实现的一个内部类。

下面给出MappingRegistry类的定义：

```java
class MappingRegistry {
    // 用于存放RequestMappingInfo到MappingRegistration的映射关系。
    private final Map<T, MappingRegistration<T>> registry = new HashMap<>();
    
    // 用于存放RequestMappingInfo到HandlerMethod的映射关系。
    private final Map<T, HandlerMethod> mappingLookup = new LinkedHashMap<>();
    
    // 用于存放path到RequestMappingInfo的映射关系。
    private final MultiValueMap<String, T> urlLookup = new LinkedMultiValueMap<>();
    
    // 用于存放name到HandlerMethod的映射关系
    private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();
    
    // 用于存放HandlerMethod到CorsConfiguration的映射关系
    private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();
    
    // registry的读写锁
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    
    // 省略方法的定义
}
```

register()方法完成handler、method和mapping的登记操作，下面我们来看一下它的实现。

它的源码如下：

```java
public void register(T mapping, Object handler, Method method) {
    // Assert that the handler method is not a suspending one.
    if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if ((parameterTypes.length > 0) && "kotlin.coroutines.Continuation".equals(parameterTypes[parameterTypes.length - 1].getName())) {
            throw new IllegalStateException("Unsupported suspending handler method detected: " + method);
        }
    }
    // 上写锁
    this.readWriteLock.writeLock().lock();
    try {
        // 使用HandlerMethod对象封装Handler与Method。
        // 通过上文我们已经知道handler其实就是被@Controller或@RequestMapping所注解的Bean。
        // 而method就是handler中一个存在@RequestMapping注解的方法。
        HandlerMethod handlerMethod = createHandlerMethod(handler, method);
        // 检测mapping是否已经注册过了，已注册过就抛出异常。
        validateMethodMapping(handlerMethod, mapping);
        // 保存mapping与handlerMethod的对应关系。
        this.mappingLookup.put(mapping, handlerMethod);

        // 获取mapping上绑定的url（就是@RequestMapping注解中的path属性）。
        List<String> directUrls = getDirectUrls(mapping);
        // 建立url到mapping的映射关系。
        for (String url : directUrls) {
            this.urlLookup.add(url, mapping);
        }

        String name = null;
        if (getNamingStrategy() != null) {
            // 使用给定的命令策略为handlerMethod起个名称。
            name = getNamingStrategy().getName(handlerMethod, mapping);
            // 建立name到handlerMethod的映射关系。
            addMappingName(name, handlerMethod);
        }

        // 解析handler或者method上的@CrossOrigin注解，该注解用于支持跨站请求。
        CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
        if (corsConfig != null) {
            // 建立handlerMethod与corsConfig的映射关系。
            this.corsLookup.put(handlerMethod, corsConfig);
        }
        // 建立mapping到handlerMethod、directUrls和name的映射关系。
        // MappingRegistration类用于存放mapping、HandlerMethod、directUrls和name。
        this.registry.put(mapping, new MappingRegistration<>(mapping, handlerMethod, directUrls, name));
    }
    finally {
        // 解写锁
        this.readWriteLock.writeLock().unlock();
    }
}
```

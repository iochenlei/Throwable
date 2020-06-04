---
title: 输出Mybatis的日志
tags: Mybatis
category: Java
date: 2020-06-04 14:36:58
---


在日常开发中，有时候我们可能需要查阅Mybatis的日志。

默认情况下，Mybatis是不会打印任何日志的。

为了让Mybatis打印日志，首先我们需要为Mybatis指定它的日志实现类。

我们可以通过两种方式来指定Mybatis的日志实现类。

第一个方法是在配置文件中指定：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <setting name="logImpl" value="LOG4J2"/>
    </settings>
</configuration>
```

如上所示，只要在`<settings>`中配置`logImpl`属性就可以指定Mybatis的日志实现，它支持下面这些值：

+ SLF4J，使用Slf4j输出日志
+ LOG4J，使用Log4j输出日志
+ LOG4J2，使用Log4j2输出日志（即log4j的2.x版本）
+ JDK_LOGGING，使用JDK的日志库输出日志
+ COMMONS_LOGGING，使用`apache-commons-logging`输出日志
+ STDOUT_LOGGING，直接打印日志到标准输出流
+ NO_LOGGING，无日志

**注意：如果不手动指定日志实现类，Mybatis会自动按下面的顺序查找日志实现类，如果没找到则会禁用日志功能。**

+ SLF4J
+ Apache Commons Logging
+ Log4j 2
+ Log4j
+ JDK logging

另一个方法是在代码中调用下列函数之一来指定Mybatis所使用的日志库：

```java
org.apache.ibatis.logging.LogFactory.useSlf4jLogging();
org.apache.ibatis.logging.LogFactory.useLog4JLogging();
org.apache.ibatis.logging.LogFactory.useJdkLogging();
org.apache.ibatis.logging.LogFactory.useCommonsLogging();
org.apache.ibatis.logging.LogFactory.useStdOutLogging();
```

在这里，我使用的是第一种方式设置使用`LOG4J2`输出日志：

```xml
<settings>
    <setting name="logImpl" value="LOG4J2"/>
</settings>
```

上面说过`LOG4J2`使用的是`log4j`的2.x版本，因此我们必须在项目中引入`log4j`的2.x版本:

```text
implementation 'org.apache.logging.log4j:log4j-api:2.13.3'
implementation 'org.apache.logging.log4j:log4j-core:2.13.3'
```

这里特别需要注意，如果不引入对应的日志库依赖到classpath中，Mybatis会抛出`NoClassDefFoundError`异常：

```java
Caused by: org.apache.ibatis.logging.LogException: Error setting Log implementation.  Cause: java.lang.reflect.InvocationTargetException
	at org.apache.ibatis.logging.LogFactory.setImplementation(LogFactory.java:109)
	at org.apache.ibatis.logging.LogFactory.useCustomLogging(LogFactory.java:59)
	at org.apache.ibatis.session.Configuration.setLogImpl(Configuration.java:230)
	at org.apache.ibatis.builder.xml.XMLConfigBuilder.loadCustomLogImpl(XMLConfigBuilder.java:156)
	at org.apache.ibatis.builder.xml.XMLConfigBuilder.parseConfiguration(XMLConfigBuilder.java:108)
	... 52 more
Caused by: java.lang.reflect.InvocationTargetException
	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
	at org.apache.ibatis.logging.LogFactory.setImplementation(LogFactory.java:103)
	... 56 more
Caused by: java.lang.NoClassDefFoundError: org/apache/logging/log4j/LogManager
	at org.apache.ibatis.logging.log4j2.Log4j2Impl.<init>(Log4j2Impl.java:31)
	... 61 more
```

最后一点，我们还需要在`log4j2.xml`文件中指定日志的级别与输出方式，这里我指定日志级别为`debug`，输出到标准输出流：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <Console name="STDOUT">
            <PatternLayout pattern="%m%n"/>
        </Console>
    </Appenders>

    <Loggers>
        <Root level="DEBUG">
            <AppenderRef ref="STDOUT"/>
        </Root>
    </Loggers>
</Configuration>
```

**注意：如果你使用其它的日志库，也可能需要指定日志输出级别。**

至此，如果执行Mybatis的查询可以看到Mybatis的日志输出：

```text
Opening JDBC Connection
Checked out connection 1991691115 from pool.
Setting autocommit to false on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@76b6cb6b]
==>  Preparing: SELECT * FROM employees WHERE emp_no=? 
==> Parameters: 10001(Integer)
====>  Preparing: SELECT * FROM salaries WHERE emp_no=? 
====> Parameters: 10001(Integer)
<====      Total: 17
<==      Total: 1
Resetting autocommit to true on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@76b6cb6b]
Closing JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@76b6cb6b]
Returned connection 1991691115 to pool.
```

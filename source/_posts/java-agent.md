---
title: Java Agent快速入门
date: 2020-03-05 14:41:56
tags:
- java-agent
categories:
- Java
---

## 什么是Java Agent？

Agent是一种从Java 6开始引入的机制。Agent以Jar包的形式存在，JVM在执行主程序的`main`方法之前会先调用Agent中的`premain`方法，这样一来，我们就就有机会在主程序启动之前做其它的事情。

<!--more-->

## Agent有什么用？

Java Agent主要的功能体现在“运行时”三个字上面，通过“运行时”可以使某些功能以“外挂”的方式提供：

+ APM工具，例如SkyWalking和Zipkin这类性能监控平台会针对一些常用的框架（例如：JDBC、Spring Cloud和Dubbo）提供一个Agent（即一个jar包）用于实现非侵入式的性能指标收集，以及在服务间调用时自动传递traceId。
+ 运行时增强，阿里开源的[transmittable-thread-local](https://github.com/alibaba/transmittable-thread-local)可以在运行时对[`InheritableThreadLocal`](https://docs.oracle.com/javase/10/docs/api/java/lang/InheritableThreadLocal.html)进行增强，使任务提交时ThreadLocal的值可以传递到任务执行时，用于解决ThreadLocal的值不能准确传递到线程池中的问题。
+ 热加载，[Jrebel](https://zeroturnaround.com/software/jrebel/)可以实现类的热加载，甚至该工具还可以实现资源文件的热加载，例如，Mybatis的xml文件。
+ 运行时Debug，[BTrace](https://github.com/btraceio/btrace)和阿里开源的[arthas](https://github.com/alibaba/arthas)可以对运行中的Java程序进行在线调试，这样用于追踪一些线下难以复现的问题。

## 如何实现一个Agent？

上文中提到Agent以Jar包的形式存在，JVM在运行主程序的`main`方法之前会先调用Agent的`premain`方法。因此一个Agent需要有一个`premain`方法。

目前，JVM支持两种形式`premain`方法的定义，JVM首先尝试调用下面的`premain`方法：

```java
public static void premain(String agentArgs, Instrumentation inst);
```

如果JVM没有找到上述方法，它就会调用下面这个`premain`方法：

```java
public static void premain(String agentArgs);
```

下面我们来实现一个简单的Agent，它会在`premain`方法执行时输出：`I'm AgentDemo1`。

先创建一个类存放`premain`方法：

**AgentDemo1.java**

```java
package cn.throwable;

public class AgentDemo1 {
    public static void premain(String agentArgs) {
        System.out.println("I'm AgentDemo1");
    }
}
```

接下来需要在MANIFEST.MF文件中使用`Premain-Class`属性指定`premain`方法的所在类：

**MANIFEST.MF**

```text
Premain-Class: cn.throwable.AgentDemo1
```

最后把上面的程序打包为AgetnDemo1.jar

## Agent如何加载？

Agent的加载方式有两种，第一种方式是在主程序启动之前加载，第二种方式是在主程序启动之后加载。

为了方便后面的演示，我们先来准备一个Hello World程序，它被打包成为`HelloWorld.jar`。

**HelloWorld.java**

```java
package cn.throwable;

public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
    }
}
```

### 在主程序启动之前加载

在主程序启动之前加载一个Agent非常简单，在启动主程序时指定参数`-javaagent`即可：

```java
java -javaagent:target/AgentDemo1-1.0-SNAPSHOT.jar -jar HelloWorld.jar
```

其中`target/AgentDemo1-1.0-SNAPSHOT.jar`是Agent的所在路径。

**输出**

```text
I'm AgentDemo1
Hello World!
```

如果你看见的是下面的输出：

```text
I'm AgentDemo1
no main manifest attribute, in HelloWorld.jar
```

说明我们在HelloWolrd.jar中没有指定`main`方法的位置，此时可以使用下面的命令指定`Agent`的位置与main方法的位置：

```text
java -javaagent:target/AgentDemo1-1.0-SNAPSHOT.jar -cp HelloWorld.jar cn.throwable.App
```

> PS：一般来说应该在HelloWorld.jar的MANIFEST.MF文件中使用属性Main-Class指定main方法的所在类。

### 在主程序启动之后加载

在主程序启动之后加载也称动态加载，这种加载方式可以把Agent挂载到正在运行的Java程序上，下面来看一下这种加载方式。

我们需要稍微改造一下HelloWorld程序，不然它在输出`Hello, World!`之后便直接结束了导致我们没机会挂载Agent：

**HelloWorld.java**

```java
package cn.throwable;

public class App
{
    public static void main( String[] args ) throws InterruptedException {
        System.out.println( "Hello World!" );
        Thread.sleep(1000 * 60);
    }
}
```

现在该程序在输出`Hello, World!`之后会等待1分钟。

还有，与第一种加载方式不同的是，使用动态加载时JVM会调用Agent的`agentmain`而不是`premain`方法，因此我们也需要修改一下我们的Agent：

**AgentDemo1.java**

```java
package cn.throwable;

public class AgentDemo1 {
    public static void premain(String agentArgs) {
        System.out.println("I'm AgentDemo1");
    }

    public static void agentmain(String agentArgs) {
        System.out.println("I'm AgentDemo1(Agentmain)");
    }
}
```

如你所见，在上面的代码中同时存在`premain`和`agentmain`方法，这是允许的，因为在MANIFEST.MF文件中使用了两个不同的属性来指示两者的所在位置，其中`Premain-Class`属性表示`premain`方法的所在类，`Agent-Class`属性表示`agentmain`方法的所在类：

**MANIFEST.MF**

```text
Premain-Class: cn.throwable.AgentDemo1
Agent-Class: cn.throwable.AgentDemo1
```

此外，为了实现Agent的动态加载还需要编写一个程序来把Agent挂载到正在运行的Java程序之上：

**Injector.java**

```java
package cn.throwable;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.IOException;

public class Injector
{
    public static void main(String[] args ) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        // 目标程序的PID, 可通过jps命令查看
        String jvmId = "4315";
        VirtualMachine vm = VirtualMachine.attach(jvmId);
        // Agent所在的位置
        File agentFile = new File("AgentDemo1.jar");
        // 载入Agent
        vm.loadAgent(agentFile.getAbsolutePath());
        vm.detach();
    }
}
```

上面的代码中，变量`jvmId`存放的是目标程序的PID，该值可以使用`jps`命令查看。

最后一步就是使用Injector把Agent挂载到运行中的HelloWorld程序上：

+ 先运行HelloWorld程序。

  ```text
  java -jar target/HelloWorld.jar
  ```

+ 然后使用`jps`命令查看HelloWorld程序的PID。

  ```text
  18374 RemoteMavenServer36
  19946 Launcher
  19979 HelloWorld-1.0-SNAPSHOT.jar
  20221 Jps
  18126 
  ```

+ 由上可见HelloWorld程序的PID是19979，把这个值放入变量`jvmId`之中并运行Injector。

**输出**

```text
Hello World!
I'm AgentDemo1(Agentmain)
```

最后需要注意的是，Java的[官方文档](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html)上特别指出：

> Agent的Jar包是由`SystemClassLoader`进行加载的，而主程序的`main`方法所在类也是由`SystemClassLoader`加载的，这意味着Agent的`premain`方法与主程序的`main`方法使用相同的classloader加载规则与安全性设置。

## Java Agent的异常处理策略

#### 主程序启动之前加载的异常处理策略

对于在主程序启动之前加载的Agent来说，如果在`premain`方法中产生异常则会导致主程序启动失败，下面我们演示一下该场景。

首先，我们修改一下Agent的实现，在`premain`方法中抛出一个运行时异常：

**AgentDemo1.java**

```java
package cn.throwable;

public class AgentDemo1 {
    public static void premain(String agentArgs) {
        System.out.println("I'm AgentDemo1");
        throw new RuntimeException("abc");
    }
}
```

然后加载该Agent：

```text
java -javaagent:target/AgentDemo1-1.0-SNAPSHOT.jar -jar HelloWorld.jar
```

**输出**

```text
I'm AgentDemo1
Exception in thread "main" java.lang.reflect.InvocationTargetException
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at sun.instrument.InstrumentationImpl.loadClassAndStartAgent(InstrumentationImpl.java:388)
        at sun.instrument.InstrumentationImpl.loadClassAndCallPremain(InstrumentationImpl.java:401)
Caused by: java.lang.RuntimeException: abc
        at cn.throwable.AgentDemo1.premain(AgentDemo1.java:7)
        ... 6 more
FATAL ERROR in native method: processing of -javaagent failed
[1]    19266 abort      java -javaagent:target/AgentDemo1-1.0-SNAPSHOT.jar -jar HelloWorld.jar
```

#### 主程序启动之后加载的异常处理策略

对于动态加载的Agent来说，在`agentmain`方法中产生的异常是不会对正在运行的Java程序有任何影响的，下面我们来演示一下这种场景。

与上面一样，我们先修改一下Agent的实现，在`agentmain`方法中抛出一个运行时异常：

**AgentDemo1.java**

```java
package cn.throwable;

public class AgentDemo1 {
    public static void agentmain(String agentArgs) {
        System.out.println("I'm AgentDemo1(Agentmain)");
        throw new RuntimeException("abc");
    }
}
```

然后依次运行HelloWorld.jar与Injector程序，此时，虽然会抛出Agent启动失败的异常但是主程序的运行却不会受到影响。

```text
Hello World!
I'm AgentDemo1(Agentmain)
Exception in thread "Attach Listener" java.lang.reflect.InvocationTargetException
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at sun.instrument.InstrumentationImpl.loadClassAndStartAgent(InstrumentationImpl.java:388)
        at sun.instrument.InstrumentationImpl.loadClassAndCallAgentmain(InstrumentationImpl.java:411)
Caused by: java.lang.RuntimeException: abc
        at cn.throwable.AgentDemo1.agentmain(AgentDemo1.java:12)
        ... 6 more
Agent failed to start!
```


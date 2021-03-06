---
title: sshpass的使用方法
date: 2020-04-12 12:58:55
tags: sshpass
categories: Linux命令
---

sshpass是一个命令行工具，用于在非交互式环境中（例如CI中）提供ssh的密码。

该命令的语法如下：

<!--more-->

```text
Usage: sshpass [-f|-d|-p|-e] [-hV] command parameters
   -f filename   Take password to use from file
   -d number     Use number as file descriptor for getting password
   -p password   Provide password as argument (security unwise)
   -e            Password is passed as env-var "SSHPASS"
   With no parameters - password will be taken from stdin

   -h            Show help (this screen)
   -V            Print version information
At most one of -f, -d, -p or -e should be used
```

下面是两种常用提供密码的方式，如果一种都不指定，那么就从标准输入流中读取密码：

-p，表示密码来自于命令行参数：

```text
$ sshpass -p '这里填密码' ssh username@10.42.0.1
```

-e，表示密码来自于环境变量：

```text
$ export SSHPASS='这里填密码'
$ echo $SSHPASS
$ sshpass -e ssh username@10.42.0.1
```

另外，sshpass不仅可以用于执行ssh命令，用于执行scp或者rsync命令也是可以的：

```shell
# 执行rsync命令
$ sshpass -e rsync -r --delete-after --quiet public xxxx@18.163.206.60:~/throwable.cn
# 执行scp命令
$ sshpass -e scp -r public xxxx@18.163.206.60:~/throwable.cn
```


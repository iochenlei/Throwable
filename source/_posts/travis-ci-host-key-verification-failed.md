---
title: Travis CI中报Host key verification failed的解决方法
date: 2020-04-12 09:52:55
tags: travis-ci
---

最近，在Travis CI中使用sshpass + rsync来部署的时候，发现下面这条命令会报：Host key verification failed.

```text
sshpass -e rsync -r --delete-after --quiet public myuser@18.163.206.00:~/throwable.cn
```

是什么原因导致了该问题？其实很简单，如果我们使用SSH客户端连接服务器，为了防止中间人攻击，在第一次连接上服务器时，会有个公钥确让阶段让你选yes或no。

输入yes表示接受，并且把服务器对应的公钥保存到`~/.ssh/known_hosts`文件中，如果之后使用SSH连接时发现服务器的公钥与`~/.ssh/known_hosts`中保存的不一致则会连接失败。

输入no表示不接受，如下所示，如果我们选择no或者直接键入回车都会得到：**Host key verification failed.**

```text
The authenticity of host 'myec2 (18.163.206.00)' can't be established.
ECDSA key fingerprint is SHA256:xxx47SDTj2IfL52BLVmYnFD1FwTc/HNdlh1712xxxx.
Are you sure you want to continue connecting (yes/no)? no
Host key verification failed.
```

那么是否可以直接输入yes来解决问题？理论上是这样的，但是别忘了我们是在CI环境中，我们无法使用键盘，尽管我们已经使用了sshpass来完成自动输入密码的工作，但是sshpass目前不能完成服务器公钥确让这一步。经过一番Google之后，我在Travis CI的文档中找到了下面这个解决方法。

假设服务器的地址是`git.example.com`，那么只需要在`.travis.yml`文件中添加下面这一节配置即可：

```yml
addons:
  ssh_known_hosts: git.example.com
```

如果有多台服务器，那么可以把该参数配置为数组：

```yml
addons:
  ssh_known_hosts:
  - git.example.com
  - 111.22.33.44
```

如果使用的不是22端口，那么还可以指定一个端口：

```yml
addons:
  ssh_known_hosts: git.example.com:1234
```


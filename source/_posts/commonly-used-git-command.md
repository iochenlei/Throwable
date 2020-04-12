---
title: Git常用命令
date: 2020-04-12 10:39:56
tags: git
categories: Git
---

## 删除远程仓库中的分支

```java
git push origin -d gh-pages
```

+ -d是--delete的缩写
+ gh-pages是被删除的远程分支
+ origin是gh-pages分支所在的远程仓库

## 克隆指定分支的最近N次提交记录

```text
git clone --depth=1 --branch=master https://github.com/iochenlei/Throwable.git iochenlei/Throwable
```

+ --depth=1表示只克隆分支上最近的一次提交记录
+ --branch=master指定要克隆的分支

这样做的优点是可以大幅提升克隆的速度(对于大仓库也许可以提升CI的构建速度)，缺点是不能获得完整的仓库。


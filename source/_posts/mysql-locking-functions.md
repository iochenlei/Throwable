---
title: MySQL锁函数
date: 2020-04-13 10:39:05
tags:
- 锁
categories:
- MySQL
---

| 名称                | 描述             |
| ------------------- | ---------------- |
| GET_LOCK()          | 获取指定名称的锁 |
| IS_FREE_LOCK()      | 检测锁是否空闲   |
| IS_USED_LOCK()      | 检测是否被占用   |
| RELEASE_ALL_LOCKS() | 释放全部锁       |
| RELEASE_LOCK()      | 释放指定名称的锁 |

**GET_LOCK(str, timeout)**

+ str，锁的名称，在MySQL 5.7或更高的版本中，它的长度是64个字符
+ timeout，超时时间, 如果timeout是负数表示无限等待

该函数获取到的锁是排它锁，同样名称的锁在同一时刻只能被一个Session所持有，如果其它的Session试图获取占用中的锁则会被阻塞。

如果获取锁成功则返回1，如果获取锁超时则返回0，如果发生错误则返回NULL(例如运行内存不足或Session线程被Kill)。

使用GET_LOCK()取到的锁需要显式调用RELEASE_LOCK()释放，或在Session终止时被隐式释放。而且在事务回滚或者提交时也不会释放GET_LOCK()取到的锁。

此外，在一个Session中多次可以获取同样名称的锁，除非在该Session调用同样次数的RELEASE_LOCK()函数来释放掉所有获取到的锁，否则其它的Session不能获取到该名称的锁。

当GET_LOCK()成功获取锁时，MySQL会在performance_schema数据库的`metadata_locks`表中注册一条与该锁对应的记录。`OBJECT_TYPE`列的值为`USER LEVEL LOCK`而`OBJECT_NAME`则是锁的名称。如果一个Session中多次获取了同样名称的锁，也只会有一条记录保存在`metadata_locks`表中。当该锁被释放时，`metadata_locks`中对应的记录也被删除。

<!--more-->

**IS_FREE_LOCK(str)**

检测锁是否为空闲状态，返回1表示锁空闲，返回0表示锁已被占用，返回NULL表示发生错误。

**IS_USED_LOCK(str)**

检测锁是否为占用状态，如果锁已被占用则返回锁持有者的连接标识符，否则返回NULL。

**RELEASE_ALL_LOCKS()**

释放当前线程持有的全部锁，返回值是被释放的锁数量。

**RELEASE_LOCK(str)**

释放名称为str的锁，返回1表示锁释放成功，返回0表示锁的持有者不是当前Session，返回NULL表示名称为str的锁不存在。

**注意：上面这些函数无法通过binlog复制到从机上。**


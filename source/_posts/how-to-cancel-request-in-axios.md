---
title: 如何在axios中取消请求
date: 2020-04-19 10:48:13
tags:
- axios
categories:
- 前端摸鱼
---

最近，需要实现这样一个需求，我在单页面应用中使用axios发送Long Polling请求对某业务的状态进行更新，请求的等待超时时间为30s，如果等待期间页面路由发生改变(即组件销毁)，此时则应该取消等待中的请求。一番搜索之后，我在axios的README中找到了下面这个解决办法：

axios中基于[可取消的Promise提案](https://github.com/tc39/proposal-cancelable-promises)实现了cancel token接口，于是我们可以像下面这样使用`CancelToken.source()`来创建一个cancel token：

```javascript
const CancelToken = axios.CancelToken;
const source = CancelToken.source();

axios.get('/user/12345', {
  cancelToken: source.token
}).catch(function (thrown) {
  // 判断错误是不是由source.cancel操作引起的
  if (axios.isCancel(thrown)) {
    // thrown.message对应的是调用source.cancel()所传入的参数
    console.log('Request canceled', thrown.message);
  } else {
    // handle error
  }
});

axios.post('/user/12345', {
  name: 'new name'
}, {
  cancelToken: source.token
})

// 取消请求(cancel函数的参数是可选的)
source.cancel('Operation canceled by the user.');
```

还有一种方法是传递一个`executor`函数给`CancelToken`构造器：

```javascript
const CancelToken = axios.CancelToken;
let cancel;

axios.get('/user/12345', {
  cancelToken: new CancelToken(function executor(c) {
    // executor会收到一个函数c作为参数传入，调用函数c即可取消请求
    cancel = c; // 把函数c存入外部变量
  })
});

// 取消请求
cancel();
```

最后，axios的文档上还指出，多个请求可以共享同样的token。如此一来，调用一次`cancel()`就能同时取消多个请求。


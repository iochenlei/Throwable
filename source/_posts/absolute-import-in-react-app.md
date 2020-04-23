---
title: 在React应用开发中使用绝对路径导入组件
date: 2020-04-23 16:38:41
tags:
- React
categories:
- 前端
---

默认情况下，在对由React脚手架创建的应用进行开发时，我们会使用相对路径来引入组件(假设该组件位于`src/components/SubmittingToolbar`目录)：

```text
import SubmittingToolbar from '../../../../components/SubmittingToolbar';
```

如果组件的路径很深，就会出现上面演示的这种情况：在路径前面有一串`../../../../`。这看起来有点丑陋，而且也不容易看出`SubmittingToolbar`组件所在的位置。

此时，使用绝对路径来导入组件就很有必要了：

```text
import SubmittingToolbar from 'components/SubmittingToolbar';
```

那么我们如何配置才能达到该目的？其实很简单，只需要在根目录创建一个`jsconfig.json`文件：

```json
{
  "compilerOptions": {
    "baseUrl": "src"
  },
  "include": ["src"]
}
```

如果你配置了eslint，为了避免eslint误报，还要进行额外的配置，在eslint的配置文件的`settings`中加入以下内容：

```json
{
  "settings": {
    "import/resolver": {
      "node": {
        "paths": ["src"]
      }
    }
  }
}
```


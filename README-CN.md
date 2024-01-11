# Alibaba Cloud Developer Toolkit for JetBrains

[![CI](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml/badge.svg)](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)

Alibaba Cloud Developer Toolkit for JetBrains 让您访问阿里云服务更加便捷.

## 支持的 IDE
IntelliJ IDEA(Ultimate, Community) 2022.2 ~ 2023.3

Pycharm (Professional, Community) 2022.2 ~ 2023.3

GoLand 2022.2 ~ 2023.3

## 开始

### 安装
您可以通过在 IDE 中的插件市场中搜索“Alibaba Cloud Developer Toolkit”来安装，或者通过浏览器跳转 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit) 来获取下载文件。
![Install](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/install.png)

### 功能
<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/config-user.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/switch-user.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/api-list-with-search.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/api-debug1.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/api-debug2.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/sdkSample.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/auto-import.png" style="display: inline-block;">
</div>


* **用户配置：** 支持通过静态凭证切换和编辑用户配置，与阿里云 CLI 的配置方式一致。 位于 IDE 底部的状态栏显示当前用户。 [了解更多 CLI 配置相关](https://help.aliyun.com/document_detail/123181.html?spm=a2c4g.121544.0.0.2d7e76e3XWMs4u)
  * 如果您已经安装了阿里云 CLI，您可以切换已有用户。您可以通过点击“Edit Profile”打开config.json配置文件，进一步编辑用户配置。
  * 如果您还未安装阿里云 CLI，本插件会自动创建一个空的配置文件。


* **产品导航：** 云产品的树状视图，搜索框可以快速导航到对应产品，或者单击某个云产品，可以跳转到对应的 API 列表，API 列表上方的搜索框可以快速导航到您指定的 API。
  点击窗口工具栏中的刷新按钮可以刷新产品列表或 API 展示页面。通过缓存机制，用户再次访问同一 API 时可实现快速加载展示页面。


* **API 文档展示：** 单击某个 API 将打开一个新窗口，显示相应的 API 文档，包括接口描述、请求参数、响应参数和错误码等。


* **API 调试：** 支持对照参数列表进行便捷的 IDE 内 API 调试，或点击“去调试”按钮可快速跳转浏览器进行调试。


* **SDK 示例代码查看：** 支持 IDE 内查看 SDK 示例代码，填写参数后同步生成对应示例代码。支持跳转浏览器获取安装依赖方式、查看源码。对于 Java 
和 Java-async，支持 Maven 依赖的自动导入。


* 更多功能正在开发中，敬请期待。



## 许可证

[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright (c) 2009-present, Alibaba Cloud All rights reserved.
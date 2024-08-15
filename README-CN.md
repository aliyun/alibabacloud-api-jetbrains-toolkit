[English](./README.md) | 简体中文

# Alibaba Cloud Developer Toolkit for JetBrains

[![CI](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml/badge.svg)](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)

Alibaba Cloud Developer Toolkit for JetBrains 让您访问阿里云服务更加便捷.

## 支持的 IDE
IntelliJ IDEA(Ultimate, Community) 2022.2 ~ 2024.1

Pycharm (Professional, Community) 2022.2 ~ 2024.1

GoLand 2022.2 ~ 2024.1

## 开始

### 安装
* 您可以通过在 IDE 中的插件市场中搜索“Alibaba Cloud Developer Toolkit”来安装

  ![Install-marketplace](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/install-market.png)

* 或者通过浏览器跳转 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit) 下载压缩包，通过 IDE 中的设置-从磁盘安装来完成插件安装。

  ![Install-zip](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/install-zip.png)


## 插件 UI 导览
![UI](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/ui-guide.png)

## 功能

### 用户配置
支持切换和添加用户配置。非必须，如需使用阿里云 API 调试功能，需要配置 AK 信息，如果已安装阿里云CLI，可以复用其配置。[了解更多 CLI 配置相关](https://help.aliyun.com/document_detail/123181.html?spm=a2c4g.121544.0.0.2d7e76e3XWMs4u)

* 如果您已经安装了[阿里云 CLI](https://help.aliyun.com/document_detail/123181.html?spm=a2c4g.121544.0.0.2d7e76e3XWMs4u)，您可以切换、查看已有用户。
* 如果未安装阿里云 CLI，需要时您可以通过点击“New Profile”添加用户配置。
  <div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/new-profile.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/view-profile.png" style="display: inline-block;">
  </div>

### 云产品 & API 搜索
展示云产品的树状视图，搜索框可以快速导航到对应产品或 API。可通过窗口上方的刷新按钮刷新云产品列表或 API 文档页面。

![Search](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/search.png)

### API 文档 & API 调试
单击某个 API 将打开一个新窗口，显示相应的 API 文档，包括接口描述、请求参数、响应参数和错误码等。

支持对照参数列表进行便捷的 IDE 内 API 调试。(**如需调试 API 请提前配置用户信息。**)

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/api-doc.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/debug.png" style="display: inline-block;">
</div>

### SDK 示例代码
支持查看和快速打开 SDK 示例代码，支持 Maven 和 Python 依赖的自动导入。

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/code-sample.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/auto-import.png" style="display: inline-block;">
</div>

### Code Snippets
支持 Java，Java 异步， Python 和 Go SDK 示例代码的自动补全、插入和依赖导入，可通过快捷键 `ctrl + cmd + p`
或 >设置 >Alibaba Cloud Developer Toolkit 开关此功能。

插件启动后约半分钟（拉取元数据）后可使用此功能，如果拉取失败，可手动重新拉取。

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/codesnippets.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/codesnippets_res.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/codesnippets_switch.png" style="display: inline-block;">
</div>

### 文档增强
编写 SDK 代码时，支持在弹出的文档中展示 OpenAPI 的描述信息及更多相关示例链接，以获得更多示例参考。前提：IDE 允许弹出文档（勾选
「设置-编辑器-常规-代码补全-显示文档弹出窗口）。

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/enhance.png" style="display: inline-block;">
</div>

### 内嵌提示
编写阿里云 OpenAPI 相关代码时，支持在代码中显示内嵌提示，点击可以打开 API 详情页面，进行 API 文档、示例代码的查看以及 API 调试。
<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/hints.png" style="display: inline-block;">
</div>

## 许可证

[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright (c) 2009-present, Alibaba Cloud All rights reserved.
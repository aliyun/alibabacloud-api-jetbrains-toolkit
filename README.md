English | [简体中文](./README-CN.md)

# Alibaba Cloud Developer Toolkit for JetBrains

[![CI](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml/badge.svg)](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)

The Alibaba Cloud Developer Toolkit for JetBrains makes it easier to access Alibaba Cloud services.

## Supported IDEs
IntelliJ IDEA(Ultimate, Community) 2022.2 ~ 2024.1

Pycharm (Professional, Community) 2022.2 ~ 2024.1

GoLand 2022.2 ~ 2023.3

## Getting Start

### Installation
* You can download the Alibaba Cloud Developer Toolkit for JetBrains by searching for "Alibaba Cloud Developer Toolkit" in
  the Marketplace in your IDE.
  ![Install-marketplace](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/install-market.png)
* Or download zip file from your web browser through [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit) and choose "Install Plugin from Disk..." in
  settings to finish installation.
  ![Install-zip](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/install-zip.png)

### Features

* **Profile Configuration:** Support switch and new user profiles. Not necessary but needed to be configured when execute API debug.
  If you have installed the Alibaba Cloud CLI, you can reuse its configuration.


* **Product Explorer:** A tree-view of Alibaba Cloud products with search, quickly navigate you to the corresponding products or APIs.


* **API Document View:** Clicking on an API can navigate you to a new tab which displays the corresponding API document,
  including descriptions, request parameters, response parameters and error codes.


* **API Debug:** Support convenient API debugging within the IDE against the parameter form.


* **SDK Code Sample View:** Support viewing, quickly opening SDK code samples and automatic import of Maven and Python dependencies.


* **SDK Code Sample Auto-completion:** Support auto-completion of Java, Java Async and Python SDK code sample. Switch on and off 
  by keyboard shortcuts `ctrl + cmd + p` or check in >settings >Alibaba Cloud Developer Toolkit.


* See the [user guide](https://help.aliyun.com/zh/openapi/user-guide/using-the-alibaba-cloud-developer-toolkit-plugin-in-jetbrains-ides)
  for more details. More features are under development, please stay tuned and welcome to submit your questions and suggestions on the [experience questionnaire](https://g.alicdn.com/aes/tracker-survey-preview/0.0.13/survey.html?pid=fePxMy&id=3494).

## Plugin UI Guide
![UI](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/ui.png)

### Profile Configuration

* If you have already installed [Alibaba Cloud CLI](https://help.aliyun.com/document_detail/123181.html?spm=a2c4g.121544.0.0.2d7e76e3XWMs4u), you can switch your existing profiles.
* Without Alibaba Cloud CLI installed, you can select "New Profile" to new user profiles when needed.
  <div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/new-profile.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/view-profile.png" style="display: inline-block;">
  </div>

### Product and API Searching

Refresh the product list or API detail page by clicking the refresh button in the window toolbar.

![Search](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/search.png)

### API Document and Debug

Please configure the user profile in advance if you need debug API.

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/api-doc.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/debug.png" style="display: inline-block;">
</div>

### SDK Code Sample

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/code-sample.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/auto-import.png" style="display: inline-block;">
</div>

### Code Snippets

Available about half a minute after plugin started (loading metadata). If the pull fails, you can pull it again manually.

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/codesnippets.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/codesnippets_res.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/codesnippets_switch.png" style="display: inline-block;">
</div>

## Licensing

[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright (c) 2009-present, Alibaba Cloud All rights reserved.
English | [简体中文](./README-CN.md)

# Alibaba Cloud Developer Toolkit for JetBrains

[![CI](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml/badge.svg)](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)

The Alibaba Cloud Developer Toolkit for JetBrains makes it easier to access Alibaba Cloud services.

## Supported IDEs
IntelliJ IDEA(Ultimate, Community) 2022.2 ~ 2024.1

Pycharm (Professional, Community) 2022.2 ~ 2024.1

GoLand 2022.2 ~ 2024.1

## Getting Start

### Installation
* You can download the Alibaba Cloud Developer Toolkit for JetBrains by searching for "Alibaba Cloud Developer Toolkit" in
  the Marketplace in your IDE.
  ![Install-marketplace](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/install-market.png)
* Or download zip file from your web browser through [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit) and choose "Install Plugin from Disk..." in
  settings to finish installation.
  ![Install-zip](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/install-zip.png)


## Plugin UI Guide
![UI](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/ui-15-en.png)

## Features

### Profile Configuration
Support switch and new user profiles. Not necessary but needed to be configured when execute API debug.
If you have installed the Alibaba Cloud CLI, you can reuse its configuration.

* If you have already installed [Alibaba Cloud CLI](https://help.aliyun.com/document_detail/123181.html?spm=a2c4g.121544.0.0.2d7e76e3XWMs4u), you can switch your existing profiles.
* Without Alibaba Cloud CLI installed, you can select "New Profile" to new user profiles when needed.
  <div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/new-profile-15-en.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/view-profile-15-en.png" style="display: inline-block;">
  </div>

### Product and API Searching
A tree-view of Alibaba Cloud products with search, quickly navigate you to the corresponding products or APIs.

Refresh the product list or API detail page by clicking the refresh button in the window toolbar.

![Search](https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/search-15-en.png)

### API Document and Debug
Clicking on an API can navigate you to a new tab which displays the corresponding API document,
including descriptions, request parameters, response parameters and error codes.

Support convenient API debugging within the IDE against the parameter form. (**Please configure the user profile
in advance if you need debug API.**)

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/api-doc-15-en.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/api-debug-15-en.png" style="display: inline-block;">
</div>

### SDK Code Sample
Support viewing, quickly opening SDK code samples and automatic import of Maven and Python dependencies.

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/sdk-code-sample-15-en.png" style="display: inline-block;">
</div>

### Code Snippets
Support auto-completion of Java, Java Async, Python and Go SDK code sample. Switch on and off
by keyboard shortcuts `ctrl + cmd + p` or check in >settings >Alibaba Cloud Developer Toolkit.

Available about half a minute after plugin started (loading metadata). If the pull fails, you can pull it again manually.

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/code-snippets-15-en.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/code-snippets-auto-install-15-en.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/code-snippets-enable-15-en.png" style="display: inline-block;">
</div>

### Document Enhancement
#### Related Code Samples
When writing SDK code, it supports displaying OpenAPI description information and more relevant example links
in the documentation popup to obtain more code samples. Prerequisite: IDE allows documentation popup (check
「settings-Editor-General-Code Completion-Show Documentation Popup」).

#### Inlay Hints for API Info
Support inlay hints for viewing API Info when coding with Cloud OpenAPI. Click on the hints can navigate you
to API detail page to view API documentation and code sample, and debug the API.
<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/v0.0.15/en/doc-enhance-15-en.png" style="display: inline-block;">
</div>

### Plaintext AK Inspections and Warning
Support inspections of plaintext AK pairs in code. The warning given will not affect the normal operation of your code.
We only use regular expressions to match the plaintext AK in your code and will not save or upload anything.

>An Alibaba Cloud account has full access to all resources of the account. AccessKey pair leaks of an Alibaba Cloud account
pose critical threats to the system. It is not recommended to use AK in plaintext.

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/ak-inspections.png" style="display: inline-block;">
</div>

You can close this inspections in settings (strongly recommended to keep it enabled):
<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/close-ak.png" style="display: inline-block;">
</div>

## Licensing

[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright (c) 2009-present, Alibaba Cloud All rights reserved.
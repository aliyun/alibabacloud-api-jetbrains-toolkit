# Alibaba Cloud Developer Toolkit for JetBrains

[![CI](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml/badge.svg)](https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/actions/workflows/ci.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23361-alibaba-cloud-developer-toolkit.svg)](https://plugins.jetbrains.com/plugin/23361-alibaba-cloud-developer-toolkit)

The Alibaba Cloud Developer Toolkit for JetBrains makes it easier to access Alibaba Cloud services.

## Supported IDEs
IntelliJ IDEA Community 2022.2+

IntelliJ IDEA Ultimate 2022.2+

## Getting Start

### Installation
You can download the Alibaba Cloud Developer Toolkit for JetBrains by searching for "Alibaba Cloud Developer Toolkit" in the JetBrains
Marketplace in your IDE, or download installation files from your web browser.

### Features

<div style="overflow-x: scroll; white-space: nowrap;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/config-user.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/switch-user.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/api-list-with-search.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/api-debug1.png" style="display: inline-block;">
    <img src="https://aliyunsdk-pages.alicdn.com/plugin_demo/idea/pics/api-debug2.png" style="display: inline-block;">
</div>

* **Profile Configuration** Support switch and edit user profile through static credentials, which is consistent with Alibaba
  Cloud CLI. The StatusBar located at the bottom of the IDE displays the current profile. [Learn more about CLI Configuration](https://help.aliyun.com/document_detail/123181.html?spm=a2c4g.121544.0.0.2d7e76e3XWMs4u)
    * If you have already installed Alibaba Cloud CLI, you can switch your existing profiles. You can open the config.json
      configuration file by clicking "Edit Profile" to further edit user profiles.
    * If you don't have Alibaba Cloud CLI installed, this plugin can automatically create a config file for you and provide
      a default empty configuration.


* **Product Explorer** A tree-view of cloud products with search: Quickly navigate you to the corresponding products or APIs.
  You can refresh the product list or API detail page by clicking the refresh button in the window toolbar.


* **API Document View** Clicking on an API can navigate you to a new tab which displays the corresponding API document,
  including descriptions, request parameters, response parameters and error codes. With cache implemented, subsequent 
  visits to the same API will be loaded swiftly.


* **API Debug (Beta)** Support convenient API debugging within the IDE against the parameter list, and also offers a
  "Go Debug" button for seamless transition to debug in browser.


* More features are under development, please stay tuned.


## Licensing

[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright (c) 2009-present, Alibaba Cloud All rights reserved.
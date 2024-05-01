# Dex加密

## 一、简介

该项目展示了如何对应用加固，且能够一键对安装包进行加固。

## 二、原理

将项目的主要代码对应的 Dex 文件加密，仅保留用于解密的代码。启动应用时，
会在代理的 Application 中加载解密流程，使得应用能够正常运行。

## 三、如何运行项目

1、构建整个项目，会在 workspace 文件夹生成 App-origin.apk、DecryptionLib.aar；

2、请确保SDK目录中下载了版本为 30 或者更低的“build-tools”，因为第3步执行命令行时会用到，
而且高版本的 build-tools 里没有 dx.bat。代码中默认使用了 30.0.3 版本，如果本地没有此版本，
需要在代码中指定其他版本：
```java
private static String getBuildToolsPath() {
    // build-tools 版本号
    String buildToolsVersion = "30.0.3";
    // ...
}
```

3、打开 EncryptionLib 模块，运行 MainTest 类的 doEncryption 方法，会执行 App 加密逻辑，
包含解压、Dex加密、重新打包、对齐以及签名操作。最终会生成 App-signed-aligned.apk 安装包。
使用反编译软件对它反编译时，会无法看到其中的Java类，只能看到加密和解密相关的代码。
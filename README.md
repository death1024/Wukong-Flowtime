## 简介

app文件夹中为kotlin端，主要负责接收脑电头环的信息，并通过websocket发送出去。为了能够从脑电头环中获得各类情感云信息等，我们依靠回车科技的sdk，集成了回车科技的基础蓝牙功能、蓝牙设备管理界面、情感云SDK、以及自定义的数据展示控件。

Wukong中为控制悟空机器人的文件，主要实现了通过websocket接收脑电信息和情感云信息，并且控制悟空机器人做出对应编码好的动作等，实现了大模型对话聊天、情感监测、互动反馈、脑电模式、冥想模式、运动模式、语音控制等功能


## 模块说明

kotlin端集成以下四个模块，以下几个模块sdk都可以在回车科技的官网上下载：

- [蓝牙SDK](https://github.com/Entertech/Enter-Biomodule-BLE-Android-SDK/tree/master/ble)
- [蓝牙设备管理SDK](https://github.com/Entertech/Enter-Biomodule-BLE-Android-SDK/tree/master/bleuisdk)
- [情感云SDK](https://github.com/Entertech/Enter-AffectiveCloud-Android-SDK)
- [自定义控件](https://github.com/Entertech/Enter-UIComponent-Android-SDK)

悟空机器人中运用到了优必选官方的sdk，同样可以在官网下载：

- [悟空python SDK](https://docs.ubtrobot.com/alphamini/python-sdk/guide.html)

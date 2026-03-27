# 晓物 · AI动物识别 Android App

<p align="center">
  <img src="app/logo/pandaLogo.png" width="120" alt="晓物图标"/>
</p>

<p align="center">
  <strong>拍一张照片，认识世界上的动物</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt=""/>
  <img src="https://img.shields.io/badge/Min%20SDK-26-green.svg" alt=""/>
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt=""/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg" alt=""/>
</p>

---

## 项目简介

晓物是一款 AI 驱动的动物识别 Android 应用。用户拍摄动物照片后，百度 AI 自动识别种类（支持 8000+ 种，准确率 >95%），字节豆包生成结构化科普内容，识别结果可收录进「动物图鉴」，配合四级成就系统激励用户持续探索。

---

## 功能特性

### 🔍 智能识别
- 拍照或从相册选图，自动识别动物种类
- 支持 8000+ 种动物，识别准确率 >95%
- 返回 Top3 候选结果，置信度展示
- 识别失败支持手动标注

### 📖 科普内容
- 字节豆包 AI 生成结构化科普内容
- 包含学名、栖息地、食性、寿命、濒危等级、详细介绍
- IUCN 濒危等级彩色标签（LC/NT/VU/EN/CR/DD）
- 支持手动刷新科普内容

### 📚 动物图鉴
- 3列网格展示已收录动物
- 照片墙：同一动物多张历次拍摄照片
- 支持设置封面照片
- 发现时间、发现地点记录
- 用户备注功能
- 搜索功能

### 🏆 成就系统
- 四级成就：初级探险家(10种) / 中级博物学家(30种) / 高级动物学家(60种) / 传奇收藏家(100种)
- 收录新动物时自动检测成就
- 成就解锁庆祝弹窗动画

### 📷 历史记录
- 每次识别自动记录（含失败记录）
- 识别地点、时间记录
- 点击历史记录查看详情
- 支持搜索、单条删除、清空

---

## 技术栈

| 模块 | 技术 |
|------|------|
| UI框架 | Jetpack Compose + Material3 |
| 架构模式 | MVVM + Clean Architecture |
| 依赖注入 | Hilt |
| 相机 | CameraX |
| 网络请求 | Retrofit + OkHttp |
| 本地存储 | Room Database |
| 图片加载 | Coil |
| 异步处理 | Kotlin Coroutines + Flow |
| 动物识别 | 百度AI动物识别API |
| 科普生成 | 字节豆包API（Doubao-1.5-pro-32k） |
| 位置服务 | Android LocationManager |

---

## 系统架构
```
app/
├── data/
│   ├── remote/          # 网络请求（百度API、豆包API）
│   ├── local/           # Room数据库（图鉴、历史、照片）
│   ├── location/        # 位置获取
│   └── repository/      # 数据仓库
├── domain/
│   ├── model/           # 数据模型
│   └── achievement/     # 成就系统
├── ui/
│   ├── camera/          # 相机页
│   ├── result/          # 识别结果页
│   ├── pokedex/         # 图鉴页 + 详情页
│   ├── history/         # 历史记录页 + 详情页
│   ├── common/          # 公共组件
│   ├── navigation/      # 导航
│   └── theme/           # 主题配色
└── di/                  # 依赖注入模块
```

---

## API 接入

### 百度动物识别API
- 注册地址：https://ai.baidu.com/tech/imagerecognition/animal
- 免费额度：500次/天
- 支持 8000+ 种动物

### 字节豆包API
- 注册地址：https://console.volcengine.com
- 模型：Doubao-1.5-pro-32k
- 免费额度：每天200万Token

---

## 本地运行

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 11+
- Android SDK 26+

### 配置步骤

1. Clone 项目
```bash
git clone https://github.com/用户名/AnimalGuide.git
```

2. 在项目根目录创建 `local.properties`，添加以下配置：
```properties
BAIDU_API_KEY=百度API Key
BAIDU_SECRET_KEY=百度Secret Key
DOUBAO_API_KEY=豆包API Key
DOUBAO_ENDPOINT_ID=豆包Endpoint ID
```

3. 在 Android Studio 中打开项目，Sync Gradle

4. 运行到设备或模拟器

---

## 数据库设计

### 图鉴表（pokedex）
| 字段 | 类型 | 说明 |
|------|------|------|
| animalName | String | 动物名称（主键） |
| scientificName | String | 学名 |
| imageUri | String | 封面图片路径 |
| conservationStatus | String | 濒危等级 |
| description | String | 科普介绍 |
| unlockedAt | Long | 首次收录时间 |
| lastSeenAt | Long | 最近识别时间 |
| latitude | Double? | 发现纬度 |
| longitude | Double? | 发现经度 |
| note | String | 用户备注 |
| recognizeCount | Int | 累计识别次数 |
| isManual | Boolean | 是否手动标注 |

### 历史记录表（history）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Int | 自增主键 |
| animalName | String | 识别到的动物 |
| imageUri | String | 识别图片路径 |
| confidence | Float | 置信度 |
| recognizedAt | Long | 识别时间 |
| isSuccess | Boolean | 是否识别成功 |
| latitude | Double? | 识别位置纬度 |
| longitude | Double? | 识别位置经度 |

### 照片表（animal_photos）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Int | 自增主键 |
| animalName | String | 关联动物名称 |
| imageUri | String | 图片路径 |
| takenAt | Long | 拍摄时间 |

---

## 简历描述

**项目名称**

晓物 · AI动物识别 Android App

**项目描述**

用户拍摄动物照片，百度AI自动识别种类（支持8000+种，准确率>95%），字节豆包生成结构化科普内容，识别结果可收录进「动物图鉴」，记录发现时间与地点，配合照片墙和四级成就系统激励用户持续探索。

**技术要点**

- Jetpack Compose + MVVM + Clean Architecture 三层架构，职责清晰
- CameraX 实现相机实时预览、捏合缩放、点击对焦及相册图片选取
- 双API协同设计：百度动物识别（专业识别）+ 字节豆包（科普内容生成）
- Kotlin Flow 驱动识别状态流转，协程并行处理识别与位置获取
- Room 三表设计（图鉴/历史/照片），持久化收藏记录与历史数据
- Hilt 依赖注入 + Retrofit 网络层封装，支持接口扩展
- 游戏化图鉴+四级成就系统+照片墙，提升用户探索留存体验
- Android LocationManager 获取发现地点，Geocoder 逆地理编码

---

## License
```
MIT License

Copyright (c) 2026 Zer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```
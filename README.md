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

晓物是一款 AI 驱动的动物识别 Android 应用。用户拍摄动物照片后，百度 AI 自动识别种类（支持 8000+ 种），字节豆包生成结构化科普内容，识别结果可收录进「动物图鉴」。内置社区广场、AI 对话、用户关注体系与成就系统，形成完整的动物科普与社交闭环。

---

## 功能特性

### 🔍 智能识别
- 拍照或从相册选图，自动识别动物种类
- 支持 8000+ 种动物，返回 Top3 候选结果与置信度
- 识别失败支持手动标注

### 📖 结构化科普
- 字节豆包 AI 生成五大维度科普内容：
  - 基础信息（学名、科属、俗名、分布区域）
  - 形态特征（体型、外观、雌雄差异、标志性结构）
  - 生态与行为（栖息地、食性、活动习性、繁殖、社会行为）
  - 保护与价值（IUCN 等级、生态作用、与人类关系、数量趋势）
  - 其他信息（寿命、天敌威胁、有趣趣闻）
- 支持手动刷新，每次随机换角度生成

### 📚 动物图鉴
- 3 列网格展示已收录动物
- 照片墙：同一动物多张历次拍摄照片，支持设置封面
- 记录发现时间、地点（逆地理编码显示地名）
- 用户备注、搜索功能

### 🏆 成就系统
- 四级成就：初级探险家(10种) / 中级博物学家(30种) / 高级动物学家(60种) / 传奇收藏家(100种)
- 收录新动物时自动检测，解锁庆祝弹窗动画

### 👥 社区广场
- 发布图文帖子（支持多图、动物标签、位置）
- 推荐（热度排序）/ 最新（时间排序）双 Feed 流
- 点赞、评论（热度置顶）、收藏、楼中楼回复
- 话题 / 标签页：点击标签进入同主题帖子聚合页
- 搜索帖子（标题、内容、标签联合检索）
- 帖子详情 60s 轮询静默刷新评论
- 缓存优先策略：Feed、详情页、评论均先展示本地缓存，再后台静默刷新
- 离线模式：断网时展示缓存内容并标注提示

### 🙋 用户系统
- 手机号注册 / 登录，支持设置密码
- 个人主页：头像、背景图、简介、发帖/收藏/获赞统计
- 关注 / 粉丝列表
- 编辑资料（昵称、头像、背景图、性别、简介）
- 用户主页点击跳转，支持关注他人

### 🤖 AI 对话
- 动物专题对话：基于识别结果，与 AI 深入聊某种动物
- 通用对话：自由提问，对话历史本地持久化
- 多会话管理，支持查看历史对话记录

### 📷 历史记录
- 每次识别自动记录（含失败记录）
- 记录地点、时间、置信度
- 支持搜索、单条删除、清空

---

## 技术栈

| 模块 | 技术 |
|------|------|
| UI 框架 | Jetpack Compose + Material3 BOM 2024.12 |
| 架构模式 | MVVM + Clean Architecture |
| 依赖注入 | Hilt 2.51.1 + KSP |
| 相机 | CameraX 1.3.4 |
| 网络请求 | Retrofit 2.11.0 + OkHttp 4.12.0 |
| 本地存储 | Room 2.6.1（v8，8 张表） |
| 图片加载 | Coil 2.7.0 |
| 异步处理 | Kotlin Coroutines + Flow 1.8.1 |
| 导航 | Navigation Compose 2.7.7 |
| 动物识别 | 百度 AI 动物识别 API |
| 科普生成 | 字节豆包 API（Doubao-1.5-pro-32k） |
| 社区后端 | 腾讯 CloudBase HTTP API |
| 位置服务 | Android LocationManager + Geocoder |

---

## 系统架构

```
app/
├── data/
│   ├── remote/
│   │   ├── BaiduApi / DoubaoApi     # 识别 & 科普生成
│   │   └── cloudbase/               # 社区后端 DataSources
│   ├── local/
│   │   ├── entity/                  # Room 实体
│   │   └── *Dao.kt                  # 8 个 DAO
│   └── repository/                  # 数据仓库层
├── domain/
│   ├── model/                       # 业务模型
│   ├── usecase/                     # UseCases
│   └── achievement/                 # 成就系统
├── ui/
│   ├── camera/                      # 相机页
│   ├── result/                      # 识别结果页
│   ├── pokedex/                     # 图鉴列表 & 详情
│   ├── history/                     # 历史记录
│   ├── social/
│   │   ├── feed/                    # Feed 流
│   │   ├── detail/                  # 帖子详情
│   │   ├── publish/                 # 发帖
│   │   ├── topic/                   # 话题页
│   │   ├── search/                  # 搜索
│   │   ├── profile/                 # 用户主页
│   │   └── follow/                  # 关注/粉丝列表
│   ├── me/                          # 个人中心
│   ├── auth/                        # 登录 & 注册
│   ├── chat/                        # AI 对话
│   ├── qa/                          # 问答首页
│   ├── common/                      # 公共组件
│   ├── navigation/                  # NavGraph + Routes
│   └── theme/                       # 主题配色
└── di/                              # Hilt 模块
```

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
CLOUDBASE_ENV_ID=腾讯CloudBase环境ID
CLOUDBASE_ACCESS_TOKEN=腾讯CloudBase访问Token
```

3. 在 Android Studio 中打开项目，Sync Gradle

4. 运行到设备或模拟器（需 Android 8.0+）

---

## 数据库设计（Room v8）

### pokedex（动物图鉴）
| 字段 | 类型 | 说明 |
|------|------|------|
| animalName | String | 主键 |
| scientificName | String | 拉丁学名 |
| taxonomy | String | 科属 & 俗名 |
| distribution | String | 分布区域 |
| morphology | String | 形态特征 |
| habitat | String | 栖息地 |
| diet | String | 食性 |
| activityPattern | String | 活动习性与繁殖 |
| socialBehavior | String | 社会行为 |
| lifespan | String | 寿命 |
| ecologicalRole | String | 生态价值 |
| funFacts | String | 趣闻 |
| conservationStatus | String | IUCN 等级 |
| description | String | 叙述性简介 |
| imageUri | String | 封面图片路径 |
| unlockedAt / lastSeenAt | Long | 收录/最近识别时间 |
| latitude / longitude | Double? | 发现坐标 |
| recognizeCount | Int | 累计识别次数 |
| note | String | 用户备注 |

### history（识别历史）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Int | 自增主键 |
| animalName | String | 识别结果 |
| imageUri | String | 图片路径 |
| confidence | Float | 置信度 |
| recognizedAt | Long | 识别时间 |
| isSuccess | Boolean | 是否成功 |
| latitude / longitude | Double? | 识别位置 |

### animal_photos（照片墙）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Int | 自增主键 |
| animalName | String | 关联动物 |
| imageUri | String | 图片路径 |
| takenAt | Long | 拍摄时间 |

### cached_posts（帖子缓存）
Feed 首页 + 用户主页帖子缓存，`sortType` 区分 `hot`/`latest`/`user_{uid}`，存储点赞/收藏状态。

### cached_comments（评论缓存）
帖子详情评论首页缓存，按 `postId` 查询。

### cached_users（用户缓存）
用户主页数据缓存，供缓存优先展示。

### chat_messages / chat_conversations（AI 对话）
本地持久化对话记录，支持多会话管理。

---

## 简历描述

**项目名称**

晓物 · AI动物识别 Android App

**项目描述**

独立开发的 Android 应用，集动物 AI 识别、结构化科普、动物图鉴、社区广场与 AI 对话于一体。用户拍照后由百度 AI 识别种类（8000+种），字节豆包生成五维度科普内容，收录至本地图鉴并可分享至社区；社区支持发帖、评论、关注、话题聚合等主流社交功能，AI 对话支持动物专题和通用问答两种模式。

**技术要点**

- Jetpack Compose + MVVM + Clean Architecture 三层架构，UI / Domain / Data 职责清晰分离
- CameraX 实现相机预览、捏合缩放、点击对焦及相册选图
- 双 AI 协同：百度动物识别 API（专业识别）+ 字节豆包（五维度结构化科普生成，随机视角防重复）
- Kotlin Coroutines + Flow 驱动全链路状态流转，协程并行处理识别、科普生成、位置获取
- 缓存优先策略：Room 缓存 Feed / 详情 / 评论，进入页面无 Loading 直接展示，后台静默刷新；互动状态（isLiked/isCollected）写透缓存，重进详情无闪变
- 帖子详情 60s 轮询检测他人新评论，仅有变化时更新 UI，ViewModel 销毁自动取消
- 腾讯 CloudBase HTTP API 实现无服务器社区后端，支持帖子/评论/点赞/收藏/关注全链路
- Room v8 多表设计（图鉴/历史/照片/帖子缓存/评论缓存/用户缓存/对话），含完整迁移链
- Hilt 依赖注入，双 Retrofit 客户端（不同 timeout 配置），Repository 单例保证状态一致
- 游戏化图鉴 + 四级成就系统 + 照片墙，提升用户探索留存体验

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

package com.permissionx.animalguide.domain.model

data class AnimalInfo(
    val name: String,
    val scientificName: String,
    val habitat: String,
    val diet: String,
    val lifespan: String,
    val conservationStatus: String,
    val description: String,
    // 新增字段
    val taxonomy: String = "",         // 科属 + 俗名/别名
    val distribution: String = "",     // 分布区域
    val morphology: String = "",       // 形态特征（体型/外观/性别差异/特殊结构）
    val activityPattern: String = "",  // 活动习性 + 繁殖方式
    val socialBehavior: String = "",   // 社会行为
    val ecologicalRole: String = "",   // 生态作用 + 与人类关系 + 数量趋势
    val researchValue: String = ""     // 科研价值与代表性研究成果
)

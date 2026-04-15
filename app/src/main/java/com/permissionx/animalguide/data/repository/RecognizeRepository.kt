package com.permissionx.animalguide.data.repository

import android.content.Context
import android.net.Uri
import com.permissionx.animalguide.BuildConfig
import com.permissionx.animalguide.data.remote.BaiduApi
import com.permissionx.animalguide.data.remote.DoubaoApi
import com.permissionx.animalguide.data.remote.ImageCompressor
import com.permissionx.animalguide.data.remote.dto.DoubaoMessage
import com.permissionx.animalguide.data.remote.dto.DoubaoRequest
import com.permissionx.animalguide.domain.error.AppError
import com.permissionx.animalguide.domain.error.toAppError
import com.permissionx.animalguide.domain.model.AnimalInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecognizeRepository @Inject constructor(
    private val baiduApi: BaiduApi,
    private val doubaoApi: DoubaoApi,
    @ApplicationContext private val context: Context
) {
    // Token 缓存
    private var cachedToken: String? = null
    private var tokenExpireTime: Long = 0L

    private suspend fun getAccessToken(): String {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpireTime) {
            return cachedToken!!
        }
        val response = baiduApi.getAccessToken(
            apiKey = BuildConfig.BAIDU_API_KEY,
            secretKey = BuildConfig.BAIDU_SECRET_KEY
        )
        cachedToken = response.accessToken
        tokenExpireTime = now + (response.expiresIn - 3600) * 1000
        return response.accessToken
    }

    // 百度识别
    suspend fun recognizeAnimal(imageUri: Uri): Result<List<Pair<String, Float>>> {
        return try {
            val token = getAccessToken()
            val base64 = ImageCompressor.uriToBase64(context, imageUri)
            val response = baiduApi.recognizeAnimal(
                accessToken = token,
                imageBase64 = base64,
                topNum = 3,
                baikeNum = 1
            )
            val results = response.result
                ?.map { Pair(it.name, it.score.toFloat()) }
                ?: emptyList()

            if (results.isEmpty()) {
                Result.failure(AppError.NotAnimalError())
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }

    // 豆包科普生成
    suspend fun generateAnimalInfoOnce(animalName: String): Result<AnimalInfo> {
        val angles = listOf(
            "请重点突出该物种的形态分类特征与近缘种野外辨别要点",
            "请重点突出该物种的保护生物学现状、主要威胁因素及已有保护措施",
            "请重点突出该物种的种群生态、栖息地偏好与微生境需求",
            "请重点突出该物种的行为生态学特征、觅食策略与日活动节律",
            "请重点突出该物种的繁殖策略、繁殖周期及育幼行为的研究进展"
        )
        val randomAngle = angles.random()

        val prompt = """
            请根据动物名称"$animalName"，生成严谨、准确、可供生物科考研究人员参考的物种档案信息。本次重点角度：$randomAngle。
            所有内容须以客观事实为依据，优先引用具体数据（如体重范围、分布海拔、种群数量级别等），避免模糊表述。
            严格以JSON格式返回，不要返回任何其他内容，不要加markdown代码块。
            conservationStatus字段必须且只能是：LC、NT、VU、EN、CR、DD 之一（依据IUCN红色名录最新评估）。
            如该物种有多个亚种保护级别不同，填写指名亚种或最具代表性的等级。
            JSON格式如下：
            {
              "name": "动物中文名",
              "scientificName": "拉丁学名（含命名人及年份，如已知）",
              "conservationStatus": "LC或NT或VU或EN或CR或DD之一",
              "description": "100字左右的物种概述，语言客观严谨，涵盖分类地位、典型特征及研究价值，不重复下面字段的内容",
              "taxonomy": "所属目、科、属，以及常见俗名/别名，如：食肉目 猫科 豹属；别名：金钱豹、豹子",
              "distribution": "主要分布的大洲、国家/地区及垂直海拔范围，简洁列举",
              "habitat": "栖息环境类型及关键生境参数（如温度、植被类型、水源依赖性等）",
              "morphology": "成体体长、体重范围、外观鉴别特征、雌雄差异、标志性形态结构，合并为一段客观描述",
              "diet": "食性分类（肉食/草食/杂食等）、主要食物种类及捕食或觅食策略",
              "activityPattern": "昼行性或夜行性、季节性迁徙或游荡规律、繁殖季节、产仔数、孵化/妊娠期、育幼期长度",
              "socialBehavior": "社会组织形式（独居/群居/配对）、领地范围、种内及种间互作特征",
              "lifespan": "野外与圈养寿命数据、主要天敌及自然死亡率影响因素",
              "ecologicalRole": "营养级地位、关键生态功能（如种子传播、害虫控制等）、种群数量趋势及与人类活动的相互影响",
              "researchValue": "该物种在生态学、进化生物学或保护生物学领域的代表性研究成果或科研价值"
            }
        """.trimIndent()

        return try {
            val response = doubaoApi.generateAnimalInfo(
                authorization = "Bearer ${BuildConfig.DOUBAO_API_KEY}",
                request = DoubaoRequest(
                    model = BuildConfig.DOUBAO_ENDPOINT_ID,
                    messages = listOf(DoubaoMessage(role = "user", content = prompt))
                )
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(AppError.NotFoundError())

            val clean = content.replace("```json", "").replace("```", "").trim()

            if (!clean.startsWith("{")) {
                return Result.failure(AppError.ParseError())
            }

            try {
                val gson = com.google.gson.Gson()
                val info = gson.fromJson(clean, AnimalInfo::class.java)
                val normalizedStatus = when {
                    info.conservationStatus.uppercase().contains("LC") -> "LC"
                    info.conservationStatus.uppercase().contains("NT") -> "NT"
                    info.conservationStatus.uppercase().contains("VU") -> "VU"
                    info.conservationStatus.uppercase().contains("EN") -> "EN"
                    info.conservationStatus.uppercase().contains("CR") -> "CR"
                    info.conservationStatus.uppercase().contains("DD") -> "DD"
                    else -> "LC"
                }
                Result.success(info.copy(conservationStatus = normalizedStatus))
            } catch (_: Exception) {
                Result.failure(AppError.ParseError())
            }
        } catch (_: java.net.SocketTimeoutException) {
            Result.failure(AppError.TimeoutError())
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }
}
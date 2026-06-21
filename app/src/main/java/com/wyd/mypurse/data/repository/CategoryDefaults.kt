package com.wyd.mypurse.data.repository

import com.wyd.mypurse.domain.model.Category

/**
 * 内置分类种子数据定义。所有默认分类集中管理，便于恢复。
 *
 * V1.0.1 新增 color 字段：每个内置分类有固定颜色，各页面统一从 Category.color 取色。
 * 支出侧分类使用红/橙/紫等暖色系，收入侧使用绿/蓝/青等冷色系。
 */
object CategoryDefaults {

    /** 支出侧默认颜色（未设置 color 时的 fallback）：浅红 */
    const val expenseDefaultColor: Long = 0xFFFFCDD2  // 浅红

    /** 收入侧默认颜色（未设置 color 时的 fallback）：浅绿 */
    const val incomeDefaultColor: Long = 0xFFC8E6C9   // 浅绿

    /**
     * V1.1 新建分类颜色轮转表（8 色）。
     * 新建自定义分类时按已有自定义分类数取模轮转，避免所有新分类共用同一 fallback 色。
     */
    val rotationColors: List<Long> = listOf(
        0xFFFF7043, // 橙
        0xFF42A5F5, // 蓝
        0xFFAB47BC, // 紫
        0xFF43A047, // 绿
        0xFFEF5350, // 红
        0xFF26A69A, // 青
        0xFFEC407A, // 粉
        0xFF5C6BC0, // 靛蓝
    )

    /** 支出侧内置一级分类（flowSign = -1） */
    val expenseCategories: List<Category> = listOf(
        Category(id = 0, name = "餐饮", parentId = null, isDefault = true, sortOrder = 1, flowSign = -1, color = 0xFFFF7043),
        Category(id = 0, name = "交通", parentId = null, isDefault = true, sortOrder = 2, flowSign = -1, color = 0xFF42A5F5),
        Category(id = 0, name = "购物", parentId = null, isDefault = true, sortOrder = 3, flowSign = -1, color = 0xFFAB47BC),
        Category(id = 0, name = "娱乐", parentId = null, isDefault = true, sortOrder = 4, flowSign = -1, color = 0xFFEF5350),
        Category(id = 0, name = "住房", parentId = null, isDefault = true, sortOrder = 5, flowSign = -1, color = 0xFF8D6E63),
        Category(id = 0, name = "通讯", parentId = null, isDefault = true, sortOrder = 6, flowSign = -1, color = 0xFF26A69A),
        Category(id = 0, name = "医疗", parentId = null, isDefault = true, sortOrder = 7, flowSign = -1, color = 0xFFEC407A),
        Category(id = 0, name = "教育", parentId = null, isDefault = true, sortOrder = 8, flowSign = -1, color = 0xFF5C6BC0),
        Category(id = 0, name = "其他支出", parentId = null, isDefault = true, sortOrder = 9, flowSign = -1, color = 0xFFBDBDBD),
    )

    /** 收入侧内置一级分类（flowSign = 1） */
    val incomeCategories: List<Category> = listOf(
        Category(id = 0, name = "工资", parentId = null, isDefault = true, sortOrder = 1, flowSign = 1, color = 0xFF43A047),
        Category(id = 0, name = "奖金", parentId = null, isDefault = true, sortOrder = 2, flowSign = 1, color = 0xFF66BB6A),
        Category(id = 0, name = "理财", parentId = null, isDefault = true, sortOrder = 3, flowSign = 1, color = 0xFF26C6DA),
        Category(id = 0, name = "兼职", parentId = null, isDefault = true, sortOrder = 4, flowSign = 1, color = 0xFF7E57C2),
        Category(id = 0, name = "报销", parentId = null, isDefault = true, sortOrder = 5, flowSign = 1, color = 0xFF29B6F6),
        Category(id = 0, name = "退款", parentId = null, isDefault = true, sortOrder = 6, flowSign = 1, color = 0xFF9CCC65),
        Category(id = 0, name = "红包", parentId = null, isDefault = true, sortOrder = 7, flowSign = 1, color = 0xFFEF5350),
        Category(id = 0, name = "其他收入", parentId = null, isDefault = true, sortOrder = 8, flowSign = 1, color = 0xFFBDBDBD),
    )

    /** 合并后的一级分类（兼容旧代码调用 allCategories 的场景） */
    val allCategories: List<Category>
        get() = expenseCategories + incomeCategories

    /**
     * 支出侧二级分类。key 为一级分类名。
     */
    val expenseSubCategories: Map<String, List<String>> = mapOf(
        "餐饮" to listOf("早餐", "午餐", "晚餐", "零食", "饮品", "水果"),
        "交通" to listOf("公交", "地铁", "打车", "加油", "停车", "火车/高铁"),
        "购物" to listOf("日用品", "服饰", "数码", "家居", "美妆"),
        "娱乐" to listOf("电影", "游戏", "运动", "旅游", "KTV"),
        "住房" to listOf("房租", "水电", "物业", "维修"),
        "通讯" to listOf("话费", "宽带", "快递"),
        "医疗" to listOf("门诊", "药品", "体检"),
        "教育" to listOf("培训", "书籍", "文具"),
        "其他支出" to listOf("捐赠", "宠物"),
    )

    /**
     * 收入侧二级分类。key 为一级分类名。
     */
    val incomeSubCategories: Map<String, List<String>> = mapOf(
        "工资" to listOf("基本工资", "加班费", "补贴"),
        "奖金" to listOf("年终奖", "项目奖金", "绩效奖金"),
        "理财" to listOf("股票", "基金", "利息", "分红"),
        "兼职" to listOf("设计", "写作", "咨询", "讲课"),
        "报销" to listOf("差旅报销", "办公报销", "医疗报销"),
        "退款" to listOf("购物退款", "服务退款"),
        "红包" to listOf("微信红包", "礼金"),
        "其他收入" to listOf("二手出售", "其他"),
    )

    /**
     * 合并后的二级分类（兼容旧代码）。key 为一级分类名。
     */
    val subCategories: Map<String, List<String>>
        get() = expenseSubCategories + incomeSubCategories

    /**
     * V1.1 根据名称和 flowSign 查找内置分类的默认颜色。
     * @return 默认 ARGB 色值，非内置分类返回 null
     */
    fun getDefaultColor(name: String, flowSign: Int): Long? {
        val list = if (flowSign == -1) expenseCategories else incomeCategories
        return list.find { it.name == name }?.color
    }
}

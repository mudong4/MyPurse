package com.wyd.mypurse.data.repository

import com.wyd.mypurse.domain.model.Category

/**
 * 内置分类种子数据定义。所有默认分类集中管理，便于恢复。
 */
object CategoryDefaults {

    /** 支出侧内置一级分类（flowSign = -1） */
    val expenseCategories: List<Category> = listOf(
        Category(id = 0, name = "餐饮", parentId = null, icon = "restaurant", isDefault = true, sortOrder = 1, flowSign = -1),
        Category(id = 0, name = "交通", parentId = null, icon = "directions_car", isDefault = true, sortOrder = 2, flowSign = -1),
        Category(id = 0, name = "购物", parentId = null, icon = "shopping_cart", isDefault = true, sortOrder = 3, flowSign = -1),
        Category(id = 0, name = "娱乐", parentId = null, icon = "movie", isDefault = true, sortOrder = 4, flowSign = -1),
        Category(id = 0, name = "住房", parentId = null, icon = "home", isDefault = true, sortOrder = 5, flowSign = -1),
        Category(id = 0, name = "通讯", parentId = null, icon = "phone", isDefault = true, sortOrder = 6, flowSign = -1),
        Category(id = 0, name = "医疗", parentId = null, icon = "local_hospital", isDefault = true, sortOrder = 7, flowSign = -1),
        Category(id = 0, name = "教育", parentId = null, icon = "school", isDefault = true, sortOrder = 8, flowSign = -1),
        Category(id = 0, name = "其他支出", parentId = null, icon = "more_horiz", isDefault = true, sortOrder = 9, flowSign = -1),
    )

    /** 收入侧内置一级分类（flowSign = 1） */
    val incomeCategories: List<Category> = listOf(
        Category(id = 0, name = "工资", parentId = null, icon = "work", isDefault = true, sortOrder = 1, flowSign = 1),
        Category(id = 0, name = "奖金", parentId = null, icon = "emoji_events", isDefault = true, sortOrder = 2, flowSign = 1),
        Category(id = 0, name = "理财", parentId = null, icon = "trending_up", isDefault = true, sortOrder = 3, flowSign = 1),
        Category(id = 0, name = "兼职", parentId = null, icon = "handyman", isDefault = true, sortOrder = 4, flowSign = 1),
        Category(id = 0, name = "报销", parentId = null, icon = "receipt_long", isDefault = true, sortOrder = 5, flowSign = 1),
        Category(id = 0, name = "退款", parentId = null, icon = "undo", isDefault = true, sortOrder = 6, flowSign = 1),
        Category(id = 0, name = "红包", parentId = null, icon = "redeem", isDefault = true, sortOrder = 7, flowSign = 1),
        Category(id = 0, name = "其他收入", parentId = null, icon = "more_horiz", isDefault = true, sortOrder = 8, flowSign = 1),
    )

    /** 合并后的一级分类（兼容旧代码调用 allCategories 的场景） */
    val allCategories: List<Category>
        get() = expenseCategories + incomeCategories

    /**
     * 支出侧二级分类。key 为一级分类名。
     */
    val expenseSubCategories: Map<String, List<Pair<String, String>>> = mapOf(
        "餐饮" to listOf(
            "早餐" to "free_breakfast",
            "午餐" to "lunch_dining",
            "晚餐" to "dinner_dining",
            "零食" to "bakery_dining",
            "饮品" to "local_cafe",
            "水果" to "grocery",
        ),
        "交通" to listOf(
            "公交" to "directions_bus",
            "地铁" to "subway",
            "打车" to "local_taxi",
            "加油" to "local_gas_station",
            "停车" to "local_parking",
            "火车/高铁" to "train",
        ),
        "购物" to listOf(
            "日用品" to "cleaning_services",
            "服饰" to "checkroom",
            "数码" to "devices",
            "家居" to "chair",
            "美妆" to "face",
        ),
        "娱乐" to listOf(
            "电影" to "theaters",
            "游戏" to "sports_esports",
            "运动" to "fitness_center",
            "旅游" to "flight",
            "KTV" to "mic",
        ),
        "住房" to listOf(
            "房租" to "apartment",
            "水电" to "water_drop",
            "物业" to "business",
            "维修" to "build",
        ),
        "通讯" to listOf(
            "话费" to "phone_android",
            "宽带" to "wifi",
            "快递" to "local_shipping",
        ),
        "医疗" to listOf(
            "门诊" to "medical_services",
            "药品" to "medication",
            "体检" to "biotech",
        ),
        "教育" to listOf(
            "培训" to "cast_for_education",
            "书籍" to "menu_book",
            "文具" to "draw",
        ),
        "其他支出" to listOf(
            "捐赠" to "volunteer_activism",
            "宠物" to "pets",
        ),
    )

    /**
     * 收入侧二级分类。key 为一级分类名。
     */
    val incomeSubCategories: Map<String, List<Pair<String, String>>> = mapOf(
        "工资" to listOf(
            "基本工资" to "payments",
            "加班费" to "schedule",
            "补贴" to "card_giftcard",
        ),
        "奖金" to listOf(
            "年终奖" to "stars",
            "项目奖金" to "military_tech",
            "绩效奖金" to "trending_up",
        ),
        "理财" to listOf(
            "股票" to "show_chart",
            "基金" to "account_balance",
            "利息" to "percent",
            "分红" to "payments",
        ),
        "兼职" to listOf(
            "设计" to "design_services",
            "写作" to "edit_note",
            "咨询" to "psychology",
            "讲课" to "cast_for_education",
        ),
        "报销" to listOf(
            "差旅报销" to "flight",
            "办公报销" to "print",
            "医疗报销" to "local_hospital",
        ),
        "退款" to listOf(
            "购物退款" to "shopping_cart",
            "服务退款" to "support_agent",
        ),
        "红包" to listOf(
            "微信红包" to "chat",
            "礼金" to "featured_seasonal_and_gifts",
        ),
        "其他收入" to listOf(
            "二手出售" to "sell",
            "其他" to "more_horiz",
        ),
    )

    /**
     * 合并后的二级分类（兼容旧代码）。key 为一级分类名。
     */
    val subCategories: Map<String, List<Pair<String, String>>>
        get() = expenseSubCategories + incomeSubCategories
}

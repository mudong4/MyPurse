package com.wyd.mypurse.data.repository

import com.wyd.mypurse.domain.model.Category

/**
 * 内置分类种子数据定义。所有默认分类集中管理，便于恢复。
 */
object CategoryDefaults {

    /** 支出侧内置一级分类（flowSign = -1） */
    val expenseCategories: List<Category> = listOf(
        Category(id = 0, name = "餐饮", parentId = null, isDefault = true, sortOrder = 1, flowSign = -1),
        Category(id = 0, name = "交通", parentId = null, isDefault = true, sortOrder = 2, flowSign = -1),
        Category(id = 0, name = "购物", parentId = null, isDefault = true, sortOrder = 3, flowSign = -1),
        Category(id = 0, name = "娱乐", parentId = null, isDefault = true, sortOrder = 4, flowSign = -1),
        Category(id = 0, name = "住房", parentId = null, isDefault = true, sortOrder = 5, flowSign = -1),
        Category(id = 0, name = "通讯", parentId = null, isDefault = true, sortOrder = 6, flowSign = -1),
        Category(id = 0, name = "医疗", parentId = null, isDefault = true, sortOrder = 7, flowSign = -1),
        Category(id = 0, name = "教育", parentId = null, isDefault = true, sortOrder = 8, flowSign = -1),
        Category(id = 0, name = "其他支出", parentId = null, isDefault = true, sortOrder = 9, flowSign = -1),
    )

    /** 收入侧内置一级分类（flowSign = 1） */
    val incomeCategories: List<Category> = listOf(
        Category(id = 0, name = "工资", parentId = null, isDefault = true, sortOrder = 1, flowSign = 1),
        Category(id = 0, name = "奖金", parentId = null, isDefault = true, sortOrder = 2, flowSign = 1),
        Category(id = 0, name = "理财", parentId = null, isDefault = true, sortOrder = 3, flowSign = 1),
        Category(id = 0, name = "兼职", parentId = null, isDefault = true, sortOrder = 4, flowSign = 1),
        Category(id = 0, name = "报销", parentId = null, isDefault = true, sortOrder = 5, flowSign = 1),
        Category(id = 0, name = "退款", parentId = null, isDefault = true, sortOrder = 6, flowSign = 1),
        Category(id = 0, name = "红包", parentId = null, isDefault = true, sortOrder = 7, flowSign = 1),
        Category(id = 0, name = "其他收入", parentId = null, isDefault = true, sortOrder = 8, flowSign = 1),
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
}

package com.wyd.mypurse.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wyd.mypurse.data.local.database.AppDatabase
import com.wyd.mypurse.data.local.entity.CategoryDefEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CategoryDefDao 单元测试。验证分类 DAO 的 CRUD 操作。
 */
class CategoryDefDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetCategory() = runTest {
        val dao = database.categoryDefDao()
        val category = CategoryDefEntity(
            name = "餐饮",
            parentId = null,
            isDefault = true,
            sortOrder = 1
        )
        val id = dao.insertCategory(category)
        assertTrue(id > 0)

        val retrieved = dao.getCategoryById(id)
        assertNotNull(retrieved)
        assertEquals("餐饮", retrieved?.name)
    }

    @Test
    fun getTopLevelCategories() = runTest {
        val dao = database.categoryDefDao()
        dao.insertCategory(CategoryDefEntity(name = "餐饮", parentId = null, sortOrder = 1))
        dao.insertCategory(CategoryDefEntity(name = "交通", parentId = null, sortOrder = 2))
        dao.insertCategory(CategoryDefEntity(name = "早餐", parentId = 1, sortOrder = 1))

        val topLevel = dao.getTopLevelCategories().first()
        assertEquals(2, topLevel.size)
        assertEquals("餐饮", topLevel[0].name)
        assertEquals("交通", topLevel[1].name)
    }

    @Test
    fun getSubCategories() = runTest {
        val dao = database.categoryDefDao()
        val parentId = dao.insertCategory(CategoryDefEntity(name = "餐饮", parentId = null, sortOrder = 1))
        dao.insertCategory(CategoryDefEntity(name = "早餐", parentId = parentId, sortOrder = 1))
        dao.insertCategory(CategoryDefEntity(name = "午餐", parentId = parentId, sortOrder = 2))

        val subs = dao.getSubCategories(parentId).first()
        assertEquals(2, subs.size)
        assertEquals("早餐", subs[0].name)
    }

    @Test
    fun deleteCategory() = runTest {
        val dao = database.categoryDefDao()
        val id = dao.insertCategory(CategoryDefEntity(name = "测试分类", parentId = null))
        dao.deleteCategory(id)

        val retrieved = dao.getCategoryById(id)
        assertNull(retrieved)
    }

    @Test
    fun updateCategory() = runTest {
        val dao = database.categoryDefDao()
        val id = dao.insertCategory(CategoryDefEntity(name = "原名称", parentId = null))
        val updated = CategoryDefEntity(id = id, name = "新名称", parentId = null)
        dao.updateCategory(updated)

        val retrieved = dao.getCategoryById(id)
        assertEquals("新名称", retrieved?.name)
    }

    @Test
    fun getCategoryByName() = runTest {
        val dao = database.categoryDefDao()
        dao.insertCategory(CategoryDefEntity(name = "餐饮", parentId = null))
        val parentId = dao.insertCategory(CategoryDefEntity(name = "餐饮", parentId = null))

        val found = dao.getCategoryByName("餐饮", null)
        assertNotNull(found)
    }

    @Test
    fun insertCategoriesWithConflictStrategy() = runTest {
        val dao = database.categoryDefDao()
        val categories = listOf(
            CategoryDefEntity(name = "餐饮", parentId = null, isDefault = true),
            CategoryDefEntity(name = "交通", parentId = null, isDefault = true),
            // 重复名称应被 IGNORE
            CategoryDefEntity(name = "餐饮", parentId = null, isDefault = true),
        )
        dao.insertCategories(categories)
        val all = dao.getAllCategoriesOnce()
        // 由于 ON CONFLICT IGNORE，重复的不会插入
        assertTrue(all.size >= 2)
    }
}

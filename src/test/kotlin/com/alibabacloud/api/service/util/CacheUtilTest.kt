package com.alibabacloud.api.service.util

import com.alibabacloud.api.service.constants.ApiConstants
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.intellij.ui.treeStructure.Tree
import junit.framework.TestCase.assertFalse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.*
import java.io.File
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlin.io.path.createTempDirectory

internal class CacheUtilTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `test write and read MapCache`() {
        val cacheFile = File(tempDir, "mapCache")
        val expectedMap = mutableMapOf(
            "key1" to listOf("val1", "val2", "val3"),
            "key2" to listOf("val4", "val5", "val6")
        )

        CacheUtil.writeMapCache(cacheFile, expectedMap)
        val actualMap = CacheUtil.readMapCache(cacheFile)

        assertEquals(expectedMap, actualMap)
    }

    @Test
    fun `test write and read TreeCache`() {
        val rootNode = DefaultMutableTreeNode("root")
        rootNode.add(DefaultMutableTreeNode("child1"))
        rootNode.add(DefaultMutableTreeNode("child2"))

        val treeModel = DefaultTreeModel(rootNode)
        val tree = Tree(treeModel)

        val cacheFile = File(tempDir, "treeCache")

        CacheUtil.writeTreeCache(cacheFile, tree)

        assertTrue(cacheFile.exists())
        assertTrue(cacheFile.length() > 0)

        val restoredTree = CacheUtil.readTreeCache(cacheFile)
        val restoredTreeNode = restoredTree?.model?.root as? DefaultMutableTreeNode
        assertEquals(restoredTreeNode?.userObject, "root")
        assertEquals(restoredTreeNode?.childCount, 2)
    }

    @Test
    fun `test write and read ApiListCache`() {
        val cacheFile = File(tempDir, "apiListCache")
        val expectedJsonArray = JsonArray().apply {
            add(JsonPrimitive("api1"))
            add(JsonPrimitive("api2"))
        }

        CacheUtil.writeApiListCache(cacheFile, expectedJsonArray)
        val actualJsonArray = CacheUtil.readApiListCache(cacheFile)

        assertEquals(expectedJsonArray, actualJsonArray)

    }

    @Test
    fun `test clean exceed cache`() {
        val originalCachePath = ApiConstants.CACHE_PATH
        val tempDir = createTempDirectory("cleanExceedCacheTest").toFile()
        ApiConstants.CACHE_PATH = tempDir.absolutePath
        val maxCacheNum = 5
        ApiConstants.MAX_CACHE_NUM = maxCacheNum

        for (i in 1..maxCacheNum + 1) {
            File(tempDir, "test$i.txt").apply {
                val created = createNewFile()
                if (created) {
                    setLastModified(System.currentTimeMillis() - i * 1000)
                }
            }
        }

        CacheUtil.cleanExceedCache()

        val remainingFiles = tempDir.listFiles() ?: emptyArray()
        assertEquals(maxCacheNum, remainingFiles.size)
        Assertions.assertFalse(remainingFiles.any { it.name ==  "test6.txt"})
        tempDir.deleteRecursively()

        ApiConstants.CACHE_PATH = originalCachePath
        ApiConstants.MAX_CACHE_NUM = 200
    }
}
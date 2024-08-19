package com.alibabacloud.api.service.util

import com.alibabacloud.api.service.constants.ApiConstants
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.intellij.ui.treeStructure.Tree
import java.io.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CacheUtil {
    companion object {
        //  缓存 nameAndVersionMap
        fun writeMapCache(file: File, map: MutableMap<String, List<String>>) {
            ObjectOutputStream(FileOutputStream(file)).use { outputStream ->
                outputStream.writeObject(map)
            }
        }

        // 读取 nameAndVersionMap
        fun readMapCache(file: File): MutableMap<String, List<String>> {
            var map: MutableMap<String, List<String>>
            ObjectInputStream(FileInputStream(file)).use { inputStream ->
                map = inputStream.readObject() as MutableMap<String, List<String>>
            }
            return map
        }

        // 缓存 apiContentTree
        fun writeTreeCache(file: File, tree: Tree) {
            val rootNode = tree.model.root as? DefaultMutableTreeNode
            val treeModel = DefaultTreeModel(rootNode)

            ObjectOutputStream(FileOutputStream(file)).use { outputStream ->
                outputStream.writeObject(treeModel)
            }
        }

        // 读取 apiContentTree
        fun readTreeCache(file: File): Tree? {
            var restoredTree: Tree?

            ObjectInputStream(FileInputStream(file)).use { inputStream ->
                val treeModel = inputStream.readObject() as? DefaultTreeModel
                val rootNode = treeModel?.root as? DefaultMutableTreeNode
                restoredTree = Tree(DefaultTreeModel(rootNode))
            }
            return restoredTree
        }

        fun writeApiListCache(file: File, data: JsonArray) {
            val jsonString = data.toString()
            BufferedWriter(FileWriter(file)).use { writer ->
                writer.write(jsonString)
            }
        }

        // 从文件中读取并恢复 API 列表
        fun readApiListCache(file: File): JsonArray {
            var jsonString: String
            BufferedReader(FileReader(file)).use { reader ->
                jsonString = reader.readText()
            }
            return Gson().fromJson(jsonString, JsonArray::class.java)
        }

        fun cleanExceedCache() {
            val cacheFolder = File(ApiConstants.CACHE_PATH)
            var files = cacheFolder.listFiles()?.asList()

            if (files != null && files.size > ApiConstants.MAX_CACHE_NUM) {
                files = files.sortedBy { it.lastModified() }
                while (files?.isNotEmpty() == true && files.size > ApiConstants.MAX_CACHE_NUM) {
                    val oldestFile = files.first()
                    if (oldestFile.delete()) {
                        files = cacheFolder.listFiles()?.asList() ?: emptyList()
                    } else {
                        break
                    }
                }
            }
        }
    }
}

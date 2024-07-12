package com.alibabacloud.api.service.sdksample.document.java

import com.alibabacloud.api.service.sdksample.util.GenerateDocUtil
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.psi.*
import java.util.regex.Pattern

class JavaCodeSampleDocument {
    companion object {
        private val defaultJavaDocProvider: DocumentationProvider? = try {
            JavaDocumentationProvider()
        } catch (e: NoClassDefFoundError) {
            null
        }

        private fun getJavaKeyInfo(name: String?): String? {
            val methodName = name ?: return null
            val pattern = Pattern.compile(
                "^com\\.aliyun(\\.sdk\\.service)?\\.([a-z0-9_-]+)(\\.(models\\.([A-Za-z0-9_-]+Request)|Client))?\$",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = pattern.matcher(methodName)

            return if (matcher.find()) {
                val productVersion = matcher.group(2)
                if (matcher.group(3) == null) {
                    "whenPkg$productVersion"
                } else {
                    val apiName = if (matcher.group(5) != null) {
                        matcher.group(5).removeSuffix("Request")
                    } else {
                        if (matcher.group(4) == "Client")
                            "whenClient"
                        else
                            null
                    }
                    apiName + productVersion
                }
            } else {
                null
            }
        }

        // 获取包含包、类、方法信息等的的全限定名
        private fun getQualifiedJavaName(element: PsiElement): String? {
            when (element) {
                is PsiMethod -> {
                    val methodName = element.containingClass?.qualifiedName
                    return getJavaKeyInfo(methodName)
                }

                is PsiPackage -> {
                    return getJavaKeyInfo(element.qualifiedName)
                }

                is PsiClass -> {
                    val className = element.qualifiedName ?: return null
                    return getJavaKeyInfo(className)
                }

                is PsiJavaCodeReferenceElement -> {
                    return getJavaKeyInfo(element.qualifiedName)
                }

                else -> return null
            }
        }

        private fun getJavaApiSamplesDoc(qualifiedName: String?): String? {
            val classInfo = qualifiedName
                ?.replace("models", "")
                ?.replace("Request", "")
                ?.replace("com", "")
                ?.replace("aliyun", "")
                ?.replace("sdk", "")
                ?.replace("service", "")
                ?.replace("-", "")
                ?.replace("_", "")
                ?.replace(".", "")

            if (classInfo?.startsWith("whenClient") == true || classInfo?.startsWith("whenPkg") == true) {
                val productVersion = classInfo.removePrefix("whenClient").replace("whenPkg", "")
                return GenerateDocUtil.generateProductDoc(productVersion)
            }

            return GenerateDocUtil.generateApiDoc(classInfo)
        }

        internal fun generateJavaDoc(element: PsiElement): String? {
            val originalDoc = defaultJavaDocProvider?.generateDoc(element, null)
            val qualifiedName = getQualifiedJavaName(element)
            val apiSampleDoc = getJavaApiSamplesDoc(qualifiedName)
            return if (originalDoc == null && apiSampleDoc == null) {
                null
            } else {
                listOfNotNull(apiSampleDoc, originalDoc).joinToString("<br><br>")
            }
        }
    }
}
package com.alibabacloud.api.service.inlayhints

import com.alibabacloud.api.service.completion.CompletionIndexPersistentComponent
import com.alibabacloud.models.api.ApiInfo
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.mockito.Mockito.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

class JavaViewApiDocCodeVisionProviderTest : BasePlatformTestCase() {
    private lateinit var provider: JavaViewApiDocCodeVisionProvider
    private lateinit var spyCompletionIndex: CompletionIndexPersistentComponent

    override fun setUp() {
        super.setUp()
        val completionIndex = CompletionIndexPersistentComponent.getInstance()
        spyCompletionIndex = spy(completionIndex)
        spyCompletionIndex.state.completionIndex = mutableMapOf(
            "DescribeRegions::Ecs::2014-05-26" to "查询可以使用的阿里云地域"
        )
        whenever(spyCompletionIndex.needToRefresh()).thenReturn(false)
        provider = JavaViewApiDocCodeVisionProvider(spyCompletionIndex)
    }

    fun `test computeForEditor adds lenses for valid api`() {
        // java
        val psiFile1 = myFixture.configureByText(
            "Test.java",
            """
            public class TestClass {
                public void testMethod() {
                    new com.aliyun.ecs20140526.models.DescribeRegionsRequest();
                }
            }
            """.trimIndent()
        )

        // java
        val psiFile2 = myFixture.configureByText(
            "Test.java",
            """
            import com.aliyun.ecs20140526.models.DescribeRegionsRequest;
            public class TestClass {
                public void testMethod() {
                    new DescribeRegionsRequest();
                }
            }
            """.trimIndent()
        )

        // java-async
        val psiFile3 = myFixture.configureByText(
            "Test.java",
            """
            public class TestClass {
                public void testMethod() {
                    com.aliyun.sdk.service.ecs20140526.models.DescribeRegionsRequest.builder();
                }
            }
            """.trimIndent()
        )

        // java-async
        val psiFile4 = myFixture.configureByText(
            "Test.java",
            """
            import com.aliyun.sdk.service.ecs20140526.models.DescribeRegionsRequest;
            public class TestClass {
                public void testMethod() {
                    DescribeRegionsRequest.builder();
                }
            }
            """.trimIndent()
        )

        val psiFileList = listOf(psiFile1, psiFile2, psiFile3, psiFile4)
        for (psiFile in psiFileList) {
            val classes = psiFile.children.filterIsInstance<PsiClass>()
            val methods = classes.flatMap { it.methods.toList() }
            assertTrue(methods.isNotEmpty())
            val result = provider.computeForEditor(myFixture.editor, psiFile)
            assertTrue(result.isNotEmpty())
            val hint = (result[0].second as ClickableTextCodeVisionEntry).text
            assertEquals("Alibaba Cloud: View API Info", hint)
        }
    }

    fun `test computeForEditor adds lenses for inValid api`() {
        // not alibaba cloud api
        val psiFile1 = myFixture.configureByText(
            "Test.java",
            """
            public class TestClass {
                public void testMethod() {
                    new com.aliyun.ecs20140526.models.MyRequest();
                }
            }
            """.trimIndent()
        )

        // null import
        val psiFile2 = myFixture.configureByText(
            "Test.java",
            """
            public class TestClass {
                public void testMethod() {
                    new DescribeRegionsRequest();
                }
            }
            """.trimIndent()
        )

        val psiFile3 = myFixture.configureByText(
            "Test.java",
            """
            public class TestClass {
                public void testMethod() {
                    DescribeRegionsRequest.builder();
                }
            }
            """.trimIndent()
        )

        val psiFileList = listOf(psiFile1, psiFile2, psiFile3)
        for (psiFile in psiFileList) {
            val classes = psiFile.children.filterIsInstance<PsiClass>()
            val methods = classes.flatMap { it.methods.toList() }
            assertTrue(methods.isNotEmpty())
            val result = provider.computeForEditor(myFixture.editor, psiFile)
            assertTrue(result.isEmpty())
        }
    }

    fun `test for unSupported language`() {
        val unsupportedFile = myFixture.configureByText("Test.xyz", "Unsupported language test")
        val result = provider.computeForEditor(myFixture.editor, unsupportedFile)
        assertTrue(result.isEmpty())
    }

    fun `test find corresponding api info for line`() {
        var line = "new com.aliyun.ecs20140526.models.DescribeRegionsRequest()"
        val regexList = provider.getRegexList("JAVA")
        var apiInfo = provider.findQualifiedApiInfoInCode(line, regexList!!)
        assertEquals("DescribeRegions", apiInfo?.apiName)
        assertEquals("Ecs", apiInfo?.productName)
        assertEquals("2014-05-26", apiInfo?.defaultVersion)

        line = "com.aliyun.sdk.service.ecs20140526.models.DescribeRegionsRequest.builder()"
        apiInfo = provider.findQualifiedApiInfoInCode(line, regexList)
        assertEquals("DescribeRegions", apiInfo?.apiName)
        assertEquals("Ecs", apiInfo?.productName)
        assertEquals("2014-05-26", apiInfo?.defaultVersion)


        line = "com.aliyun.sdk.service.ecs20140526.models.MyRequest.builder()"
        apiInfo = provider.findQualifiedApiInfoInCode(line, regexList)
        assertEquals(null, apiInfo?.apiName)
        assertEquals(null, apiInfo?.productName)
        assertEquals(null, apiInfo?.defaultVersion)
    }

    fun `test find corresponding api info for short line`() {
        val regexList = provider.getRegexList("JAVA")
        var psiFile = myFixture.configureByText(
            "Test.java",
            """
            import com.aliyun.ecs20140526.models.DescribeRegionsRequest;
            public class TestClass {
                public void testMethod() {
                    new DescribeRegionsRequest();
                }
            }
            """.trimIndent()
        )
        var line = "new DescribeRegionsRequest();"
        var shortApiInfo = provider.findShortApiInfoInCode(line, regexList!!, psiFile)
        assertEquals("DescribeRegions", shortApiInfo?.apiInfo?.apiName)
        assertEquals("Ecs", shortApiInfo?.apiInfo?.productName)
        assertEquals("2014-05-26", shortApiInfo?.apiInfo?.defaultVersion)

        psiFile = myFixture.configureByText(
            "Test.java",
            """
            import com.aliyun.sdk.service.ecs20140526.models.DescribeRegionsRequest;
            public class TestClass {
                public void testMethod() {
                    DescribeRegionsRequest.builder().build();
                }
            }
            """.trimIndent()
        )
        line = "DescribeRegionsRequest.builder().build();"
        shortApiInfo = provider.findShortApiInfoInCode(line, regexList, psiFile)
        assertEquals("DescribeRegions", shortApiInfo?.apiInfo?.apiName)
        assertEquals("Ecs", shortApiInfo?.apiInfo?.productName)
        assertEquals("2014-05-26", shortApiInfo?.apiInfo?.defaultVersion)

        psiFile = myFixture.configureByText(
            "Test.java",
            """
            import com.aliyun.sdk.service.ecs20140526.models.MyRequest;
            public class TestClass {
                public void testMethod() {
                    MyRequest.builder().build();
                }
            }
            """.trimIndent()
        )
        line = "MyRequest.builder().build();"
        shortApiInfo = provider.findShortApiInfoInCode(line, regexList, psiFile)
        assertEquals(null, shortApiInfo?.apiInfo?.apiName)
        assertEquals(null, shortApiInfo?.apiInfo?.productName)
        assertEquals(null, shortApiInfo?.apiInfo?.defaultVersion)

    }

    fun `test addLens`() {
        val lenses = mutableListOf<Pair<TextRange, CodeVisionEntry>>()
        val apiInfo = ApiInfo("DescribeRegions", "Ecs", "2014-05-26")
        val line = "new com.aliyun.ecs20140526.models.DescribeRegionsRequest()"
        val overallOffset = 0
        val project: Project = mock()

        provider.addLens(lenses, apiInfo, line, overallOffset, line, project)
        assertEquals(1, lenses.size)
    }
}
package com.alibabacloud.api.service.inspection

import com.alibabacloud.api.service.constants.AKRegex
import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.states.ToolkitSettingsState
import com.intellij.codeInspection.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.regex.Matcher
import java.util.regex.Pattern


class AKLocalInspection : LocalInspectionTool() {
    private val problemDescription = "Alibaba Cloud: " + I18nUtils.getMsg("inspections.tips")

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        if (!ToolkitSettingsState.getInstance().state.isAKInspectionEnabled) {
            return emptyArray()
        }

        val holder = ProblemsHolder(manager, file, isOnTheFly)
        val fileText = file.text

        val akPattern = Pattern.compile(AKRegex.COMMON_AK_REGEX, Pattern.CASE_INSENSITIVE)
        val skPattern = Pattern.compile(AKRegex.COMMON_SK_PATTERN, Pattern.CASE_INSENSITIVE)
        val newAKPattern = Pattern.compile(AKRegex.NEW_AK_REGEX, Pattern.CASE_INSENSITIVE)
        val javaAKPattern = Pattern.compile(AKRegex.JAVA_AK_REGEX, Pattern.CASE_INSENSITIVE)
        val javaSKPattern = Pattern.compile(AKRegex.JAVA_SK_REGEX, Pattern.CASE_INSENSITIVE)
        val goAKPattern = Pattern.compile(AKRegex.GO_AK_REGEX, Pattern.CASE_INSENSITIVE)
        val goSKPattern = Pattern.compile(AKRegex.GO_SK_REGEX, Pattern.CASE_INSENSITIVE)

        val akPatterns = listOf(akPattern, javaAKPattern, goAKPattern)
        val skPatterns = listOf(javaSKPattern, goSKPattern, skPattern)

        for (pattern in akPatterns) {
            val matcher = pattern.matcher(fileText)
            findAndRegisterAKSKPatternProblems(file, matcher, "ak", holder)
        }

        for (pattern in skPatterns) {
            val matcher = pattern.matcher(fileText)
            findAndRegisterAKSKPatternProblems(file, matcher, "sk", holder)
        }

        val akMatcher = newAKPattern.matcher(fileText)
        findAndRegisterNewAKPatternProblems(file, akMatcher, holder)

        return holder.resultsArray

    }

    override fun getDisplayName(): String {
        return "AK inspections"
    }

    override fun getGroupDisplayName(): String {
        return "Alibaba Cloud"
    }


    private fun removeQuotation(origin: String): String {
        return origin.replace("\"", "").replace("'", "")
    }

    private fun isProblemAlreadyRegistered(
        holder: ProblemsHolder,
        akElement: PsiElement?,
    ): Boolean {
        for (problem in holder.resultsArray) {
            if (problem.descriptionTemplate == problemDescription &&
                problem.psiElement == akElement
            ) {
                return true
            }
        }
        return false
    }

    private fun registerProblems(keyElement: PsiElement?, keyType: String, holder: ProblemsHolder) {
        if (!isProblemAlreadyRegistered(holder, keyElement) && keyElement != null) {
            if (keyType == "ak") {
                if (removeQuotation(keyElement.text).length in intArrayOf(16, 20, 22, 24, 26)) {
                    holder.registerProblem(
                        keyElement,
                        problemDescription,
                        ProblemHighlightType.ERROR,
                        AKLocalQuickFix()
                    )
                }
            } else if (keyType == "sk") {
                if (removeQuotation(keyElement.text).length == 30) {
                    holder.registerProblem(
                        keyElement,
                        problemDescription,
                        ProblemHighlightType.ERROR,
                        AKLocalQuickFix()
                    )
                }
            }
        }
    }

    private fun findAndRegisterAKSKPatternProblems(
        file: PsiFile,
        matcher: Matcher,
        keyType: String,
        holder: ProblemsHolder
    ) {
        while (matcher.find()) {
            val startIndex = matcher.start("key")
            val keyElement = file.findElementAt(startIndex)
            registerProblems(keyElement, keyType, holder)
        }
    }

    private fun findAndRegisterNewAKPatternProblems(
        file: PsiFile,
        matcher: Matcher,
        holder: ProblemsHolder
    ) {
        while (matcher.find()) {
            val startIndex = matcher.start("key")
            val keyElement = file.findElementAt(startIndex)
            if (keyElement != null) {
                val key = removeQuotation(keyElement.text.substringAfter("LTAI"))
                if (!isProblemAlreadyRegistered(holder, keyElement) && key.length in intArrayOf(12, 16, 18, 20, 22)) {
                    holder.registerProblem(
                        keyElement,
                        problemDescription,
                        ProblemHighlightType.ERROR,
                        AKLocalQuickFix()
                    )
                }
            }
        }
    }
}



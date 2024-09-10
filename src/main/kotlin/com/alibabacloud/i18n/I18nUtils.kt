package com.alibabacloud.i18n

import com.alibabacloud.constants.PropertiesConstants
import com.intellij.DynamicBundle
import com.intellij.ide.util.PropertiesComponent
import java.util.*

class I18nUtils {
    companion object {
        fun getMsg(key: String): String {
            val bundle = ResourceBundle.getBundle("message.Bundle", getLocale())
            return bundle.getString(key)
        }

        fun getLocale(): Locale {
            val properties = PropertiesComponent.getInstance()
            val userPreferenceLocale = properties.getValue(PropertiesConstants.PREFERENCE_LANGUAGE)
            return if (userPreferenceLocale !== null) {
                if (userPreferenceLocale == "zh_CN") {
                    Locale.CHINA
                } else {
                    Locale.US
                }
            } else if (DynamicBundle.getLocale().language == "zh") {
                Locale.CHINA
            } else {
                Locale.US
            }
        }
    }
}
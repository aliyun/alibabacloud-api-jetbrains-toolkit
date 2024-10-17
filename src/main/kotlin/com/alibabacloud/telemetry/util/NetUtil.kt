package com.alibabacloud.telemetry.util

import org.apache.commons.lang3.StringUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.net.SocketException
import java.util.regex.Pattern


object NetUtil {
    private val FULL_MAC = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$")
    private val MAC = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}")
    private val INVALID_MAC = arrayOf("00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff")
    private val UNIX_COMMAND = arrayOf("/sbin/ifconfig -a || /sbin/ip link")
    private val WIN_COMMAND = arrayOf("getmac")

    val mac: String
        get() {
            if (StringUtils.startsWithIgnoreCase(os, "win")) {
                return commonMac
            }
            return unixMac
        }

    internal val os: String
        get() = System.getProperty("os.name").lowercase()

    private val commonMac: String
        get() {
            val macFromCommand = commandMac
            if (macFromCommand.isNotBlank()) {
                return macFromCommand
            }
            return programMac
        }

    private val unixMac: String
        get() {
            if (macFromName.isBlank()) {
                return commonMac
            }
            return macFromName
        }

    private val commandMac: String
        get() {
            val macs = commandMacList
            return if (macs.isNotEmpty()) macs[0] else StringUtils.EMPTY
        }

    internal val macFromName: String
        get() {
            return try {
                val networkInterface = NetworkInterface.getByName("en0") ?: NetworkInterface.getByName("eth0")
                val hardwareAddress = networkInterface?.hardwareAddress

                if (networkInterface !== null && !networkInterface.isLoopback && !networkInterface.isVirtual && hardwareAddress != null && hardwareAddress.isNotEmpty()) {
                    return hardwareAddress.joinToString(":") { byte -> "%02X".format(byte) }
                }
                ""
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }

    private val commandMacList: List<String>
        get() {
            val macList: MutableList<String> = ArrayList()
            val ret = StringBuilder()
            try {
                val command = if (os.startsWith("win", true)) WIN_COMMAND else UNIX_COMMAND
                val processBuilder = ProcessBuilder(*command)
                val process = processBuilder.start()
                process.inputStream.use { inputStream ->
                    InputStreamReader(inputStream).use { inputStreamReader ->
                        BufferedReader(inputStreamReader).use { br ->
                            var tmp: String?
                            while ((br.readLine().also { tmp = it }) != null) {
                                ret.append(tmp)
                            }
                        }
                    }
                }
                if (process.waitFor() != 0) {
                    throw IOException("Command ${command.joinToString { " " }}")
                }
            } catch (ex: Exception) {
                return macList
            }

            val matcher = MAC.matcher(ret.toString())
            while (matcher.find()) {
                val mac = matcher.group(0)
                if (isValidMac(mac)) {
                    macList.add(mac.replace("-", ":"))
                }
            }
            return macList
        }

    private val programMac: String
        get() {
            val macs = programMacs
            if (macs.isEmpty()) {
                return StringUtils.EMPTY
            }
            return macs[0]
        }

    private val programMacs: List<String>
        get() {
            val macs: MutableList<String> = ArrayList()
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback) {
                        continue
                    }
                    if (networkInterface.hardwareAddress != null) {
                        val mac = networkInterface.hardwareAddress
                        val sb = StringBuilder()
                        for (i in mac.indices) {
                            sb.append(String.format("%02X%s", mac[i], if ((i < mac.size - 1)) ":" else ""))
                        }
                        val macStr = sb.toString()
                        if (isValidMac(macStr)) {
                            macs.add(macStr)
                        }
                    }
                }
            } catch (e: SocketException) {
                return macs
            }
            return macs
        }

    private fun isValidMac(mac: String): Boolean {
        if (mac.isEmpty()) {
            return false
        }
        if (!isValidRawMac(mac)) {
            return false
        }
        val fixedMac = mac.replace("-", ":")
        return !INVALID_MAC.any { it.equals(fixedMac, true) }
    }

    private fun isValidRawMac(raw: String): Boolean {
        return raw.isNotEmpty() && FULL_MAC.matcher(raw).find()
    }
}
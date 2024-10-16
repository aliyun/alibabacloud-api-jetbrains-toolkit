package com.alibabacloud.telemetry.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.NetworkInterface
import java.util.*

class NetUtilTest {
    @Before
    fun setUp() {
        mockkObject(NetUtil)
        every { NetUtil.os }.returns("Mac OS X")
        every { NetUtil.macFromName }.returns("")
    }

    @Test
    fun `test valid MAC address from network interface`() {
        mockkStatic(NetworkInterface::class)
        val mockInterface1 = mockk<NetworkInterface>(relaxed = true)
        every { mockInterface1.hardwareAddress } returns byteArrayOf(0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E)

        val mockInterface2 = mockk<NetworkInterface>(relaxed = true)
        every { mockInterface2.hardwareAddress } returns byteArrayOf(0x6F, 0x7A, 0x0B, 0x1C, 0x2D, 0x3E)

        val interfaces = listOf(mockInterface1, mockInterface2)
        val enumeration = Collections.enumeration(interfaces)
        every { NetworkInterface.getNetworkInterfaces() } returns enumeration

        val mac = NetUtil.mac
        assertEquals("00:1A:2B:3C:4D:5E", mac)
    }


    @Test
    fun `test invalid MAC address`() {
        mockkStatic(NetworkInterface::class)

        val invalidMockInterface1 = mockk<NetworkInterface>(relaxed = true)
        every { invalidMockInterface1.hardwareAddress } returns byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        val invalidMockInterface2 = mockk<NetworkInterface>(relaxed = true)
        every { invalidMockInterface2.hardwareAddress } returns byteArrayOf(
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte()
        )

        val interfaces = listOf(invalidMockInterface1, invalidMockInterface2)
        val enumeration = Collections.enumeration(interfaces)
        every { NetworkInterface.getNetworkInterfaces() } returns enumeration

        val mac = NetUtil.mac
        assertEquals("", mac)
    }
}
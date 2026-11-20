package com.example.ethfinance.data.sms.parser

import com.example.ethfinance.data.local.db.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BankParserTest {

    private val cbeParser = CbeParser()
    private val telebirrParser = TelebirrParser()
    private val genericParser = GenericFallbackParser()

    @Test
    fun cbeParser_debitMessage_parsedSuccessfully() {
        // FAKE test fixture
        val fakeSms = "Dear Customer, Your Account 1*****8018 has been debited with ETB 120.00 on 12/11/2026 at 14:32. Your Current Balance is ETB 6,194.34. Receipt: FT26316XYZ"
        val parsed = cbeParser.parse("CBE", fakeSms, 1794493920000L)

        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed!!.direction)
        assertEquals(12000L, parsed.amountMinor) // 120.00 ETB in santim
        assertEquals(619434L, parsed.balanceAfterMinor) // 6,194.34 ETB in santim
        assertEquals("FT26316XYZ", parsed.reference)
        assertEquals("1*****8018", parsed.accountHint)
    }

    @Test
    fun cbeParser_creditMessage_parsedSuccessfully() {
        // FAKE test fixture
        val fakeSms = "Dear Customer, Your Account 1*****8018 has been Credited with ETB 4,000.00 from EMPLOYER PLC. Your Current Balance is ETB 10,194.34"
        val parsed = cbeParser.parse("CBE", fakeSms, 1794493920000L)

        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed!!.direction)
        assertEquals(400000L, parsed.amountMinor)
        assertEquals(1019434L, parsed.balanceAfterMinor)
        assertEquals("EMPLOYER PLC", parsed.counterparty)
    }

    @Test
    fun cbeParser_otpMessage_ignored() {
        val fakeOtp = "Your CBE Birr OTP is 849201. Do not share this code with anyone."
        val parsed = cbeParser.parse("CBE", fakeOtp, 1794493920000L)
        assertNull(parsed)
    }

    @Test
    fun telebirrParser_transferMessage_parsedSuccessfully() {
        // FAKE test fixture
        val fakeSms = "You have transferred ETB 300.00 to Abebe Kebede (0911****22) on 2026-11-12 14:05. Your telebirr account balance is ETB 1,840.50. Transaction number: ABC123XYZ. Service charge ETB 0.57"
        val parsed = telebirrParser.parse("127", fakeSms, 1794493920000L)

        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed!!.direction)
        assertEquals(30000L, parsed.amountMinor)
        assertEquals(57L, parsed.feeMinor) // 0.57 ETB in santim
        assertEquals(184050L, parsed.balanceAfterMinor)
        assertEquals("ABC123XYZ", parsed.reference)
    }

    @Test
    fun telebirrParser_airtimeMessage_parsedSuccessfully() {
        // FAKE test fixture
        val fakeSms = "You have bought airtime of ETB 50.00 for 0911****22. Balance ETB 1,790.50"
        val parsed = telebirrParser.parse("127", fakeSms, 1794493920000L)

        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed!!.direction)
        assertEquals(5000L, parsed.amountMinor)
        assertEquals(179050L, parsed.balanceAfterMinor)
    }

    @Test
    fun telebirrParser_billPayment_parsedSuccessfully() {
        // FAKE test fixture
        val fakeSms = "You have paid ETB 140.76 to EEU Utility. Transaction number: EEU884920. Balance ETB 1,649.74"
        val parsed = telebirrParser.parse("127", fakeSms, 1794493920000L)

        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed!!.direction)
        assertEquals(14076L, parsed.amountMinor)
        assertEquals(164974L, parsed.balanceAfterMinor)
        assertEquals("EEU884920", parsed.reference)
    }

    @Test
    fun genericParser_fallbackDetected_confidenceForcesReview() {
        val fakeSms = "You have paid 250.00 ETB for your order at merchant store."
        val parsed = genericParser.parse("UNKNOWN_BANK", fakeSms, 1794493920000L)

        assertNotNull(parsed)
        assertEquals(25000L, parsed!!.amountMinor)
        assertEquals(0.40f, parsed.confidence, 0.01f) // Forces review
    }
}

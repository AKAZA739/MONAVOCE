package com.myexpenses.app

import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.domain.parser.NumberWordParser
import com.myexpenses.app.domain.parser.QueryPeriod
import com.myexpenses.app.domain.parser.VoiceCommand
import com.myexpenses.app.domain.parser.VoiceInputParser
import com.myexpenses.app.domain.parser.VoiceParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VoiceInputParserTest {

    private val refDate = LocalDate.of(2026, 11, 22)
    private val categories = DefaultCategories.list

    @Test
    fun `parses simple Arabic expense with digits`() {
        val result = VoiceInputParser.parse("صرفت 500 ريال على الفطور", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(1, exp.items.size)
        assertEquals(500.0, exp.items[0].amount, 0.01)
        assertEquals(DefaultCategories.FOOD, exp.items[0].categoryId)
        assertFalse(exp.needsConfirmation)
    }

    @Test
    fun `parses Arabic spoken dual thousand - الفين ريال بنزين`() {
        val result = VoiceInputParser.parse("دفعت ألفين ريال بنزين", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(1, exp.items.size)
        assertEquals(2000.0, exp.items[0].amount, 0.01)
        assertEquals(DefaultCategories.TRANSPORT, exp.items[0].categoryId)
    }

    @Test
    fun `parses Arabic attached ba prefix with spoken number - بعشرة آلاف ريال`() {
        val result = VoiceInputParser.parse("اشتريت غداء بعشرة آلاف ريال", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(1, exp.items.size)
        assertEquals(10000.0, exp.items[0].amount, 0.01)
        assertEquals(DefaultCategories.FOOD, exp.items[0].categoryId)
    }

    @Test
    fun `parses English groceries expense`() {
        val result = VoiceInputParser.parse("I spent 500 on groceries", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(1, exp.items.size)
        assertEquals(500.0, exp.items[0].amount, 0.01)
        assertEquals(DefaultCategories.GROCERIES, exp.items[0].categoryId)
        assertFalse(exp.needsConfirmation)
    }

    @Test
    fun `parses English transportation expense with Today`() {
        val result = VoiceInputParser.parse("Today I paid 2000 for transportation", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(1, exp.items.size)
        assertEquals(2000.0, exp.items[0].amount, 0.01)
        assertEquals(DefaultCategories.TRANSPORT, exp.items[0].categoryId)
        assertEquals(refDate, exp.items[0].date)
    }

    @Test
    fun `splits multi-expense Arabic utterance into three separate records`() {
        val result = VoiceInputParser.parse("صرفت 500 بقالة و1500 مواصلات و3000 غداء", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(3, exp.items.size)

        assertEquals(500.0, exp.items[0].amount, 0.01)
        assertEquals(DefaultCategories.GROCERIES, exp.items[0].categoryId)

        assertEquals(1500.0, exp.items[1].amount, 0.01)
        assertEquals(DefaultCategories.TRANSPORT, exp.items[1].categoryId)

        assertEquals(3000.0, exp.items[2].amount, 0.01)
        assertEquals(DefaultCategories.FOOD, exp.items[2].categoryId)
    }

    @Test
    fun `handles compound Arabic numbers without wrongly splitting on waw`() {
        val result = VoiceInputParser.parse("خمسة وعشرين ألف إيجار وثلاثة آلاف مواصلات", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(2, exp.items.size)
        assertEquals(25000.0, exp.items[0].amount, 0.01)
        assertEquals(DefaultCategories.BILLS, exp.items[0].categoryId)
        assertEquals(3000.0, exp.items[1].amount, 0.01)
        assertEquals(DefaultCategories.TRANSPORT, exp.items[1].categoryId)
    }

    @Test
    fun `handles Arabic-Indic digits and formatted commas`() {
        val r1 = VoiceInputParser.parse("دفعت ١٠٠٠٠ فاتورة نت", categories, refDate) as VoiceParseResult.Expenses
        assertEquals(10000.0, r1.items[0].amount, 0.01)
        assertEquals(DefaultCategories.BILLS, r1.items[0].categoryId)

        val r2 = VoiceInputParser.parse("Paid 10,000 for pharmacy", categories, refDate) as VoiceParseResult.Expenses
        assertEquals(10000.0, r2.items[0].amount, 0.01)
        assertEquals(DefaultCategories.HEALTH, r2.items[0].categoryId)
    }

    @Test
    fun `handles colloquial fractions and English spoken numbers`() {
        assertEquals(1500.0, NumberWordParser.extractFirstNumber("ألف ونص")!!, 0.01)
        assertEquals(2500.0, NumberWordParser.extractFirstNumber("ألفين ونص")!!, 0.01)
        assertEquals(500.0, NumberWordParser.extractFirstNumber("نص ألف")!!, 0.01)
        assertEquals(5500.0, NumberWordParser.extractFirstNumber("خمسة آلاف وخمسمية")!!, 0.01)
        assertEquals(10500.0, NumberWordParser.extractFirstNumber("ten thousand five hundred")!!, 0.01)
        assertEquals(2500.0, NumberWordParser.extractFirstNumber("2.5k")!!, 0.01)
    }

    @Test
    fun `handles mixed Arabic and English utterance`() {
        val result = VoiceInputParser.parse("صرفت 500 على coffee و 2000 groceries", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(2, exp.items.size)
        assertEquals(500.0, exp.items[0].amount, 0.01)
        assertEquals(DefaultCategories.FOOD, exp.items[0].categoryId)
        assertEquals(2000.0, exp.items[1].amount, 0.01)
        assertEquals(DefaultCategories.GROCERIES, exp.items[1].categoryId)
    }

    @Test
    fun `resolves yesterday date across multiple expenses`() {
        val result = VoiceInputParser.parse("أمس صرفت 500 بقالة و2000 غداء", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = (result as VoiceParseResult.Expenses)
        assertEquals(2, exp.items.size)
        assertEquals(refDate.minusDays(1), exp.items[0].date)
        assertEquals(refDate.minusDays(1), exp.items[1].date)
    }

    @Test
    fun `detects all voice commands accurately`() {
        val qToday = VoiceInputParser.parse("كم صرفت اليوم؟", categories, refDate)
        assertTrue(qToday is VoiceParseResult.Command)
        val c1 = (qToday as VoiceParseResult.Command).command as VoiceCommand.QueryTotal
        assertEquals(QueryPeriod.TODAY, c1.period)
        assertEquals(null, c1.categoryId)

        val qWeek = VoiceInputParser.parse("كم صرفت هذا الأسبوع؟", categories, refDate)
        val c2 = (qWeek as VoiceParseResult.Command).command as VoiceCommand.QueryTotal
        assertEquals(QueryPeriod.THIS_WEEK, c2.period)

        val qFoodMonth = VoiceInputParser.parse("كم صرفت على الأكل هذا الشهر؟", categories, refDate)
        val c3 = (qFoodMonth as VoiceParseResult.Command).command as VoiceCommand.QueryTotal
        assertEquals(QueryPeriod.THIS_MONTH, c3.period)
        assertEquals(DefaultCategories.FOOD, c3.categoryId)

        val del = VoiceInputParser.parse("احذف آخر مصروف", categories, refDate)
        assertTrue((del as VoiceParseResult.Command).command is VoiceCommand.DeleteLastExpense)

        val upd = VoiceInputParser.parse("عدل آخر مصروف إلى 2000 ريال", categories, refDate)
        val c5 = (upd as VoiceParseResult.Command).command as VoiceCommand.UpdateLastExpense
        assertEquals(2000.0, c5.newAmount, 0.01)

        val show = VoiceInputParser.parse("اعرض مصروفات أمس", categories, refDate)
        val c6 = (show as VoiceParseResult.Command).command as VoiceCommand.ShowExpenses
        assertEquals(QueryPeriod.YESTERDAY, c6.period)
    }

    @Test
    fun `flags ambiguous utterance with no description for confirmation`() {
        val result = VoiceInputParser.parse("صرفت 500 ريال", categories, refDate)
        assertTrue(result is VoiceParseResult.Expenses)
        val exp = result as VoiceParseResult.Expenses
        assertEquals(500.0, exp.items[0].amount, 0.01)
        assertTrue(exp.needsConfirmation)
    }

    @Test
    fun `returns Unclear when no amount is spoken`() {
        val result = VoiceInputParser.parse("اشتريت بقالة اليوم", categories, refDate)
        assertTrue(result is VoiceParseResult.Unclear)
    }

    @Test
    fun `matches user-added custom category keywords`() {
        val customCats = categories + CategoryEntity(
            id = "custom_edu",
            nameAr = "التعليم والدورات",
            nameEn = "Education",
            iconName = "school",
            colorHex = 0xFF5C6BC0,
            keywords = "جامعة,كورس,دورة,رسوم دراسية,course,tuition",
            isDefault = false,
            sortOrder = 9
        )
        val result = VoiceInputParser.parse("دفعت 15000 رسوم جامعة", customCats, refDate) as VoiceParseResult.Expenses
        assertEquals(15000.0, result.items[0].amount, 0.01)
        assertEquals("custom_edu", result.items[0].categoryId)
    }

    @Test
    fun `handles offline ASR Arabic phonetic number variants like ستا ألف`() {
        val result = VoiceInputParser.parse("لقد اشتريت ستا ألف حليب", categories, refDate) as VoiceParseResult.Expenses
        assertEquals(1, result.items.size)
        assertEquals(6000.0, result.items[0].amount, 0.01)
        assertEquals(DefaultCategories.GROCERIES, result.items[0].categoryId)
    }
}

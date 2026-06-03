package ncku.calculator;

import ncku.calculator.model.Course;
import ncku.calculator.model.CreditCategory;
import ncku.calculator.model.Student;
import ncku.calculator.service.CreditCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CreditCalculator 單元測試
 * 驗證學分計算的各種情境。
 *
 * <p>遵循 TDD 流程（Red → Green → Refactor）。
 */
@DisplayName("CreditCalculator 學分計算測試")
class CreditCalculatorTest {

    private CreditCalculator calculator;
    private Student student;

    @BeforeEach
    void setUp() {
        calculator = new CreditCalculator();
        student = new Student("B11234567", "測試學生");
    }

    // ─── 測試一：正確計算總學分 ───────────────────────────────────────────────

    @Test
    @DisplayName("TC01：學生無任何課程時，總學分應為 0")
    void testTotalCredits_noCoursesReturnZero() {
        assertEquals(0, calculator.calculateTotalCredits(student));
    }

    @Test
    @DisplayName("TC02：正確計算多門課程的總學分")
    void testTotalCredits_correctSum() {
        student.addCompletedCourse(new Course("C001", "程式設計", 3, CreditCategory.REQUIRED_MAJOR));
        student.addCompletedCourse(new Course("C002", "資料結構", 3, CreditCategory.REQUIRED_MAJOR));
        student.addCompletedCourse(new Course("C003", "通識一", 2, CreditCategory.GENERAL_EDUCATION));

        assertEquals(8, calculator.calculateTotalCredits(student));
    }

    @Test
    @DisplayName("TC03：包含 0 學分課程（體育）時，總學分計算正確")
    void testTotalCredits_withZeroCredits() {
        student.addCompletedCourse(new Course("PE001", "體育一", 0, CreditCategory.SCHOOL_REQUIRED));
        student.addCompletedCourse(new Course("C001", "程式設計", 3, CreditCategory.REQUIRED_MAJOR));

        assertEquals(3, calculator.calculateTotalCredits(student));
    }

    // ─── 測試二：正確計算分類學分 ─────────────────────────────────────────────

    @Test
    @DisplayName("TC04：正確依類別統計學分")
    void testCreditsByCategory_correctGrouping() {
        student.addCompletedCourse(new Course("C001", "程式設計", 3, CreditCategory.REQUIRED_MAJOR));
        student.addCompletedCourse(new Course("C002", "演算法", 3, CreditCategory.REQUIRED_MAJOR));
        student.addCompletedCourse(new Course("E001", "機器學習", 3, CreditCategory.MAJOR_ELECTIVE));
        student.addCompletedCourse(new Course("G001", "哲學", 2, CreditCategory.GENERAL_EDUCATION));

        Map<CreditCategory, Integer> result = calculator.calculateCreditsByCategory(student);

        assertEquals(6, result.get(CreditCategory.REQUIRED_MAJOR));
        assertEquals(3, result.get(CreditCategory.MAJOR_ELECTIVE));
        assertEquals(2, result.get(CreditCategory.GENERAL_EDUCATION));
        assertEquals(0, result.get(CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        assertEquals(0, result.get(CreditCategory.SCHOOL_REQUIRED));
    }

    @Test
    @DisplayName("TC05：無已修課程時，所有類別學分皆為 0")
    void testCreditsByCategory_allZeroWhenNoCourses() {
        Map<CreditCategory, Integer> result = calculator.calculateCreditsByCategory(student);

        for (CreditCategory category : CreditCategory.values()) {
            assertEquals(0, result.get(category),
                    "類別 " + category.getDisplayName() + " 應為 0");
        }
    }

    // ─── 測試三：重複課程處理 ─────────────────────────────────────────────────

    @Test
    @DisplayName("TC06：重複新增相同課程代碼，應只計算一次學分")
    void testDuplicateCourse_countedOnlyOnce() {
        Course course = new Course("C001", "程式設計", 3, CreditCategory.REQUIRED_MAJOR);
        student.addCompletedCourse(course);
        boolean addedAgain = student.addCompletedCourse(course); // 相同物件

        assertFalse(addedAgain, "重複課程應回傳 false");
        assertEquals(3, calculator.calculateTotalCredits(student),
                "重複課程學分只能計算一次");
    }

    @Test
    @DisplayName("TC07：相同課程代碼但不同物件，應只計算一次")
    void testDuplicateCourse_sameIdDifferentObject() {
        student.addCompletedCourse(new Course("C001", "程式設計", 3, CreditCategory.REQUIRED_MAJOR));
        boolean addedAgain = student.addCompletedCourse(
                new Course("C001", "程式設計（重修）", 3, CreditCategory.REQUIRED_MAJOR));

        assertFalse(addedAgain, "相同 courseId 應視為重複");
        assertEquals(3, calculator.calculateTotalCredits(student));
    }
}

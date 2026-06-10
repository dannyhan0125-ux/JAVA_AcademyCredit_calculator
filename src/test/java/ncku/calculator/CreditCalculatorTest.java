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
 * 驗證學分計算（含 0.5 單位）與次數統計的各種情境。
 */
@DisplayName("CreditCalculator 學分計算測試")
class CreditCalculatorTest {

    private static final double EPS = 0.001;

    private CreditCalculator calculator;
    private Student student;

    @BeforeEach
    void setUp() {
        calculator = new CreditCalculator();
        student = new Student("B11234567", "測試學生");
    }

    // ─── TC01：無課程 → 總學分為 0 ───────────────────────────────────────────

    @Test
    @DisplayName("TC01：學生無任何課程時，總學分應為 0")
    void testTotalCredits_noCoursesReturnZero() {
        assertEquals(0.0, calculator.calculateTotalCredits(student), EPS);
    }

    // ─── TC02：多門課程加總 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC02：正確計算多門課程的總學分")
    void testTotalCredits_correctSum() {
        student.addCompletedCourse(new Course("C001", "程式設計",  3, CreditCategory.REQUIRED_MAJOR));
        student.addCompletedCourse(new Course("C002", "資料結構",  3, CreditCategory.REQUIRED_MAJOR));
        student.addCompletedCourse(new Course("G001", "藝術鑑賞",  2, CreditCategory.GENERAL_HUMANITIES));

        assertEquals(8.0, calculator.calculateTotalCredits(student), EPS);
    }

    // ─── TC03：含 0 學分體育課 ────────────────────────────────────────────────

    @Test
    @DisplayName("TC03：含 0 學分課程（體育）時，總學分計算正確")
    void testTotalCredits_withZeroCredits() {
        student.addCompletedCourse(new Course("PE001", "健康體育", 0, CreditCategory.PE_HEALTH));
        student.addCompletedCourse(new Course("C001",  "程式設計", 3, CreditCategory.REQUIRED_MAJOR));

        assertEquals(3.0, calculator.calculateTotalCredits(student), EPS);
    }

    // ─── TC04：0.5 學分課程支援 ───────────────────────────────────────────────

    @Test
    @DisplayName("TC04：支援 0.5 學分單位，正確計算含半學分的總學分")
    void testTotalCredits_halfCreditCourses() {
        student.addCompletedCourse(new Course("C001", "課程A", 1.5, CreditCategory.MAJOR_ELECTIVE));
        student.addCompletedCourse(new Course("C002", "課程B", 2.5, CreditCategory.MAJOR_ELECTIVE));
        student.addCompletedCourse(new Course("C003", "課程C", 0.5, CreditCategory.MAJOR_ELECTIVE));

        assertEquals(4.5, calculator.calculateTotalCredits(student), EPS);
    }

    // ─── TC05：0.5 學分驗證 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC05：非 0.5 倍數的學分應拋出 IllegalArgumentException")
    void testCourse_invalidCreditThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Course("X001", "無效課程", 1.3, CreditCategory.MAJOR_ELECTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> new Course("X002", "無效課程", 0.3, CreditCategory.MAJOR_ELECTIVE));
    }

    // ─── TC06：分類統計 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("TC06：正確依類別統計學分")
    void testCreditsByCategory_correctGrouping() {
        student.addCompletedCourse(new Course("C001", "程式設計",  3, CreditCategory.REQUIRED_MAJOR));
        student.addCompletedCourse(new Course("C002", "演算法",    3, CreditCategory.REQUIRED_MAJOR));
        student.addCompletedCourse(new Course("E001", "機器學習",  3, CreditCategory.MAJOR_ELECTIVE));
        student.addCompletedCourse(new Course("G001", "哲學思維",  2, CreditCategory.GENERAL_HUMANITIES));

        Map<CreditCategory, Double> result = calculator.calculateCreditsByCategory(student);

        assertEquals(6.0, result.get(CreditCategory.REQUIRED_MAJOR),  EPS);
        assertEquals(3.0, result.get(CreditCategory.MAJOR_ELECTIVE),  EPS);
        assertEquals(2.0, result.get(CreditCategory.GENERAL_HUMANITIES), EPS);
        assertEquals(0.0, result.get(CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE), EPS);
        assertEquals(0.0, result.get(CreditCategory.CHINESE), EPS);
    }

    // ─── TC07：無課程時所有類別為 0.0 ────────────────────────────────────────

    @Test
    @DisplayName("TC07：無已修課程時，所有類別學分皆為 0.0")
    void testCreditsByCategory_allZeroWhenNoCourses() {
        Map<CreditCategory, Double> result = calculator.calculateCreditsByCategory(student);
        for (CreditCategory category : CreditCategory.values()) {
            assertEquals(0.0, result.get(category), EPS,
                    "類別 " + category.getDisplayName() + " 應為 0.0");
        }
    }

    // ─── TC08：重複課程只計一次 ───────────────────────────────────────────────

    @Test
    @DisplayName("TC08：重複新增相同課程代碼，應只計算一次學分")
    void testDuplicateCourse_countedOnlyOnce() {
        Course course = new Course("C001", "程式設計", 3, CreditCategory.REQUIRED_MAJOR);
        student.addCompletedCourse(course);
        boolean addedAgain = student.addCompletedCourse(course);

        assertFalse(addedAgain, "重複課程應回傳 false");
        assertEquals(3.0, calculator.calculateTotalCredits(student), EPS);
    }

    // ─── TC09：體育次數統計 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC09：正確計算體育各子類別的次數（不受學分影響）")
    void testCountCoursesByCategory_peCount() {
        student.addCompletedCourse(new Course("PE001","健康與體育知識",0, CreditCategory.PE_HEALTH));
        student.addCompletedCourse(new Course("PE002","游泳",          0, CreditCategory.PE_SPORT));
        student.addCompletedCourse(new Course("PE003","二年級體育一",  0, CreditCategory.PE_SOPHOMORE));
        student.addCompletedCourse(new Course("PE004","二年級體育二",  0, CreditCategory.PE_SOPHOMORE));
        student.addCompletedCourse(new Course("C001", "程式設計",      3, CreditCategory.REQUIRED_MAJOR));

        Map<CreditCategory, Integer> counts = calculator.countCoursesByCategory(student);

        assertEquals(1, counts.get(CreditCategory.PE_HEALTH));
        assertEquals(1, counts.get(CreditCategory.PE_SPORT));
        assertEquals(2, counts.get(CreditCategory.PE_SOPHOMORE));
        assertEquals(1, counts.get(CreditCategory.REQUIRED_MAJOR));
    }

    // ─── TC10：通識子類別 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TC10：通識各子類別學分（含 0.5 單位）分別正確計算")
    void testCreditsByCategory_generalWithHalfCredit() {
        student.addCompletedCourse(new Course("G001","藝術鑑賞",  2.0, CreditCategory.GENERAL_HUMANITIES));
        student.addCompletedCourse(new Course("G002","社會學概論",1.5, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        student.addCompletedCourse(new Course("G003","科技與人文",4.5, CreditCategory.GENERAL_FUSION));

        Map<CreditCategory, Double> result = calculator.calculateCreditsByCategory(student);

        assertEquals(2.0, result.get(CreditCategory.GENERAL_HUMANITIES),      EPS);
        assertEquals(1.5, result.get(CreditCategory.GENERAL_SOCIAL_SCIENCE),  EPS);
        assertEquals(4.5, result.get(CreditCategory.GENERAL_FUSION),           EPS);
        assertEquals(0.0, result.get(CreditCategory.GENERAL_NATURAL_SCIENCE), EPS);
    }
}

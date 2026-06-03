package ncku.calculator;

import ncku.calculator.model.Course;
import ncku.calculator.model.CreditCategory;
import ncku.calculator.model.GraduationReport;
import ncku.calculator.model.GraduationRequirement;
import ncku.calculator.model.Student;
import ncku.calculator.service.CreditCalculator;
import ncku.calculator.service.GraduationChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GraduationChecker 單元測試
 * 驗證畢業資格判斷與報告產生的各種情境。
 */
@DisplayName("GraduationChecker 畢業資格測試")
class GraduationCheckerTest {

    private GraduationChecker checker;
    private CreditCalculator calculator;
    private GraduationRequirement requirement;

    @BeforeEach
    void setUp() {
        calculator = new CreditCalculator();
        requirement = GraduationRequirement.defaultNckuRequirement();
        checker = new GraduationChecker(calculator, requirement);
    }

    // ─── 建立符合畢業標準的學生 ───────────────────────────────────────────────

    /**
     * 建立達到所有畢業門檻的學生（使用最少學分）。
     */
    private Student buildEligibleStudent() {
        Student s = new Student("A12345678", "合格學生");

        // 系上必修：60 學分
        for (int i = 1; i <= 20; i++) {
            s.addCompletedCourse(new Course(
                    "REQ" + i, "必修課" + i, 3, CreditCategory.REQUIRED_MAJOR));
        }

        // 系上選修：21 學分
        for (int i = 1; i <= 7; i++) {
            s.addCompletedCourse(new Course(
                    "ELE" + i, "選修課" + i, 3, CreditCategory.MAJOR_ELECTIVE));
        }

        // 通識：19 學分（需為整數，此處用幾門湊成19）
        for (int i = 1; i <= 9; i++) {
            s.addCompletedCourse(new Course(
                    "GE" + i, "通識" + i, 2, CreditCategory.GENERAL_EDUCATION)); // 18
        }
        s.addCompletedCourse(new Course("GE10", "通識十", 1, CreditCategory.GENERAL_EDUCATION)); // 19

        // 校訂必修：9 學分
        s.addCompletedCourse(new Course("CHN1", "國文一", 2, CreditCategory.SCHOOL_REQUIRED));
        s.addCompletedCourse(new Course("CHN2", "國文二", 2, CreditCategory.SCHOOL_REQUIRED));
        s.addCompletedCourse(new Course("ENG1", "英文一", 2, CreditCategory.SCHOOL_REQUIRED));
        s.addCompletedCourse(new Course("ENG2", "英文二", 2, CreditCategory.SCHOOL_REQUIRED));
        s.addCompletedCourse(new Course("STEP", "踏實課", 1, CreditCategory.SCHOOL_REQUIRED));

        // 系外選修：12 學分（不超過上限21）
        for (int i = 1; i <= 4; i++) {
            s.addCompletedCourse(new Course(
                    "OUT" + i, "系外選修" + i, 3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        }

        // 目前學分：60+21+19+9+12 = 121，還需 9 分才到 130
        // 補足至 130 學分（系上選修）
        s.addCompletedCourse(new Course("ELE8", "選修課8", 3, CreditCategory.MAJOR_ELECTIVE));
        s.addCompletedCourse(new Course("ELE9", "選修課9", 3, CreditCategory.MAJOR_ELECTIVE));
        s.addCompletedCourse(new Course("ELE10", "選修課10", 3, CreditCategory.MAJOR_ELECTIVE));

        return s; // 總計：60+30+19+9+12 = 130
    }

    // ─── 測試四：符合畢業資格 ─────────────────────────────────────────────────

    @Test
    @DisplayName("TC08：達到所有門檻時，應符合畢業資格")
    void testCheckGraduation_eligible() {
        Student s = buildEligibleStudent();
        assertTrue(checker.checkGraduation(s), "應符合畢業資格");
    }

    // ─── 測試五：不符合畢業資格 ───────────────────────────────────────────────

    @Test
    @DisplayName("TC09：系上必修學分不足，應不符合畢業資格")
    void testCheckGraduation_insufficientRequiredMajor() {
        Student s = new Student("B00000001", "必修不足學生");
        s.addCompletedCourse(new Course("REQ1", "必修課1", 59, CreditCategory.REQUIRED_MAJOR));
        // 缺 1 學分必修，即使其他學分足夠也不符合

        assertFalse(checker.checkGraduation(s));
    }

    @Test
    @DisplayName("TC10：總學分不足，應不符合畢業資格")
    void testCheckGraduation_insufficientTotalCredits() {
        Student s = new Student("B00000002", "總學分不足學生");
        // 各類別足夠但總學分不到 130
        s.addCompletedCourse(new Course("REQ1", "必修課1", 60, CreditCategory.REQUIRED_MAJOR));
        s.addCompletedCourse(new Course("ELE1", "選修課1", 21, CreditCategory.MAJOR_ELECTIVE));
        s.addCompletedCourse(new Course("GE1",  "通識一",  19, CreditCategory.GENERAL_EDUCATION));
        s.addCompletedCourse(new Course("SCH1", "校訂一",   9, CreditCategory.SCHOOL_REQUIRED));
        // 總計 109，不到 130

        assertFalse(checker.checkGraduation(s));
    }

    // ─── 測試六：超過門檻時缺額應為 0 ────────────────────────────────────────

    @Test
    @DisplayName("TC11：超過各類別門檻時，缺額應為 0（不為負數）")
    void testShortfall_exceedingRequirementShouldBeZero() {
        Student s = buildEligibleStudent();
        // 再多加幾門課，確保超過門檻
        s.addCompletedCourse(new Course("EXTRA1", "額外必修", 10, CreditCategory.REQUIRED_MAJOR));

        Map<CreditCategory, Integer> shortfall = checker.calculateShortfall(s);

        shortfall.values().forEach(value ->
                assertTrue(value >= 0, "缺額不可為負數，但得到：" + value));

        assertEquals(0, shortfall.get(CreditCategory.REQUIRED_MAJOR),
                "超過門檻時必修缺額應為 0");
    }

    // ─── 測試七：計算缺少學分 ─────────────────────────────────────────────────

    @Test
    @DisplayName("TC12：正確計算各類別缺少學分")
    void testShortfall_correctCalculation() {
        Student s = new Student("C00000001", "缺學分學生");
        s.addCompletedCourse(new Course("REQ1", "必修課1", 55, CreditCategory.REQUIRED_MAJOR)); // 缺 5
        s.addCompletedCourse(new Course("ELE1", "選修課1", 18, CreditCategory.MAJOR_ELECTIVE)); // 缺 3
        s.addCompletedCourse(new Course("GE1",  "通識一",  10, CreditCategory.GENERAL_EDUCATION)); // 缺 9
        s.addCompletedCourse(new Course("SCH1", "校訂一",   9, CreditCategory.SCHOOL_REQUIRED)); // 足夠

        Map<CreditCategory, Integer> shortfall = checker.calculateShortfall(s);

        assertEquals(5, shortfall.get(CreditCategory.REQUIRED_MAJOR));
        assertEquals(3, shortfall.get(CreditCategory.MAJOR_ELECTIVE));
        assertEquals(9, shortfall.get(CreditCategory.GENERAL_EDUCATION));
        assertEquals(0, shortfall.get(CreditCategory.SCHOOL_REQUIRED));
    }

    // ─── 測試：產生報告 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("TC13：符合畢業資格時，報告 eligible 應為 true")
    void testGenerateReport_eligibleStudent() {
        Student s = buildEligibleStudent();
        GraduationReport report = checker.generateReport(s);

        assertTrue(report.isEligible());
        assertEquals("合格學生", report.getStudentName());
        assertEquals(130, report.getTotalRequired());
        assertTrue(report.getTotalCompleted() >= 130);
    }

    @Test
    @DisplayName("TC14：不符合畢業資格時，報告 eligible 應為 false 且報告不為空")
    void testGenerateReport_ineligibleStudent() {
        Student s = new Student("D00000001", "不合格學生");
        s.addCompletedCourse(new Course("REQ1", "必修課1", 30, CreditCategory.REQUIRED_MAJOR));

        GraduationReport report = checker.generateReport(s);

        assertFalse(report.isEligible());
        assertNotNull(report.toString());
        assertTrue(report.toString().contains("尚未符合畢業資格"));
    }

    @Test
    @DisplayName("TC15：報告中各類別已修學分總和等於 calculateTotalCredits 的結果")
    void testGenerateReport_categoryCreditsSum() {
        Student s = buildEligibleStudent();
        GraduationReport report = checker.generateReport(s);

        int sumFromCategories = report.getCompletedByCategory().values()
                .stream().mapToInt(Integer::intValue).sum();

        assertEquals(report.getTotalCompleted(), sumFromCategories,
                "各類別學分加總應等於總學分");
    }
}

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
 * GraduationChecker 單元測試（支援 double 學分）
 */
@DisplayName("GraduationChecker 畢業資格測試")
class GraduationCheckerTest {

    private static final double EPS = 0.001;

    private GraduationChecker checker;
    private CreditCalculator calculator;
    private GraduationRequirement requirement;

    @BeforeEach
    void setUp() {
        calculator  = new CreditCalculator();
        requirement = GraduationRequirement.defaultNckuRequirement();
        checker     = new GraduationChecker(calculator, requirement);
    }

    // ─── 建立符合畢業標準的學生 ───────────────────────────────────────────────

    /**
     * 恰好達到所有畢業門檻的學生（總學分 = 130.0）。
     */
    private Student buildEligibleStudent() {
        Student s = new Student("A12345678", "合格學生");

        // 系上必修：20 × 3 = 60
        for (int i = 1; i <= 20; i++) {
            s.addCompletedCourse(new Course("REQ" + i, "必修課" + i, 3, CreditCategory.REQUIRED_MAJOR));
        }
        // 系上選修：10 × 3 = 30（> 21）
        for (int i = 1; i <= 10; i++) {
            s.addCompletedCourse(new Course("ELE" + i, "選修課" + i, 3, CreditCategory.MAJOR_ELECTIVE));
        }

        // 通識：一般三類各 2 學分 + 融合 12 = 18，三類涵蓋 ✓
        s.addCompletedCourse(new Course("GH1","藝術鑑賞",   2, CreditCategory.GENERAL_HUMANITIES));
        s.addCompletedCourse(new Course("GS1","社會學概論", 2, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        s.addCompletedCourse(new Course("GN1","科學與技術", 2, CreditCategory.GENERAL_NATURAL_SCIENCE));
        s.addCompletedCourse(new Course("GF1","科技與人文", 6, CreditCategory.GENERAL_FUSION));
        s.addCompletedCourse(new Course("GF2","當代議題",   6, CreditCategory.GENERAL_FUSION));

        // 校訂必修
        s.addCompletedCourse(new Course("CHN1","大學國文一",2, CreditCategory.CHINESE));
        s.addCompletedCourse(new Course("CHN2","大學國文二",2, CreditCategory.CHINESE));
        s.addCompletedCourse(new Course("ENG1","大學英文一",2, CreditCategory.ENGLISH));
        s.addCompletedCourse(new Course("ENG2","大學英文二",2, CreditCategory.ENGLISH));
        s.addCompletedCourse(new Course("TN01","踏溯台南",  1, CreditCategory.INTRO_TAINAN));

        // 體育：健康×1、專項×1、二年級×2（0 學分）
        s.addCompletedCourse(new Course("PE01","健康與體育知識",0, CreditCategory.PE_HEALTH));
        s.addCompletedCourse(new Course("PE02","游泳",          0, CreditCategory.PE_SPORT));
        s.addCompletedCourse(new Course("PE03","二年級體育一",  0, CreditCategory.PE_SOPHOMORE));
        s.addCompletedCourse(new Course("PE04","二年級體育二",  0, CreditCategory.PE_SOPHOMORE));

        // 系外選修：4 × 3 = 12
        for (int i = 1; i <= 4; i++) {
            s.addCompletedCourse(new Course("OUT" + i,"系外選修" + i, 3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        }
        // 補 1 學分到達 130
        s.addCompletedCourse(new Course("GL1","生命倫理",1, CreditCategory.GENERAL_LIFE_SCIENCE));

        return s; // 總計：60+30+18+1+4+4+1+0+12 = 130.0
    }

    // ─── TC08：符合畢業資格 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC08：達到所有門檻時，應符合畢業資格")
    void testCheckGraduation_eligible() {
        assertTrue(checker.checkGraduation(buildEligibleStudent()));
    }

    // ─── TC09：系上必修不足 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC09：系上必修學分不足，應不符合畢業資格")
    void testCheckGraduation_insufficientRequiredMajor() {
        Student s = new Student("B00000001", "必修不足");
        s.addCompletedCourse(new Course("REQ1","必修課1", 59, CreditCategory.REQUIRED_MAJOR));
        assertFalse(checker.checkGraduation(s));
    }

    // ─── TC10：總學分不足 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TC10：各類別足夠但總學分不到 130，應不符合畢業資格")
    void testCheckGraduation_insufficientTotalCredits() {
        Student s = new Student("B00000002", "總分不足");
        s.addCompletedCourse(new Course("REQ1","必修",   60, CreditCategory.REQUIRED_MAJOR));
        s.addCompletedCourse(new Course("ELE1","選修",   21, CreditCategory.MAJOR_ELECTIVE));
        s.addCompletedCourse(new Course("GH1", "人文",    6, CreditCategory.GENERAL_HUMANITIES));
        s.addCompletedCourse(new Course("GS1", "社科",    4, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        s.addCompletedCourse(new Course("GN1", "理工",    4, CreditCategory.GENERAL_NATURAL_SCIENCE));
        s.addCompletedCourse(new Course("GF1", "融合",    4, CreditCategory.GENERAL_FUSION));
        s.addCompletedCourse(new Course("CHN1","國文",    4, CreditCategory.CHINESE));
        s.addCompletedCourse(new Course("ENG1","英文",    4, CreditCategory.ENGLISH));
        s.addCompletedCourse(new Course("TN01","踏溯",   1, CreditCategory.INTRO_TAINAN));
        s.addCompletedCourse(new Course("PE01","健康",   0, CreditCategory.PE_HEALTH));
        s.addCompletedCourse(new Course("PE02","專項",   0, CreditCategory.PE_SPORT));
        s.addCompletedCourse(new Course("PE03","二年1",  0, CreditCategory.PE_SOPHOMORE));
        s.addCompletedCourse(new Course("PE04","二年2",  0, CreditCategory.PE_SOPHOMORE));
        // 總計 108，不到 130
        assertFalse(checker.checkGraduation(s));
    }

    // ─── TC11：通識學分不足 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC11：通識總學分不足（僅 10 學分），應不符合畢業資格")
    void testCheckGraduation_insufficientGeneralEducation() {
        Student s = new Student("B00000003","通識不足");
        for (int i = 1; i <= 20; i++)
            s.addCompletedCourse(new Course("REQ"+i,"必修"+i,3,CreditCategory.REQUIRED_MAJOR));
        for (int i = 1; i <= 10; i++)
            s.addCompletedCourse(new Course("ELE"+i,"選修"+i,3,CreditCategory.MAJOR_ELECTIVE));
        // 通識只有 10 學分（不足 18）
        s.addCompletedCourse(new Course("GH1","人文", 4, CreditCategory.GENERAL_HUMANITIES));
        s.addCompletedCourse(new Course("GS1","社科", 4, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        s.addCompletedCourse(new Course("GF1","融合", 2, CreditCategory.GENERAL_FUSION));
        s.addCompletedCourse(new Course("CHN1","國文",4, CreditCategory.CHINESE));
        s.addCompletedCourse(new Course("ENG1","英文",4, CreditCategory.ENGLISH));
        s.addCompletedCourse(new Course("TN01","踏溯",1, CreditCategory.INTRO_TAINAN));
        s.addCompletedCourse(new Course("PE01","健康",0, CreditCategory.PE_HEALTH));
        s.addCompletedCourse(new Course("PE02","專項",0, CreditCategory.PE_SPORT));
        s.addCompletedCourse(new Course("PE03","二年1",0, CreditCategory.PE_SOPHOMORE));
        s.addCompletedCourse(new Course("PE04","二年2",0, CreditCategory.PE_SOPHOMORE));
        assertFalse(checker.checkGraduation(s));
    }

    // ─── TC12：體育次數不足 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC12：二年級體育只有 1 次（需 2 次），應不符合畢業資格")
    void testCheckGraduation_insufficientPeSophomore() {
        Student s = new Student("B00000004","體育不足");
        for (int i = 1; i <= 20; i++)
            s.addCompletedCourse(new Course("REQ"+i,"必修"+i,3,CreditCategory.REQUIRED_MAJOR));
        for (int i = 1; i <= 10; i++)
            s.addCompletedCourse(new Course("ELE"+i,"選修"+i,3,CreditCategory.MAJOR_ELECTIVE));
        s.addCompletedCourse(new Course("GH1","人文",4,CreditCategory.GENERAL_HUMANITIES));
        s.addCompletedCourse(new Course("GS1","社科",4,CreditCategory.GENERAL_SOCIAL_SCIENCE));
        s.addCompletedCourse(new Course("GN1","自然",4,CreditCategory.GENERAL_NATURAL_SCIENCE));
        s.addCompletedCourse(new Course("GF1","融合",6,CreditCategory.GENERAL_FUSION));
        s.addCompletedCourse(new Course("CHN1","國文",4,CreditCategory.CHINESE));
        s.addCompletedCourse(new Course("ENG1","英文",4,CreditCategory.ENGLISH));
        s.addCompletedCourse(new Course("TN01","踏溯",1,CreditCategory.INTRO_TAINAN));
        s.addCompletedCourse(new Course("PE01","健康",0,CreditCategory.PE_HEALTH));
        s.addCompletedCourse(new Course("PE02","專項",0,CreditCategory.PE_SPORT));
        s.addCompletedCourse(new Course("PE03","二年1",0,CreditCategory.PE_SOPHOMORE)); // 只有 1 次
        assertFalse(checker.checkGraduation(s), "二年級體育只有 1 次，應不符合畢業資格");
    }

    // ─── TC13：超過門檻缺額為 0 ──────────────────────────────────────────────

    @Test
    @DisplayName("TC13：超過各門檻時，缺額應為 0.0（不為負數）")
    void testShortfall_exceedingRequirementShouldBeZero() {
        Student s = buildEligibleStudent();
        s.addCompletedCourse(new Course("EXTRA1","額外必修",10, CreditCategory.REQUIRED_MAJOR));

        Map<CreditCategory, Double> shortfall = checker.calculateShortfall(s);
        shortfall.values().forEach(v ->
                assertTrue(v >= 0.0 - EPS, "缺額不可為負數，但得到：" + v));
        assertEquals(0.0, shortfall.get(CreditCategory.REQUIRED_MAJOR), EPS);
    }

    // ─── TC14：學分缺額計算 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC14：正確計算必修與選修的缺少學分（含 0.5 單位）")
    void testShortfall_correctCreditCalculation() {
        Student s = new Student("C00000001","缺學分學生");
        s.addCompletedCourse(new Course("REQ1","必修課1", 55.5, CreditCategory.REQUIRED_MAJOR)); // 缺 4.5
        s.addCompletedCourse(new Course("ELE1","選修課1", 18.5, CreditCategory.MAJOR_ELECTIVE)); // 缺 2.5

        Map<CreditCategory, Double> shortfall = checker.calculateShortfall(s);

        assertEquals(4.5, shortfall.get(CreditCategory.REQUIRED_MAJOR), EPS);
        assertEquals(2.5, shortfall.get(CreditCategory.MAJOR_ELECTIVE), EPS);
    }

    // ─── TC15：體育次數缺額 ───────────────────────────────────────────────────

    @Test
    @DisplayName("TC15：體育次數缺額：二年級只修 1 次應缺 1.0")
    void testShortfall_peCountShortfall() {
        Student s = new Student("C00000002","體育缺次");
        s.addCompletedCourse(new Course("PE01","健康",0, CreditCategory.PE_HEALTH));
        s.addCompletedCourse(new Course("PE02","二年1",0, CreditCategory.PE_SOPHOMORE));

        Map<CreditCategory, Double> shortfall = checker.calculateShortfall(s);

        assertEquals(0.0, shortfall.get(CreditCategory.PE_HEALTH),    EPS);
        assertEquals(1.0, shortfall.get(CreditCategory.PE_SPORT),     EPS);
        assertEquals(1.0, shortfall.get(CreditCategory.PE_SOPHOMORE), EPS);
    }

    // ─── TC16：報告 eligible = true ──────────────────────────────────────────

    @Test
    @DisplayName("TC16：符合畢業資格時，報告 eligible 應為 true")
    void testGenerateReport_eligibleStudent() {
        Student s = buildEligibleStudent();
        GraduationReport report = checker.generateReport(s);

        assertTrue(report.isEligible());
        assertEquals("合格學生", report.getStudentName());
        assertEquals(130, report.getTotalRequired());
        assertTrue(report.getTotalCompleted() >= 130.0 - EPS);
    }

    // ─── TC17：報告 eligible = false ─────────────────────────────────────────

    @Test
    @DisplayName("TC17：不符合畢業資格時，報告 eligible 為 false 且含「尚未符合畢業資格」")
    void testGenerateReport_ineligibleStudent() {
        Student s = new Student("D00000001","不合格學生");
        s.addCompletedCourse(new Course("REQ1","必修課1",30, CreditCategory.REQUIRED_MAJOR));

        GraduationReport report = checker.generateReport(s);

        assertFalse(report.isEligible());
        assertTrue(report.toString().contains("尚未符合畢業資格"));
    }

    // ─── TC18：報告各類別加總等於總學分 ──────────────────────────────────────

    @Test
    @DisplayName("TC18：報告各類別學分總和等於 calculateTotalCredits 的結果")
    void testGenerateReport_categoryCreditsSum() {
        Student s = buildEligibleStudent();
        GraduationReport report = checker.generateReport(s);

        double sumFromCategories = report.getCreditsByCategory().values()
                .stream().mapToDouble(Double::doubleValue).sum();

        assertEquals(report.getTotalCompleted(), sumFromCategories, EPS);
    }

    // ─── TC19：通識類別涵蓋數 ────────────────────────────────────────────────

    @Test
    @DisplayName("TC19：報告中正確顯示一般通識涵蓋 3 個類別")
    void testGenerateReport_generalCategoryCount() {
        Student s = new Student("E00000001","通識測試");
        s.addCompletedCourse(new Course("GH1","藝術鑑賞",  2.0, CreditCategory.GENERAL_HUMANITIES));
        s.addCompletedCourse(new Course("GS1","社會學",    2.0, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        s.addCompletedCourse(new Course("GN1","自然科學",  2.0, CreditCategory.GENERAL_NATURAL_SCIENCE));
        s.addCompletedCourse(new Course("GF1","融合通識",  6.0, CreditCategory.GENERAL_FUSION));

        GraduationReport report = checker.generateReport(s);

        assertEquals(3, report.getGeneralCategoryCount());
        assertEquals(12.0, report.getGeneralTotalCredits(), EPS);
    }

    // ─── TC20：體育次數在報告中正確 ──────────────────────────────────────────

    @Test
    @DisplayName("TC20：報告中體育各子類別次數正確")
    void testGenerateReport_peCountInReport() {
        GraduationReport report = checker.generateReport(buildEligibleStudent());
        Map<CreditCategory, Integer> counts = report.getCountsByCategory();

        assertEquals(1, counts.get(CreditCategory.PE_HEALTH));
        assertEquals(1, counts.get(CreditCategory.PE_SPORT));
        assertEquals(2, counts.get(CreditCategory.PE_SOPHOMORE));
    }

    // ─── TC21：含 0.5 學分課程的畢業判斷 ─────────────────────────────────────

    @Test
    @DisplayName("TC21：用 0.5 學分課程湊出畢業門檻，應正確判斷為符合")
    void testCheckGraduation_halfCreditCoursesEligible() {
        // 用 1.5 學分課程湊出必修 60 學分
        Student s = new Student("F00000001","半學分測試");
        // 40 × 1.5 = 60（系上必修）
        for (int i = 1; i <= 40; i++) {
            s.addCompletedCourse(new Course("REQ" + i, "必修" + i, 1.5, CreditCategory.REQUIRED_MAJOR));
        }
        // 選修：14 × 1.5 = 21
        for (int i = 1; i <= 14; i++) {
            s.addCompletedCourse(new Course("ELE" + i, "選修" + i, 1.5, CreditCategory.MAJOR_ELECTIVE));
        }
        // 通識：三類各 2 + 融合 12
        s.addCompletedCourse(new Course("GH1","人文",   2, CreditCategory.GENERAL_HUMANITIES));
        s.addCompletedCourse(new Course("GS1","社科",   2, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        s.addCompletedCourse(new Course("GN1","自然",   2, CreditCategory.GENERAL_NATURAL_SCIENCE));
        s.addCompletedCourse(new Course("GF1","融合",  12, CreditCategory.GENERAL_FUSION));
        // 校訂
        s.addCompletedCourse(new Course("CHN1","國文",  4, CreditCategory.CHINESE));
        s.addCompletedCourse(new Course("ENG1","英文",  4, CreditCategory.ENGLISH));
        s.addCompletedCourse(new Course("TN01","踏溯",  1, CreditCategory.INTRO_TAINAN));
        // 體育
        s.addCompletedCourse(new Course("PE01","健康",  0, CreditCategory.PE_HEALTH));
        s.addCompletedCourse(new Course("PE02","專項",  0, CreditCategory.PE_SPORT));
        s.addCompletedCourse(new Course("PE03","二年1", 0, CreditCategory.PE_SOPHOMORE));
        s.addCompletedCourse(new Course("PE04","二年2", 0, CreditCategory.PE_SOPHOMORE));
        // 補足總學分到 130：60+21+18+4+4+1 = 108，缺 22 → 加系外
        for (int i = 1; i <= 8; i++) {
            s.addCompletedCourse(new Course("OUT" + i,"系外"+i,
                    i <= 7 ? 3.0 : 1.0, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        }
        // 60+21+18+4+4+1+22 = 130 ✓

        assertTrue(checker.checkGraduation(s), "含 0.5 學分課程應正確判斷符合畢業");
    }
}

package ncku.calculator.service;

import ncku.calculator.model.CreditCategory;
import ncku.calculator.model.GraduationReport;
import ncku.calculator.model.GraduationRequirement;
import ncku.calculator.model.Student;

import java.util.EnumMap;
import java.util.Map;

/**
 * 畢業資格審核服務
 *
 * <p>依賴 {@link CreditCalculator} 取得學分（double）與次數（int）計算結果，
 * 再套用 {@link GraduationRequirement} 的門檻規則進行判斷。
 *
 * <p>畢業需同時滿足：
 * <ol>
 *   <li>總學分 ≥ 130</li>
 *   <li>系上必修 ≥ 60 學分</li>
 *   <li>系上選修 ≥ 21 學分</li>
 *   <li>通識總學分 ≥ 18（含一般通識三類以上、融合通識 ≥ 1 且 ≤ 15）</li>
 *   <li>國文 ≥ 4 學分</li>
 *   <li>英文 ≥ 4 學分</li>
 *   <li>踏溯台南 ≥ 1 學分</li>
 *   <li>體育：健康知能 ≥ 1 次、專項體能 ≥ 1 次、二年級體育 ≥ 2 次</li>
 * </ol>
 */
public class GraduationChecker {

    /** 浮點比較容差 */
    private static final double EPS = 0.001;

    private final CreditCalculator creditCalculator;
    private final GraduationRequirement req;

    public GraduationChecker(CreditCalculator creditCalculator,
                             GraduationRequirement requirement) {
        this.creditCalculator = creditCalculator;
        this.req = requirement;
    }

    /**
     * 判斷學生是否符合畢業資格。
     *
     * @param student 學生資料
     * @return true 表示符合畢業資格
     */
    public boolean checkGraduation(Student student) {
        Map<CreditCategory, Double>  credits = creditCalculator.calculateCreditsByCategory(student);
        Map<CreditCategory, Integer> counts  = creditCalculator.countCoursesByCategory(student);

        return checkTotalCredits(student)
                && checkRequiredMajor(credits)
                && checkMajorElective(credits)
                && checkGeneralEducation(credits)
                && checkChinese(credits)
                && checkEnglish(credits)
                && checkIntroTainan(credits)
                && checkPE(counts);
    }

    // ── 個別規則判斷 ─────────────────────────────────────────────────────────

    private boolean checkTotalCredits(Student student) {
        return creditCalculator.calculateTotalCredits(student) >= req.getTotalCreditsRequired() - EPS;
    }

    private boolean checkRequiredMajor(Map<CreditCategory, Double> credits) {
        return credits.getOrDefault(CreditCategory.REQUIRED_MAJOR, 0.0) >= req.getRequiredMajorMin() - EPS;
    }

    private boolean checkMajorElective(Map<CreditCategory, Double> credits) {
        return credits.getOrDefault(CreditCategory.MAJOR_ELECTIVE, 0.0) >= req.getMajorElectiveMin() - EPS;
    }

    private boolean checkGeneralEducation(Map<CreditCategory, Double> credits) {
        double generalTotal = computeGeneralTotal(credits);
        if (generalTotal < req.getGeneralTotalMin() - EPS) return false;

        double fusionCredits = credits.getOrDefault(CreditCategory.GENERAL_FUSION, 0.0);
        if (fusionCredits < req.getGeneralFusionMin() - EPS) return false;
        if (fusionCredits > req.getGeneralFusionMax() + EPS) return false;

        int nonFusionCategoryCount = 0;
        double nonFusionTotal = 0.0;
        for (CreditCategory cat : CreditCategory.values()) {
            if (cat.isGeneralEducationType()) {
                double c = credits.getOrDefault(cat, 0.0);
                if (c > EPS) nonFusionCategoryCount++;
                nonFusionTotal += c;
            }
        }
        return nonFusionCategoryCount >= req.getGeneralCategoryMinCount()
                && nonFusionTotal >= req.getGeneralNonFusionMin() - EPS;
    }

    private boolean checkChinese(Map<CreditCategory, Double> credits) {
        return credits.getOrDefault(CreditCategory.CHINESE, 0.0) >= req.getChineseCredits() - EPS;
    }

    private boolean checkEnglish(Map<CreditCategory, Double> credits) {
        return credits.getOrDefault(CreditCategory.ENGLISH, 0.0) >= req.getEnglishCredits() - EPS;
    }

    private boolean checkIntroTainan(Map<CreditCategory, Double> credits) {
        return credits.getOrDefault(CreditCategory.INTRO_TAINAN, 0.0) >= req.getIntroTainanCredits() - EPS;
    }

    private boolean checkPE(Map<CreditCategory, Integer> counts) {
        return counts.getOrDefault(CreditCategory.PE_HEALTH,    0) >= req.getPeHealthMin()
                && counts.getOrDefault(CreditCategory.PE_SPORT,   0) >= req.getPeSportMin()
                && counts.getOrDefault(CreditCategory.PE_SOPHOMORE, 0) >= req.getPeSophomoreMin();
    }

    // ── 缺額計算 ─────────────────────────────────────────────────────────────

    /**
     * 計算各類別尚缺學分數（double），PE 類別回傳次數缺額（以 double 格式）。
     * 若已超過門檻，缺額為 0.0（不出現負數）。
     *
     * @param student 學生資料
     * @return 各類別 → 尚缺學分（或次數）的 Map
     */
    public Map<CreditCategory, Double> calculateShortfall(Student student) {
        Map<CreditCategory, Double>  credits = creditCalculator.calculateCreditsByCategory(student);
        Map<CreditCategory, Integer> counts  = creditCalculator.countCoursesByCategory(student);
        Map<CreditCategory, Double>  shortfall = new EnumMap<>(CreditCategory.class);

        // 學分類缺額
        shortfall.put(CreditCategory.REQUIRED_MAJOR,
                creditShortfall(credits, CreditCategory.REQUIRED_MAJOR, req.getRequiredMajorMin()));
        shortfall.put(CreditCategory.MAJOR_ELECTIVE,
                creditShortfall(credits, CreditCategory.MAJOR_ELECTIVE, req.getMajorElectiveMin()));
        shortfall.put(CreditCategory.CHINESE,
                creditShortfall(credits, CreditCategory.CHINESE, req.getChineseCredits()));
        shortfall.put(CreditCategory.ENGLISH,
                creditShortfall(credits, CreditCategory.ENGLISH, req.getEnglishCredits()));
        shortfall.put(CreditCategory.INTRO_TAINAN,
                creditShortfall(credits, CreditCategory.INTRO_TAINAN, req.getIntroTainanCredits()));

        // 通識缺額（放在第一個通識類別）
        double generalShortfall =
                Math.max(0.0, req.getGeneralTotalMin() - computeGeneralTotal(credits));
        boolean placed = false;
        for (CreditCategory cat : CreditCategory.values()) {
            if (cat.isAnyGeneralEducation()) {
                shortfall.put(cat, placed ? 0.0 : generalShortfall);
                placed = true;
            }
        }

        // 體育次數缺額（以 double 存放 int 缺次）
        shortfall.put(CreditCategory.PE_HEALTH,
                (double) Math.max(0, req.getPeHealthMin()    - counts.getOrDefault(CreditCategory.PE_HEALTH,    0)));
        shortfall.put(CreditCategory.PE_SPORT,
                (double) Math.max(0, req.getPeSportMin()     - counts.getOrDefault(CreditCategory.PE_SPORT,     0)));
        shortfall.put(CreditCategory.PE_SOPHOMORE,
                (double) Math.max(0, req.getPeSophomoreMin() - counts.getOrDefault(CreditCategory.PE_SOPHOMORE, 0)));

        // 其餘補 0.0
        for (CreditCategory cat : CreditCategory.values()) {
            shortfall.putIfAbsent(cat, 0.0);
        }

        return shortfall;
    }

    // ── 報告產生 ─────────────────────────────────────────────────────────────

    /**
     * 產生完整的畢業進度報告。
     *
     * @param student 學生資料
     * @return 畢業進度報告
     */
    public GraduationReport generateReport(Student student) {
        Map<CreditCategory, Double>  credits = creditCalculator.calculateCreditsByCategory(student);
        Map<CreditCategory, Integer> counts  = creditCalculator.countCoursesByCategory(student);
        double totalCompleted = creditCalculator.calculateTotalCredits(student);

        double generalTotalCredits = computeGeneralTotal(credits);
        int generalCategoryCount = 0;
        for (CreditCategory cat : CreditCategory.values()) {
            if (cat.isGeneralEducationType() && credits.getOrDefault(cat, 0.0) > EPS) {
                generalCategoryCount++;
            }
        }
        double outsideTotalCredits = credits.getOrDefault(CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE, 0.0)
                + credits.getOrDefault(CreditCategory.SECOND_LANGUAGE, 0.0);

        return new GraduationReport(
                student.getStudentName(),
                totalCompleted,
                req.getTotalCreditsRequired(),
                credits,
                counts,
                generalTotalCredits,
                generalCategoryCount,
                outsideTotalCredits,
                req,
                checkGraduation(student)
        );
    }

    // ── 私有輔助方法 ─────────────────────────────────────────────────────────

    /** 計算通識課程總學分（一般通識五類 + 融合通識） */
    private double computeGeneralTotal(Map<CreditCategory, Double> credits) {
        double total = 0.0;
        for (CreditCategory cat : CreditCategory.values()) {
            if (cat.isAnyGeneralEducation()) {
                total += credits.getOrDefault(cat, 0.0);
            }
        }
        return total;
    }

    /** 計算單一類別的學分缺額（不低於 0） */
    private double creditShortfall(Map<CreditCategory, Double> credits,
                                   CreditCategory cat, int required) {
        double completed = credits.getOrDefault(cat, 0.0);
        return Math.max(0.0, required - completed);
    }
}

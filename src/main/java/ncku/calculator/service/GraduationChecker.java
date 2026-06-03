package ncku.calculator.service;

import ncku.calculator.model.CreditCategory;
import ncku.calculator.model.GraduationReport;
import ncku.calculator.model.GraduationRequirement;
import ncku.calculator.model.Student;

import java.util.EnumMap;
import java.util.Map;

/**
 * 畢業資格審核服務
 * 負責判斷學生是否符合畢業資格，並產生畢業進度報告。
 *
 * <p>依賴 {@link CreditCalculator} 取得學分計算結果，再套用
 * {@link GraduationRequirement} 的門檻規則進行判斷。
 */
public class GraduationChecker {

    private final CreditCalculator creditCalculator;
    private final GraduationRequirement requirement;

    public GraduationChecker(CreditCalculator creditCalculator,
                             GraduationRequirement requirement) {
        this.creditCalculator = creditCalculator;
        this.requirement = requirement;
    }

    /**
     * 判斷學生是否符合畢業資格。
     *
     * <p>需同時滿足：
     * <ol>
     *   <li>總學分 >= 門檻</li>
     *   <li>每個類別已修學分 >= 最低需求（系外選修無下限）</li>
     * </ol>
     *
     * @param student 學生資料
     * @return true 表示符合畢業資格
     */
    public boolean checkGraduation(Student student) {
        int totalCredits = creditCalculator.calculateTotalCredits(student);
        if (totalCredits < requirement.getTotalCreditsRequired()) {
            return false;
        }

        Map<CreditCategory, Integer> creditsByCategory =
                creditCalculator.calculateCreditsByCategory(student);

        for (CreditCategory category : CreditCategory.values()) {
            int completed = creditsByCategory.getOrDefault(category, 0);
            int required = requirement.getMinCreditsForCategory(category);
            if (completed < required) {
                return false;
            }
        }

        return true;
    }

    /**
     * 計算各類別尚缺學分數。
     * 若已超過門檻，缺額為 0（不會出現負數）。
     *
     * @param student 學生資料
     * @return 各類別 → 尚缺學分數的 Map
     */
    public Map<CreditCategory, Integer> calculateShortfall(Student student) {
        Map<CreditCategory, Integer> creditsByCategory =
                creditCalculator.calculateCreditsByCategory(student);
        Map<CreditCategory, Integer> shortfall = new EnumMap<>(CreditCategory.class);

        for (CreditCategory category : CreditCategory.values()) {
            int completed = creditsByCategory.getOrDefault(category, 0);
            int required = requirement.getMinCreditsForCategory(category);
            // 超過門檻時缺額為 0
            shortfall.put(category, Math.max(0, required - completed));
        }

        return shortfall;
    }

    /**
     * 產生完整的畢業進度報告。
     *
     * @param student 學生資料
     * @return 畢業進度報告
     */
    public GraduationReport generateReport(Student student) {
        int totalCompleted = creditCalculator.calculateTotalCredits(student);
        Map<CreditCategory, Integer> completedByCategory =
                creditCalculator.calculateCreditsByCategory(student);
        Map<CreditCategory, Integer> requiredByCategory =
                requirement.getMinCategoryRequirements();
        Map<CreditCategory, Integer> shortfallByCategory =
                calculateShortfall(student);
        boolean eligible = checkGraduation(student);

        return new GraduationReport(
                student.getStudentName(),
                totalCompleted,
                requirement.getTotalCreditsRequired(),
                completedByCategory,
                requiredByCategory,
                shortfallByCategory,
                eligible
        );
    }
}

package ncku.calculator.model;

import java.util.Map;

/**
 * 畢業進度報告 Model（Value Object）
 * 彙整學生的畢業資格審核結果，為唯讀資料物件。
 */
public class GraduationReport {

    private final String studentName;
    private final int totalCompleted;
    private final int totalRequired;
    private final Map<CreditCategory, Integer> completedByCategory;
    private final Map<CreditCategory, Integer> requiredByCategory;
    private final Map<CreditCategory, Integer> shortfallByCategory;
    private final boolean eligible;

    public GraduationReport(
            String studentName,
            int totalCompleted,
            int totalRequired,
            Map<CreditCategory, Integer> completedByCategory,
            Map<CreditCategory, Integer> requiredByCategory,
            Map<CreditCategory, Integer> shortfallByCategory,
            boolean eligible) {
        this.studentName = studentName;
        this.totalCompleted = totalCompleted;
        this.totalRequired = totalRequired;
        this.completedByCategory = Map.copyOf(completedByCategory);
        this.requiredByCategory = Map.copyOf(requiredByCategory);
        this.shortfallByCategory = Map.copyOf(shortfallByCategory);
        this.eligible = eligible;
    }

    public String getStudentName() { return studentName; }
    public int getTotalCompleted() { return totalCompleted; }
    public int getTotalRequired() { return totalRequired; }
    public Map<CreditCategory, Integer> getCompletedByCategory() { return completedByCategory; }
    public Map<CreditCategory, Integer> getRequiredByCategory() { return requiredByCategory; }
    public Map<CreditCategory, Integer> getShortfallByCategory() { return shortfallByCategory; }
    public boolean isEligible() { return eligible; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 畢業進度報告 ===\n");
        sb.append(String.format("學生：%s%n", studentName));
        sb.append(String.format("總學分：%d/%d%n", totalCompleted, totalRequired));

        for (CreditCategory category : CreditCategory.values()) {
            int completed = completedByCategory.getOrDefault(category, 0);
            int required = requiredByCategory.getOrDefault(category, 0);
            int shortfall = shortfallByCategory.getOrDefault(category, 0);
            sb.append(String.format("  %-12s：%d/%d（尚缺 %d 學分）%n",
                    category.getDisplayName(), completed, required, shortfall));
        }

        sb.append(String.format("畢業狀態：%s%n",
                eligible ? "✅ 符合畢業資格" : "❌ 尚未符合畢業資格"));
        return sb.toString();
    }
}

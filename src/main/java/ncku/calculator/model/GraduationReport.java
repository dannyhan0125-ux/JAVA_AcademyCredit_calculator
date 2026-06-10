package ncku.calculator.model;

import java.util.Map;

import static ncku.calculator.model.Course.formatCredits;

/**
 * 畢業進度報告 Model（Value Object）
 * 彙整學生的畢業資格審核結果，含學分（double）與體育次數（int）。
 */
public class GraduationReport {

    private final String studentName;
    /** 已修總學分（double，支援 0.5 單位） */
    private final double totalCompleted;
    private final int totalRequired;

    /** 各類別已修學分（double） */
    private final Map<CreditCategory, Double> creditsByCategory;

    /** 各類別已修課程次數（int，用於體育） */
    private final Map<CreditCategory, Integer> countsByCategory;

    /** 通識課程總學分（一般通識五類 + 融合通識，double） */
    private final double generalTotalCredits;

    /** 一般通識五類中有修習學分的類別數 */
    private final int generalCategoryCount;

    /** 系外選修 + 第二外語的總學分（double） */
    private final double outsideTotalCredits;

    private final GraduationRequirement requirement;
    private final boolean eligible;

    public GraduationReport(
            String studentName,
            double totalCompleted,
            int totalRequired,
            Map<CreditCategory, Double> creditsByCategory,
            Map<CreditCategory, Integer> countsByCategory,
            double generalTotalCredits,
            int generalCategoryCount,
            double outsideTotalCredits,
            GraduationRequirement requirement,
            boolean eligible) {
        this.studentName         = studentName;
        this.totalCompleted      = totalCompleted;
        this.totalRequired       = totalRequired;
        this.creditsByCategory   = Map.copyOf(creditsByCategory);
        this.countsByCategory    = Map.copyOf(countsByCategory);
        this.generalTotalCredits = generalTotalCredits;
        this.generalCategoryCount = generalCategoryCount;
        this.outsideTotalCredits = outsideTotalCredits;
        this.requirement         = requirement;
        this.eligible            = eligible;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getStudentName()                              { return studentName; }
    public double getTotalCompleted()                           { return totalCompleted; }
    public int    getTotalRequired()                            { return totalRequired; }
    public Map<CreditCategory, Double>  getCreditsByCategory() { return creditsByCategory; }
    public Map<CreditCategory, Integer> getCountsByCategory()  { return countsByCategory; }
    public double getGeneralTotalCredits()                      { return generalTotalCredits; }
    public int    getGeneralCategoryCount()                     { return generalCategoryCount; }
    public double getOutsideTotalCredits()                      { return outsideTotalCredits; }
    public boolean isEligible()                                 { return eligible; }

    // ── toString ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String line = "─".repeat(44);

        sb.append("╔══════════════════════════════════════════╗\n");
        sb.append("║          畢業進度報告                    ║\n");
        sb.append("╚══════════════════════════════════════════╝\n");
        sb.append(String.format("  學生姓名：%s%n", studentName));

        int pct = totalRequired > 0 ? (int) (totalCompleted * 100 / totalRequired) : 0;
        sb.append(String.format("  總學分  ：%s / %d（%d%%）%n",
                formatCredits(totalCompleted), totalRequired, pct));
        sb.append(line).append("\n");

        // ── 系上必修 ──────────────────────────────────────────────────────────
        appendCreditRow(sb, "系上必修",
                credits(CreditCategory.REQUIRED_MAJOR), requirement.getRequiredMajorMin());

        // ── 系上選修 ──────────────────────────────────────────────────────────
        appendCreditRow(sb, "系上選修",
                credits(CreditCategory.MAJOR_ELECTIVE), requirement.getMajorElectiveMin());

        // ── 通識課程 ──────────────────────────────────────────────────────────
        sb.append(line).append("\n");
        boolean genMet = generalTotalCredits >= requirement.getGeneralTotalMin() - 0.001;
        sb.append(String.format("  %-14s：%s / %d 學分  %s%n",
                "通識（總計）",
                formatCredits(generalTotalCredits),
                requirement.getGeneralTotalMin(),
                statusIcon(genMet)));
        sb.append(String.format("    ├ 一般通識類別涵蓋：%d / %d 類  %s%n",
                generalCategoryCount,
                requirement.getGeneralCategoryMinCount(),
                statusIcon(generalCategoryCount >= requirement.getGeneralCategoryMinCount())));

        for (CreditCategory cat : CreditCategory.values()) {
            if (cat.isGeneralEducationType() && credits(cat) > 0.001) {
                sb.append(String.format("    │  %-16s：%s 學分%n",
                        cat.getDisplayName(), formatCredits(credits(cat))));
            }
        }

        double fusionCredits = credits(CreditCategory.GENERAL_FUSION);
        boolean fusionMet = fusionCredits >= requirement.getGeneralFusionMin() - 0.001
                && fusionCredits <= requirement.getGeneralFusionMax() + 0.001;
        sb.append(String.format("    └ %-16s：%s 學分（限 %d）  %s%n",
                CreditCategory.GENERAL_FUSION.getDisplayName(),
                formatCredits(fusionCredits),
                requirement.getGeneralFusionMax(),
                statusIcon(fusionMet)));

        // ── 系外選修 ──────────────────────────────────────────────────────────
        sb.append(line).append("\n");
        boolean outsideMet = outsideTotalCredits <= requirement.getOutsideElectiveMax() + 0.001;
        sb.append(String.format("  %-14s：%s 學分（上限 %d）  %s%n",
                "系外選修",
                formatCredits(outsideTotalCredits),
                requirement.getOutsideElectiveMax(),
                outsideMet ? "✓ 未超限" : "⚠ 已超上限"));

        // ── 校訂必修 ──────────────────────────────────────────────────────────
        sb.append(line).append("\n");
        appendCreditRow(sb, CreditCategory.CHINESE.getDisplayName(),
                credits(CreditCategory.CHINESE), requirement.getChineseCredits());
        appendCreditRow(sb, CreditCategory.ENGLISH.getDisplayName(),
                credits(CreditCategory.ENGLISH), requirement.getEnglishCredits());
        appendCreditRow(sb, CreditCategory.INTRO_TAINAN.getDisplayName(),
                credits(CreditCategory.INTRO_TAINAN), requirement.getIntroTainanCredits());

        // ── 體育 ──────────────────────────────────────────────────────────────
        sb.append(line).append("\n");
        appendCountRow(sb, CreditCategory.PE_HEALTH.getDisplayName(),
                counts(CreditCategory.PE_HEALTH), requirement.getPeHealthMin());
        appendCountRow(sb, CreditCategory.PE_SPORT.getDisplayName(),
                counts(CreditCategory.PE_SPORT), requirement.getPeSportMin());
        appendCountRow(sb, CreditCategory.PE_SOPHOMORE.getDisplayName(),
                counts(CreditCategory.PE_SOPHOMORE), requirement.getPeSophomoreMin());

        // ── 畢業狀態 ──────────────────────────────────────────────────────────
        sb.append(line).append("\n");
        sb.append(String.format("  畢業狀態：%s%n",
                eligible ? "✅ 符合畢業資格" : "❌ 尚未符合畢業資格"));

        return sb.toString();
    }

    // ── 私有輔助 ─────────────────────────────────────────────────────────────

    private double credits(CreditCategory cat) {
        return creditsByCategory.getOrDefault(cat, 0.0);
    }

    private int counts(CreditCategory cat) {
        return countsByCategory.getOrDefault(cat, 0);
    }

    private String statusIcon(boolean met) {
        return met ? "✓" : "✗";
    }

    /** 學分達標列（n/required 學分，支援 double） */
    private void appendCreditRow(StringBuilder sb, String label, double completed, int required) {
        boolean met = completed >= required - 0.001;
        sb.append(String.format("  %-14s：%s / %d 學分  %s%n",
                label, formatCredits(completed), required,
                met ? "✓ 已達標"
                    : String.format("✗ 尚缺 %s", formatCredits(required - completed))));
    }

    /** 次數達標列（n/required 次） */
    private void appendCountRow(StringBuilder sb, String label, int completed, int required) {
        boolean met = completed >= required;
        sb.append(String.format("  %-14s：%d / %d 次  %s%n",
                label, completed, required,
                met ? "✓ 已達標"
                    : String.format("✗ 尚缺 %d 次", required - completed)));
    }
}

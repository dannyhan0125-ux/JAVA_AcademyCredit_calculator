package ncku.calculator.model;

/**
 * 畢業門檻設定 Model（資訊工程學系）
 *
 * <p>記錄各類別最低需求學分、次數規定與上限，依成大資工系規格設定。
 * <ul>
 *   <li>系上必修：60 學分</li>
 *   <li>系上選修：≥ 21 學分</li>
 *   <li>通識總學分：≥ 18（一般通識 + 融合通識）</li>
 *   <li>一般通識：至少涵蓋 3 個類別，且一般通識學分 ≥ 4</li>
 *   <li>融合通識：≥ 1 學分，≤ 15 學分</li>
 *   <li>系外選修（含第二外語）：上限 21 學分</li>
 *   <li>校訂必修—國文：4 學分</li>
 *   <li>校訂必修—英文：4 學分</li>
 *   <li>校訂必修—踏溯台南：1 學分</li>
 *   <li>體育—健康知能：至少 1 次</li>
 *   <li>體育—專項體能：至少 1 次</li>
 *   <li>體育—二年級體育：至少 2 次</li>
 *   <li>畢業總學分：130</li>
 * </ul>
 */
public class GraduationRequirement {

    // ── 總學分 ────────────────────────────────────────────────────────────────
    private final int totalCreditsRequired;

    // ── 系上 ──────────────────────────────────────────────────────────────────
    private final int requiredMajorMin;
    private final int majorElectiveMin;

    // ── 通識 ──────────────────────────────────────────────────────────────────
    /** 通識課程總學分（一般通識 + 融合通識）最低需求 */
    private final int generalTotalMin;
    /** 一般通識五類中，至少需涵蓋的不同類別數 */
    private final int generalCategoryMinCount;
    /** 一般通識（五類）至少應修學分數 */
    private final int generalNonFusionMin;
    /** 融合通識最低學分 */
    private final int generalFusionMin;
    /** 融合通識上限學分 */
    private final int generalFusionMax;

    // ── 系外 ──────────────────────────────────────────────────────────────────
    /** 系外選修（含第二外語）上限學分 */
    private final int outsideElectiveMax;

    // ── 校訂必修（學分） ──────────────────────────────────────────────────────
    private final int chineseCredits;
    private final int englishCredits;
    private final int introTainanCredits;

    // ── 體育（次數，非學分） ──────────────────────────────────────────────────
    private final int peHealthMin;
    private final int peSportMin;
    private final int peSophomoreMin;

    private GraduationRequirement(Builder builder) {
        this.totalCreditsRequired   = builder.totalCreditsRequired;
        this.requiredMajorMin       = builder.requiredMajorMin;
        this.majorElectiveMin       = builder.majorElectiveMin;
        this.generalTotalMin        = builder.generalTotalMin;
        this.generalCategoryMinCount= builder.generalCategoryMinCount;
        this.generalNonFusionMin    = builder.generalNonFusionMin;
        this.generalFusionMin       = builder.generalFusionMin;
        this.generalFusionMax       = builder.generalFusionMax;
        this.outsideElectiveMax     = builder.outsideElectiveMax;
        this.chineseCredits         = builder.chineseCredits;
        this.englishCredits         = builder.englishCredits;
        this.introTainanCredits     = builder.introTainanCredits;
        this.peHealthMin            = builder.peHealthMin;
        this.peSportMin             = builder.peSportMin;
        this.peSophomoreMin         = builder.peSophomoreMin;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getTotalCreditsRequired()    { return totalCreditsRequired; }
    public int getRequiredMajorMin()        { return requiredMajorMin; }
    public int getMajorElectiveMin()        { return majorElectiveMin; }
    public int getGeneralTotalMin()         { return generalTotalMin; }
    public int getGeneralCategoryMinCount() { return generalCategoryMinCount; }
    public int getGeneralNonFusionMin()     { return generalNonFusionMin; }
    public int getGeneralFusionMin()        { return generalFusionMin; }
    public int getGeneralFusionMax()        { return generalFusionMax; }
    public int getOutsideElectiveMax()      { return outsideElectiveMax; }
    public int getChineseCredits()          { return chineseCredits; }
    public int getEnglishCredits()          { return englishCredits; }
    public int getIntroTainanCredits()      { return introTainanCredits; }
    public int getPeHealthMin()             { return peHealthMin; }
    public int getPeSportMin()              { return peSportMin; }
    public int getPeSophomoreMin()          { return peSophomoreMin; }

    /**
     * 建立預設的成大資工系畢業門檻設定。
     */
    public static GraduationRequirement defaultNckuRequirement() {
        return new Builder().build();
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static class Builder {
        private int totalCreditsRequired    = 130;
        private int requiredMajorMin        = 60;
        private int majorElectiveMin        = 21;
        private int generalTotalMin         = 18;
        private int generalCategoryMinCount = 3;
        private int generalNonFusionMin     = 4;
        private int generalFusionMin        = 1;
        private int generalFusionMax        = 15;
        private int outsideElectiveMax      = 21;
        private int chineseCredits          = 4;
        private int englishCredits          = 4;
        private int introTainanCredits      = 1;
        private int peHealthMin             = 1;
        private int peSportMin              = 1;
        private int peSophomoreMin          = 2;

        public Builder totalCreditsRequired(int v)    { this.totalCreditsRequired = v;    return this; }
        public Builder requiredMajorMin(int v)        { this.requiredMajorMin = v;        return this; }
        public Builder majorElectiveMin(int v)        { this.majorElectiveMin = v;        return this; }
        public Builder generalTotalMin(int v)         { this.generalTotalMin = v;         return this; }
        public Builder generalCategoryMinCount(int v) { this.generalCategoryMinCount = v; return this; }
        public Builder generalNonFusionMin(int v)     { this.generalNonFusionMin = v;     return this; }
        public Builder generalFusionMin(int v)        { this.generalFusionMin = v;        return this; }
        public Builder generalFusionMax(int v)        { this.generalFusionMax = v;        return this; }
        public Builder outsideElectiveMax(int v)      { this.outsideElectiveMax = v;      return this; }
        public Builder chineseCredits(int v)          { this.chineseCredits = v;          return this; }
        public Builder englishCredits(int v)          { this.englishCredits = v;          return this; }
        public Builder introTainanCredits(int v)      { this.introTainanCredits = v;      return this; }
        public Builder peHealthMin(int v)             { this.peHealthMin = v;             return this; }
        public Builder peSportMin(int v)              { this.peSportMin = v;              return this; }
        public Builder peSophomoreMin(int v)          { this.peSophomoreMin = v;          return this; }

        public GraduationRequirement build() {
            return new GraduationRequirement(this);
        }
    }
}

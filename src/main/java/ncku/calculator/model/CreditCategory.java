package ncku.calculator.model;

/**
 * 課程分類 Enum（細分類版本）
 *
 * <p>依 NCKU 資工系畢業規格劃分，含：
 * <ul>
 *   <li>系上必修 / 系上選修</li>
 *   <li>通識一般五類（人文學、社會科學、自然與工程科學、生命科學與健康、科際整合）</li>
 *   <li>通識融合通識</li>
 *   <li>系外選修 / 第二外語</li>
 *   <li>校訂必修（國文、英文）</li>
 *   <li>體育三類（健康知能、專項體能、二年級體育）</li>
 *   <li>校訂必修—踏溯台南</li>
 * </ul>
 */
public enum CreditCategory {

    // ── 系上 ──────────────────────────────────────────────────────────────────

    /** 系上必修 */
    REQUIRED_MAJOR("系上必修"),

    /** 系上選修 */
    MAJOR_ELECTIVE("系上選修"),

    // ── 通識課程（一般通識五類） ───────────────────────────────────────────────

    /** 通識—人文學 */
    GENERAL_HUMANITIES("通識—人文學"),

    /** 通識—社會科學 */
    GENERAL_SOCIAL_SCIENCE("通識—社會科學"),

    /** 通識—自然與工程科學 */
    GENERAL_NATURAL_SCIENCE("通識—自然與工程科學"),

    /** 通識—生命科學與健康 */
    GENERAL_LIFE_SCIENCE("通識—生命科學與健康"),

    /** 通識—科際整合 */
    GENERAL_INTERDISCIPLINARY("通識—科際整合"),

    // ── 通識課程（融合通識） ──────────────────────────────────────────────────

    /** 通識—融合通識 */
    GENERAL_FUSION("通識—融合通識"),

    // ── 系外 ──────────────────────────────────────────────────────────────────

    /** 系外選修 */
    OUTSIDE_DEPARTMENT_ELECTIVE("系外選修"),

    /** 第二外語（固定 2 學分，計入系外選修上限） */
    SECOND_LANGUAGE("第二外語"),

    // ── 校訂必修 ──────────────────────────────────────────────────────────────

    /** 校訂必修—國文（上限 4 學分） */
    CHINESE("校訂必修—國文"),

    /** 校訂必修—英文（上限 4 學分） */
    ENGLISH("校訂必修—英文"),

    // ── 體育 ──────────────────────────────────────────────────────────────────

    /** 體育—健康知能（至少 1 次） */
    PE_HEALTH("體育—健康知能"),

    /** 體育—專項體能（至少 1 次） */
    PE_SPORT("體育—專項體能"),

    /** 體育—二年級體育（至少 2 次） */
    PE_SOPHOMORE("體育—二年級"),

    // ── 其他校訂必修 ──────────────────────────────────────────────────────────

    /** 校訂必修—踏溯台南（1 學分） */
    INTRO_TAINAN("校訂必修—踏溯台南");

    private final String displayName;

    CreditCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 判斷是否為一般通識五類（不含融合通識）。
     */
    public boolean isGeneralEducationType() {
        return this == GENERAL_HUMANITIES
                || this == GENERAL_SOCIAL_SCIENCE
                || this == GENERAL_NATURAL_SCIENCE
                || this == GENERAL_LIFE_SCIENCE
                || this == GENERAL_INTERDISCIPLINARY;
    }

    /**
     * 判斷是否為任何通識類別（含融合通識）。
     */
    public boolean isAnyGeneralEducation() {
        return isGeneralEducationType() || this == GENERAL_FUSION;
    }

    /**
     * 判斷是否為體育類別。
     */
    public boolean isPE() {
        return this == PE_HEALTH || this == PE_SPORT || this == PE_SOPHOMORE;
    }
}

package ncku.calculator.model;

/**
 * 課程分類 Enum
 * 代表每門課程所屬的學分類別。
 */
public enum CreditCategory {

    /** 系上必修 */
    REQUIRED_MAJOR("系上必修"),

    /** 系上選修 */
    MAJOR_ELECTIVE("系上選修"),

    /** 通識課程 */
    GENERAL_EDUCATION("通識課程"),

    /** 系外選修 */
    OUTSIDE_DEPARTMENT_ELECTIVE("系外選修"),

    /** 校訂必修（含國文、英文、體育、踏實） */
    SCHOOL_REQUIRED("校訂必修");

    private final String displayName;

    CreditCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

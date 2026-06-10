package ncku.calculator.service;

import ncku.calculator.model.CreditCategory;

/**
 * 課程代碼分類器
 *
 * <p>依 NCKU 課程代碼前置規則，自動判斷課程所屬類型。
 * 代碼識別規則：
 * <ul>
 *   <li>{@code F7} 開頭 → 系內課程（需再由 Repository 或使用者確認必修/選修）</li>
 *   <li>{@code A9} 開頭 → 通識課程（需互動選擇子類別）</li>
 *   <li>{@code A7} 開頭 → 國文（校訂必修）</li>
 *   <li>{@code A1-100} ~ {@code A1-300} → 英文（校訂必修）</li>
 *   <li>{@code A1-500} ~ {@code A1-590} → 第二外語</li>
 *   <li>{@code A2} 開頭 → 體育（需互動選擇子類別）</li>
 *   <li>其他 → 系外選修</li>
 * </ul>
 */
public class CourseClassifier {

    /**
     * 課程代碼粗分類型。
     * 部分類型（DEPARTMENT_COURSE、GENERAL_EDUCATION、PE）需進一步互動確認細分類。
     */
    public enum CodeType {
        /** F7 開頭 — 系內課程（需確認必修/選修） */
        DEPARTMENT_COURSE,
        /** A9 開頭 — 通識課程（需選子類別） */
        GENERAL_EDUCATION,
        /** A7 開頭 — 國文（校訂必修） */
        CHINESE,
        /** A1-100 ~ A1-300 — 英文（校訂必修） */
        ENGLISH,
        /** A1-500 ~ A1-590 — 第二外語 */
        SECOND_LANGUAGE,
        /** A2 開頭 — 體育（需選子類別） */
        PE,
        /** 其他 — 系外選修 */
        OUTSIDE_ELECTIVE
    }

    /**
     * 依課程代碼判斷代碼類型（大小寫不敏感）。
     *
     * @param courseId 課程代碼
     * @return CodeType
     * @throws IllegalArgumentException 若課程代碼為 null 或空白
     */
    public CodeType detectType(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("課程代碼不可為空");
        }
        String id = courseId.trim().toUpperCase();

        if (id.startsWith("F7")) return CodeType.DEPARTMENT_COURSE;
        if (id.startsWith("A9")) return CodeType.GENERAL_EDUCATION;
        if (id.startsWith("A7")) return CodeType.CHINESE;
        if (id.startsWith("A2")) return CodeType.PE;
        if (id.startsWith("A1-")) {
            try {
                int num = Integer.parseInt(id.substring(3));
                if (num >= 100 && num <= 300) return CodeType.ENGLISH;
                if (num >= 500 && num <= 590) return CodeType.SECOND_LANGUAGE;
            } catch (NumberFormatException ignored) {
                // 非數字後綴，視為系外選修
            }
        }
        return CodeType.OUTSIDE_ELECTIVE;
    }

    /**
     * 直接取得 {@link CreditCategory}。
     *
     * <p>適用於代碼可唯一對應一個類別的情況（國文、英文、第二外語、系外）。
     * 需要使用者互動選擇子類的類型（F7/A9/A2）返回 {@code null}。
     *
     * @param courseId 課程代碼
     * @return CreditCategory，若需額外互動則返回 null
     */
    public CreditCategory classifyDirect(String courseId) {
        return switch (detectType(courseId)) {
            case CHINESE -> CreditCategory.CHINESE;
            case ENGLISH -> CreditCategory.ENGLISH;
            case SECOND_LANGUAGE -> CreditCategory.SECOND_LANGUAGE;
            case OUTSIDE_ELECTIVE -> CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE;
            default -> null; // DEPARTMENT_COURSE / GENERAL_EDUCATION / PE 需互動決定
        };
    }
}

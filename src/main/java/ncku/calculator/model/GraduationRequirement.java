package ncku.calculator.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 畢業門檻設定 Model
 * 記錄各類別最低需求學分與畢業總學分。
 *
 * <p>依規格：
 * <ul>
 *   <li>系上必修：60 學分</li>
 *   <li>系上選修：>= 21 學分</li>
 *   <li>通識課程：19 學分</li>
 *   <li>系外選修：<= 21 學分（上限，非下限）</li>
 *   <li>校訂必修：9 學分（國文4 + 英文4 + 體育0×4 + 踏實1）</li>
 *   <li>總學分：130</li>
 * </ul>
 */
public class GraduationRequirement {

    private final int totalCreditsRequired;
    /** 各類別「最低需求」學分數 */
    private final Map<CreditCategory, Integer> minCategoryRequirements;
    /** 系外選修上限（該類別不需達到下限，但不可超過此上限） */
    private final int outsideElectiveMaxCredits;

    private GraduationRequirement(Builder builder) {
        this.totalCreditsRequired = builder.totalCreditsRequired;
        this.minCategoryRequirements = Collections.unmodifiableMap(builder.minCategoryRequirements);
        this.outsideElectiveMaxCredits = builder.outsideElectiveMaxCredits;
    }

    public int getTotalCreditsRequired() {
        return totalCreditsRequired;
    }

    /**
     * 取得指定類別最低需求學分（唯讀）。
     */
    public Map<CreditCategory, Integer> getMinCategoryRequirements() {
        return minCategoryRequirements;
    }

    public int getMinCreditsForCategory(CreditCategory category) {
        return minCategoryRequirements.getOrDefault(category, 0);
    }

    public int getOutsideElectiveMaxCredits() {
        return outsideElectiveMaxCredits;
    }

    /**
     * 建立預設的成大畢業門檻設定。
     */
    public static GraduationRequirement defaultNckuRequirement() {
        return new Builder()
                .totalCreditsRequired(130)
                .require(CreditCategory.REQUIRED_MAJOR, 60)
                .require(CreditCategory.MAJOR_ELECTIVE, 21)
                .require(CreditCategory.GENERAL_EDUCATION, 19)
                .require(CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE, 0) // 無下限
                .require(CreditCategory.SCHOOL_REQUIRED, 9)
                .outsideElectiveMaxCredits(21)
                .build();
    }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static class Builder {
        private int totalCreditsRequired = 130;
        private final Map<CreditCategory, Integer> minCategoryRequirements =
                new EnumMap<>(CreditCategory.class);
        private int outsideElectiveMaxCredits = 21;

        public Builder totalCreditsRequired(int total) {
            this.totalCreditsRequired = total;
            return this;
        }

        public Builder require(CreditCategory category, int minCredits) {
            this.minCategoryRequirements.put(category, minCredits);
            return this;
        }

        public Builder outsideElectiveMaxCredits(int max) {
            this.outsideElectiveMaxCredits = max;
            return this;
        }

        public GraduationRequirement build() {
            return new GraduationRequirement(this);
        }
    }
}

package ncku.calculator.model;

import java.util.Objects;

/**
 * 課程 Model
 * 代表單一課程的基本資料。
 *
 * <p>學分數支援 0.5 為單位（如 0.5、1.5、2.5），以 {@code double} 儲存。
 */
public class Course {

    private final String courseId;
    private final String courseName;
    /** 學分數，允許 0.5 的倍數（含 0）。 */
    private final double credits;
    private final CreditCategory category;

    public Course(String courseId, String courseName, double credits, CreditCategory category) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("課程代碼不可為空");
        }
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("課程名稱不可為空");
        }
        if (credits < 0) {
            throw new IllegalArgumentException("學分數不可為負數");
        }
        // 驗證必須為 0.5 的倍數（用 ×2 轉整數避免浮點誤差）
        double doubled = credits * 2;
        if (Math.abs(doubled - Math.round(doubled)) > 0.001) {
            throw new IllegalArgumentException(
                    "學分數必須為 0.5 的倍數（如 0.5, 1.0, 1.5, 2.0, ...），實際值：" + credits);
        }
        if (category == null) {
            throw new IllegalArgumentException("課程分類不可為空");
        }
        this.courseId = courseId;
        this.courseName = courseName;
        this.credits = credits;
        this.category = category;
    }

    public String getCourseId()    { return courseId; }
    public String getCourseName()  { return courseName; }
    public double getCredits()     { return credits; }
    public CreditCategory getCategory() { return category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course other)) return false;
        return Objects.equals(courseId, other.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s 學分) — %s",
                courseId, courseName, formatCredits(credits), category.getDisplayName());
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    /**
     * 格式化學分數：整數顯示無小數點，半學分顯示一位小數。
     * 例如：3.0 → "3"，1.5 → "1.5"
     */
    public static String formatCredits(double credits) {
        if (Math.abs(credits - Math.floor(credits)) < 0.001) {
            return String.valueOf((int) Math.round(credits));
        }
        return String.format("%.1f", credits);
    }
}

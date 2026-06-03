package ncku.calculator.model;

import java.util.Objects;

/**
 * 課程 Model
 * 代表單一課程的基本資料。
 */
public class Course {

    private final String courseId;
    private final String courseName;
    private final int credits;
    private final CreditCategory category;

    public Course(String courseId, String courseName, int credits, CreditCategory category) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("課程代碼不可為空");
        }
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("課程名稱不可為空");
        }
        if (credits < 0) {
            throw new IllegalArgumentException("學分數不可為負數");
        }
        if (category == null) {
            throw new IllegalArgumentException("課程分類不可為空");
        }
        this.courseId = courseId;
        this.courseName = courseName;
        this.credits = credits;
        this.category = category;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getCredits() {
        return credits;
    }

    public CreditCategory getCategory() {
        return category;
    }

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
        return String.format("[%s] %s (%d 學分) — %s",
                courseId, courseName, credits, category.getDisplayName());
    }
}

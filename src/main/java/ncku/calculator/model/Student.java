package ncku.calculator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 學生 Model
 * 包含學生基本資料與已修課程清單。
 * 採用 Set 儲存課程以避免重複課程（依 courseId 判斷）。
 */
public class Student {

    private final String studentId;
    private final String studentName;
    /** 使用 LinkedHashSet 保留新增順序並避免重複 */
    private final Set<Course> completedCourses;

    public Student(String studentId, String studentName) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("學號不可為空");
        }
        if (studentName == null || studentName.isBlank()) {
            throw new IllegalArgumentException("學生姓名不可為空");
        }
        this.studentId = studentId;
        this.studentName = studentName;
        this.completedCourses = new LinkedHashSet<>();
    }

    /**
     * 新增已修課程。
     * 若課程代碼已存在則忽略（不重複新增）。
     *
     * @param course 欲新增的課程
     * @return true 表示新增成功；false 表示課程已存在
     */
    public boolean addCompletedCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("課程不可為 null");
        }
        return completedCourses.add(course);
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    /**
     * 取得已修課程（唯讀清單）。
     */
    public List<Course> getCompletedCourses() {
        return Collections.unmodifiableList(new ArrayList<>(completedCourses));
    }

    @Override
    public String toString() {
        return String.format("學生：%s（%s）", studentName, studentId);
    }
}

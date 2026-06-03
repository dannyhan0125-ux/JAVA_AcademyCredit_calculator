package ncku.calculator.service;

import ncku.calculator.model.Course;
import ncku.calculator.model.CreditCategory;
import ncku.calculator.model.Student;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 學分計算服務
 * 負責統計學生已修學分的商業邏輯。
 *
 * <p>遵循 Single Responsibility Principle：此類別只負責「學分計算」，
 * 畢業資格判斷交由 {@link GraduationChecker} 處理。
 */
public class CreditCalculator {

    /**
     * 計算學生已修總學分。
     *
     * @param student 學生資料
     * @return 總學分數
     */
    public int calculateTotalCredits(Student student) {
        return student.getCompletedCourses().stream()
                .mapToInt(Course::getCredits)
                .sum();
    }

    /**
     * 依類別統計已修學分。
     *
     * @param student 學生資料
     * @return 各類別 → 已修學分數的 Map
     */
    public Map<CreditCategory, Integer> calculateCreditsByCategory(Student student) {
        Map<CreditCategory, Integer> result = new EnumMap<>(CreditCategory.class);

        // 初始化所有類別為 0（確保未修課的類別也出現在結果中）
        for (CreditCategory category : CreditCategory.values()) {
            result.put(category, 0);
        }

        for (Course course : student.getCompletedCourses()) {
            result.merge(course.getCategory(), course.getCredits(), Integer::sum);
        }

        return result;
    }

    /**
     * 計算指定類別的已修學分。
     *
     * @param student  學生資料
     * @param category 課程類別
     * @return 該類別已修學分數
     */
    public int calculateCreditsForCategory(Student student, CreditCategory category) {
        return student.getCompletedCourses().stream()
                .filter(course -> course.getCategory() == category)
                .mapToInt(Course::getCredits)
                .sum();
    }

    /**
     * 取得學生已修課程清單（委派給 Student，供顯示用）。
     *
     * @param student 學生資料
     * @return 已修課程清單（唯讀）
     */
    public List<Course> getCompletedCourseList(Student student) {
        return student.getCompletedCourses();
    }
}

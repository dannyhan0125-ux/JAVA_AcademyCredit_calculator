package ncku.calculator.service;

import ncku.calculator.model.Course;
import ncku.calculator.model.CreditCategory;
import ncku.calculator.model.Student;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 學分計算服務
 * 負責統計學生已修學分與課程次數的商業邏輯。
 *
 * <p>學分以 {@code double} 表示，支援 0.5 學分單位。
 * 課程次數（體育）仍以 {@code int} 表示。
 */
public class CreditCalculator {

    /**
     * 計算學生已修總學分。
     *
     * @param student 學生資料
     * @return 總學分數（double，支援 0.5 單位）
     */
    public double calculateTotalCredits(Student student) {
        return student.getCompletedCourses().stream()
                .mapToDouble(Course::getCredits)
                .sum();
    }

    /**
     * 依類別統計已修學分。
     * 所有類別皆會出現在回傳 Map 中（未修課的類別值為 0.0）。
     *
     * @param student 學生資料
     * @return 各類別 → 已修學分數的 Map（double）
     */
    public Map<CreditCategory, Double> calculateCreditsByCategory(Student student) {
        Map<CreditCategory, Double> result = new EnumMap<>(CreditCategory.class);

        for (CreditCategory category : CreditCategory.values()) {
            result.put(category, 0.0);
        }
        for (Course course : student.getCompletedCourses()) {
            result.merge(course.getCategory(), course.getCredits(), Double::sum);
        }
        return result;
    }

    /**
     * 計算指定類別的已修學分。
     *
     * @param student  學生資料
     * @param category 課程類別
     * @return 該類別已修學分數（double）
     */
    public double calculateCreditsForCategory(Student student, CreditCategory category) {
        return student.getCompletedCourses().stream()
                .filter(course -> course.getCategory() == category)
                .mapToDouble(Course::getCredits)
                .sum();
    }

    /**
     * 依類別統計已修課程次數（用於體育等 0 學分課程的計數判斷）。
     * 所有類別皆會出現在回傳 Map 中（未修課的類別值為 0）。
     *
     * @param student 學生資料
     * @return 各類別 → 已修課程門數的 Map（int）
     */
    public Map<CreditCategory, Integer> countCoursesByCategory(Student student) {
        Map<CreditCategory, Integer> result = new EnumMap<>(CreditCategory.class);

        for (CreditCategory category : CreditCategory.values()) {
            result.put(category, 0);
        }
        for (Course course : student.getCompletedCourses()) {
            result.merge(course.getCategory(), 1, Integer::sum);
        }
        return result;
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

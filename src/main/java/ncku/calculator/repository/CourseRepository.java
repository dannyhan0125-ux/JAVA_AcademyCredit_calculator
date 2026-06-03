package ncku.calculator.repository;

import ncku.calculator.model.Course;
import ncku.calculator.model.CreditCategory;

import java.util.List;
import java.util.Optional;

/**
 * 課程資料存取介面（Repository Pattern）
 *
 * <p>此介面定義了所有課程資料的讀寫操作。
 * 目前由 {@link InMemoryCourseRepository} 提供記憶體實作；
 * 未來若需要接資料庫，只需新增實作類別（例如 JdbcCourseRepository 或
 * JpaCourseRepository），無需修改 Service 層，符合 Open/Closed Principle。
 *
 * <p>預留資料庫讀取設計說明：
 * <pre>
 *  CourseRepository (Interface)
 *      ├── InMemoryCourseRepository   ← 目前使用（記憶體）
 *      ├── JdbcCourseRepository       ← 預留：使用 JDBC 連線 DB
 *      └── JpaCourseRepository        ← 預留：使用 JPA/Hibernate
 * </pre>
 */
public interface CourseRepository {

    /**
     * 儲存（新增或更新）一門課程。
     *
     * @param course 欲儲存的課程
     */
    void save(Course course);

    /**
     * 依課程代碼查詢課程。
     *
     * @param courseId 課程代碼
     * @return 查詢結果（若不存在則為 Optional.empty()）
     */
    Optional<Course> findById(String courseId);

    /**
     * 取得所有課程清單。
     *
     * @return 所有課程（唯讀）
     */
    List<Course> findAll();

    /**
     * 依分類查詢課程。
     *
     * @param category 課程分類
     * @return 屬於該分類的所有課程
     */
    List<Course> findByCategory(CreditCategory category);

    /**
     * 刪除指定課程代碼的課程。
     *
     * @param courseId 課程代碼
     * @return true 表示刪除成功；false 表示找不到課程
     */
    boolean deleteById(String courseId);

    /**
     * 清空所有課程（主要供測試使用）。
     */
    void clear();
}

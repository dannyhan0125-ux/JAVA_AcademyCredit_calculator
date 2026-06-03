package ncku.calculator.repository;

import ncku.calculator.model.Course;
import ncku.calculator.model.CreditCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 記憶體課程 Repository 實作（In-Memory）
 *
 * <p>使用 {@link LinkedHashMap} 儲存課程（以 courseId 為 key），保留新增順序。
 * 適合開發與測試階段使用。
 *
 * <p>若未來需要改用資料庫，請新增對應的 Repository 實作類別並實作
 * {@link CourseRepository} 介面，然後在應用程式入口注入新實作即可。
 *
 * <p>預留資料庫擴充範例（僅為說明，無需修改此類）：
 * <pre>
 *   // JDBC 實作範例（未來擴充用）
 *   public class JdbcCourseRepository implements CourseRepository {
 *       private final DataSource dataSource;
 *
 *       {@literal @}Override
 *       public void save(Course course) {
 *           // INSERT INTO courses (course_id, name, credits, category) VALUES (?, ?, ?, ?)
 *       }
 *
 *       {@literal @}Override
 *       public Optional<Course> findById(String courseId) {
 *           // SELECT * FROM courses WHERE course_id = ?
 *       }
 *
 *       // ... 其他方法
 *   }
 * </pre>
 */
public class InMemoryCourseRepository implements CourseRepository {

    /** 以 courseId 為 key 儲存課程，保留新增順序 */
    private final Map<String, Course> storage = new LinkedHashMap<>();

    @Override
    public void save(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("課程不可為 null");
        }
        storage.put(course.getCourseId(), course);
    }

    @Override
    public Optional<Course> findById(String courseId) {
        return Optional.ofNullable(storage.get(courseId));
    }

    @Override
    public List<Course> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(storage.values()));
    }

    @Override
    public List<Course> findByCategory(CreditCategory category) {
        return storage.values().stream()
                .filter(course -> course.getCategory() == category)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean deleteById(String courseId) {
        return storage.remove(courseId) != null;
    }

    @Override
    public void clear() {
        storage.clear();
    }
}

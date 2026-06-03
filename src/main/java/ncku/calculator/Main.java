package ncku.calculator;

import ncku.calculator.model.Course;
import ncku.calculator.model.CreditCategory;
import ncku.calculator.model.GraduationReport;
import ncku.calculator.model.GraduationRequirement;
import ncku.calculator.model.Student;
import ncku.calculator.repository.CourseRepository;
import ncku.calculator.repository.InMemoryCourseRepository;
import ncku.calculator.service.CreditCalculator;
import ncku.calculator.service.GraduationChecker;

import java.util.List;
import java.util.Scanner;

/**
 * 成大畢業學分計算器 — 程式入口（互動式 CLI）
 *
 * <p>使用者可透過文字選單：
 * <ol>
 *   <li>新增已修課程</li>
 *   <li>顯示課程清單</li>
 *   <li>計算總學分</li>
 *   <li>分類統計學分</li>
 *   <li>計算剩餘學分</li>
 *   <li>判斷畢業資格</li>
 *   <li>產生畢業進度報告</li>
 *   <li>載入預設範例資料</li>
 *   <li>結束程式</li>
 * </ol>
 */
public class Main {

    public static void main(String[] args) {
        // ─── 初始化各層元件 ────────────────────────────────────────────────────
        CourseRepository courseRepository = new InMemoryCourseRepository();
        /*
         * 預留資料庫切換點：
         * 若未來改用 JDBC 或 JPA，只需將上方替換為：
         *   CourseRepository courseRepository = new JdbcCourseRepository(dataSource);
         * Service 層完全無需修改。
         */

        CreditCalculator creditCalculator = new CreditCalculator();
        GraduationRequirement requirement = GraduationRequirement.defaultNckuRequirement();
        GraduationChecker graduationChecker = new GraduationChecker(creditCalculator, requirement);

        // ─── 啟動時自動載入課程目錄（預留：未來改由 DB 讀取） ──────────────────
        seedCourses(courseRepository);

        Scanner scanner = new Scanner(System.in);

        // ─── 1. 輸入學生資料 ─────────────────────────────────────────────────
        Student student = inputStudentInfo(scanner);

        // ─── 2. 主選單迴圈 ────────────────────────────────────────────────────
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addCourseInteractive(scanner, student, courseRepository);
                case "2" -> printCourseList(creditCalculator.getCompletedCourseList(student));
                case "3" -> {
                    int total = creditCalculator.calculateTotalCredits(student);
                    System.out.printf("%n已修總學分：%d 學分%n", total);
                }
                case "4" -> {
                    System.out.println();
                    creditCalculator.calculateCreditsByCategory(student)
                            .forEach((cat, credits) ->
                                    System.out.printf("  %-10s：%d 學分%n",
                                            cat.getDisplayName(), credits));
                }
                case "5" -> {
                    System.out.println();
                    graduationChecker.calculateShortfall(student)
                            .forEach((cat, shortfall) ->
                                    System.out.printf("  %-10s 尚缺 %d 學分%n",
                                            cat.getDisplayName(), shortfall));
                }
                case "6" -> {
                    boolean eligible = graduationChecker.checkGraduation(student);
                    System.out.println();
                    System.out.println(eligible ? "✅ 符合畢業資格" : "❌ 尚未符合畢業資格");
                }
                case "7" -> {
                    GraduationReport report = graduationChecker.generateReport(student);
                    System.out.println();
                    System.out.println(report);
                }
                case "8" -> printCourseCatalog(courseRepository);
                case "0" -> {
                    System.out.println("\n掰掰！");
                    running = false;
                }
                default -> System.out.println("\n⚠ 請輸入有效選項（0～8）");
            }
        }

        scanner.close();
    }

    // ─── 使用者互動：輸入學生資料 ──────────────────────────────────────────────

    private static Student inputStudentInfo(Scanner scanner) {
        System.out.println("=".repeat(40));
        System.out.println("  成大畢業學分計算器");
        System.out.println("=".repeat(40));

        String studentId;
        while (true) {
            System.out.print("請輸入學號：");
            studentId = scanner.nextLine().trim();
            if (!studentId.isBlank()) break;
            System.out.println("⚠ 學號不可為空");
        }

        String studentName;
        while (true) {
            System.out.print("請輸入姓名：");
            studentName = scanner.nextLine().trim();
            if (!studentName.isBlank()) break;
            System.out.println("⚠ 姓名不可為空");
        }

        System.out.printf("%n歡迎，%s（%s）！%n", studentName, studentId);
        return new Student(studentId, studentName);
    }

    // ─── 使用者互動：新增已修課程 ──────────────────────────────────────────────

    /**
     * 使用者輸入課程代碼，從 Repository（課程目錄）查詢後加入已修清單。
     *
     * <p>課程資料（名稱、學分、分類）由 Repository 提供，無需使用者重複輸入。
     * 這與「資料庫查詢」的概念相同：課程目錄預先存好，學生只需提供代碼。
     */
    private static void addCourseInteractive(Scanner scanner,
                                             Student student,
                                             CourseRepository repo) {
        System.out.println("\n─── 新增已修課程 ───");
        System.out.println("（輸入 'list' 可查看課程目錄，直接按 Enter 取消）");
        System.out.print("請輸入課程代碼：");
        String courseId = scanner.nextLine().trim();

        if (courseId.isBlank()) {
            System.out.println("已取消。");
            return;
        }

        // 輸入 list 顯示課程目錄
        if (courseId.equalsIgnoreCase("list")) {
            printCourseCatalog(repo);
            System.out.print("請輸入課程代碼：");
            courseId = scanner.nextLine().trim();
            if (courseId.isBlank()) {
                System.out.println("已取消。");
                return;
            }
        }

        // 從 Repository 查詢課程（未來此處可改為 DB 查詢）
        final String finalCourseId = courseId;
        repo.findById(finalCourseId).ifPresentOrElse(
                course -> {
                    boolean added = student.addCompletedCourse(course);
                    if (added) {
                        System.out.printf("✅ 已新增：%s%n", course);
                    } else {
                        System.out.printf("⚠  [%s] 已在已修清單中，未重複新增。%n", finalCourseId);
                    }
                },
                () -> System.out.printf("⚠  找不到課程代碼 [%s]，請輸入 'list' 查看可用課程。%n", finalCourseId)
        );
    }

    // ─── 選單顯示 ────────────────────────────────────────────────────────────

    private static void printMainMenu() {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("  請選擇功能");
        System.out.println("=".repeat(40));
        System.out.println("  1. 新增已修課程（輸入課程代碼）");
        System.out.println("  2. 顯示已修課程清單");
        System.out.println("  3. 計算總學分");
        System.out.println("  4. 分類統計學分");
        System.out.println("  5. 計算剩餘學分");
        System.out.println("  6. 判斷畢業資格");
        System.out.println("  7. 產生畢業進度報告");
        System.out.println("  8. 查看課程目錄");
        System.out.println("  0. 結束");
        System.out.print(">> ");
    }

    private static void printCourseList(List<Course> courses) {
        System.out.println();
        if (courses.isEmpty()) {
            System.out.println("  （尚無已修課程）");
            return;
        }
        courses.forEach(c -> System.out.println("  " + c));
        System.out.printf("%n共 %d 門課程%n", courses.size());
    }

    // ─── 範例資料 ─────────────────────────────────────────────────────────────

    /**
     * 載入預設範例課程資料到 Repository。
     * 此處為硬編碼資料；未來可改由資料庫讀取。
     */
    private static void seedCourses(CourseRepository repo) {
        repo.clear();

        // 系上必修
        repo.save(new Course("CS1001", "程式設計", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS1002", "資料結構", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS2001", "演算法", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS2002", "計算機組織", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS2003", "作業系統", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS3001", "計算機網路", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS3002", "軟體工程", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS3003", "資料庫系統", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS3004", "編譯器設計", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS3005", "離散數學", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS4001", "機器學習", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("CS4002", "深度學習", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("MATH001", "微積分一", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("MATH002", "微積分二", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("MATH003", "線性代數", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("PHYS001", "普通物理", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("PHYS002", "普通物理實驗", 1, CreditCategory.REQUIRED_MAJOR));

        // 系上選修
        repo.save(new Course("CS5001", "自然語言處理", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("CS5002", "電腦視覺", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("CS5003", "分散式系統", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("CS5004", "資訊安全", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("CS5005", "雲端計算", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("CS5006", "行動應用開發", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("CS5007", "嵌入式系統", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("CS5008", "計算生物", 3, CreditCategory.MAJOR_ELECTIVE));

        // 通識課程
        repo.save(new Course("GE1001", "文學與藝術", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1002", "歷史與社會", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1003", "哲學思維", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1004", "環境與生態", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1005", "科技與社會", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1006", "批判思考", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1007", "全球化議題", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1008", "創意思維", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1009", "倫理學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("GE1010", "數位媒體", 2, CreditCategory.GENERAL_EDUCATION));

        // 系外選修
        repo.save(new Course("OUT001", "管理學", 3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        repo.save(new Course("OUT002", "經濟學", 3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        repo.save(new Course("OUT003", "心理學", 3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        repo.save(new Course("OUT004", "社會學", 3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));

        // 校訂必修
        repo.save(new Course("SCH001", "國文一", 2, CreditCategory.SCHOOL_REQUIRED));
        repo.save(new Course("SCH002", "國文二", 2, CreditCategory.SCHOOL_REQUIRED));
        repo.save(new Course("SCH003", "英文一", 2, CreditCategory.SCHOOL_REQUIRED));
        repo.save(new Course("SCH004", "英文二", 2, CreditCategory.SCHOOL_REQUIRED));
        repo.save(new Course("SCH005", "踏實課程", 1, CreditCategory.SCHOOL_REQUIRED));
        repo.save(new Course("SCH006", "體育一", 0, CreditCategory.SCHOOL_REQUIRED));
        repo.save(new Course("SCH007", "體育二", 0, CreditCategory.SCHOOL_REQUIRED));
        repo.save(new Course("SCH008", "體育三", 0, CreditCategory.SCHOOL_REQUIRED));
        repo.save(new Course("SCH009", "體育四", 0, CreditCategory.SCHOOL_REQUIRED));
    }

    /**
     * 顯示課程目錄（依類別分組），供使用者查詢可輸入的課程代碼。
     * 未來此資料可從資料庫動態讀取。
     */
    private static void printCourseCatalog(CourseRepository repo) {
        System.out.println("\n=== 課程目錄 ===");
        for (CreditCategory category : CreditCategory.values()) {
            List<Course> courses = repo.findByCategory(category);
            if (courses.isEmpty()) continue;
            System.out.println("\n【" + category.getDisplayName() + "】");
            courses.forEach(c ->
                    System.out.printf("  %-10s %s（%d 學分）%n",
                            c.getCourseId(), c.getCourseName(), c.getCredits()));
        }
        System.out.println();
    }
}

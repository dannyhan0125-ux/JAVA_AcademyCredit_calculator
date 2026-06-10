package ncku.calculator;

import ncku.calculator.model.Course;
import ncku.calculator.model.CreditCategory;
import ncku.calculator.model.GraduationReport;
import ncku.calculator.model.GraduationRequirement;
import ncku.calculator.model.Student;
import ncku.calculator.repository.CourseRepository;
import ncku.calculator.repository.InMemoryCourseRepository;
import ncku.calculator.service.CourseClassifier;
import ncku.calculator.service.CourseClassifier.CodeType;
import ncku.calculator.service.CreditCalculator;
import ncku.calculator.service.GraduationChecker;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static ncku.calculator.model.Course.formatCredits;

/**
 * 成大畢業學分計算器 — 程式入口（互動式 CLI）
 *
 * <p>使用者可透過文字選單：
 * <ol>
 *   <li>新增已修課程（依課程代碼自動分類）</li>
 *   <li>顯示課程清單（依類別分組）</li>
 *   <li>計算總學分</li>
 *   <li>分類統計學分</li>
 *   <li>計算剩餘學分</li>
 *   <li>判斷畢業資格</li>
 *   <li>產生畢業進度報告</li>
 *   <li>查看課程目錄</li>
 *   <li>載入模擬範例學生</li>
 *   <li>結束程式</li>
 * </ol>
 */
public class Main {

    public static void main(String[] args) {
        // ─── 初始化各層元件 ────────────────────────────────────────────────────
        CourseRepository courseRepo = new InMemoryCourseRepository();
        CreditCalculator creditCalculator = new CreditCalculator();
        GraduationRequirement requirement = GraduationRequirement.defaultNckuRequirement();
        GraduationChecker graduationChecker = new GraduationChecker(creditCalculator, requirement);
        CourseClassifier courseClassifier = new CourseClassifier();

        seedCourseCatalog(courseRepo);

        Scanner scanner = new Scanner(System.in);
        Student student = inputStudentInfo(scanner);

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addCourseInteractive(scanner, student, courseRepo, courseClassifier);
                case "2" -> printCourseListGrouped(student);
                case "3" -> {
                    double total = creditCalculator.calculateTotalCredits(student);
                    System.out.printf("%n已修總學分：%s / %d 學分%n",
                            formatCredits(total), requirement.getTotalCreditsRequired());
                }
                case "4" -> {
                    System.out.println();
                    creditCalculator.calculateCreditsByCategory(student)
                            .forEach((cat, credits) -> {
                                if (credits > 0.001) {
                                    System.out.printf("  %-18s：%s 學分%n",
                                            cat.getDisplayName(), formatCredits(credits));
                                }
                            });
                }
                case "5" -> {
                    System.out.println();
                    boolean anyShortfall = false;
                    for (Map.Entry<CreditCategory, Double> e :
                            graduationChecker.calculateShortfall(student).entrySet()) {
                        if (e.getValue() > 0.001) {
                            anyShortfall = true;
                            System.out.printf("  %-18s 尚缺 %s %s%n",
                                    e.getKey().getDisplayName(),
                                    formatCredits(e.getValue()),
                                    e.getKey().isPE() ? "次" : "學分");
                        }
                    }
                    if (!anyShortfall) System.out.println("  🎉 所有類別均已達標！");
                }
                case "6" -> {
                    System.out.println();
                    System.out.println(graduationChecker.checkGraduation(student)
                            ? "✅ 符合畢業資格" : "❌ 尚未符合畢業資格");
                }
                case "7" -> {
                    System.out.println();
                    System.out.println(graduationChecker.generateReport(student));
                }
                case "8" -> printCourseCatalog(courseRepo);
                case "9" -> {
                    student = loadDemoStudent();
                    System.out.printf("%n✅ 已載入模擬範例學生：%s%n", student.getStudentName());
                }
                case "0" -> { System.out.println("\n掰掰！"); running = false; }
                default  -> System.out.println("\n⚠ 請輸入有效選項（0～9）");
            }
        }
        scanner.close();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 功能二：新增已修課程（依代碼自動分類）
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 輸入課程代碼後依 NCKU 代碼規則自動分類並引導輸入：
     * <ul>
     *   <li>F7 → 僅從課程目錄查找（不在目錄則顯示錯誤）</li>
     *   <li>A9 → 選通識子類 + 學分（名稱自動為代碼）</li>
     *   <li>A7 → 僅輸入學分，名稱自動為代碼</li>
     *   <li>A1-100~300 → 僅輸入學分，名稱自動為代碼</li>
     *   <li>A1-500~590 → 固定 2 學分，無需任何輸入</li>
     *   <li>A2 → 選體育子類，固定 0 學分，名稱自動為代碼</li>
     *   <li>系外選修 → 輸入名稱與學分（唯一需輸入名稱的類型）</li>
     * </ul>
     */
    private static void addCourseInteractive(Scanner scanner,
                                             Student student,
                                             CourseRepository repo,
                                             CourseClassifier classifier) {
        System.out.println("\n─── 新增已修課程 ───");
        System.out.println("（輸入 'list' 可查看課程目錄，直接按 Enter 取消）");
        System.out.print("請輸入課程代碼：");
        String courseId = scanner.nextLine().trim();

        if (courseId.isBlank()) { System.out.println("已取消。"); return; }

        if (courseId.equalsIgnoreCase("list")) {
            printCourseCatalog(repo);
            System.out.print("請輸入課程代碼：");
            courseId = scanner.nextLine().trim();
            if (courseId.isBlank()) { System.out.println("已取消。"); return; }
        }

        // ── 優先從課程目錄查找 ─────────────────────────────────────────────────
        final String finalCourseId = courseId;
        if (repo.findById(finalCourseId).isPresent()) {
            repo.findById(finalCourseId).ifPresent(course -> {
                boolean added = student.addCompletedCourse(course);
                if (added) System.out.printf("✅ 已新增（來自課程目錄）：%s%n", course);
                else       System.out.printf("⚠  [%s] 已在已修清單中。%n", finalCourseId);
            });
            return;
        }

        // ── 不在目錄中，依代碼規則引導 ─────────────────────────────────────────
        CodeType type;
        try { type = classifier.detectType(courseId); }
        catch (IllegalArgumentException e) { System.out.println("⚠ 無效的課程代碼。"); return; }

        Course course = switch (type) {
            case DEPARTMENT_COURSE -> {
                // F7 課程應在課程目錄中，不在則視為代碼輸入錯誤
                System.out.printf("⚠ 系內課程 [%s] 不在課程目錄中。%n", courseId);
                System.out.println("  請確認課程代碼是否正確，或輸入 'list' 查看可用課程目錄。");
                yield null;
            }
            case GENERAL_EDUCATION -> inputGeneralEducationCourse(scanner, courseId);
            case CHINESE           -> inputChineseCourse(scanner, courseId);
            case ENGLISH           -> inputEnglishCourse(scanner, courseId);
            case SECOND_LANGUAGE   -> {
                // 固定 2 學分，無需任何輸入
                System.out.printf("識別為【第二外語】（固定 2 學分）%n");
                yield new Course(courseId, courseId, 2.0, CreditCategory.SECOND_LANGUAGE);
            }
            case PE                -> inputPECourse(scanner, courseId);
            case OUTSIDE_ELECTIVE  -> inputOutsideElectiveCourse(scanner, courseId, repo);
        };

        if (course == null) { System.out.println("已取消。"); return; }

        boolean added = student.addCompletedCourse(course);
        if (added) System.out.printf("✅ 已新增：%s%n", course);
        else       System.out.printf("⚠  [%s] 已在已修清單中。%n", courseId);
    }

    // ── 各代碼類型的輸入方法 ──────────────────────────────────────────────────

    /**
     * A9：通識課程（選子類別 + 學分，不需輸入名稱）
     * 課程名稱自動設為課程代碼。
     */
    private static Course inputGeneralEducationCourse(Scanner scanner, String courseId) {
        System.out.printf("識別為【通識課程】（[%s] 不在課程目錄）%n", courseId);
        System.out.println("  1. 通識—人文學");
        System.out.println("  2. 通識—社會科學");
        System.out.println("  3. 通識—自然與工程科學");
        System.out.println("  4. 通識—生命科學與健康");
        System.out.println("  5. 通識—科際整合");
        System.out.println("  6. 通識—融合通識（上限 15 學分）");
        System.out.print("請選擇類別（Enter 取消）：");
        String sel = scanner.nextLine().trim();
        CreditCategory cat = switch (sel) {
            case "1" -> CreditCategory.GENERAL_HUMANITIES;
            case "2" -> CreditCategory.GENERAL_SOCIAL_SCIENCE;
            case "3" -> CreditCategory.GENERAL_NATURAL_SCIENCE;
            case "4" -> CreditCategory.GENERAL_LIFE_SCIENCE;
            case "5" -> CreditCategory.GENERAL_INTERDISCIPLINARY;
            case "6" -> CreditCategory.GENERAL_FUSION;
            default  -> null;
        };
        if (cat == null) return null;

        double credits = readCredits(scanner, 0.5, 15);
        if (credits < 0) return null;
        return new Course(courseId, courseId, credits, cat);
    }

    /**
     * A7：國文（僅輸入學分，上限 4，不需輸入名稱）
     */
    private static Course inputChineseCourse(Scanner scanner, String courseId) {
        System.out.println("識別為【校訂必修—國文】（上限 4 學分）");
        double credits = readCredits(scanner, 0.5, 4);
        if (credits < 0) return null;
        return new Course(courseId, courseId, credits, CreditCategory.CHINESE);
    }

    /**
     * A1-100~300：英文（僅輸入學分，上限 4，不需輸入名稱）
     */
    private static Course inputEnglishCourse(Scanner scanner, String courseId) {
        System.out.println("識別為【校訂必修—英文】（上限 4 學分）");
        double credits = readCredits(scanner, 0.5, 4);
        if (credits < 0) return null;
        return new Course(courseId, courseId, credits, CreditCategory.ENGLISH);
    }

    /**
     * A2：體育（選子類別，固定 0 學分，不需輸入名稱）
     */
    private static Course inputPECourse(Scanner scanner, String courseId) {
        System.out.printf("識別為【體育】（[%s] 不在課程目錄）%n", courseId);
        System.out.println("  1. 體育—健康知能（至少 1 次）");
        System.out.println("  2. 體育—專項體能（至少 1 次）");
        System.out.println("  3. 體育—二年級體育（至少 2 次）");
        System.out.print("請選擇類別（Enter 取消）：");
        String sel = scanner.nextLine().trim();
        CreditCategory cat = switch (sel) {
            case "1" -> CreditCategory.PE_HEALTH;
            case "2" -> CreditCategory.PE_SPORT;
            case "3" -> CreditCategory.PE_SOPHOMORE;
            default  -> null;
        };
        if (cat == null) return null;
        return new Course(courseId, courseId, 0.0, cat);
    }

    /**
     * 系外選修（唯一需要輸入名稱的類型，可詢問是否抵免系上必修）
     */
    private static Course inputOutsideElectiveCourse(Scanner scanner,
                                                     String courseId,
                                                     CourseRepository repo) {
        System.out.println("識別為【系外選修】");
        System.out.print("請輸入課程名稱（Enter 取消）：");
        String name = scanner.nextLine().trim();
        if (name.isBlank()) return null;

        // 檢查是否與系上必修同名（抵免）
        boolean nameMatchesRequired = repo.findByCategory(CreditCategory.REQUIRED_MAJOR)
                .stream()
                .anyMatch(c -> c.getCourseName().equals(name));

        CreditCategory finalCat = CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE;
        if (nameMatchesRequired) {
            System.out.printf("⚠  「%s」與系上必修課程同名。是否申請抵免？（y/n）：", name);
            String answer = scanner.nextLine().trim();
            if (answer.equalsIgnoreCase("y")) {
                finalCat = CreditCategory.REQUIRED_MAJOR;
                System.out.println("   已標記為系上必修（抵免）。");
            }
        }

        double credits = readCredits(scanner, 0.5, 99);
        if (credits < 0) return null;
        return new Course(courseId, name, credits, finalCat);
    }

    // ── 共用輸入工具 ──────────────────────────────────────────────────────────

    /**
     * 讀取學分數，支援 0.5 為單位，輸入空白時回傳 -1.0（取消訊號）。
     */
    private static double readCredits(Scanner scanner, double min, double max) {
        while (true) {
            System.out.printf("請輸入學分數（%s ~ %s，以 0.5 為單位，Enter 取消）：",
                    formatCredits(min), formatCredits(max));
            String input = scanner.nextLine().trim();
            if (input.isBlank()) return -1.0;
            try {
                double v = Double.parseDouble(input);
                if (v < min - 0.001 || v > max + 0.001) {
                    System.out.printf("⚠ 請輸入 %s 到 %s 之間的學分數%n",
                            formatCredits(min), formatCredits(max));
                    continue;
                }
                // 驗證為 0.5 的倍數
                double doubled = v * 2;
                if (Math.abs(doubled - Math.round(doubled)) > 0.001) {
                    System.out.println("⚠ 學分數必須為 0.5 的倍數（如 0.5, 1.0, 1.5, 2.0）");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("⚠ 請輸入數字（如 2, 2.5, 1.5）");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 功能三：課程清單分類輸出
    // ═══════════════════════════════════════════════════════════════════════════

    private static void printCourseListGrouped(Student student) {
        List<Course> courses = student.getCompletedCourses();
        System.out.println();

        if (courses.isEmpty()) {
            System.out.println("  （尚無已修課程）");
            return;
        }

        Map<CreditCategory, List<Course>> grouped = new EnumMap<>(CreditCategory.class);
        for (CreditCategory cat : CreditCategory.values()) {
            List<Course> catCourses = courses.stream()
                    .filter(c -> c.getCategory() == cat)
                    .toList();
            if (!catCourses.isEmpty()) grouped.put(cat, catCourses);
        }

        double totalCredits = 0;
        for (CreditCategory cat : CreditCategory.values()) {
            List<Course> catCourses = grouped.get(cat);
            if (catCourses == null) continue;

            double catCredits = catCourses.stream().mapToDouble(Course::getCredits).sum();
            totalCredits += catCredits;

            if (cat.isPE()) {
                System.out.printf("【%s】（%d 次）%n", cat.getDisplayName(), catCourses.size());
                catCourses.forEach(c ->
                        System.out.printf("  - [%s] %s%n", c.getCourseId(), c.getCourseName()));
            } else {
                System.out.printf("【%s】（%s 學分）%n",
                        cat.getDisplayName(), formatCredits(catCredits));
                catCourses.forEach(c ->
                        System.out.printf("  - [%s] %s（%s 學分）%n",
                                c.getCourseId(), c.getCourseName(), formatCredits(c.getCredits())));
            }
            System.out.println();
        }
        System.out.printf("共 %d 門課程，已修 %s 學分%n", courses.size(), formatCredits(totalCredits));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 模擬範例學生（王小明）
    // ═══════════════════════════════════════════════════════════════════════════

    private static Student loadDemoStudent() {
        Student s = new Student("B11234567", "王小明");

        // 系上必修（55 學分，缺 5）
        String[][] required = {
            {"F74401","程式設計","3"},{"F74402","資料結構","3"},{"F74403","演算法","3"},
            {"F74404","計算機組織","3"},{"F74405","作業系統","3"},{"F74406","計算機網路","3"},
            {"F74407","軟體工程","3"},{"F74408","資料庫系統","3"},{"F74409","編譯器設計","3"},
            {"F74410","離散數學","3"},{"F74411","機器學習","3"},{"F74413","微積分一","3"},
            {"F74414","微積分二","3"},{"F74415","線性代數","3"},{"F74416","普通物理","3"},
            {"F74417","普通物理實驗","1"},{"F74418","計算理論","3"},{"F74419","數位設計","3"},
            {"F74420","電子學","3"}
        };
        for (String[] r : required) {
            s.addCompletedCourse(new Course(r[0], r[1],
                    Double.parseDouble(r[2]), CreditCategory.REQUIRED_MAJOR));
        }

        // 系上選修（27 學分）
        String[][] elective = {
            {"F74501","自然語言處理","3"},{"F74502","電腦視覺","3"},{"F74503","分散式系統","3"},
            {"F74504","資訊安全","3"},{"F74505","雲端計算","3"},{"F74506","行動應用開發","3"},
            {"F74507","嵌入式系統","3"},{"F74508","計算生物學","3"},{"F74509","量子計算","3"}
        };
        for (String[] e : elective) {
            s.addCompletedCourse(new Course(e[0], e[1],
                    Double.parseDouble(e[2]), CreditCategory.MAJOR_ELECTIVE));
        }

        // 通識（16 學分，缺 2）
        s.addCompletedCourse(new Course("A9H001","藝術鑑賞",    2, CreditCategory.GENERAL_HUMANITIES));
        s.addCompletedCourse(new Course("A9H002","文學導論",    2, CreditCategory.GENERAL_HUMANITIES));
        s.addCompletedCourse(new Course("A9S001","社會學概論",  2, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        s.addCompletedCourse(new Course("A9N001","科學與技術",  2, CreditCategory.GENERAL_NATURAL_SCIENCE));
        s.addCompletedCourse(new Course("A9F001","科技與人文",  4, CreditCategory.GENERAL_FUSION));
        s.addCompletedCourse(new Course("A9F002","當代議題研究",4, CreditCategory.GENERAL_FUSION));

        // 系外選修 + 第二外語（8 學分）
        s.addCompletedCourse(new Course("ECON001","經濟學",3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        s.addCompletedCourse(new Course("PSYC001","心理學",3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        s.addCompletedCourse(new Course("A1-501","日語初級",2, CreditCategory.SECOND_LANGUAGE));

        // 校訂必修（國文 ✓、英文 ✓、踏溯台南 ✓）
        s.addCompletedCourse(new Course("A70001","大學國文一",2, CreditCategory.CHINESE));
        s.addCompletedCourse(new Course("A70002","大學國文二",2, CreditCategory.CHINESE));
        s.addCompletedCourse(new Course("A1-100","大學英文一",2, CreditCategory.ENGLISH));
        s.addCompletedCourse(new Course("A1-200","大學英文二",2, CreditCategory.ENGLISH));
        s.addCompletedCourse(new Course("TNTOUR01","踏溯台南",1, CreditCategory.INTRO_TAINAN));

        // 體育（健康 ✓、專項 ✓、二年級 1/2 次）
        s.addCompletedCourse(new Course("A2-001","健康與體育知識",0, CreditCategory.PE_HEALTH));
        s.addCompletedCourse(new Course("A2-002","游泳",          0, CreditCategory.PE_SPORT));
        s.addCompletedCourse(new Course("A2-003","二年級體育一",  0, CreditCategory.PE_SOPHOMORE));

        return s;
        // 總學分：55+27+16+8+4+4+1=115（缺15）、系上必修缺5、通識缺2、二年級體育缺1次
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 選單與學生輸入
    // ═══════════════════════════════════════════════════════════════════════════

    private static void printMainMenu() {
        System.out.println("\n" + "=".repeat(44));
        System.out.println("  成大畢業學分計算器");
        System.out.println("=".repeat(44));
        System.out.println("  1. 新增已修課程（依代碼自動分類）");
        System.out.println("  2. 顯示已修課程清單（依類別分組）");
        System.out.println("  3. 計算總學分");
        System.out.println("  4. 分類統計學分");
        System.out.println("  5. 計算剩餘學分");
        System.out.println("  6. 判斷畢業資格");
        System.out.println("  7. 產生畢業進度報告");
        System.out.println("  8. 查看課程目錄");
        System.out.println("  9. 載入模擬範例學生（王小明）");
        System.out.println("  0. 結束");
        System.out.print(">> ");
    }

    private static Student inputStudentInfo(Scanner scanner) {
        System.out.println("=".repeat(44));
        System.out.println("  成大畢業學分計算器");
        System.out.println("=".repeat(44));
        System.out.println("  （提示：主選單選 9 可快速載入模擬範例）");
        System.out.println();
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

    // ═══════════════════════════════════════════════════════════════════════════
    // 課程目錄
    // ═══════════════════════════════════════════════════════════════════════════

    private static void printCourseCatalog(CourseRepository repo) {
        System.out.println("\n=== 課程目錄 ===");
        for (CreditCategory category : CreditCategory.values()) {
            List<Course> courses = repo.findByCategory(category);
            if (courses.isEmpty()) continue;
            System.out.println("\n【" + category.getDisplayName() + "】");
            courses.forEach(c ->
                    System.out.printf("  %-12s %-20s%s%n",
                            c.getCourseId(), c.getCourseName(),
                            c.getCategory().isPE() ? ""
                                    : String.format("（%s 學分）", formatCredits(c.getCredits()))));
        }
        System.out.println();
    }

    private static void seedCourseCatalog(CourseRepository repo) {
        repo.clear();

        // ── 系上必修（F744xx） ─────────────────────────────────────────────────
        repo.save(new Course("F74401","程式設計",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74402","資料結構",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74403","演算法",          3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74404","計算機組織",      3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74405","作業系統",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74406","計算機網路",      3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74407","軟體工程",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74408","資料庫系統",      3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74409","編譯器設計",      3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74410","離散數學",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74411","機器學習",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74412","深度學習",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74413","微積分一",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74414","微積分二",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74415","線性代數",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74416","普通物理",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74417","普通物理實驗",    1, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74418","計算理論",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74419","數位設計",        3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F74420","電子學",          3, CreditCategory.REQUIRED_MAJOR));

        // ── 系上選修（F745xx） ─────────────────────────────────────────────────
        repo.save(new Course("F74501","自然語言處理",    3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F74502","電腦視覺",        3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F74503","分散式系統",      3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F74504","資訊安全",        3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F74505","雲端計算",        3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F74506","行動應用開發",    3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F74507","嵌入式系統",      3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F74508","計算生物學",      3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F74509","量子計算",        3, CreditCategory.MAJOR_ELECTIVE));

        // ── 通識（A9）─────────────────────────────────────────────────────────
        repo.save(new Course("A9H001","藝術鑑賞",        2, CreditCategory.GENERAL_HUMANITIES));
        repo.save(new Course("A9H002","文學導論",        2, CreditCategory.GENERAL_HUMANITIES));
        repo.save(new Course("A9S001","社會學概論",      2, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        repo.save(new Course("A9S002","政治學",          2, CreditCategory.GENERAL_SOCIAL_SCIENCE));
        repo.save(new Course("A9N001","科學與技術",      2, CreditCategory.GENERAL_NATURAL_SCIENCE));
        repo.save(new Course("A9N002","環境科學",        2, CreditCategory.GENERAL_NATURAL_SCIENCE));
        repo.save(new Course("A9L001","生命倫理",        2, CreditCategory.GENERAL_LIFE_SCIENCE));
        repo.save(new Course("A9I001","跨學科研究",      2, CreditCategory.GENERAL_INTERDISCIPLINARY));
        repo.save(new Course("A9F001","科技與人文",      4, CreditCategory.GENERAL_FUSION));
        repo.save(new Course("A9F002","當代議題研究",    4, CreditCategory.GENERAL_FUSION));
        repo.save(new Course("A9F003","全球化與在地化",  3, CreditCategory.GENERAL_FUSION));

        // ── 國文（A7）────────────────────────────────────────────────────────
        repo.save(new Course("A70001","大學國文一",      2, CreditCategory.CHINESE));
        repo.save(new Course("A70002","大學國文二",      2, CreditCategory.CHINESE));

        // ── 英文（A1-100~200）────────────────────────────────────────────────
        repo.save(new Course("A1-100","大學英文一",      2, CreditCategory.ENGLISH));
        repo.save(new Course("A1-200","大學英文二",      2, CreditCategory.ENGLISH));

        // ── 第二外語（A1-500~590）────────────────────────────────────────────
        repo.save(new Course("A1-501","日語初級",        2, CreditCategory.SECOND_LANGUAGE));
        repo.save(new Course("A1-502","法語初級",        2, CreditCategory.SECOND_LANGUAGE));
        repo.save(new Course("A1-503","德語初級",        2, CreditCategory.SECOND_LANGUAGE));

        // ── 體育（A2）────────────────────────────────────────────────────────
        repo.save(new Course("A2-001","健康與體育知識",  0, CreditCategory.PE_HEALTH));
        repo.save(new Course("A2-002","游泳",            0, CreditCategory.PE_SPORT));
        repo.save(new Course("A2-003","羽球",            0, CreditCategory.PE_SPORT));
        repo.save(new Course("A2-011","二年級體育一",    0, CreditCategory.PE_SOPHOMORE));
        repo.save(new Course("A2-012","二年級體育二",    0, CreditCategory.PE_SOPHOMORE));

        // ── 踏溯台南 ──────────────────────────────────────────────────────────
        repo.save(new Course("TNTOUR01","踏溯台南",      1, CreditCategory.INTRO_TAINAN));

        // ── 系外選修 ──────────────────────────────────────────────────────────
        repo.save(new Course("ECON001","經濟學",         3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        repo.save(new Course("PSYC001","心理學",         3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        repo.save(new Course("MGT001", "管理學",         3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
        repo.save(new Course("SOC001", "社會學",         3, CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE));
    }
}

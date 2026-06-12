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
        //大一
        repo.save(new Course("F711110", "程式設計（一）", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F711120-1", "程式設計（二）甲", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F711120-2", "程式設計（二）乙", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F715611", "微積分一", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F715621", "微積分二", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F720100-1", "線性代數甲", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F720100-2", "線性代數乙", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F715910", "普通物理學（一）", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F715920", "普通物理學（二）", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F711610", "資訊技術專案開發與實作（一）", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F711620", "資訊技術專案開發與實作（二）", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F720401-1", "數位電路導論甲", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F720401-2", "數位電路導論乙", 3, CreditCategory.REQUIRED_MAJOR));
        //大二
        repo.save(new Course("F720300-1", "資料結構", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F720600", "離散數學", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F721300-1", "演算法甲", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F721300-2", "演算法乙", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F720200", "計算機組織", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F723500", "電腦網路概論", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F721100-1", "機率與統計甲", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F721100-2", "機率與統計乙", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F720900-1", "數位系統導論", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F721000-1", "數位系統實驗甲", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F721000-2", "數位系統實驗乙", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F711630", "資訊技術專案開發與實作（三）", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F711640", "資訊技術專案開發與實作（四）", 3, CreditCategory.REQUIRED_MAJOR));
        //大三
        repo.save(new Course("F730210", "資訊專題（一）", 2, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F730220", "資訊專題（二）", 2, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F732400", "編譯系統", 3, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F732500-1", "微算機原理與應用（含實驗）甲", 4, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F732500-2", "微算機原理與應用（含實驗）乙", 4, CreditCategory.REQUIRED_MAJOR));
        repo.save(new Course("F720800", "作業系統", 3, CreditCategory.REQUIRED_MAJOR));
        //大四 無
        


        // 系上選修
        //大一
        repo.save(new Course("F711700", "機器人軟體系統專案", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("E111900", "從創意到創新", 1, CreditCategory.MAJOR_ELECTIVE));
        //大二
        repo.save(new Course("F730500", "資訊工程倫理與生涯規劃", 1, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F724800", "進階英語課程-專業科學英文（一）", 1, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F723400", "Linux 系統與開源軟體", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F721900", "視窗程式設計", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F731200", "資訊安全", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("E120400", "從創意到創新（二）", 2, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F724100", "機器學習II: 非監督式學習", 1, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F724200", "集成卷積神經網路", 1, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F725200", "資料視覺化與探索", 2, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("E222400", "傅立葉轉換在類比與數位通訊的應用", 1, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F722700", "JAVA軟體開發", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F725300", "機器人系統軟體開發", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F733500", "即時系統導論", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F723700", "Linux系統程式設計", 1, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F725100", "物聯網設備之程式設計與界面", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F725200", "資料視覺化與探索", 2, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F724000", "機器學習I:監督式學習", 2, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F724900", "進階英語課程-專業科學英文（二）", 1, CreditCategory.MAJOR_ELECTIVE));
        //大三
        repo.save(new Course("F732001", "計算理論", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F722500", "多處理機平行程式設計", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F733800", "統計學習導論", 1, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F730600", "多媒體系統與應用", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F711000", "人工智慧在中醫的應用", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F730300", "訊號與系統", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F733400", "Linux核心設計", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F733200", "智慧型醫學資訊系統實作", 3, CreditCategory.MAJOR_ELECTIVE));
        //大四
        repo.save(new Course("F742900", "人工智慧導論", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F743000", "影像處理、電腦視覺及深度學習概論", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F743800", "佇列系統與模型建構導論", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F743900-1", "機率模型及數據科學甲", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F743900-2", "機率模型及數據科學乙", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("P75J100", "創意行動網路APPs之系統技術與設計研發", 3, CreditCategory.MAJOR_ELECTIVE));
        repo.save(new Course("F741900", "繪圖技術設計與應用", 3, CreditCategory.MAJOR_ELECTIVE));


        // 通識課程
        repo.save(new Course("A92F300", "流行樂賞析與實務", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92F100", "音樂文化導引", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92U400", "戲劇表演與戲劇療癒", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A918600", "戲劇與小說", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92P400", "劇本讀演概論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92D300-1", "戲劇賞析", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94C200", "朝聖的世界史", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93A300", "傳統戲曲與表演藝術", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94H500", "你所不知道的臺灣藝術家", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92L000", "戲劇製作", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92P500", "電影藝術與生活", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A925603-1", "女性文學選讀", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A925603-2", "女性文學選讀", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A915101-1", "臺灣古典文學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A915101-2", "臺灣古典文學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92K800", "愛爾蘭歷史與文化", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A937800", "佛教思想與現代社會", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92V900", "跨文化溝通的建構與實踐", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92U300", "語言學概論: 語言的科學及應用", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92B600", "解碼台灣當代電影", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92T400-2", "臺灣閩南語曲謠之詞曲賞析與實務", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92U500", "漫遊藝術", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94H700", "基督宗教的世界觀", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94G000", "生命之道：基督思想的觀點與世界觀", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A914502-2", "藝術叩門", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94H800", "一沙一世界：從文本再現與影像觀看世界", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94H900", "錦織成大", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94F200", "從韓國文化中學習韓文表達", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94F700", "儒家倫理與文化交談", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94H600", "現代生活哲學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92B700", "死亡文化史", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94F400", "歌劇與舞劇賞析", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94F300", "音樂劇作品賞析", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A919800", "巴洛克時期至浪漫樂派的音樂與作品探討", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91A100", "生命故事與個人觀點下的民族誌", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92R000-1", "數位音樂創作", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91A000", "作曲家與音樂敘事", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94F900", "跨文化的音樂之旅", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A919700", "亞洲電影配樂風格賞析", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94F800", "華語流行音樂發展史-風格與賞析", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A911001", "越南社會與文化", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I900", "外國作家、作品及文化背景概論（重點為美國、日本及歐洲文學）", 3, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92V000", "基本人權案例分析", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91A200", "民刑事法律案例分析", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I000", "性別關係 : 身體、文化與權力", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93E600", "台灣原住民族當代議題與社會實踐", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A974500-1", "心理學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A974500-3", "心理學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A959800", "創意思考", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93E500", "心理急救你我他—從心理疾病復元之生命敘事", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A952900", "家庭關係", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92S800", "拖延心理學：理論篇", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A9E3300", "潛能開發-魅力學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94D700", "現代社會與刑罰", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93E700", "生活經濟學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93E800", "運動經濟學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91B600", "權力與秩序：台灣、中國、日本與美國的政治發展與挑戰", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92U200", "從心理學看成功老化", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94G700", "中藥治療學概論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91A500", "魔術與全人醫療", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A962402", "職能治療與健康", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A962902", "疾病媒介", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94G800", "聊聊婦產科：疑惑與解析", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A962201", "生物多樣性", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A934101", "生命科學概論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A952502", "情緒與壓力管理", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A948800", "食品安全與衛生", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A966001", "兒童青少年的健康", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A944300-1", "基因改造食品的好與壞", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A963002", "急難救助", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A963102", "動物寄生蟲與生活", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A962501", "家庭與健康", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92C400-1", "日常疼痛控制", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A968002", "運動與健康", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A960502", "身體結構與功能", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92V500", "生命故事傳遞生命動力", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92T900", "高齡輔具", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92W400", "肌肉解剖與肌力訓練入門", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94E600", "運動醫學與科學入門", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94C500", "生活中之細胞功能運作", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92T800", "心血管保健之道", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92N200", "生活中的毒物", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94G600", "電影中的健康醫學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A934200", "光電科技導論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A936800", "生活中玩光電", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A932002-1", "應用化學與實驗", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A932002-2", "應用化學與實驗", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I100", "綠領人才：用綠色改變生活", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A945501-1", "環境污染與防治", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94G500", "永續健康居住環境-綠建築技術", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A930903", "材料科技概論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A941000", "界面化學的生活應用", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A936900", "認知神經科學導論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A943502", "應用電學", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92T600", "安全耐震我的家", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94G400", "現代戰爭的致勝關鍵 : 高科技武器", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91A400", "未來戰爭決勝時刻：資訊戰", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93F000", "發現台灣在地脊椎動物化石", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94E900", "虛擬世界之設計與研究", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I200", "從差異到共融：神經多樣性兒童的生活參與和健康", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I300", "AI 時代：變動世界之素養與技能", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I600", "美感與科技藝術概論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92C000", "無障礙智慧生活環境", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I400", "生老病死—生命四季樂章~談人生旅程", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A9E2900", "生醫用工程學導論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91B100", "設計思考遇見生成式ＡＩ應用之旅", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91B300", "系統思維與統計應用—人文與科技", 3, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A941100", "校園文化資產", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92J400", "開放資料與智慧生活", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92A600", "諾貝爾生醫獎得主所改變的世界", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92W500", "智慧科技與運動治療", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91B200", "健康照護志願服務之理念與實作", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94H000", "心理學與健康生活", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91B000", "資料科學應用與專利資訊畫布", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92Q000", "朗讀者：錄製有聲書服務視障者", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91A900", "ＡＩ在音樂創作與編曲中的應用", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93D500", "科技與生命價值", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93D600", "藝術治療與情緒:理論與實作", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A947900", "環境,職業與健康人生", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92T100", "從電影與繪本談老年", 3, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94D300", "永續發展目標（SDGS）概論", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93F500", "半導體通路商產業分析", 3, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I700", "全民國防教育軍事訓練-國防科技", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I800", "全民國防教育軍事訓練-防衛動員", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91B500", "薪傳之言", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A9K0201-1", "通識教育生活實踐（一）", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A9K0202", "通識教育生活實踐（二）", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A9K0203", "通識教育生活實踐（三）", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A9K0201-2", "通識教育生活實踐（一）", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94B400", "通識教育生活實踐（甲）", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A934500", "通識教育生活實踐（乙）", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A934600", "通識教育生活實踐（丙）", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A934700", "通識教育生活實踐（丁）", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93I000", "通識教育生活實踐（戊）", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93I100", "通識教育生活實踐（己）", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91B700", "通識專題講座-大學導航", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A92M500", "拍出跨域資訊應用首部曲--python", 1, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A93D000", "個人品牌經營-職涯發展與就業增能", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A94I500", "科技領航：菁英養成課程 （Ｃ）", 2, CreditCategory.GENERAL_EDUCATION));
        repo.save(new Course("A91B400", "通識競賽專題實作", 3, CreditCategory.GENERAL_EDUCATION));

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

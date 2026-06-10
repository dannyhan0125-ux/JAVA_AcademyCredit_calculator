package ncku.calculator;

import ncku.calculator.model.CreditCategory;
import ncku.calculator.service.CourseClassifier;
import ncku.calculator.service.CourseClassifier.CodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CourseClassifier 單元測試（TDD — 先撰寫測試案例）
 * 驗證各類課程代碼規則的正確判斷。
 */
@DisplayName("CourseClassifier 課程代碼分類測試")
class CourseClassifierTest {

    private CourseClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new CourseClassifier();
    }

    // ─── F7 開頭：系內課程 ────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-CLS01：F7 開頭應識別為系內課程")
    void testF7_departmentCourse() {
        assertEquals(CodeType.DEPARTMENT_COURSE, classifier.detectType("F74401"));
        assertEquals(CodeType.DEPARTMENT_COURSE, classifier.detectType("F7B0001"));
        assertEquals(CodeType.DEPARTMENT_COURSE, classifier.detectType("f74401")); // 小寫
    }

    // ─── A9 開頭：通識課程 ────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-CLS02：A9 開頭應識別為通識課程")
    void testA9_generalEducation() {
        assertEquals(CodeType.GENERAL_EDUCATION, classifier.detectType("A9H001"));
        assertEquals(CodeType.GENERAL_EDUCATION, classifier.detectType("A9F001"));
        assertEquals(CodeType.GENERAL_EDUCATION, classifier.detectType("a9s001")); // 小寫
    }

    // ─── A7 開頭：國文 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-CLS03：A7 開頭應識別為國文")
    void testA7_chinese() {
        assertEquals(CodeType.CHINESE, classifier.detectType("A70001"));
        assertEquals(CodeType.CHINESE, classifier.detectType("A70002"));
        assertEquals(CreditCategory.CHINESE, classifier.classifyDirect("A70001"));
    }

    // ─── A1-100 ~ A1-300：英文 ───────────────────────────────────────────────

    @Test
    @DisplayName("TC-CLS04：A1-100 應識別為英文")
    void testA1_100_english() {
        assertEquals(CodeType.ENGLISH, classifier.detectType("A1-100"));
        assertEquals(CreditCategory.ENGLISH, classifier.classifyDirect("A1-100"));
    }

    @Test
    @DisplayName("TC-CLS05：A1-200 應識別為英文")
    void testA1_200_english() {
        assertEquals(CodeType.ENGLISH, classifier.detectType("A1-200"));
    }

    @Test
    @DisplayName("TC-CLS06：A1-300 應識別為英文（邊界值）")
    void testA1_300_english_boundary() {
        assertEquals(CodeType.ENGLISH, classifier.detectType("A1-300"));
    }

    // ─── A1-500 ~ A1-590：第二外語 ───────────────────────────────────────────

    @Test
    @DisplayName("TC-CLS07：A1-500 應識別為第二外語")
    void testA1_500_secondLanguage() {
        assertEquals(CodeType.SECOND_LANGUAGE, classifier.detectType("A1-500"));
        assertEquals(CreditCategory.SECOND_LANGUAGE, classifier.classifyDirect("A1-500"));
    }

    @Test
    @DisplayName("TC-CLS08：A1-590 應識別為第二外語（邊界值）")
    void testA1_590_secondLanguage_boundary() {
        assertEquals(CodeType.SECOND_LANGUAGE, classifier.detectType("A1-590"));
    }

    @Test
    @DisplayName("TC-CLS09：A1-400 不在英文或第二外語範圍內，應識別為系外選修")
    void testA1_400_outsideElective() {
        assertEquals(CodeType.OUTSIDE_ELECTIVE, classifier.detectType("A1-400"));
    }

    // ─── A2 開頭：體育 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-CLS10：A2 開頭應識別為體育")
    void testA2_pe() {
        assertEquals(CodeType.PE, classifier.detectType("A20001"));
        assertEquals(CodeType.PE, classifier.detectType("A2-001"));
        assertEquals(CodeType.PE, classifier.detectType("a20002")); // 小寫
    }

    // ─── 其他：系外選修 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-CLS11：不符合任何前置規則的代碼應識別為系外選修")
    void testOther_outsideElective() {
        assertEquals(CodeType.OUTSIDE_ELECTIVE, classifier.detectType("ECON001"));
        assertEquals(CodeType.OUTSIDE_ELECTIVE, classifier.detectType("PSYCH101"));
        assertEquals(CodeType.OUTSIDE_ELECTIVE, classifier.detectType("MGT-001"));
        assertEquals(CreditCategory.OUTSIDE_DEPARTMENT_ELECTIVE,
                classifier.classifyDirect("ECON001"));
    }

    // ─── 例外情況 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-CLS12：空白或 null 代碼應拋出例外")
    void testBlankCourseId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> classifier.detectType(""));
        assertThrows(IllegalArgumentException.class, () -> classifier.detectType("  "));
        assertThrows(IllegalArgumentException.class, () -> classifier.detectType(null));
    }

    @Test
    @DisplayName("TC-CLS13：classifyDirect 對需互動類型（F7/A9/A2）應回傳 null")
    void testClassifyDirect_nullForInteractiveTypes() {
        assertNull(classifier.classifyDirect("F74401")); // 需確認必修/選修
        assertNull(classifier.classifyDirect("A9H001")); // 需選通識子類
        assertNull(classifier.classifyDirect("A20001")); // 需選體育子類
    }
}

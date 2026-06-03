# 成大畢業學分計算器 (NCKU Graduation Credit Calculator)

## 專案簡介
本專案為一套以 Java 開發的「畢業學分計算器」。
系統目標是協助學生快速了解自己的畢業進度，自動統計已修學分、各類別學分以及畢業資格狀態。

本專案除了實作功能外，也希望實踐軟體工程相關概念，包括：
- 軟體架構設計 (Software Architecture)
- Clean Code
- Unit Testing
- Test Driven Development (TDD)
- Debugging
- Refactoring
- AI Assisted Development

## 專案目標
系統應能夠：
1. 統計學生已修總學分
2. 統計各類別已修學分
3. 計算各類別尚缺學分
4. 判斷是否符合畢業資格
5. 產生畢業進度報告

## 課程分類
每門課程皆屬於以下其中一種類別：
1. 系上必修 (Required Major)
2. 系上選修 (Major Elective)
3. 通識課程 (General Education)
4. 系外選修 (Outside Department Elective)
5. 校訂必修

## 學生資料
每位學生應包含：
- 學號
- 姓名
- 已修課程清單

## 課程資料
每門課程應包含：
- 課程代碼
- 課程名稱
- 學分數
- 課程分類 (通識)

## 畢業門檻
初始版本使用以下設定：

| 類別 | 需求學分 |
| --- | --- |
| 系上必修 | 60 |
| 系上選修 | >=21 |
| 通識 | 19 |
| 系外選修 | <=21 |
| 校訂必修 | 4+4+0*4+1 (國+英+體+踏) |

**畢業總學分：130**

## 系統功能
### 功能一：新增已修課程
學生可以新增已修課程資料。

### 功能二：顯示課程清單
顯示所有已修課程。

### 功能三：計算總學分
統計目前已修總學分。

### 功能四：分類統計學分
依課程類別統計學分。
例如：
- 系上必修：55 學分
- 系上選修：20 學分
- 通識：18 學分

### 功能五：計算剩餘學分
顯示每個類別尚缺多少學分。
例如：
- 系上必修尚缺 5 學分
- 通識尚缺 10 學分

### 功能六：判斷畢業資格
若所有類別與總學分皆達標：
- 顯示：「符合畢業資格」
- 否則顯示：「尚未符合畢業資格」

### 功能七：產生畢業進度報告
範例：
- 學生：王小明
- 總學分：102/128
- 系上必修：55/60
- 系上選修：24/30
- 通識：20/28
- 系外選修：12/12
- 畢業狀態：尚未符合畢業資格

## 建議系統架構
採用分層架構 (Layered Architecture)。

### Model 層
負責資料結構。
類別：
- Student
- Course
- GraduationRequirement
- GraduationReport
- Enum CreditCategory

### Service 層
負責商業邏輯。
類別：
- CreditCalculator
- GraduationChecker

### Repository 層
負責資料存取。
類別：
- CourseRepository (初期可使用記憶體儲存 In-Memory Repository)

## UML 初步規劃
### Student
- studentId
- studentName
- completedCourses

### Course
- courseId
- courseName
- credits
- category

### GraduationRequirement
- totalCreditsRequired
- categoryRequirements

### CreditCalculator
- calculateTotalCredits()
- calculateCreditsByCategory()

### GraduationChecker
- checkGraduation()
- generateReport()

## Clean Code 要求
請遵守以下原則：
- 使用有意義的命名
- Method 不宜過長
- 一個 Method 只負責一件事
- 避免重複程式碼
- 避免 Main 方法過於肥大
- 遵守 Single Responsibility Principle

## 單元測試需求
使用 JUnit 5。
至少包含以下測試案例：
1. 正確計算總學分
2. 正確計算分類學分
3. 正確計算缺少學分
4. 符合畢業資格
5. 不符合畢業資格
6. 超過門檻時缺額應為 0
7. 重複課程處理

## TDD 開發流程
遵循：Red -> Green -> Refactor
流程：
1. 先撰寫測試案例
2. 執行測試（失敗）
3. 撰寫最小可行程式碼
4. 讓測試通過
5. 重構程式碼
6. 再次執行所有測試

## Debugging 流程
遇到 Bug 時遵循：
1. 重現問題
2. 提出假設
3. 設計驗證方法
4. 驗證假設
5. 修正根本原因

## AI 協助開發內容
本專案希望利用 AI 協助：
- 需求分析
- 專案規劃
- UML 設計
- 類別設計
- 單元測試設計
- Debugging
- Code Review
- Refactoring
- 技術決策討論

*重要 AI 對話應保留截圖作為期末報告素材。*

## Git Commit 規劃
建議開發歷程：
1. 初始化專案
2. 建立 Model 類別
3. 建立 CreditCategory
4. 建立 CreditCalculator
5. 完成總學分計算
6. 完成分類統計
7. 新增畢業資格判斷
8. 撰寫單元測試
9. 修正 Bug
10. 重構程式碼
11. 完成 Demo

## 非本次專案範圍
本次專案不實作：
- 資料庫
- 使用者登入
- 網頁系統
- Spring Boot
- 複雜 GUI

*重點放在軟體工程流程、測試、架構與 AI 輔助開發。*

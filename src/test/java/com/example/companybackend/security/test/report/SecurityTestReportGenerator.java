package com.example.companybackend.security.test.report;

import com.example.companybackend.security.test.SecurityTestDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * セキュリティテストレポート生成クラス
 * 
 * 目的:
 * - セキュリティテスト結果の詳細レポート生成
 * - テスト失敗時の修正提案生成
 * - テスト統計情報の可視化
 * 
 * 機能:
 * - 詳細テスト結果レポート生成機能
 * - テスト失敗時の修正提案機能
 * 
 * 要件対応:
 * - フェーズ5の要件6.2を満たす
 * - フェーズ5の要件6.3を満たす
 */
@Component
public class SecurityTestReportGenerator {

    @Autowired
    private SecurityTestDataManager testDataManager;

    /**
     * 詳細テスト結果レポートを生成する
     * 
     * @param reportPath レポート出力先パス
     * @throws IOException ファイル書き込みエラー
     */
    public void generateDetailedTestReport(String reportPath) throws IOException {
        StringBuilder report = new StringBuilder();
        
        // レポートヘッダー
        report.append("# セキュリティテスト詳細レポート\n\n");
        report.append("## レポート情報\n");
        report.append("- 生成日時: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        report.append("- レポート種別: 詳細テスト結果\n\n");
        
        // テスト統計情報
        report.append("## テスト統計情報\n");
        Map<String, Object> statistics = testDataManager.getTestStatistics();
        appendStatistics(report, statistics);
        
        // 詳細テスト結果
        report.append("\n## 詳細テスト結果\n");
        List<Map<String, Object>> detailedResults = testDataManager.getDetailedTestResults();
        appendDetailedResults(report, detailedResults);
        
        // 失敗したテストの修正提案
        report.append("\n## 修正提案\n");
        List<Map<String, Object>> failedTests = testDataManager.getFailedTests();
        appendFixSuggestions(report, failedTests);
        
        // レポート出力
        writeReportToFile(reportPath, report.toString());
    }

    /**
     * テスト統計情報をレポートに追加する
     * 
     * @param report レポートStringBuilder
     * @param statistics 統計情報
     */
    private void appendStatistics(StringBuilder report, Map<String, Object> statistics) {
        if (statistics.isEmpty()) {
            report.append("統計情報がありません。\n");
            return;
        }
        
        report.append("| テスト種別 | 合計テスト数 | 成功数 | 失敗数 | 平均実行時間(ms) |\n");
        report.append("|------------|--------------|--------|--------|------------------|\n");
        
        // 各テスト種別の統計情報を追加
        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> stats = (Map<String, Object>) entry.getValue();
                report.append("| ").append(entry.getKey()).append(" | ")
                      .append(stats.getOrDefault("total", "N/A")).append(" | ")
                      .append(stats.getOrDefault("passed", "N/A")).append(" | ")
                      .append(stats.getOrDefault("failed", "N/A")).append(" | ")
                      .append(stats.getOrDefault("averageTime", "N/A")).append(" |\n");
            } else {
                report.append("| ").append(entry.getKey()).append(" | ");
                report.append("N/A | N/A | N/A | N/A |\n");
            }
        }
    }

    /**
     * 詳細テスト結果をレポートに追加する
     * 
     * @param report レポートStringBuilder
     * @param detailedResults 詳細テスト結果リスト
     */
    private void appendDetailedResults(StringBuilder report, List<Map<String, Object>> detailedResults) {
        if (detailedResults.isEmpty()) {
            report.append("詳細テスト結果がありません。\n");
            return;
        }
        
        report.append("| テストスイート | テストケース | テスト種別 | ステータス | 実行時間(ms) | エラーメッセージ |\n");
        report.append("|----------------|--------------|------------|------------|--------------|----------------|\n");
        
        for (Map<String, Object> result : detailedResults) {
            report.append("| ")
                  .append(result.getOrDefault("test_suite_name", "N/A")).append(" | ")
                  .append(result.getOrDefault("test_case_name", "N/A")).append(" | ")
                  .append(result.getOrDefault("test_type", "N/A")).append(" | ")
                  .append(result.getOrDefault("status", "N/A")).append(" | ")
                  .append(result.getOrDefault("execution_time_ms", "N/A")).append(" | ")
                  .append(result.getOrDefault("error_message", "N/A")).append(" |\n");
        }
    }

    /**
     * 失敗したテストの修正提案をレポートに追加する
     * 
     * @param report レポートStringBuilder
     * @param failedTests 失敗したテストリスト
     */
    private void appendFixSuggestions(StringBuilder report, List<Map<String, Object>> failedTests) {
        if (failedTests.isEmpty()) {
            report.append("失敗したテストはありません。すべてのテストが成功しました。\n");
            return;
        }
        
        report.append("以下のテストが失敗しました。各エラーに対して修正提案を提供します。\n\n");
        
        for (Map<String, Object> failedTest : failedTests) {
            String testCaseName = (String) failedTest.getOrDefault("test_case_name", "N/A");
            String errorMessage = (String) failedTest.getOrDefault("error_message", "N/A");
            String testType = (String) failedTest.getOrDefault("test_type", "N/A");
            
            report.append("### テストケース: ").append(testCaseName).append("\n");
            report.append("- **エラー内容**: ").append(errorMessage).append("\n");
            report.append("- **修正提案**: \n");
            
            switch (testType) {
                case "SQL_INJECTION_PROTECTION":
                    report.append("  1. SQLインジェクション対策フィルターが適切に設定されているか確認してください。\n");
                    report.append("  2. パラメータ化クエリまたはORMを使用しているか確認してください。\n");
                    report.append("  3. 入力値検証ロジックを強化してください。\n");
                    break;
                case "XSS_PROTECTION":
                    report.append("  1. XSS対策フィルターが適切に設定されているか確認してください。\n");
                    report.append("  2. 出力エスケープ処理が適切に行われているか確認してください。\n");
                    report.append("  3. Content Security Policyヘッダーが適切に設定されているか確認してください。\n");
                    break;
                case "CSRF_PROTECTION":
                    report.append("  1. CSRFトークン生成・検証ロジックが適切に実装されているか確認してください。\n");
                    report.append("  2. すべての状態変更リクエストにCSRFトークンが含まれているか確認してください。\n");
                    report.append("  3. Origin/Refererヘッダー検証が適切に行われているか確認してください。\n");
                    break;
                case "JWT_AUTHENTICATION":
                    report.append("  1. JWTトークンの生成・検証ロジックが適切に実装されているか確認してください。\n");
                    report.append("  2. トークンの有効期限設定が適切か確認してください。\n");
                    report.append("  3. 秘密鍵の管理方法を確認してください。\n");
                    break;
                case "RATE_LIMITING":
                    report.append("  1. レート制限アルゴリズムの実装を確認してください。\n");
                    report.append("  2. IPアドレスまたはユーザー単位の制限設定を確認してください。\n");
                    report.append("  3. 制限超過時の適切なレスポンスを確認してください。\n");
                    break;
                case "COMMAND_INJECTION":
                    report.append("  1. コマンド実行時の入力値検証を確認してください。\n");
                    report.append("  2. システムコマンドの実行を避けるか、安全な方法で実行しているか確認してください。\n");
                    report.append("  3. 入力値のエスケープ処理を確認してください。\n");
                    break;
                case "PATH_TRAVERSAL":
                    report.append("  1. ファイルパスの検証ロジックを確認してください。\n");
                    report.append("  2. 相対パスの使用を制限しているか確認してください。\n");
                    report.append("  3. アップロードファイルの保存場所を確認してください。\n");
                    break;
                case "FILE_UPLOAD_SECURITY":
                    report.append("  1. アップロードファイルの拡張子制限を確認してください。\n");
                    report.append("  2. ファイルコンテンツの検証を確認してください。\n");
                    report.append("  3. アップロードファイルの保存場所とアクセス制限を確認してください。\n");
                    break;
                default:
                    report.append("  1. エラーメッセージを確認し、失敗の原因を特定してください。\n");
                    report.append("  2. 関連するセキュリティフィルターや検証ロジックを確認してください。\n");
                    report.append("  3. 必要に応じてセキュリティ設定を調整してください。\n");
                    break;
            }
            
            report.append("  4. 再テストを実行して修正を検証してください。\n\n");
        }
    }

    /**
     * レポートをファイルに出力する
     * 
     * @param reportPath レポート出力先パス
     * @param content レポート内容
     * @throws IOException ファイル書き込みエラー
     */
    private void writeReportToFile(String reportPath, String content) throws IOException {
        Path path = Paths.get(reportPath);
        Files.createDirectories(path.getParent());
        
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write(content);
        }
    }

    /**
     * テスト失敗時の修正提案を生成する
     * 
     * @param failedTest 失敗したテスト情報
     * @return 修正提案
     */
    public String generateFixSuggestion(Map<String, Object> failedTest) {
        StringBuilder suggestion = new StringBuilder();
        
        String testCaseName = (String) failedTest.getOrDefault("test_case_name", "N/A");
        String errorMessage = (String) failedTest.getOrDefault("error_message", "N/A");
        String testType = (String) failedTest.getOrDefault("test_type", "N/A");
        
        suggestion.append("### テストケース: ").append(testCaseName).append("\n");
        suggestion.append("- **テスト種別**: ").append(testType).append("\n");
        suggestion.append("- **エラー内容**: ").append(errorMessage).append("\n");
        suggestion.append("- **修正提案**: \n");
        
        switch (testType) {
            case "SQL_INJECTION_PROTECTION":
                suggestion.append("  1. SQLインジェクション対策フィルターが適切に設定されているか確認してください。\n");
                suggestion.append("  2. パラメータ化クエリまたはORMを使用しているか確認してください。\n");
                suggestion.append("  3. 入力値検証ロジックを強化してください。\n");
                break;
            case "XSS_PROTECTION":
                suggestion.append("  1. XSS対策フィルターが適切に設定されているか確認してください。\n");
                suggestion.append("  2. 出力エスケープ処理が適切に行われているか確認してください。\n");
                suggestion.append("  3. Content Security Policyヘッダーが適切に設定されているか確認してください。\n");
                break;
            case "CSRF_PROTECTION":
                suggestion.append("  1. CSRFトークン生成・検証ロジックが適切に実装されているか確認してください。\n");
                suggestion.append("  2. すべての状態変更リクエストにCSRFトークンが含まれているか確認してください。\n");
                suggestion.append("  3. Origin/Refererヘッダー検証が適切に行われているか確認してください。\n");
                break;
            case "JWT_AUTHENTICATION":
                suggestion.append("  1. JWTトークンの生成・検証ロジックが適切に実装されているか確認してください。\n");
                suggestion.append("  2. トークンの有効期限設定が適切か確認してください。\n");
                suggestion.append("  3. 秘密鍵の管理方法を確認してください。\n");
                break;
            case "RATE_LIMITING":
                suggestion.append("  1. レート制限アルゴリズムの実装を確認してください。\n");
                suggestion.append("  2. IPアドレスまたはユーザー単位の制限設定を確認してください。\n");
                suggestion.append("  3. 制限超過時の適切なレスポンスを確認してください。\n");
                break;
            case "COMMAND_INJECTION":
                suggestion.append("  1. コマンド実行時の入力値検証を確認してください。\n");
                suggestion.append("  2. システムコマンドの実行を避けるか、安全な方法で実行しているか確認してください。\n");
                suggestion.append("  3. 入力値のエスケープ処理を確認してください。\n");
                break;
            case "PATH_TRAVERSAL":
                suggestion.append("  1. ファイルパスの検証ロジックを確認してください。\n");
                suggestion.append("  2. 相対パスの使用を制限しているか確認してください。\n");
                suggestion.append("  3. アップロードファイルの保存場所を確認してください。\n");
                break;
            case "FILE_UPLOAD_SECURITY":
                suggestion.append("  1. アップロードファイルの拡張子制限を確認してください。\n");
                suggestion.append("  2. ファイルコンテンツの検証を確認してください。\n");
                suggestion.append("  3. アップロードファイルの保存場所とアクセス制限を確認してください。\n");
                break;
            default:
                suggestion.append("  1. エラーメッセージを確認し、失敗の原因を特定してください。\n");
                suggestion.append("  2. 関連するセキュリティフィルターや検証ロジックを確認してください。\n");
                suggestion.append("  3. 必要に応じてセキュリティ設定を調整してください。\n");
                break;
        }
        
        suggestion.append("  4. 再テストを実行して修正を検証してください。\n");
        
        return suggestion.toString();
    }
}
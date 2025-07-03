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

## GameTestの実行
```bash
./gradlew runGameTestServer      # GameTestサーバーを実行してテストを自動実行
```

## GameTestHelper 使用ガイド

### 基本的なテスト構造
```kotlin
@GameTestHolder(RefinedMonitorsMod.MODID)
@PrefixGameTestTemplate(false)
object MyGameTest {
    @GameTest(template = "empty")
    @JvmStatic
    fun testSomething(helper: GameTestHelper) {
        // テストロジック
        helper.succeed()
    }
}
```

### ブロック操作
```kotlin
// ブロック設置
helper.setBlock(BlockPos(1, 1, 1), Blocks.STONE)
helper.setBlock(1, 2, 1, MyMod.MY_BLOCK.get())

// ブロック状態取得
val state = helper.getBlockState(BlockPos(1, 1, 1))

// ブロック破壊
helper.destroyBlock(BlockPos(1, 1, 1))

// ボタン・レバー操作
helper.pressButton(BlockPos(1, 1, 1))
helper.pullLever(BlockPos(1, 1, 1))

// ブロック使用（右クリック）
helper.useBlock(BlockPos(1, 1, 1))
helper.useBlock(pos, player)
```

### BlockEntity操作
```kotlin
// BlockEntity取得（型安全）
val blockEntity = helper.getBlockEntity<MyBlockEntity>(BlockPos(1, 1, 1))

// BlockEntityデータ検証
helper.assertBlockEntityData<MyBlockEntity>(pos, { be -> be.getValue() > 0 }, 
    { "Expected value > 0" })
```

### エンティティ操作
```kotlin
// エンティティ生成
val entity = helper.spawn(EntityType.PIG, BlockPos(1, 1, 1))
val entity2 = helper.spawn(EntityType.ZOMBIE, Vec3(1.5, 1.0, 1.5))

// AIなしでエンティティ生成
val mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, BlockPos(1, 1, 1))

// アイテムエンティティ生成
val itemEntity = helper.spawnItem(Items.DIAMOND, BlockPos(1, 1, 1))

// エンティティ検索
val pig = helper.findOneEntity(EntityType.PIG)
val nearestZombie = helper.findClosestEntity(EntityType.ZOMBIE, 1, 1, 1, 5.0)
val allPigs = helper.findEntities(EntityType.PIG, Vec3(1.0, 1.0, 1.0), 10.0)

// エンティティ削除
helper.killAllEntities()
helper.killAllEntitiesOfClass(Monster::class.java)
```

### アサーション (検証)
```kotlin
// 基本的なアサーション
helper.assertTrue(condition, "Error message")
helper.assertFalse(condition, "Error message")
helper.assertValueEqual(actual, expected, "value name")

// ブロック検証
helper.assertBlockPresent(Blocks.STONE, BlockPos(1, 1, 1))
helper.assertBlockNotPresent(Blocks.AIR, pos)
helper.assertBlockProperty(pos, BlockStateProperties.POWERED, true)

// エンティティ検証
helper.assertEntityPresent(EntityType.PIG)
helper.assertEntityPresent(EntityType.ZOMBIE, pos)
helper.assertEntityNotPresent(EntityType.CREEPER, pos)
helper.assertEntitiesPresent(EntityType.PIG, 3) // 正確に3匹

// アイテム検証
helper.assertItemEntityPresent(Items.DIAMOND, pos, 1.0)
helper.assertItemEntityCountIs(Items.DIAMOND, pos, 1.0, 5)
helper.assertContainerContains(pos, Items.DIAMOND)
helper.assertContainerEmpty(pos)
```

### タイミング制御
```kotlin
// 即座に成功
helper.succeed()

// 条件が満たされるまで待機してから成功
helper.succeedWhen { helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, pos) }

// 特定ティックで条件チェック
helper.succeedOnTickWhen(20) { 
    helper.assertEntityPresent(EntityType.PIG) 
}

// 遅延実行
helper.runAfterDelay(10) { 
    helper.setBlock(pos, Blocks.TNT) 
}

// 特定ティックで実行
helper.runAtTickTime(100) { 
    helper.pressButton(pos) 
}

// 失敗条件設定
helper.failIf { helper.assertEntityPresent(EntityType.CREEPER) }

// シーケンス作成
helper.startSequence()
    .thenExecute { helper.setBlock(pos, Blocks.TNT) }
    .thenWaitUntil(10) { helper.assertBlockPresent(Blocks.AIR, pos) }
    .thenSucceed()
```

### ゲーム環境操作
```kotlin
// 時間設定
helper.setNight()
helper.setDayTime(6000)

// レッドストーン信号
helper.pulseRedstone(pos, 20) // 20ティック後にOFF

// ランダムティック
helper.randomTick(pos)

// 降水
helper.tickPrecipitation(pos)
helper.tickPrecipitation() // テスト範囲全体

// バイオーム設定
helper.setBiome(Biomes.DESERT)
```

### 座標系
```kotlin
// 相対座標 → 絶対座標
val absolutePos = helper.absolutePos(BlockPos(1, 1, 1))
val absoluteVec = helper.absoluteVec(Vec3(1.5, 1.0, 1.5))

// 絶対座標 → 相対座標
val relativePos = helper.relativePos(absolutePos)
val relativeVec = helper.relativeVec(absoluteVec)

// テスト境界取得
val bounds = helper.getBounds()
val tick = helper.getTick()
```

### モックプレイヤー
```kotlin
// プレイヤー作成
val player = helper.makeMockPlayer(GameType.CREATIVE)
val player2 = helper.makeMockPlayer(GameType.SURVIVAL)

// プレイヤーでブロック使用
helper.useBlock(pos, player)

// アイテム配置
helper.placeAt(player, ItemStack(Items.TORCH), pos, Direction.UP)
```

### エラーハンドリング
```kotlin
// 手動でテスト失敗
helper.fail("Something went wrong")
helper.fail("Block not found", pos)
helper.fail("Entity issue", entity)

// エラーメッセージ付きアサーション
helper.assertBlock(pos, { block -> block == Blocks.STONE }, "Expected stone block")
```

### 使用例
```kotlin
@GameTest(template = "empty")
@JvmStatic
fun testMyBlock(helper: GameTestHelper) {
    val pos = BlockPos(1, 1, 1)
    
    // ブロック設置
    helper.setBlock(pos, MyMod.MY_BLOCK.get())
    
    // BlockEntity取得・検証
    val blockEntity = helper.getBlockEntity<MyBlockEntity>(pos)
    helper.assertTrue(blockEntity != null, "BlockEntity should exist")
    
    // 初期状態確認
    helper.assertTrue(blockEntity.getValue() == 0, "Initial value should be 0")
    
    // 操作後の確認
    blockEntity.setValue(42)
    helper.assertValueEqual(blockEntity.getValue(), 42, "block entity value")
    
    helper.succeed()
}
```
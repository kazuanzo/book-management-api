# book-management-api

書籍と著者を管理する REST API です。書籍・著者の登録／更新、著者に紐づく書籍の取得ができます。

## 技術スタック

| 分類 | 採用技術 |
|------|----------|
| 言語 | Kotlin 2.2.20 |
| フレームワーク | Spring Boot 4.1.0 |
| DB アクセス | jOOQ 3.21.5（KotlinGenerator でコード自動生成） |
| マイグレーション | Flyway |
| DB | PostgreSQL |
| ビルド | Gradle 9.5.1 / JDK 21 |
| Lint | ktlint |
| テスト | JUnit 5 / Mockito |

## 機能

- 書籍・著者を RDB に登録／更新する
- 著者に紐づく書籍を取得する
- バリデーション
  - **書籍**: タイトル必須 / 価格は 0 以上 / 著者は最低 1 人（複数可） / 出版状況は「未出版」か「出版済み」
  - **著者**: 名前必須 / 生年月日は現在日以前
- 業務ルール
  - 出版状況「出版済み → 未出版」への変更は不可
  - 書籍に指定された著者が存在しない場合はエラー

## 動かし方

### 前提

- **JDK 21**（Gradle のツールチェーン機能で自動取得されるため手動インストールは必須ではありません）
- **Docker** が起動していること（PostgreSQL をコンテナで起動します）

### 起動

```bash
./gradlew bootRun
```

`spring-boot-docker-compose` 連携により、`bootRun` 実行時に `compose.yaml` の PostgreSQL コンテナが自動で起動します。起動後、Flyway がマイグレーション（テーブル作成）を自動実行します。

アプリは `http://localhost:8080` で待ち受けます。

> **補足**: DB を手動で起動したい場合は `docker compose up -d` を先に実行してください。
> 接続情報は `compose.yaml` に定義されています（DB 名 `mydatabase` / ユーザー `myuser` / パスワード `secret` / ポート `5432`）。

### ビルド・テスト・Lint

```bash
./gradlew build        # jOOQ コード生成 → コンパイル → Lint → テスト まで一括実行
./gradlew test         # テストのみ
./gradlew ktlintCheck  # Lint チェックのみ
./gradlew ktlintFormat # Lint 自動整形
```

> **初回 / IDE で開くとき**: jOOQ の生成コードは `build/generated-src` に出力されます（Git 管理外）。
> `./gradlew build` を一度実行すると生成され、IDE 上でも補完・定義ジャンプが効くようになります。
> （`compileKotlin` が `jooqCodegen` に依存しているため、ビルドすれば自動で生成されます）

## API 仕様

ベース URL: `http://localhost:8080`
リクエスト／レスポンスはすべて JSON（`Content-Type: application/json`）です。

### エンドポイント一覧

| メソッド | パス | 説明 | 成功時ステータス |
|----------|------|------|------------------|
| POST | `/authors` | 著者を登録 | 201 Created |
| PUT | `/authors/{id}` | 著者を更新 | 200 OK |
| GET | `/authors/{id}/books` | 著者に紐づく書籍一覧を取得 | 200 OK |
| POST | `/books` | 書籍を登録 | 201 Created |
| PUT | `/books/{id}` | 書籍を更新 | 200 OK |

### 著者 API

#### リクエストボディ（登録・更新共通）

```json
{
  "name": "夏目漱石",
  "birthDate": "1867-02-09"
}
```

| フィールド | 型 | 必須 | 制約 |
|------------|----|------|------|
| `name` | string | ✔ | 空文字不可 |
| `birthDate` | string (date) | ✔ | 現在日以前 |

#### レスポンス

```json
{
  "id": 1,
  "name": "夏目漱石",
  "birthDate": "1867-02-09"
}
```

#### 例

```bash
# 登録
curl -X POST http://localhost:8080/authors \
  -H "Content-Type: application/json" \
  -d '{"name":"夏目漱石","birthDate":"1867-02-09"}'

# 更新
curl -X PUT http://localhost:8080/authors/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"夏目 漱石","birthDate":"1867-02-09"}'

# 著者に紐づく書籍を取得
curl http://localhost:8080/authors/1/books
```

### 書籍 API

#### リクエストボディ（登録・更新共通）

```json
{
  "title": "吾輩は猫である",
  "price": 800,
  "publicationStatus": "PUBLISHED",
  "authorIds": [1]
}
```

| フィールド | 型 | 必須 | 制約 |
|------------|----|------|------|
| `title` | string | ✔ | 空文字不可 |
| `price` | integer | ✔ | 0 以上 |
| `publicationStatus` | string | ✔ | `UNPUBLISHED` または `PUBLISHED` |
| `authorIds` | array(integer) | ✔ | 1 件以上。実在する著者 ID |

#### レスポンス

```json
{
  "id": 1,
  "title": "吾輩は猫である",
  "price": 800,
  "publicationStatus": "PUBLISHED",
  "authorIds": [1]
}
```

#### 例

```bash
# 登録
curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{"title":"吾輩は猫である","price":800,"publicationStatus":"UNPUBLISHED","authorIds":[1]}'

# 更新（出版済みへ変更）
curl -X PUT http://localhost:8080/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"吾輩は猫である","price":800,"publicationStatus":"PUBLISHED","authorIds":[1]}'
```

### 出版状況の遷移ルール

| 変更前 → 変更後 | 可否 |
|------------------|------|
| 未出版 → 出版済み | ✔ 可 |
| 出版済み → 未出版 | ✗ 不可（409 Conflict） |
| 同じ状態のまま | ✔ 可 |

### エラーレスポンス

エラー時は以下の形式で返ります。

```json
{
  "message": "Validation failed",
  "errors": {
    "price": "must be greater than or equal to 0"
  }
}
```

| ステータス | 発生条件 |
|------------|----------|
| 400 Bad Request | バリデーション違反、リクエストボディの形式不正・必須項目の欠落（JSON 不正・不明な出版状況など）、または指定した著者が存在しない |
| 404 Not Found | 更新対象の著者・書籍が存在しない |
| 409 Conflict | 出版済み → 未出版への変更 |

> `errors` フィールドはバリデーション違反時のみ含まれ、フィールド名ごとのエラー内容を表します。

## アーキテクチャ

レイヤードアーキテクチャを採用しています。

```
Controller  … HTTP の入出力、リクエストの形式バリデーション
   ↓
Service     … 業務ルール（出版状況の遷移チェック、著者存在チェック）、トランザクション境界
   ↓
Repository  … jOOQ の DSLContext で DB アクセス
```

### ディレクトリ構成（`src/main/kotlin/com/example/bookmanagement`）

| パッケージ | 内容 |
|------------|------|
| `domain/` | ドメインモデル（`Book`, `PublicationStatus`） |
| `author/` | 著者の Controller / Service / Repository / DTO |
| `book/` | 書籍の Controller / Service / Repository / DTO |
| `common/` | 例外定義、グローバル例外ハンドラ |

## データベース設計

マイグレーションは `src/main/resources/db/migration/` に配置しています。

| テーブル | 内容 |
|----------|------|
| `authors` | 著者（`name`, `birth_date`） |
| `books` | 書籍（`title`, `price`, `publication_status`） |
| `book_authors` | 書籍と著者の多対多を表す中間テーブル |

- `books.price >= 0` と `publication_status IN ('UNPUBLISHED', 'PUBLISHED')` は **DB の CHECK 制約**で担保
- 「著者は最低 1 人」「生年月日は現在日以前」「出版状況の遷移制約」は **アプリケーション層**で担保

## 設計上の判断

- **バリデーションの分担**: 形式的な検証（必須・範囲など）は DTO の Bean Validation、業務ルール（状態遷移・関連の存在）は Service 層に置いて責務を分離。
- **jOOQ の KotlinGenerator**: プロジェクト言語に合わせ、生成コードも Kotlin で統一。生成コードは `build/` 配下のビルド成果物として扱い、Git 管理対象外（マイグレーション SQL から再生成可能）。
- **コード生成方式**: Flyway のマイグレーション SQL を直接パースして生成（`DDLDatabase`）。コード生成時に稼働中の DB を必要としません。

# Delete Mod 🗑️

`/delete <対象>` コマンドを追加するFabric MODです。

## ⚡ JARの入手方法（3ステップ）

### 1. GitHubにアップロード
1. [github.com](https://github.com) でアカウント作成（無料）
2. 右上の「+」→「New repository」でリポジトリ作成
3. このフォルダの中身を全部アップロード

### 2. 自動ビルドを待つ
- アップロードしたら「Actions」タブを開く
- 黄色いぐるぐる → 緑のチェックになるまで待つ（約3〜5分）

### 3. JARをダウンロード
- 緑チェックをクリック → 「delete-mod-jar」をダウンロード
- ZIPを解凍すると `delete-mod-1.0.0.jar` が出てくる！

---

## 🎮 MODの入れ方

1. [fabricmc.net](https://fabricmc.net) からFabric Installerをダウンロード・実行
2. [modrinth.com](https://modrinth.com) で「Fabric API」を検索してダウンロード
3. `.minecraft/mods/` フォルダに以下を入れる：
   - `fabric-api-xxxx.jar`
   - `delete-mod-1.0.0.jar`
4. ランチャーで **fabric** プロファイルを選んで起動！

---

## 🎯 使い方

```
/delete @e[type=zombie,limit=1]   ← ゾンビ1体に警告
/delete @a                         ← 全プレイヤーに警告
/delete @e[distance=..10]          ← 近くの全エンティティ
```

**OP権限（/op 自分の名前）が必要です！**

---

## 動作内容
1. 対象に `enchanted_hit` パーティクルが出続ける
2. プレイヤーなら `⚠ WARNING ⚠` が画面に点滅
3. **10秒後** Y座標が `-1024` に飛ばされる（ボイドで即死）

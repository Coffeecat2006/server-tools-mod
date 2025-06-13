# server tools mod  ![Mod Icon](src/main/resources/assets/server_tools_mod/icon.png)

**Version:** 1.0.3  
**Minecraft:** 1.21.5  
**Fabric Loader:** 1.21.5  
**Fabric API:** 0.88.0+  
**Java:** 17+

一款 Fabric 伺服器模組，讓管理員可動態建立、管理「禮包碼」，玩家輸入後可領取自訂物品、訊息，並自動執行伺服器指令。支援完整郵件系統，方便玩家互相寄送信件與物品。

>  **English version**：[en_README.md](en_README.md)**

---

## 目錄

- [功能總覽](#功能總覽)  
- [安裝](#安裝)  
- [禮包碼指令用法](#禮包碼指令用法)  
  - [新增/移除/列表](#新增移除列表)  
  - [兌換](#兌換)  
  - [預覽](#預覽)  
  - [修改現有 Code](#修改現有-code)  
    - [物品](#物品)  
    - [欄位](#欄位)  
    - [事件](#事件)  
  - [查閱日誌](#查閱日誌)
  - [幫助](#幫助)
- [信箱指令用法](#信箱指令用法)  
  - [開啟信箱](#開啟信箱)  
  - [寄送信件](#寄送信件)  
  - [查看信件](#查看信件)  
  - [領取包裹](#領取包裹)  
  - [刪除信件](#刪除信件)  
  - [批量刪除](#批量刪除)  
  - [黑名單](#黑名單)  
  - [日誌查詢](#日誌查詢)  
  - [幫助](#幫助)  
- [授權](#授權)  
- [持久化存檔](#持久化存檔)  

---

## 功能總覽

- **動態禮包碼管理**：可隨時建立、刪除、列出、查詢禮包碼
- **自訂回饋內容**：支援自訂物品列表、文字訊息
- **進階修改功能**：可動態調整禮包碼內容，包括物品、訊息、領取上限、有效時間、單／多次領取規則等
- **隱藏／顯示禮包碼**：可設定禮包碼為隱藏，玩家輸入隱藏碼時會顯示「無此禮包碼」
- **自訂事件觸發**：領取時可執行一或多個伺服器指令（支援其他模組指令），指令目標可用 `@s` 代表領取玩家
- **領取與編輯日誌**：完整記錄所有禮包碼的領取與編輯操作，方便查詢
- **信箱系統**：支援玩家間寄送信件與物品，含黑名單、批量刪除、日誌查詢等功能
- **自動持久化存檔**：所有資料自動儲存於世界存檔，伺服器重啟後自動載入
- **MIT 授權**：開源、可自由修改與分發 

---

## 安裝

1. 把 `server_tools_mod-1.0.3.jar` 放到 `mods/` 資料夾（需同時放 Fabric API）  
2. 啟動伺服器或客戶端(客戶端非必要安裝，僅伺服器端需要)  

---

## 禮包碼指令用法

### 新增 / 移除 / 列表

```shell
# 新增一組 Code
/redeem add <code> "<message>" <limit> <time> <singleUse>
# 例如：只限領一次、24 小時過期
/redeem add VIP123 "Welcome, VIP!" 1 1440 true

# 刪除 Code
/redeem remove <code>

# 列出所有 Code（管理員專用）
/redeem list
```

### 兌換

```shell
# 玩家輸入 Code
/redeem <code>
```

### 預覽（管理員專用）

```shell
# 預覽物品
/redeem preview item <code>

# 預覽文字
/redeem preview text <code>

# 預覽事件
/redeem preview event <code> all

/redeem preview event <code> <eventName>
```

### 修改現有 Code
管理員可用 `/redeem modify` 指令動態調整已存在的 Code。

```shell
/redeem modify <code> <field> [args...]
```

#### 物品
- `item reset`：清空該 Code 的物品列表  
- `item transform`：將物品列表替換為管理員當前副手持物  
- `item add`：在現有列表後追加管理員當前副手持物  

#### 欄位
- `code <newCode>`：重新命名 Code  
- `text "<newMessage>"`：更新領取後顯示的文字  
- `limit <number|infinity>`：設定總領取次數上限（-1 表示無限）  
- `time <minutes|infinity>`：設定過期時間（分），或 `infinity`  
- `rules <true|false>`：單／多次領取規則（`true` = 單次每人）  
- `receive_status <playerUUID> <true|false>`：手動標記某玩家是否已領取  
- `available <true|false>`：顯示（`true`）或隱藏（`false`）此 Code  

#### 事件
- `event add <eventName> <command…>`：新增一個領取時執行的伺服器指令，`@s` 解析為領取玩家  
  例： `/redeem modify VIP123 event add greet title @s title "歡迎 VIP"`  
- `event remove <eventName>`：移除指定事件  
- `event reset`：清空所有事件  

### 查閱日誌
```shell
# 顯示所有日誌
/redeem log all [recent <n>] [page <p>]

# 查詢指定 Code 的編輯紀錄
/redeem log code <code> edits [recent <n>] [page <p>]

# 查詢指定 Code 的領取紀錄
/redeem log code <code> redeems [recent <n>] [page <p>]

# 查詢指定玩家的所有操作紀錄
/redeem log player <player> [recent <n>] [page <p>]
```

### 幫助
```shell
# 顯示所有 /redeem 指令概述
/redeem help

# 顯示指定子指令詳細說明
/redeem help <子指令>
```

## 信箱指令用法

### 開啟信箱
```shell
/mail open [<頁碼>]
```
- 列出當前玩家信箱，每頁最多 5 封信
- 若不輸入頁碼則預設為1

### 寄送信件
```shell
/mail send <player> <title> <content> <item>
```
- `player`：若為管理員可輸入 `@a` 或 `all`，否則僅單一玩家 ID
- `title`：信件標題
- `content`：內文，可用 `
` 換行
- `item`：`true` 打包副手物品、`false` 無包裹

### 查看信件
```shell
/mail read <id>
```
- 顯示指定 ID 信件內容，並標記為已閱讀

### 領取包裹
```shell
/mail pickup <id>
```
- 領取信件中包裹物品

### 刪除信件
```shell
/mail delete <id>
```
- 命令後會出現 `[確認] [取消]` 按鈕，再次輸入 `/mail delete <id> confirm` 或 `cancel`

### 批量刪除
```shell
/mail delete all [read|received]
```
- `all`：刪除所有
- `read`：刪除所有已閱讀
- `received`：刪除所有已領包裹
- 同樣需後續 `confirm` 或 `cancel`

### 黑名單
```shell
/mail blacklist
/mail blacklist add <player>
/mail blacklist remove <player>
```
- `add`/`remove` 操作皆需 `confirm`

### 日誌查詢（管理員）
```shell
/mail log [<page>]
```
- 分頁顯示所有寄送紀錄，格式：`[時間] sender -> recipient <id> [查看]`

### 幫助
```shell
/mail help
```
- 顯示所有 `/mail` 指令概述


---

## 授權

本模組採 MIT License，詳見 LICENSE 文件。

---

## 持久化存檔

- 禮包碼資料：`world/data/redeemmod_codes.dat`
- 信箱資料：`world/data/mailmod_data.dat`
- 自動儲存：資料變更時即時儲存
- 自動載入：伺服器啟動時自動讀取
>  **完整資料文件請見：[DOCS.md](DOCS.md)**


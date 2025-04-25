# RedeemMod

**Version:** 1.0.0  
**Minecraft:** 1.21.5  
**Fabric Loader:** 1.21.5  
**Fabric API:** 0.88.0+  
**Java:** 17+

一款 Fabric 模組，讓伺服器管理員能輕鬆定義「禮包碼」，玩家輸入後可領取自訂物品、文字，並觸發伺服器指令事件。所有資料檔自動持久化到世界存檔。

---

## 目錄

- [功能總覽](#功能總覽)  
- [安裝](#安裝)  
- [指令用法](#指令用法)  
  - [新增/移除/列表](#新增移除列表)  
  - [兌換](#兌換)  
  - [預覽](#預覽)  
  - [修改現有 Code](#修改現有-code)  
    - [物品](#物品)  
    - [欄位](#欄位)  
    - [事件](#事件)  
- [授權](#授權)  

---

## 功能總覽

- **動態禮包碼管理**：建立、刪除、列出、使用  
- **自訂回饋**：物品列表、文字訊息  
- **進階修改**：隨時變更 Code、文字、上限、時間、單／多次領取規則  
- **隱藏/顯示**：可隱藏 Code，嘗試使用會顯示「無此禮包碼」  
- **自訂事件**：領取時執行伺服器指令（含其他模組指令），目標為領取玩家 `@s`  
- **持久化存檔**：自動儲存到 `data/redeemmod_codes.dat`，重啟自動載入  

---

## 安裝

1. 把 `RedeemMod-1.0.0.jar` 放到 `mods/` 資料夾（需同時放 Fabric API）  
2. 啟動伺服器或客戶端  

---

## 指令用法

### 新增 / 移除 / 列表

# 新增一組 Code
/redeem add <code> "<message>" <limit> <time> <singleUse>
# 例如：只限領一次、24 小時過期
/redeem add VIP123 "Welcome, VIP!" 1 1440 true

# 刪除 Code
/redeem remove <code>

# 列出所有 Code（管理員專用）
/redeem list

### 兌換
# 玩家輸入 Code
/redeem <code>

### 預覽（管理員專用）
# 預覽物品
/redeem preview item <code>

# 預覽文字
/redeem preview text <code>

### 修改現有 Code
管理員可用 /redeem modify 指令動態調整已存在的 Code。

/redeem modify <code> <field> [args...]
### 物品
- **item reset**: 清空該 Code 的物品列表  
- **item transform**: 將物品列表替換為管理員當前副手持物  
- **item add**: 在現有列表後追加管理員當前副手持物  

### 欄位
- **code <newCode>**: 重新命名 Code  
- **text "<newMessage>"**: 更新領取後顯示的文字  
- **limit <number\|infinity>**: 設定總領取次數上限（-1 表示無限）  
- **time <minutes\|infinity>**: 設定過期時間（分），或 `infinity`  
- **rules <true\|false>**: 單／多次領取規則（`true` = 單次每人）  
- **receive_status <playerUUID> <true\|false>**: 手動標記某玩家是否已領取  
- **available <true\|false>**: 顯示（`true`）或隱藏（`false`）此 Code  

### 事件
- **event add <eventName> <command…>**  
  新增一個領取時執行的伺服器指令，`@s` 解析為領取玩家  
  例：  

例：/redeem_modify VIP123 event add greet title @s title "歡迎 VIP"

- **event remove <eventName>**: 移除指定事件  
- **event reset**: 清空所有事件  

## 授權
本模組採 MIT License
詳見 LICENSE 文件。
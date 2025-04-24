# Redeem Mod

Redeem Mod 是一個用於 Minecraft 的模組，允許伺服器管理員創建和管理禮包碼，玩家可以使用這些禮包碼來兌換獎勵物品或訊息。

## 功能介紹

- **新增禮包碼**：管理員可以創建帶有自定義訊息、使用限制、有效期限和規則的禮包碼。
- **移除禮包碼**：管理員可以刪除不需要的禮包碼。
- **列出禮包碼**：查看所有可用的禮包碼及其詳細資訊。
- **預覽禮包碼**：檢視禮包碼的獎勵內容或訊息。
- **兌換禮包碼**：玩家可以使用禮包碼來獲取獎勵。

## 指令教學

### `/redeem add`
新增一個禮包碼。

**語法**：
/redeem add <code> <text> <limit> <time> <rules>

**參數**：
- `code`：禮包碼名稱（如 `gift123`）。
- `text`：兌換後顯示的訊息。
- `limit`：使用次數限制（數字或 `infinity`）。
- `time`：有效期限（分鐘數或 `infinity`）。
- `rules`：是否啟用單次使用規則（`true` 或 `false`）。

**範例**：
/redeem add gift123 "感謝您的支持！" 10 1440 true


### `/redeem remove`
移除一個禮包碼。

**語法**：
/redeem remove <code>

**參數**：
- `code`：要移除的禮包碼名稱。

**範例**：
/redeem remove gift123


### `/redeem list`
列出所有禮包碼。

**語法**：
/redeem list

**範例**：
/redeem list

### `/redeem preview`
預覽禮包碼的內容。

**語法**：
/redeem preview item <code>
/redeem preview text <code>

**參數**：
- `code`：要預覽的禮包碼名稱。

**範例**：
/redeem preview item gift123
/redeem preview text gift123

### `/redeem`
兌換禮包碼。

**語法**：
/redeem <code>

**參數**：
- `code`：要兌換的禮包碼名稱。

**範例**：
/redeem gift123


## 安裝教學

1. 確保您已安裝 [Fabric](https://fabricmc.net/) 和 [Fabric API](https://modrinth.com/mod/fabric-api)。
2. 將模組檔案放入 `mods` 資料夾。
3. 啟動伺服器或客戶端，模組將自動加載。

## 開發者資訊

- **作者**：CoffeeCat2006
- **版本**：1.21.5
- **授權**：MIT License

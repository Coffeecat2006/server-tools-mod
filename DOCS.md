# Server Tools Mod 詳細文件

## 資料儲存

### 檔案位置
- 禮包碼系統：`world/data/redeemmod_codes.dat`
  - 儲存所有禮包碼設定
  - 包含使用紀錄與操作日誌
  
- 信箱系統：`world/data/mailmod_data.dat`
  - 儲存所有信件資料
  - 黑名單設定
  - 收件箱對應關係
  - 玩家資訊快取
  - 操作日誌

### 自動保存機制
- 即時保存：任何資料變更都會觸發存檔
- 安全保存：使用 Minecraft 原生 NBT 格式
- 自動備份：與世界存檔同步備份

### 資料結構
#### RedeemState（禮包碼）
```json
{
  "codes": {
    "<code>": {
      "items": [...],
      "message": "string",
      "limit": number,
      "expiryEpoch": number,
      "singleUse": boolean,
      "available": boolean,
      "redeemedCount": number,
      "usedPlayers": ["uuid", ...],
      "events": {
        "<eventName>": "command"
      }
    }
  },
  "logs": [...]
}
```

#### MailState（信箱）
```json
{
  "mails": {
    "<mailId>": {
      "sender": "string",
      "recipient": "string",
      "title": "string",
      "content": "string",
      "timestamp": number,
      "hasItem": boolean,
      "isRead": boolean,
      "isPickedUp": boolean,
      "packageItem": {...}
    }
  },
  "byRecipient": {
    "<player>": ["mailId", ...]
  },
  "blacklist": ["player", ...],
  "logs": [...],
  "knownPlayersUuids": ["uuid", ...],
  "knownPlayerNames": {
    "<uuid>": "name"
  },
  "playerLastLoginTimestamps": {
    "<uuid>": number
  }
}
```

> 💡 所有資料使用 Minecraft 的 DataFixer 系統確保版本相容性

[返回 README](README.md)

package com.coffeecat2006.mail;

import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.datafixer.DataFixTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import java.util.*;

public class MailState extends PersistentState {
    private final Map<String, Mail> mails = new LinkedHashMap<>();
    private final Map<String, List<String>> byRecipient = new HashMap<>();
    private final HashSet<String> blacklist = new HashSet<>();
    private final List<LogEntry> logs = new ArrayList<>();
    private final Set<UUID> knownPlayersUuids = new HashSet<>();
    private final Map<UUID, String> knownPlayerNames = new HashMap<>();
    private final Map<UUID, Long> playerLastLoginTimestamps = new HashMap<>();

    public static final Codec<LogEntry> LOG_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.LONG.fieldOf("timestamp").forGetter((MailState.LogEntry le) -> le.timestamp),
        Codec.STRING.optionalFieldOf("sender", "").forGetter((MailState.LogEntry le) -> le.sender),
        Codec.STRING.optionalFieldOf("recipient", "").forGetter((MailState.LogEntry le) -> le.recipient),
        Codec.STRING.optionalFieldOf("mailId", "").forGetter((MailState.LogEntry le) -> le.mailId),
        Codec.STRING.optionalFieldOf("action", "SEND").forGetter((MailState.LogEntry le) -> le.action),
        Codec.STRING.optionalFieldOf("details", "").forGetter((MailState.LogEntry le) -> le.details)
    ).apply(inst, LogEntry::new));

    public static final Codec<Mail> MAIL_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.STRING.optionalFieldOf("id", "").forGetter((MailState.Mail m) -> m.id),
        Codec.STRING.optionalFieldOf("sender", "").forGetter((MailState.Mail m) -> m.sender),
        Codec.STRING.optionalFieldOf("recipient", "").forGetter((MailState.Mail m) -> m.recipient),
        Codec.STRING.optionalFieldOf("title", "").forGetter((MailState.Mail m) -> m.title),
        Codec.STRING.optionalFieldOf("content", "").forGetter((MailState.Mail m) -> m.content),
        Codec.LONG.fieldOf("timestamp").forGetter((MailState.Mail m) -> m.timestamp),
        Codec.BOOL.fieldOf("hasItem").forGetter((MailState.Mail m) -> m.hasItem),
        Codec.BOOL.fieldOf("isRead").forGetter((MailState.Mail m) -> m.isRead),
        Codec.BOOL.fieldOf("isPickedUp").forGetter((MailState.Mail m) -> m.isPickedUp),
        ItemStack.CODEC.optionalFieldOf("packageItem", ItemStack.EMPTY).forGetter((MailState.Mail m) -> m.packageItem)
    ).apply(inst, (id, sender, recipient, title, content, timestamp, hasItem, isRead, isPickedUp, packageItem) -> {
        Mail mail = new Mail(id, sender, recipient, title, content, timestamp, hasItem, isRead, isPickedUp);
        mail.packageItem = packageItem;
        return mail;
    }));

    public static final Codec<MailState> STATE_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.unboundedMap(Codec.STRING, MAIL_CODEC).fieldOf("mails").forGetter((MailState ms) -> ms.getMails()),
        Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING)).fieldOf("byRecipient").forGetter((MailState ms) -> ms.getByRecipient()),
        Codec.STRING.listOf()
            .xmap(
                (List<String> list) -> new HashSet<>(list),
                (Set<String> set)   -> new ArrayList<>(set)
            )
            .fieldOf("blacklist")
            .forGetter((MailState ms) -> ms.getBlacklist()),
        LOG_CODEC.listOf().optionalFieldOf("logs", Collections.emptyList()).forGetter((MailState ms) -> ms.getLogs()),
        Codec.list(Codec.STRING.xmap(UUID::fromString, UUID::toString))
            .xmap(
                (List<UUID> list) -> new HashSet<>(list),      // For deserialization (List -> Set)
                (Set<UUID> set) -> new ArrayList<>(set)        // For serialization (Set -> List)
            )
            .optionalFieldOf("known_players_uuids", new HashSet<UUID>()) // Use new HashSet<UUID>() for default
            .forGetter((MailState ms) -> ms.getKnownPlayersUuids()), // Provide the Set for serialization
        Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Codec.STRING)
            .optionalFieldOf("known_player_names", new HashMap<>()) // Default to empty map
            .forGetter((MailState ms) -> ms.getKnownPlayerNames()), // Explicit lambda
        Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Codec.LONG)
            .optionalFieldOf("player_last_login_timestamps", new HashMap<>()) // Default to empty map
            .forGetter((MailState ms) -> ms.getPlayerLastLoginTimestamps()) // Explicit lambda
    ).apply(inst, (mails, byRec, bl, logs, knownUuids, knownNames, lastLogins) -> new MailState(mails, byRec, bl, logs, knownUuids, knownNames, lastLogins)));

    public static final PersistentStateType<MailState> TYPE = new PersistentStateType<>(
        "mailmod_data",
        MailState::new, // No-arg constructor for initial creation
        STATE_CODEC,    // Codec for loading/saving
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public MailState() { super(); } // Used by PersistentStateType for initial creation

    // Constructor for deserialization by STATE_CODEC
    private MailState(Map<String,Mail> mailsData,
                      Map<String,List<String>> byRecipientData,
                      HashSet<String> blacklistData,
                      List<LogEntry> logsData,
                      Set<UUID> knownPlayersUuidsData,
                      Map<UUID, String> knownPlayerNamesData,
                      Map<UUID, Long> playerLastLoginTimestampsData) {
        super();
        this.mails.putAll(mailsData);
        byRecipientData.forEach((key, value) -> this.byRecipient.put(key, new ArrayList<>(value)));
        this.blacklist.addAll(blacklistData);
        this.logs.addAll(logsData);

        if (knownPlayersUuidsData != null) {
            this.knownPlayersUuids.addAll(knownPlayersUuidsData);
        }
        if (knownPlayerNamesData != null) {
            this.knownPlayerNames.putAll(knownPlayerNamesData);
        }
        if (playerLastLoginTimestampsData != null) {
            this.playerLastLoginTimestamps.putAll(playerLastLoginTimestampsData);
        }
    }

    public Map<String, Mail> getMails() { return mails; }
    public Map<String, List<String>> getByRecipient() { return byRecipient; }
    public HashSet<String> getBlacklist() { return blacklist; }
    public List<LogEntry> getLogs() { return logs; }
    public Set<UUID> getKnownPlayersUuids() { return knownPlayersUuids; }
    public Map<UUID, String> getKnownPlayerNames() { return knownPlayerNames; }
    public Map<UUID, Long> getPlayerLastLoginTimestamps() { return playerLastLoginTimestamps; }

    public static class Mail {
        public String id, sender, recipient, title, content;
        public long timestamp;
        public boolean hasItem, isRead, isPickedUp;
        public ItemStack packageItem;
        public Mail() {
            this.id = "";
            this.sender = "";
            this.recipient = "";
            this.title = "";
            this.content = "";
            this.packageItem = ItemStack.EMPTY;
        }
        public Mail(String id, String sender, String recipient, String title, String content, long timestamp,
                    boolean hasItem, boolean isRead, boolean isPickedUp) {
            this.id=id; this.sender=sender; this.recipient=recipient; this.title=title;
            this.content=content; this.timestamp=timestamp; this.hasItem=hasItem;
            this.isRead=isRead; this.isPickedUp=isPickedUp;
        }
    }

    public static class LogEntry {
        public long timestamp;
        public String sender, recipient, mailId;
        public String action;
        public String details;

        public LogEntry() {
            this.sender = "";
            this.recipient = "";
            this.mailId = "";
            this.action = "";
            this.details = "";
        }
        public LogEntry(long timestamp, String sender, String recipient, String mailId, String action, String details) {
            this.timestamp = timestamp;
            this.sender = sender;
            this.recipient = recipient;
            this.mailId = mailId;
            this.action = action;
            this.details = details;
        }
    }
}

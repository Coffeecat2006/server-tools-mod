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
    private final Set<String> blacklist = new HashSet<>();
    private final List<LogEntry> logs = new ArrayList<>();

    public static final Codec<LogEntry> LOG_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.LONG.fieldOf("timestamp").forGetter(le -> le.timestamp),
        Codec.STRING.fieldOf("sender").forGetter(le -> le.sender),
        Codec.STRING.fieldOf("recipient").forGetter(le -> le.recipient),
        Codec.STRING.fieldOf("mailId").forGetter(le -> le.mailId)
    ).apply(inst, LogEntry::new));

    public static final Codec<Mail> MAIL_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.STRING.fieldOf("id").forGetter(m -> m.id),
        Codec.STRING.fieldOf("sender").forGetter(m -> m.sender),
        Codec.STRING.fieldOf("recipient").forGetter(m -> m.recipient),
        Codec.STRING.fieldOf("title").forGetter(m -> m.title),
        Codec.STRING.fieldOf("content").forGetter(m -> m.content),
        Codec.LONG.fieldOf("timestamp").forGetter(m -> m.timestamp),
        Codec.BOOL.fieldOf("hasItem").forGetter(m -> m.hasItem),
        Codec.BOOL.fieldOf("isRead").forGetter(m -> m.isRead),
        Codec.BOOL.fieldOf("isPickedUp").forGetter(m -> m.isPickedUp)
    ).apply(inst, Mail::new));

    public static final Codec<MailState> STATE_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.unboundedMap(Codec.STRING, MAIL_CODEC).fieldOf("mails").forGetter(ms -> ms.mails),
        Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING)).fieldOf("byRecipient").forGetter(ms -> ms.byRecipient),
        Codec.STRING.listOf()
            .xmap(
                (List<String> list) -> new HashSet<>(list),
                (Set<String> set)   -> new ArrayList<>(set)
            )
            .fieldOf("blacklist")
            .forGetter(ms -> ms.blacklist),
        LOG_CODEC.listOf().optionalFieldOf("logs", Collections.emptyList()).forGetter(ms -> ms.logs)
    ).apply(inst, MailState::new));

    public static final PersistentStateType<MailState> TYPE = new PersistentStateType<>(
        "mailmod_data",
        MailState::new,
        STATE_CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public MailState() { super(); }
    private MailState(Map<String, Mail> mails, Map<String, List<String>> byRec, Set<String> bl, List<LogEntry> logs) {
        super(); this.mails.putAll(mails); this.byRecipient.putAll(byRec); this.blacklist.addAll(bl); this.logs.addAll(logs);
    }

    public Map<String, Mail> getMails() { return mails; }
    public Map<String, List<String>> getByRecipient() { return byRecipient; }
    public Set<String> getBlacklist() { return blacklist; }
    public List<LogEntry> getLogs() { return logs; }

    public static class Mail {
        public String id, sender, recipient, title, content;
        public long timestamp;
        public boolean hasItem, isRead, isPickedUp;
        public ItemStack packageItem;
        public Mail() {}
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
        public LogEntry() {}
        public LogEntry(long timestamp, String sender, String recipient, String mailId) {
            this.timestamp=timestamp; this.sender=sender; this.recipient=recipient; this.mailId=mailId;
        }
    }
}

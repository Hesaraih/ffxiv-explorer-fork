package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.helpers.EARandomAccessFile;
import com.fragmenterworks.ffxivextract.helpers.FFXIV_String;
import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.io.IOException;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Log_File {

    private static final int CHANNEL_SAY = 0xA;
    private static final int CHANNEL_SHOUT = 0xB;
    private static final int CHANNEL_TELLOUT = 0xC;
    private static final int CHANNEL_TELLIN = 0xD;
    private static final int CHANNEL_PARTY = 0xE;
    private static final int CHANNEL_ALLIANCE = 0xF;
    private static final int CHANNEL_LS1 = 0x10;
    private static final int CHANNEL_LS2 = 0x11;
    private static final int CHANNEL_LS3 = 0x12;
    private static final int CHANNEL_LS4 = 0x13;
    private static final int CHANNEL_LS5 = 0x14;
    private static final int CHANNEL_LS6 = 0x15;
    private static final int CHANNEL_LS7 = 0x16;
    private static final int CHANNEL_LS8 = 0x17;
    private static final int CHANNEL_FC = 0x18;
    private static final int CHANNEL_EMOTE_CUSTOM = 0x1C;
    private static final int CHANNEL_EMOTE_STANDARD = 0x1D;
    private static final int CHANNEL_YELL = 0x1E;
    private static final int CHANNEL_SYSTEM = 0x39;

    private final Log_Entry[] entries;

    public Log_File(String path) throws IOException {
        EARandomAccessFile file = new EARandomAccessFile(path, "r", ByteOrder.LITTLE_ENDIAN);

        //Read in sizes
        int bodySize = file.readInt();
        int fileSize = file.readInt();

        //Read in size table
        int numOffsets = (fileSize - bodySize);
        int[] offsets = new int[numOffsets];
        entries = new Log_Entry[numOffsets];

        int maxBufferSize = 0;

        for (int i = 0; i < numOffsets; i++) {
            offsets[i] = file.readInt();
            if (i == 0)
                maxBufferSize = offsets[i];
            else {
                final int offset1 = offsets[i] - offsets[i - 1];
                if (offset1 > maxBufferSize)
                    maxBufferSize = offset1;
            }
        }

        //Read in log entries
        byte[] buffer = new byte[maxBufferSize];
        for (int i = 0; i < offsets.length; i++) {
            final int offsetLen = offsets[i] - (i == 0 ? 0 : offsets[i - 1]);
            file.read(buffer, 0, offsetLen);

            //Corrupted?
            if (buffer[0] == 0 && buffer[1] == 0 && buffer[2] == 0 && buffer[3] == 0 && buffer[4] == 0 && buffer[5] == 0 && buffer[6] == 0 && buffer[7] == 0) {
                entries[i] = new Log_Entry(0, -1, -1, "", "Missing Log Entry");
                continue;
            }

            String data = new String(buffer, 0, offsetLen);

            String[] splitData = data.split(":");

            String info = splitData[0];
            String sender = splitData.length >= 2 ? FFXIV_String.parseFFXIVString(splitData[1].getBytes()) : "";

            //Tこれは、メッセージ文字列に：が含まれている可能性があるファックアップ用です
            if (splitData.length > 3) {
                for (int s = 3; s < splitData.length; s++)
                    splitData[2] += splitData[s];
            }

            String message = splitData.length >= 3 ? FFXIV_String.parseFFXIVString(splitData[2].getBytes()) : "";

            long time = Long.parseLong(info.substring(0, 8), 16);
            int filter = Integer.parseInt(info.substring(8, 10), 16);
            int channel = Integer.parseInt(info.substring(10, 12), 16);

            entries[i] = new Log_Entry(time, filter, channel, sender, message);

            Utils.getGlobalLogger().debug("{} : {}", entries[i].sender.isEmpty() ? "" : entries[i].sender, entries[i].message);
        }
        file.close();
    }

    public Log_Entry[] getEntries() {
        return entries;
    }

    public static class Log_Entry {
        final public long time;
        final public int channel;
        final public int filter;
        final public String sender;
        final public String message;
        final public String formattedTime;

        Log_Entry(long time, int filter, int channel, String string, String message) {
            this.time = time;
            this.filter = filter;
            this.channel = channel;
            this.sender = string;
            this.message = message;

            Date date = new Date(time * 1000);
            DateFormat format = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

            formattedTime = format.format(date);
        }

        @Override
        public String toString() {
            return message;
        }
    }

    public static String getChannelName(int channel) {
        switch (channel) {
            case Log_File.CHANNEL_SAY:
                return "Say";
            case Log_File.CHANNEL_SHOUT:
                return "Shout";
            case Log_File.CHANNEL_TELLIN:
                return "Tell (in)";
            case Log_File.CHANNEL_TELLOUT:
                return "Tell (out)";
            case Log_File.CHANNEL_PARTY:
                return "Party";
            case Log_File.CHANNEL_ALLIANCE:
                return "Alliance";
            case Log_File.CHANNEL_LS1:
                return "Linkshell 1";
            case Log_File.CHANNEL_LS2:
                return "Linkshell 2";
            case Log_File.CHANNEL_LS3:
                return "Linkshell 3";
            case Log_File.CHANNEL_LS4:
                return "Linkshell 4";
            case Log_File.CHANNEL_LS5:
                return "Linkshell 5";
            case Log_File.CHANNEL_LS6:
                return "Linkshell 6";
            case Log_File.CHANNEL_LS7:
                return "Linkshell 7";
            case Log_File.CHANNEL_LS8:
                return "Linkshell 8";
            case Log_File.CHANNEL_FC:
                return "Free Company";
            case Log_File.CHANNEL_EMOTE_STANDARD:
                return "Emote";
            case Log_File.CHANNEL_EMOTE_CUSTOM:
                return "Emote (Custom)";
            case Log_File.CHANNEL_YELL:
                return "Yell";
            case Log_File.CHANNEL_SYSTEM:
                return "System";
        }
        return "Undefined";
    }

}

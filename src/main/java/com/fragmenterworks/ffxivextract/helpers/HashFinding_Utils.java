package com.fragmenterworks.ffxivextract.helpers;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.gui.components.EXDF_View;
import com.fragmenterworks.ffxivextract.models.*;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_File;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_Folder;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * ハッシュ検索用ユーティリティ集
 */
public class HashFinding_Utils extends Component {
    private static SqPack_IndexFile bgcommonIndex;
    private static int variant = 1;
    private static boolean IsDebug = false;

    /**
     *  Exhハッシュ検索
     *  game_scriptも追加
     */
    public static void findExhHashes() {
        Utils.getGlobalLogger().info("root.exlを開いています...");

        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        SqPack_IndexFile index2 = SqPack_IndexFile.GetIndexFileForArchiveID("0b0000", true);
        byte[] rootData = index.extractFile("exd/root.exl");

        if (rootData == null) {
            return;
        }

        Utils.getGlobalLogger().info("root.exl読み込み中...");

        try {
            InputStream in = new ByteArrayInputStream(rootData);
            //root.exlはテキストデータであるためBufferedReaderを使用する
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            HashDatabase.beginConnection();
            HashDatabase.setAutoCommit(false);
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                //データが Achievement,209のようにカンマ区切りになっている
                String path;
                String pathID;
                path = line.substring(0, line.lastIndexOf(','));
                pathID = line.substring(line.lastIndexOf(',') + 1);

                //game_scriptのluabファイル登録
                if (Objects.equals(pathID, "-1")){
                    if (path.startsWith("quest/")) {
                        String[] battleText = new String[]{"_","Btl_","Btl2_"};
                        for (String btl: battleText){
                            String path2 = path.replaceFirst("_",btl);
                            String luabName = String.format("game_script/%s.luab", path2);
                            if (index2.extractFile(luabName) != null) {
                                HashDatabase.addPathToDB(luabName, "0b0000",true);
                            }
                        }
                    } else {
                        String luabName = String.format("game_script/%s.luab", path);
                        if (index2.extractFile(luabName) != null) {
                            HashDatabase.addPathToDB(luabName, "0b0000",true);
                        }
                    }
                }

                //noinspection ConstantConditions
                if (false){
                    //exhFileの登録
                    EXHF_File exhFile = null;

                    String exhName = String.format("exd/%s.exh", path);
                    if (index.extractFile(exhName) != null) {
                        HashDatabase.addPathToDB(exhName, "0a0000",true);
                        exhFile = new EXHF_File(index.extractFile(exhName));
                    }

                    //exdFileの登録
                    for (EXHF_File.EXDF_Page exdPage : Objects.requireNonNull(exhFile).getPageTable()){
                        int pageNum = exdPage.pageNum;
                        for (String code : EXHF_File.languageCodes) {
                            //String exdName = String.format("exd/%s_0%s.exd", sBuilder, code);
                            //exdページ対応
                            String exdName = String.format("exd/%s_%s%s.exd", path, pageNum, code);
                            if (index.extractFile(exdName) != null) { //ファイルの存在チェック
                                HashDatabase.addPathToDB(exdName, "0a0000",true);
                            }
                        }
                    }
                }
            }

            HashDatabase.commit();
            in.close();

        } catch (IOException e1) {
            Utils.getGlobalLogger().error(e1);
        }

        Utils.getGlobalLogger().info("EXD/EXHの検索完了");
    }

    /**
     * Musicハッシュ検索
     */
    public static void findMusicHashes() {
        byte[] exhData;
        SqPack_IndexFile index;
        EXDF_View viewer;

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        Utils.getGlobalLogger().info("BGM.exhを開いています...");

        exhData = index.extractFile("exd/BGM.exh");
        viewer = new EXDF_View(index, "exd/BGM.exh", new EXHF_File(exhData));

        Utils.getGlobalLogger().info("BGM.exhを読み込みました");

        for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
            String path = String.format("%s", viewer.getTable().getValueAt(i, 1));

            if (path == null || path.isEmpty()) {
                continue;
            }

            //蒼天までしか対応していなかったので変更
            String archive = HashDatabase.getArchiveID(path);

            HashDatabase.addPathToDB(path, archive, true);
        }

        //OrchestrionPath.exh
        exhData = index.extractFile("exd/OrchestrionPath.exh");
        viewer = new EXDF_View(index, "exd/OrchestrionPath.exh", new EXHF_File(exhData));

        Utils.getGlobalLogger().info("OrchestrionPath.exhを読み込みました");

        for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
            String path = String.format("%s", viewer.getTable().getValueAt(i, 1));

            if (path == null || path.isEmpty()) {
                continue;
            }

            String archive = HashDatabase.getArchiveID(path);
            if (!archive.equals("*")) {
                HashDatabase.addPathToDB(path, archive);
            }
        }


        HashDatabase.commit();

        Utils.getGlobalLogger().info("MusicHashの検索完了");
    }

    /**
     * プログラムのテスト
     */
    @SuppressWarnings("ConstantConditions")
    public static void TestPrg(){
        IsDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

        if (false) {
            //データ検証用
            testPapLoadTable();
        }

        if (false) {
            getModelsFromModelChara();
        }

        if (false) {
            getModelsFromItem();
        }

        if (false) {
            findSoundHashes();
        }

        if (false) {
            findVFXHashes();
        }

        if (false) {
            getHousingModels();
        }

        if (false) {
            findAnimationPAP_Hashes();
        }

        if (false) {
            openEveryCutb();
        }

        if (IsDebug) {
            findCutSceneHashes();
        }

        if (false) {
            //OK
            findSoundVoiceHashes();
        }

        if (false) {
            findCollisionHashes();
        }

        if (false) {
            findSkeletonHashes();
        }

        if (false) {
            //データ検証用
            testAnimationWorkTable();
        }
    }

    /**
     * Soundハッシュ検索
     */
    @SuppressWarnings("unused")
    public static void findSoundHashes() {

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        SqPack_IndexFile index2 = SqPack_IndexFile.GetIndexFileForArchiveID("070000", true);
        Utils.getGlobalLogger().info("SE.exhを開いています...");

        byte[] exhData = index.extractFile("exd/SE.exh");
        EXDF_View viewer = new EXDF_View(index, "exd/SE.exh", new EXHF_File(exhData));

        Utils.getGlobalLogger().info("SE.exhを読み込みました");

        for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
            //battle/etc/SE_Bt_Etc_UnicornVo.scd
            //      ↓
            //sound/battle/etc/SE_Bt_Etc_UnicornVo.scd
            String path = String.format("sound/%s", viewer.getTable().getValueAt(i, 1));

            if (path == null || path.isEmpty()) {
                continue;
            }

            String archive = HashDatabase.getArchiveID(path);
            if (index2.findFile(path) == 2) {
                HashDatabase.addPathToDB(path, archive);
            }
        }

        Utils.getGlobalLogger().info("Perform.exhを開いています...");

        exhData = index.extractFile("exd/Perform.exh");
        viewer = new EXDF_View(index, "exd/Perform.exh", new EXHF_File(exhData));

        Utils.getGlobalLogger().info("Perform.exhを読み込みました");

        for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
            //001grandpiano
            //      ↓
            //sound/instruments/001grandpiano.scd
            String path = String.format("sound/instruments/%s.scd", viewer.getTable().getValueAt(i, 1));

            if (path == null || path.isEmpty()) {
                continue;
            }

            String archive = HashDatabase.getArchiveID(path);
            if (index2.findFile(path) == 2) {
                HashDatabase.addPathToDB(path, archive);
            }
        }

        //noinspection ConstantConditions
        if (true){ //デバッグ用フラグ
            int searchIconNum = 300;

            //sound/event/検索
            for (int i = 1; i < searchIconNum; i++) {
                String soundPath;
                //例：sound/event/se_event_246.scd
                soundPath = String.format("sound/event/SE_Event_%03d.scd",i);

                //ui/icon登録
                cAddPathToDB(soundPath, "070000");
            }
        }

        //noinspection ConstantConditions
        if (true){ //デバッグ用フラグ
            int searchScdNum = 14000;

            //sound/battle/mon検索
            for (int i = 3001; i < searchScdNum; i++) {
                String soundPath;
                //例：sound/battle/mon/13001.scd
                String[] multi = {"_ja","_en","_fr","_de"};
                if (i <= 3900 || i > 3990){
                    soundPath = String.format("sound/battle/mon/%d.scd", i);
                    cAddPathToDB(soundPath, "070000");
                }else{
                    for (String lang :multi) {
                        soundPath = String.format("sound/battle/mon/%d%s.scd",i , lang);
                        cAddPathToDB(soundPath, "070000");
                    }
                }
            }
        }

        //noinspection ConstantConditions
        if (true){ //デバッグ用フラグ
            int searchScdNum = 3000;

            //sound/battle/mon検索
            for (int i = 1000; i < searchScdNum; i++) {
                String soundPath;
                //例：sound/voice/vo_line/8201000_ja.scd
                String[] multi = {"_ja","_en","_fr","_de"};
                for (String lang :multi) {
                    soundPath = String.format("sound/voice/vo_line/820%04d%s.scd",i, lang);
                    cAddPathToDB(soundPath, "070000");
                }
            }
        }

        HashDatabase.commit();

        Utils.getGlobalLogger().info("Soundの検索完了");
    }

    /**
     * SoundVoiceハッシュ検索
     */
    @SuppressWarnings("unused")
    public static void findSoundVoiceHashes() {
        byte[] exhData;
        SqPack_IndexFile index;
        SqPack_IndexFile index3 = null;
        SqPack_IndexFile index3_1 = null;
        SqPack_IndexFile index3_2 = null;
        SqPack_IndexFile index3_3 = null;
        EXDF_View viewer;

        String[] multi = {"ja","en","fr","de"};
        String[] voiceType = {"m","f"};
        String ExPath = "ffxiv";
        String archive = "030000";

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", false);

        SqPack_Folder[] folders = index.getPackFolders();

        for (SqPack_Folder spPack_folder : folders){
            SqPack_File[] files = spPack_folder.getFiles();
            for (SqPack_File sqPack_file : files) {
                String folderName = spPack_folder.getName();
                if (folderName != null && folderName.startsWith("exd/cut_scene/")) {
                    String fileName = sqPack_file.getName();
                    if (fileName != null && fileName.endsWith(".exh")) {
                        if (fileName.endsWith(".exh")) {
                            //例：exd/cut_scene/055/VoiceMan_05500.exh
                            String VoiceMan_File = folderName + "/" + fileName;
                            exhData = index.extractFile(VoiceMan_File);
                            viewer = new EXDF_View(index, VoiceMan_File, new EXHF_File(exhData));

                            Utils.getGlobalLogger().info(String.format("%sを読み込みました", fileName));
                            if (folderName.charAt(15) == '2'){
                                ExPath = "ffxiv";
                                archive = HashDatabase.getArchiveID(String.format("cut/%s/", ExPath));
                                if(index3 == null) {
                                    index3 = SqPack_IndexFile.GetIndexFileForArchiveID(archive, true);
                                }
                                bgcommonIndex = index3;
                            }else if (folderName.charAt(15) == '3'){
                                ExPath = "ex1";
                                archive = HashDatabase.getArchiveID(String.format("cut/%s/", ExPath));
                                if(index3_1 == null) {
                                    index3_1 = SqPack_IndexFile.GetIndexFileForArchiveID(archive, true);
                                }
                                bgcommonIndex = index3_1;
                            }else if (folderName.charAt(15) == '4'){
                                ExPath = "ex2";
                                archive = HashDatabase.getArchiveID(String.format("cut/%s/", ExPath));
                                if(index3_2 == null) {
                                    index3_2 = SqPack_IndexFile.GetIndexFileForArchiveID(archive, true);
                                }
                                bgcommonIndex = index3_2;
                            }else if (folderName.charAt(15) == '5'){
                                ExPath = "ex3";
                                archive = HashDatabase.getArchiveID(String.format("cut/%s/", ExPath));
                                if(index3_3 == null) {
                                    index3_3 = SqPack_IndexFile.GetIndexFileForArchiveID(archive, true);
                                }
                                bgcommonIndex = index3_3;
                            }else if (folderName.charAt(15) == '6'){
                                //END WALKER 暁の終焉 用
                                ExPath = "ex4";

                                continue;
                            }

                            if (IsDebug){
                                for (int i = 0; i < viewer.getTable().getRowCount(); i++) {
                                    String VoiceSign = String.format("%s", viewer.getTable().getValueAt(i, 1));
                                    //TEXT_VOICEMAN_05500_000010_MERLWYB

                                    if (VoiceSign == null || VoiceSign.isEmpty()) {
                                        continue;
                                    }

                                    //TEXT_VOICEMAN_05500_Q1_000_001_NONE_VOICE等の排除
                                    if (VoiceSign.startsWith("TEXT_VOICEMAN_") && !VoiceSign.endsWith("_NONE_VOICE")){
                                        String[] VoiceID = VoiceSign.split("_");
                                        for (String lang : multi) {
                                            for (String voiceType2 : voiceType) {
                                                //cut/ex3/sound/voicem/VoiceMan_05401/Vo_VoiceMan_05401_000010_m_ja.scd
                                                String fullPass = String.format("cut/%s/sound/voicem/VoiceMan_%s/Vo_VoiceMan_%s_%s_%s_%s.scd", ExPath, VoiceID[2], VoiceID[2], VoiceID[3], voiceType2, lang);

                                                cAddPathToDB(fullPass, archive, 2);
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        HashDatabase.commit();

        Utils.getGlobalLogger().info("SoundVoiceの検索完了");
    }

    /**
     * VFXハッシュ検索
     */
    @SuppressWarnings({"unused", "ConstantConditions"})
    public static void findVFXHashes() {
        byte[] exhData;
        SqPack_IndexFile index;
        SqPack_IndexFile index2;
        EXDF_View viewer = null;

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        index2 = SqPack_IndexFile.GetIndexFileForArchiveID("080000", true);
        //TODO: "vfx/cut/general/eff/%s.avfx"も自動登録したい

        if(false) {
            Utils.getGlobalLogger().info("VFX.exhを開いています...");

            exhData = index.extractFile("exd/VFX.exh");
            viewer = new EXDF_View(index, "exd/VFX.exh", new EXHF_File(exhData));

            Utils.getGlobalLogger().info("VFX.exhを読み込みました");

            for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
                //m0689_stlp2b_c0t1
                //      ↓
                //vfx/common/eff/m0689_stlp2b_c0t1.avfx
                String path = String.format("vfx/common/eff/%s.avfx", viewer.getTable().getValueAt(i, 1));

                if (path == null || path.isEmpty()) {
                    continue;
                }

                String archive = HashDatabase.getArchiveID(path);
                if (index2.findFile(path) == 2) {
                    int result = HashDatabase.addPathToDB(path, archive);

                    if (result == 1) {
                        //新規追加の場合
                        try {
                            //avfxファイル内のパスも解析
                            byte[] data2 = index2.extractFile(path);
                            AVFX_File avfxFile = new AVFX_File(index2, data2, index2.getEndian());
                            avfxFile.regHash(true);
                        } catch (Exception avfxException) {
                            avfxException.printStackTrace();
                        }
                    }

                }
            }
        }

        if(false) {
            Utils.getGlobalLogger().info("Omen.exhを開いています...");

            exhData = index.extractFile("exd/Omen.exh");
            viewer = new EXDF_View(index, "exd/Omen.exh", new EXHF_File(exhData));

            Utils.getGlobalLogger().info("Omen.exhを読み込みました");

            for (int col = 1; col <= 2; col++) {
                for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
                    //bahamut2_bomu_omen0s
                    //      ↓
                    //vfx/omen/eff/bahamut2_bomu_omen0s.avfx
                    String path = String.format("vfx/omen/eff/%s.avfx", viewer.getTable().getValueAt(i, col));

                    if (path == null || path.isEmpty()) {
                        continue;
                    }

                    String archive = HashDatabase.getArchiveID(path);
                    if (index2.findFile(path) == 2) {
                        int result = HashDatabase.addPathToDB(path, archive);

                        if (result == 1) {
                            //新規追加の場合
                            try {
                                //avfxファイル内のパスも解析
                                byte[] data2 = index2.extractFile(path);
                                AVFX_File avfxFile = new AVFX_File(index2, data2, index2.getEndian());
                                avfxFile.regHash(true);
                            } catch (Exception avfxException) {
                                avfxException.printStackTrace();
                            }
                        }

                    }
                }
            }
        }

        if(false) {
            Utils.getGlobalLogger().info("EventVfx.exhを開いています...");

            exhData = index.extractFile("exd/EventVfx.exh");
            viewer = new EXDF_View(index, "exd/EventVfx.exh", new EXHF_File(exhData));

            Utils.getGlobalLogger().info("EventVfx.exhを読み込みました");

            for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
                //gpose_akatsuki
                //      ↓
                //vfx/grouppose/eff/gpose_akatsuki.avfx
                int type = (int) viewer.getTable().getValueAt(i, 2);
                String path = null;
                if (type == 1) {
                    path = String.format("vfx/general/eff/%s.avfx", viewer.getTable().getValueAt(i, 3));
                } else if (type == 2) {
                    path = String.format("vfx/grouppose/eff/%s.avfx", viewer.getTable().getValueAt(i, 3));
                }

                if (path == null || path.isEmpty()) {
                    continue;
                }

                String archive = HashDatabase.getArchiveID(path);
                if (index2.findFile(path) == 2) {
                    int result = HashDatabase.addPathToDB(path, archive);

                    if (result == 1) {
                        //新規追加の場合
                        try {
                            //avfxファイル内のパスも解析
                            byte[] data2 = index2.extractFile(path);
                            AVFX_File avfxFile = new AVFX_File(index2, data2, index2.getEndian());
                            avfxFile.regHash(true);
                        } catch (Exception avfxException) {
                            avfxException.printStackTrace();
                        }
                    }

                }
            }
        }

        if(false) {
            Utils.getGlobalLogger().info("Channeling.exhを開いています...");

            exhData = index.extractFile("exd/Channeling.exh");
            viewer = new EXDF_View(index, "exd/Channeling.exh", new EXHF_File(exhData));

            Utils.getGlobalLogger().info("Channeling.exhを読み込みました");

            for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
                //chn_dark001f
                //      ↓
                //vfx/channeling/eff/chn_dark001f.avfx
                String path = String.format("vfx/channeling/eff/%s.avfx", viewer.getTable().getValueAt(i, 1));

                if (path == null || path.isEmpty()) {
                    continue;
                }

                String archive = HashDatabase.getArchiveID(path);
                if (index2.findFile(path) == 2) {
                    int result = HashDatabase.addPathToDB(path, archive);

                    if (result == 1) {
                        //新規追加の場合
                        try {
                            //avfxファイル内のパスも解析
                            byte[] data2 = index2.extractFile(path);
                            AVFX_File avfxFile = new AVFX_File(index2, data2, index2.getEndian());
                            avfxFile.regHash(true);
                        } catch (Exception avfxException) {
                            avfxException.printStackTrace();
                        }
                    }

                }
            }
        }

        if(true) {
            Utils.getGlobalLogger().info("Lockon.exhを開いています...");

            exhData = index.extractFile("exd/Lockon.exh");
            viewer = new EXDF_View(index, "exd/Lockon.exh", new EXHF_File(exhData));

            Utils.getGlobalLogger().info("Lockon.exhを読み込みました");

            for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
                //tar_ring0af
                //      ↓
                //vfx/lockon/eff/tar_ring0af.avfx
                String path = String.format("vfx/lockon/eff/%s.avfx", viewer.getTable().getValueAt(i, 1));

                if (path == null || path.isEmpty()) {
                    continue;
                }

                String archive = HashDatabase.getArchiveID(path);
                if (index2.findFile(path) == 2) {
                    int result = HashDatabase.addPathToDB(path, archive);

                    if (result == 1) {
                        //新規追加の場合
                        try {
                            //avfxファイル内のパスも解析
                            byte[] data2 = index2.extractFile(path);
                            AVFX_File avfxFile = new AVFX_File(index2, data2, index2.getEndian());
                            avfxFile.regHash(true);
                        } catch (Exception avfxException) {
                            avfxException.printStackTrace();
                        }
                    }

                }
            }
        }

        if(false) {
            for (int i = 1; i < 1000; i++) {
                String path;
                path = String.format("vfx/lovm/eff/%03d.avfx", i);

                if (path == null || path.isEmpty()) {
                    continue;
                }

                String archive = HashDatabase.getArchiveID(path);
                if (index2.findFile(path) == 2) {
                    int result = HashDatabase.addPathToDB(path, archive);

                    if (result == 1) {
                        //新規追加の場合
                        try {
                            //avfxファイル内のパスも解析
                            byte[] data2 = index2.extractFile(path);
                            AVFX_File avfxFile = new AVFX_File(index2, data2, index2.getEndian());
                            avfxFile.regHash(true);
                        } catch (Exception avfxException) {
                            avfxException.printStackTrace();
                        }
                    }

                }
            }
        }

        if (viewer == null) {
            return;
        }

        HashDatabase.commit();

        Utils.getGlobalLogger().info("VFXの検索完了");
    }

    /**
     * CutSceneハッシュ検索
     */
    @SuppressWarnings("unused")
    public static void findCutSceneHashes() {
        byte[] exhData;
        SqPack_IndexFile index;
        SqPack_IndexFile index3 = null;
        SqPack_IndexFile index3_1 = null;
        SqPack_IndexFile index3_2 = null;
        SqPack_IndexFile index3_3 = null;
        EXDF_View viewer;

        String[] multi = {"ja","en","fr","de"};
        String[] voiceType = {"m","f"};
        String ExPath;
        String archive;

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", false);

        String folderName = "exd";
        String fileName = "Cutscene.exh";
        //例：exd/Cutscene.exh
        String VoiceMan_File = folderName + "/" + fileName;
        exhData = index.extractFile(VoiceMan_File);
        viewer = new EXDF_View(index, VoiceMan_File, new EXHF_File(exhData));

        Utils.getGlobalLogger().info(String.format("%sを読み込みました", fileName));


        for (int i = 0; i < viewer.getTable().getRowCount(); i++) {
            String CutSign = String.format("%s", viewer.getTable().getValueAt(i, 1));
            //ffxiv/manfst/manfst00000/manfst00000

            if (CutSign == null || CutSign.isEmpty()) {
                continue;
            }

            String[] CutPath = CutSign.split("/");

            ExPath = CutPath[0];
            CutPath[1] = CutPath[1].substring(0, 1).toUpperCase() + CutPath[1].substring(1, 3) + CutPath[1].substring(3, 4).toUpperCase() + CutPath[1].substring(4);
            CutPath[2] = CutPath[1] + CutPath[2].substring(6);

            archive = HashDatabase.getArchiveID(String.format("cut/%s/", ExPath));

            String fullPass = String.format("cut/%s/%s/%s/%s.cutb",ExPath,CutPath[1],CutPath[2],CutPath[2]);
            cAddPathToDB(fullPass, archive, 2);

            //cut/ffxiv/sound/airfst/airfst00010/se_nc_airfst00010.scd
            //fullPass = String.format("cut/%s/sound/%s/%s/SE_Nc_%s.scd",ExPath,CutPath[1],CutPath[2],CutPath[2])
            //cAddPathToDB(fullPass, archive, 2)
        }

        HashDatabase.commit();

        Utils.getGlobalLogger().info("Cutsceneの検索完了");
    }
    /**
     * Mapハッシュ検索
     */
    public static void findMapHashes() {
        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        SqPack_IndexFile index2 = SqPack_IndexFile.GetIndexFileForArchiveID("060000", true);
        byte[] exhData = index.extractFile("exd/map.exh");

        if (exhData == null) {
            return;
        }

        EXHF_File exhfFile = new EXHF_File(exhData);
        EXDF_View mapView = new EXDF_View(index, "exd/map.exh", exhfFile);

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        //noinspection ConstantConditions
        if (true){ //デバッグ用フラグ
            //Map用テクスチャファイル登録
            for (int i = 1; i < mapView.getTable().getRowCount(); i++) {
                String map = mapView.getTable().getValueAt(i, 7).toString();

                if (map == null || map.isEmpty()) {
                    continue;
                }

                String mapFolder = "ui/map/" + map;

                if (!map.contains("/")) {
                    continue;
                }

                String[] split = map.split("/");
                String[] mapTexEx = new String[]{"_m.tex","_s.tex","m_m.tex","m_s.tex","d.tex"};
                for (String mapEx :mapTexEx){
                    String mapPath = mapFolder + "/" + split[0] + split[1] + mapEx;
                    if (index2.findFile(mapPath) == 2) { //存在チェック
                        HashDatabase.addPathToDB(mapPath, "060000");
                    }
                }
            }
            Utils.getGlobalLogger().info("Map検索完了");
        }

        //noinspection ConstantConditions
        if (true){ //デバッグ用フラグ
            int searchIconNum = 182000;

            //ui/icon検索
            boolean multiFlag = false;
            for (int i = 0; i < searchIconNum; i++) {
                String iconPath;
                String iconPath2;
                //例：ui/icon/181000/ja/181001.tex
                String[] multi = {"ja/","en/","fr/","de/"};
                int pathNum = (i / 1000) * 1000;
                if (i < 20000 || (i >= 60000 && i < 120000) || (i >= 130000 && i < 150000)){
                    iconPath = String.format("ui/icon/%06d/%06d.tex",pathNum, i);
                    iconPath2 = String.format("ui/icon/%06d/%06d_hr1.tex",pathNum, i); //高画質用
                }else if (i < 60000){
                    iconPath = String.format("ui/icon/%06d/%s%06d.tex",pathNum, "hq/", i);
                    iconPath2 = String.format("ui/icon/%06d/%s%06d_hr1.tex",pathNum, "hq/", i); //高画質用
                }else{
                    if ((i % 1000) == 0){multiFlag = true;}
                    if (multiFlag){
                        for (String lang :multi) {
                            iconPath = String.format("ui/icon/%06d/%s%06d.tex",pathNum, lang, i);
                            iconPath2 = String.format("ui/icon/%06d/%s%06d_hr1.tex",pathNum, lang, i); //高画質用

                            if (index2.findFile(iconPath) == 2) { //存在チェック
                                //ui/icon登録
                                HashDatabase.addPathToDB(iconPath, "060000");
                                HashDatabase.addPathToDB(iconPath2, "060000"); //高画質用
                            }
                            multiFlag = false;
                        }
                        continue;
                    }else{
                        iconPath = String.format("ui/icon/%06d/ja/%06d.tex",pathNum, i);
                        iconPath2 = String.format("ui/icon/%06d/ja/%06d_hr1.tex",pathNum, i); //高画質用
                    }

                }

                if (index2.findFile(iconPath) == 2) { //存在チェック
                    //ui/icon登録
                    HashDatabase.addPathToDB(iconPath, "060000");
                    HashDatabase.addPathToDB(iconPath2, "060000"); //高画質用
                }
            }
            Utils.getGlobalLogger().info("Icon検索完了");
        }

        HashDatabase.commit();
    }

    /**
     * ModelChara.exhファイルからハッシュDB登録
     */
    @SuppressWarnings("unused")
    public static void getModelsFromModelChara() {
        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        SqPack_IndexFile index2 = SqPack_IndexFile.GetIndexFileForArchiveID("040000", true);
        byte[] exhData = index.extractFile("exd/ModelChara.exh");
        EXDF_View viewer = new EXDF_View(index, "exd/ModelChara.exh", new EXHF_File(exhData));

        Utils.getGlobalLogger().info("ModelChara.exhを読み込みました");

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        //for (int i = 3034; i < viewer.getTable().getRowCount(); i++) {
        for (int i = 3089; i < viewer.getTable().getRowCount(); i++) {

            int id = (int) viewer.getTable().getValueAt(i, 0);
            int type = (int) viewer.getTable().getValueAt(i, 1);
            int model = (int) viewer.getTable().getValueAt(i, 2);
            int base = (int) viewer.getTable().getValueAt(i, 3);
            variant = (int) viewer.getTable().getValueAt(i, 4);

            Utils.getGlobalLogger().info("ID:{}をチェック中", id);

            String typePath;
            String imcPath, modelPath, skelPath, papPath;

            switch (type) {
                case 2: //demihuman
                    typePath = "chara/demihuman/d";
                    imcPath = String.format("%s%04d/obj/equipment/e%04d/e%04d.imc", typePath, model, base, base);
                    cAddPathToDB(imcPath, "040000");

                    modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_met.mdl", typePath, model, base, model, base);
                    cAddPathToDB(modelPath, "040000");
                    modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_top.mdl", typePath, model, base, model, base);
                    cAddPathToDB(modelPath, "040000");
                    modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_glv.mdl", typePath, model, base, model, base);
                    cAddPathToDB(modelPath, "040000");
                    modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_dwn.mdl", typePath, model, base, model, base);
                    cAddPathToDB(modelPath, "040000");
                    modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_sho.mdl", typePath, model, base, model, base);
                    cAddPathToDB(modelPath, "040000");

                    //chara/demihuman/d1024/obj/equipment/e0001/vfx/eff/ve0001.avfx
                    String avfxPath = String.format("%s%04d/obj/equipment/e%04d/vfx/eff/ve0001.avfx", typePath, model, base);
                    String atexPath = String.format("%s%04d/obj/equipment/e%04d/vfx/texture/", typePath, model, base);
                    int pathCheck = index2.findFile(avfxPath);
                    if (pathCheck == 2){
                        HashDatabase.addPathToDB(avfxPath, "040000");
                        HashDatabase.addFolderToDB(atexPath, "040000");
                    }else if (pathCheck == 1){
                        //ファイルパスのみ追加
                        String folder = avfxPath.substring(0, avfxPath.lastIndexOf('/'));
                        HashDatabase.addFolderToDB(folder, "040000");
                        HashDatabase.addFolderToDB(atexPath, "040000");
                    }

                    skelPath = String.format("%s%04d/skeleton/base/b%04d/eid_m%04db%04d.eid", typePath, model, base, model, base);
                    cAddPathToDB(skelPath, "040000");
                    skelPath = String.format("%s%04d/skeleton/base/b%04d/phy_m%04db%04d.phyb", typePath, model, base, model, base);
                    cAddPathToDB(skelPath, "040000");
                    skelPath = String.format("%s%04d/skeleton/base/b%04d/skl_m%04db%04d.skp", typePath, model, base, model, base);
                    cAddPathToDB(skelPath, "040000");
                    skelPath = String.format("%s%04d/skeleton/base/b%04d/skl_m%04db%04d.sklb", typePath, model, base, model, base);
                    cAddPathToDB(skelPath, "040000");

                    for (int animationID = 1; animationID < 6; animationID++) {
                        //chara/demihuman/d1039/animation/a0001/bt_common/resident/action.pap
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/resident/action.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        //chara/demihuman/d1041/animation/f0000/resident/face.pap
                        //chara/demihuman/d1041/animation/f0000/nonresident/eden/eden_cookie_gaia.pap
                        papPath = String.format("%s%04d/animation/f0000/resident/face.pap", typePath, model);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/f0000/nonresident/distress.pap", typePath, model);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/f0000/nonresident/eden/eden_cookie_gaia.pap", typePath, model);
                        cAddPathToDB(papPath, "040000");
                        //chara/demihuman/d1002/animation/a0001/bt_2sp_emp/ws/bt_swd_sld/wsh001.pap
                        papPath = String.format("%s%04d/animation/a%04d/bt_2ax_emp/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2ax_emp/ws/bt_2ax_emp/wsh001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2bw_emp/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2bw_emp/ws/bt_2bw_emp/wsh001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");

                        papPath = String.format("%s%04d/animation/a%04d/bt_2sp_emp/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2sp_emp/ws/bt_2sp_emp/wsh001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2sp_emp/ws/bt_2ax_emp/wsh001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2sp_emp/ws/bt_2bw_emp/wsh001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2sp_emp/ws/bt_swd_sld/wsh001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2sp_sld/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2sp_sld/ws/bt_2sp_emp/wsh001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        //chara/demihuman/d1007/animation/a0001/bt_2sp_sld/ws/bt_2sp_emp/wsh001.pap
                        papPath = String.format("%s%04d/animation/a%04d/bt_2st_emp/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_2sw_emp/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_clw_clw/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        //chara/demihuman/d1001/animation/a0001/bt_common/ability/thm_black/abl001.pap
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/ability/2ax_warrior/abl001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/ability/2bw_bard/abl001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/ability/2sp_dragoon/abl001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/ability/clw_monk/abl001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/ability/cnj_white/abl001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/ability/swd_knight/abl001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/ability/swl_summon/abl001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/ability/thm_black/abl001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");

                        papPath = String.format("%s%04d/animation/a%04d/bt_common/bnpc_passmove/bnpc_pasmove_loop.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/buddy/glad.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/cr/ability.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/emote/jmn.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/event/event_cry.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/event_base/event_base_stand_talk.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/normal/revive.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/specialpop/specialpop.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/warp/warp_end.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        //chara/demihuman/d1003/animation/a0001/bt_stf_sld/resident/attack.pap
                        papPath = String.format("%s%04d/animation/a%04d/bt_emp_emp/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_stf_sld/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_swd_sld/resident/attack.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_swd_sld/ws/bt_swd_sld/wsh001.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");


                        papPath = String.format("%s%04d/animation/a%04d/bt_common/mon_sp/m%04d/mon_sp001.pap", typePath, model, animationID, model);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/mon_sp/m%04d/hide/mon_sp001.pap", typePath, model, animationID, model);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/mon_sp/m%04d/show/mon_sp001.pap", typePath, model, animationID, model);
                        cAddPathToDB(papPath, "040000");
                    }

                    break;
                case 3: //monster
                    typePath = "chara/monster/m";
                    imcPath = String.format("%s%04d/obj/body/b%04d/b%04d.imc", typePath, model, base, base);
                    modelPath = String.format("%s%04d/obj/body/b%04d/model/m%04db%04d.mdl", typePath, model, base, model, base);

                    cAddPathToDB(imcPath, "040000");
                    cAddPathToDB(modelPath, "040000");

                    skelPath = String.format("%s%04d/skeleton/base/b%04d/eid_m%04db%04d.eid", typePath, model, base, model, base);
                    cAddPathToDB(skelPath, "040000");
                    skelPath = String.format("%s%04d/skeleton/base/b%04d/skl_m%04db%04d.skp", typePath, model, base, model, base);
                    cAddPathToDB(skelPath, "040000");
                    skelPath = String.format("%s%04d/skeleton/base/b%04d/skl_m%04db%04d.sklb", typePath, model, base, model, base);
                    cAddPathToDB(skelPath, "040000");

                    for (int animationID = 1; animationID < 6; animationID++) {
                        //chara/monster/m0378/animation/a0001/bt_common/bnpc_passmove/bnpc_pasmove_end.pap
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/resident/monster.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/bnpc_gimmickjump/bnpc_gmcjump_end.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/bnpc_gesture/gesture01.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/bnpc_passmove/bnpc_pasmove_end.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/event/event_wandering_action.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/event_base/event_base_caution.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/emote/jmn.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/idle_sp/idle_sp_1.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/lcut/show01.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/loop_sp/loop_sp1.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/normal/revive.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/parts_idle_sp/sp1.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/specialpop/specialdepop.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/warp/warp_start.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");

                        //chara/monster/m0402/animation/a0001/bt_common/mount_sp/m0402/mon_sp001.pap
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/mon_sp/m%04d/mon_sp001.pap", typePath, model, animationID, model);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/mon_sp/m%04d/hide/mon_sp001.pap", typePath, model, animationID, model);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/mon_sp/m%04d/show/mon_sp001.pap", typePath, model, animationID, model);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/mount_sp/m%04d/mon_sp001.pap", typePath, model, animationID, model);
                        cAddPathToDB(papPath, "040000");
                        papPath = String.format("%s%04d/animation/a%04d/bt_common/mon_sp/common/mon_sph010.pap", typePath, model, animationID);
                        cAddPathToDB(papPath, "040000");
                    }

                    break;
            }
        }

        HashDatabase.commit();

        Utils.getGlobalLogger().info("ModelChara.exhの検索完了");
    }

    /**
     * Item.exhからcharaモデルのハッシュDB登録
     * ※アイテム名称が登録されているものだけのため、カットシーンのみに登場するモデルなどは対象から外れます。
     */
    @SuppressWarnings({"unused"})
    public static void getModelsFromItem() {
        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        SqPack_IndexFile index2 = SqPack_IndexFile.GetIndexFileForArchiveID("040000", true);
        byte[] exhData = index.extractFile("exd/Item.exh");
        //TODO: NPC専用の装備モデルは「exd/ENpcDressUpDress.exh」から取得できるはずだが
        // EXDF_Viewの作りが古いためデータがずれていて現在の所不可
        // equipment,weapon:9000番以降

        EXDF_View viewer;
        viewer = new EXDF_View(index, "exd/Item.exh", new EXHF_File(exhData));

        Utils.getGlobalLogger().info("Item.exhを読み込みました");

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        int[] charaID = new int[]{101,201,301,401,501,601,701,801,901,1001,1101,1201,1301,1401,1501,1801};
        //i = 0 から 登録した方が確実だが時間がかかりそうなので途中から
        for (int i = 33146; i < viewer.getTable().getRowCount(); i++) {

            int keyID = (int) viewer.getTable().getValueAt(i, 0);
            String itemName = (String) viewer.getTable().getValueAt(i, 1);
            int type = (int) viewer.getTable().getValueAt(i, 18);
            String[] models = new String[2];
            models[0] = (String) viewer.getTable().getValueAt(i, 48);
            models[1] = (String) viewer.getTable().getValueAt(i, 49);

            Utils.getGlobalLogger().info("{}:{}を調査しています", keyID,itemName);

            String typePath, equipSlot = null;
            String imcPath, modelPath, mtrlPath;


            switch (type) {
                case 0: //調理品(モデルあり)、その他アイテム(モデル無し)
                case 1: //片手武器(メインスロット用)
                case 2: //片手武器(サブスロット用)
                case 13: //両手武器(メイン装備可、サブ装備不可)
                case 14: //片手武器(メインとサブスロットのどちらでも可)(二刀流？)
                    typePath = "chara/weapon/w";
                    for (String models1 : models){
                        if (!models1.equals("0, 0, 0, 0")) {
                            String[] model1Split = models1.split(", ");
                            int modelNum = Integer.parseInt(model1Split[0].trim());
                            int bodyNum = Integer.parseInt(model1Split[1].trim());
                            variant = Integer.parseInt(model1Split[2].trim());

                            imcPath = String.format("%s%04d/obj/body/b%04d/b%04d.imc", typePath, modelNum, bodyNum, bodyNum);
                            cAddPathToDB(imcPath, "040000");

                            modelPath = String.format("%s%04d/obj/body/b%04d/model/w%04db%04d.mdl", typePath, modelNum, bodyNum, modelNum, bodyNum);
                            cAddPathToDB(modelPath, "040000");

                            mtrlPath = String.format("%s%04d/obj/body/b%04d/material/v%04d/mt_w%04db%04d_a.mtrl", typePath, modelNum, bodyNum, variant, modelNum, bodyNum);
                            cAddPathToDB(mtrlPath, "040000");

                            String sklbPath = String.format("%s%04d/skeleton/base/b0001/skl_w%04db0001.sklb", typePath, modelNum, modelNum);
                            cAddPathToDB(sklbPath, "040000");

                            String animationPath = String.format("%s%04d/animation/a0001/wp_common/resident/weapon.pap", typePath, modelNum);
                            cAddPathToDB(animationPath, "040000");

                            animationPath = String.format("%s%04d/animation/s0001/body/material.pap", typePath, modelNum);
                            cAddPathToDB(animationPath, "040000");

                            String avfxPath;
                            int vfxNum = 1;
                            int pathCheck;
                            do {
                                //vw○○○.avfxファイルを検索して追加を試みる
                                avfxPath = String.format("%s%04d/obj/body/b%04d/vfx/eff/vw%04d.avfx", typePath, modelNum, bodyNum, vfxNum);
                                pathCheck = index2.findFile(avfxPath);
                                vfxNum++;
                            } while (vfxNum < 10 && pathCheck == 2);

                            if (pathCheck == 2){
                                HashDatabase.addPathToDB(avfxPath, "040000");
                                byte[] data = index2.extractFile(avfxPath);
                                AVFX_File avfxFile = new AVFX_File(index2, data, index2.getEndian());
                                avfxFile.regHash();

                            }else if (pathCheck == 1){
                                //ファイルパスのみ追加
                                String folder = avfxPath.substring(0, avfxPath.lastIndexOf('/'));
                                HashDatabase.addFolderToDB(folder, "040000",true);

                                String atexPath = String.format("%s%04d/obj/body/b%04d/vfx/texture/", typePath, modelNum, bodyNum);
                                HashDatabase.addFolderToDB(atexPath, "040000",true);
                            }
                        }
                    }
                    break;
                case 3: //頭防具
                    equipSlot = "met";
                    break;
                case 4: //胴防具
                case 15: //胴防具(頭装備不可)
                case 16: //胴防具(手脚足装備不可)
                case 19: //胴防具(頭手脚足装備不可)
                case 20: //胴防具(手脚装備不可)
                case 21: //胴防具(脚足装備不可)
                    equipSlot = "top";
                    break;
                case 5: //手防具
                    equipSlot = "glv";
                    break;
                case 6: //帯防具
                    break;
                case 7: //脚防具
                case 18: //脚防具(足装備不可)
                    equipSlot = "dwn";
                    break;
                case 8: //足防具
                    equipSlot = "sho";
                    break;
                case 9: //耳飾り
                    equipSlot = "ear";
                    break;
                case 10: //首飾り
                    equipSlot = "nek";
                    break;
                case 11: //腕輪
                    equipSlot = "wrs";
                    break;
                case 12: //指輪
                    typePath = "chara/accessory/a";
                    for (String models1 : models){
                        for(int id : charaID){
                            if (!models1.equals("0, 0, 0, 0")) {
                                String[] model1Split = models1.split(", ");
                                int model = Integer.parseInt(model1Split[0].trim());
                                variant = Integer.parseInt(model1Split[1].trim());
                                int base = Integer.parseInt(model1Split[2].trim());

                                imcPath = String.format("%s%04d/a%04d.imc", typePath, model, model);
                                cAddPathToDB(imcPath, "040000");

                                equipSlot = "ril"; //左指
                                modelPath = String.format("%s%04d/model/c%04da%04d_%s.mdl", typePath, model, id, model, equipSlot);
                                cAddPathToDB(modelPath, "040000");

                                mtrlPath = String.format("%s%04d/material/v%04d/mt_c%04da%04d_%s_a.mtrl", typePath, model, variant, id, model, equipSlot);
                                cAddPathToDB(mtrlPath, "040000");

                                equipSlot = "rir"; //右指
                                modelPath = String.format("%s%04d/model/c%04da%04d_%s.mdl", typePath, model, id, model, equipSlot);
                                cAddPathToDB(modelPath, "040000");

                                mtrlPath = String.format("%s%04d/material/v%04d/mt_c%04da%04d_%s_a.mtrl", typePath, model, variant, id, model, equipSlot);
                                cAddPathToDB(mtrlPath, "040000");
                            }
                        }
                    }
                    break;
                case 17: //ソウルクリスタル
                    break;
            }

            //上記スイッチの重複処理のみ抽出
            switch (type) {
                case 3: //頭防具
                case 4: //胴防具
                case 5: //手防具
                case 6: //帯防具
                case 7: //脚防具
                case 8: //足防具
                case 15: //胴防具(頭装備不可)
                case 16: //胴防具(手脚足装備不可)
                case 19: //胴防具(頭手脚足装備不可)
                case 20: //胴防具(手脚装備不可)
                case 21: //胴防具(脚足装備不可)
                case 18: //脚防具(足装備不可)
                    typePath = "chara/equipment/e";
                    for (String models1 : models){
                        for(int id : charaID){
                            if (!models1.equals("0, 0, 0, 0")) {
                                String[] model1Split = models1.split(", ");
                                int model = Integer.parseInt(model1Split[0].trim());
                                variant = Integer.parseInt(model1Split[1].trim());
                                int base = Integer.parseInt(model1Split[2].trim());

                                imcPath = String.format("%s%04d/e%04d.imc", typePath, model, model);
                                cAddPathToDB(imcPath, "040000");

                                modelPath = String.format("%s%04d/model/c%04de%04d_%s.mdl", typePath, model, id, model, equipSlot);
                                cAddPathToDB(modelPath, "040000");
                                //chara/accessory/a0129/material/v0009/mt_c0101a0129_ear_a.mtrl
                                mtrlPath = String.format("%s%04d/material/v%04d/mt_c%04de%04d_%s_a.mtrl", typePath, model, variant, id, model, equipSlot);
                                cAddPathToDB(mtrlPath, "040000");
                            }
                        }
                    }
                    break;
                case 9: //耳飾り
                case 10: //首飾り
                case 11: //腕輪
                    typePath = "chara/accessory/a";
                    for (String models1 : models){
                        for(int id : charaID){
                            if (!models1.equals("0, 0, 0, 0")) {
                                String[] model1Split = models1.split(", ");
                                int model = Integer.parseInt(model1Split[0].trim());
                                variant = Integer.parseInt(model1Split[1].trim());
                                int base = Integer.parseInt(model1Split[2].trim());

                                imcPath = String.format("%s%04d/a%04d.imc", typePath, model, model);
                                cAddPathToDB(imcPath, "040000");

                                modelPath = String.format("%s%04d/model/c%04da%04d_%s.mdl", typePath, model, id, model, equipSlot);
                                cAddPathToDB(modelPath, "040000");

                                mtrlPath = String.format("%s%04d/material/v%04d/mt_c%04da%04d_%s_a.mtrl", typePath, model, variant, id, model, equipSlot);
                                cAddPathToDB(mtrlPath, "040000");
                            }
                        }
                    }
                    break;
            }
        }

        HashDatabase.commit();

        Utils.getGlobalLogger().info("Item.exhの検索完了");
    }

    /**
     * HousingYardObject.exhとHousingFurniture.exhからハウジングモデルのハッシュDB登録
     * ※アイテム名称が登録されているものだけのため、カットシーンのみに登場するモデルなどは対象から外れます。
     */
    @SuppressWarnings({"unused"})
    public static void getHousingModels() {
        byte[] exhData;
        SqPack_IndexFile index;
        EXDF_View viewer;

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        bgcommonIndex = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", false);

        //庭具
        exhData = index.extractFile("exd/HousingYardObject.exh");
        viewer = new EXDF_View(index, "exd/HousingYardObject.exh", new EXHF_File(exhData));

        Utils.getGlobalLogger().info("HousingYardObject.exhを読み込みました");

        String typePath, equipSlot = null;
        String imcPath, modelPath, pcbPath;
        int pathCheck, result ;

        //i = 0 はデータなし
        for (int i = 1; i < viewer.getTable().getRowCount(); i++) {

            int ModelKey = (int) viewer.getTable().getValueAt(i, 1);

            Utils.getGlobalLogger().info("庭具:{}を調査しています", ModelKey);

            //bgcommon/hou/outdoor/general/0225/asset/gar_b0_m0225.sgb
            modelPath = String.format("bgcommon/hou/outdoor/general/%04d/asset/gar_b0_m%04d.sgb", ModelKey, ModelKey);
            if (bgcommonIndex.findFile(modelPath) == 2){

                result = HashDatabase.addPathToDB(modelPath, "010000");
                if (result == 1) {
                    //新規に登録した場合のみSGBファイルを解析する
                    byte[] data2 = bgcommonIndex.extractFile(modelPath);
                    SgbFile sgbFile = new SgbFile(data2, bgcommonIndex.getEndian());
                }
                continue;
            }

            //assetのsgbファイルが存在しない場合
            //bgcommon/hou/outdoor/general/0225/bgparts/gar_b0_m0225.mdl
            //bgcommon/hou/outdoor/general/0224/collision/gar_b0_m0224.pcb
            modelPath = String.format("bgcommon/hou/outdoor/general/%04d/bgparts/gar_b0_m%04d.mdl", ModelKey, ModelKey);
            pathCheck = bgcommonIndex.findFile(modelPath);
            if (pathCheck >= 1){
                if (pathCheck == 2) {
                    result = cAddPathToDB(modelPath, "010000", 2);
                    if (result > 1) {
                        continue; //ファイルが登録済みだった場合
                    }
                    //collision(ない場合も多い)
                    modelPath = String.format("bgcommon/hou/outdoor/general/%04d/collision/gar_b0_m%04d.pcb", ModelKey, ModelKey);
                    cAddPathToDB(modelPath, "010000", 2);
                }
                for (int cNum = 97; cNum <= 112; cNum++) {
                    //a～zまでだと97-123 取りあえずa～pまで
                    modelPath = String.format("bgcommon/hou/outdoor/general/%04d/bgparts/gar_b0_m%04d%s.mdl", ModelKey, ModelKey, (char)cNum);
                    if (bgcommonIndex.findFile(modelPath) == 2) {
                        result = cAddPathToDB(modelPath, "010000", 2);
                        if (result != 1) {
                            break;  //ファイルが登録済みまたは存在しない場合
                        }
                        //collision(ない場合も多い)
                        modelPath = String.format("bgcommon/hou/outdoor/general/%04d/collision/gar_b0_m%04d%s.pcb", ModelKey, ModelKey, (char) cNum);
                        cAddPathToDB(modelPath, "010000", 2);
                    }
                }

            }
        }
        Utils.getGlobalLogger().info("庭具検索完了");

        //家具
        exhData = index.extractFile("exd/HousingFurniture.exh");
        viewer = new EXDF_View(index, "exd/HousingFurniture.exh", new EXHF_File(exhData));

        Utils.getGlobalLogger().info("HousingYardObject.exhを読み込みました");

        //i = 0 はデータなし
        for (int i = 538; i < viewer.getTable().getRowCount(); i++) {

            int ModelKey = (int) viewer.getTable().getValueAt(i, 1);

            Utils.getGlobalLogger().info("家具:{}を調査しています", ModelKey);

            //bgcommon/hou/indoor/general/0982/asset/fun_b0_m0982.sgb
            modelPath = String.format("bgcommon/hou/indoor/general/%04d/asset/fun_b0_m%04d.sgb", ModelKey, ModelKey);
            if (bgcommonIndex.findFile(modelPath) == 2){
                HashDatabase.addPathToDB(modelPath, "010000");
                byte[] data2 = bgcommonIndex.extractFile(modelPath);
                SgbFile sgbFile = new SgbFile(data2, bgcommonIndex.getEndian());
                continue;
            }

            //assetのsgbファイルが存在しない場合
            modelPath = String.format("bgcommon/hou/outdoor/indoor/%04d/bgparts/fun_b0_m%04d.mdl", ModelKey, ModelKey);
            pathCheck = bgcommonIndex.findFile(modelPath);
            if (pathCheck >= 1){
                if (pathCheck == 2) {
                    result = cAddPathToDB(modelPath, "010000", 2);
                    if (result > 1) {
                        continue; //ファイルが登録済みだった場合
                    }
                    //collision(ない場合も多い)
                    modelPath = String.format("bgcommon/hou/indoor/general/%04d/collision/fun_b0_m%04d.pcb", ModelKey, ModelKey);
                    cAddPathToDB(modelPath, "010000", 2);
                }
                for (int cNum = 97; cNum <= 112; cNum++) {
                    //a～zまでだと97-123 取りあえずa～pまで
                    modelPath = String.format("bgcommon/hou/indoor/general/%04d/bgparts/fun_b0_m%04d%s.mdl", ModelKey, ModelKey, (char)cNum);
                    if (bgcommonIndex.findFile(modelPath) == 2) {
                        result = cAddPathToDB(modelPath, "010000", 2);
                        if (result != 1) {
                            break; //ファイルが登録済みまたは存在しない場合
                        }
                        //collision(ない場合も多い)
                        modelPath = String.format("bgcommon/hou/indoor/general/%04d/collision/fun_b0_m%04d%s.pcb", ModelKey, ModelKey, (char) cNum);
                        cAddPathToDB(modelPath, "010000", 2);
                    }
                }

            }
        }
        Utils.getGlobalLogger().info("家具検索完了");


        HashDatabase.commit();

        Utils.getGlobalLogger().info("ハウジングモデルの検索完了");
    }

    /**
     * animation_work_table-○○○.awtのデータ検証用
     * ※○○○はdemihuman,monster,weapon等
     */
    public static void testAnimationWorkTable() {

        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID("040000", true);
        //awtData = index.extractFile("chara/xls/animation/animation_work_table-monster.awt");
        //chara/xls/animation/animation_work_table-demihuman.awt
        byte[] awtData = index.extractFile("chara/xls/animation/animation_work_table-weapon.awt");

        ByteBuffer bb = ByteBuffer.wrap(awtData);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.getInt(); //twa
        bb.getShort(); //不明 0x300
        bb.getShort(); //不明 0x100
        short awTable_Num = bb.getShort(); //テーブル数 0x02a6 = 678 1テーブル8バイト

        bb.position(0x18); //ポインタをawTableの先頭に移動
        if(IsDebug){
            String dirPath = "./";
            String SavePath;
            SavePath = dirPath  + "AnimationWorkTable解析-Weapon.csv";
            try {
                File file = new File(SavePath);
                FileWriter filewriter = new FileWriter(file, false); //上書きモード
                String line;
                line = "awTable_ID,charaID,charaType,AnimationHeader_ID,AnimationBody_ID,Unknown_ID,GeneratePath\r\n";
                filewriter.write(line);

                //ここから8バイトのpapヘッダーテーブル(0x51A3×4 = 20899個)
                for(int awTable_ID = 0; awTable_ID < awTable_Num; awTable_ID++){
                    if ((bb.limit() - 7) < bb.position()){
                        Utils.getGlobalLogger().info("バッファーオーバーフロー防止のため読み込み停止");
                        break;
                    }
                    // chara/human/c0101/animation/a0033/bt_common/warp/warp_end.pap
                    //   charaType↑ ↑Character ID  ↑Animation_ID
                    int charaID = bb.getShort() & 0xFFFF;          //Character ID
                    short charaType = bb.getShort();        //charaType(0:human,1:monster,2:demi)
                    int AnimationHeader_ID = bb.get() & 0xFF;     //AnimationType_ID
                    int AnimationBody_ID = bb.get() & 0xFF;     //Animation_ID
                    int Unknown_ID = bb.getShort();    //不明

                    String charaTypeSign;
                    String charaTypeFull;
                    String GeneratePath;
                    switch(charaType){
                        case 0:
                            charaTypeSign = "c";
                            charaTypeFull = "human";
                            break;
                        case 1:
                            charaTypeSign = "m";
                            charaTypeFull = "monster";
                            break;
                        case 2:
                            charaTypeSign = "d";
                            charaTypeFull = "demihuman";
                            break;
                        case 3:
                            charaTypeSign = "w";
                            charaTypeFull = "weapon";
                            break;
                        default:
                            charaTypeSign = String.valueOf(charaType);
                            charaTypeFull = "unknown";
                    }
                    //String papAnimationTypeValue = papAnimationType.getOrDefault(AnimationType_ID, "Nothing("+AnimationType_ID+")");
                    GeneratePath = String.format("chara/%s/%s%04d/animation/a0001/", charaTypeFull, charaTypeSign, charaID);

                    //記録に残す。
                    line = awTable_ID + "," + charaID + "," + charaType + ","
                            + AnimationHeader_ID + "," + AnimationBody_ID + "," + Unknown_ID + "," + GeneratePath + "\r\n";
                    filewriter.write(line);
                }

                filewriter.close();
                Utils.getGlobalLogger().info("AnimationWorkTable解析を保存しました");

            } catch (FileNotFoundException e) {
                Utils.getGlobalLogger().error(e);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * paploadtable.pltのデータ検証用
     */
    public static void testPapLoadTable() {
        SqPack_IndexFile index;

        index = SqPack_IndexFile.GetIndexFileForArchiveID("040000", true);
        byte[] pltData = index.extractFile("chara/xls/animation/papLoadTable.plt");
        // chara/xls/animation/paploadtable.pltからpap関係の文字列取得
        HashMap<Integer, String> papAnimationType = new HashMap<>();
        HashMap<Integer, String> papFilePathTable = new HashMap<>();   //ability/2ax_warrior/abl024 等

        ByteBuffer bb = ByteBuffer.wrap(pltData);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        short papTable1_Num = bb.getShort(); //テーブル数 0x0968 = 2408 1テーブル8バイト
        short papTable2_Num = bb.getShort(); //不明 0xD3  0x68(104)バイトのデータブロックの数
        int papTable3_Num = bb.getInt(); //不明 0x51A3  0x20(8)バイトのデータブロックの数÷4
        int papAnimationTypeTablePos = bb.getInt(); //papTableの戦闘タイプテーブルの開始位置
        int papFilePathTablePos = bb.getInt(); //papTableのファイルパステーブルの開始位置

        bb.position(papAnimationTypeTablePos); //ポインタをpapTableの先頭に移動
        // EOFに達するまで読みこむ
        do {
            int papOffset;
            boolean IsAnimationType = true;
            if (bb.position() < papFilePathTablePos){
                //オフセットをハッシュマップのキーとして使う
                papOffset = bb.position() - papAnimationTypeTablePos;
            }else{
                //オフセットをハッシュマップのキーとして使う
                IsAnimationType = false;
                papOffset = bb.position() - papFilePathTablePos;
            }

            StringBuilder papString;
            papString = new StringBuilder();
            while (true) {
                byte c = bb.get();
                if (c == 0) {
                    break;
                } else {
                    papString.append((char) c);
                }
            }

            if (IsAnimationType){
                //オフセットをハッシュマップのキーとして使う
                papAnimationType.put(papOffset, papString.toString());
            }else{
                //オフセットをハッシュマップのキーとして使う
                papFilePathTable.put(papOffset, papString.toString());
            }
        } while (bb.position() < bb.capacity());

        bb.position(0x10); //ポインタをpapTableヘッダー？の先頭に移動

        if(IsDebug){
            String dirPath = "./";
            String SavePath;
            SavePath = dirPath  + "PapLoadTable解析.csv";
            try {
                File file = new File(SavePath);
                FileWriter filewriter = new FileWriter(file, false); //上書きモード
                String line;
                line = "papTable1_ID,Unknown_ID1,Unknown_ID2,papPathPattern\r\n";
                filewriter.write(line);

                //ここから8バイトのpapパターンテーブル(0x0968 = 2408個)
                for(short papTable1_ID = 0; papTable1_ID < papTable1_Num; papTable1_ID++){
                    int Unknown_ID1 = bb.getShort();      //不明
                    int Unknown_ID2 = bb.getShort();      //不明
                    int AnimationType_ID = bb.getShort(); //戦闘タイプテーブル先頭からのオフセット 例：0 → bt_common、0x0a → ot_m6000
                    int papFilePath_ID = bb.getShort();   //ファイルパステーブル先頭からのオフセット

                    //記録に残す。
                    line = papTable1_ID + "," + Unknown_ID1 + "," + Unknown_ID2 + ","
                            + papAnimationType.get(AnimationType_ID) +"/" + papFilePathTable.get(papFilePath_ID) + ".pap\r\n";
                    filewriter.write(line);
                }

                line = "\r\n";
                filewriter.write(line);
                line = "papTable2_ID,charaID,charaType,Animation_ID,papTable3_ID,GeneratePath\r\n";
                filewriter.write(line);

                //ここから108バイトのテーブル(0xD3 = 211個)
                int papTable2_pos = bb.position();
                for(int papTable2_ID = 0; papTable2_ID < papTable2_Num; papTable2_ID++){
                    bb.position(papTable2_pos + 0x68 * papTable2_ID);
                    // chara/human/c0101/animation/a0033/bt_common/warp/warp_end.pap
                    //   charaType↑ ↑Character ID  ↑Animation_ID
                    int charaID = bb.getShort() & 0xFFFF;              //Character ID
                    short charaType = bb.getShort();            //charaType(0:human,1:monster,2:demihuman)
                    int Animation_ID = bb.getShort();         //Animation_ID
                    int papPathPattern_ID = bb.getShort();    //不明 papTable3関連？
                    //bb.getLong() ×12 ビットフラグっぽい値が並んでいる

                    String charaTypeSign;
                    String charaTypeFull;
                    String GeneratePath;
                    switch(charaType){
                        case 0:
                        case -1:
                            charaTypeSign = "c";
                            charaTypeFull = "human";
                            break;
                        case 1:
                            charaTypeSign = "m";
                            charaTypeFull = "monster";
                            break;
                        case 2:
                            charaTypeSign = "d";
                            charaTypeFull = "demihuman";
                            break;
                        case 3:
                            charaTypeSign = "w";
                            charaTypeFull = "weapon";
                            break;
                        default:
                            charaTypeSign = String.valueOf(charaType);
                            charaTypeFull = "unknown";
                    }
                    GeneratePath = String.format("chara/%s/%s%04d/animation/a%04d/", charaTypeFull, charaTypeSign, charaID,Animation_ID);

                    //記録に残す。
                    line = papTable2_ID + "," + charaID + "," + charaType + ","
                            + Animation_ID + "," + papPathPattern_ID  + "," + GeneratePath + "\r\n";
                    filewriter.write(line);
                }

                line = "\r\n";
                filewriter.write(line);
                line = "papTable3_ID,charaID,charaType,Animation_ID,papAnimationType,GeneratePath\r\n";
                filewriter.write(line);

                //ここから8バイトのpapヘッダーテーブル(0x51A3×4 = 20899個)
                bb.position(papTable2_pos + 0x68 * papTable2_Num);
                for(int papTable3_ID = 0; papTable3_ID < (papTable3_Num * 4); papTable3_ID++){
                    if (papAnimationTypeTablePos < bb.position()){
                        Utils.getGlobalLogger().info("バッファーオーバーフロー防止のため読み込み停止");
                        break;
                    }
                    // chara/human/c0101/animation/a0033/bt_common/warp/warp_end.pap
                    //   charaType↑ ↑Character ID  ↑Animation_ID
                    int charaID = bb.getShort() & 0xFFFF;          //Character ID
                    short charaType = bb.getShort();        //charaType(0:human,1:monster,2:demi)
                    int Animation_ID = bb.getShort();     //Animation_ID
                    int AnimationType_ID = bb.getShort();    //戦闘タイプテーブル先頭からのオフセット 例：0 → bt_common、0x0a → ot_m6000

                    String charaTypeSign;
                    String charaTypeFull;
                    String GeneratePath;
                    switch(charaType){
                        case 0:
                            charaTypeSign = "c";
                            charaTypeFull = "human";
                            break;
                        case 1:
                            charaTypeSign = "m";
                            charaTypeFull = "monster";
                            break;
                        case 2:
                            charaTypeSign = "d";
                            charaTypeFull = "demihuman";
                            break;
                        default:
                            charaTypeSign = String.valueOf(charaType);
                            charaTypeFull = "unknown";
                    }
                    String papAnimationTypeValue = papAnimationType.getOrDefault(AnimationType_ID, "Nothing("+AnimationType_ID+")");
                    GeneratePath = String.format("chara/%s/%s%04d/animation/a%04d/%s/", charaTypeFull, charaTypeSign, charaID,Animation_ID, papAnimationTypeValue);

                    //記録に残す。
                    line = papTable3_ID + "," + charaID + "," + charaType + ","
                            + Animation_ID + "," + papAnimationTypeValue + "," + GeneratePath + "\r\n";
                    filewriter.write(line);
                }

                filewriter.close();
                Utils.getGlobalLogger().info("PapLoadTable解析.csvを保存しました");

            } catch (FileNotFoundException e) {
                Utils.getGlobalLogger().error(e);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * アニメーション関係ハッシュ検索
     */
    @SuppressWarnings({"unused", "ConstantConditions"})
    public static void findAnimationPAP_Hashes() {
        Utils.getGlobalLogger().info("ActionTimeline.exhを開いています...");

        EXDF_View viewer;
        String papPath;
        byte[] pltData;
        ArrayList<String> ActionTimeline = new ArrayList<>();       //ActionTimelineリスト
        ArrayList<String> WeaponTimeLine = new ArrayList<>();       //WeaponTimeLineリスト

        SqPack_IndexFile index = SqPack_IndexFile.GetIndexFileForArchiveID("0a0000", true);
        SqPack_IndexFile index2 = SqPack_IndexFile.GetIndexFileForArchiveID("040000", true);
        byte[] exhData = index.extractFile("exd/ActionTimeline.exh");
        viewer = new EXDF_View(index, "exd/ActionTimeline.exh", new EXHF_File(exhData));
        for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
            String actionTimeLine = String.format("%s", viewer.getTable().getValueAt(i, 7));

            if (actionTimeLine == null || actionTimeLine.isEmpty() || actionTimeLine.contains("[SKL_ID]")) {
                continue;
            }
            ActionTimeline.add(actionTimeLine);
        }
        Utils.getGlobalLogger().info("ActionTimeline.exhを読み込みました");
        Utils.getGlobalLogger().info("WeaponTimeline.exhを開いています...");
        exhData = index.extractFile("exd/WeaponTimeline.exh");
        viewer = new EXDF_View(index, "exd/WeaponTimeline.exh", new EXHF_File(exhData));
        for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
            String weaponTimeLine = String.format("%s", viewer.getTable().getValueAt(i, 1));

            if (weaponTimeLine == null || weaponTimeLine.isEmpty()) {
                continue;
            }
            WeaponTimeLine.add(weaponTimeLine);
        }
        Utils.getGlobalLogger().info("WeaponTimeline.exhを読み込みました");

        pltData = index2.extractFile("chara/xls/animation/papLoadTable.plt");

        // chara/xls/animation/paploadtable.pltからpap関係の文字列取得
        ArrayList<String> BattleType = new ArrayList<>();       //bt_emp_emp..
        ArrayList<String> Craft_GatherType = new ArrayList<>(); //ギャザラー＆クラフター
        ArrayList<String> CraftType = new ArrayList<>();        //bt_alc_emp..
        ArrayList<String> GatherType = new ArrayList<>();       //bt_alc_emp..
        ArrayList<String> BattleType_Demi = new ArrayList<>();       //bt_emp_emp..
        ArrayList<String> MountAll = new ArrayList<>();         //mt_d0001..
        ArrayList<String> OrnamentAll = new ArrayList<>();      //ot_m6000...
        ArrayList<String> papTableString = new ArrayList<>();   //ability/2ax_warrior/abl024 等

        int bt_Flag = 0;
        ByteBuffer bb = ByteBuffer.wrap(pltData);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.getShort(); //テーブル数 0x0968 = 2408 1テーブル8バイト
        bb.getShort(); //不明 0xD3  0x68(104)バイトのデータブロックの数
        bb.getInt(); //不明 0x51A3  0x20(32)バイトのデータブロックの数
        int papTablePos = bb.getInt(); //papTableの戦闘タイプテーブルの開始位置
        bb.getInt(); //papTableのファイルパステーブルの開始位置

        bb.position(papTablePos); //ポインタをpapTableの先頭に移動
        // EOFに達するまで読みこむ
        do {
            StringBuilder papString;
            papString = new StringBuilder();
            while (true) {
                byte c = bb.get();
                if (c == 0) {
                    break;
                } else {
                    papString.append((char) c);
                }
            }

            if (!papString.toString().equals("bt_common") && !papString.toString().contains("*")) {
                if (papString.toString().startsWith("ot_")) {
                    OrnamentAll.add(papString.toString());
                } else if (papString.toString().startsWith("mt_")) {
                    MountAll.add(papString.toString());
                } else if (papString.toString().startsWith("bt_")) {
                    switch (papString.toString()) {
                        case "bt_alc_emp":
                            bt_Flag = 1;
                            break;
                        case "bt_fel_emp":
                            bt_Flag = 2;
                            break;
                        case "bt_2sp_sld":
                            bt_Flag = 3;
                            break;
                    }

                    if (bt_Flag == 0) {
                        BattleType.add(papString.toString());
                        BattleType_Demi.add(papString.toString());
                    } else if (bt_Flag == 1) {
                        Craft_GatherType.add(papString.toString());
                        CraftType.add(papString.toString());
                    } else if (bt_Flag == 2) {
                        Craft_GatherType.add(papString.toString());
                        GatherType.add(papString.toString());
                    } else {
                        BattleType_Demi.add(papString.toString());
                    }
                } else {
                    papTableString.add(papString.toString());
                }
            }

        } while (bb.position() < bb.capacity());

        HashDatabase.beginConnection();
        HashDatabase.setAutoCommit(false);

        int[] charaID = new int[]{101,201,301,401,501,601,701,801,901,1001,1101,1201,1301,1401,1501,1801};
        int[] npc_charaID = new int[]{104,204,504,604,704,804,1304,1404,9104,9204};

        //noinspection ConstantConditions
        if (false){
            //paploadtable.pltのタイムラインテーブルを登録してみる
            for (String actionTimeLine : papTableString){
                //タイムライン登録
                String tmbPath = "chara/action/" + actionTimeLine + ".tmb";
                cAddPathToDB(tmbPath, "040000");
            }

            //ActionTimeline.exhから読み込んだものを登録してみる
            // TODO: 処理が重いので分けた方が良さそう
            for (String actionTimeLine : ActionTimeline) {
                //タイムライン登録
                String tmbPath = "chara/action/" + actionTimeLine + ".tmb";
                cAddPathToDB(tmbPath, "040000");

                boolean regFlag_demi = false; //pap登録フラグ(demihuman)
                boolean regFlag_mon = false; //pap登録フラグ(monster)

                if(actionTimeLine.startsWith("cr_mon/") || actionTimeLine.startsWith("gs_roulette/")
                        || actionTimeLine.startsWith("trans_sp/")){
                    //monsterのみ(charaID固定)
                    //cr_mon/m0016/cr_mon001
                    //gs_roulette/m0078/mon_sp001
                    //chara/monster/m0016/animation/a0001/bt_common/cr_mon/m0016/cr_mon001.pap
                    String[] monsterID = actionTimeLine.split("/");
                    papPath = String.format("chara/monster/%s/animation/a0001/bt_common/", monsterID[1]) + actionTimeLine + ".pap";
                    cAddPathToDB(papPath, "040000");
                }else if(actionTimeLine.startsWith("magic/") || actionTimeLine.startsWith("specialdead/")){
                    //human(c0101)かつa0001のみ
                    papPath = "chara/human/c0101/animation/a0001/bt_common/" + actionTimeLine + ".pap";
                    cAddPathToDB(papPath, "040000");
                }else if(actionTimeLine.startsWith("buddy/")){
                    //demihuman(d0001)のみ
                    //chara/demihuman/d0001/animation/a0001/bt_common/buddy/glad.pap
                    papPath = String.format("chara/demihuman/d%04d/animation/a0001/bt_common/", 1) + actionTimeLine + ".pap";
                    cAddPathToDB(papPath, "040000");
                }else if(actionTimeLine.startsWith("bnpc_passmove/") || actionTimeLine.startsWith("lcut/") ){
                    //demihumanとmonsterのみ
                    regFlag_demi = true;
                    regFlag_mon = true;
                }else if(actionTimeLine.startsWith("change/") || actionTimeLine.startsWith("eyelid/")
                        || actionTimeLine.startsWith("facial/ ") || actionTimeLine.startsWith("flying_mount/")
                        || actionTimeLine.startsWith("mon_sp/gimmick/") || actionTimeLine.startsWith("overlay/")
                        || actionTimeLine.startsWith("partsbreak/") || actionTimeLine.startsWith("speak/")
                        || actionTimeLine.startsWith("special/") || actionTimeLine.startsWith("status/")
                        || actionTimeLine.startsWith("swim_mount/")){
                    //pap登録無し
                    continue;
                }else {
                    for (int id : charaID) {
                        if (actionTimeLine.startsWith("ability/") || actionTimeLine.startsWith("battle/")
                                || actionTimeLine.startsWith("emote/") || actionTimeLine.startsWith("event/")
                                || actionTimeLine.startsWith("event_base/") || actionTimeLine.startsWith("human_sp/")
                                || actionTimeLine.startsWith("idle_sp/") || actionTimeLine.startsWith("mount_sp/")
                                || actionTimeLine.startsWith("normal/") || actionTimeLine.startsWith("specialpop/")
                                || actionTimeLine.startsWith("resident/") || actionTimeLine.startsWith("warp/")) {
                            if (actionTimeLine.startsWith("human_sp/c")){
                                //human_sp/c0101/human_sp001←こんなデータの時
                                String[] humanID = actionTimeLine.split("/");
                                if(!humanID[1].equals(String.format("c%04d", id))){
                                    //「c0101」の部分とcharaIDが一致しなければ以降の処理をしない
                                    continue;
                                }
                            }

                            for (int Animation_ID = 1; Animation_ID < 150; Animation_ID++) {
                                if (Animation_ID == 50) { //2020/4/27現在46まで
                                    Animation_ID = 100;
                                }
                                //chara/human/c0101/animation/a0001/bt_common/ability/2ax_warrior
                                papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/", id, Animation_ID) + actionTimeLine + ".pap";
                                cAddPathToDB(papPath, "040000");

                                if (actionTimeLine.startsWith("emote/") || actionTimeLine.startsWith("event/")
                                        || actionTimeLine.startsWith("resident/")){
                                    //ジョブ固有アニメーション関係
                                    for(String battleType : BattleType){
                                        papPath = String.format("chara/human/c%04d/animation/a0001/%s/", id, battleType) + actionTimeLine + ".pap";
                                        cAddPathToDB(papPath, "040000");
                                    }
                                }
                            }
                        } else if (actionTimeLine.startsWith("blownaway/") || actionTimeLine.startsWith("bnpc_gesture/")
                                || actionTimeLine.startsWith("bnpc_gimmickjump/") || actionTimeLine.startsWith("cardgame/")
                                || actionTimeLine.startsWith("cr/") || actionTimeLine.startsWith("craft/")
                                || actionTimeLine.startsWith("diving_gather/") || actionTimeLine.startsWith("emote_ajust/")
                                || actionTimeLine.startsWith("emote_sp/") || actionTimeLine.startsWith("etherialwheel/")
                                || actionTimeLine.startsWith("eureka/") || actionTimeLine.startsWith("gs/")
                                || actionTimeLine.startsWith("inn_event/") || actionTimeLine.startsWith("limitbreak/")
                                || actionTimeLine.startsWith("mannequin_ft/") || actionTimeLine.startsWith("mon_sp/c")
                                || actionTimeLine.startsWith("mount/") || actionTimeLine.startsWith("music/")
                                || actionTimeLine.startsWith("ornament/") || actionTimeLine.startsWith("pc_contentsaction/")
                                || actionTimeLine.startsWith("rol_common/") || actionTimeLine.startsWith("season/")
                                || actionTimeLine.startsWith("weeklylot/") || actionTimeLine.startsWith("ws/")) {
                            //a0001しかないもの
                            if (actionTimeLine.startsWith("mon_sp/c") && !actionTimeLine.startsWith("mon_sp/common")){
                                //mon_sp/c0101/mon_sp001←こんなデータの時(多分なさそう)
                                String[] humanID = actionTimeLine.split("/");
                                if(!humanID[1].equals(String.format("c%04d", id))){
                                    //「c0101」の部分とcharaIDが一致しなければ以降の処理をしない
                                    continue;
                                }
                            }

                            if (actionTimeLine.startsWith("ws/")){
                                //ウェポンスキル関係
                                for(String battleType : BattleType){
                                    papPath = String.format("chara/human/c%04d/animation/a0001/%s/", id, battleType) + actionTimeLine + ".pap";
                                    cAddPathToDB(papPath, "040000");
                                }
                                continue;
                            }

                            papPath = String.format("chara/human/c%04d/animation/a0001/bt_common/", id) + actionTimeLine + ".pap";
                            cAddPathToDB(papPath, "040000");

                            if (actionTimeLine.startsWith("craft/")){
                                //クラフター追加
                                for(String battleType : CraftType){
                                    papPath = String.format("chara/human/c%04d/animation/a0001/%s/", id, battleType) + actionTimeLine + ".pap";
                                    cAddPathToDB(papPath, "040000");
                                }
                            }else if (actionTimeLine.startsWith("diving_gather/")){
                                //ギャザラー追加
                                for(String battleType : GatherType){
                                    papPath = String.format("chara/human/c%04d/animation/a0001/%s/", id, battleType) + actionTimeLine + ".pap";
                                    cAddPathToDB(papPath, "040000");
                                }
                            }else if (actionTimeLine.startsWith("ornament/")){
                                //ファッションアクセサリ
                                for(String battleType : OrnamentAll){
                                    papPath = String.format("chara/human/c%04d/animation/a0001/%s/", id, battleType) + actionTimeLine + ".pap";
                                    cAddPathToDB(papPath, "040000");
                                }
                            }
                        } else if (actionTimeLine.startsWith("fishing/") || actionTimeLine.startsWith("fishing_chair/")
                                || actionTimeLine.startsWith("harpoon/")) {
                            //漁道具装備時のみ
                            papPath = String.format("chara/human/c%04d/animation/a0001/bt_fsh_emp/", id) + actionTimeLine + ".pap";
                            cAddPathToDB(papPath, "040000");
                        }else if (actionTimeLine.startsWith("gather/")){
                            for(String battleType : GatherType){
                                papPath = String.format("chara/human/c%04d/animation/a0001/%s/", id, battleType) + actionTimeLine + ".pap";
                                cAddPathToDB(papPath, "040000");
                            }
                        }
                    }
                }

                if (regFlag_mon || actionTimeLine.startsWith("ability/") || actionTimeLine.startsWith("emote/")
                        || actionTimeLine.startsWith("event/") || actionTimeLine.startsWith("event_base/")
                        || actionTimeLine.startsWith("idle_sp/") || actionTimeLine.startsWith("lcut/")
                        || actionTimeLine.startsWith("loop_sp/") || actionTimeLine.startsWith("minion/")
                        || actionTimeLine.startsWith("mon_sp/common/") || actionTimeLine.startsWith("mon_sp/m")
                        || actionTimeLine.startsWith("mount_sp/m") || actionTimeLine.startsWith("normal/")
                        || actionTimeLine.startsWith("parts_idle_sp/") || actionTimeLine.startsWith("pc_contentsaction/")
                        || actionTimeLine.startsWith("resident/") || actionTimeLine.startsWith("season/")
                        || actionTimeLine.startsWith("specialpop/") || actionTimeLine.startsWith("warp/")){
                    //Monster関係
                    int searchMonsterNum = 9999;

                    for (int id = 1; id < searchMonsterNum; id++) {
                        //欠番部分を読み飛ばす
                        if (id == 1000){ //2020/4/27現在729までしかないが将来的に1000より増えるかも
                            id = 5010;
                        } else if (id == 5030){ //2020/4/27現在5019しかない
                            id = 6001;
                        } else if (id == 6005){ //2020/4/27現在6001,6002しかない
                            id = 7001;
                        } else if (id == 7010){ //召喚獣関係：2020/4/27現在7008まで
                            id = 7100;
                        } else if (id == 7110){ //機工士オートタレット・オートマトン関係：2020/4/27現在7106まで
                            id = 8001;
                        } else if (id == 8500){ //ミニオン関係：2020/4/27現在8378まで
                            //m9002～9009もあるがresident以外はなさそう
                            id = 9991; //設置物？：2020/4/27現在9998まで
                        }

                        if (actionTimeLine.startsWith("mon_sp/m") || actionTimeLine.startsWith("mount_sp/m")){
                            //mon_sp/m0001/mon_sp001←こんなデータの時
                            String[] monID = actionTimeLine.split("/");
                            if(!monID[1].equals(String.format("m%04d", id))){
                                //「m0001」の部分とcharaIDが一致しなければ以降の処理をしない
                                continue;
                            }
                        }

                        for (int Animation_ID = 1; Animation_ID < 150; Animation_ID++) {
                            if (Animation_ID == 10) { //2020/4/27現在9,99～132まで
                                Animation_ID = 99;
                            }

                            papPath = String.format("chara/monster/m%04d/animation/a%04d/bt_common/", id, Animation_ID) + actionTimeLine + ".pap";
                            cAddPathToDB(papPath, "040000");
                        }
                    }
                }

                if (regFlag_demi || actionTimeLine.startsWith("ability/") || actionTimeLine.startsWith("emote/")
                        || actionTimeLine.startsWith("event/") || actionTimeLine.startsWith("event_base/")
                        || actionTimeLine.startsWith("idle_sp/") || actionTimeLine.startsWith("lcut/")
                        || actionTimeLine.startsWith("mon_sp/common/") || actionTimeLine.startsWith("mon_sp/d")
                        || actionTimeLine.startsWith("mount_sp/d") || actionTimeLine.startsWith("normal/")
                        || actionTimeLine.startsWith("resident/") || actionTimeLine.startsWith("season/")
                        || actionTimeLine.startsWith("specialpop/") || actionTimeLine.startsWith("warp/")
                        || actionTimeLine.startsWith("ws/")){
                    //DemiHuman関係
                    int searchDemiHumanNum = 1100; //2020/4/27現在1059までしかないが将来的に1100より増えるかも

                    for (int id = 1; id < searchDemiHumanNum; id++) {
                        //欠番部分を読み飛ばす
                        if (id == 3){ //2020/4/27現在1,2 1001～1059まで
                            id = 1000;
                        }

                        if (actionTimeLine.startsWith("mon_sp/d") || actionTimeLine.startsWith("mount_sp/d")){
                            //mon_sp/d0001/mon_sp001←こんなデータの時
                            String[] demiID = actionTimeLine.split("/");
                            if(!demiID[1].equals(String.format("d%04d", id))){
                                //「d0001」の部分とcharaIDが一致しなければ以降の処理をしない
                                continue;
                            }
                        }

                        if (actionTimeLine.startsWith("emote/") || actionTimeLine.startsWith("event/")
                                || actionTimeLine.startsWith("resident/") || actionTimeLine.startsWith("ws/")){
                            //ウェポンスキル関係
                            for(String battleType : BattleType_Demi){
                                papPath = String.format("chara/demihuman/d%04d/animation/a0001/%s/", id, battleType) + actionTimeLine + ".pap";
                                cAddPathToDB(papPath, "040000");
                            }
                            if (actionTimeLine.startsWith("ws/")){
                                continue;
                            }
                        }

                        for (int Animation_ID = 1; Animation_ID < 150; Animation_ID++) {
                            if (Animation_ID == 10) { //2020/4/27現在1,2,102,117のみ
                                Animation_ID = 100;
                            }

                            papPath = String.format("chara/demihuman/d%04d/animation/a%04d/bt_common/", id, Animation_ID) + actionTimeLine + ".pap";
                            cAddPathToDB(papPath, "040000");
                        }
                    }
                }

                if (actionTimeLine.startsWith("event/") || actionTimeLine.startsWith("resident/")){
                    //weapon関係
                    //chara/weapon/w9022/animation/a0001/bt_common/event/event_sensor_open.pap
                    int searchWeaponNum = 9999; //2020/4/27現在1059までしかないが将来的に1100より増えるかも

                    for (int id = 101; id < searchWeaponNum; id++) {
                        //欠番部分を読み飛ばす
                        if(id <= 5000){
                            if (id == 120) { //盾：2020/4/27現在111まで
                                id = 201;
                            } else if (id == 230) { //片手剣：2020/4/27現在215まで
                                id = 301;
                            } else if (id == 340) { //格闘武器・クロー系(右)：2020/4/27現在333まで
                                id = 351;
                            } else if (id == 390) { //格闘武器・クロー系(左)：2020/4/27現在383まで
                                id = 401;
                            } else if (id == 430) { //両手斧：2020/4/27現在414まで
                                id = 501;
                            } else if (id == 540) { //両手槍：2020/4/27現在523まで
                                id = 551;
                            } else if (id == 510) { //旗：2020/4/27現在551のみ
                                id = 601;
                            } else if (id == 650) { //弓：2020/4/27現在628まで
                                id = 691;
                            } else if (id == 699) { //楽器：691～697、矢筒：698のみ 2020/4/27現在698まで
                                id = 701;
                            } else if (id == 710) { //片手幻具：2020/4/27現在701まで
                                id = 801;
                            } else if (id == 830) { //両手幻具：2020/4/27現在824まで
                                id = 901;
                            } else if (id == 910) { //片手呪具：2020/4/27現在902まで
                                id = 1001;
                            } else if (id == 1030) { //両手呪具：2020/4/27現在1024まで
                                id = 1101;
                            } else if (id == 1102) { //片手杖？：2020/4/27現在1101のみ
                                id = 1201;
                            } else if (id == 1202) { //両手杖？蛮族用かな？：2020/4/27現在1201のみ
                                id = 1301;
                            } else if (id == 1310) { //銃：2020/4/27現在1303まで
                                id = 1353;
                            } else if (id == 1354) { //銃：2020/4/27現在1353まで
                                id = 1401;
                            } else if (id == 1402) { //投擲物？：2020/4/27現在1401のみ
                                id = 1501;
                            } else if (id == 1520) { //両手剣：2020/4/27現在1515まで
                                id = 1551;
                            } else if (id == 1552) { //鞘(両手剣)：2020/4/27現在1551のみ
                                id = 1599;
                            } else if (id == 1600) { //黒い剣状の何か：2020/4/27現在1599のみ
                                id = 1601;
                            } else if (id == 1610) { //格闘武器・ナックル系(右)：2020/4/27現在1608まで
                                id = 1651;
                            } else if (id == 1660) { //格闘武器・ナックル系(左)：2020/4/27現在1658まで
                                id = 1701;
                            } else if (id == 1750) { //魔道書：2020/4/27現在1744まで
                                id = 1799;
                            } else if (id == 1800) { //はねペン：2020/4/27現在1799のみ
                                id = 1801;
                            } else if (id == 1820) { //双剣(右)：2020/4/27現在1814まで
                                id = 1848;
                            } else if (id == 1850) { //双剣(左) ：2020/4/27現在1849まで
                                id = 1851;
                            } else if (id == 1870) { //ナイフ？ ：2020/4/27現在1864まで
                                id = 1898;
                            } else if (id == 1900) { //武器？：2020/4/27現在1899まで
                                id = 1901;
                            } else if (id == 1915) { //ディアディム関係？：2020/4/27現在1910まで
                                id = 2001;
                            } else if (id == 2030) { //機工士の銃：2020/4/27現在2024まで
                                id = 2099;
                            } else if (id == 2100) { //機工士の銃の弾倉：2020/4/27現在2099まで
                                id = 2101;
                            } else if (id == 2130) { //天球儀：2020/4/27現在2123まで
                                id = 2199;
                            } else if (id == 2200) { //天球儀・サブアーム：2020/4/27現在2199まで
                                id = 2201;
                            } else if (id == 2220) { //刀：2020/4/27現在2213まで
                                id = 2251;
                            } else if (id == 2260) { //刀の鞘：2020/4/27現在2255まで
                                id = 2301;
                            } else if (id == 2310) { //細剣：2020/4/27現在2307まで
                                id = 2351;
                            } else if (id == 2380) { //赤魔道士のサブアーム(魔法のクリスタル？)：2020/4/27現在2372まで
                                id = 2401;
                            } else if (id == 2402) { //青魔器(青魔道士)：2020/4/27現在2401まで
                                id = 2501;
                            } else if (id == 2510) { //ガンブレード：2020/4/27現在2506まで
                                id = 2601;
                            } else if (id == 2610) { //投擲武器・右(踊り子)：2020/4/27現在2606まで
                                id = 2651;
                            } else if (id == 2660) { //投擲武器・左(踊り子)：2020/4/27現在2656まで
                                id = 5001;
                            }
                            //新規ジョブは2700番台、2800番台になると思われる
                        }
                        else if(id <= 6000){
                            //ここから先クラフター道具
                            if (id == 5010) { //木工道具(主道具 のこぎり)：2020/4/27現在5006まで
                                id = 5041;
                            } else if (id == 5042) { //木工道具(副道具 金槌)：2020/4/27現在5041のみ
                                id = 5061;
                            } else if (id == 5062) { //木工道具(副道具? のみ)：2020/4/27現在5061のみ
                                id = 5081;
                            } else if (id == 5082) { //木工道具(作業台)：2020/4/27現在5081のみ
                                id = 5101;
                            } else if (id == 5110) { //鍛冶道具(主道具)：2020/4/27現在5102まで
                                id = 5121;
                            } else if (id == 5122) { //鍛冶道具(未使用？)：2020/4/27現在5121のみ
                                id = 5141;
                            } else if (id == 5142) { //鍛冶道具(副道具)：2020/4/27現在5141のみ
                                id = 5181;
                            } else if (id == 5182) { //鍛冶道具(作業台)：2020/4/27現在5181のみ
                                id = 5201;
                            } else if (id == 5210) { //甲冑道具(主道具)：2020/4/27現在5202まで
                                id = 5241;
                            } else if (id == 5242) { //甲冑道具(副道具)：2020/4/27現在5241のみ
                                id = 5281;
                            } else if (id == 5283) { //甲冑道具(作業台)：2020/4/27現在5282まで
                                id = 5301;
                            } else if (id == 5302) { //彫金道具(主道具)：2020/4/27現在5301のみ
                                id = 5321;
                            } else if (id == 5323) { //彫金道具(副道具？)：2020/4/27現在5322まで
                                id = 5341;
                            } else if (id == 5342) { //彫金道具(副道具)：2020/4/27現在5341のみ
                                id = 5361;
                            } else if (id == 5362) { //彫金道具(作業台・副)：2020/4/27現在5361のみ
                                id = 5381;
                            } else if (id == 5382) { //彫金道具(作業台・主)：2020/4/27現在5381のみ
                                id = 5401;
                            } else if (id == 5405) { //革細工道具(主道具)：2020/4/27現在5404まで
                                id = 5441;
                            } else if (id == 5442) { //革細工道具(副道具)：2020/4/27現在5441のみ
                                id = 5461;
                            } else if (id == 5462) { //革細工道具(副道具？すきべら)：2020/4/27現在5461のみ
                                id = 5481;
                            } else if (id == 5482) { //革細工道具(作業台・主)：2020/4/27現在5481のみ
                                id = 5501;
                            } else if (id == 5503) { //裁縫道具(主道具 刺繍台)：2020/4/27現在5502まで
                                id = 5521;
                            } else if (id == 5522) { //裁縫道具(主道具 針)：2020/4/27現在5521のみ
                                id = 5561;
                            } else if (id == 5562) { //裁縫道具(副道具 スピニングホイール)：2020/4/27現在5561のみ
                                id = 5571;
                            } else if (id == 5572) { //裁縫道具(副道具)：2020/4/27現在5571のみ
                                id = 5601;
                            } else if (id == 5605) { //錬金道具(主道具 フラスコ？)：2020/4/27現在5604まで
                                id = 5641;
                            } else if (id == 5642) { //錬金道具(副道具 薬研車)：2020/4/27現在5641のみ
                                id = 5661;
                            } else if (id == 5662) { //錬金道具(副道具 薬研)：2020/4/27現在5661のみ
                                id = 5681;
                            } else if (id == 5682) { //錬金道具(主道具 蒸留装置)：2020/4/27現在5681のみ
                                id = 5701;
                            } else if (id == 5703) { //調理道具(主道具 フライパン)：2020/4/27現在5702まで
                                id = 5721;
                            } else if (id == 5723) { //調理道具(主道具 コンロ)：2020/4/27現在5722まで
                                id = 5741;
                            } else if (id == 5742) { //調理道具(副道具 ナイフ)：2020/4/27現在5741のみ
                                id = 5781;
                            } else if (id == 5782) { //調理道具(副道具 調理台)：2020/4/27現在5781のみ
                                id = 7001;
                            }
                        }
                        else if(id <= 8000){
                            //ここからギャザラー道具
                            if (id == 7004) { //採掘道具(主道具)：2020/4/27現在7003まで
                                id = 7051;
                            } else if (id == 7052) { //採掘道具(副道具)：2020/4/27現在7051のみ
                                id = 7101;
                            } else if (id == 7103) { //園芸道具(主道具)：2020/4/27現在7102まで
                                id = 7151;
                            } else if (id == 7152) { //園芸道具(副道具)：2020/4/27現在7151のみ
                                id = 7201;
                            } else if (id == 7202) { //漁道具(主道具)：2020/4/27現在7201のみ
                                id = 7151;
                            } else if (id == 7256) { //漁道具(副道具)：2020/4/27現在7251,7255のみ
                                id = 7281;
                            } else if (id == 7282) { //漁道具(主道具 椅子)：2020/4/27現在7281のみ
                                id = 8001;
                            }
                        }
                        else if(id <= 9000){
                            if (id == 8050) { //NPC専用武器？：2020/4/27現在8037まで
                                id = 9001;
                            }
                        }
                        else{
                            if (id == 9090) { //カットシーン用 小道具・キャラクター：2020/4/27現在9074まで
                                id = 9101;
                            } else if (id == 9110) { //補助キャラクター：2020/4/27現在9102まで
                                //9101:妖精、9102:オートタレット？
                                id = 9801;
                            } else if (id == 9810) { //ボーンのみ？：2020/4/27現在9801のみ
                                id = 9901;
                            } else if (id == 9902) { //食べ物：2020/4/27現在9901のみ
                                break;
                            }
                        }

                        for (int Animation_ID = 1; Animation_ID < 150; Animation_ID++) {
                            if (Animation_ID == 10) { //2020/4/27現在1,2,102,117のみ
                                Animation_ID = 100;
                            }

                            papPath = String.format("chara/weapon/w%04d/animation/a%04d/bt_common/", id, Animation_ID) + actionTimeLine + ".pap";
                            cAddPathToDB(papPath, "040000");
                        }
                    }
                }
                //ここまでActionTimeline.exh読み込みループ
            }
        }

        //Monster関係
        if (IsDebug){
            int searchMonsterNum = 9999;

            //action/mon_sp/m????登録
            for (int id = 1; id < searchMonsterNum; id++) {
                //欠番部分を読み飛ばす
                if (id == 1000){ //2020/4/27現在729までしかないが将来的に1000より増えるかも
                    id = 5010;
                } else if (id == 5030){ //2020/4/27現在5019しかない
                    id = 6001;
                } else if (id == 6005){ //2020/4/27現在6001,6002しかない
                    id = 7001;
                } else if (id == 7010){ //召喚獣関係：2020/4/27現在7008まで
                    id = 7100;
                } else if (id == 7110){ //機工士オートタレット・オートマトン関係：2020/4/27現在7106まで
                    id = 8001;
                } else if (id == 8500){ //ミニオン関係：2020/4/27現在8378まで
                    //m9002～9009もあるがタイムラインはなさそう
                    id = 9991; //設置物？：2020/4/27現在9998まで
                }

                if (false) {
                    String tmbPath;
                    tmbPath = String.format("chara/action/mon_sp/m%04d/mon_sp001.tmb", id);
                    cAddPathToDB(tmbPath, "040000");
                    tmbPath = String.format("chara/action/mon_sp/m%04d/hide/mon_sp001.tmb", id);
                    cAddPathToDB(tmbPath, "040000");
                    tmbPath = String.format("chara/action/mon_sp/m%04d/show/mon_sp001.tmb", id);
                    cAddPathToDB(tmbPath, "040000");

                    //Animationとは関係ないが軽い処理なのでついでに
                    tmbPath = String.format("hara/xls/attachoffset/m%04d.atch", id);
                    cAddPathToDB(tmbPath, "040000");

                    if (id <= 1000) { //2020/4/27現在1059までしかないが将来的に1000より増えるかも
                        tmbPath = String.format("chara/action/mount_sp/m%04d/mon_sp001.tmb", id);
                        cAddPathToDB(tmbPath, "040000");
                        tmbPath = String.format("chara/action/mount_sp/m%04d/hide/mon_sp001.tmb", id);
                        cAddPathToDB(tmbPath, "040000");
                        tmbPath = String.format("chara/action/mount_sp/m%04d/show/mon_sp001.tmb", id);
                        cAddPathToDB(tmbPath, "040000");
                    }
                }

                //chara/monster/m0692/animation/s0001/body/material.pap
                //chara/monster/m0724/animation/a0001/bt_common/warp/warp_end.pap
                //chara/monster/m0215/animation/a0003/bt_common/resident/monster.pap
                //chara/monster/m0715/animation/a0001/bt_common/specialpop/specialpop.pap
                papPath = String.format("chara/monster/m%04d/animation/a0001/bt_common/idle_sp/normal_idle_sp_1.pap", id);
                cAddPathToDB(papPath, "040000");

                //テクスチャ総当たり検索(処理が重いので非推奨)
                if (false){
                    Utils.getGlobalLogger().info(String.format("テクスチャ検索(monster m%04d)", id));
                    for (int Body_ID = 1; Body_ID < 12; Body_ID++) {
                        //chara/monster/m0563/obj/body/b0001/texture/v01_m0563b0001_d.tex
                        papPath = String.format("chara/monster/m%04d/obj/body/b%04d/texture/v01_m%04db%04d_d.tex", id, Body_ID, id, Body_ID);
                        if (index2.findFile(papPath) >= 1) {
                            cAddPathToDB(papPath, "040000", 2);
                            papPath = String.format("chara/monster/m%04d/obj/body/b%04d/texture/v01_m%04db%04d_n.tex", id, Body_ID, id, Body_ID);
                            cAddPathToDB(papPath, "040000", 2);
                            papPath = String.format("chara/monster/m%04d/obj/body/b%04d/texture/v01_m%04db%04d_s.tex", id, Body_ID, id, Body_ID);
                            cAddPathToDB(papPath, "040000", 2);
                            papPath = String.format("chara/monster/m%04d/obj/body/b%04d/texture/v01_m%04db%04d_m.tex", id, Body_ID, id, Body_ID);
                            cAddPathToDB(papPath, "040000", 2);
                        }
                    }

                }
            }
        }

        //DemiHuman関係
        //noinspection ConstantConditions
        if (false){
            int searchMonsterNum = 1100; //2020/4/27現在1059までしかないが将来的に1100より増えるかも

            //action/mon_sp/d????登録
            for (int i = 1; i < searchMonsterNum; i++) {
                //欠番部分を読み飛ばす
                if (i == 3){ //2020/4/27現在2まで
                    i = 1000;
                }

                String tmbPath;
                tmbPath = String.format("chara/action/mon_sp/d%04d/mon_sp001.tmb",i);
                cAddPathToDB(tmbPath, "040000");
                tmbPath = String.format("chara/action/mon_sp/d%04d/hide/mon_sp001.tmb",i);
                cAddPathToDB(tmbPath, "040000");
                tmbPath = String.format("chara/action/mon_sp/d%04d/show/mon_sp001.tmb",i);
                cAddPathToDB(tmbPath, "040000");

                tmbPath = String.format("chara/action/mount_sp/d%04d/mon_sp001.tmb",i);
                cAddPathToDB(tmbPath, "040000");
                tmbPath = String.format("chara/action/mount_sp/d%04d/hide/mon_sp001.tmb",i);
                cAddPathToDB(tmbPath, "040000");
                tmbPath = String.format("chara/action/mount_sp/d%04d/show/mon_sp001.tmb",i);
                cAddPathToDB(tmbPath, "040000");

                //Animationとは関係ないが軽い処理なのでついでに
                tmbPath = String.format("hara/xls/attachoffset/m%04d.atch",i);
                cAddPathToDB(tmbPath, "040000");


                //chara/demihuman/d1045/animation/a0001/bt_common/idle_sp/idle_sp_1.pap
            }

            if(false) {
                //Demihumanスケルトン関係
                byte[] awtData = index2.extractFile("chara/xls/animation/animation_work_table-demihuman.awt");

                bb = ByteBuffer.wrap(awtData);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.getInt(); //twa
                bb.getShort(); //不明 0x300
                bb.getShort(); //不明 0x100
                short awTable_Num = bb.getShort(); //テーブル数 0x02a6 = 678 1テーブル8バイト

                bb.position(0x18); //ポインタをawTableの先頭に移動

                //ここから8バイトのpapヘッダーテーブル(0x51A3×4 = 20899個)
                for(int awTable_ID = 0; awTable_ID < awTable_Num; awTable_ID++){
                    if ((bb.limit() - 7) < bb.position()){
                        Utils.getGlobalLogger().info("バッファーオーバーフロー防止のため読み込み停止");
                        break;
                    }
                    int charaID2 = bb.getShort() & 0xFFFF;          //Character ID
                    short charaType = bb.getShort();        //charaType(0:human,1:monster,2:demi)
                    bb.get();     //AnimationType_ID
                    bb.get();     //Animation_ID
                    bb.getShort();    //不明

                    String charaTypeSign;
                    String charaTypeFull;
                    String fullPath;
                    switch(charaType){
                        case 0:
                            charaTypeSign = "c";
                            charaTypeFull = "human";
                            break;
                        case 1:
                            charaTypeSign = "m";
                            charaTypeFull = "monster";
                            break;
                        case 2:
                            charaTypeSign = "d";
                            charaTypeFull = "demihuman";
                            break;
                        case 3:
                            charaTypeSign = "w";
                            charaTypeFull = "weapon";
                            break;
                        default:
                            charaTypeSign = String.valueOf(charaType);
                            charaTypeFull = "unknown";
                    }

                    for (String battleType : BattleType) {
                        papPath = String.format("chara/%s/%s%04d/animation/a0001/%s/resident/demihuman.pap", charaTypeFull, charaTypeSign, charaID2, battleType);
                        cAddPathToDB(papPath, "040000");
                    }
                }
                //chara/demihuman/d1034/animation/a0001/bt_2ax_emp/resident/demihuman.pap
            }
        }

        //Human関係
        //noinspection ConstantConditions
        if (false){
            //特定用
            if (false) {
                for (int Animation_ID = 1; Animation_ID < 9999; Animation_ID++) {
                    //chara/monster/m0080/animation/a0001/bt_common/mon_sp/m0080/mon_sp001.pap
                    papPath = String.format("chara/monster/m0080/animation/a%04d/bt_common/mon_sp/m0080/show/mon_sp001.pap", Animation_ID);
                    //papPath = String.format("chara/human/c%04d/animation/a%04d/%s/human_sp/c%04d/human_sp037.pap", 201, Animation_ID, "bt_common", 201);
                    cAddPathToDB(papPath, "040000",2);
                }
            }

            if (true) {
                for (int id : charaID) {
                    if (false) {
                        //chara/action/mon_sp/c0101/hide/mon_sp001.tmb
                        String tmbPath = String.format("chara/action/mon_sp/c%04d/mon_sp001.tmb", id);
                        cAddPathToDB(tmbPath, "040000");
                        tmbPath = String.format("chara/action/mon_sp/c%04d/hide/mon_sp001.tmb", id);
                        cAddPathToDB(tmbPath, "040000");
                        tmbPath = String.format("chara/action/mon_sp/c%04d/show/mon_sp001.tmb", id);
                        cAddPathToDB(tmbPath, "040000");
                    }

                    //バトルアニメーション関係
                    //chara/human/c0601/animation/a0001/bt_2ax_emp/battle/auto_attack1.pap

                    for (int Animation_ID = 1; Animation_ID < 150; Animation_ID++) {
                        if (Animation_ID == 50) { //2020/4/27現在46まで
                            Animation_ID = 100;
                        }
                        // papバイナリでキャラIDを推測する方法
                        // endianとrootの文字列の間にc0101等のIDが出現することが多いので通常はそこから推測できる
                        // papバイナリをみてもID等不明な場合
                        // 0x0A short : charaID
                        // 0x0C short : charaType

                        //chara/human/c0601/animation/a0001/bt_common/warp/warp_end.pap
                        papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/warp/warp_end.pap", id, Animation_ID);
                        cAddPathToDB(papPath, "040000");

                        papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/resident/action.pap", id, Animation_ID);
                        cAddPathToDB(papPath, "040000");

                        papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/human_sp/c%04d/human_sp037.pap", id, Animation_ID, id);
                        cAddPathToDB(papPath, "040000");
                        //chara/human/c0101/animation/a0008/bt_common/specialpop/specialpop.pap
                        papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/specialpop/specialpop.pap", id, Animation_ID);
                        cAddPathToDB(papPath, "040000",2);


                        if (false) {
                            for (String battleType : BattleType) {
                                //オートアタック
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/battle/auto_attack1.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");

                                //戦いに備える？
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/emote/b_pose01_start.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");

                                //抜刀
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/resident/idle.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");

                                //イベント
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/event/event_bt_active.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");

                                //種族特有
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/human_sp/c%04d/human_sp001.pap", id, Animation_ID, battleType, id);
                                cAddPathToDB(papPath, "040000");

                                //chara/human/c0101/animation/a0001/bt_swd_sld/craft/repair.pap
                                //修理
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/craft/repair.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");

                                //ウェポンスキル関係
                                if (false) { //保留
                                    for (String battleType2 : BattleType) {
                                        //chara/human/c0101/animation/a0001/bt_2ax_emp/ws/bt_2ax_emp/wsh001.pap等
                                        papPath = String.format("chara/human/c%04d/animation/a%04d/%s/ws/%s/", id, Animation_ID, battleType, battleType2);
                                        HashDatabase.addFolderToDB(papPath, "040000");
                                    }
                                }
                            }

                            for (String battleType : Craft_GatherType) {
                                //基本(ギャザラーはresident/gather.pap)
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/resident/craft.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");

                                //制作開始
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/craft/start.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");

                                //採取開始
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/gather/start.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");
                                //潜水採取開始
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/diving_gather/start.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");

                                //釣り開始
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/fishing/cast_normal.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");
                                //釣り開始(椅子モード)
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/fishing_chair/cast_normal.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");
                                //刺突漁開始
                                papPath = String.format("chara/human/c%04d/animation/a%04d/%s/harpoon/swim_under_hr_start.pap", id, Animation_ID, battleType);
                                cAddPathToDB(papPath, "040000");
                            }
                        }
                        if (true) {
                            if (Animation_ID == 1 || (Animation_ID == 9 && id == 101)) {
                                //chara/human/c0101/animation/a0001/bt_common/mount/mount_start.pap
                                papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/mount/mount_start.pap", id, Animation_ID);
                                cAddPathToDB(papPath, "040000");

                                for (String mountID : MountAll) {
                                    //マウント関係
                                    //chara/human/c0101/animation/a0001/mt_d0001/resident/mount.pap
                                    papPath = String.format("chara/human/c%04d/animation/a%04d/%s/resident/mount.pap", id, Animation_ID, mountID);
                                    cAddPathToDB(papPath, "040000");
                                    //chara/human/c0101/animation/a0001/mt_m0482/human_sp/c0101/human_sp001.pap
                                    papPath = String.format("chara/human/c%04d/animation/a%04d/%s/human_sp/cc%04d/human_sp001.pap", id, Animation_ID, mountID, id);
                                    cAddPathToDB(papPath, "040000");
                                    //chara/human/c0101/animation/a0001/bt_common/mount_sp/d0002/mon_sp001.pap
                                    String mountID2 = mountID.replace("mt_", "");
                                    papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/mount_sp/%s/mon_sp001.pap", id, Animation_ID, mountID2);
                                    cAddPathToDB(papPath, "040000");
                                    papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/mount_sp/%s/hide/mon_sp001.pap", id, Animation_ID, mountID2);
                                    cAddPathToDB(papPath, "040000");
                                    papPath = String.format("chara/human/c%04d/animation/a%04d/bt_common/mount_sp/%s/show/mon_sp001.pap", id, Animation_ID, mountID2);
                                    cAddPathToDB(papPath, "040000");

                                }

                                for (String ornamentID : OrnamentAll) {
                                    //ファッションアクセサリ関係
                                    //chara/human/c0101/animation/a0001/ot_m6000/resident/ornament.pap
                                    papPath = String.format("chara/human/c%04d/animation/a%04d/%s/resident/ornament.pap", id, Animation_ID, ornamentID);
                                    cAddPathToDB(papPath, "040000");
                                }
                            }
                        }
                        //ここまでAnimation_IDループ
                    }
                }
            }
        }

        //weapon関係
        if (false){
            if (false) {
                for (String actionTimeLine : WeaponTimeLine) {
                    //タイムライン登録
                    String tmbPath = "chara/action/" + actionTimeLine + ".tmb";
                    cAddPathToDB(tmbPath, "040000");
                }
            }

            //chara/weapon/w9022/animation/a0001/bt_common/event/event_sensor_open.pap
            int searchWeaponNum = 9999; //2020/4/27現在1059までしかないが将来的に1100より増えるかも

            for (int id = 101; id < searchWeaponNum; id++) {
                //欠番部分を読み飛ばす
                if(id <= 5000){
                    if (id == 120) { //盾：2020/4/27現在111まで
                        id = 201;
                    } else if (id == 230) { //片手剣：2020/4/27現在215まで
                        id = 301;
                    } else if (id == 340) { //格闘武器・クロー系(右)：2020/4/27現在333まで
                        id = 351;
                    } else if (id == 390) { //格闘武器・クロー系(左)：2020/4/27現在383まで
                        id = 401;
                    } else if (id == 430) { //両手斧：2020/4/27現在414まで
                        id = 501;
                    } else if (id == 540) { //両手槍：2020/4/27現在523まで
                        id = 551;
                    } else if (id == 510) { //旗：2020/4/27現在551のみ
                        id = 601;
                    } else if (id == 650) { //弓：2020/4/27現在628まで
                        id = 691;
                    } else if (id == 699) { //楽器：691～697、矢筒：698のみ 2020/4/27現在698まで
                        id = 701;
                    } else if (id == 710) { //片手幻具：2020/4/27現在701まで
                        id = 801;
                    } else if (id == 830) { //両手幻具：2020/4/27現在824まで
                        id = 901;
                    } else if (id == 910) { //片手呪具：2020/4/27現在902まで
                        id = 1001;
                    } else if (id == 1030) { //両手呪具：2020/4/27現在1024まで
                        id = 1101;
                    } else if (id == 1102) { //片手杖？：2020/4/27現在1101のみ
                        id = 1201;
                    } else if (id == 1202) { //両手杖？蛮族用かな？：2020/4/27現在1201のみ
                        id = 1301;
                    } else if (id == 1310) { //銃：2020/4/27現在1303まで
                        id = 1353;
                    } else if (id == 1354) { //銃：2020/4/27現在1353まで
                        id = 1401;
                    } else if (id == 1402) { //投擲物？：2020/4/27現在1401のみ
                        id = 1501;
                    } else if (id == 1520) { //両手剣：2020/4/27現在1515まで
                        id = 1551;
                    } else if (id == 1552) { //鞘(両手剣)：2020/4/27現在1551のみ
                        id = 1599;
                    } else if (id == 1600) { //黒い剣状の何か：2020/4/27現在1599のみ
                        id = 1601;
                    } else if (id == 1610) { //格闘武器・ナックル系(右)：2020/4/27現在1608まで
                        id = 1651;
                    } else if (id == 1660) { //格闘武器・ナックル系(左)：2020/4/27現在1658まで
                        id = 1701;
                    } else if (id == 1750) { //魔道書：2020/4/27現在1744まで
                        id = 1799;
                    } else if (id == 1800) { //はねペン：2020/4/27現在1799のみ
                        id = 1801;
                    } else if (id == 1820) { //双剣(右)：2020/4/27現在1814まで
                        id = 1848;
                    } else if (id == 1850) { //双剣(左) ：2020/4/27現在1849まで
                        id = 1851;
                    } else if (id == 1870) { //ナイフ？ ：2020/4/27現在1864まで
                        id = 1898;
                    } else if (id == 1900) { //武器？：2020/4/27現在1899まで
                        id = 1901;
                    } else if (id == 1915) { //ディアディム関係？：2020/4/27現在1910まで
                        id = 2001;
                    } else if (id == 2030) { //機工士の銃：2020/4/27現在2024まで
                        id = 2099;
                    } else if (id == 2100) { //機工士の銃の弾倉：2020/4/27現在2099まで
                        id = 2101;
                    } else if (id == 2130) { //天球儀：2020/4/27現在2123まで
                        id = 2199;
                    } else if (id == 2200) { //天球儀・サブアーム：2020/4/27現在2199まで
                        id = 2201;
                    } else if (id == 2220) { //刀：2020/4/27現在2213まで
                        id = 2251;
                    } else if (id == 2260) { //刀の鞘：2020/4/27現在2255まで
                        id = 2301;
                    } else if (id == 2310) { //細剣：2020/4/27現在2307まで
                        id = 2351;
                    } else if (id == 2380) { //赤魔道士のサブアーム(魔法のクリスタル？)：2020/4/27現在2372まで
                        id = 2401;
                    } else if (id == 2402) { //青魔器(青魔道士)：2020/4/27現在2401まで
                        id = 2501;
                    } else if (id == 2510) { //ガンブレード：2020/4/27現在2506まで
                        id = 2601;
                    } else if (id == 2610) { //投擲武器・右(踊り子)：2020/4/27現在2606まで
                        id = 2651;
                    } else if (id == 2660) { //投擲武器・左(踊り子)：2020/4/27現在2656まで
                        id = 5001;
                    }
                    //新規ジョブは2700番台、2800番台になると思われる
                }
                else if(id <= 6000){
                    //ここから先クラフター道具
                    if (id == 5010) { //木工道具(主道具 のこぎり)：2020/4/27現在5006まで
                        id = 5041;
                    } else if (id == 5042) { //木工道具(副道具 金槌)：2020/4/27現在5041のみ
                        id = 5061;
                    } else if (id == 5062) { //木工道具(副道具? のみ)：2020/4/27現在5061のみ
                        id = 5081;
                    } else if (id == 5082) { //木工道具(作業台)：2020/4/27現在5081のみ
                        id = 5101;
                    } else if (id == 5110) { //鍛冶道具(主道具)：2020/4/27現在5102まで
                        id = 5121;
                    } else if (id == 5122) { //鍛冶道具(未使用？)：2020/4/27現在5121のみ
                        id = 5141;
                    } else if (id == 5142) { //鍛冶道具(副道具)：2020/4/27現在5141のみ
                        id = 5181;
                    } else if (id == 5182) { //鍛冶道具(作業台)：2020/4/27現在5181のみ
                        id = 5201;
                    } else if (id == 5210) { //甲冑道具(主道具)：2020/4/27現在5202まで
                        id = 5241;
                    } else if (id == 5242) { //甲冑道具(副道具)：2020/4/27現在5241のみ
                        id = 5281;
                    } else if (id == 5283) { //甲冑道具(作業台)：2020/4/27現在5282まで
                        id = 5301;
                    } else if (id == 5302) { //彫金道具(主道具)：2020/4/27現在5301のみ
                        id = 5321;
                    } else if (id == 5323) { //彫金道具(副道具？)：2020/4/27現在5322まで
                        id = 5341;
                    } else if (id == 5342) { //彫金道具(副道具)：2020/4/27現在5341のみ
                        id = 5361;
                    } else if (id == 5362) { //彫金道具(作業台・副)：2020/4/27現在5361のみ
                        id = 5381;
                    } else if (id == 5382) { //彫金道具(作業台・主)：2020/4/27現在5381のみ
                        id = 5401;
                    } else if (id == 5405) { //革細工道具(主道具)：2020/4/27現在5404まで
                        id = 5441;
                    } else if (id == 5442) { //革細工道具(副道具)：2020/4/27現在5441のみ
                        id = 5461;
                    } else if (id == 5462) { //革細工道具(副道具？すきべら)：2020/4/27現在5461のみ
                        id = 5481;
                    } else if (id == 5482) { //革細工道具(作業台・主)：2020/4/27現在5481のみ
                        id = 5501;
                    } else if (id == 5503) { //裁縫道具(主道具 刺繍台)：2020/4/27現在5502まで
                        id = 5521;
                    } else if (id == 5522) { //裁縫道具(主道具 針)：2020/4/27現在5521のみ
                        id = 5561;
                    } else if (id == 5562) { //裁縫道具(副道具 スピニングホイール)：2020/4/27現在5561のみ
                        id = 5571;
                    } else if (id == 5572) { //裁縫道具(副道具)：2020/4/27現在5571のみ
                        id = 5601;
                    } else if (id == 5605) { //錬金道具(主道具 フラスコ？)：2020/4/27現在5604まで
                        id = 5641;
                    } else if (id == 5642) { //錬金道具(副道具 薬研車)：2020/4/27現在5641のみ
                        id = 5661;
                    } else if (id == 5662) { //錬金道具(副道具 薬研)：2020/4/27現在5661のみ
                        id = 5681;
                    } else if (id == 5682) { //錬金道具(主道具 蒸留装置)：2020/4/27現在5681のみ
                        id = 5701;
                    } else if (id == 5703) { //調理道具(主道具 フライパン)：2020/4/27現在5702まで
                        id = 5721;
                    } else if (id == 5723) { //調理道具(主道具 コンロ)：2020/4/27現在5722まで
                        id = 5741;
                    } else if (id == 5742) { //調理道具(副道具 ナイフ)：2020/4/27現在5741のみ
                        id = 5781;
                    } else if (id == 5782) { //調理道具(副道具 調理台)：2020/4/27現在5781のみ
                        id = 7001;
                    }
                }
                else if(id <= 8000){
                    //ここからギャザラー道具
                    if (id == 7004) { //採掘道具(主道具)：2020/4/27現在7003まで
                        id = 7051;
                    } else if (id == 7052) { //採掘道具(副道具)：2020/4/27現在7051のみ
                        id = 7101;
                    } else if (id == 7103) { //園芸道具(主道具)：2020/4/27現在7102まで
                        id = 7151;
                    } else if (id == 7152) { //園芸道具(副道具)：2020/4/27現在7151のみ
                        id = 7201;
                    } else if (id == 7202) { //漁道具(主道具)：2020/4/27現在7201のみ
                        id = 7251;
                    } else if (id == 7256) { //漁道具(副道具)：2020/4/27現在7251,7255のみ
                        id = 7281;
                    } else if (id == 7282) { //漁道具(主道具 椅子)：2020/4/27現在7281のみ
                        id = 8001;
                    }
                }
                else if(id <= 9000){
                    if (id == 8050) { //NPC専用武器？：2020/4/27現在8037まで
                        id = 9001;
                    }
                }
                else{
                    if (id == 9090) { //カットシーン用 小道具・キャラクター：2020/4/27現在9074まで
                        id = 9101;
                    } else if (id == 9110) { //補助キャラクター：2020/4/27現在9102まで
                        //9101:妖精、9102:オートタレット？
                        id = 9801;
                    } else if (id == 9810) { //ボーンのみ？：2020/4/27現在9801のみ
                        id = 9901;
                    } else if (id == 9902) { //食べ物：2020/4/27現在9901のみ
                        break;
                    }
                }

                //テクスチャ総当たり検索(処理が重いので非推奨)
                if (false){
                    Utils.getGlobalLogger().info(String.format("テクスチャ検索(weapon w%04d)", id));
                    for (int Body_ID = 1; Body_ID < 330; Body_ID++) {
                        //chara/weapon/w0101/obj/body/b0001/texture/v01_w0101b0001_d.tex
                        papPath = String.format("chara/weapon/w%04d/obj/body/b%04d/texture/v01_w%04db%04d_d.tex", id, Body_ID, id, Body_ID);
                        if (index2.findFile(papPath) >= 1) {
                            cAddPathToDB(papPath, "040000", 2);
                            papPath = String.format("chara/weapon/w%04d/obj/body/b%04d/texture/v01_w%04db%04d_n.tex", id, Body_ID, id, Body_ID);
                            cAddPathToDB(papPath, "040000", 2);
                            papPath = String.format("chara/weapon/w%04d/obj/body/b%04d/texture/v01_w%04db%04d_s.tex", id, Body_ID, id, Body_ID);
                            cAddPathToDB(papPath, "040000", 2);
                            papPath = String.format("chara/weapon/w%04d/obj/body/b%04d/texture/v01_w%04db%04d_m.tex", id, Body_ID, id, Body_ID);
                            cAddPathToDB(papPath, "040000", 2);
                        }
                    }

                }

                if (false) {
                    for (int Animation_ID = 1; Animation_ID < 20; Animation_ID++) {
                        //Animation_S：2020/4/27現在1～15
                        //chara/weapon/w0104/animation/s0001/body/material.pap
                        papPath = String.format("chara/weapon/w%04d/animation/s%04d/body/material.pap", id, Animation_ID);
                        cAddPathToDB(papPath, "040000");

                        //Animation wp_common：2020/4/27現在1のみ
                        if (Animation_ID <= 2) {
                            //chara/weapon/w0507/animation/a0001/wp_common/resident/weapon.pap
                            papPath = String.format("chara/weapon/w%04d/animation/a%04d/wp_common/resident/weapon.pap", id, Animation_ID);
                            cAddPathToDB(papPath, "040000");

                            for (String actionTimeLine : WeaponTimeLine) {
                                //ヒットするかどうかは不明
                                papPath = String.format("chara/weapon/w%04d/animation/a%04d/wp_common/%s.pap", id, Animation_ID, actionTimeLine);
                                cAddPathToDB(papPath, "040000");
                            }
                        }
                    }
                }
            }
        }

        HashDatabase.commit();

        Utils.getGlobalLogger().info("ActionTimelineの検索完了");
    }

    /**
     * スケルトン関係ハッシュ検索
     */
    @SuppressWarnings({"ConstantConditions", "MismatchedReadAndWriteOfArray"})
    public static void findSkeletonHashes() {
        SqPack_IndexFile index;
        byte[] estData;
        int[] charaID_All = new int[]{101,201,301,401,501,601,701,801,901,1001,1101,1201,1301,1401,1501,1801};

        index = SqPack_IndexFile.GetIndexFileForArchiveID("040000", true);
        //バイナリをみてもID等不明な場合
        // 0x14 short : charaID
        // 0x16 short : charaType

        //顔スケルトン関係
        if(true) {
            estData = index.extractFile("chara/xls/charadb/FaceSkeletonTemplate.est");
            ByteBuffer bb = ByteBuffer.wrap(estData);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            int estData_Num = bb.getInt(); //データ数
            int[] SkeletonModelID = new int[estData_Num];
            int[] charaID = new int[estData_Num];
            int[] SkeletonID = new int[estData_Num];

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                SkeletonModelID[SkeletonTable_ID] = bb.getShort(); //SkeletonModelID
                charaID[SkeletonTable_ID] = bb.getShort(); //Character ID
            }

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                SkeletonID[SkeletonTable_ID] = bb.getShort(); //SkeletonID
            }

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                //chara/human/c9104/obj/face/f0001/model/c9104f0001_fac.mdl
                String fullPath = String.format("chara/human/c%04d/obj/face/f%04d/model/c%04df%04d_fac.mdl"
                        , charaID[SkeletonTable_ID], SkeletonModelID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonModelID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");

                //chara/human/c9104/skeleton/face/f0002/skl_c9104f0002.sklb
                fullPath = String.format("chara/human/c%04d/obj/face/f%04d/skl_c%04df%04d.sklb"
                        , charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");

                //faceアニメーション関係
                fullPath = String.format("chara/human/c%04d/animation/f%04d/resident/face.pap", charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                int pathCheck = index.findFile(fullPath);
                if (pathCheck == 2){
                    //face.papが存在する時のみ
                    HashDatabase.addPathToDB(fullPath, "040000");
                    //ファイル名は違うがフォルダは多分あるはず
                    fullPath = String.format("chara/human/c%04d/animation/f%04d/nonresident/angry_cl.pap", charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                    cAddPathToDB(fullPath, "040000");
                    //ないかも
                    fullPath = String.format("chara/human/c%04d/animation/f%04d/nonresident/cut/m_clench_cleye.pap", charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                    cAddPathToDB(fullPath, "040000");
                    //ないかも
                    fullPath = String.format("chara/human/c%04d/animation/f%04d/nonresident/emj/emj_kan.pap", charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                    cAddPathToDB(fullPath, "040000");
                    //ないかも
                    fullPath = String.format("chara/human/c%04d/animation/f%04d/nonresident/emot/angry.pap", charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                    cAddPathToDB(fullPath, "040000");
                }

            }
            Utils.getGlobalLogger().info("Face(顔) Skeleton検索完了");
        }

        //FaceSkeletonTemplate.estで検索できなかった分の追加
        if(false) {
            int charID2 = 601;
            for (int SkeletonID2 = 1; SkeletonID2 < 250; SkeletonID2++) {
                String fullPath;
                //chara/human/c0501/skeleton/face/f0215/skl_c0501f0215.sklb
                fullPath = String.format("chara/human/c%04d/skeleton/face/f%04d/skl_c%04df%04d.sklb"
                        , charID2, SkeletonID2, charID2, SkeletonID2);
                cAddPathToDB(fullPath, "040000");
            }
            Utils.getGlobalLogger().info("顔Skeleton追加検索完了");
        }

        //髪スケルトン関係
        if(true) {
            estData = index.extractFile("chara/xls/charadb/HairSkeletonTemplate.est");
            ByteBuffer bb = ByteBuffer.wrap(estData);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            int estData_Num = bb.getInt(); //データ数
            int[] SkeletonModelID = new int[estData_Num];
            int[] charaID = new int[estData_Num];
            int[] SkeletonID = new int[estData_Num];

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                SkeletonModelID[SkeletonTable_ID] = bb.getShort(); //SkeletonModelID
                charaID[SkeletonTable_ID] = bb.getShort(); //Character ID
            }

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                SkeletonID[SkeletonTable_ID] = bb.getShort(); //SkeletonID
            }

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                //chara/human/c9104/obj/hair/h0202/model/c9104h0202_hir.mdl
                String fullPath = String.format("chara/human/c%04d/obj/hair/h%04d/model/c%04dh%04d_hir.mdl"
                        , charaID[SkeletonTable_ID], SkeletonModelID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonModelID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");

                //chara/human/c9104/skeleton/hair/h0002/skl_c9104h0002.sklb
                fullPath = String.format("chara/human/c%04d/obj/hair/h%04d/skl_c%04dh%04d.sklb"
                        , charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");
                //chara/human/c9104/skeleton/hair/h0002/phy_c9104h0002.phyb
                fullPath = String.format("chara/human/c%04d/obj/hair/h%04d/phy_c%04dh%04d.phyb"
                        , charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");
            }
            Utils.getGlobalLogger().info("Hair(髪型) Skeleton検索完了");
        }

        //胴スケルトン関係
        if(true) {
            estData = index.extractFile("chara/xls/charadb/extra_top.est");
            ByteBuffer bb = ByteBuffer.wrap(estData);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            int estData_Num = bb.getInt(); //データ数
            int[] SkeletonModelID = new int[estData_Num];
            int[] charaID = new int[estData_Num];
            int[] SkeletonID = new int[estData_Num];

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                SkeletonModelID[SkeletonTable_ID] = bb.getShort(); //SkeletonModelID
                charaID[SkeletonTable_ID] = bb.getShort(); //Character ID
            }

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                SkeletonID[SkeletonTable_ID] = bb.getShort(); //SkeletonID
            }

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                //chara/human/c1801/obj/body/b0002/model/c1801b0002_top.mdl
                //モデル無し
                String fullPath;

                //chara/human/c1501/skeleton/top/t0621/skl_c1501t0621.sklb
                fullPath = String.format("chara/human/c%04d/skeleton/top/t%04d/skl_c%04dt%04d.sklb"
                        , charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");
                //chara/human/c1501/skeleton/top/t0621/phy_c1501t0621.phyb
                fullPath = String.format("chara/human/c%04d/skeleton/top/t%04d/phy_c%04dt%04d.phyb"
                        , charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");
            }
            Utils.getGlobalLogger().info("Top(胴) Skeleton検索完了");
        }

        //頭スケルトン関係
        if(true) {
            estData = index.extractFile("chara/xls/charadb/extra_met.est");
            ByteBuffer bb = ByteBuffer.wrap(estData);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            int estData_Num = bb.getInt(); //データ数
            int[] SkeletonModelID = new int[estData_Num];
            int[] charaID = new int[estData_Num];
            int[] SkeletonID = new int[estData_Num];

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                SkeletonModelID[SkeletonTable_ID] = bb.getShort(); //SkeletonModelID
                charaID[SkeletonTable_ID] = bb.getShort(); //Character ID
            }

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                SkeletonID[SkeletonTable_ID] = bb.getShort(); //SkeletonID
            }

            for (int SkeletonTable_ID = 0; SkeletonTable_ID < estData_Num; SkeletonTable_ID++) {
                //chara/human/c1801/obj/body/b0002/model/c1801b0002_top.mdl
                //モデル無し
                String fullPath;

                //chara/human/c1801/skeleton/met/m6103/skl_c1801m6103.sklb
                fullPath = String.format("chara/human/c%04d/skeleton/met/m%04d/skl_c%04dm%04d.sklb"
                        , charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");
                //chara/human/c1801/skeleton/met/m6103/phy_c1801m6103.phyb
                fullPath = String.format("chara/human/c%04d/skeleton/met/m%04d/phy_c%04dm%04d.phyb"
                        , charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID], charaID[SkeletonTable_ID], SkeletonID[SkeletonTable_ID]);
                cAddPathToDB(fullPath, "040000");
            }
            Utils.getGlobalLogger().info("Met(頭) Skeleton検索完了");
        }

        //extra_met.estで検索できなかった分の追加
        if(true) {
            for (int charID2 : charaID_All) {
                for (int SkeletonID2 = 6100; SkeletonID2 < 6300; SkeletonID2++) {
                    String fullPath;
                    //chara/human/c1801/skeleton/met/m6103/skl_c1801m6103.sklb
                    fullPath = String.format("chara/human/c%04d/skeleton/met/m%04d/skl_c%04dm%04d.sklb"
                            , charID2, SkeletonID2, charID2, SkeletonID2);
                    cAddPathToDB(fullPath, "040000");
                    //chara/human/c1801/skeleton/met/m6103/phy_c1801m6103.phyb
                    fullPath = String.format("chara/human/c%04d/skeleton/met/m%04d/phy_c%04dm%04d.phyb"
                            , charID2, SkeletonID2, charID2, SkeletonID2);
                    cAddPathToDB(fullPath, "040000");
                }
            }
            Utils.getGlobalLogger().info("Met(頭) Skeleton追加検索完了");
        }

        //Demihumanスケルトン関係
        if(true) {
            byte[] awtData = index.extractFile("chara/xls/animation/animation_work_table-demihuman.awt");
            ByteBuffer bb = ByteBuffer.wrap(awtData);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.getInt(); //twa
            bb.getShort(); //不明 0x300
            bb.getShort(); //不明 0x100
            short awTable_Num = bb.getShort(); //テーブル数 0x02a6 = 678 1テーブル8バイト

            bb.position(0x18); //ポインタをawTableの先頭に移動

            //ここから8バイトのpapヘッダーテーブル(0x51A3×4 = 20899個)
            for(int awTable_ID = 0; awTable_ID < awTable_Num; awTable_ID++){
                if ((bb.limit() - 7) < bb.position()){
                    Utils.getGlobalLogger().info("バッファーオーバーフロー防止のため読み込み停止");
                    break;
                }
                int charaID = bb.getShort() & 0xFFFF;          //Character ID
                short charaType = bb.getShort();        //charaType(0:human,1:monster,2:demi)
                bb.get();     //AnimationType_ID
                bb.get();     //Animation_ID
                bb.getShort();    //不明

                String charaTypeSign;
                String charaTypeFull;
                String fullPath;
                switch(charaType){
                    case 0:
                        charaTypeSign = "c";
                        charaTypeFull = "human";
                        break;
                    case 1:
                        charaTypeSign = "m";
                        charaTypeFull = "monster";
                        break;
                    case 2:
                        charaTypeSign = "d";
                        charaTypeFull = "demihuman";
                        break;
                    case 3:
                        charaTypeSign = "w";
                        charaTypeFull = "weapon";
                        break;
                    default:
                        charaTypeSign = String.valueOf(charaType);
                        charaTypeFull = "unknown";
                }

                //chara/monster/m0432/skeleton/base/b0001/eid_m0432b0001.eid
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/eid_%s%04db0001.eid", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/phy_m0432b0001.phyb
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/phy_%s%04db0001.phyb", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/skl_m0432b0001.sklb
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/skl_%s%04db0001.sklb", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/skl_m0432b0001.skp
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/skl_%s%04db0001.skp", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");
            }
            Utils.getGlobalLogger().info("Demihumanスケルトン検索完了");
        }

        //Monsterスケルトン関係
        if(true) {
            byte[] awtData = index.extractFile("chara/xls/animation/animation_work_table-monster.awt");
            ByteBuffer bb = ByteBuffer.wrap(awtData);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.getInt(); //twa
            bb.getShort(); //不明 0x300
            bb.getShort(); //不明 0x100
            short awTable_Num = bb.getShort(); //テーブル数 0x02a6 = 678 1テーブル8バイト

            bb.position(0x18); //ポインタをawTableの先頭に移動

            //ここから8バイトのpapヘッダーテーブル(0x51A3×4 = 20899個)
            for(int awTable_ID = 0; awTable_ID < awTable_Num; awTable_ID++){
                if ((bb.limit() - 7) < bb.position()){
                    Utils.getGlobalLogger().info("バッファーオーバーフロー防止のため読み込み停止");
                    break;
                }
                int charaID = bb.getShort() & 0xFFFF;          //Character ID
                short charaType = bb.getShort();        //charaType(0:human,1:monster,2:demi)
                bb.get();     //AnimationType_ID
                bb.get();     //Animation_ID
                bb.getShort();    //不明

                String charaTypeSign;
                String charaTypeFull;
                String fullPath;
                switch(charaType){
                    case 0:
                        charaTypeSign = "c";
                        charaTypeFull = "human";
                        break;
                    case 1:
                        charaTypeSign = "m";
                        charaTypeFull = "monster";
                        break;
                    case 2:
                        charaTypeSign = "d";
                        charaTypeFull = "demihuman";
                        break;
                    case 3:
                        charaTypeSign = "w";
                        charaTypeFull = "weapon";
                        break;
                    default:
                        charaTypeSign = String.valueOf(charaType);
                        charaTypeFull = "unknown";
                }

                //chara/monster/m0432/skeleton/base/b0001/eid_m0432b0001.eid
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/eid_%s%04db0001.eid", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/phy_m0432b0001.phyb
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/phy_%s%04db0001.phyb", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/skl_m0432b0001.sklb
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/skl_%s%04db0001.sklb", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/skl_m0432b0001.skp
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/skl_%s%04db0001.skp", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");
            }
            Utils.getGlobalLogger().info("Monsterスケルトン検索完了");
        }

        //Weaponスケルトン関係
        if(true) {
            byte[] awtData = index.extractFile("chara/xls/animation/animation_work_table-weapon.awt");
            ByteBuffer bb = ByteBuffer.wrap(awtData);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.getInt(); //twa
            bb.getShort(); //不明 0x300
            bb.getShort(); //不明 0x100
            short awTable_Num = bb.getShort(); //テーブル数 0x02a6 = 678 1テーブル8バイト

            bb.position(0x18); //ポインタをawTableの先頭に移動

            //ここから8バイトのpapヘッダーテーブル(0x51A3×4 = 20899個)
            for(int awTable_ID = 0; awTable_ID < awTable_Num; awTable_ID++){
                if ((bb.limit() - 7) < bb.position()){
                    Utils.getGlobalLogger().info("バッファーオーバーフロー防止のため読み込み停止");
                    break;
                }
                int charaID = bb.getShort() & 0xFFFF;          //Character ID
                short charaType = bb.getShort();        //charaType(0:human,1:monster,2:demi)
                bb.get();     //AnimationType_ID
                bb.get();     //Animation_ID
                bb.getShort();    //不明

                String charaTypeSign;
                String charaTypeFull;
                String fullPath;
                switch(charaType){
                    case 0:
                        charaTypeSign = "c";
                        charaTypeFull = "human";
                        break;
                    case 1:
                        charaTypeSign = "m";
                        charaTypeFull = "monster";
                        break;
                    case 2:
                        charaTypeSign = "d";
                        charaTypeFull = "demihuman";
                        break;
                    case 3:
                        charaTypeSign = "w";
                        charaTypeFull = "weapon";
                        break;
                    default:
                        charaTypeSign = String.valueOf(charaType);
                        charaTypeFull = "unknown";
                }

                //chara/monster/m0432/skeleton/base/b0001/eid_m0432b0001.eid
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/eid_%s%04db0001.eid", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/phy_m0432b0001.phyb
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/phy_%s%04db0001.phyb", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/skl_m0432b0001.sklb
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/skl_%s%04db0001.sklb", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");

                //chara/monster/m0432/skeleton/base/b0001/skl_m0432b0001.skp
                fullPath = String.format("chara/%s/%s%04d/skeleton/base/b0001/skl_%s%04db0001.skp", charaTypeFull, charaTypeSign, charaID, charaTypeSign, charaID);
                cAddPathToDB(fullPath, "040000");
            }
            Utils.getGlobalLogger().info("Weaponスケルトン検索完了");
        }

    }

    /**
     * 衝突定義ファイル関係ハッシュ検索
     * mdlファイルに対応するpcbファイルを検索するため
     * 事前にmdlファイル名が登録されていないと機能しません
     */
    public static void findCollisionHashes() {
        SqPack_IndexFile index;

        index = SqPack_IndexFile.GetIndexFileForArchiveID("010000", false);
        bgcommonIndex = index;

        SqPack_Folder[] folders = index.getPackFolders();

        for (SqPack_Folder spPack_folder : folders){
            SqPack_File[] files = spPack_folder.getFiles();
            for (SqPack_File sqPack_file : files) {
                String folderName = spPack_folder.getName();

                String fileName = sqPack_file.getName();
                if (fileName != null && folderName != null) {
                    if (fileName.endsWith(".mdl")) {
                        //bgcommon/world/cut/057/bgparts/w_cut_057_01a.mdl
                        //bgcommon/world/cut/057/collision/w_cut_057_01a.pcb
                        folderName = folderName.replace("bgparts","collision");
                        fileName = fileName.replace(".mdl",".pcb");
                        String fullPath = folderName + "/" + fileName;
                        cAddPathToDB(fullPath, "010000");

                    }
                }

            }
        }
        Utils.getGlobalLogger().info("Collisionファイル検索完了");
    }

    @SuppressWarnings("unused")
    public static void openEveryModel() {
        SqPack_IndexFile currentIndex = SqPack_IndexFile.GetIndexFileForArchiveID("040000", false);

        for (int i = 0; i < currentIndex.getPackFolders().length; i++) {
            SqPack_Folder folder = currentIndex.getPackFolders()[i];
            for (int j = 0; j < folder.getFiles().length; j++) {
                if (folder.getFiles()[j].getName().contains(".mdl")) {
                    Utils.getGlobalLogger().info("=> {}モデルを取得", folder.getFiles()[j].getName());
                    Model m = new Model(folder.getName() + "/" + folder.getFiles()[j].getName(), currentIndex, currentIndex.extractFile(folder.getFiles()[j].dataOffset, null), currentIndex.getEndian());
                    for (int x = 0; x < m.getNumVariants(); x++) {
                        m.loadVariant(x);
                    }
                }
            }
        }

    }

    public static void openEveryCutb() {
        SqPack_IndexFile currentIndex = SqPack_IndexFile.GetIndexFileForArchiveID("030300", false);

        for (int i = 0; i < currentIndex.getPackFolders().length; i++) {
            SqPack_Folder folder = currentIndex.getPackFolders()[i];
            for (int j = 0; j < folder.getFiles().length; j++) {
                if (folder.getFiles()[j].getName().contains(".cutb")) {
                    Utils.getGlobalLogger().info("=> {}を調査", folder.getFiles()[j].getName());
                    String CutSceneBlock_File = folder.getName() + "/" + folder.getFiles()[j].getName();
                    byte[] data = currentIndex.extractFile(CutSceneBlock_File);

                    @SuppressWarnings("unused")
                    CutbFile cutbFile = new CutbFile(currentIndex, data, currentIndex.getEndian());
                }
            }
        }

    }
    @SuppressWarnings("unused")
    public static void findStains() {
        SqPack_IndexFile currentIndex = SqPack_IndexFile.GetIndexFileForArchiveID("040000", false);

        for (int i = 0; i < currentIndex.getPackFolders().length; i++) {
            SqPack_Folder folder = currentIndex.getPackFolders()[i];
            String folderName = folder.getName();
            if (folderName.contains("equipment") && folderName.contains("material/v")) {
                String newFolder = folderName + "/staining";
                //Check if exists
                int folderHash = HashDatabase.computeCRC(newFolder.getBytes(), 0, newFolder.getBytes().length);
                for (int j = 0; j < currentIndex.getPackFolders().length; j++) {
                    SqPack_Folder folder2 = currentIndex.getPackFolders()[j];
                    if (folder2.getId() == folderHash) {

                        for (int y = 0; y < folder2.getFiles().length; y++) {
                            SqPack_File file = folder2.getFiles()[y];

                            if (!file.getName().endsWith(".tex") && !file.getName().endsWith(".mtrl")) {
                                continue;
                            }

                            if (file.getName().contains(".tex")) {
                                for (int x = 1; x <= 85; x++) {
                                    cAddPathToDB(newFolder + "/" + file.getName().replace(".tex", String.format("_s%04d.tex", x)), "040000");
                                }

                                for (int x = 101; x <= 120; x++) {
                                    cAddPathToDB(newFolder + "/" + file.getName().replace(".tex", String.format("_s%04d.tex", x)), "040000");
                                }
                            } else if (file.getName().contains(".mtrl")) {
                                for (int x = 1; x <= 85; x++) {
                                    cAddPathToDB(newFolder + "/" + file.getName().replace(".mtrl", String.format("_s%04d.mtrl", x)), "040000");
                                }

                                for (int x = 101; x <= 120; x++) {
                                    cAddPathToDB(newFolder + "/" + file.getName().replace(".mtrl", String.format("_s%04d.mtrl", x)), "040000");
                                }
                            }
                        }

                    }
                }
            }
        }

    }


    /**
     * ファイルの存在チェック後、ハッシュデータベース登録
     * (フォルダ名の一致のみでも登録する。)
     * @param fullPath フルパス
     * @param archive Indexファイル名
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    @SuppressWarnings("UnusedReturnValue")
    private static int cAddPathToDB(String fullPath, String archive){
        return cAddPathToDB(fullPath, archive,1);
    }

    /**
     * ファイルの存在チェック後、ハッシュデータベース登録
     * @param fullPath フルパス
     * @param archive Indexファイル名
     * @param regMode 1:フォルダ名の一致のみでも登録する。2:ファイル名・パスが完全一致した時のみ登録
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    private static int cAddPathToDB(String fullPath, String archive, int regMode){
        int result = 0;
        SqPack_IndexFile temp_IndexFile = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);

        if (temp_IndexFile == null){
            return 0;
        }

        int pathCheck = temp_IndexFile.findFile(fullPath);
        if (pathCheck == 2){
            result = HashDatabase.addPathToDB(fullPath, archive);

            if (fullPath.endsWith(".mdl")) {
                try {
                    byte[] data = temp_IndexFile.extractFile(fullPath);
                    Model tempModel = new Model(fullPath, temp_IndexFile, data, temp_IndexFile.getEndian());
                    tempModel.loadVariant(variant); //mdlファイルに関連するmtrlとtexの登録を試みる。
                } catch (Exception modelException) {
                    modelException.printStackTrace();
                }
            }
            if (fullPath.endsWith(".cutb")) {
                byte[] data = temp_IndexFile.extractFile(fullPath);

                @SuppressWarnings("unused")
                CutbFile cutbFile = new CutbFile(temp_IndexFile, data, temp_IndexFile.getEndian());
            }

        }else if (pathCheck == 1 && regMode == 1){
            //ファイルパスのみ追加
            String folder;
            if (fullPath.contains(".")) {
                folder = fullPath.substring(0, fullPath.lastIndexOf("/"));
            }else{
                folder = fullPath;
            }

            if (fullPath.endsWith(".mtrl") || fullPath.endsWith(".mdl")) {
                result = HashDatabase.addFolderToDB(folder, archive, false);
            }else {
                result = HashDatabase.addFolderToDB(folder, archive);
            }
        }
        return result;
    }

}

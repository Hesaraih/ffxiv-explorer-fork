package com.fragmenterworks.ffxivextract.helpers;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.gui.components.EXDF_View;
import com.fragmenterworks.ffxivextract.models.EXHF_File;
import com.fragmenterworks.ffxivextract.models.Model;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_File;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_Folder;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.io.*;
import java.sql.SQLException;
import java.util.Objects;

/**
 * ハッシュ検索用ユーティリティ集
 */
public class HashFinding_Utils {

    /**
     *  Exhハッシュ検索
     *  game_scriptも追加
     */
    public static void findExhHashes() {
        Utils.getGlobalLogger().info("root.exlを開いています...");

        byte[] rootData = null;
        SqPack_IndexFile index = null;
        SqPack_IndexFile index2 = null;
        try {
            index = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\0a0000.win32.index", true);
            index2 = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\0b0000.win32.index", true);
            rootData = index.extractFile("exd/root.exl");
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }

        if (rootData == null) {
            return;
        }

        Utils.getGlobalLogger().info("root.exl読み込み中...");

        try {
            InputStream in = new ByteArrayInputStream(rootData);
            //root.exlはテキストデータであるためBufferedReaderを使用する
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            HashDatabase.beginConnection();
            try {
                HashDatabase.setAutoCommit(false);
            } catch (SQLException e1) {
                Utils.getGlobalLogger().error(e1);
            }
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                //データが Achievement,209のようにカンマ区切りになっている
                String path;
                String pathID;
                path = line.substring(0, line.lastIndexOf(','));
                pathID = line.substring(line.lastIndexOf(',') + 1);

                //game_scriptのlua登録
                if (Objects.equals(pathID, "-1")){
                    if (path.startsWith("quest/")) {
                        String[] battleText = new String[]{"_","Btl_","Btl2_"};
                        for (String btl: battleText){
                            String path2 = path.replaceFirst("_",btl);
                            String luabName = String.format("game_script/%s.luab", path2);
                            if (index2.extractFile(luabName) != null) {
                                HashDatabase.addPathToDB(luabName, "0b0000", HashDatabase.globalConnection,true);
                            }
                        }
                    } else {
                        String luabName = String.format("game_script/%s.luab", path);
                        if (index2.extractFile(luabName) != null) {
                            HashDatabase.addPathToDB(luabName, "0b0000", HashDatabase.globalConnection,true);
                        }
                    }
                }

                //noinspection ConstantConditions
                if (false){
                    //exhFileの登録
                    EXHF_File exhFile = null;

                    String exhName = String.format("exd/%s.exh", path);
                    if (index.extractFile(exhName) != null) {
                        HashDatabase.addPathToDB(exhName, "0a0000", HashDatabase.globalConnection,true);
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
                                HashDatabase.addPathToDB(exdName, "0a0000", HashDatabase.globalConnection,true);
                            }
                        }
                    }
                }
            }

            try {
                HashDatabase.commit();
            } catch (SQLException e) {
                Utils.getGlobalLogger().error(e);
            }
            HashDatabase.closeConnection();
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
        Utils.getGlobalLogger().info("bgm.exhを開いています...");

        byte[] exhData = null;
        SqPack_IndexFile index = null;
        try {
            index = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\0a0000.win32.index", true);
            exhData = index.extractFile("exd/bgm.exh");
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }

        if (exhData == null) {
            return;
        }

        EXDF_View viewer = null;
        //noinspection ConstantConditions
        if (true){ //bgm.exh
            try {
                viewer = new EXDF_View(index, "exd/bgm.exh", new EXHF_File(exhData));
            } catch (IOException e2) {
                Utils.getGlobalLogger().error(e2);
            }

            if (viewer == null) {
                return;
            }

            Utils.getGlobalLogger().info("bgm.exhを読み込みました");

            HashDatabase.beginConnection();
            try {
                HashDatabase.setAutoCommit(false);
            } catch (SQLException e1) {
                Utils.getGlobalLogger().error(e1);
            }
            for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
                String path = String.format("%s", viewer.getTable().getValueAt(i, 1));

                if (path == null || path.isEmpty()) {
                    continue;
                }

                //蒼天までしか対応していなかったので変更
                String archive = HashDatabase.getArchiveID(path);

                HashDatabase.addPathToDB(path, archive, HashDatabase.globalConnection, true);
            }
        }

        //noinspection ConstantConditions
        if (true){ //bgm.exh
            try {
                viewer = new EXDF_View(index, "exd/OrchestrionPath.exh", new EXHF_File(exhData));
            } catch (IOException e2) {
                Utils.getGlobalLogger().error(e2);
            }

            Utils.getGlobalLogger().info("OrchestrionPath.exhを読み込みました");

            HashDatabase.beginConnection();
            try {
                HashDatabase.setAutoCommit(false);
            } catch (SQLException e1) {
                Utils.getGlobalLogger().error(e1);
            }
            for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
                String path = String.format("%s", viewer.getTable().getValueAt(i, 1));

                if (path == null || path.isEmpty()) {
                    continue;
                }

                String archive = HashDatabase.getArchiveID(path);

                HashDatabase.addPathToDB(path, archive, HashDatabase.globalConnection, true);
            }
        }
        try {
            HashDatabase.commit();
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
        }
        HashDatabase.closeConnection();

        Utils.getGlobalLogger().info("BGMの検索完了");
    }

    /**
     * Soundハッシュ検索
     */
    public static void findSoundHashes() {
        Utils.getGlobalLogger().info("bgm.exhを開いています...");

        byte[] exhData = null;
        SqPack_IndexFile index = null;
        SqPack_IndexFile index2 = null;
        try {
            index = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\0a0000.win32.index", true);
            index2 = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\070000.win32.index", true);
            exhData = index.extractFile("exd/bgm.exh");
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }

        if (exhData == null) {
            return;
        }

        EXDF_View viewer = null;
        try {
            viewer = new EXDF_View(index, "exd/bgm.exh", new EXHF_File(exhData));
        } catch (IOException e2) {
            Utils.getGlobalLogger().error(e2);
        }

        if (viewer == null) {
            return;
        }

        Utils.getGlobalLogger().info("bgm.exhを読み込みました");

        HashDatabase.beginConnection();
        try {
            HashDatabase.setAutoCommit(false);
        } catch (SQLException e1) {
            Utils.getGlobalLogger().error(e1);
        }
        for (int i = 1; i < viewer.getTable().getRowCount(); i++) {
            String path = String.format("%s", viewer.getTable().getValueAt(i, 1));

            if (path == null || path.isEmpty()) {
                continue;
            }

            String archive = HashDatabase.getArchiveID(path);

            HashDatabase.addPathToDB(path, archive, HashDatabase.globalConnection, true);
        }

        //noinspection ConstantConditions
        if (true){ //デバッグ用フラグ
            int searchIconNum = 300;

            //sound/event/検索
            for (int i = 1; i < searchIconNum; i++) {
                String soundPath ="";
                //例：sound/event/se_event_246.scd
                soundPath = String.format("sound/event/se_event_%03d.scd",i);

                if (index2.existsFile(soundPath)) { //存在チェック
                    //ui/icon登録
                    HashDatabase.addPathToDB(soundPath, "070000", HashDatabase.globalConnection);
                }
            }
        }

        //noinspection ConstantConditions
        if (true){ //デバッグ用フラグ
            int searchIconNum = 14000;

            //sound/battle/mon検索
            for (int i = 3001; i < searchIconNum; i++) {
                String soundPath ="";
                //例：sound/battle/mon/13001.scd
                String[] multi = {"_ja","_en","_fr","_de"};
                if (i <= 3900 || i > 3990){
                    soundPath = String.format("sound/battle/mon/%d.scd", i);
                    if (index2.existsFile(soundPath)) { //存在チェック
                        //ui/icon登録
                        HashDatabase.addPathToDB(soundPath, "070000", HashDatabase.globalConnection);
                    }
                }else{
                    for (String lang :multi) {
                        soundPath = String.format("sound/battle/mon/%d%s.scd",i , lang);
                        if (index2.existsFile(soundPath)) { //存在チェック
                            //ui/icon登録
                            HashDatabase.addPathToDB(soundPath, "070000", HashDatabase.globalConnection);
                        }
                    }
                }
            }
        }

        //noinspection ConstantConditions
        if (true){ //デバッグ用フラグ
            int searchIconNum = 3000;

            //sound/battle/mon検索
            for (int i = 1000; i < searchIconNum; i++) {
                String soundPath ="";
                //例：sound/voice/vo_line/8201000_ja.scd
                String[] multi = {"_ja","_en","_fr","_de"};
                for (String lang :multi) {
                    soundPath = String.format("sound/battle/mon/820%04d%s.scd",i, lang);
                    if (index2.existsFile(soundPath)) { //存在チェック
                        //ui/icon登録
                        HashDatabase.addPathToDB(soundPath, "070000", HashDatabase.globalConnection);
                    }
                }
            }
        }

        try {
            HashDatabase.commit();
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
        }
        HashDatabase.closeConnection();

        Utils.getGlobalLogger().info("Soundの検索完了");
    }
    /**
     * Mapハッシュ検索
     */
    public static void findMapHashes() {
        try {
            byte[] exhData = null;
            SqPack_IndexFile index = null;
            SqPack_IndexFile index2 = null;
            try {
                index = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\0a0000.win32.index", true);
                index2 = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\060000.win32.index", true);
                exhData = index.extractFile("exd/map.exh");
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }

            if (exhData == null) {
                return;
            }

            EXHF_File exhfFile = new EXHF_File(exhData);
            EXDF_View mapView = new EXDF_View(index, "exd/map.exh", exhfFile);

            HashDatabase.beginConnection();
            try {
                HashDatabase.setAutoCommit(false);
            } catch (SQLException e1) {
                Utils.getGlobalLogger().error(e1);
            }

            //noinspection ConstantConditions
            if (false){ //デバッグ用フラグ
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
                        if (index2.existsFile(mapPath)) { //存在チェック
                            HashDatabase.addPathToDB(mapPath, "060000", HashDatabase.globalConnection);
                        }
                    }
                }
            }

            //noinspection ConstantConditions
            if (true){ //デバッグ用フラグ
                int searchIconNum = 182000;

                //ui/icon検索
                boolean multiFlag = false;
                for (int i = 0; i < searchIconNum; i++) {
                    String iconPath ="";
                    String iconPath2 ="";
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

                                if (index2.existsFile(iconPath)) { //存在チェック
                                    //ui/icon登録
                                    HashDatabase.addPathToDB(iconPath, "060000", HashDatabase.globalConnection);
                                    HashDatabase.addPathToDB(iconPath2, "060000", HashDatabase.globalConnection); //高画質用
                                }
                                multiFlag = false;
                            }
                            continue;
                        }else{
                            iconPath = String.format("ui/icon/%06d/ja/%06d.tex",pathNum, i);
                            iconPath2 = String.format("ui/icon/%06d/ja/%06d_hr1.tex",pathNum, i); //高画質用
                        }

                    }

                    if (index2.existsFile(iconPath)) { //存在チェック
                        //ui/icon登録
                        HashDatabase.addPathToDB(iconPath, "060000", HashDatabase.globalConnection);
                        HashDatabase.addPathToDB(iconPath2, "060000", HashDatabase.globalConnection); //高画質用
                    }
                }
            }

            try {
                HashDatabase.commit();
            } catch (SQLException e) {
                Utils.getGlobalLogger().error(e);
            }
            HashDatabase.closeConnection();

        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

    /**
     * キャラクタモデルから他ファイル登録？
     * @param path ファイルパス
     */
    @SuppressWarnings("unused")
    public static void getModelsFromModelChara(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            while (true) {
                String in = reader.readLine();
                if (in == null) {
                    break;
                }
                String[] split = in.split(",");

                int id = Integer.parseInt(split[1]);
                int type = Integer.parseInt(split[3]);
                int model = Integer.parseInt(split[4]);
                int variant = Integer.parseInt(split[5]);

                String typePath;
                String imcPath, modelPath, skelPath;

                if (type != 3) {
                    type = 20;
                }

                switch (type) {
                    case 3:
                        typePath = "chara/monster/m";
                        imcPath = String.format("%s%04d/obj/body/b%04d/b%04d.imc", typePath, id, model, model);
                        modelPath = String.format("%s%04d/obj/body/b%04d/model/m%04db%04d.mdl", typePath, id, model, id, model);

                        HashDatabase.addPathToDB(imcPath, "040000");
                        HashDatabase.addPathToDB(modelPath, "040000");

                        skelPath = String.format("%s%04d/skeleton/base/b%04d/eid_m%04db%04d.eid", typePath, id, model, id, model);
                        HashDatabase.addPathToDB(skelPath, "040000");
                        skelPath = String.format("%s%04d/skeleton/base/b%04d/skl_m%04db%04d.sklp", typePath, id, model, id, model);
                        HashDatabase.addPathToDB(skelPath, "040000");
                        skelPath = String.format("%s%04d/skeleton/base/b%04d/skl_m%04db%04d.sklb", typePath, id, model, id, model);
                        HashDatabase.addPathToDB(skelPath, "040000");
                        skelPath = String.format("%s%04d/animation/a%04d/bt_common/resident/monster.pap", typePath, id, 0);
                        HashDatabase.addPathToDB(skelPath, "040000");
                        skelPath = String.format("%s%04d/animation/a%04d/bt_common/event/event_wandering_action.pap", typePath, id, 0);
                        HashDatabase.addPathToDB(skelPath, "040000");
                        skelPath = String.format("%s%04d/animation/a%04d/bt_common/mon_sp/m%04d/mon_sp001.pap", typePath, id, 0, id);
                        HashDatabase.addPathToDB(skelPath, "040000");
                        break;
                    //noinspection ConstantConditions
                    case 4:
                        typePath = "chara/demihuman/d";
                        imcPath = String.format("%s%04d/obj/equipment/e%04d/e%04d.imc", typePath, id, model, model);
                        HashDatabase.addPathToDB(imcPath, "040000");

                        modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_met.mdl", typePath, id, model, id, model);
                        HashDatabase.addPathToDB(modelPath, "040000");
                        modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_top.mdl", typePath, id, model, id, model);
                        HashDatabase.addPathToDB(modelPath, "040000");
                        modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_dwn.mdl", typePath, id, model, id, model);
                        HashDatabase.addPathToDB(modelPath, "040000");
                        modelPath = String.format("%s%04d/obj/equipment/e%04d/model/d%04de%04d_sho.mdl", typePath, id, model, id, model);
                        HashDatabase.addPathToDB(modelPath, "040000");
                        break;
                }
            }
            reader.close();
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

    @SuppressWarnings({"unused", "UnusedAssignment", "ConstantConditions", "CommentedOutCode"})
    public static void getModels(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            BufferedWriter writer = new BufferedWriter(new FileWriter(path + "out.txt"));

            while (true) {
                String in = reader.readLine();
                if (in == null) {
                    break;
                }
                String[] split = in.split(":");

                String model1 = split[0];
                String model2 = split[1];

                //Model1
                if (!model1.equals("0, 0, 0, 0")) {
                    String[] model1Split = model1.split(",");
                    int section1 = Integer.parseInt(model1Split[3]);
                    int modelNum1 = Integer.parseInt(model1Split[2]);
                    int variant1 = Integer.parseInt(model1Split[1]);

                    String type1 = null;

                    if (section1 < 30) {
                        type1 = "chara/accessory/";
                    } else if (section1 < 3) {
                        type1 = "chara/equipment/";
                    }
                    // TODO wat?
//                    else if (section1 < 3)
//                        type1 = "chara/weapon/";

                    String imcPath1 = "";
                    String modelPath1 = "";
                    String materialPath1 = "";
                    String texturePath1 = "";
                }
                //Model2
                if (!model2.equals("0, 0, 0, 0")) {
                    String[] model2Split = model2.split(",");
                    int section2 = Integer.parseInt(model2Split[3]);
                    int modelNum2 = Integer.parseInt(model2Split[2]);
                    int variant2 = Integer.parseInt(model2Split[1]);

                    String type2 = null;

                    if (section2 < 30) {
                        type2 = "chara/accessory/";
                    } else if (section2 < 3) {
                        type2 = "chara/equipment/";
                    }
                    // TODO wat?
//                    else if (section2 < 3)
//                        type2 = "chara/weapon/";

                    String imcPath2 = "%s%04d.imc";
                    String modelPath2 = "%s%04d.mdl";
                    String materialPath2 = "texture/a";
                    String texturePath2 = "texture/a";
                }
            }
            reader.close();
            writer.close();
        } catch (IOException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

    @SuppressWarnings("unused")
    public static void openEveryModel() {
        try {
            SqPack_IndexFile currentIndex = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\040000.win32.index", true);

            for (int i = 0; i < currentIndex.getPackFolders().length; i++) {
                SqPack_Folder folder = currentIndex.getPackFolders()[i];
                for (int j = 0; j < folder.getFiles().length; j++) {
                    if (folder.getFiles()[j].getName().contains(".mdl")) {
                        Utils.getGlobalLogger().info("=> Getting model {}", folder.getFiles()[j].getName());
                        Model m = new Model(folder.getName() + "/" + folder.getFiles()[j].getName(), currentIndex, currentIndex.extractFile(folder.getFiles()[j].dataOffset, null), currentIndex.getEndian());
                        for (int x = 0; x < m.getNumVariants(); x++) {
                            m.loadVariant(x);
                        }
                    }
                }
            }

        } catch (IOException e2) {
            Utils.getGlobalLogger().error(e2);
        }
    }

    @SuppressWarnings("unused")
    public static void findStains() {
        try {
            SqPack_IndexFile currentIndex = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\040000.win32.index", true);

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
                                        HashDatabase.addPathToDB(newFolder + "/" + file.getName().replace(".tex", String.format("_s%04d.tex", x)), "040000");
                                    }

                                    for (int x = 101; x <= 120; x++) {
                                        HashDatabase.addPathToDB(newFolder + "/" + file.getName().replace(".tex", String.format("_s%04d.tex", x)), "040000");
                                    }
                                } else if (file.getName().contains(".mtrl")) {
                                    for (int x = 1; x <= 85; x++) {
                                        HashDatabase.addPathToDB(newFolder + "/" + file.getName().replace(".mtrl", String.format("_s%04d.mtrl", x)), "040000");
                                    }

                                    for (int x = 101; x <= 120; x++) {
                                        HashDatabase.addPathToDB(newFolder + "/" + file.getName().replace(".mtrl", String.format("_s%04d.mtrl", x)), "040000");
                                    }
                                }
                            }

                        }
                    }
                }
            }

        } catch (IOException e2) {
            Utils.getGlobalLogger().error(e2);
        }
    }

}

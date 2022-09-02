package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.SparseArray;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.uldStuff.*;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by Roze on 2017-06-17.
 *
 * @author Roze
 */
public class ULD_File extends Game_File {

    //他のファイルを見つけるために使用されます
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static SqPack_IndexFile currentIndex;

    public SqPack_IndexFile spIndex;

    /**
     * グラフィカルノード パーサ(構文解析器)の一覧
     */
    private static final SparseArray<Class<? extends GraphicsNodeTypeData>> graphicsTypes = new SparseArray<>();

    /**
     * COHD ノードのパーサーのリスト
     */
    private static final SparseArray<Class<? extends COHDEntryType>> cohdTypes = new SparseArray<>();

    /*
     * デフォルトのパーサーを初期化する
     */
    static {
        putGraphicsType(1, GraphicsNodeTypeData_1.class);
        putGraphicsType(2, GraphicsNodeTypeData_2.class);
        putGraphicsType(3, GraphicsNodeTypeData_3.class);
        putGraphicsType(4, GraphicsNodeTypeData_4.class);
        putGraphicsType(5, GraphicsNodeTypeData_5.class);

        putCOHDType(1, COHDEntryType_Graphics.class);
        putCOHDType(2, COHDEntryType_Frame.class);
        putCOHDType(9, COHDEntryType_List.class);
        putCOHDType(13, COHDEntryType_Scrollbar.class);
        putCOHDType(6, COHDEntryType_Slider.class);
    }

    /**
     * ULD ヘッダー解析
     */
    public ULDH uldHeader;

    /**
     * 指定された ULD データ プールを解析します
     *
     * @param data 使用するデータプール
     */
    public ULD_File(SqPack_IndexFile index, final byte[] data, ByteOrder endian) {
        super(endian);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(endian);
        currentIndex = index;
        spIndex = index;
        uldHeader = new ULDH(bb);
    }

    /**
     * 新しいグラフィカル ノード パーサーを追加します
     *
     * @param kind      新しいハンドラーに関連付ける型識別子
     * @param nodeClass 指定された識別子のノード ハンドラー
     */
    private static void putGraphicsType(int kind, Class<? extends GraphicsNodeTypeData> nodeClass) {
        graphicsTypes.put(kind, nodeClass);
    }

    /**
     * 新しい COHD ノード パーサーを追加します
     *
     * @param kind      新しいハンドラーに関連付ける型識別子
     * @param nodeClass 指定された識別子のノード ハンドラー
     */
    private static void putCOHDType(int kind, Class<? extends COHDEntryType> nodeClass) {
        cohdTypes.put(kind, nodeClass);
    }

    /**
     * 指定された型のパーサーを検索し、型に従って解析されたデータを含む新しいインスタンスを返します。
     * 利用可能なパーサーは、putGraphicsType への呼び出しによって事前に追加されている必要があります。
     *
     * @param type ハンドラーを見つけるための型識別子
     * @param data 使用するデータプール
     * @return 指定されたタイプの解析済みデータの新しいインスタンス
     */
    public static GraphicsNodeTypeData getGraphicsNodeByType(int type, ByteBuffer data) {
        Class<? extends GraphicsNodeTypeData> aClass = graphicsTypes.get(type);
        if (aClass != null) {
            try {
                Constructor<? extends GraphicsNodeTypeData> c = aClass.getDeclaredConstructor(ByteBuffer.class);
                return c.newInstance(data);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                Utils.getGlobalLogger().error(e);
            }
        }
        return null;
    }

    /**
     * 指定された型のパーサーを検索し、型に従って解析されたデータを含む新しいインスタンスを返します。
     * 使用可能なパーサーは、putCOHDType への呼び出しによって事前に追加されている必要があります。
     *
     * @param type ハンドラーを見つけるための型識別子
     * @param data 使用するデータプール
     * @return 指定されたタイプの解析済みデータの新しいインスタンス
     */
    public static COHDEntryType getCOHDNodeByType(int type, ByteBuffer data) {
        Class<? extends COHDEntryType> aClass = cohdTypes.get(type);
        if (aClass != null) {
            try {
                Constructor<? extends COHDEntryType> c = aClass.getDeclaredConstructor(ByteBuffer.class);
                return c.newInstance(data);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                Utils.getGlobalLogger().error(e);
            }
        }
        return null;
    }

    /**
     * 解析テストするためのメインルーチン
     *
     * @param args プログラムの引数
     */
    public static void main(String[] args) {
        SqPack_IndexFile index;
        index = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\060000.win32.index", true);
        byte[] data = index.extractFile("ui/uld/botanistgame.uld");

        @SuppressWarnings("unused")
        ULD_File uld = new ULD_File(index, data, ByteOrder.LITTLE_ENDIAN);

    }

    /**
     * ByteBuffer から固定長文字列を読み取るための関数
     *
     * @param buffer     読み取りバッファ
     * @param byteLength 読み取るバイト数
     * @return 指定されたバッファから指定された長さで読み取られた、トリミングされていない新しい文字列。
     */
    private static String getString(ByteBuffer buffer, int byteLength) {
        byte[] input = new byte[byteLength];
        buffer.get(input);
        return new String(input);
    }

    /**
     * ULD ヘッダー チャンク
     */
    public static class ULDH {
        public final ATKH[] atkhs = new ATKH[2];
        private final int atkh0offset;
        private final int atkh1offset;

        /**
         * 指定されたデータプールからこのULDヘッダーチャンクを初期化します
         *
         * @param data データプール
         */
        ULDH(ByteBuffer data) {
            String sig = getString(data, 8);
            if (sig.equalsIgnoreCase("uldh0100")) {
                atkh0offset = data.getInt();
                atkh1offset = data.getInt();
                if (atkh0offset > 0) {
                    data.position(atkh0offset);
                    atkhs[0] = new ATKH(data);
                }
                if (atkh1offset > 0) {
                    data.position(atkh1offset);
                    atkhs[1] = new ATKH(data);
                }
            } else {
                throw new RuntimeException("No ULDH Sginature");
            }
        }

        @Override
        public String toString() {
            return String.format("ULDH{atkh0offset=%d, atkh1offset=%d, atkhs=%s}\n",
                    atkh0offset,
                    atkh1offset,
                    Arrays.toString(atkhs));
        }
    }

    /**
     * ULD ATKヘッダー チャンク
     */
    public static class ATKH {
        public ASHD ashd;
        public TPHD tphd;
        public COHD cohd;
        TLHD tlhd;
        public WDHD wdhd;

        /**
         * 指定されたデータ プールを解析して、ATKH を初期化します。
         *
         * @param data 使用するデータプール
         */
        ATKH(ByteBuffer data) {
            int atkhOffset = data.position();
            String signature = getString(data, 8);
            if (signature.equalsIgnoreCase("atkh0100")) {
                int ashdOffset = data.getInt() & 0xFFFF;
                int tphdOffset = data.getInt() & 0xFFFF;
                int cohdOffset = data.getInt() & 0xFFFF;
                int tlhdOffset = data.getInt() & 0xFFFF;
                int wdhdOffset = data.getInt() & 0xFFFF;
                if (ashdOffset > 0) {
                    data.position(atkhOffset + ashdOffset);
                    ashd = new ASHD(data);
                }
                if (tphdOffset > 0) {
                    data.position(atkhOffset + tphdOffset);
                    tphd = new TPHD(data);
                }
                if (cohdOffset > 0) {
                    data.position(atkhOffset + cohdOffset);
                    cohd = new COHD(data);
                }
                if (tlhdOffset > 0) {
                    data.position(atkhOffset + tlhdOffset);
                    tlhd = new TLHD(data);
                }
                if (wdhdOffset > 0) {
                    data.position(atkhOffset + wdhdOffset);
                    wdhd = new WDHD(data);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("ATKH{ashd=%s, tphd=%s, cohd=%s, tlhd=%s, wdhd=%s}\n",
                    ashd != null ? ashd : "null",
                    tphd != null ? tphd : "null",
                    cohd != null ? cohd : "null",
                    tlhd != null ? tlhd : "null",
                    wdhd != null ? wdhd : "null");
        }
    }

    /**
     * ASHデータ チャンク
     */
    public static class ASHD {
        public final SparseArray<String> paths = new SparseArray<>();
        public String ashd_Ver = "1.00";

        /**
         * @param data 使用するデータプール
         */
        ASHD(ByteBuffer data) {
            boolean multiFlag = false;
            String[] multi = {"en/","fr/","de/", "ja/"};
            String signature = getString(data, 8);
            if (signature.equals("ashd0100")) {
                int count = data.getInt() & 0xFFFF;
                data.getInt();  //Align?
                for (int i = 0; i < count; i++) {
                    int index = data.getInt();
                    String path = getString(data, 0x30).trim();
                    paths.append(index, path);

                    //ファイル登録
                    String archive = HashDatabase.getArchiveID(path);
                    if (!archive.equals("*")) {
                        cAddPathToDB(path, archive,2);

                        if (!path.contains("_hr1")){
                            String folder = path.substring(0, path.lastIndexOf('/'));
                            String filename = path.substring(path.lastIndexOf('/') + 1);
                            String texName = filename.substring(0, filename.lastIndexOf('.'));
                            String fullPath = String.format("%s/%s_hr1.tex", folder, texName);
                            cAddPathToDB(fullPath, archive);

                        }

                    }
                }
            }else if (signature.equals("ashd0101")) {
                ashd_Ver = "1.01";
                //ashdのバージョンが上がった？
                int count = data.getInt() & 0xFFFF; //ファイル数
                data.getInt();  //Align?
                for (int i = 0; i < count; i++) {
                    //ファイルブロックは0x38byte (index:4byte,filePath:0x2Cbyte,iconID:4byte,不明:4byte)
                    int index = data.getInt();
                    String path = getString(data, 0x2c).trim();
                    String archive = HashDatabase.getArchiveID(path);
                    int iconID = data.getInt();
                    data.getInt(); //不明 通常0、時々3が入っている
                    //ファイルパスがiconIDの時、ファイルパスを生成
                    if (!path.contains(".")){
                        String iconPath = "";
                        String iconPath2 = "";
                        int pathNum = (iconID / 1000) * 1000;
                        if (iconID < 20000 || (iconID >= 60000 && iconID < 120000) || (iconID >= 130000 && iconID < 150000)){
                            iconPath = String.format("ui/icon/%06d/%06d.tex",pathNum, iconID);
                            iconPath2 = String.format("ui/icon/%06d/%06d_hr1.tex",pathNum, iconID); //高画質用
                        }else if (iconID < 60000){
                            iconPath = String.format("ui/icon/%06d/%s%06d.tex",pathNum, "hq/", iconID);
                            iconPath2 = String.format("ui/icon/%06d/%s%06d_hr1.tex",pathNum, "hq/", iconID); //高画質用
                        }else{
                            if ((iconID % 1000) == 0){multiFlag = true;}
                            if (multiFlag){
                                for (String lang :multi) {
                                    //multi配列中の「ja」を最後に配置しループ終わりが日本語となるようにした
                                    iconPath = String.format("ui/icon/%06d/%s%06d.tex",pathNum, lang, iconID);
                                    iconPath2 = String.format("ui/icon/%06d/%s%06d_hr1.tex",pathNum, lang, iconID); //高画質用

                                    //ui/icon登録
                                    if (currentIndex.findFile(iconPath) == 2) {
                                        archive = HashDatabase.getArchiveID(iconPath);
                                        cAddPathToDB(iconPath, archive);
                                        cAddPathToDB(iconPath2, archive); //高画質用
                                    }

                                    multiFlag = false;
                                }
                            }else{
                                iconPath = String.format("ui/icon/%06d/ja/%06d.tex",pathNum, iconID);
                                iconPath2 = String.format("ui/icon/%06d/ja/%06d_hr1.tex",pathNum, iconID); //高画質用
                            }
                        }
                        if (currentIndex.findFile(iconPath) == 2) {
                            archive = HashDatabase.getArchiveID(iconPath);
                            cAddPathToDB(iconPath2, archive); //高画質用
                        }
                        //高画質表示したい時は以下でiconPath2をpathに代入
                        path = iconPath;
                    }
                    if (path.contains(".")) {
                        paths.append(index, path);
                    }

                    //ファイル登録
                    if (!archive.equals("*")) {
                        cAddPathToDB(path, archive,2);

                        if (!path.contains("_hr1")){
                            String folder = path.substring(0, path.lastIndexOf('/'));
                            String filename = path.substring(path.lastIndexOf('/') + 1);
                            String texName = filename.substring(0, filename.lastIndexOf('.'));
                            String fullPath = String.format("%s/%s_hr1.tex", folder, texName);
                            cAddPathToDB(fullPath, archive);
                        }
                    }
                }
            }
        }
    }

    /**
     * TPHデータ チャンク
     */
    public static class TPHD {
        public final SparseArray<ImageSet> imageSets = new SparseArray<>();

        /**
         * 指定されたデータ プールから TPHD チャンクを初期化します
         *
         * @param data 使用するデータプール
         */
        TPHD(ByteBuffer data) {
            String signature = getString(data, 8);
            if (signature.equals("tphd0100")) {
                int count = data.getInt() & 0xFFFF;
                data.getInt(); //Align?
                for (int i = 0; i < count; i++) {
                    ImageSet set = new ImageSet(data);
                    imageSets.append(set.index, set);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("TPHD{imageSets=%s}\n", imageSets);
        }
    }

    /**
     * TLHデータ チャンク
     */
    public static class TLHD {
        final SparseArray<TLHDSet> entries = new SparseArray<>();

        /**
         * 指定されたデータ プールから TLHD チャンクを初期化します
         *
         * @param data 使用するデータプール
         */
        TLHD(ByteBuffer data) {
            String signature = getString(data, 8);
            if (signature.equals("tlhd0100")) {
                int count = data.getInt() & 0xFFFF;
                data.getInt(); //Align?
                for (int i = 0; i < count; i++) {
                    TLHDSet entry = new TLHDSet(data);
                    entries.put(entry.index, entry);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("TLHD{entries=%s}\n", entries);
        }
    }

    /**
     * COHデータ チャンク
     */
    public static class COHD {
        final SparseArray<COHDEntry> entries = new SparseArray<>();

        public SparseArray<COHDEntry> getEntries() {
            return entries;
        }

        /**
         * Initializes this COHD Chunk from the given data pool
         *
         * @param data The data pool to use
         */
        COHD(ByteBuffer data) {
            String signature = getString(data, 8);
            if (signature.equals("cohd0100")) {
                int count = data.getInt() & 0xFFFF;
                data.getInt(); //Align?
                for (int i = 0; i < count; i++) {
                    COHDEntry entry = new COHDEntry(data);
                    entries.put(entry.index, entry);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("COHD{entries=%s}\n", entries);
        }
    }

    /**
     * WDHデータ チャンク
     */
    public static class WDHD {
        final SparseArray<WDHDEntry> entries = new SparseArray<>();

        public SparseArray<WDHDEntry> getEntries() {
            return entries;
        }

        /**
         * Initializes this WDHD Chunk from the given data pool
         *
         * @param data The data pool to use
         */
        WDHD(ByteBuffer data) {
            String signature = getString(data, 8);
            if (signature.equals("wdhd0100")) {
                int count = data.getInt() & 0xFFFF;
                data.getInt(); //Align?
                for (int i = 0; i < count; i++) {
                    WDHDEntry entry = new WDHDEntry(data);
                    entries.put(entry.index, entry);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("WDHD{entries=%s}", entries);
        }
    }

    @Override
    public String toString() {
        return String.format("ULD_File{uldHeader=%s}\n",
                uldHeader != null ? uldHeader : "null");
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
    @SuppressWarnings("SameParameterValue")
    private static int cAddPathToDB(String fullPath, String archive, int regMode){
        int result = 0;
        SqPack_IndexFile temp_IndexFile = SqPack_IndexFile.GetIndexFileForArchiveID(archive, false);

        if (temp_IndexFile == null){
            return 0;
        }

        int pathCheck = temp_IndexFile.findFile(fullPath);
        if (pathCheck == 2){
            result = HashDatabase.addPathToDB(fullPath, archive);
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

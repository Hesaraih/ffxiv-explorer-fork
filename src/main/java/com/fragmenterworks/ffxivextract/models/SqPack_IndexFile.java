package com.fragmenterworks.ffxivextract.models;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.gui.components.Loading_Dialog;
import com.fragmenterworks.ffxivextract.helpers.EARandomAccessFile;
import com.fragmenterworks.ffxivextract.helpers.LERandomAccessFile;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import javax.swing.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SqPack_IndexFile {

    private final String path;
    private ByteOrder endian;

    private final SqPack_DataSegment[] segments = new SqPack_DataSegment[4];
    private SqPack_Folder[] packFolders;
    private boolean noFolder = false;

    private boolean isFastLoaded = false;
    private int totalFiles = 0;
    private int unHashedFiles;

    private static final Map<String, SqPack_IndexFile> cachedIndexes = new HashMap<>();

    /**
     * @param pathToIndex Indexファイルのパス
     * @param fastLoad 高速読み込みモード
     * @return ハッシュ値とオフセットを返す
     */
    public static SqPack_IndexFile createIndexFileForPath(String pathToIndex, boolean fastLoad) {
        //Fast load will blindly load all files regardless of folder
        String cacheKey = pathToIndex + (fastLoad ? ":fast" : "");
        if (cachedIndexes.containsKey(cacheKey)) {
            return cachedIndexes.get(cacheKey);
        } else {
            try {
                SqPack_IndexFile indexFile = new SqPack_IndexFile(pathToIndex, fastLoad);
                cachedIndexes.put(cacheKey, indexFile);
                return indexFile;
            } catch (IOException e) {
                Utils.getGlobalLogger().error(e);
            }
        }
        return null;
    }

    /**
     * コンストラクター： 主に、GUI関連のものを処理するためにFileManagerWindowによって使用されます。
     * すべてのファイル情報+構造情報と名前をロードします。
     *
     * @param pathToIndex         開きたいSqPackインデックスファイルへのパス
     * @param prgLoadingBar       コントロールするプログレスバー
     * @param lblLoadingBarString プログレスバーのテキスト
     */
    public SqPack_IndexFile(String pathToIndex, JProgressBar prgLoadingBar, JLabel lblLoadingBarString) throws IOException {

        path = pathToIndex;

        //リトルエンディアン用RandomAccessFile
        LERandomAccessFile lref = new LERandomAccessFile(pathToIndex, "r");
        //ビッグエンディアン用(通常)RandomAccessFile
        RandomAccessFile bref = new RandomAccessFile(pathToIndex, "r");

        int sqPackHeaderLength = checkSqPackHeader(lref, bref);
        if (sqPackHeaderLength < 0) {
            return;
        }

        //両エンディアン対応RandomAccessFile
        EARandomAccessFile ref = new EARandomAccessFile(pathToIndex, "r", endian);

        getSegments(ref, sqPackHeaderLength);

        // フォルダセグメントがあるかどうかを確認します。ない場合は...ファイルのみをロードします
        if (segments[3] != null && segments[3].offset != 0) {
            int offset = segments[3].getOffset();
            int size = segments[3].getSize();
            int numFolders = size / 0x10;

            if (prgLoadingBar != null) {
                prgLoadingBar.setMaximum(segments[0].getSize() / 0x10);
            }

            if (lblLoadingBarString != null) {
                lblLoadingBarString.setText("0%");
            }

            packFolders = new SqPack_Folder[numFolders];

            for (int i = 0; i < numFolders; i++) {
                ref.seek(offset + (i * 16)); // フォルダオフセットヘッダーはすべて16byte

                int id = ref.readInt();
                int fileIndexOffset = ref.readInt();
                int folderSize = ref.readInt();
                int numFiles = folderSize / 0x10;
                ref.readInt(); // 4byteスキップ

                packFolders[i] = new SqPack_Folder(id, numFiles, fileIndexOffset);
                unHashedFiles += packFolders[i].readFiles(ref, prgLoadingBar, lblLoadingBarString, false);
            }
        } else {
            //ファイルのみをロードの場合
            noFolder = true;

            if (prgLoadingBar != null) {
                prgLoadingBar.setMaximum((pathToIndex.contains("index2") ? 2 : 1) * segments[0].getSize() / 0x10);
            }

            if (lblLoadingBarString != null) {
                lblLoadingBarString.setText("0%");
            }

            packFolders = new SqPack_Folder[1];
            packFolders[0] = new SqPack_Folder(0, (pathToIndex.contains("index2") ? 2 : 1) * segments[0].getSize() / 0x10, segments[0].getOffset());
            unHashedFiles += packFolders[0].readFiles(ref, prgLoadingBar, lblLoadingBarString, pathToIndex.contains("index2"));
        }

        ref.close();

        for (SqPack_Folder folder : packFolders) {
            for (SqPack_File ignored : folder.files) {
                totalFiles++;
            }
        }
    }

    /**
     * コンストラクター
     *
     * @param pathToIndex 開きたいSqPackインデックスファイルへのパス(例：ゲームパス\sqpack\ffxiv\0a0000.win32.index)
     * @param fastLoad    これをtrueに設定すると、アーカイブのファイル情報のみが読み込まれ、その構造とファイル/フォルダー名が省略されます。
     */

    public SqPack_IndexFile(String pathToIndex, boolean fastLoad) throws IOException {

        path = pathToIndex;

        LERandomAccessFile lref = new LERandomAccessFile(pathToIndex, "r");
        RandomAccessFile bref = new RandomAccessFile(pathToIndex, "r");

        int sqpackHeaderLength = checkSqPackHeader(lref, bref);
        if (sqpackHeaderLength < 0) {
            return;
        }

        EARandomAccessFile ref = new EARandomAccessFile(pathToIndex, "r", endian);
        getSegments(ref, sqpackHeaderLength);

        //高速ロードでは、フォルダに関係なくすべてのファイルが盲目的にロードされます
        if (fastLoad) {
            isFastLoaded = true;

            noFolder = true;
            packFolders = new SqPack_Folder[1];
            packFolders[0] = new SqPack_Folder(0, (pathToIndex.contains("index2") ? 2 : 1) * segments[0].getSize() / 0x10, segments[0].getOffset());

            ref.seek(segments[0].getOffset());

            for (int i = 0; i < packFolders[0].files.length; i++) {
                int id = ref.readInt();
                if (!pathToIndex.contains("index2")) {
                    int id2 = ref.readInt();
                    long dataOffset = ref.readInt();
                    ref.readInt();

                    packFolders[0].getFiles()[i] = new SqPack_File(id, id2, dataOffset, false);
                } else {
                    long dataOffset = ref.readInt();
                    packFolders[0].getFiles()[i] = new SqPack_File(id, -1, dataOffset, false);
                }
            }
        } else {
            // フォルダセグメントがあるかどうかを確認します。ない場合はファイルのみをロードします
            if (segments[3] != null) {
                int offset = segments[3].getOffset();
                int size = segments[3].getSize();
                int numFolders = size / 0x10;

                packFolders = new SqPack_Folder[numFolders];

                for (int i = 0; i < numFolders; i++) {
                    ref.seek(offset + (i * 16)); // フォルダオフセットヘッダーはすべて16byte

                    int id = ref.readInt();
                    int fileIndexOffset = ref.readInt();
                    int folderSize = ref.readInt();
                    int numFiles = folderSize / 0x10;
                    ref.readInt(); // 4byteスキップ

                    packFolders[i] = new SqPack_Folder(id, numFiles,
                            fileIndexOffset);

                    unHashedFiles += packFolders[i].readFiles(ref, false);
                }
            } else {
                noFolder = true;
                packFolders = new SqPack_Folder[1];
                packFolders[0] = new SqPack_Folder(0, (pathToIndex.contains("index2") ? 2 : 1) * segments[0].getSize() / 0x10, segments[0].getOffset());
                unHashedFiles += packFolders[0].readFiles(ref, pathToIndex.contains("index2"));
            }
        }

        ref.close();

        for (SqPack_Folder folder : packFolders) {
            totalFiles += folder.files.length;
        }
    }

    /**
     * SqPackヘッダーをチェックし、ファイルポインタをそのサイズだけ進めます。
     */
	private int checkSqPackHeader(LERandomAccessFile ref, RandomAccessFile bref) throws IOException {
        // SqPackヘッダーをチェック
        byte[] buffer = new byte[6];
        byte[] bigBuffer = new byte[6];

        ref.readFully(buffer, 0, 6);
        bref.readFully(bigBuffer, 0, 6);

        String Magic = new String(buffer).trim();

        if (!Magic.equals("SqPack")) {
            ref.close();

            Utils.getGlobalLogger().error("SqPack magicが正しくありませんでした。");

            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                s.append(String.format("%X", buffer[i]));
            }
            String strMagic = new String(buffer);
            Utils.getGlobalLogger().debug("Magic 0x{} // {}", s.toString(), strMagic);
            return -1;
        }

        // ヘッダーサイズ取得
        ref.seek(0x0c);
        bref.seek(0x0c);
        int headerLength = ref.readInt();
        int bHeaderLength = bref.readInt();

        ref.readInt(); // 不明：4byteスキップ
        bref.readInt(); // 不明：4byteスキップ

        // ヘッダータイプを取得します。インデックスは2である必要があります
        int type = ref.readInt();
        int bType = bref.readInt();

        if (type != 2 && bType != 2) {
            Utils.getGlobalLogger().error("SqPackタイプが正しくありませんでした。");
            Utils.getGlobalLogger().debug("Type LE: {}, BE: {}", type, bType);
            return -1;
        }

        if (type == 2) {
            endian = ByteOrder.LITTLE_ENDIAN;
            return headerLength;
        }
        endian = ByteOrder.BIG_ENDIAN;
        return bHeaderLength;
    }

    @SuppressWarnings({"UnusedReturnValue", "unused"})
    private int getSegments(EARandomAccessFile ref, int segmentHeaderStart) throws IOException {

        ref.seek(segmentHeaderStart);

        int headerLength = ref.readInt();

        for (int i = 0; i < segments.length; i++) {
            int firstVal = ref.readInt();
            int offset = ref.readInt();
            int size = ref.readInt();
            byte[] sha1 = new byte[20];
            ref.readFully(sha1, 0, 20);
            segments[i] = new SqPack_DataSegment(offset, size, sha1);

            if (i == 0) {
                ref.skipBytes(0x4);
            }
            ref.skipBytes(0x28);
        }

        return headerLength;
    }

    /**
     * このアーカイブ内のフォルダを返します。
     */
    public SqPack_Folder[] getPackFolders() {
        return packFolders;
    }

    /**
     * このアーカイブにフォルダがない場合に返されます（通常はindex2の場合で、高速ロード用になります）
     */
    public boolean hasNoFolders() {
        return noFolder;
    }

    public int getNumUnHashedFiles() {
        return unHashedFiles;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < getPackFolders().length; i++) {
            b.append("Folder: ");
//			b.append(String.format("%X", getPackFolders()[i].getId()));
            b.append(getPackFolders()[i].getName());
            b.append("\n");

            b.append("Num files: ");
            b.append(getPackFolders()[i].getFiles().length);
            b.append("\n");

            b.append("Files:\n");

            for (int j = 0; j < getPackFolders()[i].getFiles().length; j++) {
                b.append("\t");
//				b.append(String.format("%X", getPackFolders()[i].getFiles()[j].id));
                b.append(getPackFolders()[i].getFiles()[j].getName());

                b.append(" @ offset ");

                b.append(String.format("%X", getPackFolders()[i].getFiles()[j].dataOffset));
                b.append("\n");
            }
        }

        return b.toString();
    }

    static class SqPack_DataSegment {

        private final int offset;
        private final int size;
        private final byte[] sha1;

        SqPack_DataSegment(int offset, int size, byte[] sha1) {
            this.offset = offset;
            this.size = size;
            this.sha1 = sha1;
        }

        int getOffset() {
            return offset;
        }

        int getSize() {
            return size;
        }

        @SuppressWarnings("unused")
        public byte[] getSha1() {
            return sha1;
        }
    }

    public static class SqPack_Folder {

        private final int id;
        private final SqPack_File[] files;
        private final long fileIndexOffset;
        private String name;

        /**
         * フォルダパス読み込み
         * @param id フォルダのハッシュ値
         * @param numFiles ファイル数
         * @param fileIndexOffset ファイルインデックスのオフセット
         */
        SqPack_Folder(int id, int numFiles, long fileIndexOffset) {
            this.id = id;
            this.files = new SqPack_File[numFiles];
            this.fileIndexOffset = fileIndexOffset;
            this.name = HashDatabase.getFolder(id);
            if (this.name == null) {
                this.name = String.format("~%x", id);
            }
            else if (Constants.DEBUG){
                HashDatabase.beginConnection();
                //いらないかな？
                try {
                    HashDatabase.setAutoCommit(false);
                } catch (SQLException e1) {
                    Utils.getGlobalLogger().error(e1);
                }

                HashDatabase.flagFolderNameAsUsed(id);

                try {
                    HashDatabase.commit();
                } catch (SQLException e) {
                    Utils.getGlobalLogger().error(e);
                }

                HashDatabase.closeConnection();
            }
        }

        /**
         * ファイル名読み込み(通常読み込み用)
         * @param ref Indexファイルのバッファ
         * @param prgLoadingBar プログレスバーコントロールへの参照
         * @param lblLoadingBarString プログレスバー用テキストコントロールへの参照
         * @param isIndex2 Index2ファイルの有無
         * @return ファイル名
         * @throws IOException IOエラー
         */
        int readFiles(EARandomAccessFile ref, JProgressBar prgLoadingBar, JLabel lblLoadingBarString, boolean isIndex2) throws IOException {
            ref.seek(fileIndexOffset);
            int namedFiles = 0;

            if (Constants.DEBUG){
                System.out.println(this.id + ": " + this.getName());
            }

            int i = 0;
            while (i < files.length) {
                if (!isIndex2) {
                    int id, id2; //id:ファイルハッシュ値、id2:フォルダハッシュ値

                    // ハッシュは実際には長いので、シフトして行う必要があります
                    if (ref.isBigEndian()) {
                        id2 = ref.readInt();
                        id = ref.readInt();
                    } else {
                        id = ref.readInt();
                        id2 = ref.readInt();
                    }

                    long dataOffset = ref.readInt();
                    ref.readInt();

                    files[i] = new SqPack_File(id, id2, dataOffset, true);

                } else {
                    //Index2がない場合
                    int id = ref.readInt();
                    long dataOffset = ref.readInt();
                    files[i] = new SqPack_File(id, -1, dataOffset, true);

                }

                if (prgLoadingBar != null) {
                    prgLoadingBar.setValue(prgLoadingBar.getValue() + 1);
                }
                if (lblLoadingBarString != null) {
                    lblLoadingBarString.setText((int) (Objects.requireNonNull(prgLoadingBar).getPercentComplete() * 100) + "%");
                }

                if (Constants.DEBUG){
                    System.out.println(files[i].id + ": " + files[i].getName());
                    System.out.println(files[i].id2 + ": " + files[i].getName());
                    if (!files[i].getName().startsWith("~")) {
                        System.out.println(this.getName() + "/" + files[i].getName());
                        namedFiles++;
                    }
                }
                i++;
            }
            return namedFiles;
        }

        /**
         * ファイル名読み込み(高速読み込み用)※おそらく未使用
         * @param ref Indexファイルのバッファ
         * @param isIndex2 Index2ファイルの有無
         * @return ファイル名
         * @throws IOException IOエラー
         */
        int readFiles(EARandomAccessFile ref, boolean isIndex2) throws IOException {
            ref.seek(fileIndexOffset);
            int namedFiles = 0;

            int i = 0;
            while (i < files.length) {
                int id = ref.readInt();
                if (!isIndex2) {
                    int id2 = ref.readInt();
                    long dataOffset = ref.readInt();
                    ref.readInt();

                    files[i] = new SqPack_File(id, id2, dataOffset, true);
                } else {
                    long dataOffset = ref.readInt();
                    files[i] = new SqPack_File(id, -1, dataOffset, true);
                }
                if (!files[i].getName().startsWith("~")) {
                    namedFiles++;
                }
                i++;
            }
            return namedFiles;
        }

        public int getId() {
            return id;
        }

        public SqPack_File[] getFiles() {
            return files;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class SqPack_File {
        public final int id;
		final int id2;
        public final long dataOffset;
        private String name;

        /**
         * ファイル名読み込み
         * @param id ファイルまたはフルパスのハッシュ値
         * @param id2 Index2のハッシュ値
         * @param offset オフセット
         * @param loadNames ファイル名の読み込み可否
         */
        public SqPack_File(int id, int id2, long offset, boolean loadNames) {
            this.id = id;
            this.id2 = id2;
            this.dataOffset = offset;

            //For Index2
            if (Constants.DEBUG){
                if (id2 == -1){
                    this.name = HashDatabase.getFullpath(id);
                }
            }

            if (loadNames) {
                if (id2 != -1) {
                    this.name = HashDatabase.getFileName(id);
                }
                if (this.name == null) {
                    this.name = String.format("~%x", id);
                }
            }
            else if (Constants.DEBUG){
                HashDatabase.beginConnection();
                //いらないかな？
                try {
                    HashDatabase.setAutoCommit(false);
                } catch (SQLException e1) {
                    Utils.getGlobalLogger().error(e1);
                }

                HashDatabase.flagFileNameAsUsed(id);

                try {
                    HashDatabase.commit();
                } catch (SQLException e) {
                    Utils.getGlobalLogger().error(e);
                }

                HashDatabase.closeConnection();
            }
        }

        /**
         * ファイルまたはフルパスのハッシュ値を取得
         * @return ファイルまたはフルパスのハッシュ値
         */
        public int getId() {
            return id;
        }

        public long getOffset() {
            return dataOffset;
        }

        /**
         * フォルダのハッシュ値を取得
         * @return フォルダのハッシュ値
         */
        public int getId2() {
            return id2;
        }

        /**
         *ファイル名を取得
         * @return ファイル名
         */
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         *ファイル名を取得 ファイル名が不明な場合はハッシュ(Hex)を取得
         * @return ファイル名またはハッシュ(Hex)
         */
        public String getName2() {
            String name = null;
            if (id2 != -1) {
                name = HashDatabase.getFileName(id);
            }
            if (name == null) {
                name = String.format("~%x", id);
            }
            return name;
        }
    }

    /**
     * 指定されたオフセットでファイルのコンテンツタイプを取得します。
     */
    public int getContentType(long dataOffset) throws IOException {
        String pathToOpen = path;

        int datNum = getDatNum(dataOffset);
        long realOffset = getOffsetInBytes(dataOffset);

        pathToOpen = pathToOpen.replace("index2", "dat" + datNum);
        pathToOpen = pathToOpen.replace("index", "dat" + datNum);

        SqPack_DatFile datFile = new SqPack_DatFile(pathToOpen, endian);
        int contentType = datFile.getContentType(realOffset);
        datFile.close();
        return contentType;
    }

    /**
     * 指定したフルパス先のファイルとパスの照合
     * @param fullPath ファイルパス
     * @return 2:ファイルがある 1:パスは合ってる 0:ファイル名もパスもなし
     */
    public Integer findFile(String fullPath){
        String folder,file;
        if (fullPath.contains(".")) {
            folder = fullPath.substring(0, fullPath.lastIndexOf("/"));
            file = fullPath.substring(fullPath.lastIndexOf("/") + 1);
        }else{
            folder = fullPath;
            file = "";
        }
        return findFile(folder, file);
    }

    /**
     * 指定したフルパス先のファイルの存在確認
     * @param folderName フォルダパス
     * @param filename ファイル名
     * @return  2:ファイルがある 1:パスは合ってる 0:ファイル名もパスもなし
     */
    public Integer findFile(String folderName, String filename){
        int returnValue = 0;
        if (getPath().contains("index2")) {
            String fullPath = folderName + "/" + filename;
            int hash = HashDatabase.computeCRC(fullPath.toLowerCase().getBytes(), 0, fullPath.getBytes().length);
            for (SqPack_File f : getPackFolders()[0].getFiles()) {
                if (f.getId() == hash) {
                    returnValue = 2;
                    return returnValue;
                }
            }
        } else {
            int hash1 = HashDatabase.computeCRC(folderName.toLowerCase().getBytes(), 0, folderName.getBytes().length);

            if (!isFastLoaded) {
                for (SqPack_Folder f : getPackFolders()) {
                    if (f.getId() == hash1) {
                        returnValue = 1;
                        int hash2 = HashDatabase.computeCRC(filename.toLowerCase().getBytes(), 0, filename.getBytes().length);
                        for (SqPack_File file : f.getFiles()) {
                            if (file.id == hash2) {
                                returnValue = 2;
                                return returnValue;
                            }
                        }

                        break;
                    }
                }
            } else {
                for (int i = 0; i < packFolders[0].getFiles().length; i++) {
                    SqPack_File file = packFolders[0].getFiles()[i];
                    if (file.getId2() == hash1) {
                        returnValue = 1;
                        int hash2 = HashDatabase.computeCRC(filename.toLowerCase().getBytes(), 0, filename.getBytes().length);
                        if (file.getId() == hash2) {
                            returnValue = 2;
                            return returnValue;
                        }
                    }
                }
            }
        }
        return returnValue;
    }

    /**
     * 指定されたパスでファイルを抽出します
     * @param fullPath　ファイルパス
     * @return ファイル
     * @throws IOException IOエラー
     */
    public byte[] extractFile(String fullPath) throws IOException {
        String folder = fullPath.substring(0, fullPath.lastIndexOf("/"));
        String file = fullPath.substring(fullPath.lastIndexOf("/") + 1);

        return extractFile(folder, file);
    }

    /**
     * 指定されたファイル名で指定されたフォルダーにあるファイルを抽出します
     * @param folderName フォルダパス
     * @param filename ファイル名
     * @return ファイル
     * @throws IOException IOエラー
     */
    public byte[] extractFile(String folderName, String filename) throws IOException {
        if (getPath().contains("index2")) {
            String fullPath = folderName + "/" + filename;
            int hash = HashDatabase.computeCRC(fullPath.toLowerCase().getBytes(), 0, fullPath.getBytes().length);
            for (SqPack_File f : getPackFolders()[0].getFiles()) {
                if (f.getId() == hash) {
                    return extractFile(f.getOffset());
                }
            }
        } else {
            int hash1 = HashDatabase.computeCRC(folderName.toLowerCase().getBytes(), 0, folderName.getBytes().length);

            if (!isFastLoaded) {
                for (SqPack_Folder f : getPackFolders()) {
                    if (f.getId() == hash1) {

                        int hash2 = HashDatabase.computeCRC(filename.toLowerCase().getBytes(), 0, filename.getBytes().length);
                        for (SqPack_File file : f.getFiles()) {
                            if (file.id == hash2) {
                                return extractFile(file.getOffset());
                            }
                        }

                        break;
                    }
                }
            } else {
                for (int i = 0; i < packFolders[0].getFiles().length; i++) {
                    SqPack_File file = packFolders[0].getFiles()[i];
                    if (file.getId2() == hash1) {
                        int hash2 = HashDatabase.computeCRC(filename.toLowerCase().getBytes(), 0, filename.getBytes().length);
                        if (file.getId() == hash2) {
                            return extractFile(file.getOffset());
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 指定されたオフセットでファイルデータを抽出します
     * @param dataOffset データのオフセット
     * @return ファイル
     * @throws IOException IOエラー
     */
	private byte[] extractFile(long dataOffset) throws IOException {
        return extractFile(dataOffset, null);
    }

    /**
     * 指定されたオフセットでファイルデータを抽出します...進捗バー情報も指定されています
     * @param dataOffset データのオフセット
     * @param loadingDialog ダイアログ
     * @return ファイル
     * @throws IOException IOエラー
     */
    public byte[] extractFile(long dataOffset, Loading_Dialog loadingDialog) throws IOException {

        String pathToOpen = path;

        int datNum = getDatNum(dataOffset);
        long realOffset = getOffsetInBytes(dataOffset);

        pathToOpen = pathToOpen.replace("index2", "dat" + datNum);
        pathToOpen = pathToOpen.replace("index", "dat" + datNum);

        SqPack_DatFile datFile = new SqPack_DatFile(pathToOpen, endian);
        byte[] data = datFile.extractFile(realOffset, loadingDialog);
        datFile.close();
        return data;
    }

    public int getDatNum(long dataOffset) {
        if (endian == ByteOrder.BIG_ENDIAN) {
            return 0;
        }
        return (int) ((dataOffset & 0x000F) / 2);
    }

    public long getOffsetInBytes(long dataOffset) {
        if (endian == ByteOrder.BIG_ENDIAN) {
            return dataOffset * 128;    //128 byte alignment
        }

        dataOffset -= dataOffset & 0x000F;
        return dataOffset * 8;            //8 byte alignment
    }

    /**
     * インデックスファイルの名前を返します
     * @return ファイル名
     */
    public String getName() {

        if (path.lastIndexOf("/") == -1) {
            String frontStripped = path.substring(path.lastIndexOf("\\") + 1);
            return frontStripped.substring(0, frontStripped.indexOf("."));
        }

        String frontStripped = path.substring(path.lastIndexOf("/") + 1);
        return frontStripped.substring(0, frontStripped.indexOf("."));

    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public Calendar getDatTimestamp(int datNum) throws IOException {
        String pathToOpen = path;

        //正しいデータ番号を取得する
        pathToOpen = pathToOpen.replace("index2", "dat" + datNum);
        pathToOpen = pathToOpen.replace("index", "dat" + datNum);

        SqPack_DatFile datFile = new SqPack_DatFile(pathToOpen, endian);
        Calendar timestamp = datFile.getTimeStamp();
        datFile.close();
        return timestamp;
    }

    public boolean isBigEndian() {
        return endian == ByteOrder.BIG_ENDIAN;
    }

    public ByteOrder getEndian() {
        return endian;
    }
}

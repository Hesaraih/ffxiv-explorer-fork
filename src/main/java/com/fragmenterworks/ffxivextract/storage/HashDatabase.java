package com.fragmenterworks.ffxivextract.storage;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.EARandomAccessFile;
import com.fragmenterworks.ffxivextract.helpers.Utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.util.HashMap;

public class HashDatabase {

    public static Connection globalConnection = null;

    private static HashMap<Long, String> files;
    private static HashMap<Long, String> folders;

    public static void init() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");

        try {
            if (globalConnection == null) {
                beginConnection();
            }

            files = new HashMap<>();
            folders = new HashMap<>();

            PreparedStatement fileq = globalConnection.prepareStatement("select hash, name from filenames");
            PreparedStatement folderq = globalConnection.prepareStatement("select hash, path from folders");

            ResultSet rs = fileq.executeQuery();
            while (rs.next()) {
                files.put(rs.getLong(1), rs.getString(2));
            }

            ResultSet rs2 = folderq.executeQuery();
            while (rs2.next()) {
                folders.put(rs2.getLong(1), rs2.getString(2));
            }

            rs.close();
            rs2.close();

        } catch (SQLException e) {
            // エラーメッセージが「メモリ不足」の場合は、データベースファイルが見つからない可能性があります
            Utils.getGlobalLogger().error(e);
        }
    }

    public static int getHashDBVersion() {
        String version = "-1";
        try {
            if (globalConnection == null) {
                beginConnection();
            }

            Statement statement = globalConnection.createStatement();
            ResultSet rs = statement.executeQuery("select * from dbinfo where type = 'version'");

            while (rs.next()) {
                version = rs.getString("value");
            }

            rs.close();
            statement.close();
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
            return -1;
        }

        return Integer.parseInt(version);
    }

    /**
     * hashlist.dbにフォルダパスのみ追加(カプセル化)
     * @param folderName フォルダ名
     * @param archive  Indexファイル名
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    @SuppressWarnings("unused")
    public static int addFolderToDB(String folderName, String archive) {
        return addFolderToDB(folderName, archive, false);
    }

    /**
     * フォルダパスのみ追加
     * @param folderName フォルダ名
     * @param archive   Indexファイル名
     * @param ForcedDB 強制登録フラグ
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    public static int addFolderToDB(String folderName, String archive, boolean ForcedDB) {
        if (globalConnection == null) {
            beginConnection();
        }

        return addPathToDB(folderName, archive, ForcedDB, true);
     }

    /**
     * hashlist.dbにファイルを追加(カプセル化)
     * dbに追加するために使用 パスとファイル名に分割し自動的にハッシュ
     * @param fullPath フルパス名
     * @param archive Indexファイル名
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    public static int addPathToDB(String fullPath, String archive) {
        if (globalConnection == null) {
            beginConnection();
        }

        return addPathToDB(fullPath, archive,false);
    }

    public static int addPathToDB(String fullPath, String archive, boolean ForcedDB) {
        if (globalConnection == null) {
            beginConnection();
        }
        return addPathToDB(fullPath, archive, ForcedDB,false);
    }

    /**
     * hashlist.dbにファイルを追加(実装)
     * dbに追加するために使用 パスとファイル名に分割し自動的にハッシュ
     * @param fullPath フルパス名
     * @param archive Indexファイル名
     * @param ForcedDB 強制追加モード true:強制上書き追加、false:hashが一致していた場合追加しない
     * @return 登録結果 0:登録失敗 1:登録成功 2:ファイル名変更 3:ファイルパス変更 4:登録済みのため何もしない
     */
    public static int addPathToDB(String fullPath, String archive, boolean ForcedDB, boolean IsFolder) {

        String folder;
        String filename = "";
        boolean IsFileOnly = false;
        int result = 1;
        if (IsFolder){
            //フォルダのみの処理
            if (fullPath.endsWith("/")) {
                folder = fullPath.substring(0, fullPath.length() - 1);
            }else{
                folder = fullPath;
            }
            if (fullPath.contains(".")){
                //ファイル名が入ってしまっている場合(プログラムミス以外ないはず)
                folder = fullPath.substring(0, fullPath.lastIndexOf('/'));
            }
        }else{
            folder = fullPath.substring(0, fullPath.lastIndexOf('/'));
            filename = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        }
        String folder_L = folder.toLowerCase(); //hash値計算用
        String filename_L = filename.toLowerCase(); //hash値計算用

        int folderHash = computeCRC(folder_L.getBytes(), 0, folder_L.getBytes().length);
        if (folder.startsWith("~")){
            String hex = folder.replace("~","");
            folderHash = Integer.decode(hex);
            IsFileOnly = true;
        }
        int fileHash = 0;
        if (!IsFolder) {
            fileHash = computeCRC(filename_L.getBytes(), 0, filename_L.getBytes().length);
        }

        boolean IsHashRegistered = false;
        //HashMapにキーが存在するか確認
        if (IsFolder) {
            if (folders.containsKey((long) folderHash) && !ForcedDB) {
                IsHashRegistered = true;
            }
        }
        else {
            if (folders.containsKey((long) folderHash) && files.containsKey((long) fileHash) && !ForcedDB) {
                IsHashRegistered = true;
            }
        }

        if (IsHashRegistered){
            String regFolder = folders.get((long) folderHash);

            if (regFolder.equals(folder) || IsFileOnly){
                //同じパス名が登録済みのため処理終了、ファイル名は違うかもしれないがHashは一致
                //ファイル名の上書き保存については強制登録を使用し明示的に実行したほうが安全なので自動化しない
                //大文字小文字の置き換えのみ自動的に上書き
                if (!(filename.equals(""))){
                    String regFile = files.get((long) fileHash);
                    if (regFile.equalsIgnoreCase(filename) && !regFile.equals(filename)){
                        //大文字・小文字が違う時のみ
                        Utils.getGlobalLogger().info("ファイル名を変更(上書き)します{} => {}", regFile, fullPath);
                    }else{
                        //ファイル名も完全一致している場合
                        return 2;
                    }
                }else{
                    //フォルダパスのみの登録実行時に既に同じパスが存在している場合
                    return 4;
                }
            }else {
                //同じHash値なのにパスが異なる時
                if(Constants.DEBUG){
                    String SavePath = "./フォルダハッシュ変更.txt";
                    try {
                        File file = new File(SavePath);
                        FileWriter filewriter = new FileWriter(file, true); //追記モード
                        //記録に残す。
                        String path = regFolder + " => " + fullPath + "\r\n";
                        filewriter.write(path);
                        filewriter.close();

                    } catch (FileNotFoundException e) {
                        Utils.getGlobalLogger().error(e);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //パスのみ変更する。(同Hash値、異パス名は存在しないはず:存在してもどちらかが間違い)
                Utils.getGlobalLogger().info("パスを変更(上書き)します{} => {}", regFolder, fullPath);
                result = 3;
            }
        }else{
            Utils.getGlobalLogger().info("{}をエントリーに追加しています", fullPath);
        }


        if(!archive.startsWith("0") || !archive.startsWith("1")){
            archive = getArchiveID(fullPath);
        }

        if (globalConnection == null) {
            beginConnection();
        }

        try {
            Statement statement = globalConnection.createStatement();
            statement.setQueryTimeout(30); //タイムアウトを30秒に設定
            if (!IsFileOnly) {
                statement.executeUpdate(String.format("insert or ignore into folders values(%d, '%s', 0, '%s', '%s')", folderHash, folder, archive, Constants.DB_VERSION_CODE));
                statement.executeUpdate(String.format("UPDATE  folders set path = '%s' where hash = %d and archive = '%s'", folder, folderHash, archive));
            }
            if (!IsFolder) {
                statement.executeUpdate(String.format("insert or ignore into filenames values(%d, '%s', 0, '%s', '%s')", fileHash, filename, archive, Constants.DB_VERSION_CODE));
                statement.executeUpdate(String.format("UPDATE  filenames set name = '%s' where hash = %d and archive = '%s'", filename, fileHash, archive));
            }
            statement.close();

            folders.put((long) folderHash, folder);
            if (!IsFolder) {
                files.put((long) fileHash, filename);
            }

        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
            return 0;
        }
        return result;
    }

    /**
     * フルパスからindexファイルの名前を取得
     * @param fullPath フルパス
     * @return indexファイル名の先頭6文字
     */
    public static String getArchiveID(String fullPath){
        if (fullPath.startsWith("common/")){return "000000";}
        else if (fullPath.startsWith("bgcommon/")){return "010000";}
        else if (fullPath.startsWith("bg/")){
            if (fullPath.charAt(5)=='x'){
                //bg/ffxiv
                return "020000";
            }
            else{
                //bg/ex1/01_
                return "020"+ fullPath.charAt(5) + fullPath.substring(7,9);
            }
        }
        else if (fullPath.startsWith("cut/")){
            if (fullPath.charAt(6)=='x'){
                //cut/ffxiv
                return "030000";
            }
            else{
                //cut/ex3/
                return "030"+fullPath.charAt(6)+"00";
            }
        }
        else if (fullPath.startsWith("chara/")){return "040000";}
        else if (fullPath.startsWith("shader/")){return "050000";}
        else if (fullPath.startsWith("ui/")){return "060000";}
        else if (fullPath.startsWith("sound/")){return "070000";}
        else if (fullPath.startsWith("vfx/")){return "080000";}
        else if (fullPath.startsWith("ui_script/")){return "090000";}
        else if (fullPath.startsWith("exd/")){return "0a0000";}
        else if (fullPath.startsWith("game_script/")){return "0b0000";}
        else if (fullPath.startsWith("music/")){
            if (fullPath.charAt(8)=='x'){
                //music/ffxiv
                return "0c0000";
            }
            else{
                //music/ex1
                return "0c0"+fullPath.charAt(8)+"00";
            }
        }
        else if (fullPath.startsWith("sqpack_test/")){return "120000";}
        else if (fullPath.startsWith("debug/")){return "130000";}
        return "*";
    }

    /**
     * 他のhashlist.dbからのインポート
     * @param selectedFile Hashlist.dbのフルパス
     */
    public static void importDatabase(File selectedFile) {

        if (globalConnection == null) {
            beginConnection();
        }
        HashDatabase.setAutoCommit(false);

        HashMap<Long, String> tmpFiles = new HashMap<>();
        HashMap<Long, String> tmpFileNameArchives = new HashMap<>();
        HashMap<Long, String> tmpFolders = new HashMap<>();

        int fileCount = 0;
        int folderCount = 0;
        try {
            PreparedStatement fileq = globalConnection.prepareStatement("select hash, name, archive from filenames");
            PreparedStatement folderq = globalConnection.prepareStatement("select hash, path from folders");

            ResultSet rs = fileq.executeQuery();
            while (rs.next()) {
                tmpFiles.put(rs.getLong(1), rs.getString(2));
                tmpFileNameArchives.put(rs.getLong(1), rs.getString(3));
            }
            rs.close();

            ResultSet rs2 = folderq.executeQuery();
            while (rs2.next()) {
                tmpFolders.put(rs2.getLong(1), rs2.getString(2));
            }
            rs2.close();

            for (Long val : tmpFiles.keySet()) {
                if (files.containsKey(val)) {
                    continue;
                }

                PreparedStatement fstmt = globalConnection.prepareStatement("insert or ignore into filenames values(?, ?, 0, ?, ?)");
                fstmt.setLong(1, val);
                fstmt.setString(2, tmpFiles.get(val));
                fstmt.setString(3, getArchiveID(tmpFiles.get(val)));
                fstmt.setInt(4, Constants.DB_VERSION_CODE);

                fstmt.execute();
                fstmt.close();
                fileCount++;
            }

            for (Long val : tmpFolders.keySet()) {
                if (folders.containsKey( val)) {
                    continue;
                }

                PreparedStatement fstmt = globalConnection.prepareStatement("insert or ignore into folders values(?, ?, 0, ?, ?)");
                fstmt.setLong(1, val);
                fstmt.setString(2, tmpFiles.get(val));
                if (tmpFileNameArchives.get(val).startsWith("0") || tmpFileNameArchives.get(val).startsWith("1")){
                    fstmt.setString(3, tmpFileNameArchives.get(val));
                }else{
                    fstmt.setString(3, "*");
                }
                fstmt.setString(3, "*");
                fstmt.setInt(4, Constants.DB_VERSION_CODE);

                fstmt.execute();
                fstmt.close();
                folderCount++;
            }

        } catch (Exception exc) {
            Utils.getGlobalLogger().error("パスを追加しようとしてエラーが発生しました。", exc);
        }

        HashDatabase.commit();

        Utils.getGlobalLogger().info("Added {} new folders and {} new files from {} to current database.", folderCount, fileCount, selectedFile.getAbsolutePath());
    }

    /**
     * テキストファイルからインポート
     * @param selectedFile テキストファイルのパス
     * @return 成功可否
     */
    public static int importFilePaths(File selectedFile) {

        if (globalConnection == null) {
            beginConnection();
        }
        HashDatabase.setAutoCommit(false);

        int count = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(selectedFile));

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String fullPath;
                try {
                    int indexof;
                    if ((indexof = line.indexOf(',')) == -1) {
                        fullPath = line;
                    } else {
                        fullPath = line.substring(indexof + 2);
                    }

                    if (HashDatabase.addPathToDB(fullPath, getArchiveID(fullPath), true) == 1) {
                        count++;
                    }
                } catch (java.lang.StringIndexOutOfBoundsException e) {
                    Utils.getGlobalLogger().error("行を解析できませんでした {}", line, e);
                }
            }
        } catch (Exception exc) {
            Utils.getGlobalLogger().error("パスを追加しようとしてエラーが発生しました。", exc);
            return count * -1;
        }

        HashDatabase.commit();

        return count;
    }

    // 文字列からパスのリストを読み取るために使用 (exeまたはメモリから抽出)
    @SuppressWarnings("unused")
    public static void loadPathsFromTXT(String path) {
        int numAdded = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;

            if (globalConnection == null) {
                beginConnection();
            }
            HashDatabase.setAutoCommit(false);

            while ((line = br.readLine()) != null) {
                if (line.equals("")) {
                    continue;
                }

                HashDatabase.addPathToDB(line, getArchiveID(line));

                numAdded++;
            }

            HashDatabase.commit();

            br.close();
        } catch (IOException e) {
            Utils.getGlobalLogger().error("TXTからのパスのバッチ追加でエラーが発生しました。", e);
        }
        Utils.getGlobalLogger().info("{}の新しいDBエントリを追加しました。", numAdded);
    }

    /**
     * これは簡易SQDBリーダーです。 0x800バイトのヘッダー、
     * 各エントリの他のヘッダーデータをスキップし、ハッシュ+パスで読み込みます。
     * エントリごとに0x108バイトをスキップします。
     * エントリヘッダーは次のとおり：
     * 4バイトの空きデータ、いくつかのval、コンテンツタイプ、4バイトの空きデータ。それからDBデータが始まります。
     * @param path sqlite3データのパス
     */
    @SuppressWarnings("unused")
    public static void loadPathsFromSQDB(String path) {
        try {
            //TODO: sqdbのロードに失敗した場合は、ここでエンディアンを変更
            EARandomAccessFile file = new EARandomAccessFile(path, "r", ByteOrder.LITTLE_ENDIAN);

            file.skipBytes(0x800); // Headers

            long folderHash;
            long fileHash;
            StringBuilder fullPath;
            String folder;
            String filename;

            // ヘッダーデータをスキップ
            file.readInt();
            file.readInt();
            file.readInt();
            file.readInt();
            long lastStartPosition;

            if (globalConnection == null) {
                beginConnection();
            }
            HashDatabase.setAutoCommit(false);

            // EOFに達するまで読みこむ
            while (true) {
                lastStartPosition = file.getFilePointer();

                // 読み込み
                file.readInt(); //fileHash
                file.readInt(); //folderHash

                fullPath = new StringBuilder();
                while (true) {
                    byte c = file.readByte();
                    if (c == 0) {
                        break;
                    } else {
                        fullPath.append((char) c);
                    }
                }

                HashDatabase.addPathToDB(fullPath.toString(), getArchiveID(fullPath.toString()));

                // Reset
                file.seek(lastStartPosition);

                // Check EOF
                if (file.getFilePointer() + 0x108 >= file.length()) {
                    break;
                }

                file.skipBytes(0x108);
            }

            HashDatabase.commit();

            file.close();
        } catch (IOException e) {
            Utils.getGlobalLogger().error("SQDBからのパスのバッチ追加でエラーが発生しました。", e);
        }
    }

    public static void beginConnection() {
        try {
            if (globalConnection == null) {
                globalConnection = DriverManager.getConnection("jdbc:sqlite:./hashlist.db");
            }
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

    public static void closeConnection() {
        try {
            globalConnection.close();
            globalConnection = null;
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

    public static String getFolder(long hash) {
        return folders.getOrDefault(hash, null);
    }

    public static String getFolder2(long hash) {
        return folders.getOrDefault(hash, String.format("~%x", hash));
    }

    public static String getFileName(long hash) {
        String fullPath = getFileFullPathName(hash);
        if (fullPath == null) {
            return null;
        }
        //return fullPath.substring(fullPath.lastIndexOf('/') + 1).toLowerCase();
        return fullPath.substring(fullPath.lastIndexOf('/') + 1);
    }

    public static String getFullpath(long hash) {
        String fullPath = getFileFullPathName(hash);
        if (fullPath == null) {
            return null;
        }
        //return fullPath.substring(fullPath.lastIndexOf('/') + 1).toLowerCase();
        return fullPath.substring(fullPath.lastIndexOf('/') + 1);
    }

    private static String getFileFullPathName(long hash) {
        return files.getOrDefault(hash, null);
    }

    //MSDNのCRCテーブルとコード、およびRozeDoyanawaのJava実装から作成されました
    public static int computeCRC(byte[] bytes, int offset, int length) {
        ByteBuffer pbBuffer = ByteBuffer.wrap(bytes, offset, length);
        pbBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int dwCRC = -1;
        int cbRunningLength = ((length < 4) ? 0 : ((length) / 4) * 4);
        int cbEndUnalignedBytes = length - cbRunningLength;
        for (int i = 0; i < cbRunningLength / 4; ++i) {
            dwCRC ^= pbBuffer.getInt();
            dwCRC = crc_table_0f091d0[dwCRC & 0x000000FF] ^
                    crc_table_0f08dd0[(dwCRC >>> 8) & 0x000000FF] ^
                    crc_table_0f089d0[(dwCRC >>> 16) & 0x000000FF] ^
                    crc_table_0f085d0[(dwCRC >>> 24) & 0x000000FF];
        }

        for (int i = 0; i < cbEndUnalignedBytes; ++i) {
            dwCRC = crc_table_0f085d0[(dwCRC ^ pbBuffer.get()) & 0x000000FF] ^ (dwCRC >>> 8);
        }

        return dwCRC;
    }

    /**
     * ファイル使用フラグを立てる
     * @param hash ファイルのハッシュ値
     */
    public static void flagFileNameAsUsed(int hash) {
        if (globalConnection == null) {
            beginConnection();
        }

        try {
            Statement statement = globalConnection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            statement.executeUpdate("update 'filenames' set used = 1 where hash = " + hash);
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

    /**
     * フォルダ使用フラグを立てる
     * @param hash フォルダのハッシュ値
     */
    public static void flagFolderNameAsUsed(int hash) {
        try {
            Statement statement = globalConnection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            statement.executeUpdate("update 'folders' set used = 1 where hash = " + hash);
        } catch (SQLException e) {
            Utils.getGlobalLogger().error(e);
        }
    }

    /**
     * SQLの自動更新の状態変更
     * @param flag true:自動更新する  、false:自働更新しない(トランザクションに記録しておく)
     */
    public static void setAutoCommit(boolean flag){
        if (globalConnection != null) {
            try {
                globalConnection.setAutoCommit(flag);
            } catch (SQLException e) {
                Utils.getGlobalLogger().error(e);
            }
        }
    }

    /**
     * SQLデータベースのトランザクションを終了しデータをDBに反映
     */
    public static void commit(){
        if (globalConnection != null) {
            try {
                globalConnection.commit();
                globalConnection.setAutoCommit(true);
            } catch (SQLException e) {
                Utils.getGlobalLogger().error(e);
            }
        }
    }

    //region CRCテーブル
    // Hashing
    private static final int[] crc_table_0f085d0 = new int[]{0x00000000, 0x77073096,
            0xEE0E612C, 0x990951BA, 0x076DC419, 0x706AF48F, 0xE963A535,
            0x9E6495A3, 0x0EDB8832, 0x79DCB8A4, 0xE0D5E91E, 0x97D2D988,
            0x09B64C2B, 0x7EB17CBD, 0xE7B82D07, 0x90BF1D91, 0x1DB71064,
            0x6AB020F2, 0xF3B97148, 0x84BE41DE, 0x1ADAD47D, 0x6DDDE4EB,
            0xF4D4B551, 0x83D385C7, 0x136C9856, 0x646BA8C0, 0xFD62F97A,
            0x8A65C9EC, 0x14015C4F, 0x63066CD9, 0xFA0F3D63, 0x8D080DF5,
            0x3B6E20C8, 0x4C69105E, 0xD56041E4, 0xA2677172, 0x3C03E4D1,
            0x4B04D447, 0xD20D85FD, 0xA50AB56B, 0x35B5A8FA, 0x42B2986C,
            0xDBBBC9D6, 0xACBCF940, 0x32D86CE3, 0x45DF5C75, 0xDCD60DCF,
            0xABD13D59, 0x26D930AC, 0x51DE003A, 0xC8D75180, 0xBFD06116,
            0x21B4F4B5, 0x56B3C423, 0xCFBA9599, 0xB8BDA50F, 0x2802B89E,
            0x5F058808, 0xC60CD9B2, 0xB10BE924, 0x2F6F7C87, 0x58684C11,
            0xC1611DAB, 0xB6662D3D, 0x76DC4190, 0x01DB7106, 0x98D220BC,
            0xEFD5102A, 0x71B18589, 0x06B6B51F, 0x9FBFE4A5, 0xE8B8D433,
            0x7807C9A2, 0x0F00F934, 0x9609A88E, 0xE10E9818, 0x7F6A0DBB,
            0x086D3D2D, 0x91646C97, 0xE6635C01, 0x6B6B51F4, 0x1C6C6162,
            0x856530D8, 0xF262004E, 0x6C0695ED, 0x1B01A57B, 0x8208F4C1,
            0xF50FC457, 0x65B0D9C6, 0x12B7E950, 0x8BBEB8EA, 0xFCB9887C,
            0x62DD1DDF, 0x15DA2D49, 0x8CD37CF3, 0xFBD44C65, 0x4DB26158,
            0x3AB551CE, 0xA3BC0074, 0xD4BB30E2, 0x4ADFA541, 0x3DD895D7,
            0xA4D1C46D, 0xD3D6F4FB, 0x4369E96A, 0x346ED9FC, 0xAD678846,
            0xDA60B8D0, 0x44042D73, 0x33031DE5, 0xAA0A4C5F, 0xDD0D7CC9,
            0x5005713C, 0x270241AA, 0xBE0B1010, 0xC90C2086, 0x5768B525,
            0x206F85B3, 0xB966D409, 0xCE61E49F, 0x5EDEF90E, 0x29D9C998,
            0xB0D09822, 0xC7D7A8B4, 0x59B33D17, 0x2EB40D81, 0xB7BD5C3B,
            0xC0BA6CAD, 0xEDB88320, 0x9ABFB3B6, 0x03B6E20C, 0x74B1D29A,
            0xEAD54739, 0x9DD277AF, 0x04DB2615, 0x73DC1683, 0xE3630B12,
            0x94643B84, 0x0D6D6A3E, 0x7A6A5AA8, 0xE40ECF0B, 0x9309FF9D,
            0x0A00AE27, 0x7D079EB1, 0xF00F9344, 0x8708A3D2, 0x1E01F268,
            0x6906C2FE, 0xF762575D, 0x806567CB, 0x196C3671, 0x6E6B06E7,
            0xFED41B76, 0x89D32BE0, 0x10DA7A5A, 0x67DD4ACC, 0xF9B9DF6F,
            0x8EBEEFF9, 0x17B7BE43, 0x60B08ED5, 0xD6D6A3E8, 0xA1D1937E,
            0x38D8C2C4, 0x4FDFF252, 0xD1BB67F1, 0xA6BC5767, 0x3FB506DD,
            0x48B2364B, 0xD80D2BDA, 0xAF0A1B4C, 0x36034AF6, 0x41047A60,
            0xDF60EFC3, 0xA867DF55, 0x316E8EEF, 0x4669BE79, 0xCB61B38C,
            0xBC66831A, 0x256FD2A0, 0x5268E236, 0xCC0C7795, 0xBB0B4703,
            0x220216B9, 0x5505262F, 0xC5BA3BBE, 0xB2BD0B28, 0x2BB45A92,
            0x5CB36A04, 0xC2D7FFA7, 0xB5D0CF31, 0x2CD99E8B, 0x5BDEAE1D,
            0x9B64C2B0, 0xEC63F226, 0x756AA39C, 0x026D930A, 0x9C0906A9,
            0xEB0E363F, 0x72076785, 0x05005713, 0x95BF4A82, 0xE2B87A14,
            0x7BB12BAE, 0x0CB61B38, 0x92D28E9B, 0xE5D5BE0D, 0x7CDCEFB7,
            0x0BDBDF21, 0x86D3D2D4, 0xF1D4E242, 0x68DDB3F8, 0x1FDA836E,
            0x81BE16CD, 0xF6B9265B, 0x6FB077E1, 0x18B74777, 0x88085AE6,
            0xFF0F6A70, 0x66063BCA, 0x11010B5C, 0x8F659EFF, 0xF862AE69,
            0x616BFFD3, 0x166CCF45, 0xA00AE278, 0xD70DD2EE, 0x4E048354,
            0x3903B3C2, 0xA7672661, 0xD06016F7, 0x4969474D, 0x3E6E77DB,
            0xAED16A4A, 0xD9D65ADC, 0x40DF0B66, 0x37D83BF0, 0xA9BCAE53,
            0xDEBB9EC5, 0x47B2CF7F, 0x30B5FFE9, 0xBDBDF21C, 0xCABAC28A,
            0x53B39330, 0x24B4A3A6, 0xBAD03605, 0xCDD70693, 0x54DE5729,
            0x23D967BF, 0xB3667A2E, 0xC4614AB8, 0x5D681B02, 0x2A6F2B94,
            0xB40BBE37, 0xC30C8EA1, 0x5A05DF1B, 0x2D02EF8D};
    private static final int[] crc_table_0f089d0 = new int[]{0x00000000, 0x191B3141,
            0x32366282, 0x2B2D53C3, 0x646CC504, 0x7D77F445, 0x565AA786,
            0x4F4196C7, 0xC8D98A08, 0xD1C2BB49, 0xFAEFE88A, 0xE3F4D9CB,
            0xACB54F0C, 0xB5AE7E4D, 0x9E832D8E, 0x87981CCF, 0x4AC21251,
            0x53D92310, 0x78F470D3, 0x61EF4192, 0x2EAED755, 0x37B5E614,
            0x1C98B5D7, 0x05838496, 0x821B9859, 0x9B00A918, 0xB02DFADB,
            0xA936CB9A, 0xE6775D5D, 0xFF6C6C1C, 0xD4413FDF, 0xCD5A0E9E,
            0x958424A2, 0x8C9F15E3, 0xA7B24620, 0xBEA97761, 0xF1E8E1A6,
            0xE8F3D0E7, 0xC3DE8324, 0xDAC5B265, 0x5D5DAEAA, 0x44469FEB,
            0x6F6BCC28, 0x7670FD69, 0x39316BAE, 0x202A5AEF, 0x0B07092C,
            0x121C386D, 0xDF4636F3, 0xC65D07B2, 0xED705471, 0xF46B6530,
            0xBB2AF3F7, 0xA231C2B6, 0x891C9175, 0x9007A034, 0x179FBCFB,
            0x0E848DBA, 0x25A9DE79, 0x3CB2EF38, 0x73F379FF, 0x6AE848BE,
            0x41C51B7D, 0x58DE2A3C, 0xF0794F05, 0xE9627E44, 0xC24F2D87,
            0xDB541CC6, 0x94158A01, 0x8D0EBB40, 0xA623E883, 0xBF38D9C2,
            0x38A0C50D, 0x21BBF44C, 0x0A96A78F, 0x138D96CE, 0x5CCC0009,
            0x45D73148, 0x6EFA628B, 0x77E153CA, 0xBABB5D54, 0xA3A06C15,
            0x888D3FD6, 0x91960E97, 0xDED79850, 0xC7CCA911, 0xECE1FAD2,
            0xF5FACB93, 0x7262D75C, 0x6B79E61D, 0x4054B5DE, 0x594F849F,
            0x160E1258, 0x0F152319, 0x243870DA, 0x3D23419B, 0x65FD6BA7,
            0x7CE65AE6, 0x57CB0925, 0x4ED03864, 0x0191AEA3, 0x188A9FE2,
            0x33A7CC21, 0x2ABCFD60, 0xAD24E1AF, 0xB43FD0EE, 0x9F12832D,
            0x8609B26C, 0xC94824AB, 0xD05315EA, 0xFB7E4629, 0xE2657768,
            0x2F3F79F6, 0x362448B7, 0x1D091B74, 0x04122A35, 0x4B53BCF2,
            0x52488DB3, 0x7965DE70, 0x607EEF31, 0xE7E6F3FE, 0xFEFDC2BF,
            0xD5D0917C, 0xCCCBA03D, 0x838A36FA, 0x9A9107BB, 0xB1BC5478,
            0xA8A76539, 0x3B83984B, 0x2298A90A, 0x09B5FAC9, 0x10AECB88,
            0x5FEF5D4F, 0x46F46C0E, 0x6DD93FCD, 0x74C20E8C, 0xF35A1243,
            0xEA412302, 0xC16C70C1, 0xD8774180, 0x9736D747, 0x8E2DE606,
            0xA500B5C5, 0xBC1B8484, 0x71418A1A, 0x685ABB5B, 0x4377E898,
            0x5A6CD9D9, 0x152D4F1E, 0x0C367E5F, 0x271B2D9C, 0x3E001CDD,
            0xB9980012, 0xA0833153, 0x8BAE6290, 0x92B553D1, 0xDDF4C516,
            0xC4EFF457, 0xEFC2A794, 0xF6D996D5, 0xAE07BCE9, 0xB71C8DA8,
            0x9C31DE6B, 0x852AEF2A, 0xCA6B79ED, 0xD37048AC, 0xF85D1B6F,
            0xE1462A2E, 0x66DE36E1, 0x7FC507A0, 0x54E85463, 0x4DF36522,
            0x02B2F3E5, 0x1BA9C2A4, 0x30849167, 0x299FA026, 0xE4C5AEB8,
            0xFDDE9FF9, 0xD6F3CC3A, 0xCFE8FD7B, 0x80A96BBC, 0x99B25AFD,
            0xB29F093E, 0xAB84387F, 0x2C1C24B0, 0x350715F1, 0x1E2A4632,
            0x07317773, 0x4870E1B4, 0x516BD0F5, 0x7A468336, 0x635DB277,
            0xCBFAD74E, 0xD2E1E60F, 0xF9CCB5CC, 0xE0D7848D, 0xAF96124A,
            0xB68D230B, 0x9DA070C8, 0x84BB4189, 0x03235D46, 0x1A386C07,
            0x31153FC4, 0x280E0E85, 0x674F9842, 0x7E54A903, 0x5579FAC0,
            0x4C62CB81, 0x8138C51F, 0x9823F45E, 0xB30EA79D, 0xAA1596DC,
            0xE554001B, 0xFC4F315A, 0xD7626299, 0xCE7953D8, 0x49E14F17,
            0x50FA7E56, 0x7BD72D95, 0x62CC1CD4, 0x2D8D8A13, 0x3496BB52,
            0x1FBBE891, 0x06A0D9D0, 0x5E7EF3EC, 0x4765C2AD, 0x6C48916E,
            0x7553A02F, 0x3A1236E8, 0x230907A9, 0x0824546A, 0x113F652B,
            0x96A779E4, 0x8FBC48A5, 0xA4911B66, 0xBD8A2A27, 0xF2CBBCE0,
            0xEBD08DA1, 0xC0FDDE62, 0xD9E6EF23, 0x14BCE1BD, 0x0DA7D0FC,
            0x268A833F, 0x3F91B27E, 0x70D024B9, 0x69CB15F8, 0x42E6463B,
            0x5BFD777A, 0xDC656BB5, 0xC57E5AF4, 0xEE530937, 0xF7483876,
            0xB809AEB1, 0xA1129FF0, 0x8A3FCC33, 0x9324FD72};
    private static final int[] crc_table_0f08dd0 = new int[]{0x00000000, 0x01C26A37,
            0x0384D46E, 0x0246BE59, 0x0709A8DC, 0x06CBC2EB, 0x048D7CB2,
            0x054F1685, 0x0E1351B8, 0x0FD13B8F, 0x0D9785D6, 0x0C55EFE1,
            0x091AF964, 0x08D89353, 0x0A9E2D0A, 0x0B5C473D, 0x1C26A370,
            0x1DE4C947, 0x1FA2771E, 0x1E601D29, 0x1B2F0BAC, 0x1AED619B,
            0x18ABDFC2, 0x1969B5F5, 0x1235F2C8, 0x13F798FF, 0x11B126A6,
            0x10734C91, 0x153C5A14, 0x14FE3023, 0x16B88E7A, 0x177AE44D,
            0x384D46E0, 0x398F2CD7, 0x3BC9928E, 0x3A0BF8B9, 0x3F44EE3C,
            0x3E86840B, 0x3CC03A52, 0x3D025065, 0x365E1758, 0x379C7D6F,
            0x35DAC336, 0x3418A901, 0x3157BF84, 0x3095D5B3, 0x32D36BEA,
            0x331101DD, 0x246BE590, 0x25A98FA7, 0x27EF31FE, 0x262D5BC9,
            0x23624D4C, 0x22A0277B, 0x20E69922, 0x2124F315, 0x2A78B428,
            0x2BBADE1F, 0x29FC6046, 0x283E0A71, 0x2D711CF4, 0x2CB376C3,
            0x2EF5C89A, 0x2F37A2AD, 0x709A8DC0, 0x7158E7F7, 0x731E59AE,
            0x72DC3399, 0x7793251C, 0x76514F2B, 0x7417F172, 0x75D59B45,
            0x7E89DC78, 0x7F4BB64F, 0x7D0D0816, 0x7CCF6221, 0x798074A4,
            0x78421E93, 0x7A04A0CA, 0x7BC6CAFD, 0x6CBC2EB0, 0x6D7E4487,
            0x6F38FADE, 0x6EFA90E9, 0x6BB5866C, 0x6A77EC5B, 0x68315202,
            0x69F33835, 0x62AF7F08, 0x636D153F, 0x612BAB66, 0x60E9C151,
            0x65A6D7D4, 0x6464BDE3, 0x662203BA, 0x67E0698D, 0x48D7CB20,
            0x4915A117, 0x4B531F4E, 0x4A917579, 0x4FDE63FC, 0x4E1C09CB,
            0x4C5AB792, 0x4D98DDA5, 0x46C49A98, 0x4706F0AF, 0x45404EF6,
            0x448224C1, 0x41CD3244, 0x400F5873, 0x4249E62A, 0x438B8C1D,
            0x54F16850, 0x55330267, 0x5775BC3E, 0x56B7D609, 0x53F8C08C,
            0x523AAABB, 0x507C14E2, 0x51BE7ED5, 0x5AE239E8, 0x5B2053DF,
            0x5966ED86, 0x58A487B1, 0x5DEB9134, 0x5C29FB03, 0x5E6F455A,
            0x5FAD2F6D, 0xE1351B80, 0xE0F771B7, 0xE2B1CFEE, 0xE373A5D9,
            0xE63CB35C, 0xE7FED96B, 0xE5B86732, 0xE47A0D05, 0xEF264A38,
            0xEEE4200F, 0xECA29E56, 0xED60F461, 0xE82FE2E4, 0xE9ED88D3,
            0xEBAB368A, 0xEA695CBD, 0xFD13B8F0, 0xFCD1D2C7, 0xFE976C9E,
            0xFF5506A9, 0xFA1A102C, 0xFBD87A1B, 0xF99EC442, 0xF85CAE75,
            0xF300E948, 0xF2C2837F, 0xF0843D26, 0xF1465711, 0xF4094194,
            0xF5CB2BA3, 0xF78D95FA, 0xF64FFFCD, 0xD9785D60, 0xD8BA3757,
            0xDAFC890E, 0xDB3EE339, 0xDE71F5BC, 0xDFB39F8B, 0xDDF521D2,
            0xDC374BE5, 0xD76B0CD8, 0xD6A966EF, 0xD4EFD8B6, 0xD52DB281,
            0xD062A404, 0xD1A0CE33, 0xD3E6706A, 0xD2241A5D, 0xC55EFE10,
            0xC49C9427, 0xC6DA2A7E, 0xC7184049, 0xC25756CC, 0xC3953CFB,
            0xC1D382A2, 0xC011E895, 0xCB4DAFA8, 0xCA8FC59F, 0xC8C97BC6,
            0xC90B11F1, 0xCC440774, 0xCD866D43, 0xCFC0D31A, 0xCE02B92D,
            0x91AF9640, 0x906DFC77, 0x922B422E, 0x93E92819, 0x96A63E9C,
            0x976454AB, 0x9522EAF2, 0x94E080C5, 0x9FBCC7F8, 0x9E7EADCF,
            0x9C381396, 0x9DFA79A1, 0x98B56F24, 0x99770513, 0x9B31BB4A,
            0x9AF3D17D, 0x8D893530, 0x8C4B5F07, 0x8E0DE15E, 0x8FCF8B69,
            0x8A809DEC, 0x8B42F7DB, 0x89044982, 0x88C623B5, 0x839A6488,
            0x82580EBF, 0x801EB0E6, 0x81DCDAD1, 0x8493CC54, 0x8551A663,
            0x8717183A, 0x86D5720D, 0xA9E2D0A0, 0xA820BA97, 0xAA6604CE,
            0xABA46EF9, 0xAEEB787C, 0xAF29124B, 0xAD6FAC12, 0xACADC625,
            0xA7F18118, 0xA633EB2F, 0xA4755576, 0xA5B73F41, 0xA0F829C4,
            0xA13A43F3, 0xA37CFDAA, 0xA2BE979D, 0xB5C473D0, 0xB40619E7,
            0xB640A7BE, 0xB782CD89, 0xB2CDDB0C, 0xB30FB13B, 0xB1490F62,
            0xB08B6555, 0xBBD72268, 0xBA15485F, 0xB853F606, 0xB9919C31,
            0xBCDE8AB4, 0xBD1CE083, 0xBF5A5EDA, 0xBE9834ED};

    private static final int[] crc_table_0f091d0 = new int[]{0x00000000, 0xB8BC6765,
            0xAA09C88B, 0x12B5AFEE, 0x8F629757, 0x37DEF032, 0x256B5FDC,
            0x9DD738B9, 0xC5B428EF, 0x7D084F8A, 0x6FBDE064, 0xD7018701,
            0x4AD6BFB8, 0xF26AD8DD, 0xE0DF7733, 0x58631056, 0x5019579F,
            0xE8A530FA, 0xFA109F14, 0x42ACF871, 0xDF7BC0C8, 0x67C7A7AD,
            0x75720843, 0xCDCE6F26, 0x95AD7F70, 0x2D111815, 0x3FA4B7FB,
            0x8718D09E, 0x1ACFE827, 0xA2738F42, 0xB0C620AC, 0x087A47C9,
            0xA032AF3E, 0x188EC85B, 0x0A3B67B5, 0xB28700D0, 0x2F503869,
            0x97EC5F0C, 0x8559F0E2, 0x3DE59787, 0x658687D1, 0xDD3AE0B4,
            0xCF8F4F5A, 0x7733283F, 0xEAE41086, 0x525877E3, 0x40EDD80D,
            0xF851BF68, 0xF02BF8A1, 0x48979FC4, 0x5A22302A, 0xE29E574F,
            0x7F496FF6, 0xC7F50893, 0xD540A77D, 0x6DFCC018, 0x359FD04E,
            0x8D23B72B, 0x9F9618C5, 0x272A7FA0, 0xBAFD4719, 0x0241207C,
            0x10F48F92, 0xA848E8F7, 0x9B14583D, 0x23A83F58, 0x311D90B6,
            0x89A1F7D3, 0x1476CF6A, 0xACCAA80F, 0xBE7F07E1, 0x06C36084,
            0x5EA070D2, 0xE61C17B7, 0xF4A9B859, 0x4C15DF3C, 0xD1C2E785,
            0x697E80E0, 0x7BCB2F0E, 0xC377486B, 0xCB0D0FA2, 0x73B168C7,
            0x6104C729, 0xD9B8A04C, 0x446F98F5, 0xFCD3FF90, 0xEE66507E,
            0x56DA371B, 0x0EB9274D, 0xB6054028, 0xA4B0EFC6, 0x1C0C88A3,
            0x81DBB01A, 0x3967D77F, 0x2BD27891, 0x936E1FF4, 0x3B26F703,
            0x839A9066, 0x912F3F88, 0x299358ED, 0xB4446054, 0x0CF80731,
            0x1E4DA8DF, 0xA6F1CFBA, 0xFE92DFEC, 0x462EB889, 0x549B1767,
            0xEC277002, 0x71F048BB, 0xC94C2FDE, 0xDBF98030, 0x6345E755,
            0x6B3FA09C, 0xD383C7F9, 0xC1366817, 0x798A0F72, 0xE45D37CB,
            0x5CE150AE, 0x4E54FF40, 0xF6E89825, 0xAE8B8873, 0x1637EF16,
            0x048240F8, 0xBC3E279D, 0x21E91F24, 0x99557841, 0x8BE0D7AF,
            0x335CB0CA, 0xED59B63B, 0x55E5D15E, 0x47507EB0, 0xFFEC19D5,
            0x623B216C, 0xDA874609, 0xC832E9E7, 0x708E8E82, 0x28ED9ED4,
            0x9051F9B1, 0x82E4565F, 0x3A58313A, 0xA78F0983, 0x1F336EE6,
            0x0D86C108, 0xB53AA66D, 0xBD40E1A4, 0x05FC86C1, 0x1749292F,
            0xAFF54E4A, 0x322276F3, 0x8A9E1196, 0x982BBE78, 0x2097D91D,
            0x78F4C94B, 0xC048AE2E, 0xD2FD01C0, 0x6A4166A5, 0xF7965E1C,
            0x4F2A3979, 0x5D9F9697, 0xE523F1F2, 0x4D6B1905, 0xF5D77E60,
            0xE762D18E, 0x5FDEB6EB, 0xC2098E52, 0x7AB5E937, 0x680046D9,
            0xD0BC21BC, 0x88DF31EA, 0x3063568F, 0x22D6F961, 0x9A6A9E04,
            0x07BDA6BD, 0xBF01C1D8, 0xADB46E36, 0x15080953, 0x1D724E9A,
            0xA5CE29FF, 0xB77B8611, 0x0FC7E174, 0x9210D9CD, 0x2AACBEA8,
            0x38191146, 0x80A57623, 0xD8C66675, 0x607A0110, 0x72CFAEFE,
            0xCA73C99B, 0x57A4F122, 0xEF189647, 0xFDAD39A9, 0x45115ECC,
            0x764DEE06, 0xCEF18963, 0xDC44268D, 0x64F841E8, 0xF92F7951,
            0x41931E34, 0x5326B1DA, 0xEB9AD6BF, 0xB3F9C6E9, 0x0B45A18C,
            0x19F00E62, 0xA14C6907, 0x3C9B51BE, 0x842736DB, 0x96929935,
            0x2E2EFE50, 0x2654B999, 0x9EE8DEFC, 0x8C5D7112, 0x34E11677,
            0xA9362ECE, 0x118A49AB, 0x033FE645, 0xBB838120, 0xE3E09176,
            0x5B5CF613, 0x49E959FD, 0xF1553E98, 0x6C820621, 0xD43E6144,
            0xC68BCEAA, 0x7E37A9CF, 0xD67F4138, 0x6EC3265D, 0x7C7689B3,
            0xC4CAEED6, 0x591DD66F, 0xE1A1B10A, 0xF3141EE4, 0x4BA87981,
            0x13CB69D7, 0xAB770EB2, 0xB9C2A15C, 0x017EC639, 0x9CA9FE80,
            0x241599E5, 0x36A0360B, 0x8E1C516E, 0x866616A7, 0x3EDA71C2,
            0x2C6FDE2C, 0x94D3B949, 0x090481F0, 0xB1B8E695, 0xA30D497B,
            0x1BB12E1E, 0x43D23E48, 0xFB6E592D, 0xE9DBF6C3, 0x516791A6,
            0xCCB0A91F, 0x740CCE7A, 0x66B96194, 0xDE0506F1};
    //endregion
}

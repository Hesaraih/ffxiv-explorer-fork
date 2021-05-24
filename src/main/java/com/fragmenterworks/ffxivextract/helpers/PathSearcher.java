package com.fragmenterworks.ffxivextract.helpers;

import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_File;
import com.fragmenterworks.ffxivextract.models.SqPack_IndexFile.SqPack_Folder;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;

import javax.swing.*;
import java.io.*;
import java.sql.SQLException;

public class PathSearcher extends JFrame {
    public PathSearcher() {
    }
/*
	private static final String[] folders = {
			"common/",
			"bgcommon/",
			"exd/",
			"bg/",
			"music/",
			"game_script/",
			"cut/",
			"chara/",
			"ui/",
			"sound/",
			"shader/",
			"vfx/",
	};
	*/

    private static final String[] folders = {

            "bgcommon/"
    };

    public static void doPathSearch(String pathToIndex, String folder) throws IOException {
        folders[0] = folder;
        doPathSearch(pathToIndex);
    }

    private static void doPathSearch(String pathToIndex) throws IOException {

        Utils.getGlobalLogger().info("{}を開いています...", pathToIndex);

        SqPack_IndexFile currentIndexFile = new SqPack_IndexFile(pathToIndex, true);

        int numFound = 0;
        int numFoundFolder = 0;

        int numNewFound = 0;
        int numNewFoundFolder = 0;

        for (String folder : folders) {
            Utils.getGlobalLogger().info("フォルダ{}を検索しています...", folder);

            HashDatabase.beginConnection();
            try {
                HashDatabase.setAutoCommit(false);
            } catch (SQLException e1) {
                Utils.getGlobalLogger().error(e1);
            }
            for (int i = 0; i < currentIndexFile.getPackFolders().length; i++) {
                try {
                    HashDatabase.commit();
                } catch (SQLException e1) {
                    Utils.getGlobalLogger().error(e1);
                }
                SqPack_Folder f = currentIndexFile.getPackFolders()[i];
                for (int j = 0; j < f.getFiles().length; j++) {
                    SqPack_File fi = f.getFiles()[j];
                    byte[] data;
                    try {
                        if (currentIndexFile.getContentType(fi.dataOffset) == 4) {
                            continue;
                        }
                        data = currentIndexFile.extractFile(fi.dataOffset, null);
                        if (data == null || (data.length >= 8 && data[0] == 'S' && data[1] == 'E' && data[2] == 'D' && data[3] == 'B' && data[4] == 'S' && data[5] == 'S' && data[6] == 'C' && data[7] == 'F')) {
                            continue;
                        }

                        for (int i2 = 0; i2 < data.length - folder.length(); i2++) {
                            for (int j2 = 0; j2 < folder.length(); j2++) {
                                if (data[i2 + j2] == folder.charAt(j2)) {
                                    if (j2 == folder.length() - 1) {

                                        //Check if this is bgcommon while looking for common
                                        if (data[i2 - 1] == 'g' && data[i2 - 2] == 'b' || data[i2 - 1] == '/') {
                                            break;
                                        }
                                        //Look for end
                                        int endString = 0;
                                        for (int endSearch = i2; endSearch < data.length - folder.length(); endSearch++) {
                                            if (data[endSearch] == 0) {
                                                endString = endSearch;
                                                break;
                                            }
                                        }
                                        //Hack for last file
                                        if (endString == 0) {
                                            endString = data.length - 1;
                                        }
                                        //Get full pathToIndex
                                        String fullpath = new String(data, i2, endString - i2);

                                        //Add to list
                                        Utils.getGlobalLogger().info("NEW => {}", fullpath);
                                        numNewFound++;
                                        numNewFoundFolder++;
                                        HashDatabase.addPathToDB(fullpath, currentIndexFile.getName());
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        Utils.getGlobalLogger().error(e);
                    }

                }

            }
            Utils.getGlobalLogger().info("パスを{}個発見し、うち{}は新しいものでした。", numFoundFolder, numNewFoundFolder);
            numFoundFolder = 0;
            numNewFoundFolder = 0;
            try {
                HashDatabase.commit();
            } catch (SQLException e) {
                Utils.getGlobalLogger().error(e);
            }
            HashDatabase.closeConnection();
        }
        Utils.getGlobalLogger().info("ファイルを{}個発見し、うち{}は新しいものでした。", numFound, numNewFound);
    }

    @SuppressWarnings("unused")
    public static void addModelsFromItemsTable(String path) {
        InputStream in;
        BufferedWriter writer;

        boolean readingName = true;

        try {
            in = new FileInputStream(path);
            writer = new BufferedWriter(new FileWriter("./exddump2.txt"));

            while (true) {
                int b = in.read();

                if (b == -1) {
                    break;
                }

                if (b == ',' || b == 0x0D) {
                    if (b == 0x0D) {
                        //noinspection ResultOfMethodCallIgnored
                        in.read();
                    }
                    if (readingName) {
                        writer.append("\r\n");
                    }
                    readingName = !readingName;
                }

                if (readingName) {
                    writer.append((char) b);
                }
            }
            in.close();
            writer.close();
        } catch (IOException e1) {
            Utils.getGlobalLogger().error(e1);
        }

    }

}

package com.fragmenterworks.ffxivextract;

import com.fragmenterworks.ffxivextract.gui.FileManagerWindow;
import com.fragmenterworks.ffxivextract.gui.components.Update_Dialog;
import com.fragmenterworks.ffxivextract.helpers.PathSearcher;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.helpers.VersionUpdater;
import com.fragmenterworks.ffxivextract.helpers.VersionUpdater.VersionCheckObject;
import com.fragmenterworks.ffxivextract.storage.HashDatabase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class Main {

    public static void main(String[] args) {

        Utils.getGlobalLogger().info("FFXIV Explorerを開始しています...");

        // Set to windows UI
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Init the hash database
        try {
            @SuppressWarnings("unused")
            File dbFile = new File("./" + Constants.DBFILE_NAME);
            if (dbFile.exists()) {
                HashDatabase.init();
            } else {
                JOptionPane.showMessageDialog(null,
                        Paths.get(dbFile.getAbsolutePath()).normalize().toString()+ "が見つかりませんでした。\nファイル名またはフォルダ名は表示されません...代わりに、ファイルのハッシュが表示されます。",
                        "Hash DB 読み込みエラー", JOptionPane.ERROR_MESSAGE);
            }
        } catch (ClassNotFoundException e1) {
            Utils.getGlobalLogger().error(e1);
        }

        Level currentLevel = LogManager.getRootLogger().getLevel();

        // Arguments
        if (args.length > 0) {

            // Info
            if (args.length == 1) {
                if (args[0].equals("-help")) {
                    System.out.println("コマンド: -help, -debug, -pathsearch");
                } else if (args[0].equals("-pathsearch")) {
                    System.out.println("<str>で始まる文字列をアーカイブで検索します。" +
                            "\n-pathsearch <path to index> <str>");
                }
            }

            if (args[0].equals("-debug") && currentLevel.intLevel() < Level.DEBUG.intLevel()) {
                Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
                Constants.DEBUG = true;
            }

            //Constants.DEBUG = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

            // PATHSEARCH
            if (args[0].equals("-pathsearch")) {
                if (args.length < 3) {
                    Utils.getGlobalLogger().info("パス検索の引数が少なすぎます!");
                    return;
                }

                Utils.getGlobalLogger().info("パス検索開始(しばらく時間がかかります)");

                try {
                    PathSearcher.doPathSearch(args[1], args[2]);
                } catch (IOException e) {
                    Utils.getGlobalLogger().error("パス検索中にエラーが発生しました。", e);
                }
                return;
            }
        }

        Utils.getGlobalLogger().info("ロギングを{}に設定", LogManager.getRootLogger().getLevel());

        // Open up the main window
        FileManagerWindow fileMan = new FileManagerWindow(Constants.APPNAME);
        fileMan.setVisible(true);

        // Load Prefs
        Preferences prefs = Preferences
                .userNodeForPackage(com.fragmenterworks.ffxivextract.Main.class);
        boolean firstRun = prefs.getBoolean(Constants.PREF_FIRSTRUN, true);
        Constants.datPath = prefs.get(Constants.PREF_DAT_PATH, null);

        // First Run
        if (firstRun) {
            prefs.putBoolean(Constants.PREF_FIRSTRUN, false);

            int n = JOptionPane
                    .showConfirmDialog(
                            fileMan,
                            "FFXIV Extractorで新しいハッシュデータベースをチェックしますか？",
                            "Hash DB Version Check", JOptionPane.YES_NO_OPTION);
            prefs.putBoolean(Constants.PREF_DO_DB_UPDATE, n == JOptionPane.YES_OPTION);
        }

        // Version Check
        if (prefs.getBoolean(Constants.PREF_DO_DB_UPDATE, false)) {
            VersionCheckObject checkObj = VersionUpdater.checkForUpdates();

            if (HashDatabase.getHashDBVersion() < checkObj.currentDbVer
                    || Constants.APP_VERSION_CODE < checkObj.currentAppVer) {
                Update_Dialog updateDialog = new Update_Dialog(checkObj);
                updateDialog.setLocationRelativeTo(fileMan);
                updateDialog.setVisible(true);
            }
        }

    }

}

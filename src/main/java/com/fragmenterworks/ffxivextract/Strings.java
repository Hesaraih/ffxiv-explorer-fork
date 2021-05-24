package com.fragmenterworks.ffxivextract;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class Strings {

    //Dialog Titles
    public static final String DIALOG_TITLE_ABOUT = "FFXIV Explorerについて";
    public static final String DIALOG_TITLE_MUSICSWAPPER = "Music 交換ツール";
    public static final String DIALOG_TITLE_FILEINJECT = "File Injector";
    public static final String DIALOG_TITLE_SCDCONVERTER = "OGG→SCD コンバーター";
    public static final String DIALOG_TITLE_MODELVIEWER = "モデルビューア";
    public static final String DIALOG_TITLE_OUTFITTER = "Outfitter (テスト中)";
    public static final String DIALOG_TITLE_SEARCH = "検索";
    public static final String DIALOG_TITLE_SETTINGS = "設定";
    public static final String DIALOG_TITLE_ERROR = "エラー";

    //About Dialog
    public static final String ABOUTDIALOG_VERSION = "バージョン:";
    public static final String ABOUTDIALOG_GITVERSION = "Git Commit:";

    //Music Swapper
    public static final String MUSICSWAPPER_FRAMETITLE_ARCHIVE = "Musicアーカイブ";
    public static final String MUSICSWAPPER_FRAMETITLE_SWAPPING = "交換中";
    public static final String MUSICSWAPPER_PATHTOFILE = "Path to music pack: ";
    public static final String MUSICSWAPPER_DEFAULTPATHTEXT = "Point to 0c0000.win32.index";
    public static final String MUSICSWAPPER_FROMID = "From Id:";
    public static final String MUSICSWAPPER_TOID = "Set to:";
    public static final String MUSICSWAPPER_ORIGINALID = "Original Id:";
    public static final String MUSICSWAPPER_CURRENTOFFSET = "Currently set to offset: %08X";
    public static final String MUSICSWAPPER_CURRENTSETTO = "Currently set to: ";

    //File Inject
    public static final String FILEINJECT_FRAMETITLE_ARCHIVE = "Dat index";
    public static final String FILEINJECT_PATHTOFILE = "Path to index: ";
    public static final String FILEINJECT_DEFAULTPATHTEXT = "No index selected";

    //Path to Hash Tool
    public static final String PATHTOHASH_TITLE = "パス -> ハッシュ値 計算";
    public static final String PATHTOHASH_PATH = "パス: ";
    public static final String PATHTOHASH_FOLDER_HASH = "フォルダ ハッシュ値: ";
    public static final String PATHTOHASH_FILE_HASH = "ファイル ハッシュ値: ";
    public static final String PATHTOHASH_FULL_HASH = "フルパス ハッシュ値: ";
    public static final String PATHTOHASH_BUTTON_HASHTHIS = "Hash値を強制登録";
    public static final String PATHTOHASH_BUTTON_CLOSE = "閉じる";
    public static final String PATHTOHASH_INTRO = "「folder/subfolder/file.ext」のような有効なパスを入力し\n計算ボタンを押してください。";
    public static final String PATHTOHASH_ERROR_INVALID = "有効なパスではありません。";

    //Search Window
    public static final String SEARCH_FRAMETITLE_BYSTRING = "文字列";
    public static final String SEARCH_FRAMETITLE_BYBYTES = "バイトデータ";
    public static final String SEARCH_SEARCH = "検索: ";

    //File Types
    public static final String FILETYPE_FFXIV_INDEX = "FFXIV Index File (.index)";
    public static final String FILETYPE_FFXIV_INDEX2 = "FFXIV Index2 File (.index2)";
    public static final String FILETYPE_FFXIV_MUSICINDEX = "FFXIV Music Archive Index (0c0000.win32.index)";
    public static final String FILETYPE_OGG = "OGG Vorbis File (.ogg)";
    public static final String FILETYPE_FFXIV_LOG = "FFXIV Log File (.log)";

    //Menu and Menu Items
    public static final String MENU_FILE = "ファイル(F)";
    public static final String MENU_SEARCH = "検索(S)";
    public static final String MENU_DATAVIEWERS = "データビューア(V)";
    public static final String MENU_TOOLS = "ツール(T)";
    public static final String MENU_DATABASE = "データベース(D)";
    public static final String MENU_OPTIONS = "オプション(O)";
    public static final String MENU_HELP = "ヘルプ(H)";

    public static final String MENUITEM_OPEN = "開く";
    public static final String MENUITEM_CLOSE = "閉じる";
    public static final String MENUITEM_EXTRACT = "出力";
    public static final String MENUITEM_EXTRACTRAW = "出力(Rawデータ)";
    public static final String MENUITEM_SEARCH = "検索";
    public static final String MENUITEM_SEARCHAGAIN = "再検索";
    public static final String MENUITEM_MODELVIEWER = "モデルビューア";
    public static final String MENUITEM_OUTFITTER = "Outfitter (テスト中)";
    public static final String MENUITEM_MUSICSWAPPER = "Music交換";
    public static final String MENUITEM_FILEINJECT = "File injector";
    public static final String MENUITEM_HASHCALC = "パス -> ハッシュ値 計算";
    public static final String MENUITEM_CEDUMPIMPORT = "パス一覧テキストからのインポート";
    public static final String MENUITEM_DBIMPORT = "SQLiteデータベースからのインポート";
    public static final String MENUITEM_MACROEDITOR = "マクロエディター";
    public static final String MENUITEM_LOGVIEWER = "ログビューア";
    public static final String MENUITEM_FIND_EXH = "Exhハッシュ検索";
    public static final String MENUITEM_FIND_MUSIC = "Musicハッシュ検索";
    public static final String MENUITEM_FIND_MAPS = "Mapハッシュ検索";
    public static final String MENUITEM_SETTINGS = "設定";
    public static final String MENUITEM_ENABLEUPDATE = "更新の確認";
    public static final String MENUITEM_QUIT = "終了";
    public static final String MENUITEM_ABOUT = "FFXIV Explorerについて";
    public static final String MENUITEM_EXD_HEX_OPTION = "EX番号を16進数で表示";
    public static final String MENUITEM_EXD_OFFSET_OPTION = "EXDの列をオフセットで並べ替え";

    //Buttons
    public static final String BUTTONNAMES_BROWSE = "参照";
    public static final String BUTTONNAMES_SET = "セット";
    public static final String BUTTONNAMES_REVERT = "元に戻す";
    public static final String BUTTONNAMES_SEARCH = "検索";
    public static final String BUTTONNAMES_CLOSE = "閉じる";
    public static final String BUTTONNAMES_GOTOFILE = "最初のリストにあるファイルを検索";

    //Errors
    public static final String ERROR_CANNOT_OPEN_INDEX = "インデックスファイルが開けません。起動中のFFXIVを終了してください。";
    public static final String ERROR_EDITIO = "不正なIOExceptionが発生しました。 編集したインデックスファイルをバックアップに置き換える必要があります。";

    //Misc
    public static final String MSG_MUSICSWAPPER_TITLE = "Before Using Swapper";
    public static final String MSG_FILEINJECT_TITLE = "Before Using FileInject";
    public static final String MSG_MUSICSWAPPER = "このプログラムはFFXIVARRのファイルを変更します。 音楽ファイルの交換によってゲームが破損したりアカウントが停止されたとしても、責任を負えません。 自己責任で使用してください。\n\n定期バージョンアップは変更したファイルに問題をおよぼす可能性があるため、ゲームにパッチを適用する前に忘れずに元のファイルに復元しておいてください。\n	\nインデックスファイルは、「0c0000.win32.index」と同じフォルダに「0c0000.win32.index.bak」として自動的にバックアップされます。";
    public static final String MSG_FILEINJECT = "このプログラムはFFXIVARRのファイルを変更します。 音楽ファイルの交換によってゲームが破損したりアカウントが停止されたとしても、責任を負えません。 自己責任で使用してください。\n\n定期バージョンアップは変更したファイルに問題をおよぼす可能性があるため、ゲームにパッチを適用する前に忘れずに元のファイルに復元しておいてください。\n	\nインデックスファイルは、「<file名>.index」と同じフォルダに「<file名>.index.bak」として自動的にバックアップされます。";
    public static final String MSG_OUTFITTER_TITLE = "これはまだ実験段階です!";
    public static final String MSG_OUTFITTER = "この機能はまだ解明され、実験されています! 一部の種族やアイテムは正しく見えない場合があります。\n一部の種族とアイテムは、モデルの再利用が原因で正しく読み込まれず、その後スケーリング/変換されます。\nほとんどのアイテムは、ヒューラン、ルガディン♂、およびララフェルモデルに対して適切にレンダリングされるはずです。\n髪と顔を0に設定すると、モデルが消去されます。これは、ヘルメット系のアイテムのモデリング時に有用です。 \n\nとにかく、楽しんでください!";
}

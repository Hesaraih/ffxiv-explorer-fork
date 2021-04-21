package com.fragmenterworks.ffxivextract.storage;

import java.io.File;
import java.util.Calendar;
import java.util.Hashtable;

//更新間をチェックするためのマスターファイルリストを保存します。

@SuppressWarnings("unused")
public class CompareFile {

    private String lastPatchDate;
    private String currentPatchDate;

    private final Hashtable<Integer, String> files; //File Hash -> Patch Date

    private boolean newFilesFound = false;

    private CompareFile(String indexPath) {

        files = new Hashtable<>();
        Calendar c = Calendar.getInstance();
        currentPatchDate = c.get(Calendar.YEAR) + "" + c.get(Calendar.DAY_OF_YEAR) + "" + c.get(Calendar.HOUR) + "" + c.get(Calendar.MINUTE) + "" + c.get(Calendar.SECOND);
    }

    @SuppressWarnings("unused")
    public void updateDate() {
        Calendar c = Calendar.getInstance();
        currentPatchDate = c.get(Calendar.YEAR) + "" + c.get(Calendar.DAY_OF_YEAR) + "" + c.get(Calendar.HOUR) + "" + c.get(Calendar.MINUTE) + "" + c.get(Calendar.SECOND);
    }

    public static CompareFile getCompareFile(String indexName) {
        File file = new File("./" + indexName + ".cmp");
        if (file.exists()) {/*
			Kryo kryo = new Kryo();
			Input in = null;
			try {
				in = new Input(new FileInputStream(new File("./" + indexName + ".cmp")));
			} catch (FileNotFoundException e) {
				return new CompareFile(indexName);
			}
			CompareFile obj = kryo.readObject(in, CompareFile.class);
			in.close();*/
            //return obj;
            return null;
        } else
            return new CompareFile(indexName);
    }

    @SuppressWarnings("unused")
    public boolean isNewFile(int hash) {
        if (files.get(hash) == null && lastPatchDate == null) //Init file
        {
            if (!newFilesFound) {
                Calendar c = Calendar.getInstance();
                currentPatchDate = c.get(Calendar.YEAR) + "" + c.get(Calendar.DAY_OF_YEAR) + "" + c.get(Calendar.HOUR) + "" + c.get(Calendar.MINUTE) + "" + c.get(Calendar.SECOND);
                newFilesFound = true;
                lastPatchDate = currentPatchDate;
            }
            files.put(hash, currentPatchDate);
            return false;
        } else //New File, but already recorded
            //Old File
            if ((files.get(hash) == null) && lastPatchDate != null) //New File
            {
                if (!newFilesFound) {
                    lastPatchDate = currentPatchDate;
                    Calendar c = Calendar.getInstance();
                    currentPatchDate = c.get(Calendar.YEAR) + "" + c.get(Calendar.DAY_OF_YEAR) + "" + c.get(Calendar.HOUR) + "" + c.get(Calendar.MINUTE) + "" + c.get(Calendar.SECOND);
                    newFilesFound = true;
                }
                files.put(hash, currentPatchDate);
                return true;
            } else
                return files.get(hash) != null && files.get(hash).equals(currentPatchDate) && lastPatchDate != null && !lastPatchDate.equals(currentPatchDate);
    }

    @SuppressWarnings("unused")
    public void save() {
        newFilesFound = false;
        //	Kryo kryo = new Kryo();
        //	Output out = new Output(new FileOutputStream(new File("./" + loadedIndexName + ".cmp")));
        //	kryo.writeObject(out, this);
        //	out.close();
    }
}

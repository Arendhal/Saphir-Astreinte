package com.saphir.astreinte;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import com.saphir.astreinte.providers.StopwatchProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.log4j.chainsaw.Main;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;


public class Stopwatch {

	public int id = 0;
    public long startTime = 0;
    public long stopTime = 0;
    public boolean running = false;
    public boolean resume = false;
    public String title = null;
    public long Elapsed;
    public String elapsedTimeString="";
    public static int RowNumber=1;
    public static boolean created = false;

    public void start() {
    	if(startTime==0)
    		//startTime = System.currentTimeMillis()-86340000;
    		startTime = System.currentTimeMillis();
    	else
    		startTime = System.currentTimeMillis() - (stopTime - startTime);
        running = true;
       /* if(!created) {
            CreateWorkbook(MainActivity.context);
        }*/
    }

    public void reset() {
    	startTime=0;
    	stopTime=0;
    	running=false;
    	resume=false;
    }
    
    public void stop() {
    	stopTime = System.currentTimeMillis();
        running = false;
        StopwatchWriteToFile(MainActivity.context,StopwatchActivity.wb_stopwatch, Elapsed, formatElapsedTime(Elapsed));

    }

    public long getElapsedTime() {
        long elapsed;
        if (running) {
             elapsed = (System.currentTimeMillis() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }
        Elapsed=elapsed;
        return elapsed;
    }

    public String formatElapsedTime(long elapsedtime){
    	long seconds= elapsedtime/1000;
    	long mins= seconds/60;
    	long hours=mins/60;

		elapsedTimeString = "Chronomètre:"+title+": Temps écoulé :"+hours%24+"H "+mins%60+"m "+seconds%60+"s.\n";
    	return elapsedTimeString;
	}
    
	public static void stopAllStopwatches(Context context, Uri URI) {
		Cursor cursor;
		Stopwatch stopWatch;
		
		cursor = context.getContentResolver().query(
				URI, 
				new String[] {Stopwatches.STOPWATCH_ID}, 
				Stopwatches.RUNNING+"=1", 
				null, 
				null
			);
		for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()) {
			stopWatch=fetchStopwatchFromDB(context,URI,cursor.getInt(cursor.getColumnIndex(Stopwatches.STOPWATCH_ID)));
			stopWatch.stop();
			updateStopwatchRecord(stopWatch,context,URI);

		}
		cursor.close();
	}
	
	public static void updateStopwatchRecord(Stopwatch stopWatch, Context context, Uri URI) {
		ContentValues contentValues;

		contentValues = new ContentValues();
		contentValues.put(Stopwatch.Stopwatches.TITLE, stopWatch.title);
		contentValues.put(Stopwatch.Stopwatches.START_TIME, stopWatch.startTime);
		contentValues.put(Stopwatch.Stopwatches.STOP_TIME, stopWatch.stopTime);
		contentValues.put(Stopwatch.Stopwatches.RUNNING, (stopWatch.running?1:0));

		context.getContentResolver().update(URI, contentValues, Stopwatches.STOPWATCH_ID+"="+stopWatch.id, null);
	}

    public static Stopwatch fetchStopwatchFromDB(Context context, Uri URI, int stopWatchId) {
    	Stopwatch stopWatch = null;
    	
    	Cursor cursor = context.getContentResolver().query(
    			URI, 
				null, 
				Stopwatches.STOPWATCH_ID+"="+stopWatchId, 
				null, 
				null);
		
		if(cursor.moveToFirst()) {
			stopWatch=new Stopwatch();
			stopWatch.id=cursor.getInt(cursor.getColumnIndex(Stopwatches.STOPWATCH_ID));
			stopWatch.title=cursor.getString(cursor.getColumnIndex(Stopwatches.TITLE));
			stopWatch.startTime=cursor.getLong(cursor.getColumnIndex(Stopwatches.START_TIME));
			stopWatch.stopTime=cursor.getLong(cursor.getColumnIndex(Stopwatches.STOP_TIME));
			stopWatch.running=(cursor.getInt(cursor.getColumnIndex(Stopwatches.RUNNING)) != 0);
		}
		cursor.close();
    	
    	return stopWatch;
    }

    public int getStatus(long ElapsedMillis){
        long mInterval24 = 86400000;
        long mInterval35 = 126000000;
        long compare = mInterval35 - ElapsedMillis;
        int status;
        if (compare >=mInterval35) {
            status = 0;
            return status; // Agent disponible
        }
        if (compare >= mInterval24 && compare <= mInterval35) {
                status = 1;
                return status; //Agent pour lequel il faudra donner du repos
        }
        if (compare < mInterval24) {
            status = 2;
            return status; // Agent qui doit se reposer immédiatement
        }
        return -1;
    }

    public CellStyle setCellStyle(int status,Workbook workbook) {
        switch (status){
            case 0:
                CellStyle cs0 = workbook.createCellStyle();
                cs0.setFillForegroundColor(HSSFColor.GREEN.index);
                cs0.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
                return cs0;
            case 1:
                CellStyle cs1 = workbook.createCellStyle();
                cs1.setFillForegroundColor(HSSFColor.YELLOW.index);
                cs1.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
                return cs1;
            case 2:
                CellStyle cs2 = workbook.createCellStyle();
                cs2.setFillForegroundColor(HSSFColor.RED.index);
                cs2.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
                return cs2;
            default:
                CellStyle cs = workbook.createCellStyle();
                cs.setFillForegroundColor(HSSFColor.WHITE.index);
                cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
                return cs;
        }
    }

    public String setStatusInfo(int status){
        String str="";
        switch (status) {
            case 0:
                str = "Cet agent est disponible";
                return str;
            case 1:
                str = "Cet agent est disponible. Cependant envisagez des periodes de repos pour celui ci.";
                return str;
            case 2 :
                str = "Cet agent doit se reposer immediatement.";
                return str;
            default:
                str="Error getting agent status";
                return str;
        }
    }

	public  void StopwatchWriteToFile(Context context, Workbook workbook, long ElapsedMillis,String ElapsedTimeHours){


        int status = getStatus(ElapsedMillis);
        CellStyle cs = workbook.createCellStyle();
        cs.setFillForegroundColor(HSSFColor.WHITE.index);
        cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        CellStyle statusStyle = setCellStyle(status,workbook);
        String statusInfo = setStatusInfo(status);

        Sheet sheet1=workbook.getSheetAt(0);//workbook.getSheet("Rapport_Agents");

        Row row = sheet1.createRow(RowNumber);
        Cell c = row.createCell(0);
        c.setCellValue(ElapsedTimeHours);
        c.setCellStyle(cs);

        Cell c2 = row.createCell(1);
        c2.setCellValue(statusInfo);
        c2.setCellStyle(statusStyle);

        sheet1.setColumnWidth(0, (20 * 700));
        sheet1.setColumnWidth(1, (20 * 1250));

        File folder = new File(context.getExternalFilesDir(null),"");
		File file = new File(folder,"Rapport_Chronos.xls");
		FileOutputStream fOut = null;
		try {
            fOut =  new FileOutputStream(file);
        	workbook.write(fOut);
		    //fOut.flush();
			fOut.close();
			Log.i("Stopwatch","Write Successful");
		}
		catch (IOException e){
			Log.e("Stopwatch","File write failed : " + e.toString());
		}
     RowNumber++;
	}

	public static class Stopwatches implements BaseColumns {
		private Stopwatches() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ StopwatchProvider.AUTHORITY + "/stopwatches");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.saphir.astreinte";
		
		public static final String STOPWATCH_ID = "_id";

		public static final String TITLE = "title";

	    public static final String START_TIME = "start_time";
	    
	    public static final String STOP_TIME = "stop_time";
	    
	    public static final String RUNNING = "running";

	}


}
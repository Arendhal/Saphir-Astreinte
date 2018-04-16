package com.saphir.astreinte.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import com.saphir.astreinte.MainActivity;
import com.saphir.astreinte.R;
import com.saphir.astreinte.Timer;
import com.saphir.astreinte.Timer.Timers;
import com.saphir.astreinte.TimerActivity;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TimerExpiry extends BroadcastReceiver
{
	
	public static final Uri URI = Timer.Timers.CONTENT_URI;
    public static int RowNumber=1;
    @Override
    public void onReceive(final Context context, final Intent intent) 
    {
		//Log.d("blah","TimerExpiry onReceive()");

	    //this background thread is created so that ActivityManager.RunningAppProcessInfo works correctly,
		// ActivityManager.RunningAppProcessInfo will always be IMPORTANCE_FOREGROUND if onReceive is still executing!
    	//the alarmmanager wakes the device up and keeps it awake only till onReceive() of the broadcastreceiver is running.
		// since we're using a background thread, onReceive() will exit right away so we need to use a wakelock

    	final Thread myThread = new Thread(new Runnable() {    
	    	public void run() {

	    		Cursor cursor;
	    		MediaPlayer mediaPlayer;
	            Timer timer;
	            Timer nextTimer = null;
	    		PowerManager powerManager;
	    		PowerManager.WakeLock wakeLock;
	        	boolean appInForeground;
	        	ContentValues contentValues;
	        	Vibrator vibrate = null;

	    		powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	    		wakeLock = powerManager.newWakeLock(
	    				PowerManager.PARTIAL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP,
	    				"TimerBroadcastReceiver"
	    			);
	    		wakeLock.acquire();
	    		
	    		timer=Timer.fetchTimerFromDB(context, URI, intent.getIntExtra(Timer.Timers.TIMER_ID, 0));
	    		if(timer==null) {
	    			wakeLock.release();
	    			return;
	    		}
	            
	    		contentValues = new ContentValues();
	    		contentValues.put(Timer.Timers.START_TIME, 0);
	    		contentValues.put(Timer.Timers.STOP_TIME, 0);
	    		contentValues.put(Timer.Timers.RUNNING, false);
	    		contentValues.put(Timer.Timers.REMAINING_TIME, 0);
	    		context.getContentResolver().update(URI, contentValues, Timers.TIMER_ID+"="+timer.id, null);

	            cursor = context.getContentResolver().query(
	        			URI, 
	        			new String[] {Timers.TIMER_ID},
	        			Timers.INTERVAL_PARENT_ID+"="+timer.intervalParentId+" and "+Timers.INTERVAL+">"+timer.interval+" and "+Timers.LENGTH+"<>0",
	        			null, 
	        			null
	        		);
	            if(cursor.moveToFirst()) {
		    		nextTimer=Timer.fetchTimerFromDB(context, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
	            }
	            cursor.close();
	            if(nextTimer==null && timer.repeat) {
		            cursor = context.getContentResolver().query(
		        			URI, 
		        			new String[] {Timers.TIMER_ID},
		        			Timers.INTERVAL_PARENT_ID+"="+timer.intervalParentId+" and "+Timers.INTERVAL+"=1",
		        			null, 
		        			null
		        		);
		            if(cursor.moveToFirst()) {
			    		nextTimer=Timer.fetchTimerFromDB(context, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
		            }
		            cursor.close();
	            }
	            
	            if(nextTimer!=null) {
	            	nextTimer.reset();
	            	nextTimer.startTime=timer.stopTime;
	            	nextTimer.stopTime=timer.stopTime+nextTimer.length;
	            	nextTimer.running=true;
	            	//SimpleDateFormat sdf = new SimpleDateFormat(context.getResources().getString(R.string.date_time_format));
	            	//Log.d("TimerBroadcastReceiver","interval="+nextTimer.interval+", startTime="+sdf.format(nextTimer.startTime)+", stopTime="+sdf.format(nextTimer.stopTime));
	            	contentValues = new ContentValues();
		    		contentValues.put(Timer.Timers.START_TIME, nextTimer.startTime);
		    		contentValues.put(Timer.Timers.STOP_TIME, nextTimer.stopTime);
		    		contentValues.put(Timer.Timers.RUNNING, nextTimer.running);
		    		context.getContentResolver().update(URI, contentValues, Timers.TIMER_ID+"="+nextTimer.id, null);
		    		Timer.addAlarmManager(context, nextTimer);
	            }
	            
	    		appInForeground = false;
	    		ActivityManager activityManager = (ActivityManager) context.getSystemService( Context.ACTIVITY_SERVICE );
	    		List<ActivityManager.RunningAppProcessInfo> processList = activityManager.getRunningAppProcesses(); 
	    		for(int i=0; i<processList.size(); i++) {
	    			if(processList.get(i).processName.equalsIgnoreCase(MainActivity.class.getPackage().getName()) && processList.get(i).importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
	    				appInForeground=true;
	    			}
	    		}
	    		if(!appInForeground) {
	    			Timer.putTimerExpiredNotification(context, timer.title, timer.interval, timer.intervalParentId, new Date(timer.stopTime));		            
		    		MainActivity.addOrRemoveMainNotification(context,false);
		    		TimerWriteToFile(context,TimerActivity.wb_timer,Timer.length,formatElapsedTime(Timer.length));
	    		}
	    		else {
	    			Intent intent = new Intent(MainActivity.class.getPackage().getName()+".timer_expired");
	    			intent.putExtra(Timer.Timers.TIMER_ID, timer.id);
	    			intent.putExtra(Timer.Timers.TITLE, timer.title);
	    			intent.putExtra(Timer.Timers.INTERVAL, timer.interval);
	    			intent.putExtra(Timer.Timers.INTERVAL_PARENT_ID, timer.intervalParentId);
	    			if(nextTimer != null) {
	    				intent.putExtra("next"+Timer.Timers.TIMER_ID, nextTimer.id);
	    			}
                    TimerWriteToFile(context,TimerActivity.wb_timer,Timer.length,formatElapsedTime(Timer.length));
	    			context.sendBroadcast(intent);
	    		}
	    		
	    		if(timer.vibrate)
	    			vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

	    		if(timer.numBeeps>0) {
		    		final Thread thisThread=Thread.currentThread();
		    		mediaPlayer = MediaPlayer.create(context, TimerActivity.getBeepResourceId(timer.beep));
		    		if(mediaPlayer!=null) {
			            mediaPlayer.setLooping(false);
			            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {                        
			            	public void onCompletion(MediaPlayer arg0) {                       
		            			thisThread.interrupt();
		            		}        
		            	});
			            if(timer.vibrate) {
			            	long[] pattern = { 50, 500 };
			            	vibrate.vibrate(pattern, 0);
			            }
			            for(int i=0; i<timer.numBeeps; i++) {
			            	mediaPlayer.start();
				            try {
				            	Thread.sleep(5000);
							} catch (InterruptedException e) {
							}
							mediaPlayer.stop();
							try {
								mediaPlayer.prepare();
							} catch (IllegalStateException e) {
								return;
							} catch (IOException e) {
								return;
							}
							mediaPlayer.seekTo(0);
			    		}
			            if(timer.vibrate) {
			            	vibrate.cancel();
			            }
	
			    		mediaPlayer.release();
		    		}
	    		}
	    		else if(timer.vibrate) {
	    			vibrate.vibrate(2000);
	    		}
	            
	            wakeLock.release();
	    	}  
	    });
	    myThread.start();
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

    public CellStyle setCellStyle(int status, Workbook workbook) {
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

    public void TimerWriteToFile(Context context, Workbook workbook, long ElapsedMillis, String ElapsedTimeHours ){

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
        File file = new File(folder,"RapportTimers_"+TimerActivity.strDate+".xls");
        FileOutputStream fOut = null;
        try {

            fOut =  new FileOutputStream(file);
            workbook.write(fOut);
            //fOut.flush();
            fOut.close();
            Log.i("Timer","Write Successful");
        }
        catch (IOException e){
            Log.e("Timer","File write failed : " + e.toString());
        }
        RowNumber++;

    }

    public String formatElapsedTime(long elapsed){
        long seconds= elapsed/1000;
        long mins= seconds/60;
        long hours=mins/60;
        String str = "CàRebours:"+Timer.title+": Temps écoulé :"+hours%24+"H "+mins%60+"m "+seconds%60+"s.\n";
        return str;
    }

    public static String formatTime(Timer timer){
        Cursor cursor = MainActivity.context.getContentResolver().query(
                Timers.CONTENT_URI,
                new String[] {Timers.TIMER_ID},
                Timers.INTERVAL_PARENT_ID+"="+timer.intervalParentId+" and "+Timers.LENGTH+"<>0",
                null,
                null);
        int countIntervals = cursor.getCount();
        cursor.close();

        String elapsedTimeString="";
        SimpleDateFormat sdf = new SimpleDateFormat(MainActivity.context.getResources().getString(R.string.date_time_format));
        long elapsedtime=timer.stopTime-timer.startTime;
        long seconds= elapsedtime/1000;
        long mins= seconds/60;
        long hours=mins/60;


        if(countIntervals==1) {
            elapsedTimeString = "Compte a rebours '" + timer.title + "' expiré le " + sdf.format(timer.stopTime) + ".\n";
            elapsedTimeString += "Compte a rebours:" + timer.title + ": Temps écoulé :" + hours % 24 + "H " + mins % 60 + "m " + seconds % 60 + "s.\n";
        }
        else{
            elapsedTimeString = "Compte a rebours '" + timer.title + "' expiré le " + sdf.format(timer.stopTime) + ".\n";
            elapsedTimeString += "Compte a rebours '" + timer.title + "', Intervalle #" + timer.interval + " expiré à " + sdf.format(timer.stopTime) + ".\n";
        }
        return elapsedTimeString;
    }
}


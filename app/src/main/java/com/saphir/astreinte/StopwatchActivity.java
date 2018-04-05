package com.saphir.astreinte;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.saphir.astreinte.Stopwatch.Stopwatches;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class StopwatchActivity extends Activity implements OnClickListener {

	private class StopwatchUI {
		
		private EditText swTitleEditText = null;
		private ImageButton swAddButton = null;
		private ImageButton swXButton = null;
		private ImageButton swLeftButton = null;
		private ImageButton swRightButton = null;
		private TextView swDaysTextView = null;
		private TextView swHrsTextView = null;
		private TextView swMinsTextView = null;
		private TextView swSecsTextView = null;
		private TextView swTenthsTextView = null;
		private TextView swTitleTextView = null;
		private Button swStartStopResumeButton = null;
		private Button swResetButton = null;
		private LinearLayout swMainRowLinearLayout = null;
		private LinearLayout swMainRowEditTitleLinearLayout = null;

		//public Button swLapButton = null;
		private ProgressBar swProgressBar = null;
	}
	
	private Stopwatch stopwatch1 = null;
	private Stopwatch stopwatch2 = null;
	private Stopwatch stopwatch3 = null;
	private View stopwatch1View = null;
	private View stopwatch2View = null;
	private View stopwatch3View = null;
	private StopwatchUI stopwatch1UI = new StopwatchUI();
	private StopwatchUI stopwatch2UI = new StopwatchUI();
	private StopwatchUI stopwatch3UI = new StopwatchUI();
	private Handler stopwatch1Handler = null;
	private Handler stopwatch2Handler = null;
	private Handler stopwatch3Handler = null;
	private RealView realView = null;
	private boolean deleteStopwatch = false;

	private Handler screenSwitchHandler = null;

	public static Workbook wb_stopwatch;

	private ContentValues contentValues = null;
	private Cursor cursor = null;
	private SharedPreferences sharedPrefs = null;
	private SharedPreferences.Editor sharedPrefsEditor = null; 
	private DialogInterface.OnClickListener alertDialogClickListener = null;
	private AlertDialog.Builder alertDialog = null;
	private Toast toast = null;
	
	//private static final String TAG = "StopwatchActivity";
	public static final Uri URI = Stopwatch.Stopwatches.CONTENT_URI;
	public static final int ACTIVITY_TAB_NUMBER = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.stopwatch);

		screenSwitchHandler = new Handler();

		stopwatch1Handler = new Handler();
		stopwatch2Handler = new Handler();
		stopwatch3Handler = new Handler();

    	stopwatch1View = getLayoutInflater().inflate(R.layout.stopwatch, null);
    	stopwatch2View = getLayoutInflater().inflate(R.layout.stopwatch, null);
    	stopwatch3View = getLayoutInflater().inflate(R.layout.stopwatch, null);
		stopwatch1UI = new StopwatchUI();
		stopwatch2UI = new StopwatchUI();
		stopwatch3UI = new StopwatchUI();
    	setupStopwatchViewUI(stopwatch1View,stopwatch1UI);
    	setupStopwatchViewUI(stopwatch2View,stopwatch2UI);
    	setupStopwatchViewUI(stopwatch3View,stopwatch3UI);

    	realView = new RealView(getApplicationContext());
    	realView.setupStopwatchActivity(this);
		realView.setOnScreenSwitchListener(realViewOnScreenSwitchListener);
   		realView.addView(stopwatch1View);
    	realView.addView(stopwatch2View);
   		realView.addView(stopwatch3View);

		sharedPrefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
		sharedPrefsEditor = sharedPrefs.edit();

   		toast = Toast.makeText(this, "", Toast.LENGTH_LONG);

   		setVolumeControlStream(AudioManager.STREAM_MUSIC);
   		CreateWorkbook(this);

	}
    public  Workbook CreateWorkbook(Context context){

        wb_stopwatch = new HSSFWorkbook();
        Cell c = null;

        //Cell style for header row
        CellStyle cs = wb_stopwatch.createCellStyle();
        cs.setFillForegroundColor(HSSFColor.BLUE_GREY.index);
        cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        //New Sheet
        Sheet sheet1 = null;
        sheet1 = wb_stopwatch.createSheet("Rapport_Agents");

        // Generate column headings
        Row row = sheet1.createRow(0);
        c = row.createCell(0);
        c.setCellValue("Nom & Temps ecoulé (H)");
        c.setCellStyle(cs);
        c = row.createCell(1);
        c.setCellValue("Statut");
        c.setCellStyle(cs);

        sheet1.setColumnWidth(0, (15 * 700));
        sheet1.setColumnWidth(1, (15 * 500));

        File folder = new File(context.getExternalFilesDir(null),"");
        folder.mkdirs();
        File file = new File(folder,"Rapport_Chronos.xls");
        try {
            file.createNewFile();

            FileOutputStream fOut = new FileOutputStream(file,true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            //myOutWriter.append(writeMe);
            wb_stopwatch.write(fOut);
            myOutWriter.close();

            fOut.flush();
            fOut.close();
            Log.i("Stopwatch","Create workbook Successful");
            return wb_stopwatch;
        }
        catch (IOException e){
            Log.e("Stopwatch","Create workbook failed : " + e.toString());
        }
      return wb_stopwatch;
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && sharedPrefs.getString("volumeButtonsPref", getString(R.string.volumeButtonsPrefDefaultValue)).equals("0")) 
	    {
	    	stopwatch2UI.swStartStopResumeButton.performClick();
	        return(true);
	    }
	    else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && sharedPrefs.getString("volumeButtonsPref", getString(R.string.volumeButtonsPrefDefaultValue)).equals("0"))
	    {
	    	stopwatch2UI.swResetButton.performClick();
	        return(true);
	    }
	    return super.onKeyDown(keyCode, event);
	}

	
	private final RealView.OnScreenSwitchListener realViewOnScreenSwitchListener = new RealView.OnScreenSwitchListener() {
		public void onScreenSwitched(int screen) {
			if(screen==0)
				screenSwitchHandler.post(navigateToPrevViewTask);
			else if(screen==2) 
				screenSwitchHandler.post(navigateToNextViewTask);
		}
	};
	
    private void showToast(String text) {
		toast.cancel();
		toast.setText(text);
		toast.show();
    }
	
	public void onPause() {
		super.onPause();
		
		stopwatch2UI.swTitleEditText.removeTextChangedListener(titleTextWatcher);
    	
		stopwatch1Handler.removeCallbacks(stopwatch1UpdateTimeTask);
		stopwatch2Handler.removeCallbacks(stopwatch2UpdateTimeTask);
		stopwatch3Handler.removeCallbacks(stopwatch3UpdateTimeTask);
	}

	@Override
    public void onResume() {
    	super.onResume();
		sharedPrefsEditor.putInt("LastMainActivity", ACTIVITY_TAB_NUMBER);
		sharedPrefsEditor.commit();
		
		int count;
		cursor = this.getContentResolver().query(URI, new String[] {Stopwatches.STOPWATCH_ID}, null, null, null);
        assert cursor != null;
        count = cursor.getCount();
		cursor.close();
		if(count==0) {
			stopwatch2=addRecord();
		}
		else {

			int stopWatchLastViewed = sharedPrefs.getInt("StopwatchLastViewed", 1);
			
			stopwatch2=Stopwatch.fetchStopwatchFromDB(this, URI, stopWatchLastViewed);
			if(stopwatch2==null)
				stopwatch2=Stopwatch.fetchStopwatchFromDB(this, URI, 1);
		}
		
		setupRealViewSwitcher();
		
		stopwatch2UI.swTitleEditText.addTextChangedListener(titleTextWatcher);
    }	
    
    private void setupStopwatchViewUI(View v, final StopwatchUI ui) {
		ui.swTitleEditText = (EditText) v.findViewById(R.id.swTitleEditText);
		ui.swAddButton = (ImageButton) v.findViewById(R.id.swAddButton);
		ui.swXButton = (ImageButton) v.findViewById(R.id.swXButton);
		ui.swLeftButton = (ImageButton) v.findViewById(R.id.swLeftImageButton);
		ui.swRightButton = (ImageButton) v.findViewById(R.id.swRightImageButton);
		ui.swDaysTextView = (TextView) v.findViewById(R.id.swDaysTextView);
		ui.swHrsTextView = (TextView) v.findViewById(R.id.swHrsTextView);
		ui.swMinsTextView = (TextView) v.findViewById(R.id.swMinsTextView);
		ui.swSecsTextView = (TextView) v.findViewById(R.id.swSecsTextView);
		ui.swTenthsTextView = (TextView) v.findViewById(R.id.swTenthsTextView);
		ui.swStartStopResumeButton = (Button) v.findViewById(R.id.swStartStopResumeButton);
		ui.swResetButton = (Button) v.findViewById(R.id.swResetButton);
		ui.swMainRowLinearLayout = (LinearLayout) v.findViewById(R.id.swMainRowLinearLayout);
		ui.swMainRowEditTitleLinearLayout = (LinearLayout) v.findViewById(R.id.swMainRowEditTitleLinearLayout);
		ui.swTitleTextView = (TextView) v.findViewById(R.id.swTitleTextView);
		//ui.swLapButton = (Button) v.findViewById(R.id.swLapButton);
		ui.swProgressBar = (ProgressBar) v.findViewById(R.id.swProgressBar);
		
		ui.swAddButton.setOnClickListener(this);
		ui.swXButton.setOnClickListener(this);
		ui.swLeftButton.setOnClickListener(this);
		ui.swRightButton.setOnClickListener(this);
		ui.swStartStopResumeButton.setOnClickListener(this);
		ui.swResetButton.setOnClickListener(this);
		ui.swTitleTextView.setOnClickListener(this);
		//ui.swLapButton.setOnClickListener(this);
		
		ui.swDaysTextView.setTypeface(MainActivity.getDigitsTypeface(this));
		ui.swHrsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
		ui.swMinsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
		ui.swSecsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
		ui.swTenthsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swDaysBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swHrsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swMinsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swSecsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swTenthsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swDaysBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swHrsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swMinsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swSecsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swTenthsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
		
        ui.swTitleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		        	saveTitle();
		            return true;
		        }
		        return false;
		    }
		});
        ui.swTitleEditText.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if (event.getAction() == KeyEvent.ACTION_DOWN)
		            if (keyCode == KeyEvent.KEYCODE_ENTER) {
		            	saveTitle();
		                return true;
		            }
		        return false;
		    }
		});
		ui.swTitleEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
					saveTitle();
			}
		});
    }

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			View v = getCurrentFocus();
			if ( v instanceof EditText) {
				Rect outRect = new Rect();
				v.getGlobalVisibleRect(outRect);
				if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
					v.clearFocus();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
			}
		}
		return super.dispatchTouchEvent(event);
	}

	public void startLeftRightScreenHandlers() {
    	if(stopwatch1!=null && stopwatch1.running) {
    		stopwatch1Handler.post(stopwatch1UpdateTimeTask);
    	}
    	if(stopwatch3!=null && stopwatch3.running) {
    		stopwatch3Handler.post(stopwatch3UpdateTimeTask);
    	}
    }

    public void stopLeftRightScreenHandlers() {
    	if(stopwatch1!=null) {
    		stopwatch1Handler.removeCallbacks(stopwatch1UpdateTimeTask);
    	}
    	if(stopwatch3!=null) {
    		stopwatch3Handler.removeCallbacks(stopwatch3UpdateTimeTask);
    	}
    }

	Runnable navigateToPrevViewTask = new Runnable() {
		public void run() {
	    	if(deleteStopwatch) {
				int thisStopwatchId = stopwatch2.id;
				String thisStopwatchTitle = stopwatch2.title;
				stopwatch2.reset();
				stopwatch2=fetchPrevStopwatch(stopwatch2.id);
				getContentResolver().delete(URI, Stopwatches.STOPWATCH_ID+"="+thisStopwatchId, null);
				showToast("Chornomètre '"+thisStopwatchTitle+"' supprimé.");
				
		    	if(nextStopwatchExists(stopwatch2.id)) {
		    		stopwatch3=fetchNextStopwatch(stopwatch2.id);
		        	setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
		    		realView.allowScrollNextView=true;
		    	}
		    	else {
		    		stopwatch3=null;
		    		stopwatch3Handler.removeCallbacks(stopwatch3UpdateTimeTask);
		    		realView.allowScrollNextView=false;
		    	}
				
	    		deleteStopwatch=false;
	    	}
	    	else {
	    		stopwatch3=stopwatch2;
	        	setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
	    		realView.allowScrollNextView=true;
	    		
	    		stopwatch2=fetchPrevStopwatch(stopwatch2.id);
	    	}

	    	setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, stopwatch2UpdateTimeTask);
			sharedPrefsEditor.putInt("StopwatchLastViewed", stopwatch2.id);
			sharedPrefsEditor.commit();

	    	if(prevStopwatchExists(stopwatch2.id)) {
	    		stopwatch1=fetchPrevStopwatch(stopwatch2.id);
	        	setupDisplay(stopwatch1UI, stopwatch1, stopwatch1Handler, null);
	    		realView.allowScrollPrevView=true;
	    	}
	    	else {
	    		stopwatch1=null;
	    		stopwatch1Handler.removeCallbacks(stopwatch1UpdateTimeTask);
	    		realView.allowScrollPrevView=false;
	    	}

	    	realView.setCurrentScreen(1);
		}
	};

	Runnable navigateToNextViewTask = new Runnable() {
		public void run() {

	    	stopwatch1=stopwatch2;
	    	setupDisplay(stopwatch1UI, stopwatch1, stopwatch1Handler, null);
			realView.allowScrollPrevView=true;

			stopwatch2=fetchNextStopwatch(stopwatch2.id);
	    	setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, stopwatch2UpdateTimeTask);
			sharedPrefsEditor.putInt("StopwatchLastViewed", stopwatch2.id);
			sharedPrefsEditor.commit();
	    	
	    	if(nextStopwatchExists(stopwatch2.id)) {
	    		stopwatch3=fetchNextStopwatch(stopwatch2.id);
	        	setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
	    		realView.allowScrollNextView=true;
	    	}
	    	else {
	    		stopwatch3=null;
	    		stopwatch3Handler.removeCallbacks(stopwatch3UpdateTimeTask);
	    		realView.allowScrollNextView=false;
	    	}
	    	
	    	realView.setCurrentScreen(1);
			
		}
	};

    private void setupRealViewSwitcher() {
    	if(prevStopwatchExists(stopwatch2.id)) {
    		stopwatch1=fetchPrevStopwatch(stopwatch2.id);
        	setupDisplay(stopwatch1UI, stopwatch1, stopwatch1Handler, null);
    		realView.allowScrollPrevView=true;
    	}
    	else {
    		stopwatch1=null;
    		realView.allowScrollPrevView=false;
    	}

    	setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, stopwatch2UpdateTimeTask);
		sharedPrefsEditor.putInt("StopwatchLastViewed", stopwatch2.id);
		sharedPrefsEditor.commit();
    	
    	if(nextStopwatchExists(stopwatch2.id)) {
    		stopwatch3=fetchNextStopwatch(stopwatch2.id);
    		setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
    		realView.allowScrollNextView=true;
    	}
    	else {
    		stopwatch3=null;
    		realView.allowScrollNextView=false;
    	}

   		realView.setCurrentScreen(1);

    	setContentView(realView);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {    
		MainActivity.hapticFeedback(StopwatchActivity.this);
		switch (item.getItemId()) {    
		case R.id.swStopAllMenuItem:       
			Stopwatch.stopAllStopwatches(this,URI);
			if(stopwatch1!=null) {
				stopwatch1=Stopwatch.fetchStopwatchFromDB(this, URI, stopwatch1.id);
				setupDisplay(stopwatch1UI, stopwatch1, stopwatch1Handler, null);
			}

			stopwatch2=Stopwatch.fetchStopwatchFromDB(this, URI, stopwatch2.id);
			setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, stopwatch2UpdateTimeTask);
			
			if(stopwatch3!=null) {
				stopwatch3=Stopwatch.fetchStopwatchFromDB(this, URI, stopwatch3.id);
				setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
			}
			return true;
		case R.id.prefsMenuItem:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;
		/*case R.id.whatsNewMenuItem:
			MainActivity.showChangeLog(this, true);*/
		default:        
			return false;
		}
	}

	public Stopwatch fetchPrevStopwatch(int id) {
    	Stopwatch stopwatch = null;
		cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Stopwatches.STOPWATCH_ID}, 
    			Stopwatches.STOPWATCH_ID+"<"+id, 
    			null, 
    			Stopwatches.STOPWATCH_ID+" desc");
    	if(cursor.moveToFirst())
    		stopwatch=Stopwatch.fetchStopwatchFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Stopwatches.STOPWATCH_ID)));
    	else
    		stopwatch=Stopwatch.fetchStopwatchFromDB(this, URI, 1);
    	cursor.close();
    	return(stopwatch);
	}

	public Stopwatch fetchNextStopwatch(int id) {
    	Stopwatch stopwatch = null;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Stopwatches.STOPWATCH_ID}, 
    			Stopwatches.STOPWATCH_ID+">"+id, 
    			null, 
    			null);
    	if(cursor.moveToFirst())
    		stopwatch=Stopwatch.fetchStopwatchFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Stopwatches.STOPWATCH_ID)));
    	else
    		stopwatch=Stopwatch.fetchStopwatchFromDB(this, URI, 1);
    	cursor.close();
    	return(stopwatch);
	}

	
	public boolean prevStopwatchExists(int id) {
    	boolean exists=false;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Stopwatches.STOPWATCH_ID}, 
    			Stopwatches.STOPWATCH_ID+"<"+id, 
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		exists=true;
    	}
    	cursor.close();
    	return(exists);

	}
	
	public boolean nextStopwatchExists(int id) {
    	boolean exists=false;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Stopwatches.STOPWATCH_ID}, 
    			Stopwatches.STOPWATCH_ID+">"+id, 
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		exists=true;
    	}
    	cursor.close();
    	return(exists);
	}
    
	public Stopwatch addRecord() {
		Stopwatch stopwatch = new Stopwatch();
		contentValues = new ContentValues();
		contentValues.put(Stopwatch.Stopwatches.TITLE, "");
		contentValues.put(Stopwatch.Stopwatches.START_TIME, 0);
		contentValues.put(Stopwatch.Stopwatches.STOP_TIME, 0);
		contentValues.put(Stopwatch.Stopwatches.RUNNING, false);
		stopwatch.id=Integer.parseInt(this.getContentResolver().insert(URI, contentValues).getPathSegments().get(1));
		if(stopwatch.id==1) 
			stopwatch.title="Chrnomètre #1 (tapez pour changer)";
		else
			stopwatch.title="Chrnomètre #"+stopwatch.id;
		Stopwatch.updateStopwatchRecord(stopwatch,StopwatchActivity.this,URI);
		return(stopwatch);
	}
	
	public void setupDisplay(StopwatchUI ui, Stopwatch stopwatch, Handler stopwatchHandler, Runnable stopwatchUpdateTimeTask) {
		ui.swTitleEditText.setText(stopwatch.title);
		ui.swTitleTextView.setText(stopwatch.title);
		ui.swTitleEditText.setSelection(ui.swTitleEditText.getText().length());
		setTime(ui,stopwatch);
		if(stopwatch.running==true) {
			ui.swStartStopResumeButton.setText("Arreter");
			ui.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_pause,0,0,0);
			stopwatch.resume=true;
			stopwatchHandler.post(stopwatchUpdateTimeTask);
			ui.swProgressBar.setVisibility(View.VISIBLE);
		}
		else {
			if(stopwatch.stopTime==0)
				ui.swStartStopResumeButton.setText("Commencer");
			else
				ui.swStartStopResumeButton.setText("Reprendre");
			ui.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
			stopwatchHandler.removeCallbacks(stopwatchUpdateTimeTask);
			ui.swProgressBar.setVisibility(View.GONE);
		}
		
		ui.swXButton.setEnabled(false);
		ui.swLeftButton.setEnabled(false);
		ui.swXButton.setVisibility(View.VISIBLE);
		ui.swLeftButton.setVisibility(View.VISIBLE);

		if(!prevStopwatchExists(stopwatch.id)) {
			ui.swXButton.setEnabled(false);
			ui.swLeftButton.setEnabled(false);
			ui.swXButton.setVisibility(View.GONE);
			ui.swLeftButton.setVisibility(View.GONE);

		}
		else {
			ui.swXButton.setEnabled(true);
			ui.swLeftButton.setEnabled(true);
			ui.swXButton.setVisibility(View.VISIBLE);
			ui.swLeftButton.setVisibility(View.VISIBLE);
		}

		if(!nextStopwatchExists(stopwatch.id)) {
			ui.swRightButton.setEnabled(false);
			ui.swRightButton.setVisibility(View.GONE);
		}
		else {
			ui.swRightButton.setEnabled(true);
			ui.swRightButton.setVisibility(View.VISIBLE);
		}

	}
	
	public void setTime(StopwatchUI ui, Stopwatch stopwatch) {
		long milliseconds = stopwatch.getElapsedTime();
		int tenth_of_seconds = (int) (milliseconds % 1000 / 10);
		int seconds = (int) ((milliseconds / 1000) % 60);
		int minutes = (int) (((milliseconds / 1000) / 60) % 60);
		int hours   = (int) (((milliseconds / 1000) / 3600) % 24);
		int days    = (int) ((milliseconds / 1000) / 86400);
		ui.swDaysTextView.setText(""+(days<10?"0"+days:days));
		ui.swHrsTextView.setText(""+(hours<10?"0"+hours:hours));
		ui.swMinsTextView.setText(""+(minutes<10?"0"+minutes:minutes));
		ui.swSecsTextView.setText(""+(seconds<10?"0"+seconds:seconds));
		ui.swTenthsTextView.setText(""+(tenth_of_seconds<10?"0"+tenth_of_seconds:tenth_of_seconds));
	}
	
	TextWatcher titleTextWatcher = new TextWatcher() {
    	public void afterTextChanged(Editable s) {
    		if(!stopwatch2.title.equals(s.toString())) {
    			stopwatch2.title=s.toString();
    			Stopwatch.updateStopwatchRecord(stopwatch2,StopwatchActivity.this,URI);
    		}
    	}                
    	public void beforeTextChanged(CharSequence s, int start, int count, int after) {                
    	}                
    	public void onTextChanged(CharSequence s, int start, int before, int count) {                    
    	}            
	};
	
	Runnable stopwatch1UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(stopwatch1UI,stopwatch1);
			stopwatch1Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
		}
	};

	Runnable stopwatch2UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(stopwatch2UI,stopwatch2);
			stopwatch2Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
		}
	};

	Runnable stopwatch3UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(stopwatch3UI,stopwatch3);
			stopwatch3Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
		}
	};

	private void saveTitle()
	{
		stopwatch2UI.swTitleTextView.setText(stopwatch2UI.swTitleEditText.getText());
		stopwatch2UI.swMainRowEditTitleLinearLayout.setVisibility(View.GONE);
		stopwatch2UI.swMainRowLinearLayout.setVisibility(View.VISIBLE);
	}
	
	public void onClick(View v) {
		MainActivity.hapticFeedback(StopwatchActivity.this);
		switch(v.getId()) {
		case R.id.swAddButton:
			stopwatch3=addRecord();
			setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
			stopwatch2=fetchPrevStopwatch(stopwatch3.id);
			setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, null);
			realView.snapToScreenExternal(2);
			showToast("Chronomètre ajouté.");
			break;
		case R.id.swXButton:
			alertDialogClickListener = new DialogInterface.OnClickListener() {    
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.hapticFeedback(StopwatchActivity.this);
					switch (which) {        
					case DialogInterface.BUTTON_POSITIVE:            
						deleteStopwatch=true;
						if(stopwatch1!=null && stopwatch1.running)
							stopwatch1Handler.post(stopwatch1UpdateTimeTask);
						realView.snapToScreenExternal(0);
						break;        
					case DialogInterface.BUTTON_NEGATIVE:            
						break;        
					}    
				}};
			alertDialog = new AlertDialog.Builder(StopwatchActivity.this);
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
	        alertDialog.setTitle("Confirmation de la suppression");
			alertDialog.setMessage("Etes vous sur de vouloir supprimer ce Chronomètre?");
	        alertDialog.setPositiveButton("Oui", alertDialogClickListener);
	        alertDialog.setNegativeButton("Non", alertDialogClickListener);
	        alertDialog.setCancelable(true);
	        alertDialog.create().show();
			break;
		case R.id.swLeftImageButton:
			if(stopwatch1!=null) {
				if(stopwatch1.running)
					stopwatch1Handler.post(stopwatch1UpdateTimeTask);
				setTime(stopwatch1UI,stopwatch1);
				realView.snapToScreenExternal(0);
			}
			break;
		case R.id.swRightImageButton:
			if(stopwatch3!=null) {
				if(stopwatch3.running)
					stopwatch3Handler.post(stopwatch3UpdateTimeTask);
				setTime(stopwatch3UI,stopwatch3);
				realView.snapToScreenExternal(2);
			}
			break;
		case R.id.swStartStopResumeButton:
			if(!stopwatch2.resume) {
				stopwatch2UI.swStartStopResumeButton.setText("Arreter");
				stopwatch2UI.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_pause,0,0,0);
				stopwatch2.resume=true;
				stopwatch2.start();
				Stopwatch.updateStopwatchRecord(stopwatch2,StopwatchActivity.this,URI);
				stopwatch2UI.swProgressBar.setVisibility(View.VISIBLE);
				stopwatch2Handler.post(stopwatch2UpdateTimeTask);
			}
			else {
				stopwatch2UI.swStartStopResumeButton.setText("Reprendre");
				stopwatch2UI.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
				stopwatch2.resume=false;
				stopwatch2.stop();
				Stopwatch.updateStopwatchRecord(stopwatch2,StopwatchActivity.this,URI);
				stopwatch2UI.swProgressBar.setVisibility(View.GONE);
				stopwatch2Handler.removeCallbacks(stopwatch2UpdateTimeTask);
			}
			break;
		case R.id.swResetButton:
			stopwatch2UI.swStartStopResumeButton.setText("Commencer");
			stopwatch2UI.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
			stopwatch2.reset();
			Stopwatch.updateStopwatchRecord(stopwatch2,StopwatchActivity.this,URI);
			stopwatch2Handler.removeCallbacks(stopwatch2UpdateTimeTask);
			stopwatch2UI.swProgressBar.setVisibility(View.GONE);
			setTime(stopwatch2UI,stopwatch2);
			break;
		case R.id.swTitleTextView:
			stopwatch2UI.swTitleEditText.requestFocus();
			stopwatch2UI.swTitleEditText.requestFocusFromTouch();
			stopwatch2UI.swMainRowLinearLayout.setVisibility(View.GONE);
			stopwatch2UI.swMainRowEditTitleLinearLayout.setVisibility(View.VISIBLE);
			break;
		/*case R.id.swLapButton:
			alertDialogClickListener = new DialogInterface.OnClickListener() {    
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.hapticFeedback(StopwatchActivity.this);
				}};
			alertDialog = new AlertDialog.Builder(StopwatchActivity.this);
	        alertDialog.setTitle("StopTimer");
			alertDialog.setMessage("Stopwatch laps are not yet supported. Laps feature will be implemented in a future release. Stay tuned!");
	        alertDialog.setPositiveButton("OK", alertDialogClickListener);
	        alertDialog.setCancelable(true);
	        alertDialog.create().show();
			break;*/
		}
	}

}

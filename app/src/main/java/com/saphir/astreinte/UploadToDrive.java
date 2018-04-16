package com.saphir.astreinte;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class UploadToDrive extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    public static final String TAG = "UploadToDrive";
    public static final int REQUEST_CODE = 101;
    public File ChronoFile;
    public File TimerFile;
    public static GoogleApiClient googleApiClient;

    public static String drive_ID;
    public static DriveId driveId;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        ChronoFile = StopwatchActivity.getFile();
        TimerFile = TimerActivity.getFile();
        buildGoogleApiClient();
    }

    @Override
    public void onStart(){
        super.onStart();
        Log.i(TAG,"Starting and connecting");
        googleApiClient.connect();
    }

    @Override
    public void onStop(){
        super.onStop();
        if(googleApiClient != null){
            Log.i(TAG,"Stopping and disconnecting");
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Log.i(TAG, "onActivityResult() and connecting");
            googleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG,"onConnected and connected");
        Drive.DriveApi.newDriveContents(googleApiClient).setResultCallback(driveContentsCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {
        switch(i){
            case 1:
                Log.i(TAG, "Connection suspended : " + "Service disconnected");
                break;
            case 2:
                Log.i(TAG, "Connection suspended : " + "Connection lost");
                break;
            default:
                Log.i(TAG, "Connection suspended : " + "Unknown");
                break;
        }

    }

    final private ResultCallback<DriveContentsResult> driveContentsCallback = new ResultCallback<DriveContentsResult>() {
        @Override
        public void onResult(@NonNull DriveContentsResult driveContentsResult) {
            if(!driveContentsResult.getStatus().isSuccess()){
                Log.e(TAG,"Error creating new file");
                return;
            }

            final DriveContents driveContents = driveContentsResult.getDriveContents();
            new Thread(){
                @Override
                public void run(){
                    OutputStream outputStream = driveContents.getOutputStream();
                    OutputStream outputStream2 = driveContents.getOutputStream();
                    addChronoFileToOutputStream(outputStream);
                    addTimerFileToOutputStream(outputStream2);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("File")
                            .setMimeType("application/vnd.ms-excel")
                            .setDescription("File uploaded from device")
                            .setStarred(true).build();
                    Drive.DriveApi.getRootFolder(googleApiClient).createFile(googleApiClient,changeSet,driveContents)
                            .setResultCallback(fileCallback);
                }
            }.start();
        }
    };

    private void addChronoFileToOutputStream(OutputStream outputStream){
        Log.i(TAG,"Adding file to output stream");
        byte[] buffer = new byte[1024];
        int bytesRead;
        try{
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(ChronoFile));
            while((bytesRead = inputStream.read(buffer)) != -1 )
            {
                outputStream.write(buffer,0,bytesRead);
            }

        }catch (IOException e) {
            Log.e(TAG,"Error adding file : "+e.toString());
        }
    }

    private void addTimerFileToOutputStream(OutputStream outputStream){
        Log.i(TAG,"Adding file to output stream");
        byte[] buffer = new byte[1024];
        int bytesRead;
        try{
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(TimerFile));
            while((bytesRead = inputStream.read(buffer)) != -1 )
            {
                outputStream.write(buffer,0,bytesRead);
            }

        }catch (IOException e) {
            Log.e(TAG,"Error adding file : "+e.toString());
        }
    }


    final private ResultCallback<DriveFileResult> fileCallback = new ResultCallback<DriveFileResult>() {
        @Override
        public void onResult(@NonNull DriveFileResult driveFileResult) {
            if(!driveFileResult.getStatus().isSuccess()){
                Log.e(TAG,"Error creating file");
                Toast.makeText(UploadToDrive.this,"Error adding file to drive",Toast.LENGTH_LONG).show();
                return;
            }
            Log.i(TAG,"File Added to drive");
            Log.i(TAG,"Created file :"+driveFileResult.getDriveFile().getDriveId());
            Toast.makeText(UploadToDrive.this,"Successfully added file to drive",Toast.LENGTH_LONG).show();

            final PendingResult<DriveResource.MetadataResult> metadata = driveFileResult.getDriveFile().getMetadata(googleApiClient);
            metadata.setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(@NonNull DriveResource.MetadataResult metadataResult) {
                    Metadata data = metadataResult.getMetadata();
                    Log.i(TAG, "Title: " + data.getTitle());drive_ID = data.getDriveId().encodeToString();
                    Log.i(TAG, "DrivId: " + drive_ID);
                    driveId = data.getDriveId();
                    Log.i(TAG, "Description: " + data.getDescription().toString());
                    Log.i(TAG, "MimeType: " + data.getMimeType());
                    Log.i(TAG, "File size: " + String.valueOf(data.getFileSize()));
                }
            });
        }
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG,"Connetion Failed");
        if(connectionResult.hasResolution()){
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(),this,0).show();
            return;
        }

        try{
            Log.i(TAG,"Trying to reolve connection failed");
            connectionResult.startResolutionForResult(this,REQUEST_CODE);
        }catch(IntentSender.SendIntentException e){
            Log.e(TAG,"Exception while starting resolution : "+e.toString());
        }
    }

    private void buildGoogleApiClient(){
        if(googleApiClient == null){
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }
}

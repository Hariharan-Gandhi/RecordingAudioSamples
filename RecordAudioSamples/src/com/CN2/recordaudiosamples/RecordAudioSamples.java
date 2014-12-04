/********************************************************************************/
/* D E S C R I P T I O N    O F    T H E    A P P P L I C A T I O N             */
/********************************************************************************/
/* This Android Application is to Record Audio Samples. It can also playback 	*/
/* and browse through older recordings. It supports Android versions starting   */
/* Android 2.2 Froyo. Additionally, user can play, pause, stop recordings and   */
/* they can choose from the provided list of Sampling Rates                     */
/********************************************************************************/
/* DEVELOPERS:																	*/
/********************************************************************************/
/* Hariharan Gandhi, Master Student, Technical University of Darmstadt          */
/* Harini Gunabalan, Master Student, Technical University of Darmstadt          */															
/********************************************************************************/
package com.CN2.recordaudiosamples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
//import android.util.Log;

public class RecordAudioSamples extends Activity {
	
/* Parameters to handle Audio Data*/	
	private static final String RAS_FILE_EXTN = ".wav";						//setting output Audio File Format
	private static final String RAS_DESTN_FOLDER = "CN2 Audio Samples";
	private static final String RAS_TEMP_AUD_FILE = "tempaudio.raw";
	private static final int RECORDER_BPP = 16;  							//used for calculating the byte_rate and for forming the WAV Header
	
	private int  chosenRate = 0;
	private int samplingRates[]= {8000, 11025, 16000, 22050, 32000, 44100, 48000};
	private String sampleRates_disp[] = {"8kHz (phone)" , "11kHz", "16kHz", "22kHz (FM radio)", "32kHz","44.1kHz (CD)", "48kHz"  };
	
/* Parameters to the Android AudioRecorder Class */	
	private static int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
	private AudioRecord RASRecorder = null;
	private int bufferSize = 0;
	private Thread recordingThread = null;
	private boolean recordingState = false;
	private static String fileName;											//used to Toast the Name of the File name and in Play-back
	
	
/* ------------------------------------ */	
/* Module: Activity's Main Start Point  
/* ------------------------------------ */
	
@Override
public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_record_audio_samples);
	
	setAudioButtonHandlers();
	buttonsState(false);

/* Initializing the minimum buffer size */		
	bufferSize = AudioRecord.getMinBufferSize(8000,
					AudioFormat.CHANNEL_IN_STEREO,
					AudioFormat.ENCODING_PCM_16BIT);
	}

/* ------------------------------------------------ */	
/* Module: Setting Listeners for Activity's Buttons
/* ------------------------------------------------ */
	private void setAudioButtonHandlers() {
			((Button) findViewById(R.id.btnSampleRates)).setOnClickListener(btnClick);
			((Button)findViewById(R.id.btnStart)).setOnClickListener(btnClick);
			((Button)findViewById(R.id.btnStop)).setOnClickListener(btnClick);
			((Button) findViewById(R.id.btnPlayLastRecord)).setOnClickListener(btnClick);
			((Button) findViewById(R.id.btnExplore)).setOnClickListener(btnClick);
		}

/* -------------------------------------------------------------------- */	
/* Module: Managing the State of the Button in the RAS Activity Screen  */
/* -------------------------------------------------------------------- */
	private void buttonsState(boolean recordingState) {
			enableButton(R.id.btnStart,!recordingState);
			enableButton(R.id.btnStop,recordingState);
			enableButton(R.id.btnSampleRates,!recordingState);
			enableButton(R.id.btnPlayLastRecord,!recordingState);
			//Need to Analyze and Decide the State of the EXPLORE button in the Middle of Recording
			enableButton(R.id.btnExplore,!recordingState);
		}
	
/* Function to Enable any of the buttons*/		
	private void enableButton(int id,boolean isEnable){
			((Button)findViewById(id)).setEnabled(isEnable);
		}

/* ----------------------------------------------------------------------------------------- */	
/* Module: Dialog Box with Choices to Choose the Sampling Rates and Display it on the Button */
/* ----------------------------------------------------------------------------------------- */
	private void setSampleRatesButtonCaption(){
		((Button) findViewById(R.id.btnSampleRates)).setText(getString(R.string.sampling_rate) + "[" + sampleRates_disp[chosenRate] +"]");
		RASLog.writeLog("Chosen Sampling Rate: "+sampleRates_disp[chosenRate]);
	}
	
	private void chooseSampleRatesDialog(){
		AlertDialog.Builder sampleRateChoice = new AlertDialog.Builder(this);
		 																		//TODO Try to optimize reusing Global Variable
	
		sampleRateChoice.setTitle(getString(R.string.select_sample_rates))
				.setSingleChoiceItems(sampleRates_disp, chosenRate,
						new DialogInterface.OnClickListener() {
	
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								chosenRate = which;
								setSampleRatesButtonCaption();
	
								dialog.dismiss();
							}
						}).show();
		RASLog.writeLog("Old Sampling Rate: "+sampleRates_disp[chosenRate]);
	}

/* ---------------------------------------------------------------------------------------------------------- */	
/* Module: Setting and Getting the FILEPATH and Creating a FILENAME for Audio Files and Temporary Audio Files */
/* ---------------------------------------------------------------------------------------------------------- */
	private String getFilename(){
			String filepath = Environment.getExternalStorageDirectory().getPath();
			File file = new File(filepath,RAS_DESTN_FOLDER);
			
			if(!file.exists()){
				file.mkdirs();
				}
			
			fileName = ("CN2_"+sampleRates_disp[chosenRate]+"_"+ System.currentTimeMillis()%10000 + RAS_FILE_EXTN);
			return (file.getAbsolutePath() + "/" + fileName);
		}
	
	/*private String savedFileName(){
		return ("CN2_"+sampleRates_disp[chosenRate]+"_"+ System.currentTimeMillis()%10000 + RAS_FILE_EXTN);
	}*/
	
	
	private String getTempAudioName(){
				String filepath = Environment.getExternalStorageDirectory().getPath();
				File file = new File(filepath,RAS_DESTN_FOLDER);
				
				if(!file.exists()){
					file.mkdirs();
					
				}
				
				File tempFile = new File(filepath,RAS_TEMP_AUD_FILE);
				
				if(tempFile.exists()){
					tempFile.delete();
					RASLog.writeLog("Deleting already existing Temp Audio File");
				}
				return (file.getAbsolutePath() + "/" + RAS_TEMP_AUD_FILE);
			}

/* -------------------------------------------------------------------------------------------------------- */	
/* Module: START Recording using AUDIORECORD class and Start the Recording Thread to Write a Temporary File */
/* -------------------------------------------------------------------------------------------------------- */
private void startRecording(){
	
	RECORDER_SAMPLERATE = samplingRates[chosenRate];
	
	try{
		RASRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
			RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize*10);
		RASLog.writeLog("This is working");
	
		
				RASLog.writeLog("Inside 'startRecording()' method"+ '\n'
								+"Sampling Rate:" + RECORDER_SAMPLERATE + '\n' 
								+ "Channels :" + RECORDER_CHANNELS + '\n'
								+ "Buffer Size :" + bufferSize 
								);
				
		//int i = RASRecorder.getState();
			if(RASRecorder.getState()==1)
			{	RASRecorder.startRecording();
			
			recordingState = true;
			 
			recordingThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					writeRecordAudioToFile();
					}
				},"RAS Thread");
			recordingThread.start();
			}
			else
				samplingRateNotSupported();
	}
	catch(IllegalArgumentException e){
		//Toast.makeText(getApplicationContext(),"This Sampling Rate is not Supported by Your Mobile phone. Sorry..!!", Toast.LENGTH_LONG).show();
		System.err.println("Invalid Parameters to AUDIORECORDER Class");
		abruptStop();
	}
			
		}
		
/* The recorded sample is written into the temp file as RAW data */
private void writeRecordAudioToFile(){
	byte audioData[] = new byte[bufferSize];
	String filename = getTempAudioName();
	FileOutputStream FOS = null;
	RASLog.writeLog("Inside writeRecordAudioToFile() Method - Writing Temporary File");
	try {
		FOS = new FileOutputStream(filename);
		}
	catch (FileNotFoundException e) {
		e.printStackTrace();
	}
	
	int read = 0;
	
	if(null != FOS){
		while(recordingState){
			read = RASRecorder.read(audioData, 0, bufferSize);
			
			if(AudioRecord.ERROR_INVALID_OPERATION != read){
				try {
					FOS.write(audioData);
					}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

/* -------------------------------------------------------------------------------------------------------- */	
/* Module: STOP Recording & Release Resources of the AUDIORECORD class and Copy Temporary File to Main File */
/* -------------------------------------------------------------------------------------------------------- */
private void stopRecording(){
	RASLog.writeLog("Inside stopRecording() Method");
	
	if(null != RASRecorder){
		
		recordingState = false;
		
		int i = RASRecorder.getState();
			if(i==1)
				RASRecorder.stop();
			
		releaseRecorder(); 														// Calling method to release 
	}
	copyWaveFile(getTempAudioName(),getFilename());
	deleteTempFile();
}

private void abruptStop(){
RASLog.writeLog("STOPPING ABRUPTLY");
Toast.makeText(getApplicationContext(),"Invalid Arguments - Recording Stopped \n Contact Admin", Toast.LENGTH_LONG).show();
	buttonsState(false);
	if(null != RASRecorder){
		
		recordingState = false;
		
		int i = RASRecorder.getState();
			if(i==1)
				RASRecorder.stop();
			
		releaseRecorder(); 														// Calling method to release
		onDestroy();
	}
}
private void samplingRateNotSupported(){
RASLog.writeLog("UNSUPPORTED SAMPLING RATE(in this mobile) WAS CHOOSEN");
Toast.makeText(getApplicationContext(),"This Sampling Rate is not Supported by Your Mobile phone. Sorry..!!\nTry other Rates", Toast.LENGTH_LONG).show();
	buttonsState(false);
	if(null != RASRecorder){
		
		recordingState = false;
		
		int i = RASRecorder.getState();
			if(i==1)
				RASRecorder.stop();
			
		releaseRecorder(); 														// Calling method to release
		
	}
}
/* --------------------------------------------------------------------------------- */	
/* Module: RELEASE & DESTROY resources - AudioRecorder Object, AudioRecording Thread */
/* --------------------------------------------------------------------------------- */
	/*Modularizing Release Module*/
	public void releaseRecorder(){
		if(RASRecorder != null){
		RASRecorder.release();
		RASRecorder = null;
		recordingThread = null;
		RASLog.writeLog("Inside releaseRecorder() Method - Released");
		}
	}
	
	@Override
	  protected void onDestroy() {							//TODO Need to Examine this method -X-
	    super.onDestroy();
	    releaseRecorder();
	    RASLog.writeLog("Inside onDestroy Method");	
	  }
	
	
/* -------------------------------------------------------------------------------- */	
/* Module: Copying Temporary Audio File into WAV File and Destroying Temporary File */
/* -------------------------------------------------------------------------------- */
	
	private void copyWaveFile(String inFilename,String outFilename){
			FileInputStream in = null;
			FileOutputStream out = null;
			long totAudioLen = 0;
			long totalDataLen = totAudioLen + 36;
			long longSampleRate = RECORDER_SAMPLERATE;
			int channels = 2;
			long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
			
			byte[] data = new byte[bufferSize];
			RASLog.writeLog("Inside copyWaveFile() Method");
			try {
					in = new FileInputStream(inFilename);
					out = new FileOutputStream(outFilename);
					totAudioLen = in.getChannel().size();
					totalDataLen = totAudioLen + 36;
					RASLog.writeLog("File size: " + totalDataLen);
					
					creatingWAVHeader(out, totAudioLen, totalDataLen,
						longSampleRate, channels, byteRate);
					
					while(in.read(data) != -1){
						out.write(data);
					}
					in.close();
					out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

		
/* Temp file containing raw data is deleted */	
	private void deleteTempFile() {
		File file = new File(getTempAudioName());
		
		file.delete();
		RASLog.writeLog("Inside deleteTempFile() Method - Deleted");
}	
	
/* ---------------------------------------------------------------- */	
/* Module: Creating WAV Headers and Prefixing it to the Audio Data  */
/* ---------------------------------------------------------------- */
		
	private void creatingWAVHeader(
					FileOutputStream out, long totAudioLen,
					long totalDataLen, long longSampleRate, int channels,
					long byteRate) throws IOException {

	byte[] WAV_Header = new byte[44];
	
	RASLog.writeLog("Inside creatingWAVHeader() Method");
	
			WAV_Header[0] = 'R';	// RIFF/WAVE header
			WAV_Header[1] = 'I';
			WAV_Header[2] = 'F';
			WAV_Header[3] = 'F';
			WAV_Header[4] = (byte) (totalDataLen & 0xff);
			WAV_Header[5] = (byte) ((totalDataLen >> 8) & 0xff);
			WAV_Header[6] = (byte) ((totalDataLen >> 16) & 0xff);
			WAV_Header[7] = (byte) ((totalDataLen >> 24) & 0xff);
			WAV_Header[8] = 'W';
			WAV_Header[9] = 'A';
			WAV_Header[10] = 'V';
			WAV_Header[11] = 'E';
			WAV_Header[12] = 'f';	// 'format ' chunk
			WAV_Header[13] = 'm';
			WAV_Header[14] = 't';
			WAV_Header[15] = ' ';
			WAV_Header[16] = 16;	// 4 bytes: size of 'format ' chunk
			WAV_Header[17] = 0;
			WAV_Header[18] = 0;
			WAV_Header[19] = 0;
			WAV_Header[20] = 1;	// format = 1
			WAV_Header[21] = 0;
			WAV_Header[22] = (byte) channels;
			WAV_Header[23] = 0;
			WAV_Header[24] = (byte) (longSampleRate & 0xff);
			WAV_Header[25] = (byte) ((longSampleRate >> 8) & 0xff);
			WAV_Header[26] = (byte) ((longSampleRate >> 16) & 0xff);
			WAV_Header[27] = (byte) ((longSampleRate >> 24) & 0xff);
			WAV_Header[28] = (byte) (byteRate & 0xff);
			WAV_Header[29] = (byte) ((byteRate >> 8) & 0xff);
			WAV_Header[31] = (byte) ((byteRate >> 24) & 0xff);
			WAV_Header[32] = (byte) (2 * 16 / 8);
			//WAV_Header[32] = (byte) (channels * 16 / 8);	// block align
			WAV_Header[33] = 0;
			WAV_Header[34] = RECORDER_BPP;	// bits per sample
			WAV_Header[35] = 0;
			WAV_Header[36] = 'd';
			WAV_Header[37] = 'a';
			WAV_Header[38] = 't';
			WAV_Header[39] = 'a';
			WAV_Header[40] = (byte) (totAudioLen & 0xff);
			WAV_Header[41] = (byte) ((totAudioLen >> 8) & 0xff);
			WAV_Header[42] = (byte) ((totAudioLen >> 16) & 0xff);
			WAV_Header[43] = (byte) ((totAudioLen >> 24) & 0xff);
			out.write(WAV_Header, 0, 44);
			}

/* --------------------------------------------------------------- */	
/* Module: Plays the Last Recorded File using Default Music Player */
/* --------------------------------------------------------------- */
	private void playLastRecord(){
		
		RASLog.writeLog("Inside 'playLastRecord()' method");
				
		String path = Environment.getExternalStorageDirectory().getPath();
				
		if(fileName == null){
				Toast.makeText(getApplicationContext(),"Please Record a File to play it", Toast.LENGTH_SHORT).show();
				RASLog.writeLog("Attempted to Play without even Recording a single Audio");
			}
		
		else{
			File audioFile = new File((path+"/"+RAS_DESTN_FOLDER),fileName);
					
		    try{
	       		  Intent playBackIntent = new Intent();
	       		  playBackIntent.setAction(android.content.Intent.ACTION_VIEW);
	       		  playBackIntent.setDataAndType(Uri.fromFile(audioFile), "audio/WAV");
				  startActivity(playBackIntent);
				  RASLog.writeLog("The File: "+fileName+" is played Successfully");
		        }
			  catch(Exception e){
				  System.err.println("Error in AUDIO PLAYBACK INTENT CREATION");
				}
		}
	} 
	
/* --------------------------------------------------- */	
/* Module: Opens EXPLORER to browse for Recorded Files */
/* --------------------------------------------------- */
	private void openExplorer(){
		
		RASLog.writeLog("Inside 'openRecordedSamples()' method");
					  
		   try{
			   	File file = new File(Environment.getExternalStorageDirectory().getPath(),RAS_DESTN_FOLDER);
				Uri dataLocation = Uri.fromFile(file);
				
				Intent explorerIntent = new Intent();
				explorerIntent.setData(dataLocation);												//Location of the Content
				explorerIntent.setType("*/*");													//Format of the Content
				explorerIntent.setAction(Intent.ACTION_VIEW);
		        startActivity(explorerIntent);														// Allows User to Set Default App!
		        //startActivity(Intent.createChooser(explorerIntent, "Select a File Explorer"));	// Does not Allow User to Set Default App!
		  	}
		                                
           catch(Exception e){
			  System.err.println("Error in INTENT CREATION");
			  }
		}
	
/* -------------------------------------- */	
/* Module: Activity Button Click Listener */
/* -------------------------------------- */	
	
private View.OnClickListener btnClick = new View.OnClickListener() {
	@Override
	public void onClick(View v) {
			switch(v.getId()){
					case R.id.btnStart:{
								RASLog.writeLog("START button is clicked");
								Toast.makeText(getApplicationContext(), "Recording Started..", Toast.LENGTH_SHORT).show();
						
								buttonsState(true);
								startRecording();
								
								break;
							}
					
					case R.id.btnStop:{
								RASLog.writeLog("STOP button is clicked");
								
								buttonsState(false);
								stopRecording();
								
								Toast.makeText(getApplicationContext(),"Saved :"+fileName, Toast.LENGTH_LONG).show();
								
								break;
							}
					
					case R.id.btnSampleRates:{
								RASLog.writeLog("SAMPLING RATES button is clicked");
								chooseSampleRatesDialog();
								break;
							}
					
					case R.id.btnPlayLastRecord:{
								RASLog.writeLog("PLAYLAST RECORDED button is clicked");
								playLastRecord();
								break;
							}
					
					case R.id.btnExplore:{
								RASLog.writeLog("EXPLORER button is clicked");
								openExplorer();
								break;
							}
						}
					} 												//-- End of OnClick()
		}; 															//-- End of OnClickListener()
	}																//-- End of RecordAudioSamples Activity
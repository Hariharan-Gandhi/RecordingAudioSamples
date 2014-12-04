/********************************************************************************/
/* D E S C R I P T I O N    O F    T H E    A P P P L I C A T I O N             */
/********************************************************************************/
/* This Android Application is to Record Audio Samples. It can also playback 	*/
/* and browse through older recordings. It supports Android versions starting   */
/* Android 2.2 Froyo. Additionally, user can play, pause, stop recordings and   */
/* they can choose from the provided list of Sampling Rates . MainActivity is   */
/* the file: "RecordAudioSamples" 												*/
/********************************************************************************/
/* DEVELOPERS:																	*/
/********************************************************************************/
/* Hariharan Gandhi, Master Student, Technical University of Darmstadt          */
/* Harini Gunabalan, Master Student, Technical University of Darmstadt          */															
/********************************************************************************/
package com.CN2.recordaudiosamples;

import android.util.Log;

/* For writing the status of the Running Application in the Logcat*/
public class RASLog {
	private static final String APP_TAG = "CN2_RecordAudioSample";
	 
	public static int writeLog(String message){
		return Log.i(APP_TAG,message);
		
		}
	}

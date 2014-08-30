package com.example.libpdexample;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;
import org.puredata.core.utils.PdDispatcher;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {
	
	// State variable for containing data for the variable pitch, when you switch focus away from the app
	private static final String PITCH_STATE = "PITCH_STATE";
	
	// the variable that is sent to the pure data patch
	private double pitch;
	
	// Android seekbar to manipulate the pitch variable
	SeekBar pitchSeekBar;
	
	// TAG is used when sending pitch variable to patch
	private static final String TAG = "LibPdExample";
	
	// holder for dispatcher
	private PdDispatcher dispatcher;
	
	// holder for service interface
	private PdService pdService = null;
	
	
	// used when service is started in onCreate()
	private final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder)service).getService();
			try {
				initPD();
				loadPatch();
			}
			catch (IOException e) {
				Log.e(TAG, e.toString());
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// this method will never be called			
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// initializes variable whether app is just started or has regained focus 
		super.onCreate(savedInstanceState);
		if(savedInstanceState == null) {
			pitch = 50;
		}
		else {
			pitch = savedInstanceState.getDouble(PITCH_STATE);
		}
		
		// initialize the GUI elements
		initGUI();
		
		// starts pdlib as a service
		bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
		
	}
	
	private void initGUI() {
		// use res/layout/activity_main.xml to set view
		setContentView(R.layout.activity_main);
		
		// connect GUI seekbar from activity_main.xml with seekbar in java code 
		pitchSeekBar = (SeekBar) findViewById(R.id.pitchSeekBar);
		
		// set a change listener for the seekbar
		pitchSeekBar.setOnSeekBarChangeListener(pitchSeekBarListener);
		
	}
	
	private void initPD() throws IOException {
		// configure the audio glue
		int sampleRate = AudioParameters.suggestSampleRate(); // finds best sample rate the current unit's audio hardware is capable of
		pdService.initAudio(sampleRate,  0,  2,  10.0f); // initialize audio with the found sample rate, 0 inputs, 2 outputs (stereo) and 10 milliseconds of buffer 
		pdService.startAudio();		
		
		// create and install the dispatcher
		dispatcher = new PdUiDispatcher();
		dispatcher.addListener("something", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, float x) {
				Log.i(TAG, "something : " + x);
				System.out.println(x);
			}
		});
		
		PdBase.setReceiver(dispatcher);		
	}
	
	// loads and initializes the pure data patch res\raw\pdlibexample.pd
	private void loadPatch() throws IOException {
		File dir = getFilesDir();
		IoUtils.extractZipResource(getResources().openRawResource(R.raw.libpdexample) , dir, true);
		File patchFile = new File(dir, "libpdexample.pd");
		PdBase.openPatch(patchFile.getAbsolutePath());
	}
	
	private void start() {
		if (!pdService.isRunning()) {
			Intent intent = new Intent(MainActivity.this, MainActivity.class);
			pdService.startAudio(intent, R.drawable.icon, "PdLibExample", "Return to PdLibExample");
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// close service when app closes
		unbindService(pdConnection);
	}
	
	// set up change listener for seekbar
	private OnSeekBarChangeListener pitchSeekBarListener = new OnSeekBarChangeListener() {

		// when seekbar changes ...
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			
			// gets pitch data from seekbar. As a standard a android seekbar moves from 0-100 and a pure data seekbar moves from 0-127 hence the recalculation 
			pitch = (pitchSeekBar.getProgress()) * 1.27;
			
			// send the data from the variable pitch to the pure data patch, which will recieve it as a float variable named "pitch"
			PdBase.sendFloat("pitch", (float) pitch);
			
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
		
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	// this method will be called whenever the device changes in any way eg. a change in focus
	protected void onSaveInstanceState(Bundle outState) {

		// stores data pitch variable if app is left but not closed
		super.onSaveInstanceState(outState);
		outState.putDouble(PITCH_STATE, pitch);
		
	}
}

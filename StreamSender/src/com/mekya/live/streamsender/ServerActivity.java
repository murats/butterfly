package com.mekya.live.streamsender;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

import com.mekya.live.streamsender.servers.RtspServer;

public class ServerActivity extends Activity implements IStreamer{

	public static final String TAG = "ServerActivity";

	private static final String PREFS_NAME = "PREFS";

	private static final String IS_INITIALIZED = "IS_INITIALIZED";

	private Camera mCamera;

	private Button sendStreamButton;

	PowerManager pm;
	private WakeLock wl;

	private Process ffmpegaudioProcess;

	private AudioRecord audioRecord;

	private byte[] audioBuffer;

	private RtspServer rtspServer;

	protected OutputStream audioOutputStream;

	private OutputStream videoOutputStream;

	private Process ffmpegVideoProcess;

	private FrameLayout cameraFrame;

	Handler handler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;


		initialize();

		setContentView(R.layout.activity_server);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
		cameraFrame = (FrameLayout) findViewById(R.id.camera_preview);

		sendStreamButton = (Button) findViewById(R.id.button_capture);

		rtspServer = new RtspServer(this);

		printSomeInfo();

		openCameraPreview();

		sendStreamButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				startVideo("192.168.1.23", 24455);

			}
		});
	}

	private void initialize() {
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		boolean is_initialized = prefs.getBoolean(IS_INITIALIZED, false);

		if (is_initialized == false) {
			try {
				//copy from assets to files directory
				InputStream istream = getAssets().open("ffmpeg");
				FileOutputStream fos = openFileOutput("ffmpeg", Context.MODE_PRIVATE);
				int read;
				byte[] buffer = new byte[1024];
				while ((read = istream.read(buffer)) != -1) {
					fos.write(buffer, 0, read);
				}
				fos.close();

				istream = getAssets().open("libx264-ultrafast.ffpreset");
				fos = openFileOutput("libx264-ultrafast.ffpreset", Context.MODE_PRIVATE);
				while ((read = istream.read(buffer)) != -1) {
					fos.write(buffer, 0, read);
				}
				fos.close();

				// make ffmpeg executable
				File fileDir = getFilesDir();
				String ffmpegPath = fileDir.getAbsolutePath()+ "/ffmpeg";				
				String command = "chmod 755 "+ ffmpegPath;
				Runtime.getRuntime().exec(command);

				//save the initialization flag 
				Editor editor = prefs.edit();
				editor.putBoolean(IS_INITIALIZED, true);
				editor.commit();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}

	}

	private void openCameraPreview(){
		// Create an instance of Camera
		mCamera = getCameraInstance();
		// Create our Preview view and set it as the content of our activity.

		Camera.Parameters params = mCamera.getParameters();
		params.setPreviewFormat(ImageFormat.NV21);
		params.setPreviewSize(480, 360);
		//params.setPreviewFpsRange(20000, 20000);
		mCamera.setParameters(params);

		CameraPreview mPreview = new CameraPreview(this, mCamera);

		cameraFrame.addView(mPreview);
	}

	private void prepareAudioRecord(){
		int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		int size = bufferSize/8;
		audioBuffer = new byte[size];
		audioRecord.startRecording();
	}

	private void printSomeInfo(){

		int[] range = new int[2];
		List<int[]> list = getCameraInstance().getParameters().getSupportedPreviewFpsRange();

		for (int[] is : list) {
			Log.i(TAG, " supported range between " + is[0] +"  "+ is[1]);
		}
		List<Size> l= getCameraInstance().getParameters().getSupportedPreviewSizes();


		for (Size is : l) {
			Log.i(TAG, "supported width height " + is.width +"  "+ is.height);
		}

		getCameraInstance().getParameters().getPreviewFpsRange(range);
		Log.i(TAG, "preview fps min " + range[0] + " max " + range[1]);

		Size t = getCameraInstance().getParameters().getPreviewSize();

		Log.i(TAG, " previews size "+ t.width +"x" + t.height);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopStreaming();

		if (rtspServer != null) {
			rtspServer.stop();
		}
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_server, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_client_mode:
			Intent i = new Intent(this, ClientActivity.class);
			startActivity(i);
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}



	/** A safe way to get an instance of the Camera object. */
	public Camera getCameraInstance(){
		if (mCamera ==null) {
			try {

				mCamera = Camera.open(0); // attempt to get a Camera instance
			}
			catch (Exception e){
				// Camera is not available (in use or does not exist)
			}
		}
		return mCamera; // returns null if camera is unavailable
	}

	@Override
	protected void onPause() {
		releaseCamera(); // release the camera immediately on pause event 
		stopStreaming();
		rtspServer.stop();
		super.onPause();
	}


	private void releaseCamera(){
		if (mCamera != null){
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();        // release the camera for other applications
			mCamera = null;
		}
	}


	/** A basic Camera preview class */
	public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
		private SurfaceHolder mHolder;
		private Camera mCamera;


		public CameraPreview(Context context, Camera camera) {
			super(context);
			mCamera = camera;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		}

		public void surfaceCreated(SurfaceHolder holder) {
			// The Surface has been created, now tell the camera where to draw the preview.
			try {
				wl.acquire();
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
				Log.d(TAG, "Error setting camera preview: " + e.getMessage());
			}

		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			wl.release();
			releaseCamera();
			//mCamera.stopPreview();
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
			// If your preview can change or rotate, take care of those events here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null){
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e){
				// ignore: tried to stop a non-existent preview
			}

			// make any resize, rotate or reformatting changes here

			// start preview with new settings

			try {
				mCamera.startPreview();

			} catch (Exception e){
				Log.d(TAG, "Error starting camera preview: " + e.getMessage());
			}
		}
	}


	private void readErrorStream(final InputStream iserror)
	{
		new Thread(){
			public void run() {
				byte[] buffer = new byte[1024];
				int len;
				try {
					while ((len = iserror.read(buffer)) != -1) {

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}


	@Override
	public void startAudio(String address, int port) {

		String ffmpegPath = getFilesDir().getAbsolutePath() + "/ffmpeg";
		String audioCommand = ffmpegPath + " -analyzeduration 0 -f s16le -ar 44100 -ac 1 -i -  -ac 1 -acodec libfdk_aac -f adts -vbr 3 udp://"+address+":"+ port +"/ ";

		try {
			ffmpegaudioProcess = Runtime.getRuntime().exec(audioCommand);
			//	final InputStream istream = ffmpegaudioProcess.getInputStream();
			audioOutputStream = ffmpegaudioProcess.getOutputStream();
			readErrorStream(ffmpegaudioProcess.getErrorStream());


			prepareAudioRecord();

			Thread streamAudio = new Thread () {
				public void run() {
					try {
						while (true) {
							int len = audioRecord.read(audioBuffer, 0, audioBuffer.length);
							audioOutputStream.write(audioBuffer, 0, len);
							audioOutputStream.flush();
						}
					} catch (IOException e) {

						e.printStackTrace();
					}	

				};
			};
			streamAudio.start();

		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
//		prepareInterface2();
		super.onResume();
	}


//	private void prepareInterface() {
//		try {
//			Process process = Runtime.getRuntime().exec("su");
//			DataOutputStream oStream = new DataOutputStream(process.getOutputStream());
//
//			oStream.writeBytes("route add default gw 192.168.43.2 dev wlan1" + "\n");
//			oStream.flush();
//
//			oStream.writeBytes("ifconfig wlan1 mtu 1370 up" + "\n");
//			oStream.flush();
//
//			oStream.writeBytes("exit" + "\n");
//			oStream.flush();
//
//			process.waitFor();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}

//	private void prepareInterface2() {
//		try {
//			Process process = Runtime.getRuntime().exec("su");
//			DataOutputStream oStream = new DataOutputStream(process.getOutputStream());
//
//			oStream.writeBytes("route add default gw 53.0.0.1 dev wlan1" + "\n");
//			oStream.flush();
//
//			oStream.writeBytes("ifconfig wlan1 mtu 1370 up" + "\n");
//			oStream.flush();
//
//			oStream.writeBytes("exit" + "\n");
//			oStream.flush();
//
//			process.waitFor();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}

	@Override
	public void stopStreaming() {
		if (audioRecord != null) {
			audioRecord.stop();
		}
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
		}
		try {
			if (audioOutputStream != null) {
				audioOutputStream.close();
			}

			if (videoOutputStream != null) {
				videoOutputStream.close();
			}

			if (ffmpegaudioProcess != null) {
				ffmpegaudioProcess.destroy();
			}
			if (ffmpegVideoProcess != null) {
				ffmpegVideoProcess.destroy();
			}
			handler.post(new Runnable() {

				@Override
				public void run() {
					if (sendStreamButton != null) {
						sendStreamButton.setVisibility(View.INVISIBLE);
					}
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void startVideo(String address, int port) {
		String ffmpegPath = getFilesDir().getAbsolutePath() + "/ffmpeg";
		String videoCommand = "/data/ffmpeg/bin/ffmpeg -analyzeduration 0 -pix_fmt nv21 -s 480x360 -vcodec rawvideo -f image2pipe -i - -s 320x240 -crf 18 -g 5 -preset ultrafast -tune zerolatency -vcodec libx264 -f rtp rtp://"+address+":"+ port +"/  ";

		try {
			ffmpegVideoProcess = Runtime.getRuntime().exec(videoCommand);
			InputStream isr = ffmpegVideoProcess.getErrorStream();
			videoOutputStream = ffmpegVideoProcess.getOutputStream();
			readErrorStream(isr);

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		mCamera.setPreviewCallback(new PreviewCallback() {					
			@Override
			public void onPreviewFrame(final byte[] buffer, Camera arg1) {
				try {
					videoOutputStream.write(buffer);
					videoOutputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});


		handler.post(new Runnable() {
			@Override
			public void run() {
				sendStreamButton.setVisibility(View.VISIBLE);
			}
		});
	}


	public void startStreaming(String address, int port){
		String ffmpegPath = getFilesDir().getAbsolutePath() + "/ffmpeg";
		String command = ffmpegPath + " -re -analyzeduration 0 -pix_fmt nv21 -s 480x360 -vcodec rawvideo -f image2pipe -i /data/ffmpeg/bin/mypipe  -analyzeduration 0 -f s16le -ar 44100 -ac 1 -i /data/ffmpeg/bin/audiopipe -ar 44100 -ac 1 -strict -2 -acodec libfdk_aac -b:a 24k -f mpegts -b 56k  -crf 30 -preset ultrafast -tune zerolatency -vcodec libx264 udp://"+address+":"+ port +"/  ";

		//		String command = "/data/ffmpeg/bin/ffmpeg -re -analyzeduration 0 -pix_fmt nv21 -s 320x180 -vcodec rawvideo -f image2pipe -i /data/ffmpeg/bin/mypipe -re -analyzeduration 0 -f s16le -ar 44100 -ac 1 -i /data/ffmpeg/bin/audiopipe -fflags nobuffer -ar 44100 -ac 1 -strict -2 -acodec libfdk_aac -b:a 24k -async 1 -r 25 -b 56k -maxrate 56k -crf 30 -preset ultrafast -tune zerolatency -vcodec libx264 -y /mnt/sdcard/ouytyt.ts ";


		try {
			Process ffmpegProcess = Runtime.getRuntime().exec(command);

			final InputStream isr = ffmpegProcess.getErrorStream();
			//OutputStream oStream = ffmpegProcess.getOutputStream();

			new Thread(){
				public void run() {
					byte[] buffer = new byte[1024];
					int len;
					try {
						while ((len = isr.read(buffer)) != -1) {
							System.out.println(new String(buffer, 0, len));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				};
			}.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			final FileOutputStream oVideoStream = new FileOutputStream("/data/ffmpeg/bin/mypipe");

			mCamera.setPreviewCallback(new PreviewCallback() {					
				@Override
				public void onPreviewFrame(final byte[] buffer, Camera arg1) {
					//System.out.println("Writing frame to pipe");
					try {
						//System.out.println("writing to video pipe");
						oVideoStream.write(buffer);
						oVideoStream.flush();
						//System.out.println("wrote to video pipe");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});


			final FileOutputStream oaudioStream = new FileOutputStream("/data/ffmpeg/bin/audiopipe");
			prepareAudioRecord();
			new Thread () {
				public void run() {
					try {
						while (true) {
							int len = audioRecord.read(audioBuffer, 0, audioBuffer.length);
							//System.out.println("writing to audio pipe");
							oaudioStream.write(audioBuffer, 0, len);	
							oaudioStream.flush();
							//System.out.println("wrote to audio pipe");
						}
					}
					catch (IOException e) {
						e.printStackTrace();
					} 
				};
			}.start();

		} catch (FileNotFoundException e1) {

			e1.printStackTrace();
		}



	}
}

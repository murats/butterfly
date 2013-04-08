package com.mekya.live.streamsender;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.widget.VideoView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ToggleButton;

public class ClientActivity extends Activity {

	public static final String TAG = "TAG";

	private static final String PREFS_NAME = "PREFS";

	private static final String IS_INITIALIZED = "IS_INITIALIZED";

	//	private SurfaceView surfaceView;
	private VideoView videoView;

	private ToggleButton startButton;

	private EditText serverAddressEditText;

	private DatagramSocket udpAudioSocket;

	protected AudioTrack audioTrack;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;


		initialize();

		setContentView(R.layout.activity_client);

		serverAddressEditText = (EditText) findViewById(R.id.portNumText);

		videoView = (VideoView) findViewById(R.id.surface_view);
		//surfaceView = (SurfaceView) findViewById(R.id.surface_view);

		startButton = (ToggleButton) findViewById(R.id.button_start);

		startButton.setOnClickListener(new OnClickListener() {
			private int audioSession;

			@Override
			public void onClick(View arg0) {

				if (startButton.isChecked()) {
					
					videoView.setVideoPath("rtsp://"+serverAddressEditText.getText().toString()+":6454/live.ts");
					videoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
					videoView.setBufferSize(1024*10);
					
					
					//surfaceView.setVideoPath("rtsp://192.168.1.192:6454/live.ts");

					receiveAudio();

					//surfaceView.setVideoPath("rtsp://v1.cache5.c.youtube.com/CjYLENn73wlaLQnhfxT-xQuPjRMYDSANFEIJbXYtZ29vZ2xlSARSBXdhdGNoYPiN9fWmz6VUQw=/0/0/0/video.3gp");
					//surfaceView.setVideoPath("rtsp://192.168.43.11:6454/live.ts");
					//surfaceView.setVideoPath("rtsp://53.0.0.11:6454/live.ts");


					videoView.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
						int i = 0;
						@Override
						public void onBufferingUpdate(MediaPlayer arg0, int percent) {

							if (percent>=0 && videoView.isPlaying() == false) {
								videoView.start();
							}
							if (i > 20) {
								Log.i("Buffer update", "buffer " + percent);
								i = 0;
							}
							i++;

						}
					});

					videoView.setOnInfoListener(new OnInfoListener() {

						@Override
						public boolean onInfo(MediaPlayer arg0, int what, int extra) {
							if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
								videoView.start();
							}
							else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END && videoView.isPlaying() == false) {
								videoView.start();
							}
							return false;
						}
					});


					videoView.setOnErrorListener(new OnErrorListener() {

						@Override
						public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
							System.out.println("Error on media player arg1:" + arg1 + " arg2:"+ arg2);
							return false;
						}
					});
				}
				else {
					stopStreaming();
				}


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


	public void playAudio(final InputStream istream){

		new Thread(){
			public void run() {
				byte[] buffer = new byte[2048];
				int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

				while (true) {
					try {
						int len = istream.read(buffer, 0, buffer.length);

						audioTrack.write(buffer, 0, len);
						audioTrack.flush();

						if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING ) {
							audioTrack.play();
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};

		}.start();
	}


	public void receiveAudio(){

		new Thread(){
			public void run() {
				try {
					String ffmpegPath = getFilesDir().getAbsolutePath() + "/ffmpeg"; 
					String ffmpegCommand = ffmpegPath + " -analyzeduration 0 -f aac -strict -2 -acodec aac -b:a 120k -ac 1 -i - -f s16le -acodec pcm_s16le -ar 44100 -ac 1 -";

					final Process audioProcess = Runtime.getRuntime().exec(ffmpegCommand);

					OutputStream ostream = audioProcess.getOutputStream();
					readErrorStream(audioProcess.getErrorStream());
					udpAudioSocket = new DatagramSocket(53008);
					byte[] data = new byte[2048];
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);

					playAudio(audioProcess.getInputStream());



					while (true) {
						udpAudioSocket.receive(datagramPacket);
						ostream.write(datagramPacket.getData(), 0, datagramPacket.getLength());
						ostream.flush();
					}
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopStreaming();
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_client, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_server_mode:
			startActivity(new Intent(this, ServerActivity.class));
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}



	@Override
	protected void onPause() {
		stopStreaming();
		finish();
		super.onPause();
	}





	/** A basic Camera preview class */

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
	protected void onResume() {
		//	prepareInterface2();
		super.onResume();
	}


	private void prepareInterface() {
		try {
			Process process = Runtime.getRuntime().exec("su");
			DataOutputStream oStream = new DataOutputStream(process.getOutputStream());

			oStream.writeBytes("route add default gw 192.168.43.2 dev wlan1" + "\n");
			oStream.flush();

			oStream.writeBytes("ifconfig wlan1 mtu 1370 up" + "\n");
			oStream.flush();

			oStream.writeBytes("exit" + "\n");
			oStream.flush();

			process.waitFor();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void prepareInterface2() {
		try {
			Process process = Runtime.getRuntime().exec("su");
			DataOutputStream oStream = new DataOutputStream(process.getOutputStream());

			oStream.writeBytes("route add default gw 53.0.0.1 dev wlan1" + "\n");
			oStream.flush();

			oStream.writeBytes("ifconfig wlan1 mtu 1370 up" + "\n");
			oStream.flush();

			oStream.writeBytes("exit" + "\n");
			oStream.flush();

			process.waitFor();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stopStreaming() {
		if (udpAudioSocket != null) {
			udpAudioSocket.close();
		}

		if (audioTrack != null) {
			audioTrack.stop();
		}
		videoView.stopPlayback();
	}


	//	
	//	public void startStreaming(String address, int port, int portAudio){
	//		String command = "/data/ffmpeg/bin/ffmpeg -analyzeduration 0 -pix_fmt nv21 -s 480x360 -vcodec rawvideo -f image2pipe -i /data/ffmpeg/bin/mypipe -analyzeduration 0 -b 90k -f s16le -ar 44100 -ac 1 -i /data/ffmpeg/bin/audiopipe -vn -ar 44100 -ac 1 -acodec libfdk_aac  -f rtp rtp://"+address+":"+ portAudio +"/ -an -b:v 120k -crf 30 -preset ultrafast -vcodec libx264 -f rtp rtp://"+address+":"+ port +"/  ";
	//
	//		try {
	//			Process ffmpegProcess = Runtime.getRuntime().exec(command);
	//
	//			final InputStream isr = ffmpegProcess.getErrorStream();
	//			//OutputStream oStream = ffmpegProcess.getOutputStream();
	//
	//			new Thread(){
	//				public void run() {
	//					byte[] buffer = new byte[1024];
	//					int len;
	//					try {
	//						while ((len = isr.read(buffer)) != -1) {
	//							System.out.println(new String(buffer, 0, len));
	//						}
	//					} catch (IOException e) {
	//						e.printStackTrace();
	//					}
	//
	//				};
	//			}.start();
	//		} catch (IOException e1) {
	//			e1.printStackTrace();
	//		}
	//
	//		try {
	//			final FileOutputStream oVideoStream = new FileOutputStream("/data/ffmpeg/bin/mypipe");
	//
	//			mCamera.setPreviewCallback(new PreviewCallback() {
	//
	//				long start = 0;
	//				int frameCount = 0;
	//				@Override
	//				public void onPreviewFrame(final byte[] buffer, Camera arg1) {
	//					//System.out.println("Writing frame to pipe");
	//					try {
	//						frameCount++;
	//						long now = System.currentTimeMillis();
	//
	//						if (start == 0) {
	//							start = System.currentTimeMillis();
	//						}
	//						else if ((now - start) >= 1000) {
	//							start = System.currentTimeMillis();
	//							//System.out.println("Frame count in last 1 second is " + frameCount);
	//							frameCount = 0;
	//						}
	//						else if (frameCount < 20) 
	//						{
	//							oVideoStream.write(buffer);
	//							oVideoStream.flush();
	//						}
	//
	//						//System.out.println("wrote to video pipe");
	//					} catch (IOException e) {
	//						e.printStackTrace();
	//					}
	//				}
	//			});
	//
	//
	//			final FileOutputStream oaudioStream = new FileOutputStream("/data/ffmpeg/bin/audiopipe");
	//			prepareAudioRecord();
	//			new Thread () {
	//				public void run() {
	//					try {
	//						while (true) {
	//							int len = audioRecord.read(audioBuffer, 0, audioBuffer.length);
	//							//System.out.println("writing to audio pipe");
	//							oaudioStream.write(audioBuffer, 0, len);	
	//							oaudioStream.flush();
	//							//System.out.println("wrote to audio pipe length " + len);
	//						}
	//					}
	//					catch (IOException e) {
	//						e.printStackTrace();
	//					} 
	//				};
	//			}.start();
	//
	//		} catch (FileNotFoundException e1) {
	//
	//			e1.printStackTrace();
	//		}
	//	}

}

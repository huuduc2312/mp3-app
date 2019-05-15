package assignment.mobile.playmp3;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import dyanamitechetan.vusikview.VusikView;

public class PlayingActivity extends AppCompatActivity implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener {
    private boolean is_playing = true;
    private boolean is_shuffled = false;
    private int is_repeated = 0;
    private SeekBar seekBar;

    Handler handler = new Handler();
    ImageView _playing;
    ImageView _playing_shuffle;
    ImageView _playing_repeat;
    ImageView _playing_pre;
    ImageView _playing_next;
    ImageView _btn_back;
    ImageView _btn_add;
    TextView _name_song;
    TextView _name_author;
    private TextView timeView;
    private MediaPlayer mediaPlayer;
    private int mediaFileLength;
    private int realtimeLength;
    private VusikView musicView;
    Song playingSong;
    private Intent intent;
    TextView timePos, timeDur;
    private static String GETFILE_URL = "http://192.168.1.109/mp3-server/get-song-by-id.php?id=";
    int totalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        _playing = (ImageView) findViewById(R.id.playing);
        _playing_shuffle = findViewById(R.id.playing_shuffle);
        _playing_repeat = findViewById(R.id.playing_repeat);
        _playing_pre = findViewById(R.id.playing_pre);
        _playing_next = findViewById(R.id.playing_next);
        _btn_back = findViewById(R.id.btn_back);
        _btn_add = findViewById(R.id.btn_add);
        _name_song = findViewById(R.id.name_song);
        _name_author = findViewById(R.id.name_author);
        timePos = findViewById(R.id.pos);
//        timeDur = findViewById(R.id.dur);

        musicView = (VusikView) findViewById(R.id.musicView);
        _playing.setEnabled(false);

        intent = getIntent();
        String songId = intent.getStringExtra("song_selected");
        Log.v("song_id", songId);

        getJSON(GETFILE_URL + songId);
        if (playingSong != null) {
            _name_song.setText(playingSong.getName());
            _name_author.setText(playingSong.getSinger());
        }

        musicView = (VusikView) findViewById(R.id.musicView);


        seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBar.setMax(99); // 100% (0~99)
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mediaPlayer.isPlaying()) {
                    SeekBar seekBar = (SeekBar) v;
                    int playPosition = (mediaFileLength / 100) * seekBar.getProgress();
                    mediaPlayer.seekTo(playPosition);
                    realtimeLength = mediaPlayer.getCurrentPosition();
                    timePos.setText(createTimeLabel(realtimeLength));
                }
                return false;
            }
        });

        _playing.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                final ProgressDialog mDialog = new ProgressDialog(PlayingActivity.this);


                AsyncTask<String, String, String> mp3Play = new AsyncTask<String, String, String>() {

                    @Override
                    protected void onPreExecute() {
                        mDialog.setMessage("Please wait");
                        mDialog.show();
                    }

                    @Override
                    protected String doInBackground(String... params) {
                        try {
                            mediaPlayer.setDataSource(params[0]);
                            mediaPlayer.prepare();
                        } catch (Exception ex) {

                        }
                        return "";
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        mediaFileLength = mediaPlayer.getDuration();
                        realtimeLength = mediaPlayer.getCurrentPosition();
                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.start();
                            _playing.setImageResource(R.drawable.playing_pause);
                        } else {
                            mediaPlayer.pause();
                            _playing.setImageResource(R.drawable.playing_play);
                        }

                        updateSeekBar();
                        mDialog.dismiss();
                    }
                };

                mp3Play.execute(playingSong.getUrl()); // direct link mp3 file

                musicView.start();
            }
        });

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);


    }

    private void updateSeekBar() {
        seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLength) * 100));
        if (mediaPlayer.isPlaying()) {
            Runnable updater = new Runnable() {
                @Override
                public void run() {
                    updateSeekBar();
                    realtimeLength += 1000; // declare 1 second
                    timePos.setText(createTimeLabel(realtimeLength));

                }

            };
            handler.postDelayed(updater, 1000); // 1 second
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        _playing.setImageResource(R.drawable.playing_play);
        musicView.stopNotesFall();

    }

    public String createTimeLabel(int time) {
        String timeLabel = "";
        int min = time / 1000 / 60;
        int sec = time / 1000 % 60;

        timeLabel = min + ":";
        if (sec < 10) timeLabel += "0";
        timeLabel += sec;

        return timeLabel;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBar.setSecondaryProgress(percent);
    }

    //this method is actually fetching the json string
    private void getJSON(final String urlWebService) {
        /*
         * As fetching the json string is a network operation
         * And we cannot perform a network operation in main thread
         * so we need an AsyncTask
         * The constrains defined here are
         * Void -> We are not passing anything
         * Void -> Nothing at progress update as well
         * String -> After completion it should return a string and it will be the json string
         * */
        class GetJSON extends AsyncTask<Void, Void, String> {
            private Song songFromServer;

            //this method will be called before execution
            //you can display a progress bar or something
            //so that user can understand that he should wait
            //as network operation may take some time
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            //this method will be called after execution
            //so here we are displaying a toast with the json string
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Toast.makeText(getApplicationContext(), "Load song successfully", Toast.LENGTH_SHORT).show();

                try {
                    JSONObject objjson = new JSONObject(s);
                    String id = objjson.getString("id");
                    String name = objjson.getString("name");
                    String url = objjson.getString("url");
                    String singer = objjson.getString("singer");
                    playingSong = new Song(id, name, url, singer);
                    _name_song.setText(playingSong.getName());
                    _name_author.setText(playingSong.getSinger());
                    _playing.setEnabled(true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                playingSong = this.songFromServer;

            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    //creating a URL
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    //StringBuilder object to read the string from the service
                    StringBuilder sb = new StringBuilder();

                    //We will use a buffered reader to read the string from service
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    //A simple string to read values from each line
                    String json;

                    //reading until we don't find null
                    while ((json = bufferedReader.readLine()) != null) {

                        //appending it to string builder
                        sb.append(json + "\n");
                    }

                    //finally returning the read string
                    return sb.toString().trim();
                } catch (Exception e) {
                    return null;
                }

            }
        }

        //creating asynctask object and executing it
        GetJSON getJSON = new GetJSON();
        getJSON.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaPlayer.release();
        mediaPlayer = null;
        handler.removeCallbacksAndMessages(null);
    }
}

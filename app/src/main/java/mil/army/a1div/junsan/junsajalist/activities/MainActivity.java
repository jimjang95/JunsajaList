package mil.army.a1div.junsan.junsajalist.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import mil.army.a1div.junsan.junsajalist.Controller.FileController;
import mil.army.a1div.junsan.junsajalist.POJO.Rank;
import mil.army.a1div.junsan.junsajalist.POJO.Soldier;
import mil.army.a1div.junsan.junsajalist.POJO.Sosok;
import mil.army.a1div.junsan.junsajalist.R;

public class MainActivity extends Activity {
    BufferedReader bufferedReader = null;
    TextView junsajaList = null;
    TextView junsaDate = null;
    TextView count = null;
    TextView sosok = null;
    TextView name = null;
    private MusicService mService;
    private boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.ServiceBinder binder = (MusicService.ServiceBinder) iBinder;
            mService = binder.getService();
            mService.mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    findViewById(R.id.playStop).setBackgroundResource(R.mipmap.ic_start);
                }
            });

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        junsajaList= (TextView) findViewById(R.id.junsaja_list);
        junsaDate = (TextView) findViewById(R.id.junsa_date);
        count = (TextView) findViewById(R.id.count);
        sosok = (TextView) findViewById(R.id.sosok);
        name = (TextView) findViewById(R.id.name);

        Date today = new Date();
        int month = today.getMonth() + 1;
        int day = today.getDay() + 1;
        String fileName = "sortByDate/";
        if (month < 10) {
            fileName += '0';
        }
        fileName += month;
        if (day < 10) {
            fileName += '0';
        }
        fileName += day;
        fileName += ".csv";

        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(getAssets().open(fileName), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("ControlTest", fileName);
        FileController controller = new FileController(bufferedReader);
        for (Soldier s : controller.getSoldiers()) {
            Log.d("ControlTest", s.toString());
        }

        // Find better way to do this
        Soldier[] soldiers = controller.getSoldiers().toArray(new Soldier[controller.size]);


        Log.d("전사자 명부 총원", String.valueOf(soldiers.length) + "명");
        Arrays.sort(soldiers, new Comparator<Soldier>() {
            @Override
            public int compare(Soldier a, Soldier b) {
                if (a.getYear() == b.getYear()) {
                    if (a.getSosok() == b.getSosok()) {
                        return b.getRank().ordinal() - a.getRank().ordinal();
                    } else {
                        return a.getSosok().ordinal() - b.getSosok().ordinal();
                    }
                } else {
                    return a.getYear() - b.getYear();
                }
            }
        });

        int startOfYear = 0;
        int startOfSosok = 0;
        int endOfSection = 0;
        int sosokCount = 0;
        int curYear = 0;
        Sosok curSosok = null;
        for (int i = 0; i < soldiers.length; i++) {
            if (soldiers[i].getYear() != curYear) {
                //연도가 바뀌면 그 때 연도를 TextView에 출력
                curYear = soldiers[i].getYear();
                junsaDate.append(Integer.toString(curYear));
                junsaDate.append(". ");
                int temp = soldiers[i].getMonth();
                if (temp < 10) {
                    junsaDate.append(" ");
                }
                junsaDate.append(Integer.toString(temp));
                temp = soldiers[i].getDay();
                if (day < 10) {
                    junsaDate.append(".  ");
                    junsaDate.append(Integer.toString(temp));
                } else {
                    junsaDate.append(". ");
                    junsaDate.append(Integer.toString(temp));
                }
                junsaDate.append("\n");

                //연도 예하(?)에 있는 다른 소속 수 만큼 빈 줄 넣어주기
                count.append("" + (i - startOfYear + 1) + "명\n");
                for (int j = 0; j < sosokCount; j++) {
                    junsaDate.append("\n");
                    count.append("\n");
                }
                //연도 바뀌었으니까 다 초기화
                startOfYear = i;
                startOfSosok = i;
                curSosok = null;
                sosokCount = 0;
            }
            if (!soldiers[i].getSosok().equals(curSosok)) {
                // 소속 바뀌는걸 감지
                sosok.append(soldiers[i].getSosok().toString());
                sosok.append("\n");
                if (i - startOfSosok > 2) {
                    // 한 줄에 3명 이상이 들어가야함 (전 줄에 입력을 다 끝났을 때 해주는 것임)
                    name.append(soldiers[startOfSosok].nameRank());
                    name.append(" 등 " + (i - startOfSosok) + "명");
                } else {
                    for (int j = startOfSosok; j <= i; j++) {
                        name.append(soldiers[j].nameRank());
                        if (j != i - 1) {
                            name.append(", ");
                        }
                    }
                }
                name.append("\n");
                startOfSosok = i;
                curSosok = soldiers[i].getSosok();
                sosokCount++;
            } else {
                //
            }
        }

        final ImageButton playStop = findViewById(R.id.playStop);
        playStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("PLAYSTOP_BUTTON", "OnClick Activated!!!");
                if (!mBound) {
                    return;
                }
                if (!mService.mPlayer.isPlaying()) {
                    mService.startMusic();
                    playStop.setBackgroundResource(R.mipmap.ic_stop);
                } else {
                    mService.stopMusic();
                    playStop.setBackgroundResource(R.mipmap.ic_start);
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

}

package com.dreamwalker.knu2018.myservice102;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 동적 브로드케스트리시버 등록하여
 * 메니페스트에 등록할 필요 없음.
 */
public class Main2Activity extends AppCompatActivity {
    private static final String TAG = "Main2Activity";

    @BindView(R.id.serviceDataTv)
    TextView serviceDataTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serviceDataTv.setText("");
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.e(TAG, "onReceive: 리시버 액션 받았어요" + action );
            if (Const.CHAR_READ_ACTION.equals(action)) {
                final String data  = intent.getStringExtra(Const.EXTRA_DATA);
                Log.e(TAG, "onReceive: 브로드케스트 리시버에서 데이터 받았어요" + data );
                serviceDataTv.append(data);
            }
        }
    };

    // TODO: 2018-02-13 브로드 케스트 리시버에서 받을 인텐트를 정의한다.
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Const.CHAR_READ_ACTION);
        intentFilter.addAction(Const.CHAT_NOTIFY_ACTION);
        intentFilter.addAction(Const.EXTRA_DATA);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}

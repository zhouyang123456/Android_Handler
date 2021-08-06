package com.llw.helloworld;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    protected final int MSG_GET = 1;
    protected final int MSG_RESULT = 2;

    private HandlerThread mHandlerThread;

    //子线程中的Handler实例
    private Handler mSubTreadHandler;
    private Handler mUiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.i(TAG, "mUiHandler handleMessage thread : " + Thread.currentThread());
            if (msg.what == MSG_RESULT) {
                Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
            } else {
                throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate thread : " + Thread.currentThread());
        //点击该按钮，向子线程handler发消息
        findViewById(R.id.get).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSubTreadHandler.sendEmptyMessage(MSG_GET);//就是 1
                Log.i(TAG,"I first");
            }
        });

        findViewById(R.id.jumoToSecond).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, SecondActivity.class);
            startActivity(intent);
        });
        initHandlerThread();
    }

    private void initHandlerThread() {
        //创建HandlerThread实例
        mHandlerThread = new HandlerThread("handler_thread");//开启第二个线程
        //开始运行线程
        mHandlerThread.start();
        //获取HandlerThread线程中的Looper实例
        Looper loop = mHandlerThread.getLooper();//TODO 为什么会有looper？

        //NEW,当消息队列没有消息时，会回调到该idelHandler执行方法
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                Log.i(TAG,"The queue is empty");
                return false;
            }
        });
        //创建Handler与线程绑定
        mSubTreadHandler = new Handler(loop) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.i(TAG, "mSubThreadHandler handleMessage thread : " + Thread.currentThread());
                if (msg.what == MSG_GET) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "sorry , I first");
                    double number = Math.random();
                    String result = "dopezhi: " + number;
                    //向UI线程发送消息，更新UI
                    Message message = new Message();
                    message.what = MSG_RESULT;
                    message.obj = result;
                    //子线程收到消息后，向主线程Handler发送消息
                    mUiHandler.sendMessage(message);
                }
            }
        };
    }
}
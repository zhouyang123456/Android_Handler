package com.llw.helloworld;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


public class SecondActivity extends AppCompatActivity {

    private final String TAG = "SecondActivity";

    public final int MSG_GET = 1;
    public final int MSG_RESULT = 2;

    public final int MSG_PASS = 3;

    //主线程中初始化mUIhandler，与主线程的Looper挂钩
    /**
     * 主线程可以不像子线程一样，在创建Handler的时候需要执行Looper.prepare()和Looper.loop();
     * 这是因为应用启动时会在主线程中初始化一个Looper，prepareMainLooper()可以得到主线程的Looper
     *
     */
    //这里的在构造函数中传入looper是因为handler的创建过程需要传入looper和MessageQueue，
    // 此处可以说将looper和handler进行挂钩,挂钩之后方便通过handler发送消息的时候是发送给
    // 之前绑定了的messageQueue
    //只不过这里的主线程Looper是Looper.getMainLooper()
    public Handler mUiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.i(TAG, "mUiHandler handleMessage thread : " + Thread.currentThread());
            switch (msg.what) {
                case MSG_RESULT:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        Log.i(TAG, "onCreate thread : " + Thread.currentThread());
        //在主线程中创建另外一个子线程（looper_thread），这个LooperThread是自定义的
        LooperThread looperThread = new LooperThread("looper_thread");
        looperThread.start();
        findViewById(R.id.get_second).setOnClickListener(v -> {
            // 在主线程中调用子线程的方法向子线程发送消息
            looperThread.mSubHandler.sendEmptyMessage(MSG_GET);
        });

    }

    class LooperThread extends Thread {
        public Handler mSubHandler;

        public LooperThread(String name) {
            super(name);
        }

        @Override
        public void run() {
             //初始化子线程的Looper和messageQueue
            Looper.prepare();
            Handler.Callback callback = new Handler.Callback() {
                @Override
                public boolean handleMessage(@NonNull Message msg) {
                    switch (msg.what) {
                        case MSG_PASS:
                            return false;
                        default:
                            return true;
                    }
                }
            };

            //创建子线程的hander,即mSubHandler，并与子线程的Looper挂钩
            mSubHandler = new Handler(Looper.myLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    Log.i(TAG, "mSubHandler handler handleMessage thread : " + Thread.currentThread());
                    switch (msg.what) {
                        case MSG_GET:
                            double number = Math.random();
//                            Message message = new Message();
                            Message message = Message.obtain();
                            message.what = MSG_RESULT;
                            message.obj = "dopezhi : " + number;
                            //在子线程中向主线程发送消息
                            //mUiHandler与Looper.getMainLooper()和对应的MessageQueue进行了挂钩
                            //所以在发送消息的时候其实是在向之前挂钩的MessageQueue发送消息
                            //这里其实是可以继续深挖一下的，sendMessage其实是向主线程与mUiHandler挂钩的
                            // 消息队列发送消息。然后再由主线程的Looper去不断循环去处理消息。
                            mUiHandler.sendMessage(message);
                            break;
                        default:
                            break;
                    }
                }
            };
            //本质上是一个死循环通过next来处理从主线程那边发送过来的的消息
            //里面关键有两个方法，一个是在looper()中的msg.target.dispatchMessage(msg);
            //取出消息后交由当初发送消息的子线程中的Handler来处理
            //另一个是在dispatchMessage()方法中的handleMessage()
            Looper.loop();
        }
    }
}

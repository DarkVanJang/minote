/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;

import java.io.IOException;

// 闹钟提醒活动，继承Activity并实现对话框点击和关闭监听
public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {
    private long mNoteId;          // 当前提醒关联的笔记ID
    private String mSnippet;       // 笔记内容摘要
    private static final int SNIPPET_PREW_MAX_LEN = 60; // 摘要最大显示长度
    MediaPlayer mPlayer;           // 媒体播放器用于播放提醒音

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 去除标题栏

        // 窗口属性设置
        final Window win = getWindow();
        // 允许在锁屏界面显示
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 如果屏幕未点亮，设置保持屏幕唤醒的标志
        if (!isScreenOn()) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON  // 点亮屏幕
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }

        // 从Intent获取数据
        Intent intent = getIntent();
        try {
            // 从URI路径中解析笔记ID
            mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
            // 通过内容提供器获取笔记摘要
            mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
            // 截断过长的摘要并添加省略提示
            mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0,
                    SNIPPET_PREW_MAX_LEN) + getResources().getString(R.string.notelist_string_info)
                    : mSnippet;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }

        mPlayer = new MediaPlayer();
        // 检查笔记是否有效存在
        if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
            showActionDialog();  // 显示操作对话框
            playAlarmSound();    // 播放提醒音
        } else {
            finish(); // 无效笔记直接结束
        }
    }

    // 检查屏幕是否点亮
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    // 播放提醒铃声
    private void playAlarmSound() {
        // 获取系统默认闹钟铃声
        Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);

        // 获取静音模式影响的音频流设置
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

        // 根据系统设置设置音频流类型
        if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {
            mPlayer.setAudioStreamType(silentModeStreams);
        } else {
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM); // 使用闹钟音频流
        }
        try {
            mPlayer.setDataSource(this, url);
            mPlayer.prepare();
            mPlayer.setLooping(true); // 循环播放
            mPlayer.start();
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    // 显示操作对话框
    private void showActionDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.app_name); // 使用应用名称作为标题
        dialog.setMessage(mSnippet);         // 显示笔记摘要
        dialog.setPositiveButton(R.string.notealert_ok, this); // 确认按钮
        // 屏幕点亮时显示进入按钮
        if (isScreenOn()) {
            dialog.setNegativeButton(R.string.notealert_enter, this); // 进入查看笔记
        }
        dialog.show().setOnDismissListener(this); // 设置关闭监听
    }

    // 对话框按钮点击事件处理
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                // 跳转到笔记编辑界面
                Intent intent = new Intent(this, NoteEditActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_UID, mNoteId); // 传递笔记ID
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    // 对话框关闭时回调
    public void onDismiss(DialogInterface dialog) {
        stopAlarmSound(); // 停止铃声
        finish();         // 结束活动
    }

    // 停止并释放媒体播放器资源
    private void stopAlarmSound() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release(); // 释放资源
            mPlayer = null;
        }
    }
}

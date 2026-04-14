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

package net.micode.notes.gtask.remote; // 定义包名

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import net.micode.notes.R;
import net.micode.notes.ui.NotesListActivity;
import net.micode.notes.ui.NotesPreferenceActivity;

// GTask异步任务类，用于处理Google任务同步
public class GTaskASyncTask extends AsyncTask<Void, String, Integer> {

    private static int GTASK_SYNC_NOTIFICATION_ID = 5234235; // 同步通知的ID

    // 定义接口OnCompleteListener，在任务完成时调用
    public interface OnCompleteListener {
        void onComplete(); // 完成回调方法
    }

    private Context mContext; // 上下文环境
    private NotificationManager mNotifiManager; // 通知管理器
    private GTaskManager mTaskManager; // Google任务管理器
    private OnCompleteListener mOnCompleteListener; // 完成监听器

    // 构造函数，初始化上下文、监听器、通知管理器和任务管理器
    public GTaskASyncTask(Context context, OnCompleteListener listener) {
        mContext = context;
        mOnCompleteListener = listener;
        mNotifiManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mTaskManager = GTaskManager.getInstance();
    }

    // 取消同步的方法
    public void cancelSync() {
        mTaskManager.cancelSync(); // 调用任务管理器取消同步
    }

    // 发布进度更新的方法
    public void publishProgess(String message) {
        publishProgress(new String[] { message }); // 使用父类方法发布进度更新
    }

    // 显示通知的方法
    private void showNotification(int tickerId, String content) {
        PendingIntent pendingIntent;
        if (tickerId != R.string.ticker_success) { // 如果不是成功状态，则跳转到设置页面
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesPreferenceActivity.class), PendingIntent.FLAG_IMMUTABLE);
        } else { // 成功状态则跳转到笔记列表页面
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesListActivity.class), PendingIntent.FLAG_IMMUTABLE);
        }
        Notification.Builder builder = new Notification.Builder(mContext)
                .setAutoCancel(true) // 设置点击后自动取消
                .setContentTitle(mContext.getString(R.string.app_name)) // 设置标题
                .setContentText(content) // 设置内容
                .setContentIntent(pendingIntent) // 设置点击意图
                .setWhen(System.currentTimeMillis()) // 设置时间
                .setOngoing(true); // 设置为持续的通知
        Notification notification = builder.getNotification(); // 创建通知对象
        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification); // 发送通知
    }

    // 后台执行的任务方法，返回整型结果表示同步状态
    @Override
    protected Integer doInBackground(Void... unused) {
        publishProgess(mContext.getString(R.string.sync_progress_login, NotesPreferenceActivity.getSyncAccountName(mContext))); // 发布登录进度
        return mTaskManager.sync(mContext, this); // 执行同步并返回结果
    }

    // 更新进度的方法，显示同步过程中的通知
    @Override
    protected void onProgressUpdate(String... progress) {
        showNotification(R.string.ticker_syncing, progress[0]); // 显示同步中的通知
        if (mContext instanceof GTaskSyncService) { // 如果上下文是GTaskSyncService类型
            ((GTaskSyncService) mContext).sendBroadcast(progress[0]); // 发送广播
        }
    }

    // 在任务完成后调用的方法，根据同步结果显示不同的通知
    @Override
    protected void onPostExecute(Integer result) {
        if (result == GTaskManager.STATE_SUCCESS) { // 如果同步成功
            showNotification(R.string.ticker_success, mContext.getString(
                    R.string.success_sync_account, mTaskManager.getSyncAccount())); // 显示成功通知
            NotesPreferenceActivity.setLastSyncTime(mContext, System.currentTimeMillis()); // 更新最后同步时间
        } else if (result == GTaskManager.STATE_NETWORK_ERROR) { // 网络错误
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_network)); // 显示网络错误通知
        } else if (result == GTaskManager.STATE_INTERNAL_ERROR) { // 内部错误
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_internal)); // 显示内部错误通知
        } else if (result == GTaskManager.STATE_SYNC_CANCELLED) { // 同步被取消
            showNotification(R.string.ticker_cancel, mContext.getString(R.string.error_sync_cancelled)); // 显示取消通知
        }
        if (mOnCompleteListener != null) { // 如果有完成监听器
            new Thread(new Runnable() {
                public void run() {
                    mOnCompleteListener.onComplete(); // 回调完成方法
                }
            }).start();
        }
    }
}
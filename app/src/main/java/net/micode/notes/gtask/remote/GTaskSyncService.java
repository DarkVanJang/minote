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

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

// GTask同步服务类，用于处理Google任务的后台同步
public class GTaskSyncService extends Service {
    public final static String ACTION_STRING_NAME = "sync_action_type"; // 操作类型的键名

    public final static int ACTION_START_SYNC = 0; // 开始同步操作类型

    public final static int ACTION_CANCEL_SYNC = 1; // 取消同步操作类型

    public final static int ACTION_INVALID = 2; // 无效操作类型

    public final static String GTASK_SERVICE_BROADCAST_NAME = "net.micode.notes.gtask.remote.gtask_sync_service"; // 广播名称

    public final static String GTASK_SERVICE_BROADCAST_IS_SYNCING = "isSyncing"; // 同步状态的键名

    public final static String GTASK_SERVICE_BROADCAST_PROGRESS_MSG = "progressMsg"; // 进度消息的键名

    private static GTaskASyncTask mSyncTask = null; // 当前同步任务实例

    private static String mSyncProgress = ""; // 当前同步进度信息

    // 开始同步方法
    private void startSync() {
        if (mSyncTask == null) { // 如果当前没有同步任务在运行
            mSyncTask = new GTaskASyncTask(this, new GTaskASyncTask.OnCompleteListener() { // 创建新的同步任务
                public void onComplete() { // 在任务完成后回调
                    mSyncTask = null; // 清空当前同步任务实例
                    sendBroadcast(""); // 发送广播通知
                    stopSelf(); // 停止服务
                }
            });
            sendBroadcast(""); // 发送开始同步的广播通知
            mSyncTask.execute(); // 执行异步同步任务
        }
    }

    // 取消同步方法
    private void cancelSync() {
        if (mSyncTask != null) { // 如果有正在运行的同步任务
            mSyncTask.cancelSync(); // 调用取消同步的方法
        }
    }

    @Override
    public void onCreate() {
        mSyncTask = null; // 初始化时清空同步任务实例
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras(); // 获取传入的额外数据
        if (bundle != null && bundle.containsKey(ACTION_STRING_NAME)) { // 检查是否包含操作类型的数据
            switch (bundle.getInt(ACTION_STRING_NAME, ACTION_INVALID)) { // 根据操作类型执行相应操作
                case ACTION_START_SYNC:
                    startSync(); // 开始同步
                    break;
                case ACTION_CANCEL_SYNC:
                    cancelSync(); // 取消同步
                    break;
                default:
                    break;
            }
            return START_STICKY; // 服务被杀死后尝试重启
        }
        return super.onStartCommand(intent, flags, startId); // 默认行为
    }

    @Override
    public void onLowMemory() {
        if (mSyncTask != null) { // 在低内存情况下
            mSyncTask.cancelSync(); // 取消同步任务以释放资源
        }
    }

    public IBinder onBind(Intent intent) {
        return null; // 不支持绑定服务
    }

    // 发送广播通知方法
    public void sendBroadcast(String msg) {
        mSyncProgress = msg; // 更新同步进度信息
        Intent intent = new Intent(GTASK_SERVICE_BROADCAST_NAME); // 创建广播意图
        intent.putExtra(GTASK_SERVICE_BROADCAST_IS_SYNCING, mSyncTask != null); // 添加同步状态信息
        intent.putExtra(GTASK_SERVICE_BROADCAST_PROGRESS_MSG, msg); // 添加进度消息
        sendBroadcast(intent); // 发送广播
    }

    // 静态方法，从Activity启动同步服务
    public static void startSync(Activity activity) {
        GTaskManager.getInstance().setActivityContext(activity); // 设置Activity上下文
        Intent intent = new Intent(activity, GTaskSyncService.class); // 创建服务意图
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_START_SYNC); // 添加操作类型
        activity.startService(intent); // 启动服务
    }

    // 静态方法，取消同步服务
    public static void cancelSync(Context context) {
        Intent intent = new Intent(context, GTaskSyncService.class); // 创建服务意图
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_CANCEL_SYNC); // 添加操作类型
        context.startService(intent); // 启动服务
    }

    // 判断是否有同步任务正在运行
    public static boolean isSyncing() {
        return mSyncTask != null;
    }

    // 获取当前同步进度信息
    public static String getProgressString() {
        return mSyncProgress;
    }
}
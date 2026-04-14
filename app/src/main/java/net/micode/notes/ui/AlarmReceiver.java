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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// 闹钟触发广播接收器，用于启动提醒界面
public class AlarmReceiver extends BroadcastReceiver {
    /**
     * 接收闹钟触发广播后的处理逻辑
     * @param context 上下文对象
     * @param intent 携带触发数据的Intent（包含笔记URI信息）
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 重定向到提醒界面Activity
        intent.setClass(context, AlarmAlertActivity.class);

        // 添加新任务栈标志（从后台服务启动Activity必须添加此标志）
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 启动提醒界面（此时设备可能处于锁屏状态）
        context.startActivity(intent);
    }
}
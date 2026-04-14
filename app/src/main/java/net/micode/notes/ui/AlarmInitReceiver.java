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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;

// 闹钟初始化广播接收器，用于设备启动后重新设置有效提醒
public class AlarmInitReceiver extends BroadcastReceiver {
    // 数据库查询的列投影（需要查询的字段）
    private static final String [] PROJECTION = new String [] {
            NoteColumns.ID,           // 笔记ID列
            NoteColumns.ALERTED_DATE // 提醒时间列
    };

    // 列索引常量
    private static final int COLUMN_ID                = 0; // ID列的索引位置
    private static final int COLUMN_ALERTED_DATE      = 1; // 提醒时间列的索引位置

    @Override
    public void onReceive(Context context, Intent intent) {
        long currentDate = System.currentTimeMillis(); // 获取当前系统时间

        // 查询所有未过期的笔记提醒（提醒时间>当前时间 且 类型为普通笔记）
        Cursor c = context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[] { String.valueOf(currentDate) }, // 绑定当前时间作为查询参数
                null);

        if (c != null) {
            // 遍历查询结果，为每个有效提醒设置闹钟
            if (c.moveToFirst()) {
                do {
                    long alertDate = c.getLong(COLUMN_ALERTED_DATE); // 获取提醒时间

                    // 创建指向AlarmReceiver的Intent
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    // 构建包含笔记ID的URI（格式：content://net.micode.notes/note/id）
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));

                    // 创建PendingIntent用于触发闹钟
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, PendingIntent.FLAG_IMMUTABLE);

                    // 获取系统闹钟服务
                    AlarmManager alarmManager = (AlarmManager) context
                            .getSystemService(Context.ALARM_SERVICE);

                    // 设置精确闹钟，设备休眠时也会唤醒
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                } while (c.moveToNext()); // 遍历所有查询结果
            }
            c.close(); // 关闭游标释放资源
        }
    }
}
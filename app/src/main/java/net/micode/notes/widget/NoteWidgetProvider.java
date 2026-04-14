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
// 指定代码所在的包
package net.micode.notes.widget;
// 引入 Android 和应用程序中的必要类
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NoteEditActivity;
import net.micode.notes.ui.NotesListActivity;

// 定义类 用于创建和更新桌面小部件
public abstract class NoteWidgetProvider extends AppWidgetProvider {
    // 查询笔记时需要提取的列
    // 笔记索引、背景颜色、内容摘要
    public static final String [] PROJECTION = new String [] {
        NoteColumns.ID,
        NoteColumns.BG_COLOR_ID,
        NoteColumns.SNIPPET
    };
    // 用于在游标中引用各列的索引
    public static final int COLUMN_ID           = 0;
    public static final int COLUMN_BG_COLOR_ID  = 1;
    public static final int COLUMN_SNIPPET      = 2;

    private static final String TAG = "NoteWidgetProvider";

    @Override
    // 当小部件被删除时调用 输入：上下文信息、被删除的小部件的ID
    public void onDeleted(Context context, int[] appWidgetIds) {
        // 要更新的数据库值
        ContentValues values = new ContentValues();
        // 将小部件设置为不再有效
        values.put(NoteColumns.WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        // 遍历每一个将要被删除的小部件，设置为无效
        for (int i = 0; i < appWidgetIds.length; i++) {
            context.getContentResolver().update(Notes.CONTENT_NOTE_URI,
                    values,
                    NoteColumns.WIDGET_ID + "=?",
                    new String[] { String.valueOf(appWidgetIds[i])});
        }
    }

    // 从内容提供者查询特定小部件相关的笔记信息 输入：上下文、小部件索引
    private Cursor getNoteWidgetInfo(Context context, int widgetId) {
        // 调用 ContentResolver 的 query 方法，从内容提供者查询数据
        // Notes.CONTENT_NOTE_URI：指定要查询的内容提供者的 URI，指向笔记数据的表
        // PROJECTION：指定要查询的列，这里使用之前定义的列数组，只提取需要的数据
        // NoteColumns.WIDGET_ID + "=? AND " + NoteColumns.PARENT_ID + "<>?"：
        // 构建查询条件。表示只查询小部件 ID 匹配 widgetId 且父级 ID 不等于回收站 ID 的笔记
        return context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.WIDGET_ID + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(widgetId), String.valueOf(Notes.ID_TRASH_FOLER) },
                null);
    }
    // 用于更新小部件 输入：上下文、管理小部件的类、要更新的小部件的索引 直接调用下面的update函数
    protected void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds, false);
    }

    // 用于更新小部件的具体实现
    private void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,
            boolean privacyMode) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            // 检查每个小部件是否有效
            if (appWidgetIds[i] != AppWidgetManager.INVALID_APPWIDGET_ID) {
                int bgId = ResourceParser.getDefaultBgId(context);//获取默认背景 ID
                String snippet = ""; // 初始化一个空字符串，用于存储笔记片段
                // 创建一个 Intent，用于启动 NoteEditActivity，并设置其标志和相关额外数据
                Intent intent = new Intent(context, NoteEditActivity.class);
                // 如果目标活动已经位于栈顶，则不创建新的实例，而是重用现有实例。
                // 如果该活动已经在栈顶，调用 onNewIntent() 方法来处理新的 Intent
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                // 将附加数据放入 Intent 中
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_ID, appWidgetIds[i]);
                // 将小部件的类型信息放入 Intent 中
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_TYPE, getWidgetType());

                // 查询与当前小部件 ID 相关的笔记信息
                Cursor c = getNoteWidgetInfo(context, appWidgetIds[i]);

                if (c != null && c.moveToFirst()) {
                    // 如果查询返回多条记录，记录错误并关闭游标
                    if (c.getCount() > 1) {
                        Log.e(TAG, "Multiple message with same widget id:" + appWidgetIds[i]);
                        c.close();
                        return;
                    }
                    snippet = c.getString(COLUMN_SNIPPET);
                    bgId = c.getInt(COLUMN_BG_COLOR_ID);
                    intent.putExtra(Intent.EXTRA_UID, c.getLong(COLUMN_ID));
                    intent.setAction(Intent.ACTION_VIEW);
                    // 否则，提取笔记片段和背景颜色 ID，并将笔记 ID 添加到 Intent
                } else {
                    snippet = context.getResources().getString(R.string.widget_havenot_content);
                    intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                }
                // 确保游标被关闭，以释放资源
                if (c != null) {
                    c.close();
                }
                // 在小部件的布局中设置视图属性 输入：当前应用包名、小部件布局资源索引（定义了小部件的外观）
                RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());
                rv.setImageViewResource(R.id.widget_bg_image, getBgResourceId(bgId));
                intent.putExtra(Notes.INTENT_EXTRA_BACKGROUND_ID, bgId);
                /**
                 * Generate the pending intent to start host for the widget
                 */
                PendingIntent pendingIntent = null;
                // 如果是隐私模式
                if (privacyMode) {
                    rv.setTextViewText(R.id.widget_text,
                            context.getString(R.string.widget_under_visit_mode));
                    // 当用户点击小部件时，将启用此活动，如果有现有的pendingIntent，则更新
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], new Intent(
                            context, NotesListActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                }
                // 不是隐私模式
                else {
                    // 显示文件内容的摘要
                    rv.setTextViewText(R.id.widget_text, snippet);
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                }
                // 为小部件的文本视图设置点击事件。当用户点击该视图时，将触发之前创建的 PendingIntent。
                // 这样，用户点击小部件文本时，会根据隐私模式启动相应的活动
                rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
                // 调用 AppWidgetManager 更新指定 ID 的小部件。
                // 将之前配置的 RemoteViews 对象 rv 传递给它，应用所有的更改，包括文本和点击事件
                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
            }
        }
    }

    protected abstract int getBgResourceId(int bgId);

    protected abstract int getLayoutId();

    protected abstract int getWidgetType();
}

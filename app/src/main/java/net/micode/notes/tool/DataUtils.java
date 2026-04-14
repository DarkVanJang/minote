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

package net.micode.notes.tool; // 定义包名

// 导入所需的类和包
import android.content.ContentProviderOperation; // 内容提供者操作类
import android.content.ContentProviderResult; // 内容提供者结果类
import android.content.ContentResolver; // 内容解析器类
import android.content.ContentUris; // 内容 URI 工具类
import android.content.ContentValues; // 内容值类
import android.content.OperationApplicationException; // 操作应用异常类
import android.database.Cursor; // 数据库游标类
import android.os.RemoteException; // 远程异常类
import android.util.Log; // 日志工具类

import net.micode.notes.data.Notes; // 笔记数据相关类
import net.micode.notes.data.Notes.CallNote; // 通话笔记相关类
import net.micode.notes.data.Notes.NoteColumns; // 笔记列名相关类
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute; // 应用小部件属性类

import java.util.ArrayList; // 动态数组类
import java.util.HashSet; // 哈希集合类

/**
 * DataUtils 类用于处理笔记数据的工具方法。
 */
public class DataUtils {
    public static final String TAG = "DataUtils"; // 日志标签

    /**
     * 批量删除笔记。
     *
     * @param resolver 内容解析器
     * @param ids 需要删除的笔记 ID 集合
     * @return 删除操作是否成功
     */
    public static boolean batchDeleteNotes(ContentResolver resolver, HashSet<Long> ids) {
        if (ids == null) { // 如果 ID 集合为空
            Log.d(TAG, "the ids is null"); // 记录日志
            return true; // 返回成功
        }
        if (ids.size() == 0) { // 如果 ID 集合为空
            Log.d(TAG, "no id is in the hashset"); // 记录日志
            return true; // 返回成功
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>(); // 创建操作列表
        for (long id : ids) { // 遍历 ID 集合
            if (id == Notes.ID_ROOT_FOLDER) { // 如果是根文件夹
                Log.e(TAG, "Don't delete system folder root"); // 记录错误日志
                continue; // 跳过
            }
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newDelete(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id)); // 创建删除操作
            operationList.add(builder.build()); // 添加到操作列表
        }
        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList); // 批量执行操作
            if (results == null || results.length == 0 || results[0] == null) { // 如果结果为空
                Log.d(TAG, "delete notes failed, ids:" + ids.toString()); // 记录日志
                return false; // 返回失败
            }
            return true; // 返回成功
        } catch (RemoteException e) { // 捕获远程异常
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // 记录错误日志
        } catch (OperationApplicationException e) { // 捕获操作应用异常
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // 记录错误日志
        }
        return false; // 返回失败
    }

    /**
     * 将笔记移动到指定文件夹。
     *
     * @param resolver 内容解析器
     * @param id 笔记 ID
     * @param srcFolderId 源文件夹 ID
     * @param desFolderId 目标文件夹 ID
     */
    public static void moveNoteToFoler(ContentResolver resolver, long id, long srcFolderId, long desFolderId) {
        ContentValues values = new ContentValues(); // 创建内容值对象
        values.put(NoteColumns.PARENT_ID, desFolderId); // 设置目标文件夹 ID
        values.put(NoteColumns.ORIGIN_PARENT_ID, srcFolderId); // 设置源文件夹 ID
        values.put(NoteColumns.LOCAL_MODIFIED, 1); // 设置本地修改标志
        resolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id), values, null, null); // 更新笔记
    }

    /**
     * 批量将笔记移动到指定文件夹。
     *
     * @param resolver 内容解析器
     * @param ids 需要移动的笔记 ID 集合
     * @param folderId 目标文件夹 ID
     * @return 移动操作是否成功
     */
    public static boolean batchMoveToFolder(ContentResolver resolver, HashSet<Long> ids, long folderId) {
        if (ids == null) { // 如果 ID 集合为空
            Log.d(TAG, "the ids is null"); // 记录日志
            return true; // 返回成功
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>(); // 创建操作列表
        for (long id : ids) { // 遍历 ID 集合
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id)); // 创建更新操作
            builder.withValue(NoteColumns.PARENT_ID, folderId); // 设置目标文件夹 ID
            builder.withValue(NoteColumns.LOCAL_MODIFIED, 1); // 设置本地修改标志
            operationList.add(builder.build()); // 添加到操作列表
        }

        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList); // 批量执行操作
            if (results == null || results.length == 0 || results[0] == null) { // 如果结果为空
                Log.d(TAG, "delete notes failed, ids:" + ids.toString()); // 记录日志
                return false; // 返回失败
            }
            return true; // 返回成功
        } catch (RemoteException e) { // 捕获远程异常
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // 记录错误日志
        } catch (OperationApplicationException e) { // 捕获操作应用异常
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // 记录错误日志
        }
        return false; // 返回失败
    }

    /**
     * 获取用户文件夹的数量（不包括系统文件夹）。
     *
     * @param resolver 内容解析器
     * @return 用户文件夹的数量
     */
    public static int getUserFolderCount(ContentResolver resolver) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{"COUNT(*)"}, // 查询数量
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?", // 查询条件
                new String[]{String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)}, // 查询参数
                null); // 排序方式

        int count = 0; // 初始化数量
        if (cursor != null) { // 如果游标不为空
            if (cursor.moveToFirst()) { // 如果游标移动到第一行
                try {
                    count = cursor.getInt(0); // 获取数量
                } catch (IndexOutOfBoundsException e) { // 捕获索引越界异常
                    Log.e(TAG, "get folder count failed:" + e.toString()); // 记录错误日志
                } finally {
                    cursor.close(); // 关闭游标
                }
            }
        }
        return count; // 返回数量
    }

    /**
     * 检查笔记是否在笔记数据库中可见。
     *
     * @param resolver 内容解析器
     * @param noteId 笔记 ID
     * @param type 笔记类型
     * @return 笔记是否可见
     */
    public static boolean visibleInNoteDatabase(ContentResolver resolver, long noteId, int type) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null, // 查询所有列
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER, // 查询条件
                new String[]{String.valueOf(type)}, // 查询参数
                null); // 排序方式

        boolean exist = false; // 初始化存在标志
        if (cursor != null) { // 如果游标不为空
            if (cursor.getCount() > 0) { // 如果查询结果数量大于 0
                exist = true; // 设置存在标志
            }
            cursor.close(); // 关闭游标
        }
        return exist; // 返回存在标志
    }

    /**
     * 检查笔记是否存在于笔记数据库中。
     *
     * @param resolver 内容解析器
     * @param noteId 笔记 ID
     * @return 笔记是否存在
     */
    public static boolean existInNoteDatabase(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null, null, null, null); // 查询笔记

        boolean exist = false; // 初始化存在标志
        if (cursor != null) { // 如果游标不为空
            if (cursor.getCount() > 0) { // 如果查询结果数量大于 0
                exist = true; // 设置存在标志
            }
            cursor.close(); // 关闭游标
        }
        return exist; // 返回存在标志
    }

    /**
     * 检查数据是否存在于数据数据库中。
     *
     * @param resolver 内容解析器
     * @param dataId 数据 ID
     * @return 数据是否存在
     */
    public static boolean existInDataDatabase(ContentResolver resolver, long dataId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId),
                null, null, null, null); // 查询数据

        boolean exist = false; // 初始化存在标志
        if (cursor != null) { // 如果游标不为空
            if (cursor.getCount() > 0) { // 如果查询结果数量大于 0
                exist = true; // 设置存在标志
            }
            cursor.close(); // 关闭游标
        }
        return exist; // 返回存在标志
    }

    /**
     * 检查文件夹名称是否可见。
     *
     * @param resolver 内容解析器
     * @param name 文件夹名称
     * @return 文件夹名称是否可见
     */
    public static boolean checkVisibleFolderName(ContentResolver resolver, String name) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI, null,
                NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + // 查询文件夹
                        " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + // 排除垃圾文件夹
                        " AND " + NoteColumns.SNIPPET + "=?", // 查询指定名称
                new String[]{name}, null); // 查询参数
        boolean exist = false; // 初始化存在标志
        if (cursor != null) { // 如果游标不为空
            if (cursor.getCount() > 0) { // 如果查询结果数量大于 0
                exist = true; // 设置存在标志
            }
            cursor.close(); // 关闭游标
        }
        return exist; // 返回存在标志
    }

    /**
     * 获取文件夹笔记的小部件属性集合。
     *
     * @param resolver 内容解析器
     * @param folderId 文件夹 ID
     * @return 小部件属性集合
     */
    public static HashSet<AppWidgetAttribute> getFolderNoteWidget(ContentResolver resolver, long folderId) {
        Cursor c = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE}, // 查询小部件 ID 和类型
                NoteColumns.PARENT_ID + "=?", // 查询条件
                new String[]{String.valueOf(folderId)}, null); // 查询参数
        HashSet<AppWidgetAttribute> set = null; // 初始化小部件属性集合
        if (c != null) { // 如果游标不为空
            if (c.moveToFirst()) { // 如果游标移动到第一行
                set = new HashSet<AppWidgetAttribute>(); // 创建小部件属性集合
                do {
                    try {
                        AppWidgetAttribute widget = new AppWidgetAttribute(); // 创建小部件属性对象
                        widget.widgetId = c.getInt(0); // 设置小部件 ID
                        widget.widgetType = c.getInt(1); // 设置小部件类型
                        set.add(widget); // 添加到集合
                    } catch (IndexOutOfBoundsException e) { // 捕获索引越界异常
                        Log.e(TAG, e.toString()); // 记录错误日志
                    }
                } while (c.moveToNext()); // 移动到下一行
            }
            c.close(); // 关闭游标
        }
        return set; // 返回小部件属性集合
    }

    /**
     * 通过笔记 ID 获取通话号码。
     *
     * @param resolver 内容解析器
     * @param noteId 笔记 ID
     * @return 通话号码
     */
    public static String getCallNumberByNoteId(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String[]{CallNote.PHONE_NUMBER}, // 查询通话号码
                CallNote.NOTE_ID + "=? AND " + CallNote.MIME_TYPE + "=?", // 查询条件
                new String[]{String.valueOf(noteId), CallNote.CONTENT_ITEM_TYPE}, null); // 查询参数
        if (cursor != null && cursor.moveToFirst()) { // 如果游标不为空且移动到第一行
            try {
                return cursor.getString(0); // 返回通话号码
            } catch (IndexOutOfBoundsException e) { // 捕获索引越界异常
                Log.e(TAG, "Get call number fails " + e.toString()); // 记录错误日志
            } finally {
                cursor.close(); // 关闭游标
            }
        }
        return ""; // 返回空字符串
    }

    /**
     * 通过通话号码和通话日期获取笔记 ID。
     *
     * @param resolver 内容解析器
     * @param phoneNumber 通话号码
     * @param callDate 通话日期
     * @return 笔记 ID
     */
    public static long getNoteIdByPhoneNumberAndCallDate(ContentResolver resolver, String phoneNumber, long callDate) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String[]{CallNote.NOTE_ID}, // 查询笔记 ID
                CallNote.CALL_DATE + "=? AND " + CallNote.MIME_TYPE + "=? AND PHONE_NUMBERS_EQUAL("
                        + CallNote.PHONE_NUMBER + ",?)", // 查询条件
                new String[]{String.valueOf(callDate), CallNote.CONTENT_ITEM_TYPE, phoneNumber}, null); // 查询参数
        if (cursor != null) { // 如果游标不为空
            if (cursor.moveToFirst()) { // 如果游标移动到第一行
                try {
                    return cursor.getLong(0); // 返回笔记 ID
                } catch (IndexOutOfBoundsException e) { // 捕获索引越界异常
                    Log.e(TAG, "Get call note id fails " + e.toString()); // 记录错误日志
                }
            }
            cursor.close(); // 关闭游标
        }
        return 0; // 返回 0
    }

    /**
     * 通过笔记 ID 获取笔记摘要。
     *
     * @param resolver 内容解析器
     * @param noteId 笔记 ID
     * @return 笔记摘要
     */
    public static String getSnippetById(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{NoteColumns.SNIPPET}, // 查询笔记摘要
                NoteColumns.ID + "=?", // 查询条件
                new String[]{String.valueOf(noteId)}, null); // 查询参数
        if (cursor != null) { // 如果游标不为空
            String snippet = ""; // 初始化笔记摘要
            if (cursor.moveToFirst()) { // 如果游标移动到第一行
                snippet = cursor.getString(0); // 获取笔记摘要
            }
            cursor.close(); // 关闭游标
            return snippet; // 返回笔记摘要
        }
        throw new IllegalArgumentException("Note is not found with id: " + noteId); // 抛出异常
    }

    /**
     * 格式化笔记摘要。
     *
     * @param snippet 笔记摘要
     * @return 格式化后的笔记摘要
     */
    public static String getFormattedSnippet(String snippet) {
        if (snippet != null) { // 如果笔记摘要不为空
            snippet = snippet.trim(); // 去除前后空格
            int index = snippet.indexOf('\n'); // 查找换行符位置
            if (index != -1) { // 如果找到换行符
                snippet = snippet.substring(0, index); // 截取换行符前的部分
            }
        }
        return snippet;
    }
}

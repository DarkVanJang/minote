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

// 导入必要的包
package net.micode.notes.model;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;

import java.util.ArrayList;

// Note类用于管理笔记对象
public class Note {
    // 存储笔记的不同值
    private ContentValues mNoteDiffValues;
    // 存储笔记的数据
    private NoteData mNoteData;
    // 日志标签
    private static final String TAG = "Note";

    /**
     * 创建一个新的笔记ID，用于向数据库添加新笔记
     */
    public static synchronized long getNewNoteId(Context context, long folderId) {
        // 创建一个ContentValues对象来存储新的笔记信息
        ContentValues values = new ContentValues();
        // 获取当前时间作为创建和修改日期
        long createdTime = System.currentTimeMillis();
        values.put(NoteColumns.CREATED_DATE, createdTime); // 设置创建日期
        values.put(NoteColumns.MODIFIED_DATE, createdTime); // 设置修改日期
        values.put(NoteColumns.TYPE, Notes.TYPE_NOTE); // 设置笔记类型
        values.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地已修改
        values.put(NoteColumns.PARENT_ID, folderId); // 设置父文件夹ID
        // 插入新笔记到数据库，并获取返回的Uri
        Uri uri = context.getContentResolver().insert(Notes.CONTENT_NOTE_URI, values);
        long noteId = 0;
        try {
            // 尝试从Uri中提取出笔记ID
            noteId = Long.valueOf(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            // 如果提取失败，记录错误日志并设置noteId为0
            Log.e(TAG, "Get note id error :" + e.toString());
            noteId = 0;
        }
        if (noteId == -1) {
            // 如果noteId为-1，抛出异常表示获取笔记ID出错
            throw new IllegalStateException("Wrong note id:" + noteId);
        }
        return noteId; // 返回新的笔记ID
    }

    // 构造函数，初始化成员变量
    public Note() {
        mNoteDiffValues = new ContentValues();
        mNoteData = new NoteData();
    }

    // 设置笔记的某个键的值
    public void setNoteValue(String key, String value) {
        mNoteDiffValues.put(key, value); // 放入内容值
        mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地已修改
        mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
    }

    // 设置文本数据
    public void setTextData(String key, String value) {
        mNoteData.setTextData(key, value); // 调用内部类方法设置文本数据
    }

    // 设置文本数据的ID
    public void setTextDataId(long id) {
        mNoteData.setTextDataId(id); // 设置文本数据的ID
    }

    // 获取文本数据的ID
    public long getTextDataId() {
        return mNoteData.mTextDataId; // 返回文本数据的ID
    }

    // 设置通话数据的ID
    public void setCallDataId(long id) {
        mNoteData.setCallDataId(id); // 设置通话数据的ID
    }

    // 设置通话数据
    public void setCallData(String key, String value) {
        mNoteData.setCallData(key, value); // 调用内部类方法设置通话数据
    }

    // 检查笔记是否有本地修改
    public boolean isLocalModified() {
        return mNoteDiffValues.size() > 0 || mNoteData.isLocalModified(); // 如果有内容值或数据被修改，则返回true
    }

    // 同步笔记到数据库
    public boolean syncNote(Context context, long noteId) {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Wrong note id:" + noteId); // 如果noteId不合法，抛出异常
        }
        if (!isLocalModified()) {
            return true; // 如果没有本地修改，直接返回true
        }
        // 理论上，一旦数据改变，应该更新LOCAL_MODIFIED和MODIFIED_DATE。为了数据安全，即使更新笔记失败也继续更新数据信息
        if (context.getContentResolver().update(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), mNoteDiffValues, null,
                null) == 0) {
            Log.e(TAG, "Update note error, should not happen"); // 记录更新失败的日志
            // 不返回，继续执行
        }
        mNoteDiffValues.clear(); // 清空mNoteDiffValues

        if (mNoteData.isLocalModified()
                && (mNoteData.pushIntoContentResolver(context, noteId) == null)) {
            return false; // 如果数据有修改但推送失败，返回false
        }

        return true; // 成功同步后返回true
    }

    // 内部类，用于处理笔记的具体数据
    private class NoteData {
        private long mTextDataId; // 文本数据的ID
        private ContentValues mTextDataValues; // 文本数据的内容值
        private long mCallDataId; // 通话数据的ID
        private ContentValues mCallDataValues; // 通话数据的内容值
        private static final String TAG = "NoteData"; // 日志标签

        // 构造函数，初始化成员变量
        public NoteData() {
            mTextDataValues = new ContentValues();
            mCallDataValues = new ContentValues();
            mTextDataId = 0;
            mCallDataId = 0;
        }

        // 检查是否有本地修改
        boolean isLocalModified() {
            return mTextDataValues.size() > 0 || mCallDataValues.size() > 0; // 如果有任何数据值，返回true
        }

        // 设置文本数据的ID
        void setTextDataId(long id) {
            if(id <= 0) {
                throw new IllegalArgumentException("Text data id should larger than 0"); // 如果ID不合法，抛出异常
            }
            mTextDataId = id; // 设置文本数据的ID
        }

        // 设置通话数据的ID
        void setCallDataId(long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("Call data id should larger than 0"); // 如果ID不合法，抛出异常
            }
            mCallDataId = id; // 设置通话数据的ID
        }

        // 设置通话数据
        void setCallData(String key, String value) {
            mCallDataValues.put(key, value); // 放入通话数据
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地已修改
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
        }

        // 设置文本数据
        void setTextData(String key, String value) {
            mTextDataValues.put(key, value); // 放入文本数据
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地已修改
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
        }

        // 将数据推送到内容提供者
        Uri pushIntoContentResolver(Context context, long noteId) {
            if (noteId <= 0) {
                throw new IllegalArgumentException("Wrong note id:" + noteId); // 如果noteId不合法，抛出异常
            }
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation.Builder builder = null;

            if(mTextDataValues.size() > 0) {
                mTextDataValues.put(DataColumns.NOTE_ID, noteId); // 添加笔记ID到文本数据
                if (mTextDataId == 0) { // 如果是新数据
                    mTextDataValues.put(DataColumns.MIME_TYPE, TextNote.CONTENT_ITEM_TYPE); // 设置MIME类型
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI, mTextDataValues); // 插入文本数据
                    try {
                        setTextDataId(Long.valueOf(uri.getPathSegments().get(1))); // 从返回的Uri中提取文本数据ID
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new text data fail with noteId" + noteId); // 记录插入失败的日志
                        mTextDataValues.clear();
                        return null;
                    }
                } else { // 如果是更新现有数据
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, mTextDataId));
                    builder.withValues(mTextDataValues); // 使用新的内容值
                    operationList.add(builder.build()); // 添加到操作列表
                }
                mTextDataValues.clear(); // 清空文本数据内容值
            }

            if(mCallDataValues.size() > 0) {
                mCallDataValues.put(DataColumns.NOTE_ID, noteId); // 添加笔记ID到通话数据
                if (mCallDataId == 0) { // 如果是新数据
                    mCallDataValues.put(DataColumns.MIME_TYPE, CallNote.CONTENT_ITEM_TYPE); // 设置MIME类型
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI, mCallDataValues); // 插入通话数据
                    try {
                        setCallDataId(Long.valueOf(uri.getPathSegments().get(1))); // 从返回的Uri中提取通话数据ID
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new call data fail with noteId" + noteId); // 记录插入失败的日志
                        mCallDataValues.clear();
                        return null;
                    }
                } else { // 如果是更新现有数据
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, mCallDataId));
                    builder.withValues(mCallDataValues); // 使用新的内容值
                    operationList.add(builder.build()); // 添加到操作列表
                }
                mCallDataValues.clear(); // 清空通话数据内容值
            }

            if (operationList.size() > 0) {
                try {
                    ContentProviderResult[] results = context.getContentResolver().applyBatch(Notes.AUTHORITY, operationList); // 执行批量操作
                    return (results == null || results.length == 0 || results[0] == null) ? null : ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId); // 返回结果Uri
                } catch (RemoteException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // 记录远程异常日志
                    return null;
                } catch (OperationApplicationException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // 记录操作异常日志
                    return null;
                }
            }
            return null; // 如果没有需要执行的操作，返回null
        }
    }
}
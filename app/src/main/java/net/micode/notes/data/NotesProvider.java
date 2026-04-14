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

package net.micode.notes.data;


import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.NotesDatabaseHelper.TABLE;


public class NotesProvider extends ContentProvider {
    // 创建UriMatcher对象以匹配不同的URI请求
    private static final UriMatcher mMatcher;

    // 数据库帮助类实例
    private NotesDatabaseHelper mHelper;

    // 日志标签
    private static final String TAG = "NotesProvider";

    // 定义不同URI对应的整型常量
    private static final int URI_NOTE            = 1; // 笔记列表
    private static final int URI_NOTE_ITEM       = 2; // 单条笔记
    private static final int URI_DATA            = 3; // 数据列表
    private static final int URI_DATA_ITEM       = 4; // 单条数据
    private static final int URI_SEARCH          = 5; // 搜索笔记
    private static final int URI_SEARCH_SUGGEST  = 6; // 提供搜索建议

    // 初始化UriMatcher
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(Notes.AUTHORITY, "note", URI_NOTE); // 匹配笔记列表URI
        mMatcher.addURI(Notes.AUTHORITY, "note/#", URI_NOTE_ITEM); // 匹配单条笔记URI
        mMatcher.addURI(Notes.AUTHORITY, "data", URI_DATA); // 匹配数据列表URI
        mMatcher.addURI(Notes.AUTHORITY, "data/#", URI_DATA_ITEM); // 匹配单条数据URI
        mMatcher.addURI(Notes.AUTHORITY, "search", URI_SEARCH); // 匹配搜索笔记URI
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_SEARCH_SUGGEST); // 匹配搜索建议URI
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", URI_SEARCH_SUGGEST); // 匹配带查询参数的搜索建议URI
    }

    /**
     * x'0A' 在SQLite中表示换行符('\n')。
     * 对于搜索结果中的标题和内容，我们将修剪掉换行符和空白字符以便显示更多信息。
     */
    private static final String NOTES_SEARCH_PROJECTION = NoteColumns.ID + ","
            + NoteColumns.ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA + "," // 笔记ID作为额外数据
            + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_1 + "," // 第一行文本
            + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_2 + "," // 第二行文本
            + R.drawable.search_result + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1 + "," // 图标
            + "'" + Intent.ACTION_VIEW + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_ACTION + "," // 操作动作
            + "'" + Notes.TextNote.CONTENT_TYPE + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA; // 内容类型

    // SQL查询字符串用于执行搜索
    private static String NOTES_SNIPPET_SEARCH_QUERY = "SELECT " + NOTES_SEARCH_PROJECTION
            + " FROM " + TABLE.NOTE
            + " WHERE " + NoteColumns.SNIPPET + " LIKE ?"
            + " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER // 排除回收站文件夹中的笔记
            + " AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE; // 只搜索笔记类型的记录

    @Override
    public boolean onCreate() {
        // 获取数据库帮助类实例
        mHelper = NotesDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor c = null;
        SQLiteDatabase db = mHelper.getReadableDatabase(); // 获取可读数据库实例
        String id = null;
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // 查询所有笔记
                c = db.query(TABLE.NOTE, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case URI_NOTE_ITEM:
                // 查询特定笔记
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.NOTE, projection, NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_DATA:
                // 查询所有数据项
                c = db.query(TABLE.DATA, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case URI_DATA_ITEM:
                // 查询特定数据项
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.DATA, projection, DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_SEARCH:
            case URI_SEARCH_SUGGEST:
                if (sortOrder != null || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection" + "with this query");
                }

                String searchString = null;
                if (mMatcher.match(uri) == URI_SEARCH_SUGGEST) {
                    if (uri.getPathSegments().size() > 1) {
                        searchString = uri.getPathSegments().get(1); // 获取搜索关键词
                    }
                } else {
                    searchString = uri.getQueryParameter("pattern"); // 从查询参数获取搜索关键词
                }

                if (TextUtils.isEmpty(searchString)) {
                    return null; // 如果搜索关键词为空，则返回null
                }

                try {
                    searchString = String.format("%%%s%%", searchString); // 格式化搜索关键词以支持模糊查询
                    c = db.rawQuery(NOTES_SNIPPET_SEARCH_QUERY, new String[] { searchString }); // 执行搜索查询
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "got exception: " + ex.toString()); // 记录异常信息
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri); // 抛出异常，未知URI
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri); // 设置通知URI
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mHelper.getWritableDatabase(); // 获取可写数据库实例
        long dataId = 0, noteId = 0, insertedId = 0;
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // 插入新笔记
                insertedId = noteId = db.insert(TABLE.NOTE, null, values);
                break;
            case URI_DATA:
                // 插入新数据项
                if (values.containsKey(DataColumns.NOTE_ID)) {
                    noteId = values.getAsLong(DataColumns.NOTE_ID);
                } else {
                    Log.d(TAG, "Wrong data format without note id:" + values.toString()); // 记录错误格式的数据
                }
                insertedId = dataId = db.insert(TABLE.DATA, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri); // 抛出异常，未知URI
        }
        // 通知插入笔记的URI变化
        if (noteId > 0) {
            getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), null);
        }

        // 通知插入数据项的URI变化
        if (dataId > 0) {
            getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId), null);
        }

        return ContentUris.withAppendedId(uri, insertedId); // 返回插入记录的URI
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        String id = null;
        SQLiteDatabase db = mHelper.getWritableDatabase(); // 获取可写数据库实例
        boolean deleteData = false;
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // 删除符合条件的笔记
                selection = "(" + selection + ") AND " + NoteColumns.ID + ">0 "; // 添加条件防止删除系统文件夹
                count = db.delete(TABLE.NOTE, selection, selectionArgs);
                break;
            case URI_NOTE_ITEM:
                // 删除特定笔记
                id = uri.getPathSegments().get(1);
                long noteId = Long.valueOf(id);
                if (noteId <= 0) {
                    break; // 系统文件夹不允许删除
                }
                count = db.delete(TABLE.NOTE, NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                break;
            case URI_DATA:
                // 删除所有符合条件的数据项
                count = db.delete(TABLE.DATA, selection, selectionArgs);
                deleteData = true;
                break;
            case URI_DATA_ITEM:
                // 删除特定数据项
                id = uri.getPathSegments().get(1);
                count = db.delete(TABLE.DATA, DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                deleteData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri); // 抛出异常，未知URI
        }
        if (count > 0) {
            // 通知相关URI的变化
            if (deleteData) {
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        String id = null;
        SQLiteDatabase db = mHelper.getWritableDatabase(); // 获取可写数据库实例
        boolean updateData = false;
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // 更新所有符合条件的笔记版本号
                increaseNoteVersion(-1, selection, selectionArgs);
                count = db.update(TABLE.NOTE, values, selection, selectionArgs);
                break;
            case URI_NOTE_ITEM:
                // 更新特定笔记及其版本号
                id = uri.getPathSegments().get(1);
                increaseNoteVersion(Long.valueOf(id), selection, selectionArgs);
                count = db.update(TABLE.NOTE, values, NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                break;
            case URI_DATA:
                // 更新所有符合条件的数据项
                count = db.update(TABLE.DATA, values, selection, selectionArgs);
                updateData = true;
                break;
            case URI_DATA_ITEM:
                // 更新特定数据项
                id = uri.getPathSegments().get(1);
                count = db.update(TABLE.DATA, values, DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                updateData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri); // 抛出异常，未知URI
        }

        if (count > 0) {
            // 通知相关URI的变化
            if (updateData) {
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    // 解析selection字符串，使其成为有效的SQL语句的一部分
    private String parseSelection(String selection) {
        return (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
    }

    // 增加指定笔记的版本号
    private void increaseNoteVersion(long id, String selection, String[] selectionArgs) {
        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        sql.append(TABLE.NOTE);
        sql.append(" SET ");
        sql.append(NoteColumns.VERSION);
        sql.append("=" + NoteColumns.VERSION + "+1 "); // 版本号加1

        if (id > 0 || !TextUtils.isEmpty(selection)) {
            sql.append(" WHERE ");
        }
        if (id > 0) {
            sql.append(NoteColumns.ID + "=" + String.valueOf(id)); // 指定笔记ID
        }
        if (!TextUtils.isEmpty(selection)) {
            String selectString = id > 0 ? parseSelection(selection) : selection;
            for (String args : selectionArgs) {
                selectString = selectString.replaceFirst("\\?", args); // 替换占位符
            }
            sql.append(selectString);
        }

        mHelper.getWritableDatabase().execSQL(sql.toString()); // 执行SQL更新语句
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null; // 此方法尚未实现
    }
}

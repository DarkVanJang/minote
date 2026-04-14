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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;


public class NotesDatabaseHelper extends SQLiteOpenHelper {
    // 数据库名称和版本
    private static final String DB_NAME = "note.db";
    private static final int DB_VERSION = 5;  // 更改数据库版本

    // 定义表名的接口
    public interface TABLE {
        public static final String NOTE = "note"; // 笔记表
        public static final String DATA = "data"; // 数据表
    }

    // 日志标签
    private static final String TAG = "NotesDatabaseHelper";

    // 单例实例
    private static NotesDatabaseHelper mInstance;

    // 创建笔记表的SQL语句
    private static final String CREATE_NOTE_TABLE_SQL =
            "CREATE TABLE " + TABLE.NOTE + "(" +
                    NoteColumns.ID + " INTEGER PRIMARY KEY," + // 主键ID
                    NoteColumns.PARENT_ID + " INTEGER NOT NULL DEFAULT 0," + // 父级ID，默认为0
                    NoteColumns.ALERTED_DATE + " INTEGER NOT NULL DEFAULT 0," + // 提醒日期，默认为0
                    NoteColumns.BG_COLOR_ID + " INTEGER NOT NULL DEFAULT 0," + // 背景颜色ID，默认为0
                    NoteColumns.CREATED_DATE + " INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)," + // 创建日期，默认为当前时间戳（毫秒）
                    NoteColumns.HAS_ATTACHMENT + " INTEGER NOT NULL DEFAULT 0," + // 是否有附件，默认为0
                    NoteColumns.MODIFIED_DATE + " INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)," + // 修改日期，默认为当前时间戳（毫秒）
                    NoteColumns.NOTES_COUNT + " INTEGER NOT NULL DEFAULT 0," + // 笔记数量，默认为0
                    NoteColumns.SNIPPET + " TEXT NOT NULL DEFAULT ''," + // 笔记摘要，默认为空字符串
                    NoteColumns.TYPE + " INTEGER NOT NULL DEFAULT 0," + // 笔记类型，默认为0
                    NoteColumns.WIDGET_ID + " INTEGER NOT NULL DEFAULT 0," + // 小部件ID，默认为0
                    NoteColumns.WIDGET_TYPE + " INTEGER NOT NULL DEFAULT -1," + // 小部件类型，默认为-1
                    NoteColumns.SYNC_ID + " INTEGER NOT NULL DEFAULT 0," + // 同步ID，默认为0
                    NoteColumns.LOCAL_MODIFIED + " INTEGER NOT NULL DEFAULT 0," + // 本地修改标志，默认为0
                    NoteColumns.ORIGIN_PARENT_ID + " INTEGER NOT NULL DEFAULT 0," + // 原始父级ID，默认为0
                    NoteColumns.GTASK_ID + " TEXT NOT NULL DEFAULT ''," + // Google任务ID，默认为空字符串
                    NoteColumns.VERSION + " INTEGER NOT NULL DEFAULT 0," + // 版本号，默认为0
                    NoteColumns.PASSWORD + " TEXT NOT NULL DEFAULT ''" + //便签密码，默认为空
                    ")";

    // 创建数据表的SQL语句
    private static final String CREATE_DATA_TABLE_SQL =
            "CREATE TABLE " + TABLE.DATA + "(" +
                    DataColumns.ID + " INTEGER PRIMARY KEY," + // 主键ID
                    DataColumns.MIME_TYPE + " TEXT NOT NULL," + // MIME类型，默认不能为空
                    DataColumns.NOTE_ID + " INTEGER NOT NULL DEFAULT 0," + // 关联的笔记ID，默认为0
                    NoteColumns.CREATED_DATE + " INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)," + // 创建日期，默认为当前时间戳（毫秒）
                    NoteColumns.MODIFIED_DATE + " INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)," + // 修改日期，默认为当前时间戳（毫秒）
                    DataColumns.CONTENT + " TEXT NOT NULL DEFAULT ''," + // 内容，默认为空字符串
                    DataColumns.DATA1 + " INTEGER," + // 自定义数据1
                    DataColumns.DATA2 + " INTEGER," + // 自定义数据2
                    DataColumns.DATA3 + " TEXT NOT NULL DEFAULT ''," + // 自定义数据3，默认为空字符串
                    DataColumns.DATA4 + " TEXT NOT NULL DEFAULT ''," + // 自定义数据4，默认为空字符串
                    DataColumns.DATA5 + " TEXT NOT NULL DEFAULT ''" + // 自定义数据5，默认为空字符串
                    ")";

    // 在数据表上创建索引的SQL语句
    private static final String CREATE_DATA_NOTE_ID_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS note_id_index ON " +
                    TABLE.DATA + "(" + DataColumns.NOTE_ID + ");";

    /**
     * 移动笔记到文件夹时增加文件夹中的笔记计数
     */
    private static final String NOTE_INCREASE_FOLDER_COUNT_ON_UPDATE_TRIGGER =
            "CREATE TRIGGER increase_folder_count_on_update "+
                    " AFTER UPDATE OF " + NoteColumns.PARENT_ID + " ON " + TABLE.NOTE +
                    " BEGIN " +
                    "  UPDATE " + TABLE.NOTE +
                    "   SET " + NoteColumns.NOTES_COUNT + "=" + NoteColumns.NOTES_COUNT + " + 1" +
                    "  WHERE " + NoteColumns.ID + "=new." + NoteColumns.PARENT_ID + ";" +
                    " END";

    /**
     * 从文件夹移动笔记时减少文件夹中的笔记计数
     */
    private static final String NOTE_DECREASE_FOLDER_COUNT_ON_UPDATE_TRIGGER =
            "CREATE TRIGGER decrease_folder_count_on_update " +
                    " AFTER UPDATE OF " + NoteColumns.PARENT_ID + " ON " + TABLE.NOTE +
                    " BEGIN " +
                    "  UPDATE " + TABLE.NOTE +
                    "   SET " + NoteColumns.NOTES_COUNT + "=" + NoteColumns.NOTES_COUNT + "-1" +
                    "  WHERE " + NoteColumns.ID + "=old." + NoteColumns.PARENT_ID +
                    "  AND " + NoteColumns.NOTES_COUNT + ">0" + ";" +
                    " END";

    /**
     * 插入新笔记到文件夹时增加文件夹中的笔记计数
     */
    private static final String NOTE_INCREASE_FOLDER_COUNT_ON_INSERT_TRIGGER =
            "CREATE TRIGGER increase_folder_count_on_insert " +
                    " AFTER INSERT ON " + TABLE.NOTE +
                    " BEGIN " +
                    "  UPDATE " + TABLE.NOTE +
                    "   SET " + NoteColumns.NOTES_COUNT + "=" + NoteColumns.NOTES_COUNT + " + 1" +
                    "  WHERE " + NoteColumns.ID + "=new." + NoteColumns.PARENT_ID + ";" +
                    " END";

    /**
     * 删除笔记时减少文件夹中的笔记计数
     */
    private static final String NOTE_DECREASE_FOLDER_COUNT_ON_DELETE_TRIGGER =
            "CREATE TRIGGER decrease_folder_count_on_delete " +
                    " AFTER DELETE ON " + TABLE.NOTE +
                    " BEGIN " +
                    "  UPDATE " + TABLE.NOTE +
                    "   SET " + NoteColumns.NOTES_COUNT + "=" + NoteColumns.NOTES_COUNT + "-1" +
                    "  WHERE " + NoteColumns.ID + "=old." + NoteColumns.PARENT_ID +
                    "  AND " + NoteColumns.NOTES_COUNT + ">0;" +
                    " END";

    /**
     * 插入类型为 {@link DataConstants#NOTE} 的数据时更新笔记的内容
     */
    private static final String DATA_UPDATE_NOTE_CONTENT_ON_INSERT_TRIGGER =
            "CREATE TRIGGER update_note_content_on_insert " +
                    " AFTER INSERT ON " + TABLE.DATA +
                    " WHEN new." + DataColumns.MIME_TYPE + "='" + DataConstants.NOTE + "'" +
                    " BEGIN" +
                    "  UPDATE " + TABLE.NOTE +
                    "   SET " + NoteColumns.SNIPPET + "=new." + DataColumns.CONTENT +
                    "  WHERE " + NoteColumns.ID + "=new." + DataColumns.NOTE_ID + ";" +
                    " END";

    /**
     * 类型为 {@link DataConstants#NOTE} 的数据发生变化时更新笔记的内容
     */
    private static final String DATA_UPDATE_NOTE_CONTENT_ON_UPDATE_TRIGGER =
            "CREATE TRIGGER update_note_content_on_update " +
                    " AFTER UPDATE ON " + TABLE.DATA +
                    " WHEN old." + DataColumns.MIME_TYPE + "='" + DataConstants.NOTE + "'" +
                    " BEGIN" +
                    "  UPDATE " + TABLE.NOTE +
                    "   SET " + NoteColumns.SNIPPET + "=new." + DataColumns.CONTENT +
                    "  WHERE " + NoteColumns.ID + "=new." + DataColumns.NOTE_ID + ";" +
                    " END";

    /**
     * 删除类型为 {@link DataConstants#NOTE} 的数据时清空笔记的内容
     */
    private static final String DATA_UPDATE_NOTE_CONTENT_ON_DELETE_TRIGGER =
            "CREATE TRIGGER update_note_content_on_delete " +
                    " AFTER delete ON " + TABLE.DATA +
                    " WHEN old." + DataColumns.MIME_TYPE + "='" + DataConstants.NOTE + "'" +
                    " BEGIN" +
                    "  UPDATE " + TABLE.NOTE +
                    "   SET " + NoteColumns.SNIPPET + "=''" +
                    "  WHERE " + NoteColumns.ID + "=old." + DataColumns.NOTE_ID + ";" +
                    " END";

    /**
     * 删除笔记时删除关联的数据
     */
    private static final String NOTE_DELETE_DATA_ON_DELETE_TRIGGER =
            "CREATE TRIGGER delete_data_on_delete " +
                    " AFTER DELETE ON " + TABLE.NOTE +
                    " BEGIN" +
                    "  DELETE FROM " + TABLE.DATA +
                    "   WHERE " + DataColumns.NOTE_ID + "=old." + NoteColumns.ID + ";" +
                    " END";

    /**
     * 删除文件夹时删除该文件夹下的所有笔记
     */
    private static final String FOLDER_DELETE_NOTES_ON_DELETE_TRIGGER =
            "CREATE TRIGGER folder_delete_notes_on_delete " +
                    " AFTER DELETE ON " + TABLE.NOTE +
                    " BEGIN" +
                    "  DELETE FROM " + TABLE.NOTE +
                    "   WHERE " + NoteColumns.PARENT_ID + "=old." + NoteColumns.ID + ";" +
                    " END";

    /**
     * 文件夹被移到回收站时，将文件夹内的所有笔记也移到回收站
     */
    private static final String FOLDER_MOVE_NOTES_ON_TRASH_TRIGGER =
            "CREATE TRIGGER folder_move_notes_on_trash " +
                    " AFTER UPDATE ON " + TABLE.NOTE +
                    " WHEN new." + NoteColumns.PARENT_ID + "=" + Notes.ID_TRASH_FOLER +
                    " BEGIN" +
                    "  UPDATE " + TABLE.NOTE +
                    "   SET " + NoteColumns.PARENT_ID + "=" + Notes.ID_TRASH_FOLER +
                    "  WHERE " + NoteColumns.PARENT_ID + "=old." + NoteColumns.ID + ";" +
                    " END";

    // 构造函数
    public NotesDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
//        context.deleteDatabase(DB_NAME);
    }

    /**
     * 创建笔记表并初始化触发器和系统文件夹
     * @param db SQLiteDatabase对象
     */
    public void createNoteTable(SQLiteDatabase db) {
        db.execSQL(CREATE_NOTE_TABLE_SQL); // 执行创建笔记表的SQL语句
        reCreateNoteTableTriggers(db); // 重新创建触发器
        createSystemFolder(db); // 创建系统文件夹
        Log.d(TAG, "note table has been created"); // 记录日志
    }

    /**
     * 重新创建笔记表的所有触发器
     * @param db SQLiteDatabase对象
     */
    private void reCreateNoteTableTriggers(SQLiteDatabase db) {
        // 删除旧的触发器
        db.execSQL("DROP TRIGGER IF EXISTS increase_folder_count_on_update");
        db.execSQL("DROP TRIGGER IF EXISTS decrease_folder_count_on_update");
        db.execSQL("DROP TRIGGER IF EXISTS decrease_folder_count_on_delete");
        db.execSQL("DROP TRIGGER IF EXISTS delete_data_on_delete");
        db.execSQL("DROP TRIGGER IF EXISTS increase_folder_count_on_insert");
        db.execSQL("DROP TRIGGER IF EXISTS folder_delete_notes_on_delete");
        db.execSQL("DROP TRIGGER IF EXISTS folder_move_notes_on_trash");

        // 创建新的触发器
        db.execSQL(NOTE_INCREASE_FOLDER_COUNT_ON_UPDATE_TRIGGER);
        db.execSQL(NOTE_DECREASE_FOLDER_COUNT_ON_UPDATE_TRIGGER);
        db.execSQL(NOTE_DECREASE_FOLDER_COUNT_ON_DELETE_TRIGGER);
        db.execSQL(NOTE_DELETE_DATA_ON_DELETE_TRIGGER);
        db.execSQL(NOTE_INCREASE_FOLDER_COUNT_ON_INSERT_TRIGGER);
        db.execSQL(FOLDER_DELETE_NOTES_ON_DELETE_TRIGGER);
        db.execSQL(FOLDER_MOVE_NOTES_ON_TRASH_TRIGGER);
    }

    /**
     * 创建系统文件夹
     * @param db SQLiteDatabase对象
     */
    private void createSystemFolder(SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        /**
         * 创建通话记录文件夹用于存储通话笔记
         */
        values.put(NoteColumns.ID, Notes.ID_CALL_RECORD_FOLDER);
        values.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM);
        db.insert(TABLE.NOTE, null, values);

        /**
         * 创建根文件夹作为默认文件夹
         */
        values.clear();
        values.put(NoteColumns.ID, Notes.ID_ROOT_FOLDER);
        values.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM);
        db.insert(TABLE.NOTE, null, values);

        /**
         * 创建临时文件夹用于移动笔记
         */
        values.clear();
        values.put(NoteColumns.ID, Notes.ID_TEMPARAY_FOLDER);
        values.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM);
        db.insert(TABLE.NOTE, null, values);

        /**
         * 创建回收站文件夹
         */
        values.clear();
        values.put(NoteColumns.ID, Notes.ID_TRASH_FOLER);
        values.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM);
        db.insert(TABLE.NOTE, null, values);
    }

    /**
     * 创建数据表并初始化触发器和索引
     * @param db SQLiteDatabase对象
     */
    public void createDataTable(SQLiteDatabase db) {
        db.execSQL(CREATE_DATA_TABLE_SQL); // 执行创建数据表的SQL语句
        reCreateDataTableTriggers(db); // 重新创建触发器
        db.execSQL(CREATE_DATA_NOTE_ID_INDEX_SQL); // 创建索引
        Log.d(TAG, "data table has been created"); // 记录日志
    }

    /**
     * 重新创建数据表的所有触发器
     * @param db SQLiteDatabase对象
     */
    private void reCreateDataTableTriggers(SQLiteDatabase db) {
        // 删除旧的触发器
        db.execSQL("DROP TRIGGER IF EXISTS update_note_content_on_insert");
        db.execSQL("DROP TRIGGER IF EXISTS update_note_content_on_update");
        db.execSQL("DROP TRIGGER IF EXISTS update_note_content_on_delete");

        // 创建新的触发器
        db.execSQL(DATA_UPDATE_NOTE_CONTENT_ON_INSERT_TRIGGER);
        db.execSQL(DATA_UPDATE_NOTE_CONTENT_ON_UPDATE_TRIGGER);
        db.execSQL(DATA_UPDATE_NOTE_CONTENT_ON_DELETE_TRIGGER);
    }

    /**
     * 获取数据库帮助类的单例实例
     * @param context 上下文环境
     * @return NotesDatabaseHelper实例
     */
    static synchronized NotesDatabaseHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NotesDatabaseHelper(context);
        }
        return mInstance;
    }

    /**
     * 当数据库首次创建时调用此方法
     * @param db SQLiteDatabase对象
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        createNoteTable(db); // 创建笔记表
        createDataTable(db); // 创建数据表
    }

    /**
     * 当数据库需要升级时调用此方法
     * @param db SQLiteDatabase对象
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        boolean reCreateTriggers = false;
        boolean skipV2 = false;

        if (oldVersion == 1) {
            upgradeToV2(db); // 升级到版本2
            skipV2 = true; // 这次升级包括从v2到v3的升级
            oldVersion++;
        }

        if (oldVersion == 2 && !skipV2) {
            upgradeToV3(db); // 升级到版本3
            reCreateTriggers = true;
            oldVersion++;
        }

        if (oldVersion == 3) {
            upgradeToV4(db); // 升级到版本4
            oldVersion++;
        }

        if (oldVersion == 4) {
            upgradeToV5(db); // 升级到版本5
            oldVersion++;
        }

        if (reCreateTriggers) {
            reCreateNoteTableTriggers(db); // 重新创建笔记表触发器
            reCreateDataTableTriggers(db); // 重新创建数据表触发器
        }

        if (oldVersion != newVersion) {
            throw new IllegalStateException("Upgrade notes database to version " + newVersion + " fails");
        }
    }

    /**
     * 升级到版本2的操作
     * @param db SQLiteDatabase对象
     */
    private void upgradeToV2(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE.NOTE); // 删除旧的笔记表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE.DATA); // 删除旧的数据表
        createNoteTable(db); // 创建新的笔记表
        createDataTable(db); // 创建新的数据表
    }

    /**
     * 升级到版本3的操作
     * @param db SQLiteDatabase对象
     */
    private void upgradeToV3(SQLiteDatabase db) {
        // 删除不再使用的触发器
        db.execSQL("DROP TRIGGER IF EXISTS update_note_modified_date_on_insert");
        db.execSQL("DROP TRIGGER IF EXISTS update_note_modified_date_on_delete");
        db.execSQL("DROP TRIGGER IF EXISTS update_note_modified_date_on_update");

        // 添加一个Google任务ID列
        db.execSQL("ALTER TABLE " + TABLE.NOTE + " ADD COLUMN " + NoteColumns.GTASK_ID
                + " TEXT NOT NULL DEFAULT ''");

        // 添加一个回收站系统文件夹
        ContentValues values = new ContentValues();
        values.put(NoteColumns.ID, Notes.ID_TRASH_FOLER);
        values.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM);
        db.insert(TABLE.NOTE, null, values);
    }

    /**
     * 升级到版本4的操作
     * @param db SQLiteDatabase对象
     */
    private void upgradeToV4(SQLiteDatabase db) {
        // 添加一个版本号列
        db.execSQL("ALTER TABLE " + TABLE.NOTE + " ADD COLUMN " + NoteColumns.VERSION
                + " INTEGER NOT NULL DEFAULT 0");
    }
    private void upgradeToV5(SQLiteDatabase db) {
        // 添加一个密码字段
        db.execSQL("ALTER TABLE " + TABLE.NOTE + " ADD COLUMN " + NoteColumns.PASSWORD
                + " TEXT NOT NULL DEFAULT ''");
    }
}





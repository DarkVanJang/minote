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
import android.content.Context; // 上下文类
import android.database.Cursor; // 数据库游标
import android.os.Environment; // 环境类，用于访问外部存储
import android.text.TextUtils; // 文本工具类
import android.text.format.DateFormat; // 日期格式化工具
import android.util.Log; // 日志工具

import net.micode.notes.R; // 资源文件
import net.micode.notes.data.Notes; // 笔记数据相关类
import net.micode.notes.data.Notes.DataColumns; // 数据列名
import net.micode.notes.data.Notes.DataConstants; // 数据常量
import net.micode.notes.data.Notes.NoteColumns; // 笔记列名

import java.io.File; // 文件类
import java.io.FileNotFoundException; // 文件未找到异常
import java.io.FileOutputStream; // 文件输出流
import java.io.IOException; // IO 异常
import java.io.PrintStream; // 打印流

/**
 * BackupUtils 类用于实现笔记数据的备份功能，将笔记数据导出为文本文件。
 */
public class BackupUtils {
    private static final String TAG = "BackupUtils"; // 日志标签

    // 单例模式相关
    private static BackupUtils sInstance; // 单例实例

    /**
     * 获取 BackupUtils 的单例实例。
     *
     * @param context 上下文对象
     * @return BackupUtils 实例
     */
    public static synchronized BackupUtils getInstance(Context context) {
        if (sInstance == null) { // 如果实例为空
            sInstance = new BackupUtils(context); // 创建新实例
        }
        return sInstance; // 返回实例
    }

    /**
     * 定义备份或恢复的状态常量。
     */
    public static final int STATE_SD_CARD_UNMOUONTED = 0; // SD 卡未挂载
    public static final int STATE_BACKUP_FILE_NOT_EXIST = 1; // 备份文件不存在
    public static final int STATE_DATA_DESTROIED = 2; // 数据被破坏
    public static final int STATE_SYSTEM_ERROR = 3; // 系统错误
    public static final int STATE_SUCCESS = 4; // 备份或恢复成功

    private TextExport mTextExport; // 文本导出工具

    /**
     * 私有构造函数，初始化 TextExport。
     *
     * @param context 上下文对象
     */
    private BackupUtils(Context context) {
        mTextExport = new TextExport(context); // 初始化 TextExport
    }

    /**
     * 检查外部存储是否可用。
     *
     * @return 外部存储是否可用
     */
    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()); // 检查外部存储状态
    }

    /**
     * 导出笔记数据为文本文件。
     *
     * @return 导出操作的状态
     */
    public int exportToText() {
        return mTextExport.exportToText(); // 调用 TextExport 的导出方法
    }

    /**
     * 获取导出的文本文件名。
     *
     * @return 文件名
     */
    public String getExportedTextFileName() {
        return mTextExport.mFileName; // 返回文件名
    }

    /**
     * 获取导出的文本文件目录。
     *
     * @return 文件目录
     */
    public String getExportedTextFileDir() {
        return mTextExport.mFileDirectory; // 返回文件目录
    }

    /**
     * TextExport 类用于将笔记数据导出为文本文件。
     */
    private static class TextExport {
        // 查询笔记的列名
        private static final String[] NOTE_PROJECTION = {
                NoteColumns.ID, // 笔记 ID
                NoteColumns.MODIFIED_DATE, // 修改日期
                NoteColumns.SNIPPET, // 笔记摘要
                NoteColumns.TYPE // 笔记类型
        };

        private static final int NOTE_COLUMN_ID = 0; // 笔记 ID 列索引
        private static final int NOTE_COLUMN_MODIFIED_DATE = 1; // 修改日期列索引
        private static final int NOTE_COLUMN_SNIPPET = 2; // 笔记摘要列索引

        // 查询笔记数据的列名
        private static final String[] DATA_PROJECTION = {
                DataColumns.CONTENT, // 内容
                DataColumns.MIME_TYPE, // MIME 类型
                DataColumns.DATA1, // 数据1
                DataColumns.DATA2, // 数据2
                DataColumns.DATA3, // 数据3
                DataColumns.DATA4 // 数据4
        };

        private static final int DATA_COLUMN_CONTENT = 0; // 内容列索引
        private static final int DATA_COLUMN_MIME_TYPE = 1; // MIME 类型列索引
        private static final int DATA_COLUMN_CALL_DATE = 2; // 通话日期列索引
        private static final int DATA_COLUMN_PHONE_NUMBER = 4; // 电话号码列索引

        private final String[] TEXT_FORMAT; // 文本格式数组
        private static final int FORMAT_FOLDER_NAME = 0; // 文件夹名称格式索引
        private static final int FORMAT_NOTE_DATE = 1; // 笔记日期格式索引
        private static final int FORMAT_NOTE_CONTENT = 2; // 笔记内容格式索引

        private Context mContext; // 上下文对象
        private String mFileName; // 导出的文件名
        private String mFileDirectory; // 导出的文件目录

        /**
         * TextExport 构造函数，初始化文本格式数组。
         *
         * @param context 上下文对象
         */
        public TextExport(Context context) {
            TEXT_FORMAT = context.getResources().getStringArray(R.array.format_for_exported_note); // 从资源文件中加载文本格式
            mContext = context; // 初始化上下文
            mFileName = ""; // 初始化文件名
            mFileDirectory = ""; // 初始化文件目录
        }

        /**
         * 获取指定索引的文本格式。
         *
         * @param id 格式索引
         * @return 文本格式
         */
        private String getFormat(int id) {
            return TEXT_FORMAT[id]; // 返回指定索引的文本格式
        }

        /**
         * 将指定文件夹的笔记导出为文本。
         *
         * @param folderId 文件夹 ID
         * @param ps 打印流
         */
        private void exportFolderToText(String folderId, PrintStream ps) {
            // 查询属于该文件夹的笔记
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[]{folderId}, null);

            if (notesCursor != null) { // 如果游标不为空
                if (notesCursor.moveToFirst()) { // 如果游标移动到第一行
                    do {
                        // 打印笔记的最后修改日期
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // 查询属于该笔记的数据
                        String noteId = notesCursor.getString(NOTE_COLUMN_ID); // 获取笔记 ID
                        exportNoteToText(noteId, ps); // 导出该笔记
                    } while (notesCursor.moveToNext()); // 移动到下一行
                }
                notesCursor.close(); // 关闭游标
            }
        }

        /**
         * 将指定笔记导出为文本。
         *
         * @param noteId 笔记 ID
         * @param ps 打印流
         */
        private void exportNoteToText(String noteId, PrintStream ps) {
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                    DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[]{noteId}, null);

            if (dataCursor != null) { // 如果游标不为空
                if (dataCursor.moveToFirst()) { // 如果游标移动到第一行
                    do {
                        String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE); // 获取 MIME 类型
                        if (DataConstants.CALL_NOTE.equals(mimeType)) { // 如果是通话笔记
                            // 打印电话号码
                            String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER); // 获取电话号码
                            long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE); // 获取通话日期
                            String location = dataCursor.getString(DATA_COLUMN_CONTENT); // 获取通话位置

                            if (!TextUtils.isEmpty(phoneNumber)) { // 如果电话号码不为空
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        phoneNumber)); // 打印电话号码
                            }
                            // 打印通话日期
                            ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), DateFormat
                                    .format(mContext.getString(R.string.format_datetime_mdhm),
                                            callDate))); // 打印通话日期
                            // 打印通话附件位置
                            if (!TextUtils.isEmpty(location)) { // 如果位置不为空
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        location)); // 打印位置
                            }
                        } else if (DataConstants.NOTE.equals(mimeType)) { // 如果是普通笔记
                            String content = dataCursor.getString(DATA_COLUMN_CONTENT); // 获取笔记内容
                            if (!TextUtils.isEmpty(content)) { // 如果内容不为空
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        content)); // 打印笔记内容
                            }
                        }
                    } while (dataCursor.moveToNext()); // 移动到下一行
                }
                dataCursor.close(); // 关闭游标
            }
            // 在笔记之间打印分隔符
            try {
                ps.write(new byte[]{Character.LINE_SEPARATOR, Character.LETTER_NUMBER}); // 写入分隔符
            } catch (IOException e) {
                Log.e(TAG, e.toString()); // 记录异常日志
            }
        }

        /**
         * 将笔记导出为文本文件。
         *
         * @return 导出操作的状态
         */
        public int exportToText() {
            if (!externalStorageAvailable()) { // 如果外部存储不可用
                Log.d(TAG, "Media was not mounted"); // 记录日志
                return STATE_SD_CARD_UNMOUONTED; // 返回 SD 卡未挂载状态
            }

            PrintStream ps = getExportToTextPrintStream(); // 获取打印流
            if (ps == null) { // 如果打印流为空
                Log.e(TAG, "get print stream error"); // 记录错误日志
                return STATE_SYSTEM_ERROR; // 返回系统错误状态
            }
            // 首先导出文件夹及其笔记
            Cursor folderCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                            + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, null);

            if (folderCursor != null) { // 如果游标不为空
                if (folderCursor.moveToFirst()) { // 如果游标移动到第一行
                    do {
                        // 打印文件夹名称
                        String folderName = ""; // 初始化文件夹名称
                        if (folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) { // 如果是通话记录文件夹
                            folderName = mContext.getString(R.string.call_record_folder_name); // 获取通话记录文件夹名称
                        } else {
                            folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET); // 获取文件夹摘要
                        }
                        if (!TextUtils.isEmpty(folderName)) { // 如果文件夹名称不为空
                            ps.println(String.format(getFormat(FORMAT_FOLDER_NAME), folderName)); // 打印文件夹名称
                        }
                        String folderId = folderCursor.getString(NOTE_COLUMN_ID); // 获取文件夹 ID
                        exportFolderToText(folderId, ps); // 导出该文件夹
                    } while (folderCursor.moveToNext()); // 移动到下一行
                }
                folderCursor.close(); // 关闭游标
            }

            // 导出根目录下的笔记
            Cursor noteCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    NoteColumns.TYPE + "=" + +Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                            + "=0", null, null);

            if (noteCursor != null) { // 如果游标不为空
                if (noteCursor.moveToFirst()) { // 如果游标移动到第一行
                    do {
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE)))); // 打印笔记日期
                        // 查询属于该笔记的数据
                        String noteId = noteCursor.getString(NOTE_COLUMN_ID); // 获取笔记 ID
                        exportNoteToText(noteId, ps); // 导出该笔记
                    } while (noteCursor.moveToNext()); // 移动到下一行
                }
                noteCursor.close(); // 关闭游标
            }
            ps.close(); // 关闭打印流

            return STATE_SUCCESS; // 返回成功状态
        }

        /**
         * 获取指向导出文件的打印流。
         *
         * @return 打印流
         */
        private PrintStream getExportToTextPrintStream() {
            File file = generateFileMountedOnSDcard(mContext, R.string.file_path,
                    R.string.file_name_txt_format); // 生成导出文件
            if (file == null) { // 如果文件为空
                Log.e(TAG, "create file to exported failed"); // 记录错误日志
                return null; // 返回空
            }
            mFileName = file.getName(); // 设置文件名
            mFileDirectory = mContext.getString(R.string.file_path); // 设置文件目录
            PrintStream ps = null; // 初始化打印流
            try {
                FileOutputStream fos = new FileOutputStream(file); // 创建文件输出流
                ps = new PrintStream(fos); // 创建打印流
            } catch (FileNotFoundException e) {
                e.printStackTrace(); // 打印异常堆栈
                return null; // 返回空
            } catch (NullPointerException e) {
                e.printStackTrace(); // 打印异常堆栈
                return null; // 返回空
            }
            return ps; // 返回打印流
        }
    }

    /**
     * 在 SD 卡上生成用于存储导出数据的文本文件。
     *
     * @param context 上下文对象
     * @param filePathResId 文件路径资源 ID
     * @param fileNameFormatResId 文件名格式资源 ID
     * @return 生成的文件
     */
    private static File generateFileMountedOnSDcard(Context context, int filePathResId, int fileNameFormatResId) {
        StringBuilder sb = new StringBuilder(); // 创建字符串构建器
        sb.append(Environment.getExternalStorageDirectory()); // 添加外部存储目录
        sb.append(context.getString(filePathResId)); // 添加文件路径
        File filedir = new File(sb.toString()); // 创建文件目录对象
        sb.append(context.getString(
                fileNameFormatResId,
                DateFormat.format(context.getString(R.string.format_date_ymd),
                        System.currentTimeMillis()))); // 添加文件名
        File file = new File(sb.toString()); // 创建文件对象

        try {
            if (!filedir.exists()) { // 如果目录不存在
                filedir.mkdir(); // 创建目录
            }
            if (!file.exists()) { // 如果文件不存在
                file.createNewFile(); // 创建文件
            }
            return file; // 返回文件
        } catch (SecurityException e) {
            e.printStackTrace(); // 打印异常堆栈
        } catch (IOException e) {
            e.printStackTrace(); // 打印异常堆栈
        }

        return null; // 返回空
    }
}
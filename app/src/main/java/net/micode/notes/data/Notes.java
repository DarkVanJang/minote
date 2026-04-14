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

import android.net.Uri;

/**
 * 笔记应用核心常量定义类
 * 包含：内容提供者URI、笔记类型、系统文件夹ID、数据表列名等全局配置
 */
public class Notes {
    // 内容提供者的授权标识（对应AndroidManifest中配置的authorities）
    public static final String AUTHORITY = "micode_notes";
    // 日志标签
    public static final String TAG = "Notes";

    // 笔记类型常量
    public static final int TYPE_NOTE     = 0;   // 普通笔记类型
    public static final int TYPE_FOLDER   = 1;   // 文件夹类型
    public static final int TYPE_SYSTEM   = 2;   // 系统文件夹类型

    /**
     * 系统文件夹ID定义：
     * - {@link Notes#ID_ROOT_FOLDER} 根文件夹（默认展示的文件夹）
     * - {@link Notes#ID_TEMPARAY_FOLDER} 临时文件夹（存放未分类笔记，拼写注意应为TEMPORARY）
     * - {@link Notes#ID_CALL_RECORD_FOLDER} 通话记录文件夹
     * - {@link Notes#ID_TRASH_FOLER} 回收站文件夹（正确拼写应为TRASH_FOLDER）
     */
    public static final int ID_ROOT_FOLDER = 0;          // 根文件夹ID
    public static final int ID_TEMPARAY_FOLDER = -1;      // 临时文件夹ID
    public static final int ID_CALL_RECORD_FOLDER = -2;  // 通话记录文件夹ID
    public static final int ID_TRASH_FOLER = -3;         // 回收站文件夹ID（拼写错误保留）

    // Intent 附加参数键名
    public static final String INTENT_EXTRA_ALERT_DATE = "net.micode.notes.alert_date";       // 提醒时间
    public static final String INTENT_EXTRA_BACKGROUND_ID = "net.micode.notes.background_color_id"; // 背景色ID
    public static final String INTENT_EXTRA_WIDGET_ID = "net.micode.notes.widget_id";         // 桌面小部件ID
    public static final String INTENT_EXTRA_WIDGET_TYPE = "net.micode.notes.widget_type";     // 小部件类型
    public static final String INTENT_EXTRA_FOLDER_ID = "net.micode.notes.folder_id";         // 文件夹ID
    public static final String INTENT_EXTRA_CALL_DATE = "net.micode.notes.call_date";         // 通话日期

    // 小部件类型常量
    public static final int TYPE_WIDGET_INVALIDE      = -1; // 无效小部件类型
    public static final int TYPE_WIDGET_2X            = 0;  // 2x尺寸小部件
    public static final int TYPE_WIDGET_4X            = 1;  // 4x尺寸小部件

    /** 笔记数据类型常量 */
    public static class DataConstants {
        public static final String NOTE = TextNote.CONTENT_ITEM_TYPE;      // 文本笔记MIME类型
        public static final String CALL_NOTE = CallNote.CONTENT_ITEM_TYPE; // 通话笔记MIME类型
    }

    // 内容提供者URI定义
    /** 查询所有笔记和文件夹的URI */
    public static final Uri CONTENT_NOTE_URI = Uri.parse("content://" + AUTHORITY + "/note");
    /** 查询笔记详细数据的URI */
    public static final Uri CONTENT_DATA_URI = Uri.parse("content://" + AUTHORITY + "/data");

    /** 笔记数据表列名定义 */
    public interface NoteColumns {
        /**
         * 行唯一ID
         * <P> 类型: LONG </P>
         */
        public static final String ID = "_id";

        /**
         * 父项ID（用于构建文件夹层级）
         * <P> 类型: LONG </P>
         */
        public static final String PARENT_ID = "parent_id";

        /**
         * 创建时间（Unix时间戳）
         * <P> 类型: LONG </P>
         */
        public static final String CREATED_DATE = "created_date";

        /**
         * 最后修改时间（Unix时间戳）
         * <P> 类型: LONG </P>
         */
        public static final String MODIFIED_DATE = "modified_date";

        /**
         * 提醒时间（Unix时间戳）
         * <P> 类型: LONG </P>
         */
        public static final String ALERTED_DATE = "alert_date";

        /**
         * 内容摘要（文件夹显示名称/笔记首行文本）
         * <P> 类型: TEXT </P>
         */
        public static final String SNIPPET = "snippet";

        /**
         * 关联的小部件ID
         * <P> 类型: LONG </P>
         */
        public static final String WIDGET_ID = "widget_id";

        /**
         * 小部件类型（2x或4x）
         * <P> 类型: INT </P>
         */
        public static final String WIDGET_TYPE = "widget_type";

        /**
         * 背景颜色ID
         * <P> 类型: INT </P>
         */
        public static final String BG_COLOR_ID = "bg_color_id";

        /**
         * 是否有附件（0无/1有）
         * <P> 类型: INT </P>
         */
        public static final String HAS_ATTACHMENT = "has_attachment";

        /**
         * 文件夹内笔记数量
         * <P> 类型: LONG </P>
         */
        public static final String NOTES_COUNT = "notes_count";

        /**
         * 条目类型（笔记/文件夹/系统文件夹）
         * <P> 类型: INT </P>
         */
        public static final String TYPE = "type";

        /**
         * 同步ID（用于云同步）
         * <P> 类型: LONG </P>
         */
        public static final String SYNC_ID = "sync_id";

        /**
         * 本地修改标记（0未修改/1已修改）
         * <P> 类型: INT </P>
         */
        public static final String LOCAL_MODIFIED = "local_modified";

        /**
         * 原始父ID（移动至临时文件夹前的位置）
         * <P> 类型: LONG </P>
         */
        public static final String ORIGIN_PARENT_ID = "origin_parent_id";

        /**
         * Google任务ID（如果与GTasks集成）
         * <P> 类型: TEXT </P>
         */
        public static final String GTASK_ID = "gtask_id";

        /**
         * 数据版本号（用于冲突解决）
         * <P> 类型: LONG </P>
         */
        public static final String VERSION = "version";

        /**
         * 便签密码
         * <P> 类型: STRING </P>
         */
        public static final String PASSWORD = "password";
    }

    /** 笔记内容数据表列名定义 */
    public interface DataColumns {
        /**
         * 行唯一ID
         * <P> 类型: LONG </P>
         */
        public static final String ID = "_id";

        /**
         * MIME类型（区分文本笔记、通话记录等数据类型）
         * <P> 类型: TEXT </P>
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * 关联的笔记ID
         * <P> 类型: LONG </P>
         */
        public static final String NOTE_ID = "note_id";

        /**
         * 创建时间（Unix时间戳）
         * <P> 类型: LONG </P>
         */
        public static final String CREATED_DATE = "created_date";

        /**
         * 最后修改时间（Unix时间戳）
         * <P> 类型: LONG </P>
         */
        public static final String MODIFIED_DATE = "modified_date";

        /**
         * 主要内容（文本笔记的完整内容）
         * <P> 类型: TEXT </P>
         */
        public static final String CONTENT = "content";

        /**
         * 扩展数据1（类型由MIME_TYPE决定，存储整型数据）
         * <P> 类型: INT </P>
         */
        public static final String DATA1 = "data1";

        /**
         * 扩展数据2（类型由MIME_TYPE决定，存储整型数据）
         * <P> 类型: INT </P>
         */
        public static final String DATA2 = "data2";

        /**
         * 扩展数据3（类型由MIME_TYPE决定，存储文本数据）
         * <P> 类型: TEXT </P>
         */
        public static final String DATA3 = "data3";

        /**
         * 扩展数据4（类型由MIME_TYPE决定，存储文本数据）
         * <P> 类型: TEXT </P>
         */
        public static final String DATA4 = "data4";

        /**
         * 扩展数据5（类型由MIME_TYPE决定，存储文本数据）
         * <P> 类型: TEXT </P>
         */
        public static final String DATA5 = "data5";
    }

    /** 文本笔记具体实现 */
    public static final class TextNote implements DataColumns {
        /**
         * 模式标识（0-普通文本模式 / 1-清单模式）
         * <P> 类型: INT </P>
         */
        public static final String MODE = DATA1;

        public static final int MODE_CHECK_LIST = 1; // 清单模式标识值

        // MIME类型定义
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/text_note";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/text_note";

        // 文本笔记内容URI
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/text_note");
    }

    /** 通话记录笔记具体实现 */
    public static final class CallNote implements DataColumns {
        /**
         * 通话时间（Unix时间戳）
         * <P> 类型: LONG </P>
         */
        public static final String CALL_DATE = DATA1;

        /**
         * 电话号码
         * <P> 类型: TEXT </P>
         */
        public static final String PHONE_NUMBER = DATA3;

        // MIME类型定义
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/call_note";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/call_note";

        // 通话笔记内容URI
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/call_note");
    }
}

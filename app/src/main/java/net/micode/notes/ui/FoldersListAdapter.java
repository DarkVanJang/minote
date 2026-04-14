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

package net.micode.notes.ui; // 定义包名

// 导入所需的类和包
import android.content.Context; // 上下文类
import android.database.Cursor; // 数据库游标类
import android.view.View; // 视图类
import android.view.ViewGroup; // 视图组类
import android.widget.CursorAdapter; // 游标适配器类
import android.widget.LinearLayout; // 线性布局类
import android.widget.TextView; // 文本视图类

import net.micode.notes.R; // 资源文件
import net.micode.notes.data.Notes; // 笔记数据相关类
import net.micode.notes.data.Notes.NoteColumns; // 笔记列名

/**
 * FoldersListAdapter 类用于管理文件夹列表的适配器。
 */
public class FoldersListAdapter extends CursorAdapter {
    // 定义查询的列名
    public static final String[] PROJECTION = {
            NoteColumns.ID, // 笔记 ID
            NoteColumns.SNIPPET // 笔记摘要
    };

    public static final int ID_COLUMN = 0; // ID 列的索引
    public static final int NAME_COLUMN = 1; // 名称列的索引

    /**
     * 构造函数，初始化适配器。
     *
     * @param context 上下文对象
     * @param c       游标对象
     */
    public FoldersListAdapter(Context context, Cursor c) {
        super(context, c); // 调用父类构造函数
        // TODO Auto-generated constructor stub
    }

    /**
     * 创建新的列表项视图。
     *
     * @param context 上下文对象
     * @param cursor  游标对象
     * @param parent  父视图组
     * @return 新的列表项视图
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new FolderListItem(context); // 返回一个新的文件夹列表项视图
    }

    /**
     * 绑定数据到列表项视图。
     *
     * @param view    列表项视图
     * @param context 上下文对象
     * @param cursor  游标对象
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof FolderListItem) { // 如果视图是 FolderListItem 类型
            // 获取文件夹名称，如果是根文件夹则使用特定字符串，否则使用游标中的名称
            String folderName = (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                    .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
            ((FolderListItem) view).bind(folderName); // 绑定文件夹名称到视图
        }
    }

    /**
     * 获取指定位置的文件夹名称。
     *
     * @param context  上下文对象
     * @param position 列表项位置
     * @return 文件夹名称
     */
    public String getFolderName(Context context, int position) {
        Cursor cursor = (Cursor) getItem(position); // 获取指定位置的游标
        // 返回文件夹名称，如果是根文件夹则使用特定字符串，否则使用游标中的名称
        return (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
    }

    /**
     * FolderListItem 类用于表示文件夹列表项的视图。
     */
    private class FolderListItem extends LinearLayout {
        private TextView mName; // 文件夹名称文本视图

        /**
         * 构造函数，初始化文件夹列表项视图。
         *
         * @param context 上下文对象
         */
        public FolderListItem(Context context) {
            super(context); // 调用父类构造函数
            inflate(context, R.layout.folder_list_item, this); // 加载布局文件
            mName = (TextView) findViewById(R.id.tv_folder_name); // 初始化文件夹名称文本视图
        }

        /**
         * 绑定文件夹名称到视图。
         *
         * @param name 文件夹名称
         */
        public void bind(String name) {
            mName.setText(name); // 设置文件夹名称文本
        }
    }
}
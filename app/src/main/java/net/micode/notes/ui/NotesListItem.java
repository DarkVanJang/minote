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

import android.content.Context; // 导入Context类
import android.text.format.DateUtils; // 导入日期格式化工具类
import android.view.View; // 导入View类
import android.widget.CheckBox; // 导入CheckBox类
import android.widget.ImageView; // 导入ImageView类
import android.widget.LinearLayout; // 导入LinearLayout类，作为NotesListItem的父类
import android.widget.TextView; // 导入TextView类

import net.micode.notes.R; // 导入资源类
import net.micode.notes.data.Notes; // 导入笔记数据类型定义
import net.micode.notes.tool.DataUtils; // 导入DataUtils工具类
import net.micode.notes.tool.ResourceParser.NoteItemBgResources; // 导入背景资源解析工具类

// NotesListItem类用于显示笔记列表中的每一项
public class NotesListItem extends LinearLayout {

    // UI组件声明
    private ImageView mAlert;       // 提醒图标
    private ImageView mPass;       // 密码图标
    private TextView mTitle;        // 标题文本
    private TextView mTime;         // 时间文本
    private TextView mCallName;     // 来电名称文本
    private NoteItemData mItemData; // 数据模型
    private CheckBox mCheckBox;     // 复选框，用于多选模式

    // 构造函数，初始化UI组件
    public NotesListItem(Context context) {
        super(context); // 调用父类构造函数
        inflate(context, R.layout.note_item, this); // 加载布局文件到当前视图中
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon); // 初始化提醒图标
        mTitle = (TextView) findViewById(R.id.tv_title); // 初始化标题文本
        mTime = (TextView) findViewById(R.id.tv_time); // 初始化时间文本
        mCallName = (TextView) findViewById(R.id.tv_name); // 初始化来电名称文本
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox); // 初始化复选框
        mPass = (ImageView) findViewById(R.id.iv_lock_icon); // 初始化加密图标
    }

    // 绑定数据到视图的方法
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) { // 如果是选择模式并且是普通笔记类型
            mCheckBox.setVisibility(View.VISIBLE); // 显示复选框
            mCheckBox.setChecked(checked); // 设置复选框状态
        } else {
            mCheckBox.setVisibility(View.GONE); // 隐藏复选框
        }

        mItemData = data; // 保存数据引用
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) { // 特殊处理来电记录文件夹
            mCallName.setVisibility(View.GONE); // 隐藏来电名称
            mAlert.setVisibility(View.VISIBLE); // 显示提醒图标
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem); // 设置标题样式
            mTitle.setText(context.getString(R.string.call_record_folder_name) // 设置标题文本
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount()));
            mAlert.setImageResource(R.drawable.call_record); // 设置提醒图标资源
        } else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) { // 特殊处理来电记录子项
            mCallName.setVisibility(View.VISIBLE); // 显示来电名称
            mCallName.setText(data.getCallName()); // 设置来电名称文本
            mTitle.setTextAppearance(context,R.style.TextAppearanceSecondaryItem); // 设置标题样式
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet())); // 设置标题文本
            if (data.hasAlert()) { // 如果有提醒
                mAlert.setImageResource(R.drawable.clock); // 设置提醒图标资源
                mAlert.setVisibility(View.VISIBLE); // 显示提醒图标
            } else {
                mAlert.setVisibility(View.GONE); // 隐藏提醒图标
            }
        } else { // 普通笔记或文件夹的处理
            mCallName.setVisibility(View.GONE); // 隐藏来电名称
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem); // 设置标题样式

            if (data.getType() == Notes.TYPE_FOLDER) { // 如果是文件夹类型
                String text = data.getSnippet() // 设置标题文本
                        + context.getString(R.string.format_folder_files_count,
                        data.getNotesCount());
                if (data.hasPassword()) {
                    text = "文件夹已加密";
                    mPass.setImageResource(R.drawable.ic_lock);
                    mPass.setVisibility(View.VISIBLE);
                }
                mTitle.setText(text);
                mAlert.setVisibility(View.GONE); // 隐藏提醒图标
            } else { // 对于普通笔记
                String text=DataUtils.getFormattedSnippet(data.getSnippet());
                if (data.hasPassword()) {
                    text = "便签已加密";
                    mPass.setImageResource(R.drawable.ic_lock);
                    mPass.setVisibility(View.VISIBLE);
                }
                mTitle.setText(text); // 设置标题文本
                if (data.hasAlert()) { // 如果有提醒
                    mAlert.setImageResource(R.drawable.clock); // 设置提醒图标资源
                    mAlert.setVisibility(View.VISIBLE); // 显示提醒图标
                } else {
                    mAlert.setVisibility(View.GONE); // 隐藏提醒图标
                }
            }
        }
        // 设置时间显示为相对时间格式
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        // 根据数据设置背景
        setBackground(data);
    }

    // 根据笔记数据设置背景资源
    private void setBackground(NoteItemData data) {
        int id = data.getBgColorId(); // 获取背景颜色ID
        if (data.getType() == Notes.TYPE_NOTE) { // 如果是普通笔记类型
            if (data.isSingle() || data.isOneFollowingFolder()) { // 单一或跟随一个文件夹的笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id)); // 设置单一笔记背景
            } else if (data.isLast()) { // 最后一项笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id)); // 设置最后笔记背景
            } else if (data.isFirst() || data.isMultiFollowingFolder()) { // 第一个或跟随多个文件夹的笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id)); // 设置第一个笔记背景
            } else { // 其他笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id)); // 设置正常笔记背景
            }
        } else { // 文件夹类型
            setBackgroundResource(NoteItemBgResources.getFolderBgRes()); // 设置文件夹背景
        }
    }

    // 获取当前绑定的数据
    public NoteItemData getItemData() {
        return mItemData; // 返回当前数据模型
    }
}
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
 *//*
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
import android.app.Activity; // Activity 类
import android.app.AlarmManager; // 闹钟管理类
import android.app.AlertDialog; // 提示对话框类
import android.app.PendingIntent; // 延迟意图类
import android.app.SearchManager; // 搜索管理类
import android.appwidget.AppWidgetManager; // 应用小部件管理类
import android.content.ContentUris; // URI 工具类
import android.content.Context; // 上下文类
import android.content.DialogInterface; // 对话框接口类
import android.content.Intent; // 意图类
import android.content.SharedPreferences; // 共享偏好设置类
import android.graphics.Paint; // 画笔类
import android.os.Bundle; // Bundle 类
import android.preference.PreferenceManager; // 偏好设置管理类
import android.text.Spannable; // 可样式化文本类
import android.text.SpannableString; // 可样式化字符串类
import android.text.TextUtils; // 文本工具类
import android.text.format.DateUtils; // 日期工具类
import android.text.style.BackgroundColorSpan; // 背景颜色样式类
import android.util.Log; // 日志工具类
import android.view.LayoutInflater; // 布局加载类
import android.view.Menu; // 菜单类
import android.view.MenuItem; // 菜单项类
import android.view.MotionEvent; // 触摸事件类
import android.view.View; // 视图类
import android.view.View.OnClickListener; // 点击监听器接口
import android.view.WindowManager; // 窗口管理类
import android.widget.CheckBox; // 复选框类
import android.widget.CompoundButton; // 复合按钮类
import android.widget.CompoundButton.OnCheckedChangeListener; // 复选框状态变化监听器
import android.widget.EditText; // 编辑文本框类
import android.widget.ImageButton;
import android.widget.ImageView; // 图片视图类
import android.widget.LinearLayout; // 线性布局类
import android.widget.TextView; // 文本视图类
import android.widget.Toast; // 提示信息类

import net.micode.notes.R; // 资源文件
import net.micode.notes.data.Notes; // 笔记数据相关类
import net.micode.notes.data.Notes.TextNote; // 笔记文本类
import net.micode.notes.model.WorkingNote; // 工作笔记类
import net.micode.notes.model.WorkingNote.NoteSettingChangedListener; // 笔记设置变化监听器接口
import net.micode.notes.tool.DataUtils; // 数据工具类
import net.micode.notes.tool.ResourceParser; // 资源解析类
import net.micode.notes.tool.ResourceParser.TextAppearanceResources; // 文本外观资源类
import net.micode.notes.ui.DateTimePickerDialog.OnDateTimeSetListener; // 日期时间选择监听器接口
import net.micode.notes.ui.NoteEditText.OnTextViewChangeListener; // 文本视图变化监听器接口
import net.micode.notes.widget.NoteWidgetProvider_2x; // 2x 笔记小部件提供者类
import net.micode.notes.widget.NoteWidgetProvider_4x; // 4x 笔记小部件提供者类

import java.util.HashMap; // HashMap 类
import java.util.HashSet; // HashSet 类
import java.util.Map; // Map 接口
import java.util.Objects;
import java.util.regex.Matcher; // 正则匹配器类
import java.util.regex.Pattern; // 正则表达式类

/**
 * NoteEditActivity 类用于编辑笔记，实现了 OnClickListener、NoteSettingChangedListener 和 OnTextViewChangeListener 接口。
 */
public class NoteEditActivity extends Activity implements OnClickListener,
        NoteSettingChangedListener, OnTextViewChangeListener {
    private static final int REQUEST_CODE_SET_LOCK = 1;
    private static final int REQUEST_CODE_UNLOCK = 2;

    // 头部视图持有者类
    private class HeadViewHolder {
        public TextView tvModified; // 修改日期文本视图
        public ImageView ivAlertIcon; // 提醒图标图片视图
        public TextView tvAlertDate; // 提醒日期文本视图
        public ImageView ibSetBgColor; // 设置背景颜色图片视图
        public ImageButton ibSetLock;
    }

    // 背景选择器按钮与背景颜色 ID 的映射
    private static final Map<Integer, Integer> sBgSelectorBtnsMap = new HashMap<Integer, Integer>();
    static {
        sBgSelectorBtnsMap.put(R.id.iv_bg_yellow, ResourceParser.YELLOW); // 黄色背景
        sBgSelectorBtnsMap.put(R.id.iv_bg_red, ResourceParser.RED); // 红色背景
        sBgSelectorBtnsMap.put(R.id.iv_bg_blue, ResourceParser.BLUE); // 蓝色背景
        sBgSelectorBtnsMap.put(R.id.iv_bg_green, ResourceParser.GREEN); // 绿色背景
        sBgSelectorBtnsMap.put(R.id.iv_bg_white, ResourceParser.WHITE); // 白色背景
    }

    // 背景选择器与选中状态的映射
    private static final Map<Integer, Integer> sBgSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        sBgSelectorSelectionMap.put(ResourceParser.YELLOW, R.id.iv_bg_yellow_select); // 黄色选中状态
        sBgSelectorSelectionMap.put(ResourceParser.RED, R.id.iv_bg_red_select); // 红色选中状态
        sBgSelectorSelectionMap.put(ResourceParser.BLUE, R.id.iv_bg_blue_select); // 蓝色选中状态
        sBgSelectorSelectionMap.put(ResourceParser.GREEN, R.id.iv_bg_green_select); // 绿色选中状态
        sBgSelectorSelectionMap.put(ResourceParser.WHITE, R.id.iv_bg_white_select); // 白色选中状态
    }

    // 字体大小按钮与字体大小 ID 的映射
    private static final Map<Integer, Integer> sFontSizeBtnsMap = new HashMap<Integer, Integer>();
    static {
        sFontSizeBtnsMap.put(R.id.ll_font_large, ResourceParser.TEXT_LARGE); // 大字体
        sFontSizeBtnsMap.put(R.id.ll_font_small, ResourceParser.TEXT_SMALL); // 小字体
        sFontSizeBtnsMap.put(R.id.ll_font_normal, ResourceParser.TEXT_MEDIUM); // 中等字体
        sFontSizeBtnsMap.put(R.id.ll_font_super, ResourceParser.TEXT_SUPER); // 超大字体
    }

    // 字体选择器与选中状态的映射
    private static final Map<Integer, Integer> sFontSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_LARGE, R.id.iv_large_select); // 大字体选中状态
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SMALL, R.id.iv_small_select); // 小字体选中状态
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_MEDIUM, R.id.iv_medium_select); // 中等字体选中状态
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SUPER, R.id.iv_super_select); // 超大字体选中状态
    }

    private static final String TAG = "NoteEditActivity"; // 日志标签

    private HeadViewHolder mNoteHeaderHolder; // 头部视图持有者对象
    private View mHeadViewPanel; // 头部视图面板
    private View mNoteBgColorSelector; // 笔记背景颜色选择器
    private View mFontSizeSelector; // 字体大小选择器
    private EditText mNoteEditor; // 笔记编辑框
    private View mNoteEditorPanel; // 笔记编辑面板
    private WorkingNote mWorkingNote; // 工作笔记对象
    private SharedPreferences mSharedPrefs; // 共享偏好设置对象
    private int mFontSizeId; // 当前字体大小 ID

    private static final String PREFERENCE_FONT_SIZE = "pref_font_size"; // 字体大小偏好设置键

    private static final int SHORTCUT_ICON_TITLE_MAX_LEN = 10; // 快捷图标标题最大长度

    public static final String TAG_CHECKED = String.valueOf('\u221A'); // 选中标记
    public static final String TAG_UNCHECKED = String.valueOf('\u25A1'); // 未选中标记

    private LinearLayout mEditTextList; // 编辑文本框列表
    private String mUserQuery; // 用户查询字符串
    private Pattern mPattern; // 正则表达式模式

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.note_edit); // 设置布局文件

        if (savedInstanceState == null && !initActivityState(getIntent())) { // 初始化活动状态
            finish(); // 结束活动
            return;
        }
        initResources(); // 初始化资源
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SET_LOCK) {
            if (resultCode == RESULT_OK && data != null) {
                String newPassword = data.getStringExtra("NEW_PASSWORD");
                if (newPassword != null) {
                    mWorkingNote.setPassword(newPassword); // 设置新密码
                    Toast.makeText(this, "密码已设置", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else if(requestCode == REQUEST_CODE_UNLOCK){
            if (resultCode != RESULT_OK)
                finish();
        }
    }
    /**
     * 当活动被系统杀死后恢复时调用此方法，用于恢复之前的状态。
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(Intent.EXTRA_UID)) { // 检查是否有保存的状态
            Intent intent = new Intent(Intent.ACTION_VIEW); // 创建意图
            intent.putExtra(Intent.EXTRA_UID, savedInstanceState.getLong(Intent.EXTRA_UID)); // 添加笔记 ID
            if (!initActivityState(intent)) { // 初始化活动状态
                finish(); // 结束活动
                return;
            }
            Log.d(TAG, "Restoring from killed activity"); // 记录日志
        }
    }

    /**
     * 初始化活动状态。
     *
     * @param intent 意图对象
     * @return 是否初始化成功
     */
    private boolean initActivityState(Intent intent) {
        mWorkingNote = null; // 清空工作笔记对象
        if (TextUtils.equals(Intent.ACTION_VIEW, intent.getAction())) { // 如果是查看笔记的意图
            long noteId = intent.getLongExtra(Intent.EXTRA_UID, 0); // 获取笔记 ID
            mUserQuery = ""; // 初始化用户查询字符串

            // 如果是从搜索结果启动
            if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) { // 检查是否有搜索数据
                noteId = Long.parseLong(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY)); // 获取笔记 ID
                mUserQuery = intent.getStringExtra(SearchManager.USER_QUERY); // 获取用户查询字符串
            }

            // 检查笔记是否存在于数据库中
            if (!DataUtils.visibleInNoteDatabase(getContentResolver(), noteId, Notes.TYPE_NOTE)) {
                Intent jump = new Intent(this, NotesListActivity.class); // 跳转到笔记列表活动
                startActivity(jump); // 启动活动
                showToast(R.string.error_note_not_exist); // 显示提示信息
                finish(); // 结束活动
                return false;
            } else {
                mWorkingNote = WorkingNote.load(this, noteId); // 加载工作笔记
                if (mWorkingNote == null) { // 如果加载失败
                    Log.e(TAG, "load note failed with note id" + noteId); // 记录日志
                    finish(); // 结束活动
                    return false;
                }
            }
            getWindow().setSoftInputMode( // 设置软键盘模式
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else if (TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, intent.getAction())) { // 如果是插入或编辑笔记的意图
            // 新建笔记
            long folderId = intent.getLongExtra(Notes.INTENT_EXTRA_FOLDER_ID, 0); // 获取文件夹 ID
            int widgetId = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID); // 获取小部件 ID
            int widgetType = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_TYPE,
                    Notes.TYPE_WIDGET_INVALIDE); // 获取小部件类型
            int bgResId = intent.getIntExtra(Notes.INTENT_EXTRA_BACKGROUND_ID,
                    ResourceParser.getDefaultBgId(this)); // 获取背景资源 ID

            // 解析通话记录笔记
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER); // 获取电话号码
            long callDate = intent.getLongExtra(Notes.INTENT_EXTRA_CALL_DATE, 0); // 获取通话日期
            if (callDate != 0 && phoneNumber != null) { // 如果通话日期和电话号码不为空
                if (TextUtils.isEmpty(phoneNumber)) { // 如果电话号码为空
                    Log.w(TAG, "The call record number is null"); // 记录日志
                }
                long noteId = 0;
                if ((noteId = DataUtils.getNoteIdByPhoneNumberAndCallDate(getContentResolver(),
                        phoneNumber, callDate)) > 0) { // 获取笔记 ID
                    mWorkingNote = WorkingNote.load(this, noteId); // 加载工作笔记
                    if (mWorkingNote == null) { // 如果加载失败
                        Log.e(TAG, "load call note failed with note id" + noteId); // 记录日志
                        finish(); // 结束活动
                        return false;
                    }
                } else {
                    mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId,
                            widgetType, bgResId); // 创建空笔记
                    mWorkingNote.convertToCallNote(phoneNumber, callDate); // 转换为通话记录笔记
                }
            } else {
                mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId, widgetType,
                        bgResId); // 创建空笔记
            }

            getWindow().setSoftInputMode( // 设置软键盘模式
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            Log.e(TAG, "Intent not specified action, should not support"); // 记录日志
            finish(); // 结束活动
            return false;
        }
        mWorkingNote.setOnSettingStatusChangedListener(this); // 设置笔记设置变化监听器
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        initNoteScreen(); // 初始化笔记屏幕
    }

    /**
     * 初始化笔记屏幕。
     */
    private void initNoteScreen() {
        mNoteEditor.setTextAppearance(this, TextAppearanceResources
                .getTexAppearanceResource(mFontSizeId)); // 设置字体大小
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) { // 如果是清单模式
            switchToListMode(mWorkingNote.getContent()); // 切换到清单模式
        } else {
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery)); // 设置笔记内容
            mNoteEditor.setSelection(mNoteEditor.getText().length()); // 设置光标位置
        }
        for (Integer id : sBgSelectorSelectionMap.keySet()) { // 隐藏所有背景选择器的选中状态
            findViewById(sBgSelectorSelectionMap.get(id)).setVisibility(View.GONE);
        }
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId()); // 设置头部背景
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId()); // 设置编辑面板背景

        mNoteHeaderHolder.tvModified.setText(DateUtils.formatDateTime(this,
                mWorkingNote.getModifiedDate(), DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR)); // 设置修改日期

        // TODO: 添加设置提醒的菜单，目前由于 DateTimePicker 未准备好，暂时禁用
        showAlertHeader(); // 显示提醒头部
    }

    /**
     * 显示提醒头部。
     */
    private void showAlertHeader() {
        if (mWorkingNote.hasClockAlert()) { // 如果有提醒
            long time = System.currentTimeMillis(); // 获取当前时间
            if (time > mWorkingNote.getAlertDate()) { // 如果提醒已过期
                mNoteHeaderHolder.tvAlertDate.setText(R.string.note_alert_expired); // 设置提醒过期文本
            } else {
                mNoteHeaderHolder.tvAlertDate.setText(DateUtils.getRelativeTimeSpanString(
                        mWorkingNote.getAlertDate(), time, DateUtils.MINUTE_IN_MILLIS)); // 设置相对时间
            }
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.VISIBLE); // 显示提醒日期
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.VISIBLE); // 显示提醒图标
        } else {
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.GONE); // 隐藏提醒日期
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.GONE); // 隐藏提醒图标
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initActivityState(intent); // 初始化活动状态
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /**
         * For new note without note id, we should firstly save it to
         * generate a id. If the editing note is not worth saving, there
         * is no id which is equivalent to create new note
         */
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }
        outState.putLong(Intent.EXTRA_UID, mWorkingNote.getNoteId());
        Log.d(TAG, "Save working note id: " + mWorkingNote.getNoteId() + " onSaveInstanceState");
    }@Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 如果背景颜色选择器可见且触摸事件不在其范围内，则隐藏背景颜色选择器
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mNoteBgColorSelector, ev)) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        }

        // 如果字体大小选择器可见且触摸事件不在其范围内，则隐藏字体大小选择器
        if (mFontSizeSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mFontSizeSelector, ev)) {
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        // 调用父类的 dispatchTouchEvent 方法处理事件
        return super.dispatchTouchEvent(ev);
    }

    // 判断触摸事件是否在指定视图的范围内
    private boolean inRangeOfView(View view, MotionEvent ev) {
        int[] location = new int[2];
        view.getLocationOnScreen(location); // 获取视图在屏幕上的位置
        int x = location[0];
        int y = location[1];
        // 判断触摸事件的坐标是否在视图范围内
        if (ev.getX() < x
                || ev.getX() > (x + view.getWidth())
                || ev.getY() < y
                || ev.getY() > (y + view.getHeight())) {
            return false; // 不在范围内
        }
        return true; // 在范围内
    }

    // 初始化资源
    private void initResources() {
        mHeadViewPanel = findViewById(R.id.note_title); // 查找标题面板
        mNoteHeaderHolder = new HeadViewHolder(); // 初始化标题视图持有者
        mNoteHeaderHolder.tvModified = (TextView) findViewById(R.id.tv_modified_date); // 查找修改日期文本视图
        mNoteHeaderHolder.ivAlertIcon = (ImageView) findViewById(R.id.iv_alert_icon); // 查找提醒图标视图
        mNoteHeaderHolder.tvAlertDate = (TextView) findViewById(R.id.tv_alert_date); // 查找提醒日期文本视图
        mNoteHeaderHolder.ibSetBgColor = (ImageView) findViewById(R.id.btn_set_bg_color); // 查找设置背景颜色按钮
        mNoteHeaderHolder.ibSetBgColor.setOnClickListener(this); // 设置背景颜色按钮的点击监听器
        mNoteHeaderHolder.ibSetLock = (ImageButton) findViewById(R.id.add_password_btn);//设置密码按钮
        mNoteHeaderHolder.ibSetLock .setOnClickListener(this);// 设置密码按钮监听器
        mNoteEditor = (EditText) findViewById(R.id.note_edit_view); // 查找笔记编辑框
        mNoteEditorPanel = findViewById(R.id.sv_note_edit); // 查找笔记编辑面板
        mNoteBgColorSelector = findViewById(R.id.note_bg_color_selector); // 查找背景颜色选择器
        for (int id : sBgSelectorBtnsMap.keySet()) { // 遍历背景颜色选择器按钮
            ImageView iv = (ImageView) findViewById(id); // 查找按钮视图
            iv.setOnClickListener(this); // 设置按钮的点击监听器
        }

        mFontSizeSelector = findViewById(R.id.font_size_selector); // 查找字体大小选择器
        for (int id : sFontSizeBtnsMap.keySet()) { // 遍历字体大小选择器按钮
            View view = findViewById(id); // 查找按钮视图
            view.setOnClickListener(this); // 设置按钮的点击监听器
        }
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this); // 获取共享偏好设置
        mFontSizeId = mSharedPrefs.getInt(PREFERENCE_FONT_SIZE, ResourceParser.BG_DEFAULT_FONT_SIZE); // 获取字体大小 ID
        /**
         * HACKME: 修复在共享偏好设置中存储资源 ID 的 bug。
         * 如果 ID 大于资源数组的长度，则返回默认字体大小。
         */
        if (mFontSizeId >= TextAppearanceResources.getResourcesSize()) {
            mFontSizeId = ResourceParser.BG_DEFAULT_FONT_SIZE; // 设置为默认字体大小
        }
        mEditTextList = (LinearLayout) findViewById(R.id.note_edit_list); // 查找笔记编辑列表

        if(Objects.equals(getIntent().getStringExtra("REQUEST_CODE"), "OPEN_NOTE") &&mWorkingNote.hasPassword()){
            Intent intent = new Intent(this, PasswordView.class);
            intent.putExtra("ACTION_TYPE", "VERIFY_PASSWORD");
            intent.putExtra("CURRENT_PASSWORD",mWorkingNote.getPassword());
            this.startActivityForResult(intent, REQUEST_CODE_UNLOCK); // 使用合适的请求码
        }
    }

    @Override
    protected void onPause() {
        super.onPause(); // 调用父类的 onPause 方法
        if (saveNote()) { // 如果保存笔记成功
            Log.d(TAG, "Note data was saved with length:" + mWorkingNote.getContent().length()); // 记录日志
        }
        clearSettingState(); // 清除设置状态
    }

    // 更新小部件
    private void updateWidget() {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE); // 创建小部件更新意图
        if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_2X) { // 如果是 2x 小部件
            intent.setClass(this, NoteWidgetProvider_2x.class); // 设置小部件提供者类
        } else if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_4X) { // 如果是 4x 小部件
            intent.setClass(this, NoteWidgetProvider_4x.class); // 设置小部件提供者类
        } else {
            Log.e(TAG, "Unspported widget type"); // 记录错误日志
            return;
        }

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{ // 设置小部件 ID
                mWorkingNote.getWidgetId()
        });

        sendBroadcast(intent); // 发送广播
        setResult(RESULT_OK, intent); // 设置结果
    }

    // 处理点击事件
    public void onClick(View v) {
        int id = v.getId(); // 获取点击视图的 ID
        if (id == R.id.btn_set_bg_color) { // 如果是设置背景颜色按钮
            mNoteBgColorSelector.setVisibility(View.VISIBLE); // 显示背景颜色选择器
            findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                    View.VISIBLE); // 显示当前背景颜色选择项
        }
        else if (id == R.id.add_password_btn) {
            Intent intent = new Intent(this, PasswordView.class);
            intent.putExtra("ACTION_TYPE", "SET_PASSWORD");
            startActivityForResult(intent, REQUEST_CODE_SET_LOCK); // 使用合适的请求码
        }
        else if (sBgSelectorBtnsMap.containsKey(id)) { // 如果是背景颜色选择器按钮
            findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                    View.GONE); // 隐藏当前背景颜色选择项
            mWorkingNote.setBgColorId(sBgSelectorBtnsMap.get(id)); // 设置背景颜色 ID
            mNoteBgColorSelector.setVisibility(View.GONE); // 隐藏背景颜色选择器
        } else if (sFontSizeBtnsMap.containsKey(id)) { // 如果是字体大小选择器按钮
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.GONE); // 隐藏当前字体大小选择项
            mFontSizeId = sFontSizeBtnsMap.get(id); // 设置字体大小 ID
            mSharedPrefs.edit().putInt(PREFERENCE_FONT_SIZE, mFontSizeId).commit(); // 保存字体大小到共享偏好设置
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE); // 显示新字体大小选择项
            if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) { // 如果是清单模式
                getWorkingText(); // 获取工作文本
                switchToListMode(mWorkingNote.getContent()); // 切换到清单模式
            } else {
                mNoteEditor.setTextAppearance(this,
                        TextAppearanceResources.getTexAppearanceResource(mFontSizeId)); // 设置字体外观
            }
            mFontSizeSelector.setVisibility(View.GONE); // 隐藏字体大小选择器
        }
    }

    @Override
    public void onBackPressed() {
        if (clearSettingState()) { // 如果清除设置状态成功
            return;
        }

        saveNote(); // 保存笔记
        super.onBackPressed(); // 调用父类的 onBackPressed 方法
    }

    // 清除设置状态
    private boolean clearSettingState() {
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE) { // 如果背景颜色选择器可见
            mNoteBgColorSelector.setVisibility(View.GONE); // 隐藏背景颜色选择器
            return true;
        } else if (mFontSizeSelector.getVisibility() == View.VISIBLE) { // 如果字体大小选择器可见
            mFontSizeSelector.setVisibility(View.GONE); // 隐藏字体大小选择器
            return true;
        }
        return false; // 未清除设置状态
    }

    // 背景颜色改变时的处理
    public void onBackgroundColorChanged() {
        findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                View.VISIBLE); // 显示当前背景颜色选择项
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId()); // 设置笔记编辑面板的背景资源
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId()); // 设置标题面板的背景资源
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isFinishing()) { // 如果 Activity 正在结束
            return true;
        }
        clearSettingState(); // 清除设置状态
        menu.clear(); // 清除菜单
        if (mWorkingNote.getFolderId() == Notes.ID_CALL_RECORD_FOLDER) { // 如果是通话记录文件夹
            getMenuInflater().inflate(R.menu.call_note_edit, menu); // 加载通话记录笔记编辑菜单
        } else {
            getMenuInflater().inflate(R.menu.note_edit, menu); // 加载笔记编辑菜单
        }
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) { // 如果是清单模式
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_normal_mode); // 设置菜单项标题为“普通模式”
        } else {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_list_mode); // 设置菜单项标题为“清单模式”
        }
        if (mWorkingNote.hasClockAlert()) { // 如果有闹钟提醒
            menu.findItem(R.id.menu_alert).setVisible(false); // 隐藏提醒菜单项
        } else {
            menu.findItem(R.id.menu_delete_remind).setVisible(false); // 隐藏删除提醒菜单项
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) { // 根据菜单项 ID 处理
            case R.id.menu_new_note: // 如果是新建笔记菜单项
                createNewNote(); // 创建新笔记
                break;
            case R.id.menu_delete: // 如果是删除菜单项
                AlertDialog.Builder builder = new AlertDialog.Builder(this); // 创建警告对话框
                builder.setTitle(getString(R.string.alert_title_delete)); // 设置对话框标题
                builder.setIcon(android.R.drawable.ic_dialog_alert); // 设置对话框图标
                builder.setMessage(getString(R.string.alert_message_delete_note)); // 设置对话框消息
                builder.setPositiveButton(android.R.string.ok, // 设置确定按钮
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteCurrentNote(); // 删除当前笔记
                                finish(); // 结束 Activity
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null); // 设置取消按钮
                builder.show(); // 显示对话框
                break;
            case R.id.menu_font_size: // 如果是字体大小菜单项
                mFontSizeSelector.setVisibility(View.VISIBLE); // 显示字体大小选择器
                findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE); // 显示当前字体大小选择项
                break;
            case R.id.menu_list_mode: // 如果是清单模式菜单项
                mWorkingNote.setCheckListMode(mWorkingNote.getCheckListMode() == 0 ?
                        TextNote.MODE_CHECK_LIST : 0); // 切换清单模式
                break;
            case R.id.menu_share: // 如果是分享菜单项
                getWorkingText(); // 获取工作文本
                sendTo(this, mWorkingNote.getContent()); // 分享笔记内容
                break;
            case R.id.menu_send_to_desktop: // 如果是发送到桌面菜单项
                sendToDesktop(); // 发送到桌面
                break;
            case R.id.menu_alert: // 如果是提醒菜单项
                setReminder(); // 设置提醒
                break;
            case R.id.menu_delete_remind: // 如果是删除提醒菜单项
                mWorkingNote.setAlertDate(0, false); // 删除提醒日期
                break;
            default:
                break;
        }
        return true;
    }

    // 设置提醒
    private void setReminder() {
        DateTimePickerDialog d = new DateTimePickerDialog(this, System.currentTimeMillis()); // 创建日期时间选择对话框
        d.setOnDateTimeSetListener(new OnDateTimeSetListener() {
            public void OnDateTimeSet(AlertDialog dialog, long date) {
                mWorkingNote.setAlertDate(date, true); // 设置提醒日期
            }
        });
        d.show(); // 显示对话框
    }

    /**
     * 将笔记分享到支持 {@link Intent#ACTION_SEND} 操作和 {@text/plain} 类型的应用
     */
    private void sendTo(Context context, String info) {
        Intent intent = new Intent(Intent.ACTION_SEND); // 创建分享意图
        intent.putExtra(Intent.EXTRA_TEXT, info); // 设置分享文本
        intent.setType("text/plain"); // 设置分享类型
        context.startActivity(intent); // 启动分享活动
    }

    // 创建新笔记
    private void createNewNote() {
        // 首先保存当前正在编辑的笔记
        saveNote();

        // 为了安全，启动一个新的 NoteEditActivity
        finish(); // 结束当前 Activity
        Intent intent = new Intent(this, NoteEditActivity.class); // 创建 NoteEditActivity 意图
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT); // 设置操作为插入或编辑
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mWorkingNote.getFolderId()); // 设置文件夹 ID
        startActivity(intent); // 启动 Activity
    }

    // 删除当前笔记
    private void deleteCurrentNote() {
        if (mWorkingNote.existInDatabase()) { // 如果笔记存在于数据库中
            HashSet<Long> ids = new HashSet<Long>(); // 创建 ID 集合
            long id = mWorkingNote.getNoteId(); // 获取笔记 ID
            if (id != Notes.ID_ROOT_FOLDER) { // 如果笔记 ID 不是根文件夹
                ids.add(id); // 添加笔记 ID 到集合
            } else {
                Log.d(TAG, "Wrong note id, should not happen"); // 记录日志
            }
            if (!isSyncMode()) { // 如果不是同步模式
                if (!DataUtils.batchDeleteNotes(getContentResolver(), ids)) { // 批量删除笔记
                    Log.e(TAG, "Delete Note error"); // 记录错误日志
                }
            } else {
                if (!DataUtils.batchMoveToFolder(getContentResolver(), ids, Notes.ID_TRASH_FOLER)) { // 批量移动到回收站
                    Log.e(TAG, "Move notes to trash folder error, should not happens"); // 记录错误日志
                }
            }
        }
        mWorkingNote.markDeleted(true); // 标记笔记为已删除
    }

    // 判断是否是同步模式
    private boolean isSyncMode() {
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0; // 判断同步账户名是否为空
    }

    // 闹钟提醒改变时的处理
    public void onClockAlertChanged(long date, boolean set) {
        /**
         * User could set clock to an unsaved note, so before setting the
         * alert clock, we should save the note first
         */
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }
        if (mWorkingNote.getNoteId() > 0) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mWorkingNote.getNoteId()));
            int flags =PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
            AlarmManager alarmManager = ((AlarmManager) getSystemService(ALARM_SERVICE));
            showAlertHeader();
            if(!set) {
                alarmManager.cancel(pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);
            }
        } else {
            /**
             * There is the condition that user has input nothing (the note is
             * not worthy saving), we have no note id, remind the user that he
             * should input something
             */
            Log.e(TAG, "Clock alert setting error");
            showToast(R.string.error_note_empty_for_clock);
        }
    }

    public void onWidgetChanged() {
        updateWidget(); // 调用 updateWidget 方法更新小部件
    }

    public void onEditTextDelete(int index, String text) {
        int childCount = mEditTextList.getChildCount(); // 获取编辑框列表的子视图数量
        if (childCount == 1) { // 如果只有一个子视图
            return; // 直接返回，因为不能删除唯一的编辑框
        }

        // 更新后续编辑框的索引
        for (int i = index + 1; i < childCount; i++) {
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i - 1); // 设置新的索引
        }

        mEditTextList.removeViewAt(index); // 移除指定索引的编辑框
        NoteEditText edit = null; // 初始化 NoteEditText 对象
        if (index == 0) { // 如果删除的是第一个编辑框
            edit = (NoteEditText) mEditTextList.getChildAt(0).findViewById(
                    R.id.et_edit_text); // 获取第一个编辑框
        } else { // 否则
            edit = (NoteEditText) mEditTextList.getChildAt(index - 1).findViewById(
                    R.id.et_edit_text); // 获取前一个编辑框
        }
        int length = edit.length(); // 获取编辑框文本的长度
        edit.append(text); // 将删除的文本追加到前一个编辑框中
        edit.requestFocus(); // 请求焦点
        edit.setSelection(length); // 设置光标位置
    }

    public void onEditTextEnter(int index, String text) {
        // 如果索引超出范围，记录错误日志
        if (index > mEditTextList.getChildCount()) {
            Log.e(TAG, "Index out of mEditTextList boundrary, should not happen");
        }

        View view = getListItem(text, index); // 获取新的列表项视图
        mEditTextList.addView(view, index); // 将视图添加到指定索引位置
        NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text); // 获取编辑框
        edit.requestFocus(); // 请求焦点
        edit.setSelection(0); // 设置光标位置为起始位置
        // 更新后续编辑框的索引
        for (int i = index + 1; i < mEditTextList.getChildCount(); i++) {
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i); // 设置新的索引
        }
    }

    private void switchToListMode(String text) {
        mEditTextList.removeAllViews(); // 移除所有子视图
        String[] items = text.split("\n"); // 将文本按换行符分割成数组
        int index = 0; // 初始化索引
        for (String item : items) { // 遍历数组
            if (!TextUtils.isEmpty(item)) { // 如果项不为空
                mEditTextList.addView(getListItem(item, index)); // 将项添加到列表
                index++; // 增加索引
            }
        }
        mEditTextList.addView(getListItem("", index)); // 添加一个空项
        mEditTextList.getChildAt(index).findViewById(R.id.et_edit_text).requestFocus(); // 请求焦点

        mNoteEditor.setVisibility(View.GONE); // 隐藏普通编辑器
        mEditTextList.setVisibility(View.VISIBLE); // 显示列表模式编辑器
    }

    private Spannable getHighlightQueryResult(String fullText, String userQuery) {
        SpannableString spannable = new SpannableString(fullText == null ? "" : fullText); // 创建 SpannableString
        if (!TextUtils.isEmpty(userQuery)) { // 如果用户查询不为空
            mPattern = Pattern.compile(userQuery); // 编译正则表达式
            Matcher m = mPattern.matcher(fullText); // 创建匹配器
            int start = 0; // 初始化起始位置
            while (m.find(start)) { // 查找匹配项
                spannable.setSpan(
                        new BackgroundColorSpan(this.getResources().getColor(
                                R.color.user_query_highlight)), m.start(), m.end(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE); // 设置背景颜色
                start = m.end(); // 更新起始位置
            }
        }
        return spannable; // 返回 Spannable
    }

    private View getListItem(String item, int index) {
        View view = LayoutInflater.from(this).inflate(R.layout.note_edit_list_item, null); // 加载布局
        final NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text); // 获取编辑框
        edit.setTextAppearance(this, TextAppearanceResources.getTexAppearanceResource(mFontSizeId)); // 设置文本外观
        CheckBox cb = ((CheckBox) view.findViewById(R.id.cb_edit_item)); // 获取复选框
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() { // 设置复选框监听器
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { // 如果复选框被选中
                    edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // 设置删除线
                } else { // 否则
                    edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG); // 清除删除线
                }
            }
        });

        // 如果项以 TAG_CHECKED 开头，设置复选框为选中状态
        if (item.startsWith(TAG_CHECKED)) {
            cb.setChecked(true);
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            item = item.substring(TAG_CHECKED.length(), item.length()).trim(); // 去除标记
        } else if (item.startsWith(TAG_UNCHECKED)) { // 如果项以 TAG_UNCHECKED 开头
            cb.setChecked(false);
            edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            item = item.substring(TAG_UNCHECKED.length(), item.length()).trim(); // 去除标记
        }

        edit.setOnTextViewChangeListener(this); // 设置文本变化监听器
        edit.setIndex(index); // 设置索引
        edit.setText(getHighlightQueryResult(item, mUserQuery)); // 设置文本并高亮查询结果
        return view; // 返回视图
    }

    public void onTextChange(int index, boolean hasText) {
        if (index >= mEditTextList.getChildCount()) { // 如果索引超出范围
            Log.e(TAG, "Wrong index, should not happen"); // 记录错误日志
            return; // 直接返回
        }
        if (hasText) { // 如果有文本
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.VISIBLE); // 显示复选框
        } else { // 否则
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.GONE); // 隐藏复选框
        }
    }

    public void onCheckListModeChanged(int oldMode, int newMode) {
        if (newMode == TextNote.MODE_CHECK_LIST) { // 如果新模式是清单模式
            switchToListMode(mNoteEditor.getText().toString()); // 切换到清单模式
        } else { // 否则
            if (!getWorkingText()) { // 如果获取工作文本失败
                mWorkingNote.setWorkingText(mWorkingNote.getContent().replace(TAG_UNCHECKED + " ",
                        "")); // 替换未选中标记
            }
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery)); // 设置文本并高亮查询结果
            mEditTextList.setVisibility(View.GONE); // 隐藏列表模式编辑器
            mNoteEditor.setVisibility(View.VISIBLE); // 显示普通编辑器
        }
    }

    private boolean getWorkingText() {
        boolean hasChecked = false; // 初始化是否有选中项
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) { // 如果是清单模式
            StringBuilder sb = new StringBuilder(); // 创建 StringBuilder
            for (int i = 0; i < mEditTextList.getChildCount(); i++) { // 遍历子视图
                View view = mEditTextList.getChildAt(i); // 获取子视图
                NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text); // 获取编辑框
                if (!TextUtils.isEmpty(edit.getText())) { // 如果编辑框文本不为空
                    if (((CheckBox) view.findViewById(R.id.cb_edit_item)).isChecked()) { // 如果复选框被选中
                        sb.append(TAG_CHECKED).append(" ").append(edit.getText()).append("\n"); // 添加选中标记和文本
                        hasChecked = true; // 标记有选中项
                    } else { // 否则
                        sb.append(TAG_UNCHECKED).append(" ").append(edit.getText()).append("\n"); // 添加未选中标记和文本
                    }
                }
            }
            mWorkingNote.setWorkingText(sb.toString()); // 设置工作文本
        } else { // 否则
            mWorkingNote.setWorkingText(mNoteEditor.getText().toString()); // 设置工作文本为编辑器内容
        }
        return hasChecked; // 返回是否有选中项
    }

    private boolean saveNote() {
        getWorkingText(); // 获取工作文本
        boolean saved = mWorkingNote.saveNote(); // 保存笔记
        if (saved) { // 如果保存成功
            /**
             * 从列表视图切换到编辑视图有两种模式：打开一个笔记或创建/编辑一个笔记。
             * 打开笔记时需要返回到列表视图中的原始位置，而创建新笔记时需要返回到列表顶部。
             * 这里使用 {@link #RESULT_OK} 来区分创建/编辑状态。
             */
            setResult(RESULT_OK); // 设置结果
        }
        return saved; // 返回保存结果
    }

    private void sendToDesktop() {
        /**
         * 在发送消息到桌面之前，需要确保当前编辑的笔记存在于数据库中。
         * 因此，对于新笔记，首先保存它。
         */
        if (!mWorkingNote.existInDatabase()) { // 如果笔记不存在于数据库
            saveNote(); // 保存笔记
        }

        if (mWorkingNote.getNoteId() > 0) { // 如果笔记 ID 有效
            Intent sender = new Intent(); // 创建 Intent
            Intent shortcutIntent = new Intent(this, NoteEditActivity.class); // 创建快捷方式 Intent
            shortcutIntent.setAction(Intent.ACTION_VIEW); // 设置动作
            shortcutIntent.putExtra(Intent.EXTRA_UID, mWorkingNote.getNoteId()); // 添加笔记 ID
            sender.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent); // 添加快捷方式 Intent
            sender.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                    makeShortcutIconTitle(mWorkingNote.getContent())); // 添加快捷方式名称
            sender.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.icon_app)); // 添加快捷方式图标
            sender.putExtra("duplicate", true); // 允许重复
            sender.setAction("com.android.launcher.action.INSTALL_SHORTCUT"); // 设置动作为安装快捷方式
            showToast(R.string.info_note_enter_desktop); // 显示提示信息
            sendBroadcast(sender); // 发送广播
        } else { // 否则
            /**
             * 如果用户没有输入任何内容（笔记不值得保存），则没有笔记 ID，
             * 提醒用户需要输入一些内容。
             */
            Log.e(TAG, "Send to desktop error"); // 记录错误日志
            showToast(R.string.error_note_empty_for_send_to_desktop); // 显示错误提示
        }
    }

    private String makeShortcutIconTitle(String content) {
        content = content.replace(TAG_CHECKED, ""); // 去除选中标记
        content = content.replace(TAG_UNCHECKED, ""); // 去除未选中标记
        return content.length() > SHORTCUT_ICON_TITLE_MAX_LEN ? content.substring(0,
                SHORTCUT_ICON_TITLE_MAX_LEN) : content; // 截取最大长度
    }

    private void showToast(int resId) {
        showToast(resId, Toast.LENGTH_SHORT); // 显示短时 Toast
    }

    private void showToast(int resId, int duration) {
        Toast.makeText(this, resId, duration).show(); // 显示指定时长的 Toast
    }
    }

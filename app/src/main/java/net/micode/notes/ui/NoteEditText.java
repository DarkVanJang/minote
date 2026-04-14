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
import android.graphics.Rect; // 矩形类
import android.text.Layout; // 文本布局类
import android.text.Selection; // 文本选择类
import android.text.Spanned; // 带样式的文本类
import android.text.TextUtils; // 文本工具类
import android.text.style.URLSpan; // URL 样式类
import android.util.AttributeSet; // 属性集类
import android.util.Log; // 日志工具类
import android.view.ContextMenu; // 上下文菜单类
import android.view.KeyEvent; // 按键事件类
import android.view.MenuItem; // 菜单项类
import android.view.MenuItem.OnMenuItemClickListener; // 菜单项点击监听器
import android.view.MotionEvent; // 触摸事件类
import android.widget.EditText; // 编辑文本框类

import net.micode.notes.R; // 资源文件

import java.util.HashMap; // HashMap 类
import java.util.Map; // Map 接口

/**
 * NoteEditText 类是一个自定义的 EditText，用于处理笔记编辑中的特殊逻辑。
 */
public class NoteEditText extends EditText {
    private static final String TAG = "NoteEditText"; // 日志标签
    private int mIndex; // 当前编辑框的索引
    private int mSelectionStartBeforeDelete; // 删除操作前的光标起始位置

    // 定义 URL 的协议
    private static final String SCHEME_TEL = "tel:"; // 电话协议
    private static final String SCHEME_HTTP = "http:"; // HTTP 协议
    private static final String SCHEME_EMAIL = "mailto:"; // 邮件协议

    // 定义协议与资源 ID 的映射
    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel); // 电话协议对应的资源 ID
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web); // HTTP 协议对应的资源 ID
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email); // 邮件协议对应的资源 ID
    }

    /**
     * OnTextViewChangeListener 接口用于处理编辑框的删除、回车和文本变化事件。
     */
    public interface OnTextViewChangeListener {
        /**
         * 当按下删除键且文本为空时，删除当前编辑框。
         *
         * @param index 当前编辑框的索引
         * @param text  当前编辑框的文本
         */
        void onEditTextDelete(int index, String text);

        /**
         * 当按下回车键时，在当前编辑框后添加一个新的编辑框。
         *
         * @param index 当前编辑框的索引
         * @param text  当前编辑框的文本
         */
        void onEditTextEnter(int index, String text);

        /**
         * 当文本变化时，隐藏或显示选项菜单。
         *
         * @param index    当前编辑框的索引
         * @param hasText  当前编辑框是否有文本
         */
        void onTextChange(int index, boolean hasText);
    }

    private OnTextViewChangeListener mOnTextViewChangeListener; // 文本变化监听器

    /**
     * 构造函数，初始化 NoteEditText。
     *
     * @param context 上下文对象
     */
    public NoteEditText(Context context) {
        super(context, null); // 调用父类构造函数
        mIndex = 0; // 初始化索引为 0
    }

    /**
     * 设置当前编辑框的索引。
     *
     * @param index 编辑框的索引
     */
    public void setIndex(int index) {
        mIndex = index; // 设置索引
    }

    /**
     * 设置文本变化监听器。
     *
     * @param listener 文本变化监听器
     */
    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener; // 设置监听器
    }

    /**
     * 构造函数，初始化 NoteEditText。
     *
     * @param context 上下文对象
     * @param attrs   属性集
     */
    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle); // 调用父类构造函数
    }

    /**
     * 构造函数，初始化 NoteEditText。
     *
     * @param context  上下文对象
     * @param attrs    属性集
     * @param defStyle 默认样式
     */
    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); // 调用父类构造函数
        // TODO Auto-generated constructor stub
    }

    /**
     * 处理触摸事件。
     *
     * @param event 触摸事件
     * @return 是否处理了事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) { // 根据事件类型处理
            case MotionEvent.ACTION_DOWN: // 按下事件
                int x = (int) event.getX(); // 获取触摸点的 X 坐标
                int y = (int) event.getY(); // 获取触摸点的 Y 坐标
                x -= getTotalPaddingLeft(); // 减去左侧内边距
                y -= getTotalPaddingTop(); // 减去顶部内边距
                x += getScrollX(); // 加上水平滚动距离
                y += getScrollY(); // 加上垂直滚动距离

                Layout layout = getLayout(); // 获取文本布局
                int line = layout.getLineForVertical(y); // 获取垂直方向的行号
                int off = layout.getOffsetForHorizontal(line, x); // 获取水平方向的偏移量
                Selection.setSelection(getText(), off); // 设置光标位置
                break;
        }

        return super.onTouchEvent(event); // 调用父类方法处理事件
    }

    /**
     * 处理按键按下事件。
     *
     * @param keyCode 按键码
     * @param event   按键事件
     * @return 是否处理了事件
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) { // 根据按键码处理
            case KeyEvent.KEYCODE_ENTER: // 回车键
                if (mOnTextViewChangeListener != null) { // 如果监听器不为空
                    return false; // 不处理事件
                }
                break;
            case KeyEvent.KEYCODE_DEL: // 删除键
                mSelectionStartBeforeDelete = getSelectionStart(); // 记录删除前的光标起始位置
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event); // 调用父类方法处理事件
    }

    /**
     * 处理按键抬起事件。
     *
     * @param keyCode 按键码
     * @param event   按键事件
     * @return 是否处理了事件
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) { // 根据按键码处理
            case KeyEvent.KEYCODE_DEL: // 删除键
                if (mOnTextViewChangeListener != null) { // 如果监听器不为空
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) { // 如果光标在起始位置且不是第一个编辑框
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString()); // 触发删除事件
                        return true; // 处理事件
                    }
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted"); // 记录日志
                }
                break;
            case KeyEvent.KEYCODE_ENTER: // 回车键
                if (mOnTextViewChangeListener != null) { // 如果监听器不为空
                    int selectionStart = getSelectionStart(); // 获取光标起始位置
                    String text = getText().subSequence(selectionStart, length()).toString(); // 获取光标后的文本
                    setText(getText().subSequence(0, selectionStart)); // 设置光标前的文本
                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text); // 触发回车事件
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted"); // 记录日志
                }
                break;
            default:
                break;
        }
        return super.onKeyUp(keyCode, event); // 调用父类方法处理事件
    }

    /**
     * 处理焦点变化事件。
     *
     * @param focused               是否获得焦点
     * @param direction             焦点方向
     * @param previouslyFocusedRect 之前获得焦点的矩形区域
     */
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mOnTextViewChangeListener != null) { // 如果监听器不为空
            if (!focused && TextUtils.isEmpty(getText())) { // 如果失去焦点且文本为空
                mOnTextViewChangeListener.onTextChange(mIndex, false); // 触发文本变化事件
            } else {
                mOnTextViewChangeListener.onTextChange(mIndex, true); // 触发文本变化事件
            }
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect); // 调用父类方法处理事件
    }

    /**
     * 创建上下文菜单。
     *
     * @param menu 上下文菜单
     */
    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        if (getText() instanceof Spanned) { // 如果文本是带样式的文本
            int selStart = getSelectionStart(); // 获取光标起始位置
            int selEnd = getSelectionEnd(); // 获取光标结束位置

            int min = Math.min(selStart, selEnd); // 获取最小位置
            int max = Math.max(selStart, selEnd); // 获取最大位置

            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class); // 获取 URL 样式
            if (urls.length == 1) { // 如果只有一个 URL
                int defaultResId = 0; // 默认资源 ID
                for (String schema : sSchemaActionResMap.keySet()) { // 遍历协议映射
                    if (urls[0].getURL().indexOf(schema) >= 0) { // 如果 URL 包含协议
                        defaultResId = sSchemaActionResMap.get(schema); // 获取资源 ID
                        break;
                    }
                }

                if (defaultResId == 0) { // 如果未找到匹配的协议
                    defaultResId = R.string.note_link_other; // 使用默认资源 ID
                }

                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener( // 添加菜单项
                        new OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) { // 菜单项点击事件
                                urls[0].onClick(NoteEditText.this); // 触发 URL 点击事件
                                return true; // 处理事件
                            }
                        });
            }
        }
        super.onCreateContextMenu(menu); // 调用父类方法创建菜单
    }
}
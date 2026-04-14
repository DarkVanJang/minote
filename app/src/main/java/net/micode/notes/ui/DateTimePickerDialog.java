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

package net.micode.notes.ui;

import java.util.Calendar;

import net.micode.notes.R;
import net.micode.notes.ui.DateTimePicker;
import net.micode.notes.ui.DateTimePicker.OnDateTimeChangedListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

/**
 * DateTimePickerDialog 类，用于显示日期时间选择对话框
 */
public class DateTimePickerDialog extends AlertDialog implements OnClickListener {

    private Calendar mDate = Calendar.getInstance(); // 日历对象，用于存储选择的日期时间
    private boolean mIs24HourView; // 是否使用 24 小时制视图
    private OnDateTimeSetListener mOnDateTimeSetListener; // 日期时间设置监听器
    private DateTimePicker mDateTimePicker; // 日期时间选择器控件

    /**
     * 定义日期时间设置监听器的接口
     */
    public interface OnDateTimeSetListener {
        /**
         * 当用户确认选择日期时间时调用
         * @param dialog 当前对话框
         * @param date 选择的日期时间（毫秒数）
         */
        void OnDateTimeSet(AlertDialog dialog, long date);
    }

    /**
     * 构造函数，初始化日期时间选择对话框
     * @param context 上下文
     * @param date 初始日期时间（毫秒数）
     */
    public DateTimePickerDialog(Context context, long date) {
        super(context); // 调用父类构造函数
        mDateTimePicker = new DateTimePicker(context); // 创建日期时间选择器实例
        setView(mDateTimePicker); // 将选择器设置为对话框视图
        // 设置日期时间变化监听器
        mDateTimePicker.setOnDateTimeChangedListener(new OnDateTimeChangedListener() {
            public void onDateTimeChanged(DateTimePicker view, int year, int month,
                                          int dayOfMonth, int hourOfDay, int minute) { // 当日期时间改变时触发
                mDate.set(Calendar.YEAR, year); // 设置年份
                mDate.set(Calendar.MONTH, month); // 设置月份
                mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth); // 设置日期
                mDate.set(Calendar.HOUR_OF_DAY, hourOfDay); // 设置小时
                mDate.set(Calendar.MINUTE, minute); // 设置分钟
                updateTitle(mDate.getTimeInMillis()); // 更新对话框标题
            }
        });
        mDate.setTimeInMillis(date); // 设置初始时间
        mDate.set(Calendar.SECOND, 0); // 将秒数置为 0
        mDateTimePicker.setCurrentDate(mDate.getTimeInMillis()); // 设置选择器的当前日期时间
        setButton(context.getString(R.string.datetime_dialog_ok), this); // 设置“确定”按钮
        setButton2(context.getString(R.string.datetime_dialog_cancel), (OnClickListener)null); // 设置“取消”按钮
        set24HourView(DateFormat.is24HourFormat(this.getContext())); // 根据系统设置确定是否为 24 小时制
        updateTitle(mDate.getTimeInMillis()); // 初始化标题
    }

    /**
     * 设置是否使用 24 小时制视图
     * @param is24HourView true 为 24 小时制，false 为 12 小时制
     */
    public void set24HourView(boolean is24HourView) {
        mIs24HourView = is24HourView; // 更新 24 小时制标志
    }

    /**
     * 设置日期时间选择的回调监听器
     * @param callBack 监听器实例
     */
    public void setOnDateTimeSetListener(OnDateTimeSetListener callBack) {
        mOnDateTimeSetListener = callBack; // 保存监听器引用
    }

    /**
     * 更新对话框标题，显示当前日期时间
     * @param date 当前日期时间（毫秒数）
     */
    private void updateTitle(long date) {
        int flag = // 定义日期时间格式标志
                DateUtils.FORMAT_SHOW_YEAR | // 显示年份
                        DateUtils.FORMAT_SHOW_DATE | // 显示日期
                        DateUtils.FORMAT_SHOW_TIME;  // 显示时间
        flag |= mIs24HourView ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_24HOUR; // 根据设置选择 24 小时制
        setTitle(DateUtils.formatDateTime(this.getContext(), date, flag)); // 设置格式化后的标题
    }

    /**
     * 处理按钮点击事件（实现 OnClickListener 接口）
     * @param arg0 对话框接口
     * @param arg1 点击的按钮 ID
     */
    public void onClick(DialogInterface arg0, int arg1) {
        if (mOnDateTimeSetListener != null) { // 如果监听器存在
            mOnDateTimeSetListener.OnDateTimeSet(this, mDate.getTimeInMillis()); // 回调监听器，传递当前时间
        }
    }
}
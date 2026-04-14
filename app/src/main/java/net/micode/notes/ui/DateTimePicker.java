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

import java.text.DateFormatSymbols;
import java.util.Calendar;

import net.micode.notes.R;


import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

public class DateTimePicker extends FrameLayout {

    private static final boolean DEFAULT_ENABLE_STATE = true;

    private static final int HOURS_IN_HALF_DAY = 12;
    private static final int HOURS_IN_ALL_DAY = 24;
    private static final int DAYS_IN_ALL_WEEK = 7;
    private static final int DATE_SPINNER_MIN_VAL = 0;
    private static final int DATE_SPINNER_MAX_VAL = DAYS_IN_ALL_WEEK - 1;
    private static final int HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW = 0;
    private static final int HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW = 23;
    private static final int HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW = 1;
    private static final int HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW = 12;
    private static final int MINUT_SPINNER_MIN_VAL = 0;
    private static final int MINUT_SPINNER_MAX_VAL = 59;
    private static final int AMPM_SPINNER_MIN_VAL = 0;
    private static final int AMPM_SPINNER_MAX_VAL = 1;

    private final NumberPicker mDateSpinner;
    private final NumberPicker mHourSpinner;
    private final NumberPicker mMinuteSpinner;
    private final NumberPicker mAmPmSpinner;
    private Calendar mDate;

    private String[] mDateDisplayValues = new String[DAYS_IN_ALL_WEEK];

    private boolean mIsAm;

    private boolean mIs24HourView;

    private boolean mIsEnabled = DEFAULT_ENABLE_STATE;

    private boolean mInitialising;

    private OnDateTimeChangedListener mOnDateTimeChangedListener;

    private NumberPicker.OnValueChangeListener mOnDateChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            mDate.add(Calendar.DAY_OF_YEAR, newVal - oldVal);
            updateDateControl();
            onDateTimeChanged();
        }
    };

    /**
     * 小时选择器的值变更监听器，用于处理小时变化时的逻辑
     */
    private NumberPicker.OnValueChangeListener mOnHourChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) { // 当小时值变化时触发
            boolean isDateChanged = false; // 标记日期是否因小时变化而调整
            Calendar cal = Calendar.getInstance(); // 创建日历实例用于日期计算

            if (!mIs24HourView) { // 如果使用 12 小时制视图
                // 处理从 11 PM 到 12 AM（跨天前进）和从 12 AM 到 11 PM（跨天后退）
                if (!mIsAm && oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY) { // 从 11 PM 到 12 AM
                    cal.setTimeInMillis(mDate.getTimeInMillis()); // 设置当前时间
                    cal.add(Calendar.DAY_OF_YEAR, 1); // 日期加 1 天
                    isDateChanged = true; // 标记日期已改变
                } else if (mIsAm && oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) { // 从 12 AM 到 11 PM
                    cal.setTimeInMillis(mDate.getTimeInMillis()); // 设置当前时间
                    cal.add(Calendar.DAY_OF_YEAR, -1); // 日期减 1 天
                    isDateChanged = true; // 标记日期已改变
                }
                // 处理 AM/PM 切换
                if (oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY || // 从 11 到 12
                        oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) { // 从 12 到 11
                    mIsAm = !mIsAm; // 切换 AM/PM 状态
                    updateAmPmControl(); // 更新 AM/PM 显示控件
                }
            } else { // 如果使用 24 小时制视图
                // 处理从 23 到 0（跨天前进）和从 0 到 23（跨天后退）
                if (oldVal == HOURS_IN_ALL_DAY - 1 && newVal == 0) { // 从 23 到 0
                    cal.setTimeInMillis(mDate.getTimeInMillis()); // 设置当前时间
                    cal.add(Calendar.DAY_OF_YEAR, 1); // 日期加 1 天
                    isDateChanged = true; // 标记日期已改变
                } else if (oldVal == 0 && newVal == HOURS_IN_ALL_DAY - 1) { // 从 0 到 23
                    cal.setTimeInMillis(mDate.getTimeInMillis()); // 设置当前时间
                    cal.add(Calendar.DAY_OF_YEAR, -1); // 日期减 1 天
                    isDateChanged = true; // 标记日期已改变
                }
            }

            // 计算并设置新的小时值（转换为 24 小时制）
            int newHour = mHourSpinner.getValue() % HOURS_IN_HALF_DAY + (mIsAm ? 0 : HOURS_IN_HALF_DAY);
            mDate.set(Calendar.HOUR_OF_DAY, newHour); // 更新日期对象的小时
            onDateTimeChanged(); // 触发日期时间变化事件

            if (isDateChanged) { // 如果日期发生变化
                setCurrentYear(cal.get(Calendar.YEAR)); // 更新年份
                setCurrentMonth(cal.get(Calendar.MONTH)); // 更新月份
                setCurrentDay(cal.get(Calendar.DAY_OF_MONTH)); // 更新日期
            }
        }
    };

    /**
     * 分钟选择器的值变更监听器，用于处理分钟变化时的逻辑
     */
    private NumberPicker.OnValueChangeListener mOnMinuteChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) { // 当分钟值变化时触发
            int minValue = mMinuteSpinner.getMinValue(); // 获取分钟选择器的最小值
            int maxValue = mMinuteSpinner.getMaxValue(); // 获取分钟选择器的最大值
            int offset = 0; // 小时偏移量，用于跨小时调整
            if (oldVal == maxValue && newVal == minValue) { // 从最大值（59）到最小值（0）
                offset += 1; // 小时加 1
            } else if (oldVal == minValue && newVal == maxValue) { // 从最小值（0）到最大值（59）
                offset -= 1; // 小时减 1
            }
            if (offset != 0) { // 如果小时发生变化
                mDate.add(Calendar.HOUR_OF_DAY, offset); // 调整日期对象的小时
                mHourSpinner.setValue(getCurrentHour()); // 更新小时选择器的值
                updateDateControl(); // 更新日期控件
                int newHour = getCurrentHourOfDay(); // 获取当前 24 小时制的小时
                if (newHour >= HOURS_IN_HALF_DAY) { // 如果小时超过 12（下午）
                    mIsAm = false; // 设置为 PM
                    updateAmPmControl(); // 更新 AM/PM 显示
                } else { // 如果小时小于 12（上午）
                    mIsAm = true; // 设置为 AM
                    updateAmPmControl(); // 更新 AM/PM 显示
                }
            }
            mDate.set(Calendar.MINUTE, newVal); // 设置分钟值
            onDateTimeChanged(); // 触发日期时间变化事件
        }
    };

    /**
     * AM/PM 选择器的值变更监听器，用于处理 AM/PM 切换时的逻辑
     */
    private NumberPicker.OnValueChangeListener mOnAmPmChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) { // 当 AM/PM 值变化时触发
            mIsAm = !mIsAm; // 切换 AM/PM 状态
            if (mIsAm) { // 如果切换到 AM
                mDate.add(Calendar.HOUR_OF_DAY, -HOURS_IN_HALF_DAY); // 小时减去半天（12小时）
            } else { // 如果切换到 PM
                mDate.add(Calendar.HOUR_OF_DAY, HOURS_IN_HALF_DAY); // 小时加上半天（12小时）
            }
            updateAmPmControl(); // 更新 AM/PM 显示
            onDateTimeChanged(); // 触发日期时间变化事件
        }
    };

    /**
     * 定义日期时间变化监听器的接口
     */
    public interface OnDateTimeChangedListener {
        /**
         * 当日期时间发生变化时调用
         * @param view 当前 DateTimePicker 视图
         * @param year 年份
         * @param month 月份（0-11）
         * @param dayOfMonth 日期
         * @param hourOfDay 小时（24小时制）
         * @param minute 分钟
         */
        void onDateTimeChanged(DateTimePicker view, int year, int month,
                               int dayOfMonth, int hourOfDay, int minute);
    }

    /**
     * 构造函数，初始化日期时间选择器，默认使用当前时间
     * @param context 上下文
     */
    public DateTimePicker(Context context) {
        this(context, System.currentTimeMillis()); // 调用带时间的构造函数，使用当前时间
    }

    /**
     * 构造函数，初始化日期时间选择器
     * @param context 上下文
     * @param date 初始日期时间（毫秒数）
     */
    public DateTimePicker(Context context, long date) {
        this(context, date, DateFormat.is24HourFormat(context)); // 调用完整构造函数，检测系统是否为 24 小时制
    }

    public DateTimePicker(Context context, long date, boolean is24HourView) {
        super(context);
        mDate = Calendar.getInstance();
        mInitialising = true;
        mIsAm = getCurrentHourOfDay() >= HOURS_IN_HALF_DAY;
        inflate(context, R.layout.datetime_picker, this);

        mDateSpinner = (NumberPicker) findViewById(R.id.date);
        mDateSpinner.setMinValue(DATE_SPINNER_MIN_VAL);
        mDateSpinner.setMaxValue(DATE_SPINNER_MAX_VAL);
        mDateSpinner.setOnValueChangedListener(mOnDateChangedListener);

        mHourSpinner = (NumberPicker) findViewById(R.id.hour);
        mHourSpinner.setOnValueChangedListener(mOnHourChangedListener);
        mMinuteSpinner =  (NumberPicker) findViewById(R.id.minute);
        mMinuteSpinner.setMinValue(MINUT_SPINNER_MIN_VAL);
        mMinuteSpinner.setMaxValue(MINUT_SPINNER_MAX_VAL);
        mMinuteSpinner.setOnLongPressUpdateInterval(100);
        mMinuteSpinner.setOnValueChangedListener(mOnMinuteChangedListener);

        String[] stringsForAmPm = new DateFormatSymbols().getAmPmStrings();
        mAmPmSpinner = (NumberPicker) findViewById(R.id.amPm);
        mAmPmSpinner.setMinValue(AMPM_SPINNER_MIN_VAL);
        mAmPmSpinner.setMaxValue(AMPM_SPINNER_MAX_VAL);
        mAmPmSpinner.setDisplayedValues(stringsForAmPm);
        mAmPmSpinner.setOnValueChangedListener(mOnAmPmChangedListener);

        // update controls to initial state
        updateDateControl();
        updateHourControl();
        updateAmPmControl();

        set24HourView(is24HourView);

        // set to current time
        setCurrentDate(date);

        setEnabled(isEnabled());

        // set the content descriptions
        mInitialising = false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }
        super.setEnabled(enabled);
        mDateSpinner.setEnabled(enabled);
        mMinuteSpinner.setEnabled(enabled);
        mHourSpinner.setEnabled(enabled);
        mAmPmSpinner.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * Get the current date in millis
     *
     * @return the current date in millis
     */
    public long getCurrentDateInTimeMillis() {
        return mDate.getTimeInMillis();
    }

    /**
     * Set the current date
     *
     * @param date The current date in millis
     */
    public void setCurrentDate(long date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        setCurrentDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    /**
     * Set the current date
     *
     * @param year The current year
     * @param month The current month
     * @param dayOfMonth The current dayOfMonth
     * @param hourOfDay The current hourOfDay
     * @param minute The current minute
     */
    public void setCurrentDate(int year, int month,
            int dayOfMonth, int hourOfDay, int minute) {
        setCurrentYear(year);
        setCurrentMonth(month);
        setCurrentDay(dayOfMonth);
        setCurrentHour(hourOfDay);
        setCurrentMinute(minute);
    }

    /**
     * Get current year
     *
     * @return The current year
     */
    public int getCurrentYear() {
        return mDate.get(Calendar.YEAR);
    }

    /**
     * Set current year
     *
     * @param year The current year
     */
    public void setCurrentYear(int year) {
        if (!mInitialising && year == getCurrentYear()) {
            return;
        }
        mDate.set(Calendar.YEAR, year);
        updateDateControl();
        onDateTimeChanged();
    }

    /**
     * Get current month in the year
     *
     * @return The current month in the year
     */
    public int getCurrentMonth() {
        return mDate.get(Calendar.MONTH);
    }

    /**
     * Set current month in the year
     *
     * @param month The month in the year
     */
    public void setCurrentMonth(int month) {
        if (!mInitialising && month == getCurrentMonth()) {
            return;
        }
        mDate.set(Calendar.MONTH, month);
        updateDateControl();
        onDateTimeChanged();
    }

    /**
     * Get current day of the month
     *
     * @return The day of the month
     */
    public int getCurrentDay() {
        return mDate.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Set current day of the month
     *
     * @param dayOfMonth The day of the month
     */
    public void setCurrentDay(int dayOfMonth) {
        if (!mInitialising && dayOfMonth == getCurrentDay()) {
            return;
        }
        mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateDateControl();
        onDateTimeChanged();
    }

    /**
     * Get current hour in 24 hour mode, in the range (0~23)
     * @return The current hour in 24 hour mode
     */
    public int getCurrentHourOfDay() {
        return mDate.get(Calendar.HOUR_OF_DAY);
    }

    private int getCurrentHour() {
        if (mIs24HourView){
            return getCurrentHourOfDay();
        } else {
            int hour = getCurrentHourOfDay();
            if (hour > HOURS_IN_HALF_DAY) {
                return hour - HOURS_IN_HALF_DAY;
            } else {
                return hour == 0 ? HOURS_IN_HALF_DAY : hour;
            }
        }
    }

    /**
     * Set current hour in 24 hour mode, in the range (0~23)
     *
     * @param hourOfDay
     */
    public void setCurrentHour(int hourOfDay) {
        if (!mInitialising && hourOfDay == getCurrentHourOfDay()) {
            return;
        }
        mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        if (!mIs24HourView) {
            if (hourOfDay >= HOURS_IN_HALF_DAY) {
                mIsAm = false;
                if (hourOfDay > HOURS_IN_HALF_DAY) {
                    hourOfDay -= HOURS_IN_HALF_DAY;
                }
            } else {
                mIsAm = true;
                if (hourOfDay == 0) {
                    hourOfDay = HOURS_IN_HALF_DAY;
                }
            }
            updateAmPmControl();
        }
        mHourSpinner.setValue(hourOfDay);
        onDateTimeChanged();
    }

    /**
     * Get currentMinute
     *
     * @return The Current Minute
     */
    public int getCurrentMinute() {
        return mDate.get(Calendar.MINUTE);
    }

    /**
     * Set current minute
     */
    public void setCurrentMinute(int minute) {
        if (!mInitialising && minute == getCurrentMinute()) {
            return;
        }
        mMinuteSpinner.setValue(minute);
        mDate.set(Calendar.MINUTE, minute);
        onDateTimeChanged();
    }

    /**
     * @return true if this is in 24 hour view else false.
     */
    public boolean is24HourView () {
        return mIs24HourView;
    }

    /**
     * Set whether in 24 hour or AM/PM mode.
     *
     * @param is24HourView True for 24 hour mode. False for AM/PM mode.
     */
    public void set24HourView(boolean is24HourView) {
        if (mIs24HourView == is24HourView) {
            return;
        }
        mIs24HourView = is24HourView;
        mAmPmSpinner.setVisibility(is24HourView ? View.GONE : View.VISIBLE);
        int hour = getCurrentHourOfDay();
        updateHourControl();
        setCurrentHour(hour);
        updateAmPmControl();
    }

    private void updateDateControl() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mDate.getTimeInMillis());
        cal.add(Calendar.DAY_OF_YEAR, -DAYS_IN_ALL_WEEK / 2 - 1);
        mDateSpinner.setDisplayedValues(null);
        for (int i = 0; i < DAYS_IN_ALL_WEEK; ++i) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            mDateDisplayValues[i] = (String) DateFormat.format("MM.dd EEEE", cal);
        }
        mDateSpinner.setDisplayedValues(mDateDisplayValues);
        mDateSpinner.setValue(DAYS_IN_ALL_WEEK / 2);
        mDateSpinner.invalidate();
    }

    /**
     * 更新 AM/PM 控件的显示状态
     */
    private void updateAmPmControl() {
        if (mIs24HourView) { // 如果使用 24 小时制视图
            mAmPmSpinner.setVisibility(View.GONE); // 隐藏 AM/PM 选择器
        } else { // 如果使用 12 小时制视图
            int index = mIsAm ? Calendar.AM : Calendar.PM; // 根据当前 AM/PM 状态设置索引（0 为 AM，1 为 PM）
            mAmPmSpinner.setValue(index); // 设置 AM/PM 选择器的值
            mAmPmSpinner.setVisibility(View.VISIBLE); // 显示 AM/PM 选择器
        }
    }

    /**
     * 更新小时控件的取值范围
     */
    private void updateHourControl() {
        if (mIs24HourView) { // 如果使用 24 小时制视图
            mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW); // 设置小时选择器的最小值（通常为 0）
            mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW); // 设置小时选择器的最大值（通常为 23）
        } else { // 如果使用 12 小时制视图
            mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW); // 设置小时选择器的最小值（通常为 1）
            mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW); // 设置小时选择器的最大值（通常为 12）
        }
    }

    /**
     * 设置日期时间变化的回调监听器
     * @param callback 监听器实例，如果为 null 则不执行任何操作
     */
    public void setOnDateTimeChangedListener(OnDateTimeChangedListener callback) {
        mOnDateTimeChangedListener = callback; // 保存回调监听器引用
    }

    /**
     * 触发日期时间变化事件
     */
    private void onDateTimeChanged() {
        if (mOnDateTimeChangedListener != null) { // 如果监听器存在
            // 调用监听器的回调方法，传递当前日期时间信息
            mOnDateTimeChangedListener.onDateTimeChanged(this, getCurrentYear(),
                    getCurrentMonth(), getCurrentDay(), getCurrentHourOfDay(), getCurrentMinute());
        }
    }
}

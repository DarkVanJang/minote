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
import android.preference.PreferenceManager; // 偏好设置管理类

import net.micode.notes.R; // 资源文件
import net.micode.notes.ui.NotesPreferenceActivity; // 笔记偏好设置活动类

/**
 * ResourceParser 类用于解析和管理笔记的背景、字体大小等资源。
 */
public class ResourceParser {

    // 定义背景颜色的常量
    public static final int YELLOW = 0; // 黄色
    public static final int BLUE = 1; // 蓝色
    public static final int WHITE = 2; // 白色
    public static final int GREEN = 3; // 绿色
    public static final int RED = 4; // 红色

    public static final int BG_DEFAULT_COLOR = YELLOW; // 默认背景颜色为黄色

    // 定义字体大小的常量
    public static final int TEXT_SMALL = 0; // 小字体
    public static final int TEXT_MEDIUM = 1; // 中等字体
    public static final int TEXT_LARGE = 2; // 大字体
    public static final int TEXT_SUPER = 3; // 超大字体

    public static final int BG_DEFAULT_FONT_SIZE = TEXT_MEDIUM; // 默认字体大小为中等

    /**
     * NoteBgResources 类用于管理笔记编辑界面的背景资源。
     */
    public static class NoteBgResources {
        // 定义笔记编辑界面的背景资源数组
        private final static int[] BG_EDIT_RESOURCES = new int[]{
                R.drawable.edit_yellow, // 黄色背景
                R.drawable.edit_blue, // 蓝色背景
                R.drawable.edit_white, // 白色背景
                R.drawable.edit_green, // 绿色背景
                R.drawable.edit_red // 红色背景
        };

        // 定义笔记编辑界面的标题背景资源数组
        private final static int[] BG_EDIT_TITLE_RESOURCES = new int[]{
                R.drawable.edit_title_yellow, // 黄色标题背景
                R.drawable.edit_title_blue, // 蓝色标题背景
                R.drawable.edit_title_white, // 白色标题背景
                R.drawable.edit_title_green, // 绿色标题背景
                R.drawable.edit_title_red // 红色标题背景
        };

        /**
         * 获取笔记编辑界面的背景资源。
         *
         * @param id 背景资源的索引
         * @return 背景资源 ID
         */
        public static int getNoteBgResource(int id) {
            return BG_EDIT_RESOURCES[id]; // 返回指定索引的背景资源
        }

        /**
         * 获取笔记编辑界面的标题背景资源。
         *
         * @param id 标题背景资源的索引
         * @return 标题背景资源 ID
         */
        public static int getNoteTitleBgResource(int id) {
            return BG_EDIT_TITLE_RESOURCES[id]; // 返回指定索引的标题背景资源
        }
    }

    /**
     * 获取默认背景资源的索引。
     *
     * @param context 上下文对象
     * @return 背景资源的索引
     */
    public static int getDefaultBgId(Context context) {
        // 检查偏好设置中是否启用了随机背景颜色
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                NotesPreferenceActivity.PREFERENCE_SET_BG_COLOR_KEY, false)) {
            return (int) (Math.random() * NoteBgResources.BG_EDIT_RESOURCES.length); // 返回随机背景颜色索引
        } else {
            return BG_DEFAULT_COLOR; // 返回默认背景颜色索引
        }
    }

    /**
     * NoteItemBgResources 类用于管理笔记列表项的背景资源。
     */
    public static class NoteItemBgResources {
        // 定义笔记列表项的第一项背景资源数组
        private final static int[] BG_FIRST_RESOURCES = new int[]{
                R.drawable.list_yellow_up, // 黄色第一项背景
                R.drawable.list_blue_up, // 蓝色第一项背景
                R.drawable.list_white_up, // 白色第一项背景
                R.drawable.list_green_up, // 绿色第一项背景
                R.drawable.list_red_up // 红色第一项背景
        };

        // 定义笔记列表项的普通项背景资源数组
        private final static int[] BG_NORMAL_RESOURCES = new int[]{
                R.drawable.list_yellow_middle, // 黄色普通项背景
                R.drawable.list_blue_middle, // 蓝色普通项背景
                R.drawable.list_white_middle, // 白色普通项背景
                R.drawable.list_green_middle, // 绿色普通项背景
                R.drawable.list_red_middle // 红色普通项背景
        };

        // 定义笔记列表项的最后一项背景资源数组
        private final static int[] BG_LAST_RESOURCES = new int[]{
                R.drawable.list_yellow_down, // 黄色最后一项背景
                R.drawable.list_blue_down, // 蓝色最后一项背景
                R.drawable.list_white_down, // 白色最后一项背景
                R.drawable.list_green_down, // 绿色最后一项背景
                R.drawable.list_red_down, // 红色最后一项背景
        };

        // 定义笔记列表项的单一项背景资源数组
        private final static int[] BG_SINGLE_RESOURCES = new int[]{
                R.drawable.list_yellow_single, // 黄色单一项背景
                R.drawable.list_blue_single, // 蓝色单一项背景
                R.drawable.list_white_single, // 白色单一项背景
                R.drawable.list_green_single, // 绿色单一项背景
                R.drawable.list_red_single // 红色单一项背景
        };

        /**
         * 获取笔记列表项的第一项背景资源。
         *
         * @param id 背景资源的索引
         * @return 背景资源 ID
         */
        public static int getNoteBgFirstRes(int id) {
            return BG_FIRST_RESOURCES[id]; // 返回指定索引的第一项背景资源
        }

        /**
         * 获取笔记列表项的最后一项背景资源。
         *
         * @param id 背景资源的索引
         * @return 背景资源 ID
         */
        public static int getNoteBgLastRes(int id) {
            return BG_LAST_RESOURCES[id]; // 返回指定索引的最后一项背景资源
        }

        /**
         * 获取笔记列表项的单一项背景资源。
         *
         * @param id 背景资源的索引
         * @return 背景资源 ID
         */
        public static int getNoteBgSingleRes(int id) {
            return BG_SINGLE_RESOURCES[id]; // 返回指定索引的单一项背景资源
        }

        /**
         * 获取笔记列表项的普通项背景资源。
         *
         * @param id 背景资源的索引
         * @return 背景资源 ID
         */
        public static int getNoteBgNormalRes(int id) {
            return BG_NORMAL_RESOURCES[id]; // 返回指定索引的普通项背景资源
        }

        /**
         * 获取文件夹的背景资源。
         *
         * @return 文件夹背景资源 ID
         */
        public static int getFolderBgRes() {
            return R.drawable.list_folder; // 返回文件夹背景资源
        }
    }

    /**
     * WidgetBgResources 类用于管理小部件的背景资源。
     */
    public static class WidgetBgResources {
        // 定义 2x 小部件的背景资源数组
        private final static int[] BG_2X_RESOURCES = new int[]{
                R.drawable.widget_2x_yellow, // 黄色 2x 小部件背景
                R.drawable.widget_2x_blue, // 蓝色 2x 小部件背景
                R.drawable.widget_2x_white, // 白色 2x 小部件背景
                R.drawable.widget_2x_green, // 绿色 2x 小部件背景
                R.drawable.widget_2x_red, // 红色 2x 小部件背景
        };

        /**
         * 获取 2x 小部件的背景资源。
         *
         * @param id 背景资源的索引
         * @return 背景资源 ID
         */
        public static int getWidget2xBgResource(int id) {
            return BG_2X_RESOURCES[id]; // 返回指定索引的 2x 小部件背景资源
        }

        // 定义 4x 小部件的背景资源数组
        private final static int[] BG_4X_RESOURCES = new int[]{
                R.drawable.widget_4x_yellow, // 黄色 4x 小部件背景
                R.drawable.widget_4x_blue, // 蓝色 4x 小部件背景
                R.drawable.widget_4x_white, // 白色 4x 小部件背景
                R.drawable.widget_4x_green, // 绿色 4x 小部件背景
                R.drawable.widget_4x_red // 红色 4x 小部件背景
        };

        /**
         * 获取 4x 小部件的背景资源。
         *
         * @param id 背景资源的索引
         * @return 背景资源 ID
         */
        public static int getWidget4xBgResource(int id) {
            return BG_4X_RESOURCES[id]; // 返回指定索引的 4x 小部件背景资源
        }
    }

    /**
     * TextAppearanceResources 类用于管理文本外观资源。
     */
    public static class TextAppearanceResources {
        // 定义文本外观资源数组
        private final static int[] TEXTAPPEARANCE_RESOURCES = new int[]{
                R.style.TextAppearanceNormal, // 正常文本外观
                R.style.TextAppearanceMedium, // 中等文本外观
                R.style.TextAppearanceLarge, // 大文本外观
                R.style.TextAppearanceSuper // 超大文本外观
        };

        /**
         * 获取文本外观资源。
         *
         * @param id 文本外观资源的索引
         * @return 文本外观资源 ID
         */
        public static int getTexAppearanceResource(int id) {
            /**
             * HACKME: 修复在共享偏好设置中存储资源 ID 的 bug。
             * 如果 ID 大于资源数组的长度，则返回默认字体大小。
             */
            if (id >= TEXTAPPEARANCE_RESOURCES.length) {
                return BG_DEFAULT_FONT_SIZE; // 返回默认字体大小
            }
            return TEXTAPPEARANCE_RESOURCES[id]; // 返回指定索引的文本外观资源
        }

        /**
         * 获取文本外观资源的数量。
         *
         * @return 文本外观资源的数量
         */
        public static int getResourcesSize() {
            return TEXTAPPEARANCE_RESOURCES.length; // 返回文本外观资源的数量
        }
    }
}
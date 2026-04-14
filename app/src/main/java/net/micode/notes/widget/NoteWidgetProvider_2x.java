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

package net.micode.notes.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.ResourceParser;

// 名为 NoteWidgetProvider_2x 的小部件提供者类，它扩展自 NoteWidgetProvider
public class NoteWidgetProvider_2x extends NoteWidgetProvider {
    @Override
    // 调用父类的 update 方法，传递上下文、应用小部件管理器和小部件 ID 数组
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.update(context, appWidgetManager, appWidgetIds);
    }

    @Override
    // 用于返回小部件的布局资源索引
    // 返回定义为 widget_2x 的布局资源，表示该小部件使用的 XML 布局文件
    protected int getLayoutId() {
        return R.layout.widget_2x;
    }

    @Override
    // 返回小部件的背景索引
    protected int getBgResourceId(int bgId) {
        return ResourceParser.WidgetBgResources.getWidget2xBgResource(bgId);
    }

    @Override
    // 用于返回小部件的类型
    // 返回常量 TYPE_WIDGET_2X，表示这是一个 2x 类型的小部件
    protected int getWidgetType() {
        return Notes.TYPE_WIDGET_2X;
    }
}

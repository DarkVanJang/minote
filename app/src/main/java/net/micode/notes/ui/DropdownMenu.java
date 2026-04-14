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

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import net.micode.notes.R;

/**
 * DropdownMenu 类，用于创建和管理下拉菜单
 */
public class DropdownMenu {
    private Button mButton; // 下拉菜单的按钮
    private PopupMenu mPopupMenu; // 下拉菜单的弹出窗口
    private Menu mMenu; // 下拉菜单的菜单对象

    /**
     * 构造函数，初始化下拉菜单
     * @param context 上下文，用于创建 PopupMenu
     * @param button 用于触发下拉菜单的按钮
     * @param menuId 菜单资源的 ID，用于填充菜单项
     */
    public DropdownMenu(Context context, Button button, int menuId) {
        mButton = button; // 保存按钮引用
        mButton.setBackgroundResource(R.drawable.dropdown_icon); // 设置按钮背景为下拉图标
        mPopupMenu = new PopupMenu(context, mButton); // 创建 PopupMenu，绑定到按钮
        mMenu = mPopupMenu.getMenu(); // 获取 PopupMenu 的菜单对象
        mPopupMenu.getMenuInflater().inflate(menuId, mMenu); // 从资源文件中加载菜单项
        mButton.setOnClickListener(new OnClickListener() { // 为按钮设置点击监听器
            public void onClick(View v) { // 点击事件处理
                mPopupMenu.show(); // 显示下拉菜单
            }
        });
    }

    /**
     * 设置下拉菜单项的点击监听器
     * @param listener 菜单项点击监听器
     */
    public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
        if (mPopupMenu != null) { // 检查 PopupMenu 是否存在
            mPopupMenu.setOnMenuItemClickListener(listener); // 设置菜单项点击监听器
        }
    }

    /**
     * 根据 ID 查找菜单项
     * @param id 菜单项的 ID
     * @return 对应的 MenuItem 对象，未找到返回 null
     */
    public MenuItem findItem(int id) {
        return mMenu.findItem(id); // 在菜单中查找指定 ID 的菜单项
    }

    /**
     * 设置按钮的标题文字
     * @param title 标题文字
     */
    public void setTitle(CharSequence title) {
        mButton.setText(title); // 设置按钮的显示文本
    }
}

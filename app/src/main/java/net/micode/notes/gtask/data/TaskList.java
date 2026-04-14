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

package net.micode.notes.gtask.data;

import android.database.Cursor;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class TaskList extends Node {
    private static final String TAG = TaskList.class.getSimpleName();

    private int mIndex;

    private ArrayList<Task> mChildren;

    public TaskList() {
        super();
        mChildren = new ArrayList<Task>();
        mIndex = 1;
    }

    public JSONObject getCreateAction(int actionId) {
        JSONObject js = new JSONObject();

        try {
            // action_type
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_CREATE);

            // action_id
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);

            // index
            js.put(GTaskStringUtils.GTASK_JSON_INDEX, mIndex);

            // entity_delta
            JSONObject entity = new JSONObject();
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            entity.put(GTaskStringUtils.GTASK_JSON_CREATOR_ID, "null");
            entity.put(GTaskStringUtils.GTASK_JSON_ENTITY_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_GROUP);
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate tasklist-create jsonobject");
        }

        return js;
    }

    public JSONObject getUpdateAction(int actionId) {
        JSONObject js = new JSONObject();

        try {
            // action_type
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_UPDATE);

            // action_id
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);

            // id
            js.put(GTaskStringUtils.GTASK_JSON_ID, getGid());

            // entity_delta
            JSONObject entity = new JSONObject();
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            entity.put(GTaskStringUtils.GTASK_JSON_DELETED, getDeleted());
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate tasklist-update jsonobject");
        }

        return js;
    }

    public void setContentByRemoteJSON(JSONObject js) {
        if (js != null) {
            try {
                // id
                if (js.has(GTaskStringUtils.GTASK_JSON_ID)) {
                    setGid(js.getString(GTaskStringUtils.GTASK_JSON_ID));
                }

                // last_modified
                if (js.has(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)) {
                    setLastModified(js.getLong(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED));
                }

                // name
                if (js.has(GTaskStringUtils.GTASK_JSON_NAME)) {
                    setName(js.getString(GTaskStringUtils.GTASK_JSON_NAME));
                }

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("fail to get tasklist content from jsonobject");
            }
        }
    }

    public void setContentByLocalJSON(JSONObject js) {
        if (js == null || !js.has(GTaskStringUtils.META_HEAD_NOTE)) {
            Log.w(TAG, "setContentByLocalJSON: nothing is avaiable");
        }

        try {
            JSONObject folder = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);

            if (folder.getInt(NoteColumns.TYPE) == Notes.TYPE_FOLDER) {
                String name = folder.getString(NoteColumns.SNIPPET);
                setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + name);
            } else if (folder.getInt(NoteColumns.TYPE) == Notes.TYPE_SYSTEM) {
                if (folder.getLong(NoteColumns.ID) == Notes.ID_ROOT_FOLDER)
                    setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT);
                else if (folder.getLong(NoteColumns.ID) == Notes.ID_CALL_RECORD_FOLDER)
                    setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX
                            + GTaskStringUtils.FOLDER_CALL_NOTE);
                else
                    Log.e(TAG, "invalid system folder");
            } else {
                Log.e(TAG, "error type");
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public JSONObject getLocalJSONFromContent() {
        try {
            JSONObject js = new JSONObject();
            JSONObject folder = new JSONObject();

            String folderName = getName();
            if (getName().startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX))
                folderName = folderName.substring(GTaskStringUtils.MIUI_FOLDER_PREFFIX.length(),
                        folderName.length());
            folder.put(NoteColumns.SNIPPET, folderName);
            if (folderName.equals(GTaskStringUtils.FOLDER_DEFAULT)
                    || folderName.equals(GTaskStringUtils.FOLDER_CALL_NOTE))
                folder.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM);
            else
                folder.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);

            js.put(GTaskStringUtils.META_HEAD_NOTE, folder);

            return js;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取同步操作类型
     * @param c 数据库游标，提供本地数据信息
     * @return 同步操作类型常量值
     */
    public int getSyncAction(Cursor c) {
        try {
            if (c.getInt(SqlNote.LOCAL_MODIFIED_COLUMN) == 0) { // 检查本地是否修改
                // 本地无修改
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) { // 比较本地同步时间与远程最后修改时间
                    // 本地和远程均无更新
                    return SYNC_ACTION_NONE; // 无需同步
                } else {
                    // 本地无更新，需要应用远程修改到本地
                    return SYNC_ACTION_UPDATE_LOCAL; // 更新本地
                }
            } else {
                // 本地有修改
                if (!c.getString(SqlNote.GTASK_ID_COLUMN).equals(getGid())) { // 验证 Google Task ID 是否匹配
                    Log.e(TAG, "gtask id doesn't match"); // ID 不匹配，记录错误日志
                    return SYNC_ACTION_ERROR; // 返回错误状态
                }
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) { // 比较同步时间与最后修改时间
                    // 仅本地有修改
                    return SYNC_ACTION_UPDATE_REMOTE; // 更新远程
                } else {
                    // 本地和远程均有修改，此处针对文件夹冲突，默认应用本地修改
                    return SYNC_ACTION_UPDATE_REMOTE; // 更新远程
                }
            }
        } catch (Exception e) { // 捕获异常
            Log.e(TAG, e.toString()); // 记录错误日志
            e.printStackTrace(); // 打印堆栈信息
        }

        return SYNC_ACTION_ERROR; // 默认返回错误状态
    }

    /**
     * 获取子任务数量
     * @return 子任务列表的大小
     */
    public int getChildTaskCount() {
        return mChildren.size(); // 返回子任务列表的元素个数
    }

    /**
     * 添加子任务到列表末尾
     * @param task 要添加的子任务
     * @return 是否成功添加
     */
    public boolean addChildTask(Task task) {
        boolean ret = false; // 初始化返回值为 false
        if (task != null && !mChildren.contains(task)) { // 检查任务非空且不在列表中
            ret = mChildren.add(task); // 尝试添加任务到列表末尾
            if (ret) { // 如果添加成功
                // 设置前一个兄弟任务和父任务
                task.setPriorSibling(mChildren.isEmpty() ? null : mChildren
                        .get(mChildren.size() - 1)); // 如果列表为空则无前兄弟，否则取最后一个任务
                task.setParent(this); // 设置当前对象为父任务
            }
        }
        return ret; // 返回添加是否成功
    }

    /**
     * 在指定位置添加子任务
     * @param task 要添加的子任务
     * @param index 插入位置索引
     * @return 是否成功添加
     */
    public boolean addChildTask(Task task, int index) {
        if (index < 0 || index > mChildren.size()) { // 检查索引是否有效
            Log.e(TAG, "add child task: invalid index"); // 无效索引，记录错误日志
            return false; // 返回失败
        }

        int pos = mChildren.indexOf(task); // 检查任务是否已存在
        if (task != null && pos == -1) { // 任务非空且不在列表中
            mChildren.add(index, task); // 在指定位置插入任务

            // 更新任务列表的前后关系
            Task preTask = null; // 前一个任务
            Task afterTask = null; // 后一个任务
            if (index != 0) // 如果不是插入在开头
                preTask = mChildren.get(index - 1); // 获取前一个任务
            if (index != mChildren.size() - 1) // 如果不是插入在末尾
                afterTask = mChildren.get(index + 1); // 获取后一个任务

            task.setPriorSibling(preTask); // 设置前一个兄弟任务
            if (afterTask != null) // 如果后一个任务存在
                afterTask.setPriorSibling(task); // 更新后一个任务的前兄弟
        }

        return true; // 返回成功
    }

    /**
     * 移除指定子任务
     * @param task 要移除的子任务
     * @return 是否成功移除
     */
    public boolean removeChildTask(Task task) {
        boolean ret = false; // 初始化返回值为 false
        int index = mChildren.indexOf(task); // 查找任务在列表中的位置
        if (index != -1) { // 如果任务存在
            ret = mChildren.remove(task); // 移除任务

            if (ret) { // 如果移除成功
                // 重置任务的前兄弟和父任务
                task.setPriorSibling(null); // 清空前兄弟
                task.setParent(null); // 清空父任务

                // 更新任务列表的前后关系
                if (index != mChildren.size()) { // 如果移除的不是最后一个任务
                    mChildren.get(index).setPriorSibling(
                            index == 0 ? null : mChildren.get(index - 1)); // 更新下一个任务的前兄弟
                }
            }
        }
        return ret; // 返回移除是否成功
    }

    /**
     * 移动子任务到指定位置
     * @param task 要移动的子任务
     * @param index 目标位置索引
     * @return 是否成功移动
     */
    public boolean moveChildTask(Task task, int index) {
        if (index < 0 || index >= mChildren.size()) { // 检查索引是否有效
            Log.e(TAG, "move child task: invalid index"); // 无效索引，记录错误日志
            return false; // 返回失败
        }

        int pos = mChildren.indexOf(task); // 查找任务当前位置
        if (pos == -1) { // 如果任务不在列表中
            Log.e(TAG, "move child task: the task should in the list"); // 记录错误日志
            return false; // 返回失败
        }

        if (pos == index) // 如果目标位置与当前位置相同
            return true; // 直接返回成功
        return (removeChildTask(task) && addChildTask(task, index)); // 先移除再插入，返回操作结果
    }

    /**
     * 根据 Google Task ID 查找子任务
     * @param gid Google Task ID
     * @return 匹配的子任务对象，未找到返回 null
     */
    public Task findChildTaskByGid(String gid) {
        for (int i = 0; i < mChildren.size(); i++) { // 遍历子任务列表
            Task t = mChildren.get(i); // 获取当前任务
            if (t.getGid().equals(gid)) { // 比较 Google Task ID
                return t; // 找到匹配任务，返回
            }
        }
        return null; // 未找到，返回 null
    }

    /**
     * 获取子任务在列表中的索引
     * @param task 要查找的子任务
     * @return 子任务的索引，未找到返回 -1
     */
    public int getChildTaskIndex(Task task) {
        return mChildren.indexOf(task); // 返回任务在列表中的位置
    }

    /**
     * 根据索引获取子任务
     * @param index 子任务索引
     * @return 指定索引处的子任务对象，无效索引返回 null
     */
    public Task getChildTaskByIndex(int index) {
        if (index < 0 || index >= mChildren.size()) { // 检查索引是否有效
            Log.e(TAG, "getTaskByIndex: invalid index"); // 无效索引，记录错误日志
            return null; // 返回 null
        }
        return mChildren.get(index); // 返回指定位置的任务
    }

    /**
     * 根据 Google Task ID 获取子任务
     * @param gid Google Task ID
     * @return 匹配的子任务对象，未找到返回 null
     */
    public Task getChilTaskByGid(String gid) {
        for (Task task : mChildren) { // 遍历子任务列表
            if (task.getGid().equals(gid)) // 比较 Google Task ID
                return task; // 找到匹配任务，返回
        }
        return null; // 未找到，返回 null
    }

    public ArrayList<Task> getChildTaskList() {
        return this.mChildren;
    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

    public int getIndex() {
        return this.mIndex;
    }
}

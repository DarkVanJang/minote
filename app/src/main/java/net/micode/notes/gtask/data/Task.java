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
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Task extends Node {
    private static final String TAG = Task.class.getSimpleName();

    private boolean mCompleted;

    private String mNotes;

    private JSONObject mMetaInfo;

    private Task mPriorSibling;

    private TaskList mParent;

    public Task() {
        super();
        mCompleted = false;
        mNotes = null;
        mPriorSibling = null;
        mParent = null;
        mMetaInfo = null;
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
            js.put(GTaskStringUtils.GTASK_JSON_INDEX, mParent.getChildTaskIndex(this));

            // entity_delta
            JSONObject entity = new JSONObject();
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            entity.put(GTaskStringUtils.GTASK_JSON_CREATOR_ID, "null");
            entity.put(GTaskStringUtils.GTASK_JSON_ENTITY_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_TASK);
            if (getNotes() != null) {
                entity.put(GTaskStringUtils.GTASK_JSON_NOTES, getNotes());
            }
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

            // parent_id
            js.put(GTaskStringUtils.GTASK_JSON_PARENT_ID, mParent.getGid());

            // dest_parent_type
            js.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_GROUP);

            // list_id
            js.put(GTaskStringUtils.GTASK_JSON_LIST_ID, mParent.getGid());

            // prior_sibling_id
            if (mPriorSibling != null) {
                js.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, mPriorSibling.getGid());
            }

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate task-create jsonobject");
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
            if (getNotes() != null) {
                entity.put(GTaskStringUtils.GTASK_JSON_NOTES, getNotes());
            }
            entity.put(GTaskStringUtils.GTASK_JSON_DELETED, getDeleted());
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate task-update jsonobject");
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

                // notes
                if (js.has(GTaskStringUtils.GTASK_JSON_NOTES)) {
                    setNotes(js.getString(GTaskStringUtils.GTASK_JSON_NOTES));
                }

                // deleted
                if (js.has(GTaskStringUtils.GTASK_JSON_DELETED)) {
                    setDeleted(js.getBoolean(GTaskStringUtils.GTASK_JSON_DELETED));
                }

                // completed
                if (js.has(GTaskStringUtils.GTASK_JSON_COMPLETED)) {
                    setCompleted(js.getBoolean(GTaskStringUtils.GTASK_JSON_COMPLETED));
                }
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("fail to get task content from jsonobject");
            }
        }
    }

    /**
     * 通过本地 JSON 数据设置内容
     * @param js 本地 JSON 对象
     */
    public void setContentByLocalJSON(JSONObject js) {
        // 检查输入的 JSON 对象是否有效
        if (js == null || !js.has(GTaskStringUtils.META_HEAD_NOTE)
                || !js.has(GTaskStringUtils.META_HEAD_DATA)) {
            Log.w(TAG, "setContentByLocalJSON: nothing is avaiable"); // 如果 JSON 为空或缺少必要字段，记录警告日志
        }

        try {
            // 从 JSON 中提取笔记信息和数据数组
            JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE); // 获取笔记元信息
            JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA); // 获取数据数组

            // 检查笔记类型是否为普通笔记
            if (note.getInt(NoteColumns.TYPE) != Notes.TYPE_NOTE) {
                Log.e(TAG, "invalid type"); // 如果类型不是普通笔记，记录错误日志
                return; // 直接返回，不继续处理
            }

            // 遍历数据数组，寻找 MIME 类型为笔记的数据项
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject data = dataArray.getJSONObject(i); // 获取当前数据对象
                if (TextUtils.equals(data.getString(DataColumns.MIME_TYPE), DataConstants.NOTE)) { // 如果 MIME 类型匹配
                    setName(data.getString(DataColumns.CONTENT)); // 将数据内容设置为笔记名称
                    break; // 找到匹配项后跳出循环
                }
            }

        } catch (JSONException e) { // 捕获 JSON 解析异常
            Log.e(TAG, e.toString()); // 记录错误日志
            e.printStackTrace(); // 打印异常堆栈
        }
    }

    /**
     * 从内容生成本地 JSON 对象
     * @return 表示笔记内容的 JSON 对象，如果出错则返回 null
     */
    public JSONObject getLocalJSONFromContent() {
        String name = getName(); // 获取笔记名称
        try {
            if (mMetaInfo == null) { // 如果元信息为空，表示可能是从网络新建的任务
                // 新建任务的情况
                if (name == null) { // 如果名称为空
                    Log.w(TAG, "the note seems to be an empty one"); // 记录警告日志
                    return null; // 返回 null
                }

                JSONObject js = new JSONObject(); // 创建新的 JSON 对象
                JSONObject note = new JSONObject(); // 创建笔记 JSON 对象
                JSONArray dataArray = new JSONArray(); // 创建数据数组
                JSONObject data = new JSONObject(); // 创建单个数据对象
                data.put(DataColumns.CONTENT, name); // 将名称设置为数据内容
                dataArray.put(data); // 将数据对象添加到数组
                js.put(GTaskStringUtils.META_HEAD_DATA, dataArray); // 将数据数组添加到主 JSON
                note.put(NoteColumns.TYPE, Notes.TYPE_NOTE); // 设置笔记类型为普通笔记
                js.put(GTaskStringUtils.META_HEAD_NOTE, note); // 将笔记对象添加到主 JSON
                return js; // 返回构造好的 JSON 对象
            } else { // 如果元信息存在，表示已同步的任务
                // 已同步任务的情况
                JSONObject note = mMetaInfo.getJSONObject(GTaskStringUtils.META_HEAD_NOTE); // 获取笔记元信息
                JSONArray dataArray = mMetaInfo.getJSONArray(GTaskStringUtils.META_HEAD_DATA); // 获取数据数组

                for (int i = 0; i < dataArray.length(); i++) { // 遍历数据数组
                    JSONObject data = dataArray.getJSONObject(i); // 获取单个数据对象
                    if (TextUtils.equals(data.getString(DataColumns.MIME_TYPE), DataConstants.NOTE)) { // 如果 MIME 类型为笔记
                        data.put(DataColumns.CONTENT, getName()); // 更新内容为当前名称
                        break; // 找到并更新后跳出循环
                    }
                }

                note.put(NoteColumns.TYPE, Notes.TYPE_NOTE); // 设置笔记类型为普通笔记
                return mMetaInfo; // 返回更新后的元信息 JSON 对象
            }
        } catch (JSONException e) { // 捕获 JSON 解析或操作异常
            Log.e(TAG, e.toString()); // 记录错误日志
            e.printStackTrace(); // 打印异常堆栈
            return null; // 返回 null 表示失败
        }
    }

    public void setMetaInfo(MetaData metaData) {
        if (metaData != null && metaData.getNotes() != null) {
            try {
                mMetaInfo = new JSONObject(metaData.getNotes());
            } catch (JSONException e) {
                Log.w(TAG, e.toString());
                mMetaInfo = null;
            }
        }
    }

    public int getSyncAction(Cursor c) {
        try {
            JSONObject noteInfo = null;
            if (mMetaInfo != null && mMetaInfo.has(GTaskStringUtils.META_HEAD_NOTE)) {
                noteInfo = mMetaInfo.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
            }

            if (noteInfo == null) {
                Log.w(TAG, "it seems that note meta has been deleted");
                return SYNC_ACTION_UPDATE_REMOTE;
            }

            if (!noteInfo.has(NoteColumns.ID)) {
                Log.w(TAG, "remote note id seems to be deleted");
                return SYNC_ACTION_UPDATE_LOCAL;
            }

            // validate the note id now
            if (c.getLong(SqlNote.ID_COLUMN) != noteInfo.getLong(NoteColumns.ID)) {
                Log.w(TAG, "note id doesn't match");
                return SYNC_ACTION_UPDATE_LOCAL;
            }

            if (c.getInt(SqlNote.LOCAL_MODIFIED_COLUMN) == 0) {
                // there is no local update
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // no update both side
                    return SYNC_ACTION_NONE;
                } else {
                    // apply remote to local
                    return SYNC_ACTION_UPDATE_LOCAL;
                }
            } else {
                // validate gtask id
                if (!c.getString(SqlNote.GTASK_ID_COLUMN).equals(getGid())) {
                    Log.e(TAG, "gtask id doesn't match");
                    return SYNC_ACTION_ERROR;
                }
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // local modification only
                    return SYNC_ACTION_UPDATE_REMOTE;
                } else {
                    return SYNC_ACTION_UPDATE_CONFLICT;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

        return SYNC_ACTION_ERROR;
    }

    public boolean isWorthSaving() {
        return mMetaInfo != null || (getName() != null && getName().trim().length() > 0)
                || (getNotes() != null && getNotes().trim().length() > 0);
    }

    public void setCompleted(boolean completed) {
        this.mCompleted = completed;
    }

    public void setNotes(String notes) {
        this.mNotes = notes;
    }

    public void setPriorSibling(Task priorSibling) {
        this.mPriorSibling = priorSibling;
    }

    public void setParent(TaskList parent) {
        this.mParent = parent;
    }

    public boolean getCompleted() {
        return this.mCompleted;
    }

    public String getNotes() {
        return this.mNotes;
    }

    public Task getPriorSibling() {
        return this.mPriorSibling;
    }

    public TaskList getParent() {
        return this.mParent;
    }

}

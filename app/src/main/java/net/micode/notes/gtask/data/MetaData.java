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

import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONException;
import org.json.JSONObject;


public class MetaData extends Task {
    // 日志标签，使用类的简单名称
    private final static String TAG = MetaData.class.getSimpleName();

    // 存储相关联的 Google Task ID
    private String mRelatedGid = null;

    /**
     * 设置元数据
     * @param gid 关联的 Google Task ID
     * @param metaInfo 元数据的 JSON 对象
     */
    public void setMeta(String gid, JSONObject metaInfo) {
        try {
            // 将 Google Task ID 添加到元数据 JSON 对象中
            metaInfo.put(GTaskStringUtils.META_HEAD_GTASK_ID, gid);
        } catch (JSONException e) {
            // 如果添加失败，记录错误日志
            Log.e(TAG, "failed to put related gid");
        }
        // 将 JSON 对象转为字符串并设置为笔记内容
        setNotes(metaInfo.toString());
        // 设置名称为预定义的元数据笔记名称
        setName(GTaskStringUtils.META_NOTE_NAME);
    }

    /**
     * 获取相关联的 Google Task ID
     * @return 返回 mRelatedGid 字段值
     */
    public String getRelatedGid() {
        return mRelatedGid;
    }

    /**
     * 判断元数据是否值得保存
     * @return 如果笔记内容不为空，则返回 true
     */
    @Override
    public boolean isWorthSaving() {
        return getNotes() != null;
    }

    /**
     * 通过远程 JSON 数据设置内容
     * @param js 远程 JSON 对象
     */
    @Override
    public void setContentByRemoteJSON(JSONObject js) {
        // 调用父类的设置方法
        super.setContentByRemoteJSON(js);
        // 如果笔记内容不为空
        if (getNotes() != null) {
            try {
                // 将笔记内容解析为 JSON 对象
                JSONObject metaInfo = new JSONObject(getNotes().trim());
                // 从中提取相关 Google Task ID
                mRelatedGid = metaInfo.getString(GTaskStringUtils.META_HEAD_GTASK_ID);
            } catch (JSONException e) {
                // 如果解析失败，记录警告日志并将 mRelatedGid 设置为 null
                Log.w(TAG, "failed to get related gid");
                mRelatedGid = null;
            }
        }
    }

    /**
     * 通过本地 JSON 数据设置内容（禁止调用）
     * @param js 本地 JSON 对象
     * @throws IllegalAccessError 该方法不应被调用
     */
    @Override
    public void setContentByLocalJSON(JSONObject js) {
        // this function should not be called
        throw new IllegalAccessError("MetaData:setContentByLocalJSON should not be called");
    }

    /**
     * 从内容生成本地 JSON 对象（禁止调用）
     * @return 不返回任何值
     * @throws IllegalAccessError 该方法不应被调用
     */
    @Override
    public JSONObject getLocalJSONFromContent() {
        // 抛出异常，明确禁止调用此方法
        throw new IllegalAccessError("MetaData:getLocalJSONFromContent should not be called");
    }

    /**
     * 获取同步操作类型（禁止调用）
     * @param c 数据库游标
     * @return 不返回任何值
     * @throws IllegalAccessError 该方法不应被调用
     */
    @Override
    public int getSyncAction(Cursor c) {
        // 抛出异常，明确禁止调用此方法
        throw new IllegalAccessError("MetaData:getSyncAction should not be called");
    }

}

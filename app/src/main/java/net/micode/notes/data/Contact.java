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

package net.micode.notes.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * 联系人信息查询工具类
 * 功能：根据电话号码查询联系人姓名，并缓存查询结果以提高性能
 */
public class Contact {
    // 缓存电话号码与对应姓名的映射，避免重复查询数据库
    private static HashMap<String, String> sContactCache;

    // 日志标签
    private static final String TAG = "Contact";

    /**
     * 查询联系人的 SQL WHERE 条件模板
     * 说明：
     * 1. PHONE_NUMBERS_EQUAL 用于比较电话号码是否相等（考虑国际格式）
     * 2. 限制 MIME 类型为电话类型（过滤非电话数据）
     * 3. 通过子查询 phone_lookup 表匹配最小匹配规则（min_match 参数待替换）
     */
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')"; // '+' 将被替换为实际的最小匹配值

    /**
     * 根据电话号码获取联系人姓名
     * @param context    上下文对象（用于访问 ContentResolver）
     * @param phoneNumber 要查询的电话号码
     * @return 联系人姓名（未找到时返回 null）
     */
    public static String getContact(Context context, String phoneNumber) {
        // 延迟初始化缓存
        if (sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }

        // 优先从缓存中读取
        if (sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }

        // 构建完整查询条件：替换 min_match 参数为电话号码的最小匹配长度
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));

        // 查询联系人数据库
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    Data.CONTENT_URI,
                    new String[]{Phone.DISPLAY_NAME},
                    selection,
                    new String[]{phoneNumber}, // 绑定查询参数
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                sContactCache.put(phoneNumber, name); // 更新缓存
                return name;
            } else {
                Log.d(TAG, "No contact matched with number:" + phoneNumber);
                return null;
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Cursor get string error " + e.toString());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close(); // 确保关闭 Cursor 释放资源
            }
        }
    }
}

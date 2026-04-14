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

package net.micode.notes.gtask.remote;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.data.MetaData;
import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.SqlNote;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


public class GTaskManager {
    // 日志标签，用于标识日志输出的来源类
    private static final String TAG = GTaskManager.class.getSimpleName();

    // 定义同步操作的状态码
    public static final int STATE_SUCCESS = 0; // 成功状态
    public static final int STATE_NETWORK_ERROR = 1; // 网络错误状态
    public static final int STATE_INTERNAL_ERROR = 2; // 内部错误状态
    public static final int STATE_SYNC_IN_PROGRESS = 3; // 同步正在进行中状态
    public static final int STATE_SYNC_CANCELLED = 4; // 同步被取消状态

    // 单例模式实例变量
    private static GTaskManager mInstance = null;

    // 存储Activity上下文，用于获取认证令牌
    private Activity mActivity;

    // 应用程序上下文和内容解析器，用于访问内容提供者
    private Context mContext;
    private ContentResolver mContentResolver;

    // 标记当前是否正在同步数据及同步是否被取消
    private boolean mSyncing;
    private boolean mCancelled;

    // 数据结构，用于存储任务列表、节点（任务或任务列表）、元数据等信息
    private HashMap<String, TaskList> mGTaskListHashMap;
    private HashMap<String, Node> mGTaskHashMap;
    private HashMap<String, MetaData> mMetaHashMap;
    private TaskList mMetaList;
    private HashSet<Long> mLocalDeleteIdMap;
    private HashMap<String, Long> mGidToNid;
    private HashMap<Long, String> mNidToGid;

    // 私有构造函数，确保使用单例模式创建实例
    private GTaskManager() {
        mSyncing = false;
        mCancelled = false;
        mGTaskListHashMap = new HashMap<>();
        mGTaskHashMap = new HashMap<>();
        mMetaHashMap = new HashMap<>();
        mMetaList = null;
        mLocalDeleteIdMap = new HashSet<>();
        mGidToNid = new HashMap<>();
        mNidToGid = new HashMap<>();
    }

    // 获取单例实例的方法
    public static synchronized GTaskManager getInstance() {
        if (mInstance == null) {
            mInstance = new GTaskManager(); // 如果实例不存在，则创建新实例
        }
        return mInstance; // 返回现有实例
    }

    // 设置Activity上下文，用于获取认证令牌
    public synchronized void setActivityContext(Activity activity) {
        mActivity = activity;
    }

    // 同步方法，负责与Google任务服务同步数据
    public int sync(Context context, GTaskASyncTask asyncTask) {
        if (mSyncing) { // 检查是否已经在进行同步
            Log.d(TAG, "Sync is in progress");
            return STATE_SYNC_IN_PROGRESS;
        }

        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mSyncing = true;
        mCancelled = false;

        clearDataStructures(); // 清空所有缓存的数据结构

        try {
            GTaskClient client = GTaskClient.getInstance();
            client.resetUpdateArray(); // 重置更新队列

            // 登录Google任务服务
            if (!mCancelled && !client.login(mActivity)) {
                throw new NetworkFailureException("login google task failed");
            }

            // 初始化Google任务列表
            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_init_list));
            initGTaskList();

            // 执行内容同步工作
            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_syncing));
            syncContent();
        } catch (NetworkFailureException | ActionFailureException e) {
            Log.e(TAG, e.toString());
            return handleException(e);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return STATE_INTERNAL_ERROR;
        } finally {
            clearDataStructures(); // 无论成功还是失败，最后都清空数据结构
            mSyncing = false;
        }

        return mCancelled ? STATE_SYNC_CANCELLED : STATE_SUCCESS;
    }

    // 清空所有数据结构的方法
    private void clearDataStructures() {
        mGTaskListHashMap.clear();
        mGTaskHashMap.clear();
        mMetaHashMap.clear();
        mLocalDeleteIdMap.clear();
        mGidToNid.clear();
        mNidToGid.clear();
    }

    // 根据异常类型返回对应的状态码
    private int handleException(Exception e) {
        if (e instanceof NetworkFailureException) {
            return STATE_NETWORK_ERROR;
        } else if (e instanceof ActionFailureException) {
            return STATE_INTERNAL_ERROR;
        } else {
            return STATE_INTERNAL_ERROR;
        }
    }

    // 初始化Google任务列表的方法
    private void initGTaskList() throws NetworkFailureException {
        if (mCancelled) return; // 如果同步被取消，则直接返回

        GTaskClient client = GTaskClient.getInstance(); // 获取GTaskClient实例
        try {
            JSONArray jsTaskLists = client.getTaskLists(); // 从Google获取任务列表

            // 初始化元数据列表
            for (int i = 0; i < jsTaskLists.length(); i++) {
                JSONObject object = jsTaskLists.getJSONObject(i);
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME);

                if (name.equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META)) {
                    mMetaList = new TaskList();
                    mMetaList.setContentByRemoteJSON(object);

                    // 加载元数据
                    JSONArray jsMetas = client.getTaskList(gid);
                    for (int j = 0; j < jsMetas.length(); j++) {
                        object = jsMetas.getJSONObject(j);
                        MetaData metaData = new MetaData();
                        metaData.setContentByRemoteJSON(object);
                        if (metaData.isWorthSaving()) {
                            mMetaList.addChildTask(metaData);
                            if (metaData.getGid() != null) {
                                mMetaHashMap.put(metaData.getRelatedGid(), metaData);
                            }
                        }
                    }
                }
            }

            // 如果元数据列表不存在，则创建一个新的
            if (mMetaList == null) {
                mMetaList = new TaskList();
                mMetaList.setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META);
                GTaskClient.getInstance().createTaskList(mMetaList);
            }

            // 初始化任务列表
            for (int i = 0; i < jsTaskLists.length(); i++) {
                JSONObject object = jsTaskLists.getJSONObject(i);
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME);

                if (name.startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX)
                        && !name.equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META)) {
                    TaskList tasklist = new TaskList();
                    tasklist.setContentByRemoteJSON(object);
                    mGTaskListHashMap.put(gid, tasklist);
                    mGTaskHashMap.put(gid, tasklist);

                    // 加载任务
                    JSONArray jsTasks = client.getTaskList(gid);
                    for (int j = 0; j < jsTasks.length(); j++) {
                        object = jsTasks.getJSONObject(j);
                        gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                        Task task = new Task();
                        task.setContentByRemoteJSON(object);
                        if (task.isWorthSaving()) {
                            task.setMetaInfo(mMetaHashMap.get(gid));
                            tasklist.addChildTask(task);
                            mGTaskHashMap.put(gid, task);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("initGTaskList: handing JSONObject failed");
        }
    }

    /**
     * 同步内容方法，负责同步笔记和文件夹的内容。
     * 包括处理本地删除的笔记、同步文件夹、同步数据库中存在的笔记、处理剩余未同步项目，
     * 以及在同步完成后清除本地删除表并刷新本地同步ID。
     */
    private void syncContent() throws NetworkFailureException {
        // 清除本地删除ID集合
        mLocalDeleteIdMap.clear();

        // 如果同步被取消，则直接返回
        if (mCancelled) {
            return;
        }

        Cursor c = null;
        try {
            // 查询位于回收站中的非系统类型笔记
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id=?)", new String[]{String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)}, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN); // 获取Google任务ID
                    Node node = mGTaskHashMap.get(gid); // 在哈希映射中查找对应节点
                    if (node != null) {
                        mGTaskHashMap.remove(gid); // 从哈希映射中移除该节点
                        doContentSync(Node.SYNC_ACTION_DEL_REMOTE, node, c); // 执行内容同步操作（远程删除）
                    }
                    mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN)); // 添加到本地删除ID集合
                }
            } else {
                Log.w(TAG, "failed to query trash folder"); // 日志记录查询失败信息
            }
        } finally {
            if (c != null) {
                c.close(); // 关闭游标防止资源泄露
                c = null;
            }
        }

        // 调用syncFolder方法来同步文件夹
        syncFolder();

        try {
            // 查询数据库中存在的笔记
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[]{String.valueOf(Notes.TYPE_NOTE), String.valueOf(Notes.ID_TRASH_FOLER)}, NoteColumns.TYPE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    Node node = mGTaskHashMap.get(gid);
                    int syncType;
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN)); // 更新Google ID到本地ID的映射
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid); // 更新本地ID到Google ID的映射
                        syncType = node.getSyncAction(c); // 确定同步类型
                    } else {
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            syncType = Node.SYNC_ACTION_ADD_REMOTE; // 如果没有Google ID，则是本地添加需要同步到远程
                        } else {
                            syncType = Node.SYNC_ACTION_DEL_LOCAL; // 如果有Google ID但不在哈希映射中，则认为是远程删除
                        }
                    }
                    doContentSync(syncType, node, c); // 执行内容同步操作
                }
            } else {
                Log.w(TAG, "failed to query existing note in database"); // 日志记录查询失败信息
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 处理剩余未同步的项目
        Iterator<Map.Entry<String, Node>> iter = mGTaskHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Node> entry = iter.next();
            Node node = entry.getValue();
            doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null); // 对于剩余项执行本地添加操作
        }

        // 如果同步未被取消，则清除本地删除表，并刷新本地同步ID
        if (!mCancelled) {
            if (!DataUtils.batchDeleteNotes(mContentResolver, mLocalDeleteIdMap)) {
                throw new ActionFailureException("failed to batch-delete local deleted notes"); // 如果批量删除失败则抛出异常
            }
            GTaskClient.getInstance().commitUpdate(); // 提交更新
            refreshLocalSyncId(); // 刷新本地同步ID
        }
    }

    /**
     * 同步文件夹方法，负责同步根文件夹、通话记录文件夹及本地存在的其他文件夹。
     * 包括检查是否同步被取消、处理文件夹同步逻辑等。
     */
    private void syncFolder() throws NetworkFailureException {
        // 如果同步被取消，则直接返回
        if (mCancelled) {
            return;
        }

        Cursor c = null;
        try {
            // 处理根文件夹
            c = mContentResolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, Notes.ID_ROOT_FOLDER), SqlNote.PROJECTION_NOTE, null, null, null);
            if (c != null) {
                if (c.moveToNext()) {
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    Node node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, (long) Notes.ID_ROOT_FOLDER); // 更新映射关系
                        mNidToGid.put((long) Notes.ID_ROOT_FOLDER, gid);
                        if (!node.getName().equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT))
                            doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c); // 如果名称不同则更新远程
                    } else {
                        doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c); // 如果节点不存在则添加
                    }
                }
            } else {
                Log.w(TAG, "failed to query root folder");
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        try {
            // 处理通话记录文件夹
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE, "(_id=?)",
                    new String[]{String.valueOf(Notes.ID_CALL_RECORD_FOLDER)}, null);
            if (c != null) {
                if (c.moveToNext()) {
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    Node node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, (long) Notes.ID_CALL_RECORD_FOLDER); // 更新映射关系
                        mNidToGid.put((long) Notes.ID_CALL_RECORD_FOLDER, gid);
                        if (!node.getName().equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_CALL_NOTE))
                            doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c); // 如果名称不同则更新远程
                    } else {
                        doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c); // 如果节点不存在则添加
                    }
                }
            } else {
                Log.w(TAG, "failed to query call note folder");
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        try {
            // 查询本地存在的其他文件夹
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[]{String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)}, NoteColumns.TYPE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    Node node = mGTaskHashMap.get(gid);
                    int syncType;
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN)); // 更新Google ID到本地ID的映射
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid); // 更新本地ID到Google ID的映射
                        syncType = node.getSyncAction(c); // 确定同步类型
                    } else {
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            syncType = Node.SYNC_ACTION_ADD_REMOTE; // 如果没有Google ID，则是本地添加需要同步到远程
                        } else {
                            syncType = Node.SYNC_ACTION_DEL_LOCAL; // 如果有Google ID但不在哈希映射中，则认为是远程删除
                        }
                    }
                    doContentSync(syncType, node, c); // 执行内容同步操作
                }
            } else {
                Log.w(TAG, "failed to query existing folder");
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 对于远程新增加的文件夹
        Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, TaskList> entry = iter.next();
            String gid = entry.getKey();
            Node node = entry.getValue();
            if (mGTaskHashMap.containsKey(gid)) {
                mGTaskHashMap.remove(gid);
                doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null); // 对于远程新增文件夹执行本地添加操作
            }
        }

        if (!mCancelled)
            GTaskClient.getInstance().commitUpdate(); // 如果未取消同步，则提交更新
    }

    /**
     * 根据同步类型执行内容同步操作。
     * 包括添加本地节点、添加远程节点、删除本地节点、删除远程节点、更新本地节点和更新远程节点等。
     */
    private void doContentSync(int syncType, Node node, Cursor c) throws NetworkFailureException {
        // 如果同步被取消，则直接返回
        if (mCancelled) {
            return;
        }

        MetaData meta;
        switch (syncType) {
            case Node.SYNC_ACTION_ADD_LOCAL:
                addLocalNode(node); // 添加本地节点
                break;
            case Node.SYNC_ACTION_ADD_REMOTE:
                addRemoteNode(node, c); // 添加远程节点
                break;
            case Node.SYNC_ACTION_DEL_LOCAL:
                meta = mMetaHashMap.get(c.getString(SqlNote.GTASK_ID_COLUMN));
                if (meta != null) {
                    GTaskClient.getInstance().deleteNode(meta); // 删除元数据中的节点
                }
                mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN)); // 添加到本地删除ID集合
                break;
            case Node.SYNC_ACTION_DEL_REMOTE:
                meta = mMetaHashMap.get(node.getGid());
                if (meta != null) {
                    GTaskClient.getInstance().deleteNode(meta); // 删除元数据中的节点
                }
                GTaskClient.getInstance().deleteNode(node); // 从远程删除节点
                break;
            case Node.SYNC_ACTION_UPDATE_LOCAL:
                updateLocalNode(node, c); // 更新本地节点
                break;
            case Node.SYNC_ACTION_UPDATE_REMOTE:
                updateRemoteNode(node, c); // 更新远程节点
                break;
            case Node.SYNC_ACTION_UPDATE_CONFLICT:
                // 对于冲突更新，目前简单地使用本地更新覆盖
                updateRemoteNode(node, c);
                break;
            case Node.SYNC_ACTION_NONE:
                break;
            case Node.SYNC_ACTION_ERROR:
            default:
                throw new ActionFailureException("unknown sync action type"); // 抛出未知同步类型的异常
        }
    }

    /**
     * 添加本地节点的方法。
     * 处理不同类型（如文件夹或任务）的节点，并确保其正确添加到本地数据库中。
     */
    private void addLocalNode(Node node) throws NetworkFailureException {
        // 如果同步被取消，则直接返回
        if (mCancelled) {
            return;
        }

        SqlNote sqlNote;
        if (node instanceof TaskList) { // 如果是文件夹类型
            if (node.getName().equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT)) {
                sqlNote = new SqlNote(mContext, Notes.ID_ROOT_FOLDER); // 根文件夹
            } else if (node.getName().equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_CALL_NOTE)) {
                sqlNote = new SqlNote(mContext, Notes.ID_CALL_RECORD_FOLDER); // 通话记录文件夹
            } else {
                sqlNote = new SqlNote(mContext);
                sqlNote.setContent(node.getLocalJSONFromContent()); // 设置节点内容
                sqlNote.setParentId(Notes.ID_ROOT_FOLDER); // 设置父ID为根文件夹
            }
        } else { // 如果是任务类型
            sqlNote = new SqlNote(mContext);
            JSONObject js = node.getLocalJSONFromContent();
            try {
                // 检查并处理可能存在的重复ID问题
                if (js.has(GTaskStringUtils.META_HEAD_NOTE)) {
                    JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
                    if (note.has(NoteColumns.ID)) {
                        long id = note.getLong(NoteColumns.ID);
                        if (DataUtils.existInNoteDatabase(mContentResolver, id)) {
                            note.remove(NoteColumns.ID); // 移除重复ID
                        }
                    }
                }

                if (js.has(GTaskStringUtils.META_HEAD_DATA)) {
                    JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject data = dataArray.getJSONObject(i);
                        if (data.has(DataColumns.ID)) {
                            long dataId = data.getLong(DataColumns.ID);
                            if (DataUtils.existInDataDatabase(mContentResolver, dataId)) {
                                data.remove(DataColumns.ID); // 移除重复数据ID
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Log.w(TAG, e.toString());
                e.printStackTrace();
            }
            sqlNote.setContent(js); // 设置内容

            Long parentId = mGidToNid.get(((Task) node).getParent().getGid());
            if (parentId == null) {
                Log.e(TAG, "cannot find task's parent id locally");
                throw new ActionFailureException("cannot add local node");
            }
            sqlNote.setParentId(parentId.longValue()); // 设置父ID
        }

        // 创建本地节点
        sqlNote.setGtaskId(node.getGid());
        sqlNote.commit(false); // 提交更改

        // 更新Google ID到本地ID的映射
        mGidToNid.put(node.getGid(), sqlNote.getId());
        mNidToGid.put(sqlNote.getId(), node.getGid());

        // 更新元数据
        updateRemoteMeta(node.getGid(), sqlNote);
    }

    /**
     * 更新本地节点的方法。
     * 主要用于更新已有节点的内容及其在数据库中的表示。
     */
    private void updateLocalNode(Node node, Cursor c) throws NetworkFailureException {
        // 如果同步被取消，则直接返回
        if (mCancelled) {
            return;
        }

        SqlNote sqlNote;
        // 更新本地笔记
        sqlNote = new SqlNote(mContext, c);
        sqlNote.setContent(node.getLocalJSONFromContent()); // 设置新的内容

        Long parentId = (node instanceof Task) ? mGidToNid.get(((Task) node).getParent().getGid()) : new Long(Notes.ID_ROOT_FOLDER);
        if (parentId == null) {
            Log.e(TAG, "cannot find task's parent id locally");
            throw new ActionFailureException("cannot update local node");
        }
        sqlNote.setParentId(parentId.longValue()); // 设置父ID
        sqlNote.commit(true); // 提交更改

        // 更新元数据信息
        updateRemoteMeta(node.getGid(), sqlNote);
    }

    /**
     * 添加远程节点的方法。
     * 根据节点类型（笔记或文件夹）更新远程服务器，并确保本地与远程数据同步。
     */
    private void addRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        // 如果同步被取消，则直接返回
        if (mCancelled) {
            return;
        }

        SqlNote sqlNote = new SqlNote(mContext, c);
        Node n;

        // 更新远程节点
        if (sqlNote.isNoteType()) { // 如果是笔记类型
            Task task = new Task();
            task.setContentByLocalJSON(sqlNote.getContent()); // 设置内容

            String parentGid = mNidToGid.get(sqlNote.getParentId());
            if (parentGid == null) {
                Log.e(TAG, "cannot find task's parent tasklist");
                throw new ActionFailureException("cannot add remote task");
            }
            mGTaskListHashMap.get(parentGid).addChildTask(task); // 将任务添加到对应的任务列表中

            GTaskClient.getInstance().createTask(task); // 创建远程任务
            n = (Node) task;

            // 添加元数据
            updateRemoteMeta(task.getGid(), sqlNote);
        } else { // 如果是文件夹类型
            TaskList tasklist = null;

            // 检查是否已存在同名文件夹
            String folderName = GTaskStringUtils.MIUI_FOLDER_PREFFIX;
            if (sqlNote.getId() == Notes.ID_ROOT_FOLDER)
                folderName += GTaskStringUtils.FOLDER_DEFAULT;
            else if (sqlNote.getId() == Notes.ID_CALL_RECORD_FOLDER)
                folderName += GTaskStringUtils.FOLDER_CALL_NOTE;
            else
                folderName += sqlNote.getSnippet();

            Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, TaskList> entry = iter.next();
                TaskList list = entry.getValue();
                if (list.getName().equals(folderName)) {
                    tasklist = list;
                    if (mGTaskHashMap.containsKey(entry.getKey())) {
                        mGTaskHashMap.remove(entry.getKey());
                    }
                    break;
                }
            }

            // 若不存在匹配项，则创建新文件夹
            if (tasklist == null) {
                tasklist = new TaskList();
                tasklist.setContentByLocalJSON(sqlNote.getContent());
                GTaskClient.getInstance().createTaskList(tasklist); // 创建远程文件夹
                mGTaskListHashMap.put(tasklist.getGid(), tasklist);
            }
            n = (Node) tasklist;
        }

        // 更新本地节点信息
        sqlNote.setGtaskId(n.getGid());
        sqlNote.commit(false);
        sqlNote.resetLocalModified();
        sqlNote.commit(true);

        // 更新Google ID到本地ID的映射
        mGidToNid.put(n.getGid(), sqlNote.getId());
        mNidToGid.put(sqlNote.getId(), n.getGid());
    }

    /**
     * 更新远程节点的方法。
     * 处理笔记或文件夹类型的节点更新，并根据需要移动任务。
     */
    private void updateRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        // 如果同步被取消，则直接返回
        if (mCancelled) {
            return;
        }

        SqlNote sqlNote = new SqlNote(mContext, c);

        // 更新远程节点
        node.setContentByLocalJSON(sqlNote.getContent());
        GTaskClient.getInstance().addUpdateNode(node); // 更新节点

        // 更新元数据
        updateRemoteMeta(node.getGid(), sqlNote);

        // 如果是笔记类型，检查并处理父级变更
        if (sqlNote.isNoteType()) {
            Task task = (Task) node;
            TaskList preParentList = task.getParent();

            String curParentGid = mNidToGid.get(sqlNote.getParentId());
            if (curParentGid == null) {
                Log.e(TAG, "cannot find task's parent tasklist");
                throw new ActionFailureException("cannot update remote task");
            }
            TaskList curParentList = mGTaskListHashMap.get(curParentGid);

            // 父级变更则移动任务
            if (preParentList != curParentList) {
                preParentList.removeChildTask(task);
                curParentList.addChildTask(task);
                GTaskClient.getInstance().moveTask(task, preParentList, curParentList);
            }
        }

        // 清除本地修改标记
        sqlNote.resetLocalModified();
        sqlNote.commit(true);
    }

    /**
     * 更新远程元数据的方法。
     * 根据提供的Google ID和SqlNote对象更新或创建元数据。
     */
    private void updateRemoteMeta(String gid, SqlNote sqlNote) throws NetworkFailureException {
        if (sqlNote != null && sqlNote.isNoteType()) {
            MetaData metaData = mMetaHashMap.get(gid);
            if (metaData != null) {
                metaData.setMeta(gid, sqlNote.getContent());
                GTaskClient.getInstance().addUpdateNode(metaData); // 更新现有元数据
            } else {
                metaData = new MetaData();
                metaData.setMeta(gid, sqlNote.getContent());
                mMetaList.addChildTask(metaData);
                mMetaHashMap.put(gid, metaData);
                GTaskClient.getInstance().createTask(metaData); // 创建新的元数据
            }
        }
    }

    /**
     * 刷新本地同步ID的方法。
     * 清空并重新初始化任务和元数据哈希表，并为每个本地项目设置最新的同步ID。
     */
    private void refreshLocalSyncId() throws NetworkFailureException {
        // 如果同步被取消，则直接返回
        if (mCancelled) {
            return;
        }

        // 获取最新的gtask列表
        mGTaskHashMap.clear();
        mGTaskListHashMap.clear();
        mMetaHashMap.clear();
        initGTaskList(); // 重新初始化任务列表

        Cursor c = null;
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id<>?)", new String[]{
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    Node node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        ContentValues values = new ContentValues();
                        values.put(NoteColumns.SYNC_ID, node.getLastModified());
                        mContentResolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                                c.getLong(SqlNote.ID_COLUMN)), values, null, null);
                    } else {
                        Log.e(TAG, "something is missed");
                        throw new ActionFailureException(
                                "some local items don't have gid after sync");
                    }
                }
            } else {
                Log.w(TAG, "failed to query local note to refresh sync id");
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
    }

    public String getSyncAccount() {
        return GTaskClient.getInstance().getSyncAccount().name;
    }

    public void cancelSync() {
        mCancelled = true;
    }
}
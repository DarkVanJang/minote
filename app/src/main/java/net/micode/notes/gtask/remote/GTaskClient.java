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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
import net.micode.notes.tool.GTaskStringUtils;
import net.micode.notes.ui.NotesPreferenceActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


public class GTaskClient {
    // 使用类名作为日志标签
    private static final String TAG = GTaskClient.class.getSimpleName();

    // 定义Google任务服务的基础URL
    private static final String GTASK_URL = "https://mail.google.com/tasks/";

    // 获取任务数据的URL
    private static final String GTASK_GET_URL = "https://mail.google.com/tasks/ig";

    // 发布任务数据的URL
    private static final String GTASK_POST_URL = "https://mail.google.com/tasks/r/ig";

    // 单例模式实例
    private static GTaskClient mInstance = null;

    // HTTP客户端用于执行网络请求
    private DefaultHttpClient mHttpClient;

    // GET请求使用的URL
    private String mGetUrl;

    // POST请求使用的URL
    private String mPostUrl;

    // 客户端版本号
    private long mClientVersion;

    // 登录状态
    private boolean mLoggedin;

    // 最后一次登录的时间戳
    private long mLastLoginTime;

    // 动作ID
    private int mActionId;

    // 当前账户信息
    private Account mAccount;

    // 更新的任务数组
    private JSONArray mUpdateArray;

    // 私有构造函数防止外部实例化
    private GTaskClient() {
        mHttpClient = null;
        mGetUrl = GTASK_GET_URL;
        mPostUrl = GTASK_POST_URL;
        mClientVersion = -1;
        mLoggedin = false;
        mLastLoginTime = 0;
        mActionId = 1;
        mAccount = null;
        mUpdateArray = null;
    }

    // 获取单例实例的方法
    public static synchronized GTaskClient getInstance() {
        if (mInstance == null) {
            mInstance = new GTaskClient();
        }
        return mInstance;
    }

    // 登录方法
    public boolean login(Activity activity) {
        // 假设cookie会在5分钟后过期，则需要重新登录
        final long interval = 1000 * 60 * 5;
        if (mLastLoginTime + interval < System.currentTimeMillis()) {
            mLoggedin = false;
        }

        // 账户切换后需要重新登录
        if (mLoggedin && !TextUtils.equals(getSyncAccount().name, NotesPreferenceActivity.getSyncAccountName(activity))) {
            mLoggedin = false;
        }

        if (mLoggedin) {
            Log.d(TAG, "already logged in");
            return true;
        }

        mLastLoginTime = System.currentTimeMillis();
        String authToken = loginGoogleAccount(activity, false);
        if (authToken == null) {
            Log.e(TAG, "login google account failed");
            return false;
        }

        // 如果是自定义域名，则使用特定的登录URL
        if (!(mAccount.name.toLowerCase().endsWith("gmail.com") || mAccount.name.toLowerCase().endsWith("googlemail.com"))) {
            StringBuilder url = new StringBuilder(GTASK_URL).append("a/");
            int index = mAccount.name.indexOf('@') + 1;
            String suffix = mAccount.name.substring(index);
            url.append(suffix + "/");
            mGetUrl = url.toString() + "ig";
            mPostUrl = url.toString() + "r/ig";

            if (tryToLoginGtask(activity, authToken)) {
                mLoggedin = true;
            }
        }

        // 尝试使用Google官方URL进行登录
        if (!mLoggedin) {
            mGetUrl = GTASK_GET_URL;
            mPostUrl = GTASK_POST_URL;
            if (!tryToLoginGtask(activity, authToken)) {
                return false;
            }
        }

        mLoggedin = true;
        return true;
    }

    // 登录Google账户获取认证令牌
    private String loginGoogleAccount(Activity activity, boolean invalidateToken) {
        String authToken;
        AccountManager accountManager = AccountManager.get(activity);
        Account[] accounts = accountManager.getAccountsByType("com.google");

        // 检查是否有可用的Google账户
        if (accounts.length == 0) {
            Log.e(TAG, "there is no available google account");
            return null;
        }

        String accountName = NotesPreferenceActivity.getSyncAccountName(activity);
        Account account = null;
        for (Account a : accounts) {
            if (a.name.equals(accountName)) {
                account = a;
                break;
            }
        }
        if (account != null) {
            mAccount = account;
        } else {
            Log.e(TAG, "unable to get an account with the same name in the settings");
            return null;
        }

        // 获取认证令牌
        AccountManagerFuture<Bundle> accountManagerFuture = accountManager.getAuthToken(account, "goanna_mobile", null, activity, null, null);
        try {
            Bundle authTokenBundle = accountManagerFuture.getResult();
            authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
            if (invalidateToken) {
                accountManager.invalidateAuthToken("com.google", authToken);
                loginGoogleAccount(activity, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "get auth token failed");
            authToken = null;
        }

        return authToken;
    }


    /**
     * 尝试登录Google任务服务。
     * 如果当前认证令牌无效，则尝试重新获取认证令牌并再次尝试登录。
     *
     * @param activity  当前Activity上下文
     * @param authToken 认证令牌
     * @return 登录成功返回true，否则返回false
     */
    private boolean tryToLoginGtask(Activity activity, String authToken) {
        if (!loginGtask(authToken)) { // 首先尝试使用提供的authToken登录
            // 如果登录失败，可能是由于authToken过期。现在我们使令牌失效，并再次尝试登录
            authToken = loginGoogleAccount(activity, true); // 重新获取认证令牌
            if (authToken == null) { // 如果无法获取新的认证令牌
                Log.e(TAG, "login google account failed"); // 打印错误日志
                return false; // 返回false表示登录失败
            }

            if (!loginGtask(authToken)) { // 再次尝试登录
                Log.e(TAG, "login gtask failed"); // 打印错误日志
                return false; // 返回false表示登录失败
            }
        }
        return true; // 成功登录返回true
    }

    /**
     * 使用给定的认证令牌执行登录到Google任务的具体操作。
     *
     * @param authToken 认证令牌
     * @return 登录成功返回true，否则返回false
     */
    private boolean loginGtask(String authToken) {
        int timeoutConnection = 10000; // 设置连接超时时间为10秒
        int timeoutSocket = 15000; // 设置套接字超时时间为15秒
        HttpParams httpParameters = new BasicHttpParams(); // 创建HTTP参数对象
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection); // 设置连接超时时间
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket); // 设置套接字超时时间
        mHttpClient = new DefaultHttpClient(httpParameters); // 创建默认的HTTP客户端实例
        BasicCookieStore localBasicCookieStore = new BasicCookieStore(); // 创建一个新的cookie存储
        mHttpClient.setCookieStore(localBasicCookieStore); // 设置HTTP客户端使用的cookie存储
        HttpProtocolParams.setUseExpectContinue(mHttpClient.getParams(), false); // 禁用期望继续

        // 开始登录到Google任务服务
        try {
            String loginUrl = mGetUrl + "?auth=" + authToken; // 构建带有认证令牌的URL
            HttpGet httpGet = new HttpGet(loginUrl); // 创建一个GET请求
            HttpResponse response = mHttpClient.execute(httpGet); // 执行GET请求

            // 获取响应中的cookie
            List<Cookie> cookies = mHttpClient.getCookieStore().getCookies();
            boolean hasAuthCookie = false;
            for (Cookie cookie : cookies) {
                if (cookie.getName().contains("GTL")) { // 检查是否包含特定名称的cookie（用于验证）
                    hasAuthCookie = true;
                }
            }
            if (!hasAuthCookie) { // 如果没有找到有效的认证cookie
                Log.w(TAG, "it seems that there is no auth cookie"); // 打印警告日志
            }

            // 解析响应内容以获取客户端版本号
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup("; // 定义开始标记
            String jsEnd = ")}</script>"; // 定义结束标记
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) { // 查找并提取JSON字符串
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString); // 解析JSON字符串
            mClientVersion = js.getLong("v"); // 获取客户端版本号
        } catch (JSONException e) {
            Log.e(TAG, e.toString()); // 打印异常堆栈跟踪
            e.printStackTrace();
            return false; // 返回false表示解析JSON失败
        } catch (Exception e) {
            Log.e(TAG, "httpget gtask_url failed"); // 打印异常信息
            return false; // 返回false表示其他异常
        }

        return true; // 成功返回true
    }

    /**
     * 获取下一个动作ID。
     * 动作ID是递增的整数，用于区分不同的操作。
     *
     * @return 下一个可用的动作ID
     */
    private int getActionId() {
        return mActionId++;
    }

    /**
     * 创建一个HttpPost请求。
     * 设置请求头的内容类型和AT字段。
     *
     * @return 新创建的HttpPost对象
     */
    private HttpPost createHttpPost() {
        HttpPost httpPost = new HttpPost(mPostUrl); // 创建POST请求，URL为mPostUrl
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8"); // 设置请求头
        httpPost.setHeader("AT", "1"); // 设置AT头
        return httpPost; // 返回创建好的HttpPost对象
    }

    /**
     * 获取HTTP响应的内容。
     * 支持处理gzip或deflate编码的内容。
     *
     * @param entity HTTP实体
     * @return 响应内容的字符串表示
     * @throws IOException 如果读取输入流失败
     */
    private String getResponseContent(HttpEntity entity) throws IOException {
        String contentEncoding = null;
        if (entity.getContentEncoding() != null) { // 检查是否有内容编码
            contentEncoding = entity.getContentEncoding().getValue(); // 获取编码方式
            Log.d(TAG, "encoding: " + contentEncoding); // 打印编码信息
        }

        InputStream input = entity.getContent(); // 获取输入流
        if ("gzip".equalsIgnoreCase(contentEncoding)) { // 如果是gzip编码
            input = new GZIPInputStream(entity.getContent()); // 使用GZIPInputStream解压
        } else if ("deflate".equalsIgnoreCase(contentEncoding)) { // 如果是deflate编码
            Inflater inflater = new Inflater(true); // 创建Inflater
            input = new InflaterInputStream(entity.getContent(), inflater); // 使用InflaterInputStream解压
        }

        try {
            InputStreamReader isr = new InputStreamReader(input); // 创建InputStreamReader
            BufferedReader br = new BufferedReader(isr); // 创建BufferedReader
            StringBuilder sb = new StringBuilder(); // 创建StringBuilder用于构建响应内容

            while (true) {
                String buff = br.readLine(); // 逐行读取输入流
                if (buff == null) { // 如果到达文件末尾
                    return sb.toString(); // 返回构建好的字符串
                }
                sb.append(buff); // 将每一行添加到StringBuilder
            }
        } finally {
            input.close(); // 关闭输入流
        }
    }

    /**
     * 发送POST请求。
     * 处理各种异常情况，并将结果转换为JSONObject。
     *
     * @param js 要发送的JSON数据
     * @return 服务器响应的JSON对象
     * @throws NetworkFailureException 如果网络请求失败
     */
    private JSONObject postRequest(JSONObject js) throws NetworkFailureException {
        if (!mLoggedin) { // 如果用户未登录
            Log.e(TAG, "please login first"); // 打印错误日志
            throw new ActionFailureException("not logged in"); // 抛出异常提示未登录
        }

        HttpPost httpPost = createHttpPost(); // 创建HttpPost请求
        try {
            LinkedList<BasicNameValuePair> list = new LinkedList<>(); // 创建参数列表
            list.add(new BasicNameValuePair("r", js.toString())); // 添加参数
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8"); // 创建表单实体
            httpPost.setEntity(entity); // 设置POST请求的实体

            // 执行POST请求
            HttpResponse response = mHttpClient.execute(httpPost); // 发送请求
            String jsString = getResponseContent(response.getEntity()); // 获取响应内容
            return new JSONObject(jsString); // 将响应内容转换为JSONObject

        } catch (ClientProtocolException e) { // 处理协议异常
            Log.e(TAG, e.toString()); // 打印异常堆栈跟踪
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed"); // 抛出网络失败异常
        } catch (IOException e) { // 处理IO异常
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (JSONException e) { // 处理解析JSON异常
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("unable to convert response content to jsonobject");
        } catch (Exception e) { // 处理其他异常
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("error occurs when posting request");
        }
    }

    /**
     * 创建一个新任务。
     * 发送创建任务的请求，并设置任务的GID。
     *
     * @param task 要创建的任务
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void createTask(Task task) throws NetworkFailureException {
        commitUpdate(); // 提交更新
        try {
            JSONObject jsPost = new JSONObject(); // 创建JSON对象
            JSONArray actionList = new JSONArray(); // 创建动作列表

            // 添加创建任务的动作到动作列表
            actionList.put(task.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList); // 将动作列表放入JSON对象

            // 添加客户端版本号
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送POST请求
            JSONObject jsResponse = postRequest(jsPost); // 发送创建任务的请求
            JSONObject jsResult = jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_RESULTS).getJSONObject(0); // 获取第一个结果
            task.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID)); // 设置任务的GID

        } catch (JSONException e) { // 处理解析JSON异常
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create task: handing jsonobject failed");
        }
    }

    /**
     * 创建一个新的任务列表。
     * 该方法首先提交任何未完成的更新，然后发送创建新任务列表的请求，并设置返回的任务列表GID。
     *
     * @param tasklist 要创建的任务列表对象
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void createTaskList(TaskList tasklist) throws NetworkFailureException {
        commitUpdate(); // 提交所有未完成的更新
        try {
            JSONObject jsPost = new JSONObject(); // 创建新的JSON对象用于POST请求
            JSONArray actionList = new JSONArray(); // 创建动作列表

            // 添加创建任务列表的动作到动作列表
            actionList.put(tasklist.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList); // 将动作列表添加到POST请求中

            // 添加客户端版本号
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送POST请求并处理响应
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0); // 获取结果数组中的第一个元素
            tasklist.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID)); // 设置任务列表的GID

        } catch (JSONException e) { // 处理JSON异常
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create tasklist: handing jsonobject failed");
        }
    }

    /**
     * 提交所有待处理的更新操作。
     * 如果有需要更新的操作，则将它们打包成一个请求发送给服务器。
     *
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void commitUpdate() throws NetworkFailureException {
        if (mUpdateArray != null) { // 如果存在待处理的更新
            try {
                JSONObject jsPost = new JSONObject();

                // 添加动作列表
                jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, mUpdateArray);

                // 添加客户端版本号
                jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

                postRequest(jsPost); // 发送请求
                mUpdateArray = null; // 清空更新队列
            } catch (JSONException e) { // 处理JSON异常
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("commit update: handing jsonobject failed");
            }
        }
    }

    /**
     * 向更新队列中添加一个新的更新节点。
     * 如果当前更新队列中的项目数量超过了限制（10个），则先提交这些更新。
     *
     * @param node 需要添加到更新队列的节点
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void addUpdateNode(Node node) throws NetworkFailureException {
        if (node != null) { // 确保节点不为空
            // 如果更新队列超过10项，则先提交这些更新
            if (mUpdateArray != null && mUpdateArray.length() > 10) {
                commitUpdate();
            }

            if (mUpdateArray == null)
                mUpdateArray = new JSONArray(); // 初始化更新队列
            mUpdateArray.put(node.getUpdateAction(getActionId())); // 将更新动作添加到队列中
        }
    }

    public void moveTask(Task task, TaskList preParent, TaskList curParent)
            throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();
            JSONObject action = new JSONObject();

            // action_list
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_MOVE);
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            action.put(GTaskStringUtils.GTASK_JSON_ID, task.getGid());
            if (preParent == curParent && task.getPriorSibling() != null) {
                // put prioring_sibing_id only if moving within the tasklist and
                // it is not the first one
                action.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, task.getPriorSibling());
            }
            action.put(GTaskStringUtils.GTASK_JSON_SOURCE_LIST, preParent.getGid());
            action.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT, curParent.getGid());
            if (preParent != curParent) {
                // put the dest_list only if moving between tasklists
                action.put(GTaskStringUtils.GTASK_JSON_DEST_LIST, curParent.getGid());
            }
            actionList.put(action);
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            postRequest(jsPost);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("move task: handing jsonobject failed");
        }
    }

    public void deleteNode(Node node) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // action_list
            node.setDeleted(true);
            actionList.put(node.getUpdateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            postRequest(jsPost);
            mUpdateArray = null;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("delete node: handing jsonobject failed");
        }
    }

    public JSONArray getTaskLists() throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        try {
            HttpGet httpGet = new HttpGet(mGetUrl);
            HttpResponse response = null;
            response = mHttpClient.execute(httpGet);

            // get the task list
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString);
            return js.getJSONObject("t").getJSONArray(GTaskStringUtils.GTASK_JSON_LISTS);
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("get task lists: handing jasonobject failed");
        }
    }

    public JSONArray getTaskList(String listGid) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();
            JSONObject action = new JSONObject();

            // action_list
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_GETALL);
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            action.put(GTaskStringUtils.GTASK_JSON_LIST_ID, listGid);
            action.put(GTaskStringUtils.GTASK_JSON_GET_DELETED, false);
            actionList.put(action);
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            JSONObject jsResponse = postRequest(jsPost);
            return jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_TASKS);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("get task list: handing jsonobject failed");
        }
    }

    public Account getSyncAccount() {
        return mAccount;
    }

    public void resetUpdateArray() {
        mUpdateArray = null;
    }
}
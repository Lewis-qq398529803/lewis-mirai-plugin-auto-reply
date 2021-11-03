package com.lewis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lewis.utils.FileUtils;
import com.lewis.utils.HttpUtils;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 插件主体
 *
 * @author Lewis
 */
public final class PluginMain extends JavaPlugin {

	/**
	 * 1、继承JavaPlugin
	 * 2、静态初始化单例class，必须public static，并且必须命名为INSTANCE
	 * 3、构造函数不接受参数，并调用super(description)
	 */
	public static final PluginMain INSTANCE = new PluginMain();

	/**
	 * 线程池
	 */
	public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

	/**
	 * 全局配置json
	 */
	private JSONObject configJson = new JSONObject();

	/**
	 * 初始化配置文件
	 */
	private void init() {
		// 检查是否存在配置文件 lewis-auto-reply-config.json
		String configPath = System.getProperty("user.dir") + "/config/lewis_auto_reply/lewis-auto-reply-config.json";
		File file = new File(configPath);
		if (file.exists()) {
			getLogger().info("恭迎主人回归鸭~~~~~~(*╹▽╹*)");
			// 存在则直接读取
			configJson = FileUtils.jsonFile2Object(configPath);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("authorization", configJson.getString("authorization"));
			String resultStr = HttpUtils.sendPostWithJson(Constant.LIST, jsonObject);
			if ("".equals(resultStr)) {
				configJson.put("linkList", "linkList");
			} else {
				JSONObject linkListJson = JSONObject.parseObject(resultStr);
				configJson.put("linkListJson", linkListJson);
			}
		} else {
			getLogger().info("主人是第一次来的吧，小的先给您创建默认的设置，记得手动改下哦~~~~~~(*╹▽╹*)");
			// 不存在则创建默认
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
				JSONObject defultConfig = new JSONObject();
				List<String> groupIdList = new ArrayList<>();
				groupIdList.add("959268265");
				groupIdList.add("711609020");
				defultConfig.put("groupIdList", groupIdList);
				defultConfig.put("authorization", "authorization");
				defultConfig.put("linkList", "linkList");
				configJson = FileUtils.object2JsonFile(configPath, defultConfig);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 启动时自动调用的方法
	 */
	@Override
	public void onEnable() {
		// 初始化
		init();

		getLogger().info("\n作者[ Lewis : qq398529803 ]：感谢各位的使用，Lewis自动回复插件已成功加载 ==================》》》");

		GlobalEventChannel.INSTANCE.registerListenerHost(new SimpleListenerHost() {
			@Override
			public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
				super.handleException(context, exception);
			}

			/**
			 * 群消息处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(GroupMessageEvent event) {
				EXECUTOR_SERVICE.execute(() -> {
					// 分群处理
					// 群号
					long groupId = event.getGroup().getId();
					JSONArray groupIdListTest = configJson.getJSONArray("groupIdList");
					List<Long> groupIdList = new ArrayList<>();
					if (groupIdListTest.size() != 0) {
						for (Object o : groupIdListTest) {
							long aLong = Long.parseLong(String.valueOf(o));
							groupIdList.add(aLong);
						}
						// 判断是否群号属于配置文件中的列表中 属于才运行操作
						if (groupIdList.contains(groupId)) {
							msgHandle(event);
						}
					}
				});
			}

			/**
			 * 好友消息处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(FriendMessageEvent event) {
				EXECUTOR_SERVICE.execute(() -> {
					msgHandle(event);
				});
			}

			/**
			 * 陌生人消息处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(StrangerMessageEvent event) {
				EXECUTOR_SERVICE.execute(() -> {
					msgHandle(event);
				});
			}

			/**
			 * 群临时消息事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(GroupTempMessageEvent event) {
				EXECUTOR_SERVICE.execute(() -> {
					msgHandle(event);
				});
			}

			/**
			 * 好友添加请求事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(NewFriendRequestEvent event) {
				// 自动同意
				event.accept();
			}

			/**
			 * 邀请入群请求事件处理
			 * @param event
			 */
			@EventHandler
			public void handleMessage(BotInvitedJoinGroupRequestEvent event) {
				// 自动同意
				event.accept();
			}
		});
	}

	private PluginMain() {
		super(new JvmPluginDescriptionBuilder("com.lewis.AutoReply", "0.1")
				.name("自定义回话插件 [ Lewis-qq398529803 ]")
				.info("自定义回话插件")
				.author("Lewis")
				.build());
	}

	String key = "";
	int flag = 1;

	/**
	 * 存库
	 * @param event
	 */
	private void addCommand(MessageEvent event, String autoReplyAddUrl) {
		// 获取消息文本 - 去除空格
		String msg = event.getMessage().serializeToMiraiCode().trim();
		// qq号
		long qq = event.getSender().getId();

		// 存词库
		// 排除存在类似于 [mirai: 字符串的内容
		if (!msg.contains("[mirai:")) {
			if (flag == 1) {
				key = msg;
				flag++;
			} else {
				// 第二次之后放入数据库 并且重置flag
				addOne(qq, key, msg, autoReplyAddUrl);
				flag = 1;
			}
		}
	}

	/**
	 * 消息处理
	 *
	 * @param event 消息实体
	 */
	private void msgHandle(MessageEvent event) {

		// 获取configJson中动态获取的linkList
		JSONObject linkListJson = configJson.getJSONObject("linkListJson");
		List<JSONObject> linkList = (List<JSONObject>) linkListJson.get("data");
		getLogger().info("linkList - " + linkList);
		String autoReplyAddUrl = "";
		String autoReplyListUrl = "";
		for (JSONObject linkListItem : linkList) {
			if ("autoReplyAdd".equals(linkListItem.getString("urlName"))) {
				autoReplyAddUrl = linkListItem.getString("linkUrl");
			}
			if ("autoReplyList".equals(linkListItem.getString("urlName"))) {
				autoReplyListUrl = linkListItem.getString("linkUrl");
			}
		}
		getLogger().info("autoReplyAddUrl - " + autoReplyAddUrl);
		getLogger().info("autoReplyListUrl - " + autoReplyListUrl);

		addCommand(event, autoReplyAddUrl);

		// 获取消息文本 - 去除空格
		String msg = event.getMessage().serializeToMiraiCode().trim();

		// 若存在回复词，则返回对应的回复值
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", msg);
		String resultJson = HttpUtils.sendPostWithJson(autoReplyListUrl, jsonObject);
		JSONObject json = JSONObject.parseObject(resultJson);
		JSONArray data = json.getJSONArray("data");
		if (data != null && data.size() != 0) {
			List<JSONObject> result = (List<JSONObject>) json.get("data");
			String answer = getRandom(result).getString("result");
			getLogger().info("answer - " + answer);
			event.getSubject().sendMessage(answer);
		}
	}

	/**
	 * 新增一个指令
	 *
	 * @param qq
	 * @param key
	 * @param value
	 * @return
	 */
	private String addOne(long qq, String key, String value, String autoReplyAddUrl) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", key);
		jsonObject.put("result", value);
		jsonObject.put("forQqCreate", qq);
		String responseJsonStr = HttpUtils.sendPostWithJson(autoReplyAddUrl, jsonObject);
		if (responseJsonStr != null) {
			JSONObject responseJson = JSONObject.parseObject(responseJsonStr);
			Integer code = responseJson.getInteger("code");
			return code == 200 ? "我学会啦w~~~" : "我不太听的懂诶";
		}
		return "主人SAMA的服务器好像没理我诶，请联系我主人理我一下w QAQ";
	}

	/**
	 * 随机从list中获取一个jsonobject元素
	 *
	 * @param jsonObjectList
	 * @return
	 */
	private JSONObject getRandom(List<JSONObject> jsonObjectList) {
		Random random = new Random();
		int n = random.nextInt(jsonObjectList.size());
		return jsonObjectList.get(n);
	}
}
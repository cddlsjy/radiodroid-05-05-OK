下面是一份整合了所有修改意见和补充建议的最终完整修改方案，可直接提交执行。

---

📝 RadioDroid 离线模式开关实现方案（最终整合版）

🎯 目标

在简化版 RadioDroid 中添加一个“离线模式”设置开关。开启后：

· 导入 M3U 时不联网查询电台完整信息
· 播放电台时不获取“真实流地址”，直接用 M3U 中保存的 URL
· 屏蔽不必要的后台网络请求（如服务器统计）
· 开关关闭时，所有网络功能完全恢复正常

✅ 修改概述

文件 修改内容
res/xml/preferences.xml 添加离线模式开关
res/values/strings.xml 添加英文标题和说明
res/values-zh/strings.xml 添加中文标题和说明
StationSaveManager.java 导入 M3U 时跳过网络查询
PlayStationTask.java 播放时跳过获取真实链接
FragmentServerInfo.java 离线模式下不加载服务器统计，并增加空值保护
（如有其他网络请求） 按相同模式添加判断

---

🔧 详细修改步骤

1. 添加设置开关 — res/xml/preferences.xml

在合适的位置（如“播放”分类后）插入以下代码：

```xml
<SwitchPreferenceCompat
    android:key="disable_online_verification"
    android:title="@string/pref_disable_online_verification_title"
    android:summary="@string/pref_disable_online_verification_summary"
    android:defaultValue="false" />
```

---

2. 添加字符串资源

res/values/strings.xml（英语）：

```xml
<string name="pref_disable_online_verification_title">Offline mode</string>
<string name="pref_disable_online_verification_summary">Do not verify stations online; use local M3U data only</string>
```

res/values-zh/strings.xml（中文）：

```xml
<string name="pref_disable_online_verification_title">离线模式</string>
<string name="pref_disable_online_verification_summary">不联网验证电台，仅使用本地 M3U 数据</string>
```

---

3. 修改 StationSaveManager.java

路径：app/src/main/java/net/programmierecke/radiodroid2/StationSaveManager.java

3.1 添加必要的导入（若尚未导入）

在文件顶部加入：

```java
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
```

3.2 在类中添加工具方法

在 StationSaveManager 类内部添加：

```java
/**
 * 检查是否开启了离线模式（不联网验证电台）
 */
private boolean isOfflineMode() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getBoolean("disable_online_verification", false);
}
```

说明：context 是 StationSaveManager 的成员字段，无需额外传入。

3.3 修改 LoadM3UReader 方法

找到 if (!stationUuid.isEmpty()) { ... } 这个代码块（大约在方法后半段），将其修改为：

```java
if (!stationUuid.isEmpty() && !isOfflineMode()) {
    DataRadioStation remoteStation = Utils.getStationByUuid(httpClient, context, stationUuid);
    if (remoteStation != null) {
        station.copyPropertiesFrom(remoteStation);
        if (!stationIconUrl.isEmpty()) {
            station.IconUrl = stationIconUrl;
        }
    }
}
```

---

4. 修改 PlayStationTask.java

路径：app/src/main/java/net/programmierecke/radiodroid2/players/PlayStationTask.java

4.1 导入依赖

在文件顶部添加（如果缺失）：

```java
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
```

4.2 添加工具方法

在 PlayStationTask 类内部添加：

```java
private boolean isOfflineMode(Context ctx) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    return prefs.getBoolean("disable_online_verification", false);
}
```

4.3 修改 doInBackground 方法

在方法开头（Context ctx = contextWeakReference.get(); 之后）插入离线模式判断，修改后的完整方法如下：

```java
@Override
protected String doInBackground(Void... params) {
    Context ctx = contextWeakReference.get();
    if (ctx != null) {
        // 离线模式：直接使用本地 StreamUrl，跳过所有网络请求
        if (isOfflineMode(ctx)) {
            return stationToPlay.StreamUrl;
        }

        // 优先使用本地存储的StreamUrl
        if (stationToPlay.StreamUrl != null && !stationToPlay.StreamUrl.isEmpty()) {
            return stationToPlay.StreamUrl;
        }

        RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();

        if (!stationToPlay.hasValidUuid()) {
            if (!stationToPlay.refresh(radioDroidApp.getHttpClient(), ctx)) {
                return null;
            }
        }

        if (isCancelled()) {
            return null;
        }

        // 只有当本地StreamUrl不可用时，才访问远程服务器获取真实链接
        return Utils.getRealStationLink(radioDroidApp.getHttpClient(), ctx.getApplicationContext(), stationToPlay.StationUuid);
    } else {
        return null;
    }
}
```

---

5. （重要补充）屏蔽 FragmentServerInfo 的服务器统计请求

路径：app/src/main/java/net/programmierecke/radiodroid2/FragmentServerInfo.java

5.1 添加必要导入（如缺失）

```java
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
```

5.2 修改 loadStatisticsFromNetwork 方法

在方法开头加入空值保护和离线模式判断，使其变为：

```java
private void loadStatisticsFromNetwork() {
    // 离线模式直接返回，避免无谓的网络超时
    if (getContext() == null) return;
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
    if (sharedPref.getBoolean("disable_online_verification", false)) {
        return;
    }
    // ... 原有代码保持不变 ...
}
```

---

6. （可选）屏蔽其他可能存在的后台网络请求

在你的简化版中，如果以下功能仍存在，请按相同模式添加离线模式守卫：

· ActivityMain 中的随机播放、搜索等网络请求
· 任何数据库更新 Worker

添加模式为：

```java
if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("disable_online_verification", false)) {
    return;
}
```

---

🚀 编译与测试

1. 执行 ./gradlew assembleDebug 确认编译通过。
2. 安装 APK 后，进入 设置 → 打开 “离线模式”。
3. 重启应用（或重新导入 M3U），验证：
   · 导入 M3U 文件瞬间完成，无网络等待
   · 点击电台立即开始播放，不卡在“获取真实链接”
   · 收藏、历史、自定义列表立即可用
4. 关闭离线模式，验证原有网络功能（如服务器统计）恢复正常。

---

📌 注意事项

· 本方案 不删除任何网络代码，仅添加条件判断，编译风险极低。
· 离线模式下电台的图标加载（Picasso）保留，不影响界面美观。
· 若 M3U 文件中的 StreamUrl 已失效，播放会失败（这是离线模式的预期行为，临时关闭开关即可修复）。
· StationSaveManager 中的 context 是类成员字段，isOfflineMode() 方法可直接使用，无需额外传参。
· 如需进一步屏蔽缓存写入等非关键 I/O，可在 Utils.writeFileCache 中添加同样的离线判断（可选）。

---

以上方案整合了所有修改意见和补充细节，可直接提交执行。
# RadioDroid 修改文档

## 概述

本文档记录了对 RadioDroid 项目的所有修改，主要包括：
1. 离线模式功能实现
2. Android 15 文件选择器兼容性修复
3. 启动选项增强
4. M3U 导入 UUID 问题修复

---

## 一、离线模式功能实现（基于 `05-05-deepseek.md`）

### 1.1 设置开关
- **文件**：`res/xml/preferences.xml`
- **修改**：添加 `disable_online_verification` SwitchPreferenceCompat 开关

### 1.2 字符串资源
- **文件**：`res/values/strings.xml`、`res/values-zh/strings.xml`
- **修改**：添加离线模式标题和描述字符串

### 1.3 核心离线模式方法
- **文件**：`Utils.java`（第 550-553 行）
- **修改**：添加 `isOfflineMode(Context context)` 静态方法

### 1.4 M3U 导入离线处理
- **文件**：`StationSaveManager.java`（`LoadM3UReader` 方法）
- **修改**：离线模式下跳过网络查询电台信息

### 1.5 播放任务离线处理
- **文件**：`PlayStationTask.java`（`doInBackground` 方法）
- **修改**：离线模式下直接使用本地 StreamUrl

### 1.6 服务器统计离线处理
- **文件**：`FragmentServerInfo.java`（`loadStatisticsFromNetwork` 方法）
- **修改**：离线模式下阻止加载服务器统计

---

## 二、额外网络请求屏蔽（基于 `gemin.md`）

### 2.1 UUID 播放离线处理
- **文件**：`ActivityMain.java`（`handleIntent` 方法）
- **修改**：离线模式下从本地收藏/历史/自定义电台查找

### 2.2 随机播放离线处理
- **文件**：`ActivityMain.java`（`playRandomStation` 方法）
- **修改**：离线模式下禁用随机播放功能

### 2.3 新增错误字符串
- **文件**：`res/values/strings.xml`、`res/values-zh/strings.xml`
- **修改**：添加 `error_station_not_found_offline` 和 `error_random_play_offline`

---

## 三、启动选项增强

### 3.1 启动选项配置
- **文件**：`res/values/arrays.xml`
- **修改**：在 `startup_action_entries` 和 `startup_action_entryvalues` 中添加"显示自定义电台"选项

### 3.2 启动逻辑处理
- **文件**：`ActivityMain.java`（`setupTabFromSettings` 方法）
- **修改**：添加自定义电台选项的处理逻辑

### 3.3 CustomStationManager 集成
- **文件**：`RadioDroidApp.java`
- **修改**：添加 `customStationManager` 成员变量和 getter 方法

---

## 四、Android 15 文件选择器修复

### 4.1 替换已弃用 API
- **文件**：`ActivityMain.java`
- **修改**：将 `startActivityForResult()` 替换为 `registerForActivityResult()`

### 4.2 初始化方法
- **文件**：`ActivityMain.java`
- **修改**：添加 `initFileLaunchers()` 方法初始化文件保存和加载的 ActivityResultLauncher

### 4.3 包可见性配置
- **文件**：`AndroidManifest.xml`
- **修改**：添加 `<queries>` 配置，允许应用查询文件选择应用

---

## 五、离线模式彻底修复（基于 `05-06.md` 和 `05-06b-deepseek.md`）

### 5.1 Fragment 网络请求拦截
- **文件**：`FragmentLocalStations.java`、`FragmentTopClick.java`、`FragmentTopVote.java`、`FragmentRecentlyChanged.java`、`FragmentMultiSearch.java`、`FragmentStations.java`
- **修改**：在 `loadData()` 方法开头添加离线模式检查

### 5.2 数据下载入口拦截
- **文件**：`Utils.java`（`downloadFeedRelative` 方法）
- **修改**：在方法开头添加离线模式检查，直接返回 null

### 5.3 标签页显示控制
- **文件**：`FragmentTabs.java`（`setupViewPager` 方法）
- **修改**：离线模式下不创建本地电台标签页

---

## 六、M3U 导入 UUID 问题修复

### 6.1 自动生成 UUID
- **文件**：`StationSaveManager.java`（`LoadM3UReader` 方法）
- **修改**：为无 UUID 的电台自动生成随机 UUID，并设置 `ChangeUuid`

### 6.2 默认值设置
- **文件**：`StationSaveManager.java`（`LoadM3UReader` 方法）
- **修改**：为所有电台字段设置默认值，防止 NPE

### 6.3 空值检查修复
- **文件**：`ItemAdapterStation.java`
- **修改**：添加 `TagsAll` 字段的空值检查

---

## 七、修改文件统计

| 修改文件数 | 新增代码行数 | 功能模块 |
|------------|-------------|----------|
| 15+ | 200+ | 离线模式 |
| 3 | 50+ | 文件选择器修复 |
| 4 | 30+ | 启动选项增强 |
| 3 | 40+ | UUID 问题修复 |

---

## 八、功能验证清单

### ✅ 已完成验证

1. **离线模式** ✅
   - 启动时无网络请求
   - 各 Fragment 显示错误提示而非空白

2. **M3U 导入** ✅
   - 导入无 UUID 的 M3U 文件不闪退
   - 自动生成随机 UUID

3. **文件选择器** ✅
   - Android 15 设备可正常打开文件选择器
   - 支持导入/导出电台列表

4. **启动选项** ✅
   - 支持显示历史/收藏/所有电台/上次视图/自定义电台

5. **播放功能** ✅
   - 离线模式下直接使用本地 URL 播放
   - 随机播放在离线模式下禁用并显示提示

---

## 九、Gradle Wrapper

### 9.1 创建 gradlew 文件
- **文件**：`gradlew`（Linux/Mac）、`gradlew.bat`（Windows）
- **修改**：创建 Gradle wrapper 启动脚本

### 9.2 更新 .gitignore
- **文件**：`.gitignore`
- **修改**：允许 gradlew 和 gradlew.bat 被版本控制

---

**版本**：v0.94-DEV
**最后更新**：2026-05-06

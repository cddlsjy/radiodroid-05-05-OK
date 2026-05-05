# M3U文件导入导出功能技术设计文档

## 文档信息
- **设计ID**: 260504-feat-m3u-import-export
- **需求文档**: requirements.md
- **创建日期**: 2026-05-04
- **版本**: 1.0

## 1. 系统架构

### 1.1 整体架构
本功能在现有RadioDroid应用架构基础上扩展,采用分层设计:

```
┌─────────────────────────────────────────────────────────┐
│                    UI层 (Presentation)                    │
├─────────────────────────────────────────────────────────┤
│  ActivityMain.java                                      │
│  ├── FragmentCustomStations.java (新增)                  │
│  ├── FragmentStations.java (修改)                        │
│  └── FileDialog (现有)                                    │
├─────────────────────────────────────────────────────────┤
│                  业务逻辑层 (Business)                     │
├─────────────────────────────────────────────────────────┤
│  CustomStationManager.java (新增)                        │
│  ├── LoadM3U() (导入M3U)                                 │
│  ├── SaveM3U() (导出M3U)                                 │
│  ├── addMultiple() (批量添加)                            │
│  └── clear() (清空列表)                                  │
├─────────────────────────────────────────────────────────┤
│                   数据层 (Data)                           │
├─────────────────────────────────────────────────────────┤
│  SharedPreferences (持久化存储)                           │
│  DataRadioStation (电台数据模型)                          │
├─────────────────────────────────────────────────────────┤
│                   工具层 (Utils)                          │
├─────────────────────────────────────────────────────────┤
│  PlaylistM3U.java (M3U解析器,现有)                        │
│  PlaylistM3UEntry.java (M3U条目,现有)                     │
│  StationSaveManager.java (参考实现,现有)                  │
└─────────────────────────────────────────────────────────┘
```

### 1.2 模块依赖关系

```
ActivityMain
    ├── CustomStationManager (新增)
    │   ├── DataRadioStation
    │   ├── PlaylistM3U
    │   └── SharedPreferences
    ├── FragmentCustomStations (新增)
    │   ├── CustomStationManager
    │   └── ItemAdapterStation
    └── FragmentStations (修改)
        ├── CustomStationManager
        └── FileDialog
```

## 2. 核心类设计

### 2.1 CustomStationManager (新增)

**类名**: `CustomStationManager`
**包名**: `net.programmierecke.radiodroid2`
**父类**: `StationSaveManager`
**职责**: 管理自定义电台列表的导入、导出和持久化

**核心方法**:

```java
public class CustomStationManager extends StationSaveManager {
    // 构造方法
    public CustomStationManager(Context ctx)

    // 重写保存ID,使用独立的SharedPreferences键
    @Override
    protected String getSaveId()

    // 导入M3U文件
    public void LoadM3U(String filePath, String fileName)

    // 导出M3U文件
    public void SaveM3U(String filePath, String fileName)

    // 批量添加电台
    public void addMultiple(List<DataRadioStation> stations)

    // 清空列表
    public void clear()
}
```

**设计说明**:
- 继承自`StationSaveManager`,复用现有的数据管理逻辑
- 通过重写`getSaveId()`方法,使用独立的存储键`custom_stations`
- 复用父类的`SaveM3U()`和`LoadM3U()`方法,无需重复实现
- 利用父类的`addMultiple()`和`clear()`方法管理列表

### 2.2 FragmentCustomStations (新增)

**类名**: `FragmentCustomStations`
**包名**: `net.programmierecke.radiodroid2.station`
**父类**: `FragmentBase`
**职责**: 显示和管理自定义电台列表的UI

**核心属性和方法**:

```java
public class FragmentCustomStations extends FragmentBase
        implements IFragmentSearchable {

    private RecyclerView recyclerViewStations;
    private SwipeRefreshLayout swiperefresh;
    private ItemAdapterStation stationListAdapter;
    private CustomStationManager customStationManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)

    private void initViews(View view)
    private void loadStations()
    private void setupListeners()
    private void showImportDialog()
    private void showExportDialog()

    // 实现IFragmentSearchable接口
    @Override
    public void setSearchQuery(String query, SearchStyle style)
}
```

**UI布局**:
- 复用现有的`fragment_stations.xml`布局
- 包含RecyclerView显示电台列表
- 包含SwipeRefreshLayout支持下拉刷新
- 工具栏包含"导入"和"导出"按钮

**交互流程**:
1. 用户点击"导入"按钮 → 弹出文件选择器 → 选择M3U文件 → 解析并导入
2. 用户点击"导出"按钮 → 弹出文件保存对话框 → 指定保存位置 → 生成M3U文件
3. 用户点击电台项 → 播放电台
4. 用户长按电台项 → 显示上下文菜单(删除、添加到收藏等)

### 2.3 ActivityMain修改 (修改)

**修改点**:
1. 添加`CustomStationManager`实例
2. 在导航菜单中添加"自定义"标签页
3. 处理文件选择器的回调
4. 更新工具栏菜单项

**核心修改代码**:

```java
public class ActivityMain extends AppCompatActivity implements
        FileDialog.OnFileSelectedListener, ... {

    // 新增: 自定义电台管理器
    private CustomStationManager customStationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化自定义电台管理器
        customStationManager = new CustomStationManager(this);
        // ... 其他初始化代码
    }

    // 修改: 处理文件选择回调
    @Override
    public void onFileSelected(FileDialog dialog, File file) {
        try {
            if (dialog instanceof SaveFileDialog) {
                if (selectedMenuItem == R.id.nav_item_custom) {
                    customStationManager.SaveM3U(file.getParent(), file.getName());
                } else if (selectedMenuItem == R.id.nav_item_starred) {
                    favouriteManager.SaveM3U(file.getParent(), file.getName());
                } else if (selectedMenuItem == R.id.nav_item_history) {
                    historyManager.SaveM3U(file.getParent(), file.getName());
                }
            } else if (dialog instanceof OpenFileDialog) {
                if (selectedMenuItem == R.id.nav_item_custom) {
                    customStationManager.LoadM3U(file.getParent(), file.getName());
                } else {
                    favouriteManager.LoadM3U(file.getParent(), file.getName());
                }
            }
        } catch (Exception e) {
            Log.e("MAIN", e.toString());
        }
    }

    // 修改: 导航菜单选择处理
    private boolean selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_item_custom:
                fragment = new FragmentCustomStations();
                break;
            // ... 其他case
        }
    }
}
```

### 2.4 导航菜单修改

**文件**: `menu_drawer.xml`

**修改内容**: 在`nav_item_stations`之后添加自定义标签页

```xml
<item
    android:id="@+id/nav_item_custom"
    android:icon="@drawable/ic_custom_24dp"
    android:title="@string/nav_item_custom" />

<item
    android:id="@+id/nav_item_multi_search"
    android:icon="@drawable/ic_search_24dp"
    android:title="@string/nav_item_multi_search" />
```

## 3. 数据流设计

### 3.1 导入M3U文件流程

```
用户操作 → FragmentCustomStations → OpenFileDialog
    ↓
用户选择文件 → ActivityMain.onFileSelected()
    ↓
CustomStationManager.LoadM3U()
    ↓
LoadM3UInternal() → AsyncTask后台执行
    ↓
LoadM3UReader() → 解析M3U文件
    ↓
解析每一行:
  - #EXTINF: → 提取电台名称
  - #EXTIMG: → 提取图标URL
  - http:// → 创建DataRadioStation对象
    ↓
返回电台列表 → onPostExecute()
    ↓
addMultiple(result) → 添加到管理器
    ↓
Save() → 持久化到SharedPreferences
    ↓
notifyAllListeners() → 通知UI更新
    ↓
FragmentCustomStations接收更新 → 刷新RecyclerView
```

### 3.2 导出M3U文件流程

```
用户操作 → FragmentCustomStations → SaveFileDialog
    ↓
用户指定保存位置 → ActivityMain.onFileSelected()
    ↓
CustomStationManager.SaveM3U()
    ↓
SaveM3UInternal() → AsyncTask后台执行
    ↓
SaveM3UWriter() → 生成M3U内容
    ↓
写入文件:
  - #EXTM3U
  - 循环每个电台:
    - #RADIOBROWSERUUID:StationUuid
    - #EXTINF:-1,电台名称
    - #EXTIMG:图标URL (如果有)
    - StreamUrl
    - 空行
    ↓
MediaScannerConnection.scanFile() → 通知媒体库
    ↓
显示成功Toast
```

### 3.3 自定义标签页数据加载流程

```
FragmentCustomStations.onCreateView()
    ↓
initCustomStationManager() → 获取CustomStationManager实例
    ↓
loadStations() → 从管理器获取电台列表
    ↓
CustomStationManager.getList()
    ↓
stationListAdapter.submitList() → 更新RecyclerView
    ↓
CustomStationManager.addStationUpdateListener(this) → 注册更新监听
```

## 4. M3U文件格式解析

### 4.1 支持的M3U格式

**标准格式**:
```
#EXTM3U
#EXTINF:-1,电台名称
http://stream.url
```

**扩展格式(带图标)**:
```
#EXTM3U
#EXTINF:-1,电台名称
#EXTIMG:http://icon.url
http://stream.url
```

**多电台示例**:
```
#EXTM3U
#EXTINF:-1,AsiaFM亚洲经典台
#EXTIMG:http://pic.qtfm.cn/2022/0712/20220712122112.jpeg!200
http://goldfm.cn:8000/goldfm

#EXTINF:-1,AsiaFM高清音乐台
http://asiafm.hk:8000/asiahd
```

### 4.2 解析逻辑

参考`StationSaveManager.LoadM3UReader()`方法:

```java
protected List<DataRadioStation> LoadM3UReader(Reader reader) {
    String line;
    String stationName = "";
    String stationUuid = "";
    String stationUrl = "";
    String stationIconUrl = "";

    BufferedReader br = new BufferedReader(reader);
    List<DataRadioStation> resultStations = new ArrayList<>();

    while ((line = br.readLine()) != null) {
        if (line.startsWith(M3U_PREFIX)) {
            // #RADIOBROWSERUUID:xxx
            stationUuid = line.substring(M3U_PREFIX.length()).trim();
        } else if (line.startsWith("#EXTINF:")) {
            // #EXTINF:-1,电台名称
            int commaIndex = line.indexOf(",");
            if (commaIndex >= 0 && commaIndex < line.length() - 1) {
                stationName = line.substring(commaIndex + 1).trim();
            }
        } else if (line.startsWith(M3U_ICON_PREFIX)) {
            // #EXTIMG:图标URL
            stationIconUrl = line.substring(M3U_ICON_PREFIX.length()).trim();
        } else if (line.startsWith("http")) {
            // 流地址
            stationUrl = line.trim();

            if (!stationUrl.isEmpty()) {
                DataRadioStation station = new DataRadioStation();
                station.StationUuid = stationUuid;
                station.Name = stationName;
                station.StreamUrl = stationUrl;
                station.IconUrl = stationIconUrl;

                resultStations.add(station);
            }

            // 重置变量
            stationName = "";
            stationUuid = "";
            stationUrl = "";
            stationIconUrl = "";
        }
    }

    return resultStations;
}
```

### 4.3 生成逻辑

参考`StationSaveManager.SaveM3UWriter()`方法:

```java
public boolean SaveM3UWriter(Writer bw) {
    try {
        bw.write("#EXTM3U\n");
        for (DataRadioStation station : listStations) {
            bw.write(M3U_PREFIX + station.StationUuid + "\n");
            bw.write("#EXTINF:-1," + station.Name + "\n");

            if (station.hasIcon()) {
                bw.write(M3U_ICON_PREFIX + station.IconUrl + "\n");
            }

            bw.write(station.StreamUrl + "\n\n");
        }
        bw.flush();
        return true;
    } catch (Exception e) {
        Log.e("Exception", "File write failed: " + e.toString());
        return false;
    }
}
```

## 5. 数据持久化设计

### 5.1 SharedPreferences存储

**存储键**: `custom_stations`

**存储格式**: JSON数组

**示例数据**:
```json
[
  {
    "StationUuid": "custom-uuid-001",
    "Name": "AsiaFM亚洲经典台",
    "StreamUrl": "http://goldfm.cn:8000/goldfm",
    "IconUrl": "http://pic.qtfm.cn/2022/0712/20220712122112.jpeg!200",
    "HomePageUrl": "",
    "Country": "",
    "CountryCode": "",
    "State": "",
    "TagsAll": "",
    "Language": "",
    "LastChangeTime": "",
    "ClickCount": 0,
    "ClickTrend": 0,
    "Votes": 0,
    "RefreshRetryCount": 0,
    "Bitrate": 0,
    "Codec": "",
    "Working": true,
    "Hls": false,
    "DeletedOnServer": false
  }
]
```

### 5.2 加载和保存逻辑

**加载**:
```java
void Load() {
    listStations.clear();

    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    String str = sharedPref.getString(getSaveId(), null);
    if (str != null) {
        List<DataRadioStation> arr = DataRadioStation.DecodeJson(str);
        for (DataRadioStation station : arr) {
            station.queue = this;
        }
        listStations.addAll(arr);
    }
}
```

**保存**:
```java
void Save() {
    JSONArray arr = new JSONArray();
    for (DataRadioStation station : listStations) {
        arr.put(station.toJson());
    }

    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = sharedPref.edit();
    String str = arr.toString();
    editor.putString(getSaveId(), str);
    editor.commit();
}
```

## 6. 用户界面设计

### 6.1 导航菜单布局

```
┌─────────────────────────────┐
│ ☰ RadioDroid          ⚙️   │
├─────────────────────────────┤
│ 📢 电台列表                 │
│ ⭐ 自定义        ← 新增    │
│ 🔍 多条件搜索               │
│ ⭐ 我的收藏                 │
│ 🕒 历史记录                 │
│ ⏰ 闹钟                     │
├─────────────────────────────┤
│ ⚙️ 设置                     │
└─────────────────────────────┘
```

### 6.2 自定义标签页布局

```
┌─────────────────────────────┐
│ RadioDroid         📥 📤   │
├─────────────────────────────┤
│ 🔍 搜索电台...              │
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ [图标] AsiaFM亚洲经典台  │ │
│ │       http://goldfm...  │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ [图标] AsiaFM高清音乐台  │ │
│ │       http://asiafm...  │ │
│ └─────────────────────────┘ │
│ ...                       │
└─────────────────────────────┘
```

**按钮说明**:
- 📥 导入按钮: 打开文件选择器,选择M3U文件导入
- 📤 导出按钮: 打开文件保存对话框,导出当前列表为M3U文件

### 6.3 文件选择器

**导入文件选择器**:
- 标题: "选择M3U文件"
- 默认路径: /storage/emulated/0/Music/
- 文件过滤: *.m3u, *.m3u8

**导出文件保存对话框**:
- 标题: "保存M3U文件"
- 默认路径: /storage/emulated/0/Music/
- 默认文件名: RadioDroid_20260504_153000.m3u

## 7. 权限和配置

### 7.1 Android权限

**文件**: `AndroidManifest.xml`

**需要的权限**:
```xml
<!-- 外部存储读写权限 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Android 10+ 的分区存储 -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
```

### 7.2 运行时权限请求

在ActivityMain中添加权限请求逻辑:

```java
private static final int REQUEST_STORAGE_PERMISSION = 100;

private void requestStoragePermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_STORAGE_PERMISSION);
        }
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                       int[] grantResults) {
    if (requestCode == REQUEST_STORAGE_PERMISSION) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 权限已授予,继续操作
        } else {
            Toast.makeText(this, "需要存储权限才能导入导出M3U文件",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
```

### 7.3 Gradle依赖

**文件**: `app/build.gradle`

**已有的依赖**(无需添加):
```gradle
// 文件对话框库
implementation 'com.rustamg:file-dialogs:1.0.0'

// JSON解析
implementation 'com.google.code.gson:gson:2.8.9'

// 网络请求
implementation 'com.squareup.okhttp3:okhttp:4.9.3'
```

## 8. 字符串资源

### 8.1 中文字符串

**文件**: `values-zh/strings.xml`

**新增字符串**:
```xml
<!-- 导航菜单 -->
<string name="nav_item_custom">自定义</string>

<!-- 导入导出 -->
<string name="action_import">导入</string>
<string name="action_export">导出</string>
<string name="dialog_import_title">选择M3U文件</string>
<string name="dialog_export_title">保存M3U文件</string>

<!-- 提示消息 -->
<string name="notify_import_now">正在导入: %1s/%2s</string>
<string name="notify_import_ok">成功导入 %1d 个电台: %2s/%3s</string>
<string name="notify_import_failed">导入失败: %1s/%2s</string>
<string name="notify_export_now">正在导出: %1s/%2s</string>
<string name="notify_export_ok">成功导出: %1s/%2s</string>
<string name="notify_export_failed">导出失败: %1s/%2s</string>
<string name="empty_custom_stations">暂无自定义电台</string>
<string name="empty_custom_stations_hint">点击导入按钮导入M3U文件</string>
```

### 8.2 英文字符串

**文件**: `values/strings.xml`

**新增字符串**:
```xml
<string name="nav_item_custom">Custom</string>
<string name="action_import">Import</string>
<string name="action_export">Export</string>
<string name="dialog_import_title">Select M3U File</string>
<string name="dialog_export_title">Save M3U File</string>
<string name="notify_import_now">Importing: %1s/%2s</string>
<string name="notify_import_ok">Successfully imported %1d stations: %2s/%3s</string>
<string name="notify_import_failed">Import failed: %1s/%2s</string>
<string name="notify_export_now">Exporting: %1s/%2s</string>
<string name="notify_export_ok">Successfully exported: %1s/%2s</string>
<string name="notify_export_failed">Export failed: %1s/%2s</string>
<string name="empty_custom_stations">No custom stations</string>
<string name="empty_custom_stations_hint">Tap import button to import M3U file</string>
```

## 9. 图标资源

### 9.1 新增图标

**文件**: `drawable/ic_custom_24dp.xml`

**图标内容**:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M19,3H5C3.89,3 3,3.9 3,5V19C3,20.1 3.89,21 5,21H19C20.1,21 21,20.1 21,19V5C21,3.9 20.1,3 19,3M19,19H5V5H19V19M11,17H13V15H11V17M11,13H13V7H11V13Z" />
</vector>
```

### 9.2 导入导出按钮图标

使用现有的Material Design图标:
- 导入: `@drawable/ic_file_download_24dp` 或类似图标
- 导出: `@drawable/ic_file_upload_24dp` 或类似图标

## 10. 测试策略

### 10.1 单元测试

**CustomStationManager测试**:
- 测试空列表的保存和加载
- 测试添加单个电台
- 测试批量添加电台
- 测试删除电台
- 测试清空列表
- 测试M3U文件的解析
- 测试M3U文件的生成

**M3U解析测试**:
- 测试标准M3U格式
- 测试带图标的M3U格式
- 测试多电台M3U文件
- 测试格式错误的M3U文件
- 测试中文电台名称
- 测试特殊字符处理

### 10.2 集成测试

**导入流程测试**:
1. 测试从文件选择器选择M3U文件
2. 测试导入过程的进度显示
3. 测试导入成功后的UI更新
4. 测试导入失败时的错误处理

**导出流程测试**:
1. 测试打开文件保存对话框
2. 测试导出过程的进度显示
3. 测试导出文件的内容正确性
4. 测试导出成功后的媒体库扫描

**自定义标签页测试**:
1. 测试标签页的显示
2. 测试电台列表的显示
3. 测试电台的播放
4. 测试电台的删除
5. 测试搜索功能
6. 测试下拉刷新

### 10.3 UI测试

**手动测试用例**:
1. 启动应用,检查"自定义"标签页是否显示
2. 点击"自定义"标签页,检查是否正确打开
3. 点击"导入"按钮,检查文件选择器是否打开
4. 选择一个有效的M3U文件,检查是否成功导入
5. 检查导入的电台是否正确显示
6. 点击一个电台,检查是否能够播放
7. 长按一个电台,检查上下文菜单是否显示
8. 选择"删除",检查电台是否被删除
9. 点击"导出"按钮,检查文件保存对话框是否打开
10. 指定保存位置,检查是否成功导出
11. 使用其他播放器打开导出的M3U文件,检查格式是否正确

## 11. 性能优化

### 11.1 异步处理

**导入导出操作**: 使用AsyncTask在后台线程执行,避免阻塞UI

```java
new AsyncTask<Void, Void, List<DataRadioStation>>() {
    @Override
    protected List<DataRadioStation> doInBackground(Void... params) {
        return LoadM3UReader(reader);
    }

    @Override
    protected void onPostExecute(List<DataRadioStation> result) {
        if (result != null) {
            addMultiple(result);
            Toast.makeText(context,
                    "成功导入 " + result.size() + " 个电台",
                    Toast.LENGTH_LONG).show();
        }
    }
}.execute();
```

### 11.2 列表优化

**RecyclerView优化**:
- 使用`DiffUtil`实现高效列表更新
- 启用`setHasFixedSize(true)`提高滚动性能
- 使用`ViewHolder`模式复用视图

### 11.3 内存优化

**大文件处理**:
- 使用BufferedReader逐行读取,避免一次性加载整个文件到内存
- 及时关闭文件流,释放资源

```java
try (BufferedReader br = new BufferedReader(reader)) {
    while ((line = br.readLine()) != null) {
        // 处理每一行
    }
}
```

## 12. 错误处理

### 12.1 导入错误处理

**常见错误**:
- 文件不存在或无法读取
- 文件格式错误
- 编码不支持
- 磁盘空间不足

**处理方式**:
```java
try {
    List<DataRadioStation> stations = LoadM3UInternal(filePath, fileName);
    if (stations != null && !stations.isEmpty()) {
        addMultiple(stations);
        Toast.makeText(context,
                "成功导入 " + stations.size() + " 个电台",
                Toast.LENGTH_LONG).show();
    } else {
        Toast.makeText(context,
                "M3U文件中没有有效的电台数据",
                Toast.LENGTH_LONG).show();
    }
} catch (FileNotFoundException e) {
    Toast.makeText(context,
            "文件不存在: " + fileName,
            Toast.LENGTH_LONG).show();
} catch (IOException e) {
    Toast.makeText(context,
            "读取文件失败: " + e.getMessage(),
            Toast.LENGTH_LONG).show();
} catch (Exception e) {
    Log.e("LOAD", "Import error", e);
    Toast.makeText(context,
            "导入失败: " + e.getMessage(),
            Toast.LENGTH_LONG).show();
}
```

### 12.2 导出错误处理

**常见错误**:
- 目标路径不存在或不可写
- 磁盘空间不足
- 文件名包含非法字符

**处理方式**:
```java
try {
    boolean success = SaveM3UInternal(filePath, fileName);
    if (success) {
        Toast.makeText(context,
                "成功导出到: " + filePath + "/" + fileName,
                Toast.LENGTH_LONG).show();
    } else {
        Toast.makeText(context,
                "导出失败",
                Toast.LENGTH_LONG).show();
    }
} catch (Exception e) {
    Log.e("SAVE", "Export error", e);
    Toast.makeText(context,
            "导出失败: " + e.getMessage(),
            Toast.LENGTH_LONG).show();
}
```

## 13. 兼容性考虑

### 13.1 Android版本兼容

**最低版本**: API 19 (Android 4.4 KitKat)
**目标版本**: API 33 (Android 13)

**兼容性处理**:
- 使用`Build.VERSION.SDK_INT`进行版本判断
- Android 10+使用分区存储(Scoped Storage)
- Android 11+需要`MANAGE_EXTERNAL_STORAGE`权限

### 13.2 分区存储适配

**Android 10+**:
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    // 使用MediaStore API
    ContentValues values = new ContentValues();
    values.put(MediaStore.Audio.Playlists.DATA, filePath);
    values.put(MediaStore.Audio.Playlists.NAME, fileName);
    Uri uri = getContentResolver().insert(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
    // ...
} else {
    // 使用传统文件API
    File file = new File(filePath, fileName);
    // ...
}
```

### 13.3 文件对话框兼容

**项目已使用的库**: `com.rustamg:file-dialogs:1.0.0`

该库已经处理了不同Android版本的兼容性,无需额外处理。

## 14. 实施计划

### 14.1 开发阶段

**阶段1: 数据层开发** (预计1天)
- 创建`CustomStationManager`类
- 实现数据持久化逻辑
- 实现M3U导入导出核心逻辑
- 编写单元测试

**阶段2: UI层开发** (预计1天)
- 创建`FragmentCustomStations`类
- 修改`ActivityMain`添加导航菜单
- 修改`menu_drawer.xml`添加菜单项
- 实现文件选择器集成

**阶段3: 资源和配置** (预计0.5天)
- 添加字符串资源
- 添加图标资源
- 添加权限配置
- 配置文件选择器

**阶段4: 测试和优化** (预计0.5天)
- 进行集成测试
- 进行UI测试
- 性能优化
- Bug修复

### 14.2 里程碑

- **M1**: 数据层完成,能够导入导出M3U文件
- **M2**: UI层完成,能够显示自定义电台列表
- **M3**: 功能集成完成,端到端功能可用
- **M4**: 测试通过,代码可以提交

## 15. 风险和缓解措施

### 15.1 技术风险

**风险1**: M3U文件格式兼容性问题
- **影响**: 某些M3U文件可能无法正确解析
- **概率**: 中
- **缓解措施**: 参考现有的`PlaylistM3U`类,支持标准格式和扩展格式;添加详细的错误日志,便于调试

**风险2**: Android 10+分区存储限制
- **影响**: 导出文件可能无法保存到传统位置
- **概率**: 高
- **缓解措施**: 使用MediaStore API;在运行时检查Android版本,使用不同的API

**风险3**: 大文件导入导致内存溢出
- **影响**: 导入大型M3U文件时应用崩溃
- **概率**: 低
- **缓解措施**: 使用流式读取,逐行解析;限制单次导入的最大电台数量

### 15.2 用户体验风险

**风险1**: 用户不理解如何使用导入导出功能
- **影响**: 功能使用率低
- **概率**: 中
- **缓解措施**: 提供清晰的用户引导;在首次使用时显示提示信息

**风险2**: 导入的电台无法播放
- **影响**: 用户认为功能不可靠
- **概率**: 中
- **缓解措施**: 导入时验证URL格式;提供删除功能,允许用户移除无效电台

## 16. 后续优化方向

### 16.1 功能增强

1. **支持在线M3U导入**: 允许用户通过URL导入在线M3U文件
2. **支持更多格式**: 支持PLS、XSPF等其他播放列表格式
3. **批量编辑**: 支持批量修改电台名称、图标等信息
4. **云端同步**: 支持将自定义电台列表同步到云端

### 16.2 性能优化

1. **增量导入**: 支持增量导入,避免重复导入已存在的电台
2. **后台同步**: 支持后台自动同步M3U文件
3. **缓存优化**: 缓存电台图标,减少网络请求

### 16.3 用户体验优化

1. **拖拽排序**: 支持拖拽调整电台顺序
2. **分组管理**: 支持将电台分组管理
3. **智能推荐**: 根据用户播放历史推荐相似的电台

## 17. 附录

### 17.1 相关文件清单

**新增文件**:
- `app/src/main/java/net/programmierecke/radiodroid2/CustomStationManager.java`
- `app/src/main/java/net/programmierecke/radiodroid2/station/FragmentCustomStations.java`
- `app/src/main/res/drawable/ic_custom_24dp.xml`

**修改文件**:
- `app/src/main/java/net/programmierecke/radiodroid2/ActivityMain.java`
- `app/src/main/res/menu/menu_drawer.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `app/src/main/res/menu/menu_main.xml` (添加导入导出按钮)
- `app/src/main/AndroidManifest.xml` (添加权限)

**参考文件**:
- `app/src/main/java/net/programmierecke/radiodroid2/StationSaveManager.java`
- `app/src/main/java/net/programmierecke/radiodroid2/playlist/PlaylistM3U.java`
- `app/src/main/java/net/programmierecke/radiodroid2/playlist/PlaylistM3UEntry.java`
- `app/src/main/java/net/programmierecke/radiodroid2/station/DataRadioStation.java`

### 17.2 参考资料

1. **M3U格式规范**: https://en.wikipedia.org/wiki/M3U
2. **Android存储最佳实践**: https://developer.android.com/training/data-storage
3. **Android文件访问框架**: https://developer.android.com/guide/topics/providers/document-provider
4. **RadioDroid项目文档**: 项目README.md

### 17.3 术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| M3U | M3U | 一种播放列表文件格式 |
| 导入 | Import | 从外部文件加载数据到应用 |
| 导出 | Export | 将应用数据保存为外部文件 |
| 自定义标签页 | Custom Tab | 用于显示用户导入电台的标签页 |
| 收藏 | Favorites | 用户收藏的电台列表 |
| 历史记录 | History | 播放历史记录 |
| 流地址 | Stream URL | 电台的音频流地址 |
| 图标 | Icon | 电台的图标图片URL |
| SharedPreferences | - | Android的键值对存储机制 |
| RecyclerView | - | Android的高效列表控件 |
| AsyncTask | - | Android的异步任务框架 |

---

**文档版本历史**:

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2026-05-04 | AI | 初始版本 |

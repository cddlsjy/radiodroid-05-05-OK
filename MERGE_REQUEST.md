# Merge Request: 遥控器支持的启动大屏显示功能

## 分支信息
- 源分支：`260502-feat-remote-fullscreen-display`
- 目标分支：`main`

## 功能描述

本次 PR 实现了遥控器支持的启动大屏显示功能，包括：

### 新增功能
1. **启动设置选项** - 在设置中新增"启动时自动进入大屏播放模式"选项，用户可自主选择是否在启动时展开播放器
2. **焦点高亮显示** - 使用淡蓝色主题的焦点高亮效果，符合 Material Design 规范
3. **焦点管理** - 实现焦点初始化和自动恢复，记住上次焦点位置
4. **平滑滚动** - 自定义 FocusSmoothScroller，实现流畅的列表滚动效果
5. **遥控器导航** - 支持遥控器上下键导航和确认键操作

### 技术实现
- 新增 `FocusManager` 焦点管理器
- 新增 `FocusSmoothScroller` 平滑滚动器
- 修改 `ActivityMain` 支持启动设置
- 修改 `FragmentStations` 实现焦点初始化
- 修改列表项支持焦点状态变化

## 文件变更

### 新增文件
- `app/src/main/java/net/programmierecke/radiodroid2/utils/FocusManager.java`
- `app/src/main/java/net/programmierecke/radiodroid2/utils/FocusSmoothScroller.java`
- `app/src/main/res/drawable/focusable_station_item.xml`
- `.monkeycode/specs/260502-remote-fullscreen-display/requirements.md`
- `.monkeycode/specs/260502-remote-fullscreen-display/design.md`

### 修改文件
- `app/src/main/java/net/programmierecke/radiodroid2/ActivityMain.java`
- `app/src/main/java/net/programmierecke/radiodroid2/FragmentTabs.java`
- `app/src/main/java/net/programmierecke/radiodroid2/station/FragmentStations.java`
- `app/src/main/java/net/programmierecke/radiodroid2/station/ItemAdapterStation.java`
- `app/src/main/res/layout/list_item_station.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/preferences.xml`

## 构建说明

### 本地构建
```bash
./gradlew assembleDebug
```

### Gitee CI 构建注意事项

如果在 Gitee CI 构建中遇到 `ClassNotFoundException` 错误，请检查：

1. **Gradle Wrapper 配置**
   - 确认 `gradle/wrapper/gradle-wrapper.jar` 存在且未损坏
   - 确认 `gradle-wrapper.properties` 配置正确

2. **Java 版本**
   - 本项目使用 Java 17
   - 确认 CI 环境的 JAVA_HOME 配置正确

3. **可能的解决方案**
   ```bash
   # 重新生成 gradle wrapper
   gradle wrapper --gradle-version 8.5
   
   # 或者使用系统 gradle 构建
   gradle assembleDebug
   ```

## 测试建议

1. **功能测试**
   - [ ] 设置中启用"自动进入大屏模式"，重启应用验证
   - [ ] 使用遥控器上下键导航，验证焦点高亮
   - [ ] 验证焦点自动滚动功能
   - [ ] 验证焦点位置记忆功能

2. **兼容性测试**
   - [ ] 在 Android TV 设备上测试
   - [ ] 在手机/平板设备上测试遥控器支持
   - [ ] 验证触摸屏操作不受影响

## 相关 Issue

无关联 Issue

## 截图

### 设置界面
（待补充）

### 焦点高亮效果
（待补充）

---

**提交者**: AI Assistant
**提交时间**: 2026-05-02
**Commit Hash**: cd4f4d7

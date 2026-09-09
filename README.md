# Simple Permissions

轻量 Android 运行时权限库（Java）。通过透明 `Activity` + Callback 申请权限，自动适配存储 / 媒体 / 蓝牙 / 通知等版本差异，并处理「拒绝且不再询问」引导跳转设置页。

- **坐标**：`com.gitee.yemesoft:simple-permissions:1.6.1`
- **minSdk**：23（Android 6.0+）
- **compileSdk**：35
- **兼容目标**：Android 7.0 – 当前主流版本；国内主流机型、三星、谷歌

## 特性

- 跨版本权限自动映射 / 升级 / 降级：存储、媒体、蓝牙、通知、精确闹钟、附近 Wi‑Fi、身体活动、电话号码等
- Callback 申请，支持透传 `extras`；结果按**调用方原始权限名**回报
- 区分 **临时拒绝** / **永久拒绝（不再询问）**
- 默认在永久拒绝后弹确认框并跳转应用权限设置（可关闭）
- 默认将传统 `READ/WRITE` 在 Android 11+ 自动升级为 `MANAGE`（可关闭，改走 `READ_MEDIA_*`）
- OEM 设置页适配：小米 / 华为 / 荣耀 / OPPO / realme / 一加 / vivo / iQOO / 魅族；三星与谷歌走应用详情兜底
- 特殊权限：全部文件访问、悬浮窗、安装未知应用、通知开关（API&lt;33）、精确闹钟（走设置页）
- 后台定位在前台定位通过后单独申请
- Android 14 部分相册授权视为媒体可读
- 支持自定义权限说明页（推荐返回任意 View，无 id 约束；未自定义用默认样式）

## 接入

在宿主 `build.gradle` 中添加依赖（按你实际的 Maven 仓库配置）：

```gradle
implementation 'com.gitee.yemesoft:simple-permissions:1.6.1'
```

在 `AndroidManifest.xml` 中声明你要申请的权限（库本身不替宿主声明业务权限）。

## 自定义权限说明页样式

申请权限时顶部的「权限使用说明」可完全自定义。**推荐用 View 回调**，任意布局、任意 id，库只把标题和说明文案传给你；不自定义则用库默认样式。

优先级：单次 Callback 自定义 View → 全局 `PermissionExplainUi` →（可选）布局资源 / 默认 XML。

### 方式一：Callback 返回自定义 View（推荐，无 id 约束）

```java
Permission.request(this, "需要相机权限才能拍照", new Permission.Callback<Void>() {
    @Override
    public View onCreatePermissionExplainView(Activity activity, String title, String description) {
        View view = activity.getLayoutInflater().inflate(R.layout.my_any_layout, null);
        // 用你自己的 id 绑定 title / description 即可
        TextView desc = view.findViewById(R.id.tv_reason);
        if (desc != null) {
            desc.setText(description);
        }
        return view; // 返回 null 则走默认样式
    }

    @Override
    public void onDestroyPermissionExplainView(Activity activity) {
        // 可选：释放动画 / 引用
    }

    @Override
    public void onResult(List<String> granted, List<String> rejected,
                         List<String> permanentlyDenied, Void extras) {
    }
}, Manifest.permission.CAMERA);
```

若不需要说明面板，可返回一个空 `View`：

```java
return new View(activity);
```

### 方式二：全局 PermissionExplainUi

适合全 App 统一皮肤：

```java
Permission.setPermissionExplainUi(new PermissionExplainUi() {
    @Override
    public View onCreateView(Activity activity, String title, String description) {
        View view = LayoutInflater.from(activity).inflate(R.layout.my_permission_banner, null);
        // ...
        return view;
    }
});

// 恢复默认
Permission.setPermissionExplainUi(null);
```

### 方式三：同名资源 / 布局 res（仍走库的默认绑定）

仅当你愿意沿用库的默认 id 绑定时使用：

- 宿主放置 `res/layout/sp_activity_permission.xml` 覆盖，或
- `Permission.setPermissionExplainLayout(R.layout.xxx)`（传 `0` 恢复默认）

此时需保留：`sp_main`、`permission_declare_panel`、`permission_title`、`permission_description`。

## 快速使用

```java
Permission.request(this, "需要相机权限才能拍照", new Permission.Callback<Void>() {
    @Override
    public void onResult(List<String> granted, List<String> rejected, Void extras) {
        // 旧三参数回调：永久拒绝会合并进 rejected
    }
}, Manifest.permission.CAMERA);
```

推荐使用四参数回调，单独处理永久拒绝：

```java
Permission.request(this, "需要相机与麦克风权限", new Permission.Callback<Void>() {
    @Override
    public void onResult(List<String> granted, List<String> rejected,
                         List<String> permanentlyDenied, Void extras) {
        if (!permanentlyDenied.isEmpty()) {
            // 用户勾选了「不再询问」，需去系统设置开启
            return;
        }
        if (rejected.isEmpty()) {
            // 全部通过
        }
    }
}, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);
```

带业务透传参数：

```java
Permission.request(this, "需要定位权限", requestId, new Permission.Callback<String>() {
    @Override
    public void onResult(List<String> granted, List<String> rejected,
                         List<String> permanentlyDenied, String extras) {
        // extras == requestId
    }
}, Manifest.permission.ACCESS_FINE_LOCATION);
```

## 永久拒绝（不再询问）

| API | 说明 |
|-----|------|
| `Permission.setAutoGuideOnPermanentDenied(true/false)` | 默认 `true`：永久拒绝后弹框引导去设置；设为 `false` 则仅回调，由业务自行处理 |
| `Permission.setAutoUpgradeStorageToManage(true/false)` | 默认 `true`：传统 `READ/WRITE` 在 Android 11+ 升级为 `MANAGE`；`false` 则 API 33+ 走 `READ_MEDIA_*`（更适合上架 / 最小权限） |
| `Permission.isPermanentlyDenied(activity, permission)` | 判断某权限是否已永久拒绝 |
| `Permission.openAppPermissionSettings(context)` | 手动打开应用权限设置页（OEM 感知） |

流程简述：

1. 用户拒绝且不再询问 → 进入 `permanentlyDenied`
2. 若开启自动引导 → 确认框（列出权限名）→ 跳转设置页 → 返回后复核并回调
3. 再次 `request` 时会重新向系统核对；已在设置中开启的会直接视为授权

自定义确认框文案 / 样式：覆盖 `Callback.onShowConfirmDialog`。

## 权限检查

```java
if (Permission.hasAllPermissions(context,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO)) {
    // 已具备
}
```

`hasPermission` / `hasAllPermissions` 同样做跨版本等价判断（例如低版本用 `WRITE` 满足 `READ_MEDIA_IMAGES`，高版本用 `READ_MEDIA_*` 或 `MANAGE` 满足传统存储名）。

## 跨版本自动映射（可随意组合）

调用方无需关心 API 差异，可任意混用旧存储名、媒体名、`MANAGE` 等；库会改写成当前设备真正该申请的权限，回调仍返回你传入的原始权限名。

### 存储（默认自动升级 MANAGE）

| 调用方申请 | 行为（`setAutoUpgradeStorageToManage(true)`，默认） | 关闭升级后（`false`） |
|------------|-----------------------------------------------------|------------------------|
| `READ/WRITE_EXTERNAL_STORAGE` | API 30+ → `MANAGE`（所有文件访问设置）；更低 → 传统存储 | API 33+ → `READ_MEDIA_*`；更低 → 传统存储 |
| `READ_MEDIA_*`（未混传 READ/WRITE） | API &lt; 33 → 传统存储；API 33+ 按类型申请 | 同左（**不会**因开关升级为 MANAGE） |
| `MANAGE_EXTERNAL_STORAGE` | API 30+ 设置页；更低 → `READ/WRITE` | 同左 |
| `MANAGE` + `WRITE` 等同批 | 只走 `MANAGE`（去掉不可授予的 `WRITE`，避免误判「不再询问」） | 同左 |

上架 / 仅访问相册视频音频时，建议：

```java
Permission.setAutoUpgradeStorageToManage(false);
```

### 其它常见权限

| 调用方申请 | 低版本 | 高版本 |
|------------|--------|--------|
| `POST_NOTIFICATIONS` | 检测通知总开关；未开则引导通知设置页 | 运行时申请 |
| `SCHEDULE_EXACT_ALARM` | 视为已具备（&lt; API 31） | 确认框 → 精确闹钟设置 |
| 蓝牙经典名 / 新名 | 互转；经典名常附带定位 | SCAN/CONNECT/ADVERTISE |
| `NEARBY_WIFI_DEVICES` | → 定位 | 运行时申请（API 33+） |
| `ACTIVITY_RECOGNITION` | 视为已具备（&lt; Q） | 运行时申请 |
| `READ_PHONE_NUMBERS` | → `READ_PHONE_STATE`（&lt; O） | 运行时申请 |
| `ACCESS_BACKGROUND_LOCATION` | 低于 Q 时按前台定位处理 | 前台通过后再单独申请 |

## 特殊权限路径

| 场景 | 行为 |
|------|------|
| `MANAGE_EXTERNAL_STORAGE` | API 30+ 确认框 → 所有文件访问设置 |
| `SYSTEM_ALERT_WINDOW` | 确认框 → 悬浮窗设置（魅族有专用页） |
| `REQUEST_INSTALL_PACKAGES` | 确认框 → 安装未知应用设置 |
| `POST_NOTIFICATIONS`（API &lt; 33） | 确认框 → 应用通知设置 |
| `SCHEDULE_EXACT_ALARM`（API 31+） | 确认框 → 精确闹钟设置 |
## 注意事项

1. **权限须在宿主 Manifest 中声明**，否则系统会直接拒绝。
2. 申请过程中若进程被系统杀掉，静态回调无法送达（同类库常见限制）；再次进入可重新 `request`。
3. 个别国产机首次普通拒绝也可能 `rationale=false`，可能被判为永久拒绝并多一次设置引导（偏保守，不会卡死）。
4. 库模块自身不做 release 混淆；由宿主 App 决定混淆策略。

## 模块结构

```
simple-permissions/
├── library/          # 可发布 AAR
└── app/              # 演示工程
```

主要入口：`com.dlkw.permissions.Permission`

## License

按仓库现有版权声明使用；悬浮窗相关工具类含 Facishare 历史版权头。

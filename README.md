# SimplePermissions

### 介绍
简化动态权限申请

### 软件架构
AspectJ + 注解


### 使用说明

#### 项目根目录build.gradle配置:
```
buildscript {
    repositories {
        maven {
            url "https://jitpack.io"
        }
    }
    dependencies {
        classpath 'com.hujiang.aspectjx:gradle-android-plugin-aspectjx:2.0.4'
    }
}
```

#### 模块build.gradle配置:
```
apply plugin: 'android-aspectjx'

dependencies {
    implementation 'com.gitee.afterfinal:simple-permissions:1.0.9'
}
```

#### 初始化：
```
Permissions.init(this);
```

#### 注解申请权限：
```
    //execWhenRejected = true，权限被拒绝后，被注解的函数不会执行；
    //execWhenRejected = false，权限被拒绝后，被注解的函数也会执行；
    @RequestPermissions(value = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    }, tipMode = TipMode.Dialog, execWhenRejected = true)
    private void doRequestUsePermissions() {
        Toast.makeText(MainActivity.this, "doRequestUsePermissions", Toast.LENGTH_SHORT).show();
    }
```
如果@RequestPermissions所在的类实现了PermissionRequestCallback接口，则会直接调用PermissionRequestCallback的重写方法
```
class MainActivity extends AppCompatActivity implements PermissionRequestCallback{
    @RequestPermissions(value = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    }, tipMode = TipMode.Dialog)
    private void doRequestUsePermissions() {
        Toast.makeText(MainActivity.this, "doRequestUsePermissions", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionRequestResult(IProceedingJoinPoint joinPoint, List<String> granted, List<String> rejected) {
        try {
            joinPoint.proceed();//此处相当于实际调用doRequestUsePermissions方法
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
```

#### 接口申请权限：
```
       Permissions.request(new Permissions.Callback() {
            @Override
            public void onResult(final List<String> granted, final List<String> rejected) {
                if (0 == rejected.size()) {
                    //权限申请成功
                } else {
                    Toast.makeText(context, "以下权限申请失败:\n" + Permissions.getPermissionNames(rejected, "\n"),Toast.LENGTH_SHORT).show();
                }
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA);
```

#### 更多高级用法请参考源码

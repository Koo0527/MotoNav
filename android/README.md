# 两个独立安卓版本

| 版本 | Android Studio 工程 | 使用场景 |
| --- | --- | --- |
| [普通版 8.3.26](standard/README.md) | `android/standard` | 不需要红绿灯读秒，按原方式自行编译、签名、申请高德 Key |
| [读秒版 8.3.26-single.30](countdown/README.md) | `android/countdown` | 需要读秒实验功能，完整构建入口自动下载并校验兼容数据组件 |

两套工程分别维护签名和本地配置，不能互相覆盖源码目录。根目录 `一键打包.bat` 默认构建普通版；读秒版用 `一键打包-读秒版.bat`。完整区别与依赖见各自说明。

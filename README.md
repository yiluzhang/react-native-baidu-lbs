# react-native-baidu-lbs

百度地图定位 SDK (Android: 9.6.4, iOS: 2.1.3) 简易封装，支持 Android 和 iOS 平台。
运行 example 需要将包名改为自己的应用包名。

## Installation

```sh
yarn add react-native-baidu-lbs
```

## Usage

```js
import BaiduLbs from 'react-native-baidu-lbs';

// 初始化
BaiduLbs.init({ android: 'xxx', ios: 'xxx' });

// 设置参数，必须在 init 之后 start 之前调用
BaiduLbs.setOption({ coorType: 'gcj02' });

// 监听位置信息
BaiduLbs.addLocationListener(location => console.log(location));

// 开始定位
BaiduLbs.start();

// 停止定位
BaiduLbs.stop();

// 销毁
BaiduLbs.destroy();

```

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

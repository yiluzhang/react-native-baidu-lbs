import { NativeEventEmitter, NativeModules, Platform, type EmitterSubscription } from 'react-native';

const { BaiduLbs } = NativeModules;

export interface Option {
  coorType?: 'gcj02' | 'bd09ll'; // 坐标类型，默认 bd09ll
  reGeocode?: boolean; // 逆地理编码，默认 true
  locationMode?: number; // 定位模式，0: 高精度，1: 低功耗，2: 仅设备，3: 模糊定位，默认高精度，只在 Android 上生效
  scanSpan?: number; // 扫描间隔，小于 1000 不生效，默认 1000ms，只在 Android 上生效
}

export interface Poi {
  id: string;
  name: string;
  tags: string;
  addr: string;
  rank?: number; // 只在 Android 上生效
  relaiability?: number; // 只在 iOS 上生效
}

export interface Location {
  errorCode: number;
  errorMessage?: string;
  iosErrorCode?: number;
  latitude?: number;
  longitude?: number;
  altitude?: number;
  speed?: number;
  direction?: number;
  radius?: number; // 只在 Android 上生效
  coorType?: string; // bd09ll | gcj02
  timestamp?: number;
  mockProbability?: number; // 只在 iOS 上生效
  provider?: number; // 只在 iOS 上生效
  mockGnssProbability?: number; // 只在 Android 上生效
  gnssProvider?: string; // 只在 Android 上生效
  country?: string;
  countryCode?: string;
  province?: string;
  city?: string;
  cityCode?: string;
  district?: string;
  town?: string;
  street?: string;
  streetNumber?: string;
  adcode?: string;
  address?: string;
  locationDescribe?: string;
  locType?: number; // 只在 Android 上生效
  locTypeDescription?: string; // 只在 Android 上生效
  poiList?: Poi[];
}

type BaiduLbsType = {
  // 初始化
  init(key: { android: string; ios: string }): Promise<boolean>;
  // 设置参数，必须在 init 之后 start 之前调用
  setOption(option: Option): void;
  // 开始监听位置，执行前需要确保已经获取了定位权限
  start(): void;
  // 停止监听位置
  stop(): void;
  // 销毁
  destroy(): void;
  // 位置变化时会触发此事件
  addLocationListener(listener: (event: Location) => void): EmitterSubscription;
  // 获取两点距离，坐标类型为 bd09ll，返回单位为米
  getDistance(point1: { latitude: number; longitude: number }, point2: { latitude: number; longitude: number }): Promise<number>;
};

const emitter = new NativeEventEmitter(BaiduLbs);

const addLocationListener: BaiduLbsType['addLocationListener'] = (listener) => emitter.addListener('onBaiduLocation', listener);

const BaiduLbsModule: BaiduLbsType = {
  init: (key: { android: string; ios: string }) => BaiduLbs.init(Platform.select(key)),
  setOption: BaiduLbs.setOption,
  start: BaiduLbs.start,
  stop: BaiduLbs.stop,
  destroy: BaiduLbs.destroy,
  addLocationListener,
  getDistance: BaiduLbs.getDistance,
};

export default BaiduLbsModule;

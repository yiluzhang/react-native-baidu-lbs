import React, { useEffect, useState } from 'react';
import { Dimensions, PermissionsAndroid, Platform, StatusBar, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import BaiduLbs, { type Location } from 'react-native-baidu-lbs';
import dayjs from 'dayjs';

export const { width: SCREEN_WIDTH } = Dimensions.get('window');

// TODO 百度地图应用 AK
const key = {
  android: 'xxx',
  ios: 'xxx',
};

const requestPermission = async () => {
  if (Platform.OS === 'android') {
    try {
      const granted = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
      ]);

      const hasFine = granted[PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION] === PermissionsAndroid.RESULTS.GRANTED;
      const hasCoarse = granted[PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION] === PermissionsAndroid.RESULTS.GRANTED;

      return hasFine || hasCoarse;
    } catch (err) {
      console.warn(err);
      return false;
    }
  }

  return;
};

function App(): React.JSX.Element {
  const [isStarted, setIsStarted] = useState(false);
  const [reGeocode, setReGeocode] = useState(true);
  const [location, setLocation] = useState<Location>();

  useEffect(() => {
    const sub = BaiduLbs.addLocationListener(setLocation);
    BaiduLbs.init(key);

    return () => {
      sub.remove();
    };
  }, []);

  const start = async () => {
    await requestPermission();
    BaiduLbs.start();
    setIsStarted(true);
  };

  const stop = () => {
    BaiduLbs.stop();
    setIsStarted(false);
    setLocation(undefined);
  };

  const toggleReGeocode = () => {
    setReGeocode(!reGeocode);
    BaiduLbs.setOption({ reGeocode: !reGeocode });
    if (isStarted) {
      stop();
      start();
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#fff" />
      <View style={styles.tipContainer}>
        {location?.errorCode === 0 && (
          <>
            <Text style={{ fontSize: 22, paddingBottom: 4, color: '#000', textAlign: 'center' }}>{dayjs(location.timestamp).format('HH:mm:ss')}</Text>
            <Text style={styles.txt}>经度: {location.longitude}</Text>
            <Text style={styles.txt}>纬度: {location.latitude}</Text>
            <Text style={styles.txt}>海拔: {location.altitude}</Text>
            <Text style={styles.txt}>速度: {location.speed}</Text>
            <Text style={styles.txt}>方向: {location.direction}</Text>
            <Text style={styles.txt}>{location.address}</Text>
            <Text style={styles.txt}>{location.locationDescribe}</Text>
            {!!location.poiList?.length && (
              <View style={{ marginTop: 12, marginBottom: 6, marginHorizontal: 16, width: SCREEN_WIDTH - 64, height: 1, backgroundColor: '#f0f0f0' }} />
            )}
            {location.poiList?.map((o: { id: string; name: string }) => (
              <Text key={o.id} style={[styles.txt, styles.center, { fontSize: 14 }]}>
                {o.name}
              </Text>
            ))}
          </>
        )}
        {location && location.errorCode !== 0 ? (
          <>
            <Text style={styles.txt}>
              错误码: {location.iosErrorCode}&nbsp;{location.locType}
            </Text>
            <Text style={styles.txt}>
              {location.errorMessage}
              {location.locTypeDescription}
            </Text>
          </>
        ) : null}
      </View>
      {isStarted ? (
        <TouchableOpacity onPress={stop} style={styles.btn}>
          <Text style={styles.txt}>停止定位</Text>
        </TouchableOpacity>
      ) : (
        <TouchableOpacity onPress={start} style={styles.btn}>
          <Text style={styles.txt}>开始定位</Text>
        </TouchableOpacity>
      )}
      <TouchableOpacity onPress={toggleReGeocode} style={styles.btn}>
        <Text style={styles.txt}>{reGeocode && '不'}获取地址</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  tipContainer: {
    height: 360,
    width: '100%',
    paddingHorizontal: 18,
  },
  center: {
    textAlign: 'center',
  },
  btn: {
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 18,
    width: 180,
    height: 50,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
  },
  txt: {
    fontSize: 16,
    lineHeight: 20,
    color: '#000',
  },
});

export default App;

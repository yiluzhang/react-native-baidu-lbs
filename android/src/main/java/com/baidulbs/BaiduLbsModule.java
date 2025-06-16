package com.baidulbs;

import android.location.Location;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;

import com.baidu.location.Poi;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

public class BaiduLbsModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private LocationClient locationClient;
    private BDAbstractLocationListener locationListener;

    public BaiduLbsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Nonnull
    @Override
    public String getName() {
        return "BaiduLbs";
    }

    @ReactMethod
    public void init(String appKey, Promise promise) {
        try {
            LocationClient.setAgreePrivacy(true);
            LocationClient.setKey(appKey);
            locationClient = new LocationClient(reactContext);
            locationClient.setLocOption(getLocationClientOption());

            if (locationListener != null) {
                locationClient.unRegisterLocationListener(locationListener);
            }

            locationListener = new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation bdLocation) {
                    emitLocation(bdLocation);
                }
            };
            locationClient.registerLocationListener(locationListener);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("-1", e.getMessage(), e);
        }
    }

    @ReactMethod
    public void setOption(ReadableMap option) {
        if (locationClient == null) return;

        LocationClientOption opt = getLocationClientOptionFromMap(option);
        locationClient.setLocOption(opt);
    }

    @ReactMethod
    public void getDistance(ReadableMap point1, ReadableMap point2, Promise promise) {
      try {
        double lat1 = point1.getDouble("latitude");
        double lon1 = point1.getDouble("longitude");
        double lat2 = point2.getDouble("latitude");
        double lon2 = point2.getDouble("longitude");

        float[] result = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, result);
        promise.resolve((double) result[0]); // 距离（米）
      } catch (Exception e) {
        promise.reject("DISTANCE_ERROR", e.getMessage(), e);
      }
    }

    private LocationClientOption getLocationClientOptionFromMap(ReadableMap map) {
        LocationClientOption option = getLocationClientOption();

        if (map.hasKey("locationMode")) {
            int mode = map.getInt("locationMode");
            LocationClientOption.LocationMode[] modes = {
                    LocationClientOption.LocationMode.Hight_Accuracy,
                    LocationClientOption.LocationMode.Battery_Saving,
                    LocationClientOption.LocationMode.Device_Sensors,
                    LocationClientOption.LocationMode.Fuzzy_Locating
            };

            if (mode >= 1 && mode <= 3) {
                option.setLocationMode(modes[mode]);
            } else {
                option.setLocationMode(modes[0]);
            }
        }

        if (map.hasKey("scanSpan") && map.getInt("scanSpan") > 1000) {
            option.setScanSpan(map.getInt("scanSpan"));
        }

        if (map.hasKey("coorType")) {
            if ("gcj02".equals(map.getString("coorType"))) {
                option.setCoorType("gcj02");
            } else {
                option.setCoorType("bd09ll");
            }
        }

        if (map.hasKey("reGeocode")) {
            boolean bool = map.getBoolean("reGeocode");
            option.setIsNeedAddress(bool);
            option.setIsNeedLocationDescribe(bool);
            option.setIsNeedLocationPoiList(bool);
        }

        return option;
    }

    @ReactMethod
    public void start() {
        if (locationClient != null && !locationClient.isStarted()) {
            locationClient.start();
        }
    }

    @ReactMethod
    public void stop() {
        if (locationClient != null) {
            locationClient.stop();
        }
    }

    @ReactMethod
    public void destroy() {
        if (locationClient != null) {
            locationClient.stop();

            if (locationListener != null) {
                locationClient.unRegisterLocationListener(locationListener);
                locationListener = null;
            }
            locationClient = null;
        }
    }

    @Nonnull
    private static LocationClientOption getLocationClientOption() {
        LocationClientOption locationOption = new LocationClientOption();
        locationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        locationOption.setCoorType("bd09ll");
        locationOption.setScanSpan(1000);
        locationOption.setOpenGnss(true);
        locationOption.setIsNeedAltitude(true);
        locationOption.setNeedDeviceDirect(true);
        locationOption.setIsNeedAddress(true);
        locationOption.setIsNeedLocationDescribe(true);
        locationOption.setIsNeedLocationPoiList(true);
        return locationOption;
    }

    private void emitLocation(BDLocation location) {
        WritableMap map = Arguments.createMap();
        try {
            int locType = location.getLocType();
            map.putInt("errorCode", locType == 61 || locType == 161 ? 0 : -1);
            map.putDouble("latitude", location.getLatitude());
            map.putDouble("longitude", location.getLongitude());
            map.putDouble("altitude", location.getAltitude());
            map.putDouble("speed", location.getSpeed());
            map.putDouble("direction", location.getDirection());
            map.putDouble("radius", location.getRadius());
            try {
                String timeStr = location.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                Date date = sdf.parse(timeStr);
                if (date != null) {
                    map.putDouble("timestamp", date.getTime());
                }
            } catch (Exception e) {
                map.putDouble("timestamp", System.currentTimeMillis());
            }
            if (location.hasAddr()) {
                map.putString("country", location.getCountry());
                map.putString("countryCode", location.getCountryCode());
                map.putString("province", location.getProvince());
                map.putString("city", location.getCity());
                map.putString("cityCode", location.getCityCode());
                map.putString("district", location.getDistrict());
                map.putString("town", location.getTown());
                map.putString("street", location.getStreet());
                map.putString("streetNumber", location.getStreetNumber());
                map.putString("adcode", location.getAdCode());
                map.putString("address", location.getAddrStr());
                map.putString("locationDescribe", location.getLocationDescribe());
            }

            map.putInt("locType", locType);
            map.putString("locTypeDescription", location.getLocTypeDescription());
            map.putString("coorType", location.getCoorType());
            map.putInt("mockGnssProbability", location.getMockGnssProbability());
            map.putString("gnssProvider", location.getGnssProvider());

            if (location.getPoiList() != null) {
                List<Poi> poiList = location.getPoiList();
                WritableArray poiArray = Arguments.createArray();
                for (int i = 0; i < poiList.size(); i++) {
                    WritableMap poiMap = Arguments.createMap();
                    Poi poi = poiList.get(i);
                    poiMap.putString("id", poi.getId());
                    poiMap.putDouble("rank", poi.getRank());
                    poiMap.putString("name", poi.getName());
                    poiMap.putString("tags", poi.getTags());
                    poiMap.putString("addr", poi.getAddr());
                    poiArray.pushMap(poiMap);
                }
                map.putArray("poiList", poiArray);
            }
        } catch (Exception e) {
            map.putInt("errorCode", -2);
            map.putString("errorMessage", e.getLocalizedMessage());
        } finally {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onBaiduLocation", map);
        }
    }
}

#import "BaiduLbs.h"
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTConvert.h>
#import <BMKLocationKit/BMKLocationAuth.h>
#import <BMKLocationKit/BMKLocationManager.h>

@interface BaiduLbs () <RCTBridgeModule, BMKLocationAuthDelegate, BMKLocationManagerDelegate>

@property (nonatomic, strong) BMKLocationManager *locationManager;

@end

@implementation BaiduLbs

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents {
  return @[@"onBaiduLocation"];
}

- (void)ensureLocationManager {
  if (!self.locationManager) {
    self.locationManager = [[BMKLocationManager alloc] init];
    self.locationManager.delegate = self;
    self.locationManager.coordinateType = BMKLocationCoordinateTypeBMK09LL;
    self.locationManager.desiredAccuracy = kCLLocationAccuracyBest;
    self.locationManager.locatingWithReGeocode = YES;
  }
}

RCT_EXPORT_METHOD(init:(NSString *)appKey resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  dispatch_async(dispatch_get_main_queue(), ^{
    if (self.locationManager) {
      [self.locationManager stopUpdatingLocation];
      self.locationManager.delegate = nil;
      self.locationManager = nil;
    }

    [[BMKLocationAuth sharedInstance] setAgreePrivacy:YES];
    [[BMKLocationAuth sharedInstance] checkPermisionWithKey:appKey authDelegate:self];
    [self ensureLocationManager];
    resolve(@(YES));
  });
}

RCT_EXPORT_METHOD(setOption:(NSDictionary *)option) {
  if (!self.locationManager) {
    return;
  }

  if (option[@"coorType"]) {
    NSString *type = [RCTConvert NSString:option[@"coorType"]];
    if ([type isEqualToString:@"gcj02"]) {
      self.locationManager.coordinateType = BMKLocationCoordinateTypeGCJ02;
    } else {
      self.locationManager.coordinateType = BMKLocationCoordinateTypeBMK09LL;
    }
  }

  if (option[@"reGeocode"]) {
    self.locationManager.locatingWithReGeocode = [RCTConvert BOOL:option[@"reGeocode"]];
  }
}

RCT_EXPORT_METHOD(start) {
  if (self.locationManager) {
    [self.locationManager startUpdatingLocation];
  }
}

RCT_EXPORT_METHOD(stop) {
  if (self.locationManager) {
    [self.locationManager stopUpdatingLocation];
  }
}

RCT_EXPORT_METHOD(destroy) {
  if (self.locationManager) {
    [self.locationManager stopUpdatingLocation];
    self.locationManager.delegate = nil;
    self.locationManager = nil;
  }
}

RCT_EXPORT_METHOD(getDistance:(NSDictionary *)point1
                  point2:(NSDictionary *)point2
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  @try {
    double lat1 = [point1[@"latitude"] doubleValue];
    double lon1 = [point1[@"longitude"] doubleValue];
    double lat2 = [point2[@"latitude"] doubleValue];
    double lon2 = [point2[@"longitude"] doubleValue];

    CLLocation *loc1 = [[CLLocation alloc] initWithLatitude:lat1 longitude:lon1];
    CLLocation *loc2 = [[CLLocation alloc] initWithLatitude:lat2 longitude:lon2];

    CLLocationDistance distance = [loc1 distanceFromLocation:loc2];
    resolve(@(distance)); // 距离（米）
  }
  @catch (NSException *exception) {
    reject(@"DISTANCE_ERROR", exception.reason, nil);
  }
}

#pragma mark - BMKLocationManagerDelegate

- (void)BMKLocationManager:(BMKLocationManager *)manager
         didUpdateLocation:(BMKLocation *)location
                   orError:(NSError *)error {
  if (error) {
    [self sendEventWithName:@"onBaiduLocation" body:@{ @"errorCode": @(-2), @"iosErrorCode": @(error.code), @"errorMessage": error.localizedDescription }];
    return;
  }

  CLLocation *core = location.location;
  BMKLocationReGeocode *rgc = location.rgcData;
  
  NSMutableDictionary *data = [NSMutableDictionary dictionary];
  data[@"errorCode"] = @(0);
  data[@"latitude"] = @(core.coordinate.latitude);
  data[@"longitude"] = @(core.coordinate.longitude);
  data[@"altitude"] = @(core.altitude);
  data[@"speed"] = @(core.speed);
  data[@"direction"] = @(core.course);
  NSString *coordType = @"bd09ll";
  if (self.locationManager.coordinateType == BMKLocationCoordinateTypeGCJ02) {
    coordType = @"gcj02";
  }
  data[@"coorType"] = coordType;
  data[@"timestamp"] = @((long long)([core.timestamp timeIntervalSince1970] * 1000));
  data[@"mockProbability"] = @(location.mockProbability);
  data[@"provider"] = @(location.provider);

  if (rgc) {
    data[@"country"] = rgc.country ?: @"";
    data[@"countryCode"] = rgc.countryCode ?: @"";
    data[@"province"] = rgc.province ?: @"";
    data[@"city"] = rgc.city ?: @"";
    data[@"cityCode"] = rgc.cityCode ?: @"";
    data[@"district"] = rgc.district ?: @"";
    data[@"town"] = rgc.town ?: @"";
    data[@"street"] = rgc.street ?: @"";
    data[@"streetNumber"] = rgc.streetNumber ?: @"";
    data[@"adcode"] = rgc.adCode ?: @"";
    data[@"address"] = rgc.address ?: @"";
    data[@"locationDescribe"] = rgc.locationDescribe ?: @"";

    if (rgc.poiList && rgc.poiList.count > 0) {
      NSMutableArray *poiArray = [NSMutableArray array];
      for (BMKLocationPoi *poi in rgc.poiList) {
        NSMutableDictionary *poiMap = [NSMutableDictionary dictionary];
        poiMap[@"id"] = poi.uid ?: @"";
        poiMap[@"name"] = poi.name ?: @"";
        poiMap[@"tags"] = poi.tags ?: @"";
        poiMap[@"addr"] = poi.addr ?: @"";
        poiMap[@"relaiability"] = @(poi.relaiability);
        [poiArray addObject:poiMap];
      }
      data[@"poiList"] = poiArray;
    }
  }

  [self sendEventWithName:@"onBaiduLocation" body:data];
}

@end

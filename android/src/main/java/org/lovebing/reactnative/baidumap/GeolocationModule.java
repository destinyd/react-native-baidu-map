package org.lovebing.reactnative.baidumap;

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

/**
 * Created by lovebing on 2016/10/28.
 */
public class GeolocationModule extends BaseModule
        implements BDLocationListener, OnGetGeoCoderResultListener {

    private LocationClient locationClient;
    private static GeoCoder geoCoder;

    public GeolocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
    }

    public String getName() {
        return "BaiduGeolocationModule";
    }


    private void initLocationClient() {
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setIsNeedAltitude(true);
        option.setIsNeedLocationDescribe(true);
        option.setOpenGps(true);
        locationClient = new LocationClient(context.getApplicationContext());
        locationClient.setLocOption(option);
        Log.i("locationClient", "locationClient");
        locationClient.registerLocationListener(this);
    }
    /**
     *
     * @return
     */
    protected GeoCoder getGeoCoder() {
        if(geoCoder != null) {
            geoCoder.destroy();
        }
        geoCoder = GeoCoder.newInstance();
        geoCoder.setOnGetGeoCodeResultListener(this);
        return geoCoder;
    }

    /**
     *
     * @param sourceLatLng
     * @return
     */
    protected LatLng getBaiduCoorFromGPSCoor(LatLng sourceLatLng) {
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(sourceLatLng);
        LatLng desLatLng = converter.convert();
        return desLatLng;

    }

    @ReactMethod
    public void getCurrentPosition() {
        if(locationClient == null) {
            initLocationClient();
        }
        Log.i("getCurrentPosition", "getCurrentPosition");
        locationClient.start();
    }
    @ReactMethod
    public void geocode(String city, String addr) {
        getGeoCoder().geocode(new GeoCodeOption()
                .city(city).address(addr));
    }

    @ReactMethod
    public void reverseGeoCode(double lat, double lng) {
        getGeoCoder().reverseGeoCode(new ReverseGeoCodeOption()
                .location(new LatLng(lat, lng)));
    }

    @ReactMethod
    public void reverseGeoCodeGPS(double lat, double lng) {
        getGeoCoder().reverseGeoCode(new ReverseGeoCodeOption()
                .location(getBaiduCoorFromGPSCoor(new LatLng(lat, lng))));
    }

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        WritableMap params = Arguments.createMap();
        if(bdLocation.getLocType() == BDLocation.TypeServerError){
            params.putString("error", "服务端网络定位失败");
            sendEvent("onGetCurrentLocationPosition", params);
        } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkException) {
            params.putString("error", "网络不同导致定位失败，请检查网络是否通畅");
            sendEvent("onGetCurrentLocationPosition", params);
        } else if (bdLocation.getLocType() == BDLocation.TypeCriteriaException) {
            params.putString("error", "无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            sendEvent("onGetCurrentLocationPosition", params);
        } else {
            params.putDouble("latitude", bdLocation.getLatitude());
            params.putDouble("longitude", bdLocation.getLongitude());
            params.putDouble("direction", bdLocation.getDirection());
            params.putDouble("altitude", bdLocation.getAltitude());
            params.putDouble("radius", bdLocation.getRadius());
            params.putString("address", bdLocation.getAddrStr());
            params.putString("countryCode", bdLocation.getCountryCode());
            params.putString("country", bdLocation.getCountry());
            params.putString("province", bdLocation.getProvince());
            params.putString("cityCode", bdLocation.getCityCode());
            params.putString("city", bdLocation.getCity());
            params.putString("district", bdLocation.getDistrict());
            params.putString("street", bdLocation.getStreet());
            params.putString("streetNumber", bdLocation.getStreetNumber());
            params.putString("buildingId", bdLocation.getBuildingID());
            params.putString("buildingName", bdLocation.getBuildingName());
            Log.i("onReceiveLocation", "onGetCurrentLocationPosition");
            sendEvent("onGetCurrentLocationPosition", params);
        }
        locationClient.stop();
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        WritableMap params = Arguments.createMap();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            params.putInt("errcode", -1);
        }
        else {
            params.putDouble("latitude",  result.getLocation().latitude);
            params.putDouble("longitude",  result.getLocation().longitude);
        }
        sendEvent("onGetGeoCodeResult", params);
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        WritableMap params = Arguments.createMap();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            params.putInt("errcode", -1);
        }
        else {
            ReverseGeoCodeResult.AddressComponent addressComponent = result.getAddressDetail();
            params.putString("address", result.getAddress());
            params.putString("province", addressComponent.province);
            params.putString("city", addressComponent.city);
            params.putString("district", addressComponent.district);
            params.putString("street", addressComponent.street);
            params.putString("streetNumber", addressComponent.streetNumber);
        }
        sendEvent("onGetReverseGeoCodeResult", params);
    }
}

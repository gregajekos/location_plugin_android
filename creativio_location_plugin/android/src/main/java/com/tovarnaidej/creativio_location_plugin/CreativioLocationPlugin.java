package com.tovarnaidej.creativio_location_plugin;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.Log;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * CreativioLocationPlugin
 */
public class CreativioLocationPlugin implements MethodCallHandler, EventChannel.StreamHandler {
    public static final String STARTLISTENING = "startUpdatingLocation";
    public static final String STOPLISTENING = "stopUpdatingLocation";
    public static final String CURRENTLOCATION = "currentLocation";


    /**
     * Plugin registration.
     */
    Activity context;
    MethodChannel methodChannel;
    EventChannel eventChannel;
    EventChannel.EventSink event;
    public BackgroundService gpsService;
    private final PluginRegistry.Registrar registrar;
    private BroadcastReceiver chargingStateChangeReceiver;
    Map<String, Object> lastLocation = null;

    public static void registerWith(final Registrar registrar) {


        setChannels(registrar);


        //setChannels(registrar);
    }

    public static void setChannels(Registrar registrar) {
        Log.d("perms", "perms checked");

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "creativio_location_plugin");
        final EventChannel eventChannel =
                new EventChannel(registrar.messenger(), "creativio_location_plugin_stream");
        CreativioLocationPlugin instance = new CreativioLocationPlugin(registrar.activity(), channel, eventChannel, registrar);
        channel.setMethodCallHandler(instance);

        eventChannel.setStreamHandler(instance);
    }

    public CreativioLocationPlugin(Activity activity, MethodChannel methodChannel, EventChannel eventChannel, Registrar registrar) {
        this.context = activity;
        this.methodChannel = methodChannel;
        this.methodChannel.setMethodCallHandler(this);
        this.registrar = registrar;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else {
            if (call.method.equals(STARTLISTENING)) {
                Dexter.withActivity(context)
                        .withPermissions(Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {

                                Intent myService = new Intent(context, BackgroundService.class);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(myService);
                                } else {
                                    context.startService(myService);
                                }


                                context.bindService(myService, serviceConnection, Context.BIND_AUTO_CREATE);
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
                        }).check();

                result.success("Listening to location updates");
            } else {
                if (call.method.equals(STOPLISTENING)) {

                    if (gpsService != null) {
                        gpsService.stopTracking();
                        context.unbindService(serviceConnection);
                    }
                    result.success("Stopped listening location updates");
                } else {
                    if (call.method.equals(CURRENTLOCATION)) {
                        if (lastLocation != null) {
                            result.success(lastLocation);
                        }
                    } else {
                        result.notImplemented();
                    }
                }
            }

        }
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.contains("BackgroundService")) {
                Log.i("", "service connected");

                gpsService = ((BackgroundService.LocationServiceBinder) service).getService();

                gpsService.startTracking();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("BackgroundService")) {
                gpsService = null;
            }
        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @java.lang.Override
    public void onListen(java.lang.Object o, EventChannel.EventSink eventSink) {
        chargingStateChangeReceiver = createChargingStateChangeReceiver(eventSink);
        this.registrar
                .context()
                .registerReceiver(
                        chargingStateChangeReceiver, new IntentFilter(BackgroundService.INTENT_ACTION));


    }

    @java.lang.Override
    public void onCancel(java.lang.Object o) {
        registrar.context().unregisterReceiver(chargingStateChangeReceiver);
        chargingStateChangeReceiver = null;


    }

    private BroadcastReceiver createChargingStateChangeReceiver(final EventChannel.EventSink eventSink) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Map<String, Object> map = new HashMap<>();

                if (intent.hasExtra("lon")) {
                    Log.d("key", "1");
                    map.put("longitude", intent.getDoubleExtra("lon", 0));
                }
                if (intent.hasExtra("lat")) {
                    Log.d("key", "2");

                    map.put("latitude", intent.getDoubleExtra("lat", 0));
                }

                if (intent.hasExtra("alt")) {
                    Log.d("key", "3");

                    map.put("altitude", intent.getDoubleExtra("alt", 0));
                }

                if (intent.hasExtra("speed")) {
                    Log.d("key", "4");

                    map.put("speed", intent.getFloatExtra("speed", 0));
                }

                if (intent.hasExtra("acc")) {
                    Log.d("key", "5");

                    map.put("accuracy", intent.getFloatExtra("acc", 0));
                }

                if (intent.hasExtra("timestamp")) {
                    Log.d("key", "6 " + System.currentTimeMillis());

                    map.put("timestamp", System.currentTimeMillis());
                }
                lastLocation = map;
                eventSink.success(map);
            }
        };
    }


}

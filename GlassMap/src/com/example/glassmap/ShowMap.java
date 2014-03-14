package com.example.glassmap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import java.util.*;

import android.content.Context;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationService;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapOptions.MapType;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import android.location.LocationManager;


public class ShowMap extends Activity {
	
	MapView mMapView;
	GraphicsLayer mGraphicsLayer;
	LocationService mLocationService;
	LocationManager mLocationManager;
	GPSLocationManager mGPSLocationManager;
	
	private GestureDetector mGestureDetector;
	private GestureDetector createGestureDetector(Context context) {
	    GestureDetector gestureDetector = new GestureDetector(context);
	        //Create a base listener for generic gestures
	        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
	            @Override
	            public boolean onGesture(Gesture gesture) {
	            	if (gesture == Gesture.SWIPE_RIGHT) {
	                    // do something on right (forward) swipe, zoom in
	            		mMapView.zoomout();
	                    return true;
	                } else if (gesture == Gesture.SWIPE_LEFT) {
	                    // do something on left (backwards) swipe, zoom out
	                	mMapView.zoomin();
	                    return true;
	                } else if (gesture == Gesture.TWO_TAP) {
	                	//disable the auto zoom feature
	                	Log.i("ZoomON", "Auto Zoom: "+mGPSLocationManager.auto_zoom);
	                	mGPSLocationManager.auto_zoom = !mGPSLocationManager.auto_zoom;
	                	Log.i("ZoomON", "Auto Zoom: "+mGPSLocationManager.auto_zoom);
	                }
	            	
	                return false;
	            }
	        });
	        return gestureDetector;
	    }
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_map_layout);
		
		// Retrieve the map and map options from XML layout
		mMapView = (MapView)findViewById(R.id.map);
		//Add the basemap
		ArcGISTiledMapServiceLayer basemap = new ArcGISTiledMapServiceLayer(this.getResources().getString(R.string.WORLD_TOPO));
		mMapView.addLayer(basemap);
		
		//Add graphics layer
		mGraphicsLayer = new GraphicsLayer();
		mMapView.addLayer(mGraphicsLayer);
		
		//create our location manager
		mGPSLocationManager = new GPSLocationManager(mMapView, mGraphicsLayer, this);
		mGPSLocationManager.start();
		
		//create gesture detector so that we can zoom the map in and out
		mGestureDetector = createGestureDetector(this);
		
	}
	
	@Override
    public boolean onGenericMotionEvent(MotionEvent event) {
		//Fire the gesture detector on d-pad motion events.
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
	

	@Override
	protected void onPause() {
		super.onPause();
		mMapView.pause();
		mGPSLocationManager.stop();
	}

	@Override
	protected void onResume() {
		super.onResume(); 
		mMapView.unpause();
		mGPSLocationManager.start();
	}
	

}

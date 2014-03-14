package com.example.glassmap;

import java.util.List;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
//import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GPSLocationManager implements LocationListener, SensorEventListener {
	private final float[] mRotationMatrix;
	private final float[] mOrientation;
	
	private static final int ARM_DISPLACEMENT_DEGREES = 6;
	
	MapView mMapView;
	GraphicsLayer mGraphicsLayer;
	Context mContext;
	Location mCurrentLocation;
	GeomagneticField mGeomagneticField;
	
	LocationManager mLocationManager;
	SensorManager mSensorManager;
	
	public boolean auto_zoom = true;
	
	
	public GPSLocationManager(MapView mMapView, GraphicsLayer mGraphicsLayer, Context mContext) {
		this.mMapView = mMapView;
		this.mGraphicsLayer = mGraphicsLayer;
		this.mContext = mContext;
		
		mRotationMatrix = new float[16];
        mOrientation = new float[9];
		
		//setup a location listener
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
	    //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
	    
	    
	}
	
	/**
	 * Starts the location manager.  This method will connect to all available provides based on 
	 * the criteria: No Accuracy, Need altitude, no Bearing, no Speed.
	 */
	public void start() {
		
		//Buid the criteria object so that we can get the best location provider
		Criteria criteria = new Criteria();
	    criteria.setAccuracy(Criteria.NO_REQUIREMENT);
	    criteria.setAltitudeRequired(true);
	    criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);

	    List<String> providers = mLocationManager.getProviders(criteria, true);
		
	    //Loop over providers, add listeners and call update location.
	    for (String provider : providers) {
	    	Log.i("GPSLocationManager", "Provider "+ provider);
	    	mLocationManager.requestLocationUpdates(provider, 0, 0, this);
	        Location tmpLocation = mLocationManager.getLastKnownLocation(provider);
	        if (tmpLocation != null)
	        {
	        	this.updateLocation(tmpLocation);
	        }
	        else {
	        	Log.i("GPSLocationManager", "No location for provider");
	        }
	    }
	    
	    //register sensor listener
	    mSensorManager.registerListener(this, 
	    		mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), 
	    		SensorManager.SENSOR_DELAY_UI);
	}
	
	public void stop() {
		this.mLocationManager.removeUpdates(this);
		this.mSensorManager.unregisterListener(this);
	}
	
	
	private void updateLocation(Location location) {
		this.mCurrentLocation = location;
		double latitude = location.getLatitude();
    	double longitude = location.getLongitude();
    	Log.i("GPSLocationManager", "Latitude: " + latitude + ", Longitude: " + longitude);
    	Log.i("GPSLocationManager", "Current Scale: "+this.mMapView.getScale());
    	Log.i("GPSLocationManager", "Current Resolution: "+this.mMapView.getResolution());
    	Point p = GeometryEngine.project(longitude, latitude, this.mMapView.getSpatialReference());
    	Graphic g = new Graphic(p, new SimpleMarkerSymbol(Color.RED, 25, SimpleMarkerSymbol.STYLE.CIRCLE));

    	mGraphicsLayer.removeAll();
    	mGraphicsLayer.addGraphic(g);
    	
    	if (this.auto_zoom) {
    		mMapView.zoomToScale(p, 6000);
    	}
    	else {
    		mMapView.centerAt(p, true);
    	}
	}
	
	/**
	 * Gets the Geomagnetic field object using the last saved location.  This is used to calculate the declination
	 */
	private void updateGeomagneticField() {
        this.mGeomagneticField = new GeomagneticField((float) this.mCurrentLocation.getLatitude(),
        		(float) mCurrentLocation.getLongitude(), (float) mCurrentLocation.getAltitude(),
        		mCurrentLocation.getTime());
    }
	
	private float computeTrueNorth(float heading) {
        if (mGeomagneticField != null) {
            return heading + mGeomagneticField.getDeclination();
        } else {
            return heading;
        }
    }
	
	/**
     * Calculates {@code a mod b} in a way that respects negative values (for example,
     * {@code mod(-1, 5) == 4}, rather than {@code -1}).
     *
     * @param a the dividend
     * @param b the divisor
     * @return {@code a mod b}
     */
    public static float mod(float a, float b) {
        return (a % b + b) % b;
    }

	@Override
	public void onLocationChanged(Location location) {
		this.updateLocation(location);
		this.updateGeomagneticField();
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            //Get the current heading from the sensor
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            
            //Log.i("GPSLocationManager-heading", "ort: "+mOrientation[0]);

            // Convert the heading (which is relative to magnetic north) to one that is
            // relative to true north, using the user's current location to compute this.
            float magneticHeading = (float) Math.toDegrees(mOrientation[0]);
            float mHeading = mod(computeTrueNorth(magneticHeading), 360.0f) - ARM_DISPLACEMENT_DEGREES; 
            //Log.i("GPSLocationManager-heading", "heading: "+mHeading);
            //Set the map's rotation angle to the true north heading.
            this.mMapView.setRotationAngle(mHeading);
            

        }
		
	}

}

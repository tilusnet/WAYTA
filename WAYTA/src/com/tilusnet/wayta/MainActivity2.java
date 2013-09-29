/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tilusnet.wayta;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity2 extends FragmentActivity {
	private static MainActivity2 instance;

	private static final String LOG_TAG = "WAYTA";

	//	private static final String SERVICE_URL = "http://172.16.17.20:6060/geonames/?query=London";
	private static final String SERVICE_URL = "http://192.168.0.13:6060/geonames/?query=";

	protected EditText editText;
	protected GoogleMap map;
	private String errMsg;

	private final Map<Long, MarkerOptions> markers = new HashMap<Long, MarkerOptions>();

	public MainActivity2() {
		instance = this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		editText = (EditText) findViewById(R.id.editText);
		editText.setText("London is not Birmingham.");
		setUpMapIfNeeded();
		allowWayta();
	}

	private void allowWayta() {
		if (map != null) {
			Button button = (Button) findViewById(R.id.btnClavin);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					launchClavin();
				}
			});
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpMapIfNeeded();
	}

	private void setUpMapIfNeeded() {
		if (map == null) {
			map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		}
	}

	private void launchClavin() {
		// Retrieve the city data from the web service
		// In a worker thread since it's a network operation.

		map.clear();
		markers.clear();

		// Hide soft keyboard
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

		Toast.makeText(getApplicationContext(), "Querying...", Toast.LENGTH_SHORT).show();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					queryClavin();
				} catch (IOException e) {
					errMsg = "Cannot retrieve cities";
					Log.e(LOG_TAG, errMsg, e);
					//					Toast.makeText(instance, errMsg, Toast.LENGTH_SHORT).show();
					return;
				}
			}
		}).start();
	}

	protected void queryClavin() throws IOException {
		HttpURLConnection conn = null;
		final StringBuilder json = new StringBuilder();
		try {
			// Connect to the web service
			String query = SERVICE_URL + URLEncoder.encode(editText.getEditableText().toString(), "utf-8");
			Log.i(LOG_TAG, "Querying " + query);
			URL url = new URL(query);
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			// Read the JSON data into the StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				json.append(buff, 0, read);
			}
		} catch (IOException e) {
			errMsg = "Error connecting to service";
			Log.e(LOG_TAG, errMsg, e);
			throw new IOException(errMsg, e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		// Create markers for the query data; also highlight matches in text
		// Must run this on the UI thread since it's a UI operation.
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					createMarkersFromJson(json.toString());
					zoomToArea();
					highlightText(json.toString());
				} catch (JSONException e) {
					Log.e(LOG_TAG, "Error processing JSON", e);
					//					Toast.makeText(MainActivity2.instance, errMsg, Toast.LENGTH_SHORT).show();
				}
			}
		});

		// 
	}

	protected void highlightText(String json) throws JSONException {
		JSONArray jsonArray = new JSONArray(json);
		Spannable sps = editText.getText();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = jsonArray.getJSONObject(i);
			JSONObject jsonGeoName = jsonObj.getJSONObject("geoname");
			String matchedName = jsonObj.getString("matchedName");
			JSONObject jsonLocation = jsonObj.getJSONObject("location");
			int pos = jsonLocation.getInt("position");
			double confid = jsonObj.getDouble("confidence");
			final long id = jsonGeoName.getLong("geonameID");
			sps.setSpan(new WordSpan(0, (int) (200 * confid) + 10) {
				//			sps.setSpan(new ClickableSpan() {
				@Override
				public void onClick(View widget) {
					map.animateCamera(CameraUpdateFactory.newLatLngZoom(markers.get(id).getPosition(), 9));
				}
			}, pos, pos + matchedName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			//			}, i * 10 + 0, i * 10 + 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		}
		//		editText.setEnabled(false);
	}

	void zoomToArea() {
		LatLngBounds.Builder bldr = LatLngBounds.builder();
		for (MarkerOptions mo : markers.values()) {
			LatLng pnt = mo.getPosition();
			bldr.include(pnt);
		}
		if (markers.size() == 1)
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(markers.values().toArray(new MarkerOptions[0])[0].getPosition(), 9));
		else
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(bldr.build(), 200));
	}

	void createMarkersFromJson(String json) throws JSONException {
		// De-serialize the JSON string into an array of city objects
		JSONArray jsonArray = new JSONArray(json);
		map.clear();
		markers.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			// Create a marker for each city in the JSON data.
			JSONObject jsonObj = jsonArray.getJSONObject(i);
			JSONObject jsonGeoName = jsonObj.getJSONObject("geoname");
			JSONObject jsonLocation = jsonObj.getJSONObject("location");
			String pop = Integer.toString(jsonGeoName.getInt("population"));
			String country = jsonGeoName.getString("primaryCountryName");
			String pos = Integer.toString(jsonLocation.getInt("position"));
			String matchedName = jsonObj.getString("matchedName");
			String confid = Integer.toString(jsonObj.getInt("confidence"));
			long id = jsonGeoName.getLong("geonameID");
			// @formatter:off
			MarkerOptions markOpt = new MarkerOptions()
				.title(jsonGeoName.getString("name"))
//				.snippet(String.format("Population: %s\nCountry: %s\nMatched name: %s\nOffset: %s\nConfidence: %s", pop, country, matchedName, pos, confid))
				.position(new LatLng(jsonGeoName.getDouble("latitude"), jsonGeoName.getDouble("longitude")));
			// @formatter:on
			markers.put(id, markOpt);
			map.addMarker(markOpt);
		}
	}
}
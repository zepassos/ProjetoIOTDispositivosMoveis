package com.example.sensores;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    SensorManager mSensorManager;
    Sensor mLuz, mProx;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Location location;
    private Address endereco;
    double latitude, longitude;
    String Pais = "", Cidade = "",  Estado = "", Bairro = "", Rua = "";
    float luminosidade = 0;
    float proximidade = 0;
    int temperatura = 0;
    int umidade = 0;
    JSONObject jsonEnvio = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        LinearLayout ll = findViewById(R.id.layoutt);
        ll.setBackgroundColor(getResources().getColor(R.color.minhaCor));*/

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        try {
            MontarJson();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(!thread.isAlive()){
            thread.start();
        }

    }

    public void MontarJson() throws JSONException {

        jsonEnvio.put("Pais", Pais);
        jsonEnvio.put("Cidade", Cidade);
        jsonEnvio.put("Estado", Estado);
        jsonEnvio.put("Bairro", Bairro);
        jsonEnvio.put("Rua", Rua);
        jsonEnvio.put("Latitude", Double.toString(latitude));
        jsonEnvio.put("Longitude", Double.toString(longitude));
        jsonEnvio.put("Temperatura", Integer.toString(temperatura));
        jsonEnvio.put("Umidade", Integer.toString(umidade));
        jsonEnvio.put("Proximidade", Float.toString(proximidade));
        jsonEnvio.put("Luminosidade", Float.toString(luminosidade));

    }

    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try  {
                while(true){
                    Log.i("Thread","PASSEI NA Thread!!!");
                    if (jsonEnvio.length() > 0)
                    {
                        Log.i("Thread","ENVIANDO JSON E AGUARDANDO 30s para o próximo!!!");
                        post(jsonEnvio.toString());
                        //SystemClock.sleep(30000);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    public void TEMPERATURA(View view) throws JSONException {

        temperatura = (int)(Math.random() * 40 ) + 1;
        umidade = (int)(Math.random() * 75 ) + 1;

        jsonEnvio.put("Temperatura", Integer.toString(temperatura));
        jsonEnvio.put("Umidade", Integer.toString(umidade));

    }

    public void LUMINOSIDADE(View view) {
        mLuz = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(new LuzSensor(), mLuz, SensorManager.SENSOR_DELAY_FASTEST);
    }

    class LuzSensor implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {

            luminosidade = event.values[0];

            try {

                jsonEnvio.put("Luminosidade", Float.toString(luminosidade));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.i("Sensores","Luminosidade: " + luminosidade);
        }
    }

    public void PROXIMIDADE(View view) {
        mProx = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(new ProxSensor(), mProx, SensorManager.SENSOR_DELAY_FASTEST);
    }

    class ProxSensor implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {

            proximidade = event.values[0];

            try {

                jsonEnvio.put("Proximidade", Float.toString(proximidade));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.i("Sensores","Proximidade: "+ proximidade);
        }
    }

    public void LOCALIZAR(View view) {
        pedirPermissao();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode){
            case 1: {
                if(grantResults.length > 0 &&
                        grantResults[0]==PackageManager.PERMISSION_GRANTED)
                    configurarServico();
                else
                    Toast.makeText(this, "Não vai funcionar!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pedirPermissao(){
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, 1);
        }
        else
            configurarServico();
    }

    public void configurarServico(){
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    try {
                        atualizar(location);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {  }

                @Override
                public void onProviderDisabled(String provider) {  }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        }catch(SecurityException ex){
        }
    }

    public void atualizar(Location location) throws IOException, JSONException {

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        endereco = buscarEndereco(latitude, longitude);

        Pais     = endereco.getCountryName();
        Cidade   = endereco.getLocality();
        Estado   = endereco.getAdminArea();
        Bairro   = endereco.getSubAdminArea();
        Rua      = endereco.getAddressLine(0);

        jsonEnvio.put("Pais", Pais);
        jsonEnvio.put("Cidade", Cidade);
        jsonEnvio.put("Estado", Estado);
        jsonEnvio.put("Bairro", Bairro);
        jsonEnvio.put("Rua", Rua);
        jsonEnvio.put("Latitude", Double.toString(latitude));
        jsonEnvio.put("Longitude", Double.toString(longitude));

    }

    public Address buscarEndereco(double latitude, double longitude) throws IOException {

        Geocoder geocoder;
        Address address = null;
        List<Address> addresses;

        geocoder = new Geocoder(getApplicationContext());

        addresses = geocoder.getFromLocation(latitude,
                longitude, 1);
        if(addresses.size() > 0){
            address = addresses.get(0);
        }
        return address;

    }

    public String post(String JSON) throws IOException {

        OkHttpClient client = new OkHttpClient();

        String url = "https://locationcellapi.azurewebsites.net/api/LocationSensor";

        Request.Builder builder = new Request.Builder();

        builder.url(url);

        Log.e("JSON", JSON);
        String Teste = "{'Latitude':'200','Longitude':'300'}";

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, JSON);
        builder.post(body);

        Request request = builder.build();

        Response response = client.newCall(request).execute();

        String jsonDeResposta = response.body().string();
        Log.e("POST", jsonDeResposta);
        return jsonDeResposta;

    }

    public String get() throws IOException {

        String url = "https://locationcellapi.azurewebsites.net/api/Location";

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(url).build();

        Response response = client.newCall(request).execute();

        String jsonDeResposta = response.body().string();
        Log.e("GET", jsonDeResposta);
        return jsonDeResposta;
    }

}

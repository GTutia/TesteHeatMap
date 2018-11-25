package com.example.gabri.testeheatmap;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedList;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.List;



public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    DynamoDBMapper dynamoDBMapper;
    List<LatLng> listaCoordenadas = null;
    GoogleMap mMap;
    HeatmapTileProvider mProvider;
    TileOverlay mOverlay;
    AmazonDynamoDBClient dynamoDBClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        AWSMobileClient.getInstance().initialize(this).execute();

        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();
        AWSConfiguration configuration = AWSMobileClient.getInstance().getConfiguration();

        // Add code to instantiate a AmazonDynamoDBClient
        dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);

        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(configuration)
                .build();

        listaCoordenadas =  queryLocation();

        Button bt = findViewById(R.id.button);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adicionaHeatMap(listaCoordenadas);
            }
        });
    }

    private void adicionaHeatMap(List<LatLng> lista){
        mProvider = new HeatmapTileProvider.Builder().data(lista).build();
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        Log.d("Problema", "Entrei aqui");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney, Australia, and move the camera.
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.5580,-46.7361),12));
    }

    public ArrayList<LatLng> queryLocation() {
        final ArrayList<LatLng> lista = new ArrayList<LatLng>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                PacientesDO news = new PacientesDO();
                news.setUserId("0");

                Condition rangeKeyCondition = new Condition()
                        .withComparisonOperator(ComparisonOperator.GT)
                        .withAttributeValueList(new AttributeValue().withN("0"));

                DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression().withHashKeyValues(news)
                        .withRangeKeyCondition("Location", rangeKeyCondition)
                        .withConsistentRead(false);

                PaginatedList<PacientesDO> result = dynamoDBMapper.query(PacientesDO.class, queryExpression);

                // Loop through query results
                for (int i = 0; i < result.size(); i++) {
                    double latitude = result.get(i).getLatitude();
                    double longitude = result.get(i).getLongitude();
                    lista.add(new LatLng(latitude,longitude));
                }
                if (result.isEmpty()) {
                    // There were no items matching your query.
                }
            }
        }).start();
    Log.d("MENSAGEM","Terminei");
        return lista;
    }
}

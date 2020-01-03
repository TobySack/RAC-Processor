package com.example.racprocesser;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class RACActivity extends AppCompatActivity {

    String itemStore, itemNumber, itemDes, itemService;
    EditText textStore, textNumber, textDes, textService, textFinal;
    Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_rac_layout);

        textStore = (EditText) findViewById(R.id.textStoreNumber);
        textNumber = (EditText) findViewById(R.id.textItemNumber);
        textDes = (EditText) findViewById(R.id.textItemDes);
        textService = (EditText) findViewById(R.id.textService);
        textFinal = (EditText) findViewById(R.id.textFinalEval);

        saveButton = (Button) findViewById(R.id.saveButton);

        Intent receivedIntent = getIntent();
        itemStore = receivedIntent.getStringExtra("storenum");
        textStore.setText(itemStore);
        itemNumber = receivedIntent.getStringExtra("itemnum");
        textNumber.setText(itemNumber);
        itemDes = receivedIntent.getStringExtra("itemdes");
        textDes.setText(itemDes);
        itemService = receivedIntent.getStringExtra("itemservice");
        textService.setText(itemService);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemToSheet();
            }
        });
    }

    private void addItemToSheet() {
        final ProgressDialog loading = ProgressDialog.show(this,"Adding Item","Please wait");
        //final String name = "Test1";
        //final String brand = "Test2";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbxk_S-nClOnZwPa0t6LLrhyPkZMAc6EvcJYlVnFQJttWGhSt80i/exec",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        loading.dismiss();
                        Log.d("Sheets", response);
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        finish();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> parmas = new HashMap<>();

                //here we pass params
                parmas.put("action","addItem");
                parmas.put("itemStore", itemStore);
                parmas.put("itemNumber", itemNumber);
                parmas.put("itemDes", itemDes);
                parmas.put("itemService", itemService);
                parmas.put("itemFinal", textFinal.getText().toString());

                return parmas;
            }
        };

        int socketTimeOut = 50000; //50 seconds

        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }
}

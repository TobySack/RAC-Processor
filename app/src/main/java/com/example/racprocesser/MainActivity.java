package com.example.racprocesser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button captureButton, scanButton;
    private ImageView imageView;
    private TextView textView;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap imageBitmap;
    ProgressDialog loading;
    String currentPhotoPath, itemStore, itemNumber, itemDescription, itemService;
    File finalPhotoURI;

//    HttpTransport transport = AndroidHttp.newCompatibleTransport();
//    JsonFactory factory = JacksonFactory.getDefaultInstance();
//    final Sheets sheetsService = new Sheets.Builder(transport, factory, null)
//            .setApplicationName("My Awesome App")
//            .build();
//    final String spreadsheetId = Config.spreadsheet_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        captureButton = (Button) findViewById(R.id.capture_image);
        scanButton = (Button) findViewById(R.id.scan_image);
        imageView = (ImageView) findViewById(R.id.image_view);
        textView = (TextView) findViewById(R.id.text_detected);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
                textView.setText("");
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //textView.setText("Scanning...");
                loading = ProgressDialog.show(MainActivity.this,"Scanning...","Please wait");
                scanForText();
            }
        });

//        new Thread() {
//            @Override
//            public void run() {
//                try {
//                    String range = "Sheet1!A1:B2";
//                    ValueRange result = sheetsService.spreadsheets().values()
//                            .get(spreadsheetId, range)
//                            .setKey(Config.google_api_key)
//                            .execute();
//                    int numRows = result.getValues() != null ? result.getValues().size() : 0;
//                    Log.d("Sheets SUCCESS.", "rows retrieved " + numRows);
//                    Log.d("Sheets rows", result.getValues().toString());
//                }
//                catch (IOException e) {
//                    Log.e("Sheets failed", e.getLocalizedMessage());
//                }
//            }
//        }.start();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.racprocesser.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                finalPhotoURI = photoFile;
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            //imageBitmap = (Bitmap) extras.get("data");
            //imageView.setImageBitmap(imageBitmap);

            Log.d("MainActivity", finalPhotoURI.toString());

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            imageBitmap = BitmapFactory.decodeFile(finalPhotoURI.toString(), options);
            imageView.setImageBitmap(imageBitmap);
            imageView.setRotation(90);
        }
    }

    private void scanForText() {
        if(imageBitmap != null) {
            FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
            FirebaseVisionDocumentTextRecognizer firebaseVisionTextRecognizer = FirebaseVision.getInstance().getCloudDocumentTextRecognizer();
            Task<FirebaseVisionDocumentText> result =
                    firebaseVisionTextRecognizer.processImage(firebaseVisionImage)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText >() {
                                @Override
                                public void onSuccess(FirebaseVisionDocumentText firebaseVisionText) {
                                    displayText(firebaseVisionText);
                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                                            Log.d("MainActivity", "Error: " + e.getMessage());
                                        }
                                    });
        } else {
            loading.dismiss();
            toastMessage("No image was captured.");
        }
    }

    private void displayText(FirebaseVisionDocumentText firebaseVisionText) {
        List<FirebaseVisionDocumentText.Block> blockList = firebaseVisionText.getBlocks();

        if(blockList.size() == 0) {
            Toast.makeText(this, "No text found in image.", Toast.LENGTH_SHORT).show();
            loading.dismiss();
        } else {
            ArrayList<String> array = new ArrayList<String>();
            for(FirebaseVisionDocumentText.Block block : firebaseVisionText.getBlocks()) {
                //String text = block.getText();
                if(block.getText().contains("Store ") && block.getText().contains("Store Name")) {
                    String lines[] = block.getText().split("\\r?\\n");
                    for(int i = 0; i < lines.length; i++) {
                        if(lines[i].contains("Store ")) {
                            if(checkNumbers(lines[i])) {
                                itemStore = lines[i].substring(6);
                                //textView.setText(lines[i].substring(6));
                            }
                        }
                    }
                    array.add(block.getText());
                }

                if(block.getText().contains("Item #")) {
                    String lines[] = block.getText().split("\\r?\\n");
                    for(int i = 0; i < lines.length; i++) {
                        if(lines[i].contains("Item #")) {
                            itemNumber = lines[i].replaceAll("[^\\d.]", "");
                            //textView.setText(lines[i].replaceAll("[^\\d.]", ""));
                        }
                    }
                }

                if(block.getText().contains("Item Description")) {
                    String lines[] = block.getText().split("\\r?\\n");
                    for(int i = 0; i < lines.length; i++) {
                        if(lines[i].contains("Item Description")) {
                            itemDescription = lines[i].substring(18);
                            //textView.setText(lines[i].substring(18));
                        }
                    }
                }

                if(block.getText().contains("Service Request")) {
                    String lines[] = block.getText().split("\\r?\\n");
                    for(int i = 0; i < lines.length; i++) {
                        if(lines[i].contains("Service Request")) {
                            itemService = lines[i + 1];
                            //textView.setText(lines[i + 1]);
                        }
                    }
                }
            }
            //textView.setText(array.toString());
            Log.d("MainActivity", array.toString());

            startReviewIntent(itemStore, itemNumber, itemDescription, itemService);
        }
    }

    private void startReviewIntent(String itemStore, String itemNumber, String itemDescription, String itemService) {
        Intent intent = new Intent(this, RACActivity.class);
        intent.putExtra("storenum", itemStore);
        intent.putExtra("itemnum", itemNumber);
        intent.putExtra("itemdes", itemDescription);
        intent.putExtra("itemservice", itemService);
        imageView.setImageDrawable(null);
        textView.setText("");
        loading.dismiss();
        startActivity(intent);
    }

    private static boolean checkNumbers(String string) {
        int numberOfNumbers = 0;
        for (int i = 0; i < string.length(); i++) {
            if (Character.isDigit(string.charAt(i))) {
                numberOfNumbers++;

                if (numberOfNumbers > 4) {
                    return true;
                }
            }
        }

        if(numberOfNumbers > 4 && numberOfNumbers < 7) {
            return true;
        } else {
            return false;
        }
    }

    public void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}


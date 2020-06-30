package com.example.hashapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 2;
    private FirebaseStorage firebaseStorage;
    private FirebaseDatabase firebaseDatabase;
    private Uri uri;                            // URI's are actually urls meant for local storage
    private TextView filename;
    private String name;
    private ProgressBar progressBar;
    private TextView progress_status;
    private String sha256Hash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseStorage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        Button browse = findViewById(R.id.browse);
        Button upload = findViewById(R.id.upload);
        filename = findViewById(R.id.filename);
        progressBar = findViewById(R.id.progressBar);
        progress_status = findViewById(R.id.progress_status);

        progressBar.setVisibility(View.GONE);
        progress_status.setVisibility(View.GONE);
        filename.setVisibility(View.VISIBLE);

        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    showFileChooser();
                else
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 45);
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (uri != null)
                    UploadFile();
                else
                    Toast.makeText(MainActivity.this, "Select a file", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 45 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            showFileChooser();
        else
            Toast.makeText(MainActivity.this, "Please provide permission", Toast.LENGTH_LONG).show();
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            uri = data.getData();               //return uri of selected file
            String path = null;
            if (uri != null) {
                path = uri.getPath();
            }
            if (path != null) {
                name = path.substring(path.lastIndexOf("/") + 1);
            }
            name = name.substring((name.indexOf(":")) + 1);
            filename.setText(name);

            try {
                HashGenerator();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else
            Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_LONG).show();

    }

    private void HashGenerator() throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("sha-256");
        InputStream inputStream = getContentResolver().openInputStream(uri);


        byte[] databytes = new byte[1024];
        int nread;
        if (inputStream != null) {
            while ((nread = inputStream.read(databytes)) != -1)
                md.update(databytes, 0, nread);
        }

        byte[] messagedigest = md.digest();
        StringBuilder hexString = new StringBuilder();

        for (byte b : messagedigest) {
            String h = Integer.toHexString(0xFF & b);
            String StrComplete = PrependValue(h);
            hexString.append(StrComplete);
        }
        sha256Hash = hexString.toString();
    }

    private String PrependValue(String iStr) {
        StringBuilder sReturnedStr = new StringBuilder(iStr);
        while (sReturnedStr.length() < 2)
            sReturnedStr.insert(0, "0");
        return sReturnedStr.toString();
    }

    private void UploadFile() {
        StorageReference storageReference = firebaseStorage.getReference();          //returns root path

        progressBar.setVisibility(View.VISIBLE);
        progress_status.setVisibility(View.VISIBLE);

        storageReference.child("Uploads").child(name).putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressBar.setVisibility(View.GONE);
                progress_status.setVisibility(View.GONE);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Uploading Failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressBar.setProgress((int) progress);
                progress_status.setText((int) progress + "%");
            }
        });

        name = name.substring(0, name.indexOf("."));
        DatabaseReference databaseReference = firebaseDatabase.getReference();
        databaseReference.child("Hash Values").child(name).setValue(sha256Hash);
    }
}
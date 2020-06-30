package com.example.hashapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileInputStream;
import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 2;
    private Button browse, upload;
    private FirebaseStorage firebaseStorage;
    private FirebaseDatabase firebaseDatabase;
    private Uri uri;                            // URI's are actually urls meant for local storage
    private TextView filename;
    private String name, path;
    private ProgressBar progressBar;
    private TextView progress_status;
    private FileInputStream file;
    private String sha256Hash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseStorage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        browse = findViewById(R.id.browse);
        upload = findViewById(R.id.upload);
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
            path = uri.getPath();
            name = path.substring(path.lastIndexOf("/") + 1);
            name = name.substring((name.indexOf(":"))+1);
            filename.setText(name);

            try {
                HashGenerator();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else
            Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_LONG).show();

    }

    private void HashGenerator()throws Exception {
        Toast.makeText(MainActivity.this, "Generating Hash", Toast.LENGTH_LONG).show();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        file = new FileInputStream(path);
        byte[] dataBytes = new byte[1024];

        int nread = 0;

        while((nread = file.read(dataBytes)) != -1)
            md.update(dataBytes, 0, nread);
        Toast.makeText(MainActivity.this, "File Not Found!", Toast.LENGTH_LONG).show();

        byte[] mdbytes = md.digest();

        //convert the byte to hex format method
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++)
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));

        sha256Hash = sb.toString();
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

                //Toast.makeText(MainActivity.this, "File Successfully Uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Uploading Failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressBar.setProgress((int) progress);
                progress_status.setText((int) progress + "%");
            }
        });

        name = name.substring(0, name.indexOf("."));
        DatabaseReference databaseReference = firebaseDatabase.getReference();
        databaseReference.child("Hash Values").child(name).setValue(sha256Hash + " Hash");

        /*storageReference.child("Hash Values").child(sha256Hash).putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "Hash Generated Successfully", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Hash Generation Failed!", Toast.LENGTH_LONG).show();
            }
        });*/

        /*.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(MainActivity.this, "Hash Generated Successfully", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Hash Generation Failed", Toast.LENGTH_LONG).show();
            }
        })*/
    }
}
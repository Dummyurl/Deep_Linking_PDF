package com.manavjain.sharepdf;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private EditText editText;

    private Pdf pdf;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching Deep Link...");
        progressDialog.setCancelable(false);

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    Uri deepLink;
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.getLink();
                        Log.d(TAG, deepLink.toString());
                        String pdfName = deepLink.getQueryParameter(Pdf.PDF_NAME_KEY);
                        pdfName = Utils.getDecodedString(pdfName) + ".pdf";

                        ((TextView) findViewById(R.id.pdf_name_textview)).setText(pdfName);
                        openPDFWithName(pdfName);
                        //pdf = new Pdf(uri, pdfName, this);

                        //Log.d("link_pdf_uri", uri.toString());
                    }
                })
                .addOnFailureListener(this, e -> Log.w(TAG, "getDynamicLink:onFailure", e));
    }

    private void openPDFWithName(String pdfName) {
        FilenameFilter filenameFilter = (file, s) -> s.equals(pdfName) || s.equals(Utils.getEncodedString(pdfName));

        String downloadPath = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).toString();
        Log.d(TAG, downloadPath);

        Uri uri = Uri.parse(downloadPath + "/" + Utils.getEncodedString(pdfName));

        pdf = new Pdf(uri, pdfName, this);
        Log.d(TAG, uri.toString());

//        File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
//        if (files != null && files.length > 0) {
//            File pdfFile = files[0];
//            Uri uri = Uri.fromFile(pdfFile);
//            pdf = new Pdf(uri, pdfName, this);
//            Log.d(TAG, uri.toString());
//        }
    }

    public void select(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, 1);
    }

    public void onClick(View view) {
        if (pdf == null) {
            Toast.makeText(this, "Please select a PDF to sharePDF", Toast.LENGTH_SHORT).show();
            return;
        }
        String packageName = null;
        switch (view.getId()) {
            case R.id.whatsapp_button:
                packageName = "com.whatsapp";
                progressDialog.show();
                break;

            case R.id.facebook_button:
                // Add messenger SDK
                break;

            case R.id.others_button:
                break;
        }

        //sharePDF(pdf, packageName);

        shareDeepLink(pdf, packageName);
    }

    private void shareDeepLink(Pdf pdf, String packageName) {
        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.setPackage(packageName);
        share.setType("text/plain");

        pdf.getDeepLink(deepLink -> {
            share.putExtra(Intent.EXTRA_TEXT, deepLink);
            progressDialog.cancel();
            startActivity(share);
        });
    }

    private void sharePDF(Pdf pdf, String packageName) {

        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.setPackage(packageName);

        share.putExtra(Intent.EXTRA_STREAM, pdf.getUri());
        share.setType("application/pdf");
        startActivity(share);


    }

    private void shareText(String msg, String packageName) {

        if (msg.isEmpty())
            return;

        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.setPackage(packageName);

        share.putExtra(Intent.EXTRA_TEXT, msg);
        share.setType("text/plain");
        startActivity(share);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {

            Uri uri = data.getData();
            // Getting pdf name
            String pdfName = "";
            String uriString = uri.toString();
            File myFile = new File(uriString);
            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        pdfName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else if (uriString.startsWith("file://")) {
                pdfName = myFile.getName();
            }

            ((TextView) findViewById(R.id.pdf_name_textview)).setText(pdfName);

            pdfName = Utils.getEncodedString(pdfName).replace(".pdf","");
            pdf = new Pdf(uri, pdfName, this);
        }
    }
}

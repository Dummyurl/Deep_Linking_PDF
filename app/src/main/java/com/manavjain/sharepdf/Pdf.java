package com.manavjain.sharepdf;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;

/**
 * Created by YourFather on 25-12-2017.
 */

public class Pdf {

    final static String PDF_URI_KEY = "pdf_uri";
    final static String PDF_NAME_KEY = "pdf_name";

    static Pdf previousPDF;

    private final String TAG = getClass().getName();

    private Uri uri;
    private String title;
    private String deepLink;
    private Activity activity;

    public Pdf(Uri uri, String title, Activity activity) {
        this.uri = uri;
        this.title = title.replace(".pdf","");
        this.activity = activity;

//        if (title != null && title.contains(".pdf"))
//            this.title = title.replace(".pdf","");

        Log.d(TAG + "Uri", uri.toString());
    }

    public Uri getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    private void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
    }

    public void getDeepLink(OnDownloadCompleteListener onDownloadCompleteListener) {
        if (previousPDF != null && !getUri().equals(previousPDF.getUri())) setDeepLink(null);

        if (deepLink == null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReference().child(Utils.DEEP_LINK_DB_NAME);

            ref.child(getTitle()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    deepLink = dataSnapshot.getValue(String.class);

                    if (deepLink == null) {
                        Log.i(TAG, "Creating new Deep Link");

                        //deepLink = buildDynamicLink();
                        buildShortDynamicLink(shortLink -> {
                            deepLink = shortLink;
                            addDeepLinkToFirebase(deepLink);
                            previousPDF = Pdf.this;
                            onDownloadCompleteListener.onComplete(deepLink);
                        });
                    } else {
                        deepLink = Utils.DEEP_LINK_URL + deepLink;
                        onDownloadCompleteListener.onComplete(deepLink);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void addDeepLinkToFirebase(String deepLink) {
        FirebaseDatabase.getInstance().getReference()
                .child(Utils.DEEP_LINK_DB_NAME)
                .child(getTitle())
                .setValue(deepLink.replace(Utils.DEEP_LINK_URL, ""));
        Log.i(TAG, "New DeepLink added");
    }

    @NonNull
    private String buildDynamicLink() {
        String path = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setDynamicLinkDomain("fgvm8.app.goo.gl")
                .setLink(Uri.parse("https://sharepdf.com/data?" + PDF_NAME_KEY + "=" + getTitle()))
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder("com.manavjain.sharepdf").build())
                .buildDynamicLink().getUri().toString();
        Log.d(TAG + " Long Link", path);
        return path;
    }

    private void buildShortDynamicLink(OnDownloadCompleteListener onDownloadCompleteListener) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLongLink(Uri.parse(buildDynamicLink()))
                .buildShortDynamicLink()
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        // Short link created
                        Uri shortLink = task.getResult().getShortLink();
                        Log.d(TAG + " Short Link", shortLink.toString());
                        onDownloadCompleteListener.onComplete(shortLink.toString());
                    } else {
                        // Error
                        Log.d(TAG, "Short link failed");
                    }
                })
                .addOnFailureListener(activity, task -> {
                    Log.d(TAG, "Short link failed : " + task.getMessage());
                });
    }

    public interface OnDownloadCompleteListener {
        void onComplete(String deepLink);
    }
}

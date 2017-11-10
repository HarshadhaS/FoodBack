package com.foodack.foodback;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.util.Pair;
import android.widget.Toast;

import org.apache.http.params.HttpParams;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Student on 11/1/2017.
 */

public class Tab1 extends Fragment implements View.OnClickListener {
    //String that contains the photo path
    String mCurrentPhotoPath;

    View view;
    ImageView mImageView;
    ImageButton cameraButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.tab1, container, false);
        mImageView = (ImageView) view.findViewById(R.id.mImageView);
        cameraButton = (ImageButton) view.findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cameraButton:
                //create an intenet and ask the android built in function to take a picture
                // Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // if(takePictureIntent.resolveActivity(getActivity().getPackageManager())!=null){
                //    startActivityForResult(takePictureIntent, 1);
                //}
                dispatchTakePictureIntent();

        }
    }

    //this method is invoked once the picture is taken
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            UploadImage();
            //mImageView.setImageURI(Uri.parse(mCurrentPhotoPath));

            //open  ingredients fragment
            Fragment ingredients = new Ingredients();
            FragmentManager ingredientsManager = getActivity().getSupportFragmentManager();
            FragmentTransaction ingredientsTrans = ingredientsManager.beginTransaction();
            ingredientsTrans.replace(android.R.id.content, ingredients);
            ingredientsTrans.addToBackStack(null);
            ingredientsTrans.commit();

        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                e.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this.getContext(),
                        "com.foodback.foodback.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    private void UploadImage() {
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream); //compress to which format you want.

        byte[] byte_arr = stream.toByteArray();
        final String image_str = Base64.encodeToString(byte_arr, Base64.DEFAULT);

        ArrayList<Pair<String, String>> params = new  ArrayList<>();
        params.add(new Pair<>("image", image_str));

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://foodback.ddns.net:4221/foodback/sendImage.php");
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);
                    urlConnection.setChunkedStreamingMode(0);

                    DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                    String query = "image="+ URLEncoder.encode(image_str, "UTF-8");
                    wr.writeBytes (query);
                    wr.flush ();
                    wr.close ();

                    //read response from server
                    BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String response; //holds server response

                    while((response= rd.readLine())!= null) {
                        sb.append(response);
                    }

                    //send response to be displayed for user to view and edit
                    Log.i("response", sb.toString());
                    
                }catch (Exception e){
                        e.printStackTrace();
                }finally {
                }
            }
        });
        t.start();
    }
}
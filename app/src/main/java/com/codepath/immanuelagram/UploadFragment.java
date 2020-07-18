package com.codepath.immanuelagram;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.codepath.immanuelagram.LoginActivity;
import com.codepath.immanuelagram.MainActivity;
import com.codepath.immanuelagram.User;
import com.codepath.immanuelagram.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;



import java.io.File;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.CAMERA_SERVICE;

/**
     * A simple {@link Fragment} subclass.
     */
    public class UploadFragment extends Fragment {

        public static final String TAG = "UploadFragment";

        public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 77 ;
        private EditText etDescription;
        private Button btnCaptureImage;
        private ImageView ivPostImage;
        private Button btnSubmit;
        private File photoFile;
        private FirebaseAuth mAuth;
        private FirebaseAuth.AuthStateListener mAuthListener;
        private UploadTask uploadTask;
        private  FirebaseUser this_user;

        private FirebaseStorage storage;
        private FirebaseDatabase mFirebaseDatabase;
        private DatabaseReference mRef;
        private StorageReference storageRef;
        private Context mcontext;
        public StorageReference fileref;
        public int name_count;

        public SharedPreferences sharedPref;



        public UploadFragment() {
            // Required empty public constructor
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            if (getResources().getInteger(R.integer.name_count) == 0){
                sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.name_count), name_count);
                editor.commit();
            }
            else {

                sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                name_count = getResources().getInteger(R.integer.name_count);
                Log.d(TAG, "onCreateView:" + name_count);

            }
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.fragment_upload, container, false);
        }

        @Override
        // This event is triggered soon after onCreateView()
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            etDescription = view.findViewById(R.id.etDescription);
            btnCaptureImage = view.findViewById(R.id.btnCaptureImage);
            ivPostImage = view.findViewById(R.id.ivPostImage);
            btnSubmit = view.findViewById(R.id.btnSubmit);
            mAuth = FirebaseAuth.getInstance();
            storage = FirebaseStorage.getInstance();
            this_user = mAuth.getInstance().getCurrentUser();
            storageRef =storage.getReference();
            mcontext = getContext();
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            mRef = mFirebaseDatabase.getReference("userphotos");





            btnCaptureImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    launchCamera();
                }
            });
            mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                    checkifLoggedin(this_user);
                    if (this_user != null) {
                        Log.d(TAG, "onAuthStateChanged: logged_in" + this_user.getUid());
                    } else {
                        Log.d(TAG, "onAuthStateChanged: signed_out");
                    }
                }
            };

            // queryPosts();


            btnSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String description = etDescription.getText().toString();
                    if (description.isEmpty()){
                        Toast.makeText(getContext(),"Description can not be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (photoFile == null || ivPostImage.getDrawable() == null){
                        Toast.makeText(getContext(), "There is no image", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    savePostFirebase(description, photoFile,this_user);
                }
            });
        }


        private void launchCamera() {
            // create Intent to take a picture and return control to the calling application

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Create a File reference to access to future access

            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            name_count = name_count +1;
            editor.putInt(getString(R.string.name_count), name_count);
            editor.commit();
            photoFile = getPhotoFileUri(name_count);


            // wrap File object into a content provider
            // required for API >= 24
            // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
            Log.i(TAG, "launchCamera: In launchCamera");
            Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.codepath.fileprovider", photoFile);
            Log.i(TAG, "launchCamera: In launchCamera 2");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);
            Log.i(TAG, "launchCamera: In launchCamera 3");

            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.
            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    // by this point we have the camera photo on disk
                    Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                    // RESIZE BITMAP, see section below
                    // Load the taken image into a preview
                    ivPostImage.setImageBitmap(takenImage);
                } else { // Result was a failure
                    Toast.makeText(getContext(), "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Returns the File for a photo stored on disk given the fileName
        public File getPhotoFileUri(int fileName) {
            // Get safe storage directory for photos
            // Use `getExternalFilesDir` on Context to access package-specific directories.
            // This way, we don't need to request external read/write runtime permissions.
            File mediaStorageDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
            }

            // Return the file target for the photo based on filename
            return new File(mediaStorageDir.getPath() + File.separator + this_user.getUid()+name_count );
        }

        private void checkifLoggedin(FirebaseUser user){
            Log.d(TAG, "checkifLoggedin: Running");

            if (user == null) {

                Intent intent = new Intent (getContext(), LoginActivity.class);
                startActivity(intent);
            }}
        public void savePostFirebase(final String description, final File photoFile, final FirebaseUser this_user) {
            final Uri file = Uri.fromFile(photoFile);
            fileref = storageRef.child("images/"+file.getLastPathSegment());
            Log.d(TAG, "savePostFirebase: Image about to upload.");
            uploadTask = fileref.putFile(file);


            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(),"savePostFirebase: Unsuccessful upload",Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    //double progress = (taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount())*100;
                    Toast.makeText(getContext(),"Progress"+((taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount())*100)+ " %", Toast.LENGTH_SHORT ).show();
                    Log.d(TAG, "onProgress: "+((taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount())*100));
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //File Metadata and content type
                    Log.d(TAG, "onSuccess: Uploaded Image" );
                    Log.d(TAG, "onSuccess: Uploaded Image" );

                    Uri firebaseUrl =taskSnapshot.getUploadSessionUri();
                    //addPhotoToDatabase(description, firebaseUrl.toString());
                    Intent intent = new Intent (getContext(), MainActivity.class);
                    startActivity(intent);
                }
            });

        }

        private void addPhotoToDatabase (String description, String url){
            Log.d(TAG, "addPhotoToDatabase: adding photo to database");

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("allPhotos");

            myRef.setValue(fileref.getStream());
/*
            String newPhotoKey = mRef.child(mcontext.getString(R.string.dbname_photos)).push().getKey();
            File photoFile = photo;
            String newdescripiton = description;
            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

            mRef.child(mcontext.getString(R.string.allPhotos)).child(user_id).setValue(photo);
            mRef.child(mcontext.getString(R.string.userphotos)).child(user_id)
                    .child(String.valueOf(R.string.upload)).child(String.valueOf(System.currentTimeMillis()*1000)).setValue(photo);
            mRef.child(mcontext.getString(R.string.userphotos)).child(user_id)
                    .child(String.valueOf(R.string.upload)).child(String.valueOf(R.string.description)).setValue(description);
*/
        }
    }

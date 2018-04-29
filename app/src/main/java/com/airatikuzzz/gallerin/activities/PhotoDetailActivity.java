package com.airatikuzzz.gallerin.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.airatikuzzz.gallerin.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.kc.unsplash.Unsplash;
import com.kc.unsplash.models.Photo;

import java.io.File;
import java.io.FileOutputStream;

public class PhotoDetailActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private Bitmap bitmap;
    private Uri mUri;
    private String mId;
    private File currentSavedFile;
    private MenuItem itemDownload;
    private boolean isSaved;

    ProgressBar progressBar;
    private PhotoView photoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
        mId = getIntent().getData().toString();
        Log.d("kek", "uri : " + mId.toString());
        isSaved = false;

        getWindow().setEnterTransition(null);
        progressBar = findViewById(R.id.load_progress_bar_detail);

        Unsplash unsplash = new Unsplash("ebe8195594bd221b1c86bb55d0224f1c439b41d0aa4d3736ba7bda6e57dcfde5");

        unsplash.getPhoto(mId, new Unsplash.OnPhotoLoadedListener() {
            @Override
            public void onComplete(Photo photo) {
                String photoUrl = photo.getUrls().getRegular();
                mUri = Uri.parse(photoUrl);
                loadImage();
            }

            @Override
            public void onError(String error) {
                Log.v("Error", error);
            }
        });
        photoView = findViewById(R.id.iv_photo);

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkExistsInStorage();
    }

    private boolean checkExistsInStorage() {
        String imageFileName = mId + ".jpg";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + "/Gallerin");
        File imageFile = new File(storageDir, imageFileName);
        Log.d("Photos", "check " + imageFile.getAbsolutePath());

        if (imageFile.exists()) {
            Log.d("Photos", "check ok");
            isSaved = true;
            currentSavedFile = imageFile;
            invalidateOptionsMenu();
            return true;
        }
        invalidateOptionsMenu();
        return false;
    }


    private void loadImage() {
        Glide.with(this)
                .load(mUri)
                .listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PhotoDetailActivity.this,
                                "Не удалось загрузить изображение. Повторите попытку позже",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(photoView);
    }

    public static Intent newIntent(Context context, Uri uri) {
        Intent i = new Intent(context, PhotoDetailActivity.class);
        i.setData(uri);
        return i;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_detail, menu);
        itemDownload = menu.findItem(R.id.menu_download_file);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem itemShare = menu.findItem(R.id.menu_share);
        MenuItem item = menu.findItem(R.id.menu_done);
        MenuItem itemSetWallpaper = menu.findItem(R.id.menu_set_wallpaper);
        if (isSaved) {
            itemShare.setVisible(true);
            item.setVisible(true);
            itemSetWallpaper.setVisible(true);
            itemDownload.setVisible(false);
        } else {
            itemShare.setVisible(false);
            item.setVisible(false);
            itemDownload.setVisible(true);
            itemSetWallpaper.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_download_file:
                Glide.with(this)
                        .load(mUri)
                        .asBitmap()
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                bitmap = resource;
                                if (hasPermissions()) {
                                    saveImage(resource);
                                } else {
                                    requestPerms();
                                }
                            }
                        });
                break;
            case R.id.menu_share:
                sharePhoto();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_set_wallpaper:
                sendSetWallpaper();
                break;
            case R.id.menu_done:
                Toast.makeText(this, "Изображение уже загружено", Toast.LENGTH_SHORT).show();
                break;

        }
        return true;
    }

    private void sharePhoto() {
        Uri contentUri = Uri.fromFile(currentSavedFile);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/jpeg");
        send.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(send, "Select"));
    }

    private void sendSetWallpaper() {
        Uri contentUri = Uri.fromFile(currentSavedFile);
        Intent setAs = new Intent(Intent.ACTION_ATTACH_DATA);
        setAs.setDataAndType(contentUri, "image/*");
        startActivity(Intent.createChooser(setAs, "Select"));
    }

    private void saveImage(Bitmap image) {
        String savedImagePath = null;

        String imageFileName = mId + ".jpg";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + "/Gallerin");
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }
        if (success) {
            File imageFile = new File(storageDir, imageFileName);

            savedImagePath = imageFile.getAbsolutePath();
            try {
                FileOutputStream fOut = new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush();
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Произошла ошибка. Изображение не сохранено. Нет разрешения на запись на диск", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add the image to the system gallery
            galleryAddPic(savedImagePath);
            Toast.makeText(this, "Изображение загружено", Toast.LENGTH_SHORT).show();
            isSaved = true;
            currentSavedFile = imageFile;
            invalidateOptionsMenu();
        }
    }

    private void galleryAddPic(String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);

        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    private boolean hasPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allowed = true;

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                for (int res : grantResults) {
                    // if user granted all permissions.
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }

                break;
            default:
                // if user not granted permissions.
                allowed = false;
                break;
        }

        if (allowed) {
            //user granted all permissions we can perform our task.
            saveImage(bitmap);
        } else {

        }
    }
}

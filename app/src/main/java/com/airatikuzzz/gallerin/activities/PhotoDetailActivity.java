package com.airatikuzzz.gallerin.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.airatikuzzz.gallerin.GalleryItem;
import com.airatikuzzz.gallerin.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PhotoDetailActivity extends AppCompatActivity {

    private static final String EXTRA_URL = "com.airatikuzzz.gallerin.extra_url_gallerin";
    private static final String EXTRA_ID = "com.airatikuzzz.gallerin.extra_id_gallerin";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private Bitmap bitmap;
    private Uri mUri;
    private String mId;
    private File currentSavedFile;
    private boolean isSaved;
    private boolean isVisibleToolbar = true;

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private PhotoView photoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
        mId = getIntent().getStringExtra(EXTRA_ID);                 //Достаем переданые данные при создании интента.
        mUri = Uri.parse(getIntent().getStringExtra(EXTRA_URL));
        isSaved = false;

        progressBar = findViewById(R.id.load_progress_bar_detail);

        photoView = findViewById(R.id.iv_photo);
        photoView.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(ImageView view, float x, float y) {           //Исчезновение тулбара при нажатии на фото
                if(isVisibleToolbar) {
                    toolbar.animate().translationY(-toolbar.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
                    getSupportActionBar().hide();
                    isVisibleToolbar = false;
                }
                else {
                    getSupportActionBar().show();
                    toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
                    isVisibleToolbar = true;
                }
                toolbar.invalidate();
            }
        });

        AppBarLayout appBarLayout = findViewById(R.id.app_bar_lay);
        appBarLayout.setOutlineProvider(null);

        toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        loadImageToPhotoView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkExistsInStorage();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //Проверка на наличие фото в диске. Если файл был скачан - достаем с диска.
    private void checkExistsInStorage() {
        String imageFileName = mId + ".jpg";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + "/Gallerin");
        File imageFile = new File(storageDir, imageFileName);

        if (imageFile.exists()) {
            isSaved = true;
            currentSavedFile = imageFile;
            invalidateOptionsMenu();
            return;
        }
        invalidateOptionsMenu();
    }

    //Загрузка фото по URI, переданного при создании активити.
    private void loadImageToPhotoView() {
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

    public static Intent newIntent(Context context, GalleryItem item) {
        Intent i = new Intent(context, PhotoDetailActivity.class);
        i.putExtra(EXTRA_URL, item.getUrlFull());
        i.putExtra(EXTRA_ID, item.getId());
        return i;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem itemDownload = menu.findItem(R.id.menu_download_file);
        MenuItem itemShare = menu.findItem(R.id.menu_share);
        MenuItem itemDone = menu.findItem(R.id.menu_done);
        MenuItem itemSetWallpaper = menu.findItem(R.id.menu_set_wallpaper);
        if (isSaved) {                                    //Изображение загружено - появляются возможности
            itemShare.setVisible(true);
            itemDone.setVisible(true);
            itemSetWallpaper.setVisible(true);
            itemDownload.setVisible(false);
        } else {
            itemShare.setVisible(false);
            itemDone.setVisible(false);
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
                downloadPhoto();
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

    //Скачивание фото в диск
    private void downloadPhoto() {
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
        startActivity(Intent.createChooser(setAs, "Set as"));
    }

    private void galleryAddPic(String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    private void saveImage(Bitmap image) {
        String imageFileName = mId + ".jpg";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + "/Gallerin");
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }
        if (success) {
            File imageFile = new File(storageDir, imageFileName);

            try {
                FileOutputStream fOut = new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush();
                fOut.close();
            } catch (SecurityException e) {
                e.printStackTrace();
                Toast.makeText(this, "Произошла ошибка. Изображение не сохранено. Нет разрешения на запись на диск", Toast.LENGTH_SHORT).show();
                return;
            } catch (FileNotFoundException e) {
                Toast.makeText(this, "Произошла ошибка. Изображение не сохранено.", Toast.LENGTH_SHORT).show();
                return;
            } catch (IOException e) {
                Toast.makeText(this, "Произошла ошибка. Изображение не сохранено. Нет разрешения на запись на диск", Toast.LENGTH_SHORT).show();
                return;
            }
            galleryAddPic(imageFile.getAbsolutePath());
            Toast.makeText(this, "Изображение загружено", Toast.LENGTH_SHORT).show();
            isSaved = true;
            currentSavedFile = imageFile;
            invalidateOptionsMenu();
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
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
            Toast.makeText(this, "Нет разрешений на запись на диск. Настройки - Приложения - Gallerin", Toast.LENGTH_SHORT).show();
        }
    }
}

package com.tensor.gtsrb;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camerakit.CameraKitView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.vCamera)
    CameraKitView vCamera;
    @BindView(R.id.ivPreview)
    ImageView ivPreview;

    @BindView(R.id.tvClassification)
    TextView tvClassification;
    Button button;

    private GtsrbClassifier gtsrbClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        ButterKnife.bind(this);
        loadGtsrbClassifier();

    }

    private void loadGtsrbClassifier() {
        try {
            gtsrbClassifier = GtsrbClassifier.classifier(getAssets(), GtsrbModelConfig.MODEL_FILENAME);
        } catch (IOException e) {
            Toast.makeText(this, "GTSRB model couldn't be loaded. Check logs for details.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        vCamera.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vCamera.onResume();
    }

    @Override
    protected void onPause() {
        vCamera.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        vCamera.onStop();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        vCamera.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @OnClick(R.id.btnTakePhoto)
    public void onTakePhoto() {
        vCamera.captureImage((cameraKitView, picture) -> {
            onImageCaptured(picture);
        });
    }

    public void trigger(View view){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent,"pick image"),1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK &&requestCode ==1){

            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,70,bytearrayoutputstream);
                byte[] Byte;
                Byte = bytearrayoutputstream.toByteArray();
                onImageCaptured(Byte);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onImageCaptured(byte[] picture) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
        Bitmap squareBitmap = ThumbnailUtils.extractThumbnail(bitmap, getScreenWidth(), getScreenWidth());
        ivPreview.setImageBitmap(squareBitmap);

        Bitmap preprocessedImage = ImageUtils.prepareImageForClassification(squareBitmap);


        List<Classification> recognitions = gtsrbClassifier.recognizeImage(preprocessedImage);
        tvClassification.setText(recognitions.toString());
    }

    private int getScreenWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }
}

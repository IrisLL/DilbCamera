package com.example.dlibtest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.provider.BigImageCardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.example.dlibtest.Dlib.Constants;
import com.example.dlibtest.Dlib.FaceDet;
import com.example.dlibtest.Dlib.VisionDetRet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectPic extends AppCompatActivity {

    private static final int RESULT_LOAD_IMG = 1;

    private ProgressDialog mDialog;
    private MaterialListView mListView;
    private FaceDet mFaceDet;
    //private PedestrianDet mPersonDet;
    private FaceDet mPersonDet;
    private String mTestImgPath;
    private List<Card> mCard = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectpic);
        mListView = (MaterialListView) findViewById(R.id.material_listview);

        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    protected void demoStaticImage() {
        if (mTestImgPath != null) {
            runDemosAsync(mTestImgPath);
        } else {
            Toast.makeText(SelectPic.this, "Pick an image to run algorithms", Toast.LENGTH_SHORT).show();
            // Create intent to Open Image applications like Gallery, Google Photos
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            Toast.makeText(SelectPic.this, "Demo using static images", Toast.LENGTH_SHORT).show();
            demoStaticImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mTestImgPath = cursor.getString(columnIndex);
                cursor.close();
                if (mTestImgPath != null) {
                    runDemosAsync(mTestImgPath);
                    Toast.makeText(this, "Img Path:" + mTestImgPath, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }





    // ==========================================================
    // Tasks inner class
    // ==========================================================

    @NonNull
    private void runDemosAsync(@NonNull final String imgPath) {
       // demoPersonDet(imgPath);
        demoFaceDet(imgPath);
    }

    private void demoPersonDet(final String imgPath) {
        new AsyncTask<Void, Void, List<VisionDetRet>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(List<VisionDetRet> personList) {
                super.onPostExecute(personList);
                if (personList.size() > 0) {
                    Card card = new Card.Builder(SelectPic.this)
                            .withProvider(BigImageCardProvider.class)
                            .setDrawable(drawRect(imgPath, personList, Color.BLUE))
                            .setTitle("Person det")
                            .endConfig()
                            .build();
                    mCard.add(card);
                } else {
                    Toast.makeText(getApplicationContext(), "No person", Toast.LENGTH_LONG).show();
                }
                updateCardListView();
            }

            @Override
            protected List<VisionDetRet> doInBackground(Void... voids) {
                // Init
                if (mPersonDet == null) {
                    //mPersonDet = new PedestrianDet();
                    mPersonDet = new FaceDet();
                }

                //Timber.tag(TAG).d("Image path: " + imgPath);

                List<VisionDetRet> personList = mPersonDet.detect(imgPath);
                return personList;
            }
        }.execute();
    }

    private void demoFaceDet(final String imgPath) {
        new AsyncTask<Void, Void, List<VisionDetRet>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showDiaglog("Detecting faces");
            }

            @Override
            protected void onPostExecute(List<VisionDetRet> faceList) {
                super.onPostExecute(faceList);
                if (faceList.size() > 0) {
                    Card card = new Card.Builder(SelectPic.this)
                            .withProvider(BigImageCardProvider.class)
                            .setDrawable(drawRect(imgPath, faceList, Color.GREEN))
                            .setTitle("Face det")
                            .endConfig()
                            .build();
                    mCard.add(card);
                } else {
                    Toast.makeText(getApplicationContext(), "No face", Toast.LENGTH_LONG).show();
                }
                updateCardListView();
                dismissDialog();
            }

            @Override
            protected List<VisionDetRet> doInBackground(Void... voids) {
                // Init
                if (mFaceDet == null) {
                    mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
                }

                final String targetPath = Constants.getFaceShapeModelPath();
                if (!new File(targetPath).exists()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SelectPic.this, "Copy landmark model to " + targetPath, Toast.LENGTH_SHORT).show();
                        }
                    });
                    FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, targetPath);
                }

                List<VisionDetRet> faceList = mFaceDet.detect(imgPath);
                return faceList;
            }
        }.execute();
    }

    private void updateCardListView() {
        mListView.clearAll();
        for (Card each : mCard) {
            mListView.add(each);
        }
    }

    private void showDiaglog(String title) {
        dismissDialog();
        mDialog = ProgressDialog.show(SelectPic.this, title, "process..", true);
    }

    private void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }


    private BitmapDrawable drawRect(String path, List<VisionDetRet> results, int color) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);
        int width = bm.getWidth();
        int height = bm.getHeight();
        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();

        final int MAX_SIZE = 512;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            //Timber.tag(TAG).d("Resize Bitmap");
            bm = getResizedBitmap(bm, newWidth, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
            //Timber.tag(TAG).d("resizeRatio " + resizeRatio);
        }

        // Create canvas to draw
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        // Loop result list
        for (VisionDetRet ret : results) {
            Rect bounds = new Rect();
            bounds.left = (int) (ret.getLeft() * resizeRatio);
            bounds.top = (int) (ret.getTop() * resizeRatio);
            bounds.right = (int) (ret.getRight() * resizeRatio);
            bounds.bottom = (int) (ret.getBottom() * resizeRatio);
            canvas.drawRect(bounds, paint);
            // Get landmark
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            for (Point point : landmarks) {
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);
                canvas.drawCircle(pointX, pointY, 2, paint);
            }
        }

        return new BitmapDrawable(getResources(), bm);
    }


    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return resizedBitmap;
    }
}


package wayne.com.imageidentification.GroupSetting;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.GridView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import wayne.com.imageidentification.Adapter.FaceGridViewAdapter;
import wayne.com.imageidentification.Helper.ImageHelper;
import wayne.com.imageidentification.Helper.StorageHelper;
import wayne.com.imageidentification.R;

public class AddFaceToPersonActivity extends AppCompatActivity implements View.OnClickListener {
    private final String api_Endpoint =
        "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscription_Key = "a0113b552f6a46f6a26319454421273d";
    private final FaceServiceClient faceServiceClient =
        new FaceServiceRestClient(api_Endpoint, subscription_Key);
    String mPersonGroupId;
    String mPersonId;
    String mImageUriStr;
    Bitmap mBitmap;
    FaceGridViewAdapter mFaceGridViewAdapter;
    GridView mGridView;
    ProgressDialog mProgressDialog;
    FloatingActionButton mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face_to_person);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mPersonId = bundle.getString("PersonId");
            mPersonGroupId = bundle.getString("PersonGroupId");
            mImageUriStr = bundle.getString("ImageUriStr");
        }
        initView();
    }

    void initView() {
        mGridView = findViewById(R.id.gridView_faces_to_select);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Please wait");
        mButton = findViewById(R.id.floatButton_save_face);
        mButton.setOnClickListener(this);
        mButton.setEnabled(true);
    }

    public void doneAndSave() {
        if (mFaceGridViewAdapter != null) {
            List<Integer> faceIndices = new ArrayList<>();
            for (int i = 0; i < mFaceGridViewAdapter.faceRectList.size(); ++i) {
                if (mFaceGridViewAdapter.faceChecked.get(i)) {
                    faceIndices.add(i);
                }
            }
            if (faceIndices.size() > 0) {
                new AddFaceTask(faceIndices).execute();
            } else {
                finish();
            }
        }
    }

    private void setUiBeforeBackgroundTask() {
        mProgressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress) {
        mProgressDialog.setMessage(progress);
    }

    private void setUiAfterAddingFace(boolean succeed, List<Integer> faceIndices) {
        mProgressDialog.dismiss();
        if (succeed) {
            String faceIds = "";
            for (Integer index : faceIndices) {
                String faceId = mFaceGridViewAdapter.faceIdList.get(index).toString();
                faceIds += faceId + ", ";
                FileOutputStream fileOutputStream = null;
                try {
                    File file = new File(getApplicationContext().getFilesDir(), faceId);
                    fileOutputStream = new FileOutputStream(file);
                    mFaceGridViewAdapter.faceThumbnails.get(index)
                        .compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    Uri uri = Uri.fromFile(file);
                    StorageHelper.setFaceUri(
                        faceId, uri.toString(), mPersonId, this);
                } catch (IOException e) {
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            finish();
        }
    }

    private void setUiAfterDetection(Face[] result, boolean succeed) {
        mProgressDialog.dismiss();
        if (succeed) {
            mFaceGridViewAdapter = new FaceGridViewAdapter(result, mBitmap,
                AddFaceToPersonActivity.this);
            mGridView.setAdapter(mFaceGridViewAdapter);
        }
    }

    class AddFaceTask extends AsyncTask<Void, String, Boolean> {
        List<Integer> mFaceIndices;

        AddFaceTask(List<Integer> faceIndices) {
            mFaceIndices = faceIndices;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Get an instance of face service client to detect faces in image.
            try {
                publishProgress("Adding face...");
                UUID personId = UUID.fromString(mPersonId);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());
                for (Integer index : mFaceIndices) {
                    FaceRectangle faceRect = mFaceGridViewAdapter.faceRectList.get(index);
                    // Start the request to add face.
                    AddPersistedFaceResult result =
                        faceServiceClient.addPersonFaceInLargePersonGroup(
                            mPersonGroupId,
                            personId,
                            imageInputStream,
                            "User data",
                            faceRect);
                    mFaceGridViewAdapter.faceIdList.set(index, result.persistedFaceId);
                }
                return true;
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            setUiAfterAddingFace(result, mFaceIndices);
        }
    }

    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            try {
                publishProgress("Detecting...");
                // Start detection.
                return faceServiceClient.detect(
                    params[0],  /* Input stream of image to detect */
                    true,       /* Whether to return face ID */
                    false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                    null);
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] faces) {
            // Show the result on screen when detection is done.
            setUiAfterDetection(faces, mSucceed);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Uri imageUri = Uri.parse(mImageUriStr);
        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
            imageUri, getContentResolver());
        if (mBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());
            new DetectionTask().execute(imageInputStream);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatButton_save_face:
                doneAndSave();
                break;
            default:
                break;
        }
    }

}

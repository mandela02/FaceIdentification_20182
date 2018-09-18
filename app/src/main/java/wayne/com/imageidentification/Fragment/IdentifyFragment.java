package wayne.com.imageidentification.Fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import wayne.com.imageidentification.Adapter.FaceListAdapter;
import wayne.com.imageidentification.Helper.ImageHelper;
import wayne.com.imageidentification.Helper.StorageHelper;
import wayne.com.imageidentification.R;

public class IdentifyFragment extends Fragment implements View.OnClickListener {
    private final String api_Endpoint =
        "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscription_Key = "a0113b552f6a46f6a26319454421273d";
    private final FaceServiceClient faceServiceClient =
        new FaceServiceRestClient(api_Endpoint, subscription_Key);
    public static final int REQUEST_CAPTURE_IMAGE = 100;
    public static final int PICK_IMAGE = 200;
    public Button mButton;
    public ImageView mImageTake;
    public ImageView mImageSelect;
    public ImageView mImageDisplay;
    public ProgressDialog progressDialog;
    public FaceListAdapter mFaceListAdapter;
    public Uri mImageUri;
    public Bitmap imageBitmap;
    public String mPersonGroupId;
    public boolean detected;
    public ListView mListView;
    public List<String> personGroupIdList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_identify, null);
        initView(v);
        detected = false;
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Please wait");
        return v;
    }

    private void initView(View v) {
        mImageSelect = v.findViewById(R.id.image_select);
        mImageSelect.setOnClickListener(this);
        mImageTake = v.findViewById(R.id.image_take);
        mImageTake.setOnClickListener(this);
        mImageDisplay = v.findViewById(R.id.image_display);
        mListView = v.findViewById(R.id.list_identified_faces);
        mButton = v.findViewById(R.id.button_identify);
        mButton.setOnClickListener(this);
        personGroupIdList = new ArrayList<>();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
        }
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void openCameraIntent() {
        Intent pictureIntent = new Intent(
            MediaStore.ACTION_IMAGE_CAPTURE
        );
        if (pictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(pictureIntent,
                REQUEST_CAPTURE_IMAGE);
        }
    }

    private void setIdentifyButtonEnabledStatus(boolean isEnabled) {
        mButton.setEnabled(isEnabled);
    }

    private void refreshIdentifyButtonEnabledStatus() {
        if (detected && mPersonGroupId != null) {
            setIdentifyButtonEnabledStatus(true);
        } else {
            setIdentifyButtonEnabledStatus(false);
        }
    }

    private void setUiAfterIdentification(IdentifyResult[] result, boolean succeed) {
        progressDialog.dismiss();
        setAllButtonsEnabledStatus(true);
        setIdentifyButtonEnabledStatus(false);
        if (succeed) {
            // Set the information about the detection result.
            if (result != null) {
                mFaceListAdapter.setIdentificationResult(result);
                // Show the detailed list of detected faces.
                mListView.setAdapter(mFaceListAdapter);
            }
        }
    }

    private void setUiBeforeBackgroundTask() {
        progressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress) {
        progressDialog.setMessage(progress);
    }

    private void setAllButtonsEnabledStatus(boolean isEnabled) {
        mImageTake.setEnabled(isEnabled);
        mImageSelect.setEnabled(isEnabled);
        mButton.setEnabled(isEnabled);
    }

    private void detect(Bitmap bitmap) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        setAllButtonsEnabledStatus(false);
        new DetectionTask().execute(inputStream);
    }

    public void identify() {
        // Start detection task only if the image to detect is selected.
        if (detected && mPersonGroupId != null) {
            // Start a background task to identify faces in the image.
            List<UUID> faceIds = new ArrayList<>();
            for (Face face : mFaceListAdapter.mFaces) {
                faceIds.add(face.faceId);
            }
            setAllButtonsEnabledStatus(false);
            new IdentificationTask(mPersonGroupId).execute(
                faceIds.toArray(new UUID[faceIds.size()]));
        } else {
            Toast.makeText(getContext(), "you have fail this city", Toast.LENGTH_SHORT).show();
        }
    }

    private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]> {
        private boolean mSucceed = true;
        String mPersonGroupId;

        IdentificationTask(String personGroupId) {
            this.mPersonGroupId = personGroupId;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params) {
            try {
                publishProgress("Getting person group status...");
                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(
                    this.mPersonGroupId);     /* personGroupId */
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Person group training status is " + trainingStatus.status);
                    mSucceed = false;
                    return null;
                }
                publishProgress("Identifying...");
                // Start identification.
                return faceServiceClient.identityInLargePersonGroup(
                    this.mPersonGroupId,   /* personGroupId */
                    params,                  /* faceIds */
                    1);  /* maxNumOfCandidatesReturned */
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
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a
            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(IdentifyResult[] result) {
            // Show the result on screen when detection is done.
            setUiAfterIdentification(result, mSucceed);
        }
    }

    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
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
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.
            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            progressDialog.dismiss();
            setAllButtonsEnabledStatus(true);
            if (result != null) {
                // Set the adapter of the ListView which contains the details of detected faces.
                mFaceListAdapter =
                    new FaceListAdapter(result, imageBitmap, mPersonGroupId, getContext());
                mListView.setAdapter(mFaceListAdapter);
                if (result.length == 0) {
                    detected = false;
                } else {
                    detected = true;
                }
            } else {
                detected = false;
            }
            refreshIdentifyButtonEnabledStatus();
            mImageDisplay.setImageBitmap(ImageHelper.drawFaceRectanglesOnBitmap(imageBitmap, result,
                false));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Set<String> personGroupIds
            = StorageHelper.getAllPersonGroupIds(getContext());
        for (String personGroupId : personGroupIds) {
            personGroupIdList.add(personGroupId);
        }
        if (!personGroupIdList.isEmpty()) mPersonGroupId = personGroupIdList.get(0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_select:
                openGallery();
                break;
            case R.id.image_take:
                openCameraIntent();
                break;
            case R.id.button_identify:
                identify();
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            detected = false;
            mImageUri = data.getData();
            imageBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                mImageUri, getActivity().getContentResolver());
            mImageDisplay.setImageBitmap(imageBitmap);
            detect(imageBitmap);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}


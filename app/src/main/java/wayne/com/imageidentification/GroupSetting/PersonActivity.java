package wayne.com.imageidentification.GroupSetting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.GridView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import wayne.com.imageidentification.Adapter.FaceGridViewWithLongPressAdapter;
import wayne.com.imageidentification.Helper.StorageHelper;
import wayne.com.imageidentification.R;

public class PersonActivity extends AppCompatActivity implements View.OnClickListener {
    private final String api_Endpoint =
        "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscription_Key = "a0113b552f6a46f6a26319454421273d";
    private final FaceServiceClient faceServiceClient =
        new FaceServiceRestClient(api_Endpoint, subscription_Key);
    public static final int PICK_IMAGE = 200;
    public static final int REQUEST_CAPTURE_IMAGE = 100;
    public FloatingActionButton mButtonSave;
    public FloatingActionButton mButtonAddFace;
    public FloatingActionButton mButtonTakePicture;
    public EditText editTextPersonName;
    public ProgressDialog progressDialog;
    public GridView mGridView;
    public boolean addNewPerson;
    public String personId;
    public String personGroupId;
    public String oldPersonName;
    public FaceGridViewWithLongPressAdapter faceGridViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            addNewPerson = bundle.getBoolean("AddNewPerson");
            personGroupId = bundle.getString("PersonGroupId");
            oldPersonName = bundle.getString("PersonName");
            if (!addNewPerson) {
                personId = bundle.getString("PersonId");
            }
        }
        initView();
        initializeGridView();
        editTextPersonName.setText(oldPersonName);
    }

    void initView() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        mButtonSave = findViewById(R.id.floatButton_save);
        mButtonAddFace = findViewById(R.id.floatButton_addFace);
        mButtonTakePicture = findViewById(R.id.floatButton_takePicture);
        mButtonSave.setOnClickListener(this);
        mButtonAddFace.setOnClickListener(this);
        mButtonTakePicture.setOnClickListener(this);
        editTextPersonName = findViewById(R.id.edit_person_name);
        mGridView = findViewById(R.id.gridView_faces);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
        }
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void openCameraIntent() {
        Intent pictureIntent = new Intent(
            MediaStore.ACTION_IMAGE_CAPTURE
        );
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(pictureIntent,
                REQUEST_CAPTURE_IMAGE);
        }
    }

    public void addFace(View view) {
        if (personId == null) {
            new AddPersonTask(true).execute(personGroupId);
        } else {
            openGallery();
        }
    }

    private void doneAndSave() {
        String newPersonName = editTextPersonName.getText().toString();
        if (newPersonName.equals("")) {
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(PersonActivity.this);
            builder.setTitle("warning!")
                .setMessage("Name can not be Empty")
                .setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        } else {
            new TrainPersonGroupTask().execute(personGroupId);
            StorageHelper
                .setPersonName(personId, newPersonName, personGroupId, PersonActivity.this);
            finish();
        }
    }

    public void doneAndSave(View view) {
        if (personId == null) {
            new AddPersonTask(false).execute(personGroupId);
        } else {
            doneAndSave();
        }
    }

    private void initializeGridView() {
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(
                ActionMode mode, int position, long id, boolean checked) {
                faceGridViewAdapter.faceChecked.set(position, checked);
                mGridView.setAdapter(faceGridViewAdapter);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_delete_items, menu);
                faceGridViewAdapter.longPressed = true;
                GridView gridView = findViewById(R.id.gridView_faces);
                gridView.setAdapter(faceGridViewAdapter);
                mButtonAddFace.setEnabled(false);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_delete_items:
                        deleteSelectedItems();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                faceGridViewAdapter.longPressed = false;
                for (int i = 0; i < faceGridViewAdapter.faceChecked.size(); ++i) {
                    faceGridViewAdapter.faceChecked.set(i, false);
                }
                mGridView.setAdapter(faceGridViewAdapter);
                mButtonAddFace.setEnabled(true);
            }
        });
    }

    private void deleteSelectedItems() {
        List<String> newFaceIdList = new ArrayList<>();
        List<Boolean> newFaceChecked = new ArrayList<>();
        List<String> faceIdsToDelete = new ArrayList<>();
        for (int i = 0; i < faceGridViewAdapter.faceChecked.size(); ++i) {
            boolean checked = faceGridViewAdapter.faceChecked.get(i);
            if (checked) {
                String faceId = faceGridViewAdapter.faceIdList.get(i);
                faceIdsToDelete.add(faceId);
                new DeleteFaceTask(personGroupId, personId).execute(faceId);
            } else {
                newFaceIdList.add(faceGridViewAdapter.faceIdList.get(i));
                newFaceChecked.add(false);
            }
        }
        StorageHelper.deleteFaces(faceIdsToDelete, personId, this);
        faceGridViewAdapter.faceIdList = newFaceIdList;
        faceGridViewAdapter.faceChecked = newFaceChecked;
        faceGridViewAdapter.notifyDataSetChanged();
    }

    private void setUiBeforeBackgroundTask() {
        progressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress) {
        progressDialog.setMessage(progress);
    }

    class AddPersonTask extends AsyncTask<String, String, String> {
        // Indicate the next step is to add face in this person, or finish editing this person.
        boolean mAddFace;

        AddPersonTask(boolean addFace) {
            mAddFace = addFace;
        }

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            try {
                publishProgress("Syncing with server to add person...");
                // Start the request to creating person.
                CreatePersonResult createPersonResult =
                    faceServiceClient.createPersonInLargePersonGroup(
                        params[0],
                        "Person name", "User data");
                return createPersonResult.personId.toString();
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
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result != null) {
                personId = result;
                if (mAddFace) {
                    openGallery();
                } else {
                    doneAndSave();
                }
            }
        }
    }

    class DeleteFaceTask extends AsyncTask<String, String, String> {
        String mPersonGroupId;
        UUID mPersonId;

        DeleteFaceTask(String personGroupId, String personId) {
            mPersonGroupId = personGroupId;
            mPersonId = UUID.fromString(personId);
        }

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            try {
                publishProgress("Deleting selected faces...");
                UUID faceId = UUID.fromString(params[0]);
                faceServiceClient
                    .deletePersonFaceInLargePersonGroup(personGroupId, mPersonId, faceId);
                return params[0];
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
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
        }
    }

    class TrainPersonGroupTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(String... progress) {
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        faceGridViewAdapter = new FaceGridViewWithLongPressAdapter(personId, this);
        mGridView.setAdapter(faceGridViewAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatButton_addFace:
                addFace(v);
                break;
            case R.id.floatButton_save:
                doneAndSave(v);
                break;
            case R.id.floatButton_takePicture:
                openCameraIntent();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
        {
            Uri uriImagePicked = data.getData();
            Intent intent = new Intent(this, AddFaceToPersonActivity.class);
            intent.putExtra("PersonId", personId);
            intent.putExtra("PersonGroupId", personGroupId);
            intent.putExtra("ImageUriStr", uriImagePicked.toString());
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }
}

package wayne.com.imageidentification.Fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import wayne.com.imageidentification.Adapter.PersonGridViewAdapter;
import wayne.com.imageidentification.GroupSetting.PersonActivity;
import wayne.com.imageidentification.Helper.StorageHelper;
import wayne.com.imageidentification.R;

public class GroupFragment extends Fragment {
    private final String api_Endpoint =
        "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscription_Key = "a0113b552f6a46f6a26319454421273d";
    private final FaceServiceClient faceServiceClient =
        new FaceServiceRestClient(api_Endpoint, subscription_Key);
    GridView mGridView;
    List<String> personGroupIdList;
    String mPersonGroup;
    ProgressDialog progressDialog;
    FloatingActionButton mButton;
    PersonGridViewAdapter personGridViewAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_group, null);
        initView(v);
        getPersonGroup();
        Toast.makeText(getContext(), Integer.toString(personGroupIdList.size()), Toast.LENGTH_SHORT)
            .show();
        initializeGridView();
        return v;
    }

    void initView(View v) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("please wait ...");
        personGroupIdList = new ArrayList<>();
        mGridView = v.findViewById(R.id.gridView_persons);
        mButton = v.findViewById(R.id.floatButton_appPerson);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPerson();
            }
        });
    }

    private void addPerson() {
        Intent intent = new Intent(getContext(), PersonActivity.class);
        intent.putExtra("AddNewPerson", true);
        intent.putExtra("PersonName", "");
        intent.putExtra("PersonGroupId", mPersonGroup);
        startActivity(intent);
    }

    void getPersonGroup() {
        Set<String> personGroupIds
            = StorageHelper.getAllPersonGroupIds(getContext());
        for (String personGroupId : personGroupIds) {
            personGroupIdList.add(personGroupId);
        }
        if (personGroupIdList.isEmpty()) {
            createGroup();
        } else {
            mPersonGroup = personGroupIdList.get(0);
        }
        personGridViewAdapter = new PersonGridViewAdapter(mPersonGroup, getContext());
        mGridView.setAdapter(personGridViewAdapter);
    }

    void createGroup() {
        String personGroupId = UUID.randomUUID().toString();
        mPersonGroup = personGroupId;
        new AddPersonGroupTask().execute(personGroupId);
    }

    private void initializeGridView() {
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id, boolean checked) {
                personGridViewAdapter.personChecked.set(position, checked);
                mGridView.setAdapter(personGridViewAdapter);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_delete_items, menu);
                personGridViewAdapter.longPressed = true;
                mGridView.setAdapter(personGridViewAdapter);
                mButton.setEnabled(false);
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
                personGridViewAdapter.longPressed = false;
                for (int i = 0; i < personGridViewAdapter.personChecked.size(); ++i) {
                    personGridViewAdapter.personChecked.set(i, false);
                }
                mGridView.setAdapter(personGridViewAdapter);
                mButton.setEnabled(true);
            }
        });
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!personGridViewAdapter.longPressed) {
                    String personId = personGridViewAdapter.personIdList.get(position);
                    String personName = StorageHelper.getPersonName(
                        personId, mPersonGroup, getContext());
                    Intent intent = new Intent(getContext(), PersonActivity.class);
                    intent.putExtra("AddNewPerson", false);
                    intent.putExtra("PersonName", personName);
                    intent.putExtra("PersonId", personId);
                    intent.putExtra("PersonGroupId", mPersonGroup);
                    startActivity(intent);
                }
            }
        });
    }

    private void deleteSelectedItems() {
        List<String> newPersonIdList = new ArrayList<>();
        List<Boolean> newPersonChecked = new ArrayList<>();
        List<String> personIdsToDelete = new ArrayList<>();
        for (int i = 0; i < personGridViewAdapter.personChecked.size(); ++i) {
            if (personGridViewAdapter.personChecked.get(i)) {
                String personId = personGridViewAdapter.personIdList.get(i);
                personIdsToDelete.add(personId);
                new DeletePersonTask(mPersonGroup).execute(personId);
            } else {
                newPersonIdList.add(personGridViewAdapter.personIdList.get(i));
                newPersonChecked.add(false);
            }
        }
        StorageHelper.deletePersons(personIdsToDelete, mPersonGroup, getContext());
        personGridViewAdapter.personIdList = newPersonIdList;
        personGridViewAdapter.personChecked = newPersonChecked;
        personGridViewAdapter.notifyDataSetChanged();
    }

    private void setUiBeforeBackgroundTask() {
        progressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress) {
        progressDialog.setMessage(progress);
    }

    class AddPersonGroupTask extends AsyncTask<String, String, String> {
        // Indicate the next step is to add person in this group, or finish editing this group.

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            try {
                publishProgress("Syncing with server to add person group...");
                // Start creating person group in server.
                faceServiceClient.createLargePersonGroup(
                    params[0],
                    "Person group name",
                    "user data");
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
            if (result != null) {
                personGroupIdList.add(mPersonGroup);
                StorageHelper.setPersonGroupName(mPersonGroup, "Group 1", getContext());
            }
        }
    }

    class DeletePersonTask extends AsyncTask<String, String, String> {
        String mPersonGroupId;

        DeletePersonTask(String personGroupId) {
            mPersonGroupId = personGroupId;
        }

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            try {
                publishProgress("Deleting selected persons...");
                UUID personId = UUID.fromString(params[0]);
                faceServiceClient.deletePersonInLargePersonGroup(mPersonGroupId, personId);
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

    @Override
    public void onResume() {
        super.onResume();
        getPersonGroup();
    }
}

package wayne.com.imageidentification.Adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import wayne.com.imageidentification.Helper.StorageHelper;
import wayne.com.imageidentification.R;

public class FaceGridViewWithLongPressAdapter extends BaseAdapter {
    public List<String> faceIdList;
    public List<Boolean> faceChecked;
    public boolean longPressed;
    public Context mContext;

    public FaceGridViewWithLongPressAdapter(String personId, Context context) {
        longPressed = false;
        faceIdList = new ArrayList<>();
        faceChecked = new ArrayList<>();
        mContext = context;
        Set<String> faceIdSet = StorageHelper.getAllFaceIds(personId, mContext);
        for (String faceId : faceIdSet) {
            faceIdList.add(faceId);
            faceChecked.add(false);
        }
    }

    @Override
    public int getCount() {
        return faceIdList.size();
    }

    @Override
    public Object getItem(int position) {
        return faceIdList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater
                = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(
                R.layout.item_face_with_checkbox, parent, false);
        }
        convertView.setId(position);
        Uri uri = Uri.parse(StorageHelper.getFaceUri(
            faceIdList.get(position), mContext));
        ((ImageView) convertView.findViewById(R.id.image_face)).setImageURI(uri);
        // set the checked status of the item
        CheckBox checkBox =  convertView.findViewById(R.id.checkbox_face);
        if (longPressed) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    faceChecked.set(position, isChecked);
                }
            });
            checkBox.setChecked(faceChecked.get(position));
        } else {
            checkBox.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }
}

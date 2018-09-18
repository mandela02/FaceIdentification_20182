package wayne.com.imageidentification.Adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import wayne.com.imageidentification.Helper.StorageHelper;
import wayne.com.imageidentification.R;

public class PersonGridViewAdapter extends BaseAdapter {
    public List<String> personIdList;
    public  List<Boolean> personChecked;
    public boolean longPressed;
    public  Context mContext;
    public  String mPersonGroupId;

    public PersonGridViewAdapter(String personGroupId, Context context) {
        mContext = context;
        mPersonGroupId = personGroupId;
        longPressed = false;
        personIdList = new ArrayList<>();
        personChecked = new ArrayList<>();
        Set<String>
            personIdSet = StorageHelper.getAllPersonIds(personGroupId, context);
        for (String personId : personIdSet) {
            personIdList.add(personId);
            personChecked.add(false);
        }
    }

    @Override
    public int getCount() {
        return personIdList.size();
    }

    @Override
    public Object getItem(int position) {
        return personIdList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.item_person, parent, false);
        }
        convertView.setId(position);

        String personId = personIdList.get(position);
        ImageView mImage = convertView.findViewById(R.id.image_person);
        Set<String> faceIdSet = StorageHelper.getAllFaceIds(personId, mContext);

        if (!faceIdSet.isEmpty()) {
            Iterator<String> it = faceIdSet.iterator();
            Uri uri = Uri.parse(StorageHelper.getFaceUri(it.next(), mContext));
            mImage.setImageURI(uri);
        } else {
            Drawable drawable = mContext.getResources().getDrawable(R.drawable.select_image);
            mImage.setImageDrawable(drawable);
        }

        TextView mText = convertView.findViewById(R.id.text_person);

        String personName = StorageHelper.getPersonName(personId, mPersonGroupId, mContext);
        mText.setText(personName);
        CheckBox checkBox = convertView.findViewById(R.id.checkbox_person);
        if (longPressed) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    personChecked.set(position, isChecked);
                }
            });
            checkBox.setChecked(personChecked.get(position));
        } else {
            checkBox.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }
}

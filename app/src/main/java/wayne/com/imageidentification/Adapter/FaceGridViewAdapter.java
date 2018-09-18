package wayne.com.imageidentification.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import wayne.com.imageidentification.Helper.ImageHelper;
import wayne.com.imageidentification.R;

public class FaceGridViewAdapter extends BaseAdapter {
    public List<UUID> faceIdList;
    public List<FaceRectangle> faceRectList;
    public List<Bitmap> faceThumbnails;
    public List<Boolean> faceChecked;
    public Bitmap mBitmap;
    public Context mContext;

    public FaceGridViewAdapter(Face[] detectionResult, Bitmap bitmap, Context context) {
        mContext = context;
        mBitmap = bitmap;
        faceIdList = new ArrayList<>();
        faceRectList = new ArrayList<>();
        faceThumbnails = new ArrayList<>();
        faceChecked = new ArrayList<>();
        if (detectionResult != null) {
            List<Face> faces = Arrays.asList(detectionResult);
            for (Face face : faces) {
                try {
                    // Crop face thumbnail with five main landmarks drawn from original image.
                    faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                        bitmap, face.faceRectangle));
                    faceIdList.add(null);
                    faceRectList.add(face.faceRectangle);
                    faceChecked.add(true);
                } catch (IOException e) {
                    // Show the exception when generating face thumbnail fails.
                }
            }
        }
    }

    @Override
    public int getCount() {
        return faceRectList.size();
    }

    @Override
    public Object getItem(int position) {
        return faceRectList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView =
                layoutInflater.inflate(R.layout.item_face_with_checkbox, parent, false);
        }
        convertView.setId(position);

        ((ImageView)convertView.findViewById(R.id.image_face))
            .setImageBitmap(faceThumbnails.get(position));

        // set the checked status of the item
        CheckBox checkBox = convertView.findViewById(R.id.checkbox_face);
        checkBox.setChecked(faceChecked.get(position));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                faceChecked.set(position, isChecked);
            }
        });

        return convertView;    }
}

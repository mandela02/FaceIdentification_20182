package wayne.com.imageidentification.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wayne.com.imageidentification.Helper.ImageHelper;
import wayne.com.imageidentification.Helper.StorageHelper;
import wayne.com.imageidentification.R;

public class FaceListAdapter extends BaseAdapter {
    public List<Face> mFaces;
    public List<IdentifyResult> mIdentifyResults;
    public List<Bitmap> mFaceThumbnails;
    public Context mContext;
    public String mPersonGroupId;

    public FaceListAdapter(Face[] detectionResult, Bitmap imageBitmap, String id, Context context) {
        mContext = context;
        mPersonGroupId = id;
        mFaces = new ArrayList<>();
        mFaceThumbnails = new ArrayList<>();
        mIdentifyResults = new ArrayList<>();
        if (detectionResult != null) {
            mFaces = Arrays.asList(detectionResult);
            for (Face face : mFaces) {
                try {
                    // Crop face thumbnail with five main landmarks drawn from original image.
                    mFaceThumbnails.add(ImageHelper.generateFaceThumbnail(
                        imageBitmap, face.faceRectangle));
                } catch (IOException e) {
                    // Show the exception when generating face thumbnail fails.
                }
            }
        }
    }

    public void setIdentificationResult(IdentifyResult[] identifyResults) {
        mIdentifyResults = Arrays.asList(identifyResults);
    }

    @Override
    public int getCount() {
        return mFaces.size();
    }

    @Override
    public Object getItem(int position) {
        return mFaces.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater =
                (LayoutInflater) mContext.getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(
                R.layout.item_face_with_description, parent, false);
        }
        convertView.setId(position);
        ImageView mImageView = convertView.findViewById(R.id.face_thumbnail);
        TextView mTextView = convertView.findViewById(R.id.text_detected_face);
        mImageView.setImageBitmap(mFaceThumbnails.get(position));
        if (mIdentifyResults.size() == mFaces.size()) {
            DecimalFormat formatter = new DecimalFormat("#0.00");
            if (mIdentifyResults.get(position).candidates.size() > 0) {
                String personId =
                    mIdentifyResults.get(position).candidates.get(0).personId.toString();
                String personName = StorageHelper.getPersonName(
                    personId, mPersonGroupId, mContext);
                String identity = "Person: " + personName + "\n"
                    + "Confidence: " + formatter.format(
                    mIdentifyResults.get(position).candidates.get(0).confidence);
                mTextView.setText(identity);
            } else {
                mTextView.setText("Unknown person");
            }
        }
        return convertView;
    }
}

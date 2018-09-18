package wayne.com.imageidentification;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import wayne.com.imageidentification.Adapter.ViewPagerAdapter;
import wayne.com.imageidentification.Fragment.GroupFragment;
import wayne.com.imageidentification.Fragment.IdentifyFragment;

public class MainActivity extends AppCompatActivity implements
    BottomNavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener {
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    public BottomNavigationView mNavigation;
    public IdentifyFragment fragmentMain;
    public GroupFragment fragmentGroup;
    ViewPager mViewPager;
    MenuItem prevMenuItem;

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNavigation = findViewById(R.id.navigation);
        mViewPager = findViewById(R.id.fragment_container);
        mNavigation.setOnNavigationItemSelectedListener(this);
        setupViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(this);
        addPermission();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void addPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                MY_CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }//end onRequestPermissionsResult

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        fragmentMain = new IdentifyFragment();
        fragmentGroup = new GroupFragment();
        viewPagerAdapter.addFragment(fragmentMain);
        viewPagerAdapter.addFragment(fragmentGroup);
        viewPager.setAdapter(viewPagerAdapter);
    }

    private boolean loadFragment(Fragment fragment) {
        //switching fragment
        if (fragment != null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        switch (item.getItemId()) {
            case R.id.navigation_home:
                mViewPager.setCurrentItem(0);
                break;
            case R.id.navigation_dashboard:
                mViewPager.setCurrentItem(1);
                break;
        }
        return loadFragment(fragment);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (prevMenuItem != null) {
            prevMenuItem.setChecked(false);
        } else {
            mNavigation.getMenu().getItem(0).setChecked(false);
        }
        Log.d("page", "onPageSelected: " + position);
        mNavigation.getMenu().getItem(position).setChecked(true);
        prevMenuItem = mNavigation.getMenu().getItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}

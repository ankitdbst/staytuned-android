package com.android.app.tvbuff;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, FiltersDialogFragment.FiltersDialogListener {
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;
    //TODO Cleanup and remove notification image stored in internal storage also. Clubbing id and starttime by removing : for long.parse operations
    String mCategory;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private ProgrammesFragment programmesFragment;
    private int notificationSubscriptionsCleanupLimit = 20;
    private DrawerLayout mDrawerLayout;

    private static long getFragmentId(String category, int position) {
        int idx = Arrays.asList(ProgrammesFragment.programmeCategories).indexOf(category)+1;
        return idx*10 + position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //final ActionBar actionBar = getSupportActionBar();
        Toolbar actionBar = (Toolbar) findViewById(R.id.toolbar);
        if(actionBar!=null) {
            setSupportActionBar(actionBar);
            actionBar.setTitleTextColor(Color.WHITE);
            actionBar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
//            actionBar.setTitle(mCategory);
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setStatusBarBackgroundColor(Color.parseColor("#2196F3"));
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
//        mTitle = getTitle();
//        mTitle = mCategory;

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));

//        mViewPager.setOnPageChangeListener(
//                new ViewPager.SimpleOnPageChangeListener() {
//                    @Override
//                    public void onPageSelected(int position) {
//                        // When swiping between pages, select the
//                        // corresponding tab.
//                        //getSupportActionBar().setSelectedNavigationItem(position);
//                    }
//                });
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        //Cleanup of Subscriptions preferences and notification image files
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                //background task
                cleanUpSubscriptionPrefs();
            }
        };
        new Thread(runnable).start();

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // show the given tab
                mViewPager.setCurrentItem(tab.getPosition());
            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }
        };

        for (String language : ProgrammesFragment.programmeLanguages) {
            //actionBar.addTab(actionBar.newTab().setText(language).setTabListener(tabListener));
            tabLayout.addTab(tabLayout.newTab().setText(language));
        }
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mNavigationDrawerFragment.updateDrawerCallback(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(Map<String, String> item) {
        mCategory = item.get(NavigationDrawerFragment.ITEM_CATEGORY);
        if (mViewPager != null) {
            mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
        }
    }

    private String getStringResourceByName(String aString) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(aString, "string", packageName);
        return getString(resId);
    }

    public void restoreActionBar() {
        mTitle = getStringResourceByName("title_" + mCategory);
        ActionBar actionBar = getSupportActionBar();
       // actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            restoreActionBar();
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        int position = getSupportActionBar().getSelectedNavigationIndex();

        String fragmentName = "android:switcher:" + R.id.pager + ":" +
                MainActivity.getFragmentId(mCategory, position);
        ProgrammesFragment fragment =
                (ProgrammesFragment) getSupportFragmentManager().findFragmentByTag(fragmentName);
        fragment.loadData(true);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    private void cleanUpSubscriptionPrefs() {
        final SharedPreferences notificationSubscribed = this.getSharedPreferences(ProgrammesFragment.NOTIFICATION_PREF, 0);
        final Map<String,?> storedData = notificationSubscribed.getAll();
        if(storedData.size() >= notificationSubscriptionsCleanupLimit) {
            //remove all past notifications which are already fired
            SharedPreferences.Editor editor = notificationSubscribed.edit();
            String id = "";
            for(String key:storedData.keySet()) {
                long stoptime = 0;
                try {
                    JSONObject programmejson = new JSONObject(storedData.get(key).toString());
                    stoptime = programmejson.getLong("stoptime");
                    id = programmejson.getString("id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(stoptime < System.currentTimeMillis()) {
                    editor.remove(key);
                    //deleting the compressed notification image also
                    this.deleteFile(id);
                }
            }
            editor.apply();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            return ProgrammesFragment.newInstance(mCategory,
                    ProgrammesFragment.programmeLanguages[position]);
        }

        @Override
        public long getItemId(int position) {
            return MainActivity.getFragmentId(mCategory, position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
    }
}

package com.android.app.tvbuff;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends ActionBarActivity
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

    private static long getFragmentId(String category, int position) {
        int idx = Arrays.asList(ProgrammesFragment.programmeCategories).indexOf(category)+1;
        return idx*10 + position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ActionBar actionBar = getSupportActionBar();
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));

        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getSupportActionBar().setSelectedNavigationItem(position);
                    }
                });


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
            actionBar.addTab(actionBar.newTab().setText(language).setTabListener(tabListener));
        }

        mNavigationDrawerFragment.updateDrawerCallback(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(Map<String, String> item) {
        mCategory = item.get(NavigationDrawerFragment.ITEM_CATEGORY);
        if (mViewPager != null) {
            mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
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

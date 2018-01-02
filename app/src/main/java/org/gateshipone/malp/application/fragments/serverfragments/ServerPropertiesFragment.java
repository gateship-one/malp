/*
 *  Copyright (C) 2018 Team Gateship-One
 *  (Hendrik Borghorst & Frederik Luetkes)
 *
 *  The AUTHORS.md file contains a detailed contributors list:
 *  <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.malp.application.fragments.serverfragments;


import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;

public class ServerPropertiesFragment extends Fragment implements TabLayout.OnTabSelectedListener  {
    public static final String TAG = ServerPropertiesFragment.class.getSimpleName();

    private ServerPropertiesTabAdapter mTabAdapter;

    private FABFragmentCallback mFABCallback = null;

    private ViewPager mViewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_tab_pager, container, false);


        // create tabs
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.my_music_tab_layout);

        mViewPager = (ViewPager) rootView.findViewById(R.id.my_music_viewpager);
        mTabAdapter = new ServerPropertiesTabAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mTabAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(this);

        // Icons
        final ColorStateList tabColors = tabLayout.getTabTextColors();
        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.ic_statistics_black_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }
        drawable = res.getDrawable(R.drawable.ic_hearing_black_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }

        mViewPager.setCurrentItem(0);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (null != mFABCallback) {
            mFABCallback.setupFAB(false, null);
            if ( mViewPager.getCurrentItem() == 0 ) {
                mFABCallback.setupToolbar(getString(R.string.menu_statistic), false, true, false);
            } else if (mViewPager.getCurrentItem() == 1) {
                mFABCallback.setupToolbar(getString(R.string.menu_outputs), false, true, false);
            }

        }
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);


        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        View view = this.getView();

        if (view != null ) {
            ViewPager myMusicViewPager = (ViewPager) view.findViewById(R.id.my_music_viewpager);
            myMusicViewPager.setCurrentItem(tab.getPosition());

            if (null != mFABCallback) {
                if (tab.getPosition() == 0) {
                    mFABCallback.setupToolbar(getString(R.string.menu_statistic), false, true, false);
                } else if (tab.getPosition() == 1) {
                    mFABCallback.setupToolbar(getString(R.string.menu_outputs), false, true, false);
                }
            }
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    private class ServerPropertiesTabAdapter extends FragmentStatePagerAdapter {
        static final int NUMBER_OF_PAGES = 2;

        private SparseArray<GenericMPDFragment> mRegisteredFragments;

        public ServerPropertiesTabAdapter(FragmentManager fm) {
            super(fm);
            mRegisteredFragments = new SparseArray<>();
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new ServerStatisticFragment();
                case 1:
                    return new OutputsFragment();
                default:
                    return null;
            }
        }
    }
}

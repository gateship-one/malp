/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package andrompd.org.andrompd.application.fragments.database;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import andrompd.org.andrompd.R;

public class MyMusicTabsFragment extends Fragment implements TabLayout.OnTabSelectedListener{
    public final static String TAG = MyMusicTabsFragment.class.getSimpleName();
    public final static String MY_MUSIC_REQUESTED_TAB = "ARG_REQUESTED_TAB";


    public enum DEFAULTTAB {
        ARTISTS, ALBUMS
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_mymusic_tabs, container, false);


        // create tabs
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.my_music_tab_layout);

        // Icons
        final ColorStateList tabColors = tabLayout.getTabTextColors();
        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.ic_recent_actors_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }
        drawable = res.getDrawable(R.drawable.ic_album_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }
//        drawable = res.getDrawable(R.drawable.ic_my_library_music_24dp, null);
//        if (drawable != null) {
//            Drawable icon = DrawableCompat.wrap(drawable);
//            DrawableCompat.setTintList(icon, tabColors);
//            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
//        }
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        ViewPager myMusicViewPager = (ViewPager) rootView.findViewById(R.id.my_music_viewpager);
        MyMusicPagerAdapter adapterViewPager = new MyMusicPagerAdapter(getChildFragmentManager());
        myMusicViewPager.setAdapter(adapterViewPager);
        myMusicViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(this);

        // set start page
        Bundle args = getArguments();

        DEFAULTTAB tab = DEFAULTTAB.ALBUMS;

        if (args != null) {
            tab = DEFAULTTAB.values()[args.getInt(MY_MUSIC_REQUESTED_TAB)];
        }

        switch (tab) {
            case ARTISTS:
                myMusicViewPager.setCurrentItem(0);
                break;
            case ALBUMS:
                myMusicViewPager.setCurrentItem(1);
                break;
//            case TRACKS:
//                myMusicViewPager.setCurrentItem(2);
//                break;
        }

//        // set up play button
//        activity.setUpPlayButton(null);


        return rootView;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        View view = this.getView();

        if (view != null) {
            ViewPager myMusicViewPager = (ViewPager) view.findViewById(R.id.my_music_viewpager);
            myMusicViewPager.setCurrentItem(tab.getPosition());

            View.OnClickListener listener = null;

            switch (tab.getPosition()) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    break;
            }

        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    public static class MyMusicPagerAdapter extends FragmentStatePagerAdapter {
        static final int NUMBER_OF_PAGES = 2;

        public MyMusicPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new ArtistsFragment();
                case 1:
                    return new AlbumsFragment();
//                case 2:
//                    return new AllTracksFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // this is done in order to reload all tabs
            return NUMBER_OF_PAGES;
        }
    }
}

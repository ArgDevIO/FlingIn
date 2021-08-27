package argdev.io.flingin.java.fragments;


import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

import argdev.io.flingin.R;
import argdev.io.flingin.java.adapters.FriendsSectionsPagerAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {

    private Toolbar mToolbar;

    private ViewPager mViewPager;
    private FriendsSectionsPagerAdapter mFriendsSectionsPagerAdapter;

    private TabLayout mTabLayout;

    public FriendsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_friends, container, false);

        mTabLayout = v.findViewById(R.id.friends_tabs);
        //Tabs
        mViewPager = v.findViewById(R.id.friends_tabPager);
        mFriendsSectionsPagerAdapter = new FriendsSectionsPagerAdapter(getChildFragmentManager());

        mViewPager.setAdapter(mFriendsSectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        return v;
    }

}

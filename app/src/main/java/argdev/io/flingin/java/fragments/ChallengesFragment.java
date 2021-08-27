package argdev.io.flingin.java.fragments;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import argdev.io.flingin.R;
import argdev.io.flingin.java.adapters.ChallengesSectionsPagerAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChallengesFragment extends Fragment {

    private static final String TAG = "FRAGMENT";
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    ChallengesSectionsPagerAdapter mChallengesSectionsPagerAdapter;

    public ChallengesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_challenges, container, false);

        mViewPager = v.findViewById(R.id.challenges_tabPager);
        mTabLayout = v.findViewById(R.id.challenges_tabs);
        mChallengesSectionsPagerAdapter = new ChallengesSectionsPagerAdapter(getChildFragmentManager());

        mViewPager.setAdapter(mChallengesSectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        return v;
    }


}

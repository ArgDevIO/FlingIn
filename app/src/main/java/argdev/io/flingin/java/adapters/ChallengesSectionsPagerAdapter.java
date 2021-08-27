package argdev.io.flingin.java.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import argdev.io.flingin.java.fragments.ChallengesFragment_Received;
import argdev.io.flingin.java.fragments.ChallengesFragment_Sent;

public class ChallengesSectionsPagerAdapter extends FragmentPagerAdapter {

    public ChallengesSectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return new ChallengesFragment_Sent();

            case 1:
                return new ChallengesFragment_Received();

            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    public CharSequence getPageTitle(int position) {

        switch (position) {
            case 0:
                return "SENT";
            case 1:
                return "RECEIVED";
            default:
                return null;
        }
    }
}

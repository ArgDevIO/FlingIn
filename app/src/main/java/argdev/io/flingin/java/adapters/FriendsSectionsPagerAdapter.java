package argdev.io.flingin.java.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import argdev.io.flingin.java.fragments.FriendsFragment_Chat;
import argdev.io.flingin.java.fragments.FriendsFragment_Friends;
import argdev.io.flingin.java.fragments.FriendsFragment_Requests;

public class FriendsSectionsPagerAdapter extends FragmentPagerAdapter {

    public FriendsSectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return new FriendsFragment_Requests();
            case 1:
                return new FriendsFragment_Chat();
            case 2:
                return new FriendsFragment_Friends();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    public CharSequence getPageTitle(int position) {

        switch (position) {
            case 0:
                return "REQUESTS";
            case 1:
                return "CHAT";
            case 2:
                return "FRIENDS";
            default:
                return null;
        }
    }
}

package org.codeisland.aggregato.client.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import org.codeisland.aggregato.client.fragment.UpcomingTab;

/**
 * @author AndroHN
 * @version 1.0
 */
public class UpcomingTabsAdapter extends FragmentPagerAdapter {

    boolean userLoginState;

    public void setUserLoginState(boolean userLoginState) {
        this.userLoginState = userLoginState;
    }

    public UpcomingTabsAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int index) {
        UpcomingTab tab = new UpcomingTab();

        // Filter for subscribed series only if user is logged in and second tab is selected
        tab.setFilterUserSubscribed(this.userLoginState && index == 0);

        return tab;
    }

    @Override
    public int getCount() {
        if (!this.userLoginState) {
            // Only one tab for all episodes if user is not logged in
            return 1;
        } else {
            // Return tab count
            return 2;
        }
    }

}
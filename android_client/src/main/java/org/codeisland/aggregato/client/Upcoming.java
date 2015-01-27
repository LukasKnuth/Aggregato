package org.codeisland.aggregato.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.codeisland.aggregato.client.adapter.UpcomingTabsAdapter;
import org.codeisland.aggregato.client.network.Endpoint;

/**
 * @author AndroHN
 * @version 1.0
 */
public class Upcoming extends ActionBarActivity {

    private boolean userLoginState;
    private ViewPager viewPager;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.upcoming);

        // Get user login state
        this.userLoginState = Endpoint.getLoginStatus(this);

        // Create PagerAdapter
        UpcomingTabsAdapter pagerAdapter = new UpcomingTabsAdapter(getSupportFragmentManager());
        pagerAdapter.setUserLoginState(this.userLoginState);

        // Set up ViewPager
        this.viewPager = (ViewPager) findViewById(R.id.upcoming_pager);
        this.viewPager.setAdapter(pagerAdapter);
        this.viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // Select the right tab for current page selected
                Upcoming.this.actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            @Override
            public void onPageScrollStateChanged(int arg0) {}
        });

        // Set up ap ActionBar
        this.actionBar = getSupportActionBar();
        this.actionBar.setTitle(getString(R.string.activity_upcoming_title));
        this.actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create TabListener for tabs
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                // Switch current page to selected position
                Upcoming.this.viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}
        };

        // Add tabs to ActionBar
        if (this.userLoginState) {
            // Show subscribed episodes only for logged in users
            this.actionBar.addTab(this.actionBar.newTab().setText(getString(R.string.activity_upcoming_tab_subscribed)).setTabListener(tabListener));
        }
        this.actionBar.addTab(this.actionBar.newTab().setText(getString(R.string.activity_upcoming_tab_all)).setTabListener(tabListener));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload activity after login status changes
        // TODO: this seems to be null in some situations?
        boolean newUserLoginState = Endpoint.getLoginStatus(this);
        if (newUserLoginState != this.userLoginState) {
            this.userLoginState = newUserLoginState;
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater m = getMenuInflater();
        m.inflate(R.menu.upcoming_actionbar, menu);

        // Hide logout item if not logged in
        menu.findItem(R.id.upcoming_actionbar_logout).setVisible(Endpoint.getLoginStatus(this));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upcoming_actionbar_watchlist:
                startActivity(new Intent(this, Watchlist.class));
                return true;
            case R.id.upcoming_actionbar_search:
                startActivity(new Intent(this, SearchSeries.class));
                return true;
            case R.id.upcoming_actionbar_reload:
                // TODO: Reload current tabs instead of restarting the activity
                finish();
                startActivity(getIntent());
                return true;
            case R.id.upcoming_actionbar_settings:
                startActivity(new Intent(this, Settings.class));
                return true;
            case R.id.upcoming_actionbar_logout:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog = builder.setCancelable(false)
                    .setTitle(R.string.logout)
                    .setMessage(R.string.activity_upcoming_logout_dialog_title)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Logout
                            Endpoint.clearAccountName(Upcoming.this);
                            finish();
                            startActivity(getIntent());
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Nothing
                        }
                    })
                    .create();
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
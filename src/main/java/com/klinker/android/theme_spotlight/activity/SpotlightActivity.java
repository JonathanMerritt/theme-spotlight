/*
 * Copyright (C) 2014 Klinker Apps, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.theme_spotlight.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.klinker.android.theme_spotlight.R;

public class SpotlightActivity extends Activity {

    private static final int EVOLVE_FRAGMENT = 0;
    private static final int TALON_FRAGMENT = 1;
    private static final int FEATURED_FRAGMENT = 2;

    private static final Typeface LIGHT_TEXT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final Typeface BOLD_TEXT = Typeface.create("sans-serif", Typeface.BOLD);

    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mTitle;
    private int mIcon;

    private View[] drawerButtons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotlight);

        // initialize the drawer
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer,
                R.drawable.ic_drawer, R.string.app_name, R.string.app_name) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
                getActionBar().setIcon(mIcon);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(R.string.app_name);
                getActionBar().setIcon(android.R.color.transparent);
                invalidateOptionsMenu();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawer.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        setupDrawerButtons();

        // initialize us to the evolve theme fragment
        switchFragments(EVOLVE_FRAGMENT);
    }

    /**
     * Called when drawer item is selected
     */
    private void switchFragments(int position) {
        // Create a new fragment
        Fragment fragment;
        switch (position) {
            case EVOLVE_FRAGMENT:
                fragment = new Fragment();
                break;
            case TALON_FRAGMENT:
                fragment = new Fragment();
                break;
            case FEATURED_FRAGMENT:
            default:
                fragment = new Fragment();
                break;
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        boldDrawerItem(position);
        setupActionbar(position);
        mDrawer.closeDrawer(Gravity.START);
    }

    /**
     * Correctly set title and icons in actionbar
     */
    private void setupActionbar(int position) {
        switch (position) {
            case EVOLVE_FRAGMENT:
                setTitle(R.string.evolve_sms_themes);
                mIcon = R.drawable.evolve_logo;
                break;
            case TALON_FRAGMENT:
                setTitle(R.string.talon_themes);
                mIcon = R.drawable.talon_logo;
                break;
            case FEATURED_FRAGMENT:
                setTitle(R.string.featured_themers);
                mIcon = android.R.color.transparent;
                break;
            default:
                setTitle(R.string.app_name);
                mIcon = android.R.color.transparent;
        }

        mTitle = getTitle().toString();
        getActionBar().setIcon(mIcon);
    }

    /**
     * Sets up click functionality in the drawer
     */
    private void setupDrawerButtons() {
        drawerButtons = new View[3];
        drawerButtons[0] = findViewById(R.id.evolve_button);
        drawerButtons[1] = findViewById(R.id.talon_button);
        drawerButtons[2] = findViewById(R.id.featured_button);

        drawerButtons[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchFragments(0);
            }
        });
        drawerButtons[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchFragments(1);
            }
        });
        drawerButtons[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchFragments(2);
            }
        });
    }

    /**
     * Handle bolding items in the navigation drawer when one is selected
     */
    private void boldDrawerItem(int position) {
        for (int i = 0; i < drawerButtons.length; i++) {
            if (i == position) {
                ((TextView) drawerButtons[i]).setTypeface(BOLD_TEXT);
            } else {
                ((TextView) drawerButtons[i]).setTypeface(LIGHT_TEXT);
            }
        }
    }

    /**
     * Handle drawer toggle changes
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    /**
     * Handle drawer toggle changes
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Handle actionbar menu item clicks
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Hide the search icon when drawer is open
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawer.isDrawerOpen(Gravity.START);
        // TODO hide search icon when opened
        // menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    public void onSettingsClicked(View v) {
        // TODO implement
    }

    public void onFeedbackClicked(View v) {
        // TODO implement
    }
}
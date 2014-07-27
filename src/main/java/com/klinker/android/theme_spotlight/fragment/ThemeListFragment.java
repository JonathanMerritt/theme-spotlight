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

package com.klinker.android.theme_spotlight.fragment;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.gc.android.market.api.MarketSession;
import com.gc.android.market.api.model.Market;
import com.klinker.android.theme_spotlight.R;
import com.klinker.android.theme_spotlight.activity.SpotlightActivity;
import com.klinker.android.theme_spotlight.adapter.ThemeAdapter;
import com.klinker.android.theme_spotlight.util.AppUtils;

import java.util.ArrayList;
import java.util.List;

public class ThemeListFragment extends AuthFragment {

    private static final String TAG = "ThemeListFragment";

    public static final String BASE_SEARCH = "base_search_parameter";
    public static final int NUM_THEMES_TO_QUERY = 10;
    private static final int FADE_DURATION = 400;
    private static final String EVOLVE_PACKAGE = "com.klinker.android.evolve_sms";
    private static final String TALON_PACKAGE = "com.klinker.android.twitter";

    private SpotlightActivity mContext;
    private Handler mHandler;

    private String mBaseSearch;
    private String currentSearch = "";
    private int currentSearchIndex = 0;

    private RecyclerView mRecyclerView;
    private View mProgressBar;
    private ThemeAdapter mAdapter;

    private boolean isSyncing = false;

    public static ThemeListFragment newInstance(String baseSearch) {
        ThemeListFragment frag = new ThemeListFragment();
        setArguments(frag, baseSearch);
        return frag;
    }

    public ThemeListFragment() {
        // all fragments should contain an empty constructor
    }

    public static void setArguments(ThemeListFragment frag, String baseSearch) {
        Bundle args = new Bundle();
        args.putString(BASE_SEARCH, baseSearch);
        frag.setArguments(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        superOnCreate(savedInstanceState);
        mBaseSearch = getArguments().getString(BASE_SEARCH);
    }

    public void superOnCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = (SpotlightActivity) activity;
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        superOnCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_theme_list, null);

        mRecyclerView = (RecyclerView) v.findViewById(android.R.id.list);
        setUpRecyclerView();
        mProgressBar = v.findViewById(R.id.loading_progress);

        mAdapter = new ThemeAdapter(this, new ArrayList<Market.App>());
        setRecyclerViewAdapter(mAdapter);

        if (isTwoPane()) {
            v.setBackgroundColor(getResources().getColor(android.R.color.white));
        }

        return v;
    }

    public boolean isTwoPane() {
        return mContext.isTwoPane();
    }

    public void themeItemClicked(Market.App app) {
        mContext.themeItemClicked(app);
    }

    public View superOnCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void setUpRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        // item animator looks funny with the loading view, just let the items fade in instead
        // this also was causing some issues with blank item views, presumably because of the loading footer again
        mRecyclerView.setItemAnimator(null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // start with only grabbing 4 themes, this fixes a bug in the recycler view causing it to scroll
        // down when the initial themes are loaded, weird
        getThemes(currentSearchIndex, 4);
    }

    public void getThemes() {
        getThemes(currentSearchIndex);
    }

    public void getThemes(int startIndex) {
        getThemes(startIndex, NUM_THEMES_TO_QUERY);
    }

    // get all of the themes in the supplied range. Due to API limitations, I can only get
    // 10 at a time before needing to load more
    private void getThemes(final int startIndex, final int length) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isSyncing = true;
                try {
                    MarketSession session = new MarketSession();
                    session.getContext().setAuthSubToken(mContext.getAuthToken().getAuthToken());
                    session.getContext().setAndroidId(mContext.getAuthToken().getAndroidId());

                    // create a simple query
                    final String query = getSearch(currentSearch);
                    Market.AppsRequest appsRequest = Market.AppsRequest.newBuilder()
                            .setQuery(query)
                            .setStartIndex(startIndex)
                            .setEntriesCount(length)
                            .setWithExtendedInfo(true) // get extended info so that we can verify the theme name against
                            .build();                  // either the name evolvesms or talon

                    currentSearchIndex += length;

                    try {
                        // pause the loading for a short amount of time, this helps the recycler view
                        // keep up and prevents it from scrolling janky when more views are added
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }

                    session.append(appsRequest, new MarketSession.Callback<Market.AppsResponse>() {
                        @Override
                        public void onResult(Market.ResponseContext context, Market.AppsResponse response) {
                            ArrayList<Market.App> apps = new ArrayList<Market.App>(response.getAppList());

                            // check the first for whether or not it is evolve or talon, and if so
                            // strip it out as we don't want to display it. want to also verify the query
                            // is correct so that later if I choose to do a Klinker Apps featured themer as
                            // an example, it will still show those packages
                            if (apps.size() > 0) {
                                if ((apps.get(0).getPackageName().equals(EVOLVE_PACKAGE) && query.contains(SpotlightActivity.EVOLVE_SMS)) ||
                                        (apps.get(0).getPackageName().equals(TALON_PACKAGE) && query.contains(SpotlightActivity.TALON))) {
                                    apps.remove(0);
                                }
                            }

                            setApps(apps);
                            isSyncing = false;
                        }
                    });
                    session.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void getMoreThemes() {
        getThemes(currentSearchIndex);
    }

    // set the apps to the listview and initialize other parts of the list
    public void setApps(final List<Market.App> apps) {
        final String verifyTitle;
        if (mBaseSearch.equals(SpotlightActivity.EVOLVE_SMS)) {
            verifyTitle = AppUtils.EVOLVE;
        } else {
            verifyTitle = AppUtils.TALON;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                afterListAdapterSet();

                for (Market.App app : apps) {
                    if (AppUtils.shouldAddApp(getActivity(), app, verifyTitle, mBaseSearch)) {
                        mAdapter.add(app, mAdapter.getRealItemCount());
                    }
                }
            }
        });
    }

    public void afterListAdapterSet() {
        if (mAdapter.getRealItemCount() == 0) {
            ObjectAnimator listAnimator = ObjectAnimator.ofFloat(mRecyclerView, View.ALPHA, 0.0f, 1.0f);
            listAnimator.setDuration(FADE_DURATION);
            listAnimator.start();
            ObjectAnimator progressAnimator = ObjectAnimator.ofFloat(mProgressBar, View.ALPHA, 1.0f, 0.0f);
            progressAnimator.setDuration(FADE_DURATION);
            progressAnimator.start();

            // after the first run, immediately get more themes since we can only
            // pull 10 at a time
            getMoreThemes();
        }
    }

    // combine the base search and current search param
    public String getSearch(String search) {
        return mBaseSearch + " " + search;
    }

    public void syncMoreThemes(int position) {
        if ((position >= mAdapter.getRealItemCount() - 2 || !currentSearch.equals("")) && !isSyncing) {
            isSyncing = true;
            getMoreThemes();
        }
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void setRecyclerViewAdapter(RecyclerView.Adapter adapter) {
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public boolean isSearchable() {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String search) {
        if (search.equals(currentSearch)) {
            return false;
        }

        ObjectAnimator listAnimator = ObjectAnimator.ofFloat(mRecyclerView, View.ALPHA, 1.0f, 0.0f);
        listAnimator.setDuration(FADE_DURATION);
        listAnimator.start();
        ObjectAnimator progressAnimator = ObjectAnimator.ofFloat(mProgressBar, View.ALPHA, 0.0f, 1.0f);
        progressAnimator.setDuration(FADE_DURATION);
        progressAnimator.start();

        mAdapter.removeAll();
        currentSearchIndex = 0;

        return onQueryTextSubmitted(search);
    }

    @Override
    public boolean onQueryTextSubmitted(String search) {
        currentSearch = search;
        syncMoreThemes(currentSearchIndex);

        return true;
    }
}

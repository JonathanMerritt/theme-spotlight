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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.gc.android.market.api.MarketSession;
import com.gc.android.market.api.model.Market;
import com.klinker.android.theme_spotlight.R;
import com.klinker.android.theme_spotlight.activity.SpotlightActivity;
import com.klinker.android.theme_spotlight.activity.ThemeActivity;
import com.klinker.android.theme_spotlight.adapter.ThemeArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class ThemeListFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String TAG = "ThemeListFragment";

    public static final String BASE_SEARCH = "base_search_parameter";
    private static final int NUM_THEMES_TO_QUERY = 10;
    private static final String EVOLVE_PACKAGE = "com.klinker.android.evolve_sms";
    private static final String TALON_PACKAGE = "com.klinker.android.twitter";

    private SpotlightActivity mContext;
    private Handler mHandler;

    private String mBaseSearch;
    private String currentSearch = "";
    private int currentSearchIndex = 0;
    private List<Market.App> mApps;

    private ListView mListView;
    private ThemeArrayAdapter adapter;

    // get an instance of this fragment
    public static ThemeListFragment newInstance(String baseSearch) {
        ThemeListFragment frag = new ThemeListFragment();
        setArguements(frag, baseSearch);
        return frag;
    }

    public ThemeListFragment() {
        // all fragments should contain an empty constructor
    }

    // set up our base search via arguments
    public static void setArguements(ThemeListFragment frag, String baseSearch) {
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
        // get the themes that we want to display, can only load 10 at a time
        getThemes(currentSearchIndex);

        mListView = inflateListView(inflater);
        return mListView;
    }

    // set up our view, broken out for testing purposes
    public ListView inflateListView(LayoutInflater inflater) {
        ListView list = (ListView) inflater.inflate(R.layout.fragment_theme_list, null);
        list.setOnItemClickListener(this);

        if (mContext.isTwoPane()) {
            list.setBackgroundResource(android.R.color.white);
        }

        return list;
    }

    public void getThemes(int startIndex) {
        getThemes(startIndex, NUM_THEMES_TO_QUERY);
    }

    // get all of the themes in the supplied range
    private void getThemes(final int startIndex, final int length) {
        // network call, so we need to start a thread and then post a callback back to the ui
        // once the call is completed
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // create our session to look at themes from
                    MarketSession session = new MarketSession();
                    session.getContext().setAuthSubToken(mContext.getAuthToken().getAuthToken());
                    session.getContext().setAndroidId(mContext.getAuthToken().getAndroidId());

                    // create a simple query
                    final String query = getSearch(currentSearch);
                    Market.AppsRequest appsRequest = Market.AppsRequest.newBuilder()
                            .setQuery(query)
                            .setStartIndex(startIndex)
                            .setEntriesCount(length)
                            .setWithExtendedInfo(false) // don't need extended info, this will slow us down
                            .build();

                    // post our request
                    session.append(appsRequest, new MarketSession.Callback<Market.AppsResponse>() {
                        @Override
                        public void onResult(Market.ResponseContext context, Market.AppsResponse response) {
                            // response.getAppList() is marked as unmodifiable, we need to change that so we can
                            // strip items out when necessary
                            ArrayList<Market.App> apps = new ArrayList<Market.App>(response.getAppList());

                            // check the first for whether or not it is evolve or talon, and if so
                            // strip it out as we don't want to display it. want to also verify the query
                            // is correct so that later if I choose to do a Klinker Apps featured themer as
                            // an example, it will still show those packages
                            if ((apps.get(0).getPackageName().equals(EVOLVE_PACKAGE) && query.contains(SpotlightActivity.EVOLVE_SMS)) ||
                                    (apps.get(0).getPackageName().equals(TALON_PACKAGE) && query.contains(SpotlightActivity.TALON))) {
                                apps.remove(0);
                            }

                            setApps(apps);
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
        currentSearchIndex += NUM_THEMES_TO_QUERY;
        getThemes(currentSearchIndex);
    }

    // set the apps to the listview and initialize other parts of the list
    public void setApps(final List<Market.App> apps) {
        mApps = apps;
        setListAdapterPost(mHandler, apps);
    }

    public void setListAdapterPost(Handler handler, final List<Market.App> apps) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // if we haven't yet set an adapter, set it now. If we have already, just
                // notify that our data has changed and it should reload
                if (adapter == null) {
                    adapter = new ThemeArrayAdapter(mContext, apps);
                    mListView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    // combine the base search and current search param
    public String getSearch(String search) {
        return mBaseSearch + " " + search;
    }

    public List<Market.App> getApps() {
        return mApps;
    }

    public ListView getList() {
        return mListView;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Market.App clickedApp = mApps.get(i);

        // if this is a single pane view, then start a new activity to display our theme
        // if this is a dual pane view, then post this to the spotlight activity themeItemClicked
        // where we will then display that theme in a fragment on the screen
        if (mContext.isTwoPane()) {
            mContext.themeItemClicked(clickedApp);
        } else {
            Intent intent = new Intent(getActivity(), ThemeActivity.class);
            intent.putExtra(ThemeFragment.ARG_PACKAGE_NAME, clickedApp.getPackageName());
            startActivity(intent);
        }
    }
}

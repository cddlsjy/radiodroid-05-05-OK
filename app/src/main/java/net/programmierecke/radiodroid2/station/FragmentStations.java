package net.programmierecke.radiodroid2.station;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.FragmentBase;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.StationSaveManager;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;
import net.programmierecke.radiodroid2.utils.CustomFilter;
import net.programmierecke.radiodroid2.utils.FocusManager;

import java.util.ArrayList;
import java.util.List;

public class FragmentStations extends FragmentBase implements IFragmentSearchable {
    private static final String TAG = "FragmentStations";

    public static final String KEY_SEARCH_ENABLED = "SEARCH_ENABLED";

    private RecyclerView rvStations;
    private ViewGroup layoutError;
    private MaterialButton btnRetry;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SharedPreferences sharedPref;

    private boolean searchEnabled = false;

    private StationsFilter stationsFilter;
    private StationsFilter.SearchStyle lastSearchStyle = StationsFilter.SearchStyle.ByName;
    private String lastQuery = "";
    private StationSaveManager queue;
    
    // 焦点管理器
    private FocusManager focusManager;

    void onStationClick(DataRadioStation theStation, int pos) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        Utils.showPlaySelection(radioDroidApp, theStation, getActivity().getSupportFragmentManager());
    }

    @Override
    protected void RefreshListGui() {
        loadData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("STATIONS","onCreateView()");
        queue = new StationSaveManager(getContext());
        Bundle bundle = getArguments();
        if (bundle != null) {
            searchEnabled = bundle.getBoolean(KEY_SEARCH_ENABLED, false);
        }

        View view = inflater.inflate(R.layout.fragment_stations_remote, container, false);
        rvStations = (RecyclerView) view.findViewById(R.id.recyclerViewStations);
        layoutError = view.findViewById(R.id.layoutError);
        btnRetry = view.findViewById(R.id.btnRefresh);

        rvStations.setAdapter(null);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations.setLayoutManager(llm);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvStations.getContext(),
                llm.getOrientation());
        rvStations.addItemDecoration(dividerItemDecoration);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setEnabled(true);
        swipeRefreshLayout.setOnRefreshListener(() -> loadData());

        btnRetry.setOnClickListener(v -> loadData());

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (getActivity() != null) {
            ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
            adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
                @Override
                public void onStationClick(DataRadioStation station, int pos) {
                    FragmentStations.this.onStationClick(station, pos);
                }

                @Override
                public void onStationSwiped(DataRadioStation station) {
                }

                @Override
                public void onStationMoved(int from, int to) {
                }

                @Override
                public void onStationMoveFinished() {
                }
            });

            if (searchEnabled) {
                stationsFilter = adapter.getFilter();

                stationsFilter.setDelayer(new CustomFilter.Delayer() {
                    private int previousLength = 0;

                    public long getPostingDelay(CharSequence constraint) {
                        if (constraint == null) {
                            return 0;
                        }

                        long delay = 0;
                        if (constraint.length() < previousLength) {
                            delay = 500;
                        }
                        previousLength = constraint.length();

                        return delay;
                    }
                });

                adapter.setFilterListener(searchStatus -> {
                    layoutError.setVisibility(searchStatus == StationsFilter.SearchStatus.ERROR ? View.VISIBLE : View.GONE);
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
                    swipeRefreshLayout.setRefreshing(false);
                });

                btnRetry.setOnClickListener(v -> search(lastSearchStyle, lastQuery));
            }

            rvStations.setAdapter(adapter);
            
            focusManager = new FocusManager(rvStations);
            
            loadData();
            
            if (lastQuery != null && stationsFilter != null){
                Log.d("STATIONS", "do queued search for: "+lastQuery + " style="+lastSearchStyle);
                stationsFilter.clearList();
                search(lastSearchStyle, lastQuery);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rvStations.setAdapter(null);
    }

    private void loadData() {
        if (Utils.isOfflineMode(getContext())) {
            rvStations.setVisibility(View.GONE);
            layoutError.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        showLoading(true);
        new LoadStationsTask().execute();
    }
    
    private class LoadStationsTask extends AsyncTask<Void, Void, List<DataRadioStation>> {
        @Override
        protected List<DataRadioStation> doInBackground(Void... voids) {
            try {
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), "json/stations", true, null);
                if (result != null) {
                    List<DataRadioStation> stations = DataRadioStation.DecodeJson(result);
                    return stations != null ? stations : new ArrayList<>();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading stations", e);
            }
            return new ArrayList<>();
        }
        
        @Override
        protected void onPostExecute(List<DataRadioStation> stations) {
            if (stations != null && !stations.isEmpty()) {
                Context ctx = getContext();
                if (sharedPref == null) {
                    sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
                }

                boolean show_broken = sharedPref.getBoolean("show_broken", false);
                List<DataRadioStation> filteredStationsList = new ArrayList<>();
                queue.clear();
                
                for (DataRadioStation station : stations) {
                    queue.add(station);
                    if (show_broken || station.Working) {
                        filteredStationsList.add(station);
                    }
                }

                ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();
                if (adapter != null) {
                    adapter.updateList(null, filteredStationsList);
                    if (searchEnabled) {
                        stationsFilter.filter("");
                    }
                }
                
                rvStations.setVisibility(View.VISIBLE);
                layoutError.setVisibility(View.GONE);
                Log.d(TAG, "Loaded " + filteredStationsList.size() + " stations from network");
            } else {
                rvStations.setVisibility(View.GONE);
                layoutError.setVisibility(View.VISIBLE);
                Log.e(TAG, "Error loading stations from network");
            }
            
            swipeRefreshLayout.setRefreshing(false);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
        }
    }

    @Override
    public void search(StationsFilter.SearchStyle searchStyle, String query) {
        Log.d("STATIONS", "query = "+query + " searchStyle="+searchStyle);
        lastQuery = query;
        lastSearchStyle = searchStyle;

        if (rvStations != null && searchEnabled) {
            Log.d("STATIONS", "query a = "+query);
            if (!TextUtils.isEmpty(query)) {
                showLoading(true);
                new SearchStationsTask().execute(searchStyle, query);
            } else {
                loadData();
            }
        }
    }
    
    private class SearchStationsTask extends AsyncTask<Object, Void, List<DataRadioStation>> {
        @Override
        protected List<DataRadioStation> doInBackground(Object... params) {
            StationsFilter.SearchStyle searchStyle = (StationsFilter.SearchStyle) params[0];
            String query = (String) params[1];
            
            try {
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                String urlPart;
                
                switch (searchStyle) {
                    case ByTagExact:
                        urlPart = "json/stations/bytag/" + query;
                        break;
                    case ByCountryCodeExact:
                        urlPart = "json/stations/bycountrycode/" + query;
                        break;
                    case ByLanguageExact:
                        urlPart = "json/stations/bylanguage/" + query;
                        break;
                    case ByName:
                    default:
                        urlPart = "json/stations/byname/" + query;
                        break;
                }
                
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), urlPart, true, null);
                if (result != null) {
                    List<DataRadioStation> stations = DataRadioStation.DecodeJson(result);
                    return stations != null ? stations : new ArrayList<>();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching stations", e);
            }
            return new ArrayList<>();
        }
        
        @Override
        protected void onPostExecute(List<DataRadioStation> stations) {
            if (stations != null) {
                Context ctx = getContext();
                if (sharedPref == null) {
                    sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
                }

                boolean show_broken = sharedPref.getBoolean("show_broken", false);
                List<DataRadioStation> filteredStationsList = new ArrayList<>();
                queue.clear();
                
                for (DataRadioStation station : stations) {
                    queue.add(station);
                    if (show_broken || station.Working) {
                        filteredStationsList.add(station);
                    }
                }
                
                ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();
                if (adapter != null) {
                    adapter.updateList(null, filteredStationsList);
                }
                
                rvStations.setVisibility(View.VISIBLE);
                layoutError.setVisibility(View.GONE);
                Log.d(TAG, "Search found " + filteredStationsList.size() + " stations");
            }
            
            swipeRefreshLayout.setRefreshing(false);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            rvStations.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(true);
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    void RefreshDownloadList(){
        loadData();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
        if (rvStations != null && focusManager != null) {
            View focusedView = rvStations.findFocus();
            if (focusedView != null) {
                int position = rvStations.getChildAdapterPosition(focusedView);
                if (position != RecyclerView.NO_POSITION) {
                    focusManager.saveLastFocusPosition(getContext(), position);
                }
            }
        }
    }

    public void initializeFocus() {
        if (focusManager == null || rvStations == null || rvStations.getAdapter() == null) {
            return;
        }

        rvStations.postDelayed(() -> {
            ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();
            int itemCount = adapter.getItemCount();

            if (itemCount == 0) {
                return;
            }

            int initialPosition = focusManager.getLastFocusPosition(getContext());
            if (initialPosition < 0 || initialPosition >= itemCount) {
                initialPosition = 0;
            }

            focusManager.initializeFocus(initialPosition);

            focusManager.setFocusStateListener((position, hasFocus) -> {
                if (hasFocus) {
                    focusManager.smoothScrollToFocusedItem();
                }
            });

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Focus initialized to position: " + initialPosition);
            }
        }, 500);
    }
}

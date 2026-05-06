package net.programmierecke.radiodroid2.station;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import net.programmierecke.radiodroid2.FragmentBase;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;

import java.util.ArrayList;
import java.util.List;

public class FragmentTopClick extends FragmentBase implements IFragmentSearchable {
    private static final String TAG = "FragmentTopClick";

    private RecyclerView recyclerViewStations;
    private ViewGroup layoutError;
    private MaterialButton btnRetry;
    private SwipeRefreshLayout swiperefresh;

    private ItemAdapterStation stationListAdapter;
    private StationsFilter.SearchStyle lastSearchStyle = StationsFilter.SearchStyle.ByName;
    private String lastSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations, container, false);

        recyclerViewStations = view.findViewById(R.id.recyclerViewStations);
        layoutError = view.findViewById(R.id.layoutError);
        swiperefresh = view.findViewById(R.id.swiperefresh);
        btnRetry = view.findViewById(R.id.btnRetry);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (getActivity() != null) {
            stationListAdapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
            stationListAdapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
                @Override
                public void onStationClick(DataRadioStation station, int pos) {
                    FragmentTopClick.this.onStationClick(station, pos);
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
            recyclerViewStations.setAdapter(stationListAdapter);
            recyclerViewStations.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerViewStations.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        }
        
        swiperefresh.setEnabled(true);
        swiperefresh.setOnRefreshListener(() -> loadData());
        
        btnRetry.setOnClickListener(v -> loadData());
        
        loadData();
    }

    private void onStationClick(DataRadioStation station, int pos) {
        if (getActivity() == null) {
            Log.e(TAG, "Activity is null, cannot play station");
            return;
        }
        
        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        if (radioDroidApp == null) {
            Log.e(TAG, "RadioDroidApp is null, cannot play station");
            return;
        }
        
        Utils.showPlaySelection(radioDroidApp, station, getActivity().getSupportFragmentManager());
    }

    private void loadData() {
        if (Utils.isOfflineMode(getContext())) {
            showError(true, "离线模式，不加载网络电台");
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
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), "json/stations/topclick", true, null);
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
                stationListAdapter.updateList(null, stations);
                showContent(true);
                Log.d(TAG, "Loaded " + stations.size() + " stations from network");
            } else {
                showError(true, "无法从网络获取电台数据，请检查网络连接");
            }
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            recyclerViewStations.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
            swiperefresh.setRefreshing(true);
        } else {
            swiperefresh.setRefreshing(false);
        }
    }

    private void showContent(boolean show) {
        showLoading(false);
        if (show) {
            recyclerViewStations.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
        } else {
            recyclerViewStations.setVisibility(View.GONE);
            layoutError.setVisibility(View.VISIBLE);
        }
    }

    private void showError(boolean show, String errorMessage) {
        showLoading(false);
        recyclerViewStations.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        
        if (errorMessage != null) {
            Log.e(TAG, "Error: " + errorMessage);
        }
    }

    @Override
    public void search(StationsFilter.SearchStyle searchStyle, String query) {
        lastSearchStyle = searchStyle;
        lastSearchQuery = query;
        
        if (query == null || query.isEmpty()) {
            loadData();
            return;
        }
        
        showLoading(true);
        new SearchStationsTask().execute(searchStyle, query);
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
            if (stations != null && !stations.isEmpty()) {
                stationListAdapter.updateList(null, stations);
                showContent(true);
                Log.d(TAG, "Search found " + stations.size() + " stations");
            } else {
                stationListAdapter.updateList(null, new ArrayList<>());
                showContent(true);
                Log.d(TAG, "Search found no stations");
            }
        }
    }
}

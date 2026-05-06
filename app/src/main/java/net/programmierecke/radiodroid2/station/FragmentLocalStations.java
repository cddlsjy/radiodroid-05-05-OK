package net.programmierecke.radiodroid2.station;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class FragmentLocalStations extends FragmentBase implements IFragmentSearchable {
    private static final String TAG = "FragmentLocalStations";

    private RecyclerView rvStations;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutError;
    private TextView textErrorMessage;
    private MaterialButton btnRetry;

    private ItemAdapterStation stationListAdapter;
    private StationsFilter.SearchStyle lastSearchStyle = StationsFilter.SearchStyle.ByName;
    private String lastSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations, container, false);

        rvStations = view.findViewById(R.id.recyclerViewStations);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        layoutError = view.findViewById(R.id.layoutError);
        textErrorMessage = view.findViewById(R.id.textErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);

        if (getContext() != null) {
            rvStations.setLayoutManager(new LinearLayoutManager(getContext()));
            rvStations.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        }
        
        rvStations.setAdapter(null);

        swipeRefreshLayout.setEnabled(true);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadData();
            swipeRefreshLayout.setRefreshing(false);
        });
        
        btnRetry.setOnClickListener(v -> loadData());

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
                    onStationClick(station, pos);
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
            rvStations.setAdapter(stationListAdapter);
        } else {
            Log.e(TAG, "Activity is null in onActivityCreated, cannot initialize adapter");
            showError("Activity不可用，请重启应用");
            return;
        }
        
        Bundle args = getArguments();
        if (args != null) {
            String url = args.getString("url", "");
            Log.d(TAG, "Received URL argument: " + url);
        }
        
        loadData();
    }

    private void loadData() {
        if (Utils.isOfflineMode(getContext())) {
            showError("离线模式，不加载网络电台");
            return;
        }
        showLoading(true);
        new LoadStationsTask().execute();
    }
    
    private class LoadStationsTask extends AsyncTask<Void, Void, List<DataRadioStation>> {
        @Override
        protected List<DataRadioStation> doInBackground(Void... voids) {
            try {
                java.util.Locale locale = java.util.Locale.getDefault();
                String systemCountry = locale.getCountry();
                String systemLanguage = locale.getLanguage();
                
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                
                String urlPart;
                if (systemCountry != null && !systemCountry.isEmpty()) {
                    urlPart = "json/stations/bycountrycode/" + systemCountry;
                    String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), urlPart, true, null);
                    if (result != null) {
                        List<DataRadioStation> stations = DataRadioStation.DecodeJson(result);
                        if (stations != null && !stations.isEmpty()) {
                            Log.d(TAG, "Loaded " + stations.size() + " stations by country: " + systemCountry);
                            return stations;
                        }
                    }
                }
                
                if (systemLanguage != null && !systemLanguage.isEmpty()) {
                    String languageCode = "zh".equals(systemLanguage) ? "chinese" : systemLanguage;
                    urlPart = "json/stations/bylanguage/" + languageCode;
                    String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), urlPart, true, null);
                    if (result != null) {
                        List<DataRadioStation> stations = DataRadioStation.DecodeJson(result);
                        if (stations != null && !stations.isEmpty()) {
                            Log.d(TAG, "Loaded " + stations.size() + " stations by language: " + languageCode);
                            return stations;
                        }
                    }
                }
                
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), "json/stations", true, null);
                if (result != null) {
                    List<DataRadioStation> stations = DataRadioStation.DecodeJson(result);
                    if (stations != null && !stations.isEmpty()) {
                        Log.d(TAG, "Loaded " + stations.size() + " stations");
                        return stations;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading stations", e);
            }
            return new ArrayList<>();
        }
        
        @Override
        protected void onPostExecute(List<DataRadioStation> stations) {
            if (stations != null && !stations.isEmpty()) {
                hideError();
                stationListAdapter.updateList(null, stations);
                Log.d(TAG, "Loaded " + stations.size() + " stations from network");
            } else {
                showError("无法从网络获取电台数据，请检查网络连接");
            }
            showLoading(false);
        }
    }

    private void showError(String message) {
        if (layoutError != null && textErrorMessage != null && rvStations != null) {
            textErrorMessage.setText(message);
            layoutError.setVisibility(View.VISIBLE);
            rvStations.setVisibility(View.GONE);
        } else {
            Log.e(TAG, "Error: " + message);
        }
    }

    private void hideError() {
        if (layoutError != null && rvStations != null) {
            layoutError.setVisibility(View.GONE);
            rvStations.setVisibility(View.VISIBLE);
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

    @Override
    public void search(StationsFilter.SearchStyle searchStyle, String query) {
        lastSearchStyle = searchStyle;
        lastSearchQuery = query;
        
        if (query != null && !query.isEmpty()) {
            showLoading(true);
            new SearchStationsTask().execute(searchStyle, query);
        } else {
            loadData();
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
                    if (stations != null && !stations.isEmpty()) {
                        return stations;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching stations", e);
            }
            return new ArrayList<>();
        }
        
        @Override
        protected void onPostExecute(List<DataRadioStation> stations) {
            if (stations != null && !stations.isEmpty()) {
                hideError();
                stationListAdapter.updateList(null, stations);
            } else {
                showError("没有找到匹配的电台");
            }
            showLoading(false);
        }
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
}

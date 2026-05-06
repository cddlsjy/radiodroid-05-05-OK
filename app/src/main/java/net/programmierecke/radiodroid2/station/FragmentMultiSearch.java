package net.programmierecke.radiodroid2.station;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import net.programmierecke.radiodroid2.FragmentBase;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.views.CustomSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentMultiSearch extends FragmentBase {
    private static final String TAG = "FragmentMultiSearch";

    private RecyclerView recyclerViewStations;
    private EditText etSearchQuery;
    private CustomSpinner spinnerCountry;
    private CustomSpinner spinnerLanguage;
    private CustomSpinner spinnerTag;
    private MaterialButton btnResetFilters;
    private MaterialButton btnToggleFilters;
    private MaterialButton btnExpandFilters;
    private ScrollView scrollViewFilters;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    
    private ItemAdapterStation stationListAdapter;
    
    private String selectedCountry = "";
    private String selectedLanguage = "";
    private String selectedTag = "";
    private String searchQuery = "";
    
    private List<String> countriesList = new ArrayList<>();
    private List<String> languagesList = new ArrayList<>();
    private List<String> tagsList = new ArrayList<>();
    
    private Handler searchHandler = new Handler();
    private static final long DEBOUNCE_DELAY = 500;
    private Runnable searchRunnable;
    
    private ArrayAdapter<String> countryAdapter;
    private ArrayAdapter<String> languageAdapter;
    private ArrayAdapter<String> tagAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_multi_search, container, false);

        recyclerViewStations = view.findViewById(R.id.recyclerViewStations);
        etSearchQuery = view.findViewById(R.id.etSearchQuery);
        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage);
        spinnerTag = view.findViewById(R.id.spinnerTag);
        btnResetFilters = view.findViewById(R.id.btnResetFilters);
        btnToggleFilters = view.findViewById(R.id.btnToggleFilters);
        btnExpandFilters = view.findViewById(R.id.btnExpandFilters);
        scrollViewFilters = view.findViewById(R.id.scrollViewFilters);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);

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
            
            loadFilterOptions();
            
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setRefreshing(false);
            
            boolean isDarkTheme = Utils.isDarkTheme(getContext());
            if (isDarkTheme) {
                btnResetFilters.setTextColor(android.graphics.Color.WHITE);
                btnToggleFilters.setTextColor(android.graphics.Color.WHITE);
                btnExpandFilters.setTextColor(android.graphics.Color.WHITE);
            }
            
            setupEventListeners();
            
            btnToggleFilters.setText(getString(R.string.multi_search_collapse_filters));
            scrollViewFilters.setVisibility(View.VISIBLE);
            btnExpandFilters.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (countriesList.size() <= 1 || languagesList.size() <= 1 || tagsList.size() <= 1) {
            loadFilterOptions();
        }
    }
    
    private void loadFilterOptions() {
        if (Utils.isOfflineMode(getContext())) {
            countriesList.clear();
            languagesList.clear();
            tagsList.clear();
            countriesList.add(getString(R.string.multi_search_all));
            languagesList.add(getString(R.string.multi_search_all));
            tagsList.add(getString(R.string.multi_search_all));
            updateCountrySpinner();
            updateLanguageSpinner();
            updateTagSpinner();
            showOfflineError();
            return;
        }
        
        countriesList.clear();
        languagesList.clear();
        tagsList.clear();
        
        countryAdapter = null;
        languageAdapter = null;
        tagAdapter = null;
        
        countriesList.add(getString(R.string.multi_search_all));
        new LoadCountriesTask().execute();
        
        languagesList.add(getString(R.string.multi_search_all));
        new LoadLanguagesTask().execute();
        
        tagsList.add(getString(R.string.multi_search_all));
        new LoadTagsTask().execute();
    }
    
    private void showOfflineError() {
        View layoutError = getView().findViewById(R.id.layoutError);
        if (layoutError != null) {
            layoutError.setVisibility(View.VISIBLE);
            recyclerViewStations.setVisibility(View.GONE);
        }
    }
    
    private class LoadCountriesTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> countries = new ArrayList<>();
            try {
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), "json/countries", true, null);
                if (result != null) {
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.optString("name");
                        if (name != null && !name.isEmpty()) {
                            countries.add(name);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading countries", e);
            }
            return countries;
        }
        
        @Override
        protected void onPostExecute(List<String> countries) {
            if (countries != null && !countries.isEmpty()) {
                countriesList.addAll(countries);
                updateCountrySpinner();
            }
        }
    }
    
    private class LoadLanguagesTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> languages = new ArrayList<>();
            try {
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), "json/languages", true, null);
                if (result != null) {
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.optString("name");
                        if (name != null && !name.isEmpty()) {
                            languages.add(name);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading languages", e);
            }
            return languages;
        }
        
        @Override
        protected void onPostExecute(List<String> languages) {
            if (languages != null && !languages.isEmpty()) {
                languagesList.addAll(languages);
                updateLanguageSpinner();
            }
        }
    }
    
    private class LoadTagsTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> tags = new ArrayList<>();
            try {
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), "json/tags", true, null);
                if (result != null) {
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.optString("name");
                        if (name != null && !name.isEmpty()) {
                            tags.add(name);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading tags", e);
            }
            return tags;
        }
        
        @Override
        protected void onPostExecute(List<String> tags) {
            if (tags != null && !tags.isEmpty()) {
                tagsList.addAll(tags);
                updateTagSpinner();
            }
        }
    }
    
    private void updateCountrySpinner() {
        if (countryAdapter == null) {
            countryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, countriesList);
            countryAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinnerCountry.setAdapter(countryAdapter);
        } else {
            countryAdapter.notifyDataSetChanged();
        }
    }
    
    private void updateLanguageSpinner() {
        if (languageAdapter == null) {
            languageAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, languagesList);
            languageAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinnerLanguage.setAdapter(languageAdapter);
        } else {
            languageAdapter.notifyDataSetChanged();
        }
    }
    
    private void updateTagSpinner() {
        if (tagAdapter == null) {
            tagAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, tagsList);
            tagAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinnerTag.setAdapter(tagAdapter);
        } else {
            tagAdapter.notifyDataSetChanged();
        }
    }
    
    private void setupEventListeners() {
        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCountry = position == 0 ? "" : countriesList.get(position);
                performMultiSearch();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguage = position == 0 ? "" : languagesList.get(position);
                performMultiSearch();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        spinnerTag.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTag = position == 0 ? "" : tagsList.get(position);
                performMultiSearch();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString();
                
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                searchRunnable = new Runnable() {
                    @Override
                    public void run() {
                        performMultiSearch();
                    }
                };
                
                searchHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
            }
        });
        
        btnResetFilters.setOnClickListener(v -> resetFilters());
        
        btnToggleFilters.setOnClickListener(v -> toggleFilters());
        
        btnExpandFilters.setOnClickListener(v -> toggleFilters());
    }
    
    private void toggleFilters() {
        if (scrollViewFilters.getVisibility() == View.VISIBLE) {
            scrollViewFilters.setVisibility(View.GONE);
            btnToggleFilters.setText(getString(R.string.multi_search_expand_filters));
            btnExpandFilters.setVisibility(View.VISIBLE);
        } else {
            scrollViewFilters.setVisibility(View.VISIBLE);
            btnToggleFilters.setText(getString(R.string.multi_search_collapse_filters));
            btnExpandFilters.setVisibility(View.GONE);
        }
    }
    
    private void performMultiSearch() {
        Log.d(TAG, "执行多条件搜索: 国家=" + selectedCountry + ", 语言=" + selectedLanguage + ", 标签=" + selectedTag + ", 关键词=" + searchQuery);
        new SearchStationsTask().execute(selectedCountry, selectedLanguage, selectedTag, searchQuery);
    }
    
    private class SearchStationsTask extends AsyncTask<Object, Void, List<DataRadioStation>> {
        @Override
        protected List<DataRadioStation> doInBackground(Object... params) {
            String country = (String) params[0];
            String language = (String) params[1];
            String tag = (String) params[2];
            String query = (String) params[3];
            
            String urlPart = "json/stations";
            
            if (query != null && !query.isEmpty()) {
                urlPart = "json/stations/byname/" + query;
            } else if (tag != null && !tag.isEmpty()) {
                urlPart = "json/stations/bytag/" + tag;
            } else if (language != null && !language.isEmpty()) {
                urlPart = "json/stations/bylanguage/" + language;
            } else if (country != null && !country.isEmpty()) {
                urlPart = "json/stations/bycountrycode/" + country;
            }
            
            try {
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
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
                stationListAdapter.updateList(null, stations);
                recyclerViewStations.setVisibility(View.VISIBLE);
                getView().findViewById(R.id.layoutError).setVisibility(View.GONE);
            } else {
                stationListAdapter.updateList(null, new ArrayList<>());
                recyclerViewStations.setVisibility(View.VISIBLE);
                getView().findViewById(R.id.layoutError).setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void resetFilters() {
        spinnerCountry.setSelection(0);
        spinnerLanguage.setSelection(0);
        spinnerTag.setSelection(0);
        etSearchQuery.setText("");
        
        selectedCountry = "";
        selectedLanguage = "";
        selectedTag = "";
        searchQuery = "";
        
        performMultiSearch();
    }
}

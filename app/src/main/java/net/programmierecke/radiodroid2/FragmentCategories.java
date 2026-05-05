package net.programmierecke.radiodroid2;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.programmierecke.radiodroid2.adapters.ItemAdapterCategory;
import net.programmierecke.radiodroid2.data.DataCategory;
import net.programmierecke.radiodroid2.station.StationsFilter;
import net.programmierecke.radiodroid2.data.DataCategory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FragmentCategories extends FragmentBase {
    private static final String TAG = "FragmentCategories";

    private RecyclerView rvCategories;
    private ViewGroup layoutError;
    private ItemAdapterCategory adapterCategory;
    private StationsFilter.SearchStyle searchStyle = StationsFilter.SearchStyle.ByName;
    private SwipeRefreshLayout swipeRefreshLayout;

    // 缓存变量
    private ArrayList<DataCategory> cachedTags = null;
    private ArrayList<DataCategory> cachedCountries = null;
    private ArrayList<DataCategory> cachedLanguages = null;

    public FragmentCategories() {
    }

    public void SetBaseSearchLink(StationsFilter.SearchStyle searchStyle) {
        this.searchStyle = searchStyle;
    }

    void ClickOnItem(DataCategory Data) {
        if (Data == null) {
            Log.e(TAG, "Category data is null");
            return;
        }
        
        if (Data.Name == null || Data.Name.isEmpty()) {
            Log.e(TAG, "Category name is null or empty");
            return;
        }
        
        // 检查Fragment是否已附加到Activity
        if (!isAdded() || getActivity() == null) {
            Log.e(TAG, "Fragment not attached to activity, cannot search");
            return;
        }
        
        try {
            ActivityMain m = (ActivityMain) getActivity();
            if (m == null) {
                Log.e(TAG, "Activity is null, cannot search");
                return;
            }
            
            Log.d(TAG, "Searching for category: " + Data.Name + " with style: " + searchStyle);
            m.search(this.searchStyle, Data.Name);
        } catch (Exception e) {
            Log.e(TAG, "Error when searching for category: " + Data.Name, e);
        }
    }

    private void loadCategories() {
        ArrayList<DataCategory> cachedData = null;
        switch (searchStyle) {
            case ByTagExact:
                cachedData = cachedTags;
                break;
            case ByCountryCodeExact:
                cachedData = cachedCountries;
                break;
            case ByLanguageExact:
                cachedData = cachedLanguages;
                break;
        }
        
        if (cachedData != null) {
            Log.d(TAG, "使用缓存的分类数据");
            if (adapterCategory != null) {
                adapterCategory.updateList(cachedData);
            }
            return;
        }

        Log.d(TAG, "从网络加载分类数据");
        new LoadCategoriesTask().execute();
    }
    
    private class LoadCategoriesTask extends AsyncTask<Void, Void, ArrayList<DataCategory>> {
        @Override
        protected ArrayList<DataCategory> doInBackground(Void... voids) {
            try {
                String urlPart;
                switch (searchStyle) {
                    case ByTagExact:
                        urlPart = "json/tags";
                        break;
                    case ByCountryCodeExact:
                        urlPart = "json/countries";
                        break;
                    case ByLanguageExact:
                        urlPart = "json/languages";
                        break;
                    default:
                        urlPart = "json/tags";
                        break;
                }
                
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), urlPart, true, null);
                
                if (result != null) {
                    JSONArray jsonArray = new JSONArray(result);
                    ArrayList<DataCategory> categoriesList = new ArrayList<>();
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        DataCategory category = new DataCategory();
                        category.Name = jsonObject.optString("name", "");
                        category.Label = category.Name;
                        category.UsedCount = jsonObject.optInt("stationcount", 0);
                        
                        if (category.UsedCount > 0) {
                            categoriesList.add(category);
                        }
                    }
                    
                    Collections.sort(categoriesList);
                    return categoriesList;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading categories from network", e);
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(ArrayList<DataCategory> categoriesList) {
            if (categoriesList != null && !categoriesList.isEmpty()) {
                switch (searchStyle) {
                    case ByTagExact:
                        cachedTags = new ArrayList<>(categoriesList);
                        break;
                    case ByCountryCodeExact:
                        cachedCountries = new ArrayList<>(categoriesList);
                        break;
                    case ByLanguageExact:
                        cachedLanguages = new ArrayList<>(categoriesList);
                        break;
                }
                
                if (adapterCategory != null) {
                    adapterCategory.updateList(categoriesList);
                }
                
                if (rvCategories != null) {
                    rvCategories.setVisibility(View.VISIBLE);
                }
                if (layoutError != null) {
                    layoutError.setVisibility(View.GONE);
                }
            } else {
                if (rvCategories != null) {
                    rvCategories.setVisibility(View.GONE);
                }
                if (layoutError != null) {
                    layoutError.setVisibility(View.VISIBLE);
                }
            }
            
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations, container, false);

        rvCategories = view.findViewById(R.id.recyclerViewStations);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        layoutError = view.findViewById(R.id.layoutError);
        View btnRefresh = view.findViewById(R.id.btnRetry);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RefreshListGui();
                }
            });
        }

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null) {
            rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
            adapterCategory = new ItemAdapterCategory(R.layout.list_item_category);
            adapterCategory.setCategoryClickListener(new ItemAdapterCategory.CategoryClickListener() {
                @Override
                public void onCategoryClick(DataCategory category) {
                    ClickOnItem(category);
                }
            });
            rvCategories.setAdapter(adapterCategory);

            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setEnabled(true);
                swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        RefreshListGui();
                    }
                });
            }
        }
        
        loadCategories();
    }

    @Override
    public void RefreshListGui() {
        cachedTags = null;
        cachedCountries = null;
        cachedLanguages = null;
        loadCategories();
    }
}

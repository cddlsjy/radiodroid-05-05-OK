package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import net.programmierecke.radiodroid2.adapters.ItemAdapterStatistics;
import net.programmierecke.radiodroid2.data.DataStatistics;
import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;

import org.json.JSONObject;

import java.util.ArrayList;

public class FragmentServerInfo extends Fragment implements IFragmentRefreshable {
    private static final String TAG = "FragmentServerInfo";
    private ItemAdapterStatistics itemAdapterStatistics;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_statistics, null);

        if (itemAdapterStatistics == null) {
            itemAdapterStatistics = new ItemAdapterStatistics(getActivity(), R.layout.list_item_statistic);
        }

        ListView lv = (ListView) view.findViewById(R.id.listViewStatistics);
        lv.setAdapter(itemAdapterStatistics);

        loadStatisticsFromNetwork();

        return view;
    }

    private void loadStatisticsFromNetwork() {
        // 离线模式直接返回，避免无谓的网络超时
        if (getContext() == null) return;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (sharedPref.getBoolean("disable_online_verification", false)) {
            return;
        }

        // 首先尝试从SharedPreferences获取服务器统计信息
        sharedPref = getContext().getSharedPreferences("ServerStatistics", Context.MODE_PRIVATE);
        int serverTotal = sharedPref.getInt("stations_total", -1);
        int serverWorking = sharedPref.getInt("stations_working", -1);
        int serverBroken = sharedPref.getInt("stations_broken", -1);
        long lastUpdated = sharedPref.getLong("last_updated", 0);
        
        // 检查服务器统计信息是否有效（最近1小时内更新）
        boolean hasValidServerStats = serverTotal > 0 && serverWorking > 0 && serverBroken > 0 && 
                (System.currentTimeMillis() - lastUpdated) < 60 * 60 * 1000;
        
        if (hasValidServerStats) {
            // 使用缓存的服务器统计信息
            Log.d(TAG, "Using cached server statistics: " + serverTotal + " total stations, " + serverWorking + " working, " + serverBroken + " broken");
            updateStatisticsUI(serverTotal, serverWorking, serverBroken);
        } else {
            // 从网络获取统计信息
            new LoadStatisticsTask().execute();
        }
    }
    
    private class LoadStatisticsTask extends AsyncTask<Void, Void, int[]> {
        @Override
        protected int[] doInBackground(Void... voids) {
            try {
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                String result = Utils.downloadFeedRelative(app.getHttpClient(), getContext(), "json/stats", true, null);
                
                if (result != null) {
                    JSONObject jsonObject = new JSONObject(result);
                    int totalCount = jsonObject.optInt("stations", 0);
                    int workingCount = jsonObject.optInt("stationsok", 0);
                    int brokenCount = totalCount - workingCount;
                    
                    // 保存到SharedPreferences
                    SharedPreferences.Editor editor = getContext().getSharedPreferences("ServerStatistics", Context.MODE_PRIVATE).edit();
                    editor.putInt("stations_total", totalCount);
                    editor.putInt("stations_working", workingCount);
                    editor.putInt("stations_broken", brokenCount);
                    editor.putLong("last_updated", System.currentTimeMillis());
                    editor.apply();
                    
                    return new int[]{totalCount, workingCount, brokenCount};
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading statistics from network", e);
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(int[] stats) {
            if (stats != null) {
                updateStatisticsUI(stats[0], stats[1], stats[2]);
            }
        }
    }
    
    private void updateStatisticsUI(int totalCount, int workingCount, int brokenCount) {
        // 创建统计数据
        ArrayList<DataStatistics> statistics = new ArrayList<>();
        
        // 总电台数
        DataStatistics totalStations = new DataStatistics();
        totalStations.Name = "stations_total";
        totalStations.Value = String.valueOf(totalCount);
        statistics.add(totalStations);
            
        // 工作正常的电台数
        DataStatistics workingStations = new DataStatistics();
        workingStations.Name = "stations_working";
        workingStations.Value = String.valueOf(workingCount);
        statistics.add(workingStations);
            
        // 损坏的电台数
        DataStatistics brokenStations = new DataStatistics();
        brokenStations.Name = "stations_broken";
        brokenStations.Value = String.valueOf(brokenCount);
        statistics.add(brokenStations);
            
        // 更新UI
        itemAdapterStatistics.clear();
        for (DataStatistics item : statistics) {
            itemAdapterStatistics.add(item);
        }
            
        Log.d(TAG, "Loaded statistics: " + totalCount + " total stations, " + workingCount + " working, " + brokenCount + " broken");
    }

    @Override
    public void Refresh() {
        loadStatisticsFromNetwork();
    }
}

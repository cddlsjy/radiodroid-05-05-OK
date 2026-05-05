package net.programmierecke.radiodroid2.station;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.CustomStationManager;
import net.programmierecke.radiodroid2.FragmentBase;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.StationUpdateListener;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;

import java.util.List;

/**
 * 自定义电台列表 Fragment
 * 用于显示和管理通过 M3U 文件导入的电台列表
 */
public class FragmentCustomStations extends FragmentBase implements StationUpdateListener {
    private static final String TAG = "FragmentCustomStations";

    private RecyclerView recyclerViewStations;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;

    private CustomStationManager customStationManager;
    private ItemAdapterStation stationListAdapter;
    private StationsFilter.SearchStyle lastSearchStyle = StationsFilter.SearchStyle.ByName;
    private String lastSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        
        View view = inflater.inflate(R.layout.fragment_stations, container, false);

        recyclerViewStations = view.findViewById(R.id.recyclerViewStations);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        
        // 查找或创建空视图
        emptyView = view.findViewById(R.id.textErrorMessage);
        if (emptyView == null) {
            emptyView = new TextView(getContext());
        }

        // 初始化 RecyclerView
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerViewStations.setLayoutManager(llm);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerViewStations.getContext(), llm.getOrientation());
        recyclerViewStations.addItemDecoration(dividerItemDecoration);

        // 初始化下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::loadStations);

        // 初始化电台管理器
        customStationManager = new CustomStationManager(getContext());
        customStationManager.addStationUpdateListener(this);

        // 初始化适配器
        FragmentActivity activity = getActivity();
        if (activity != null) {
            stationListAdapter = new ItemAdapterStation(activity, R.layout.list_item_station);
            stationListAdapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
                @Override
                public void onStationClick(DataRadioStation station, int pos) {
                    Log.d(TAG, "onStationClick: " + station.Name);
                    ActivityMain mainActivity = (ActivityMain) getActivity();
                    if (mainActivity != null) {
                        RadioDroidApp radioDroidApp = (RadioDroidApp) mainActivity.getApplication();
                        Utils.showPlaySelection(radioDroidApp, station, mainActivity.getSupportFragmentManager());
                    }
                }

                @Override
                public void onStationMoved(int from, int to) {
                    // 支持拖拽排序时实现
                }

                @Override
                public void onStationSwiped(DataRadioStation station) {
                    // 支持滑动删除时实现
                }

                public void onStationLongClick(DataRadioStation station, int pos) {
                    // 长按处理
                }

                @Override
                public void onStationMoveFinished() {
                    // 拖拽完成时调用
                }
            });
            recyclerViewStations.setAdapter(stationListAdapter);
        }

        // 加载数据
        loadStations();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (customStationManager != null) {
            customStationManager.removeStationUpdateListener(this);
        }
    }

    /**
     * 加载电台列表
     */
    private void loadStations() {
        Log.d(TAG, "loadStations");
        
        List<DataRadioStation> stations = customStationManager.getList();
        
        if (stations.isEmpty()) {
            // 显示空视图
            recyclerViewStations.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.empty_custom_stations);
        } else {
            // 显示列表
            emptyView.setVisibility(View.GONE);
            recyclerViewStations.setVisibility(View.VISIBLE);
            
            // 更新适配器
            if (stationListAdapter != null) {
                if (lastSearchQuery.isEmpty()) {
                    stationListAdapter.updateList(null, stations);
                } else {
                    // 如果有搜索条件，应用过滤
                    //
                }
            }
        }
        
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * 实现 StationUpdateListener 接口
     */
    @Override
    public void onStationListUpdated() {
        Log.d(TAG, "onStationListUpdated");
        if (isCreated()) {
            loadStations();
        }
    }

    /**
     * 提供导入导出接口给 ActivityMain
     */
    public CustomStationManager getCustomStationManager() {
        return customStationManager;
    }
}

package net.programmierecke.radiodroid2.utils;

import android.content.Context;
import android.view.View;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public class FocusManager {

    private static final String TAG = "FocusManager";

    private RecyclerView recyclerView;
    private FocusStateListener focusStateListener;
    private int lastFocusPosition = -1;

    public interface FocusStateListener {
        void onFocusChanged(int position, boolean hasFocus);
    }

    public FocusManager(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void initializeFocus(int position) {
        if (recyclerView == null || recyclerView.getAdapter() == null) {
            Log.w(TAG, "RecyclerView or Adapter is null, cannot initialize focus");
            return;
        }

        int itemCount = recyclerView.getAdapter().getItemCount();
        if (itemCount == 0) {
            Log.d(TAG, "No items to focus on");
            return;
        }

        int targetPosition = Math.max(0, Math.min(position, itemCount - 1));
        lastFocusPosition = targetPosition;

        recyclerView.post(() -> {
            ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(targetPosition);
            if (holder != null && holder.itemView != null) {
                holder.itemView.requestFocus();
                Log.d(TAG, "Focus initialized to position: " + targetPosition);
            } else {
                Log.w(TAG, "ViewHolder not found for position: " + targetPosition);
            }
        });
    }

    public void smoothScrollToFocusedItem() {
        if (recyclerView == null) {
            return;
        }

        View focusedChild = recyclerView.findFocus();
        if (focusedChild == null) {
            return;
        }

        int focusedPosition = recyclerView.getChildAdapterPosition(focusedChild);
        if (focusedPosition == RecyclerView.NO_POSITION) {
            return;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        if (focusedPosition < firstVisible || focusedPosition > lastVisible) {
            FocusSmoothScroller smoothScroller = new FocusSmoothScroller(recyclerView.getContext());
            smoothScroller.setTargetPosition(focusedPosition);
            layoutManager.startSmoothScroll(smoothScroller);
            Log.d(TAG, "Smooth scrolling to position: " + focusedPosition);
        }
    }

    public void setFocusStateListener(FocusStateListener listener) {
        this.focusStateListener = listener;
    }

    public void notifyFocusChanged(int position, boolean hasFocus) {
        if (hasFocus) {
            lastFocusPosition = position;
        }

        if (focusStateListener != null) {
            focusStateListener.onFocusChanged(position, hasFocus);
        }
    }

    public int getLastFocusPosition() {
        return lastFocusPosition;
    }

    public void saveLastFocusPosition(Context context, int position) {
        lastFocusPosition = position;
        android.content.SharedPreferences sharedPref =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.edit().putInt("last_focus_position", position).apply();
        Log.d(TAG, "Saved focus position: " + position);
    }

    public int getLastFocusPosition(Context context) {
        android.content.SharedPreferences sharedPref =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getInt("last_focus_position", 0);
    }
}

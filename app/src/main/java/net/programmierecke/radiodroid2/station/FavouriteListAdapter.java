package net.programmierecke.radiodroid2.station;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量级收藏列表适配器，用于横屏全屏播放器中显示收藏电台列表。
 * 点击电台项可直接播放。
 */
public class FavouriteListAdapter extends RecyclerView.Adapter<FavouriteListAdapter.FavouriteViewHolder> {

    public interface OnStationClickListener {
        void onStationClick(DataRadioStation station);
    }

    private List<DataRadioStation> stations = new ArrayList<>();
    private OnStationClickListener listener;

    public FavouriteListAdapter(OnStationClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<DataRadioStation> newStations) {
        stations.clear();
        if (newStations != null) {
            stations.addAll(newStations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavouriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_favourite_compact, parent, false);
        return new FavouriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavouriteViewHolder holder, int position) {
        DataRadioStation station = stations.get(position);

        holder.textViewName.setText(station.Name);

        // 显示简要描述（国家、编解码器等）
        String description = station.getShortDetails(holder.itemView.getContext());
        holder.textViewDescription.setText(description);

        // 加载图标
        if (station.hasIcon()) {
            Picasso.get()
                    .load(station.IconUrl)
                    .placeholder(R.drawable.ic_photo_24dp)
                    .error(R.drawable.ic_photo_24dp)
                    .into(holder.imageViewIcon);
        } else {
            holder.imageViewIcon.setImageResource(R.drawable.ic_photo_24dp);
        }

        // 显示收藏星标
        holder.imageViewStar.setImageResource(R.drawable.ic_star_24dp);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStationClick(station);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class FavouriteViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        ImageView imageViewStar;
        TextView textViewName;
        TextView textViewDescription;

        FavouriteViewHolder(View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            imageViewStar = itemView.findViewById(R.id.imageViewStar);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
        }
    }
}

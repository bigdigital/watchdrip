package com.thatguysservice.huami_xdrip.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.models.watchfaces.WatchFaceInfo;

import java.util.ArrayList;
import java.util.List;

public class WatchFaceListAdapter extends RecyclerView.Adapter<WatchFaceListAdapter.ViewHolder> {
    private String serverUrl;
    private List<WatchFaceInfo> datSet;
    private ItemClickListener mClickListener;

    public WatchFaceListAdapter(@NonNull List<WatchFaceInfo> data, String serverUrl) {
        this.serverUrl = serverUrl;
        this.datSet = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.watchface_item, parent, false);
        ViewHolder myViewHolder = new ViewHolder(view);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchFaceInfo dataModel = datSet.get(position);
        holder.txtName.setText(dataModel.info.name);
        holder.txtAuthor.setText(dataModel.info.author);
        //viewHolder.imgPreview.setOnClickListener(this);
        String url = serverUrl + dataModel.preview;
        Picasso.get().load(url).into(holder.imgPreview);
    }

    @Override
    public int getItemCount() {
        return datSet.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView txtAuthor;
        TextView txtName;
        ImageView imgPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.txtName = itemView.findViewById(R.id.wf_name);
            this.txtAuthor = itemView.findViewById(R.id.wf_author);
            this.imgPreview = itemView.findViewById(R.id.wf_preview_img);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mClickListener != null) mClickListener.onItemClick(v, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    public WatchFaceInfo getItem(int id) {
        return datSet.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

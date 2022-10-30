package com.thatguysservice.huami_xdrip;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thatguysservice.huami_xdrip.adapters.WatchFaceListAdapter;
import com.thatguysservice.huami_xdrip.models.watchfaces.WatchFaceInfo;

import java.util.List;

import static com.thatguysservice.huami_xdrip.WatchStoreActivity.DATA_FILES_DIR;
import static com.thatguysservice.huami_xdrip.WatchStoreActivity.RESULT_SUCCESS;

public class WatchStoreGridFragment extends Fragment implements WatchFaceListAdapter.ItemClickListener {
    List<WatchFaceInfo> wf;
    private RecyclerView watchfacesGrid;
    private TextView errorText;
    private Button retryButton;
    // private WatchfaceViewModel mViewModel;
    private ProgressBar progressBarLoading;
    private WatchFaceListAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WatchStoreGridFragment fragment = this;
        getParentFragmentManager().setFragmentResultListener("server_responce_result", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String msg = bundle.getString("msg");
                if (msg.equals(RESULT_SUCCESS)) {
                    WatchStoreActivity activity = (WatchStoreActivity) getActivity();
                    assert activity != null;
                    wf = activity.getWatchfaces();
                    if (wf == null) {
                    } else {
                        adapter = new WatchFaceListAdapter(wf, getString(R.string.wserviceurl) + DATA_FILES_DIR);
                        adapter.setClickListener(fragment);
                        watchfacesGrid.setAdapter(adapter);
                        removeError();
                    }
                    progressBarLoading.setVisibility(View.GONE);
                } else {
                    displayError(msg);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void displayError(String msg) {
        errorText.setText(msg);
        progressBarLoading.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
    }

    private void removeError() {
        errorText.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.watch_store_grid_fragment, container, false);
        errorText = view.findViewById(R.id.textview_watchface_grid_error);
        retryButton = view.findViewById(R.id.button_watchface_grid_retry);
        progressBarLoading = view.findViewById(R.id.progress_bar_grid_loading);
        watchfacesGrid = view.findViewById(R.id.gridview_watchfaces);
        GridLayoutManager layoutManager = new GridLayoutManager(this.getContext(), 2);
        watchfacesGrid.setLayoutManager(layoutManager);
        watchfacesGrid.setHasFixedSize(true);
        removeError();

        updateWf();
        view.findViewById(R.id.button_watchface_grid_retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateWf();
                removeError();
                progressBarLoading.setVisibility(View.VISIBLE);
            }
        });
        return view;
    }

    private void updateWf(){
        WatchStoreActivity activity = (WatchStoreActivity) getActivity();
        if ( activity != null){
            activity.updateRemoteWathcfaces();
        }
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //mViewModel = new ViewModelProvider(requireActivity()).get(WatchfaceViewModel.class);

    }

    private void openItemFragment(WatchFaceInfo wfInfo) {
        Fragment fragment = new WatchStoreDetailFragment();

        Bundle args = new Bundle();
        args.putParcelable("bundleKey", wfInfo);
        fragment.setArguments(args);

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.watchStoreFragmentCont, fragment).addToBackStack(null)
                .commit();
    }

    @Override
    public void onItemClick(View view, int position) {
        openItemFragment(adapter.getItem(position));
    }
}

package com.thatguysservice.huami_xdrip;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.models.watchfaces.WatchFaceInfo;
import com.thatguysservice.huami_xdrip.utils.FileUtils;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.thatguysservice.huami_xdrip.WatchStoreActivity.DATA_FILES_DIR;

public class WatchStoreDetailFragment extends Fragment {

    private WatchFaceInfo dataModel;
    private Fetch fetch;
    private String url;
    private Request request;
    private ProgressBar progress;
    private TextView textDownloadStatus;
    private boolean isDownloading;

    FetchListener fetchListener = new FetchListener() {
        @Override
        public void onWaitingNetwork(@NotNull Download download) {

        }

        @Override
        public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int i) {

        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @org.jetbrains.annotations.Nullable Throwable throwable) {
            textDownloadStatus.setText(getString(R.string.downloadError) + " " + error.toString());
            fetch.removeListener(fetchListener);
            isDownloading = false;
        }

        @Override
        public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {

        }

        @Override
        public void onAdded(@NotNull Download download) {

        }

        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
            if (request.getId() == download.getId()) {
                //showDownloadInList(download);
            }
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            setProgress(100);
            fetch.removeListener(fetchListener);

            startUnzipFile(download.getFile());

        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            setProgress(download.getProgress());
        }

        @Override
        public void onPaused(@NotNull Download download) {

        }

        @Override
        public void onResumed(@NotNull Download download) {

        }

        @Override
        public void onCancelled(@NotNull Download download) {

        }

        @Override
        public void onRemoved(@NotNull Download download) {

        }

        @Override
        public void onDeleted(@NotNull Download download) {

        }
    };
    private Button btnDownload;

    // String result;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       /* getParentFragmentManager().setFragmentResultListener("key", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String key, @NonNull Bundle bundle) {
                 result = bundle.getString("bundleKey");
            }
        });*/

        url = getString(R.string.wserviceurl) + DATA_FILES_DIR;

        Context context = getContext();
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(3)
                .build();

        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isDownloading) {
                    Helper.static_toast_short("Watchface downloading not finished, please wait");
                    return;
                }
                //getActivity().getSupportFragmentManager().popBackStack();
                this.setEnabled(false);
                getActivity().onBackPressed();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.watch_store_detail_fragment, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        dataModel = args != null ? args.getParcelable("bundleKey") : null;
        if (dataModel == null) return;

        TextView txtName = view.findViewById(R.id.wf_item_name);
        TextView txtAuthor = view.findViewById(R.id.wf_item_author);
        TextView txtDescription = view.findViewById(R.id.wf_item_description);
        ImageView imgPreview = view.findViewById(R.id.wf_item_preview_img);
        btnDownload = view.findViewById(R.id.wf_item_download);
        progress = view.findViewById(R.id.wf_item_download_progress);
        progress.setVisibility(View.GONE);
        textDownloadStatus = view.findViewById(R.id.wf_item_download_status);
        progress.setVisibility(View.GONE);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchFile();
            }
        });

        txtName.setText(dataModel.info.name);
        txtAuthor.setText(dataModel.info.author);
        txtDescription.setText(dataModel.info.watchfaceDescription);

        String imageUrl = url + dataModel.previewAnimated;
        Picasso.get().load(imageUrl).into(imgPreview);
    }

    private void setProgress(int progressVal) {
        progress.setProgress(progressVal);
        textDownloadStatus.setText(progressVal + "%");
    }

    private void fetchFile() {
        String urlFile = url + dataModel.file;
        String downloadedFile = FileUtils.getDownloadFolder(getContext()) + "/" + dataModel.file;

        FileUtils.deleteFile(downloadedFile);

        request = new Request(urlFile, downloadedFile);
        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.ALL);
        fetch.addListener(fetchListener);

        progress.setVisibility(View.VISIBLE);
        setProgress(0);
        textDownloadStatus.setVisibility(View.VISIBLE);
        btnDownload.setVisibility(View.GONE);
        isDownloading = true;
        fetch.enqueue(request, updatedRequest -> {
        }, error -> {
            textDownloadStatus.setText(getString(R.string.downloadError));
            isDownloading = false;
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fetch.close();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void startUnzipFile(String file) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                String destinationPath = FileUtils.getExternalDir() + "/";
                try {
                    FileUtils.unzip(file, destinationPath);
                } catch (Exception e) {
                    result = false;
                    UserError.Log.d("miband", e.getMessage());
                }

                boolean finalResult = result;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalResult) {
                            textDownloadStatus.setText(getString(R.string.downloadComplete));
                            if (!MiBandEntry.isNeedToUseCustomWatchface()) {
                                Helper.static_toast_short(getString(R.string.custom_wf_option_enabled));
                                MiBandEntry.setCustomWatchfaceUse(true);
                                FileUtils.deleteFile(file);
                            }
                        } else {
                            textDownloadStatus.setText(getString(R.string.unZipError));
                        }
                        isDownloading = false;
                    }
                });
            }
        });
    }
}

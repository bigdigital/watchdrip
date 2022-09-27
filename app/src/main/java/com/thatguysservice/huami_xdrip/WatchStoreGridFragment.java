package com.thatguysservice.huami_xdrip;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.thatguysservice.huami_xdrip.models.Helper;

public class WatchStoreGridFragment extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.watch_store_grid_fragment, container, false);
    }

    private static final String[] mContacts = { "Рыжик", "Барсик", "Мурзик",
            "Мурка", "Васька", "Полосатик", "Матроскин", "Лизка", "Томосина",
            "Бегемот", "Чеширский", "Дивуар", "Тигра", "Лаура" };

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayAdapter<String> adapter = new ArrayAdapter(this.getActivity(), android.R.layout.simple_list_item_1, mContacts);

        AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Helper.static_toast_long("Selected "
                        + parent.getItemAtPosition(position).toString());
                NavHostFragment.findNavController(WatchStoreGridFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        };

        GridView watchfacesGrid = view.findViewById(R.id.gridview_watchfaces);
        watchfacesGrid.setOnItemClickListener(itemListener);

      /*  view.findViewById(R.id.gridview_watchfaces).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });*/
    }
}

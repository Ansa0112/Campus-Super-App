package com.example.campussuperapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CampusMapFragment extends Fragment {

    // campus_map.jpg ka size (pixels). Image badlein to ye dono bhi badlein.
    private static final float IMG_W = 688f;
    private static final float IMG_H = 1110f;

    private static CampusMapView.Spot spot(String name, float x, float y) {
        return new CampusMapView.Spot(name, x / IMG_W, y / IMG_H);
    }

    // Har jagah ki position image ke pixels mein (x, y). Image par jahan label chahiye wahan ki
    // x,y yahan likh dein. Naya naam jorna ho to ek line aur add kar dein.
    private static final List<CampusMapView.Spot> SPOTS = Arrays.asList(
            spot("FUI Campus", 407, 598),
            spot("Parking", 394, 268),
            spot("Masjid", 480, 428),
            spot("Boys Gate", 512, 463),
            spot("Basketball Court", 303, 487),
            spot("Outdoor Cafe", 253, 585),
            spot("Accounts & Finance", 380, 648),
            spot("Admin Office", 598, 690),
            spot("Library", 452, 774),
            spot("Exams Cell", 338, 815),
            spot("Indoor Cafe", 448, 840),
            spot("Account Officer", 605, 830),
            spot("Administration Block", 527, 850),
            spot("Girls Gate", 624, 891)
    );

    private CampusMapView mapView;
    private EditText etSearch;
    private ListView lvResults;
    private View cardResults;
    private ArrayAdapter<String> adapter;
    private final List<String> shown = new ArrayList<>();
    private boolean ignoreText = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campus_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Header status bar ke neeche na chhupe
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets;
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        mapView = view.findViewById(R.id.campusMap);
        etSearch = view.findViewById(R.id.etSearch);
        lvResults = view.findViewById(R.id.lvResults);
        cardResults = view.findViewById(R.id.cardResults);

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.campus_map);
        mapView.setMapBitmap(bmp);
        mapView.setSpots(SPOTS);

        // Map par label dabane par search box mein uska naam aa jaye
        mapView.setOnSpotClickListener(s -> showName(s.name));

        setupSearch(view);
    }

    // ------------------------------------------------------------ Search

    private void setupSearch(View root) {
        adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, shown);
        lvResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!ignoreText) filter(s.toString());
            }
        });

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) filter(etSearch.getText().toString());
            else cardResults.setVisibility(View.GONE);
        });

        // Keyboard ka search button: pehla result chun lo
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (!shown.isEmpty()) {
                selectByName(shown.get(0));
            } else {
                Toast.makeText(requireContext(), "Ye jagah nahi mili", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        lvResults.setOnItemClickListener((parent, v, position, id) ->
                selectByName(shown.get(position)));

        root.findViewById(R.id.btnClearSearch).setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.requestFocus();
            mapView.selectSpot(null, false);
        });
    }

    private void filter(String q) {
        String query = q.trim().toLowerCase(Locale.ROOT);

        shown.clear();
        if (query.isEmpty() && !etSearch.hasFocus()) {
            adapter.notifyDataSetChanged();
            cardResults.setVisibility(View.GONE);
            return;
        }
        for (CampusMapView.Spot s : SPOTS) {
            if (query.isEmpty() || s.name.toLowerCase(Locale.ROOT).contains(query)) {
                shown.add(s.name);
            }
        }
        adapter.notifyDataSetChanged();

        if (shown.isEmpty()) {
            cardResults.setVisibility(View.GONE);
            return;
        }
        ViewGroup.LayoutParams lp = lvResults.getLayoutParams();
        lp.height = dp(48) * Math.min(shown.size(), 5);
        lvResults.setLayoutParams(lp);
        cardResults.setVisibility(View.VISIBLE);
    }

    private void selectByName(String name) {
        showName(name);
        for (CampusMapView.Spot s : SPOTS) {
            if (s.name.equals(name)) {
                mapView.selectSpot(s, true); // zoom karke us jagah par le jao
                break;
            }
        }
    }

    // Search box mein naam likho (bina list dobara khole), keyboard band karo
    private void showName(String name) {
        ignoreText = true;
        etSearch.setText(name);
        etSearch.setSelection(name.length());
        ignoreText = false;

        etSearch.clearFocus();
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        cardResults.setVisibility(View.GONE);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
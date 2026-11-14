package com.example.campussuperapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class EmergencyFragment extends Fragment {

    // Firestore na chale ya khali ho to ye dikhega
    private static final String[][] FALLBACK = {
            {"Rescue / Ambulance", "1122"},
            {"Police", "15"},
            {"Fire Brigade", "16"},
            {"Edhi Ambulance", "115"},
            {"Women Helpline Punjab", "1043"},
            {"Child Protection Helpline", "1121"},
            {"FUI", "051-111-384-111"},
            {"Campus Main Office", "051-5151437"},
            {"Campus Main Office", "051-5151438"},
            {"Campus Direct Line", "051-5151433"},
            {"Campus Administration", "051-8962192"}
    };

    private LinearLayout list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emergancy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        list = view.findViewById(R.id.contactsContainer);
        loadContacts();
    }

    private void loadContacts() {
        FirebaseFirestore.getInstance()
                .collection("emergency_contacts")
                .orderBy("order")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    if (list != null) list.removeAllViews();
                    if (snapshot == null || snapshot.isEmpty()) {
                        showFallback();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String title = doc.getString("title");
                        String number = doc.getString("number");
                        if (title != null && number != null) {
                            addItem(title, number);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        showFallback();
                    }
                });
    }

    private void showFallback() {
        if (list != null) list.removeAllViews();
        for (String[] c : FALLBACK) {
            addItem(c[0], c[1]);
        }
    }

    private void addItem(String title, String number) {
        if (getContext() == null || list == null) return;

        View item = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_emergency_contact, list, false);

        TextView tvTitle = item.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(title);

        TextView tvNumber = item.findViewById(R.id.tvNumber);
        if (tvNumber != null) tvNumber.setText(number);

        MaterialButton btnCall = item.findViewById(R.id.btnCall);
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> dial(number));
        }

        list.addView(item);
    }

    private void dial(String number) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No dialer found", Toast.LENGTH_SHORT).show();
        }
    }
}

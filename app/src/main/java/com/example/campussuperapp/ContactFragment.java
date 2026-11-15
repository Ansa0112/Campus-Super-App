package com.example.campussuperapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ContactFragment extends Fragment {

    private static final String UAN_DISPLAY = "(051) 111 384 (FUI) 111";
    private static final String UAN_DIAL = "+9251111384111";

    private static final String ADDRESS =
            "H36C+GH2 Foundation University Rawalpindi Campus, New Lalazar, Rawalpindi";

    private static final String HELP_DESK_EMAIL = "helpdesk@fui.edu.pk";

    private static final String SATELLITE_MAP_URL =
            "https://www.google.com/maps/place/Foundation+University+Rawalpindi+Campus/@33.5611194,73.0715389,90m/data=!3m1!1e3";

    private EditText etName, etEmail, etMessage;
    private MaterialButton btnSend;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Sirf status bar ki jagah chhodo. Keyboard aur bottom nav HomeActivity sambhalti hai.
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets;
        });

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSendMessage);

        // ---------- Keyboard aaye to jahan likh rahe hon wo field upar aaye ----------
        final ScrollView scroll = view.findViewById(R.id.scrollContact);
        if (scroll != null) {
            // 1) Keyboard khulne se ScrollView chhota hota hai -> focused field dikhao
            scroll.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                View focused = scroll.findFocus();
                if (focused instanceof EditText) {
                    scrollToView(scroll, focused);
                }
            });

            // 2) Field par click/focus hone par bhi dikhao
            View.OnFocusChangeListener onFocus = (field, hasFocus) -> {
                if (hasFocus) {
                    field.postDelayed(() -> {
                        if (isAdded()) scrollToView(scroll, field);
                    }, 350);
                }
            };
            if (etName != null) etName.setOnFocusChangeListener(onFocus);
            if (etEmail != null) etEmail.setOnFocusChangeListener(onFocus);
            if (etMessage != null) etMessage.setOnFocusChangeListener(onFocus);
        }

        // Campus information card
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        if (tvAddress != null) tvAddress.setText(ADDRESS);

        TextView tvUan = view.findViewById(R.id.tvUan);
        if (tvUan != null) tvUan.setText(UAN_DISPLAY);

        // Card captions
        TextView tvPhone = view.findViewById(R.id.tvPhone);
        if (tvPhone != null) tvPhone.setText("UAN");

        TextView tvHelpEmail = view.findViewById(R.id.tvHelpEmail);
        if (tvHelpEmail != null) tvHelpEmail.setText("Help desk");

        TextView tvOfficeHours = view.findViewById(R.id.tvOfficeHours);
        if (tvOfficeHours != null) tvOfficeHours.setText("H36C+GH2");

        // Back
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        // Call / UAN
        View.OnClickListener dialUan = v ->
                launch(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + UAN_DIAL)));

        View cardCall = view.findViewById(R.id.cardCall);
        if (cardCall != null) cardCall.setOnClickListener(dialUan);

        View rowUan = view.findViewById(R.id.rowUan);
        if (rowUan != null) rowUan.setOnClickListener(dialUan);

        // Email
        View cardEmail = view.findViewById(R.id.cardEmail);
        if (cardEmail != null) {
            cardEmail.setOnClickListener(v ->
                    launch(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + HELP_DESK_EMAIL))));
        }

        // Visit -> satellite map
        View cardVisit = view.findViewById(R.id.cardVisit);
        if (cardVisit != null) {
            cardVisit.setOnClickListener(v ->
                    launch(new Intent(Intent.ACTION_VIEW, Uri.parse(SATELLITE_MAP_URL))));
        }

        // Emergency banner -> Emergency fragment
        View cardEmergency = view.findViewById(R.id.cardEmergency);
        if (cardEmergency != null) {
            cardEmergency.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new EmergencyFragment())
                            .addToBackStack(null)
                            .commit());
        }

        // Send message
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendMessage());
        }
    }

    // Diya hua view ScrollView ke visible hisse mein le aata hai
    private void scrollToView(ScrollView scroll, View target) {
        Rect r = new Rect();
        target.getDrawingRect(r);
        scroll.offsetDescendantRectToMyCoords(target, r);

        int margin = 160; // field ke neeche thodi jagah, taake Send button bhi nazar aaye
        int visibleTop = scroll.getScrollY();
        int visibleBottom = visibleTop + scroll.getHeight();

        if (r.bottom + margin > visibleBottom) {
            scroll.smoothScrollTo(0, r.bottom + margin - scroll.getHeight());
        } else if (r.top - margin < visibleTop) {
            scroll.smoothScrollTo(0, Math.max(0, r.top - margin));
        }
    }

    private void sendMessage() {
        String name = getText(etName);
        String email = getText(etEmail);
        String message = getText(etMessage);

        if (name.isEmpty()) {
            if (etName != null) {
                etName.setError("Please enter your name");
                etName.requestFocus();
            }
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (etEmail != null) {
                etEmail.setError("Enter a valid email");
                etEmail.requestFocus();
            }
            return;
        }
        if (message.isEmpty()) {
            if (etMessage != null) {
                etMessage.setError("Please write a message");
                etMessage.requestFocus();
            }
            return;
        }

        if (btnSend != null) {
            btnSend.setEnabled(false);
            btnSend.setText("Sending...");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("message", message);
        data.put("createdAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("contact_messages")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Message sent. Thank you!", Toast.LENGTH_SHORT).show();
                    if (etName != null) etName.setText("");
                    if (etEmail != null) etEmail.setText("");
                    if (etMessage != null) etMessage.setText("");
                    resetButton();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to send. Check your internet.", Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        if (btnSend != null) {
            btnSend.setEnabled(true);
            btnSend.setText("Send message");
        }
    }

    private String getText(EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void launch(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No app found to handle this action", Toast.LENGTH_SHORT).show();
        }
    }
}
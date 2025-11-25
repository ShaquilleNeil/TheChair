// Shaq’s Notes:
// This fragment is lean: it watches the query box, streams the text into your
// SearchRepository, and updates the adapter. The tidy part is the decoupling:
// SearchFragment never touches Firestore directly — it only reacts to the
// repository’s callback. That gives you interchangeable back-ends later.
// Only subtle footnote: calling setupSearchListener() twice is redundant.
// Everything else flows like a clean stream.

package com.example.thechair.Customer;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.example.thechair.Adapters.SearchAdapter;
import com.example.thechair.Adapters.SearchRepository;
import com.example.thechair.R;

import java.util.ArrayList;
import java.util.Map;

public class SearchFragment extends Fragment {

    private EditText searchInput;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SearchAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater infl, ViewGroup parent, Bundle saved) {
        View v = infl.inflate(R.layout.fragment_search, parent, false);

        searchInput  = v.findViewById(R.id.searchInput);
        recyclerView = v.findViewById(R.id.recyclerView);
        progressBar  = v.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SearchAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnProfessionalClickListener(this::openPublicProfile);

        setupSearchListener();

        String arg = null;
        if (getArguments() != null) {
            arg = getArguments().getString("search");
        }

        if (arg != null) {
            searchInput.setText(arg);
            searchInput.setSelection(arg.length());
        }

        return v;
    }

    private void setupSearchListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s == null ? "" : s.toString().trim();

                if (q.isEmpty()) {
                    adapter.updateData(new ArrayList<>());
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                SearchRepository.searchProfessionals(q, results -> {
                    adapter.updateData(results);
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void openPublicProfile(Map<String, Object> pro) {
        String id = (String) pro.get("id");
        if (id == null || id.isEmpty()) return;

        Bundle args = new Bundle();
        args.putString("professionalId", id);

        com.example.thechair.Professional.PublicProfileFragment f =
                new com.example.thechair.Professional.PublicProfileFragment();
        f.setArguments(args);

        FragmentTransaction ft = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();

        ft.replace(R.id.appMainView, f);
        ft.addToBackStack(null);
        ft.commit();
    }
}

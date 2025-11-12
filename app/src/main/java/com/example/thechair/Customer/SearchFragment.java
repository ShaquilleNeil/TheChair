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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchInput = view.findViewById(R.id.searchInput);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SearchAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnProfessionalClickListener(this::openPublicProfile);

        setupSearchListener();
        return view;
    }

    private void setupSearchListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();
                if (query.isEmpty()) {
                    adapter.updateData(new ArrayList<>());
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                SearchRepository.searchProfessionals(query, results -> {
                    adapter.updateData(results);
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void openPublicProfile(Map<String, Object> professional) {
        String professionalId = (String) professional.get("id");
        if (professionalId == null || professionalId.isEmpty()) return;

        Bundle args = new Bundle();
        args.putString("professionalId", professionalId);

        com.example.thechair.Customer.PublicProfileFragment fragment = new com.example.thechair.Customer.PublicProfileFragment();
        fragment.setArguments(args);

        FragmentTransaction transaction = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.appMainView, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

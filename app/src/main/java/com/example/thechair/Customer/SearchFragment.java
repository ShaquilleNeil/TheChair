package com.example.thechair.Customer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private EditText searchInput;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SearchAdapter adapter;
    private SearchRepository searchRepository;


    public SearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SearchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      View view = inflater.inflate(R.layout.fragment_search, container, false);
        searchInput = view.findViewById(R.id.searchInput);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);

        //Shaq Notes: setting the layout with recyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //Shaq Notes: creating the adapter and setting it to the recycler view
        adapter = new SearchAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        //Shaq Notes: creating the search repository and setting it to the adapter
        searchRepository = new SearchRepository();

        setupSearchListener();





        return view;
    }

    private void setupSearchListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // No action needed here
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();

                // 👇 Prevent sending empty or null queries to Firebase
                if (query.isEmpty()) {
                    adapter.updateData(new ArrayList<>()); // clear results
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                // 👇 Optional: show loading spinner
                progressBar.setVisibility(View.VISIBLE);

                // 👇 Make the search call
                SearchRepository.searchProfessionals(query, results -> {
                    adapter.updateData(results);
                    progressBar.setVisibility(View.GONE);
                    android.util.Log.d("SearchFragment", "🔍 Results for \"" + query + "\": " + results.size());
                });
            }
        });
    }


}
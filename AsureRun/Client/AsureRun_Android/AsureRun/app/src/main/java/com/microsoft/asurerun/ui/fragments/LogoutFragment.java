// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import static com.microsoft.asurerun.util.CacheUtil.SHAREDPREFFILE;

public class LogoutFragment extends Fragment {
    public final static String TAG = "LOGOUT_FRG";
    private Button mLogoutButton;
    private TextView mErrorText;
    private SharedPreferences mSharedPref;

    public LogoutFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LogoutFragment.
     */
    public static LogoutFragment newInstance() {
        return new LogoutFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPref = getActivity().getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_logout_layout, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_logout);
        mLogoutButton = (Button) v.findViewById(R.id.logoutButton);
        mErrorText = (TextView) v.findViewById(R.id.logoutError);
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApplicationState.deleteRunItems(getActivity());
            }
        });
        return v;
    }
}

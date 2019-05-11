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

import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.ui.HomeActivity;

import static com.microsoft.asurerun.util.AuthUtil.logout;
import static com.microsoft.asurerun.util.CacheUtil.AUTH_PROVIDER;
import static com.microsoft.asurerun.util.CacheUtil.SHAREDPREFFILE;

public class AdvancedSettingsFragment extends Fragment {
    public final static String TAG = "A_SETTGS_FRG";
    private Button mResetKeysButton;

    public AdvancedSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AdvancedSettingsFragment.
     */
    public static AdvancedSettingsFragment newIstance() {
        return new AdvancedSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_advanced_settings, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_advanced_setting);
        mResetKeysButton = (Button) v.findViewById(R.id.resetKeysButton);
        mResetKeysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               ApplicationState.deleteRunItems(getActivity());
            }
        });
        return v;
    }
}

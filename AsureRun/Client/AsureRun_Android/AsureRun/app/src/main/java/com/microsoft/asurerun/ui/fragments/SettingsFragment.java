// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.microsoft.asurerun.R;
import com.microsoft.asurerun.ui.HomeActivity;

public class SettingsFragment extends Fragment {
    private LinearLayout mRunSettingsAction, mDeleteDataAction, mAdvanceSettingsAction;
    private TextView mAppVersionLabel;
    public final static String TAG = "SETTGS_FRG";

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsFragment.
     */
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_settings);
        mAdvanceSettingsAction = (LinearLayout) v.findViewById(R.id.AdvancedSettingsAction);
        mRunSettingsAction = (LinearLayout) v.findViewById(R.id.RunSettingsAction);
        mDeleteDataAction = (LinearLayout) v.findViewById(R.id.DeleteDataAction);
        mAppVersionLabel = (TextView)v.findViewById(R.id.AppVersionLabel);

        final HomeActivity homeActivity = (HomeActivity) getActivity();
        mRunSettingsAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity.loadFragment(RunSettingsFragment.newInstance(), true, true, RunSettingsFragment.TAG);
            }
        });
        mDeleteDataAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity.loadFragment(DeleteDataSettingsFragment.newIstance(), true, true, DeleteDataSettingsFragment.TAG);
            }
        });
        mAdvanceSettingsAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity.loadFragment(AdvancedSettingsFragment.newIstance(), true, true, AdvancedSettingsFragment.TAG);
            }
        });

        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            mAppVersionLabel.append(version);

        } catch (PackageManager.NameNotFoundException e) {
            mAppVersionLabel.setText("Failed to get app version");
        }
        return v;
    }
}

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
import com.microsoft.asurerun.util.AuthUtil;

import static com.microsoft.asurerun.util.AuthUtil.logout;
import static com.microsoft.asurerun.util.CacheUtil.AUTH_PROVIDER;
import static com.microsoft.asurerun.util.CacheUtil.SHAREDPREFFILE;

public class DeleteDataSettingsFragment extends Fragment {
    public final static String TAG = "D_SETTGS_FRG";
    private Button mDeleteDataButton;

    public DeleteDataSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DeleteDataSettingsFragment.
     */
    public static DeleteDataSettingsFragment newIstance() {
        return new DeleteDataSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_delete_settings, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_delete_setting);
        mDeleteDataButton = (Button) v.findViewById(R.id.deleteDataButton);
        mDeleteDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApplicationState.deleteRunItems(getActivity());
            }
        });
        return v;
    }
}

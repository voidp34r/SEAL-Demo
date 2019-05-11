// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.microsoft.asurerun.R;

import static com.microsoft.asurerun.util.CacheUtil.*;


public class RunSettingsFragment extends Fragment {
    private TextView mKmMilesText;
    private ImageButton mKmMilesButton;
    public final static String TAG = "R_SETTGS_FRG";
    private SharedPreferences mSharedPref;

    public RunSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RunSettingsFragment.
     */
    public static RunSettingsFragment newInstance() {
        return new RunSettingsFragment();
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
        View v = inflater.inflate(R.layout.fragment_run_settings, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_run_setting);
        mKmMilesText = (TextView) v.findViewById(R.id.unit_text);
        mKmMilesButton = (ImageButton) v.findViewById(R.id.unit_image);
        String unitMeasure = mSharedPref.getString(KM_MILES_KEY, MILES_VALUE);
        mKmMilesText.setText((unitMeasure.equals(MILES_VALUE) ? getString(R.string.run_settings_distance_miles) : getString(R.string.run_settings_distance_km)));
        mKmMilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String unit = mSharedPref.getString(KM_MILES_KEY, MILES_VALUE);
                SharedPreferences.Editor editor = mSharedPref.edit();
                if (unit.equals(MILES_VALUE)) {
                    editor.putString(KM_MILES_KEY, KILOMETERS_VALUE);
                    editor.commit();
                    mKmMilesText.setText(getString(R.string.run_settings_distance_km));
                } else {
                    editor.putString(KM_MILES_KEY, MILES_VALUE);
                    editor.commit();
                    mKmMilesText.setText(getString(R.string.run_settings_distance_miles));
                }
            }
        });
        return v;
    }
}

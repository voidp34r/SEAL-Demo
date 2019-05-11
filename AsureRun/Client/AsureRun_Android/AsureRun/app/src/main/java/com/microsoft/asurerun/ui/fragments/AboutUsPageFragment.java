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

public class AboutUsPageFragment extends Fragment {
    public final static String TAG = AboutUsPageFragment.class.getCanonicalName();
    private LinearLayout mLearnMoreAction,mLicenseAction, mPrivacyAction, mContactUsAction, mDebugAction;
    private TextView mAppVersionLabel;

    public AboutUsPageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AboutUsPageFragment.
     */
    public static AboutUsPageFragment newInstance() {
        return new AboutUsPageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.about_page_fragment_layout, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_about_us_page);
        mLearnMoreAction = (LinearLayout) v.findViewById(R.id.learnMoreAction);
        mLicenseAction = (LinearLayout)v.findViewById(R.id.licenseAction);
        mPrivacyAction = (LinearLayout) v.findViewById(R.id.privacyAction);
        mDebugAction = (LinearLayout) v.findViewById(R.id.debugAction);
        mContactUsAction = (LinearLayout) v.findViewById(R.id.contactUsAction);
        mAppVersionLabel = (TextView) v.findViewById(R.id.buildNumberText);
        // get root activity
        final HomeActivity homeActivity = (HomeActivity) getActivity();
        // set on click action
        mLearnMoreAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                homeActivity.loadFragment(LearnMoreFragment.newInstance(),true, true, LearnMoreFragment.TAG);
            }
        });
        mDebugAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity.loadFragment(DebugPageFragment.newInstance(),true, true, DebugPageFragment.TAG);
            }
        });
        mContactUsAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity.loadFragment(ContactUsFragment.newInstance(),true, true, ContactUsFragment.TAG);
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

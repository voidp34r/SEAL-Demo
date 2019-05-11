// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microsoft.asurerun.BuildConfig;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebugPageFragment extends Fragment {
    public final static String TAG = DebugPageFragment.class.getCanonicalName();
    private TextView mDebugLabel,mBuildNumber,mKeyDimension,mBuildDate;

    public DebugPageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContactUsFragment.
     */
    public static DebugPageFragment newInstance() {
        return new DebugPageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.debug_fragment_layout, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_debug_page);
        mDebugLabel = (TextView) v.findViewById(R.id.debugLink);
        mBuildNumber = (TextView) v.findViewById(R.id.buildNumber);
        mKeyDimension = (TextView) v.findViewById(R.id.keySize);
        mBuildDate = (TextView) v.findViewById(R.id.buildDate);
        Date buildDate = BuildConfig.BUILD_TIME;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
        mBuildDate.append(sdf.format(buildDate));
        mKeyDimension.append(Utils.getKeyDimension(getActivity())+" MB");
        mKeyDimension.setVisibility(View.INVISIBLE); // todo hidden until we have the exact size of the key
        mBuildNumber.append(Utils.getBuildNumber(getActivity()));
        mDebugLabel.setClickable(true);
        mDebugLabel.setMovementMethod(LinkMovementMethod.getInstance());
        String debugText = getString(R.string.debug_fragment_header_text_left)+getString(R.string.service_url)+
                ">"+getString(R.string.service_url)+ getString(R.string.debug_fragment_header_text_right);
        mDebugLabel.setText(Html.fromHtml(debugText));
        return v;
    }
}

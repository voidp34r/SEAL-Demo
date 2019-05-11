// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microsoft.asurerun.R;

public class LearnMoreFragment extends Fragment {
    public final static String TAG = LearnMoreFragment.class.getCanonicalName();
    private TextView mLeanMoreLink;
    private final static String URL = "https://www.microsoft.com/en-us/research/project/simple-encrypted-arithmetic-library/";

    public LearnMoreFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LearnMoreFragment.
     */
    public static LearnMoreFragment newInstance() {
        return new LearnMoreFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.learn_more_fragment, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_learn_more_page);
        mLeanMoreLink = (TextView) v.findViewById(R.id.learnMoreQuestion);
        mLeanMoreLink.setClickable(true);
        mLeanMoreLink.setMovementMethod(LinkMovementMethod.getInstance());
        String linkText = getString(R.string.learn_more_fragment_header_question)+" "
                + getString(R.string.learn_more_fragment_link_left) + URL + getString(R.string.learn_more_fragment_link_right);
        mLeanMoreLink.setText(Html.fromHtml(linkText));
        return v;
    }
}

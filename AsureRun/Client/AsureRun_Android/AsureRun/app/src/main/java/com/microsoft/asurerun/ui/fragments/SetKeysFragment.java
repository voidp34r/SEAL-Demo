// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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
import com.microsoft.asurerun.service.KeyGenService;
import com.microsoft.asurerun.ui.HomeActivity;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.asurerun.util.DialogUtil;

import static com.microsoft.asurerun.service.KeyGenService.STATE_KEY;
import static com.microsoft.asurerun.util.ServiceUtil.*;

public class SetKeysFragment extends Fragment {
    private final static String TAG = "SET_KEY_FR";
    private Button mBackgroundButton;
    private TextView mPrivateKeyText, mPublicKeyText, mCloudText, mDoneText, mTimeText;

    public SetKeysFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SetKeysFragment.
     */
    public static SetKeysFragment newInstance() {
        return new SetKeysFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // start keygenService
        startKeygenService();
    }

    /**
     * Register the broadcast receiver to KeyGenService intents
     */
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(STATE_KEY)) {
                String event = intent.getStringExtra(STATE_KEY);
                // set UI
                Log.e(TAG, event);
                if (event.equals(STATE_PUBLIC)) {
                    //set public  key completed state on UI
                    mPrivateKeyText.setTextColor(Color.WHITE);
                } else if (event.equals(STATE_PRIVATE)) {
                    //set private key completed state on UI
                    mPublicKeyText.setTextColor(Color.WHITE);
                } else if (event.equals(STATE_CLOUD)) {
                    //set send on cloud state on UI
                    mCloudText.setTextColor(Color.WHITE);
                } else if (event.equals(KEY_GEN_STATE_ERROR)) {
                    showErrorDialog();
                } else if (event.equals(STATE_DONE)) {
                    mDoneText.setTextColor(Color.WHITE);
                    HomeActivity activity = (HomeActivity) getActivity();
                    // load homeFragment
                    activity.loadFragment(HomeFragment.newInstance(), false, false, HomeFragment.TAG);
                }
            }
            if (intent.hasExtra(KEY_GEN_TIME_KEY)) {
                // update remaining time
                Log.e(TAG, "update time");
                int mRemainingTime = intent.getIntExtra(KEY_GEN_TIME_KEY, -1);
                if (mRemainingTime > 0) {
                    mTimeText.setText(mRemainingTime + " " + getActivity().getString(R.string.set_keys_time_text));
                } else if (mRemainingTime == 0) {
                    mTimeText.setText(getActivity().getString(R.string.set_keys_time_end));
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_set_keys, container, false);
        // add title to toolbar
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_gen_key);
        mPrivateKeyText = (TextView) v.findViewById(R.id.textState1);
        mPublicKeyText = (TextView) v.findViewById(R.id.textState2);
        mCloudText = (TextView) v.findViewById(R.id.textState3);
        mDoneText = (TextView) v.findViewById(R.id.textState4);
        mTimeText = (TextView) v.findViewById(R.id.timeText);
        final HomeActivity activity = (HomeActivity) getActivity();
        mBackgroundButton = (Button) v.findViewById(R.id.backgroundButton);
        mBackgroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // load homeFragment
                activity.loadFragment(HomeFragment.newInstance(), false, false, HomeFragment.TAG);
            }
        });
        return v;
    }

    /**
     * Shows the error and asks the user if to exit or try again
     */
    private void showErrorDialog() {
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // reset UI
                resetUI();
                // restart keygen service
                startKeygenService();
            }
        };
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // get the activity context
                Context context = SetKeysFragment.this.getActivity();
                // delete user token and info
                CacheUtil.deleteUserTokenAndInfo(context);
                // finish this activity
                ((Activity) context).finish();
            }
        };
        // show error dialog
        DialogUtil.showDialogPositiveAndNegativeButtons(getActivity(), getString(R.string.set_keys_error_title),
                getString(R.string.set_keys_error_message), positiveListener, getString(R.string.retry_button_error),
                negativeListener, getString(R.string.exit_button_error));
    }

    @Override
    public void onResume() {
        // check onResume if the keys are created
        if (ApplicationState.isKeysCreated()) {
            HomeActivity activity = (HomeActivity) getActivity();
            // load homeFragment
            activity.loadFragment(HomeFragment.newInstance(), false, false, HomeFragment.TAG);
        }
        super.onResume();
        //register broadcast receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(KEY_GEN_UPDATE_ACTION);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister broadcast receiver
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    private void startKeygenService() {
        // start keygenService
        Intent intent = new Intent(getActivity(), KeyGenService.class);
        getActivity().startService(intent);
    }

    /**
     * resets UI after restart of keygen service
     */
    private void resetUI() {
        String colorAccent = "#848484";
        mPrivateKeyText.setTextColor(Color.parseColor(colorAccent));
        mPublicKeyText.setTextColor(Color.parseColor(colorAccent));
        mCloudText.setTextColor(Color.parseColor(colorAccent));
        mDoneText.setTextColor(Color.parseColor(colorAccent));
        mTimeText.setText(getString(R.string.set_keys_total_time));
    }
}

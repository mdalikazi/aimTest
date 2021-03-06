package com.alikazi.codetestaim.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import com.alikazi.codetestaim.R;
import com.alikazi.codetestaim.models.PlayoutItem;
import com.alikazi.codetestaim.network.RequestsQueueHelper;
import com.alikazi.codetestaim.utils.AppConstants;
import com.alikazi.codetestaim.utils.DLog;
import com.alikazi.codetestaim.utils.Injector;
import com.alikazi.codetestaim.utils.LeftTopSnapHelper;
import com.alikazi.codetestaim.viewmodel.MainViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainFragment extends Fragment implements FeedAdapter.ItemSelectionListener {

    private static final String LOG_TAG = AppConstants.AIM_LOG_TAG;

    private static final String CURRENT_LIST_SIZE = "CURRENT_LIST_SIZE";

    private MainViewModel mMainViewModel;
    private FeedAdapter mAdapter;
    private RequestsQueueHelper mRequestsQueueHelper;

    private RecyclerView mRecyclerView;
    private TextView mEmptyMessageTextView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DLog.i(LOG_TAG, "onCreate");
        mRequestsQueueHelper = RequestsQueueHelper.getInstance(getActivity());
        mMainViewModel = ViewModelProviders.of(this,
                Injector.provideViewModelFactory(getActivity(), mRequestsQueueHelper))
                .get(MainViewModel.class);
        mMainViewModel.mFeed.observe(this, new Observer<ArrayList<PlayoutItem>>() {
            @Override
            public void onChanged(ArrayList<PlayoutItem> playoutItems) {
                DLog.d(LOG_TAG, "title: " + playoutItems.get(0).title);
                showEmptyMessage(playoutItems.isEmpty());
                mSwipeRefreshLayout.setRefreshing(false);
                mAdapter.submitList(playoutItems);
            }
        });
        mMainViewModel.mNetworkErrors.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                mEmptyMessageTextView.setText(getString(R.string.empty_list_message_error));
                showEmptyMessage(true);
                mSwipeRefreshLayout.setRefreshing(false);
                Snackbar.make(mSwipeRefreshLayout, "Oops! " + error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mRecyclerView = view.findViewById(R.id.main_recycler_view);
        mEmptyMessageTextView = view.findViewById(R.id.main_empty_list_message);
        mSwipeRefreshLayout = view.findViewById(R.id.main_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadFeed();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setupRecyclerView();
        setupAdapter();
        if (savedInstanceState == null) {
            // Don't load from API only if user changes orientation
            DLog.i(LOG_TAG, "savedInstanceState == null");
            loadFeed();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        DLog.i(LOG_TAG, "onSaveInstanceState");
        outState.putInt(CURRENT_LIST_SIZE, mAdapter.getItemCount());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.resetMediaPlayer();
        mRequestsQueueHelper.cancelAllRequests();
    }

    private void loadFeed() {
        mEmptyMessageTextView.setText(getString(R.string.empty_list_message_loading));
        showEmptyMessage(true);
        mSwipeRefreshLayout.setRefreshing(true);
        mRecyclerView.invalidate();
        mRecyclerView.scheduleLayoutAnimation();
        mMainViewModel.loadFeed();
    }

    private void setupRecyclerView() {
        DLog.i(LOG_TAG, "setupRecyclerView");
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        new LeftTopSnapHelper().attachToRecyclerView(mRecyclerView);
        LayoutAnimationController layoutAnimation = AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.vertical_layout_animation);
        mRecyclerView.setLayoutAnimation(layoutAnimation);
    }

    private void setupAdapter() {
        DLog.i(LOG_TAG, "setupAdapter");
        mAdapter = new FeedAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onItemSelected(PlayoutItem item) {
        Snackbar.make(mSwipeRefreshLayout, getString(R.string.snackbar_message_user_tapped, item.title), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * If API response is empty show empty message and hide recycler view
     * and vice versa
     * @param show
     */
    private void showEmptyMessage(boolean show) {
        mEmptyMessageTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}

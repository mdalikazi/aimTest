package com.alikazi.codetestaim.network;

import com.alikazi.codetestaim.models.ApiResponseModel;
import com.alikazi.codetestaim.models.PlayoutItem;
import com.alikazi.codetestaim.utils.AppConstants;
import com.alikazi.codetestaim.utils.DLog;

import java.util.ArrayList;

public class AppRepository {

    private static final String LOG_TAG = AppConstants.AIM_LOG_TAG;

    public ApiResponseModel loadFeed(RequestsQueueHelper requestsQueueHelper) {
        final ApiResponseModel apiResponseModel = new ApiResponseModel();
        RequestsProcessor.getFeedFromApi(requestsQueueHelper, new RequestsProcessor.FeedRequestListener() {
            @Override
            public void onSuccess(ArrayList<PlayoutItem> items) {
                DLog.i(LOG_TAG, "onSuccess");
                apiResponseModel._feed.postValue(items);
            }

            @Override
            public void onFailure(String errorMessage) {
                DLog.i(LOG_TAG, "onFailure: " + errorMessage);
                apiResponseModel._networkErrors.postValue(errorMessage);
            }
        });
        return apiResponseModel;
    }
}

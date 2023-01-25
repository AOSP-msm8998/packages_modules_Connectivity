/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.http.cts;

import static android.net.http.cts.util.TestUtilsKt.assertOKStatusCode;
import static android.net.http.cts.util.TestUtilsKt.skipIfNoInternetConnection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import android.content.Context;
import android.net.http.HttpEngine;
import android.net.http.UrlRequest;
import android.net.http.UrlRequest.Status;
import android.net.http.UrlResponseInfo;
import android.net.http.cts.util.HttpCtsTestServer;
import android.net.http.cts.util.TestStatusListener;
import android.net.http.cts.util.TestUrlRequestCallback;
import android.net.http.cts.util.TestUrlRequestCallback.ResponseStep;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UrlRequestTest {
    private TestUrlRequestCallback mCallback;
    private HttpCtsTestServer mTestServer;
    private HttpEngine mHttpEngine;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        skipIfNoInternetConnection(context);
        HttpEngine.Builder builder = new HttpEngine.Builder(context);
        mHttpEngine = builder.build();
        mCallback = new TestUrlRequestCallback();
        mTestServer = new HttpCtsTestServer(context);
    }

    @After
    public void tearDown() throws Exception {
        if (mHttpEngine != null) {
            mHttpEngine.shutdown();
        }
        if (mTestServer != null) {
            mTestServer.shutdown();
        }
    }

    private UrlRequest buildUrlRequest(String url) {
        return mHttpEngine.newUrlRequestBuilder(url, mCallback, mCallback.getExecutor()).build();
    }

    @Test
    public void testUrlRequestGet_CompletesSuccessfully() throws Exception {
        String url = mTestServer.getSuccessUrl();
        UrlRequest request = buildUrlRequest(url);
        request.start();

        mCallback.expectCallback(ResponseStep.ON_SUCCEEDED);
        UrlResponseInfo info = mCallback.mResponseInfo;
        assertOKStatusCode(info);
        assertThat("Received byte count must be > 0", info.getReceivedByteCount(), greaterThan(0L));
    }

    @Test
    public void testUrlRequestStatus_InvalidBeforeRequestStarts() throws Exception {
        UrlRequest request = buildUrlRequest(mTestServer.getSuccessUrl());
        // Calling before request is started should give Status.INVALID,
        // since the native adapter is not created.
        TestStatusListener statusListener = new TestStatusListener();
        request.getStatus(statusListener);
        statusListener.expectStatus(Status.INVALID);
    }
}
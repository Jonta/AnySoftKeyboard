/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.voiceime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.anysoftkeyboard.base.utils.Logger;

/**
 * Handles the connection, and the method call, and the call backs between the IME and the activity.
 */
class ServiceBridge {

  @SuppressWarnings("unused")
  private static final String TAG = "ServiceBridge";

  private final IntentApiTrigger.Callback mCallback;

  public ServiceBridge() {
    this(null);
  }

  public ServiceBridge(IntentApiTrigger.Callback callback) {
    mCallback = callback;
  }

  /** Start a voice search recognition. */
  public void startVoiceRecognition(final Context context, final String languageCode) {
    final ConnectionRequest conReq = new ConnectionRequest(languageCode);
    conReq.setServiceCallback(
        new ServiceHelper.Callback() {

          @Override
          public void onResult(final String recognitionResult) {
            mCallback.onRecognitionResult(recognitionResult);
            try {
              context.unbindService(conReq);
            } catch (IllegalArgumentException e) {
              // https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/432
              Logger.w(TAG, "Failed to unbind from service! Swallowing.", e);
            }
          }
        });

    context.bindService(new Intent(context, ServiceHelper.class), conReq, Context.BIND_AUTO_CREATE);
  }

  public void notifyResult(Context context, String recognitionResult) {
    ServiceConnection conn = new ConnectionResponse(context, recognitionResult);
    context.bindService(new Intent(context, ServiceHelper.class), conn, Context.BIND_AUTO_CREATE);
  }

  /** Service connection for requesting a recognition. */
  private static class ConnectionRequest implements ServiceConnection {

    private final String mLanguageCode;

    private ServiceHelper.Callback mServiceCallback;

    private ConnectionRequest(String languageCode) {
      mLanguageCode = languageCode;
    }

    private void setServiceCallback(ServiceHelper.Callback callback) {
      mServiceCallback = callback;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      ServiceHelper serviceHelper = ((ServiceHelper.ServiceHelperBinder) service).getService();
      serviceHelper.startRecognition(mLanguageCode, mServiceCallback);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      // Empty
    }
  }

  /** Service connection for notifying a recognition result. */
  private static class ConnectionResponse implements ServiceConnection {

    private final String mRecognitionResult;
    private final Context mContext;

    private ConnectionResponse(Context context, String recognitionResult) {
      mRecognitionResult = recognitionResult;
      mContext = context;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      // Empty
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      ServiceHelper serviceHelper = ((ServiceHelper.ServiceHelperBinder) service).getService();
      serviceHelper.notifyResult(mRecognitionResult);
      mContext.unbindService(this);
    }
  }
}

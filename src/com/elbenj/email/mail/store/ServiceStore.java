/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.elbenj.email.mail.store;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.elbenj.email.mail.Store;
import com.elbenj.emailcommon.mail.MessagingException;
import com.elbenj.emailcommon.provider.Account;
import com.elbenj.emailcommon.provider.HostAuth;
import com.elbenj.emailcommon.service.EmailServiceProxy;
import com.elbenj.emailcommon.service.IEmailService;

/**
 * Base class for service-based stores
 */
public abstract class ServiceStore extends Store {
    protected final HostAuth mHostAuth;

    protected abstract IEmailService getService();

    /**
     * Creates a new store for the given account.
     */
    public ServiceStore(Account account, Context context) throws MessagingException {
        mContext = context;
        mHostAuth = account.getOrCreateHostAuthRecv(mContext);
    }

    @Override
    public Bundle checkSettings() throws MessagingException {
        /**
         * Here's where we check the settings
         * @throws MessagingException if we can't authenticate the account
         */
        try {
            IEmailService svc = getService();
            // Use a longer timeout for the validate command.  Note that the instanceof check
            // shouldn't be necessary; we'll do it anyway, just to be safe
            if (svc instanceof EmailServiceProxy) {
                ((EmailServiceProxy)svc).setTimeout(90);
            }
            return svc.validate(mHostAuth);
        } catch (RemoteException e) {
            throw new MessagingException("Call to validate generated an exception", e);
        }
    }

    /**
     * We handle AutoDiscover here, wrapping the EmailService call. The service call returns a
     * HostAuth and we return null if there was a service issue
     */
    @Override
    public Bundle autoDiscover(Context context, String username, String password) {
        try {
            return getService().autoDiscover(username, password);
        } catch (RemoteException e) {
            return null;
        }
    }
}

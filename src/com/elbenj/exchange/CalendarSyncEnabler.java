/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.elbenj.exchange;

import com.elbenj.emailcommon.Logging;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import com.elbenj.email.R;

/**
 * Utility class to enable Exchange calendar sync for all existing Exchange accounts.
 *
 * <p>Exchange calendar was first supported on Froyo.  It wasn't supported on Eclair, which
 * was the first version that supported Exchange email.
 *
 * <p>This class is used only once when the devices is upgraded to Froyo (or later) from Eclair,
 * to enable calendar sync for all the existing Exchange accounts.
 */
public class CalendarSyncEnabler {
    private final Context mContext;

    public CalendarSyncEnabler(Context context) {
        this.mContext = context;
    }

    /**
     * Enable calendar sync for all the existing exchange accounts, and post a notification if any.
     */
    public final void enableEasCalendarSync() {
        String emailAddresses = enableEasCalendarSyncInternalForTest();
        if (emailAddresses.length() > 0) {
            // Exchange account(s) found.
            showNotificationForTest(emailAddresses.toString());
        }
    }

    /**
     * Enable calendar sync for all the existing exchange accounts
     *
     * @return email addresses of the Exchange accounts joined with spaces as delimiters,
     *     or the empty string if there's no Exchange accounts.
     */
    /* package for testing */ final String enableEasCalendarSyncInternalForTest() {
        StringBuilder emailAddresses = new StringBuilder();

        Account[] exchangeAccounts = AccountManager.get(mContext)
                .getAccountsByType(Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
        for (Account account : exchangeAccounts) {
            final String emailAddress = account.name;
            Log.i(Logging.LOG_TAG, "Enabling Exchange calendar sync for " + emailAddress);

            ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);

            // Accumulate addresses for notification.
            if (emailAddresses.length() > 0) {
                emailAddresses.append(' ');
            }
            emailAddresses.append(emailAddress);
        }
        return emailAddresses.toString();
    }

    // *** Taken from NotificationController
    public static final int NOTIFICATION_ID_EXCHANGE_CALENDAR_ADDED = 2;

    /**
     * Show the "Exchange calendar added" notification.
     *
     * @param emailAddresses space delimited list of email addresses of Exchange accounts.  It'll
     *     be shown on the notification.
     */
    /* package for testing */ void showNotificationForTest(String emailAddresses) {
        // Launch Calendar app when clicked.
        PendingIntent launchCalendarPendingIntent = PendingIntent.getActivity(mContext, 0,
                createLaunchCalendarIntent(), 0);

        String tickerText = mContext.getString(R.string.notification_exchange_calendar_added);
        Notification n = new Notification(android.R.drawable.stat_notify_voicemail,
                tickerText, System.currentTimeMillis());
        n.setLatestEventInfo(mContext, tickerText, emailAddresses, launchCalendarPendingIntent);
        n.flags = Notification.FLAG_AUTO_CANCEL;

        NotificationManager nm =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID_EXCHANGE_CALENDAR_ADDED, n);
    }

    /** @return {@link Intent} to launch the Calendar app. */
    private Intent createLaunchCalendarIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time"));
    }
}

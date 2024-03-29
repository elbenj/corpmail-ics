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

/**
 * MeetingResponseRequest is the EAS wrapper for responding to meeting requests.
 */
public class MeetingResponseRequest extends Request {
    public final int mResponse;

    MeetingResponseRequest(long messageId, int response) {
        super(messageId);
        mResponse = response;
    }

    // MeetingResponseRequests are unique by their message id (i.e. there's only one response to
    // a given message)
    public boolean equals(Object o) {
        if (!(o instanceof MeetingResponseRequest)) return false;
        return ((MeetingResponseRequest)o).mMessageId == mMessageId;
    }

    public int hashCode() {
        return (int)mMessageId;
    }
}

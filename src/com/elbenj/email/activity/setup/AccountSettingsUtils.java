/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.elbenj.email.activity.setup;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;

import com.elbenj.email.R;
import com.elbenj.email.VendorPolicyLoader;
import com.elbenj.email.provider.AccountBackupRestore;
import com.elbenj.emailcommon.Logging;
import com.elbenj.emailcommon.provider.Account;
import com.elbenj.emailcommon.provider.EmailContent.AccountColumns;
import com.google.common.annotations.VisibleForTesting;

import java.io.Serializable;

public class AccountSettingsUtils {

    /** Pattern to match any part of a domain */
    private final static String WILD_STRING = "*";
    /** Will match any, single character */
    private final static char WILD_CHARACTER = '?';
    private final static String DOMAIN_SEPARATOR = "\\.";

    /**
     * Commits the UI-related settings of an account to the provider.  This is static so that it
     * can be used by the various account activities.  If the account has never been saved, this
     * method saves it; otherwise, it just saves the settings.
     * @param context the context of the caller
     * @param account the account whose settings will be committed
     */
    public static void commitSettings(Context context, Account account) {
        if (!account.isSaved()) {
            account.save(context);
        } else {
            ContentValues cv = getAccountContentValues(account);
            account.update(context, cv);
        }
        // Update the backup (side copy) of the accounts
        AccountBackupRestore.backup(context);
    }

    /**
     * Returns a set of content values to commit account changes (not including the foreign keys
     * for the two host auth's and policy) to the database.  Does not actually commit anything.
     */
    public static ContentValues getAccountContentValues(Account account) {
        ContentValues cv = new ContentValues();
        cv.put(AccountColumns.IS_DEFAULT, account.mIsDefault);
        cv.put(AccountColumns.DISPLAY_NAME, account.getDisplayName());
        cv.put(AccountColumns.SENDER_NAME, account.getSenderName());
        cv.put(AccountColumns.SIGNATURE, account.getSignature());
        cv.put(AccountColumns.SYNC_INTERVAL, account.mSyncInterval);
        cv.put(AccountColumns.RINGTONE_URI, account.mRingtoneUri);
        cv.put(AccountColumns.FLAGS, account.mFlags);
        cv.put(AccountColumns.SYNC_LOOKBACK, account.mSyncLookback);
        cv.put(AccountColumns.SECURITY_SYNC_KEY, account.mSecuritySyncKey);
        return cv;
    }

    /**
     * Search the list of known Email providers looking for one that matches the user's email
     * domain.  We check for vendor supplied values first, then we look in providers_product.xml,
     * and finally by the entries in platform providers.xml.  This provides a nominal override
     * capability.
     *
     * A match is defined as any provider entry for which the "domain" attribute matches.
     *
     * @param domain The domain portion of the user's email address
     * @return suitable Provider definition, or null if no match found
     */
    public static Provider findProviderForDomain(Context context, String domain) {
        Provider p = VendorPolicyLoader.getInstance(context).findProviderForDomain(domain);
        if (p == null) {
            p = findProviderForDomain(context, domain, R.xml.providers_product);
        }
        if (p == null) {
            p = findProviderForDomain(context, domain, R.xml.providers);
        }
        return p;
    }

    /**
     * Search a single resource containing known Email provider definitions.
     *
     * @param domain The domain portion of the user's email address
     * @param resourceId Id of the provider resource to scan
     * @return suitable Provider definition, or null if no match found
     */
    /*package*/ static Provider findProviderForDomain(
            Context context, String domain, int resourceId) {
        try {
            XmlResourceParser xml = context.getResources().getXml(resourceId);
            int xmlEventType;
            Provider provider = null;
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG
                        && "provider".equals(xml.getName())) {
                    String providerDomain = getXmlAttribute(context, xml, "domain");
                    try {
                        if (matchProvider(domain, providerDomain)) {
                            provider = new Provider();
                            provider.id = getXmlAttribute(context, xml, "id");
                            provider.label = getXmlAttribute(context, xml, "label");
                            provider.domain = domain.toLowerCase();
                            provider.note = getXmlAttribute(context, xml, "note");
                        }
                    } catch (IllegalArgumentException e) {
                        Log.w(Logging.LOG_TAG, "providers line: " + xml.getLineNumber() +
                                "; Domain contains multiple globals");
                    }
                }
                else if (xmlEventType == XmlResourceParser.START_TAG
                        && "incoming".equals(xml.getName())
                        && provider != null) {
                    provider.incomingUriTemplate = getXmlAttribute(context, xml, "uri");
                    provider.incomingUsernameTemplate = getXmlAttribute(context, xml, "username");
                }
                else if (xmlEventType == XmlResourceParser.START_TAG
                        && "outgoing".equals(xml.getName())
                        && provider != null) {
                    provider.outgoingUriTemplate = getXmlAttribute(context, xml, "uri");
                    provider.outgoingUsernameTemplate = getXmlAttribute(context, xml, "username");
                }
                else if (xmlEventType == XmlResourceParser.END_TAG
                        && "provider".equals(xml.getName())
                        && provider != null) {
                    return provider;
                }
            }
        }
        catch (Exception e) {
            Log.e(Logging.LOG_TAG, "Error while trying to load provider settings.", e);
        }
        return null;
    }

    /**
     * Returns true if the string <code>s1</code> matches the string <code>s2</code>. The string
     * <code>s2</code> may contain any number of wildcards -- a '?' character -- and/or asterisk
     * characters -- '*'. Wildcards match any single character, while the asterisk matches a domain
     * part (i.e. substring demarcated by a period, '.')
     */
    @VisibleForTesting
    static boolean matchProvider(String testDomain, String providerDomain) {
        String[] testParts = testDomain.split(DOMAIN_SEPARATOR);
        String[] providerParts = providerDomain.split(DOMAIN_SEPARATOR);
        if (testParts.length != providerParts.length) {
            return false;
        }
        for (int i = 0; i < testParts.length; i++) {
            String testPart = testParts[i].toLowerCase();
            String providerPart = providerParts[i].toLowerCase();
            if (!providerPart.equals(WILD_STRING) &&
                    !matchWithWildcards(testPart, providerPart)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchWithWildcards(String testPart, String providerPart) {
        int providerLength = providerPart.length();
        if (testPart.length() != providerLength){
            return false;
        }
        for (int i = 0; i < providerLength; i++) {
            char testChar = testPart.charAt(i);
            char providerChar = providerPart.charAt(i);
            if (testChar != providerChar && providerChar != WILD_CHARACTER) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to get the given attribute as a String resource first, and if it fails
     * returns the attribute as a simple String value.
     * @param xml
     * @param name
     * @return the requested resource
     */
    private static String getXmlAttribute(Context context, XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        }
        else {
            return context.getString(resId);
        }
    }

    public static class Provider implements Serializable {
        private static final long serialVersionUID = 8511656164616538989L;

        public String id;
        public String label;
        public String domain;
        public String incomingUriTemplate;
        public String incomingUsernameTemplate;
        public String outgoingUriTemplate;
        public String outgoingUsernameTemplate;
        public String incomingUri;
        public String incomingUsername;
        public String outgoingUri;
        public String outgoingUsername;
        public String note;

        /**
         * Expands templates in all of the  provider fields that support them. Currently,
         * templates are used in 4 fields -- incoming and outgoing URI and user name.
         * @param email user-specified data used to replace template values
         */
        public void expandTemplates(String email) {
            String[] emailParts = email.split("@");
            String user = emailParts[0];

            incomingUri = expandTemplate(incomingUriTemplate, email, user);
            incomingUsername = expandTemplate(incomingUsernameTemplate, email, user);
            outgoingUri = expandTemplate(outgoingUriTemplate, email, user);
            outgoingUsername = expandTemplate(outgoingUsernameTemplate, email, user);
        }

        /**
         * Replaces all parameterized values in the given template. The values replaced are
         * $domain, $user and $email.
         */
        private String expandTemplate(String template, String email, String user) {
            String returnString = template;
            returnString = returnString.replaceAll("\\$email", email);
            returnString = returnString.replaceAll("\\$user", user);
            returnString = returnString.replaceAll("\\$domain", domain);
            return returnString;
        }
    }

    /**
     * Infer potential email server addresses from domain names
     *
     * Incoming: Prepend "imap" or "pop3" to domain, unless "pop", "pop3",
     *          "imap", or "mail" are found.
     * Outgoing: Prepend "smtp" if "pop", "pop3", "imap" are found.
     *          Leave "mail" as-is.
     * TBD: Are there any useful defaults for exchange?
     *
     * @param server name as we know it so far
     * @param incoming "pop3" or "imap" (or null)
     * @param outgoing "smtp" or null
     * @return the post-processed name for use in the UI
     */
    public static String inferServerName(String server, String incoming, String outgoing) {
        // Default values cause entire string to be kept, with prepended server string
        int keepFirstChar = 0;
        int firstDotIndex = server.indexOf('.');
        if (firstDotIndex != -1) {
            // look at first word and decide what to do
            String firstWord = server.substring(0, firstDotIndex).toLowerCase();
            boolean isImapOrPop = "imap".equals(firstWord)
                    || "pop3".equals(firstWord) || "pop".equals(firstWord);
            boolean isMail = "mail".equals(firstWord);
            // Now decide what to do
            if (incoming != null) {
                // For incoming, we leave imap/pop/pop3/mail alone, or prepend incoming
                if (isImapOrPop || isMail) {
                    return server;
                }
            } else {
                // For outgoing, replace imap/pop/pop3 with outgoing, leave mail alone, or
                // prepend outgoing
                if (isImapOrPop) {
                    keepFirstChar = firstDotIndex + 1;
                } else if (isMail) {
                    return server;
                } else {
                    // prepend
                }
            }
        }
        return ((incoming != null) ? incoming : outgoing) + '.' + server.substring(keepFirstChar);
    }

    /**
     * Helper to set error status on password fields that have leading or trailing spaces
     */
    public static void checkPasswordSpaces(Context context, EditText passwordField) {
        Editable password = passwordField.getText();
        int length = password.length();
        if (length > 0) {
            if (password.charAt(0) == ' ' || password.charAt(length-1) == ' ') {
                passwordField.setError(context.getString(R.string.account_password_spaces_error));
            }
        }
    }

}

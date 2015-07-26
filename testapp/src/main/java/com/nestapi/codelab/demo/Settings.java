/**
 * Copyright 2014 Nest Labs Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software * distributed under
 * the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nestapi.codelab.demo;

import android.content.Context;
import android.content.SharedPreferences;
import com.nestapi.lib.API.AccessToken;

public class Settings {

    private static final String TOKEN_KEY = "token";
    private static final String EXPIRATION_KEY = "expiration";

	public static void clearAuthToken(Context context) {
		getPrefs(context).edit()
				.putString(TOKEN_KEY, null)
				.putLong(EXPIRATION_KEY, -1)
				.commit();
	}

    public static void saveAuthToken(Context context, AccessToken token) {
        getPrefs(context).edit()
                .putString(TOKEN_KEY, token.getToken())
                .putLong(EXPIRATION_KEY, token.getExpiresIn())
                .commit();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(AccessToken.class.getSimpleName(), 0);
    }

    public static AccessToken loadAuthToken(Context context) {
        final SharedPreferences prefs = getPrefs(context);
        final String token = prefs.getString(TOKEN_KEY, null);
        final long expirationDate = prefs.getLong(EXPIRATION_KEY, -1);

        if(token == null || expirationDate == -1) {
            return null;
        }

        return new AccessToken.Builder()
                .setToken(token)
                .setExpiresIn(expirationDate)
                .build();
    }
}

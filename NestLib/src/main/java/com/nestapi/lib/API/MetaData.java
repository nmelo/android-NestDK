package com.nestapi.lib.API;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

import java.util.Map;

public class MetaData implements Parcelable {

    private final String mAccessToken;
    private final long mClientVersion;

    private MetaData(Builder builder) {
        mAccessToken = builder.mAccessToken;
        mClientVersion = builder.mClientVersion;
    }

    private MetaData(Parcel in) {
        mAccessToken = in.readString();
        mClientVersion = in.readLong();
    }

    /**
     * Part of user authorization, your client will use an access token to make API calls to the
     * Nest service. This access token serves as proof that a user has authorized your client
     * to make calls on their behalf.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Client version is the last user-authorized version of a client, and is associated
     * with an access_token.
     */
    public long getClientVersion() {
        return mClientVersion;
    }

    public static class Builder {
        private String mAccessToken;
        private long mClientVersion;

        public MetaData fromJSON(JSONObject json) {
            setAccessToken(json.optString(Keys.META_DATA.ACCESS_TOKEN));
            setClientVersion(json.optLong(Keys.META_DATA.CLIENT_VERSION));
            return new MetaData(this);
        }

        public MetaData fromMap(Map<String, Object> metadataMap) {
            return fromJSON(new JSONObject(metadataMap));
        }

        public Builder setAccessToken(String accessToken) {
            mAccessToken = accessToken;
            return this;
        }

        public Builder setClientVersion(long clientVersion) {
            mClientVersion = clientVersion;
            return this;
        }

        public MetaData build() {
            return new MetaData(this);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mAccessToken);
        parcel.writeLong(mClientVersion);
    }

    public static final Creator<MetaData> CREATOR = new Creator<MetaData>() {
        @Override
        public MetaData createFromParcel(Parcel parcel) {
            return new MetaData(parcel);
        }

        @Override
        public MetaData[] newArray(int i) {
            return new MetaData[i];
        }
    };

    @Override
    public String toString() {
        return "MetaData{" +
                "mAccessToken='" + mAccessToken + '\'' +
                ", mClientVersion=" + mClientVersion +
                '}';
    }
}

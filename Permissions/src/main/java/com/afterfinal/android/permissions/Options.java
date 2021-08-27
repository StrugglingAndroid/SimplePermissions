package com.afterfinal.android.permissions;

import android.os.Parcel;
import android.os.Parcelable;

public class Options implements Parcelable {
    public boolean execWhenRejected = true;
    public boolean exitWhenRejected = false;
    public TipMode tipMode = TipMode.Toast;

    public Options() {
    }

    public Options(boolean execWhenRejected, boolean exitWhenRejected, TipMode tipMode) {
        this.execWhenRejected = execWhenRejected;
        this.exitWhenRejected = exitWhenRejected;
        this.tipMode = tipMode;
    }

    protected Options(Parcel in) {
        execWhenRejected = in.readByte() != 0;
        exitWhenRejected = in.readByte() != 0;
    }

    public static final Creator<Options> CREATOR = new Creator<Options>() {
        @Override
        public Options createFromParcel(Parcel in) {
            return new Options(in);
        }

        @Override
        public Options[] newArray(int size) {
            return new Options[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (execWhenRejected ? 1 : 0));
        dest.writeByte((byte) (exitWhenRejected ? 1 : 0));
    }
}

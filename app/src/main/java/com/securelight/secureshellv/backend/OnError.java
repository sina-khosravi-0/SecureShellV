package com.securelight.secureshellv.backend;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

public class OnError implements Handler.Callback {
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        return false;
    }
}

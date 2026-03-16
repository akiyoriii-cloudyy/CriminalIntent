package com.example.criminalintent;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import java.io.Serializable;

public final class SerializationCompat {
    private SerializationCompat() {
    }

    public static <T extends Serializable> T getSerializable(Bundle bundle, String key, Class<T> valueClass) {
        if (bundle == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return bundle.getSerializable(key, valueClass);
        }

        Serializable value = bundle.getSerializable(key);
        return valueClass.isInstance(value) ? valueClass.cast(value) : null;
    }

    public static <T extends Serializable> T getSerializableExtra(
            Intent intent,
            String key,
            Class<T> valueClass
    ) {
        if (intent == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getSerializableExtra(key, valueClass);
        }

        Serializable value = intent.getSerializableExtra(key);
        return valueClass.isInstance(value) ? valueClass.cast(value) : null;
    }
}

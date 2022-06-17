package com.opengg.loader.loading;

import java.io.IOException;

public class FileLoadException extends IOException {
    public FileLoadException(String message, Throwable ex) {
        super(message, ex);
    }

}

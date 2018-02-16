package com.qmetry;

public class QMetryException extends Exception {

    public QMetryException(String message) {
        super(message);
    }

    public QMetryException() {
        super("");
    }
}
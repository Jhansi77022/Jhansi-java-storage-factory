package com.example.service;

public class TodoServiceException extends RuntimeException {

    public TodoServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

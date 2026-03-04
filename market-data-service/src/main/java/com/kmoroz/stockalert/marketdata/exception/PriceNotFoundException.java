package com.kmoroz.stockalert.marketdata.exception;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(String message) {
        super(message);
    }
}

package com.kmoroz.stockalert.injector;

import io.micronaut.runtime.Micronaut;

public class Application {
    public static void main(String[] args) {
        System.out.printf("Price injector module");
        Micronaut.run(Application.class, args);
    }
}
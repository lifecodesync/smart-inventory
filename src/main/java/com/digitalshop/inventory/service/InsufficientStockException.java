package com.digitalshop.inventory.service;

/** Thrown when a sale asks for more than is on the shelf. HTTP 409. */
public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String productName, int requested, int available) {
        super("Not enough " + productName + ": requested " + requested + ", only " + available + " left");
    }
}

package com.canteen.inventory.service;

/** level: OUT (red), LOW (orange), SHORTAGE (yellow: forecast says we'll run out soon). */
public record Alert(String level, Long productId, String productName, String message) { }

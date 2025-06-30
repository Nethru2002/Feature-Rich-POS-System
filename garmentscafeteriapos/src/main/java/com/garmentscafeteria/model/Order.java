package com.garmentscafeteria.model;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<OrderItem> items;
    private User currentUser;
    private double discountRate = 0.0; // e.g., 0.10 for 10%

    public Order(User currentUser) {
        this.currentUser = currentUser;
        this.items = new ArrayList<>();
    }

    public void addItem(OrderItem item) { this.items.add(item); }
    public List<OrderItem> getItems() { return items; }
    public User getCurrentUser() { return currentUser; }

    public void setDiscountRate(double discountRate) { this.discountRate = discountRate; }
    public double getDiscountRate() { return discountRate; }

    public double getSubtotal() {
        return items.stream().mapToDouble(OrderItem::getSubtotal).sum();
    }

    public double getDiscountAmount() {
        return getSubtotal() * discountRate;
    }

    public double getTotal() {
        return getSubtotal() - getDiscountAmount();
    }
}
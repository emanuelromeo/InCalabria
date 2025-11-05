package com.incalabria.stripe_checkout.data.booking;

import com.stripe.model.Address;

public class Customer {
    private String name;
    private String email;
    private String phone;
    private String taxId;
    private Address address;

    public Customer(String name, String email, String phone, String taxId, Address address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.taxId = taxId;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "{\n" +
               "    Name: " + name + '\n' +
               "    Email: " + email + '\n' +
               "    Phone: " + phone + '\n' +
               "    Tax ID: " + taxId + '\n' +
               "    Address: " + address.getLine1() +
               (address.getLine2() != null ? ", " + address.getLine2() : "") +
               ", " + address.getPostalCode() +
               ", " + address.getCity() +
               ", " + address.getState() +
               ", " + address.getCountry() +
               "\n}";
    }
}

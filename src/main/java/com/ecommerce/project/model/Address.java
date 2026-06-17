package com.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @NotBlank
    @Size(min = 2, message = "Street name must be least 2 characters")
    private String street;

    @NotBlank
    @Size(min = 2, message = "Building name must be least 2 characters")
    private String building;

    @NotBlank
    @Size(min = 2, message = "City name must be least 2 characters")
    private String city;

    @NotBlank
    @Size(min = 2, message = "State name must be least 2 characters")
    private String state;

    @NotBlank
    @Size(min = 2, message = "Country name must be least 2 characters")
    private String country;

    @NotBlank
    @Size(min = 6, message = "Pin code must be of 6 characters", max = 6)
    private String pinCode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Address(String street, String building, String city, String state, String pinCode) {
        this.street = street;
        this.building = building;
        this.city = city;
        this.state = state;
        this.pinCode = pinCode;
    }
}

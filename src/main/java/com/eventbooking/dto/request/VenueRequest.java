package com.eventbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VenueRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String address;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotNull
    @Positive
    private Integer capacity;

    private String description;
}

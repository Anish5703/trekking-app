package com.example.trekking_app.entity;

import com.example.trekking_app.model.ElectricitySource;
import com.example.trekking_app.model.WaterSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotel_details")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HotelDetail extends POI{

    @Column(length = 500)
    private String address;

    @Column(name = "contact_number" , length = 20)
    private String contactNumber;

    @Column(name = "total_rooms")
    private Integer totalRooms;

    @Column(name = "price_nepali")
    private Double priceNepali;

    @Column(name = "price_foreigner")
    private Double priceForeigner;

    @Enumerated(EnumType.STRING)
    @Column(name = "electricity_source" , length = 20)
    private ElectricitySource electricitySource;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_source" , length = 20)
    private WaterSource waterSource;

    @Column(name = "has_first_aid")
    private Boolean hasFirstAid;



}

package com.team19.musuimsa.shelter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "shelters")
public class Shelter {

    // RSTR_FCLTY_NO
    @Id
    private Long shelterId;

    // RSTR_NM
    @Column(nullable = false, length = 100)
    private String name;

    // DTL_ADRES
    @Column(nullable = false, length = 100)
    private String address;

    // LA
    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    // LO
    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    // WKDAY_OPER_BEGIN_TIME
    private LocalTime weekdayOpenTime;

    // WKDAY_OPER_END_TIME
    private LocalTime weekdayCloseTime;

    // WKEND_HDAY_OPER_BEGIN_TIME
    private LocalTime weekendOpenTime;

    // WKEND_HDAY_OPER_END_TIME
    private LocalTime weekendCloseTime;

    // USE_PSBL_NMPR
    private Integer capacity;

    // FCLTY_TY == '002' → true
    private Boolean isOutdoors;

    // COLR_HOLD_ELFN
    private Integer fanCount;

    // COLR_HOLD_ARCNDTN
    private Integer airConditionerCount;

    private Integer totalRating;

    private Integer reviewCount;

    @Column(length = 255)
    private String photoUrl;

}

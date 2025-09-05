package com.team19.musuimsa.shelter.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.*;

@Entity
@Table(name = "shelters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Shelter {

    @Id
    private Long shelterId; // PK: RSTR_FCLTY_NO

    @Column(nullable = false, length = 100)
    private String name; // RSTR_NM

    @Column(length = 255)
    private String photoUrl;

    @Column(nullable = false, length = 255)
    private String address; // RN_DTL_ADRES

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude; // LA

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude; // LO

    private Integer capacity; // USE_PSBL_NMPR

    private Integer fanCount; // COLR_HOLD_ELFN

    private Integer airConditionerCount; // COLR_HOLD_ARCDTN

    private Boolean isOutdoors; // FCLTY_TY == '002' → true

    private LocalTime weekdayOpenTime; // WKDAY_OPER_BEGIN_TIME

    private LocalTime weekdayCloseTime; // WKDAY_OPER_END_TIME

    private LocalTime weekendOpenTime; // WKEND_HDAY_OPER_BEGIN_TIME

    private LocalTime weekendCloseTime; // WKEND_HDAY_OPER_END_TIME

    private Integer totalRating;

    private Integer reviewCount;

    @PrePersist
    void initDefaults() {
        if (totalRating == null) {
            totalRating = 0;
        }
        if (reviewCount == null) {
            reviewCount = 0;
        }
    }
}

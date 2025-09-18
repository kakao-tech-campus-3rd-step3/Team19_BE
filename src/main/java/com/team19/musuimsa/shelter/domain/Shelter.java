package com.team19.musuimsa.shelter.domain;

import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private String photoUrl;

    @Version
    private Long version;

    public void updateTotalRating(Integer totalRating) {
        this.totalRating = totalRating;
    }

    public void updateReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public static Shelter toShelter(ExternalShelterItem i) {
        return Shelter.builder()
                .shelterId(i.rstrFcltyNo())
                .name(i.rstrNm())
                .address(i.rnDtlAdres())
                .latitude(i.la())
                .longitude(i.lo())
                .capacity(i.usePsblNmpr())
                .fanCount(i.colrHoldElefn())
                .airConditionerCount(i.colrHoldArcdtn())
                .weekdayOpenTime(parseTime(i.wkdayOperBeginTime()))
                .weekdayCloseTime(parseTime(i.wkdayOperEndTime()))
                .weekendOpenTime(parseTime(i.wkendHdayOperBeginTime()))
                .weekendCloseTime(parseTime(i.wkendHdayOperEndTime()))
                .isOutdoors("002".equals(i.fcltyTy()))
                .photoUrl(null)
                .build();
    }

    // String 시간 파싱
    private static LocalTime parseTime(String raw) {
        if (raw == null) {
            return null;
        }

        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() == 3) {
            digits = "0" + digits;
        }

        return LocalTime.parse(digits, DateTimeFormatter.ofPattern("HHmm"));
    }
}

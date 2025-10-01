package com.team19.musuimsa.shelter.domain;

import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

import static com.team19.musuimsa.batch.ShelterImportBatchConfig.parseTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "shelters")
public class Shelter {

    private static final String OUTDOOR_FACILITY_CODE = "002";

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

    @Column
    private String photoUrl;

    @Version
    private Long version;

    public void updateTotalRating(Integer totalRating) {
        this.totalRating = totalRating;
    }

    public void updateReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public boolean updatePhotoUrl(String newPhotoUrl) {
        if (newPhotoUrl == null || newPhotoUrl.isBlank()) {
            return false;
        }
        if (newPhotoUrl.equals(this.photoUrl)) {
            return false;
        }
        this.photoUrl = newPhotoUrl;
        return true;
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
                .isOutdoors(OUTDOOR_FACILITY_CODE.equals(i.fcltyTy()))
                .photoUrl(null)
                .build();
    }

    public boolean updateShelterInfo(ExternalShelterItem item, LocalTime weekdayOpen,
                                     LocalTime weekdayClose, LocalTime weekendOpen, LocalTime weekendClose) {
        boolean isChanged = false;

        if (!Objects.equals(this.name, item.rstrNm())) {
            this.name = item.rstrNm();
            isChanged = true;
        }
        if (!Objects.equals(this.address, item.rnDtlAdres())) {
            this.address = item.rnDtlAdres();
            isChanged = true;
        }
        if (this.latitude.compareTo(item.la()) != 0) {
            this.latitude = item.la();
            isChanged = true;
        }
        if (this.longitude.compareTo(item.lo()) != 0) {
            this.longitude = item.lo();
            isChanged = true;
        }
        if (!Objects.equals(this.capacity, item.usePsblNmpr())) {
            this.capacity = item.usePsblNmpr();
            isChanged = true;
        }
        if (!Objects.equals(this.fanCount, item.colrHoldElefn())) {
            this.fanCount = item.colrHoldElefn();
            isChanged = true;
        }
        if (!Objects.equals(this.airConditionerCount, item.colrHoldArcdtn())) {
            this.airConditionerCount = item.colrHoldArcdtn();
            isChanged = true;
        }
        if (!Objects.equals(this.weekdayOpenTime, weekdayOpen)) {
            this.weekdayOpenTime = weekdayOpen;
            isChanged = true;
        }
        if (!Objects.equals(this.weekdayCloseTime, weekdayClose)) {
            this.weekdayCloseTime = weekdayClose;
            isChanged = true;
        }
        if (!Objects.equals(this.weekendOpenTime, weekendOpen)) {
            this.weekendOpenTime = weekendOpen;
            isChanged = true;
        }
        if (!Objects.equals(this.weekendCloseTime, weekendClose)) {
            this.weekendCloseTime = weekendClose;
            isChanged = true;
        }
        boolean outdoors = OUTDOOR_FACILITY_CODE.equals(item.fcltyTy());
        if (!Objects.equals(this.isOutdoors, outdoors)) {
            this.isOutdoors = outdoors;
            isChanged = true;
        }
        return isChanged;
    }
}

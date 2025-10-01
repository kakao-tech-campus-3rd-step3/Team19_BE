package com.team19.musuimsa.shelter.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalShelterItem(
        @JsonProperty("RSTR_FCLTY_NO")
        Long rstrFcltyNo,

        @JsonProperty("RSTR_NM")
        String rstrNm,

        @JsonProperty("RN_DTL_ADRES")
        String rnDtlAdres,

        @JsonProperty("LA")
        BigDecimal la,

        @JsonProperty("LO")
        BigDecimal lo,

        @JsonProperty("USE_PSBL_NMPR")
        Integer usePsblNmpr,

        @JsonProperty("COLR_HOLD_ELEFN")
        Integer colrHoldElefn,

        @JsonProperty("COLR_HOLD_ARCNDTN")
        Integer colrHoldArcdtn,

        @JsonProperty("WKDAY_OPER_BEGIN_TIME")
        String wkdayOperBeginTime,

        @JsonProperty("WKDAY_OPER_END_TIME")
        String wkdayOperEndTime,

        @JsonProperty("WKEND_HDAY_OPER_BEGIN_TIME")
        String wkendHdayOperBeginTime,

        @JsonProperty("WKEND_HDAY_OPER_END_TIME")
        String wkendHdayOperEndTime,

        @JsonProperty("FCLTY_TY")
        String fcltyTy
) implements Serializable {

}


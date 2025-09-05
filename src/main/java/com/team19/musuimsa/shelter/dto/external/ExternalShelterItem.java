package com.team19.musuimsa.shelter.dto.external;

public record ExternalShelterItem(
        String RSTR_FCLTY_NO,
        String RSTR_NM,
        String RN_DTL_ADRES,
        String LA,
        String LO,
        String USE_PSBL_NMPR,
        String COLR_HOLD_ELFN,
        String COLR_HOLD_ARCDTN,
        String WKDAY_OPER_BEGIN_TIME,
        String WKDAY_OPER_END_TIME,
        String WKEND_HDAY_OPER_BEGIN_TIME,
        String WKEND_HDAY_OPER_END_TIME,
        String FCLTY_TY,
        String PHOTO_URL
) {}

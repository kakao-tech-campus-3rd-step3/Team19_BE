package com.team19.musuimsa.review.converter;

import com.team19.musuimsa.review.domain.Rating;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RatingConverter implements AttributeConverter<Rating, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Rating attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public Rating convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : Rating.of(dbData);
    }
}

package com.alfano.gathorapp.review;

import com.alfano.gathorapp.review.dto.ReviewResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Review entity and DTOs.
 */
@Component
public class ReviewMapper {

    /**
     * Convert Review entity to ReviewResponse DTO.
     */
    public ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .reviewer(ReviewResponse.ReviewerInfo.builder()
                        .id(review.getReviewer().getId())
                        .name(review.getReviewer().getName())
                        .build())
                .eventId(review.getEvent() != null ? review.getEvent().getId() : null)
                .outingId(review.getOuting() != null ? review.getOuting().getId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

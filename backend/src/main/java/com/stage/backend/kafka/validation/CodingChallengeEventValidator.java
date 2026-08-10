package com.stage.backend.kafka.validation;

import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.kafka.exception.InvalidCodingChallengeEventException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CodingChallengeEventValidator {

    public void validate(CodingChallengeEvent event) {
        if (event == null) {
            throw new InvalidCodingChallengeEventException("Coding challenge event must not be null");
        }
        if (event.test() == null) {
            throw new InvalidCodingChallengeEventException("Event must contain a test payload");
        }
        if (event.user() == null) {
            throw new InvalidCodingChallengeEventException("Event must contain a user payload");
        }
        if (event.test().id() == null || event.test().id() <= 0) {
            throw new InvalidCodingChallengeEventException("Test id must be a positive number");
        }
        if (event.user().id() == null || event.user().id() <= 0) {
            throw new InvalidCodingChallengeEventException("User id must be a positive number");
        }
        if (!StringUtils.hasText(event.test().titre())) {
            throw new InvalidCodingChallengeEventException(
                    "Test titre is required for id: " + event.test().id()
            );
        }
        if (!StringUtils.hasText(event.user().email())) {
            throw new InvalidCodingChallengeEventException(
                    "User email is required for test id: " + event.test().id()
            );
        }
    }

}
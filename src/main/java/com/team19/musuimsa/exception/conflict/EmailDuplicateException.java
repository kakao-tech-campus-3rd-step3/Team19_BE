package com.team19.musuimsa.exception.conflict;

public class EmailDuplicateException extends DataConflictException {

    public EmailDuplicateException(String email) {
        super("이미 사용 중인 이메일입니다: " + email);
    }
}

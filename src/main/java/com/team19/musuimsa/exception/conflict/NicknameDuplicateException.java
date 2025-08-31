package com.team19.musuimsa.exception.conflict;

public class NicknameDuplicateException extends DataConflictException {

    public NicknameDuplicateException(String nickname) {
        super("이미 사용 중인 닉네임입니다: " + nickname);
    }
}

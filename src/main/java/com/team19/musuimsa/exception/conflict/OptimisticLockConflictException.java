package com.team19.musuimsa.exception.conflict;

public class OptimisticLockConflictException extends DataConflictException {
    public OptimisticLockConflictException() {
        super("잠시 후 다시 시도해 주세요.");
    }
}

package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    /**
     * 충전 포인트 계산
     */
    public long calculateChargePoint(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("포인트 충전 금액은 0보다 커야 합니다.");
        }

        // 정책: 포인트 충전은 10원 단위로 가능
        if (amount % 10 != 0) {
            throw new IllegalArgumentException("포인트 충전은 10원 단위로 가능합니다.");
        }

        long newPoint = this.point + amount;

        // 정책: 보유 포인트는 50000원을 초과할 수 없음
        if (newPoint > 50000) {
            throw new IllegalArgumentException("보유 포인트는 50000원을 초과할 수 없습니다.");
        }

        return newPoint;
    }

    /**
     * 사용 포인트 계산
     */
    public long calculateUsePoint(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("포인트 사용 금액은 0보다 커야 합니다.");
        }

        if (amount > this.point) {
            throw new IllegalArgumentException("포인트 사용 금액은 현재 포인트를 초과할 수 없습니다.");
        }

        return this.point - amount;
    }

}

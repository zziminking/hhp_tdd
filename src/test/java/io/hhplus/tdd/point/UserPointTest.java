package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPointTest {

    @Test
    @DisplayName("포인트 충전 - 실패: 0원 충전")
    void charge_point_fail_zero_amount() {
        // given
        UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());
        long chargeAmount = 0L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userPoint.calculateChargePoint(chargeAmount)
        );

        assertEquals("포인트 충전 금액은 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 - 실패: 음수 충전")
    void charge_point_fail_negative_amount() {
        // given
        UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());
        long chargeAmount = -100L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userPoint.calculateChargePoint(chargeAmount)
        );

        assertEquals("포인트 충전 금액은 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 - 실패: 10원 단위가 아닌 금액")
    void charge_point_fail_not_multiple_of_10() {
        // given
        UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());
        long chargeAmount = 5L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userPoint.calculateChargePoint(chargeAmount)
        );

        assertEquals("포인트 충전은 10원 단위로 가능합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 - 실패: 총 포인트 50000원 초과")
    void charge_point_fail_exceed_total_limit() {
        // given
        UserPoint userPoint = new UserPoint(1L, 45000L, System.currentTimeMillis());
        long chargeAmount = 10000L;

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userPoint.calculateChargePoint(chargeAmount)
        );

        assertEquals("보유 포인트는 50000원을 초과할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 - 성공: 정상 충전")
    void charge_point_success() {
        // given
        UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());
        long chargeAmount = 500L;

        // when
        long result = userPoint.calculateChargePoint(chargeAmount);

        // then
        assertEquals(1500L, result);
    }

    @Test
    @DisplayName("포인트 충전 - 성공: 경계값 테스트")
    void charge_point_success_50000() {
        // given
        UserPoint userPoint = new UserPoint(1L, 40000L, System.currentTimeMillis());
        long chargeAmount = 10000L;

        // when
        long result = userPoint.calculateChargePoint(chargeAmount);

        // then
        assertEquals(50000L, result);
    }

}
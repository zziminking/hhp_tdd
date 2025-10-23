package io.hhplus.tdd.point.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.point.domain.repository.UserPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserPointRepository userPointRepository;

    @InjectMocks
    private PointService pointService;

    UserPoint testUserPoint;
    List<PointHistory> pointHistoryList;

    @BeforeEach
    void setUp() {
        testUserPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

        pointHistoryList = new ArrayList<>();
        pointHistoryList.add(new PointHistory(1L, 1L, 1000L, TransactionType.CHARGE, System.currentTimeMillis()));
        pointHistoryList.add(new PointHistory(2L, 1L, 2000L, TransactionType.CHARGE, System.currentTimeMillis()));
        pointHistoryList.add(new PointHistory(3L, 1L, 3000L, TransactionType.CHARGE, System.currentTimeMillis()));
        pointHistoryList.add(new PointHistory(4L, 1L, 4000L, TransactionType.CHARGE, System.currentTimeMillis()));
    }

    @Test
    @DisplayName("사용자 포인트 조회 - 실패")
    void get_user_point_fail() {
        // given
        long userId = 5L;
        when(userPointRepository.selectById(userId)).thenReturn(null);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> pointService.getUserPoint(userId));
    }

    @Test
    @DisplayName("사용자 포인트 조회 - 성공")
    void get_user_point_success() {
        // given
        long userId = 1L;
        when(userPointRepository.selectById(userId)).thenReturn(testUserPoint);

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertEquals(testUserPoint, result);
        verify(userPointRepository, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회 - 실패: 빈 리스트인 경우")
    void get_user_point_histories_fail_empty_list() {
        // given
        long userId = 2L;
        when(pointHistoryRepository.searchAllUserPointHistories(userId)).thenReturn(new ArrayList<>());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> pointService.getUserPointHistories(userId));
        verify(pointHistoryRepository, times(1)).searchAllUserPointHistories(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회 - 성공")
    void get_user_point_histories_success() {
        // given
        long userId = 1L;
        long expectedPoint = 10000L;

        when(pointHistoryRepository.searchAllUserPointHistories(userId)).thenReturn(pointHistoryList);

        // when
        List<PointHistory> result = pointService.getUserPointHistories(userId);

        // then
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals(expectedPoint, result.stream().mapToLong(PointHistory::amount).sum());
        assertEquals(pointHistoryList, result);
        verify(pointHistoryRepository, times(1)).searchAllUserPointHistories(userId);
    }

    @Test
    @DisplayName("포인트 충전 - 실패: 10000원 초과 충전 (비즈니스 정책)")
    void charge_user_point_fail_over_10000_policy() {
        // given
        long userId = 1L;
        long chargeAmount = 10010L; // 10000원 초과

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargeUserPoint(userId, chargeAmount)
        );

        assertEquals("포인트 충전은 10000원 까지 가능합니다.", exception.getMessage());

        verify(userPointRepository, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("포인트 충전 - 실패: 사용자를 찾을 수 없음")
    void charge_user_point_fail_user_not_found() {
        // given
        long userId = 999L;
        long chargeAmount = 1000L;

        when(userPointRepository.selectById(userId)).thenReturn(null);

        // when & then
        assertThrows(NullPointerException.class, () -> pointService.chargeUserPoint(userId, chargeAmount));
        verify(userPointRepository, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 충전 - 실패: 0원 충전")
    void charge_user_point_fail_zero_domain_rule() {
        // given
        long userId = 1L;
        long chargeAmount = 0L;

        when(userPointRepository.selectById(userId)).thenReturn(testUserPoint);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargeUserPoint(userId, chargeAmount)
        );

        assertEquals("포인트 충전 금액은 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 - 실패: 10원 단위가 아닌 금액(정책)")
    void charge_user_point_fail_not_multiple_of_10_domain_rule() {
        // given
        long userId = 1L;
        long chargeAmount = 155L;

        when(userPointRepository.selectById(userId)).thenReturn(testUserPoint);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargeUserPoint(userId, chargeAmount)
        );

        assertEquals("포인트 충전은 10원 단위로 가능합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 - 실패: 총 포인트 50000원 초과 (정책)")
    void charge_user_point_fail_exceed_total_limit_domain_rule() {
        // given
        long userId = 1L;
        long chargeAmount = 10000L;
        long currentPoint = 45000L;

        UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(currentUserPoint);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.chargeUserPoint(userId, chargeAmount)
        );

        assertEquals("보유 포인트는 50000원을 초과할 수 없습니다.", exception.getMessage());
        verify(userPointRepository, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 충전 - 성공: 정상 충전")
    void charge_user_point_success() {
        // given
        long userId = 1L;
        long chargeAmount = 500L;
        long expectedNewPoint = 1500L;

        UserPoint expectedUserPoint = new UserPoint(userId, expectedNewPoint, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(testUserPoint);
        when(userPointRepository.insertOrUpdate(userId, expectedNewPoint)).thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);

        // then
        assertNotNull(result);
        assertEquals(expectedNewPoint, result.point());
        assertEquals(userId, result.id());

        verify(userPointRepository, times(1)).selectById(userId);
        verify(pointHistoryRepository, times(1)).insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        );

        verify(userPointRepository, times(1)).insertOrUpdate(userId, expectedNewPoint);
    }

    @Test
    @DisplayName("포인트 충전 - 성공: 최대 금액(10000원) 충전")
    void charge_user_point_success_maximum_amount() {
        // given
        long userId = 1L;
        long chargeAmount = 10000L;
        long currentPoint = 5000L;
        long expectedNewPoint = 15000L;

        UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        UserPoint expectedUserPoint = new UserPoint(userId, expectedNewPoint, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(currentUserPoint);
        when(userPointRepository.insertOrUpdate(userId, expectedNewPoint)).thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);

        // then
        assertNotNull(result);
        assertEquals(expectedNewPoint, result.point());

        verify(userPointRepository, times(1)).selectById(userId);
        verify(pointHistoryRepository, times(1)).insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        );
        verify(userPointRepository, times(1)).insertOrUpdate(userId, expectedNewPoint);
    }

    @Test
    @DisplayName("포인트 충전 - 성공: 최소 금액(10원) 충전")
    void charge_user_point_success_minimum_amount() {
        // given
        long userId = 1L;
        long chargeAmount = 10L; // 최소 충전 금액
        long currentPoint = 0L;
        long expectedNewPoint = 10L;

        UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        UserPoint expectedUserPoint = new UserPoint(userId, expectedNewPoint, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(currentUserPoint);
        when(userPointRepository.insertOrUpdate(userId, expectedNewPoint)).thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);

        // then
        assertNotNull(result);
        assertEquals(expectedNewPoint, result.point());
        assertEquals(userId, result.id());

        verify(userPointRepository, times(1)).selectById(userId);
        verify(pointHistoryRepository, times(1)).insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        );

        verify(userPointRepository, times(1)).insertOrUpdate(userId, expectedNewPoint);
    }

    @Test
    @DisplayName("포인트 사용 - 실패: 사용자를 찾을 수 없음")
    void use_user_point_fail_user_not_found() {
        // given
        long userId = 999L;
        long useAmount = 100L;

        when(userPointRepository.selectById(userId)).thenReturn(null);

        // when & then
        assertThrows(NullPointerException.class, () -> pointService.useUserPoint(userId, useAmount));
        verify(userPointRepository, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 사용 - 실패: 0원 사용")
    void use_user_point_fail_zero_amount() {
        // given
        long userId = 1L;
        long useAmount = 0L;

        when(userPointRepository.selectById(userId)).thenReturn(testUserPoint);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.useUserPoint(userId, useAmount)
        );

        assertEquals("포인트 사용 금액은 0보다 커야 합니다.", exception.getMessage());
        verify(userPointRepository, times(1)).selectById(userId);
        verify(pointHistoryRepository, never()).insert(anyLong(), anyLong(), any(), anyLong());
        verify(userPointRepository, never()).insertOrUpdate(anyLong(), anyLong());
    }

    @Test
    @DisplayName("포인트 사용 - 실패: 보유 포인트 초과 사용")
    void use_user_point_fail_exceed_current_point() {
        // given
        long userId = 1L;
        long currentPoint = 1000L;
        long useAmount = 2000L;

        UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(currentUserPoint);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.useUserPoint(userId, useAmount)
        );

        // 메시지는 현재 구현의 문자열 포맷을 그대로 검증
        assertEquals("포인트 사용 금액은 현재 포인트를 초과할 수 없습니다. 포인트: {}", exception.getMessage());
        verify(userPointRepository, times(1)).selectById(userId);
        verify(pointHistoryRepository, never()).insert(anyLong(), anyLong(), any(), anyLong());
        verify(userPointRepository, never()).insertOrUpdate(anyLong(), anyLong());
    }

    @Test
    @DisplayName("포인트 사용 - 성공: 정상 사용")
    void use_user_point_success() {
        // given
        long userId = 1L;
        long useAmount = 500L;
        long expectedNewPoint = 500L;

        UserPoint expectedUserPoint = new UserPoint(userId, expectedNewPoint, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(testUserPoint);
        when(userPointRepository.insertOrUpdate(userId, expectedNewPoint)).thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.useUserPoint(userId, useAmount);

        // then
        assertNotNull(result);
        assertEquals(expectedNewPoint, result.point());
        assertEquals(userId, result.id());

        verify(userPointRepository, times(1)).selectById(userId);
        verify(pointHistoryRepository, times(1)).insert(
                eq(userId),
                eq(useAmount),
                eq(TransactionType.USE),
                anyLong()
        );
        verify(userPointRepository, times(1)).insertOrUpdate(userId, expectedNewPoint);
    }
}
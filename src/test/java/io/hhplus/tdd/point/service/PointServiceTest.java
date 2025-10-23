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
        long userId = 5L;
        when(userPointRepository.selectById(userId)).thenReturn(null);

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

}
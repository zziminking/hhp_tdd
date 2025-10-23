package io.hhplus.tdd.point.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserPointRepository userPointRepository;

    @InjectMocks
    private PointService pointService;

    UserPoint testUserPoint;

    @BeforeEach
    void setUp() {
        testUserPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());
    }

    @Test
    @DisplayName("사용자 포인트 조회 - 실패")
    void get_user_point_fail() {
        long userId = 2L;
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

}
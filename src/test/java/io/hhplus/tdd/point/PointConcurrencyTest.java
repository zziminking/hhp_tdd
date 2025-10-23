package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.assertThat;

import io.hhplus.tdd.point.domain.repository.UserPointRepository;
import io.hhplus.tdd.point.service.PointService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PointConcurrencyTest {

    @Autowired
    private PointService pointservice;

    @Autowired
    private UserPointRepository userPointRepository;

    @Test
    @DisplayName("같은 사용자에 대한 동시 충전 동시성 테스트")
    void charge_concurrent_test_same_user() throws InterruptedException {
        // Given
        long userId = 1L;
        long chargeAmount = 1000;
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 실제 Service 메서드 호출
                    pointservice.chargeUserPoint(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 실제 DB에서 조회하여 검증
        UserPoint userPoint = userPointRepository.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("여러 사용자에 대한 충전 동시성 테스트")
    void charge_concurrent_test_each_user() throws InterruptedException {
        // Given
        int threadCount = 100;
        int chargeAmount = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When: 서로 다른 user 1~100에 대해 각각 충전
        for (int i = 1; i <= threadCount; i++) {
            final Long userId = (long) i;
            executorService.submit(() -> {
                try {
                    pointservice.chargeUserPoint(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        for (int i = 1; i <= threadCount; i++) {
            UserPoint point = pointservice.getUserPoint((long) i);
            assertThat(point.point()).isEqualTo(chargeAmount);
        }
    }
}

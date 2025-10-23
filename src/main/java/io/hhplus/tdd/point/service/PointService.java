package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.point.domain.repository.UserPointRepository;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserPointRepository userPointRepository;
    private final ConcurrentHashMap<Long, ReentrantLock> userLock = new ConcurrentHashMap<>();

    /**
     * 포인트 조회
     */
    public UserPoint getUserPoint(long id) {
        UserPoint userPoint = userPointRepository.selectById(id);

        if (userPoint == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        return userPoint;
    }

    /**
     * 포인트 내역 조회
     */
    public List<PointHistory> getUserPointHistories(long id) {
        List<PointHistory> pointHistories = pointHistoryRepository.searchAllUserPointHistories(id);

        if (pointHistories.isEmpty()) {
            throw new IllegalArgumentException("포인트 내역을 조회할 수 없습니다.");
        }

        return pointHistories;
    }

    /**
     * 포인트 충전
     */
    public UserPoint chargeUserPoint(long id, long amount) {
        // 정책: 포인트 충전은 한번에 10000원까지 가능
        if (amount > 10000) {
            throw new IllegalArgumentException("포인트 충전은 10000원 까지 가능합니다.");
        }

        ReentrantLock reentrantLock = userLock.computeIfAbsent(id, k -> new ReentrantLock());
        reentrantLock.lock();

        try {
            UserPoint currentPoint = userPointRepository.selectById(id);

            long totalPoint = currentPoint.calculateChargePoint(amount);

            // 히스토리 기록
            pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            // 포인트 업데이트
            return userPointRepository.insertOrUpdate(id, totalPoint);
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * 포인트 사용
     */
    public UserPoint useUserPoint(long id, long amount) {
        ReentrantLock reentrantLock = userLock.computeIfAbsent(id, k -> new ReentrantLock());
        reentrantLock.lock();

        try {
            UserPoint currentPoint = userPointRepository.selectById(id);

            long usePoint = currentPoint.calculateUsePoint(amount);

            // 히스토리 기록
            pointHistoryRepository.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

            // 포인트 업데이트
            return userPointRepository.insertOrUpdate(id, usePoint);
        } finally {
            reentrantLock.unlock();
        }
    }
}

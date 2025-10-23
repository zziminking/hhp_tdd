package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.point.domain.repository.UserPointRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserPointRepository userPointRepository;

    public UserPoint getUserPoint(long id) {
        UserPoint userPoint = userPointRepository.selectById(id);

        if (userPoint == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        return userPoint;
    }

    public List<PointHistory> getUserPointHistories(long id) {
        List<PointHistory> pointHistories = pointHistoryRepository.searchAllUserPointHistories(id);

        if (pointHistories.isEmpty()) {
            throw new IllegalArgumentException("포인트 내역을 조회할 수 없습니다.");
        }

        return pointHistories;
    }
}

package io.hhplus.tdd.point.domain.repository;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.domain.database.PointHistoryTable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepository {

    private final PointHistoryTable pointHistoryTable;

    public List<PointHistory> searchAllUserPointHistories(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    public PointHistory insert(long userId, long amount, TransactionType type, long updateMillis) {
        return pointHistoryTable.insert(userId, amount, type, updateMillis);
    }
}

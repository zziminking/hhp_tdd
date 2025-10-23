package io.hhplus.tdd.point.domain.repository;

import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.domain.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPointRepository {

    private final UserPointTable userPointTable;

    public UserPoint selectById(long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint insertOrUpdate(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }
}

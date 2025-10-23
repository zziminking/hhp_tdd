# 동시성 이슈 처리 방안

---

## 동시성 문제 개요

### 동시성 발생 원인
```
여러 쓰레드가 공유 자원에 동시에 접근하려 할 때 발생
```
---

### 발생 가능한 동시성 이슈

포인트 시스템에서 **같은 사용자**에 대해 여러 요청이 동시에 처리될 때 발생하는 문제:

```
초기 상태: userId=1, point=1000

[Thread 1] 1000원 충전 시작
  1. 현재 포인트 조회: 1000
  2. 새 포인트 계산: 1000 + 1000 = 2000

[Thread 2] 1000원 충전 시작 (동시에 실행)
  1. 현재 포인트 조회: 1000  ← Thread 1이 아직 저장 전
  2. 새 포인트 계산: 1000 + 1000 = 2000

[Thread 1] DB 저장: 2000
[Thread 2] DB 저장: 2000  ← 2번째 충전이 사라짐!

최종 결과: 2000 (예상: 3000)
```

---

## 현재 구현 방식

### ReentrantLock을 이용하여 user 레벨로 lock

```java
@Service
@RequiredArgsConstructor
public class PointService {

    // 사용자별 Lock 관리
    private final ConcurrentHashMap<Long, ReentrantLock> userLock = new ConcurrentHashMap<>();

    public UserPoint chargeUserPoint(long id, long amount) {
        // 1. 사용자별 Lock 획득 (없으면 생성)
        ReentrantLock reentrantLock = userLock.computeIfAbsent(id, k -> new ReentrantLock());

        // 2. Lock 획득 (다른 스레드는 대기)
        reentrantLock.lock();

        try {
            // 3. 임계 영역 (Critical Section)
            UserPoint currentPoint = userPointRepository.selectById(id);
            long totalPoint = currentPoint.calculateChargePoint(amount);
            pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
            return userPointRepository.insertOrUpdate(id, totalPoint);
        } finally {
            // 4. Lock 해제 (반드시 실행)
            reentrantLock.unlock();
        }
    }

    public UserPoint useUserPoint(long id, long amount) {
        // 사용도 동일한 방식으로 Lock 적용
        ReentrantLock reentrantLock = userLock.computeIfAbsent(id, k -> new ReentrantLock());
        reentrantLock.lock();
        try {
            UserPoint currentPoint = userPointRepository.selectById(id);
            long usePoint = currentPoint.calculateUsePoint(amount);
            pointHistoryRepository.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
            return userPointRepository.insertOrUpdate(id, usePoint);
        } finally {
            reentrantLock.unlock();
        }
    }
}
```

---

#### ⚠️ 동작 방식

```
Time ─────────────────────────────────────────────────>

Thread 1 [userId=1]: ████ Lock ████████ Work ████████ Unlock ████
Thread 2 [userId=1]:      [Wait................] ████ Work ████ Unlock
Thread 3 [userId=2]: ████ Lock ████ Work ████ Unlock ████  (독립적)
```

- **Thread 1**이 userId=1의 Lock을 획득하면
- **Thread 2**는 같은 userId=1의 Lock을 기다림
- **Thread 3**는 다른 userId=2를 처리하므로 독립적으로 실행

---

#### ReenTrantLock
   - 사용자별 독립적인 Lock
   - userId=1과 userId=2는 서로 영향 없음

---

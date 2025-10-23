package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.domain.repository.UserPointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("포인트 API 통합 테스트")
public class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserPointRepository userPointRepository;

    /**
     * 포인트 조회 API 테스트
     */
    @Test
    @DisplayName("포인트 조회 API - 성공: 초기 사용자 조회")
    void getPoint_success_initial_user() throws Exception {
        // Given
        long userId = 100L;

        // When & Then: 초기 사용자는 0 포인트
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("포인트 조회 API - 성공: 충전 후 조회")
    void getPoint_success_after_charge() throws Exception {
        // Given: 먼저 포인트를 충전
        long userId = 101L;
        long chargeAmount = 5000L;

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // When & Then: 포인트 조회
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    /**
     * 포인트 내역 조회 API 테스트
     */
    @Test
    @DisplayName("포인트 내역 조회 API - 성공: 충전 내역 조회")
    void getPointHistories_success_after_charge() throws Exception {
        // Given: 포인트 충전
        long userId = 102L;
        long chargeAmount = 3000L;

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // When & Then: 내역 조회
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(chargeAmount))
                .andExpect(jsonPath("$[0].type").value("CHARGE"));
    }

    @Test
    @DisplayName("포인트 내역 조회 API - 성공: 충전 + 사용 내역 조회")
    void getPointHistories_success_charge_and_use() throws Exception {
        // Given: 포인트 충전 후 사용
        long userId = 103L;
        long chargeAmount = 5000L;
        long useAmount = 2000L;

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk());

        // When & Then: 내역 조회
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].amount").value(chargeAmount))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andExpect(jsonPath("$[1].amount").value(useAmount));
    }

    /**
     * 포인트 충전 API 테스트
     */
    @Test
    @DisplayName("포인트 충전 API - 성공: 정상 충전")
    void chargePoint_success() throws Exception {
        // Given
        long userId = 104L;
        long chargeAmount = 1000L;

        // When & Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));

        // 실제 DB에서도 확인
        UserPoint result = userPointRepository.selectById(userId);
        assertThat(result.point()).isEqualTo(chargeAmount);
    }

    @Test
    @DisplayName("포인트 충전 API - 성공: 여러 번 충전")
    void chargePoint_success_multiple_times() throws Exception {
        // Given
        long userId = 105L;
        long firstCharge = 1000L;
        long secondCharge = 2000L;
        long expectedTotal = firstCharge + secondCharge;

        // When: 첫 번째 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstCharge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(firstCharge));

        // When: 두 번째 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(expectedTotal));

        // Then: 실제 DB 확인
        UserPoint result = userPointRepository.selectById(userId);
        assertThat(result.point()).isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("포인트 충전 API - 성공: 최소 금액(10원) 충전")
    void chargePoint_success_minimum_amount() throws Exception {
        // Given
        long userId = 106L;
        long chargeAmount = 10L;

        // When & Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    @Test
    @DisplayName("포인트 충전 API - 성공: 최대 금액(10000원) 충전")
    void chargePoint_success_maximum_amount() throws Exception {
        // Given
        long userId = 107L;
        long chargeAmount = 10000L;

        // When & Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    @Test
    @DisplayName("포인트 충전 API - 실패: 10000원 초과")
    void chargePoint_fail_exceed_10000() throws Exception {
        // Given
        long userId = 108L;
        long invalidAmount = 10001L;

        // When & Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("포인트 충전 API - 실패: 0원 충전")
    void chargePoint_fail_zero_amount() throws Exception {
        // Given
        long userId = 109L;
        long invalidAmount = 0L;

        // When & Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("포인트 충전 API - 실패: 음수 충전")
    void chargePoint_fail_negative_amount() throws Exception {
        // Given
        long userId = 110L;
        long invalidAmount = -1000L;

        // When & Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("포인트 충전 API - 실패: 10원 단위가 아님")
    void chargePoint_fail_not_multiple_of_10() throws Exception {
        // Given
        long userId = 111L;
        long invalidAmount = 1005L;

        // When & Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("포인트 충전 API - 실패: 총 포인트 50000원 초과")
    void chargePoint_fail_exceed_total_limit() throws Exception {
        // Given: 먼저 40000원 충전
        long userId = 112L;
        long firstCharge = 10000L;

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(patch("/point/{id}/charge", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(firstCharge)))
                    .andExpect(status().isOk());
        }

        // When & Then: 추가로 10001원 충전 시도 (총 50001원이 되므로 실패)
        long secondCharge = 10001L;
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isInternalServerError());
    }

    /**
     * 포인트 사용 API 테스트
     */
    @Test
    @DisplayName("포인트 사용 API - 성공: 정상 사용")
    void usePoint_success() throws Exception {
        // Given: 먼저 포인트 충전
        long userId = 113L;
        long chargeAmount = 5000L;
        long useAmount = 2000L;
        long expectedBalance = chargeAmount - useAmount;

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // When & Then: 포인트 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedBalance));

        // 실제 DB 확인
        UserPoint result = userPointRepository.selectById(userId);
        assertThat(result.point()).isEqualTo(expectedBalance);
    }

    @Test
    @DisplayName("포인트 사용 API - 성공: 전액 사용")
    void usePoint_success_use_all() throws Exception {
        // Given: 포인트 충전
        long userId = 114L;
        long chargeAmount = 3000L;

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // When & Then: 전액 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("포인트 사용 API - 실패: 잔액 부족")
    void usePoint_fail_insufficient_balance() throws Exception {
        // Given: 1000원 충전
        long userId = 115L;
        long chargeAmount = 1000L;
        long useAmount = 1001L;

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // When & Then: 1001원 사용 시도 (실패)
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("포인트 사용 API - 실패: 0원 사용")
    void usePoint_fail_zero_amount() throws Exception {
        // Given
        long userId = 116L;
        userPointRepository.insertOrUpdate(userId, 1000L);

        // When & Then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(0L)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("포인트 사용 API - 실패: 음수 사용")
    void usePoint_fail_negative_amount() throws Exception {
        // Given
        long userId = 117L;
        userPointRepository.insertOrUpdate(userId, 1000L);

        // When & Then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(-100L)))
                .andExpect(status().isInternalServerError());
    }

    /**
     * 시나리오 기반 통합 테스트
     */
    @Test
    @DisplayName("시나리오: 충전 → 사용 → 조회 → 내역 조회")
    void scenario_full_flow() throws Exception {
        // Given
        long userId = 200L;
        long chargeAmount = 10000L;
        long useAmount = 3000L;
        long expectedBalance = chargeAmount - useAmount;

        // 1. 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(chargeAmount));

        // 2. 포인트 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(expectedBalance));

        // 3. 포인트 조회
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedBalance));

        // 4. 내역 조회
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].amount").value(chargeAmount))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andExpect(jsonPath("$[1].amount").value(useAmount));
    }

    @Test
    @DisplayName("시나리오: 여러 번 충전 후 여러 번 사용")
    void scenario_multiple_charges_and_uses() throws Exception {
        // Given
        long userId = 201L;

        // 1. 첫 번째 충전 (5000원)
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(5000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(5000L));

        // 2. 두 번째 충전 (3000원)
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(3000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(8000L));

        // 3. 첫 번째 사용 (2000원)
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(2000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(6000L));

        // 4. 두 번째 사용 (1000원)
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(1000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(5000L));

        // 5. 내역 조회 (총 4건)
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        // 6. 최종 잔액 확인
        UserPoint result = userPointRepository.selectById(userId);
        assertThat(result.point()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("시나리오: 최대 한도(50000원)까지 충전 후 전액 사용")
    void scenario_max_charge_and_full_use() throws Exception {
        // Given
        long userId = 202L;

        // 1. 5회 충전으로 50000원 달성 (10000원 × 5)
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(patch("/point/{id}/charge", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(10000L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(10000L * i));
        }

        // 2. 전액 사용 (50000원)
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(50000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(0L));

        // 3. 최종 확인
        UserPoint result = userPointRepository.selectById(userId);
        assertThat(result.point()).isEqualTo(0L);

        // 4. 내역 확인 (충전 5건 + 사용 1건 = 총 6건)
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("시나리오: 충전 제한 검증 - 50000원 도달 후 추가 충전 실패")
    void scenario_charge_limit_validation() throws Exception {
        // Given
        long userId = 203L;

        // 1. 50000원까지 충전 (10000원 × 5)
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(patch("/point/{id}/charge", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(10000L)))
                    .andExpect(status().isOk());
        }

        // 2. 추가 충전 시도 (실패해야 함)
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(10L)))
                .andExpect(status().isInternalServerError());

        // 3. 잔액은 여전히 50000원
        UserPoint result = userPointRepository.selectById(userId);
        assertThat(result.point()).isEqualTo(50000L);
    }
}

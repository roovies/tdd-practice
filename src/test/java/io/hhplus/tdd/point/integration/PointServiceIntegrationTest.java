package io.hhplus.tdd.point.integration;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Test
    @DisplayName("멀티쓰레드 환경에서 동시에 포인트를 충전하면 합계가 정상적으로 증가해야 한다.")
    void givenMultiThread_whenChargePointConcurrently_thenTotalPointShouldIncreaseCorrectly() throws InterruptedException {
        /** given */
        int threadCount = 500; // 동시에 실행할 스레드 개수
        long userId = 1L;

        // 1. Thread Pool 생성 (최대 threadCount 개수만큼 스레드 동시 실행)
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 2.시작 신호 역할 (초기값1에서 0이되는 순간 모든 스레드 동시 시작)
        CountDownLatch startLatch = new CountDownLatch(1);

        // 3. 종료 대기 (threadCount가 0이 될 때까지 메인 스레드 대기)
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        /** when */
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> { // Runnable을 스레드 풀의 작업 큐에 등록
                try {
                    startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                    pointService.charge(userId, 100L); // 동시에 100포인트 충전
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown(); // 해당 스레드 작업 완료 표시
                }
            });
        }

        startLatch.countDown(); // 준비된 모든 스레드 동시에 시작
        doneLatch.await();      // 모든 스레드 작업 끝날 때까지 대기
        executor.shutdown();    //스레드 풀 자원 해제

        /** then */
        UserPoint result = pointService.findUserPointByUserId(userId);

        long expected = 100L * threadCount;
        System.out.println("최종 포인트: " + result.point());

        assertThat(result.point())
                .isEqualTo(expected);

    }
}

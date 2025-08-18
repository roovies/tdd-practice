package io.hhplus.tdd.point.unit;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.service.impl.PointServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointServiceImpl pointService;

    /**
     * 포인트 조회 기능
     * - 유효한 회원의 포인트만 조회할 수 있다. (userId > 0)
     */
    @Nested
    @DisplayName("포인트 조회 테스트")
    public class GetPointTest {
        /*
            @Nested로 구조를 짠 이유:
            - PointServiceTest 내에 여러 기능들의 테스트 코드를 적을 경우 어떤 메서드가 어떤 테스트를 위한 것인지 헷갈릴 수 있어 Nested를 통해
              중첩 클래스로 테스트 코드를 작성하도록 하였다.
         */
        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        @DisplayName("잘못된 회원ID로 포인트 조회 시 예외가 발생한다.")
        void givenZeroOrNegativeUserId_whenGetPoints_thenThrowException(long userId) {
            // given: userId

            // when & then
            assertThatThrownBy(() -> pointService.findUserPointByUserId(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("잘못된 회원ID입니다.");
            /*
                1. @ParameterizedTest를 사용한 이유:
                    - 0 또는 음수 두 가지 케이스는 동일한 검증을 수행하기 때문에 given에 값을 하드코딩하여 준비하기 보다는,
                      파라미터화를 통해 하나의 코드로 여러 케이스를 검증할 수 있도록 하기 위함
                2. 일반적으로 userId는 0 또는 음수값을 갖는 경우는 없기 때문에 잘못된 입력값으로 간주하여 처리하도록 함
                3. assertThatThrownBy()를 사용한 이유:
                    - 예외 타입과 예외 메시지를 동시에 검증할 수 있으므로 사용함
             */
        }

        @Test
        @DisplayName("유효한 회원ID로 포인트 조회 시 올바른 정보를 반환해야 한다.")
        void givenValidUserId_whenGetPoints_thenResultIsNotNull() {
            // given
            long userId = 1L;
            long expectedPoint = 1000L;
            UserPoint expectedUserPoint = new UserPoint(userId, expectedPoint, 12345L);
            when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint); // stub

            // when
            UserPoint result = pointService.findUserPointByUserId(userId);

            //then
            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.point()).isEqualTo(expectedPoint);
            /*
                1. stub 처리를 수행한 이유
                    - 회원 포인트를 조회하기 위해서는 infra layer에 있는 기능을 써야 하므로, Mock 객체로 만들어둔 userPointTable이
                      기대하고 있는 expectedUserPoint를 반환하도록 stub 처리를 했음
             */
        }

        /**
         Question 1)
         - 현재 위 테스트 코드는 "유효한 회원ID로 포인트 조회 시 올바른 정보를 반환해야 한다."는 검증에서 내부적으로 userPointTable.selectById(userId)를 사용하여
         회원정보를 조회해온다고 가정하고 있습니다. (stub 객체 생성)
         - 이때 이렇게 결과 검증에 selectById()를 사용해야 한다는 것을 바로 명시하는 것이 아니라,
         사전에 미리 "유효한 회원ID로 조회할 경우 데이터베이스에서 회원정보를 조회해야 한다." 와 같은 행위 검증을 선수행 해야 할까요?
         - 즉, userPointTable.insertOrUpdate()를 결국 green 단계에서 반영해줘야 하는데,
         -> 이걸 해당 테스트 같은 곳에서 바로 사용해도 되는지,
         -> 아니면 사전에 미리 해당 userPointTable.insertOrUpdate() 메서드를 사용하도록
         "충전한 포인트는 데이터베이스 저장 메서드를 호출해야 한다."와 같은 테스트하는 코드를 작성을 해야 하는지 궁금합니다.
         */
    }

    /**
     * 포인트 충전 기능
     * 1. 잘못된 회원ID로 포인트 충전 시 예외가 발생한다.
     * 2. 유효한 회원ID로 포인트 충전 시 보유하고 있는 원래 포인트 정보를 조회해야 한다.
     * 3. 충전 금액은 1원 이상이어야 한다.
     * 4. 포인트 충전 단위는 100포인트 단위다.
     * 5. 보유 포인트가 없을 때 포인트 충전 시 충전한 금액이 최종 포인트 값이 되어야 한다.
     * 6. 보유 포인트가 있을 때 포인트 충전 시 최종 포인트 값은 두 값을 더한 금액이어야 한다.
     * 7. 최대 포인트 100만 포인트를 넘게 충전할 경우 예외가 발생한다.
     * 8. 포인트 충전에 성공하면 CHARGE 타입으로 히스토리가 기록되어야 한다.
     */
    @Nested
    @DisplayName("포인트 충전 테스트")
    public class ChargePointTest {

        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        @DisplayName("잘못된 회원ID로 포인트 충전 시 예외가 발생한다.")
        void givenZeroOrNegativeUserId_whenChargePoint_thenThrowException(long userId) {
            // given: userId
            long chargePoint = 1000L;

            // when & then
            assertThatThrownBy(() -> pointService.charge(userId, chargePoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("잘못된 회원ID입니다.");
            /*
                포인트 충전을 위해서는 기존 포인트 정보를 알아야 하므로, 조회가 필요하다.
                따라서 기존에 정의된 findUserPointByUserId()를 재사용하여, 예외 타입/메시지를 동일하게 했다.
             */
        }

        @Test
        @DisplayName("유효한 회원ID로 포인트 충전 시 기존 포인트 정보를 조회해야 한다.")
        void givenValidUserId_whenChargePoint_thenShouldRetrieveCurrentPoint() {
            // given
            long userId = 1L;
            when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 500L, 12345L)); // stub

            // when
            pointService.charge(userId, 1000L);

            // then
            verify(userPointTable, times(1)).selectById(userId); // 행위검증
            /*
                기존 포인트 정보를 조회하는지를 검증하기 위해 행위(호출) 검증만 수행했음
             */
        }

        @ParameterizedTest
        @ValueSource(longs = {-100L, -1L, 0L})
        @DisplayName("충전 금액이 1원 이상이 아닐 경우 에외가 발생한다.")
        void givenNegativeOrZeroAmount_whenChargePoint_thenThrowException(long chargePoint) {
            // given
            long userId = 1L;

            // when & then
            assertThatThrownBy(() -> pointService.charge(userId, chargePoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("올바른 충전 금액이 아닙니다.");
            /*
                충전 금액이 음수이거나, 0인 경우는 정상적인 경우가 아니므로 예외가 발생하도록 했다.
                예외 발생을 체크하기 위해 isInstanceOf로 예외 클래스 검증 및 hasMessage로 예외 메시지를 검증하여
                예외가 올바르게 발생했는지 상세 체크를 수행했다.
                입력된 값 자체를 검증하는 것이기 때문에, stub을 생성하지 않았다.
             */
        }

        @ParameterizedTest
        @ValueSource(longs = {101L, 110L})
        @DisplayName("충전 금액 단위가 100원 단위가 아닐 경우 예외가 발생한다.")
        void givenNotMultipleOf100Amount_whenChargePoint_thenThrowException(long chargePoint) {
            // given
            long userId = 1L;

            // when & then
            assertThatThrownBy(() -> pointService.charge(userId, chargePoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("포인트 충전 단위는 100원입니다.");
            /*
                충전 금액이 1원 이상이 아닐 경우 에외가 발생한다.와 동일하게 처리했다.
             */
        }

        @ParameterizedTest
        @ValueSource(longs = {1000L, 2000L, 3000L})
        @DisplayName("보유 포인트가 없을 때 포인트 충전 시 충전한 금액이 최종 포인트 값이 되어야 한다.")
        void givenZeroBalance_whenChargePoint_thenFinalPointsMatchChargedAmount(long chargePoint) {
            // given
            long userId = 1L;
            UserPoint existingUserPoint = new UserPoint(userId, 0L, 12345L);
            when(userPointTable.selectById(userId)).thenReturn(existingUserPoint); // stub

            UserPoint expectedUserPoint = new UserPoint(userId, chargePoint, 12345L);
            when(userPointTable.insertOrUpdate(userId, chargePoint)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointService.charge(userId, chargePoint);

            // then
            assertThat(result.point()).isEqualTo(chargePoint);
            /*
                보유 포인트가 0원인 stub 하나와, 충전금액을 포인트로 저장하고 있는 stub 하나를 선언했다.
                @ParameterizedTest를 통해 다양한 케이스를 하나의 코드로 테스트함
             */
        }

        @ParameterizedTest
        @ValueSource(longs = {1000L, 2000L, 3000L})
        @DisplayName("보유 포인트가 있을 때 포인트 충전 시 최종 포인트 값은 두 값을 더한 금액이어야 한다.")
        void givenExistingPoints_whenChargePoint_thenFinalPointsShouldBeSum(long chargePoint) {
            // given
            long userId = 1L;
            UserPoint existingUserPoint = new UserPoint(userId, 500L, 12345L);
            when(userPointTable.selectById(userId)).thenReturn(existingUserPoint);

            long expectedPoint = existingUserPoint.point() + chargePoint;
            UserPoint expectedUserPoint = new UserPoint(userId, expectedPoint, 12345L);
            when(userPointTable.insertOrUpdate(userId, expectedPoint)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointService.charge(userId, chargePoint);

            // then
            assertThat(result.point()).isEqualTo(expectedPoint);
            /*
                보유 포인트가 500원인 stub 하나와, 충전금액을 포인트로 저장하고 있는 stub 하나를 선언했다.
                @ParameterizedTest를 통해 다양한 케이스를 하나의 코드로 테스트함
             */
        }

        @Test
        @DisplayName("최대 포인트 100만 포인트를 넘게 충전할 경우 예외가 발생한다.")
        void givenPointOverMillion_whenChargePoint_thenThrowException() {
            // given
            long userId = 1L;
            long chargePoint = 999000L;
            UserPoint existingUserPoint = new UserPoint(userId, 1100L, 12345L);
            when(userPointTable.selectById(userId)).thenReturn(existingUserPoint);

            // when & then
            assertThatThrownBy(() -> pointService.charge(userId, chargePoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("최대 포인트는 100만 포인트여야 합니다.");
            /*
                충전한 후의 금액이 100만 포인트를 넘어가게 되면 충전되지 않도록 초기 포인트를 1100으로 선언한 후 (100원 단위 제약)
                충전 금액을 99만9천원으로 설정했다.
             */
        }

        @Test
        @DisplayName("포인트 충전에 성공하면 CHARGE 타입으로 히스토리가 기록되어야 한다.")
        void givenValidUserId_whenChargePoint_thenHistoryShouldBeRecordedAsCharge() {
            // given
            long userId = 1L;
            long chargePoint = 1000L;
            // 테스트 대상 메서드 실행 시 해당 반환값(storedUserPoint)를 사용하기 때문에 null이면 안 되는 객체이므로 stub 처리
            when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 0L, 12345L));

            // when
            pointService.charge(userId, chargePoint);

            // then
            verify(pointHistoryTable, times(1)).insert(eq(userId), eq(chargePoint), eq(TransactionType.CHARGE), anyLong());
            /*
                히스토리 적재는 Side Effect이므로, pointHistoryTable.insert() 호출의 결과값 보다 호출이 되었는지를 검증함
             */
        }
    }

    /** 포인트 사용 기능
     * 1. 포인트는 1원 이상 사용해야 한다.
     * 2. 사용할 포인트가 없을 경우 예외가 발생해야 한다.
     * 3. 사용할 포인트가 보유한 포인트보다 클 경우 예외가 발생해야 한다.
     * 4. 포인트 사용 시 보유 포인트에서 정상적으로 차감되어야 한다.
     */
    @Nested
    @DisplayName("포인트 사용 테스트")
    public class UsePointTest {

        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        @DisplayName("포인트는 1원 이상 사용해야 한다.")
        void givenPointLessThanOne_whenUsePoint_thenThrowException(long usingPoint) {
            // given
            long userId = 1L;
            // when & then
            assertThatThrownBy(() -> pointService.usePoint(userId, usingPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("포인트는 1원 이상 사용해야 합니다.");
            /*
                경계값 테스트 (-1, 0 등 1보다 작은 값들)를 효율적으로 테스트하기 위해 @ParameterizedTest를 사용했다.
             */
        }

        @Test
        @DisplayName("사용할 포인트가 없을 경우 예외가 발생해야 한다.")
        void givenNoPointAvailable_whenUsePoint_thenThrowException() {
            // given
            long userId = 1L;
            long usingPoint = 100L;
            long currentPoint = 0L;
            UserPoint storedUserPoint = new UserPoint(userId, currentPoint, 12345L);
            when(userPointTable.selectById(userId)).thenReturn(storedUserPoint);

            // when
            assertThatThrownBy(() -> pointService.usePoint(userId, usingPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("사용할 포인트가 없습니다.");

            /*
                보유 포인트를 0원으로 하여 stub으로 만든 후, 사용할 포인트를 100원으로 하여 테스트에 실패하도록 작성하였다.
                예외를 검증하기 위해 assertThatThrownBy()를 사용하여 예외타입 및 메시지까지 검증했다.
             */
        }

        @Test
        @DisplayName("사용할 포인트가 보유한 포인트보다 클 경우 예외가 발생해야 한다.")
        void givenPointMoreThanExistingPoint_whenUsePoint_thenThrowException() {
            // given
            long userId = 1L;
            long currentPoint = 100L;
            long usingPoint = 200L;
            UserPoint storedUserPoint = new UserPoint(userId, currentPoint, 12345L);
            when(userPointTable.selectById(userId)).thenReturn(storedUserPoint);

            assertThatThrownBy(() -> pointService.usePoint(userId, usingPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("포인트가 부족합니다.");
            /*
                보유 포인트가 존재하지만, 사용할 포인트가 그거보다 많을 경우 예외가 발생하도록 했다.
                이를 위해 사용할 포인트보다 적은 포인트를 가지고 있는 stub을 생성했다.
             */
        }

        @ParameterizedTest
        @ValueSource(longs = {100L, 500L, 1000L})
        @DisplayName("포인트 사용 시 최종 포인트 값은 보유 포인트에서 정상적으로 차감된 값이어야 한다.")
        void givenSufficientPoint_whenUsePoint_thenPointShouldBeDeducted(long usingPoint) {
            // given
            long userId = 1L;
            long currentPoint = 1000L;
            long expectedPoint = currentPoint - usingPoint;
            UserPoint storedUserPoint = new UserPoint(userId, currentPoint, 12345L);
            when(userPointTable.selectById(userId)).thenReturn(storedUserPoint);

            UserPoint expectedUserPoint = new UserPoint(userId, expectedPoint, 12345L);
            when(userPointTable.insertOrUpdate(userId, expectedPoint)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointService.usePoint(userId, usingPoint);

            // then
            assertThat(result.point()).isEqualTo(expectedPoint);
            /*
                실제 포인트를 사용하면, 사용한 금액만큼 차감하여 새로 반영해줘야 한다.
                이때 insertOrUpdate()를 활용하였고, insertOrUpdate()는 반환값으로 UserPoint를 반환하므로,
                보유 포인트에서 사용한 포인트를 차감한 기대 결과값(expectedPoint)을 stub으로 생성하여,
                실제 반환값이랑 일치하는지 검증하였다.
             */
        }

        @Test
        @DisplayName("포인트 사용에 성공하면 USE 타입으로 히스토리가 기록되어야 한다.")
        void givenSufficientPoint_whenUsePoint_thenHistoryShouldBeRecordedAsCharge() {
            // given
            long userId = 1L;
            long currentPoint = 1000L;
            long usingPoint = 500L;

            // 테스트 대상 메서드 실행 시 해당 반환값(storedUserPoint)를 사용하기 때문에 null이면 안 되는 객체이므로 stub 처리
            UserPoint storedUserPoint = new UserPoint(userId, currentPoint, 12345L);
            when(userPointTable.selectById(userId)).thenReturn(storedUserPoint);

            // when
            pointService.usePoint(userId, usingPoint);

            // then
            verify(pointHistoryTable, times(1)).insert(eq(userId), eq(usingPoint), eq(TransactionType.USE), anyLong());
            /*
                지금까지 작성한 테스트 코드에 의해 비즈니스 로직에 selectById(userId)를 호출한 후, 그 결과를 사용하는 로직이 존재한다.
                따라서 mock 객체로 만들고 stub을 설정하지 않을 경우 null을 반환하여 NPE가 발생한다.
                이를 방지하기 위해 selectById(userId)의 결과를 stub으로 생성하여 반환하도록 했다.

                그리고 실제 히스토리의 결과값을 검증하기 어려우므로, 제대로 인자에 맞게 호출되었는지 검증하기 위해 verify() 사용 및 matcher를 사용했다.
             */
        }
    }

    /** 특정 유저의 포인트 충전/이용 내역 조회
     *  1. 포인트 충전/이용 내역이 없으면 빈 리스트를 반환해야 한다.
     *  2. 포인트 충전/이용 내역이 있으면 비어있지 않은 리스트를 반환해야 한다.
     *  3. 포인트 충전/이용 내역은 시간순으로 저장되어야 한다.
     */
    @Nested
    @DisplayName("포인트 충전/이용 내역 조회 테스트")
    public class GetPointHistoryTest {

        /**
         Question 2)
            - Question1과 유사한 질문을 실제 코드로 구현해봤습니다.
            - 여태까지 포인트 충전/사용/조회 테스트의 경우 바로 기능이 동작하기 위한 규칙을 테스트 코드로 정의했습니다.
            - 그러나, 저렇게 바로 규칙안에 mock과 stub을 바로 정의해서 테스트 코드를 짜야 하는지

            - 아니면 아래처럼 다음 단계별로 테스트 코드를 작성해야 하는 건지 궁금합니다.
                1. 포인트 충전/이용 내역이 없으면 빈 리스트를 반환해야 한다.
                    (순수 TDD - Mock 없이 시작)
                    => Green에서 단순히 하드코딩으로 빈 리스트 반환하기
                2. 포인트 충전/이용 내역이 있으면 비어있지 않은 리스트를 반환해야 한다.
                    => 인자값으로 이용 내역의 존재 유무를 판단할 수 없으므로 외부 API를 통해 가져와야겠다고 판단될 것임
                        (외부 의존성이 필요한 순간 Mock 도입)
                    => 따라서 Green에서 작성한 1번의 하드코딩을 수정해서 pointHistoryTable를 이용하도록 테스트 코드를 작성함

            테스트 조건을 얼마나 가장 작은 단위로 어떻게 구분하고, Green에서는 얼마나 최소한의 기능으로 구현해야 할지 조금 헷갈립니다.
            실무에서는 [하드코딩 → Mock 전환 → 리팩토링] 과정을 거치면 유지보수/작성 속도가 떨어지기 때문에
            외부 호출이 필요한 순간이 명확하면 처음부터 Mock으로 바로 테스트 케이스를 작성하는 경우가 많다고 해서 궁금합니다.
         */

        @Test
        @DisplayName("포인트 충전/이용 내역이 없으면 빈 리스트를 반환해야 한다.")
        void givenNoHistory_whenGetPointHistory_thenReturnEmptyList() {
            // given
            long userId = 1L;

            // when
            List<PointHistory> result = pointService.findPointHistoryByUserId(userId);

            // then
            assertThat(result).isEmpty();
            /*
                여태까지 했던 포인트 충전, 포인트 사용, 포인트 조회 테스트 코드와는 다르게 하드코딩 방식으로 먼저 접근하도록 했다. (TDD의 이론적인 원칙)
                    => Question2에도 달아놨지만, 실무적으로 하드코딩 방식으로 안 하는 경우도 있다고 하여 두 가지 방식을 적용해봤다.
                따라서 이용내역이 없을 경우 빈 리스트를 반환하도록 했다.
             */
        }

        @Test
        @DisplayName("포인트 충전/이용 내역이 있으면 비어있지 않은 리스트를 반환해야 한다.")
        void givenVaildHistory_whenGetPointHistory_thenReturnNotEmptyList() {
            // given
            long userId = 1L;
            long updateMillis = 1000L;
            List<PointHistory> expectedHistory = Arrays.asList(
                    new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, updateMillis),
                    new PointHistory(2L, userId, 500L, TransactionType.USE, updateMillis + 1000L),
                    new PointHistory(3L, userId, 200L, TransactionType.USE, updateMillis + 2000L)
            );

            when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedHistory);

            // when
            List<PointHistory> result = pointService.findPointHistoryByUserId(userId);

            // then
            assertThat(result).isNotEmpty();
            /*
                이용내역이 없을 경우의 테스트 코드에 의해 작성된 하드코딩이 해당 테스트 조건에서 실패하기 때문에
                외부 API 사용 시점이라 생각하여 pointHistoryTable Mock 객체를 활용해 비어있지 않은 리스트 stub을 생성했다.
             */
        }
    }
}

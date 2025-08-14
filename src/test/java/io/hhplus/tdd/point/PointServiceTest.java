package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

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
}
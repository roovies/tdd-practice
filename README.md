# tdd-practice
# 1. 동시성 관련 문제 분석
## 1-1. 개요
현재 `UserPointTable`은 인스턴스 변수로 `HashMap`에 회원 포인트 정보를 담고 있고, <br>
`PointHistoryTable`은 인스턴스 변수로 `ArrayList`에 포인트 충전/사용 히스토리를 담고 있다.
```java
// UserPointTable
private final Map<Long, UserPoint> table = new HashMap<>();

// PointHistoryTable
private final List<PointHistory> table = new ArrayList<>();
```

두 자료구조는 동시성 제어를 지원하지 않는 자료구조다.
따라서 다음과 같은 동시성 관련 문제가 발생할 수 있다.
<br><br>
## 1-2. 동시성 문제 시나리오
### Lost Update
- **포인트 충전할 때**
  1. 쓰레드A에서 `userId`의 포인트 충전을 위해 `selectById(1L)`을 수행 => 0포인트 조회
  2. 동시에 쓰레드B에서 마찬가지로 `userId`의 포인트 충전을 위해 `selectById(1L)`을 수행 => 0포인트 조회
  3. 쓰레드A에서 `insertOrUpdate(1L, 1000)`을 수행하여 포인트 충전을 완료한다. => 1000포인트 저장
  4. 쓰레드B에서 `insertOrUpdate(1L, 500)`을 수행하여 포인트 충전을 완료한다. => 500포인트 저장
  5. 최종 결과가 1500포인트가 아닌, 1000포인트가 됨
- **히스토리 남길 때**
  1. 쓰레드A에서 `PointHistoryTable`에 이력을 남기기 위해 `insert()` 호출
  2. 동시에 쓰레드 B에서도 `PointHistoryTable`에 이력을 남기기 위해 `insert()` 호출
  3. 같은 `cursor` 값을 동시에 수정하기 때문에 쓰레드A 또는 쓰레드B의 이력이 누락됨
<br>
### 가시성 문제
- **히스토리 남길 때**
  1. 쓰레드A가 코어1에서 실행됨
  2. 쓰레드A에서 히스토리 남기기 위해 `insert()` 호출
  3. 인스턴스 변수인 `cursor` 값을 1 → 2로 변경
  4. 쓰레드A 작업 완료 (하지만 코어1의 CPU 캐시에만 반영됨)
  5. 쓰레드B가 코어2에서 실행됨
  6. 쓰레드B에서 히스토리 남기기 위해 `insert()` 호출
  7. 쓰레드B는 코어2의 CPU캐시에 이전 저장값인 1을 읽어옴
  8. 쓰레드B에서 `cursor=1` 값을 덮어쓰기 함 => 쓰레드A의 작업 내역 삭제됨

<br>

## 1-3. 동시성 문제 해결 방법
동시성을 해결할 때 `synchronized`를 통해 메서드 전체를 동기화 처리하는 방법과 <br>


  

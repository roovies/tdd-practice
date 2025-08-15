package io.hhplus.tdd.point.service.impl;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private static final long MAX_POINT = 1000000L;
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Override
    public UserPoint findUserPointByUserId(long userId) {
        if(userId < 1)
            throw new RuntimeException("잘못된 회원ID입니다.");
        return userPointTable.selectById(userId);
    }

    @Override
    public UserPoint charge(long userId, long amount) {
        if(amount < 1)
            throw new RuntimeException("올바른 충전 금액이 아닙니다.");

        if(amount % 100 != 0)
            throw new RuntimeException("포인트 충전 단위는 100원입니다.");

        UserPoint storedUserPoint = findUserPointByUserId(userId);
        final long chargePoint = storedUserPoint.point() + amount;
        if (chargePoint > MAX_POINT) {
            throw new RuntimeException("최대 포인트는 100만 포인트여야 합니다.");
        }

        UserPoint result = userPointTable.insertOrUpdate(userId, storedUserPoint.point() + amount);
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        return result;
    }

    @Override
    public UserPoint usePoint(long userId, long amount) {
        if(amount < 1)
            throw new RuntimeException("포인트는 1원 이상 사용해야 합니다.");

        UserPoint storedUserPoint = findUserPointByUserId(userId);
        if (storedUserPoint.point() == 0)
            throw new RuntimeException("사용할 포인트가 없습니다.");

        if (storedUserPoint.point() < amount)
            throw new RuntimeException("포인트가 부족합니다.");

        final long remainingPoint = storedUserPoint.point() - amount;
        UserPoint result = userPointTable.insertOrUpdate(userId, remainingPoint);
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
        return result;
    }

    @Override
    public List<PointHistory> findPointHistoryByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}



package io.hhplus.tdd.point.service.impl;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;

    @Override
    public UserPoint findUserPointByUserId(long userId) {
        if(userId < 1)
            throw new RuntimeException("잘못된 회원ID입니다.");
        return userPointTable.selectById(userId);
    }
}
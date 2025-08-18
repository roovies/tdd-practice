package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;

import java.util.List;

public interface PointService {
    UserPoint findUserPointByUserId(long userId);
    UserPoint charge(long userId, long amount);
    UserPoint usePoint(long userId, long amount);
    List<PointHistory> findPointHistoryByUserId(long userId);
}
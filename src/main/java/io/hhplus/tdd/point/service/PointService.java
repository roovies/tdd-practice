package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.UserPoint;

public interface PointService {
    UserPoint findUserPointByUserId(long userId);
}
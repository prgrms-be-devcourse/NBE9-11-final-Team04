package com.team04.idea.entity;

/** 아이디어 후원자에게 제공할 보상 방식을 표현하는 열거형입니다. */
public enum RewardType {

    /**
     * 후원자에게 포인트를 지급하는 보상 방식입니다.
     */
    REWARD_POINT,

    /**
     * 선착순 후원자에게 혜택을 제공하는 보상 방식입니다.
     */
    FIRST_COME,

    /**
     * 목표 달성 또는 정책에 따라 페이백을 제공하는 보상 방식입니다.
     */
    PAYBACK
}

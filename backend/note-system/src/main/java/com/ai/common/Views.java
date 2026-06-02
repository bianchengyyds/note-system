package com.ai.common;

public class Views {
    // 基础视图：仅包含 id、name、role
    public interface Base {}

    // 登录视图：继承基础视图，额外包含 token
    public interface Login extends Base {}

    // 详情视图：继承基础视图，包含更多用户信息
    public interface Detail extends Base {}
}
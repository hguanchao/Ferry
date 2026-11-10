package com.ferry.common.jdbc.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface IService<T> extends com.baomidou.mybatisplus.spring.service.IService<T> {

    /**
     * 根据queryWrapper获取数据列表
     *
     * @param queryWrapper Wrapper
     * @param function     收集的字段
     * @param <R>          泛型
     * @return List 数据列表
     */
    default <R> List<R> list(Wrapper<T> queryWrapper, Function<? super T, ? extends R> function) {
        return collect(getBaseMapper().selectList(queryWrapper), function);
    }

    /**
     * 收集数据列表
     *
     * @param list     数据列表
     * @param function 收集的字段
     * @param <R>      泛型
     * @return List 数据列表
     */
    default <R> List<R> collect(List<T> list, Function<? super T, ? extends R> function) {
        return list.stream().map(function).collect(Collectors.toList());
    }
}

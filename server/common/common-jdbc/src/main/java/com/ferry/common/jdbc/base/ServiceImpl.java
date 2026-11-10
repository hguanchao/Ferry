package com.ferry.common.jdbc.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.binding.MapperMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.function.Function;

public class ServiceImpl<M extends BaseMapper<T>, T> extends com.baomidou.mybatisplus.spring.service.impl.ServiceImpl<M, T> implements IService<T> {

    /**
     * 根据字段批量更新
     *
     * @param entityList 实体数据列表
     * @param function   函数式编程(实体字段函数)
     * @return boolean
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchBy(Collection<T> entityList, Function<T, Wrapper<T>> function) {
        String sqlStatement = this.getSqlStatement(SqlMethod.UPDATE);
        return executeBatch(entityList, (sqlSession, entity) -> {
            MapperMethod.ParamMap<Object> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            param.put(Constants.WRAPPER, function.apply(entity));
            sqlSession.update(sqlStatement, param);
            return 1;
        });
    }
}

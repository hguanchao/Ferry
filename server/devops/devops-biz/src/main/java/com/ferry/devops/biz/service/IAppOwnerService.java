package com.ferry.devops.biz.service;

import com.ferry.devops.api.entity.AppOwner;
import com.ferry.common.jdbc.base.IService;

import java.util.List;

/**
 * 应用负责人服务
 */
public interface IAppOwnerService extends IService<AppOwner> {

    /**
     * 按应用查询负责人用户id（type=1）
     *
     * @param appId 应用id
     * @return 用户id集合
     */
    List<Long> ownerIds(Long appId);

    /**
     * 按应用重建负责人列表（负责人 + 备份负责人）
     *
     * @param appId      应用id
     * @param ownerIds   负责人用户id，逗号分隔
     * @param backupIds  备份负责人用户id，逗号分隔
     */
    void replace(Long appId, String ownerIds, String backupIds);

    /**
     * 按应用查询负责人名称，顺序与传入的 ownerIds 一致
     *
     * @param appId    应用id
     * @param ownerIds 负责人用户id，逗号分隔
     * @return 名称逗号分隔
     */
    String ownerNames(Long appId, String ownerIds);
}

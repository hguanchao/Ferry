package com.ferry.system.biz.service.impl;

import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.system.api.entity.RoleDept;
import com.ferry.system.biz.mapper.RoleDeptMapper;
import com.ferry.system.biz.service.IRoleDeptService;
import org.springframework.stereotype.Service;

@Service
public class RoleDeptServiceImpl extends ServiceImpl<RoleDeptMapper, RoleDept> implements IRoleDeptService {
}

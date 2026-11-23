package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppOptionVO;
import com.ferry.devops.api.vo.AppVO;

import java.util.List;
import java.util.Map;

/**
 * 应用档案服务（应用本体、选项，以及供其他应用域服务复用的写入口）
 */
public interface IAppService extends IService<App> {

    IPage<AppVO> pageList(AppQuery body);

    boolean save(AppDTO body);

    boolean deleted(List<Long> ids);

    Map<String, String> optionMap();

    void checkCode(String code, Long id);

    boolean saveOrUpdateApp(App app);
}

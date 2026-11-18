package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.OpenTokenDTO;
import com.ferry.devops.api.dto.query.OpenTokenQuery;
import com.ferry.devops.api.entity.OpenToken;
import com.ferry.devops.api.vo.OpenTokenVO;
import com.ferry.devops.biz.mapper.OpenTokenMapper;
import com.ferry.devops.biz.service.IOpenApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

/**
 * 开放 API Token 服务实现
 */
@Service
@RequiredArgsConstructor
public class OpenApiServiceImpl extends ServiceImpl<OpenTokenMapper, OpenToken> implements IOpenApiService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public IPage<OpenTokenVO> tokenPage(OpenTokenQuery query) {
        IPage<OpenToken> pages = baseMapper.selectPage(Page.create(query), Wrappers.<OpenToken>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getAppId()), OpenToken::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), OpenToken::getStatus, query.getStatus())
                .orderByDesc(OpenToken::getCreateTime));
        IPage<OpenTokenVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        List<OpenTokenVO> records = new ArrayList<>();
        for (OpenToken item : pages.getRecords()) {
            records.add(BeanUtil.copy(item, OpenTokenVO.class));
        }
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenTokenVO issue(OpenTokenDTO body) {
        String plain = generateToken();
        OpenToken token = new OpenToken();
        token.setAppId(body.getAppId());
        token.setName(body.getName());
        token.setQuotaPerMinute(body.getQuotaPerMinute() == null ? 100 : body.getQuotaPerMinute());
        token.setTokenHash(sha256(plain));
        token.setTokenMask(mask(plain));
        token.setStatus(IntConst.ONE);
        token.setCreateTime(new Date());
        baseMapper.insert(token);
        OpenTokenVO vo = BeanUtil.copy(token, OpenTokenVO.class);
        vo.setTokenPlain(plain);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenTokenVO rotate(Long id) {
        OpenToken token = baseMapper.selectById(id);
        if (token == null) {
            throw new ServiceException("Token 不存在");
        }
        String plain = generateToken();
        token.setTokenHash(sha256(plain));
        token.setTokenMask(mask(plain));
        token.setLastRotateTime(new Date());
        baseMapper.updateById(token);
        OpenTokenVO vo = BeanUtil.copy(token, OpenTokenVO.class);
        vo.setTokenPlain(plain);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long id) {
        OpenToken token = baseMapper.selectById(id);
        if (token == null) {
            throw new ServiceException("Token 不存在");
        }
        token.setStatus(2);
        return baseMapper.updateById(token) > IntConst.ZERO;
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "otp_" + HexFormat.of().formatHex(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new ServiceException("Token 摘要计算失败");
        }
    }

    private String mask(String value) {
        if (value.length() <= 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}

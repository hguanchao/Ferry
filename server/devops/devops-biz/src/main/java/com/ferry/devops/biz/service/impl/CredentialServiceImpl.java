package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.CredentialDTO;
import com.ferry.devops.api.dto.query.CredentialQuery;
import com.ferry.devops.api.entity.Credential;
import com.ferry.devops.api.vo.CredentialVO;
import com.ferry.devops.biz.mapper.CredentialMapper;
import com.ferry.devops.biz.service.ICredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 凭据管理服务实现
 */
@Service
@RequiredArgsConstructor
public class CredentialServiceImpl extends ServiceImpl<CredentialMapper, Credential> implements ICredentialService {

    private static final String MASK = "******";
    /** M2 简化实现使用固定密钥加密存储 */
    private static final String AES_KEY = "ferry-devops-key";

    @Override
    public IPage<CredentialVO> pageList(CredentialQuery body) {
        IPage<Credential> pages = page(Page.create(body), Wrappers.<Credential>lambdaQuery()
                .like(StrUtil.isNotBlank(body.getName()), Credential::getName, body.getName())
                .eq(ObjUtil.isNotEmpty(body.getType()), Credential::getType, body.getType())
                .orderByDesc(Credential::getCreateTime));
        List<CredentialVO> records = BeanUtil.copy(pages.getRecords(), CredentialVO.class);
        for (CredentialVO vo : records) {
            vo.setCipherText(MASK);
        }
        IPage<CredentialVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(CredentialDTO body) {
        Credential credential = BeanUtil.copy(body, Credential.class);
        if (ObjUtil.isEmpty(body.getId())) {
            if (StrUtil.isBlank(body.getValue())) {
                throw new ServiceException("凭据值不能为空");
            }
            credential.setCipherText(encrypt(body.getValue()));
            credential.setRefCount(IntConst.ZERO);
            return super.save(credential);
        }
        // 编辑保留已有引用数；凭据值留空表示保持原密文
        Credential old = requireCredential(body.getId());
        credential.setRefCount(old.getRefCount());
        credential.setCipherText(StrUtil.isBlank(body.getValue()) ? old.getCipherText() : encrypt(body.getValue()));
        return updateById(credential);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rotate(Long id, String value) {
        if (StrUtil.isBlank(value)) {
            throw new ServiceException("新凭据值不能为空");
        }
        Credential credential = requireCredential(id);
        credential.setCipherText(encrypt(value));
        credential.setLastRotateTime(new Date());
        return updateById(credential);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的凭据");
        }
        for (Long id : ids) {
            Credential credential = requireCredential(id);
            if ((credential.getRefCount() != null && credential.getRefCount() > IntConst.ZERO)
                    || baseMapper.countRef(id) > IntConst.ZERO) {
                throw new ServiceException("凭据被引用，不可删除");
            }
        }
        return removeByIds(ids);
    }

    private Credential requireCredential(Long id) {
        Credential credential = getById(id);
        if (credential == null) {
            throw new ServiceException("凭据不存在");
        }
        return credential;
    }

    private String encrypt(String value) {
        return CryptoUtil.aesEncrypt(value, AES_KEY);
    }
}

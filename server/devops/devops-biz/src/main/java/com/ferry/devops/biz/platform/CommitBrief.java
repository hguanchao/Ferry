package com.ferry.devops.biz.platform;

import java.util.Date;

/**
 * 提交摘要，来自代码平台适配器的查询结果
 *
 * @param sha          提交完整 sha
 * @param author       提交人
 * @param committedTime 提交时间
 * @param message      提交说明
 */
public record CommitBrief(String sha, String author, Date committedTime, String message) {
}

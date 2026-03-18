package com.fsa.aicenter.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果
 *
 * @author FSA AI Center
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总页数
     */
    private Integer pages;

    public PageResult() {
    }

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
        this.pages = (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        return new PageResult<>(total, pageNum, pageSize, list);
    }

    /**
     * 空分页结果
     */
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        return new PageResult<>(0L, pageNum, pageSize, List.of());
    }
}

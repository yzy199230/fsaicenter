package com.fsa.aicenter.infrastructure.persistence.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PostgreSQL数组类型与Java List<String>的类型转换器
 * 用于处理PostgreSQL的VARCHAR[]等数组类型
 */
@MappedTypes(List.class)
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setNull(i, Types.ARRAY);
            return;
        }

        Connection connection = ps.getConnection();
        Array array = connection.createArrayOf("varchar", parameter.toArray());
        ps.setArray(i, array);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    /**
     * 将PostgreSQL数组转换为Java List
     */
    private List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return new ArrayList<>();
        }

        try {
            Object arrayObj = array.getArray();
            if (arrayObj == null) {
                return new ArrayList<>();
            }

            if (arrayObj instanceof String[]) {
                return new ArrayList<>(Arrays.asList((String[]) arrayObj));
            } else if (arrayObj instanceof Object[]) {
                Object[] objects = (Object[]) arrayObj;
                List<String> result = new ArrayList<>(objects.length);
                for (Object obj : objects) {
                    result.add(obj != null ? obj.toString() : null);
                }
                return result;
            }

            return new ArrayList<>();
        } finally {
            array.free();
        }
    }
}

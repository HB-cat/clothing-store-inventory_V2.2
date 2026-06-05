package com.heben.clothing_s_i_server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heben.clothing_s_i_server.entity.ProductAttrCombination;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductAttrCombinationMapper extends BaseMapper<ProductAttrCombination> {

    @Update("UPDATE product_attr_combination SET stock = stock - #{quantity}, version = version + 1, updated_at = NOW() " +
            "WHERE id = #{id} AND version = #{version} AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity, @Param("version") int version);

    @Update("UPDATE product_attr_combination SET stock = stock + #{quantity}, version = version + 1, updated_at = NOW() " +
            "WHERE id = #{id}")
    int addStock(@Param("id") Long id, @Param("quantity") int quantity);
}

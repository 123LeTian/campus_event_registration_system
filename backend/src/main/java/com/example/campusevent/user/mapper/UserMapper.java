package com.example.campusevent.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campusevent.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, username, password, name, role, enabled, created_at, updated_at FROM `user` WHERE username = #{username} LIMIT 1")
    User findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, name, role, enabled, created_at, updated_at FROM `user` WHERE id = #{id} LIMIT 1")
    User findById(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM `user` WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    @Select("SELECT enabled FROM `user` WHERE username = #{username} LIMIT 1")
    Boolean findEnabledByUsername(@Param("username") String username);
}

package com.hangzhoudianzi.demo.mapper;


import com.hangzhoudianzi.demo.pojo.people.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface UserMapper {
    List<User> findAll();
    User findByName(String username,int type);
    String findPswByName(String username,int type);
    void save(User user);
}

package com.hangzhoudianzi.demo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hangzhoudianzi.demo.pojo.people.Teacher;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    Teacher getTeacherById(String id);

}
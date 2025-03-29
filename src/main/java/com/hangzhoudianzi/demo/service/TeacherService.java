package com.hangzhoudianzi.demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hangzhoudianzi.demo.mapper.TeacherMapper;
import com.hangzhoudianzi.demo.pojo.people.Teacher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeacherService extends ServiceImpl<TeacherMapper, Teacher>
    implements TeacherServiceImp
{
    @Autowired
    private TeacherMapper teacherMapper;
    @Override
    public Teacher getTeacherById(String id){
        return teacherMapper.getTeacherById(id);
    }

}
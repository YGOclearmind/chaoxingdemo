package com.hangzhoudianzi.demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hangzhoudianzi.demo.mapper.CourseMapper;
import com.hangzhoudianzi.demo.pojo.people.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course>
        implements CourseServiceImp {
    @Autowired
    private CourseMapper courseMapper;

    @Override
    public List<Course> getCourseById(String id, String courseName, Integer credit, String teacherId) {
        return courseMapper.getCourseById(id, courseName, credit, teacherId);
    }
}

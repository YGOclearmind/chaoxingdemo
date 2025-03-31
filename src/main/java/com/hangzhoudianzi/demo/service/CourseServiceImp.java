package com.hangzhoudianzi.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hangzhoudianzi.demo.pojo.people.Course;

import java.util.List;

public interface CourseServiceImp extends IService<Course> {

    List<Course> getCourseById(String id, String courseName, Double credit, String teacherId);

//    List<Classroom> updateClassroom(Classroom classroom);

}


package com.hangzhoudianzi.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hangzhoudianzi.demo.pojo.people.Course;

public interface CourseServiceImp extends IService<Course> {

    Course getCourseById(String id);

//    List<Classroom> updateClassroom(Classroom classroom);

}


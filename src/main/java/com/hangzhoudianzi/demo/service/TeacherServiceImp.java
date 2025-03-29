package com.hangzhoudianzi.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hangzhoudianzi.demo.pojo.people.Teacher;

public interface TeacherServiceImp extends IService<Teacher> {

    Teacher getTeacherById(String id);

//    List<Classroom> updateClassroom(Classroom classroom);

}



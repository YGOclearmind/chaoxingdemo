package com.hangzhoudianzi.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hangzhoudianzi.demo.pojo.resource.Classroom;

import java.util.List;

public interface ClassroomServiceImp extends IService<Classroom> {

    Classroom getClassroomById(String id);

//    List<Classroom> updateClassroom(Classroom classroom);

}


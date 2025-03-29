package com.hangzhoudianzi.demo.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hangzhoudianzi.demo.mapper.ClassroomMapper;
import com.hangzhoudianzi.demo.pojo.resource.Classroom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class ClassroomService extends ServiceImpl<ClassroomMapper,Classroom>
    implements ClassroomServiceImp
{
    @Autowired
    private ClassroomMapper classroomMapper;

    @Override
    public Classroom getClassroomById(String id) {
        return classroomMapper.getClassroomById(id);
    }

//    @Override
//    public List<Classroom> updateClassroom(Classroom classroom) {return classroomMapper.updateClassroom(classroom);}
}

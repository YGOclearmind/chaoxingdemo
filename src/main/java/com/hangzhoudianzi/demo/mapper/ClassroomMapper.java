package com.hangzhoudianzi.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hangzhoudianzi.demo.pojo.resource.Classroom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClassroomMapper extends BaseMapper<Classroom> {

    Classroom getClassroomById(String id);


//    List<Classroom> updateClassroom(Classroom classroom);



}
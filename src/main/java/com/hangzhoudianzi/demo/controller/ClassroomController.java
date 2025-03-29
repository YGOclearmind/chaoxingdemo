package com.hangzhoudianzi.demo.controller;


import com.hangzhoudianzi.demo.mapper.ClassroomMapper;
import com.hangzhoudianzi.demo.pojo.resource.Classroom;
import com.hangzhoudianzi.demo.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {
    @Autowired
    private ClassroomService classroomService;
    @Autowired
    private ClassroomMapper classroomMapper;

    @GetMapping("/getAllClassrooms")
    public List<Classroom> getAllClassrooms() {
        List<Classroom> list = classroomService.list();
        return list;
    }

    @GetMapping("/getClassroom/{id}")
    public Classroom getClassroomById(@PathVariable("id") String id) throws Exception {
        Classroom classrooms = classroomMapper.getClassroomById(id);
        if (classrooms == null) {
            throw new Exception("没有找到此教室");
        }
        return classrooms;
    }

    @PostMapping("/insertClassroom")
    public String insertClassroom(@RequestBody Classroom classroom) {
        Classroom classrooms =classroomService.getClassroomById(classroom.getId());
        if (classrooms == null) {
            boolean save = classroomService.save(classroom);
            if (save) {
                return "添加成功";
            } else return "添加失败";
        } else return "已添加教室，请勿重复操作！";
    }


    @PostMapping("/deleteClassroom/{id}")
    public String deleteClassroom(@PathVariable("id") String id) {
    Classroom classroom = classroomService.getClassroomById(id);
    if(classroom == null){
        return "没有此教室";
    }else{
        boolean b = classroomService.removeById(id);
        if(b){
            return "删除成功";
        }else return "删除失败";
    }
    }
//    @PostMapping("/updateClassroom")
//    public String updateClassroom(@PathVariable("id") int id) {
//        Classroom classroom = classroomService.getClassroomById(id);
//        if (classroom == null) {
//            return "没有此教室";
//        }else{
//            boolean b = classroomService.removeById(id);
//            if(b){
//
//            }
//    }
}
package com.hangzhoudianzi.demo.controller;

import com.hangzhoudianzi.demo.mapper.TeacherMapper;
import com.hangzhoudianzi.demo.pojo.people.Teacher;
import com.hangzhoudianzi.demo.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private TeacherMapper teacherMapper;

    @GetMapping("/getAllTeachers")
    public List<Teacher> getAllTeachers() {
        List<Teacher> list = teacherService.list();
        return list;
    }

    @GetMapping("/getTeacherById/{id}")
    public Teacher getTeacherById(@PathVariable("id") String id) throws Exception {
        Teacher teachers = teacherMapper.getTeacherById(id);
        if(teachers == null){
            throw new Exception("没有找到此老师");
        }
        return teachers;
    }

    @PostMapping("/insertTeacher")
    public String insertTeacher(@RequestBody Teacher teacher) {
        Teacher teachers = teacherService.getTeacherById(teacher.getId());
        if(teachers == null){
            boolean save = teacherService.save(teacher);
            if(save){
                return "添加成功";
            }else return "添加失败";
        }else return "已添加老师，请勿重复操作";
    }

//    @PostMapping("/updateTeacher")
//    public List<Teacher> updateTeacher(Teacher teacher) {
//        return teacherService.updateTeacher(teacher);
//    }

    @PostMapping("/deleteTeacher/{id}")
    public String deleteTeacher(@PathVariable("id") String id) {
        Teacher teacher = teacherService.getTeacherById(id);
        if(teacher == null){
            return "没有此老师";
        }else {
            boolean b = teacherService.removeById(id);
            if(b){
                return "删除成功";
            }else return "删除失败";
        }
    }
}
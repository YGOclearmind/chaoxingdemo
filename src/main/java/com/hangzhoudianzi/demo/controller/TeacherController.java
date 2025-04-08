package com.hangzhoudianzi.demo.controller;

import com.hangzhoudianzi.demo.mapper.TeacherMapper;
import com.hangzhoudianzi.demo.pojo.people.Course;
import com.hangzhoudianzi.demo.pojo.people.Teacher;
import com.hangzhoudianzi.demo.pojo.resource.Timetable;
import com.hangzhoudianzi.demo.service.TeacherService;
import com.hangzhoudianzi.demo.mapper.TimetableMapper;
import com.hangzhoudianzi.demo.service.CourseService;
import com.hangzhoudianzi.demo.pojo.dto.TimetableDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private TimetableMapper timetableMapper;
    @Autowired
    private CourseService courseService;

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

    @GetMapping("/getTeacherTimeTable/{id}")
    public List<TimetableDTO> getTeacherTimeTable(@PathVariable("id") String id) throws Exception {
        // 首先验证教师是否存在
        Teacher teacher = teacherMapper.getTeacherById(id);
        if(teacher == null){
            throw new Exception("没有找到此老师");
        }
        
        // 获取该教师的所有课表记录
        List<Timetable> timetables = timetableMapper.getTimetablesByTeacherId(id);
        
        // 转换为DTO并添加额外信息
        return timetables.stream().map(timetable -> {
            Course course = timetable.getCourseId() != null ? courseService.getCourseById(timetable.getCourseId()) : null;
            TimetableDTO dto = TimetableDTO.fromTimetableAndCourse(timetable, course);
            dto.setTeacherName(teacher.getName());
            return dto;
        }).collect(Collectors.toList());
    }
}
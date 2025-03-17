package com.hangzhoudianzi.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hangzhoudianzi.demo.mapper.CourseMapper;
import com.hangzhoudianzi.demo.mapper.TeacherMapper;
import com.hangzhoudianzi.demo.pojo.people.Course;
import com.hangzhoudianzi.demo.pojo.people.Teacher;
import com.hangzhoudianzi.demo.pojo.vo.CourseInfo;
import com.hangzhoudianzi.demo.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private TeacherMapper teacherMapper;

    @GetMapping("/getAllCourses")
    public List<CourseInfo> getAllCourses() {
        List<Course> list = courseService.list();

        //返回的集合
        List<CourseInfo> courseInfos = new ArrayList<>();
        for (Course course : list) {

            CourseInfo courseInfo = new CourseInfo();
            courseInfo.setId(course.getId());
            courseInfo.setCourseName(course.getCourseName());
            courseInfo.setCredit(course.getCredit());
            courseInfo.setBeginTime(course.getBeginTime());
            courseInfo.setEndTime(course.getEndTime());
            courseInfo.setDate(course.getDate());
            //查询老师的信息
            Teacher teacher = teacherMapper.selectById(course.getTeacherId());
            courseInfo.setTeacherName(teacher.getName());
            courseInfo.setTeacherId(teacher.getId());
            courseInfos.add(courseInfo);
        }

        return courseInfos;
    }

    @GetMapping("/getCourse")
    public List<Course> getCourseById(@RequestParam(value = "id", required = false) String id,
                                      @RequestParam(value = "courseName", required = false) String courseName,
                                      @RequestParam(value = "credit", required = false) Integer credit,
                                      @RequestParam(value = "teacherId", required = false) String teacherId) throws Exception {
        List<Course> course = courseService.getCourseById(id, courseName, credit, teacherId);
        if (course.size() == 0) {
            throw new Exception("没有找到此课程");
        }
        return course;
    }

    @PostMapping("/insertCourse")
    public String insertCourse(@RequestBody Course course) {
        Course courses = courseService.getById(course.getId());
        if (courses == null) {
            boolean save = courseService.save(course);
            if (save) {
                return "添加成功";
            } else return "添加失败";
        } else return "已添加课程，请勿重复操作！";
    }

//    @PostMapping("/updateCourse")
//    public List<Course> updateCourse(Course course) {
//        return courseService.updateCourse(course);
//    }

    @PostMapping("/deleteCourse/{id}")
    public String deleteCourse(@PathVariable("id") String id) {
        Course course = courseService.getById(id);
        if (course == null) {
            return "没有此课程";
        } else {
            boolean b = courseService.removeById(id);
            if (b) {
                return "删除成功";
            } else return "删除失败";
        }
    }
}

package com.hangzhoudianzi.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.github.pagehelper.IPage;
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
//    private int page;
//    private int num;

    @GetMapping("/getAllCourses")
    public List<CourseInfo> getAllCourses(int page, int num) {
        //使用mybatis-plus写sql

        Page<Course> page1 = new Page<>(page, num);
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        Page<Course> coursePage = courseMapper.selectPage(page1, queryWrapper);

        List<Course> records = coursePage.getRecords();
        List<CourseInfo>  courseInfos= new ArrayList<>();
        for (Course course : records) {

            CourseInfo courseInfo = new CourseInfo();
            courseInfo.setId(course.getId());
            courseInfo.setSemester(course.getSemester());
            courseInfo.setCourseName(course.getCourseName());
            courseInfo.setCredit(course.getCredit());
            courseInfo.setBeginWeek(course.getBeginWeek());
            courseInfo.setEndWeek(course.getEndWeek());
            courseInfo.setConsecutiveSections(course.getConsecutiveSections());
            courseInfo.setClassroomType(course.getClassroomType());
            courseInfo.setClassroom(course.getClassroom());
            courseInfo.setBuilding(course.getBuilding());
            courseInfo.setDate(course.getDate());
            courseInfo.setCompositionClasses(course.getCompositionClasses());
            courseInfo.setClassesId(course.getClassesId());
            courseInfo.setClassesName(course.getClassesName());
            courseInfo.setCreditHourType(course.getCreditHourType());
            courseInfo.setPriority(course.getPriority());
            courseInfo.setClassSize(course.getClassSize());
            courseInfo.setCourseNature(course.getCourseNature());
            courseInfo.setExternallyHire(course.getExternallyHire());
            courseInfo.setDepartment(course.getDepartment());
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
                                      @RequestParam(value = "credit", required = false) Double credit,
                                      @RequestParam(value = "teacherId", required = false) String teacherId) throws Exception {
        List<Course> course = courseService.getCourseById(id, courseName,  credit, teacherId);
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

package com.hangzhoudianzi.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hangzhoudianzi.demo.pojo.people.Course;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {


    List<Course> getCoursesById(String id, String courseName, Double credit, String teacherId);
//    List<Course> insertCourse(Course course);
//    List<Course> updateCourse(Course course);
//    List<Course> deleteCourse(int id);
    Course getCourseById(String id) ;
}

package com.hangzhoudianzi.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hangzhoudianzi.demo.pojo.people.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {


    Course getCourseById(String id);
//    List<Course> insertCourse(Course course);
//    List<Course> updateCourse(Course course);
//    List<Course> deleteCourse(int id);
}

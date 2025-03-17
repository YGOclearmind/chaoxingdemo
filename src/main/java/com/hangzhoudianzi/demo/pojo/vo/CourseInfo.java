package com.hangzhoudianzi.demo.pojo.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CourseInfo {
    private String id;
    private String courseName;
    private Integer credit;     //学分
    // 关联教师
    private Integer teacherId;
    private Date beginTime;
    private Date endTime;
    private String date;

    //老师姓名
    private String teacherName;

}
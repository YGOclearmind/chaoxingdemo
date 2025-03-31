package com.hangzhoudianzi.demo.pojo.people;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Time;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Course {
    private String semester;
    private String id;
    private String courseName;
    // 关联教师
    private String teacherId;
    private Double credit;     //学分
    private int beginWeek;
    private int endWeek;
    private int consecutiveSections;
    private String classroomType;
    private String classroom;
    private String building;
    private String date;
    private String compositionClasses;
    private String classesId;
    private String classesName;
    private String creditHourType;
    private String priority;//排课优先级
    private String classSize;
    private String courseNature;
    private String externallyHire;
    private String department;
    //老师姓名
}
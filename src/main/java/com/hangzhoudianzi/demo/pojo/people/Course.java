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
    private String id;
    private String courseName;
    // 关联教师
    private String teacherId;
    private double credit;     //学分
    private int beginWeek;
    private int endWeek;
    private int consecutiveSections;
    private String classroomType;
    private String classroom;
    private String building;
    private String date;

    //老师姓名
}
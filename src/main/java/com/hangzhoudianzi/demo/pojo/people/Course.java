package com.hangzhoudianzi.demo.pojo.people;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Course {
    private String id;
    private String courseName;
    private Integer credit;     //学分
    // 关联教师
    private Integer teacherId;
    private Date beginTime;
    private Date endTime;
    private String date;
    //老师姓名
}
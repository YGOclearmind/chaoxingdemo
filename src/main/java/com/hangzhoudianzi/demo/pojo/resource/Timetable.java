package com.hangzhoudianzi.demo.pojo.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Timetable {
    private Integer id;
    private Integer courseId;
    private Integer teacherId;
    private Integer classroomId;
    private Date scheduleTime;  // 可扩展为具体的周次、节次等

}

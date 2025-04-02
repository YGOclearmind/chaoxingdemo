package com.hangzhoudianzi.demo.pojo.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Timetable {
    private Integer id;
    private Integer classId;
    private String courseId;
    private String teacherId;
    private String classroomId;
    private Date scheduleTime;  // 可扩展为具体的周次、节次等
    private String periodInfo;  // 可选：以字符串形式存储节次信息，例如："1,2,3"表示第1,2,3节课
    private Integer dayOfWeek; // 星期几，1-7表示周一到周日
}

package com.hangzhoudianzi.demo.pojo.dto;

import com.hangzhoudianzi.demo.pojo.resource.Timetable;
import com.hangzhoudianzi.demo.pojo.people.Course;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TimetableDTO {
    private Integer id;
    private Integer classId;
    private String courseId;
    private String teacherId;
    private String classroomId;
    private String scheduleTime;
    private String periodInfo;
    private Integer dayOfWeek;
    private String teacherName;
    private String courseName;
    private int beginWeek;
    private int endWeek;

    public static TimetableDTO fromTimetable(Timetable timetable) {
        TimetableDTO dto = new TimetableDTO();
        dto.setId(timetable.getId());
        dto.setClassId(timetable.getClassId());
        dto.setCourseId(timetable.getCourseId());
        dto.setTeacherId(timetable.getTeacherId());
        dto.setClassroomId(timetable.getClassroomId());
        dto.setScheduleTime(timetable.getScheduleTime() != null ? timetable.getScheduleTime().toString() : null);
        dto.setPeriodInfo(timetable.getPeriodInfo());
        dto.setDayOfWeek(timetable.getDayOfWeek());
        return dto;
    }

    public static TimetableDTO fromTimetableAndCourse(Timetable timetable, Course course) {
        TimetableDTO dto = fromTimetable(timetable);
        if (course != null) {
            dto.setCourseName(course.getCourseName());
            dto.setBeginWeek(course.getBeginWeek());
            dto.setEndWeek(course.getEndWeek());
        }
        return dto;
    }
} 
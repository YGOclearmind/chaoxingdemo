package com.hangzhoudianzi.demo.controller;
import com.alibaba.excel.EasyExcel;

import com.hangzhoudianzi.demo.mapper.CourseMapper;
import com.hangzhoudianzi.demo.pojo.people.Course;
import com.hangzhoudianzi.demo.pojo.people.Teacher;
import com.hangzhoudianzi.demo.pojo.resource.Classroom;
import com.hangzhoudianzi.demo.pojo.resource.Schedule;
import com.hangzhoudianzi.demo.pojo.resource.Timetable;
import com.hangzhoudianzi.demo.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private TimetableService timetableService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private ClassroomService classroomService;


    // 自动排课接口
    @PostMapping("/autoSchedule")
    public String autoSchedule() {
        scheduleService.autoSchedule();
        return "Automatic scheduling completed.";
    }

    // 手工排课接口
    @PostMapping("/manualSchedule")
    public String manualSchedule(@RequestBody Timetable timetable) {
        scheduleService.manualSchedule(timetable);
        return "Manual scheduling updated.";
    }

    // 查询所有课表
    @GetMapping("/timetables")
    public List<Timetable> getAllTimetables() {
        return timetableService.getAllTimetables();
    }

    @GetMapping("/exportExcel")
    public void exportExcel(HttpServletResponse response) throws IOException {



        // 查询数据库获取所有课表数据
        List<Timetable> timetableList = timetableService.getAllTimetables();

        // 课表格式：行（节次）× 列（星期）
        int maxPeriods = 12; // 假设一天最多 12 节课
        int maxDays = 7; // 一周 7 天
        String[][] timetableMatrix = new String[maxPeriods + 1][maxDays + 1];

        // 填充表头
        timetableMatrix[0][0] = "节次\\星期";
        for (int i = 1; i <= maxDays; i++) {
            if (i == 7) {
                timetableMatrix[0][i] = "星期日";
            } else {
                timetableMatrix[0][i] = "星期" + i;
            }
        }

        for (int i = 1; i <= maxPeriods; i++) {
            timetableMatrix[i][0] = "第" + i + "节";
        }

        // 填充课表内容
        for (Timetable t : timetableList) {
            int day = t.getDayOfWeek(); // 星期几
            Course course = courseService.getCourseById(t.getCourseId());
            Teacher teacher = teacherService.getTeacherById(t.getTeacherId());
            Classroom classroom = classroomService.getClassroomById(t.getClassroomId());

            String courseInfo = course.getCourseName() + " " + teacher.getName() + " " + classroom.getName();

            // 解析 periodInfo，例如："1,2,3"
            String periodInfo = t.getPeriodInfo();
            if (periodInfo != null && !periodInfo.isEmpty()) {
                String[] periods = periodInfo.split(",");
                for (String p : periods) {
                    int period = Integer.parseInt(p.trim()); // 转换成整数
                    timetableMatrix[period][day] = courseInfo; // 赋值到课表矩阵
                }
            }
        }

        // 转换为 EasyExcel 需要的 List<List<String>>
        List<List<String>> excelData = new ArrayList<>();
        for (String[] row : timetableMatrix) {
            excelData.add(Arrays.asList(row));
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("课表.xlsx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        // 使用 EasyExcel 写入
        EasyExcel.write(response.getOutputStream()).sheet("课表").doWrite(excelData);
    }

}
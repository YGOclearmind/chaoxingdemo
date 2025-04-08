package com.hangzhoudianzi.demo.controller;
import com.alibaba.excel.EasyExcel;

import com.hangzhoudianzi.demo.mapper.CourseMapper;
import com.hangzhoudianzi.demo.pojo.people.Course;
import com.hangzhoudianzi.demo.pojo.people.Teacher;
import com.hangzhoudianzi.demo.pojo.resource.Classroom;
import com.hangzhoudianzi.demo.pojo.resource.Schedule;
import com.hangzhoudianzi.demo.pojo.resource.Timetable;
import com.hangzhoudianzi.demo.service.*;
import com.hangzhoudianzi.demo.pojo.dto.TimetableDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @PostMapping("/autoSchedule/{classId}")
    public String autoSchedule(@PathVariable Integer classId) {
        if (classId == null || classId <= 0) {
            return "班级ID不能为空或小于等于0";
        }
        try {
            // 检查班级是否已经排过课程
            List<Timetable> existingTimetables = timetableService.getTimetablesByClassId(classId);
            if (!existingTimetables.isEmpty()) {
                return "第" + classId + "班已经排过课程，如需重新排课请先清空原有课表";
            }
            scheduleService.autoSchedule(classId);
            return "已完成第" + classId + "班的排课";
        } catch (Exception e) {
            return "排课失败：" + e.getMessage();
        }
    }

    // 手工排课接口
    @PostMapping("/manualSchedule")
    public String manualSchedule(@RequestBody Timetable timetable) {
        scheduleService.manualSchedule(timetable);
        return "Manual scheduling updated.";
    }

    // 查询所有课表
    @GetMapping("/timetables")
    public List<TimetableDTO> getAllTimetables() {
        List<Timetable> timetables = timetableService.getAllTimetables();
        return timetables.stream().map(timetable -> {
            Course course = timetable.getCourseId() != null ? courseService.getCourseById(timetable.getCourseId()) : null;
            Teacher teacher = timetable.getTeacherId() != null ? teacherService.getTeacherById(timetable.getTeacherId()) : null;
            
            TimetableDTO dto = TimetableDTO.fromTimetableAndCourse(timetable, course);
            if(teacher != null) {
                dto.setTeacherName(teacher.getName());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // 按班级查询课表
    @GetMapping("/class/{classId}")
    public List<TimetableDTO> getClassTimetables(@PathVariable Integer classId) {
        List<Timetable> timetables = timetableService.getTimetablesByClassId(classId);
        return timetables.stream().map(timetable -> {
            Course course = timetable.getCourseId() != null ? courseService.getCourseById(timetable.getCourseId()) : null;
            Teacher teacher = timetable.getTeacherId() != null ? teacherService.getTeacherById(timetable.getTeacherId()) : null;
            
            TimetableDTO dto = TimetableDTO.fromTimetableAndCourse(timetable, course);
            if(teacher != null) {
                dto.setTeacherName(teacher.getName());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // 修改导出Excel方法，支持按班级导出
    @GetMapping("/exportExcel/{classId}")
    public void exportExcel(@PathVariable Integer classId, HttpServletResponse response) throws IOException {
        // 查询指定班级的课表数据
        List<TimetableDTO> timetableList = getClassTimetables(classId);

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
        for (TimetableDTO t : timetableList) {
            int day = t.getDayOfWeek(); // 星期几
            String courseInfo = t.getCourseName() + " " + t.getTeacherName() + 
                              " (第" + t.getBeginWeek() + "-" + t.getEndWeek() + "周)";

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

    // 多班级排课
    @PostMapping("/autoScheduleMultiClass/{classCount}")
    public String autoScheduleMultiClass(@PathVariable int classCount) {
        if (classCount <= 0) {
            return "班级数量必须大于0";
        }
        if (classCount > 50) {  // 添加上限检查
            return "班级数量不能超过50";
        }
        try {
            scheduleService.autoScheduleMultiClass(classCount);
            return "已完成" + classCount + "个班级的排课";
        } catch (Exception e) {
            return "排课失败：" + e.getMessage();
        }
    }

    // 查询所有班级的课表
    @GetMapping("/allClasses")
    public Map<Integer, List<TimetableDTO>> getAllClassesTimetables() {
        Map<Integer, List<Timetable>> classTimetables = timetableService.getAllClassesTimetables();
        return classTimetables.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(timetable -> {
                    Course course = timetable.getCourseId() != null ? courseService.getCourseById(timetable.getCourseId()) : null;
                    Teacher teacher = timetable.getTeacherId() != null ? teacherService.getTeacherById(timetable.getTeacherId()) : null;
                    
                    TimetableDTO dto = TimetableDTO.fromTimetableAndCourse(timetable, course);
                    if(teacher != null) {
                        dto.setTeacherName(teacher.getName());
                    }
                    return dto;
                }).collect(Collectors.toList())
            ));
    }
}
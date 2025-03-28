package com.hangzhoudianzi.demo.controller;

import com.hangzhoudianzi.demo.pojo.resource.Timetable;
import com.hangzhoudianzi.demo.service.ScheduleService;
import com.hangzhoudianzi.demo.service.TimetableService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private TimetableService timetableService;

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


}
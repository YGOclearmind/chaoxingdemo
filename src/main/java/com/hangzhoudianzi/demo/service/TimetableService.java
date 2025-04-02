package com.hangzhoudianzi.demo.service;

import com.hangzhoudianzi.demo.mapper.TimetableMapper;
import com.hangzhoudianzi.demo.pojo.resource.Timetable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimetableService {
    @Autowired
    private TimetableMapper timetableMapper;

    public int addTimetable(Timetable timetable) {
        return timetableMapper.insertTimetable(timetable);
    }
    public List<Timetable> getTimetablesByClassId(Integer classId){
        return timetableMapper.getTimetablesByClassId(classId);
    };
    public List<Timetable> getAllTimetables() {
        return timetableMapper.getAllTimetables();
    }
    
    public Map<Integer, List<Timetable>> getAllClassesTimetables() {
        List<Timetable> allTimetables = timetableMapper.getAllTimetables();
        return allTimetables.stream()
                .collect(Collectors.groupingBy(Timetable::getClassId));
    }
}
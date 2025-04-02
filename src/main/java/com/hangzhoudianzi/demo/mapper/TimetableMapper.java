package com.hangzhoudianzi.demo.mapper;

import com.hangzhoudianzi.demo.pojo.resource.Timetable;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TimetableMapper {

    int insertTimetable(Timetable timetable);
    List<Timetable> getAllTimetables();
    List<Timetable> getTimetablesByClassId(Integer classId);
    int deleteByClassId(Integer classId);
}

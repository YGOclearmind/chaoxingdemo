package com.hangzhoudianzi.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hangzhoudianzi.demo.pojo.resource.ScheduleSettings;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
@Mapper
public interface ScheduleSettingsMapper {
    ScheduleSettings getScheduleSettingsById(int id);
    List<ScheduleSettings> getAllScheduleSettings();
    int insertScheduleSettings(ScheduleSettings settings);
    int updateScheduleSettings(ScheduleSettings settings);
    int deleteScheduleSettings(int id);
}

package com.hangzhoudianzi.demo.service;

import com.hangzhoudianzi.demo.mapper.ClassroomMapper;
import com.hangzhoudianzi.demo.mapper.CourseMapper;
import com.hangzhoudianzi.demo.mapper.TeacherMapper;
import com.hangzhoudianzi.demo.mapper.TimetableMapper;
import com.hangzhoudianzi.demo.pojo.people.Course;
import com.hangzhoudianzi.demo.pojo.people.Teacher;
import com.hangzhoudianzi.demo.pojo.resource.Classroom;
import com.hangzhoudianzi.demo.pojo.resource.Timetable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

//实现自动排课与手工排课示例，实际排课算法可根据需求扩展
@Service
public class ScheduleService {
    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ClassroomMapper classroomMapper;
    @Autowired
    private TimetableMapper timetableMapper;
    @Autowired
    private ClassroomService classroomService;
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private CourseService courseService;

    /**
     * 自动排课入口方法
     * <p>
     * 该方法为简化示例：
     * 1. 获取所有课程、教师、教室；
     * 2. 模拟遍历一周内（例如5天，每天8节）的时间段；
     * 3. 对于每门课程，根据教师、教室和时间段约束进行安排；
     * 4. 如果满足条件，则创建排课记录。
     * <p>
     * 实际项目中应考虑：
     * - 教师每天/每周的最大节数限制
     * - 连排节次（两节、四节）及其起始节次规则
     * - 禁排设置（学校、教师、学生、课程、教室、角色维度）
     * - 优先级（院系、课程性质、学时类型、课程类型）
     * - 固定教室及班级、教师教室集中、班级人数与教室容量匹配等
     */
    public void autoSchedule() {
        // 获取数据
        List<Course> courses = courseService.list();
        List<Teacher> teachers = teacherService.list();
        List<Classroom> classrooms = classroomService.list();

        // 模拟一周工作日（例如：5天），每天8节课
        int days = 5;
        int periodsPerDay = 8;
        // 保存已排课记录，用于冲突检查
        List<Timetable> scheduledEntries = new ArrayList<>();

        // 遍历每门课程进行排课（注意：实际情况一般以教学班为最小单位，此处示例以课程为单位）
        for (Course course : courses) {
            // 通过课程信息获取对应教师
            Teacher teacher = teacherMapper.getTeacherById(course.getTeacherId());
            boolean scheduled = false;

            // 遍历工作日和每日节次，寻找合适的时间段和教室
            for (int day = 1; day <= days && !scheduled; day++) {
                for (int period = 1; period <= periodsPerDay && !scheduled; period++) {
                    // 检查教师在该时间段是否有冲突
                    if (!isTeacherAvailable(teacher, day, period, scheduledEntries)) {
                        continue;
                    }

                    // 检查是否存在合适的教室（例如：班级人数小于教室容量，且满足禁排设置等条件）
                    Classroom availableClassroom = findAvailableClassroom(classrooms, day, period, scheduledEntries);
                    if (availableClassroom == null) {
                        continue;
                    }

                    // 模拟获取具体的上课时间（实际项目中可根据学期开始日期和节次计算出具体时间）
                    Date scheduleTime = simulateScheduleTime(day, period);

                    // 构造排课记录
                    Timetable timetable = new Timetable();
                    //timetable.setCourseId(course.getId());
                    timetable.setTeacherId(teacher.getId());
                    timetable.setClassroomId(availableClassroom.getId());
                    timetable.setScheduleTime(scheduleTime);

                    // 保存到数据库（此处直接调用 mapper，实际中建议添加事务控制）
                    timetableMapper.insertTimetable(timetable);
                    scheduledEntries.add(timetable);
                    scheduled = true;
                }
            }
            if (!scheduled) {
                // 如未能排上课，可记录原因，方便后续提示或人工干预
                System.out.println("Course " + course.getCourseName() + " could not be scheduled.");
            }
        }
    }

    /**
     * 手工排课接口
     * 前端传入调整后的排课记录，可在此进行必要的校验后插入或更新数据
     *
     * @param timetable 前端传入的排课记录
     * @return 受影响的记录数
     */
    public int manualSchedule(Timetable timetable) {
        // 此处可增加校验、冲突检测等逻辑
        return timetableMapper.insertTimetable(timetable);
    }

    /**
     * 检查教师在给定时间段是否有空闲
     * <p>
     * 实际项目中需查询教师的已有排课记录，并考虑教师每天/每周最大节数限制，
     * 这里简单模拟返回 true。
     *
     * @param teacher 教师对象
     * @param day     第几天（1~days）
     * @param period  当天的第几节课
     * @param scheduleEntries 当前已排课记录
     * @return true 表示教师在该时间段可用
     */
    private boolean isTeacherAvailable(Teacher teacher, int day, int period, List<Timetable> scheduleEntries) {
        // 伪代码：根据教师已有排课记录和限制条件进行判断
        // 例如：如果同一天教师已经排满最大节数，则返回 false
        // 这里简化处理，默认教师可用
        return true;
    }

    /**
     * 查找在给定时间段内可用且满足容量要求的教室
     * <p>
     * 实际项目中需检查该时间段内是否已有课程安排、教室禁排设置等，
     * 这里简单返回第一个满足条件的教室。
     *
     * @param classrooms      教室列表
     * @param day             第几天
     * @param period          当天的第几节课
     * @param scheduleEntries 当前已排课记录
     * @return 可用的教室对象；如果没有，返回 null
     */
    private Classroom findAvailableClassroom(List<Classroom> classrooms, int day, int period, List<Timetable> scheduleEntries) {
        // 伪代码：检查每个教室在该时间段内是否已被占用，并满足班级人数与教室容量匹配等条件
        // 此处简单返回列表中的第一个
        if (!classrooms.isEmpty()) {
            return classrooms.get(0);
        }
        return null;
    }

    /**
     * 模拟根据天数和节次计算上课时间
     * <p>
     * 实际项目中应根据学期开始日期和节次定义计算具体的 Date 值，
     * 这里直接返回当前时间作为示例。
     *
     * @param day    第几天
     * @param period 当天的第几节课
     * @return 模拟的上课时间
     */
    private Date simulateScheduleTime(int day, int period) {
        return new Date();
    }
}
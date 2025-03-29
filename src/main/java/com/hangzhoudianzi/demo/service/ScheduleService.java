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

import java.util.*;
import java.util.stream.Collectors;

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

    // 遗传算法参数
    private static final int POPULATION_SIZE = 50;       // 种群大小
    private static final int MAX_GENERATIONS = 100;      // 最大迭代次数
    private static final double CROSSOVER_RATE = 0.8;    // 交叉率
    private static final double MUTATION_RATE = 0.1;     // 变异率
    private static final int TOURNAMENT_SIZE = 5;        // 锦标赛选择大小
    private static final int ELITE_COUNT = 5;            // 精英保留数量

    // 排课约束参数
    private static final int DAYS = 5;                   // 教学天数（一周）
    private static final int PERIODS_PER_DAY = 8;        // 每天节次数
    
    /**
     * 染色体类，表示一个完整的排课方案
     */
    class Chromosome {
        // 基因序列，每个元素代表一个课程的安排，包含：[课程ID, 教师ID, 教室ID, 星期几, 第几节课]
        // 注意: 前三个元素(courseId, teacherId, classroomId)是字符串类型
        List<Object[]> genes;
        // 适应度分数
        double fitness;
        
        public Chromosome() {
            this.genes = new ArrayList<>();
            this.fitness = 0.0;
        }
        
        public Chromosome(List<Object[]> genes) {
            this.genes = genes;
            this.fitness = 0.0;
        }
        
        // 深拷贝构造函数
        public Chromosome(Chromosome other) {
            this.genes = new ArrayList<>();
            for (Object[] gene : other.genes) {
                this.genes.add(gene.clone());
            }
            this.fitness = other.fitness;
        }
    }

    /**
     * 自动排课入口方法 - 使用遗传算法
     * <p>
     * 实现基于遗传算法的课程表优化：
     * 1. 初始化种群（生成多个随机排课方案）
     * 2. 评估每个排课方案的适应度（根据约束条件计算冲突）
     * 3. 进行选择、交叉、变异操作，产生新一代种群
     * 4. 重复步骤2-3直到满足终止条件
     * 5. 选取最佳排课方案并保存
     */
    public void autoSchedule() {
        // 获取数据
        List<Course> courses = courseService.list();
        List<Teacher> teachers = teacherService.list();
        List<Classroom> classrooms = classroomService.list();
        
        if (courses.isEmpty() || teachers.isEmpty() || classrooms.isEmpty()) {
            System.out.println("缺少排课所需数据，无法进行自动排课");
            return;
        }
        
        // 初始化种群
        List<Chromosome> population = initializePopulation(courses, teachers, classrooms);
        
        // 评估初始种群适应度
        evaluatePopulation(population, courses, teachers, classrooms);
        
        // 开始进化
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            // 创建新一代种群
            List<Chromosome> newPopulation = new ArrayList<>();
            
            // 精英保留
            List<Chromosome> sortedPopulation = population.stream()
                    .sorted(Comparator.comparingDouble(c -> -c.fitness))
                    .collect(Collectors.toList());
            
            for (int i = 0; i < ELITE_COUNT && i < sortedPopulation.size(); i++) {
                newPopulation.add(new Chromosome(sortedPopulation.get(i)));
            }
            
            // 生成新个体直到填满新种群
            while (newPopulation.size() < POPULATION_SIZE) {
                // 选择父代
                Chromosome parent1 = tournamentSelection(population);
                Chromosome parent2 = tournamentSelection(population);
                
                // 交叉
                Chromosome child1 = new Chromosome();
                Chromosome child2 = new Chromosome();
                
                if (Math.random() < CROSSOVER_RATE) {
                    crossover(parent1, parent2, child1, child2);
                } else {
                    child1 = new Chromosome(parent1);
                    child2 = new Chromosome(parent2);
                }
                
                // 变异
                mutate(child1, courses, teachers, classrooms);
                mutate(child2, courses, teachers, classrooms);
                
                // 添加到新种群
                newPopulation.add(child1);
                if (newPopulation.size() < POPULATION_SIZE) {
                    newPopulation.add(child2);
                }
            }
            
            // 评估新种群
            evaluatePopulation(newPopulation, courses, teachers, classrooms);
            
            // 更新种群
            population = newPopulation;
            
            // 输出当前代最佳适应度
            double bestFitness = population.stream()
                    .mapToDouble(c -> c.fitness)
                    .max()
                    .orElse(0.0);
            
            System.out.println("第 " + (generation + 1) + " 代，最佳适应度: " + bestFitness);
            
            // 如果达到完美适应度，提前结束
            if (Math.abs(bestFitness - 1.0) < 0.001) {
                System.out.println("找到最优解，提前结束进化");
                break;
            }
        }
        
        // 选取最佳染色体
        Chromosome bestChromosome = population.stream()
                .max(Comparator.comparingDouble(c -> c.fitness))
                .orElse(null);
        
        if (bestChromosome != null) {
            // 将最佳染色体转换为实际的排课表并保存到数据库
            saveScheduleToDatabase(bestChromosome, courses, teachers, classrooms);
        } else {
            System.out.println("未能找到有效的排课方案");
        }
    }

    /**
     * 初始化种群
     * 
     * @param courses 所有课程
     * @param teachers 所有教师
     * @param classrooms 所有教室
     * @return 初始化的种群
     */
    private List<Chromosome> initializePopulation(List<Course> courses, List<Teacher> teachers, List<Classroom> classrooms) {
        List<Chromosome> population = new ArrayList<>();
        
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chromosome = generateRandomChromosome(courses, teachers, classrooms);
            population.add(chromosome);
        }
        
        return population;
    }
    
    /**
     * 生成随机染色体（一个完整的排课方案）
     */
    private Chromosome generateRandomChromosome(List<Course> courses, List<Teacher> teachers, List<Classroom> classrooms) {
        Random random = new Random();
        Chromosome chromosome = new Chromosome();
        
        for (Course course : courses) {
            String courseId = course.getId();
            String teacherId = String.valueOf(course.getTeacherId());
            
            // 随机选择教室、星期和节次
            String classroomId = classrooms.get(random.nextInt(classrooms.size())).getId();
            int day = random.nextInt(DAYS) + 1;
            int period = random.nextInt(PERIODS_PER_DAY) + 1;
            
            // 添加基因 [课程ID, 教师ID, 教室ID, 星期几, 第几节课]
            chromosome.genes.add(new Object[]{courseId, teacherId, classroomId, day, period});
        }
        
        return chromosome;
    }

    /**
     * 评估整个种群的适应度
     */
    private void evaluatePopulation(List<Chromosome> population, List<Course> courses, List<Teacher> teachers, List<Classroom> classrooms) {
        for (Chromosome chromosome : population) {
            chromosome.fitness = calculateFitness(chromosome, courses, teachers, classrooms);
        }
    }
    
    /**
     * 计算染色体的适应度
     * 适应度越高表示排课方案越好（冲突越少）
     */
    private double calculateFitness(Chromosome chromosome, List<Course> courses, List<Teacher> teachers, List<Classroom> classrooms) {
        int conflictCount = 0;
        
        // 用于检测冲突的映射
        Map<String, List<Object[]>> teacherTimeMap = new HashMap<>();  // 教师-时间冲突
        Map<String, List<Object[]>> classroomTimeMap = new HashMap<>(); // 教室-时间冲突
        
        // 检查每个基因（课程安排）
        for (Object[] gene : chromosome.genes) {
            String teacherId = (String) gene[1];
            String classroomId = (String) gene[2];
            int day = (int) gene[3];
            int period = (int) gene[4];
            
            // 检查教师时间冲突
            String teacherTimeKey = teacherId + "-" + day + "-" + period;
            if (!teacherTimeMap.containsKey(teacherTimeKey)) {
                teacherTimeMap.put(teacherTimeKey, new ArrayList<>());
            }
            teacherTimeMap.get(teacherTimeKey).add(gene);
            
            // 检查教室时间冲突
            String classroomTimeKey = classroomId + "-" + day + "-" + period;
            if (!classroomTimeMap.containsKey(classroomTimeKey)) {
                classroomTimeMap.put(classroomTimeKey, new ArrayList<>());
            }
            classroomTimeMap.get(classroomTimeKey).add(gene);
        }
        
        // 统计冲突数
        for (List<Object[]> classes : teacherTimeMap.values()) {
            if (classes.size() > 1) {
                conflictCount += classes.size() - 1;
            }
        }
        
        for (List<Object[]> classes : classroomTimeMap.values()) {
            if (classes.size() > 1) {
                conflictCount += classes.size() - 1;
            }
        }
        
        // 其他约束条件的检查（如教师每日最大课时、连排要求等）可以在此添加
        
        // 计算适应度，与冲突数成反比
        double fitness = 1.0 / (1.0 + conflictCount);
        return fitness;
    }
    
    /**
     * 锦标赛选择法
     * 从种群中随机选取一定数量的个体，然后返回其中适应度最高的个体
     */
    private Chromosome tournamentSelection(List<Chromosome> population) {
        Random random = new Random();
        List<Chromosome> tournament = new ArrayList<>();
        
        // 随机选择 TOURNAMENT_SIZE 个个体进入锦标赛
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int randomIndex = random.nextInt(population.size());
            tournament.add(population.get(randomIndex));
        }
        
        // 返回锦标赛中适应度最高的个体
        return tournament.stream()
                .max(Comparator.comparingDouble(c -> c.fitness))
                .orElse(population.get(0));
    }
    
    /**
     * 交叉操作
     * 将两个父代染色体的部分基因交换，生成两个子代染色体
     */
    private void crossover(Chromosome parent1, Chromosome parent2, Chromosome child1, Chromosome child2) {
        Random random = new Random();
        int geneLength = parent1.genes.size();
        
        if (geneLength == 0) {
            child1 = new Chromosome(parent1);
            child2 = new Chromosome(parent2);
            return;
        }
        
        // 随机选择交叉点
        int crossoverPoint = random.nextInt(geneLength);
        
        // 创建子代
        child1.genes = new ArrayList<>();
        child2.genes = new ArrayList<>();
        
        // 交叉前半部分
        for (int i = 0; i < crossoverPoint; i++) {
            child1.genes.add(parent1.genes.get(i).clone());
            child2.genes.add(parent2.genes.get(i).clone());
        }
        
        // 交叉后半部分
        for (int i = crossoverPoint; i < geneLength; i++) {
            child1.genes.add(parent2.genes.get(i).clone());
            child2.genes.add(parent1.genes.get(i).clone());
        }
    }
    
    /**
     * 变异操作
     * 随机修改染色体中的某些基因，以增加种群多样性
     */
    private void mutate(Chromosome chromosome, List<Course> courses, List<Teacher> teachers, List<Classroom> classrooms) {
        Random random = new Random();
        
        for (int i = 0; i < chromosome.genes.size(); i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                Object[] gene = chromosome.genes.get(i);
                
                // 随机决定变异什么属性（教室、星期、节次）
                int mutationType = random.nextInt(3);
                
                switch (mutationType) {
                    case 0: // 变异教室
                        if (!classrooms.isEmpty()) {
                            gene[2] = classrooms.get(random.nextInt(classrooms.size())).getId();
                        }
                        break;
                    case 1: // 变异星期
                        gene[3] = random.nextInt(DAYS) + 1;
                        break;
                    case 2: // 变异节次
                        gene[4] = random.nextInt(PERIODS_PER_DAY) + 1;
                        break;
                }
            }
        }
    }
    
    /**
     * 将最佳染色体保存到数据库
     */
    private void saveScheduleToDatabase(Chromosome chromosome, List<Course> courses, List<Teacher> teachers, List<Classroom> classrooms) {
        // 清空之前的排课记录
        // 实际项目中可能需要事务控制
        // timetableMapper.deleteAllTimetables();
        
        // 保存新的排课方案
        for (Object[] gene : chromosome.genes) {
            String courseId = (String) gene[0];
            String teacherId = (String) gene[1];
            String classroomId = (String) gene[2];
            int day = (int) gene[3];
            int period = (int) gene[4];
            
            // 计算具体日期和时间
            Date scheduleTime = calculateScheduleTime(day, period);
            
            // 创建并保存时间表记录
            Timetable timetable = new Timetable();
            timetable.setCourseId(courseId);
            timetable.setTeacherId(teacherId);
            timetable.setClassroomId(classroomId);
            timetable.setScheduleTime(scheduleTime);
            
            timetableMapper.insertTimetable(timetable);
        }
        
        System.out.println("排课方案已保存到数据库");
    }
    
    /**
     * 根据星期几和第几节课计算具体的日期时间
     */
    private Date calculateScheduleTime(int day, int period) {
        // 这里可以根据学期开始时间、作息时间等计算具体的日期和时间
        // 简化版本，返回当前时间
        return new Date();
    }

    /**
     * 手工排课接口
     * 前端传入调整后的排课记录，可在此进行必要的校验后插入或更新数据
     *
     * @param timetable 前端传入的排课记录
     * @return 受影响的记录数
     */
    public int manualSchedule(Timetable timetable) {
        // 参数校验
        if (timetable == null || timetable.getCourseId() == null || 
            timetable.getTeacherId() == null || timetable.getClassroomId() == null || 
            timetable.getScheduleTime() == null) {
            throw new IllegalArgumentException("排课信息不完整，请检查课程、教师、教室和时间信息");
        }
        
        // 获取当前已有排课记录
        List<Timetable> existingTimetables = timetableMapper.getAllTimetables();
        
        // 提取时间信息
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(timetable.getScheduleTime());
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 转换为1-7的星期
        if (day == 0) day = 7; // 把周日（0）调整为7
        
        // 假设一天上课时间从早8点开始，每节课1小时
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int period = hour - 8 + 1; // 转换为第几节课
        
        // 检查教师冲突
        for (Timetable existing : existingTimetables) {
            if (timetable.getId() != null && timetable.getId().equals(existing.getId())) {
                continue; // 跳过自身（更新情况）
            }
            
            Calendar existingCalendar = Calendar.getInstance();
            existingCalendar.setTime(existing.getScheduleTime());
            int existingDay = existingCalendar.get(Calendar.DAY_OF_WEEK) - 1;
            if (existingDay == 0) existingDay = 7;
            int existingHour = existingCalendar.get(Calendar.HOUR_OF_DAY);
            int existingPeriod = existingHour - 8 + 1;
            
            // 检查教师时间冲突
            if (existing.getTeacherId().equals(timetable.getTeacherId()) && 
                existingDay == day && existingPeriod == period) {
                throw new RuntimeException("教师 " + timetable.getTeacherId() + 
                                         " 在所选时间段已有其他课程安排");
            }
            
            // 检查教室时间冲突
            if (existing.getClassroomId().equals(timetable.getClassroomId()) && 
                existingDay == day && existingPeriod == period) {
                throw new RuntimeException("教室 " + timetable.getClassroomId() + 
                                         " 在所选时间段已被占用");
            }
        }
        
        // 检查教室容量限制
        Classroom classroom = classroomMapper.getClassroomById(timetable.getClassroomId());
        Course course = null;
        
        // 根据courseId找到对应课程
        // 由于getCourseById需要更多参数，这里调整为通过list获取
        List<Course> courses = courseService.list();
        for (Course c : courses) {
            if (c.getId().equals(String.valueOf(timetable.getCourseId()))) {
                course = c;
                break;
            }
        }
        
        // 假设课程有学生人数信息，进行容量检查
        // 这里简化处理，实际可能需要从班级或选课信息中获取实际人数
        int studentCount = estimateStudentCount(course);
        if (classroom != null && classroom.getCapacity() < studentCount) {
            throw new RuntimeException("教室容量不足，无法容纳该课程的学生人数");
        }
        
        // 教师资质检查
        Teacher teacher = teacherMapper.getTeacherById(timetable.getTeacherId());
        if (teacher != null && course != null && !isTeacherQualified(teacher, course)) {
            throw new RuntimeException("该教师可能不具备教授此课程的资质，请确认");
        }
        
        // 检查是否超出教师每日/每周最大课时
        if (isExceedingTeacherMaxHours(teacher, timetable, existingTimetables)) {
            throw new RuntimeException("此安排将超出教师最大课时限制");
        }
        
        // 所有检查通过，保存数据
        if (timetable.getId() != null) {
            // 更新现有记录
            // 由于没有updateTimetable方法，先删除后插入来模拟更新
            // 实际项目中应实现真正的更新方法
            // timetableMapper.deleteTimetableById(timetable.getId());
            return timetableMapper.insertTimetable(timetable);
        } else {
            // 插入新记录
            return timetableMapper.insertTimetable(timetable);
        }
    }
    
    /**
     * 估算课程学生人数
     * 实际项目中应从选课记录或班级信息中获取
     */
    private int estimateStudentCount(Course course) {
        // 这里简化处理，可以基于课程类型、学分等进行粗略估计
        // 实际应用中应该从选课系统获取准确人数
        return 30; // 默认返回30人
    }
    
    /**
     * 检查教师是否有资格教授特定课程
     */
    private boolean isTeacherQualified(Teacher teacher, Course course) {
        // 实际项目中可能需要检查教师专业、职称等信息
        // 这里简化处理，假设所有教师都有资格
        return true;
    }
    
    /**
     * 检查是否超出教师最大课时限制
     */
    private boolean isExceedingTeacherMaxHours(Teacher teacher, Timetable newTimetable, 
                                             List<Timetable> existingTimetables) {
        // 设定教师每周最大课时
        final int MAX_WEEKLY_HOURS = 16;
        // 设定教师每天最大课时
        final int MAX_DAILY_HOURS = 4;
        
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.setTime(newTimetable.getScheduleTime());
        int newDay = newCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (newDay == 0) newDay = 7;
        
        int weeklyHours = 0;
        int dailyHours = 0;
        
        // 统计现有课时
        for (Timetable existing : existingTimetables) {
            if (existing.getTeacherId().equals(newTimetable.getTeacherId())) {
                weeklyHours++;
                
                Calendar existingCalendar = Calendar.getInstance();
                existingCalendar.setTime(existing.getScheduleTime());
                int existingDay = existingCalendar.get(Calendar.DAY_OF_WEEK) - 1;
                if (existingDay == 0) existingDay = 7;
                
                if (existingDay == newDay) {
                    dailyHours++;
                }
            }
        }
        
        // 如果是更新操作，可能需要排除当前记录本身
        if (newTimetable.getId() != null) {
            for (Timetable existing : existingTimetables) {
                if (existing.getId().equals(newTimetable.getId())) {
                    weeklyHours--;
                    
                    Calendar existingCalendar = Calendar.getInstance();
                    existingCalendar.setTime(existing.getScheduleTime());
                    int existingDay = existingCalendar.get(Calendar.DAY_OF_WEEK) - 1;
                    if (existingDay == 0) existingDay = 7;
                    
                    if (existingDay == newDay) {
                        dailyHours--;
                    }
                    
                    break;
                }
            }
        }
        
        // 加上新安排的课时
        weeklyHours++;
        dailyHours++;
        
        return weeklyHours > MAX_WEEKLY_HOURS || dailyHours > MAX_DAILY_HOURS;
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
        if (teacher == null) {
            return false;
        }
        
        // 定义教师每天最大节数
        final int MAX_PERIODS_PER_DAY = 4;
        
        // 统计当前教师在指定日期已排课的节数
        int teacherDailyPeriods = 0;
        
        // 检查教师在该时间段是否已有课程安排
        for (Timetable entry : scheduleEntries) {
            if (entry.getTeacherId().equals(teacher.getId())) {
                // 提取时间信息
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(entry.getScheduleTime());
                int entryDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                if (entryDay == 0) entryDay = 7; // 把周日（0）调整为7
                
                int entryHour = calendar.get(Calendar.HOUR_OF_DAY);
                int entryPeriod = entryHour - 8 + 1; // 假设从8点开始上课
                
                // 检查是否在同一天
                if (entryDay == day) {
                    teacherDailyPeriods++;
                    
                    // 检查是否在同一时间段
                    if (entryPeriod == period) {
                        return false; // 已在该时间段有课
                    }
                }
            }
        }
        
        // 检查是否超出每日最大节数限制
        if (teacherDailyPeriods >= MAX_PERIODS_PER_DAY) {
            return false;
        }
        
        // 可以增加教师偏好检查，例如某教师不希望在某个时间段上课
        // 可以增加教师不可用时间段检查（例如行政工作、会议时间等）
        
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
        if (classrooms == null || classrooms.isEmpty()) {
            return null;
        }
        
        // 创建已占用教室ID集合，使用String类型
        Set<String> occupiedClassrooms = new HashSet<>();
        
        // 找出在指定时间段已被占用的教室
        for (Timetable entry : scheduleEntries) {
            // 提取时间信息
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(entry.getScheduleTime());
            int entryDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            if (entryDay == 0) entryDay = 7; // 调整周日
            
            int entryHour = calendar.get(Calendar.HOUR_OF_DAY);
            int entryPeriod = entryHour - 8 + 1; // 假设从8点开始上课
            
            // 如果在同一天同一时间段
            if (entryDay == day && entryPeriod == period) {
                occupiedClassrooms.add(entry.getClassroomId());
            }
        }
        
        // 检查每个教室是否可用
        for (Classroom classroom : classrooms) {
            // 如果教室未被占用
            if (!occupiedClassrooms.contains(classroom.getId())) {
                // 可以添加更多条件检查，如教室容量、设备要求等
                // 例如根据课程类型判断是否需要特殊设备（实验室、多媒体等）
                
                // 这里简单返回第一个未被占用的教室
                return classroom;
            }
        }
        
        return null; // 没有找到可用教室
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
        // 假设学期开始日期为当前日期
        Calendar calendar = Calendar.getInstance();
        
        // 设置为学期第一周的周一
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = currentDayOfWeek - Calendar.MONDAY;
        if (daysToSubtract < 0) daysToSubtract += 7;
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract);
        
        // 调整到指定的天（1-7对应周一到周日）
        calendar.add(Calendar.DAY_OF_YEAR, day - 1);
        
        // 设置时间为第period节课的开始时间
        // 假设第一节课从8:00开始，每节课50分钟，每节课间隔10分钟
        int startHour = 8;
        int minutesPerPeriod = 50;
        int breakMinutes = 10;
        
        int totalMinutes = (period - 1) * (minutesPerPeriod + breakMinutes);
        int hour = startHour + totalMinutes / 60;
        int minute = totalMinutes % 60;
        
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        return calendar.getTime();
    }
}
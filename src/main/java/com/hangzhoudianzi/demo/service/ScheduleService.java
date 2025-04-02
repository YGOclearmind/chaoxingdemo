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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private static final int POPULATION_SIZE = 30;       // 从50减少到30
    private static final int MAX_GENERATIONS = 50;      // 从100减少到50
    private static final double CROSSOVER_RATE = 0.8;    
    private static final double MUTATION_RATE = 0.2;     // 从0.1增加到0.2提高变异率加快收敛
    private static final int TOURNAMENT_SIZE = 3;        // 从5减少到3
    private static final int ELITE_COUNT = 2;            // 从5减少到2

    // 排课约束参数
    private static final int DAYS = 5;                   // 教学天数（一周）
    private static final int PERIODS_PER_DAY = 8;        // 每天节次数
    private static final int MAX_WEEKLY_RECORDS = 15;    // 每周最大排课记录数，增加到50节
    private static final int MAX_COURSES_PER_TEACHER = 3; // 每位教师最多教5门不同课程
    private static final int MAX_SAME_COURSE_PER_TEACHER = 2; // 每位教师同一门课程最多上2次
    
    /**
     * 染色体类，表示一个完整的排课方案
     */
    class Chromosome {
        // 基因序列，每个元素代表一个课程的安排
        // [课程ID, 教师ID, 教室ID, 星期几, 第几节课, 班级ID]
        List<Object[]> genes;
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
    public void autoSchedule(Integer classId) {
        System.out.println("======== 开始为" + classId + "班级排课 ========");
        
        // 获取数据
        List<Course> courses = courseService.list();
        List<Teacher> teachers = teacherService.list();
        List<Classroom> classrooms = classroomService.list();

        if (courses.isEmpty() || teachers.isEmpty() || classrooms.isEmpty()) {
            System.out.println("错误：缺少排课所需数据，无法进行自动排课");
            return;
        }
        
        // 不再限制课程数量，让所有课程都有机会被安排
        System.out.println("开始排课，处理 " + courses.size() + " 门课程，" + 
                         teachers.size() + " 名教师，" + 
                         classrooms.size() + " 间教室");
        
        // 简化参数，加快收敛速度
        int limitedGenerations = Math.min(20, MAX_GENERATIONS); // 最多迭代20代
        double targetFitness = 0.8; // 降低目标适应度阈值
        double previousBestFitness = 0.0;
        int noImprovementCount = 0;
        
        // 初始化种群
        List<Chromosome> population = initializePopulation(courses, teachers, classrooms, classId);
        
        // 评估初始种群适应度
        evaluatePopulation(population, courses, teachers, classrooms);
        
        // 开始进化
        for (int generation = 0; generation < limitedGenerations; generation++) {
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
            
            // 获取当前代最佳适应度
            double bestFitness = population.stream()
                    .mapToDouble(c -> c.fitness)
                    .max()
                    .orElse(0.0);
            
            System.out.println("第 " + (generation + 1) + "/" + limitedGenerations + " 代，最佳适应度: " + bestFitness);
            
            // 检查适应度改善情况
            if (Math.abs(bestFitness - previousBestFitness) < 0.001) {
                noImprovementCount++;
                if (noImprovementCount >= 3) {  // 连续3代无改善则提前终止
                    System.out.println("连续3代无明显改善，提前终止进化");
                    break;
                }
            } else {
                noImprovementCount = 0;
                previousBestFitness = bestFitness;
            }
            
            // 如果达到可接受的适应度就提前终止
            if (bestFitness >= 0.85) {
                System.out.println("达到可接受的适应度阈值，提前终止进化");
                break;
            }
        }
        
        // 选取最佳染色体
        Chromosome bestChromosome = population.stream()
                .max(Comparator.comparingDouble(c -> c.fitness))
                .orElse(null);
        
        if (bestChromosome != null) {
            // 评估最终方案
            double finalFitness = bestChromosome.fitness;
            System.out.println("排课算法完成，最终适应度: " + finalFitness);
            
            if (finalFitness < 0.5) {
                System.out.println("警告: 最终课表冲突较多，可能不够合理，建议手动调整");
            }
            
            // 保存到数据库时传入classId
            saveScheduleToDatabase(bestChromosome, courses, teachers, classrooms, classId);
        } else {
            System.out.println("未能找到有效的排课方案");
        }
        
        System.out.println("======== 自动排课结束 ========");
    }

    /**
     * 初始化种群
     * 
     * @param courses 所有课程
     * @param teachers 所有教师
     * @param classrooms 所有教室
     * @param classId 班级ID
     * @return 初始化的种群
     */
    private List<Chromosome> initializePopulation(List<Course> courses, List<Teacher> teachers, 
                                                List<Classroom> classrooms, int classId) {
        List<Chromosome> population = new ArrayList<>();
        
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chromosome = generateRandomChromosome(courses, teachers, classrooms, classId);
            population.add(chromosome);
        }
        
        return population;
    }
    
    /**
     * 生成随机染色体（一个完整的排课方案）
     */
    private Chromosome generateRandomChromosome(List<Course> courses, List<Teacher> teachers, 
                                          List<Classroom> classrooms, int classId) {
        Random random = new Random();
        Chromosome chromosome = new Chromosome();
        
        // 创建可用时间槽
        List<int[]> availableTimeSlots = new ArrayList<>();
        for (int day = 1; day <= DAYS; day++) {
            for (int period = 1; period <= PERIODS_PER_DAY; period++) {
                availableTimeSlots.add(new int[]{day, period});
            }
        }
        Collections.shuffle(availableTimeSlots);  // 随机打乱时间槽
        
        // 使用Set来跟踪已经安排的courseId
        Set<String> assignedCourseIds = new HashSet<>();
        
        // 为每个课程分配时间槽
        int timeSlotIndex = 0;
        for (Course course : courses) {
            // 如果这个courseId已经被安排过，跳过
            if (assignedCourseIds.contains(course.getId())) {
                continue;
            }
            
            if (timeSlotIndex >= availableTimeSlots.size()) break;
            
            int[] timeSlot = availableTimeSlots.get(timeSlotIndex++);
            
            // 找到最合适的教师
            String teacherId = findBestTeacher(course, teachers, classId);
            if (teacherId == null && !teachers.isEmpty()) {
                teacherId = teachers.get(random.nextInt(teachers.size())).getId();
            }
            
            String classroomId = classrooms.get(random.nextInt(classrooms.size())).getId();
            
            // 添加基因并记录已使用的courseId
            chromosome.genes.add(new Object[]{
                course.getId(), teacherId, classroomId, 
                timeSlot[0], timeSlot[1], classId
            });
            
            assignedCourseIds.add(course.getId());
        }
        
        return chromosome;
    }
    
    /**
     * 解析字符串为整数，出错时返回默认值
     */
    private int parseIntSafely(String str, int defaultValue) {
        if (str == null || str.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 为课程找到最合适的教师
     */
    private String findBestTeacher(Course course, List<Teacher> teachers, int classId) {
        if (teachers.isEmpty()) {
            return null;
        }
        
        String courseName = course.getCourseName();
        String originalTeacherId = course.getTeacherId();
        
        // 候选教师评分
        Map<String, Integer> teacherScores = new HashMap<>();
        
        // 创建一个临时的染色体来检查当前排课情况
        Chromosome tempChromosome = new Chromosome();
        List<Timetable> existingTimetables = timetableMapper.getTimetablesByClassId(classId);
        
        // 将现有课表转换为染色体格式
        for (Timetable timetable : existingTimetables) {
            Object[] gene = new Object[]{
                timetable.getCourseId(),
                timetable.getTeacherId(),
                timetable.getClassroomId(),
                timetable.getDayOfWeek(),
                Integer.parseInt(timetable.getPeriodInfo().split(",")[0]),
                timetable.getClassId()
            };
            tempChromosome.genes.add(gene);
        }
        
        for (Teacher teacher : teachers) {
            String teacherId = teacher.getId();
            int score = 100; // 基础分
            
            // 如果是原课程指定的教师，加分
            if (teacherId.equals(originalTeacherId)) {
                score += 50;
            }
            
            // 检查教师课程数量，数量越多分数越低
            int courseCount = 0;
            for (Object[] gene : tempChromosome.genes) {
                if (gene[1].equals(teacherId)) {
                    courseCount++;
                }
            }
            score -= courseCount * 10;
            
            // 检查教师是否已分配相同名称的课程，如果是则加分（同一老师教同名课程好）
            Set<String> assignedCourseNames = new HashSet<>();
            for (Object[] gene : tempChromosome.genes) {
                if (gene[1].equals(teacherId)) {
                    String assignedCourseId = (String) gene[0];
                    Course assignedCourse = courseService.getCourseById(assignedCourseId);
                    if (assignedCourse != null && assignedCourse.getCourseName().equals(courseName)) {
                        assignedCourseNames.add(courseName);
                    }
                }
            }
            
            if (!assignedCourseNames.isEmpty()) {
                score += 30;
            } else if (assignedCourseNames.size() > 0) {
                // 如果教师已有其他课程，略微减分，鼓励教师专注于少数几种课程
                score -= assignedCourseNames.size() * 5;
            }
            
            teacherScores.put(teacherId, score);
        }
        
        // 找出得分最高的教师
        return teacherScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
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
    private double calculateFitness(Chromosome chromosome, List<Course> courses, 
                              List<Teacher> teachers, List<Classroom> classrooms) {
        // 使用更高效的数据结构
        Set<String> timeSlots = new HashSet<>();  // 用于快速检查时间冲突
        Map<String, Integer> teacherLoad = new HashMap<>();  // 教师课程负载
        Set<String> courseIds = new HashSet<>();  // 用于检查课程ID重复
        
        int conflictCount = 0;
        
        for (Object[] gene : chromosome.genes) {
            String courseId = (String) gene[0];
            String teacherId = (String) gene[1];
            int day = (int) gene[3];
            int period = (int) gene[4];
            int classId = (Integer) gene[5];
            
            // 检查courseId是否重复
            String courseKey = courseId + "_" + classId;  // 组合courseId和classId
            if (!courseIds.add(courseKey)) {
                conflictCount += 10;  // 严重惩罚重复的courseId
                continue;
            }
            
            // 使用复合键检查时间冲突
            String timeSlotKey = String.format("%s_%d_%d_%d", teacherId, day, period, classId);
            if (!timeSlots.add(timeSlotKey)) {
                conflictCount += 5;
                continue;
            }
            
            // 更新教师课程负载
            teacherLoad.merge(teacherId, 1, Integer::sum);
        }
        
        // 检查教师负载是否平衡
        int avgLoad = chromosome.genes.size() / teacherLoad.size();
        for (int load : teacherLoad.values()) {
            if (Math.abs(load - avgLoad) > 2) {
                conflictCount++;
            }
        }
        
        return 1.0 / (1.0 + conflictCount);
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
        
        // 创建课程ID到课程对象的映射
        Map<String, Course> courseMap = new HashMap<>();
        for (Course course : courses) {
            courseMap.put(course.getId(), course);
        }
        
        // 统计教师课程数量
        Map<String, Integer> teacherCourseCount = new HashMap<>();
        for (Object[] gene : chromosome.genes) {
            String teacherId = (String) gene[1];
            teacherCourseCount.put(teacherId, teacherCourseCount.getOrDefault(teacherId, 0) + 1);
        }
        
        // 找出课程数最多的教师和最少的教师
        String maxTeacherId = null;
        String minTeacherId = null;
        int maxCount = -1;
        int minCount = Integer.MAX_VALUE;
        
        for (Map.Entry<String, Integer> entry : teacherCourseCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                maxTeacherId = entry.getKey();
            }
            if (entry.getValue() < minCount) {
                minCount = entry.getValue();
                minTeacherId = entry.getKey();
            }
        }
        
        // 如果不平衡度高，提高变异概率
        double currentMutationRate = MUTATION_RATE;
        if (maxCount > 0 && minCount < maxCount / 2) {
            currentMutationRate = MUTATION_RATE * 1.5;
        }
        
        // 在变异时检查课程名称
        Map<Integer, Map<String, Integer>> classCourseCounts = new HashMap<>();
        
        // 统计当前课程分布
        for (Object[] gene : chromosome.genes) {
            String courseId = (String) gene[0];
            int classId = (Integer) gene[5];
            
            Course course = courseMap.get(courseId);
            if (course == null) continue;
            
            String courseName = course.getCourseName();
            
            classCourseCounts.computeIfAbsent(classId, k -> new HashMap<>())
                            .merge(courseName, 1, Integer::sum);
        }
        
        // 在变异时避免生成过多相同课程
        for (int i = 0; i < chromosome.genes.size(); i++) {
            if (random.nextDouble() < currentMutationRate) {
                Object[] gene = chromosome.genes.get(i);
                String courseId = (String) gene[0];
                int classId = (Integer) gene[5];
                
                Course course = courseMap.get(courseId);
                if (course == null) continue;
                
                String courseName = course.getCourseName();
                
                // 如果当前课程在该班级出现次数过多，尝试替换为其他课程
                if (classCourseCounts.get(classId).getOrDefault(courseName, 0) > 2) {
                    // 寻找出现次数较少的其他课程进行替换
                    for (Course otherCourse : courses) {
                        String otherCourseName = otherCourse.getCourseName();
                        if (!otherCourseName.equals(courseName) &&
                            classCourseCounts.get(classId).getOrDefault(otherCourseName, 0) < 2) {
                            gene[0] = otherCourse.getId();
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 将最佳染色体保存到数据库
     */
    private void saveScheduleToDatabase(Chromosome chromosome, List<Course> courses, 
                                  List<Teacher> teachers, List<Classroom> classrooms, 
                                  Integer classId) {
        System.out.println("开始保存排课结果到数据库，班级ID: " + classId);
        
        try {
            // 先删除该班级的旧数据
            timetableMapper.deleteByClassId(classId);
            
            // 用于检查courseId重复
            Set<String> usedCourseIds = new HashSet<>();
            
            // 限制处理的记录数
            int maxRecords = Math.min(MAX_WEEKLY_RECORDS, chromosome.genes.size());
            System.out.println("准备插入 " + maxRecords + " 条记录");
            
            int successCount = 0;
            int failCount = 0;
            
            for (int i = 0; i < maxRecords && i < chromosome.genes.size(); i++) {
                Object[] gene = chromosome.genes.get(i);
                try {
                    String courseId = (String) gene[0];
                    
                    // 检查courseId是否已经使用过
                    if (usedCourseIds.contains(courseId)) {
                        System.out.println("警告：课程ID " + courseId + " 重复，跳过此记录");
                        continue;
                    }
                    
                    String teacherId = (String) gene[1];
                    String classroomId = (String) gene[2];
                    int day = (int) gene[3];
                    int period = (int) gene[4];
                    
                    Course course = courseService.getCourseById(courseId);
                    if (course == null) {
                        System.out.println("警告：找不到ID为 " + courseId + " 的课程，跳过此记录");
                        continue;
                    }
                    
                    // 创建排课记录
                    Timetable timetable = new Timetable();
                    timetable.setCourseId(courseId);
                    timetable.setTeacherId(teacherId);
                    timetable.setClassroomId(classroomId);
                    timetable.setScheduleTime(calculateScheduleTime(day, period));
                    timetable.setDayOfWeek(day);
                    timetable.setClassId(classId);
                    
                    // 设置节次信息
                    StringBuilder periodInfoBuilder = new StringBuilder();
                    int consecutiveSections = course.getConsecutiveSections();
                    if (consecutiveSections <= 0) {
                        consecutiveSections = 1;
                    }
                    consecutiveSections = Math.min(consecutiveSections, 3);
                    
                    for (int p = period; p < period + consecutiveSections && p <= PERIODS_PER_DAY; p++) {
                        if (periodInfoBuilder.length() > 0) {
                            periodInfoBuilder.append(",");
                        }
                        periodInfoBuilder.append(p);
                    }
                    timetable.setPeriodInfo(periodInfoBuilder.toString());
                    
                    System.out.println("尝试插入第 " + (i+1) + "/" + maxRecords + " 条记录: " + 
                                     "课程=" + courseId + 
                                     ", 教师=" + teacherId + 
                                     ", 教室=" + classroomId + 
                                     ", 节次=" + period + 
                                     ", 星期=" + day +
                                     ", 班级=" + classId);
                    
                    // 执行插入操作
                    int result = timetableMapper.insertTimetable(timetable);
                    if (result > 0) {
                        successCount++;
                        System.out.println("第 " + (i+1) + " 条记录插入成功");
                    } else {
                        failCount++;
                        System.out.println("第 " + (i+1) + " 条记录插入失败");
                    }
                    
                    // 记录已使用的courseId
                    usedCourseIds.add(courseId);
                } catch (Exception e) {
                    failCount++;
                    System.out.println("插入记录失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("\n排课保存完成: 成功 " + successCount + " 条，失败 " + failCount + " 条");
        } catch (Exception e) {
            System.out.println("保存排课结果时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("保存排课结果失败", e);
        }
    }
    
    /**
     * 确保教师的课程分配均衡，预处理基因数组
     */
    private void ensureTeacherBalance(List<Object[]> genes, List<Teacher> teachers, Map<String, Course> courseMap) {
        Map<String, Map<String, Integer>> teacherCourseNameCount = new HashMap<>();
        Map<String, Set<String>> teacherCourseNames = new HashMap<>();
        Map<String, Integer> teacherTotalCourses = new HashMap<>();
        
        // 第一遍统计
        for (Object[] gene : genes) {
            String courseId = (String) gene[0];
            String teacherId = (String) gene[1];
            
            Course course = courseMap.get(courseId);
            if (course == null) continue;
            
            String courseName = course.getCourseName();
            
            // 初始化数据结构
            if (!teacherCourseNameCount.containsKey(teacherId)) {
                teacherCourseNameCount.put(teacherId, new HashMap<>());
            }
            if (!teacherCourseNames.containsKey(teacherId)) {
                teacherCourseNames.put(teacherId, new HashSet<>());
            }
            
            // 更新计数
            teacherCourseNameCount.get(teacherId).put(
                courseName, 
                teacherCourseNameCount.get(teacherId).getOrDefault(courseName, 0) + 1
            );
            
            teacherCourseNames.get(teacherId).add(courseName);
            teacherTotalCourses.put(teacherId, teacherTotalCourses.getOrDefault(teacherId, 0) + 1);
        }
        
        // 第二遍调整
        for (int i = 0; i < genes.size(); i++) {
            Object[] gene = genes.get(i);
            String courseId = (String) gene[0];
            String teacherId = (String) gene[1];
            
            Course course = courseMap.get(courseId);
            if (course == null) continue;
            
            String courseName = course.getCourseName();
            
            // 检查教师课程多样性
            if (teacherCourseNames.containsKey(teacherId) && 
                teacherCourseNames.get(teacherId).size() > MAX_COURSES_PER_TEACHER) {
                
                // 尝试找一个教师课程最少的教师
                String bestTeacherId = null;
                int minCourseCount = Integer.MAX_VALUE;
                
                for (Teacher teacher : teachers) {
                    String candidateId = teacher.getId();
                    if (candidateId.equals(teacherId)) continue;
                    
                    int courseCount = teacherTotalCourses.getOrDefault(candidateId, 0);
                    if (courseCount < minCourseCount) {
                        minCourseCount = courseCount;
                        bestTeacherId = candidateId;
                    }
                }
                
                // 如果找到了合适的教师，重新分配
                if (bestTeacherId != null) {
                    // 更新原教师统计
                    int oldCount = teacherCourseNameCount.get(teacherId).get(courseName);
                    if (oldCount <= 1) {
                        teacherCourseNameCount.get(teacherId).remove(courseName);
                        teacherCourseNames.get(teacherId).remove(courseName);
                    } else {
                        teacherCourseNameCount.get(teacherId).put(courseName, oldCount - 1);
                    }
                    teacherTotalCourses.put(teacherId, teacherTotalCourses.get(teacherId) - 1);
                    
                    // 更新新教师统计
                    if (!teacherCourseNameCount.containsKey(bestTeacherId)) {
                        teacherCourseNameCount.put(bestTeacherId, new HashMap<>());
                    }
                    if (!teacherCourseNames.containsKey(bestTeacherId)) {
                        teacherCourseNames.put(bestTeacherId, new HashSet<>());
                    }
                    
                    teacherCourseNameCount.get(bestTeacherId).put(
                        courseName, 
                        teacherCourseNameCount.get(bestTeacherId).getOrDefault(courseName, 0) + 1
                    );
                    teacherCourseNames.get(bestTeacherId).add(courseName);
                    teacherTotalCourses.put(bestTeacherId, teacherTotalCourses.getOrDefault(bestTeacherId, 0) + 1);
                    
                    // 更新基因
                    gene[1] = bestTeacherId;
                }
            }
            
            // 检查同一课程数量
            int courseCount = teacherCourseNameCount.getOrDefault(teacherId, new HashMap<>())
                                                 .getOrDefault(courseName, 0);
            if (courseCount > MAX_SAME_COURSE_PER_TEACHER) {
                // 尝试找另一个适合教这门课的教师
                String bestTeacherId = null;
                
                for (Teacher teacher : teachers) {
                    String candidateId = teacher.getId();
                    if (candidateId.equals(teacherId)) continue;
                    
                    int candidateCourseCount = teacherCourseNameCount.getOrDefault(candidateId, new HashMap<>())
                                                                 .getOrDefault(courseName, 0);
                    
                    if (candidateCourseCount < MAX_SAME_COURSE_PER_TEACHER) {
                        bestTeacherId = candidateId;
                        break;
                    }
                }
                
                if (bestTeacherId != null) {
                    // 更新统计
                    teacherCourseNameCount.get(teacherId).put(courseName, courseCount - 1);
                    
                    if (!teacherCourseNameCount.containsKey(bestTeacherId)) {
                        teacherCourseNameCount.put(bestTeacherId, new HashMap<>());
                    }
                    if (!teacherCourseNames.containsKey(bestTeacherId)) {
                        teacherCourseNames.put(bestTeacherId, new HashSet<>());
                    }
                    
                    teacherCourseNameCount.get(bestTeacherId).put(
                        courseName, 
                        teacherCourseNameCount.get(bestTeacherId).getOrDefault(courseName, 0) + 1
                    );
                    teacherCourseNames.get(bestTeacherId).add(courseName);
                    
                    // 更新基因
                    gene[1] = bestTeacherId;
                }
            }
        }
    }

    /**
     * 提取字符串中的数字部分
     * @param input 输入字符串
     * @param defaultValue 如果没有数字则返回的默认值
     * @return 数字部分字符串
     */
    private String extractNumericPart(String input, String defaultValue) {
        if (input == null || input.isEmpty()) {
            return defaultValue;
        }
        
        // 提取所有数字
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        
        String result = sb.toString();
        return result.isEmpty() ? defaultValue : result;
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
            if (existing.getTeacherId() != null && timetable.getTeacherId() != null &&
                existing.getTeacherId().equals(timetable.getTeacherId()) && 
                existingDay == day && existingPeriod == period) {
                throw new RuntimeException("教师 " + timetable.getTeacherId() + 
                                         " 在所选时间段已有其他课程安排");
            }
            
            // 检查教室时间冲突
            if (existing.getClassroomId() != null && timetable.getClassroomId() != null &&
                existing.getClassroomId().equals(timetable.getClassroomId()) && 
                existingDay == day && existingPeriod == period) {
                throw new RuntimeException("教室 " + timetable.getClassroomId() + 
                                         " 在所选时间段已被占用");
            }
        }
        
        // 检查教室容量限制
        Classroom classroom = classroomMapper.getClassroomById(timetable.getClassroomId().toString());
        Course course = null;
        
        // 根据courseId找到对应课程
        List<Course> courses = courseService.list();
        for (Course c : courses) {
            if (c.getId().equals(timetable.getCourseId().toString())) {
                course = c;
                break;
            }
        }
        
        // 假设课程有学生人数信息，进行容量检查
        int studentCount = estimateStudentCount(course);
        if (classroom != null && classroom.getCapacity() < studentCount) {
            throw new RuntimeException("教室容量不足，无法容纳该课程的学生人数");
        }
        
        // 教师资质检查
        Teacher teacher = teacherMapper.getTeacherById(timetable.getTeacherId().toString());
        if (teacher != null && course != null && !isTeacherQualified(teacher, course)) {
            throw new RuntimeException("该教师可能不具备教授此课程的资质，请确认");
        }
        
        // 检查是否超出教师每日/每周最大课时
        if (isExceedingTeacherMaxHours(teacher, timetable, existingTimetables)) {
            throw new RuntimeException("此安排将超出教师最大课时限制");
        }
        
        // 所有检查通过，保存数据
        try {
            if (timetable.getId() != null) {
                // 更新现有记录
                // 由于没有updateTimetable方法，先删除后插入来模拟更新
                // timetableMapper.deleteTimetableById(timetable.getId());
                return timetableMapper.insertTimetable(timetable);
            } else {
                // 插入新记录
        return timetableMapper.insertTimetable(timetable);
            }
        } catch (Exception e) {
            System.out.println("保存排课记录失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("保存排课记录失败", e);
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
        
        if (teacher == null || newTimetable == null || newTimetable.getTeacherId() == null) {
            return false; // 如果没有教师信息，不进行限制检查
        }
        
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.setTime(newTimetable.getScheduleTime());
        int newDay = newCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (newDay == 0) newDay = 7;
        
        int weeklyHours = 0;
        int dailyHours = 0;
        
        // 统计现有课时
        for (Timetable existing : existingTimetables) {
            if (existing.getTeacherId() != null && existing.getTeacherId().equals(newTimetable.getTeacherId())) {
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
                if (existing.getId() != null && existing.getId().equals(newTimetable.getId())) {
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
            if (entry.getTeacherId() != null && entry.getTeacherId().equals(teacher.getId())) {
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
        
        // 创建已占用教室ID集合
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
            if (entryDay == day && entryPeriod == period && entry.getClassroomId() != null) {
                occupiedClassrooms.add(entry.getClassroomId().toString());
            }
        }
        
        // 检查每个教室是否可用
        for (Classroom classroom : classrooms) {
            // 如果教室未被占用
            if (!occupiedClassrooms.contains(classroom.getId())) {
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

    public void autoScheduleMultiClass(int classCount) {
        System.out.println("======== 开始为" + classCount + "个班级排课 ========");
        
        // 预加载所有数据，避免重复查询
        List<Course> courses = courseService.list();
        List<Teacher> teachers = teacherService.list();
        List<Classroom> classrooms = classroomService.list();
        
        if (courses.isEmpty() || teachers.isEmpty() || classrooms.isEmpty()) {
            System.out.println("错误：缺少排课所需数据，无法进行自动排课");
            return;
        }
        
        // 使用线程池并行处理多个班级的排课
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(processors);
        List<Future<?>> futures = new ArrayList<>();
        
        for (int classId = 1; classId <= classCount; classId++) {
            final int currentClassId = classId;
            futures.add(executor.submit(() -> {
                System.out.println("\n开始为第" + currentClassId + "班排课...");
                autoSchedule(currentClassId);
            }));
        }
        
        // 等待所有班级排课完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        System.out.println("======== 所有班级排课完成 ========");
    }

    // 添加新的辅助方法来检查时间冲突
    private boolean hasTimeConflict(List<Object[]> genes, String teacherId, int day, int period, int classId) {
        for (Object[] gene : genes) {
            String existingTeacherId = (String) gene[1];
            int existingDay = (int) gene[3];
            int existingPeriod = (int) gene[4];
            int existingClassId = (Integer) gene[5];
            
            // 检查教师在同一时间段是否已有课程
            if (existingTeacherId.equals(teacherId) && existingDay == day && existingPeriod == period) {
                return true;
            }
            
            // 检查班级在同一时间段是否已有课程
            if (existingClassId == classId && existingDay == day && existingPeriod == period) {
                return true;
            }
        }
        return false;
    }
}
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
    private static final int MAX_WEEKLY_RECORDS = 15;    // 每周最大排课记录数，增加到50节
    private static final int MAX_COURSES_PER_TEACHER = 3; // 每位教师最多教5门不同课程
    private static final int MAX_SAME_COURSE_PER_TEACHER = 2; // 每位教师同一门课程最多上2次
    
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
        System.out.println("======== 开始自动排课 ========");
        
        // 获取数据
        List<Course> courses = courseService.list();
        List<Teacher> teachers = teacherService.list();
        List<Classroom> classrooms = classroomService.list();
        
        if (courses.isEmpty() || teachers.isEmpty() || classrooms.isEmpty()) {
            System.out.println("错误：缺少排课所需数据，无法进行自动排课");
            return;
        }
        
        // 增加课程处理数量
        int maxCourses = Math.min(MAX_WEEKLY_RECORDS, courses.size());
        if (courses.size() > maxCourses) {
            System.out.println("注意：限制课程数量为" + maxCourses + "（原课程总数：" + courses.size() + "）");
            courses = courses.subList(0, maxCourses);
        }
        
        System.out.println("开始排课，处理 " + courses.size() + " 门课程，" + 
                         teachers.size() + " 名教师，" + 
                         classrooms.size() + " 间教室");
        
        // 简化参数，加快收敛速度
        int limitedGenerations = Math.min(20, MAX_GENERATIONS); // 最多迭代20代
        double targetFitness = 0.8; // 降低目标适应度阈值
        int stagnantGenerations = 0;
        int maxStagnantGenerations = 5; // 降低无改进容忍代数
        double lastBestFitness = 0.0;
        
        // 初始化种群
        List<Chromosome> population = initializePopulation(courses, teachers, classrooms);
        
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
            
            // 检查是否达到目标适应度
            if (bestFitness >= targetFitness) {
                System.out.println("已达到目标适应度 " + targetFitness + "，提前结束进化");
                break;
            }
            
            // 检查是否适应度长期无改进
            if (Math.abs(bestFitness - lastBestFitness) < 0.001) {
                stagnantGenerations++;
                if (stagnantGenerations >= maxStagnantGenerations) {
                    System.out.println("适应度 " + maxStagnantGenerations + " 代无显著改进，提前结束进化");
                    break;
                }
            } else {
                stagnantGenerations = 0;
                lastBestFitness = bestFitness;
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
            
            // 将最佳染色体转换为实际的排课表并保存到数据库
            saveScheduleToDatabase(bestChromosome, courses, teachers, classrooms);
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
        
        // 按排课优先级对课程进行排序
        List<Course> sortedCourses = new ArrayList<>(courses);
        sortedCourses.sort((c1, c2) -> {
            // 优先级字符串可能为空或不是数字，需要安全处理
            int p1 = parseIntSafely(c1.getPriority(), 0);
            int p2 = parseIntSafely(c2.getPriority(), 0);
            // 注意：数字越小优先级越高
            return p1 - p2;
        });
        
        // 按课程名称分组
        Map<String, List<Course>> coursesByName = new HashMap<>();
        for (Course course : sortedCourses) {
            String courseName = course.getCourseName();
            if (!coursesByName.containsKey(courseName)) {
                coursesByName.put(courseName, new ArrayList<>());
            }
            coursesByName.get(courseName).add(course);
        }
        
        // 教师分配计数
        Map<String, Integer> teacherCourseCount = new HashMap<>();
        // 教师名称分配
        Map<String, Set<String>> teacherCourseNames = new HashMap<>();
        
        // 为每个课程分配资源
        for (Course course : sortedCourses) {
            String courseId = course.getId();
            String courseName = course.getCourseName();
            
            // 找到最合适的教师
            String teacherId = findBestTeacher(course, teachers, teacherCourseCount, teacherCourseNames);
            
            // 如果没有找到合适的教师，使用课程原有教师或随机分配
            if (teacherId == null) {
                if (course.getTeacherId() != null && !course.getTeacherId().isEmpty()) {
                    teacherId = course.getTeacherId();
                } else if (!teachers.isEmpty()) {
                    teacherId = teachers.get(random.nextInt(teachers.size())).getId();
                }
            }
            
            // 更新教师分配计数
            teacherCourseCount.put(teacherId, teacherCourseCount.getOrDefault(teacherId, 0) + 1);
            
            // 更新教师课程名称集合
            if (!teacherCourseNames.containsKey(teacherId)) {
                teacherCourseNames.put(teacherId, new HashSet<>());
            }
            teacherCourseNames.get(teacherId).add(courseName);
            
            // 随机选择教室、星期和节次
            Classroom classroom = classrooms.get(random.nextInt(classrooms.size()));
            String classroomId = classroom.getId();
            int day = random.nextInt(DAYS) + 1;
            int period = random.nextInt(PERIODS_PER_DAY) + 1;
            
            // 添加基因 [课程ID, 教师ID, 教室ID, 星期几, 第几节课]
            chromosome.genes.add(new Object[]{courseId, teacherId, classroomId, day, period});
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
    private String findBestTeacher(Course course, List<Teacher> teachers, 
                                  Map<String, Integer> teacherCourseCount,
                                  Map<String, Set<String>> teacherCourseNames) {
        if (teachers.isEmpty()) {
            return null;
        }
        
        String courseName = course.getCourseName();
        String originalTeacherId = course.getTeacherId();
        
        // 候选教师评分
        Map<String, Integer> teacherScores = new HashMap<>();
        
        for (Teacher teacher : teachers) {
            String teacherId = teacher.getId();
            int score = 100; // 基础分
            
            // 如果是原课程指定的教师，加分
            if (teacherId.equals(originalTeacherId)) {
                score += 50;
            }
            
            // 检查教师课程数量，数量越多分数越低
            int courseCount = teacherCourseCount.getOrDefault(teacherId, 0);
            score -= courseCount * 10;
            
            // 检查教师是否已分配相同名称的课程，如果是则加分（同一老师教同名课程好）
            Set<String> assignedCourseNames = teacherCourseNames.getOrDefault(teacherId, new HashSet<>());
            if (assignedCourseNames.contains(courseName)) {
                score += 30;
            } else if (!assignedCourseNames.isEmpty()) {
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
    private double calculateFitness(Chromosome chromosome, List<Course> courses, List<Teacher> teachers, List<Classroom> classrooms) {
        int conflictCount = 0;
        
        // 用于检测冲突的映射
        Map<String, List<Object[]>> teacherTimeMap = new HashMap<>();  // 教师-时间冲突
        Map<String, List<Object[]>> classroomTimeMap = new HashMap<>(); // 教室-时间冲突
        Map<String, Integer> teacherCourseCount = new HashMap<>();     // 教师课程数量
        Map<String, Map<Integer, Integer>> teacherDayCount = new HashMap<>(); // 教师每天课程数量
        Map<String, Set<String>> teacherAdjPeriodsMap = new HashMap<>(); // 教师相邻时间段记录
        Map<String, Set<String>> teacherCourseNames = new HashMap<>(); // 教师教授的课程名称
        
        // 创建课程ID到课程对象的映射
        Map<String, Course> courseMap = new HashMap<>();
        for (Course course : courses) {
            courseMap.put(course.getId(), course);
        }
        
        // 检查每个基因（课程安排）
        for (Object[] gene : chromosome.genes) {
            String courseId = (String) gene[0];
            String teacherId = (String) gene[1];
            String classroomId = (String) gene[2];
            int day = (int) gene[3];
            int period = (int) gene[4];
            
            // 获取课程信息
            Course course = courseMap.get(courseId);
            String courseName = course != null ? course.getCourseName() : "";
            
            // 记录教师课程名称
            if (!teacherCourseNames.containsKey(teacherId)) {
                teacherCourseNames.put(teacherId, new HashSet<>());
            }
            teacherCourseNames.get(teacherId).add(courseName);
            
            // 更新教师课程数统计
            teacherCourseCount.put(teacherId, teacherCourseCount.getOrDefault(teacherId, 0) + 1);
            
            // 更新教师每天课程数统计
            if (!teacherDayCount.containsKey(teacherId)) {
                teacherDayCount.put(teacherId, new HashMap<>());
            }
            teacherDayCount.get(teacherId).put(day, teacherDayCount.get(teacherId).getOrDefault(day, 0) + 1);
            
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
            
            // 记录教师相邻时间段
            String dayPeriodKey = day + "-" + period;
            if (!teacherAdjPeriodsMap.containsKey(teacherId)) {
                teacherAdjPeriodsMap.put(teacherId, new HashSet<>());
            }
            teacherAdjPeriodsMap.get(teacherId).add(dayPeriodKey);
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
        
        // 检查教师课程分配不平衡问题
        if (teachers.size() > 1) {
            int maxCoursesPerTeacher = MAX_WEEKLY_RECORDS / teachers.size();
            maxCoursesPerTeacher = Math.max(maxCoursesPerTeacher, 5); // 每位教师至少可以上5节课
            
            for (Map.Entry<String, Integer> entry : teacherCourseCount.entrySet()) {
                // 课程分配过多的教师增加冲突分数
                if (entry.getValue() > maxCoursesPerTeacher) {
                    conflictCount += (entry.getValue() - maxCoursesPerTeacher) * 2;
                }
                
                // 检查教师每天课程集中问题
                if (teacherDayCount.containsKey(entry.getKey())) {
                    Map<Integer, Integer> dayCount = teacherDayCount.get(entry.getKey());
                    for (Integer count : dayCount.values()) {
                        if (count > 3) { // 一天最多安排3节课
                            conflictCount += (count - 3) * 2;
                        }
                    }
                }
                
                // 检查教师课程多样性问题（同一教师授课种类过多）
                if (teacherCourseNames.containsKey(entry.getKey())) {
                    Set<String> courseNames = teacherCourseNames.get(entry.getKey());
                    if (courseNames.size() > 3) { // 一个教师最好不要教授超过3种不同的课程
                        conflictCount += (courseNames.size() - 3);
                    }
                }
            }
        }
        
        // 检查教师连续上课问题
        for (Map.Entry<String, Set<String>> entry : teacherAdjPeriodsMap.entrySet()) {
            String teacherId = entry.getKey();
            Set<String> periods = entry.getValue();
            
            // 检查是否有相邻时间段
            for (String period1 : periods) {
                String[] parts1 = period1.split("-");
                int day1 = Integer.parseInt(parts1[0]);
                int period1Val = Integer.parseInt(parts1[1]);
                
                for (String period2 : periods) {
                    if (period1.equals(period2)) continue;
                    
                    String[] parts2 = period2.split("-");
                    int day2 = Integer.parseInt(parts2[0]);
                    int period2Val = Integer.parseInt(parts2[1]);
                    
                    // 在同一天的相邻时间段
                    if (day1 == day2 && Math.abs(period1Val - period2Val) == 1) {
                        // 连续上课不一定是冲突，但如果超过2个连续时间段，则增加冲突分数
                        int consecutiveCount = 0;
                        for (int i = Math.min(period1Val, period2Val); i <= Math.max(period1Val, period2Val) + 1; i++) {
                            if (periods.contains(day1 + "-" + i)) {
                                consecutiveCount++;
                            }
                        }
                        
                        if (consecutiveCount > 2) {
                            conflictCount += consecutiveCount - 2;
                        }
                    }
                }
            }
        }
        
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
        
        for (int i = 0; i < chromosome.genes.size(); i++) {
            if (random.nextDouble() < currentMutationRate) {
                Object[] gene = chromosome.genes.get(i);
                String currentTeacherId = (String) gene[1];
                
                // 变异类型选择
                int mutationType;
                
                // 如果当前教师是课程数最多的教师，增加变异教师的概率
                if (currentTeacherId.equals(maxTeacherId) && teachers.size() > 1) {
                    mutationType = random.nextInt(5); // 0-4中有3种情况会变异教师
                } else {
                    mutationType = random.nextInt(5); // 随机决定变异什么属性（教室、星期、节次、教师）
                }
                
                switch (mutationType) {
                    case 0: // 变异教室
                        if (!classrooms.isEmpty()) {
                            int randomIndex = random.nextInt(classrooms.size());
                            gene[2] = classrooms.get(randomIndex).getId();
                        }
                        break;
                    case 1: // 变异星期
                        gene[3] = random.nextInt(DAYS) + 1;
                        break;
                    case 2: // 变异节次
                        gene[4] = random.nextInt(PERIODS_PER_DAY) + 1;
                        break;
                    case 3: // 变异教师 - 随机选择
                    case 4: // 变异教师 - 优先选择课程少的教师
                        if (teachers.size() > 1) {
                            Teacher newTeacher = null;
                            
                            if (mutationType == 4 && minTeacherId != null && !currentTeacherId.equals(minTeacherId)) {
                                // 找到课程数最少的教师
                                for (Teacher teacher : teachers) {
                                    if (teacher.getId().equals(minTeacherId)) {
                                        newTeacher = teacher;
                                        break;
                                    }
                                }
                            } else {
                                // 随机选择一个不同的教师
                                List<Teacher> availableTeachers = new ArrayList<>();
                                for (Teacher teacher : teachers) {
                                    if (!teacher.getId().equals(currentTeacherId)) {
                                        availableTeachers.add(teacher);
                                    }
                                }
                                
                                if (!availableTeachers.isEmpty()) {
                                    newTeacher = availableTeachers.get(random.nextInt(availableTeachers.size()));
                                }
                            }
                            
                            if (newTeacher != null) {
                                gene[1] = newTeacher.getId();
                                
                                // 更新教师课程计数
                                teacherCourseCount.put(currentTeacherId, teacherCourseCount.get(currentTeacherId) - 1);
                                teacherCourseCount.put(newTeacher.getId(), teacherCourseCount.getOrDefault(newTeacher.getId(), 0) + 1);
                            }
                        }
                        break;
                }
            }
        }
    }
    
    /**
     * 将最佳染色体保存到数据库
     */
    private void saveScheduleToDatabase(Chromosome chromosome, List<Course> courses, List<Teacher> teachers, List<Classroom> classrooms) {
        System.out.println("开始保存排课结果到数据库...");
        
        // 限制处理的记录数，增大到30条
        int maxRecords = Math.min(MAX_WEEKLY_RECORDS, chromosome.genes.size());
        if (chromosome.genes.size() > maxRecords) {
            System.out.println("注意：限制插入记录数为 " + maxRecords + "（原记录总数：" + chromosome.genes.size() + "）");
        }
        
        int successCount = 0;
        int failCount = 0;
        
        // 教师课程分布统计，用于避免同一教师在短时间内频繁授课
        Map<String, Set<String>> teacherDayPeriodMap = new HashMap<>();  // 记录每个教师已排的day-period组合
        Map<String, Integer> teacherCourseCount = new HashMap<>();       // 记录每个教师排课数量
        Map<String, Map<String, Integer>> teacherCourseNameCount = new HashMap<>();  // 记录每个教师每种课程的数量
        
        // 创建课程ID到课程对象的映射
        Map<String, Course> courseMap = new HashMap<>();
        for (Course course : courses) {
            courseMap.put(course.getId(), course);
        }
        
        // 按排课优先级对基因排序
        List<Object[]> sortedGenes = new ArrayList<>(chromosome.genes);
        sortedGenes.sort((gene1, gene2) -> {
            String courseId1 = (String) gene1[0];
            String courseId2 = (String) gene2[0];
            Course course1 = courseMap.get(courseId1);
            Course course2 = courseMap.get(courseId2);
            
            if (course1 == null || course2 == null) {
                return 0;
            }
            
            // 按优先级排序（数字越小优先级越高）
            int p1 = parseIntSafely(course1.getPriority(), 0);
            int p2 = parseIntSafely(course2.getPriority(), 0);
            return p1 - p2;
        });
        
        // 确保教师的课程分配均衡，预处理基因数组
        ensureTeacherBalance(sortedGenes, teachers, courseMap);
        
        // 保存新的排课方案
        for (int i = 0; i < maxRecords && i < sortedGenes.size(); i++) {
            Object[] gene = sortedGenes.get(i);
            try {
                // 基础数据
                String courseId = (String) gene[0];
                String teacherId = (String) gene[1];
                String classroomId = (String) gene[2];
                int day = (int) gene[3];
                int period = (int) gene[4];
                
                Course course = courseMap.get(courseId);
                if (course == null) {
                    System.out.println("警告：找不到ID为 " + courseId + " 的课程，跳过此记录");
                    continue;
                }
                
                String courseName = course.getCourseName();
                
                // 教师课程分配平衡检查
                String dayPeriodKey = day + "-" + period;
                
                // 确保教师数据结构初始化
                if (!teacherDayPeriodMap.containsKey(teacherId)) {
                    teacherDayPeriodMap.put(teacherId, new HashSet<>());
                }
                if (!teacherCourseCount.containsKey(teacherId)) {
                    teacherCourseCount.put(teacherId, 0);
                }
                if (!teacherCourseNameCount.containsKey(teacherId)) {
                    teacherCourseNameCount.put(teacherId, new HashMap<>());
                }
                
                // 获取此教师已分配的此类课程数量
                int currentCourseTypeCount = teacherCourseNameCount.get(teacherId)
                        .getOrDefault(courseName, 0);
                
                // 检查相同名称课程分配给同一教师的最大数量（通常不超过2-3节）
                int maxSameCoursePerTeacher = 3;
                if (currentCourseTypeCount >= maxSameCoursePerTeacher) {
                    System.out.println("警告：教师 " + teacherId + " 的 '" + courseName + "' 课程数量已达上限 " + maxSameCoursePerTeacher);
                    
                    // 尝试分配给其他教师
                    boolean reassigned = false;
                    
                    // 首先尝试找之前教过这门课的教师
                    for (Teacher otherTeacher : teachers) {
                        String otherTeacherId = otherTeacher.getId();
                        if (!otherTeacherId.equals(teacherId) && 
                            teacherCourseNameCount.containsKey(otherTeacherId) &&
                            teacherCourseNameCount.get(otherTeacherId).containsKey(courseName) &&
                            teacherCourseNameCount.get(otherTeacherId).get(courseName) < maxSameCoursePerTeacher) {
                            
                            System.out.println("尝试将课程重新分配给之前教过此课的教师 " + otherTeacherId);
                            gene[1] = otherTeacherId;
                            teacherId = otherTeacherId;
                            
                            // 确保新教师数据结构初始化
                            if (!teacherDayPeriodMap.containsKey(teacherId)) {
                                teacherDayPeriodMap.put(teacherId, new HashSet<>());
                            }
                            if (!teacherCourseCount.containsKey(teacherId)) {
                                teacherCourseCount.put(teacherId, 0);
                            }
                            if (!teacherCourseNameCount.containsKey(teacherId)) {
                                teacherCourseNameCount.put(teacherId, new HashMap<>());
                            }
                            
                            reassigned = true;
                            break;
                        }
                    }
                    
                    // 如果没有找到之前教过这门课的教师，尝试找课程总量少的教师
                    if (!reassigned) {
                        for (Teacher otherTeacher : teachers) {
                            String otherTeacherId = otherTeacher.getId();
                            if (!otherTeacherId.equals(teacherId) && 
                                (!teacherCourseCount.containsKey(otherTeacherId) || 
                                 teacherCourseCount.get(otherTeacherId) < teacherCourseCount.get(teacherId))) {
                                
                                System.out.println("尝试将课程重新分配给课程总量较少的教师 " + otherTeacherId);
                                gene[1] = otherTeacherId;
                                teacherId = otherTeacherId;
                                
                                // 确保新教师数据结构初始化
                                if (!teacherDayPeriodMap.containsKey(teacherId)) {
                                    teacherDayPeriodMap.put(teacherId, new HashSet<>());
                                }
                                if (!teacherCourseCount.containsKey(teacherId)) {
                                    teacherCourseCount.put(teacherId, 0);
                                }
                                if (!teacherCourseNameCount.containsKey(teacherId)) {
                                    teacherCourseNameCount.put(teacherId, new HashMap<>());
                                }
                                
                                reassigned = true;
                                break;
                            }
                        }
                    }
                    
                    if (!reassigned) {
                        System.out.println("无法为课程 '" + courseName + "' 找到合适的教师，跳过此课程");
                        continue; // 跳过此课程
                    }
                }
                
                // 检查教师是否已经在相邻时间段授课
                boolean hasAdjacentPeriod = false;
                for (String existingDayPeriod : teacherDayPeriodMap.get(teacherId)) {
                    String[] parts = existingDayPeriod.split("-");
                    int existingDay = Integer.parseInt(parts[0]);
                    int existingPeriod = Integer.parseInt(parts[1]);
                    
                    if (existingDay == day && Math.abs(existingPeriod - period) == 1) {
                        hasAdjacentPeriod = true;
                        break;
                    }
                }
                
                // 如果有相邻课程且已经有2个连续的课，尝试调整时间
                if (hasAdjacentPeriod) {
                    int consecutiveCount = 0;
                    for (int p = period - 2; p <= period + 2; p++) {
                        if (p >= 1 && p <= PERIODS_PER_DAY && 
                            teacherDayPeriodMap.get(teacherId).contains(day + "-" + p)) {
                            consecutiveCount++;
                        }
                    }
                    
                    if (consecutiveCount >= 2) {
                        // 尝试调整时间
                        boolean timeAdjusted = false;
                        
                        // 尝试不同的星期
                        for (int newDay = 1; newDay <= DAYS; newDay++) {
                            if (newDay == day) continue;
                            
                            // 检查该星期是否已有过多课程
                            int dayCount = 0;
                            for (String key : teacherDayPeriodMap.get(teacherId)) {
                                if (key.startsWith(newDay + "-")) {
                                    dayCount++;
                                }
                            }
                            
                            if (dayCount < 3) { // 每天不超过3节课
                                boolean periodConflict = false;
                                for (int p = 1; p <= PERIODS_PER_DAY; p++) {
                                    if (!teacherDayPeriodMap.get(teacherId).contains(newDay + "-" + p)) {
                                        // 尝试设置到这个时间段
                                        gene[3] = newDay;
                                        gene[4] = p;
                                        day = newDay;
                                        period = p;
                                        timeAdjusted = true;
                                        break;
                                    }
                                }
                                
                                if (timeAdjusted) break;
                            }
                        }
                        
                        if (!timeAdjusted) {
                            System.out.println("无法为教师 " + teacherId + " 的课程 '" + courseName + "' 调整时间，保持原时间");
                        } else {
                            System.out.println("已将教师 " + teacherId + " 的课程 '" + courseName + "' 时间调整为 星期" + day + " 第" + period + "节");
                        }
                    }
                }
                
                // 检查教师课程是否过多
                int currentTeacherCourses = teacherCourseCount.get(teacherId);
                int maxCoursesPerTeacher = MAX_WEEKLY_RECORDS / (teachers.size() > 0 ? teachers.size() : 1);
                maxCoursesPerTeacher = Math.max(maxCoursesPerTeacher, 5); // 每位教师至少可以上5节课
                
                if (currentTeacherCourses >= maxCoursesPerTeacher) {
                    System.out.println("警告：教师 " + teacherId + " 课程数量已达上限 " + maxCoursesPerTeacher);
                    // 尝试分配给其他教师
                    boolean reassigned = false;
                    for (Teacher otherTeacher : teachers) {
                        String otherTeacherId = otherTeacher.getId();
                        if (!otherTeacherId.equals(teacherId) && 
                            (!teacherCourseCount.containsKey(otherTeacherId) || 
                             teacherCourseCount.get(otherTeacherId) < maxCoursesPerTeacher)) {
                            
                            System.out.println("尝试将课程重新分配给教师 " + otherTeacherId);
                            gene[1] = otherTeacherId;
                            teacherId = otherTeacherId;
                            
                            // 确保新教师数据结构初始化
                            if (!teacherDayPeriodMap.containsKey(teacherId)) {
                                teacherDayPeriodMap.put(teacherId, new HashSet<>());
                            }
                            if (!teacherCourseCount.containsKey(teacherId)) {
                                teacherCourseCount.put(teacherId, 0);
                            }
                            if (!teacherCourseNameCount.containsKey(teacherId)) {
                                teacherCourseNameCount.put(teacherId, new HashMap<>());
                            }
                            
                            reassigned = true;
                            break;
                        }
                    }
                    
                    if (!reassigned) {
                        System.out.println("无法重新分配课程，所有教师都达到了上限");
                        continue; // 跳过此课程
                    }
                }
                
                // 更新教师课程记录
                teacherDayPeriodMap.get(teacherId).add(dayPeriodKey);
                teacherCourseCount.put(teacherId, teacherCourseCount.get(teacherId) + 1);
                
                // 更新教师课程名称计数
                teacherCourseNameCount.get(teacherId).put(
                    courseName, 
                    teacherCourseNameCount.get(teacherId).getOrDefault(courseName, 0) + 1
                );
                
                // 创建排课记录
                Timetable timetable = new Timetable();
                
                // 直接使用原始ID，保持字符串格式
                timetable.setCourseId(courseId);
                timetable.setTeacherId(teacherId);
                timetable.setClassroomId(classroomId);
                
                // 设置时间
                timetable.setScheduleTime(calculateScheduleTime(day, period));
                
                // 设置节次信息
                StringBuilder periodInfoBuilder = new StringBuilder();
                // 考虑Course的consecutiveSections属性，支持连续多节课
                int consecutiveSections = course.getConsecutiveSections();
                if (consecutiveSections <= 0) {
                    consecutiveSections = 1; // 默认为1节课
                }
                // 限制最大连续节数为3
                consecutiveSections = Math.min(consecutiveSections, 3);
                
                // 构建格式为"1,2,3"的periodInfo字符串
                for (int p = period; p < period + consecutiveSections && p <= PERIODS_PER_DAY; p++) {
                    if (periodInfoBuilder.length() > 0) {
                        periodInfoBuilder.append(",");
                    }
                    periodInfoBuilder.append(p);
                }
                
                timetable.setPeriodInfo(periodInfoBuilder.toString());
                
                // 设置星期几
                timetable.setDayOfWeek(day);
                
                System.out.println("尝试插入第 " + (i+1) + "/" + maxRecords + " 条记录: " + 
                                 "课程=" + courseId + " (" + courseName + ")" +
                                 ", 教师=" + teacherId + 
                                 ", 教室=" + classroomId + 
                                 ", 节次=" + period + 
                                 ", 星期=" + day);
                
                // 执行插入操作
                int result = timetableMapper.insertTimetable(timetable);
                if (result > 0) {
                    successCount++;
                    System.out.println("第 " + (i+1) + " 条记录插入成功");
                } else {
                    failCount++;
                    System.out.println("第 " + (i+1) + " 条记录插入失败");
                }
            } catch (Exception e) {
                failCount++;
                System.out.println("错误信息: " + e.getMessage());
                e.printStackTrace(); // 打印详细的异常堆栈，便于排查
            }
        }
        
        // 打印教师课程分配统计
        System.out.println("\n教师课程分配统计：");
        for (Map.Entry<String, Integer> entry : teacherCourseCount.entrySet()) {
            String teacherId = entry.getKey();
            System.out.println("教师 ID: " + teacherId + ", 课程总数: " + entry.getValue());
            
            // 打印每种课程的数量
            if (teacherCourseNameCount.containsKey(teacherId)) {
                for (Map.Entry<String, Integer> courseEntry : teacherCourseNameCount.get(teacherId).entrySet()) {
                    System.out.println("  - 课程: '" + courseEntry.getKey() + "', 数量: " + courseEntry.getValue());
                }
            }
        }
        
        System.out.println("\n排课保存完成: 成功 " + successCount + " 条，失败 " + failCount + " 条");
        
        if (successCount > 0) {
            System.out.println("排课成功! 课表已保存到数据库");
        } else {
            System.out.println("警告: 所有记录均保存失败，请检查数据库错误日志");
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
}
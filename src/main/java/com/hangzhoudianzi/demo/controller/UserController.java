package com.hangzhoudianzi.demo.controller;

import com.hangzhoudianzi.demo.pojo.people.User;
import com.hangzhoudianzi.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController         //@RestController = @Controller + @ResponseBody:
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    UserService userService;

    // 登录
    @PostMapping("/login")
    public String login(@RequestBody User user) {
        return userService.login(user);
    }

    // 注册
    @PostMapping("/register")
    public String register(@RequestBody User user) {
        return userService.register(user);
    }

    // 解决查询数据库中文出现乱码问题
    @RequestMapping(value = "/alluser", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public List<User> findAll() {
        return userService.findAll();
    }
}
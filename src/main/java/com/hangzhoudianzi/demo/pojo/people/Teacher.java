package com.hangzhoudianzi.demo.pojo.people;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Teacher {
    private Integer id;
    private String name;
    private String department;

}
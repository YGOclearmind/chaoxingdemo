package com.hangzhoudianzi.demo.pojo.people;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Teacher {
    private String id;
    private String name;
    private String department;
    private String gender;
    private String ethnic;

}
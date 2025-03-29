package com.hangzhoudianzi.demo.pojo.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Classroom {
    private String id;
    private String name;
    private String building;
    private int floor;
    private String type;
    private int capacity;
    private String campus;
    private String managementDepartme;

}
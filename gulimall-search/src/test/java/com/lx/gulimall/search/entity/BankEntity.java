package com.lx.gulimall.search.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankEntity {
    private Long account_number;
    private String address;
    private Long age;
    private Long balance;
    private String city;
    private String email;
    private String employer;
    private String firstname;
    private String gender;
    private String lastname;
    private String state;
}

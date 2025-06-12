package com.api.digicell.dto;

import lombok.Data;

@Data
public class UserAccountDTO {
    private String userName;
    private String password;
    private String email;
    private String phoneNumber;
    private String status;
}


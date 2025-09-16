package com.isw.payapp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserModel extends TransactionData {
    private String username;
    private String password;
    private String role;
    private String requestType;
    private String firstName;
    private String lastName;
}

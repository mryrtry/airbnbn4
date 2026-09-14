package main.dto;

import java.util.List;


public record UserInfo(String username, String email, String firstName, String lastName,
                       List<String> groups
) {}
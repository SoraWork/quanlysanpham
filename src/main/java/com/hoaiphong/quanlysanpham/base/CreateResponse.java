package com.hoaiphong.quanlysanpham.base;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateResponse<T> {
    private int status;
    private String message;
    private T data;
}
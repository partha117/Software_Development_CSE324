package com.rafid.controllers;

/**
 * Created by ASUS on 4/17/2017.
 */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {
    @RequestMapping("/")
    String index() {
        return "index";

    }
}
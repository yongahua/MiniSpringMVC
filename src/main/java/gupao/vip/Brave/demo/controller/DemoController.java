package gupao.vip.Brave.demo.controller;

import gupao.vip.Brave.demo.service.DemoService;
import gupao.vip.Brave.mvcframework.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@GPController
@GPRequestMapping("/demo")
public class DemoController {

    @GPAutowired
    private DemoService demoService;

    @GPRequestMapping("/hello")
    public void hello(HttpServletRequest req, HttpServletResponse resp,
                      @GPRequestParam("name") String name){

        String result = "My name is " + name;
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @GPRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @GPRequestParam("a") Integer a, @GPRequestParam("b") Integer b){
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GPRequestMapping("/dou")
    public void dou(HttpServletRequest req, HttpServletResponse resp,
                    @GPRequestParam("a") Double a){
        try {
            resp.getWriter().write("a"+a);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GPRequestMapping("/sund")
    public void sund(HttpServletRequest req, HttpServletResponse resp,
                    @GPRequestParam("a") Double a, @GPRequestParam("b") Double b){
        try {
            resp.getWriter().write(a + "-" + b + "=" + (a - b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

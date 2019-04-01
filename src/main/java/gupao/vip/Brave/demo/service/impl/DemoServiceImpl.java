package gupao.vip.Brave.demo.service.impl;

import gupao.vip.Brave.demo.service.DemoService;
import gupao.vip.Brave.mvcframework.annotation.GPService;

@GPService
public class DemoServiceImpl implements DemoService{
    public String get(String name) {
        return "My name is " + name;
    }
}

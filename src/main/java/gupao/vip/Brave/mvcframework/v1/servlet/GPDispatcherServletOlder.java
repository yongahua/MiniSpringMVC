package gupao.vip.Brave.mvcframework.v1.servlet;


import gupao.vip.Brave.mvcframework.annotation.*;
import gupao.vip.Brave.mvcframework.conversion.TypeConversion;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class GPDispatcherServletOlder extends HttpServlet{


    //保存application.properties配置文件中的内容
    private Properties contextConfig = new Properties();
    //保存扫描的所有的类名
    private List<String> classNames = new ArrayList<String>();
    //IOC容器
    // 为了简化程序，暂时不考虑用ConcurrentHashMap，主要还是关注设计思想和原理
    private Map<String,Object> ioc = new HashMap<String,Object>();
    //保存url和Method的对应关系
    private Map<String,Method> handlerMapping = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6. 调用，运行阶段
        try{
            doDispatch(req,resp);
        }catch (Exception e){
            e.printStackTrace();
            resp.getWriter().write("500 Exception,Detail : "+Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws  Exception{
        //绝对路径
        String url = req.getRequestURI();
        //处理成相对路径
        String contextPath = req.getContextPath();
        url = (url.replaceAll(contextPath,"")).replaceAll("/+","/");

        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!!");
            return;
        }

        Method method = this.handlerMapping.get(url);
        //投机取巧的方式
        //通过反射拿到method所在的class，拿到class之后还是拿到class名称
        //再调用toLowerFirstCase获得beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());

        //从request中获得参数列表
        Map<String,String[]> params = req.getParameterMap();
        /*
            //暂时写死
            System.out.println(params.get("name")[0]);
            //此处有问题,因为params.get("name")[0]是一个数组,
            //还要加上req和resp
            //method.invoke(ioc.get(beanName),params.get("name")[0]);
            method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]});
        */
        /**
         * 优化
         */
        //获取方法的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //作为实参
        Object [] paramValues = new Object[parameterTypes.length];

        //用循环将形参和实参对应一下
        for(int i=0;i<parameterTypes.length;i++){
            Class parameterType = parameterTypes[i];
            //不能用instanceof，parameterTypes它不是实参，是形参
            if(parameterType == HttpServletRequest.class){
                paramValues[i] = req;
            }else if(parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
            }else {  /*else if(parameterType == String.class){

                //http://localhost/demo/hello?name=Braves
                //params key = "name" value = ["Braves"]
                //requestParam == null
                GPRequestParam requestParam = (GPRequestParam)parameterType.getAnnotation(GPRequestParam.class);
                //简化版本 空指针异常 java.lang.NullPointerException
                if(params.containsKey(requestParam.value())){
                    for(Map.Entry<String,String[]> param:params.entrySet()){
                        //  \\[\\]表示数组，\\s表示空格
                        String value = Arrays.toString(param.getValue()).replaceAll("\\[\\]","")
                                .replaceAll("\\s",",");
                        paramValues[i] = value;
                    }
                }

            }*/

                //修改版

                //把方法上的注解拿到，得到的是一个二位数组
                Annotation[][] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length; j++) {
                    for (Annotation a : pa[j]) {
                        //我们要解析的只是GPRequestParam
                        if (a instanceof GPRequestParam) {
                            //拿到参数名称  去http://localhost/demo/hello?name=Braves匹配
                            String paramName = ((GPRequestParam) a).value();

                            //从req拿到参数类表中去找对应的key
                            if (params.containsKey(paramName)) {
                                for (Map.Entry<String, String[]> param : params.entrySet()) {
                                    //拿到key所对应的值，而拿到的这个值，有一对多的关系
                                    //一个key跟一个数组
                                    //而对方接收的是String类型，把数组处理为String
                                    //  \\[\\]表示数组，\\s表示空格
                               /* String value = (Arrays.toString(param.getValue()).replaceAll("\\[\\]","")
                                        .replaceAll("\\s",",")).replaceAll(",+",",");
                                */
                                    TypeConversion typeConversion = new TypeConversion(Arrays.toString(param.getValue()));
                                    Object value = typeConversion.convert(parameterType);
                                    //Object value = convert(parameterType, Arrays.toString(param.getValue()));

                                    //类型的强制转换
                                    paramValues[i] = value;
                                }
                            }
                        }
                    }
                }
            }

        }

        method.invoke(ioc.get(beanName),paramValues);

    }

    //url 传过来的参数都是String类型的，HTTP是基于字符串协议
    //只需要把String转换为任意类型就好
    private Object convert(Class<?> type,String value){
        //如果是String
        if(String.class == type){
            return (value.replaceAll("\\[\\]","")
                    .replaceAll("\\s",",")).replaceAll(",+",",");
        }
        //如果是int
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        //.....

        return value;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件
        //从servlet的配置文件里面，把它的spring主配置路径加载进来
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2、扫描相关的类
        //解析配置文件，因为是文件，所以直接getProperty获取
        doScanner(contextConfig.getProperty("scanPackage"));

        //3、初始化扫描到的类，并且将它们放入到ICO容器之中
        doInstance();

        //4、完成依赖注入
        doAutowired();

        //5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("GP Spring framework is init.");
    }
    //加载配置文件
    private void doLoadConfig(String contextConfigLocation) {
        //直接从类路径下，找到Spring主配置文件所在的路径
        //并将其读取处来放到Properties对象中
        //相当于把scanPackage=gupao.vip.Brave.demo 从文件中保存到了内存中
        //用文件流接收
        InputStream is = null;
        is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(is!=null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    //扫描出相关的类
    private void doScanner(String scanPackage) {
        //从classpath下 找所有的class文件
        //scanPackage=gupao.vip.Brave.demo，存储的是包路径
        //转换为文件路劲，实际就是把.替换为/就OK了
        //classpath
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        //将classpath转换为文件
        File classPath = new File(url.getFile());
        //迭代classPath下所有的文件
        for(File file:classPath.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                //获取完整的类名
                String className = (scanPackage+"."+file.getName()).replace(".class","");
                classNames.add(className);
            }
        }

    }

    //初始化，为DI做准备
    private void doInstance() {

        if(classNames.isEmpty()){
            return;
        }
        try{
            for(String className:classNames){
                Class<?> clazz = Class.forName(className);

                //什么样的类才需要初始化呢？
                //加了注解的类，才需要初始化
                //为了简化逻辑，主要是体会设计思想，只举例@Controller和@Service，
                // @Componment...就不一一列举了

                //1.Spring默认类名首字母小写
                String beanName = clazz.getSimpleName();//获取类名

                Object instance = null;
                if(clazz.isAnnotationPresent(GPController.class)){
                    //初始化
                    instance = clazz.newInstance();

                    //2.自定义的beanName
                    GPController gpController = clazz.getAnnotation(GPController.class);
                    if(!"".equals(gpController.value())){
                        beanName = gpController.value();
                    }
                    //key 应该用className的小写
                    //将初始化的对象放到IOC容器中去
                    ioc.put(toLowerFirstCase(beanName),instance);
                }else if(clazz.isAnnotationPresent(GPService.class)){
                    //初始化
                    instance = clazz.newInstance();
                    //2.自定义的beanName
                    GPService gpService = clazz.getAnnotation(GPService.class);
                    if(!"".equals(gpService.value())){
                        beanName = gpService.value();
                    }
                    //key 应该用className的小写
                    //将初始化的对象放到IOC容器中去
                    ioc.put(toLowerFirstCase(beanName),instance);
                    //3.根据类名自动赋值
                    for(Class<?> i:clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The"+i.getName()+"is exists!!");
                        }
                        //把接口的类型作为key
                        ioc.put(i.getName(),instance);
                    }
                }else{
                    continue;
                }


            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //自动的依赖注入
    private void doAutowired() {
        //ioc容器为空，直接return
        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            //Declared 所有的，特定的 字段，
            //把所有的字段 包括private/protected/default
            //正常来说，普通的OOP编程只能拿到public的属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field:fields){
                if(!field.isAnnotationPresent(GPAutowired.class)){
                    continue;
                }
                GPAutowired gpAutowired = field.getAnnotation(GPAutowired.class);

                //用户没有自定义beanName，默认就根据类型注入
                String beanName = gpAutowired.value().trim();
                if("".equals(beanName)){
                    //获得接口的类型
                    beanName = field.getType().getName();
                }

                //如果是public以外的修饰符，只要加了@Autowired注解，倒要强制赋值
                field.setAccessible(true);

                try {
                    //用反射机制，动态给字段赋值
                    field.set(entry.getValue(),ioc.get(toLowerFirstCase(beanName)));

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    //初始化url和Method的一对一的对应关系
    private void initHandlerMapping() {
        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(GPController.class)){
                continue;
            }

            //保存写在类上的url  -- @GPRequestMapping("demo")
            String baseUrl = "";
            if(clazz.isAnnotationPresent(GPRequestMapping.class)){
                GPRequestMapping gpRequestMapping = clazz.getAnnotation(GPRequestMapping.class);
                baseUrl = gpRequestMapping.value();
            }

            //默认获取所有的public方法
            for(Method method:clazz.getMethods()){
                if(!method.isAnnotationPresent(GPRequestMapping.class)){
                    continue;
                }

                GPRequestMapping gpRequestMapping = method.getAnnotation(GPRequestMapping.class);
                String url =(baseUrl+"/"+gpRequestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,method);
            }

        }
    }

    //首字母转小写
    private static String toLowerFirstCase(String simpleName) {
        if("".equals(simpleName)){
            return "";
        }
        //将类名转换为char数组
        char[] chars = simpleName.toCharArray();
        //大小写字母的ASCII码相差32，且小写字母的ASCII码要大于大写字母的ASCII码
        //A的ASSCII码是65，Z的ASCII码是90
        //防止类名不是大写而报错
        System.out.println("--------------------------------");
        System.out.println(simpleName);
        System.out.println(chars[0]);
        if(chars[0]>65 && chars[0]<90){
            chars[0]+=32;
        }
        return  String.valueOf(chars);

    }
}

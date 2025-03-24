package com.hangzhoudianzi.demo.config;
/*
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
*/
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/*@Configuration用于定义配置类， 相当于 配置的头部
可替换xml配置文件，被注解的类内部包含有一个或多个被
@Bean注解的方法，这些方法将会被AnnotationConfigApplicationContext
或AnnotationConfigWebApplicationContext类进行扫描，
并用于构建bean定义，初始化Spring容器。 注意：@Configuration注解的配置类有如下要求：*/

@Configuration
public class MybatisPlusConfig {

    //@Configuration启动容器+@Bean注册Bean
    /**
     * mybatis拦截器   注册插件
     * @return
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        // 初始化拦截器
        MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

/*
    @Configuation等价于<Beans></Beans>
    @Bean等价于<Bean></Bean>
    @ComponentScan等价于<context:component-scan base-package="com.xxx"/>
    */
}

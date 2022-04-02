package top.kaluna.pub;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 86158
 */
@SpringBootApplication
@MapperScan("top.kaluna.pub.mapper")
@EnableScheduling
public class PubApplication {

    private static final Logger LOG = LoggerFactory.getLogger(PubApplication.class);
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PubApplication.class);
        Environment environment = app.run(args).getEnvironment();
        LOG.info("启动成功！！");
        LOG.info("地址：\thttp://127.0.0.1:{}",environment.getProperty("server.port"));
    }
}


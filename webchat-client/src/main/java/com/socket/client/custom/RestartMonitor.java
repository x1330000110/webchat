package com.socket.client.custom;

import cn.hutool.core.thread.ThreadUtil;
import com.socket.client.ClientApplication;
import com.socket.client.manager.UserManager;
import com.socket.core.custom.SettingSupport;
import com.socket.core.model.command.impl.CommandEnum;
import com.socket.core.model.enums.Setting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 服务器重启监视器（10秒扫描一次）
 */
@Slf4j
@Component
public class RestartMonitor {
    private ConfigurableApplicationContext context;
    private SettingSupport settingSupport;
    private UserManager userManager;

    @Scheduled(cron = "0/10 * * * * ?")
    public void restartMonitor() {
        boolean state = settingSupport.getSetting(Setting.RESTART_SERVER);
        if (state) {
            settingSupport.switchSetting(Setting.RESTART_SERVER);
            ApplicationArguments args = context.getBean(ApplicationArguments.class);
            new Thread(() -> {
                log.warn("服务器将在10秒后重启...");
                userManager.sendAll("服务器将在10秒后重启", CommandEnum.DANGER);
                ThreadUtil.sleep(10, TimeUnit.SECONDS);
                context.close();
                context = SpringApplication.run(ClientApplication.class, args.getSourceArgs());
            }).start();
        }
    }

    @Autowired
    public void setContext(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Autowired
    public void setSettingSupport(SettingSupport settingSupport) {
        this.settingSupport = settingSupport;
    }

    @Autowired
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }
}

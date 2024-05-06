package com.tc.learnflowable.handler;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import java.util.Set;

@Slf4j
public class AskForLeaveFail implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 可以抽成公共的组件，比如是发送邮件，通过执行参数传入发邮件需要的内容，这里取出参数直接发
        Set<String> variableNames = execution.getVariableNames();
        log.error("请假失败。。。");
    }
}

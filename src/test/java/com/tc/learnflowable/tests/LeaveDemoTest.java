package com.tc.learnflowable.tests;

import com.tc.learnflowable.LearnFlowableApplicationTests;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注意：有一点需要说明：如果单元测试报失败，有可能是因为你之前部署的流程有问题导致的，然后审批的时候审批到历史的错误流程（踩过）。
 *
 * @author tangcheng_cd
 * @version 1.0
 * @className LeaveDemoTest
 * @description
 * @date 2024/4/30 10:16
 **/
@Slf4j
public class LeaveDemoTest extends LearnFlowableApplicationTests {
    @Resource
    RepositoryService repositoryService;
    @Resource
    RuntimeService runtimeService;
    @Resource
    TaskService taskService;

    @Test
    @DisplayName("部署流程")
    public void deployProcesses() {
        long count = repositoryService.createProcessDefinitionQuery()
                .processDefinitionName("ask_for_leave").count();
        if (count > 0) {
            log.info("开始部署流程");

            repositoryService.createDeployment()
                    .addClasspathResource("processes_attachement/ask_for_leave.bpmn21.xml")
                    .name("请假流程")
                    .deploy();

            log.info("部署流程完成");
        } else {
            log.info("流程已经部署");
        }
    }

    @Test
    @DisplayName("直接部署流程")
    public void deployProcessesDirect() {
        repositoryService.createDeployment()
                .addClasspathResource("processes/leave.bpmn20.xml")
                .name("ask_for_leave")
                .deploy();
        log.info("流程已经部署");
    }

    @Test
    @DisplayName("查询流程")
    public void queryProcesses() {
        repositoryService.createProcessDefinitionQuery()
                .list()
                .forEach(processDefinition -> {
                    log.info("流程名称:{},流程key:{},流程版本:{},流程部署id:{}",
                            processDefinition.getName(),
                            processDefinition.getKey(),
                            processDefinition.getVersion(),
                            processDefinition.getDeploymentId());
                });
    }

    @Test
    @DisplayName("删除流程")
    public void deleteProcesses() {
        repositoryService.createProcessDefinitionQuery()
                .list()
                .forEach(processDefinition -> {
                    repositoryService.deleteDeployment(processDefinition.getDeploymentId());
                });
    }

    @Test
    @DisplayName("启动流程")
    public void startProcess() {
        String staffId = "1000";

        HashMap<String, Object> map = new HashMap<>();
        map.put("leaveTask", staffId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ask_for_leave", map);
        // 姓名
        runtimeService.setVariable(processInstance.getId(), "name", "tc");
        // 请假原因
        runtimeService.setVariable(processInstance.getId(), "reason", "休息一下");
        // 请假天数
        runtimeService.setVariable(processInstance.getId(), "days", 10);
        // runtimeService.setVariables(processInstance.getId(), new HashMap<>());
        log.info("创建请假流程 processId：{}", processInstance.getId());
    }

    /**
     * 提交给组长审批
     */
    @Test
    void submitToZuzhang() {
        String staffId = "1000";
        String zuzhangId = "90";

        //员 工查找到自己的任务，然后提交给组长审批
        List<Task> list = taskService.createTaskQuery().taskAssignee(staffId).orderByTaskId().desc().list();
        for (Task task : list) {
            log.info("任务 ID：{}；任务处理人：{}；任务是否挂起：{}", task.getId(), task.getAssignee(), task.isSuspended());
            Map<String, Object> map = new HashMap<>();
            //提交给组长的时候，需要指定组长的 id
            map.put("zuzhangTask", zuzhangId);
            taskService.complete(task.getId(), map);
        }
    }

    /**
     * 组长审批-批准
     */
    @Test
    void zuZhangApprove() {
        String zuzhangId = "90";
        String managerId = "77";
        // 组长认为是否审批通过
        boolean isApproveSuccess = false;

        List<Task> list = taskService.createTaskQuery().taskAssignee(zuzhangId).orderByTaskId().desc().list();
        for (Task task : list) {
            log.info("组长 {} 在审批 {} 任务", task.getAssignee(), task.getId());
            Map<String, Object> map = new HashMap<>();
            if (isApproveSuccess) {
                // 组长审批的时候，如果是同意，需要指定经理的 id
                map.put("managerTask", managerId);
                map.put("checkResult", "通过");
            } else {
                // 组长审批的时候，如果是拒绝，就不需要指定经理的 id
                map.put("checkResult", "拒绝");
            }

            taskService.complete(task.getId(), map);
        }
    }

    /**
     * 经理审批自己的任务-批准
     */
    @Test
    void managerApprove() {
        String managerId = "77";

        List<Task> list = taskService.createTaskQuery().taskAssignee(managerId).orderByTaskId().desc().list();
        for (Task task : list) {
            log.info("经理 {} 在审批 {} 任务", task.getAssignee(), task.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("checkResult", "通过");
            taskService.complete(task.getId(), map);
        }
    }

}

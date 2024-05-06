package com.tc.learnflowable.controller;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DiagramController {

    @Autowired
    RuntimeService runtimeService;
    @Autowired
    TaskService taskService;
    @Autowired
    RepositoryService repositoryService;
    @Autowired
    ProcessEngine processEngine;
    @Autowired
    HistoryService historyService;

    /**
     * 通过流程定义获取流程图
     */
    @GetMapping("/diagram")
    public void showDiagramDefinition(HttpServletResponse resp, String definitionKey) throws Exception {
        // 通过流程定义key找到流程定义信息
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(definitionKey)
                .orderByProcessDefinitionVersion().desc()
                .listPage(0, 1);
        if (null == processDefinitions || processDefinitions.isEmpty()) {
            throw new Exception("流程定义key未找到！~");
        }
        ProcessDefinition processDefinition = processDefinitions.get(0);

        // 通过流程定义id获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        // 获取流程引擎配置
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        // 获取流程图生成器
        // DefaultProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();

        ArrayList<String> highLightedActivities = new ArrayList<>();
        ArrayList<String> highLightedFlows = new ArrayList<>();
        // 生成流程图片流
        InputStream inputStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivities, highLightedFlows,
                engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(),
                engconf.getClassLoader(), 1.0, true);
        writeOutputStream(resp, inputStream);
    }

    /**
     * 通过流程实例id获取流程图
     */
    @GetMapping("/pic")
    public void showPic(HttpServletResponse resp, String processId) throws Exception {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        if (pi == null) {
            return;
        }

        // 当前流程执行的情况
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(processId)
                .list();

        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        /*
         * 生成流程图
         */
        generateDiagram(resp, pi.getProcessDefinitionId(), activityIds, flows);
    }

    /**
     * 通过历史流程实例id获取历史流程图
     */
    @GetMapping("/pic2")
    public void showPic2(HttpServletResponse resp, String processId) throws Exception {
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            String activityId = historicActivityInstance.getActivityId();
            activityIds.add(activityId);
            flows.add(activityId);
            historicActivityInstance.getProcessDefinitionId();
        }

        HistoricActivityInstance historicActivityInstance = historicActivityInstances.get(0);
        String processDefinitionId = historicActivityInstance.getProcessDefinitionId();

        /*
         * 生成流程图
         */
        generateDiagram(resp, processDefinitionId, activityIds, flows);
    }

    private void generateDiagram(HttpServletResponse resp, String processDefinitionId, List<String> activityIds, List<String> flows) throws IOException {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        /*
         * 参数说明：
         *      bpmnModel：BPMN 模型的实例，表示业务流程模型。
         *      imageType：一个字符串，表示要生成的图像类型，例如 "png"、"jpg" 等。
         *      highLightedActivities：包含要在图表中突出显示的活动的 ID 的字符串列表。
         *      highLightedFlows：包含要在图表中突出显示的流程的 ID 的字符串列表。
         *      activityFontName：指定用于活动标签的字体的字符串。
         *      labelFontName：指定用于其他标签的字体的字符串。
         *      annotationFontName：指定用于注释的字体的字符串。
         *      customClassLoader：用于加载自定义字体的可选 ClassLoader 实例。
         *      scaleFactor：表示图像的缩放比例的双精度值。
         *      drawSequenceFlowNameWithNoLabelDI：一个布尔值，指示是否在没有标签时绘制序列流名称。
         */
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows,
                engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(),
                engconf.getClassLoader(), 1.0, true);
        writeOutputStream(resp, in);
    }

    private static void writeOutputStream(HttpServletResponse resp, InputStream in) throws IOException {
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = resp.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}

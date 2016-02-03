/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.api.runtime.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.junit.Assert;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigrationTestRule extends TestWatcher {

  protected ProcessEngineRule processEngineRule;
  protected ProcessEngine processEngine;

  public MigrationTestRule(ProcessEngineRule processEngineRule) {
    this.processEngineRule = processEngineRule;
  }

  @Override
  protected void starting(Description description) {
    this.processEngine = processEngineRule.getProcessEngine();
  }

  @Override
  protected void finished(Description description) {
    this.processEngine = null;
  }

  public void assertProcessEnded(String processInstanceId) {
    ProcessInstance processInstance = processEngine
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    if (processInstance!=null) {
      Assert.fail("Process instance with id " + processInstanceId + " is not finished");
    }
  }

  public ProcessDefinition findProcessDefinition(String key, int version) {
    return processEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(key)
        .processDefinitionVersion(version)
        .singleResult();
  }

  public String deploy(String name, BpmnModelInstance bpmnModel) {
    Deployment deployment = processEngine.getRepositoryService()
      .createDeployment()
      .addModelInstance(name, bpmnModel)
      .deploy();

    processEngineRule.manageDeployment(deployment);

    return deployment.getId();
  }

  public BpmnModelInstance withExecutionListener(BpmnModelInstance originalInstance,
      Class<? extends ExecutionListener> listenerClass,
      String flowNodeId,
      String event) {
    BpmnModelInstance updatedModel = originalInstance.clone();

    CamundaExecutionListener executionListener = updatedModel.newInstance(CamundaExecutionListener.class);
    executionListener.setCamundaClass(listenerClass.getCanonicalName());

    updatedModel
      .<FlowNode>getModelElementById(flowNodeId)
      .builder().addExtensionElement(executionListener);

    return updatedModel;

  }
}

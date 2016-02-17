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
package org.camunda.bpm.engine.impl.migration.instance;

import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.delegate.CompositeActivityBehavior;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigratingScopeActivityInstance extends MigratingActivityInstance {

  @Override
  public ExecutionEntity resolveRepresentativeExecution() {
    // scope executions are stable, so we don't follow the replacedBy links here
    return representativeExecution;
  }

  @Override
  public void detachState() {
    ExecutionEntity currentScopeExecution = resolveRepresentativeExecution();

    ExecutionEntity parentExecution = currentScopeExecution.getParent();
    ExecutionEntity parentScopeExecution = parentExecution.isConcurrent() ? parentExecution.getParent() : parentExecution;
    currentScopeExecution.setParent(null);


    if (parentExecution.isConcurrent()) {
      parentExecution.remove();
      parentScopeExecution.tryPruneLastConcurrentChild();
      parentScopeExecution.forceUpdate();
    }
    else {
      if (sourceScope.getActivityBehavior() instanceof CompositeActivityBehavior) {
        parentExecution.leaveActivityInstance();
      }
    }
  }

  @Override
  public void attachState(ExecutionEntity newScopeExecution) {

    ExecutionEntity newParentExecution = newScopeExecution;
    if (!newScopeExecution.getNonEventScopeExecutions().isEmpty()) {
      newParentExecution = (ExecutionEntity) newScopeExecution.createConcurrentExecution();
      newScopeExecution.forceUpdate();
    }

    ExecutionEntity currentScopeExecution = resolveRepresentativeExecution();
    currentScopeExecution.setParent(newParentExecution);

    if (sourceScope.getActivityBehavior() instanceof CompositeActivityBehavior) {
      newParentExecution.setActivityInstanceId(activityInstance.getId());
    }
  }

  @Override
  public void migrateState() {
    ExecutionEntity currentScopeExecution = resolveRepresentativeExecution();
    currentScopeExecution.setProcessDefinition(targetScope.getProcessDefinition());

    ExecutionEntity parentExecution = currentScopeExecution.getParent();

    if (parentExecution != null && parentExecution.isConcurrent()) {
      parentExecution.setProcessDefinition(targetScope.getProcessDefinition());
    }

    if (!currentScopeExecution.isProcessInstanceExecution()) {
      currentScopeExecution.setActivity((PvmActivity) targetScope);
    }

    currentScopeExecution = removeExecutionIfNotScopeAnymore(currentScopeExecution);

    if (!isLeafActivity()) {
      currentScopeExecution.setActivity(null);
    }
  }

  @Override
  public void remove() {
    parentInstance.getChildren().remove(this);
    for (MigratingActivityInstance child : childInstances) {
      child.parentInstance = null;
    }

    ExecutionEntity currentExecution = resolveRepresentativeExecution();
    ExecutionEntity parentExecution = currentExecution.getParent();

    currentExecution.setActivity((PvmActivity) sourceScope);
    currentExecution.setActivityInstanceId(activityInstance.getId());

    currentExecution.deleteCascade("migration");

    if (parentExecution.isConcurrent()) {
      ExecutionEntity grandParent = parentExecution.getParent();
      parentExecution.remove();
      grandParent.tryPruneLastConcurrentChild();
      grandParent.forceUpdate();
    }
  }

  protected ExecutionEntity removeExecutionIfNotScopeAnymore(ExecutionEntity currentScopeExecution) {
    if (!targetScope.isScope()) {
      for (MigratingInstance dependentInstance : migratingDependentInstances) {
        dependentInstance.detachState();
      }

      ExecutionEntity parentExecution = currentScopeExecution.getParent();

      parentExecution.setActivity(currentScopeExecution.getActivity());
      parentExecution.setActivityInstanceId(currentScopeExecution.getActivityInstanceId());

      currentScopeExecution.remove();
      currentScopeExecution = parentExecution;

      representativeExecution = currentScopeExecution;
      for (MigratingInstance dependentInstance : migratingDependentInstances) {
        dependentInstance.attachState(currentScopeExecution);
      }
    }

    return currentScopeExecution;
  }

  protected boolean isLeafActivity() {
    return targetScope.getActivities().isEmpty();
  }
}

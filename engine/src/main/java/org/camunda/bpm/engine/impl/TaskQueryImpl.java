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
package org.camunda.bpm.engine.impl;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;
import org.camunda.bpm.engine.impl.persistence.entity.SuspensionState;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.variable.VariableTypes;
import org.camunda.bpm.engine.task.DelegationState;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;

/**
 * @author Joram Barrez
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class TaskQueryImpl extends AbstractQuery<TaskQuery, Task> implements TaskQuery {

  private static final long serialVersionUID = 1L;
  protected String taskId;
  protected String name;
  protected String nameLike;
  protected String description;
  protected String descriptionLike;
  protected Integer priority;
  protected Integer minPriority;
  protected Integer maxPriority;
  protected String assignee;
  protected String assigneeLike;
  protected String involvedUser;
  protected String owner;
  protected boolean unassigned = false;
  protected boolean noDelegationState = false;
  protected DelegationState delegationState;
  protected String candidateUser;
  protected String candidateGroup;
  protected List<String> candidateGroups;
  protected String processInstanceId;
  protected String executionId;
  protected String[] activityInstanceIdIn;
  protected Date createTime;
  protected Date createTimeBefore;
  protected Date createTimeAfter;
  protected String key;
  protected String keyLike;
  protected String processDefinitionKey;
  protected String processDefinitionId;
  protected String processDefinitionName;
  protected String processDefinitionNameLike;
  protected String processInstanceBusinessKey;
  protected String processInstanceBusinessKeyLike;
  protected List<TaskQueryVariableValue> variables = new ArrayList<TaskQueryVariableValue>();
  protected Date dueDate;
  protected Date dueBefore;
  protected Date dueAfter;
  protected Date followUpDate;
  protected Date followUpBefore;
  protected Date followUpAfter;
  protected boolean excludeSubtasks = false;
  protected SuspensionState suspensionState;
  protected boolean initializeFormKeys = false;

  // case management /////////////////////////////
  protected String caseDefinitionKey;
  protected String caseDefinitionId;
  protected String caseDefinitionName;
  protected String caseDefinitionNameLike;
  protected String caseInstanceId;
  protected String caseInstanceBusinessKey;
  protected String caseInstanceBusinessKeyLike;
  protected String caseExecutionId;


  public TaskQueryImpl() {
  }

  public TaskQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public TaskQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  public TaskQueryImpl taskId(String taskId) {
    ensureNotNull("Task id", taskId);
    this.taskId = taskId;
    return this;
  }

  public TaskQueryImpl taskName(String name) {
    this.name = name;
    return this;
  }

  public TaskQueryImpl taskNameLike(String nameLike) {
    ensureNotNull("Task nameLike", nameLike);
    this.nameLike = nameLike;
    return this;
  }

  public TaskQueryImpl taskDescription(String description) {
    ensureNotNull("Description", description);
    this.description = description;
    return this;
  }

  public TaskQuery taskDescriptionLike(String descriptionLike) {
    ensureNotNull("Task descriptionLike", descriptionLike);
    this.descriptionLike = descriptionLike;
    return this;
  }

  public TaskQuery taskPriority(Integer priority) {
    ensureNotNull("Priority", priority);
    this.priority = priority;
    return this;
  }

  public TaskQuery taskMinPriority(Integer minPriority) {
    ensureNotNull("Min Priority", minPriority);
    this.minPriority = minPriority;
    return this;
  }

  public TaskQuery taskMaxPriority(Integer maxPriority) {
    ensureNotNull("Max Priority", maxPriority);
    this.maxPriority = maxPriority;
    return this;
  }

  public TaskQueryImpl taskAssignee(String assignee) {
    ensureNotNull("Assignee", assignee);
    this.assignee = assignee;
    expressions.remove("taskAssignee");
    return this;
  }

  public TaskQuery taskAssigneeExpression(String assigneeExpression) {
    ensureNotNull("Assignee expression", assigneeExpression);
    expressions.put("taskAssignee", assigneeExpression);
    return this;
  }

  public TaskQuery taskAssigneeLike(String assignee) {
    ensureNotNull("Assignee", assignee);
    this.assigneeLike = assignee;
    expressions.remove("taskAssigneeLike");
    return this;
  }

  public TaskQuery taskAssigneeLikeExpression(String assigneeLikeExpression) {
    ensureNotNull("Assignee like expression", assigneeLikeExpression);
    expressions.put("taskAssigneeLike", assigneeLikeExpression);
    return this;
  }

  public TaskQueryImpl taskOwner(String owner) {
    ensureNotNull("Owner", owner);
    this.owner = owner;
    expressions.remove("taskOwner");
    return this;
  }

  public TaskQuery taskOwnerExpression(String ownerExpression) {
    ensureNotNull("Owner expression", ownerExpression);
    expressions.put("taskOwner", ownerExpression);
    return this;
  }

  /** @see {@link #taskUnassigned} */
  @Deprecated
  public TaskQuery taskUnnassigned() {
    return taskUnassigned();
  }

  public TaskQuery taskUnassigned() {
    this.unassigned = true;
    return this;
  }

  public TaskQuery taskDelegationState(DelegationState delegationState) {
    if (delegationState == null) {
      this.noDelegationState = true;
    } else {
      this.delegationState = delegationState;
    }
    return this;
  }

  public TaskQueryImpl taskCandidateUser(String candidateUser) {
    ensureNotNull("Candidate user", candidateUser);

    if (candidateGroup != null || expressions.containsKey("candidateGroup")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateUser and candidateGroup");
    }
    if (candidateGroups != null || expressions.containsKey("candidateGroups")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateUser and candidateGroupIn");
    }
    this.candidateUser = candidateUser;
    expressions.remove("taskCandidateUser");
    return this;
  }

  public TaskQuery taskCandidateUserExpression(String candidateUserExpression) {
    ensureNotNull("Candidate user expression", candidateUserExpression);

    if (candidateGroup != null || expressions.containsKey("candidateGroup")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateUser and candidateGroup");
    }
    if (candidateGroups != null || expressions.containsKey("candidateGroups")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateUser and candidateGroupIn");
    }

    expressions.put("taskCandidateUser", candidateUserExpression);
    return this;
  }

  public TaskQueryImpl taskInvolvedUser(String involvedUser) {
    ensureNotNull("Involved user", involvedUser);
    this.involvedUser = involvedUser;
    expressions.remove("taskInvolvedUser");
    return this;
  }

  public TaskQuery taskInvolvedUserExpression(String involvedUserExpression) {
    ensureNotNull("Involved user expression", involvedUserExpression);
    expressions.put("taskInvolvedUser", involvedUserExpression);
    return this;
  }

  public TaskQueryImpl taskCandidateGroup(String candidateGroup) {
    ensureNotNull("Candidate group", candidateGroup);

    if (candidateUser != null || expressions.containsKey("candidateUser")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroup and candidateUser");
    }
    if (candidateGroups != null || expressions.containsKey("candidateGroups")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroup and candidateGroupIn");
    }
    this.candidateGroup = candidateGroup;
    expressions.remove("taskCandidateGroup");
    return this;
  }

  public TaskQuery taskCandidateGroupExpression(String candidateGroupExpression) {
    ensureNotNull("Candidate group expression", candidateGroupExpression);

    if (candidateUser != null || expressions.containsKey("candidateUser")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroup and candidateUser");
    }
    if (candidateGroups != null || expressions.containsKey("candidateGroups")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroup and candidateGroupIn");
    }

    expressions.put("taskCandidateGroup", candidateGroupExpression);
    return this;
  }

  public TaskQuery taskCandidateGroupIn(List<String> candidateGroups) {
    ensureNotEmpty("Candidate group list", candidateGroups);

    if (candidateUser != null || expressions.containsKey("candidateUser")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroupIn and candidateUser");
    }
    if (candidateGroup != null || expressions.containsKey("candidateGroup")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroupIn and candidateGroup");
    }

    this.candidateGroups = candidateGroups;
    expressions.remove("taskCandidateGroupIn");
    return this;
  }

  public TaskQuery taskCandidateGroupInExpression(String candidateGroupsExpression) {
    ensureNotEmpty("Candidate group list expression", candidateGroupsExpression);

    if (candidateUser != null || expressions.containsKey("candidateUser")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroupIn and candidateUser");
    }
    if (candidateGroup != null || expressions.containsKey("candidateGroup")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroupIn and candidateGroup");
    }

    expressions.put("taskCandidateGroupIn", candidateGroupsExpression);
    return this;
  }

  public TaskQueryImpl processInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
    return this;
  }

  public TaskQueryImpl processInstanceBusinessKey(String processInstanceBusinessKey) {
    this.processInstanceBusinessKey = processInstanceBusinessKey;
    return this;
  }

  public TaskQuery processInstanceBusinessKeyLike(String processInstanceBusinessKey) {
  	this.processInstanceBusinessKeyLike = processInstanceBusinessKey;
  	return this;
  }

  public TaskQueryImpl executionId(String executionId) {
    this.executionId = executionId;
    return this;
  }

  public TaskQuery activityInstanceIdIn(String... activityInstanceIds) {
    this.activityInstanceIdIn = activityInstanceIds;
    return this;
  }

  public TaskQueryImpl taskCreatedOn(Date createTime) {
    this.createTime = createTime;
    expressions.remove("taskCreatedOn");
    return this;
  }

  public TaskQuery taskCreatedOnExpression(String createTimeExpression) {
    expressions.put("taskCreatedOn", createTimeExpression);
    return this;
  }

  public TaskQuery taskCreatedBefore(Date before) {
    this.createTimeBefore = before;
    expressions.remove("taskCreatedBefore");
    return this;
  }

  public TaskQuery taskCreatedBeforeExpression(String beforeExpression) {
    expressions.put("taskCreatedBefore", beforeExpression);
    return this;
  }

  public TaskQuery taskCreatedAfter(Date after) {
    this.createTimeAfter = after;
    expressions.remove("taskCreatedAfter");
    return this;
  }

  public TaskQuery taskCreatedAfterExpression(String afterExpression) {
    expressions.put("taskCreatedAfter", afterExpression);
    return this;
  }

  public TaskQuery taskDefinitionKey(String key) {
    this.key = key;
    return this;
  }

  public TaskQuery taskDefinitionKeyLike(String keyLike) {
    this.keyLike = keyLike;
    return this;
  }

  public TaskQuery caseInstanceId(String caseInstanceId) {
    ensureNotNull("caseInstanceId", caseInstanceId);
    this.caseInstanceId = caseInstanceId;
    return this;
  }

  public TaskQuery caseInstanceBusinessKey(String caseInstanceBusinessKey) {
    ensureNotNull("caseInstanceBusinessKey", caseInstanceBusinessKey);
    this.caseInstanceBusinessKey = caseInstanceBusinessKey;
    return this;
  }

  public TaskQuery caseInstanceBusinessKeyLike(String caseInstanceBusinessKeyLike) {
    ensureNotNull("caseInstanceBusinessKeyLike", caseInstanceBusinessKeyLike);
    this.caseInstanceBusinessKeyLike = caseInstanceBusinessKeyLike;
    return this;
  }

  public TaskQuery caseExecutionId(String caseExecutionId) {
    ensureNotNull("caseExecutionId", caseExecutionId);
    this.caseExecutionId = caseExecutionId;
    return this;
  }

  public TaskQuery caseDefinitionId(String caseDefinitionId) {
    ensureNotNull("caseDefinitionId", caseDefinitionId);
    this.caseDefinitionId = caseDefinitionId;
    return this;
  }

  public TaskQuery caseDefinitionKey(String caseDefinitionKey) {
    ensureNotNull("caseDefinitionKey", caseDefinitionKey);
    this.caseDefinitionKey = caseDefinitionKey;
    return this;
  }

  public TaskQuery caseDefinitionName(String caseDefinitionName) {
    ensureNotNull("caseDefinitionName", caseDefinitionName);
    this.caseDefinitionName = caseDefinitionName;
    return this;
  }

  public TaskQuery caseDefinitionNameLike(String caseDefinitionNameLike) {
    ensureNotNull("caseDefinitionNameLike", caseDefinitionNameLike);
    this.caseDefinitionNameLike = caseDefinitionNameLike;
    return this;
  }

  public TaskQuery taskVariableValueEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.EQUALS, true, false);
    return this;
  }

  public TaskQuery taskVariableValueNotEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.NOT_EQUALS, true, false);
    return this;
  }

  public TaskQuery taskVariableValueLike(String variableName, String variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LIKE, true, false);
  	return this;
  }

  public TaskQuery taskVariableValueGreaterThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN, true, false);
  	return this;
  }

  public TaskQuery taskVariableValueGreaterThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN_OR_EQUAL, true, false);
  	return this;
  }

  public TaskQuery taskVariableValueLessThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN, true, false);
  	return this;
  }

  public TaskQuery taskVariableValueLessThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN_OR_EQUAL, true, false);
  	return this;
  }

  public TaskQuery processVariableValueEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.EQUALS, false, true);
    return this;
  }

  public TaskQuery processVariableValueNotEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.NOT_EQUALS, false, true);
    return this;
  }

  public TaskQuery processVariableValueLike(String variableName, String variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LIKE, false, true);
  	return this;
  }

  public TaskQuery processVariableValueGreaterThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN, false, true);
  	return this;
  }

  public TaskQuery processVariableValueGreaterThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN_OR_EQUAL, false, true);
  	return this;
  }

  public TaskQuery processVariableValueLessThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN, false, true);
  	return this;
  }

  public TaskQuery processVariableValueLessThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN_OR_EQUAL, false, true);
  	return this;
  }

  public TaskQuery caseInstanceVariableValueEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.EQUALS, false, false);
    return this;
  }

  public TaskQuery caseInstanceVariableValueNotEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.NOT_EQUALS, false, false);
    return this;
  }

  public TaskQuery caseInstanceVariableValueLike(String variableName, String variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LIKE, false, false);
    return this;
  }

  public TaskQuery caseInstanceVariableValueGreaterThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN, false, false);
    return this;
  }

  public TaskQuery caseInstanceVariableValueGreaterThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN_OR_EQUAL, false, false);
    return this;
  }

  public TaskQuery caseInstanceVariableValueLessThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN, false, false);
    return this;
  }

  public TaskQuery caseInstanceVariableValueLessThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN_OR_EQUAL, false, false);
    return this;
  }

  public TaskQuery processDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  public TaskQuery processDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  public TaskQuery processDefinitionName(String processDefinitionName) {
    this.processDefinitionName = processDefinitionName;
    return this;
  }

  public TaskQuery processDefinitionNameLike(String processDefinitionName) {
  	this.processDefinitionNameLike = processDefinitionName;
  	return this;
  }

  public TaskQuery dueDate(Date dueDate) {
    this.dueDate = dueDate;
    expressions.remove("dueDate");
    return this;
  }

  public TaskQuery dueDateExpression(String dueDateExpression) {
    expressions.put("dueDate", dueDateExpression);
    return this;
  }

  public TaskQuery dueBefore(Date dueBefore) {
    this.dueBefore = dueBefore;
    expressions.remove("dueBefore");
    return this;
  }

  public TaskQuery dueBeforeExpression(String dueDate) {
    expressions.put("dueBefore", dueDate);
    return this;
  }

  public TaskQuery dueAfter(Date dueAfter) {
    this.dueAfter = dueAfter;
    expressions.remove("dueAfter");
    return this;
  }

  public TaskQuery dueAfterExpression(String dueDateExpression) {
    expressions.put("dueAfter", dueDateExpression);
    return this;
  }

  public TaskQuery followUpDate(Date followUpDate) {
    this.followUpDate = followUpDate;
    expressions.remove("followUpDate");
    return this;
  }

  public TaskQuery followUpDateExpression(String followUpDateExpression) {
    expressions.put("followUpDate", followUpDateExpression);
    return this;
  }

  public TaskQuery followUpBefore(Date followUpDate) {
    this.followUpBefore = followUpDate;
    expressions.remove("followUpBefore");
    return this;
  }

  public TaskQuery followUpBeforeExpression(String followUpDateExpression) {
    expressions.put("followUpBefore", followUpDateExpression);
    return this;
  }

  public TaskQuery followUpAfter(Date followUpDate) {
    this.followUpAfter = followUpDate;
    expressions.remove("followUpAfter");
    return this;
  }

  public TaskQuery followUpAfterExpression(String followUpDateExpression) {
    expressions.put("followUpAfter", followUpDateExpression);
    return this;
  }

  public TaskQuery excludeSubtasks() {
    this.excludeSubtasks = true;
    return this;
  }

  public TaskQuery active() {
    this.suspensionState = SuspensionState.ACTIVE;
    return this;
  }

  public TaskQuery suspended() {
    this.suspensionState = SuspensionState.SUSPENDED;
    return this;
  }

  public TaskQuery initializeFormKeys() {
    this.initializeFormKeys = true;
    return this;
  }

  public List<String> getCandidateGroups() {
    if (candidateGroup!=null) {
      return Collections.singletonList(candidateGroup);
    } else if (candidateUser != null) {
      return getGroupsForCandidateUser(candidateUser);
    } else if(candidateGroups != null) {
      return candidateGroups;
    }
    return null;
  }

  protected List<String> getGroupsForCandidateUser(String candidateUser) {
    // TODO: Discuss about removing this feature? Or document it properly and maybe recommend to not use it
    // and explain alternatives
    List<Group> groups = Context
      .getCommandContext()
      .getReadOnlyIdentityProvider()
      .createGroupQuery()
      .groupMember(candidateUser)
      .list();
    List<String> groupIds = new ArrayList<String>();
    for (Group group : groups) {
      groupIds.add(group.getId());
    }
    return groupIds;
  }

  protected void ensureVariablesInitialized() {
    VariableTypes types = Context.getProcessEngineConfiguration().getVariableTypes();
    for(QueryVariableValue var : variables) {
      var.initialize(types);
    }
  }

  public void addVariable(String name, Object value, QueryOperator operator, boolean isTaskVariable, boolean isProcessInstanceVariable) {
    ensureNotNull("name", name);

    if(value == null || isBoolean(value)) {
      // Null-values and booleans can only be used in EQUALS and NOT_EQUALS
      switch(operator) {
      case GREATER_THAN:
        throw new ProcessEngineException("Booleans and null cannot be used in 'greater than' condition");
      case LESS_THAN:
        throw new ProcessEngineException("Booleans and null cannot be used in 'less than' condition");
      case GREATER_THAN_OR_EQUAL:
        throw new ProcessEngineException("Booleans and null cannot be used in 'greater than or equal' condition");
      case LESS_THAN_OR_EQUAL:
        throw new ProcessEngineException("Booleans and null cannot be used in 'less than or equal' condition");
      case LIKE:
        throw new ProcessEngineException("Booleans and null cannot be used in 'like' condition");
      default:
        break;
      }
    }
    variables.add(new TaskQueryVariableValue(name, value, operator, isTaskVariable, isProcessInstanceVariable));
  }

  private boolean isBoolean(Object value) {
  	if (value == null) {
  	  return false;
  	}
  	return Boolean.class.isAssignableFrom(value.getClass()) || boolean.class.isAssignableFrom(value.getClass());
	}

  //ordering ////////////////////////////////////////////////////////////////

  public TaskQuery orderByTaskId() {
    return orderBy(TaskQueryProperty.TASK_ID);
  }

  public TaskQuery orderByTaskName() {
    return orderBy(TaskQueryProperty.NAME);
  }

  public TaskQuery orderByTaskDescription() {
    return orderBy(TaskQueryProperty.DESCRIPTION);
  }

  public TaskQuery orderByTaskPriority() {
    return orderBy(TaskQueryProperty.PRIORITY);
  }

  public TaskQuery orderByProcessInstanceId() {
    return orderBy(TaskQueryProperty.PROCESS_INSTANCE_ID);
  }

  public TaskQuery orderByCaseInstanceId() {
    return orderBy(TaskQueryProperty.CASE_INSTANCE_ID);
  }

  public TaskQuery orderByExecutionId() {
    return orderBy(TaskQueryProperty.EXECUTION_ID);
  }

  public TaskQuery orderByCaseExecutionId() {
    return orderBy(TaskQueryProperty.CASE_EXECUTION_ID);
  }

  public TaskQuery orderByTaskAssignee() {
    return orderBy(TaskQueryProperty.ASSIGNEE);
  }

  public TaskQuery orderByTaskCreateTime() {
    return orderBy(TaskQueryProperty.CREATE_TIME);
  }

  public TaskQuery orderByDueDate() {
    return orderBy(TaskQueryProperty.DUE_DATE);
  }

  public TaskQuery orderByFollowUpDate() {
    return orderBy(TaskQueryProperty.FOLLOW_UP_DATE);
  }

  //results ////////////////////////////////////////////////////////////////

  public List<Task> executeList(CommandContext commandContext, Page page) {
    ensureVariablesInitialized();
    checkQueryOk();
    List<Task> taskList = commandContext
      .getTaskManager()
      .findTasksByQueryCriteria(this);

    if(initializeFormKeys) {
      for (Task task : taskList) {
        // initialize the form keys of the tasks
        ((TaskEntity) task).initializeFormKey();
      }
    }

    return taskList;
  }

  public long executeCount(CommandContext commandContext) {
    ensureVariablesInitialized();
    checkQueryOk();
    return commandContext
      .getTaskManager()
      .findTaskCountByQueryCriteria(this);
  }

  //getters ////////////////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public String getNameLike() {
    return nameLike;
  }

  public String getAssignee() {
    return assignee;
  }

  public String getAssigneeLike() {
    return assigneeLike;
  }

  public String getInvolvedUser() {
    return involvedUser;
  }

  public String getOwner() {
    return owner;
  }

  public boolean isUnassigned() {
    return unassigned;
  }

  public DelegationState getDelegationState() {
    return delegationState;
  }

  public boolean isNoDelegationState() {
    return noDelegationState;
  }

  public String getDelegationStateString() {
    return (delegationState!=null ? delegationState.toString() : null);
  }

  public String getCandidateUser() {
    return candidateUser;
  }

  public String getCandidateGroup() {
    return candidateGroup;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public String getExecutionId() {
    return executionId;
  }

  public String[] getActivityInstanceIdIn() {
    return activityInstanceIdIn;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getDescription() {
    return description;
  }

  public String getDescriptionLike() {
    return descriptionLike;
  }

  public Integer getPriority() {
    return priority;
  }

  public Integer getMinPriority() {
    return minPriority;
  }

  public Integer getMaxPriority() {
    return maxPriority;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public Date getCreateTimeBefore() {
    return createTimeBefore;
  }

  public Date getCreateTimeAfter() {
    return createTimeAfter;
  }

  public String getKey() {
    return key;
  }

  public String getKeyLike() {
    return keyLike;
  }

  public List<TaskQueryVariableValue> getVariables() {
    return variables;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public String getProcessDefinitionName() {
    return processDefinitionName;
  }

  public String getProcessDefinitionNameLike() {
    return processDefinitionNameLike;
  }

  public String getProcessInstanceBusinessKey() {
    return processInstanceBusinessKey;
  }

  public String getProcessInstanceBusinessKeyLike() {
    return processInstanceBusinessKeyLike;
  }

  public Date getDueDate() {
    return dueDate;
  }

  public Date getDueBefore() {
    return dueBefore;
  }

  public Date getDueAfter() {
    return dueAfter;
  }

  public Date getFollowUpDate() {
    return followUpDate;
  }

  public Date getFollowUpBefore() {
    return followUpBefore;
  }

  public Date getFollowUpAfter() {
    return followUpAfter;
  }

  public boolean isExcludeSubtasks() {
    return excludeSubtasks;
  }

  public SuspensionState getSuspensionState() {
    return suspensionState;
  }

  public String getCaseInstanceId() {
    return caseInstanceId;
  }

  public String getCaseInstanceBusinessKey() {
    return caseInstanceBusinessKey;
  }

  public String getCaseInstanceBusinessKeyLike() {
    return caseInstanceBusinessKeyLike;
  }

  public String getCaseExecutionId() {
    return caseExecutionId;
  }

  public String getCaseDefinitionId() {
    return caseDefinitionId;
  }

  public String getCaseDefinitionKey() {
    return caseDefinitionKey;
  }

  public String getCaseDefinitionName() {
    return caseDefinitionName;
  }

  public String getCaseDefinitionNameLike() {
    return caseDefinitionNameLike;
  }

}

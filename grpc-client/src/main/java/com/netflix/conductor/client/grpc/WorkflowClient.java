package com.netflix.conductor.client.grpc;

import com.google.common.base.Preconditions;
import com.netflix.conductor.common.metadata.workflow.RerunWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.grpc.WorkflowServiceGrpc;
import com.netflix.conductor.grpc.WorkflowServicePb;
import com.netflix.conductor.proto.WorkflowPb;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WorkflowClient extends ClientBase {
    private WorkflowServiceGrpc.WorkflowServiceBlockingStub stub;

    public WorkflowClient(String address, int port) {
        super(address, port);
    }

    /**
     * Starts a workflow
     *
     * @param startWorkflowRequest the {@link StartWorkflowRequest} object to start the workflow
     * @return the id of the workflow instance that can be used for tracking
     */
    public String startWorkflow(StartWorkflowRequest startWorkflowRequest) {
        Preconditions.checkNotNull(startWorkflowRequest, "StartWorkflowRequest cannot be null");
        return stub.startWorkflow(
                protoMapper.toProto(startWorkflowRequest)
        ).getWorkflowId();
    }

    /**
     * Retrieve a workflow by workflow id
     *
     * @param workflowId   the id of the workflow
     * @param includeTasks specify if the tasks in the workflow need to be returned
     * @return the requested workflow
     */
    public Workflow getWorkflow(String workflowId, boolean includeTasks) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        WorkflowPb.Workflow workflow = stub.getWorkflowStatus(
                WorkflowServicePb.GetWorkflowStatusRequest.newBuilder()
                        .setWorkflowId(workflowId)
                        .setIncludeTasks(includeTasks)
                        .build()
        );
        return protoMapper.fromProto(workflow);
    }

    /**
     * Retrieve all workflows for a given correlation id and name
     *
     * @param name          the name of the workflow
     * @param correlationId the correlation id
     * @param includeClosed specify if all workflows are to be returned or only running workflows
     * @param includeTasks  specify if the tasks in the workflow need to be returned
     * @return list of workflows for the given correlation id and name
     */
    public List<Workflow> getWorkflows(String name, String correlationId, boolean includeClosed, boolean includeTasks) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(correlationId), "correlationId cannot be blank");

        WorkflowServicePb.GetWorkflowsResponse workflows = stub.getWorkflows(
                WorkflowServicePb.GetWorkflowsRequest.newBuilder()
                        .setName(name)
                        .addCorrelationId(correlationId)
                        .setIncludeClosed(includeClosed)
                        .setIncludeTasks(includeTasks)
                        .build()
        );

        if (!workflows.containsWorkflowsById(correlationId)) {
            return Collections.emptyList();
        }

        return workflows.getWorkflowsByIdOrThrow(correlationId)
                .getWorkflowsList().stream()
                .map(protoMapper::fromProto)
                .collect(Collectors.toList());
    }

    /**
     * Removes a workflow from the system
     *
     * @param workflowId      the id of the workflow to be deleted
     * @param archiveWorkflow flag to indicate if the workflow should be archived before deletion
     */
    public void deleteWorkflow(String workflowId, boolean archiveWorkflow) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "Workflow id cannot be blank");
        stub.removeWorkflow(
                WorkflowServicePb.RemoveWorkflowRequest.newBuilder()
                        .setWorkflodId(workflowId)
                        .setArchiveWorkflow(archiveWorkflow)
                        .build()
        );
    }

    /*
     * Retrieve all running workflow instances for a given name and version
     *
     * @param workflowName the name of the workflow
     * @param version      the version of the wokflow definition. Defaults to 1.
     * @return the list of running workflow instances
     */
    public List<String> getRunningWorkflow(String workflowName, Optional<Integer> version) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowName), "Workflow name cannot be blank");

        WorkflowServicePb.GetRunningWorkflowsResponse workflows = stub.getRunningWorkflows(
                WorkflowServicePb.GetRunningWorkflowsRequest.newBuilder()
                        .setName(workflowName)
                        .setVersion(version.orElse(1))
                        .build()
        );
        return workflows.getWorkflowIdsList();
    }

    /**
     * Retrieve all workflow instances for a given workflow name between a specific time period
     *
     * @param workflowName the name of the workflow
     * @param version      the version of the workflow definition. Defaults to 1.
     * @param startTime    the start time of the period
     * @param endTime      the end time of the period
     * @return returns a list of workflows created during the specified during the time period
     */
    public List<String> getWorkflowsByTimePeriod(String workflowName, int version, Long startTime, Long endTime) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowName), "Workflow name cannot be blank");
        Preconditions.checkNotNull(startTime, "Start time cannot be null");
        Preconditions.checkNotNull(endTime, "End time cannot be null");
        // TODO
        return null;
    }

    /*
     * Starts the decision task for the given workflow instance
     *
     * @param workflowId the id of the workflow instance
     */
    public void runDecider(String workflowId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        stub.decideWorkflow(WorkflowServicePb.WorkflowId.newBuilder()
                .setWorkflowId(workflowId)
                .build()
        );
    }

    /**
     * Pause a workflow by workflow id
     *
     * @param workflowId the workflow id of the workflow to be paused
     */
    public void pauseWorkflow(String workflowId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        stub.pauseWorkflow(WorkflowServicePb.WorkflowId.newBuilder()
                .setWorkflowId(workflowId)
                .build()
        );
    }

    /**
     * Resume a paused workflow by workflow id
     *
     * @param workflowId the workflow id of the paused workflow
     */
    public void resumeWorkflow(String workflowId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        stub.resumeWorkflow(WorkflowServicePb.WorkflowId.newBuilder()
                .setWorkflowId(workflowId)
                .build()
        );
    }

    /**
     * Skips a given task from a current RUNNING workflow
     *
     * @param workflowId        the id of the workflow instance
     * @param taskReferenceName the reference name of the task to be skipped
     */
    public void skipTaskFromWorkflow(String workflowId, String taskReferenceName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(taskReferenceName), "Task reference name cannot be blank");
        stub.skipTaskFromWorkflow(WorkflowServicePb.SkipTaskRequest.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskReferenceName(taskReferenceName)
                .build()
        );
    }

    /**
     * Reruns the workflow from a specific task
     *
     * @param rerunWorkflowRequest the request containing the task to rerun from
     * @return the id of the workflow
     */
    public String rerunWorkflow(RerunWorkflowRequest rerunWorkflowRequest) {
        Preconditions.checkNotNull(rerunWorkflowRequest, "RerunWorkflowRequest cannot be null");
        return stub.rerunWorkflow(
                protoMapper.toProto(rerunWorkflowRequest)
        ).getWorkflowId();
    }

    /**
     * Restart a completed workflow
     *
     * @param workflowId the workflow id of the workflow to be restarted
     */
    public void restart(String workflowId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        stub.restartWorkflow(WorkflowServicePb.WorkflowId.newBuilder()
                .setWorkflowId(workflowId)
                .build()
        );
    }

    /**
     * Retries the last failed task in a workflow
     *
     * @param workflowId the workflow id of the workflow with the failed task
     */
    public void retryLastFailedTask(String workflowId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        stub.retryWorkflow(WorkflowServicePb.WorkflowId.newBuilder()
                .setWorkflowId(workflowId)
                .build()
        );
    }


    /**
     * Resets the callback times of all IN PROGRESS tasks to 0 for the given workflow
     *
     * @param workflowId the id of the workflow
     */
    public void resetCallbacksForInProgressTasks(String workflowId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        stub.resetWorkflowCallbacks(WorkflowServicePb.WorkflowId.newBuilder()
                .setWorkflowId(workflowId)
                .build()
        );
    }

    /**
     * Terminates the execution of the given workflow instance
     *
     * @param workflowId the id of the workflow to be terminated
     * @param reason     the reason to be logged and displayed
     */
    public void terminateWorkflow(String workflowId, String reason) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "workflow id cannot be blank");
        stub.terminateWorkflow(WorkflowServicePb.TerminateWorkflowRequest.newBuilder()
                .setWorkflowId(workflowId)
                .setReason(reason)
                .build()
        );
    }
}

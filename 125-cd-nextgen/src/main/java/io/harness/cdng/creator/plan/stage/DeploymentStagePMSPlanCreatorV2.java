/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.stage;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.infrastructure.InfrastructurePmsPlanCreator;
import io.harness.cdng.creator.plan.service.ServicePlanCreator;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.steps.CdStepParametersUtils;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.AbstractStagePlanCreator;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.common.StageElementParameters.StageElementParametersBuilder;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.execution.utils.SkipInfoUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.YamlPipelineUtils;
import io.harness.when.utils.RunInfoUtils;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;

@OwnedBy(CDC)
public class DeploymentStagePMSPlanCreatorV2 extends AbstractStagePlanCreator<DeploymentStageNode> {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private ServicePlanCreator servicePlanCreator;

  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton("Deployment");
  }

  @Override
  public StepType getStepType(DeploymentStageNode stageElementConfig) {
    return DeploymentStageStep.STEP_TYPE;
  }

  @Override
  public SpecParameters getSpecParameters(String childNodeId, PlanCreationContext ctx, DeploymentStageNode stageNode) {
    return DeploymentStageStepParameters.getStepParameters(childNodeId);
  }

  @Override
  public Class<DeploymentStageNode> getFieldClass() {
    return DeploymentStageNode.class;
  }

  @SneakyThrows
  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, DeploymentStageNode stageNode, List<String> childrenNodeIds) {
    StageElementParametersBuilder stageParameters = CdStepParametersUtils.getStageParameters(stageNode);
    YamlField specField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));
    stageParameters.specConfig(getSpecParameters(specField.getNode().getUuid(), ctx, stageNode));
    return PlanNode.builder()
        .uuid(stageNode.getUuid())
        .name(stageNode.getName())
        .identifier(stageNode.getIdentifier())
        .group(StepOutcomeGroup.STAGE.name())
        .stepParameters(stageParameters.build())
        .stepType(getStepType(stageNode))
        .skipCondition(SkipInfoUtils.getSkipCondition(stageNode.getSkipCondition()))
        .whenCondition(RunInfoUtils.getRunCondition(stageNode.getWhen()))
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
        .build();
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, DeploymentStageNode field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();

    try {
      // Validate Stage Failure strategy.
      validateFailureStrategy(field);

      YamlField specField =
          Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));

      // Adding service child by resolving the serviceField
      YamlField serviceField = getResolvedServiceField(specField);
      String serviceNodeUuid = serviceField.getNode().getUuid();

      PipelineInfrastructure pipelineInfrastructure =
          ((DeploymentStageConfig) field.getStageInfoConfig()).getInfrastructure();

      // Adding Spec node
      planCreationResponseMap.put(specField.getNode().getUuid(),
          PlanCreationResponse.builder().dependencies(getDependenciesForSpecNode(specField, serviceNodeUuid)).build());

      // Adding dependency for service
      // Adding serviceField to yamlUpdates as its resolved value should be updated.
      planCreationResponseMap.put(serviceNodeUuid,
          PlanCreationResponse.builder()
              .dependencies(getDependenciesForService(serviceField, pipelineInfrastructure))
              .yamlUpdates(YamlUpdates.newBuilder()
                               .putFqnToYaml(serviceField.getYamlPath(),
                                   YamlUtils.writeYamlString(serviceField).replace("---\n", ""))
                               .build())
              .build());

      YamlField infraField = specField.getNode().getField(YamlTypes.PIPELINE_INFRASTRUCTURE);
      if (infraField == null) {
        throw new InvalidRequestException("Infrastructure section cannot be absent in a pipeline");
      }

      // Adding infrastructure node
      PlanNode infraStepNode = InfrastructurePmsPlanCreator.getInfraStepPlanNode(pipelineInfrastructure);
      planCreationResponseMap.put(
          infraStepNode.getUuid(), PlanCreationResponse.builder().node(infraStepNode.getUuid(), infraStepNode).build());
      String infraSectionNodeChildId = infraStepNode.getUuid();

      if (InfrastructurePmsPlanCreator.isProvisionerConfigured(pipelineInfrastructure)) {
        planCreationResponseMap.putAll(InfrastructurePmsPlanCreator.createPlanForProvisioner(
            pipelineInfrastructure, infraField, infraStepNode.getUuid(), kryoSerializer));
        infraSectionNodeChildId = InfrastructurePmsPlanCreator.getProvisionerNodeId(infraField);
      }

      YamlField infrastructureDefField =
          Preconditions.checkNotNull(infraField.getNode().getField(YamlTypes.INFRASTRUCTURE_DEF));
      PlanNode infraDefPlanNode =
          InfrastructurePmsPlanCreator.getInfraDefPlanNode(infrastructureDefField, infraSectionNodeChildId);
      planCreationResponseMap.put(infraDefPlanNode.getUuid(),
          PlanCreationResponse.builder().node(infraDefPlanNode.getUuid(), infraDefPlanNode).build());

      YamlNode infraNode = infraField.getNode();
      planCreationResponseMap.putAll(InfrastructurePmsPlanCreator.createPlanForInfraSection(
          infraNode, infraDefPlanNode.getUuid(), pipelineInfrastructure, kryoSerializer));

      // Add dependency for execution
      YamlField executionField = specField.getNode().getField(YAMLFieldNameConstants.EXECUTION);
      if (executionField == null) {
        throw new InvalidRequestException("Execution section cannot be absent in a pipeline");
      }
      addCDExecutionDependencies(planCreationResponseMap, executionField);

      return planCreationResponseMap;
    } catch (IOException e) {
      throw new InvalidRequestException(
          "Invalid yaml for Deployment stage with identifier - " + field.getIdentifier(), e);
    }
  }

  public Dependencies getDependenciesForService(YamlField serviceField, PipelineInfrastructure infraConfig) {
    Map<String, YamlField> serviceYamlFieldMap = new HashMap<>();
    String serviceNodeUuid = serviceField.getNode().getUuid();
    serviceYamlFieldMap.put(serviceNodeUuid, serviceField);

    Map<String, ByteString> serviceDependencyMap = new HashMap<>();
    serviceDependencyMap.put(YamlTypes.INFRASTRUCTURE_STEP_PARAMETERS,
        ByteString.copyFrom(
            kryoSerializer.asDeflatedBytes(InfrastructurePmsPlanCreator.getInfraSectionStepParams(infraConfig, ""))));
    serviceDependencyMap.put(YamlTypes.ENVIRONMENT_NODE_ID,
        ByteString.copyFrom(kryoSerializer.asDeflatedBytes("environment-" + infraConfig.getUuid())));

    Dependency serviceDependency = Dependency.newBuilder().putAllMetadata(serviceDependencyMap).build();
    return DependenciesUtils.toDependenciesProto(serviceYamlFieldMap)
        .toBuilder()
        .putDependencyMetadata(serviceNodeUuid, serviceDependency)
        .build();
  }

  private YamlField getResolvedServiceField(YamlField parentSpecNode) {
    YamlField serviceField = parentSpecNode.getNode().getField(YamlTypes.SERVICE_CONFIG);
    if (serviceField == null) {
      throw new InvalidRequestException("ServiceConfig Section cannot be absent in a pipeline");
    }
    ServiceConfig serviceConfig;
    try {
      serviceConfig = YamlUtils.read(serviceField.getNode().toString(), ServiceConfig.class);

      // Resolving service useFromStage.
      if (serviceConfig.getUseFromStage() == null) {
        return serviceField;
      } else {
        ServiceConfig actualServiceConfig = servicePlanCreator.getActualServiceConfig(serviceConfig, serviceField);
        String serviceConfigYaml = YamlPipelineUtils.getYamlString(actualServiceConfig);
        YamlField updatedServiceField = YamlUtils.injectUuidInYamlField(serviceConfigYaml);
        return new YamlField("serviceConfig",
            new YamlNode("serviceConfig", updatedServiceField.getNode().getCurrJsonNode(),
                serviceField.getNode().getParentNode()));
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid service yaml", e);
    }
  }

  public Dependencies getDependenciesForSpecNode(YamlField specField, String childNodeUuid) {
    Map<String, YamlField> specYamlFieldMap = new HashMap<>();
    String specNodeUuid = specField.getNode().getUuid();
    specYamlFieldMap.put(specNodeUuid, specField);

    Map<String, ByteString> specDependencyMap = new HashMap<>();
    specDependencyMap.put(
        YAMLFieldNameConstants.CHILD_NODE_OF_SPEC, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(childNodeUuid)));

    Dependency specDependency = Dependency.newBuilder().putAllMetadata(specDependencyMap).build();
    return DependenciesUtils.toDependenciesProto(specYamlFieldMap)
        .toBuilder()
        .putDependencyMetadata(specNodeUuid, specDependency)
        .build();
  }

  public void addCDExecutionDependencies(
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, YamlField executionField) {
    Map<String, YamlField> executionYamlFieldMap = new HashMap<>();
    executionYamlFieldMap.put(executionField.getNode().getUuid(), executionField);

    planCreationResponseMap.put(executionField.getNode().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(DependenciesUtils.toDependenciesProto(executionYamlFieldMap))
            .build());
  }

  private void validateFailureStrategy(DeploymentStageNode stageNode) {
    // Failure strategy should be present.
    List<FailureStrategyConfig> stageFailureStrategies = stageNode.getFailureStrategies();
    if (EmptyPredicate.isEmpty(stageFailureStrategies)) {
      throw new InvalidRequestException("There should be atleast one failure strategy configured at stage level.");
    }

    // checking stageFailureStrategies is having one strategy with error type as AllErrors and along with that no
    // error type is involved
    if (!GenericStepPMSPlanCreator.containsOnlyAllErrorsInSomeConfig(stageFailureStrategies)) {
      throw new InvalidRequestException(
          "There should be a Failure strategy that contains one error type as AllErrors, with no other error type along with it in that Failure Strategy.");
    }
  }
}

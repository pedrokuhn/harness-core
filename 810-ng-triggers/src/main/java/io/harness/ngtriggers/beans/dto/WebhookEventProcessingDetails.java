/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class WebhookEventProcessingDetails {
  boolean eventFound;
  String eventId;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String triggerIdentifier;
  String pipelineIdentifier;
  String pipelineExecutionId;
  boolean exceptionOccured;
  String status;
  String message;
  String payload;
  Long eventCreatedAt;
  String runtimeInput;
}

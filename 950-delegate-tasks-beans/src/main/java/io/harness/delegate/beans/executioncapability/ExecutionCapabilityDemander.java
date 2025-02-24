/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public interface ExecutionCapabilityDemander {
  List<io.harness.delegate.beans.executioncapability.ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator);
}

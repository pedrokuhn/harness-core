/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class HelmDummyCommandUnit extends AbstractCommandUnit {
  public HelmDummyCommandUnit(String name) {
    this.setName(name);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    return null;
  }
}

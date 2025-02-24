/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.instance.dashboard;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/13/17
 */
@Data
@Builder
public class InstanceStatsByService {
  private long totalCount;
  private ServiceSummary serviceSummary;
  private List<InstanceStatsByEnvironment> instanceStatsByEnvList;

  public InstanceStatsByService clone(long newCount) {
    return InstanceStatsByService.builder()
        .totalCount(newCount)
        .serviceSummary(serviceSummary)
        .instanceStatsByEnvList(instanceStatsByEnvList)
        .build();
  }
}

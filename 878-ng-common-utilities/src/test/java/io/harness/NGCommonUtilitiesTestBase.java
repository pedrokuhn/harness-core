/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class NGCommonUtilitiesTestBase extends CategoryTest implements MockableTestMixin {
  public static final String PMS_SERVICE_NAME = "PMS_SERVICE_NAME";

  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public NGCommonUtilitiesRule ngCommonUtilitiesRule = new NGCommonUtilitiesRule(lifecycleRule.getClosingFactory());
}

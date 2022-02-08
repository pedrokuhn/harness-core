/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.helm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.delegatetasks.ExceptionMessageSanitizer;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDC)
public class ArtifactoryHelmTaskHelper {
  @Inject EncryptionService encryptionService;
  @Inject ArtifactoryService artifactoryService;

  @NotNull
  public String getArtifactoryRepoNameFromHelmConfig(HttpHelmRepoConfig helmRepoConfig) {
    String repoName = helmRepoConfig.getChartRepoUrl().split("/artifactory/", 2)[1];
    if (repoName.endsWith("/")) {
      repoName = repoName.substring(0, repoName.length() - 1);
    }
    return repoName;
  }

  public ArtifactoryConfigRequest getArtifactoryConfigRequestFromHelmRepoConfig(HttpHelmRepoConfig helmRepoConfig) {
    String baseUrl = helmRepoConfig.getChartRepoUrl().split("/artifactory/", 2)[0] + "/artifactory/";
    return ArtifactoryConfigRequest.builder()
        .artifactoryUrl(baseUrl)
        .username(helmRepoConfig.getUsername())
        .password(helmRepoConfig.getPassword())
        .hasCredentials(helmRepoConfig.getUsername() != null || helmRepoConfig.getPassword() != null)
        .build();
  }

  public boolean checkIfVersionExistsInArtifactory(HelmRepoConfig helmRepoConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String chartName, String chartVersion) {
    encryptionService.decrypt(helmRepoConfig, encryptedDataDetails, false);
    ExceptionMessageSanitizer.storeAllSecretsForSanitizing(helmRepoConfig, encryptedDataDetails);
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;
    return artifactoryService.helmChartWithVersionExists(
        getArtifactoryConfigRequestFromHelmRepoConfig(httpHelmRepoConfig),
        getArtifactoryRepoNameFromHelmConfig(httpHelmRepoConfig), chartVersion, chartName);
  }
}

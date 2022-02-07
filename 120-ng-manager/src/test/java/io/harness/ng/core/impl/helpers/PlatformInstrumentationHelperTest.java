package io.harness.ng.core.impl.helpers;

import com.google.inject.Inject;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.impl.DefaultConnectorServiceImpl;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.delegate.beans.connector.k8Connector.*;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.ng.core.remote.OrganizationMapper.toOrganization;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.rule.OwnerRule.TEJAS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class PlatformInstrumentationHelperTest extends CategoryTest {
    @InjectMocks PlatformInstrumentationHelper instrumentationHelper;
    @Mock TelemetryReporter telemetryReporter;
    @Mock KubernetesConnectionValidator kubernetesConnectionValidator;
    @Inject @InjectMocks DefaultConnectorServiceImpl connectorService;

    String userName = "userName";
    String masterUrl = "https://abc.com";
    String identifier = "identifier";
    String name = "name";
    String accountIdentifier = "accountIdentifier";
    String projectIdentifier = "projectIdentifier";
    String orgIdentifier = "orgIdentifier";
    String connectorIdentifier = "connectorIdentifier";
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    SecretRefData passwordSecretRef;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private ProjectDTO createProjectDTO(String orgIdentifier, String identifier) {
        return ProjectDTO.builder()
                .orgIdentifier(orgIdentifier)
                .identifier(identifier)
                .name(randomAlphabetic(10))
                .color(randomAlphabetic(10))
                .build();
    }

    private OrganizationDTO createOrganizationDTO(String identifier) {
        return OrganizationDTO.builder().identifier(identifier).name(randomAlphabetic(10)).build();
    }

    private ConnectorDTO createKubernetesConnectorRequestDTO(String connectorIdentifier, String name) {
        KubernetesAuthDTO kubernetesAuthDTO =
                KubernetesAuthDTO.builder()
                        .authType(KubernetesAuthType.USER_PASSWORD)
                        .credentials(
                                KubernetesUserNamePasswordDTO.builder().username(userName).passwordRef(passwordSecretRef).build())
                        .build();
        KubernetesCredentialDTO connectorDTOWithDelegateCreds =
                KubernetesCredentialDTO.builder()
                        .kubernetesCredentialType(MANUAL_CREDENTIALS)
                        .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
                        .build();
        KubernetesClusterConfigDTO k8sClusterConfig =
                KubernetesClusterConfigDTO.builder().credential(connectorDTOWithDelegateCreds).build();
        ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder()
                .name(name)
                .identifier(connectorIdentifier)
                .connectorType(KUBERNETES_CLUSTER)
                .connectorConfig(k8sClusterConfig)
                .build();
        return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
    }

    private ConnectorResponseDTO createConnector(String connectorIdentifier, String name) {
        ConnectorDTO connectorRequestDTO = createKubernetesConnectorRequestDTO(connectorIdentifier, name);
        return connectorService.create(connectorRequestDTO, accountIdentifier);
    }

    @Test
    @Owner(developers = TEJAS)
    @Category(UnitTests.class)
    public void testCreateProjectTrackSend() {
        String accountIdentifier = randomAlphabetic(10);
        String orgIdentifier = randomAlphabetic(10);
        ProjectDTO projectDTO = createProjectDTO(orgIdentifier, randomAlphabetic(10));
        Project project = toProject(projectDTO);
        instrumentationHelper.sendProjectCreationEvent(project, accountIdentifier);
        try {
            verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Owner(developers = TEJAS)
    @Category(UnitTests.class)
    public void testDeleteProjectTrackSend() {
        String accountIdentifier = randomAlphabetic(10);
        String orgIdentifier = randomAlphabetic(10);
        ProjectDTO projectDTO = createProjectDTO(orgIdentifier, randomAlphabetic(10));
        Project project = toProject(projectDTO);
        instrumentationHelper.sendProjectDeletionEvent(project, accountIdentifier);
        try {
            verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Owner(developers = TEJAS)
    @Category(UnitTests.class)
    public void testCreateOrganiztionTrackSend() {
        String accountIdentifier = randomAlphabetic(10);
        OrganizationDTO organizationDTO = createOrganizationDTO(randomAlphabetic(10));
        Organization organization = toOrganization(organizationDTO);
        instrumentationHelper.sendOrganizationCreationEvent(organization, accountIdentifier);
        try {
            verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Owner(developers = TEJAS)
    @Category(UnitTests.class)
    public void testDeleteOrganiztionTrackSend() {
        String accountIdentifier = randomAlphabetic(10);
        OrganizationDTO organizationDTO = createOrganizationDTO(randomAlphabetic(10));
        Organization organization = toOrganization(organizationDTO);
        instrumentationHelper.sendOrganizationDeletionEvent(organization, accountIdentifier);
        try {
            verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Owner(developers = TEJAS)
    @Category(UnitTests.class)
    public void testCreateConnectorTrackSend() {
        ConnectorResponseDTO connectorDTOOutput = createConnector(identifier, name);
        instrumentationHelper.sendConnectorCreationFinishEvent(connectorDTOOutput.getConnector(), accountIdentifier);
        try {
            verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Owner(developers = TEJAS)
    @Category(UnitTests.class)
    public void testDeleteConnectorTrackSend() {
        instrumentationHelper.sendConnectorDeletionEvent(orgIdentifier, projectIdentifier, connectorIdentifier, accountIdentifier);
        try {
            verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package io.harness.ng.core.impl.helpers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
@Singleton
public class PlatformInstrumentationHelper {
    @Inject
    TelemetryReporter telemetryReporter;
    public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
    String PROJECT_ID = "project_id";
    String PROJECT_CREATION_TIME = "project_creation_time" ;
    String PROJECT_ORG = "project_org";
    String PROJECT_NAME = "project_name";
    String PROJECT_VERSION = "project_version";

    String ORGANIZATION_ID = "organization_id";
    String ORGANIZATION_CREATION_TIME = "organization_creation_time" ;
    String ORGANIZATION_NAME = "organization_name";
    String ORGANIZATION_VERSION = "organization_version";

    String CONNECTOR_ID = "connector_id";
    String CONNECTOR_PROJECT = "connector_project";
    String CONNECTOR_CREATION_TIME = "connector_creation_time" ;
    String CONNECTOR_ORG = "connector_org";
    String CONNECTOR_NAME = "connector_name";

    public void sendProjectCreationEvent (Project project, String accountId) {
        log.info("Platform SendProjectCreationEvent execution started.");
        try {
            if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(PROJECT_ID, project.getIdentifier());
                map.put(PROJECT_CREATION_TIME, project.getCreatedAt());
                map.put(PROJECT_ORG, project.getOrgIdentifier());
                map.put(PROJECT_NAME, project.getName());
                telemetryReporter.sendTrackEvent("Project Creation", map,
                        ImmutableMap.<Destination, Boolean>builder()
                        .put(Destination.AMPLITUDE, true)
                        .put(Destination.ALL, false)
                        .build(),
                        Category.COMMUNITY
                );
                log.info("Project Creation event sent!");
            } else {
                log.info("There is no Account found!. Can not send Project Creation event.");
            }
        } catch (Exception e) {
            log.error("Platform SendProjectCreationEvent execution failed.", e);
        } finally {
            log.info("Platform SendProjectCreationEvent execution finished.");
        }
    }
    public void sendProjectDeletionEvent (Project project, String accountId) {
        log.info("Platform SendProjectDeletionEvent execution started.");
        try {
            if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(PROJECT_NAME, project.getName());
                map.put(PROJECT_ID, project.getIdentifier());
                map.put(PROJECT_ORG, project.getOrgIdentifier());
                map.put(PROJECT_VERSION, project.getVersion());
                telemetryReporter.sendTrackEvent("Project Deletion", map,
                        ImmutableMap.<Destination, Boolean>builder()
                                .put(Destination.AMPLITUDE, true)
                                .put(Destination.ALL, false)
                                .build(),
                        Category.COMMUNITY
                );
                log.info("Project deletion event sent!");
            } else {
                log.info("There is no Account found!. Can not send Project Deletion event.");
            }
        } catch (Exception e) {
            log.error("Platform SendProjectDeletionEvent execution failed.", e);
        } finally {
            log.info("Platform SendProjectDeletionEvent execution finished.");
        }
    }

    public void sendOrganizationCreationEvent (Organization organization, String accountId) {
        log.info("Platform SendOrganizationCreationEvent execution started.");
        try {
            if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(ORGANIZATION_ID, organization.getIdentifier());
                map.put(ORGANIZATION_CREATION_TIME, organization.getCreatedAt());
                map.put(ORGANIZATION_NAME, organization.getName());
                telemetryReporter.sendTrackEvent("Organization Creation", map,
                        ImmutableMap.<Destination, Boolean>builder()
                                .put(Destination.AMPLITUDE, true)
                                .put(Destination.ALL, false)
                                .build(),
                        Category.COMMUNITY
                );
                log.info("Organization Creation event sent!");
            } else {
                log.info("There is no Account found!. Can not send Organization Creation event.");
            }
        } catch (Exception e) {
            log.error("Platform SendOrganizationCreationEvent execution failed.", e);
        } finally {
            log.info("Platform SendOrganizationCreationEvent execution finished.");
        }
    }
    public void sendOrganizationDeletionEvent (Organization organization, String accountId) {
        log.info("Platform SendOrganizationDeletionEvent execution started.");
        try {
            if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(ORGANIZATION_NAME, organization.getName());
                map.put(ORGANIZATION_ID, organization.getIdentifier());
                map.put(ORGANIZATION_VERSION, organization.getVersion());
                telemetryReporter.sendTrackEvent("Organization Deletion", map,
                        ImmutableMap.<Destination, Boolean>builder()
                                .put(Destination.AMPLITUDE, true)
                                .put(Destination.ALL, false)
                                .build(),
                        Category.COMMUNITY
                );
                log.info("Organization deletion event sent!");
            } else {
                log.info("There is no Account found!. Can not send Organization Deletion event.");
            }
        } catch (Exception e) {
            log.error("Platform SendOrganizationDeletionEvent execution failed.", e);
        } finally {
            log.info("Platform SendOrganizationDeletionEvent execution finished.");
        }
    }

    public void sendConnectorCreationFinishEvent(ConnectorInfoDTO connector, String accountId) {
        log.info("Platform SendConnectorCreationEvent execution started.");
        try {
            if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(CONNECTOR_ID, connector.getIdentifier());
                map.put(CONNECTOR_ORG, connector.getOrgIdentifier());
                map.put(CONNECTOR_NAME, connector.getName());
                map.put(CONNECTOR_PROJECT, connector.getProjectIdentifier());
                telemetryReporter.sendTrackEvent("Connector Creation", map,
                        ImmutableMap.<Destination, Boolean>builder()
                                .put(Destination.AMPLITUDE, true)
                                .put(Destination.ALL, false)
                                .build(),
                        Category.COMMUNITY
                );
                log.info("Connector Creation event sent!");
            } else {
                log.info("There is no Account found!. Can not send Connector Creation event.");
            }
        } catch (Exception e) {
            log.error("Platform SendConnectorCreationEvent execution failed.", e);
        } finally {
            log.info("Platform SendConnectorCreationEvent execution finished.");
        }
    }

    public void sendConnectorDeletionEvent(String orgIdentifier, String projectIdentifier, String connectorIdentifier,
                                           String accountId) {
        log.info("Platform SendConnectorDeletionEvent execution started.");
        try {
            if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(CONNECTOR_PROJECT, projectIdentifier);
                map.put(CONNECTOR_ID, connectorIdentifier);
                map.put(CONNECTOR_ORG, orgIdentifier);
                telemetryReporter.sendTrackEvent("Connector Deletion", map,
                        ImmutableMap.<Destination, Boolean>builder()
                                .put(Destination.AMPLITUDE, true)
                                .put(Destination.ALL, false)
                                .build(),
                        Category.COMMUNITY
                );
                log.info("Connector deletion event sent!");
            } else {
                log.info("There is no Account found!. Can not send Connector Deletion event.");
            }
        } catch (Exception e) {
            log.error("Platform SendConnectorDeletionEvent execution failed.", e);
        } finally {
            log.info("Platform SendConnectorDeletionEvent execution finished.");
        }
    }
}


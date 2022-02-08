package io.harness.batch.processing.service;

import static io.harness.batch.processing.pricing.gcp.bigquery.BQConst.CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.DATA_SET_NAME_TEMPLATE;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.getDataSetDescription;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.lang.String.format;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.pricing.gcp.bigquery.BQConst;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.service.intf.AWSOrganizationHelperService;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.filter.FilterType;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RestCallToNGManagerClientUtils;

import software.wings.beans.Account;

import com.amazonaws.services.organizations.model.Tag;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.bigquery.TimePartitioning;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class AwsAccountTagsCollectionService {
  @Autowired private ConnectorResourceClient connectorResourceClient;
  @Autowired private BatchMainConfig mainConfig;
  @Autowired private AccountShardService accountShardService;
  @Autowired AWSOrganizationHelperService awsOrganizationHelperService;
  @Autowired BigQueryService bigQueryService;

  public void update() {
    List<Account> accounts = accountShardService.getCeEnabledAccounts();
    log.info("accounts size: {}", accounts.size());
    List<Tag> tagsList = null;
    for (Account account : accounts) {
      AutoLogContext ignore = new AccountLogContext(account.getUuid(), OVERRIDE_ERROR);
      log.info("Fetching connectors for accountName {}, accountId {}", account.getAccountName(), account.getUuid());
      List<ConnectorResponseDTO> nextGenAwsConnectorResponses = getNextGenAwsConnectorResponses(account.getUuid());
      for (ConnectorResponseDTO connector : nextGenAwsConnectorResponses) {
        ConnectorInfoDTO connectorInfo = connector.getConnector();
        CEAwsConnectorDTO ceAwsConnectorDTO = (CEAwsConnectorDTO) connectorInfo.getConnectorConfig();
        try {
          tagsList = processTags(ceAwsConnectorDTO);
          String tableName = createBQTable(account);
          if (isNotEmpty(tagsList)) {
            insertInBQ(tableName, "AWS", ceAwsConnectorDTO.getAwsAccountId(), "account", tagsList);
          }
        } catch (Exception e) {
          log.error("Exception processing aws tags for connectorId: {} for accountId: {}",
              connectorInfo.getIdentifier(), account.getUuid(), e);
        }
      }
      ignore.close();
    }
  }

  public List<ConnectorResponseDTO> getNextGenAwsConnectorResponses(String accountId) {
    List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
        ConnectorFilterPropertiesDTO.builder()
            .types(Arrays.asList(ConnectorType.CE_AWS))
            .ccmConnectorFilter(CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.BILLING)).build())
            //.connectivityStatuses(Arrays.asList(ConnectivityStatus.SUCCESS))
            .build();
    connectorFilterPropertiesDTO.setFilterType(FilterType.CONNECTOR);
    int page = 0;
    int size = 100;
    try {
      do {
        response = getConnectors(accountId, page, size, connectorFilterPropertiesDTO);
        if (response != null && isNotEmpty(response.getContent())) {
          nextGenConnectorResponses.addAll(response.getContent());
        }
        page++;
      } while (response != null && isNotEmpty(response.getContent()));
      log.info("Processing batch size of {}", nextGenConnectorResponses.size());
      return nextGenConnectorResponses;
    } catch (Exception ex) {
      log.error("Error", ex);
    }
    return nextGenConnectorResponses;
  }

  PageResponse getConnectors(
      String accountId, int page, int size, ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO) {
    return RestCallToNGManagerClientUtils.execute(
        connectorResourceClient.listConnectors(accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
  }

  public List<Tag> processTags(CEAwsConnectorDTO ceAwsConnectorDTO) {
    log.info("awsAccountId: {}, roleArn: {}, externalId: {}", ceAwsConnectorDTO.getAwsAccountId(),
        ceAwsConnectorDTO.getCrossAccountAccess().getCrossAccountRoleArn(),
        ceAwsConnectorDTO.getCrossAccountAccess().getExternalId());
    List<Tag> tagsList = awsOrganizationHelperService.listAwsAccountTags(ceAwsConnectorDTO.getCrossAccountAccess(),
        mainConfig.getAwsS3SyncConfig().getAwsAccessKey(), mainConfig.getAwsS3SyncConfig().getAwsSecretKey(),
        ceAwsConnectorDTO.getAwsAccountId());
    log.info("tagsList: {}", tagsList);
    return tagsList;
  }

  public String createBQTable(Account account) {
    String accountId = account.getUuid();
    String accountName = account.getAccountName();
    String dataSetName =
        String.format(DATA_SET_NAME_TEMPLATE, BillingDataPipelineUtils.modifyStringToComplyRegex(accountId));
    String description = getDataSetDescription(accountId, accountName);
    BigQuery bigquery = bigQueryService.get();
    String tableName = format("%s.%s.%s", mainConfig.getBillingDataPipelineConfig().getGcpProjectId(), dataSetName,
        CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME);
    log.info("DatasetName: {} , TableName {}", dataSetName, tableName);
    try {
      DatasetInfo datasetInfo = DatasetInfo.newBuilder(dataSetName).setDescription(description).build();
      Dataset createdDataSet = bigquery.create(datasetInfo);
      log.info("Dataset created {}", createdDataSet);
      bigquery.create(getCloudProviderEntityTagsTableInfo(dataSetName));
      log.info("Table created {}", CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME);
    } catch (BigQueryException bigQueryEx) {
      // data set/PreAggregate Table already exists.
      if (bigquery.getTable(TableId.of(dataSetName, CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME)) == null) {
        bigquery.create(getCloudProviderEntityTagsTableInfo(dataSetName));
        log.info("Table created {}", CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME);
        return tableName;
      }
      log.error("Error code {}:", bigQueryEx.getCode(), bigQueryEx);
    }
    return tableName;
  }

  public void insertInBQ(
      String tableName, String cloudProviderId, String entityId, String entityType, List<Tag> tagsList) {
    // INSERT INTO `ccm-play.BillingReport_efofrubhttupezjuqxlhbg.cloudProviderEntityTags` (cloudProviderId, entityId,
    // entityType, labels, createdAt) VALUES ('AWS', '890436954479', 'account', ARRAY[STRUCT('team','ccm')],
    // '2022-02-08T21:30:29.693Z')
    StringBuilder tagsBQFormat = new StringBuilder();
    tagsBQFormat.append("ARRAY[");
    String prefix = "";
    for (Tag tag : tagsList) {
      tagsBQFormat.append(prefix);
      prefix = ",";
      tagsBQFormat.append(format(" STRUCT('%s', '%s')", tag.getKey(), tag.getValue()));
    }
    tagsBQFormat.append("]");
    String formattedQuery = format(BQConst.CLOUD_PROVIDER_ENTITY_TAGS_INSERT, tableName, cloudProviderId, entityId,
        entityType, tagsBQFormat, Instant.now().toString());
    log.info(formattedQuery);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(formattedQuery).build();
    try {
      bigQueryService.get().query(queryConfig);
      log.info("Labels inserted into BQ");
    } catch (BigQueryException | InterruptedException bigQueryException) {
      log.error("Error: ", bigQueryException);
    }
  }

  protected static TableInfo getCloudProviderEntityTagsTableInfo(String dataSetName) {
    TableId tableId = TableId.of(dataSetName, CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME);
    TimePartitioning partitioning =
        TimePartitioning.newBuilder(TimePartitioning.Type.DAY).setField("createdAt").build();
    Schema schema = Schema.of(Field.of("cloudProviderId", StandardSQLTypeName.STRING),
        Field.of("createdAt", StandardSQLTypeName.TIMESTAMP), Field.of("entityType", StandardSQLTypeName.STRING),
        Field.of("entityId", StandardSQLTypeName.STRING),
        Field
            .newBuilder("labels", StandardSQLTypeName.STRUCT, Field.of("key", StandardSQLTypeName.STRING),
                Field.of("value", StandardSQLTypeName.STRING))
            .setMode(Field.Mode.REPEATED)
            .build());
    StandardTableDefinition tableDefinition =
        StandardTableDefinition.newBuilder().setSchema(schema).setTimePartitioning(partitioning).build();
    return TableInfo.newBuilder(tableId, tableDefinition).build();
  }
}

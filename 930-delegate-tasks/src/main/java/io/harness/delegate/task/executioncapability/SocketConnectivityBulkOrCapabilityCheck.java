/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.SocketConnectivityBulkOrParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityBulkOrExecutionCapability;

import com.google.inject.Singleton;
import com.google.protobuf.ProtocolStringList;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SocketConnectivityBulkOrCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SocketConnectivityBulkOrExecutionCapability socketConnCapability =
        (SocketConnectivityBulkOrExecutionCapability) delegateCapability;
    try {
      boolean valid = false;
      for (String host : socketConnCapability.getHostNames()) {
        valid = connectableHost(host, socketConnCapability.getPort());
        if (valid) {
          break;
        }
      }

      return CapabilityResponse.builder().delegateCapability(socketConnCapability).validated(valid).build();
    } catch (final Exception ex) {
      log.error("Error Occurred while checking socketConnCapability", ex);
      return CapabilityResponse.builder().delegateCapability(socketConnCapability).validated(false).build();
    }
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.SOCKET_CONNECTIVITY_BULK_OR_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    SocketConnectivityBulkOrParameters socketParameters = parameters.getSocketConnectivityBulkOrParameters();
    try {
      ProtocolStringList hostNames = socketParameters.getHostNamesList();
      PermissionResult result = PermissionResult.DENIED;

      for (String host : hostNames) {
        if (connectableHost(host, socketParameters.getPort())) {
          result = PermissionResult.ALLOWED;
          break;
        }
      }

      return builder.permissionResult(result).build();
    } catch (final Exception ex) {
      log.error("Error Occurred while checking socketConnCapability with proto", ex);
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
  }

  public static boolean connectableHost(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 5000); // 5 sec timeout
      log.info("[Delegate Capability] Socket Connection Succeeded for url {} on port {}", host, port);
      return true;
    } catch (final IOException e) {
      log.error("[Delegate Capability] Socket Connection Failed for url " + host + " on port " + port, e);
    }
    return false; // Either timeout or unreachable or failed DNS lookup.
  }
}

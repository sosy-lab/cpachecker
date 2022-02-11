// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class StatusObserver implements MessageObserver {

  private final Map<String, AlgorithmStatus> statusMap;
  private AlgorithmStatus status;

  public StatusObserver() {
    statusMap = new HashMap<>();
    status = AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  @Override
  public boolean process(Message pMessage) throws CPAException {
    if (pMessage.getPayload().containsKey(Payload.STATUS)) {
      String statusString = pMessage.getPayload().get(Payload.STATUS);
      List<Boolean> properties = Splitter.on(",").splitToStream(statusString).map(Boolean::parseBoolean).collect(
          ImmutableList.toImmutableList());
      assert properties.size() == 3 : "Wrong status message" + pMessage;
      statusMap.put(pMessage.getUniqueBlockId(), new AlgorithmStatus(properties.get(0), properties.get(1), properties.get(2)));
    }
    return false;
  }

  public AlgorithmStatus getStatus() {
    return status;
  }

  @Override
  public void finish() throws InterruptedException, CPAException {
    status = statusMap.values().stream().reduce(AlgorithmStatus::update).orElse(AlgorithmStatus.NO_PROPERTY_CHECKED);
  }
}

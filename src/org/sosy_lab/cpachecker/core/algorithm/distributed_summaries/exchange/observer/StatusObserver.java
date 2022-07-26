// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class StatusObserver implements MessageObserver {

  public enum StatusSoundness {
    SOUND,
    UNSOUND
  }

  public enum StatusPropertyChecked {
    CHECKED,
    UNCHECKED
  }

  public enum StatusPrecise {
    PRECISE,
    IMPRECISE
  }

  private final Map<String, AlgorithmStatus> statusMap;
  private AlgorithmStatus status;

  public StatusObserver() {
    statusMap = new HashMap<>();
    status = AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  @Override
  public boolean process(ActorMessage pMessage) throws CPAException {
    Payload payload = pMessage.getPayload();
    if (!(payload.containsKey(Payload.PRECISE)
        && payload.containsKey(Payload.PROPERTY)
        && payload.containsKey(Payload.SOUND))) {
      return false;
    }
    StatusPrecise isPrecise = StatusPrecise.valueOf(payload.get(Payload.PRECISE));
    StatusPropertyChecked isPropertyChecked =
        StatusPropertyChecked.valueOf(payload.get(Payload.PROPERTY));
    StatusSoundness isSound = StatusSoundness.valueOf(payload.get(Payload.SOUND));
    statusMap.put(pMessage.getUniqueBlockId(), statusOf(isPropertyChecked, isSound, isPrecise));
    return false;
  }

  private AlgorithmStatus statusOf(
      StatusPropertyChecked pPropertyChecked, StatusSoundness pIsSound, StatusPrecise pIsPrecise) {
    if (pPropertyChecked == StatusPropertyChecked.UNCHECKED) {
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    if (pIsSound == StatusSoundness.SOUND) {
      if (pIsPrecise == StatusPrecise.PRECISE) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      return AlgorithmStatus.SOUND_AND_IMPRECISE;
    } else {
      if (pIsPrecise == StatusPrecise.PRECISE) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
      return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    }
  }

  public AlgorithmStatus getStatus() {
    return status;
  }

  @Override
  public void finish() throws InterruptedException, CPAException {
    status =
        statusMap.values().stream()
            .reduce(AlgorithmStatus::update)
            .orElse(AlgorithmStatus.NO_PROPERTY_CHECKED);
  }
}

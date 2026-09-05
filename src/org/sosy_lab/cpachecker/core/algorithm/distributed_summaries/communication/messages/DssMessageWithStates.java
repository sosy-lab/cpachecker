// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public interface DssMessageWithStates extends DssMessage {
  ImmutableList<ImmutableMap<String, String>> getStates();

  default int getNumberOfContainedStates() {
    return getStates().size();
  }

  default ContentReader getAbstractStateContent(
      Class<? extends AbstractState> pType, int pStateIndex) {
    return getArbitraryContent(pType.getName(), pStateIndex);
  }

  default ContentReader getPrecisionContent(
      Class<? extends Precision> pPrecision, int pStateIndex) {
    return getArbitraryContent(pPrecision.getName(), pStateIndex);
  }

  private ContentReader getArbitraryContent(String pKey, int pStateIndex) {
    Map<String, String> stateContent =
        ContentReader.read(getStates().get(pStateIndex)).pushLevel(pKey).getContent();
    checkState(!stateContent.isEmpty(), "State content cannot be empty for key %s.", pKey);
    checkState(
        stateContent.values().stream().noneMatch(Objects::isNull),
        "Null values are not allowed in content.");
    return ContentReader.read(stateContent);
  }
}

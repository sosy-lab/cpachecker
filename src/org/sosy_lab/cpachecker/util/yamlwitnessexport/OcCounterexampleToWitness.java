// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.cpachecker.cpa.oc.OcThreadInstanceState;
import org.sosy_lab.cpachecker.cpa.oc.ThreadInstance;

/** Exports counterexamples whose states carry the executing OC thread instance. */
public final class OcCounterexampleToWitness extends CounterexampleToWitness {

  public OcCounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  @Override
  protected Optional<String> getCurrentThreadNameIfExists(ARGState pState, CFAEdge pEdge) {
    Optional<String> threadName = super.getCurrentThreadNameIfExists(pState, pEdge);
    if (threadName.isPresent()) {
      return threadName;
    }
    OcThreadInstanceState ocState = extractStateByType(pState, OcThreadInstanceState.class);
    return ocState == null
        ? Optional.empty()
        : Optional.of(getOcThreadName(ocState.getThreadInstanceId()));
  }

  @Override
  protected Optional<String> getNewThreadNameIfExists(ARGState pState, ARGState pPreviousState) {
    Optional<String> threadName = super.getNewThreadNameIfExists(pState, pPreviousState);
    if (threadName.isPresent()) {
      return threadName;
    }
    OcThreadInstanceState ocState = extractStateByType(pState, OcThreadInstanceState.class);
    return ocState == null || ocState.getCreatedThreadInstanceId() == MemoryEvent.NO_INSTANCE
        ? Optional.empty()
        : Optional.of(getOcThreadName(ocState.getCreatedThreadInstanceId()));
  }

  private static String getOcThreadName(int pInstanceId) {
    return pInstanceId == ThreadInstance.MAIN_INSTANCE_ID ? "main" : "T" + pInstanceId;
  }
}

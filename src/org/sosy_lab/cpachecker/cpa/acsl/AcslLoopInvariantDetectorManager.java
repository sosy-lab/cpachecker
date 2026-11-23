// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.LoopStructure;

public class AcslLoopInvariantDetectorManager {

  private final ImmutableList<AcslLoopInvariantDetector> detectors;

  public AcslLoopInvariantDetectorManager(LogManager pLogger, FormulaToAcslConverter pConverter) {
    detectors = ImmutableList.of(new ArrayInitDetector(pLogger, pConverter));
  }

  public ImmutableList<AcslInvariantProvider> detect(
      CFAEdge pEdge, Optional<LoopStructure> pLoopStructure, PredicateAbstractState pState) {
    ImmutableList.Builder<AcslInvariantProvider> builder = ImmutableList.builder();

    for (AcslLoopInvariantDetector d : detectors) {
      d.detect(pEdge, pLoopStructure, pState).ifPresent(builder::add);
    }

    return builder.build();
  }
}

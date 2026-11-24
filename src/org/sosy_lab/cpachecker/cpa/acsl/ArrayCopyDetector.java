// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.LoopStructure;

/** Detect the pattern of an array copy that makes it possible to infer an ACSL invariant. */
@SuppressWarnings("unused")
class ArrayCopyDetector implements AcslLoopInvariantDetector {

  private final LogManager logger;
  private final FormulaToAcslConverter formulaConverter;
  private final Level logLevel = Level.FINER; // TODO change to INFO for debugging
  private PredicateAbstractState predStateAtLoopEntry;

  ArrayCopyDetector(LogManager pLogger, FormulaToAcslConverter pFormulaConverter) {
    this.logger = pLogger;
    this.formulaConverter = pFormulaConverter;
  }

  @Override
  public Optional<? extends AcslInvariantProvider> detect(
      CFAEdge pEdge, Optional<LoopStructure> pLoopStructure, PredicateAbstractState pState) {
    return Optional.empty();
  }

  @Override
  public Class<? extends AcslInvariantProvider> getInvariantClass() {
    return ArrayCopy.class;
  }
}

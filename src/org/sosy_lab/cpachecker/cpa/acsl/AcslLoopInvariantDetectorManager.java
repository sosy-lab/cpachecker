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
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.LoopStructure;

/** Detect if a loop follows a certain pattern that makes it possible to infer an ACSL invariant. */
@Options(prefix = "cpa.acsl")
public class AcslLoopInvariantDetectorManager {

  @Option(
      secure = true,
      name = "invariants.arrayInit",
      description = "whether to try detecting array initialization when creating ACSL invariants")
  private Boolean enableArrayInit = true;

  @Option(
      secure = true,
      name = "invariants.arrayCopy",
      description = "whether to try detecting array copy when creating ACSL invariants")
  private Boolean enableArrayCopy = true;

  private final LogManager logger;
  private final ImmutableList<AcslLoopInvariantDetector> detectors;

  public AcslLoopInvariantDetectorManager(
      LogManager pLogger, FormulaToAcslConverter pConverter, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    this.logger = pLogger;

    ImmutableList.Builder<AcslLoopInvariantDetector> builder = ImmutableList.builder();
    if (enableArrayInit) {
      builder.add(new ArrayInitDetector(pLogger, pConverter));
    }
    if (enableArrayCopy) {
      builder.add(new ArrayCopyDetector(pLogger, pConverter));
    }
    this.detectors = builder.build();
  }

  public ImmutableList<AcslInvariantProvider> detect(
      AbstractState pCurrState,
      CFAEdge pEdge,
      Optional<LoopStructure> pLoopStructure,
      PredicateAbstractState pState) {

    ImmutableList.Builder<AcslInvariantProvider> builder = ImmutableList.builder();

    for (AcslLoopInvariantDetector d : detectors) {
      if (pCurrState instanceof AcslState acslState
          && pEdge != null
          && !acslState
              .getInvariantsForNode(pEdge.getSuccessor(), d.getInvariantClass())
              .isEmpty()) {
        logger.log(
            Level.INFO, // TODO lower log level eventually
            "Skipping detection of array invariant, as one already exists for the node. Class: "
                + d.getInvariantClass());
      } else {
        d.detect(pEdge, pLoopStructure, pState).ifPresent(builder::add);
      }
    }

    return builder.build();
  }
}

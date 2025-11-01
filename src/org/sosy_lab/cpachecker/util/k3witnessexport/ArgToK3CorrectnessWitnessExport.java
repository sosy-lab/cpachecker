// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.k3witnessexport;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AnnotateTagCommand;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class ArgToK3CorrectnessWitnessExport {
  private final CFA cfa;
  private final Specification specification;
  private final LogManager logger;

  public ArgToK3CorrectnessWitnessExport(
      Configuration pConfig, CFA pCFA, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    // pConfig.inject(this);
    cfa = pCFA;
    specification = pSpecification;
    logger = pLogger;
  }

  public List<K3AnnotateTagCommand> generateWitnessCommands(
      ARGState pRootState, UnmodifiableReachedSet pReached) {
    return ImmutableList.of();
  }
}

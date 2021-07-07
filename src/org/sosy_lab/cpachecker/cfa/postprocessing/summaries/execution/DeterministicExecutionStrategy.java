// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.execution;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.AbstractStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;

public class DeterministicExecutionStrategy extends AbstractStrategy {

  private boolean useCompilerForSummary;

  public DeterministicExecutionStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      boolean pUseCompilerForSummary,
      StrategyDependencyInterface pStrategyDependencies) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies);
    setUseCompilerForSummary(pUseCompilerForSummary);
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    // TODO execute the Loop if all the inputs are Deterministic and write the output as assumptions
    // into the Loop.
    // To see if all variables are deterministic one can do a reverse search through the CFA in
    // order to see if all the Variables are deterministic, and if they are, what their value is.
    // This Method would result in a dependency graph between the Variables.
    // TODO how do we execute the code concretely. We could execute it in CPAchecker, but this would
    // probably be slow. A better alternative would be to write the generated code into a c file,
    // compile this and run it and make the output of the code the assigment of the variables, for
    // example through a file or stout. The problem in this approach is, it would make CPAchecker
    // dependent on having a c compiler on the target system and having the rights to write files
    // and execute them. Is there some other way this could be done efficiently or better?
    //
    // See https://root.cern.ch/root/html534/guides/users-guide/CINT.html for evading arbitrary code
    // execution using an interpreter
    // Going around the problem of arbitrary code execution will probably imply writing a custom c
    // Interpreter
    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return true;
  }

  public boolean isUseCompilerForSummary() {
    return useCompilerForSummary;
  }

  public void setUseCompilerForSummary(boolean pUseCompilerForSummary) {
    useCompilerForSummary = pUseCompilerForSummary;
  }
}

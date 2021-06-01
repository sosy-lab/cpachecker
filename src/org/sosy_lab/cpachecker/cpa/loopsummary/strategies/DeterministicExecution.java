// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class DeterministicExecution extends AbstractStrategy {

  public DeterministicExecution(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, int pStrategyNumber) {
    super(pLogger, pShutdownNotifier, pStrategyNumber);
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
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
    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return true;
  }
}

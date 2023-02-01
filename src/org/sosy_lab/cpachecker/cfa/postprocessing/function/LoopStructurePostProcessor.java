// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.LoopStructure;

/** {@link CfaPostProcessor} implementation for {@link LoopStructure}. */
public final class LoopStructurePostProcessor implements CfaPostProcessor {

  @Override
  public MutableCFA execute(
      MutableCFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    try {
      pCfa.setLoopStructure(LoopStructure.getLoopStructure(pCfa));
    } catch (ParserException e) {
      // don't abort here, because if the analysis doesn't need the loop information, we can
      // continue
      pLogger.logUserException(Level.WARNING, e, "Could not analyze loop structure of program.");
    } catch (OutOfMemoryError e) {
      pLogger.logUserException(
          Level.WARNING, e, "Could not analyze loop structure of program due to memory problems");
    }
    return pCfa;
  }
}
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

public final class LoopStructurePostProcessor implements CfaPostProcessor {

  @Override
  public MutableCFA process(
      MutableCFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier) {

    try {
      pCfa.setLoopStructure(LoopStructure.getLoopStructure(pCfa));
    } catch (ParserException ex) {
      pLogger.log(Level.WARNING, ex);
    }

    return pCfa;
  }
}

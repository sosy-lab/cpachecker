// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFAReversePostorder;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.MutableCFA;

public final class ReversePostorderPostProcessor implements CfaPostProcessor {

  @Override
  public MutableCFA process(MutableCFA pCfa, LogManager pLogger) {
    pCfa.getAllFunctionHeads().forEach(CFAReversePostorder::assignIds);
    return pCfa;
  }
}

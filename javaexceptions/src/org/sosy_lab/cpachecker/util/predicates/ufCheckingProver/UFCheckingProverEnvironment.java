// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ProverEnvironment;

public class UFCheckingProverEnvironment extends UFCheckingBasicProverEnvironment<Void>
    implements ProverEnvironment {

  public UFCheckingProverEnvironment(
      LogManager pLogger,
      ProverEnvironment pe,
      FormulaManagerView pFmgr,
      UFCheckingProverOptions options) {
    super(pLogger, pe, pFmgr, options);
  }
}

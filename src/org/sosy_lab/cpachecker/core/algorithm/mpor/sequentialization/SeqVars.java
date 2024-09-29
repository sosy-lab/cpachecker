// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqVars {

  public static final Variable numThreads = new Variable(SeqToken.NUM_THREADS);

  public static final Variable pc = new Variable(SeqToken.PC);

  public static final Variable execute = new Variable(SeqToken.EXECUTE);

  public static final Variable nextThread = new Variable(SeqToken.NEXT_THREAD);
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqInitializerBuilder.buildInitializerExpression;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;

public class SeqInitializers {

  public static class SeqInitializer {

    private static final CInitializer INT_INIT_PC =
        buildInitializerExpression(SeqIntegerLiteralExpression.INT_INIT_PC);

    private static final CInitializer INT_EXIT_PC =
        buildInitializerExpression(SeqIntegerLiteralExpression.INT_EXIT_PC);

    public static final CInitializer INT_0 =
        buildInitializerExpression(SeqIntegerLiteralExpression.INT_0);

    /**
     * Returns the {@link CInitializer} for {@link Sequentialization#INIT_PC} for the main thread
     * and {@link Sequentialization#EXIT_PC} for all other threads.
     */
    public static CInitializer getPcInitializer(boolean pIsMainThread) {
      return pIsMainThread ? INT_INIT_PC : INT_EXIT_PC;
    }
  }

  public static class SeqInitializerList {

    public static final CInitializerList EMPTY_LIST =
        new CInitializerList(FileLocation.DUMMY, ImmutableList.of());
  }
}

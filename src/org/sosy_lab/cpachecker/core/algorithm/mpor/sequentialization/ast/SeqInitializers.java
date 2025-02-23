// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;

public class SeqInitializers {

  public static class SeqInitializer {

    public static final CInitializer INT_MINUS_1 =
        buildInitializerExpression(SeqIntegerLiteralExpression.INT_MINUS_1);

    public static final CInitializer INT_0 =
        buildInitializerExpression(SeqIntegerLiteralExpression.INT_0);

    public static CInitializer buildInitializerExpression(CIntegerLiteralExpression pExpression) {
      return new CInitializerExpression(FileLocation.DUMMY, pExpression);
    }
  }

  public static class SeqInitializerList {

    public static final CInitializerList EMPTY_LIST =
        new CInitializerList(FileLocation.DUMMY, ImmutableList.of());
  }
}

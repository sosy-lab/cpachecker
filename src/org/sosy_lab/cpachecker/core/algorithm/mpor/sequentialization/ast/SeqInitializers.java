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

    public static final CInitializer INT_0 = buildIntInitializer(SeqIntegerLiteralExpression.INT_0);

    public static final CInitializer INT_1 = buildIntInitializer(SeqIntegerLiteralExpression.INT_1);

    public static CInitializer buildIntInitializer(CIntegerLiteralExpression pExpression) {
      return new CInitializerExpression(FileLocation.DUMMY, pExpression);
    }
  }

  public static class SeqInitializerList {

    public static final CInitializerList EMPTY_LIST =
        new CInitializerList(FileLocation.DUMMY, ImmutableList.of());

    public static CInitializerList buildIntInitializerList(
        CIntegerLiteralExpression pExpression, int pAmount) {

      ImmutableList.Builder<CInitializer> initializers = ImmutableList.builder();
      for (int i = 0; i < pAmount; i++) {
        initializers.add(SeqInitializer.buildIntInitializer(pExpression));
      }
      return new CInitializerList(FileLocation.DUMMY, initializers.build());
    }
  }
}

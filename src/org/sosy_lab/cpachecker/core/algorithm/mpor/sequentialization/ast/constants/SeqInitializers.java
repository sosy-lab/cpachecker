// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;

public class SeqInitializers {

  public static final CInitializer INT_0 =
      new CInitializerExpression(FileLocation.DUMMY, SeqIntegerLiteralExpressions.INT_0);

  public static final CInitializer INT_1 =
      new CInitializerExpression(FileLocation.DUMMY, SeqIntegerLiteralExpressions.INT_1);

  public static final CInitializerList EMPTY_LIST =
      new CInitializerList(FileLocation.DUMMY, ImmutableList.of());
}

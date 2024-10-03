// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqDeclarations {

  // CParameterDeclaration =======================================================================

  public static final CParameterDeclaration COND =
      new CParameterDeclaration(FileLocation.DUMMY, SeqTypes.CONST_INT, SeqToken.COND);

  public static final CParameterDeclaration ARRAY =
      new CParameterDeclaration(
          FileLocation.DUMMY, SeqTypes.CONST_POINTER_CONST_INT, SeqToken.ARRAY);

  public static final CParameterDeclaration SIZE =
      new CParameterDeclaration(FileLocation.DUMMY, SeqTypes.CONST_INT, SeqToken.SIZE);

  // CFunctionDeclarations =======================================================================

  public static final CFunctionDeclaration VERIFIER_NONDET_INT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqTypes.VERIFIER_NONDET_INT,
          SeqToken.VERIFIER_NONDET_INT,
          ImmutableList.of(),
          ImmutableSet.of());

  public static final CFunctionDeclaration ASSUME =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqTypes.ASSUME,
          SeqNameBuilder.createFuncName(SeqToken.ASSUME),
          ImmutableList.of(COND),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  public static final CFunctionDeclaration ANY_UNSIGNED =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqTypes.ANY_UNSIGNED,
          SeqNameBuilder.createFuncName(SeqToken.ANY_UNSIGNED),
          ImmutableList.of(ARRAY, SIZE),
          ImmutableSet.of());

  public static final CFunctionDeclaration MAIN =
      new CFunctionDeclaration(
          FileLocation.DUMMY, SeqTypes.MAIN, SeqToken.MAIN, ImmutableList.of(), ImmutableSet.of());
}

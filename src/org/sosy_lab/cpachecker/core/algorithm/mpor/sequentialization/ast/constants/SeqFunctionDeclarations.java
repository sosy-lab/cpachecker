// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.VerifierNondetFunctionType;

public class SeqFunctionDeclarations {

  public static final CFunctionDeclaration VERIFIER_NONDET_INT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.VERIFIER_NONDET_INT,
          VerifierNondetFunctionType.INT.getName(),
          ImmutableList.of(),
          ImmutableSet.of());

  public static final CFunctionDeclaration VERIFIER_NONDET_UINT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.VERIFIER_NONDET_UINT,
          VerifierNondetFunctionType.UINT.getName(),
          ImmutableList.of(),
          ImmutableSet.of());

  public static final CFunctionDeclaration MALLOC =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.MALLOC,
          "malloc",
          ImmutableList.of(SeqParameterDeclarations.SIZE_PARAMETER_MALLOC),
          ImmutableSet.of());
}

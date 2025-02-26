// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.declarations;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;

public class SeqDeclarationBuilder {

  public static ImmutableList<LineOfCode> buildOriginalDeclarations(
      ImmutableSet<MPORThread> pThreads) {
    ImmutableList.Builder<LineOfCode> rOriginalDeclarations = ImmutableList.builder();
    // add all original program declarations that are not substituted
    rOriginalDeclarations.add(LineOfCode.of(0, SeqComment.UNCHANGED_DECLARATIONS));
    for (MPORThread thread : pThreads) {
      ImmutableList<CDeclaration> nonVariableDeclarations =
          ThreadUtil.extractNonVariableDeclarations(thread);
      rOriginalDeclarations.addAll(LineOfCodeUtil.buildLinesOfCode(nonVariableDeclarations));
    }
    rOriginalDeclarations.add(LineOfCode.empty());
    return rOriginalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildGlobalDeclarations(
      CSimpleDeclarationSubstitution pMainThreadSubstitution) {

    ImmutableList.Builder<LineOfCode> rGlobalDeclarations = ImmutableList.builder();
    rGlobalDeclarations.add(LineOfCode.of(0, SeqComment.GLOBAL_VAR_DECLARATIONS));
    ImmutableList<CVariableDeclaration> globalDeclarations =
        pMainThreadSubstitution.getGlobalDeclarations();
    rGlobalDeclarations.addAll(LineOfCodeUtil.buildLinesOfCode(globalDeclarations));
    rGlobalDeclarations.add(LineOfCode.empty());
    return rGlobalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildLocalDeclarations(
      ImmutableCollection<CSimpleDeclarationSubstitution> pSubstitutions) {

    ImmutableList.Builder<LineOfCode> rLocalDeclarations = ImmutableList.builder();
    rLocalDeclarations.add(LineOfCode.of(0, SeqComment.LOCAL_VAR_DECLARATIONS));
    for (CSimpleDeclarationSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> localDeclarations = substitution.getLocalDeclarations();
      for (CVariableDeclaration localDeclaration : localDeclarations) {
        if (localDeclaration.getInitializer() == null) {
          // no initializer -> add declaration as is
          rLocalDeclarations.add(LineOfCode.of(0, localDeclaration.toASTString()));
        } else {
          // initializer -> add declaration without initializer (and assign later in seq)
          rLocalDeclarations.add(
              LineOfCode.of(0, localDeclaration.toASTStringWithoutInitializer()));
        }
      }
    }
    rLocalDeclarations.add(LineOfCode.empty());
    return rLocalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildParameterDeclarations(
      ImmutableCollection<CSimpleDeclarationSubstitution> pSubstitutions) {

    ImmutableList.Builder<LineOfCode> rParameterDeclarations = ImmutableList.builder();
    rParameterDeclarations.add(LineOfCode.of(0, SeqComment.PARAMETER_VAR_SUBSTITUTES));
    for (CSimpleDeclarationSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> parameterDeclarations =
          substitution.getParameterDeclarations();
      rParameterDeclarations.addAll(LineOfCodeUtil.buildLinesOfCode(parameterDeclarations));
    }
    rParameterDeclarations.add(LineOfCode.empty());
    return rParameterDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildReturnPcDeclarations(
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
          pReturnPcVariables) {

    ImmutableList.Builder<LineOfCode> rReturnPcDeclarations = ImmutableList.builder();
    rReturnPcDeclarations.add(LineOfCode.of(0, SeqComment.RETURN_PCS));
    for (ImmutableMap<CFunctionDeclaration, CIdExpression> map : pReturnPcVariables.values()) {
      for (CIdExpression returnPc : map.values()) {
        rReturnPcDeclarations.add(LineOfCode.of(0, returnPc.getDeclaration().toASTString()));
      }
    }
    rReturnPcDeclarations.add(LineOfCode.empty());
    return rReturnPcDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildThreadSimulationVariableDeclarations(
      ThreadSimulationVariables pThreadSimulationVariables) {

    ImmutableList.Builder<LineOfCode> rThreadSimulationVariableDeclarations =
        ImmutableList.builder();
    rThreadSimulationVariableDeclarations.add(LineOfCode.of(0, SeqComment.THREAD_SIMULATION));
    for (CIdExpression threadVariable : pThreadSimulationVariables.getIdExpressions()) {
      assert threadVariable.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDeclaration = (CVariableDeclaration) threadVariable.getDeclaration();
      rThreadSimulationVariableDeclarations.add(LineOfCode.of(0, varDeclaration.toASTString()));
    }
    rThreadSimulationVariableDeclarations.add(LineOfCode.empty());
    return rThreadSimulationVariableDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildFunctionDeclarations() {
    ImmutableList.Builder<LineOfCode> rFunctionDeclarations = ImmutableList.builder();
    rFunctionDeclarations.add(LineOfCode.of(0, SeqComment.CUSTOM_FUNCTION_DECLARATIONS));
    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.ASSERT_FAIL.toASTString()));
    rFunctionDeclarations.add(
        LineOfCode.of(0, SeqFunctionDeclaration.VERIFIER_NONDET_INT.toASTString()));
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.ABORT.toASTString()));
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.REACH_ERROR.toASTString()));
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.ASSUME.toASTString()));
    // main should always be duplicate
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.MAIN.toASTString()));
    rFunctionDeclarations.add(LineOfCode.empty());
    return rFunctionDeclarations.build();
  }
}

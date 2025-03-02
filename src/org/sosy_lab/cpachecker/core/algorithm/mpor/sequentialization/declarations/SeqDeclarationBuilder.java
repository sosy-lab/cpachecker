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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
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
      MPOROptions pOptions, ImmutableSet<MPORThread> pThreads) {

    ImmutableList.Builder<LineOfCode> rOriginalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rOriginalDeclarations.add(LineOfCode.of(0, SeqComment.UNCHANGED_DECLARATIONS));
    }
    // add all original program declarations that are not substituted
    for (MPORThread thread : pThreads) {
      ImmutableList<CDeclaration> nonVariableDeclarations =
          ThreadUtil.extractNonVariableDeclarations(thread);
      for (CDeclaration declaration : nonVariableDeclarations) {
        // if it is a function declaration, add it only if the option is set
        if (!(declaration instanceof CFunctionDeclaration) || pOptions.inputFunctionDeclarations) {
          rOriginalDeclarations.add(LineOfCode.of(0, declaration.toASTString()));
        }
      }
    }
    if (pOptions.comments) {
      rOriginalDeclarations.add(LineOfCode.empty());
    }
    return rOriginalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildGlobalDeclarations(
      MPOROptions pOptions, CSimpleDeclarationSubstitution pMainThreadSubstitution) {

    ImmutableList.Builder<LineOfCode> rGlobalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rGlobalDeclarations.add(LineOfCode.of(0, SeqComment.GLOBAL_VAR_DECLARATIONS));
    }
    ImmutableList<CVariableDeclaration> globalDeclarations =
        pMainThreadSubstitution.getGlobalDeclarations();
    for (CVariableDeclaration globalDeclaration : globalDeclarations) {
      if (!PthreadUtil.isPthreadObjectType(globalDeclaration.getType())) {
        rGlobalDeclarations.add(LineOfCodeUtil.buildLineOfCode(globalDeclaration));
      }
    }
    if (pOptions.comments) {
      rGlobalDeclarations.add(LineOfCode.empty());
    }
    return rGlobalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildLocalDeclarations(
      MPOROptions pOptions, ImmutableCollection<CSimpleDeclarationSubstitution> pSubstitutions) {

    ImmutableList.Builder<LineOfCode> rLocalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rLocalDeclarations.add(LineOfCode.of(0, SeqComment.LOCAL_VAR_DECLARATIONS));
    }
    for (CSimpleDeclarationSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> localDeclarations = substitution.getLocalDeclarations();
      for (CVariableDeclaration localDeclaration : localDeclarations) {
        if (!PthreadUtil.isPthreadObjectType(localDeclaration.getType())) {
          if (localDeclaration.getInitializer() == null) {
            // no initializer -> add declaration as is
            rLocalDeclarations.add(LineOfCode.of(0, localDeclaration.toASTString()));
          } else {
            // initializer -> add declaration without initializer (and assign later in cases)
            rLocalDeclarations.add(
                LineOfCode.of(0, localDeclaration.toASTStringWithoutInitializer()));
          }
        }
      }
    }
    if (pOptions.comments) {
      rLocalDeclarations.add(LineOfCode.empty());
    }
    return rLocalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildParameterDeclarations(
      MPOROptions pOptions, ImmutableCollection<CSimpleDeclarationSubstitution> pSubstitutions) {

    ImmutableList.Builder<LineOfCode> rParameterDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rParameterDeclarations.add(LineOfCode.of(0, SeqComment.PARAMETER_VAR_SUBSTITUTES));
    }
    for (CSimpleDeclarationSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> parameterDeclarations =
          substitution.getParameterDeclarations();
      for (CVariableDeclaration parameterDeclaration : parameterDeclarations) {
        if (!PthreadUtil.isPthreadObjectType(parameterDeclaration.getType())) {
          rParameterDeclarations.add(LineOfCodeUtil.buildLineOfCode(parameterDeclaration));
        }
      }
    }
    if (pOptions.comments) {
      rParameterDeclarations.add(LineOfCode.empty());
    }
    return rParameterDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildReturnPcDeclarations(
      MPOROptions pOptions,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
          pReturnPcVariables) {

    ImmutableList.Builder<LineOfCode> rReturnPcDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rReturnPcDeclarations.add(LineOfCode.of(0, SeqComment.RETURN_PCS));
    }
    for (ImmutableMap<CFunctionDeclaration, CIdExpression> map : pReturnPcVariables.values()) {
      for (CIdExpression returnPc : map.values()) {
        rReturnPcDeclarations.add(LineOfCode.of(0, returnPc.getDeclaration().toASTString()));
      }
    }
    if (pOptions.comments) {
      rReturnPcDeclarations.add(LineOfCode.empty());
    }
    return rReturnPcDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildThreadSimulationVariableDeclarations(
      MPOROptions pOptions, ThreadSimulationVariables pThreadSimulationVariables) {

    ImmutableList.Builder<LineOfCode> rThreadSimulationVariableDeclarations =
        ImmutableList.builder();
    if (pOptions.comments) {
      rThreadSimulationVariableDeclarations.add(
          LineOfCode.of(0, SeqComment.THREAD_SIMULATION_VARIABLES));
    }
    for (CIdExpression threadVariable : pThreadSimulationVariables.getIdExpressions()) {
      assert threadVariable.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDeclaration = (CVariableDeclaration) threadVariable.getDeclaration();
      rThreadSimulationVariableDeclarations.add(LineOfCode.of(0, varDeclaration.toASTString()));
    }
    if (pOptions.comments) {
      rThreadSimulationVariableDeclarations.add(LineOfCode.empty());
    }
    return rThreadSimulationVariableDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildFunctionDeclarations(MPOROptions pOptions) {
    ImmutableList.Builder<LineOfCode> rFunctionDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rFunctionDeclarations.add(LineOfCode.of(0, SeqComment.CUSTOM_FUNCTION_DECLARATIONS));
    }
    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.ASSERT_FAIL.toASTString()));
    rFunctionDeclarations.add(
        LineOfCode.of(0, SeqFunctionDeclaration.VERIFIER_NONDET_INT.toASTString()));
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.ABORT.toASTString()));
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.REACH_ERROR.toASTString()));
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.ASSUME.toASTString()));
    // main should always be duplicate
    rFunctionDeclarations.add(LineOfCode.of(0, SeqFunctionDeclaration.MAIN.toASTString()));
    if (pOptions.comments) {
      rFunctionDeclarations.add(LineOfCode.empty());
    }
    return rFunctionDeclarations.build();
  }
}

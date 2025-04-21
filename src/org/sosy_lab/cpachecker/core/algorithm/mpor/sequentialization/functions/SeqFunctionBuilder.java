// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumption;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorGlobalVariable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SeqBitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqFunctionBuilder {

  public static SeqMainFunction buildMainFunction(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      BitVectorVariables pBitVectorVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // used to store which injected variables are initialized with 1
    ImmutableList.Builder<CIdExpression> updatedVariables = ImmutableList.builder();
    // collect all global variables accessed in substitute edges, and assign unique ids
    ImmutableList<CVariableDeclaration> allGlobalVariables =
        SubstituteUtil.getAllGlobalVariables(pSubstituteEdges.values());
    ImmutableList<BitVectorGlobalVariable> bitVectorGlobalVariables =
        createBitVectorGlobalVariables(
            pOptions, SubstituteUtil.extractThreads(pSubstitutions), allGlobalVariables);
    // create case clauses in main method
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses =
        SeqCaseClauseBuilder.buildCaseClauses(
            pOptions,
            updatedVariables,
            pSubstitutions,
            pSubstituteEdges,
            bitVectorGlobalVariables,
            pBitVectorVariables,
            pPcVariables,
            pThreadSimulationVariables,
            pBinaryExpressionBuilder,
            pLogger);
    // include assumptions over thread simulation variables
    ImmutableListMultimap<MPORThread, SeqAssumption> threadSimulationAssumptions =
        SeqAssumptionBuilder.createThreadSimulationAssumptions(
            pPcVariables, pThreadSimulationVariables, pBinaryExpressionBuilder);
    return new SeqMainFunction(
        pOptions,
        updatedVariables.build(),
        pSubstitutions.size(),
        bitVectorGlobalVariables,
        threadSimulationAssumptions,
        caseClauses,
        pBitVectorVariables,
        pPcVariables,
        pBinaryExpressionBuilder);
  }

  private static ImmutableList<BitVectorGlobalVariable> createBitVectorGlobalVariables(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pThreads,
      ImmutableList<CVariableDeclaration> pGlobalVariableDeclaration) {

    ImmutableList.Builder<BitVectorGlobalVariable> rVariables = ImmutableList.builder();
    int id = 0;
    for (CVariableDeclaration variableDeclaration : pGlobalVariableDeclaration) {
      assert variableDeclaration.isGlobal();
      BitVectorGlobalVariable globalVariable =
          new BitVectorGlobalVariable(
              id++,
              variableDeclaration,
              pOptions.porBitVectorEncoding.equals(SeqBitVectorEncoding.SCALAR)
                  ? Optional.of(createAccessVariables(pOptions, pThreads, variableDeclaration))
                  : Optional.empty());
      rVariables.add(globalVariable);
    }
    return rVariables.build();
  }

  private static ImmutableMap<MPORThread, CIdExpression> createAccessVariables(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pThreads,
      CVariableDeclaration pVariableDeclaration) {
    ImmutableMap.Builder<MPORThread, CIdExpression> rAccessVariables = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      rAccessVariables.put(
          thread, BitVectorUtil.createScalarAccessVariable(pOptions, thread, pVariableDeclaration));
    }
    return rAccessVariables.buildOrThrow();
  }
}

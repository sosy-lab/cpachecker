// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCommentStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

public final class SeqAtomicBeginStatement extends CSeqThreadStatement {

  SeqAtomicBeginStatement(
      CLeftHandSide pPcLeftHandSide, ImmutableSet<SubstituteEdge> pSubstituteEdges, int pTargetPc) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc);
  }

  private SeqAtomicBeginStatement(
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    // just add a comment with the function name for better overview
    return buildExportStatements(
        new CCommentStatement(PthreadFunctionType.VERIFIER_ATOMIC_BEGIN.name + ";"));
  }

  @Override
  public SeqAtomicBeginStatement withTargetPc(int pTargetPc) {
    return new SeqAtomicBeginStatement(
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqAtomicBeginStatement(
        pcLeftHandSide, substituteEdges, Optional.empty(), Optional.of(pLabel), injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqAtomicBeginStatement(
        pcLeftHandSide, substituteEdges, targetPc, targetGoto, pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return true;
  }

  @Override
  public boolean synchronizesThreads() {
    // atomic sections do not synchronize threads per se. when encountered, no check is required
    return false;
  }
}

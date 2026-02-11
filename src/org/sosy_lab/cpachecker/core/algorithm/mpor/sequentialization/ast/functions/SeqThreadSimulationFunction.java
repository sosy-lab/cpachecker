// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportFunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;

/**
 * A (separate) function to simulate a thread in. The thread simulation is usually placed inside the
 * {@code main()} function, but can also be placed in a separate function, e.g. when {@link
 * MPOROptions#loopUnrolling()} is enabled.
 */
public final class SeqThreadSimulationFunction extends SeqFunction {

  public final MPORThread thread;

  public SeqThreadSimulationFunction(
      MPOROptions pOptions, CCompoundStatement pFunctionBody, MPORThread pThread) {

    super(new CExportFunctionDefinition(buildDeclaration(pOptions, pThread.id()), pFunctionBody));
    thread = pThread;
  }

  private static CFunctionDeclaration buildDeclaration(MPOROptions pOptions, int pThreadId) {
    CFunctionType functionType = new CFunctionType(CVoidType.VOID, ImmutableList.of(), false);
    String functionName = SeqNameUtil.buildThreadPrefix(pOptions, pThreadId) + "_sequentialized";
    return new CFunctionDeclaration(
        FileLocation.DUMMY, functionType, functionName, ImmutableList.of(), ImmutableSet.of());
  }
}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMGConsistencyVerifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.WritableSMG;


public class SMGStateBuilder {
  final private WritableSMG smg;
  final private LogManager logger;

  private static SMGRuntimeCheck runtimeCheckLevel = SMGRuntimeCheck.NONE;

  public SMGStateBuilder(SMGState pOriginal, LogManager pLogger) {
    smg = SMGFactory.createWritableCopy(pOriginal.getSMG());
    logger = pLogger;
  }

  public SMGStateBuilder(LogManager pLogger, MachineModel pModel) {
    smg = SMGFactory.createWritableSMG(pModel);
    logger = pLogger;
  }

  final public void performConsistencyCheck(SMGRuntimeCheck pLevel) throws SMGInconsistentException {
    if (SMGStateBuilder.runtimeCheckLevel.isFinerOrEqualThan(pLevel)) {
      if ( ! CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg) ) {
        throw new SMGInconsistentException("SMG was found inconsistent during a check");
      }
    }
  }

  static final public void setRuntimeCheck(SMGRuntimeCheck pLevel) {
    runtimeCheckLevel = pLevel;
  }

  public SMGState build() throws SMGInconsistentException {
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return new SMGState(logger, smg);
  }

  /**
   * Add a new stack frame for the passed function.
   *
   * Keeps consistency: yes
   *
   * @param pFunctionDeclaration A function for which to create a new stack frame
   * @throws SMGInconsistentException
   */
  public void addStackFrame(CFunctionDeclaration pFunctionDeclaration) throws SMGInconsistentException {
    smg.addStackFrame(pFunctionDeclaration);
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  public void pruneUnreachable() throws SMGInconsistentException {
    smg.pruneUnreachable();
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  public void dropStackFrame() throws SMGInconsistentException {
    smg.dropStackFrame();
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }
}

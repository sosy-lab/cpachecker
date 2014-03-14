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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public interface WritableSMG extends ReadableSMG {
  public void addHeapObject(SMGObject pObject);
  public void addGlobalObject(SMGRegion pObject);
  public void addStackObject(SMGRegion pObject);
  public void addStackFrame(CFunctionDeclaration pFunction);
  public void dropStackFrame();
  public void removeHeapObject(SMGObject pObject);
  public SMGObject addLocalVariable(CType pType, String pVarName) throws SMGInconsistentException;
  public SMGObject addGlobalVariable(CType pType, String pVarName) throws SMGInconsistentException;

  public void addValue(Integer pValue);
  public void removeValue(Integer pValue);

  public void addPointsToEdge(SMGEdgePointsTo pEdge);
  public void removePointsToEdge(Integer pValue);

  public void addHasValueEdge(SMGEdgeHasValue pEdge);
  public void removeHasValueEdge(SMGEdgeHasValue pEdge);
  public void replaceHVSet(Set<SMGEdgeHasValue> pHV);

  public void setValidity(SMGRegion pRegion, boolean pValidity);
  public void pruneUnreachable();
  public void setMemoryLeak();

  public void clearExplicit(SMGKnownSymValue pKey);
  public void putExplicit(SMGKnownSymValue pKey, SMGKnownExpValue pValue);

  public SMGEdgePointsTo addNewHeapAllocation(int pSize, String pLabel) throws SMGInconsistentException;
  public SMGEdgeHasValue writeValue(SMGObject pObject, int pOffset, CType pType, SMGSymbolicValue pValue) throws SMGInconsistentException;
  public void free(Integer pAddress, Integer pOffset, SMGRegion pRegion) throws SMGInconsistentException;
  public void copy(SMGObject pSource, SMGObject pTarget, int pSourceRangeOffset, int pSourceRangeSize, int pTargetRangeOffset) throws SMGInconsistentException;
}

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

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public interface ReadableSMG {
  public Set<SMGObject> getObjects();
  public Map<String, SMGRegion> getGlobalObjects();
  public Set<SMGObject> getHeapObjects();
  public SMGRegion getObjectForVisibleVariable(String pVariable);
  public ArrayDeque<CLangStackFrame> getStackFrames();
  public SMGObject getNullObject();
  public boolean isHeapObject(SMGObject pObject);
  public boolean isGlobalObject(SMGObject pObject);
  public SMGRegion getStackReturnObject(int pUp);
  public SMGObject getObjectPointedBy(Integer pValue);

  public boolean isObjectValid(SMGObject pRegion);
  public BitSet getNullBytesForObject(SMGObject pObject);

  public Set<Integer> getValues();
  public boolean containsValue(Integer pValue);
  public int getNullValue();
  public boolean isUnequal(int pV1, int pV2) throws SMGInconsistentException;
  public Integer readValue(SMGObject pObject, int pOffset, CType pType);

  public SMGEdgePointsTo getPointer(Integer pValue) throws SMGInconsistentException;
  public Set<SMGEdgePointsTo> getPTEdges();
  public boolean isPointer(Integer pValue);
  public Integer getAddress(SMGObject pMemory, Integer pOffset);


  public Iterable<SMGEdgeHasValue> getHVEdges();
  public Iterable<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter);
  public SMGEdgeHasValue getUniqueHV(SMGEdgeHasValueFilter pFilter, boolean pStrict);
  public boolean isCoveredByNullifiedBlocks(SMGObject pObject, int pOffset, CType pType);
  public boolean isCoveredByNullifiedBlocks(SMGEdgeHasValue pEdge);

  public MachineModel getMachineModel();
  public boolean hasMemoryLeaks();

  public SMGExplicitValue getExplicit(SMGKnownSymValue pValue);
}

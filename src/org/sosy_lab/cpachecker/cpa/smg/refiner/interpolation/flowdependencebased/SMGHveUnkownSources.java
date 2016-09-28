/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.SMGState.SMGStateEdgePair;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

public class SMGHveUnkownSources extends SMGHveSources {

  private static final SMGHveUnkownSources INSTANCE = new SMGHveUnkownSources();

  public SMGHveUnkownSources() {
    super(null, null, null, null, null, false, null, null, null, null);
  }

  @Override
  public SMGAddressValueAndStateList createBinaryAddress(
      SMGAddressValueAndStateList pResultAddressValueAndStateList, SMGAddressValue pAddressValue,
      SMGExplicitValue pAddressOffset) {
    return pResultAddressValueAndStateList;
  }

  @Override
  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress,
      SMGSymbolicValue pReadValueResult, Set<SMGKnownAddress> pAdditionalSources,
      boolean pOverlappingZeroEdges) {
    return pReadValueResult;
  }

  @Override
  public void registerTargetWrite(SMGAddress pAddress) {
    return;
  }

  @Override
  public void registerTargetWrite(SMGAddressValue pAddressValue) {
    return;
  }

  @Override
  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress,
      SMGSymbolicValue pReadValueResult, Set<SMGKnownAddress> pAdditionalSources) {
    return pReadValueResult;
  }

  @Override
  public void registerExplicitValue(SMGKnownSymValue pKey, SMGKnownExpValue pValue) {
    return;
  }

  @Override
  public Collection<Entry<SMGEdgeHasValue, SMGKnownAddress>> getHveSources() {
    return ImmutableSet.of();
  }

  @Override
  public Collection<Entry<SMGObject, SMGKnownAddress>> getObjectMap() {
    return ImmutableSet.of();
  }

  @Override
  public void registerHasValueEdge(SMGEdgeHasValue pEdge) {
    return;
  }

  @Override
  public void registerHasValueEdge(SMGObject pSourceObject, int pSourceOffset,
      SMGEdgeHasValue pEdge) {
    return;
  }

  @Override
  public void registerNewObjectAllocation(SMGKnownExpValue pSize, SMGObject pResult,
      boolean pIsStackAndVariableTypeSize) {
    return;
  }

  @Override
  public void registerHasValueEdgeFromCopy(SMGObject pObject, int pOffset,
      SMGStateEdgePair pNewSMGStateAndEdge, SMGExplicitValue pCopyRange,
      SMGKnownExpValue pTargetRangeOffset) {
    return;
  }

  @Override
  public void registerWriteValueSource(SMGKnownAddress pAddress, SMGSymbolicValue pValue,
      SMGStateEdgePair pResult) {
    return;
  }

  @Override
  public void registerMemsetCount(SMGExplicitValue pCountValue,
      SMGStateEdgePair pResultStateAndEdge) {
    return;
  }

  public static SMGHveUnkownSources getInstance() {
    return INSTANCE;
  }

  @Override
  public SMGHveSources copy() {
    return this;
  }

  @Override
  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress, SMGSymbolicValue pValue) {
    return pValue;
  }

  @Override
  public void registerHasValueEdgeFromCopy(SMGObject pObject, int pOffset, SMGEdgeHasValue pNewEdge,
      SMGExplicitValue pCopyRange, SMGKnownExpValue pTargetRangeOffset) {
    return;
  }

  @Override
  public void clear() {
    return;
  }

  @Override
  public Set<SMGKnownAddress> getNewFields() {
    return ImmutableSet.of();
  }

  @Override
  public SMGSymbolicValue createGetAddressSource(SMGSymbolicValue pResult,
      SMGKnownExpValue pOffset) {
    return pResult;
  }

  @Override
  public SMGSymbolicValue createBinaryOpValue(SMGSymbolicValue pResult, SMGSymbolicValue pV1,
      SMGSymbolicValue pV2) {
    return pResult;
  }

  @Override
  public Set<SMGRegion> getVariableTypeDclRegion() {
    return null;
  }

  @Override
  public SMGValue getPathEndValue() {
    return null;
  }

  @Override
  public Set<SMGKnownAddress> getSourcesOfDereferences() {
    return null;
  }

  @Override
  public void registerDereference(SMGValue pValue) {
    return;
  }

  @Override
  public boolean isPathEnd() {
    return false;
  }

  @Override
  public void setPathEnd(boolean pPathEnd, SMGValue pAssumptionValue) {
    return;
  }

  @Override
  public SMGAddressValue createPointer(SMGKnownSymValue pValue, SMGEdgePointsTo pAddressValue) {
    return SMGKnownAddVal.valueOf(pAddressValue.getValue(),
        pAddressValue.getObject(), pAddressValue.getOffset());
  }

  @Override
  public SMGExplicitValue createExpValue(SMGKnownSymValue pSymVal, SMGKnownExpValue pExpVal) {
    return pExpVal;
  }

  @Override
  public SMGAddressValueAndStateList createAddressSource(SMGAddressValueAndStateList pResult,
      SMGAddress pAddress) {
    return pResult;
  }
}
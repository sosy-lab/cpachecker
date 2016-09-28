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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

public class SMGHveUnkownSources extends SMGHveSources {

  private static final SMGHveUnkownSources INSTANCE = new SMGHveUnkownSources();

  public SMGHveUnkownSources() {
    super(null, null);
  }

  @Override
  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress,
      SMGSymbolicValue pReadValueResult, Set<SMGAddress> pAdditionalSources) {
    return pReadValueResult;
  }

  @Override
  public void registerExplicitValue(SMGKnownSymValue pKey, SMGKnownExpValue pValue) {
    return;
  }

  @Override
  public Collection<Entry<SMGEdgeHasValue, SMGAddress>> getHveSources() {
    return ImmutableSet.of();
  }

  @Override
  public Collection<Entry<SMGObject, SMGAddress>> getObjectMap() {
    return ImmutableSet.of();
  }

  @Override
  public void registerHasValueEdge(SMGObject pSourceObject, int pSourceOffset,
      SMGEdgeHasValue pEdge) {
    return;
  }

  @Override
  public void registerNewObjectAllocation(SMGKnownExpValue pSize, SMGObject pResult) {
    return;
  }

  @Override
  public void registerWriteValueSource(SMGAddress pAddress, SMGSymbolicValue pValue,
      SMGEdgeHasValue pResultEdge) {
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
}
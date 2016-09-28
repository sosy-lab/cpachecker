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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class SMGHveSources {

  private final Multimap<SMGEdgeHasValue, SMGAddress> hveMap;
  private final Multimap<SMGObject, SMGAddress> objectMap;

  public SMGHveSources() {
    hveMap = HashMultimap.create();
    objectMap = HashMultimap.create();
  }

  public SMGHveSources(Multimap<SMGEdgeHasValue, SMGAddress> pHveMap,
      Multimap<SMGObject, SMGAddress> pObjectMap) {
    hveMap = pHveMap;
    objectMap = pObjectMap;
  }

  public SMGHveSources(SMGHveSources pSource) {
    hveMap = HashMultimap.create();
    hveMap.putAll(pSource.hveMap);
    objectMap = HashMultimap.create();
    objectMap.putAll(pSource.objectMap);
  }

  public Collection<Entry<SMGObject, SMGAddress>> getObjectMap() {
    return objectMap.entries();
  }

  public Collection<Entry<SMGEdgeHasValue, SMGAddress>> getHveSources() {
    return hveMap.entries();
  }

  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress,
      SMGSymbolicValue pReadValueResult,
      Set<SMGAddress> pAdditionalSources) {

    if (pReadValueResult.isUnknown()) { return pReadValueResult; }

    Set<SMGAddress> source = new HashSet<>();
    source.add(pAddress);
    source.addAll(pAdditionalSources);

    SMGExplicitValue sourceOffset = pAddress.getOffset();

    if (sourceOffset.containsSourceAddreses()) {
      source.addAll(sourceOffset.getSourceAdresses());
    }

    return SMGKnownSymValueAndSource.valueOf(pReadValueResult, source);
  }

  public void registerWriteValueSource(SMGAddress pAddress, SMGSymbolicValue pValue,
      SMGEdgeHasValue pResultEdge) {

    SMGExplicitValue offset = pAddress.getOffset();

    if (!pValue.containsSourceAddreses() && !offset.containsSourceAddreses()) {
      return;
    }

    Set<SMGAddress> source = new HashSet<>();

    if (pValue.containsSourceAddreses()) {
      source.addAll(pValue.getSourceAdresses());
    }

    if (offset.containsSourceAddreses()) {
      source.addAll(offset.getSourceAdresses());
    }

    hveMap.putAll(pResultEdge, source);
  }

  public void registerHasValueEdge(SMGObject pSourceObject, int pSourceOffset,
      SMGEdgeHasValue pEdge) {
    hveMap.put(pEdge, SMGAddress.valueOf(pSourceObject, pSourceOffset));
  }

  public void registerExplicitValue(SMGKnownSymValue pKey, SMGKnownExpValue pValue) {

    if (!pValue.containsSourceAddreses()
        || pValue.getSourceAdresses().isEmpty()) {
      return;
    }

    for (SMGEdgeHasValue hve : hveMap.keys()) {
      if (hve.getValue() == pKey.getAsInt()) {
        hveMap.putAll(hve, pValue.getSourceAdresses());
      }
    }
  }

  public void registerNewObjectAllocation(SMGKnownExpValue pSize, SMGObject pResult) {

    if (pSize.containsSourceAddreses()) {
      objectMap.putAll(pResult, pSize.getSourceAdresses());
    }
  }

  public static class SMGAddressAndSource extends SMGAddress {

    private final Set<SMGAddress> source;

    protected SMGAddressAndSource(SMGObject pObject, SMGExplicitValue pOffset,
        Set<SMGAddress> pSource) {
      super(pObject, pOffset);
      source = pSource;
    }

    /**
     * Return an address with (offset + pAddedOffset).
     *
     * @param pAddedOffset The offset added to this address.
     */
    @Override
    public final SMGAddress add(SMGExplicitValue pAddedOffset) {

      if (isUnknown() || pAddedOffset.isUnknown()) {
        return SMGAddress.UNKNOWN;
      }

      return valueOf(
          getObject(),
          getOffset().add(pAddedOffset).getAsInt(),
          union(source, pAddedOffset.getSourceAdresses()));
    }

    public static SMGAddressAndSource valueOf(SMGObject pObj, int pOffset,
        Set<SMGAddress> pSource) {
      return new SMGAddressAndSource(pObj, SMGKnownExpValue.valueOf(pOffset), pSource);
    }

    private Set<SMGAddress> union(Set<SMGAddress> pSource1, Set<SMGAddress> pSource2) {
      Set<SMGAddress> result = new HashSet<>(pSource1.size() + pSource2.size());
      result.addAll(pSource1);
      result.addAll(pSource2);
      return result;
    }
  }

  public static class SMGKnownAddValueAndSource extends SMGKnownAddVal {

    private final Set<SMGAddress> source;

    protected SMGKnownAddValueAndSource(BigInteger pValue, SMGKnownAddress pAddress,
        Set<SMGAddress> pSource) {
      super(pValue, pAddress);
      source = ImmutableSet.copyOf(pSource);
    }

    @Override
    public Set<SMGAddress> getSourceAdresses() {
      return source;
    }

    @Override
    public boolean containsSourceAddreses() {
      return true;
    }

    public static SMGKnownAddValueAndSource valueOf(BigInteger pValue, SMGKnownAddress pAddress,
        Set<SMGAddress> pSource) {
      return new SMGKnownAddValueAndSource(pValue, pAddress, pSource);
    }

    public static SMGKnownAddValueAndSource valueOf(SMGKnownSymValue pValue, SMGKnownAddress pAddress,
        Set<SMGAddress> pSource) {
      return new SMGKnownAddValueAndSource(pValue.getValue(), pAddress, pSource);
    }

    public static SMGKnownAddValueAndSource valueOf(int pValue, SMGKnownAddress pAddress,
        Set<SMGAddress> pSource) {
      return new SMGKnownAddValueAndSource(BigInteger.valueOf(pValue), pAddress, pSource);
    }

    public static SMGKnownAddValueAndSource valueOf(long pValue, SMGKnownAddress pAddress,
        Set<SMGAddress> pSource) {
      return new SMGKnownAddValueAndSource(BigInteger.valueOf(pValue), pAddress, pSource);
    }

    public static SMGKnownAddValueAndSource valueOf(int pValue, SMGObject object, int offset, Set<SMGAddress> pSource) {
      return new SMGKnownAddValueAndSource(BigInteger.valueOf(pValue), SMGKnownAddress.valueOf(object, offset), pSource);
    }
  }

  public static class SMGKnownSymValueAndSource extends SMGKnownSymValue {

    private final Set<SMGAddress> source;

    protected SMGKnownSymValueAndSource(BigInteger pValue, Set<SMGAddress> pSource) {
      super(pValue);
      source = ImmutableSet.copyOf(pSource);
    }

    public static SMGSymbolicValue valueOf(SMGSymbolicValue pValue, Set<SMGAddress> pSource) {
      return new SMGKnownSymValueAndSource(pValue.getValue(), pSource);
    }

    @Override
    public Set<SMGAddress> getSourceAdresses() {
      return source;
    }

    @Override
    public boolean containsSourceAddreses() {
      return true;
    }

    public static SMGKnownSymValueAndSource valueOf(BigInteger pValue, Set<SMGAddress> pSource) {
      return new SMGKnownSymValueAndSource(pValue, pSource);
    }

    public static SMGKnownSymValueAndSource valueOf(int pValue) {
      return new SMGKnownSymValueAndSource(BigInteger.valueOf(pValue), ImmutableSet.of());
    }

    public static SMGKnownSymValueAndSource valueOf(int pValue, Set<SMGAddress> pSource) {
      return new SMGKnownSymValueAndSource(BigInteger.valueOf(pValue), pSource);
    }

    public static SMGKnownSymValueAndSource valueOf(long pValue, Set<SMGAddress> pSource) {
      return new SMGKnownSymValueAndSource(BigInteger.valueOf(pValue), pSource);
    }
  }

  public static class SMGKnownExpValueAndSource extends SMGKnownExpValue {

    private final Set<SMGAddress> source;

    protected SMGKnownExpValueAndSource(BigInteger pValue, Set<SMGAddress> pSource) {
      super(pValue);
      source = ImmutableSet.copyOf(pSource);
    }

    @Override
    public Set<SMGAddress> getSourceAdresses() {
      return source;
    }

    @Override
    public SMGExplicitValue negate() {
      return valueOf(getValue().negate(), source);
    }

    @Override
    public SMGExplicitValue xor(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().xor(pRVal.getValue()), union(source, pRVal.getSourceAdresses()));
    }

    @Override
    public SMGExplicitValue or(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().or(pRVal.getValue()), union(source, pRVal.getSourceAdresses()));
    }

    @Override
    public SMGExplicitValue and(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().and(pRVal.getValue()), union(source, pRVal.getSourceAdresses()));
    }

    @Override
    public SMGExplicitValue shiftLeft(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().shiftLeft(pRVal.getAsInt()), union(source, pRVal.getSourceAdresses()));
    }

    @Override
    public SMGExplicitValue multiply(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().multiply(pRVal.getValue()), union(source, pRVal.getSourceAdresses()));
    }

    @Override
    public SMGExplicitValue divide(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().divide(pRVal.getValue()), union(source, pRVal.getSourceAdresses()));
    }

    @Override
    public SMGExplicitValue subtract(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().subtract(pRVal.getValue()),
          union(source, pRVal.getSourceAdresses()));
    }

    @Override
    public SMGExplicitValue add(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().add(pRVal.getValue()), union(source, pRVal.getSourceAdresses()));
    }

    public static SMGKnownExpValueAndSource valueOf(BigInteger pValue, Set<SMGAddress> pSource) {
      return new SMGKnownExpValueAndSource(pValue, pSource);
    }

    public static SMGKnownExpValueAndSource valueOf(int pValue) {
      return new SMGKnownExpValueAndSource(BigInteger.valueOf(pValue), ImmutableSet.of());
    }

    public static SMGKnownExpValueAndSource valueOf(int pValue, Set<SMGAddress> pSource) {
      return new SMGKnownExpValueAndSource(BigInteger.valueOf(pValue), pSource);
    }

    public static SMGKnownExpValueAndSource valueOf(long pValue, Set<SMGAddress> pSource) {
      return new SMGKnownExpValueAndSource(BigInteger.valueOf(pValue), pSource);
    }

    private Set<SMGAddress> union(Set<SMGAddress> pSource1, Set<SMGAddress> pSource2) {
      Set<SMGAddress> result = new HashSet<>(pSource1.size() + pSource2.size());
      result.addAll(pSource1);
      result.addAll(pSource2);
      return result;
    }
  }

  public SMGHveSources copy() {
    return new SMGHveSources(this);
  }

  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress, SMGSymbolicValue pValue) {
    return createReadValueSource(pAddress, pValue, ImmutableSet.of());
  }

  public void registerHasValueEdgeFromCopy(SMGObject pObject, int pOffset, SMGEdgeHasValue pNewEdge,
      SMGExplicitValue pCopyRange, SMGKnownExpValue pTargetRangeOffset) {
    Set<SMGAddress> sources = new HashSet<>();

    if (pCopyRange.containsSourceAddreses()) {
      sources.addAll(pCopyRange.getSourceAdresses());
    }

    if (pTargetRangeOffset.containsSourceAddreses()) {
      sources.addAll(pTargetRangeOffset.getSourceAdresses());
    }

    sources.add(SMGAddress.valueOf(pObject, pOffset));
    hveMap.putAll(pNewEdge, sources);
  }

  public void registerMemsetCount(SMGExplicitValue pCountValue, SMGEdgeHasValue pNewEdge) {
    if (pCountValue.containsSourceAddreses()) {
      hveMap.putAll(pNewEdge, pCountValue.getSourceAdresses());
    }
  }
}
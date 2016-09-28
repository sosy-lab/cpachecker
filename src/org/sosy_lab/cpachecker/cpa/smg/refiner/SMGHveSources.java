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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndState;
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
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class SMGHveSources {

  private final Set<SMGKnownAddress> newFieldAllocation;
  private final Multimap<SMGEdgeHasValue, SMGKnownAddress> hveMap;
  private final Multimap<SMGKnownSymValue, SMGKnownAddress> valueMap;
  private final Multimap<SMGObject, SMGKnownAddress> objectMap;
  private final Set<SMGRegion> varTypeSizeDcl;
  private boolean pathEnd = false;
  private SMGValue pathEndValue = null;
  private final Set<SMGKnownAddress> sourcesOfDereference;
  private final Set<SMGObject> targetWriteObject;
  private final Set<SMGKnownAddress> sourcesOfUnkownTargetWrite;
  private final static Function<SMGKnownAddress, SMGKnownAddress> dropSource =
      (SMGKnownAddress add) -> {
        return add.dropSource();
      };

  public SMGHveSources() {
    hveMap = HashMultimap.create();
    objectMap = HashMultimap.create();
    newFieldAllocation = new HashSet<>();
    varTypeSizeDcl = new HashSet<>();
    sourcesOfDereference = new HashSet<>();
    valueMap = HashMultimap.create();
    targetWriteObject = new HashSet<>();
    sourcesOfUnkownTargetWrite = new HashSet<>();
  }

  public SMGHveSources(SMGHveSources pSource) {
    hveMap = HashMultimap.create();
    hveMap.putAll(pSource.hveMap);
    valueMap = HashMultimap.create();
    valueMap.putAll(pSource.valueMap);
    objectMap = HashMultimap.create();
    objectMap.putAll(pSource.objectMap);
    newFieldAllocation = new HashSet<>();
    newFieldAllocation.addAll(pSource.newFieldAllocation);
    varTypeSizeDcl = new HashSet<>();
    varTypeSizeDcl.addAll(pSource.varTypeSizeDcl);
    sourcesOfDereference = new HashSet<>();
    sourcesOfDereference.addAll(pSource.sourcesOfDereference);
    targetWriteObject = new HashSet<>();
    targetWriteObject.addAll(pSource.targetWriteObject);
    sourcesOfUnkownTargetWrite = new HashSet<>();
    sourcesOfUnkownTargetWrite.addAll(sourcesOfUnkownTargetWrite);
  }

  public SMGHveSources(Set<SMGKnownAddress> pNewFieldAllocation,
      Multimap<SMGEdgeHasValue, SMGKnownAddress> pHveMap,
      Multimap<SMGKnownSymValue, SMGKnownAddress> pValueMap,
      Multimap<SMGObject, SMGKnownAddress> pObjectMap, Set<SMGRegion> pVarTypeSizeDcl,
      boolean pPathEnd, SMGValue pPathEndValue, Set<SMGKnownAddress> pSourcesOfDereference,
      Set<SMGObject> pTargetWriteObject, Set<SMGKnownAddress> pSourcesOfUnknownTargetWrite) {
    newFieldAllocation = pNewFieldAllocation;
    hveMap = pHveMap;
    valueMap = pValueMap;
    objectMap = pObjectMap;
    varTypeSizeDcl = pVarTypeSizeDcl;
    pathEnd = pPathEnd;
    pathEndValue = pPathEndValue;
    sourcesOfDereference = pSourcesOfDereference;
    targetWriteObject = pTargetWriteObject;
    sourcesOfUnkownTargetWrite = pSourcesOfUnknownTargetWrite;
  }

  public void setPathEnd(boolean pPathEnd, SMGValue pAssumptionValue) {
    pathEnd = pPathEnd;
    pathEndValue = pAssumptionValue;
  }

  public boolean isPathEnd() {
    return pathEnd;
  }

  public SMGValue getPathEndValue() {
    return pathEndValue;
  }

  public Multimap<SMGKnownSymValue, SMGKnownAddress> getValueMap() {
    return valueMap;
  }

  public Collection<Entry<SMGObject, SMGKnownAddress>> getObjectMap() {
    return objectMap.entries();
  }

  public Collection<Entry<SMGEdgeHasValue, SMGKnownAddress>> getHveSources() {
    return hveMap.entries();
  }

  public void registerTargetWrite(SMGAddress pAddress) {
    targetWriteObject.add(pAddress.getObject());

    if (pAddress.containsSourceAddreses()) {
      sourcesOfUnkownTargetWrite.addAll(pAddress.getSourceAdresses());
    }
  }

  public void registerTargetWrite(SMGAddressValue pAddressValue) {
    targetWriteObject.add(pAddressValue.getObject());

    if (pAddressValue.containsSourceAddreses()) {
      sourcesOfUnkownTargetWrite.addAll(pAddressValue.getSourceAdresses());
    }
  }

  public Set<SMGKnownAddress> getSourcesOfUnkownTargetWrite() {
    return sourcesOfUnkownTargetWrite;
  }

  public Set<SMGObject> getTargetWriteObject() {
    return targetWriteObject;
  }

  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress,
      SMGSymbolicValue pReadValueResult,
      Set<SMGKnownAddress> pAdditionalSources) {
    return createReadValueSource(pAddress, pReadValueResult, pAdditionalSources, false);
  }

  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress,
      SMGSymbolicValue pReadValueResult,
      Set<SMGKnownAddress> pAdditionalSources,
      boolean overlappingZeroEdges) {

    if (pReadValueResult.isUnknown()) {
      return pReadValueResult;
    }

    Set<SMGKnownAddress> source = new HashSet<>();

    if (!overlappingZeroEdges) {
      source.add(pAddress.getAsKnownAddress());
    }

    source.addAll(pAdditionalSources);

    if (pAddress.containsSourceAddreses()) {
      source.addAll(pAddress.getSourceAdresses());
    }

    return SMGKnownSymValueAndSource.valueOf(pReadValueResult, source);
  }

  public void registerDereference(SMGValue pValue) {
    if (pValue.containsSourceAddreses()) {
      sourcesOfDereference.addAll(pValue.getSourceAdresses());
    }
  }

  public void registerWriteValueSource(SMGKnownAddress pAddress, SMGSymbolicValue pValue,
      SMGStateEdgePair pResult) {

    newFieldAllocation.add(pAddress.dropSource());

    SMGExplicitValue offset = pAddress.getOffset();

    if (!pResult.smgStateHasNewEdge()) {
      return;
    }

    if (!pValue.containsSourceAddreses() && !offset.containsSourceAddreses()) {
      return;
    }

    Set<SMGKnownAddress> source = new HashSet<>();

    if (pValue.containsSourceAddreses()) {
      source.addAll(pValue.getSourceAdresses());
    }

    if (offset.containsSourceAddreses()) {
      source.addAll(offset.getSourceAdresses());
    }

    hveMap.putAll(pResult.getNewEdge(), source);
  }

  public void registerHasValueEdge(SMGEdgeHasValue pEdge) {
    newFieldAllocation.add(SMGKnownAddress.valueOf(pEdge.getObject(), pEdge.getOffset()));
  }

  public void registerHasValueEdge(SMGObject pSourceObject, int pSourceOffset,
      SMGEdgeHasValue pEdge) {
    newFieldAllocation.add(SMGKnownAddress.valueOf(pEdge.getObject(), pEdge.getOffset()));
    hveMap.put(pEdge, SMGKnownAddress.valueOf(pSourceObject, pSourceOffset));
  }

  public void registerExplicitValue(SMGKnownSymValue pKey, SMGKnownExpValue pValue) {

    if (!pValue.containsSourceAddreses()
        || pValue.getSourceAdresses().isEmpty()) {
      return;
    }

    valueMap.putAll(pKey, pValue.getSourceAdresses());
  }

  public void registerNewObjectAllocation(SMGKnownExpValue pSize, SMGObject pResult,
      boolean isStackAndVariableTypeSize) {

    if (pSize.containsSourceAddreses()) {

      if (isStackAndVariableTypeSize && !pSize.getSourceAdresses().isEmpty()) {
        varTypeSizeDcl.add((SMGRegion) pResult);
      }

      objectMap.putAll(pResult, pSize.getSourceAdresses());
    }
  }

  public Set<SMGKnownAddress> getNewFields() {
    return newFieldAllocation;
  }

  public void clear() {
    newFieldAllocation.clear();
    hveMap.clear();
    objectMap.clear();
  }

  public static class SMGAddressAndSource extends SMGKnownAddress {

    private final Set<SMGKnownAddress> source;

    protected SMGAddressAndSource(SMGObject pObject, SMGKnownExpValue pOffset,
        Set<SMGKnownAddress> pSource) {
      super(pObject, pOffset);

      if (pOffset.containsSourceAddreses()) {
        pSource = Sets.union(pSource, pOffset.getSourceAdresses());
      }

      source = FluentIterable.from(pSource).transform(dropSource).toSet();
    }

    @Override
    public boolean containsSourceAddreses() {
      return true;
    }

    @Override
    public Set<SMGKnownAddress> getSourceAdresses() {
      return source;
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

      Set<SMGKnownAddress> nSource = source;

      if(pAddedOffset.containsSourceAddreses()) {
        nSource = union(pAddedOffset.getSourceAdresses(), source);
      }

      return valueOf(
          getObject(),
          getOffset().add(pAddedOffset).getAsInt(),
          nSource);
    }

    public static SMGAddressAndSource valueOf(SMGObject pObj, int pOffset,
        Set<SMGKnownAddress> pSource) {
      return new SMGAddressAndSource(pObj, SMGKnownExpValue.valueOf(pOffset), pSource);
    }

    private Set<SMGKnownAddress> union(Set<SMGKnownAddress> pSource1, Set<SMGKnownAddress> pSource2) {
      return Sets.union(pSource1, pSource2);
    }

    @Override
    public SMGKnownAddress getAsKnownAddress() {
      // TODO Auto-generated method stub
      return SMGAddressAndSource.valueOf(getObject(), getOffset().getAsInt(), source);
    }

    @Override
    public String toString() {
      return "SMGAddressAndSource [offset=" + getOffset()
          + ", object=" + getObject() + ", source=" + source + "]";
    }

    @Override
    public SMGKnownAddress dropSource() {
      return SMGKnownAddress.valueOf(getObject(), getOffset());
    }
  }

  public static class SMGKnownAddValueAndSource extends SMGKnownAddVal {

    private final Set<SMGKnownAddress> source;

    protected SMGKnownAddValueAndSource(BigInteger pValue, SMGAddressAndSource pAddress,
        Set<SMGKnownAddress> pSource) {
      super(pValue, pAddress);
      source = FluentIterable.from(Sets.union(pSource, pAddress.getSourceAdresses())).transform(dropSource).toSet();
    }

    @Override
    public Set<SMGKnownAddress> getSourceAdresses() {
      return source;
    }

    @Override
    public boolean containsSourceAddreses() {
      return true;
    }

    @Override
    public SMGExplicitValue deriveExplicitValueFromSymbolicValue() {
      SMGExplicitValue result = super.deriveExplicitValueFromSymbolicValue();

      if (!result.isUnknown() && containsSourceAddreses()) {
        result = SMGKnownExpValueAndSource.valueOf(result.getAsInt(), getSourceAdresses());
      }

      return result;
    }

    public static SMGKnownAddValueAndSource valueOf(BigInteger pValue, SMGAddressAndSource pAddress,
        Set<SMGKnownAddress> pSource) {
      return new SMGKnownAddValueAndSource(pValue, pAddress, pSource);
    }

    public static SMGKnownAddValueAndSource valueOf(SMGKnownSymValue pValue, SMGAddressAndSource pAddress,
        Set<SMGKnownAddress> pSource) {
      return new SMGKnownAddValueAndSource(pValue.getValue(), pAddress, pSource);
    }

    public static SMGKnownAddValueAndSource valueOf(int pValue, SMGAddressAndSource pAddress,
        Set<SMGKnownAddress> pSource) {
      return new SMGKnownAddValueAndSource(BigInteger.valueOf(pValue), pAddress, pSource);
    }

    public static SMGKnownAddValueAndSource valueOf(long pValue, SMGAddressAndSource pAddress,
        Set<SMGKnownAddress> pSource) {
      return new SMGKnownAddValueAndSource(BigInteger.valueOf(pValue), pAddress, pSource);
    }

    public static SMGKnownAddValueAndSource valueOf(int pValue, SMGObject object, int offset, Set<SMGKnownAddress> pSource) {
      return new SMGKnownAddValueAndSource(BigInteger.valueOf(pValue), SMGAddressAndSource.valueOf(object, offset, pSource), pSource);
    }

    @Override
    public String toString() {
      return "SMGKnownAddValueAndSource [value=" + getValue() + ", address="
          + getAddress() + ", source=" + source + "]";
    }
  }

  public static class SMGKnownSymValueAndSource extends SMGKnownSymValue {

    private final Set<SMGKnownAddress> source;

    protected SMGKnownSymValueAndSource(BigInteger pValue, Set<SMGKnownAddress> pSource) {
      super(pValue);
      source = FluentIterable.from(pSource).transform(dropSource).toSet();
    }

    public static SMGSymbolicValue valueOf(SMGSymbolicValue pValue, Set<SMGKnownAddress> pSource) {
      return new SMGKnownSymValueAndSource(pValue.getValue(), pSource);
    }

    @Override
    public Set<SMGKnownAddress> getSourceAdresses() {
      return source;
    }

    @Override
    public boolean containsSourceAddreses() {
      return true;
    }

    @Override
    public SMGExplicitValue deriveExplicitValueFromSymbolicValue() {
      SMGExplicitValue result = super.deriveExplicitValueFromSymbolicValue();

      if (!result.isUnknown() && containsSourceAddreses()) {
        result = SMGKnownExpValueAndSource.valueOf(result.getAsInt(), getSourceAdresses());
      }

      return result;
    }

    public static SMGKnownSymValueAndSource valueOf(BigInteger pValue, Set<SMGKnownAddress> pSource) {
      return new SMGKnownSymValueAndSource(pValue, pSource);
    }

    public static SMGKnownSymValueAndSource valueOf(int pValue) {
      return new SMGKnownSymValueAndSource(BigInteger.valueOf(pValue), ImmutableSet.of());
    }

    public static SMGKnownSymValueAndSource valueOf(int pValue, Set<SMGKnownAddress> pSource) {
      return new SMGKnownSymValueAndSource(BigInteger.valueOf(pValue), pSource);
    }

    public static SMGKnownSymValueAndSource valueOf(long pValue, Set<SMGKnownAddress> pSource) {
      return new SMGKnownSymValueAndSource(BigInteger.valueOf(pValue), pSource);
    }

    @Override
    public String toString() {
      return "SMGKnownSymValueAndSource [value=" + getValue() + ", source=" + source + "]";
    }
  }

  public static class SMGKnownExpValueAndSource extends SMGKnownExpValue {

    private final Set<SMGKnownAddress> source;

    protected SMGKnownExpValueAndSource(BigInteger pValue, Set<SMGKnownAddress> pSource) {
      super(pValue);
      source = FluentIterable.from(pSource).transform(dropSource).toSet();
    }

    @Override
    public boolean containsSourceAddreses() {
      return true;
    }

    @Override
    public Set<SMGKnownAddress> getSourceAdresses() {
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

      return valueOf(getValue().xor(pRVal.getValue()), union(pRVal));
    }

    @Override
    public SMGExplicitValue or(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().or(pRVal.getValue()), union(pRVal));
    }

    @Override
    public SMGExplicitValue and(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().and(pRVal.getValue()), union(pRVal));
    }

    @Override
    public SMGExplicitValue shiftLeft(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().shiftLeft(pRVal.getAsInt()), union(pRVal));
    }

    @Override
    public SMGExplicitValue multiply(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().multiply(pRVal.getValue()), union(pRVal));
    }

    @Override
    public SMGExplicitValue divide(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().divide(pRVal.getValue()), union(pRVal));
    }

    @Override
    public SMGExplicitValue subtract(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().subtract(pRVal.getValue()),
          union(pRVal));
    }

    @Override
    public SMGExplicitValue add(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().add(pRVal.getValue()), union(pRVal));
    }

    public static SMGKnownExpValueAndSource valueOf(BigInteger pValue, Set<SMGKnownAddress> pSource) {
      return new SMGKnownExpValueAndSource(pValue, pSource);
    }

    public static SMGKnownExpValueAndSource valueOf(int pValue) {
      return new SMGKnownExpValueAndSource(BigInteger.valueOf(pValue), ImmutableSet.of());
    }

    public static SMGKnownExpValueAndSource valueOf(int pValue, Set<SMGKnownAddress> pSource) {
      return new SMGKnownExpValueAndSource(BigInteger.valueOf(pValue), pSource);
    }

    public static SMGKnownExpValueAndSource valueOf(long pValue, Set<SMGKnownAddress> pSource) {
      return new SMGKnownExpValueAndSource(BigInteger.valueOf(pValue), pSource);
    }

    private Set<SMGKnownAddress> union(SMGExplicitValue pOtherVal) {

      if(!pOtherVal.containsSourceAddreses()) {
        return source;
      } else {
        return Sets.union(source, pOtherVal.getSourceAdresses());
      }

    }

    @Override
    public String toString() {
      return "SMGKnownExpValueAndSource [value=" + getValue() + ", source=" + source + "]";
    }
  }

  public SMGHveSources copy() {
    return new SMGHveSources(this);
  }

  public SMGSymbolicValue createReadValueSource(SMGAddress pAddress, SMGSymbolicValue pValue) {
    return createReadValueSource(pAddress.getAsKnownAddress(), pValue, ImmutableSet.of());
  }

  public void registerHasValueEdgeFromCopy(SMGObject pObject, int pOffset, SMGEdgeHasValue pNewEdge,
      SMGExplicitValue pCopyRange, SMGKnownExpValue pTargetRangeOffset) {
    Set<SMGKnownAddress> sources = new HashSet<>();
    newFieldAllocation.add(SMGKnownAddress.valueOf(pNewEdge.getObject(), pNewEdge.getOffset()));

    if (pCopyRange.containsSourceAddreses()) {
      sources.addAll(pCopyRange.getSourceAdresses());
    }

    if (pTargetRangeOffset.containsSourceAddreses()) {
      sources.addAll(pTargetRangeOffset.getSourceAdresses());
    }

    sources.add(SMGKnownAddress.valueOf(pObject, pOffset));
    hveMap.putAll(pNewEdge, sources);
  }

  public void registerMemsetCount(SMGExplicitValue pCountValue,
      SMGStateEdgePair pResultStateAndEdge) {
    if (pCountValue.containsSourceAddreses() && pResultStateAndEdge.smgStateHasNewEdge()) {
      hveMap.putAll(pResultStateAndEdge.getNewEdge(), pCountValue.getSourceAdresses());
    }
  }

  public void registerHasValueEdgeFromCopy(SMGObject pObject, int pOffset,
      SMGStateEdgePair pNewSMGStateAndEdge, SMGExplicitValue pCopyRange,
      SMGKnownExpValue pTargetRangeOffset) {

    if (pNewSMGStateAndEdge.smgStateHasNewEdge()) {
      registerHasValueEdgeFromCopy(pObject, pOffset, pNewSMGStateAndEdge.getNewEdge(), pCopyRange,
          pTargetRangeOffset);
    }
  }

  public SMGSymbolicValue createGetAddressSource(SMGSymbolicValue pResult,
      SMGKnownExpValue pOffset) {

    if (!pResult.isUnknown() && pOffset.containsSourceAddreses()) {
      return SMGKnownSymValueAndSource.valueOf(pResult, pOffset.getSourceAdresses());
    }

    return pResult;
  }

  public SMGSymbolicValue createBinaryOpValue(SMGSymbolicValue pResult, SMGSymbolicValue pV1,
      SMGSymbolicValue pV2) {

    if(pResult.isUnknown()) {
      return pResult;
    }

    Set<SMGKnownAddress> sources = new HashSet<>();

    if (pV1.containsSourceAddreses()) {
      sources.addAll(pV1.getSourceAdresses());
    }

    if (pV2.containsSourceAddreses()) {
      sources.addAll(pV2.getSourceAdresses());
    }

    return SMGKnownSymValueAndSource.valueOf(pResult, sources);
  }

  public Set<SMGRegion> getVariableTypeDclRegion() {
    return varTypeSizeDcl;
  }

  public Set<SMGKnownAddress> getSourcesOfDereferences() {
    return sourcesOfDereference;
  }

  public SMGAddressValue createPointer(SMGKnownSymValue pValue, SMGEdgePointsTo pAddressValue) {
    if (pValue.containsSourceAddreses()) {
      return SMGKnownAddValueAndSource.valueOf(pValue,
          SMGAddressAndSource.valueOf(pAddressValue.getObject(), pAddressValue.getOffset(),
              pValue.getSourceAdresses()),
          pValue.getSourceAdresses());
    } else {
      return SMGKnownAddVal.valueOf(pAddressValue.getValue(),
          pAddressValue.getObject(), pAddressValue.getOffset());
    }
  }

  public SMGExplicitValue createExpValue(SMGKnownSymValue symVal,
      SMGKnownExpValue expVal) {

    if (symVal.containsSourceAddreses()) {
      return SMGKnownExpValueAndSource.valueOf(expVal.getAsInt(), symVal.getSourceAdresses());
    } else {
      return expVal;
    }
  }

  public SMGAddressValueAndStateList createBinaryAddress(
      SMGAddressValueAndStateList pResultAddressValueAndStateList, SMGAddressValue pAddressValue, SMGExplicitValue pAddressOffset) {

    Set<SMGKnownAddress> newSource = new HashSet<>();

    if (pAddressValue.containsSourceAddreses()) {
      newSource.addAll(pAddressValue.getSourceAdresses());
    }

    if (pAddressOffset.containsSourceAddreses()) {
      newSource.addAll(pAddressOffset.getSourceAdresses());
    }

    if (newSource.isEmpty()) {
      return pResultAddressValueAndStateList;
    }

    Function<SMGAddressValueAndState, SMGAddressValueAndState> function =
        (SMGAddressValueAndState arg) -> {
          SMGAddressValue oldVal = arg.getObject();
          SMGAddress oldAddress = oldVal.getAddress();

          Set<SMGKnownAddress> newSourcesVal;
          if (oldVal.containsSourceAddreses()) {
            newSourcesVal = Sets.union(newSource, oldVal.getSourceAdresses());
          } else {
            newSourcesVal = newSource;
          }


          SMGAddressValue newVal = SMGKnownAddValueAndSource.valueOf(oldVal.getValue(),
              SMGAddressAndSource.valueOf(oldAddress.getObject(), oldAddress.getOffset().getAsInt(),
                  newSourcesVal),
              newSourcesVal);
          return SMGAddressValueAndState.of(arg.getSmgState(), newVal);
        };

    return SMGAddressValueAndStateList.copyOfAddressValueList(FluentIterable.from(pResultAddressValueAndStateList.asAddressValueAndStateList()).transform(function).toList());
  }

  public SMGAddressValueAndStateList createAddressSource(SMGAddressValueAndStateList pResult,
      SMGAddress pAddress) {

    if (!pAddress.containsSourceAddreses()) { return pResult; }

    Function<SMGAddressValueAndState, SMGAddressValueAndState> function =
        (SMGAddressValueAndState arg) -> {
          SMGAddressValue oldVal = arg.getObject();
          SMGAddress oldAddress = oldVal.getAddress();

          Set<SMGKnownAddress> newSourcesVal;
          if (oldVal.containsSourceAddreses()) {
            newSourcesVal = Sets.union(pAddress.getSourceAdresses(), oldVal.getSourceAdresses());
          } else {
            newSourcesVal = pAddress.getSourceAdresses();
          }


          SMGAddressValue newVal = SMGKnownAddValueAndSource.valueOf(oldVal.getValue(),
              SMGAddressAndSource.valueOf(oldAddress.getObject(), oldAddress.getOffset().getAsInt(),
                  newSourcesVal),
              newSourcesVal);
          return SMGAddressValueAndState.of(arg.getSmgState(), newVal);
        };

    return SMGAddressValueAndStateList.copyOfAddressValueList(
        FluentIterable.from(pResult.asAddressValueAndStateList()).transform(function).toList());
  }
}
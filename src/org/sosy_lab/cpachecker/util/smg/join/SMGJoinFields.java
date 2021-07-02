// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains utility classes for program slicing.
 *
 * @see org.sosy_lab.cpachecker.util.dependencegraph
 */
package org.sosy_lab.cpachecker.util.smg.join;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGExplicitValue;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSymbolicValue;

/**
 * Class implementing join algorithm from FIT-TR-2013-4 (Appendix C)
 */
public class SMGJoinFields {

  private SMG smg1;
  private SMG smg2;

  private SMGJoinStatus status = SMGJoinStatus.EQUAL;

  public SMGJoinFields(SMG pSmg1, SMG pSmg2) {
    smg1 = pSmg1;
    smg2 = pSmg2;
  }

  /**
   * Implementation of Algorithm 3.
   *
   * @param obj1 - SMGObject of smg1
   * @param obj2 - SMGObject of smg2
   */
  public void joinFields(SMGObject obj1, SMGObject obj2) {
    Preconditions.checkArgument(
        obj1.getSize().equals(obj2.getSize()),
        "SMG fields with different sizes cannot be joined.");
    Preconditions.checkArgument(
        smg1.getObjects().contains(obj1) && smg2.getObjects().contains(obj2),
        "Only objects of givens SMGs can be joined.");

    // Step 2
    PersistentSet<SMGHasValueEdge> obj1Edges = processHasValueEdgeSet(obj1, obj2, smg1, smg2);
    PersistentSet<SMGHasValueEdge> obj2Edges = processHasValueEdgeSet(obj2, obj1, smg2, smg1);

    SMG newSMG1 = smg1.copyAndSetHVEdges(obj1Edges, obj1);
    SMG newSMG2 = smg2.copyAndSetHVEdges(obj2Edges, obj2);
    // Step 4
    updateStatus(smg1, newSMG1, SMGJoinStatus.LEFT_ENTAIL, obj1);
    updateStatus(smg2, newSMG2, SMGJoinStatus.RIGHT_ENTAIL, obj2);

    // Step 5
    PersistentSet<SMGHasValueEdge> addObj1Edges = mergeNonNullValues(newSMG2, newSMG1, obj2, obj1);
    PersistentSet<SMGHasValueEdge> addObj2Edges = mergeNonNullValues(newSMG1, newSMG2, obj1, obj2);

    for (SMGHasValueEdge edge : addObj1Edges) {
      newSMG1 = newSMG1.copyAndAddValue(edge.hasValue());
      newSMG1 = newSMG1.copyAndAddHVEdge(edge, obj1);
    }
    for (SMGHasValueEdge edge : addObj2Edges) {
      newSMG2 = newSMG2.copyAndAddValue(edge.hasValue());
      newSMG2 = newSMG2.copyAndAddHVEdge(edge, obj2);
    }

    smg1 = newSMG1;
    smg2 = newSMG2;
  }

  /**
   * Implementation of Algorithm 3 step 5.
   *
   * @param pSMG1 - the smg with defined edges
   * @param pSMG2 - the smg with undefined edges
   * @param pObject1 - the object with defined edges
   * @param pObject2 - the object with defined edges
   * @return extension edge set for pSMG2
   */
  private PersistentSet<SMGHasValueEdge>
      mergeNonNullValues(SMG pSMG1, SMG pSMG2, SMGObject pObject1, SMGObject pObject2) {
    Set<SMGHasValueEdge> retSet = new HashSet<>();
    Set<SMGHasValueEdge> obj1EdgesWOZero =
        pSMG1.getEdges(pObject1)
            .stream()
            .filter(createEqNullValuePredicate(false))
            .collect(Collectors.toSet());
    TreeMap<BigInteger, SMGHasValueEdge> obj2OffsetToEdges =
        pSMG2.getEdges(pObject2).stream().collect(mapOffsetToEdgeCollector());

    obj1EdgesWOZero.stream()
        .filter(
            edge -> {
              // filter overlapping edges
              return !obj2OffsetToEdges
                  .subMap(edge.getOffset(), edge.getOffset().add(edge.getSizeInBits()))
                  .isEmpty();
            })
        .forEach(
            edge -> {
              // add fresh edges for offset and size tuples, which are defined in o1 and undefined
              // in o2
              retSet.add(
                  new SMGHasValueEdge(
                      SMGSymbolicValue.of(edge.hasValue().getNestingLevel()),
                      edge.getSizeInBits(),
                      edge.getOffset()));
            });

    return PersistentSet.copyOf(retSet);
  }

  /**
   * Implementation of Algorithm 3 step 4.
   *
   * @param oldSmg - the SMG before the join
   * @param newSmg - the SMG after the join
   * @param pNewStatus - Status update to applied maybe
   * @param object - the joined object
   */
  private void updateStatus(SMG oldSmg, SMG newSmg, SMGJoinStatus pNewStatus, SMGObject object) {
    // filter for null edges and map offsets on sizes
    TreeMap<BigInteger, BigInteger> oldEdgesWithZeroOffsetToSize =
        getNullEdgesMapOffsetToSize(object, oldSmg);
    TreeMap<BigInteger, BigInteger> newEdgesWithZeroOffsetToSize =
        getNullEdgesMapOffsetToSize(object, newSmg);

    boolean applyUpdate =
        oldEdgesWithZeroOffsetToSize.entrySet()
            .stream()
            .anyMatch(
                entry -> {
                  // find entry with equal offset
                  BigInteger newSize = newEdgesWithZeroOffsetToSize.get(entry.getKey());
                  // if newSize == null the offset was shortened
                  // if !newSize.equals(entry.getValue()) the length was shortened
                  return newSize == null || !newSize.equals(entry.getValue());
                });
    if (applyUpdate) {
      status = status.updateWith(pNewStatus);
    }
  }

  /**
   * Implementation of Algorithm 3 step 2.
   *
   * @param obj1 - SMGObject of smg1
   * @param obj2 - SMGObject of smg2
   */
  private PersistentSet<SMGHasValueEdge>
      processHasValueEdgeSet(SMGObject obj1, SMGObject obj2, SMG pSmg1, SMG pSmg2) {
    // H1 and H2
    Set<SMGHasValueEdge> obj1Edges = pSmg1.getEdges(obj1);
    Set<SMGHasValueEdge> obj2Edges = pSmg2.getEdges(obj2);

    // 2a)
    Set<SMGHasValueEdge> edgesObj1Without0Address =
        obj1Edges.stream().filter(createEqNullValuePredicate(false)).collect(Collectors.toSet());

    // 2b)
    TreeMap<BigInteger, BigInteger> obj1EdgesWithZeroOffsetToSize =
        getNullEdgesMapOffsetToSize(obj1, pSmg1);
    TreeMap<BigInteger, BigInteger> map2 = getNullEdgesMapOffsetToSize(obj2, pSmg2);
    Set<SMGHasValueEdge> commonNullValueEdgeSet = new LinkedHashSet<>();

    obj1EdgesWithZeroOffsetToSize.entrySet().stream().forEach(entry -> {
      commonNullValueEdgeSet.addAll(getNullEdgesIntersection(entry, map2));
    });

    // 2c)
    Set<SMGHasValueEdge> obj2EdgesWithoutZero =
        obj2Edges.stream().filter(createEqNullValuePredicate(false)).collect(Collectors.toSet());

    Set<SMGHasValueEdge> missingNullEdgeSet = new LinkedHashSet<>();
    obj2EdgesWithoutZero.forEach(edge -> {
      // find offset wise matching edges
      BigInteger floor = edge.getOffset();
      BigInteger celing = edge.getOffset().add(edge.getSizeInBits());
      Entry<BigInteger, BigInteger> entry = obj1EdgesWithZeroOffsetToSize.floorEntry(floor);
      if (entry != null && entry.getKey().add(entry.getValue()).compareTo(celing) >= 0) {
        missingNullEdgeSet.add(
            new SMGHasValueEdge(
                SMGExplicitValue.nullInstance(),
                edge.getSizeInBits(),
                edge.getOffset()));
      }
    });

    edgesObj1Without0Address.addAll(commonNullValueEdgeSet);
    edgesObj1Without0Address.addAll(missingNullEdgeSet);

    return PersistentSet.copyOf(edgesObj1Without0Address);
  }

  private Collection<SMGHasValueEdge> getNullEdgesIntersection(
      Entry<BigInteger, BigInteger> pEntry,
      TreeMap<BigInteger, BigInteger> pMap) {
    return pMap.subMap(pEntry.getKey(), pEntry.getKey().add(pEntry.getValue()))
        .entrySet()
        .stream()
        .map(next -> {
          BigInteger resultOffset = pEntry.getKey().max(next.getKey());
          BigInteger resultSize = pEntry.getValue().max(next.getValue());
          return new SMGHasValueEdge(SMGExplicitValue.nullInstance(), resultSize, resultOffset);
        })
        .collect(Collectors.toSet());
  }

  private TreeMap<BigInteger, BigInteger> getNullEdgesMapOffsetToSize(SMGObject pObj, SMG pSMG) {
    return pSMG.getEdges(pObj)
        .stream()
        .filter(createEqNullValuePredicate(true))
        .collect(mapOffsetToSizeCollector());

  }

  private Predicate<SMGHasValueEdge> createEqNullValuePredicate(boolean equals) {
    return edge -> equals == edge.hasValue().equals(SMGExplicitValue.nullInstance());
  }

  private Collector<SMGHasValueEdge, ?, TreeMap<BigInteger, BigInteger>>
      mapOffsetToSizeCollector() {
    return Collectors.toMap(
        SMGHasValueEdge::getOffset,
        SMGHasValueEdge::getSizeInBits,
        (e, n) -> e,
        TreeMap::new);
  }

  private Collector<SMGHasValueEdge, ?, TreeMap<BigInteger, SMGHasValueEdge>>
      mapOffsetToEdgeCollector() {
    return Collectors.toMap(SMGHasValueEdge::getOffset, e -> e, (e, n) -> e, TreeMap::new);
  }

  public SMG getSmg1() {
    return smg1;
  }

  public SMG getSmg2() {
    return smg2;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

}

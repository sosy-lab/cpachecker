// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import static org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType.DLL;
import static org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType.REG;
import static org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType.SLL;
import static org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType.ZERO;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

@SuppressWarnings("unused")
public class ListDebugger {

  private final int size;

  private final int nfo;
  private final int nextPtrTargetOffset;

  private final Optional<Integer> pfo;
  private final Optional<Integer> prevPtrTargetOffset;

  private Optional<Integer> nestedOffset = Optional.empty();

  private Optional<ListDebugger> nestedListShape = Optional.empty();

  private final Set<List<ListElement>> alreadyFound = new HashSet<>();

  private final LogManagerWithoutDuplicates logger;

  // TODO: lists with a distinct first item
  public ListDebugger(
      int pSize, int pNfo, int pNextPtrTargetOffset, LogManagerWithoutDuplicates pLogger) {
    size = pSize;
    nfo = pNfo;
    nextPtrTargetOffset = pNextPtrTargetOffset;
    pfo = Optional.empty();
    prevPtrTargetOffset = Optional.empty();
    logger = pLogger;
  }

  public ListDebugger(
      int pSize,
      int pNfo,
      int pNextPtrTargetOffset,
      int pPfo,
      int pPrevPtrTargetOffset,
      LogManagerWithoutDuplicates pLogger) {
    size = pSize;
    nfo = pNfo;
    nextPtrTargetOffset = pNextPtrTargetOffset;
    pfo = Optional.of(pPfo);
    prevPtrTargetOffset = Optional.of(pPrevPtrTargetOffset);
    logger = pLogger;
  }

  /**
   * Adds a nested list below either the deepest nested list or the top list of there is no nested
   */
  public ListDebugger withNestedList(
      int offsetTowardsNested, int pSize, int pNfo, int pNextPtrTargetOffset) {
    this.nestedOffset = Optional.of(offsetTowardsNested);
    if (nestedListShape.isEmpty()) {
      nestedListShape = Optional.of(new ListDebugger(pSize, pNfo, pNextPtrTargetOffset, logger));
    } else {
      nestedListShape
          .orElseThrow()
          .withNestedList(offsetTowardsNested, pSize, pNfo, pNextPtrTargetOffset);
    }
    return this;
  }

  /**
   * Adds a nested list below either the deepest nested list or the top list of there is no nested
   */
  public ListDebugger withNestedList(
      int offsetTowardsNested,
      int pSize,
      int pNfo,
      int pNextPtrTargetOffset,
      int pPfo,
      int pPrevPtrTargetOffset) {
    this.nestedOffset = Optional.of(offsetTowardsNested);
    if (nestedListShape.isEmpty()) {
      nestedListShape =
          Optional.of(
              new ListDebugger(
                  pSize, pNfo, pNextPtrTargetOffset, pPfo, pPrevPtrTargetOffset, logger));
    } else {
      nestedListShape
          .orElseThrow()
          .withNestedList(
              offsetTowardsNested, pSize, pNfo, pNextPtrTargetOffset, pPfo, pPrevPtrTargetOffset);
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  public void printListInfo(int printDistance) {
    if (alreadyFound.size() % printDistance == 0) {
      logger.log(Level.INFO, "Already found list elements: " + alreadyFound.size());
      for (List<ListElement> list : alreadyFound) {
        logger.log(Level.INFO, printList(list));
      }
    }
  }

  private String printList(List<ListElement> list) {
    String listString = "\n";
    String[][] doubleNestedListStrings = new String[list.size()][];
    int largestNested = 0;

    // TODO: ZERO might not be in the memoryListItems list, and other values are definitely not
    for (int i = 0; i < list.size(); i++) {
      ListElement elem = list.get(i);
      doubleNestedListStrings[i] = new String[1];
      if (!elem.listItems.isEmpty() && this.nestedOffset.isPresent()) {
        List<ListElement> maybeNested = elem.memorylistItems.get(this.nestedOffset.orElseThrow());
        if (maybeNested != null) {
          ImmutableList<ListElement> nestedList = (ImmutableList<ListElement>) maybeNested;
          doubleNestedListStrings[i] = new String[nestedList.size() + 1];
          for (int j = 0; j < nestedList.size(); j++) {
            if (i != list.size() - 1) {
              doubleNestedListStrings[i][j + 1] = nestedList.get(j).toString() + "  ";
            } else {
              doubleNestedListStrings[i][j + 1] = nestedList.get(j).toString();
            }
          }
          largestNested = Integer.max(largestNested, nestedList.size() + 1);
        }
      }
      if (i != list.size() - 1) {
        doubleNestedListStrings[i][0] = elem + "->";
      } else {
        doubleNestedListStrings[i][0] = elem.toString();
      }
    }
    for (int nested = 0; nested < largestNested; nested++) {
      for (int i = 0; i < doubleNestedListStrings.length; i++) {
        if (doubleNestedListStrings[i].length > nested
            && doubleNestedListStrings[i][nested] != null) {
          listString += doubleNestedListStrings[i][nested];
        } else {
          listString += "       ";
        }
        if (i + 1 == doubleNestedListStrings.length) {
          listString += "\n";
        }
      }
    }
    return listString + "\n";
  }

  public List<ListElement> buildListFromObjectWithPtrToList(
      SMGObject objWPtrToList, int readOffsetForPtr, SMGState state) {
    SMG smg = state.getMemoryModel().getSmg();
    List<SMGHasValueEdge> edges =
        smg.readValue(
                objWPtrToList, BigInteger.valueOf(readOffsetForPtr), smg.getSizeOfPointer(), false)
            .getHvEdges();
    if (edges.size() != 1
        || !smg.isPointer(edges.get(0).hasValue())
        || edges.get(0).hasValue().isZero()) {
      // No List yet
      return ImmutableList.of();
    }

    SMGPointsToEdge pteList = smg.getPTEdge(edges.get(0).hasValue()).orElseThrow();
    if (!pteList.getOffset().isNumericValue()
        || pteList.getOffset().asNumericValue().bigIntegerValue().intValueExact() != 0) {
      throw new RuntimeException(
          "Not yet implemented debug case for offset pointer towards list from outside");
    }
    SMGObject listElem = pteList.pointsTo();
    List<ListElement> listImage = addFirstThenRest(listElem, state);
    if (alreadyFound.contains(listImage)) {
      printListInfo(1);
      logger.log(Level.WARNING, "Duplicate found: " + printList(listImage));
      throw new RuntimeException("Already found list element.");
    }
    alreadyFound.add(listImage);
    return listImage;
  }

  private List<ListElement> addFirstThenRest(SMGObject listElem, SMGState state) {
    if (listElem.isZero()) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<ListElement> builder = ImmutableList.builder();
    SMG smg = state.getMemoryModel().getSmg();
    Preconditions.checkArgument(
        size == listElem.getSize().asNumericValue().bigIntegerValue().intValue());
    ListType listType = REG;
    Optional<Integer> abstractedMinLength = Optional.empty();
    if (listElem instanceof SMGSinglyLinkedListSegment sll) {
      abstractedMinLength = Optional.of(sll.getMinLength());
      Preconditions.checkArgument(sll.getNextOffset().intValue() == nfo);
      Preconditions.checkArgument(
          sll.getNextPointerTargetOffset().intValue() == nextPtrTargetOffset);
      listType = SLL;
      if (listElem instanceof SMGDoublyLinkedListSegment dll) {
        Preconditions.checkArgument(pfo.isPresent());
        listType = DLL;
        Preconditions.checkArgument(dll.getPrevOffset().intValue() == pfo.orElseThrow());
        Preconditions.checkArgument(
            dll.getPrevPointerTargetOffset().intValue() == prevPtrTargetOffset.orElseThrow());
      }
    }
    // Get other things in the list
    FluentIterable<SMGHasValueEdge> hvesWithoutNextAndPrev =
        smg.getHasValueEdgesByPredicate(
            listElem,
            h ->
                h.getOffset().intValue() != nfo
                    && (pfo.isEmpty() || h.getOffset().intValue() != pfo.orElseThrow()));

    Map<Integer, Object> listItems = new HashMap<>();
    Map<Integer, List<ListElement>> memoryListItems = new HashMap<>();

    for (SMGHasValueEdge hve : hvesWithoutNextAndPrev) {
      SMGValue val = hve.hasValue();
      if (smg.isPointer(hve.hasValue()) && !val.isZero()) {
        // Either a nested list, or some payload/ptr to payload
        SMGPointsToEdge pteNested = smg.getPTEdge(hve.hasValue()).orElseThrow();
        SMGObject nestedObj = pteNested.pointsTo();
        if (nestedListShape.isPresent()) {
          if (nestedObj.getSize().isNumericValue()
              && nestedObj.getSize().asNumericValue().bigIntegerValue().intValue()
                  == nestedListShape.orElseThrow().size) {
            List<ListElement> nestedList =
                nestedListShape.orElseThrow().addFirstThenRest(nestedObj, state);
            memoryListItems.put(hve.getOffset().intValue(), nestedList);
          } else {
            // Unlikely that there is a nested list AND another object
            throw new RuntimeException();
          }
        } else {
          // e.g. shared memory
          throw new RuntimeException("Implement me");
        }

      } else {
        // Numeric or symbolic value
        Optional<Value> maybeValue = state.getMemoryModel().getValueFromSMGValue(val);
        if (maybeValue.isPresent()) {
          if (maybeValue.orElseThrow().isNumericValue()) {
            listItems.put(
                hve.getOffset().intValue(),
                maybeValue.orElseThrow().asNumericValue().bigIntegerValue().intValueExact());
          } else {
            // TODO: Save constraints for this?
            listItems.put(hve.getOffset().intValue(), maybeValue.orElseThrow());
          }
        } else {
          // ?
          throw new RuntimeException();
        }
      }
    }

    // For now, we allow only the root of the list,
    //   so we check left and remember the first left ever for DLLs (to check that it's a loop)
    Optional<SMGObject> prevOfRoot = Optional.empty();
    if (pfo.isPresent()) {
      List<SMGHasValueEdge> prevEdges =
          smg.readValue(
                  listElem, BigInteger.valueOf(pfo.orElseThrow()), smg.getSizeOfPointer(), false)
              .getHvEdges();

      if (prevEdges.size() != 1 || !smg.isPointer(prevEdges.get(0).hasValue())) {
        // Wierd
        throw new RuntimeException();
      }

      if (prevEdges.get(0).hasValue().isZero()) {
        // End of list left
        builder.add(
            new ListElement(
                Optional.empty(),
                ZERO,
                0,
                nfo,
                nextPtrTargetOffset,
                pfo,
                prevPtrTargetOffset,
                ImmutableMap.of(),
                ImmutableMap.of()));

      } else {
        SMGPointsToEdge ptePrev = smg.getPTEdge(prevEdges.get(0).hasValue()).orElseThrow();
        Preconditions.checkArgument(
            ptePrev.getOffset().isNumericValue()
                && ptePrev.getOffset().asNumericValue().bigIntegerValue().intValue()
                    == prevPtrTargetOffset.orElseThrow());
        prevOfRoot = Optional.of(ptePrev.pointsTo());
        throw new RuntimeException("implement me");
      }
    }

    // Build this list element
    ListElement elem =
        new ListElement(
            abstractedMinLength,
            listType,
            size,
            nfo,
            nextPtrTargetOffset,
            pfo,
            prevPtrTargetOffset,
            ImmutableMap.<Integer, Object>builder().putAll(listItems).buildOrThrow(),
            ImmutableMap.<Integer, List<ListElement>>builder()
                .putAll(memoryListItems)
                .buildOrThrow());

    builder.add(elem);

    // Get next
    List<SMGHasValueEdge> nextEdges =
        smg.readValue(listElem, BigInteger.valueOf(nfo), smg.getSizeOfPointer(), false)
            .getHvEdges();

    if (nextEdges.size() != 1 || !smg.isPointer(nextEdges.get(0).hasValue())) {
      // Wierd
      throw new RuntimeException();
    }

    if (nextEdges.get(0).hasValue().isZero()) {
      // End of list
      builder.add(
          new ListElement(
              Optional.empty(),
              ZERO,
              0,
              nfo,
              nextPtrTargetOffset,
              pfo,
              prevPtrTargetOffset,
              ImmutableMap.of(),
              ImmutableMap.of()));

      return builder.build();

    } else {
      SMGPointsToEdge pteNext = smg.getPTEdge(nextEdges.get(0).hasValue()).orElseThrow();
      return addNext(pteNext.pointsTo(), listElem, listElem, builder, state);
    }
  }

  private List<ListElement> addNext(
      SMGObject currentObj,
      SMGObject previousObj,
      SMGObject pRoot,
      ImmutableList.Builder<ListElement> pBuilder,
      SMGState state) {
    SMG smg = state.getMemoryModel().getSmg();
    if (pRoot.equals(currentObj)) {
      // Already added
      return pBuilder.build();
    }
    Preconditions.checkArgument(
        size == currentObj.getSize().asNumericValue().bigIntegerValue().intValue());
    ListType listType = REG;
    Optional<Integer> abstractedMinLength = Optional.empty();
    if (currentObj instanceof SMGSinglyLinkedListSegment sll) {
      abstractedMinLength = Optional.of(sll.getMinLength());
      Preconditions.checkArgument(sll.getNextOffset().intValue() == nfo);
      Preconditions.checkArgument(
          sll.getNextPointerTargetOffset().intValue() == nextPtrTargetOffset);
      listType = SLL;
      if (currentObj instanceof SMGDoublyLinkedListSegment dll) {
        Preconditions.checkArgument(pfo.isPresent());
        listType = DLL;
        Preconditions.checkArgument(dll.getPrevOffset().intValue() == pfo.orElseThrow());
        Preconditions.checkArgument(
            dll.getPrevPointerTargetOffset().intValue() == prevPtrTargetOffset.orElseThrow());
      }
    }
    // Get other things in the list
    FluentIterable<SMGHasValueEdge> hvesWithoutNextAndPrev =
        smg.getHasValueEdgesByPredicate(
            currentObj,
            h ->
                h.getOffset().intValue() != nfo
                    && (pfo.isEmpty() || h.getOffset().intValue() != pfo.orElseThrow()));

    Map<Integer, Object> listItems = new HashMap<>();
    Map<Integer, List<ListElement>> memoryListItems = new HashMap<>();

    for (SMGHasValueEdge hve : hvesWithoutNextAndPrev) {
      SMGValue val = hve.hasValue();
      if (smg.isPointer(hve.hasValue()) && !val.isZero()) {
        // Either a nested list, or some payload/ptr to payload
        SMGPointsToEdge pteNested = smg.getPTEdge(hve.hasValue()).orElseThrow();
        SMGObject nestedObj = pteNested.pointsTo();
        if (nestedListShape.isPresent()) {
          if (nestedObj.getSize().isNumericValue()
              && nestedObj.getSize().asNumericValue().bigIntegerValue().intValue()
                  == nestedListShape.orElseThrow().size) {
            List<ListElement> nestedList =
                nestedListShape.orElseThrow().addFirstThenRest(nestedObj, state);
            memoryListItems.put(hve.getOffset().intValue(), nestedList);
          } else {
            // Unlikely that there is a nested list AND another object
            throw new RuntimeException();
          }
        } else {
          // e.g. shared memory
          throw new RuntimeException("Implement me");
        }

      } else {
        // Numeric or symbolic value
        Optional<Value> maybeValue = state.getMemoryModel().getValueFromSMGValue(val);
        if (maybeValue.isPresent()) {
          if (maybeValue.orElseThrow().isNumericValue()) {
            listItems.put(
                hve.getOffset().intValue(),
                maybeValue.orElseThrow().asNumericValue().bigIntegerValue().intValueExact());
          } else {
            // TODO: Save constraints for this?
            listItems.put(hve.getOffset().intValue(), maybeValue.orElseThrow());
          }
        } else {
          // ?
          throw new RuntimeException();
        }
      }
    }

    // For now, we allow only the root of the list,
    //   so we check left and remember the first left ever for DLLs (to check that it's a loop)
    Optional<SMGObject> prevOfRoot = Optional.empty();
    if (pfo.isPresent()) {
      List<SMGHasValueEdge> prevEdges =
          smg.readValue(
                  currentObj, BigInteger.valueOf(pfo.orElseThrow()), smg.getSizeOfPointer(), false)
              .getHvEdges();

      if (prevEdges.size() != 1 || !smg.isPointer(prevEdges.get(0).hasValue())) {
        // Wierd
        throw new RuntimeException();
      }

      SMGPointsToEdge ptePrev = smg.getPTEdge(prevEdges.get(0).hasValue()).orElseThrow();
      Preconditions.checkArgument(
          ptePrev.getOffset().isNumericValue()
              && ptePrev.getOffset().asNumericValue().bigIntegerValue().intValue()
                  == prevPtrTargetOffset.orElseThrow());
      Preconditions.checkArgument(ptePrev.pointsTo().equals(previousObj));
    }

    // Build this list element
    ListElement elem =
        new ListElement(
            abstractedMinLength,
            listType,
            size,
            nfo,
            nextPtrTargetOffset,
            pfo,
            prevPtrTargetOffset,
            ImmutableMap.<Integer, Object>builder().putAll(listItems).buildOrThrow(),
            ImmutableMap.<Integer, List<ListElement>>builder()
                .putAll(memoryListItems)
                .buildOrThrow());

    pBuilder.add(elem);

    // Get next
    List<SMGHasValueEdge> nextEdges =
        smg.readValue(currentObj, BigInteger.valueOf(nfo), smg.getSizeOfPointer(), false)
            .getHvEdges();

    if (nextEdges.size() != 1 || !smg.isPointer(nextEdges.get(0).hasValue())) {
      // Wierd
      throw new RuntimeException();
    }

    if (nextEdges.get(0).hasValue().isZero()) {
      // End of list
      pBuilder.add(
          new ListElement(
              Optional.empty(),
              ZERO,
              0,
              nfo,
              nextPtrTargetOffset,
              pfo,
              prevPtrTargetOffset,
              ImmutableMap.of(),
              ImmutableMap.of()));

      return pBuilder.build();

    } else {
      SMGPointsToEdge pteNext = smg.getPTEdge(nextEdges.get(0).hasValue()).orElseThrow();
      if (currentObj.equals(pRoot)) {
        // End through looping
        return pBuilder.build();
      }
      return addNext(pteNext.pointsTo(), currentObj, pRoot, pBuilder, state);
    }
  }

  public static class ListElement {

    enum ListType {
      REG,
      SLL,
      DLL,
      ZERO
    }

    private final ListType listType;
    private final Optional<Integer> abstractedMinLength;

    private final int size;

    private final int nfo;
    private final int nextPtrTargetOffset;

    private final Optional<Integer> pfo;
    private final Optional<Integer> prevPtrTargetOffset;

    // list items. E.g. values/payload excluding nfo/pfo ptrs
    private final Map<Integer, Object> listItems;

    // list items. E.g. pointers/nested lists, excluding nfo/pfo ptrs
    private final Map<Integer, List<ListElement>> memorylistItems;

    public ListElement(
        Optional<Integer> pAbstractedMinLength,
        ListType pListType,
        int pSize,
        int pNfo,
        int pNextPtrTargetOffset,
        Optional<Integer> pPfo,
        Optional<Integer> pPrevPtrTargetOffset,
        Map<Integer, Object> pListItems,
        Map<Integer, List<ListElement>> pMemorylistItems) {
      listType = pListType;
      size = pSize;
      nfo = pNfo;
      nextPtrTargetOffset = pNextPtrTargetOffset;
      pfo = pPfo;
      prevPtrTargetOffset = pPrevPtrTargetOffset;
      abstractedMinLength = pAbstractedMinLength;
      listItems = pListItems;
      memorylistItems = pMemorylistItems;
    }

    @Override
    public String toString() {
      return abstractedMinLength.isPresent()
          ? abstractedMinLength.orElseThrow() + "+" + listType
          : " " + (listType == ZERO ? listType : listType + " ");
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          listType,
          size,
          nfo,
          nextPtrTargetOffset,
          pfo,
          prevPtrTargetOffset,
          abstractedMinLength,
          listItems,
          memorylistItems);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ListElement other) {
        if (this == other) {
          return true;
        }
        return listType.equals(other.listType)
            && size == other.size
            && nfo == other.nfo
            && nextPtrTargetOffset == other.nextPtrTargetOffset
            && pfo.equals(other.pfo)
            && prevPtrTargetOffset.equals(other.prevPtrTargetOffset)
            && abstractedMinLength.equals(other.abstractedMinLength)
            && listItems.equals(other.listItems)
            && memorylistItems.equals(other.memorylistItems);
      }
      return false;
    }
  }
}

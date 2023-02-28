// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState.EqualityCache;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGCPAAbstractionManager {

  private SMGState state;

  private EqualityCache<Value> equalityCache;

  private int minimumLengthForListsForAbstraction;

  public SMGCPAAbstractionManager(SMGState pState, int pMinimumLengthForListsForAbstraction) {
    state = pState;
    minimumLengthForListsForAbstraction = pMinimumLengthForListsForAbstraction;
  }

  public SMGState findAndAbstractLists() throws SMGException {
    SMGState currentState = state;
    equalityCache = EqualityCache.of();
    ImmutableList<SMGCandidate> refinedCandidates = getRefinedLinkedCandidates();
    // Sort in DLL and SLL candidates and also order by nesting
    List<Set<SMGCandidate>> orderListCandidatesByNesting =
        orderListCandidatesByNestingAndListType(refinedCandidates);

    // Abstract top level nesting first
    for (Set<SMGCandidate> candidates : orderListCandidatesByNesting) {
      for (SMGCandidate candidate : candidates) {
        // Not valid means kicked out by abstraction
        if (!currentState.getMemoryModel().isObjectValid(candidate.getObject())) {
          continue;
        }
        if (candidate.isDLL()) {
          currentState =
              currentState.abstractIntoDLL(
                  candidate.getObject(),
                  candidate.getSuspectedNfo(),
                  candidate.getSuspectedPfo().orElseThrow(),
                  ImmutableSet.of());

        } else {
          currentState =
              currentState.abstractIntoSLL(
                  candidate.getObject(), candidate.getSuspectedNfo(), ImmutableSet.of());
        }
      }
    }
    return currentState;
  }

  /*
   * Takes refined candidates (length matches, and they are at least SLLs with their nfo pointers).
   * This checks if they are DLLs and changes the SMGCandidates to such and then checks that their
   * values match for at least the threshold length and orders the candidates according to their
   * nesting, such that a nested list nested in a top list is index + 1 of the top list.
   */
  private List<Set<SMGCandidate>> orderListCandidatesByNestingAndListType(
      ImmutableList<SMGCandidate> refinedCandidates) {

    // After findCandidatesForDLLAndSLL we know who is an SLL and a DLL, we now traverse all
    // candidates and check that their
    // values match at least for the threshold and while doing that we report the nesting of other
    // found lists, e.g. a list that a local variables pointers to is top level, while a list that
    // is pointed to by a pointer in a top level list is level 1.
    Set<SMGCandidate> trueListStarts =
        findCandidatesForDLLAndSLL(refinedCandidates).stream()
            .filter(
                candidate ->
                    candidateHasEqualValuesForAtLeastLength(
                        candidate.getObject(),
                        candidate.getObject(),
                        candidate.getSuspectedNfo(),
                        candidate.getSuspectedPfo(),
                        new HashSet<>(),
                        0))
            .collect(ImmutableSet.toImmutableSet());

    return findNestingOfCandidates(trueListStarts);
  }

  /*
   * Finds the PFO offset for DLLs and changes the SMGCandidates such that they reflect it if they
   * are DLLs, else no changes are made to the candidates.
   */
  private Set<SMGCandidate> findCandidatesForDLLAndSLL(
      ImmutableList<SMGCandidate> refinedCandidates) {

    Set<SMGCandidate> noNestingCandidates = new HashSet<>();
    for (SMGCandidate candidate : refinedCandidates) {
      Optional<BigInteger> maybePFO = isDLL(candidate, state.getMemoryModel().getSmg());
      if (maybePFO.isPresent()) {
        noNestingCandidates.add(SMGCandidate.withPfo(maybePFO.orElseThrow(), candidate));
      } else {
        noNestingCandidates.add(candidate);
      }
    }

    return noNestingCandidates;
  }

  private List<Set<SMGCandidate>> findNestingOfCandidates(Set<SMGCandidate> trueListStarts) {
    Map<SMGCandidate, Integer> nestingMap = new HashMap<>();
    List<Set<SMGCandidate>> currentOrdering = new ArrayList<>();
    Map<SMGObject, SMGCandidate> objToCandidateMap = new HashMap<>();

    for (SMGCandidate candidate : trueListStarts) {
      nestingMap.put(candidate, 0);
      objToCandidateMap.put(candidate.getObject(), candidate);
      for (SMGObject candidateListObj : candidate.getMaximalListElements()) {
        objToCandidateMap.put(candidateListObj, candidate);
      }
    }

    for (SMGCandidate candidate : trueListStarts) {
      findNestingOfCandidates(
          candidate,
          candidate.getObject(),
          candidate.getSuspectedNfo(),
          candidate.getSuspectedPfo(),
          new HashSet<>(),
          nestingMap,
          objToCandidateMap);
    }

    for (Entry<SMGCandidate, Integer> objToNestingLevel : nestingMap.entrySet()) {
      SMGCandidate obj = objToNestingLevel.getKey();
      int nesting = objToNestingLevel.getValue();
      while (currentOrdering.size() <= nesting) {
        currentOrdering.add(new HashSet<>());
      }
      // Add to correct
      currentOrdering.get(nesting).add(obj);
    }
    return currentOrdering;
  }

  // TODO: do this also for non-candidates in between candidates
  private void findNestingOfCandidates(
      SMGCandidate start,
      SMGObject currentObj,
      BigInteger nfo,
      Optional<BigInteger> maybePfo,
      Set<SMGObject> alreadySeen,
      Map<SMGCandidate, Integer> nestingMap,
      Map<SMGObject, SMGCandidate> objToCandidateMap) {
    SMG smg = state.getMemoryModel().getSmg();

    SMGPointsToEdge pointsToNext = null;
    // Get next pointer (read would do the same thing...., this way we don't care about sizes)
    for (SMGHasValueEdge hve : smg.getEdges(currentObj)) {
      SMGValue value = hve.hasValue();

      if (smg.isPointer(value)) {
        if (hve.getOffset().compareTo(nfo) == 0) {
          pointsToNext = smg.getPTEdge(value).orElseThrow();
          alreadySeen.add(currentObj);
        } else if (maybePfo.isPresent() && hve.getOffset().compareTo(maybePfo.orElseThrow()) == 0) {
          // Ignore prev pointers
        } else {
          // Points somewhere else, possibly nested list
          SMGPointsToEdge pointsToOther = smg.getPTEdge(value).orElseThrow();

          // This does take into account pointers pointing to nested lists that are not to the
          // root node or some structure that leads to a nested list below
          if (objToCandidateMap.containsKey(pointsToOther.pointsTo())) {
            SMGCandidate candidateForObj = objToCandidateMap.get(pointsToOther.pointsTo());
            if (nestingMap.containsKey(candidateForObj)) {
              // Nesting level of the source of the pointer + 1 or the old if its larger
              int nestingLevelOfNestedList = nestingMap.get(candidateForObj);
              int nestingLevelOfSource = nestingMap.get(start);
              nestingMap.put(
                  candidateForObj, Integer.max(nestingLevelOfNestedList, nestingLevelOfSource) + 1);
            }
          }
        }
      }
    }

    if (pointsToNext != null) {
      findNestingOfCandidates(
          start,
          pointsToNext.pointsTo(),
          nfo,
          maybePfo,
          alreadySeen,
          nestingMap,
          objToCandidateMap);
    }
  }

  private boolean candidateHasEqualValuesForAtLeastLength(
      SMGObject root,
      SMGObject walker,
      BigInteger nfo,
      Optional<BigInteger> maybePfo,
      Set<SMGObject> alreadySeen,
      int currentLength) {
    SMG smg = state.getMemoryModel().getSmg();
    if (!smg.isValid(root) || alreadySeen.contains(walker)) {
      return false;
    }

    SMGPointsToEdge pointsToNext = null;
    // Get next pointer (read would do the same thing...., this way we don't care about sizes)
    for (SMGHasValueEdge hve : smg.getEdges(walker)) {
      SMGValue value = hve.hasValue();

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        pointsToNext = smg.getPTEdge(value).orElseThrow();
        alreadySeen.add(walker);
      }
    }

    if (pointsToNext == null) {
      return false;
    }

    try {
      ImmutableList<BigInteger> exemptOffsetsOfList = ImmutableList.of(nfo);
      if (maybePfo.isPresent()) {
        exemptOffsetsOfList = ImmutableList.of(nfo, maybePfo.orElseThrow());
      }
      // Use the more advanced equality check that checks pointers via memory shapes
      if (state.checkEqualValuesForTwoStatesWithExemptions(
          root, walker, exemptOffsetsOfList, state, state, equalityCache)) {
        if (currentLength + 1 >= minimumLengthForListsForAbstraction) {
          return true;
        } else {
          return candidateHasEqualValuesForAtLeastLength(
              root, pointsToNext.pointsTo(), nfo, maybePfo, alreadySeen, currentLength + 1);
        }
      }
    } catch (SMGException pE) {
      // Fallthrough to false
    }
    return false;
  }

  private SMGCandidate getLinkedCandidateLength(
      SMGObject root,
      BigInteger nfo,
      int size,
      Set<SMGObject> alreadySeen,
      SMGCandidate candidate) {

    SMG smg = state.getMemoryModel().getSmg();
    if (!smg.isValid(root) || alreadySeen.contains(root)) {
      return SMGCandidate.withFoundListElements(alreadySeen, size, candidate);
    }

    int addSize = 1;
    if (root instanceof SMGSinglyLinkedListSegment) {
      addSize = ((SMGSinglyLinkedListSegment) root).getMinLength();
    }

    for (SMGHasValueEdge hve : smg.getEdges(root)) {
      SMGValue value = hve.hasValue();

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = smg.getPTEdge(value).orElseThrow();
        // We only add list elements to already seen
        // In theory this does not prevent all possible loops. But who would program such a thing?
        alreadySeen.add(root);
        return getLinkedCandidateLength(
            pointsToEdge.pointsTo(), nfo, addSize + size, alreadySeen, candidate);
      }
    }
    // We don't count the "last" element this way if it does not have a pointer at nfo
    // This is completely fine! If the list extends further it will break the threshold and be
    // abstracted eventually.
    return SMGCandidate.withFoundListElements(alreadySeen, size, candidate);
  }

  /**
   * Finds all possible linked list candidates from the states heap memory. Checks that the
   * candidates have the same size and nfo and are valid and have at least
   * minimumLengthForListsForAbstraction length in total. Does not check DLL or value equality! You
   * can expect the candidates to have the suspected root, nfo, max size independent of value
   * equality and the objects.
   *
   * @return an unsorted list of candidates.
   */
  ImmutableList<SMGCandidate> getRefinedLinkedCandidates() {
    ImmutableList<SMGCandidate> sortedCandiList =
        ImmutableList.sortedCopyOf(
            Comparator.comparing(SMGCandidate::getSuspectedNfo),
            refineCandidates(getLinkedCandidates(), state));

    ImmutableList.Builder<SMGCandidate> refinedLinkedCandidatesBuilder = ImmutableList.builder();
    for (SMGCandidate candidate : sortedCandiList) {
      SMGCandidate candidateWithListInfo =
          getLinkedCandidateLength(
              candidate.getObject(), candidate.getSuspectedNfo(), 0, new HashSet<>(), candidate);
      if (minimumLengthForListsForAbstraction <= candidateWithListInfo.maximalSizeOfList) {
        refinedLinkedCandidatesBuilder.add(candidateWithListInfo);
      }
    }
    return refinedLinkedCandidatesBuilder.build();
  }

  @VisibleForTesting
  private Set<SMGCandidate> getLinkedCandidates() {
    SMG smg = state.getMemoryModel().getSmg();
    Set<SMGCandidate> candidates = new HashSet<>();
    Set<SMGObject> alreadyVisited = new HashSet<>();
    for (SMGObject heapObj : state.getMemoryModel().getHeapObjects()) {
      if (!smg.isValid(heapObj)) {
        continue;
      }
      Optional<SMGCandidate> possibleCandidate =
          getSLLinkedCandidatesForObject(
              heapObj, smg, alreadyVisited, state.getMemoryModel().getHeapObjects());
      if (possibleCandidate.isPresent()) {
        candidates.add(possibleCandidate.orElseThrow());
      }
    }
    // Refine candidates (DLL tend to produce both ends as candidates,
    // we chose the smallest offset and traverse the list and if we git another list,
    // we kick it out)
    return candidates;
  }

  /**
   * Determines if a candidate is a DLL and returns the suspected PFO if it is. Protected for tests
   * only.
   *
   * @param candidate the {@link SMGCandidate} for a list.
   * @param smg the {@link SMG}
   * @return Optional.empty iff not a DLL. Suspected PFO else.
   */
  @VisibleForTesting
  protected Optional<BigInteger> isDLL(SMGCandidate candidate, SMG smg) {
    BigInteger nfo = candidate.getSuspectedNfo();
    SMGObject root = candidate.getObject();
    // Go to the next element, search for a pointer back and conform this for the following
    // The first element might not have a prev pointer
    // In theory the first prev pointer of a list may not have been set, we check the next
    SMGObject nextObject = null;
    if (root instanceof SMGDoublyLinkedListSegment) {
      SMGDoublyLinkedListSegment dll = (SMGDoublyLinkedListSegment) root;
      if (dll.getNextOffset().compareTo(nfo) == 0) {
        return Optional.of(dll.getPrevOffset());
      } else if (dll.getPrevOffset().compareTo(nfo) == 0) {
        // DLL got turned, turn back

      } else {
        throw new AssertionError("Error during shape refinement.");
      }

    } else if (root instanceof SMGSinglyLinkedListSegment) {
      return Optional.empty();
    }
    for (SMGHasValueEdge hve : smg.getEdges(root)) {
      SMGValue value = hve.hasValue();

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = smg.getPTEdge(value).orElseThrow();
        nextObject = pointsToEdge.pointsTo();
        break;
      }
    }
    return findBackPointerOffsetForListObject(nextObject, root, smg, nfo, 1);
  }

  Set<SMGCandidate> refineCandidates(Set<SMGCandidate> candidates, SMGState pState) {
    Set<SMGCandidate> finalCandidates = candidates;
    ImmutableList<SMGCandidate> sortedCandiList =
        ImmutableList.sortedCopyOf(Comparator.comparing(SMGCandidate::getSuspectedNfo), candidates);
    for (SMGCandidate candi : sortedCandiList) {
      // now kick all found other candidates out
      if (finalCandidates.contains(candi)) {
        finalCandidates =
            traverseAndRemoveEqual(
                candi.getObject(),
                candi.getSuspectedNfo(),
                finalCandidates,
                pState,
                new HashSet<SMGObject>());
      }
    }
    return finalCandidates;
  }

  private Set<SMGCandidate> traverseAndRemoveEqual(
      SMGObject candidate,
      BigInteger nfo,
      Set<SMGCandidate> candidates,
      SMGState pState,
      Set<SMGObject> alreadyVisited) {
    if (alreadyVisited.contains(candidate)) {
      return candidates;
    }
    alreadyVisited.add(candidate);
    // SLL is fine to traverse
    Optional<SMGObject> maybeNext = getValidNextSLL(candidate, nfo, pState);
    if (maybeNext.isPresent()) {
      SMGObject nextObject = maybeNext.orElseThrow();
      Set<SMGCandidate> newCandidates =
          candidates.stream()
              .filter(can -> !can.getObject().equals(nextObject))
              .collect(ImmutableSet.toImmutableSet());
      return traverseAndRemoveEqual(nextObject, nfo, newCandidates, pState, alreadyVisited);
    } else {
      return candidates;
    }
  }

  private Optional<SMGObject> getValidNextSLL(SMGObject root, BigInteger nfo, SMGState pState) {
    SMGValueAndSMGState valueAndState =
        pState.readSMGValue(root, nfo, pState.getMemoryModel().getSizeOfPointer());
    SMGValue value = valueAndState.getSMGValue();
    if (!state.getMemoryModel().getSmg().isPointer(value)) {
      return Optional.empty();
    }
    SMGObject nextObject =
        pState.getMemoryModel().getSmg().getPTEdge(value).orElseThrow().pointsTo();
    if (!pState.getMemoryModel().getSmg().isValid(nextObject)
        || root.getSize().compareTo(nextObject.getSize()) != 0) {
      return Optional.empty();
    }
    // Same object size, same content expect for the pointers, its valid -> ok
    // We don't need the state as it would only change for unknown reads
    return Optional.of(nextObject);
  }

  private Optional<BigInteger> nextHaveBackPointersWithAssumedPFO(
      SMGObject root,
      SMGObject previous,
      SMG smg,
      BigInteger nfo,
      BigInteger pfo,
      int lengthToCheck,
      Set<SMGObject> alreadyVisited) {
    if (lengthToCheck <= 0) {
      return Optional.of(pfo);
    }

    ImmutableSet<SMGHasValueEdge> setOfPointers =
        getPointersToSameSizeObjectsWithoutOffset(root, smg, alreadyVisited, nfo);
    if (setOfPointers.size() < 1) {
      return Optional.empty();
    }

    @Nullable SMGObject nextObject = findNextObjectForListWithNFO(root, nfo, smg);

    // Check all others for validity
    alreadyVisited.add(previous);
    for (SMGHasValueEdge hve : setOfPointers) {
      if (hve.getOffset().compareTo(pfo) != 0) {
        continue;
      }
      SMGValue value = hve.hasValue();
      SMGPointsToEdge pte = smg.getPTEdge(value).orElseThrow();
      SMGObject prevInCurrent = pte.pointsTo();
      if (prevInCurrent.equals(previous)) {
        // assume that the pfo is correct, test further
        if (nextObject == null) {
          return Optional.empty();
        }

        Optional<BigInteger> maybePFO =
            nextHaveBackPointersWithAssumedPFO(
                nextObject, root, smg, nfo, pfo, lengthToCheck - 1, alreadyVisited);

        if (maybePFO.isPresent()) {
          return maybePFO;
        }
      }
    }
    return Optional.empty();
  }

  private Optional<BigInteger> findBackPointerOffsetForListObject(
      SMGObject root, SMGObject previous, SMG smg, BigInteger nfo, int lengthToCheck) {
    Set<SMGObject> alreadyVisited = new HashSet<>();
    // pfo unknown, try to find it

    ImmutableSet<SMGHasValueEdge> setOfPointers =
        getPointersToSameSizeObjectsWithoutOffset(root, smg, alreadyVisited, nfo);
    if (setOfPointers.size() < 1) {
      return Optional.empty();
    }

    @Nullable SMGObject nextObject = findNextObjectForListWithNFO(root, nfo, smg);

    // Check all others for validity
    alreadyVisited.add(previous);
    for (SMGHasValueEdge hve : setOfPointers) {
      BigInteger pfo = hve.getOffset();
      SMGValue value = hve.hasValue();
      SMGPointsToEdge pte = smg.getPTEdge(value).orElseThrow();
      SMGObject prevInCurrent = pte.pointsTo();
      if (prevInCurrent.equals(previous)) {
        // assume that the pfo is correct, test further
        Optional<BigInteger> maybePFO =
            nextHaveBackPointersWithAssumedPFO(
                nextObject, root, smg, nfo, pfo, lengthToCheck - 1, alreadyVisited);

        if (maybePFO.isPresent()) {
          return maybePFO;
        }
      }
    }
    return Optional.empty();
  }

  private SMGObject findNextObjectForListWithNFO(SMGObject root, BigInteger nfo, SMG smg) {
    SMGObject nextObject = null;
    for (SMGHasValueEdge hve : smg.getEdges(root)) {
      SMGValue value = hve.hasValue();

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        Optional<SMGPointsToEdge> pointsToEdge = smg.getPTEdge(value);
        if (pointsToEdge.isEmpty()) {
          break;
        }
        nextObject = pointsToEdge.orElseThrow().pointsTo();
        break;
      }
    }
    return nextObject;
  }

  /* Search for followup segments based on potential root. If we find a object that is already processed in the candidatesMap, we take and remove it from the map, link it to the current segment, and return the new segment. */
  private Optional<SMGCandidate> getSLLinkedCandidatesForObject(
      SMGObject potentialRoot,
      SMG pInputSmg,
      Set<SMGObject> pAlreadyVisited,
      Collection<SMGObject> heapObjects) {
    Set<SMGObject> thisAlreadyVisited = new HashSet<>(pAlreadyVisited);
    if (thisAlreadyVisited.contains(potentialRoot) || !pInputSmg.isValid(potentialRoot)) {
      return Optional.empty();
    }
    thisAlreadyVisited.add(potentialRoot);
    if (potentialRoot instanceof SMGSinglyLinkedListSegment) {
      pAlreadyVisited.add(potentialRoot);
      SMGSinglyLinkedListSegment sll = (SMGSinglyLinkedListSegment) potentialRoot;
      return Optional.of(new SMGCandidate(potentialRoot, sll.getNextOffset()));
    }

    ImmutableSet<SMGHasValueEdge> setOfPointers =
        getPointersToSameSizeObjects(potentialRoot, pInputSmg, thisAlreadyVisited);
    // Sort by offset of pointers and beginn with the smallest
    // Lists usually have smth like a next, then prev pointer ordering
    // We abort after we find 1 valid pointer for the candidate
    ImmutableList<SMGHasValueEdge> sortedPointersList =
        ImmutableList.sortedCopyOf(Comparator.comparing(SMGHasValueEdge::getOffset), setOfPointers);
    // If there are no targets that match this one, this is either a end part or not a list
    if (setOfPointers.isEmpty()) {
      return Optional.empty();
    }
    for (SMGHasValueEdge hve : sortedPointersList) {
      SMGValue value = hve.hasValue();
      BigInteger nfo = hve.getOffset();
      SMGPointsToEdge pointsToEdge = pInputSmg.getPTEdge(value).orElseThrow();
      SMGObject reachedObject = pointsToEdge.pointsTo();
      if (reachedObject instanceof SMGSinglyLinkedListSegment) {
        SMGSinglyLinkedListSegment sll = (SMGSinglyLinkedListSegment) reachedObject;
        if (sll.getNextOffset().compareTo(nfo) == 0) {
          pAlreadyVisited.add(potentialRoot);
          return Optional.of(new SMGCandidate(potentialRoot, nfo));
        }
      }

      // Check that reached object has a pointer at the same offset
      if (followupHasNextPointerToValid(reachedObject, nfo, pInputSmg, thisAlreadyVisited)) {
        // Valid candidate found!
        // Make sure it's a "root" by checking all pointers towards this root
        // The only valid pointers towards this root are from the followup or non heap objects
        if (pInputSmg.hasPotentialListObjectsWithPointersToObject(
            potentialRoot, nfo, heapObjects)) {
          pAlreadyVisited.add(potentialRoot);
          return Optional.of(new SMGCandidate(potentialRoot, nfo));
        }
      }
    }
    return Optional.empty();
  }

  private boolean followupHasNextPointerToValid(
      SMGObject potentialFollowup,
      BigInteger nfoOfPrev,
      SMG pInputSmg,
      Set<SMGObject> alreadyVisited) {
    alreadyVisited.add(potentialFollowup);
    ImmutableSet<SMGHasValueEdge> pointers =
        getPointersToSameSizeObjects(potentialFollowup, pInputSmg, alreadyVisited);
    if (pointers.isEmpty()) {
      alreadyVisited.remove(potentialFollowup);
      return false;
    }
    // Only valid pointers are left
    for (SMGHasValueEdge hve : pointers) {
      if (hve.getOffset().compareTo(nfoOfPrev) == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns set of HVE whoes values are pointers pointing to valid same size objects that have not
   * yet been visited.
   */
  private ImmutableSet<SMGHasValueEdge> getPointersToSameSizeObjects(
      SMGObject root, SMG pInputSmg, Set<SMGObject> alreadyVisited) {
    BigInteger rootSize = root.getSize();
    ImmutableSet.Builder<SMGHasValueEdge> res = ImmutableSet.builder();
    for (SMGHasValueEdge hve : pInputSmg.getEdges(root)) {
      SMGValue value = hve.hasValue();

      if (pInputSmg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = pInputSmg.getPTEdge(value).orElseThrow();
        SMGObject reachedObject = pointsToEdge.pointsTo();
        if (alreadyVisited.contains(reachedObject)) {
          continue;
        }
        // If the followup is invalid or size does not match, next
        if (!pInputSmg.isValid(reachedObject) || reachedObject.getSize().compareTo(rootSize) != 0) {
          continue;
        }

        if (pointsToEdge.targetSpecifier() != SMGTargetSpecifier.IS_ALL_POINTER
            && pointsToEdge.targetSpecifier() != SMGTargetSpecifier.IS_REGION) {
          continue;
        }
        res.add(hve);
      }
    }
    return res.build();
  }

  private ImmutableSet<SMGHasValueEdge> getPointersToSameSizeObjectsWithoutOffset(
      SMGObject root, SMG pInputSmg, Set<SMGObject> alreadyVisted, BigInteger offsetToAvoid) {
    BigInteger rootSize = root.getSize();
    ImmutableSet.Builder<SMGHasValueEdge> res = ImmutableSet.builder();
    for (SMGHasValueEdge hve : pInputSmg.getEdges(root)) {
      SMGValue value = hve.hasValue();
      if (hve.getOffset().compareTo(offsetToAvoid) == 0) {
        continue;
      }

      if (pInputSmg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = pInputSmg.getPTEdge(value).orElseThrow();
        SMGObject reachedObject = pointsToEdge.pointsTo();
        if (alreadyVisted.contains(reachedObject)) {
          continue;
        }
        // If the followup is invalid or size does not match, next
        if (!pInputSmg.isValid(reachedObject) || reachedObject.getSize().compareTo(rootSize) != 0) {
          continue;
        }

        if (pointsToEdge.targetSpecifier() != SMGTargetSpecifier.IS_ALL_POINTER
            && pointsToEdge.targetSpecifier() != SMGTargetSpecifier.IS_REGION) {
          continue;
        }
        res.add(hve);
      }
    }
    return res.build();
  }

  @VisibleForTesting
  protected static class SMGCandidate {
    private SMGObject object;
    private BigInteger suspectedNfo;

    // If not present -> SLL
    private Optional<BigInteger> suspectedPfo;

    /*
     * Other suspected list elements. This is the maximal possible over approximation for the same size/nfo!
     * Equalities are not checked and this might not reflect the final abstracted list.
     * Used to find accurate nesting.
     */
    private Set<SMGObject> suspectedElements;

    // Max size. Not checked for abstractable length!
    private int maximalSizeOfList;

    public SMGCandidate(SMGObject pObject, BigInteger pSuspectedNfo) {
      this.object = pObject;
      this.suspectedNfo = pSuspectedNfo;
      this.suspectedPfo = Optional.empty();
      this.suspectedElements = ImmutableSet.of();
      this.maximalSizeOfList = 1;
    }

    private SMGCandidate(
        SMGObject pObject,
        BigInteger pSuspectedNfo,
        Optional<BigInteger> pSuspectedPfo,
        Set<SMGObject> pSuspectedElements,
        int maxSize) {
      this.object = pObject;
      this.suspectedNfo = pSuspectedNfo;
      this.suspectedPfo = pSuspectedPfo;
      this.suspectedElements = pSuspectedElements;
      this.maximalSizeOfList = maxSize;
    }

    public static SMGCandidate moveCandidateTo(
        SMGObject newCandidateInSameList, SMGCandidate oldCandidateOnSameList) {
      return new SMGCandidate(
          newCandidateInSameList,
          oldCandidateOnSameList.suspectedNfo,
          oldCandidateOnSameList.suspectedPfo,
          oldCandidateOnSameList.suspectedElements,
          oldCandidateOnSameList.maximalSizeOfList);
    }

    public static SMGCandidate withPfo(BigInteger pSuspectedPfo, SMGCandidate candidate) {
      return new SMGCandidate(
          candidate.object,
          candidate.suspectedNfo,
          Optional.of(pSuspectedPfo),
          candidate.suspectedElements,
          candidate.maximalSizeOfList);
    }

    public static SMGCandidate withFoundListElements(
        Set<SMGObject> pSuspectedElements, int lengthOfList, SMGCandidate candidate) {
      return new SMGCandidate(
          candidate.object,
          candidate.suspectedNfo,
          candidate.suspectedPfo,
          pSuspectedElements,
          lengthOfList);
    }

    public SMGObject getObject() {
      return object;
    }

    public BigInteger getSuspectedNfo() {
      return suspectedNfo;
    }

    public boolean isDLL() {
      return suspectedPfo.isPresent();
    }

    public Optional<BigInteger> getSuspectedPfo() {
      return suspectedPfo;
    }

    /**
     * Other suspected list elements. This is the maximal possible over approximation for the same
     * size/nfo! Equalities are not checked and this might not reflect the final abstracted list.
     * The candidate is part of that list, but it is unknown where. Used to find accurate nesting.
     *
     * @return the found list for the candidate.
     */
    public Set<SMGObject> getMaximalListElements() {
      return suspectedElements;
    }
  }
}

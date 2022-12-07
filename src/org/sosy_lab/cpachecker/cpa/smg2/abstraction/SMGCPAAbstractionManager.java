// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
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

  private int minimumLengthForListsForAbstraction;

  public SMGCPAAbstractionManager(SMGState pState, int pMinimumLengthForListsForAbstraction) {
    state = pState;
    minimumLengthForListsForAbstraction = pMinimumLengthForListsForAbstraction;
  }

  public SMGState findAndAbstractLists() throws SMG2Exception {
    SMGState currentState = state;
    for (SMGCandidate candidate : getRefinedLinkedCandidates()) {
      // Not valid means kicked out by abstraction
      if (!currentState.getMemoryModel().isObjectValid(candidate.getObject())) {
        continue;
      }
      Optional<BigInteger> maybePFO = isDLL(candidate, currentState.getMemoryModel().getSmg());
      if (maybePFO.isPresent()) {
        // We did not yet check for the values! Because we needed to know if there is a back pointer
        // or not.  Needed for nested lists!
        for (SMGCandidate trueDLLCandidate :
            checkValueEquality(
                candidate.getObject(),
                candidate.getObject(),
                candidate.getSuspectedNfo(),
                maybePFO,
                new HashSet<>(),
                new HashSet<>(),
                0)) {
          currentState =
              currentState.abstractIntoDLL(
                  trueDLLCandidate.getObject(),
                  trueDLLCandidate.getSuspectedNfo(),
                  maybePFO.orElseThrow(),
                  ImmutableSet.of());
        }
      } else {
        // We did not yet check for the values! Because we needed to know if there is a back pointer
        // or not.  Needed for nested lists!
        for (SMGCandidate trueSLLCandidate :
            checkValueEquality(
                candidate.getObject(),
                candidate.getObject(),
                candidate.getSuspectedNfo(),
                Optional.empty(),
                new HashSet<>(),
                new HashSet<>(),
                0)) {
          currentState =
              currentState.abstractIntoSLL(
                  trueSLLCandidate.getObject(),
                  trueSLLCandidate.getSuspectedNfo(),
                  ImmutableSet.of());
        }
      }
    }

    return currentState;
  }

  private Set<SMGCandidate> checkValueEquality(
      SMGObject root,
      SMGObject walker,
      BigInteger nfo,
      Optional<BigInteger> maybePfo,
      Set<SMGObject> alreadySeen,
      Set<SMGCandidate> currentCandidates,
      int currentLength) {
    SMG smg = state.getMemoryModel().getSmg();
    if (!smg.isValid(root) || alreadySeen.contains(walker)) {
      return currentCandidates;
    }

    Map<BigInteger, Value> offsetToValueMap = new HashMap<>();
    for (SMGHasValueEdge hve : smg.getEdges(root)) {
      offsetToValueMap.put(
          hve.getOffset(),
          state.getMemoryModel().getValueFromSMGValue(hve.hasValue()).orElseThrow());
    }

    SMGPointsToEdge pointsToNext = null;
    boolean notEqual = false;
    for (SMGHasValueEdge hve : smg.getEdges(walker)) {
      SMGValue value = hve.hasValue();

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        pointsToNext = smg.getPTEdge(value).orElseThrow();
        alreadySeen.add(walker);
      } else if (maybePfo.isPresent() && hve.getOffset().compareTo(maybePfo.orElseThrow()) == 0) {
        // Do nothing, we just don't want to check the back pointer
      } else {
        // Value equality check
        Value valueAtOffset = offsetToValueMap.get(hve.getOffset());
        Value thisValue = state.getMemoryModel().getValueFromSMGValue(hve.hasValue()).orElseThrow();
        // If they are equal -> fine.
        if (!thisValue.equals(valueAtOffset)) {
          if (state.getMemoryModel().isPointer(thisValue)
              || state.getMemoryModel().isPointer(valueAtOffset)) {
            // We know they are not equal.
            // If one or both are non equal pointers -> not equal
            notEqual = true;

          } else if (valueAtOffset == null
              || !(!thisValue.isExplicitlyKnown() && !valueAtOffset.isExplicitlyKnown())) {
            // If none is a pointer, they can be both unknown (symbolic) to be equal, else not
            notEqual = true;
          }
        }
      }
    }

    if (pointsToNext == null) {
      return currentCandidates;
    }

    if (notEqual) {
      return checkValueEquality(
          walker, pointsToNext.pointsTo(), nfo, maybePfo, alreadySeen, currentCandidates, 1);
    }

    if (currentLength + 1 >= minimumLengthForListsForAbstraction) {
      currentCandidates.add(new SMGCandidate(root, nfo));
      return checkValueEquality(
          root,
          pointsToNext.pointsTo(),
          nfo,
          maybePfo,
          alreadySeen,
          currentCandidates,
          currentLength + 1);
    } else {
      return checkValueEquality(
          root,
          pointsToNext.pointsTo(),
          nfo,
          maybePfo,
          alreadySeen,
          currentCandidates,
          currentLength + 1);
    }
  }

  private int getLinkedCandidateLength(SMGObject root, BigInteger nfo, Set<SMGObject> alreadySeen) {
    SMG smg = state.getMemoryModel().getSmg();
    if (!smg.isValid(root) || alreadySeen.contains(root)) {
      return 0;
    }
    for (SMGHasValueEdge hve : smg.getEdges(root)) {
      SMGValue value = hve.hasValue();
      int size = 1;
      if (root instanceof SMGSinglyLinkedListSegment) {
        size = ((SMGSinglyLinkedListSegment) root).getMinLength();
      }

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = smg.getPTEdge(value).orElseThrow();
        alreadySeen.add(root);
        return size + getLinkedCandidateLength(pointsToEdge.pointsTo(), nfo, alreadySeen);
      }
    }
    return 0;
  }

  ImmutableList<SMGCandidate> getRefinedLinkedCandidates() {
    ImmutableList<SMGCandidate> sortedCandiList =
        ImmutableList.sortedCopyOf(
            Comparator.comparing(SMGCandidate::getSuspectedNfo),
            refineCandidates(getLinkedCandidates(), state));
    return sortedCandiList.stream()
        .filter(
            c ->
                minimumLengthForListsForAbstraction
                    <= getLinkedCandidateLength(
                        c.getObject(), c.getSuspectedNfo(), new HashSet<>()))
        .collect(ImmutableList.toImmutableList());
  }

  // Protected for tests
  private Set<SMGCandidate> getLinkedCandidates() {
    SMG smg = state.getMemoryModel().getSmg();
    Set<SMGCandidate> candidates = new HashSet<>();
    Set<SMGObject> alreadyVisited = new HashSet<>();
    for (SMGObject heapObj : state.getMemoryModel().getHeapObjects()) {
      if (!smg.isValid(heapObj)) {
        continue;
      }
      Optional<SMGCandidate> maybeCandidate =
          getSLLinkedCandidatesForObject(
              heapObj, smg, alreadyVisited, state.getMemoryModel().getHeapObjects());
      if (maybeCandidate.isPresent()) {
        candidates.add(maybeCandidate.orElseThrow());
      }
    }
    // Refine candidates (DLL tend to produce both ends as candidates,
    // we chose the smallest offset and traverse the list and if we git another list,
    // we kick it out)
    return candidates;
  }

  // Protected for tests
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
    return nexthaveBackPointers(nextObject, root, smg, nfo, 1);
  }

  Set<SMGCandidate> refineCandidates(Set<SMGCandidate> candidates, SMGState pState) {
    Set<SMGCandidate> finalCandidates = candidates;
    ImmutableList<SMGCandidate> sortedCandiList =
        ImmutableList.sortedCopyOf(Comparator.comparing(SMGCandidate::getSuspectedNfo), candidates);
    for (SMGCandidate candi : sortedCandiList) {
      // now kick all found other candidates out
      if (finalCandidates.contains(candi)) {
        finalCandidates =
            traverseAndKickEqual(
                candi.getObject(),
                candi.getSuspectedNfo(),
                finalCandidates,
                pState,
                new HashSet<SMGObject>());
      }
    }
    return finalCandidates;
  }

  private Set<SMGCandidate> traverseAndKickEqual(
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
      return traverseAndKickEqual(nextObject, nfo, newCandidates, pState, alreadyVisited);
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

  private Optional<BigInteger> nexthaveBackPointers(
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
    } else {
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
              nexthaveBackPointers(
                  nextObject, root, smg, nfo, pfo, lengthToCheck - 1, alreadyVisited);
          if (maybePFO.isPresent()) {
            return maybePFO;
          }
        }
      }
    }
    return Optional.empty();
  }

  private Optional<BigInteger> nexthaveBackPointers(
      SMGObject root, SMGObject previous, SMG smg, BigInteger nfo, int lengthToCheck) {
    Set<SMGObject> alreadyVisited = new HashSet<>();
    // pfo unknown, try to find it

    ImmutableSet<SMGHasValueEdge> setOfPointers =
        getPointersToSameSizeObjectsWithoutOffset(root, smg, alreadyVisited, nfo);
    if (setOfPointers.size() < 1) {
      return Optional.empty();
    } else {
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
              nexthaveBackPointers(
                  nextObject, root, smg, nfo, pfo, lengthToCheck - 1, alreadyVisited);
          if (maybePFO.isPresent()) {
            return maybePFO;
          }
        }
      }
    }
    return Optional.empty();
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
        // Make sure its a "root" by checking all pointers towards this root
        // The only valid pointers towards this root are from the followup or non heap objects
        ImmutableSet<SMGObject> maybePointerFromHeap =
            pInputSmg.findAllAddressesForTargetObject(potentialRoot, heapObjects);
        if (maybePointerFromHeap.isEmpty()
            || (maybePointerFromHeap.size() == 1 && maybePointerFromHeap.contains(reachedObject))) {
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
      SMGObject root, SMG pInputSmg, Set<SMGObject> alreadyVisted) {
    BigInteger rootSize = root.getSize();
    ImmutableSet.Builder<SMGHasValueEdge> res = ImmutableSet.builder();
    for (SMGHasValueEdge hve : pInputSmg.getEdges(root)) {
      SMGValue value = hve.hasValue();

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

  protected static class SMGCandidate {
    private SMGObject object;
    private BigInteger suspectedNfo;

    public SMGCandidate(SMGObject object, BigInteger suspectedNfo) {
      this.object = object;
      this.suspectedNfo = suspectedNfo;
    }

    public SMGObject getObject() {
      return object;
    }

    public BigInteger getSuspectedNfo() {
      return suspectedNfo;
    }
  }
}

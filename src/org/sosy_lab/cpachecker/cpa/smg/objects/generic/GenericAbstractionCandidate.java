/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.objects.generic;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenericAbstractionCandidate implements SMGGenericAbstractionCandidate {

  private final Set<SMGObject> objectsToBeRemoved;
  private final Map<Integer, Integer> abstractToConcretePointerMap;
  private final Map<Integer, List<MaterlisationStep>> materlisationStep;
  private final int score;

  private GenericAbstractionCandidate(Set<SMGObject> pObjectsToBeRemoved,
      Map<Integer, Integer> pAbstractToConcretePointerMap,
      Map<Integer, List<MaterlisationStep>> pMaterlisationStep,
      int pScore) {
    objectsToBeRemoved = pObjectsToBeRemoved;
    abstractToConcretePointerMap = pAbstractToConcretePointerMap;
    materlisationStep = pMaterlisationStep;
    score = pScore;
  }

  public Set<SMGObject> getObjectsToBeRemoved() {
    return objectsToBeRemoved;
  }

  public Map<Integer, Integer> getAbstractToConcretePointerMap() {
    return abstractToConcretePointerMap;
  }

  public static GenericAbstractionCandidate valueOf(Set<SMGObject> pObjectsToBeRemoved,
      Map<Integer, Integer> pAbstractToConcretePointerMap,
      Map<Integer, List<MaterlisationStep>> pMaterlisationStep, int pScore) {
    return new GenericAbstractionCandidate(pObjectsToBeRemoved, pAbstractToConcretePointerMap,
        pMaterlisationStep, pScore);
  }

  @Override
  public int getScore() {
    return score;
  }

  @Override
  public SMG execute(SMG pSMG) {

    /*First, generate the abstraction object. */
    GenericAbstraction genericAbstraction =
        GenericAbstraction.valueOf(materlisationStep, abstractToConcretePointerMap);

    /* Second, create the pointer that lead from/to this abstraction. */
    Set<SMGEdgePointsTo> pointsToThisAbstraction = new HashSet<>();
    Set<SMGEdgeHasValue> pointsFromThisAbstraction = new HashSet<>();

    int c = 0;
    for (Integer pointer : abstractToConcretePointerMap.values()) {
      assert pSMG.isPointer(pointer);

      SMGEdgePointsTo pointerEdge = pSMG.getPointer(pointer);

      /* If objectsToBeRemoved contains target object of pointer,
       * pointer leads into abstraction, otherwise hve in abstraction
       * leads outside.*/
      if (objectsToBeRemoved.contains(pointerEdge.getObject())) {
        SMGEdgePointsTo newPointer = new SMGEdgePointsTo(pointer, genericAbstraction,
            pointerEdge.getOffset());
        pointsToThisAbstraction.add(newPointer);
      } else {
        //TODO Real offset might be nicer.
        SMGEdgeHasValue dummyEdge = new SMGEdgeHasValue(1, c, genericAbstraction, pointer);
        pointsFromThisAbstraction.add(dummyEdge);
        c++;
      }
    }

    /*Third, remove all object and edges that are represented by this abstraction.*/
    for (SMGObject obj : objectsToBeRemoved) {
      assert pSMG.getObjects().contains(obj);
      pSMG.removeObjectAndEdges(obj);
    }

    /*Finally, connect it to the rest of the smg.*/
    connect(pSMG, genericAbstraction, pointsToThisAbstraction, pointsFromThisAbstraction);

    return pSMG;
  }

  private void connect(SMG pSMG, GenericAbstraction pGenericAbstraction,
      Set<SMGEdgePointsTo> pAbstractAddresses, Set<SMGEdgeHasValue> pAbstractFields) {

    pSMG.addObject(pGenericAbstraction);

    for (SMGEdgePointsTo pte : pAbstractAddresses) {
      pSMG.addPointsToEdge(pte);
    }

    for(SMGEdgeHasValue hve : pAbstractFields) {
      pSMG.addHasValueEdge(hve);
    }
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  public GenericAbstractionCandidateTemplate createTemplate() {
    return GenericAbstractionCandidateTemplate.valueOf(materlisationStep);
  }
}
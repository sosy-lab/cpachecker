// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class GenericAbstractionCandidate implements SMGGenericAbstractionCandidate {

  private final MachineModel machineModel;
  private final Set<SMGObject> objectsToBeRemoved;
  private final Map<SMGValue, SMGValue> abstractToConcretePointerMap;
  private final Map<SMGValue, List<MaterlisationStep>> materlisationStep;
  private final int score;

  private GenericAbstractionCandidate(
      MachineModel pMachineModel,
      Set<SMGObject> pObjectsToBeRemoved,
      Map<SMGValue, SMGValue> pAbstractToConcretePointerMap,
      Map<SMGValue, List<MaterlisationStep>> pMaterlisationStep,
      int pScore) {
    machineModel = pMachineModel;
    objectsToBeRemoved = pObjectsToBeRemoved;
    abstractToConcretePointerMap = pAbstractToConcretePointerMap;
    materlisationStep = pMaterlisationStep;
    score = pScore;
  }

  public Set<SMGObject> getObjectsToBeRemoved() {
    return objectsToBeRemoved;
  }

  public Map<SMGValue, SMGValue> getAbstractToConcretePointerMap() {
    return abstractToConcretePointerMap;
  }

  public static GenericAbstractionCandidate valueOf(
      MachineModel pMachineModel,
      Set<SMGObject> pObjectsToBeRemoved,
      Map<SMGValue, SMGValue> pAbstractToConcretePointerMap,
      Map<SMGValue, List<MaterlisationStep>> pMaterlisationStep,
      int pScore) {
    return new GenericAbstractionCandidate(
        pMachineModel,
        pObjectsToBeRemoved,
        pAbstractToConcretePointerMap,
        pMaterlisationStep,
        pScore);
  }

  @Override
  public int getScore() {
    return score;
  }

  @Override
  public SMG execute(SMG pSMG) {

    /*First, generate the abstraction object. */
    GenericAbstraction genericAbstraction =
        GenericAbstraction.valueOf(machineModel, materlisationStep, abstractToConcretePointerMap);

    /* Second, create the pointer that lead from/to this abstraction. */
    Set<SMGEdgePointsTo> pointsToThisAbstraction = new HashSet<>();
    Set<SMGEdgeHasValue> pointsFromThisAbstraction = new HashSet<>();

    int c = 0;
    for (SMGValue pointer : abstractToConcretePointerMap.values()) {
      assert pSMG.isPointer(pointer);

      SMGEdgePointsTo pointerEdge = pSMG.getPointer(pointer);

      /* If objectsToBeRemoved contains target object of pointer,
       * pointer leads into abstraction, otherwise hve in abstraction
       * leads outside.*/
      if (objectsToBeRemoved.contains(pointerEdge.getObject())) {
        SMGEdgePointsTo newPointer =
            new SMGEdgePointsTo(pointer, genericAbstraction, pointerEdge.getOffset());
        pointsToThisAbstraction.add(newPointer);
      } else {
        // TODO Real offset might be nicer.
        SMGEdgeHasValue dummyEdge = new SMGEdgeHasValue(1, c, genericAbstraction, pointer);
        pointsFromThisAbstraction.add(dummyEdge);
        c++;
      }
    }

    /*Third, remove all object and edges that are represented by this abstraction.*/
    for (SMGObject obj : objectsToBeRemoved) {
      assert pSMG.getObjects().contains(obj);
      pSMG.markObjectDeletedAndRemoveEdges(obj);
    }

    /*Finally, connect it to the rest of the smg.*/
    connect(pSMG, genericAbstraction, pointsToThisAbstraction, pointsFromThisAbstraction);

    return pSMG;
  }

  private void connect(
      SMG pSMG,
      GenericAbstraction pGenericAbstraction,
      Set<SMGEdgePointsTo> pAbstractAddresses,
      Set<SMGEdgeHasValue> pAbstractFields) {

    pSMG.addObject(pGenericAbstraction);

    for (SMGEdgePointsTo pte : pAbstractAddresses) {
      pSMG.addPointsToEdge(pte);
    }

    for (SMGEdgeHasValue hve : pAbstractFields) {
      pSMG.addHasValueEdge(hve);
    }
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  public GenericAbstractionCandidateTemplate createTemplate(MachineModel pMachineModel) {
    return GenericAbstractionCandidateTemplate.valueOf(pMachineModel, materlisationStep);
  }
}

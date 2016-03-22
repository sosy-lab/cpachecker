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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class GenericAbstractionCandidateTemplate implements SMGObjectTemplate {

  private Map<Integer, List<MaterlisationStep>> abstractPointerToMaterlisationSteps;

  private GenericAbstractionCandidateTemplate() {}

  private GenericAbstractionCandidateTemplate(
      Map<Integer, List<MaterlisationStep>> pMaterlisationStep) {
    abstractPointerToMaterlisationSteps = pMaterlisationStep;
  }

  private GenericAbstractionCandidateTemplate(Set<SMGEdgeHasValue> sharedFields,
      Set<Pair<SMGEdgePointsTo, SMGEdgePointsTo>> sharedIPointer,
      Set<Pair<SMGEdgeHasValue, SMGEdgeHasValue>> sharedOPointer,
      Set<SMGEdgeHasValue> nonSharedOPointer,
      SMGRegion root) {
    Map<Integer, List<MaterlisationStep>> result = new HashMap<>();
    abstractPointerToMaterlisationSteps = result;

    MaterlisationStep stopStep = createStopStep(sharedIPointer, sharedOPointer, sharedFields, root);
    MaterlisationStep continueStep =
        prepareContinueStep(nonSharedOPointer, stopStep);

    List<MaterlisationStep> matSteps = ImmutableList.of(continueStep, stopStep);

    /*Also finishes creating the continue step*/
    for (SMGEdgePointsToTemplate abstractAddresses : stopStep.getAbstractAdressesToOPointer()) {
      result.put(abstractAddresses.getAbstractValue(), matSteps);
    }
  }

  private MaterlisationStep prepareContinueStep(Set<SMGEdgeHasValue> pNonSharedOPointer,
      MaterlisationStep pStopStep) {

    Set<SMGObjectTemplate> abstractObjects = new HashSet<>(pNonSharedOPointer.size() + 1);

    SMGObjectTemplate root = pStopStep.getAbstractObjects().iterator().next();
    abstractObjects.add(root);

    Set<SMGEdgePointsToTemplate> targetAdressTemplateOfPointer =
        pStopStep.getAbstractAdressesToOPointer();

    Set<SMGEdgePointsToTemplate> abstractPointer = new HashSet<>(pNonSharedOPointer.size());
    Set<SMGEdgeHasValueTemplate> abstractFieldsToIPointer = new HashSet<>(pNonSharedOPointer.size());

    Map<Integer, Integer> uPointerToPointer = new HashMap<>();

    for (SMGEdgeHasValue oPointer : pNonSharedOPointer) {
      GenericAbstractionCandidateTemplate abstraction =
          new GenericAbstractionCandidateTemplate(abstractPointerToMaterlisationSteps);

      abstractObjects.add(abstraction);

      for (SMGEdgePointsToTemplate iPointerTemplate : targetAdressTemplateOfPointer) {

        int pointerTemplate = iPointerTemplate.getAbstractValue();
        int uPointerTemplate = SMGValueFactory.getNewValue();

        uPointerToPointer.put(uPointerTemplate, pointerTemplate);

        SMGEdgeHasValueTemplate templateOEdge =
            new SMGEdgeHasValueTemplate(root, uPointerTemplate,
                oPointer.getOffset(), oPointer.getType());
        SMGEdgePointsToTemplate templateIEdge =
            new SMGEdgePointsToTemplate(abstraction, uPointerTemplate,
                iPointerTemplate.getOffset());

        abstractPointer.add(templateIEdge);
        abstractFieldsToIPointer.add(templateOEdge);
      }
    }

    Set<SMGEdgeHasValueTemplateWithConcreteValue> abstractFields = pStopStep.getAbstractFields();
    Set<SMGEdgeHasValueTemplate> abstractFieldsToOPointer = pStopStep.getAbstractFieldsToOPointer();

    return new MaterlisationStep(abstractObjects, abstractPointer, abstractFields,
        abstractFieldsToIPointer, targetAdressTemplateOfPointer, abstractFieldsToOPointer,
        uPointerToPointer, false);
  }

  private MaterlisationStep createStopStep(
      Set<Pair<SMGEdgePointsTo, SMGEdgePointsTo>> pSharedIPointer,
      Set<Pair<SMGEdgeHasValue, SMGEdgeHasValue>> pSharedOPointer,
      Set<SMGEdgeHasValue> pSharedFields, SMGRegion pRoot) {

    /*It is assumed, that shared pointer connect the abstraction to outside of the abstraction in the smg.
     *For every shared pointer, generate such an edge. The stop step has no further abstractions connected to it,
     *so the pointers connecting regions within the abstraction are empty.*/

    Set<SMGObjectTemplate> abstractObjects = new HashSet<>();
    abstractObjects.add(pRoot);

    Set<SMGEdgeHasValueTemplate> abstractFieldsToIPointer = ImmutableSet.of();
    Set<SMGEdgePointsToTemplate> abstractPt_edges = ImmutableSet.of();

    Set<SMGEdgeHasValueTemplateWithConcreteValue> abstractFields =
        new HashSet<>(pSharedFields.size());

    for (SMGEdgeHasValue field : pSharedFields) {
      SMGEdgeHasValueTemplateWithConcreteValue edgeTemplate =
          new SMGEdgeHasValueTemplate(pRoot, field.getValue(), field.getOffset(), field.getType());
      abstractFields.add(edgeTemplate);
    }

    Set<SMGEdgePointsToTemplate> abstractAdressesToOPointer = new HashSet<>(pSharedIPointer.size());

    int abstractPointerValue = 0;

    for (Pair<SMGEdgePointsTo, SMGEdgePointsTo> edges : pSharedIPointer) {
      //TODO different Values of edge
      SMGEdgePointsToTemplate edgeTemplate =
          new SMGEdgePointsToTemplate(pRoot, abstractPointerValue, edges.getFirst().getOffset());
      abstractAdressesToOPointer.add(edgeTemplate);
      abstractPointerValue++;
    }

    Set<SMGEdgeHasValueTemplate> abstractFieldsToOPointer =
        new HashSet<>(abstractAdressesToOPointer.size());

    for (Pair<SMGEdgeHasValue, SMGEdgeHasValue> edges : pSharedOPointer) {
    //TODO different Values of edge
      SMGEdgeHasValueTemplate edgeTemplate =
          new SMGEdgeHasValueTemplate(pRoot, abstractPointerValue,
              edges.getFirst().getOffset(), edges.getFirst().getType());
      abstractFieldsToOPointer.add(edgeTemplate);
      abstractPointerValue++;
    }

    Map<Integer, Integer> emptyMap = new HashMap<>();

    return new MaterlisationStep(abstractObjects, abstractPt_edges, abstractFields,
        abstractFieldsToIPointer, abstractAdressesToOPointer, abstractFieldsToOPointer, emptyMap,
        true);
  }

  public Map<Integer, List<MaterlisationStep>> getMaterlisationStepMap() {
    return abstractPointerToMaterlisationSteps;
  }

  public Set<MaterlisationStep> getMaterlisationSteps() {
    Set<MaterlisationStep> result = new HashSet<>();

    for (List<MaterlisationStep> steps : abstractPointerToMaterlisationSteps.values()) {
      for (MaterlisationStep step : steps) {
        if (!result.contains(step)) {
          result.add(step);
        }
      }
    }

    return result;
  }

  @Override
  public SMGObject createConcreteObject(Map<Integer, Integer> pAbstractToConcretePointerMap) {
    return new GenericAbstraction(100,
        "generic abtraction ID " + SMGValueFactory.getNewValue(),
        abstractPointerToMaterlisationSteps,
        pAbstractToConcretePointerMap);
  }

  /**
   * Create simple inductive abstraction with one base object, the root region, and
   * as many inductive steps, as there are pointer leading to the base object.
   *
   * The abstraction is generated from two concrete regions of two smgs. One region represents
   * the base object at the end of the abstraction, while the other represents an object in the middle
   * of the abstraction. Based on which pointer they share, that lead to and from these objects,
   * and which pointer they don't share, a simple inductive abstraction is generated.
   *
   *
   * @param sharedFields shared Fields
   * @param sharedIPointer shared Pointer
   * @param sharedOPointer shared Pointer
   * @param nonSharedOPointer non shared Pointer
   * @param pRoot smg root object
   * @return abstraction
   */
  public static GenericAbstractionCandidateTemplate createSimpleInductiveGenericAbstractionTemplate(
      Set<SMGEdgeHasValue> sharedFields, Set<Pair<SMGEdgePointsTo, SMGEdgePointsTo>> sharedIPointer,
      Set<Pair<SMGEdgeHasValue, SMGEdgeHasValue>> sharedOPointer,
      Set<SMGEdgeHasValue> nonSharedOPointer, SMGRegion pRoot) {
    return new GenericAbstractionCandidateTemplate(sharedFields, sharedIPointer, sharedOPointer, nonSharedOPointer, pRoot);
  }

  public static GenericAbstractionCandidateTemplate valueOf(
      Map<Integer, List<MaterlisationStep>> abstractPointerToMaterlisationSteps) {

    return new GenericAbstractionCandidateTemplate(ImmutableMap.copyOf(abstractPointerToMaterlisationSteps));
  }

  public static GenericAbstractionCandidateTemplate valueOf(GenericAbstraction abstraction) {
    return new GenericAbstractionCandidateTemplate(abstraction.getMaterlisationStepMap());
  }

  public boolean matchesSpecificShape(@SuppressWarnings("unused") GenericAbstractionCandidateTemplate pTemplate) {
    return false;
  }

  public boolean isSpecificShape(GenericAbstractionCandidateTemplate pTemplate) {
    // TODO Real comparison.
    return pTemplate.abstractPointerToMaterlisationSteps == abstractPointerToMaterlisationSteps;
  }
}
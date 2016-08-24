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

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GenericAbstraction extends SMGObject implements SMGAbstractObject {

  /**
   * These maps contains as keys abstract pointers and as values concrete pointers.
   * For every abstract pointer that exist in the abstraction and points to/from
   * an abstract Objects that represents concrete regions in the smg, map to a concrete pointer
   * in the smg, that points to/from the represented region.
   */
  private final Map<Integer, Integer> abstractToConcretePointerMap;

  /**
   * This map contains as keys abstract pointers and as values a list of
   * materialisation steps. The abstract pointers represent concrete pointers
   * in a smg, that point to a concrete region that has yet to be materialized.
   *
   */
  private final Map<Integer, List<MaterlisationStep>> materlisationStepMap;

  protected GenericAbstraction(int pSize, String pLabel,
      Map<Integer, List<MaterlisationStep>> pMaterlisationSteps,
      Map<Integer, Integer> pAbstractToConcretePointerMap) {
    super(pSize, pLabel, SMGObjectKind.GENERIC);
    abstractToConcretePointerMap = ImmutableMap.copyOf(pAbstractToConcretePointerMap);
    materlisationStepMap = ImmutableMap.copyOf(pMaterlisationSteps);
  }

  public GenericAbstraction(int pSize, String pLabel,
      Map<Integer, List<MaterlisationStep>> pMaterlisationStepMap,
      Map<Integer, Integer> pAbstractToConcretePointerMap, int pNewLevel) {
    super(pSize, pLabel, pNewLevel, SMGObjectKind.GENERIC);
    abstractToConcretePointerMap = ImmutableMap.copyOf(pAbstractToConcretePointerMap);
    materlisationStepMap = ImmutableMap.copyOf(pMaterlisationStepMap);
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return false;
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return false;
  }

  public static GenericAbstraction valueOf(
      Map<Integer, List<MaterlisationStep>> pMaterlisationSteps,
      Map<Integer, Integer> pAbstractToConcretePointerMap) {
    return new GenericAbstraction(100, "generic abtraction ID " + SMGValueFactory.getNewValue(),
        pMaterlisationSteps, pAbstractToConcretePointerMap);
  }

  public List<SMG> materialize(SMG pSMG, int pointer) {
    List<MaterlisationStep> steps = getSteps(pointer);

    List<SMG> result = new ArrayList<>(steps.size());

    for (MaterlisationStep step : steps) {
      result.add(step.materialize(pSMG, abstractToConcretePointerMap));
    }

    return result;
  }

  private List<MaterlisationStep> getSteps(int pPointer) {

    for (Entry<Integer, Integer> entry : abstractToConcretePointerMap.entrySet()) {
      if (entry.getValue() == pPointer) {
        return materlisationStepMap.get(entry.getKey());
      }
    }
    throw new AssertionError();
  }

  public Map<Integer, List<MaterlisationStep>> getMaterlisationStepMap() {
    return materlisationStepMap;
  }

  public Map<Integer, Integer> getAbstractToConcretePointerMap() {
    return abstractToConcretePointerMap;
  }

  @Override
  public String toString() {
    return "Generic Abstraction:\n"
        + "pointersToThisAbstraction " + abstractToConcretePointerMap.toString() + "\n"
        + "pointersToThisAbstraction " + abstractToConcretePointerMap.toString() + "\n"
        + "materlisationSteps " + materlisationStepMap.toString();
  }

  public GenericAbstractionCandidateTemplate createCandidateTemplate() {
    return GenericAbstractionCandidateTemplate.valueOf(materlisationStepMap);
  }

  @Override
  public SMGObject copy() {
    return new GenericAbstraction(getSize(), getLabel(), materlisationStepMap, abstractToConcretePointerMap);
  }

  @Override
  public SMGObject copy(int pNewLevel) {
    return new GenericAbstraction(getSize(), getLabel(), materlisationStepMap, abstractToConcretePointerMap, pNewLevel);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    return false;
  }
}
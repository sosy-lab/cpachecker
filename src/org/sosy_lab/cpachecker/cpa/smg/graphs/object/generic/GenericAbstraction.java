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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class GenericAbstraction extends SMGObject implements SMGAbstractObject {

  /**
   * These maps contains as keys abstract pointers and as values concrete pointers. For every
   * abstract pointer that exist in the abstraction and points to/from an abstract Objects that
   * represents concrete regions in the smg, map to a concrete pointer in the smg, that points
   * to/from the represented region.
   */
  private final ImmutableMap<SMGValue, SMGValue> abstractToConcretePointerMap;

  /**
   * This map contains as keys abstract pointers and as values a list of materialisation steps. The
   * abstract pointers represent concrete pointers in a smg, that point to a concrete region that
   * has yet to be materialized.
   */
  private final ImmutableMap<SMGValue, List<MaterlisationStep>> materlisationStepMap;

  protected GenericAbstraction(
      int pSize,
      String pLabel,
      Map<SMGValue, List<MaterlisationStep>> pMaterlisationSteps,
      Map<SMGValue, SMGValue> pAbstractToConcretePointerMap) {
    super(pSize, pLabel, SMGObjectKind.GENERIC);
    abstractToConcretePointerMap = ImmutableMap.copyOf(pAbstractToConcretePointerMap);
    materlisationStepMap = ImmutableMap.copyOf(pMaterlisationSteps);
  }

  public GenericAbstraction(
      int pSize,
      String pLabel,
      Map<SMGValue, List<MaterlisationStep>> pMaterlisationStepMap,
      Map<SMGValue, SMGValue> pAbstractToConcretePointerMap,
      int pNewLevel) {
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
      MachineModel pMachineModel,
      Map<SMGValue, List<MaterlisationStep>> pMaterlisationSteps,
      Map<SMGValue, SMGValue> pAbstractToConcretePointerMap) {
    return new GenericAbstraction(
        100 * pMachineModel.getSizeofCharInBits(),
        "generic abtraction ID " + SMGCPA.getNewValue(),
        pMaterlisationSteps,
        pAbstractToConcretePointerMap);
  }

  public List<SMG> materialize(SMG pSMG, SMGValue pointer) {
    return Lists.transform(
        getSteps(pointer), step -> step.materialize(pSMG, abstractToConcretePointerMap));
  }

  private List<MaterlisationStep> getSteps(SMGValue pPointer) {

    for (Entry<SMGValue, SMGValue> entry : abstractToConcretePointerMap.entrySet()) {
      if (entry.getValue().equals(pPointer)) {
        return materlisationStepMap.get(entry.getKey());
      }
    }
    throw new AssertionError();
  }

  public ImmutableMap<SMGValue, List<MaterlisationStep>> getMaterlisationStepMap() {
    return materlisationStepMap;
  }

  public ImmutableMap<SMGValue, SMGValue> getAbstractToConcretePointerMap() {
    return abstractToConcretePointerMap;
  }

  @Override
  public String toString() {
    return "Generic Abstraction:\n"
        + "pointersToThisAbstraction " + abstractToConcretePointerMap + "\n"
        + "pointersToThisAbstraction " + abstractToConcretePointerMap + "\n"
        + "materlisationSteps " + materlisationStepMap;
  }

  public GenericAbstractionCandidateTemplate createCandidateTemplate(MachineModel pMachineModel) {
    return GenericAbstractionCandidateTemplate.valueOf(pMachineModel, materlisationStepMap);
  }

  @Override
  public SMGObject copy(int pNewLevel) {
    return new GenericAbstraction(getSize(), getLabel(), materlisationStepMap, abstractToConcretePointerMap, pNewLevel);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    return false;
  }

  @Override
  public <T> T accept(SMGObjectVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {
    throw new UnsupportedOperationException("GenericAbstraction does not join"); // TODO why not?
  }

  @Override
  public boolean isAbstract() {
    throw new UnsupportedOperationException("GenericAbstraction does not know if it is abstract?"); // TODO why not?
  }
}

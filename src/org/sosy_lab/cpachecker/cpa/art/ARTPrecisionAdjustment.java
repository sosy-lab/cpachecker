/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.Map;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationClass;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class ARTPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;

  public ARTPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Triple<AbstractElement, Precision, Action> prec(AbstractElement pElement, Precision pPrecision, UnmodifiableReachedSet pElements) throws CPAException {

    Preconditions.checkArgument(pElement instanceof ARTElement);
    ARTElement element = (ARTElement)pElement;
    ARTPrecision prec = (ARTPrecision) pPrecision;

    UnmodifiableReachedSet elements = new UnmodifiableReachedSetView(
        pElements,  ARTElement.getUnwrapFunction(), Functions.<Precision>identity());

    // if location classes match partitions, then precision for location doesn't have to be adjusted
    ImmutableMap<Integer, RGLocationClass> oldLocClasses = element.getLocationClasses();
    RGLocationMapping lm = prec.getLocationMapping();
    boolean rebuild =  !lm.getParitioning().containsAll(oldLocClasses.values());

    AbstractElement oldElement = element.getWrappedElement();
    Precision oldWrappedPrecision = prec.getWrappedPrecision();
    Triple<AbstractElement, Precision, Action> unwrappedResult = wrappedPrecAdjustment.prec(oldElement, oldWrappedPrecision, elements);

    AbstractElement newElement = unwrappedResult.getFirst();
    Precision newWrappedPrecision = unwrappedResult.getSecond();
    Action action = unwrappedResult.getThird();

    if ((oldElement == newElement) && (oldWrappedPrecision == newWrappedPrecision) && !rebuild) {
      // nothing has changed
      return new Triple<AbstractElement, Precision, Action>(pElement, pPrecision, action);
    }

    Map<ARTElement, CFAEdge> parents = element.getLocalParentMap();
    Map<ARTElement, RGEnvTransition> envParents = element.getEnvParentMap();

    // adjust the precison of the location classes
    ImmutableMap<Integer, RGLocationClass> newLocClasses;
    if (rebuild){
      // paritions in the precision and in the element are different
      Builder<Integer, RGLocationClass> bldr = ImmutableMap.<Integer, RGLocationClass>builder();

      for (Integer tid : oldLocClasses.keySet()){
        RGLocationClass oldLocClass = oldLocClasses.get(tid);
        RGLocationClass newLocClass = lm.findSubsumingLocationMapping(oldLocClass.getClassNodes());
        if (newLocClass == null){
          System.out.println();
        }
        assert newLocClass != null;
        bldr = bldr.put(tid, newLocClass);
      }

      newLocClasses = bldr.build();

    } else {
      newLocClasses = oldLocClasses;
    }

    ARTElement resultElement = new ARTElement(newElement, parents, envParents, newLocClasses, element.getTid());
    resultElement.setDistanceFromRoot(element.getDistanceFromRoot());
    resultElement.setEnvApplied(element.getEnvApplied());


    Map<ARTElement, CFAEdge> localChildren = element.getLocalChildMap();
    Map<ARTElement, RGEnvTransition> envChildren = element.getEnvChildMap();
    resultElement.addLocalChildren(localChildren);
    resultElement.addEnvChildren(envChildren);

    // first copy list of covered elements, then remove element from ART, then set elements covered by new element
    ImmutableList<ARTElement> coveredElements = ImmutableList.copyOf(element.getCoveredByThis());
    element.removeFromART();

    for (ARTElement covered : coveredElements) {
      covered.setCovered(resultElement);
    }

    ARTPrecision newPrec = new ARTPrecision(prec.getLocationMapping(), (CompositePrecision) newWrappedPrecision);
    return new Triple<AbstractElement, Precision, Action>(resultElement, newPrec, action);
  }
}
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
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.util.assumptions.FormulaReportingElement;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * AbstractElement for Symbolic Predicate Abstraction CPA
 */
public class PredicateAbstractElement implements AbstractElement, Partitionable, FormulaReportingElement {

  public static Predicate<AbstractElement> FILTER_ABSTRACTION_ELEMENTS = new Predicate<AbstractElement>() {
    @Override
    public boolean apply(AbstractElement ae) {
      return (ae instanceof AbstractionElement);
    }
  };

  /**
   * Marker type for abstract elements that were generated by computing an
   * abstraction.
   */
  public static class AbstractionElement extends PredicateAbstractElement {

    public AbstractionElement(PathFormula pf, AbstractionFormula pA) {
      super(pf, pA);
      // Check whether the pathFormula of an abstraction element is just "true".
      // partialOrder relies on this for optimization.
      Preconditions.checkArgument(pf.getFormula().isTrue());
    }

    @Override
    public Object getPartitionKey() {
      if (super.abstractionFormula.asFormula().isFalse()) {
        // put unreachable states in a separate partition to avoid merging
        // them with any reachable states
        return Boolean.FALSE;
      } else {
        return null;
      }
    }

    @Override
    public String toString() {
      return "Abstraction location: true, Abstraction: " + super.abstractionFormula;
    }
  }


  public static class ComputeAbstractionElement extends PredicateAbstractElement {

    private final CFANode location;

    public ComputeAbstractionElement(PathFormula pf, AbstractionFormula pA, CFANode pLoc) {
      super(pf, pA);
      location = pLoc;
    }

    @Override
    public Object getPartitionKey() {
      return this;
    }

    @Override
    public String toString() {
      return "Abstraction location: true, Abstraction: <TO COMPUTE>";
    }

    public CFANode getLocation() {
      return location;
    }
  }

  /** The path formula for the path from the last abstraction node to this node.
   * it is set to true on a new abstraction location and updated with a new
   * non-abstraction location */
  private final PathFormula pathFormula;

  /** The abstraction which is updated only on abstraction locations */
  private final AbstractionFormula abstractionFormula;

  /**
   * The abstract element this element was merged into.
   * Used for fast coverage checks.
   */
  private PredicateAbstractElement mergedInto = null;

  public PredicateAbstractElement(PathFormula pf, AbstractionFormula a) {
    this.pathFormula = pf;
    this.abstractionFormula = a;
  }

  public AbstractionFormula getAbstractionFormula() {
    return abstractionFormula;
  }

  public PredicateAbstractElement getMergedInto() {
    return mergedInto;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  void setMergedInto(PredicateAbstractElement pMergedInto) {
    Preconditions.checkNotNull(pMergedInto);
    mergedInto = pMergedInto;
  }

  @Override
  public String toString() {
    return "Abstraction location: false";
  }

  @Override
  public Object getPartitionKey() {
    return abstractionFormula;
  }

  @Override
  public Formula getFormulaApproximation(FormulaManager manager) {
    return getAbstractionFormula().asFormula();
  }

}
/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.UninstantiatingInterpolationManager;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class ImpactRefiner extends org.sosy_lab.cpachecker.cpa.impact.ImpactRefiner {

  public static ImpactRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(ImpactRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    Region initialRegion = predicateCpa.getInitialElement(null).getAbstractionFormula().asRegion();
    if (!(initialRegion instanceof SymbolicRegionManager.SymbolicRegion)) {
      throw new InvalidConfigurationException(ImpactRefiner.class.getSimpleName() + " works only with a PredicateCPA configured to store abstractions as formulas (cpa.predicate.abstraction.type=FORMULA)");
    }

    Configuration config = predicateCpa.getConfiguration();
    LogManager logger = predicateCpa.getLogger();
    ExtendedFormulaManager fmgr = predicateCpa.getFormulaManager();
    Solver solver = predicateCpa.getSolver();

    InterpolationManager<Formula> manager = new UninstantiatingInterpolationManager(
                                                  fmgr,
                                                  predicateCpa.getPathFormulaManager(),
                                                  solver,
                                                  predicateCpa.getFormulaManagerFactory(),
                                                  config, logger);

    return new ImpactRefiner(config, logger, pCpa, manager, fmgr, solver);
  }

  private ImpactRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa,
      final InterpolationManager<Formula> pInterpolationManager,
      final ExtendedFormulaManager pFmgr, final Solver pSolver) throws InvalidConfigurationException, CPAException {

    super(config, logger, pCpa, pInterpolationManager, pFmgr, pSolver);
  }

  @Override
  protected List<ARTElement> transformPath(Path pPath) {
    // filter abstraction elements

    List<ARTElement> result = ImmutableList.copyOf(
        Iterables.filter(
            Iterables.transform(
                skip(pPath, 1),
                Pair.<ARTElement>getProjectionToFirst()),

            new Predicate<ARTElement>() {
                @Override
                public boolean apply(ARTElement pInput) {
                  return extractElementByType(pInput, PredicateAbstractElement.class).isAbstractionElement();
                }
            }));

    assert pPath.getLast().getFirst() == result.get(result.size()-1);
    return result;
  }

  @Override
  protected List<Formula> getFormulasForPath(List<ARTElement> pPath, ARTElement pInitialElement) {

    return transform(pPath,
        new Function<ARTElement, Formula>() {
          @Override
          public Formula apply(ARTElement e) {
            return extractElementByType(e, PredicateAbstractElement.class).getAbstractionFormula().getBlockFormula();
          }
        });
  }

  @Override
  protected void addFormulaToState(Formula f, ARTElement e) {
    PredicateAbstractElement predElement = AbstractElements.extractElementByType(e, PredicateAbstractElement.class);
    AbstractionFormula af = predElement.getAbstractionFormula();

    Formula newFormula = fmgr.makeAnd(f, af.asFormula());
    Formula instantiatedNewFormula = fmgr.instantiate(newFormula, predElement.getPathFormula().getSsa());
    AbstractionFormula newAF = new AbstractionFormula(new SymbolicRegionManager.SymbolicRegion(newFormula), newFormula, instantiatedNewFormula, af.getBlockFormula());
    predElement.setAbstraction(newAF);
  }

  @Override
  protected Formula getStateFormula(ARTElement pARTElement) {
    return AbstractElements.extractElementByType(pARTElement, PredicateAbstractElement.class).getAbstractionFormula().asFormula();
  }
}
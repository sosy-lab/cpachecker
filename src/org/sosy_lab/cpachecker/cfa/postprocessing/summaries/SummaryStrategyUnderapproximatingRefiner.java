// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.ast.factories.TypeFactory;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Strategy.StrategyQualifier;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

public class SummaryStrategyUnderapproximatingRefiner implements Refiner {

  private final LogManager logger;
  private int refinementNumber;
  protected final ARGCPA argCpa;

  private SummaryInformation summaryInformation;

  private SummaryStrategyUnderapproximatingRefiner(
      LogManager pLogger, final ConfigurableProgramAnalysis pCpa, CFA pCfa)
      throws InvalidConfigurationException {
    logger = pLogger;
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);
    summaryInformation = pCfa.getSummaryInformation().orElseThrow();
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    ARGCPA argCpa =
        CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SummaryStrategyUnderapproximatingRefiner.class);
    LogManager logger = argCpa.getLogger();

    return new SummaryStrategyUnderapproximatingRefiner(logger, pCpa, argCpa.getCfa());
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINE, "Refining underapproximating loopsummary strategies");
    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match before refinement";

    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa, refinementNumber++);

    Optional<AbstractState> optionalRefinementState = Optional.empty();
    Optional<GhostCFA> optionalStrategy = Optional.empty();

    // TODO: Make the search for a underapproximating state smarter. This would imply getting
    // something like craig interpolants but for proofs.
    for (AbstractState state : reached.asReachedSet()) {
      LocationPrecision locationPrecision =
          ((WrapperPrecision) pReached.getPrecision(state))
              .retrieveWrappedPrecision(LocationPrecision.class);
      if (locationPrecision.getCurrentStrategy().isPresent()
          && locationPrecision.getCurrentStrategy().orElseThrow().getStrategyQualifier()
              == StrategyQualifier.Underapproximating) {
        optionalRefinementState = Optional.of(state);
        optionalStrategy = Optional.of(locationPrecision.getCurrentStrategy().orElseThrow());
        break;
      }
    }

    if (optionalRefinementState.isEmpty() || optionalStrategy.isEmpty()) {
      return false;
    } else {
      ARGState refinementState = (ARGState) optionalRefinementState.orElseThrow();
      LocationPrecision locationPrecision =
          ((WrapperPrecision) pReached.getPrecision(refinementState))
              .retrieveWrappedPrecision(LocationPrecision.class);

      // Add forbidden strategies. This must be a since the parameters may be updated.
      locationPrecision.addForbiddenStrategy(
          locationPrecision.getCurrentStrategy().orElseThrow().copy());

      // Set the new best strategy
      Optional<GhostCFA> nextStrategy = getNextStrategy(refinementState, locationPrecision);
      locationPrecision.setCurrentStrategy(nextStrategy);

      // Using reached.removeSubtree does not remove only the children elements, but also the
      // element itself. Which in turn also removes the updated precision
      List<ARGState> children = Lists.newArrayList(refinementState.getChildren());

      for (int i = 0; i < children.size(); i++) {
        reached.removeSubtree(children.get(i));
      }

      return true;
    }
  }

  private Optional<GhostCFA> getNextStrategy(
      ARGState refinementState, LocationPrecision locationPrecision) {
    // TODO: Improve this by making it more dynamic and refactor it into some other class. This is
    // currently here only for a proof of concept.

    Optional<GhostCFA> currentStrategyBeingUsedOptional = locationPrecision.getCurrentStrategy();

    // TODO: This is an ugly hack since it is hardcoded
    if (currentStrategyBeingUsedOptional.isPresent()) {
      GhostCFA currentStrategyBeingUsed = currentStrategyBeingUsedOptional.get();
      if (currentStrategyBeingUsed.getStrategy()
          == StrategiesEnum.NONDETVARIABLEASSIGNMENTSTRATEGY) {

        // Get the type of the variable to be replaced. Since there is only a single variable this
        // is easy to do.
        ALeftHandSide parameterVariable = currentStrategyBeingUsed.getParameterVariables().get(0);
        CType typeOfVariable;
        if (parameterVariable instanceof AVariableDeclaration) {
          typeOfVariable = (CType) ((AVariableDeclaration) parameterVariable).getType();
        } else if (parameterVariable instanceof AArraySubscriptExpression) {
          typeOfVariable =
              (CType)
                  ((AArrayType)
                          ((AArraySubscriptExpression) parameterVariable)
                              .getArrayExpression()
                              .getExpressionType())
                      .getType();
        } else {
          return Optional.empty();
        }


        // Get the already used values
        List<Number> usedValues = new ArrayList<>();
        for (List<AExpression> paramList : currentStrategyBeingUsed.getUsedParameters()) {
          // For this strategy there exists only a single parameter
          AExpression parameter = paramList.get(0);
          if (parameter instanceof CIntegerLiteralExpression) {
            usedValues.add(((CIntegerLiteralExpression) parameter).asLong());
          } else if (parameter instanceof CFloatLiteralExpression) {
            usedValues.add(((CFloatLiteralExpression) parameter).getValue().doubleValue());
          }
        }

        // Get the next value to be used
        List<Number> possibleVariableValuesIntegers =
            List.of(
                Long.valueOf(0),
                Long.valueOf(1),
                Long.valueOf(-1),
                Long.valueOf(TypeFactory.getUpperLimit(typeOfVariable).longValue()),
                Long.valueOf(TypeFactory.getLowerLimit(typeOfVariable).longValue()));

        List<Number> possibleVariableValuesDecimal =
            List.of(
                Double.valueOf(0),
                Double.valueOf(1),
                Double.valueOf(-1),
                Double.valueOf(TypeFactory.getUpperLimit(typeOfVariable).doubleValue()),
                Double.valueOf(TypeFactory.getLowerLimit(typeOfVariable).doubleValue()));

        List<Number> possibleVariableValues = new ArrayList<>();

        if (usedValues.get(0) instanceof Long) {
          possibleVariableValues = possibleVariableValuesIntegers;
        } else if (usedValues.get(0) instanceof Double) {
          possibleVariableValues = possibleVariableValuesDecimal;
        }

        for (Number n : possibleVariableValues) {
          if (!usedValues.contains(n)) {
            currentStrategyBeingUsed.updateParameters(
                List.of(new AExpressionFactory().from(n, typeOfVariable).build()));
            return Optional.of(currentStrategyBeingUsed);
          }
        }

      } else if (currentStrategyBeingUsed.getStrategy()
          == StrategiesEnum.BOUNDEDLOOPUNROLLINGSTRATEGY) {
        // TODO: improve this by determining the amount of unrollings at runtime instead of being
        // hard coded like this.
        List<Integer> possibleBoundsForUnrolling = List.of(0, 1, 2, 3, 4, 8, 16, 32);

        // Get the type of the variable to be replaced. Since there is only a single variable this
        // is easy to do.
        ALeftHandSide parameterVariable = currentStrategyBeingUsed.getParameterVariables().get(0);
        CType typeOfVariable;
        if (parameterVariable instanceof AVariableDeclaration) {
          typeOfVariable = (CType) ((AVariableDeclaration) parameterVariable).getType();
        } else if (parameterVariable instanceof AArraySubscriptExpression) {
          typeOfVariable =
              (CType)
                  ((AArraySubscriptExpression) parameterVariable)
                      .getArrayExpression()
                      .getExpressionType();
        } else {
          return Optional.empty();
        }

        // Get the already used values
        List<Integer> usedValues = new ArrayList<>();
        for (List<AExpression> paramList : currentStrategyBeingUsed.getUsedParameters()) {
          // For this strategy there exists only a single parameter
          AExpression parameter = paramList.get(0);
          if (parameter instanceof CIntegerLiteralExpression) {
            usedValues.add((int) ((CIntegerLiteralExpression) parameter).asLong());
          }
        }

        for (Integer n : possibleBoundsForUnrolling) {
          if (!usedValues.contains(n)) {
            currentStrategyBeingUsed.updateParameters(
                List.of(new AExpressionFactory().from(n, typeOfVariable).build()));
            return Optional.of(currentStrategyBeingUsed);
          }
        }
      }
    }

    return summaryInformation.getBestAllowedStrategy(
        AbstractStates.extractLocation(refinementState), locationPrecision);
  }
}

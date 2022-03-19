// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import com.google.common.collect.Multimap;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.BinaryConstraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.UnaryConstraint;

public class VariableTrackingConstraintsPrecision implements ConstraintsPrecision {
  Multimap<String, String> trackedFunctions;
  Multimap<CFANode, String> trackedLocations;
  Set<String> trackedGlobal;
  ConstraintsPrecision constraintsPrecision;


  public VariableTrackingConstraintsPrecision(
      Multimap<String, String> pTrackedFunctions,
      Multimap<CFANode, String> pTrackedLocations,
      Set<String> pTrackedGlobal,
      ConstraintsPrecision pConstraintsPrecision) {

    trackedFunctions = pTrackedFunctions;
    trackedLocations = pTrackedLocations;
    trackedGlobal = pTrackedGlobal;
    constraintsPrecision = pConstraintsPrecision;
  }

  @Override
  public boolean isTracked(Constraint pConstraint, CFANode pLocation) {
    //verify if pConstraint is Unary or BinaryConstraint
    if (pConstraint instanceof UnaryConstraint) {
      //check if operand in pConstraint matches tracked variable
      String operand = ((UnaryConstraint) pConstraint).getOperand().getRepresentation();
      for (String var : trackedFunctions.get(pLocation.getFunctionName())) {
        if (operand.contains(var)) {
          return true;
        }
      }
      for (String var : trackedLocations.get(pLocation)) {
        if (operand.equals(var)) {
          return true;
        }
      }
      for (String var : trackedGlobal) {
        if (operand.equals(var)) {
          return true;
        }
      }
    }
    if (pConstraint instanceof BinaryConstraint) {
      //check if operandOne or operandTwo matches a tracked variable
      String operandOne = ((BinaryConstraint) pConstraint).getOperand1().getRepresentation();
      String operandTwo = ((BinaryConstraint) pConstraint).getOperand2().getRepresentation();
      for (String var : trackedFunctions.get(pLocation.getFunctionName())) {
        if (operandOne.equals(var) || operandTwo.equals(var)) {
          return true;
        }
      }
      for (String var : trackedLocations.get(pLocation)) {
        if (operandOne.equals(var) || operandTwo.equals(var)) {
          return true;
        }
      }
      for (String var : trackedGlobal) {
        if (operandOne.equals(var) || operandTwo.equals(var)) {
          return true;
        }
      }
    }
    // if operand(s) of pConstraints finds no match in tracked variables, check if pConstraints
    // matches a Constraint of ConstraintPrecision
    return constraintsPrecision.isTracked(pConstraint, pLocation);
  }

  @Override
  public ConstraintsPrecision join(ConstraintsPrecision pOther) {
    VariableTrackingConstraintsPrecision other = (VariableTrackingConstraintsPrecision) pOther;
    return new VariableTrackingConstraintsPrecision(trackedFunctions, trackedLocations,
        trackedGlobal, constraintsPrecision.join(other.getConstraintsPrecision()));
  }

  public ConstraintsPrecision getConstraintsPrecision() {
    return constraintsPrecision;
  }

  @Override
  public ConstraintsPrecision withIncrement(Increment pIncrement) {
    return new VariableTrackingConstraintsPrecision(trackedFunctions, trackedLocations,
        trackedGlobal, constraintsPrecision.withIncrement(pIncrement));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("VariableTrackingConstraintsPrecision [ \n");
    sb.append(constraintsPrecision.toString());
    sb.append("\nwith tracked variables: ");
    if (!trackedLocations.isEmpty()) {
      sb.append("\n\tLocations: ");
      sb.append(trackedLocations.toString());
    }
    if (!trackedFunctions.isEmpty()) {
      sb.append("\n\tFunctions: ");
      sb.append(trackedFunctions.toString());
    }
    if (!trackedGlobal.isEmpty()) {
      sb.append("\n\tGlobal: ");
      sb.append(trackedGlobal);
    }
    return sb.append("]").toString();
  }
}

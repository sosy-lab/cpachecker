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
package org.sosy_lab.cpachecker.util.ci.translators;

import gmp.Mpfr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.apron.ApronState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import apron.Coeff;
import apron.DoubleScalar;
import apron.MpfrScalar;
import apron.MpqScalar;
import apron.Scalar;
import apron.Tcons0;
import apron.Texpr0BinNode;
import apron.Texpr0CstNode;
import apron.Texpr0DimNode;
import apron.Texpr0Node;
import apron.Texpr0UnNode;

import com.google.common.collect.Sets;
import com.google.common.math.DoubleMath;


public class ApronRequirementsTranslator extends CartesianRequirementsTranslator<ApronState> {

  private Pair<ApronState, Collection<String>> stateToRequiredVars;

  public ApronRequirementsTranslator(Class<ApronState> pAbstractStateClass, LogManager pLog) {
    super(pAbstractStateClass, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(ApronState pRequirement) {
    Collection<String> result = getConvexHullRequiredVars(pRequirement, null); // TODO
    stateToRequiredVars = Pair.of(pRequirement, result);
    return new ArrayList<>(result);
  }

  @Override
  protected List<String> getVarsInRequirements(final ApronState pRequirement, final @Nullable Collection<String> pRequiredVars) {
    Collection<String> result = getConvexHullRequiredVars(pRequirement, pRequiredVars);
    stateToRequiredVars = Pair.of(pRequirement, result);
    return new ArrayList<>(result);
  }

  @Override
  protected List<String> getListOfIndependentRequirements(ApronState pRequirement, SSAMap pIndices, final @Nullable Collection<String> pRequiredVars) {
    List<String> result = new ArrayList<>();
    List<String> varNames = getAllVarNames(pRequirement);
    Collection<String> requiredVarNames = null;

    if(stateToRequiredVars != null) {
      requiredVarNames = stateToRequiredVars.getSecond();
      if(stateToRequiredVars.getFirst() != pRequirement) {
        requiredVarNames = null;
      }
    }

    if(requiredVarNames == null) {
      requiredVarNames = getConvexHullRequiredVars(pRequirement, null); // TODO
    }

    String converted;
    for(Tcons0 constraint: pRequirement.getApronNativeState().toTcons(pRequirement.getManager().getManager())) {
      converted = convertConstraintToFormula(constraint, varNames, pIndices, requiredVarNames);
      if(converted != null) {
        result.add(converted);
      }
    }

    return result;
  }

  private List<String> getAllVarNames(final ApronState pRequirement) {
    List<String> result = new ArrayList<>(pRequirement.getIntegerVariableToIndexMap().size()
        + pRequirement.getRealVariableToIndexMap().size());

    for (MemoryLocation mem : pRequirement.getIntegerVariableToIndexMap()) {
      result.add(mem.getAsSimpleString());
    }

    for (MemoryLocation mem : pRequirement.getRealVariableToIndexMap()) {
      result.add(mem.getAsSimpleString());
    }

    return result;
  }


  private Collection<String> getConvexHullRequiredVars(final ApronState pRequirement,
      final @Nullable Collection<String> requiredVars) {
    Set<String> seenRequired = new HashSet<>();
    Set<String> required;
    if (requiredVars == null) {
      required = new HashSet<>();
    } else {
      required = new HashSet<>(requiredVars);
    }
    List<String> varNames = getAllVarNames(pRequirement);
    Tcons0[] constraints = pRequirement.getApronNativeState().toTcons(pRequirement.getManager().getManager());
    List<Set<String>> constraintVars = new ArrayList<>(constraints.length);

    for (Tcons0 constraint : constraints) {
      constraintVars.add(getVarsInConstraint(constraint, varNames));
    }

    Iterator<Set<String>> it = constraintVars.iterator();

    int setSize;
    Set<String> intermediate;

    while(it.hasNext()) {
      intermediate = it.next();
      if(!Sets.intersection(required, intermediate).isEmpty()) {
        setSize = seenRequired.size();
        seenRequired.addAll(intermediate);
        required.addAll(intermediate);

        if(setSize != seenRequired.size()) {
          it = constraintVars.iterator();
        }
      }
    }

    return seenRequired;
  }


  private Set<String> getVarsInConstraint(final Tcons0 constraint, final List<String> varNames) {
    Set<String> vars = Sets.newHashSetWithExpectedSize(constraint.getSize());

    Deque<Texpr0Node> stack = new ArrayDeque<>();
    stack.push(constraint.getExpression().toTexpr0Node());

    Texpr0Node current;
    while (!stack.isEmpty()) {
      current = stack.pop();

      if (current instanceof Texpr0BinNode) {
        stack.push(((Texpr0BinNode) current).getLeftArgument());
        stack.push(((Texpr0BinNode) current).getRightArgument());
      } else if (current instanceof Texpr0UnNode) {
        stack.push(((Texpr0UnNode) current).getArgument());
      } else if (current instanceof Texpr0DimNode) {
        vars.add(varNames.get(((Texpr0DimNode) current).dim));
      }
    }
    return vars;
  }

  private @Nullable String convertConstraintToFormula(final Tcons0 constraint, final List<String> varNames,
      final SSAMap map, final Collection<String> varsConsidered) {
    StringBuilder sb = new StringBuilder();
    sb.append("(");

    switch (constraint.kind) {
    case Tcons0.EQ:
      sb.append("=");
      break;
    case Tcons0.SUPEQ:
      sb.append(">=");
      break;
    case Tcons0.SUP:
      sb.append(">");
      break;
    case Tcons0.DISEQ:
      sb.append("not (=");
      break;
    case Tcons0.EQMOD:
      sb.append("= (mod");
      break;
    default:
      assert (false);
    }

    String left = convertLeftConstraintPartToFormula(varNames, map, varsConsidered);
    if(left == null) {
      return null;
    }

    sb.append(left);

    if (constraint.kind == Tcons0.EQMOD) {
      sb.append(" ");
      sb.append(getIntegerValFromScalar(constraint.getScalar()));
      sb.append(")");
    }

    sb.append(" 0)");

    if(constraint.kind == Tcons0.DISEQ) {
      sb.append(")");
    }

    return sb.toString();
  }

  private String getIntegerValFromScalar(final Scalar pCst) {
    double value;
    if (pCst instanceof DoubleScalar) {
      value = ((DoubleScalar) pCst).get();
    } else if (pCst instanceof MpfrScalar) {
      value = ((MpfrScalar) pCst).get().doubleValue(Mpfr.RNDN);
    } else {
      assert (pCst instanceof MpqScalar);
      value = ((MpqScalar) pCst).get().doubleValue();
    }
    if (DoubleMath.isMathematicalInteger(value)) {
      return String.valueOf((int) value);
    }
    throw new AssertionError("Cannot deal with this non-integer scalar");
  }

  private @Nullable String convertLeftConstraintPartToFormula(final List<String> varNames,
      final SSAMap map, final Collection<String> varsConsidered) {
    boolean toConsider = false;
    StringBuilder sb = new StringBuilder();

    Deque<Pair<Texpr0Node, Integer>> stack = new ArrayDeque<>();

    Pair<Texpr0Node, Integer> currentPair;
    Texpr0Node current;
    Coeff cst;
    String variableName;
    while (!stack.isEmpty()) {
      currentPair = stack.pop();
      current = currentPair.getFirst();

      if (current instanceof Texpr0BinNode) {
        sb.append(" (");

        switch (((Texpr0BinNode) current).getOperation()) {
        case Texpr0BinNode.OP_ADD:
          sb.append("+");
          break;
        case Texpr0BinNode.OP_SUB:
          sb.append("-");
          break;
        case Texpr0BinNode.OP_MUL:
          sb.append("*");
          break;
        case Texpr0BinNode.OP_DIV:
          sb.append("/");
          break;
        case Texpr0BinNode.OP_MOD:
          sb.append("mod");
          break;
        default:
          throw new AssertionError("Unsupported binary operator.");
        }

        stack.push(Pair.of(((Texpr0BinNode) current).getLeftArgument(), 0));
        stack.push(Pair.of(((Texpr0BinNode) current).getRightArgument(), currentPair.getSecond() + 1));
      } else if (current instanceof Texpr0UnNode) {

        switch (((Texpr0UnNode) current).getOperation()) {
        case Texpr0UnNode.OP_NEG:
          sb.append(" (-");
          break;
        default:
          throw new AssertionError("Unsupported unary operator.");
        }

        stack.push(Pair.of(((Texpr0UnNode) current).getArgument(), currentPair.getSecond() + 1));
      } else {
        if (current instanceof Texpr0DimNode) {
          variableName = varNames.get(((Texpr0DimNode) current).dim);
          if (varsConsidered.contains(variableName)) {
            toConsider = true;
          }
          sb.append(" ");
          sb.append(getVarWithIndex(variableName, map));
        } else if (current instanceof Texpr0CstNode) {
          cst = ((Texpr0CstNode) current).getConstant();

          if (cst.isScalar()) {
            sb.append(" ");
            sb.append(getIntegerValFromScalar((Scalar) cst));
          }

          throw new AssertionError("Cannot handle coefficient");
        }
        addClosingRoundBrackets(sb, currentPair.getSecond());
      }
    }
    return toConsider ? sb.toString() : null;
  }

  private void addClosingRoundBrackets(final StringBuilder sb, final int number) {
    for (int i = 0; i < number; i++) {
      sb.append(")");
    }
  }

}

/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.propertyscope;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;


public class PropertyScopeUtil {

  private static final Pattern formulaVariablePattern =
      Pattern.compile("(?:(?<function>[^:]+)::)?(?<variable>[^@]+)(?:@(?<ssaindex>[0-9]+))?");

  static Stream<List<ARGState>> allPathStream(ARGState root) {
    Builder<List<ARGState>> reachedsb = Stream.builder();

    Deque<List<ARGState>> waitlist = new ArrayDeque<>();
    waitlist.add(Lists.newArrayList(root));

    while (!waitlist.isEmpty()) {
      List<ARGState> currPath = waitlist.removeFirst();
      ARGState currState = currPath.get(currPath.size() - 1);
      Collection<ARGState> children = currState.getChildren();
      if (children.size() == 0) {
        reachedsb.accept(Collections.unmodifiableList(currPath));
      } else if (children.size() == 1) {
        currPath.add(children.stream().findFirst().get());
        waitlist.addFirst(currPath);
      } else {
        children.stream().forEach(child -> {
          ArrayList<ARGState> newPath = new ArrayList<>(currPath);
          newPath.add(child);
          waitlist.addFirst(newPath);
        });
      }
    }
    return reachedsb.build();
  }

  public static class FormulaVariableResult {

    public final String function;
    public final String variable;
    public final int ssaIndex;

    public FormulaVariableResult(String pFunction, String pVariable, int pSsaIndex) {
      function = pFunction;
      variable = pVariable;
      ssaIndex = pSsaIndex;
    }

    public FormulaVariableResult withoutSSA() {
      return new FormulaVariableResult(function, variable, -1);
    }

    public boolean isGlobal() {
      return function == null;
    }

    public boolean equalsIgnoreSSA(FormulaVariableResult other) {
      return other != null && Objects.equals(this.variable, other.variable) &&
          Objects.equals(this.function, other.function);
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      FormulaVariableResult that = (FormulaVariableResult) pO;
      return ssaIndex == that.ssaIndex &&
          Objects.equals(function, that.function) &&
          Objects.equals(variable, that.variable);
    }

    @Override
    public int hashCode() {
      return Objects.hash(function, variable, ssaIndex);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("");
      if (function != null) {
        sb.append(function).append("::");
      }
      sb.append(variable);

      if (ssaIndex >= 0) {
        sb.append("@").append(ssaIndex);
      }

      return sb.toString();


    }

  }

  public static FormulaVariableResult splitFormulaVariable(String variable) {
    Matcher res = formulaVariablePattern.matcher(variable);
    res.matches();
    String ssaidxstr = res.group("ssaindex");
    int ssaidx = ssaidxstr == null ? -1 : Integer.parseInt(ssaidxstr);

    return new FormulaVariableResult(res.group("function"), res.group("variable"), ssaidx);

  }

  ;

  public static Stream<FormulaVariableResult> formulaVariableSplitStream(
      AbstractState ast, FormulaManagerView fmgr) {
    PredicateAbstractState past = extractStateByType(ast, PredicateAbstractState.class);
    return fmgr.extractVariableNames(past.getAbstractionFormula().asInstantiatedFormula()).stream()
        .map(PropertyScopeUtil::splitFormulaVariable);

  }

  static Optional<PredicateAbstractState> asNonTrueAbstractionState(AbstractState pState) {
    PredicateAbstractState predState = extractStateByType(pState, PredicateAbstractState.class);
    if (predState != null && predState.isAbstractionState() &&
        !predState.getAbstractionFormula().isTrue()) {
      return Optional.of(predState);
    }

    return Optional.empty();
  }

  public static class FormulaGlobalsInspector {

    public final ImmutableSet<BooleanFormula> atoms;
    public final Set<FormulaVariableResult> globalVariablesInAtoms = new HashSet<>();
    public final List<BooleanFormula> globalAtoms = new ArrayList<>();
    public final List<BooleanFormula> globalConstantAtoms = new ArrayList<>();
    public final List<BooleanFormula> globalLoopIncDecAtoms = new ArrayList<>();
    public final List<BooleanFormula> globalLoopExitCondAtoms = new ArrayList<>();

    private final FormulaManagerView fmgr;

    public FormulaGlobalsInspector(
        FormulaManagerView fmgr, BooleanFormula instform, Optional<Set<String>>
        pLoopIncDecVariables, Optional<Set<String>> pLoopExitCondVars) {
      this.fmgr = fmgr;
      atoms = fmgr.extractAtoms(instform, false);

      Set<FormulaVariableResult> loopIncDecVariablesRes =
          pLoopIncDecVariables.orElse
              (Collections.emptySet())
              .stream()
              .map(PropertyScopeUtil::splitFormulaVariable).collect(Collectors.toSet());
      Set<FormulaVariableResult> loopExitCondVarsRes =
          pLoopExitCondVars.orElse(Collections.emptySet())
              .stream()
              .map(PropertyScopeUtil::splitFormulaVariable).collect(Collectors.toSet());

      for (BooleanFormula atom : atoms) {
        Visitor visitor = new Visitor();
        fmgr.visit(visitor, atom);

        if (visitor.vars.stream().anyMatch(FormulaVariableResult::isGlobal)) {
          globalAtoms.add(atom);
        }

        visitor.vars.stream().filter(FormulaVariableResult::isGlobal).forEach(varres -> {
          globalVariablesInAtoms.add(varres.withoutSSA());
        });

        if (visitor.vars.size() > 0 && visitor.vars.stream().allMatch
            (FormulaVariableResult::isGlobal) && visitor.constants.size() > 0) {
          globalConstantAtoms.add(atom);
        }

        if (isGlobalVariableRelation(visitor, loopIncDecVariablesRes)) {
          globalLoopIncDecAtoms.add(atom);
        }

        if (isGlobalVariableRelation(visitor, loopExitCondVarsRes)) {
          globalLoopExitCondAtoms.add(atom);
        }

      }
    }

    private boolean isGlobalVariableRelation(
        Visitor visitor,
        Set<FormulaVariableResult> toTestVariables) {

      Set<FormulaVariableResult> testCandidates = visitor.vars.stream()
          .filter(fvr -> toTestVariables.stream()
              .anyMatch(fvr2 -> fvr2.equalsIgnoreSSA(fvr)))
          .collect(Collectors.toSet());

      long globalOrCandidate = visitor.vars.stream()
          .filter(varRes -> !varRes.isGlobal() && !testCandidates.contains(varRes))
          .count();

      return globalOrCandidate == 0 && visitor.vars.size() >= 2 && testCandidates.size() > 0;


    }

    private class Visitor extends DefaultFormulaVisitor<Void> {
      final List<FormulaVariableResult> vars = new ArrayList<>();
      final List<Object> constants = new ArrayList<>();

      @Override
      protected Void visitDefault(Formula f) {
        throw new IllegalStateException("Don't know how to handle this here: " + f);
      }

      @Override
      public Void visitFreeVariable(Formula f, String name) {
        vars.add(splitFormulaVariable(name));
        return null;
      }

      @Override
      public Void visitConstant(Formula f, Object value) {
        constants.add(value);
        return null;
      }

      @Override
      public Void visitFunction(
          Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
        args.forEach(pFormula -> fmgr.visit(this, pFormula));
        return null;
      }
    }
  }


}

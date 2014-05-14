/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.concrete_counterexample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.Model.Constant;
import org.sosy_lab.cpachecker.core.Model.Function;
import org.sosy_lab.cpachecker.core.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;


public class AssignmentToPathAllocator {

  @SuppressWarnings("unused")
  private final LogManager logger;

  public AssignmentToPathAllocator(LogManager pLogger) {
    logger = pLogger;
  }

  public CFAPathWithAssignments allocateAssignmentsToPath(List<CFAEdge> pPath,
      Model pModel, List<SSAMap> pSSAMaps, MachineModel pMachineModel) {

    AssignableTermsInPath assignableTerms = assignTermsToPathPosition(pSSAMaps, pModel);

    List<CFAEdgeWithAssignments> pathWithAssignments = new ArrayList<>(pPath.size());

    Multimap<CFAEdge, AssignableTerm> usedAssignableTerms = HashMultimap.create();

    Map<String, Object> addressOfVariables = getVariableAddresses(assignableTerms, pModel);

    Map<String, Assignment> variableEnvoirment = new HashMap<>();
    Multimap<String, Assignment> functionEnvoirment = HashMultimap.create();

    for (int unprecisePathIndex = 0; unprecisePathIndex < pPath.size(); unprecisePathIndex++) {

      /*We always look at the precise path, with resolved multi edges*/
      CFAEdge cfaEdge = pPath.get(unprecisePathIndex);

      int precisePathIndex = unprecisePathIndex;

      if(cfaEdge.getEdgeType() == CFAEdgeType.MultiEdge) {

        MultiEdge multiEdge = (MultiEdge) cfaEdge;

        List<CFAEdgeWithAssignments> singleEdges = new ArrayList<>();

        for (CFAEdge singleCfaEdge : multiEdge) {

          variableEnvoirment = new HashMap<>(variableEnvoirment);
          functionEnvoirment = HashMultimap.create(functionEnvoirment);
          Collection<AssignableTerm> terms = assignableTerms.getAssignableTermsAtPosition().get(precisePathIndex);

          SSAMap ssaMap = pSSAMaps.get(precisePathIndex);

          CFAEdgeWithAssignments cfaEdgeWithAssignments =
              createCFAEdgeWithAssignments(singleCfaEdge, ssaMap, variableEnvoirment,
                  functionEnvoirment, addressOfVariables,
                  terms, pModel, pMachineModel, usedAssignableTerms);

          singleEdges.add(cfaEdgeWithAssignments);
          precisePathIndex++;
        }

        CFAMultiEdgeWithAssignments edge = CFAMultiEdgeWithAssignments.valueOf(multiEdge, singleEdges);
        pathWithAssignments.add(edge);
      } else {
        variableEnvoirment = new HashMap<>(variableEnvoirment);
        functionEnvoirment = HashMultimap.create(functionEnvoirment);
        Collection<AssignableTerm> terms = assignableTerms.getAssignableTermsAtPosition().get(precisePathIndex);

        SSAMap ssaMap = pSSAMaps.get(precisePathIndex);

        CFAEdgeWithAssignments cfaEdgeWithAssignments =
            createCFAEdgeWithAssignments(cfaEdge, ssaMap, variableEnvoirment,
                functionEnvoirment, addressOfVariables,
                terms, pModel, pMachineModel, usedAssignableTerms);

        pathWithAssignments.add(cfaEdgeWithAssignments);
      }
    }

    return new CFAPathWithAssignments(pathWithAssignments, usedAssignableTerms);
  }

  private CFAEdgeWithAssignments createCFAEdgeWithAssignments(
      CFAEdge cfaEdge, SSAMap ssaMap,
      Map<String, Assignment> variableEnvoirment,
      Multimap<String, Assignment> functionEnvoirment,
      Map<String, Object> addressOfVariables,
      Collection<AssignableTerm> terms, Model pModel,
      MachineModel pMachineModel,
      Multimap<CFAEdge, AssignableTerm> usedAssignableTerms) {

    Set<Assignment> termSet = new HashSet<>();

    createAssignments(pModel, terms, termSet, variableEnvoirment, functionEnvoirment);

    removeDeallocatedVariables(ssaMap, variableEnvoirment);

    ModelAtCFAEdge modelAtEdge = new ModelAtCFAEdge(variableEnvoirment, functionEnvoirment, addressOfVariables);

    AssignmentToEdgeAllocator allocator =
        new AssignmentToEdgeAllocator(logger, cfaEdge, termSet, modelAtEdge, pMachineModel);

    CFAEdgeWithAssignments cfaEdgeWithAssignment = allocator.allocateAssignmentsToEdge();
    usedAssignableTerms.putAll(cfaEdge, terms);
    return cfaEdgeWithAssignment;
  }

  private void removeDeallocatedVariables(SSAMap pMap, Map<String, Assignment> variableEnvoirment) {

    Set<String> variableNames = new HashSet<>(variableEnvoirment.keySet());

    for (String name : variableNames) {
      if (pMap.getIndex(name) < 0) {
        variableEnvoirment.remove(name);
      }
    }
  }

  private void createAssignments(Model pModel,
      Collection<AssignableTerm> terms,
      Set<Assignment> termSet,
      Map<String, Assignment> variableEnvoirment,
      Multimap<String, Assignment> functionEnvoirment) {

    for (AssignableTerm term : terms) {
      Assignment assignment = new Assignment(term, pModel.get(term));

      if (term instanceof Variable) {

        Variable variable = (Variable) term;
        String name = variable.getName();

        if (variableEnvoirment.containsKey(name)) {
          Variable oldVariable = (Variable) variableEnvoirment.get(name).getTerm();
          int oldIndex = oldVariable.getSSAIndex();
          int newIndex = variable.getSSAIndex();
          if (oldIndex < newIndex) {
            variableEnvoirment.remove(name);
            variableEnvoirment.put(name, assignment);
          }
        } else {
          variableEnvoirment.put(name, assignment);
        }

      } else if (term instanceof Function) {
        Function function = (Function) term;
        String name = getName(function);

        if(functionEnvoirment.containsKey(name)) {

          boolean replaced = false;

          Set<Assignment> assignments = new HashSet<>(functionEnvoirment.get(name));

          for(Assignment oldAssignment : assignments) {
            Function oldFunction = (Function) oldAssignment.getTerm();

            if(isLessSSA(oldFunction, function)) {

              functionEnvoirment.remove(name, oldAssignment);
              functionEnvoirment.put(name, assignment);
              replaced = true;
            }
          }

          if(!replaced) {
            functionEnvoirment.put(name, assignment);
          }
        } else {
          functionEnvoirment.put(name, assignment);
        }
      }
      termSet.add(assignment);
    }
  }

  private Map<String, Object> getVariableAddresses(
      AssignableTermsInPath assignableTerms, Model pModel) {

    Map<String, Object> addressOfVariables = new HashMap<>();

    for (Constant constant : assignableTerms.getConstants()) {
      String name = constant.getName();
      if (name.startsWith(ModelAtCFAEdge.getAddressPrefix())
          && pModel.containsKey(constant)) {

        addressOfVariables.put(name, pModel.get(constant));
      }
    }

    return ImmutableMap.copyOf(addressOfVariables);
  }

  private boolean isLessSSA(Function pOldFunction, Function pFunction) {

    String name = getName(pFunction);
    String oldName = getName(pFunction);

    if (!name.equals(oldName)) {
      return false;
    }

    int ssa = getSSAIndex(pFunction);
    int oldSSA = getSSAIndex(pOldFunction);

    if (!(oldSSA <= ssa)) {
      return false;
    }

    int arity = pFunction.getArity();

    int oldArity = pOldFunction.getArity();

    if (arity != oldArity) {
      return false;
    }

    for (int c = 0; c < arity; c++) {
      if (!pOldFunction.getArgument(c).equals(pFunction.getArgument(c))) {
        return false;
      }
    }

    return true;
  }

  /*
   * Allocate the assignable terms with a SSAIndex in the given model
   * to the position in the path they were first used. The result of this
   * allocation is used to determine the model at each edge of the path.
   *
   */
  private AssignableTermsInPath assignTermsToPathPosition(List<SSAMap> pSsaMaps, Model pModel) {

    // Create a map that holds all AssignableTerms that occurred
    // in the given path. The referenced path is the precise path, with multi edges resolved.
    Multimap<Integer, AssignableTerm> assignedTermsPosition = HashMultimap.create();

    Set<Constant> constants = new HashSet<>();
    Set<Function> functionsWithoutSSAIndex = new HashSet<>();

    for (AssignableTerm term : pModel.keySet()) {

      if (term instanceof Variable) {
        int index = findFirstOccurrenceOfVariable((Variable) term, pSsaMaps);
        if (index >= 0) {
          assignedTermsPosition.put(index, term);
        }
      } else if(term instanceof Function) {

        Function function = (Function) term;

        if (getSSAIndex(function) == -2) {
          functionsWithoutSSAIndex.add(function);
        } else {
          int index = findFirstOccurrenceOfVariable(function, pSsaMaps);
          if (index >= 0) {
            assignedTermsPosition.put(index, term);
          }
        }
      } else if(term instanceof Constant)  {
        constants.add((Constant) term);
      }
    }

    return new AssignableTermsInPath(assignedTermsPosition, constants, functionsWithoutSSAIndex);
  }

  private int getSSAIndex(Function pTerm) {

    String[] nameAndIndex = pTerm.getName().split("@");

    if (nameAndIndex.length == 2) {
      String index = nameAndIndex[1];

      if (index.matches("\\d*")) {
        return Integer.parseInt(index);
      }

    }

    return -2;
  }

  private String getName(Function pTerm) {

    String[] nameAndIndex = pTerm.getName().split("@");

    if (nameAndIndex.length == 2) {
      return nameAndIndex[0];
    }

    return pTerm.getName();
  }

  /**
   * Search through an (ordered) list of SSAMaps
   * for the first index where a given variable appears.
   * @return -1 if the variable with the given index never occurs, or an index of pSsaMaps
   */
  private int findFirstOccurrenceOfVariable(Variable pVar, List<SSAMap> pSsaMaps) {

    // both indices are inclusive bounds of the range where we still need to look
    int lower = 0;
    int upper = pSsaMaps.size() - 1;

    int result = -1;

    /*Due to the new way to handle aliases, assignable terms of variables
    may be replaced with UIFs in the SSAMap. If this is the case, modify upper
    by looking for the variable in the other maps*/
    if (pSsaMaps.size() <= 0) {
      return result;
    } else {

      while (upper >= 0 &&
          (pSsaMaps.get(upper).getIndex(pVar.getName())
            == SSAMap.INDEX_NOT_CONTAINED)) {
        upper--;
      }

      if (upper < 0) {
        return result;
      }
    }

    // do binary search
    while (true) {
      if (upper-lower <= 0) {

        if (upper - lower == 0) {
          int ssaIndex = pSsaMaps.get(upper).getIndex(pVar.getName());

          if (ssaIndex == pVar.getSSAIndex()) {
            result = upper;
          }
        }

        return result;
      }

      int index = lower + ((upper-lower) / 2);
      assert index >= lower;
      assert index <= upper;

      int ssaIndex = pSsaMaps.get(index).getIndex(pVar.getName());

      if (ssaIndex < pVar.getSSAIndex()) {
        lower = index + 1;
      } else if (ssaIndex > pVar.getSSAIndex()) {
        upper = index - 1;
      } else {
        // found a matching SSAMap,
        // but we keep looking whether there is another one with a smaller index
        assert result == -1 || result > index;
        result = index;
        upper = index - 1;
      }
    }
  }

  private int findFirstOccurrenceOfVariable(Function pTerm, List<SSAMap> pSsaMaps) {

    int lower = 0;
    int upper = pSsaMaps.size() - 1;

    int result = -1;

    // do binary search
    while (true) {
      if (upper-lower <= 0) {

        if (upper - lower == 0) {
          int ssaIndex = pSsaMaps.get(upper).getIndex(getName(pTerm));

          if (ssaIndex == getSSAIndex(pTerm)) {
            result = upper;
          }
        }

        return result;
      }

      int index = lower + ((upper-lower) / 2);
      assert index >= lower;
      assert index <= upper;

      int ssaIndex = pSsaMaps.get(index).getIndex(getName(pTerm));

      if (ssaIndex < getSSAIndex(pTerm)) {
        lower = index + 1;
      } else if (ssaIndex > getSSAIndex(pTerm)) {
        upper = index - 1;
      } else {
        // found a matching SSAMap,
        // but we keep looking whether there is another one with a smaller index
        assert result == -1 || result > index;
        result = index;
        upper = index - 1;
      }
    }
  }

  private static final class AssignableTermsInPath {

    private final Multimap<Integer, AssignableTerm> assignableTermsAtPosition;
    private final Set<Constant> constants;
    private final Set<Function> ufFunctionsWithoutSSAIndex;

    public AssignableTermsInPath(
        Multimap<Integer, AssignableTerm> pAssignableTermsAtPosition,
        Set<Constant> pConstants, Set<Function> pUfFunctionsWithoutSSAIndex) {

      assignableTermsAtPosition = ImmutableMultimap.copyOf(pAssignableTermsAtPosition);
      constants = ImmutableSet.copyOf(pConstants);
      ufFunctionsWithoutSSAIndex = ImmutableSet.copyOf(pUfFunctionsWithoutSSAIndex);
    }

    public Multimap<Integer, AssignableTerm> getAssignableTermsAtPosition() {
      return assignableTermsAtPosition;
    }

    public Set<Constant> getConstants() {
      return constants;
    }

    @SuppressWarnings("unused")
    public Set<Function> getUfFunctionsWithoutSSAIndex() {
      return ufFunctionsWithoutSSAIndex;
    }
  }
}
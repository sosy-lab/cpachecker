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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.Model.Constant;
import org.sosy_lab.cpachecker.core.Model.Function;
import org.sosy_lab.cpachecker.core.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;


public class AssignmentToPathAllocator {

  @SuppressWarnings("unused")
  private final LogManager logger;

  public AssignmentToPathAllocator(LogManager pLogger) {
    logger = pLogger;
  }

  public CFAPathWithAssignments allocateAssignmentsToPath(List<CFAEdge> pPath,
      Multimap<Integer, AssignableTerm> pAssignedTermsPosition, Model pModel,
      Set<Constant> pConstants, Set<Function> functionsWithoutSSAIndex,
      List<SSAMap> pSSAMaps, MachineModel pMachineModel) {

    assert pSSAMaps.size() == pPath.size();

    List<CFAEdgeWithAssignments> pathWithAssignments = new ArrayList<>(pPath.size());

    Multimap<CFAEdge, AssignableTerm> multimap = HashMultimap.create();

    Map<String, Object> addressMap = new HashMap<>();

    for (Constant constant : pConstants) {
      String name = constant.getName();
      if (name.startsWith(ModelAtCFAEdge.getAddressPrefix())
          && pModel.containsKey(constant)) {

        addressMap.put(name, pModel.get(constant));
      }
    }

    Map<Function, Object> functionMap = new HashMap<>();

    for (Function function : functionsWithoutSSAIndex) {
      functionMap.put(function, pModel.get(function));
    }

    Map<String, Object> imAddressMap = ImmutableMap.copyOf(addressMap);

    Map<String, Assignment> variableEnvoirment = new HashMap<>();
    Multimap<String, Assignment> functionEnvoirment = HashMultimap.create();

    for (int index = 0; index < pPath.size(); index++) {

      variableEnvoirment = new HashMap<>(variableEnvoirment);
      functionEnvoirment = HashMultimap.create(functionEnvoirment);

      CFAEdge cfaEdge = pPath.get(index);
      Collection<AssignableTerm> terms = pAssignedTermsPosition.get(index);

      Set<Assignment> termSet = new HashSet<>();

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
          String name = PathChecker.getName(function);

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

      SSAMap map = pSSAMaps.get(index);

      Set<String> variableNames = new HashSet<>(variableEnvoirment.keySet());

      for(String name : variableNames) {
        if(map.getIndex(name) < 0) {
          variableEnvoirment.remove(name);
        }
      }

      ModelAtCFAEdge modelAtEdge = new ModelAtCFAEdge(variableEnvoirment, functionEnvoirment, imAddressMap);

      AssignmentToEdgeAllocator allocator =
          new AssignmentToEdgeAllocator(logger, cfaEdge, termSet, modelAtEdge, pMachineModel);

      CFAEdgeWithAssignments cfaEdgeWithAssignment = allocator.allocateAssignmentsToEdge();

      pathWithAssignments.add(cfaEdgeWithAssignment);
      multimap.putAll(cfaEdge, terms);
    }

    return new CFAPathWithAssignments(pathWithAssignments, multimap);
  }

  private boolean isLessSSA(Function pOldFunction, Function pFunction) {

    String name = PathChecker.getName(pFunction);
    String oldName = PathChecker.getName(pFunction);

    if (!name.equals(oldName)) {
      return false;
    }

    int ssa = PathChecker.getSSAIndex(pFunction);
    int oldSSA = PathChecker.getSSAIndex(pOldFunction);

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
}
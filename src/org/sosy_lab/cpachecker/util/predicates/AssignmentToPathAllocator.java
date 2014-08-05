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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Address;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcerteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.FieldReference;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.core.counterexample.MemoryName;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.counterexample.Model.Constant;
import org.sosy_lab.cpachecker.core.counterexample.Model.Function;
import org.sosy_lab.cpachecker.core.counterexample.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;


public class AssignmentToPathAllocator {

  private static final String ADDRESS_PREFIX = "__ADDRESS_OF_";
  private static final int FIRST = 0;
  private static final int IS_NOT_GLOBAL = 2;
  private static final int NAME_AND_FUNCTION = 0;
  private static final int IS_FIELD_REFERENCE = 1;

  @SuppressWarnings("unused")
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private MemoryName memoryName = new MemoryName() {

    @Override
    public String getMemoryName(CRightHandSide pExp, Address pAddress) {
      CType type = pExp.getExpressionType().getCanonicalType();
      type = CTypes.withoutConst(type);
      type = CTypes.withoutVolatile(type);
      return  "*" + type.toString().replace(" ", "_");
    }
  };

  public AssignmentToPathAllocator(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  /**
   * Provide a path with concrete values (like a test case)
   */
  public CFAPathWithAssignments allocateAssignmentsToPath(List<CFAEdge> pPath,
      Model pModel, List<SSAMap> pSSAMaps, MachineModel pMachineModel) throws InterruptedException {

    // create concrete state path, also remember used assignable term for legacy function
    Pair<ConcreteStatePath, Multimap<CFAEdge, AssignableTerm>> concreteStatePath = createConcreteStatePath(pPath,
        pModel, pSSAMaps, pMachineModel);

    return CFAPathWithAssignments.valueOf(concreteStatePath.getFirst(), logger, pMachineModel,
        concreteStatePath.getSecond());
  }


  private Pair<ConcreteStatePath, Multimap<CFAEdge, AssignableTerm>> createConcreteStatePath(
      List<CFAEdge> pPath, Model pModel, List<SSAMap> pSSAMaps, MachineModel pMachineModel)
          throws InterruptedException {

    AssignableTermsInPath assignableTerms = assignTermsToPathPosition(pSSAMaps, pModel);
    List<ConcerteStatePathNode> pathWithAssignments = new ArrayList<>(pPath.size());
    Multimap<CFAEdge, AssignableTerm> usedAssignableTerms = HashMultimap.create();
    Map<LeftHandSide, Address> addressOfVariables = getVariableAddresses(assignableTerms, pModel);

    /* Its too inefficient to recreate every assignment from scratch,
       but the ssaIndex of the Assignable Terms are needed, thats
       why we declare two maps of variables and functions. One for
       the calculation of the SSAIndex, the other to save the references
       to the objects we want to store in the concrete State, so we can avoid
       recreating those objects */

    Map<String, Assignment> variableEnvoirment = new HashMap<>();
    Map<LeftHandSide, Object> variables = new HashMap<>();
    Multimap<String, Assignment> functionEnvoirment = HashMultimap.create();
    //TODO Persistent Map
    Map<String, Map<Address, Object>> memory = new HashMap<>();

    int ssaMapIndex = 0;

    for (int pathIndex = 0; pathIndex < pPath.size(); pathIndex++) {
      shutdownNotifier.shutdownIfNecessary();

      /*We always look at the precise path, with resolved multi edges*/
      CFAEdge cfaEdge = pPath.get(pathIndex);

      if (cfaEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
        MultiEdge multiEdge = (MultiEdge) cfaEdge;

        List<ConcreteState> singleConcreteStates = new ArrayList<>(multiEdge.getEdges().size());

        int multiEdgeIndex = 0;
        for (CFAEdge singleCfaEdge : multiEdge) {

          variableEnvoirment = new HashMap<>(variableEnvoirment);
          variables = new HashMap<>(variables);
          functionEnvoirment = HashMultimap.create(functionEnvoirment);
          memory = new HashMap<>(memory);
          Collection<AssignableTerm> terms = assignableTerms.getAssignableTermsAtPosition().get(ssaMapIndex);

          SSAMap ssaMap = pSSAMaps.get(ssaMapIndex);

          ConcreteState concreteState = createSingleConcreteState(
              singleCfaEdge, ssaMap, variableEnvoirment, variables,
              functionEnvoirment, memory, addressOfVariables, terms,
              pModel, pMachineModel, usedAssignableTerms);

          singleConcreteStates.add(multiEdgeIndex, concreteState);
          ssaMapIndex++;
          multiEdgeIndex++;
        }

        ConcerteStatePathNode edge = ConcreteStatePath.valueOfPathNode(singleConcreteStates, multiEdge);
        pathWithAssignments.add(edge);
      } else {
        variableEnvoirment = new HashMap<>(variableEnvoirment);
        functionEnvoirment = HashMultimap.create(functionEnvoirment);
        Collection<AssignableTerm> terms = assignableTerms.getAssignableTermsAtPosition().get(ssaMapIndex);

        SSAMap ssaMap = pSSAMaps.get(ssaMapIndex);

        ConcerteStatePathNode concreteStatePathNode =
            createSingleConcreteStateNode(cfaEdge, ssaMap, variableEnvoirment, variables,
                functionEnvoirment, memory, addressOfVariables,
                terms, pModel, pMachineModel, usedAssignableTerms);

        pathWithAssignments.add(concreteStatePathNode);
        ssaMapIndex++;
      }
    }

    ConcreteStatePath concreteStatePath = new ConcreteStatePath(pathWithAssignments);
    return Pair.of(concreteStatePath, usedAssignableTerms);
  }

  private ConcerteStatePathNode createSingleConcreteStateNode(
      CFAEdge cfaEdge, SSAMap ssaMap,
      Map<String, Assignment> variableEnvoirment,
      Map<LeftHandSide, Object> variables,
      Multimap<String, Assignment> functionEnvoirment,
      Map<String, Map<Address, Object>> memory,
      Map<LeftHandSide, Address> addressOfVariables,
      Collection<AssignableTerm> terms, Model pModel,
      MachineModel pMachineModel,
      Multimap<CFAEdge, AssignableTerm> usedAssignableTerms) {

    ConcreteState concreteState = createSingleConcreteState(cfaEdge, ssaMap,
        variableEnvoirment, variables,
        functionEnvoirment, memory,
        addressOfVariables, terms, pModel,
        pMachineModel, usedAssignableTerms);

    return ConcreteStatePath.valueOfPathNode(concreteState, cfaEdge);
  }

  private ConcreteState createSingleConcreteState(
      CFAEdge cfaEdge, SSAMap ssaMap,
      Map<String, Assignment> variableEnvoirment,
      Map<LeftHandSide, Object> variables,
      Multimap<String, Assignment> functionEnvoirment,
      Map<String, Map<Address, Object>> memory,
      Map<LeftHandSide, Address> addressOfVariables,
      Collection<AssignableTerm> terms, Model pModel,
      MachineModel pMachineModel,
      Multimap<CFAEdge, AssignableTerm> usedAssignableTerms) {

    Set<Assignment> termSet = new HashSet<>();

    createAssignments(pModel, terms, termSet, variableEnvoirment, variables, functionEnvoirment, memory);

    removeDeallocatedVariables(ssaMap, variableEnvoirment);

    Map<String, Memory> allocatedMemory = createAllocatedMemory(memory);

    ConcreteState concreteState = new ConcreteState(variables, allocatedMemory, addressOfVariables, memoryName);

    // for legacy functionality, remember used assignable terms per cfa edge.
    usedAssignableTerms.putAll(cfaEdge, terms);

    return concreteState;
  }

  private Map<String, Memory> createAllocatedMemory(Map<String, Map<Address, Object>> pMemory) {

    Map<String, Memory> memory = new HashMap<>(pMemory.size());

    for (String heapName : pMemory.keySet()) {
      Map<Address, Object> heapValues = pMemory.get(heapName);
      Memory heap = new Memory(heapName, heapValues);
      memory.put(heap.getName(), heap);
    }

    return memory;
  }

  private LeftHandSide createLeftHandSide(Variable pTerm) {

    String termName = pTerm.getName();
    return createLeftHandSide(termName);
  }

  private LeftHandSide createLeftHandSide(String pTermName) {

    //TODO ugly, refactor (no splitting)

    String[] references = pTermName.split("$");
    String nameAndFunctionAsString = references[NAME_AND_FUNCTION];

    String[] nameAndFunction = nameAndFunctionAsString.split("::");

    String name = null;
    String function = null;
    boolean isNotGlobal = nameAndFunction.length == IS_NOT_GLOBAL;
    boolean isReference = references.length > IS_FIELD_REFERENCE;

    if (isNotGlobal) {
      function = nameAndFunction[0];
      name = nameAndFunction[1];
    } else {
      name = nameAndFunction[0];
    }

    if (isReference) {
      List<String> fieldNames = Arrays.asList(references);
      fieldNames.remove(NAME_AND_FUNCTION);

      if (isNotGlobal) {
        return new FieldReference(name, function, fieldNames);
      } else {
        return new FieldReference(name, fieldNames);
      }
    } else {
      if (isNotGlobal) {
        return new org.sosy_lab.cpachecker.core.counterexample.IDExpression(name, function);
      } else {
        return new org.sosy_lab.cpachecker.core.counterexample.IDExpression(name);
      }
    }
  }

  private void removeDeallocatedVariables(SSAMap pMap, Map<String, Assignment> variableEnvoirment) {

    Set<String> variableNames = new HashSet<>(variableEnvoirment.keySet());

    for (String name : variableNames) {
      if (pMap.getIndex(name) < 0) {
        variableEnvoirment.remove(name);
      }
    }
  }

  /**
   * We need the variableEnvoirment and functionEnvoirment for their SSAIndeces.
   */
  private void createAssignments(Model pModel,
      Collection<AssignableTerm> terms,
      Set<Assignment> termSet,
      Map<String, Assignment> variableEnvoirment,
      Map<LeftHandSide, Object> pVariables,
      Multimap<String, Assignment> functionEnvoirment,
      Map<String, Map<Address, Object>> memory) {

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

            //update variableEnvoirment for subsequent calculation
            variableEnvoirment.remove(name);
            variableEnvoirment.put(name, assignment);

            LeftHandSide oldlhs = createLeftHandSide(oldVariable);
            LeftHandSide lhs = createLeftHandSide(variable);
            pVariables.remove(oldlhs);
            pVariables.put(lhs, assignment.getValue());
          }
        } else {
          //update variableEnvoirment for subsequent calculation
          variableEnvoirment.put(name, assignment);

          LeftHandSide lhs = createLeftHandSide(variable);
          pVariables.put(lhs, assignment.getValue());
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

              //update functionEnvoirment for subsequent calculation
              functionEnvoirment.remove(name, oldAssignment);
              functionEnvoirment.put(name, assignment);
              replaced = true;
              removeHeapValue(memory, assignment);
              addHeapValue(memory, assignment);

            }
          }

          if(!replaced) {
            functionEnvoirment.put(name, assignment);
            addHeapValue(memory, assignment);
          }
        } else {
          functionEnvoirment.put(name, assignment);
          addHeapValue(memory, assignment);
        }
      }
      termSet.add(assignment);
    }
  }

  private void removeHeapValue(Map<String, Map<Address, Object>> memory, Assignment pFunctionAssignment) {
    Function function = (Function) pFunctionAssignment.getTerm();
    String heapName = getName(function);
    Map<Address, Object> heap = memory.get(heapName);

    if (function.getArity() == 1) {
      Address address = Address.valueOf(function.getArgument(FIRST));
      heap.remove(address);
    } else {
      throw new AssertionError();
    }
  }

  private void addHeapValue(Map<String, Map<Address, Object>> memory, Assignment pFunctionAssignment) {
    Function function = (Function) pFunctionAssignment.getTerm();
    String heapName = getName(function);
    Map<Address, Object> heap;

    if (!memory.containsKey(heapName)) {
      memory.put(heapName, new HashMap<Address, Object>());
    }

    heap = memory.get(heapName);

    if (function.getArity() == 1) {
      Address address = Address.valueOf(function.getArgument(FIRST));
      Object value = pFunctionAssignment.getValue();
      heap.put(address, value);
    } else {
      throw new AssertionError();
    }
  }

  private Map<LeftHandSide, Address> getVariableAddresses(
      AssignableTermsInPath assignableTerms, Model pModel) {

    Map<LeftHandSide, Address> addressOfVariables = new HashMap<>();

    for (Constant constant : assignableTerms.getConstants()) {
      String name = constant.getName();
      if (name.startsWith(ADDRESS_PREFIX)
          && pModel.containsKey(constant)) {

        Object addressValue = pModel.get(constant);

        Address address = Address.valueOf(addressValue);

        //TODO ugly, refactor?
        String constantName = name.substring(ADDRESS_PREFIX.length());

        LeftHandSide leftHandSide = createLeftHandSide(constantName);

        addressOfVariables.put(leftHandSide, address);
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
   * @return -1 if the variable with the given SSA-index never occurs, or an index of pSsaMaps
   */
  int findFirstOccurrenceOfVariable(Variable pVar, List<SSAMap> pSsaMaps) {

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
          !pSsaMaps.get(upper).containsVariable(pVar.getName())) {
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

  int findFirstOccurrenceOfVariable(Function pTerm, List<SSAMap> pSsaMaps) {

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

  // TODO: Why is this generic class not in the package core.counterexample?
  private static final class Assignment {

    private final AssignableTerm term;
    private final Object value;

    public Assignment(AssignableTerm pTerm, Object pValue) {
      term = pTerm;
      value = pValue;
    }

    public AssignableTerm getTerm() {
      return term;
    }

    public Object getValue() {
      return value;
    }

    @Override
    public String toString() {
      return "term: " + term.toString() + "value: " + value.toString();
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

    @Override
    public String toString() {
      return "AssignableTermsInPath\n"
          + "assignableTermsAtPosition=" + assignableTermsAtPosition + "\n "
          + "constants=" + constants + "\n"
          + "ufFunctionsWithoutSSAIndex=" + ufFunctionsWithoutSSAIndex;
    }
  }
}
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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.core.counterexample.Address;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcerteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.FieldReference;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.core.counterexample.MemoryName;
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm.Function;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.math.IntMath;


public class AssignmentToPathAllocator {

  private static final String ADDRESS_PREFIX = "__ADDRESS_OF_";
  private static final int FIRST = 0;
  private static final int IS_NOT_GLOBAL = 2;
  private static final int NAME_AND_FUNCTION = 0;
  private static final int IS_FIELD_REFERENCE = 1;

  private final ShutdownNotifier shutdownNotifier;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  private MemoryName memoryName = new MemoryName() {

    @Override
    public String getMemoryName(CRightHandSide pExp, Address pAddress) {
      CType type = pExp.getExpressionType().getCanonicalType();
      type = CTypes.withoutConst(type);
      type = CTypes.withoutVolatile(type);
      return  "*" + type.toString().replace(" ", "_");
    }
  };

  public AssignmentToPathAllocator(Configuration pConfig, ShutdownNotifier pShutdownNotifier, LogManager pLogger, MachineModel pMachineModel) throws InvalidConfigurationException {
    this.shutdownNotifier = pShutdownNotifier;
    this.assumptionToEdgeAllocator = new AssumptionToEdgeAllocator(pConfig, pLogger, pMachineModel);
  }

  /**
   * Provide a path with concrete values (like a test case).
   * Additionally, provides the information, at which {@link CFAEdge} edge which
   * {@link AssignableTerm} terms have been assigned.
   */
  public Pair<CFAPathWithAssumptions, Multimap<CFAEdge, AssignableTerm>> allocateAssignmentsToPath(List<CFAEdge> pPath,
      RichModel pModel, List<SSAMap> pSSAMaps) throws InterruptedException {

    // create concrete state path, also remember at wich edge which terms were used.
    Pair<ConcreteStatePath, Multimap<CFAEdge, AssignableTerm>> concreteStatePath = createConcreteStatePath(pPath,
        pModel, pSSAMaps);

    // create the concrete error path.
    CFAPathWithAssumptions pathWithAssignments =
        CFAPathWithAssumptions.of(concreteStatePath.getFirst(), assumptionToEdgeAllocator);

    return Pair.of(pathWithAssignments, concreteStatePath.getSecond());
  }


  private Pair<ConcreteStatePath, Multimap<CFAEdge, AssignableTerm>> createConcreteStatePath(
      List<CFAEdge> pPath, RichModel pModel, List<SSAMap> pSSAMaps)
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

    Map<String, Assignment> variableEnvironment = new HashMap<>();
    Map<LeftHandSide, Object> variables = new HashMap<>();
    Multimap<String, Assignment> functionEnvironment = HashMultimap.create();
    //TODO Persistent Map
    Map<String, Map<Address, Object>> memory = new HashMap<>();

    int ssaMapIndex = 0;

    for (CFAEdge cfaEdge : pPath) {
      shutdownNotifier.shutdownIfNecessary();

      /*We always look at the precise path, with resolved multi edges*/
      if (cfaEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
        MultiEdge multiEdge = (MultiEdge)cfaEdge;

        List<ConcreteState> singleConcreteStates =
            new ArrayList<>(multiEdge.getEdges().size());

        int multiEdgeIndex = 0;
        for (CFAEdge singleCfaEdge : multiEdge) {

          variableEnvironment = new HashMap<>(variableEnvironment);
          variables = new HashMap<>(variables);
          functionEnvironment = HashMultimap.create(functionEnvironment);
          memory = new HashMap<>(memory);
          Collection<AssignableTerm> terms =
              assignableTerms.getAssignableTermsAtPosition().get(ssaMapIndex);

          SSAMap ssaMap = pSSAMaps.get(ssaMapIndex);

          ConcreteState concreteState = createSingleConcreteState(
              singleCfaEdge, ssaMap, variableEnvironment, variables,
              functionEnvironment, memory, addressOfVariables, terms,
              pModel, usedAssignableTerms);

          singleConcreteStates.add(multiEdgeIndex, concreteState);
          ssaMapIndex++;
          multiEdgeIndex++;
        }

        ConcerteStatePathNode edge =
            ConcreteStatePath.valueOfPathNode(singleConcreteStates, multiEdge);
        pathWithAssignments.add(edge);
      } else {
        variableEnvironment = new HashMap<>(variableEnvironment);
        functionEnvironment = HashMultimap.create(functionEnvironment);
        Collection<AssignableTerm> terms =
            assignableTerms.getAssignableTermsAtPosition().get(ssaMapIndex);

        SSAMap ssaMap = pSSAMaps.get(ssaMapIndex);

        ConcerteStatePathNode concreteStatePathNode =
            createSingleConcreteStateNode(cfaEdge, ssaMap, variableEnvironment,
                variables,
                functionEnvironment, memory, addressOfVariables,
                terms, pModel, usedAssignableTerms);

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
      Collection<AssignableTerm> terms, RichModel pModel,
      Multimap<CFAEdge, AssignableTerm> usedAssignableTerms) {

    ConcreteState concreteState = createSingleConcreteState(cfaEdge, ssaMap,
        variableEnvoirment, variables,
        functionEnvoirment, memory,
        addressOfVariables, terms, pModel, usedAssignableTerms);

    return ConcreteStatePath.valueOfPathNode(concreteState, cfaEdge);
  }

  private ConcreteState createSingleConcreteState(
      CFAEdge cfaEdge, SSAMap ssaMap,
      Map<String, Assignment> variableEnvironment,
      Map<LeftHandSide, Object> variables,
      Multimap<String, Assignment> functionEnvironment,
      Map<String, Map<Address, Object>> memory,
      Map<LeftHandSide, Address> addressOfVariables,
      Collection<AssignableTerm> terms, RichModel pModel,
      Multimap<CFAEdge, AssignableTerm> usedAssignableTerms) {

    Set<Assignment> termSet = new HashSet<>();

    createAssignments(pModel, terms, termSet, variableEnvironment, variables, functionEnvironment, memory);

    removeDeallocatedVariables(ssaMap, variableEnvironment);

    Map<String, Memory> allocatedMemory = createAllocatedMemory(memory);

    ConcreteState concreteState = new ConcreteState(variables, allocatedMemory, addressOfVariables, memoryName);

    // for legacy functionality, remember used assignable terms per cfa edge.
    usedAssignableTerms.putAll(cfaEdge, terms);

    return concreteState;
  }

  private Map<String, Memory> createAllocatedMemory(Map<String, Map<Address, Object>> pMemory) {

    Map<String, Memory> memory = Maps.newHashMapWithExpectedSize(pMemory.size());

    for (Map.Entry<String, Map<Address, Object>> heapObject : pMemory.entrySet()) {
      Memory heap = new Memory(heapObject.getKey(), heapObject.getValue());
      memory.put(heap.getName(), heap);
    }

    return memory;
  }

  private LeftHandSide createLeftHandSide(String pTermName) {

    //TODO ugly, refactor (no splitting)

    String[] references = pTermName.split("$");
    String nameAndFunctionAsString = references[NAME_AND_FUNCTION];

    String[] nameAndFunction = nameAndFunctionAsString.split("::");

    String name;
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
   * We need the variableEnvironment and functionEnvironment for their SSAIndeces.
   */
  private void createAssignments(RichModel pModel,
      Collection<AssignableTerm> terms,
      Set<Assignment> termSet,
      Map<String, Assignment> variableEnvironment,
      Map<LeftHandSide, Object> pVariables,
      Multimap<String, Assignment> functionEnvironment,
      Map<String, Map<Address, Object>> memory) {

    for (AssignableTerm term : terms) {

      Assignment assignment = new Assignment(term, pModel.get(term));
      String fullName = term.getName();
      Pair<String, Integer> pair = FormulaManagerView.parseName(fullName);
      if (pair.getSecond() != null) {
        String canonicalName = pair.getFirst();
        int newIndex = pair.getSecondNotNull();

        if (variableEnvironment.containsKey(canonicalName)) {
          AssignableTerm oldVariable = variableEnvironment.get(canonicalName).getTerm();

          int oldIndex = FormulaManagerView.parseName(oldVariable.getName()).getSecondNotNull();

          if (oldIndex < newIndex) {

            //update variableEnvironment for subsequent calculation
            variableEnvironment.remove(canonicalName);
            variableEnvironment.put(canonicalName, assignment);

            LeftHandSide oldlhs = createLeftHandSide(canonicalName);
            LeftHandSide lhs = createLeftHandSide(canonicalName);
            pVariables.remove(oldlhs);
            pVariables.put(lhs, assignment.getValue());
          }
        } else {
          //update variableEnvironment for subsequent calculation
          variableEnvironment.put(canonicalName, assignment);

          LeftHandSide lhs = createLeftHandSide(canonicalName);
          pVariables.put(lhs, assignment.getValue());
        }
      }

      if (term instanceof Function) {

        Function function = (Function) term;
        String name = getName(function);

        if (functionEnvironment.containsKey(name)) {
          boolean replaced = false;
          Set<Assignment> assignments = new HashSet<>(functionEnvironment.get(name));
          for (Assignment oldAssignment : assignments) {
            Function oldFunction = (Function) oldAssignment.getTerm();

            if (isSmallerSSA(oldFunction, function)) {

              //update functionEnvironment for subsequent calculation
              functionEnvironment.remove(name, oldAssignment);
              functionEnvironment.put(name, assignment);
              replaced = true;
              removeHeapValue(memory, assignment);
              addHeapValue(memory, assignment);

            }
          }

          if (!replaced) {
            functionEnvironment.put(name, assignment);
            addHeapValue(memory, assignment);
          }
        } else {
          functionEnvironment.put(name, assignment);
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
      AssignableTermsInPath assignableTerms, RichModel pModel) {

    Map<LeftHandSide, Address> addressOfVariables = new HashMap<>();

    for (AssignableTerm constant : assignableTerms.getConstants()) {
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

  private boolean isSmallerSSA(Function pOldFunction, Function pFunction) {

    String name = FormulaManagerView.parseName(pFunction.getName()).getFirstNotNull();
    String oldName = FormulaManagerView.parseName(pOldFunction.getName()).getFirstNotNull();

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
  private AssignableTermsInPath assignTermsToPathPosition(List<SSAMap> pSsaMaps, RichModel pModel) {

    // Create a map that holds all AssignableTerms that occurred
    // in the given path. The referenced path is the precise path, with multi edges resolved.
    Multimap<Integer, AssignableTerm> assignedTermsPosition = HashMultimap.create();

    Set<AssignableTerm> constants = new HashSet<>();
    Set<Function> functionsWithoutSSAIndex = new HashSet<>();

    for (AssignableTerm term : pModel.keySet()) {

      int ssaIdx = getSSAIndex(term);
      if (term instanceof Function) {
        Function function = (Function) term;
        if (ssaIdx == -2) {
          functionsWithoutSSAIndex.add(function);
        } else {
          int index = findFirstOccurrenceOfVariable(function, pSsaMaps);
          if (index >= 0) {
            assignedTermsPosition.put(index, term);
          }
        }
      } else if (ssaIdx != -2) { // Variable.
        int index = findFirstOccurrenceOfVariable(term, pSsaMaps);
        if (index >= 0) {
          assignedTermsPosition.put(index, term);
        }
      }  else {
        constants.add(term);
      }
    }

    return new AssignableTermsInPath(assignedTermsPosition, constants, functionsWithoutSSAIndex);
  }

  private int getSSAIndex(AssignableTerm pTerm) {
    Integer out = FormulaManagerView.parseName(pTerm.getName()).getSecond();
    if (out != null) {
      return out;
    }
    return -2;
  }

  private String getName(AssignableTerm pTerm) {
    return FormulaManagerView.parseName(pTerm.getName()).getFirst();
  }

  /**
   * Search through an (ordered) list of SSAMaps
   * for the first index where a given variable appears.
   * @return -1 if the variable with the given SSA-index never occurs, or an index of pSsaMaps
   */
  int findFirstOccurrenceOfVariable(AssignableTerm pVar, List<SSAMap> pSsaMaps) {

    // both indices are inclusive bounds of the range where we still need to look
    int lower = 0;
    int upper = pSsaMaps.size() - 1;

    int result = -1;
    String canonicalName = getName(pVar);
    int varSSAIdx = getSSAIndex(pVar);

    /*Due to the new way to handle aliases, assignable terms of variables
    may be replaced with UIFs in the SSAMap. If this is the case, modify upper
    by looking for the variable in the other maps*/
    if (pSsaMaps.size() <= 0) {
      return result;
    } else {

      while (upper >= 0 &&
          !pSsaMaps.get(upper).containsVariable(canonicalName)) {
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
          int ssaIndex = pSsaMaps.get(upper).getIndex(canonicalName);

          if (ssaIndex == varSSAIdx) {
            result = upper;
          }
        }

        return result;
      }

      int index = IntMath.mean(lower, upper);
      int ssaIndex = pSsaMaps.get(index).getIndex(canonicalName);

      if (ssaIndex < varSSAIdx) {
        lower = index + 1;
      } else if (ssaIndex > varSSAIdx) {
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

      int index = IntMath.mean(lower, upper);
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
    private final Set<AssignableTerm> constants;
    private final Set<Function> ufFunctionsWithoutSSAIndex;

    public AssignableTermsInPath(
        Multimap<Integer, AssignableTerm> pAssignableTermsAtPosition,
        Set<AssignableTerm> pConstants, Set<Function> pUfFunctionsWithoutSSAIndex) {

      assignableTermsAtPosition = ImmutableMultimap.copyOf(pAssignableTermsAtPosition);
      constants = ImmutableSet.copyOf(pConstants);
      ufFunctionsWithoutSSAIndex = ImmutableSet.copyOf(pUfFunctionsWithoutSSAIndex);
    }

    public Multimap<Integer, AssignableTerm> getAssignableTermsAtPosition() {
      return assignableTermsAtPosition;
    }

    public Set<AssignableTerm> getConstants() {
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
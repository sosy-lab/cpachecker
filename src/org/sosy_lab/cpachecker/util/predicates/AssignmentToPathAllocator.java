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

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.counterexample.Address;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteExpressionEvaluator;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.IntermediateConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.SingleConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.FieldReference;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.core.counterexample.MemoryName;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

public class AssignmentToPathAllocator {

  private static final int IS_NOT_GLOBAL = 2;
  private static final int NAME_AND_FUNCTION = 0;
  private static final int IS_FIELD_REFERENCE = 1;

  private final ShutdownNotifier shutdownNotifier;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  private final MemoryName memoryName;

  public AssignmentToPathAllocator(Configuration pConfig, ShutdownNotifier pShutdownNotifier, LogManager pLogger, MachineModel pMachineModel) throws InvalidConfigurationException {
    this.shutdownNotifier = pShutdownNotifier;
    this.assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfig, pLogger, pMachineModel);

    TypeHandlerWithPointerAliasing typeHandler =
        new TypeHandlerWithPointerAliasing(
            pLogger, pMachineModel, new FormulaEncodingWithPointerAliasingOptions(pConfig));
    memoryName = exp -> typeHandler.getPointerAccessNameForType(typeHandler.getSimplifiedType(exp));
  }

  /**
   * Provide a path with concrete values (like a test case).
   */
  public CFAPathWithAssumptions allocateAssignmentsToPath(ARGPath pPath,
      Iterable<ValueAssignment> pModel, List<SSAMap> pSSAMaps) throws InterruptedException {
    ConcreteStatePath concreteStatePath = createConcreteStatePath(pPath, pModel, pSSAMaps);
    return CFAPathWithAssumptions.of(concreteStatePath, assumptionToEdgeAllocator);
  }


  private ConcreteStatePath createConcreteStatePath(
      ARGPath pPath, Iterable<ValueAssignment> pModel, List<SSAMap> pSSAMaps)
          throws InterruptedException {

    ConcreteExpressionEvaluator evaluator = createPredicateAnalysisEvaluator(pModel);
    AssignableTermsInPath assignableTerms = assignTermsToPathPosition(pSSAMaps, pModel);
    List<ConcreteStatePathNode> pathWithAssignments = new ArrayList<>(pPath.getInnerEdges().size());
    Map<LeftHandSide, Address> addressOfVariables = getVariableAddresses(assignableTerms);

    /* Its too inefficient to recreate every assignment from scratch,
       but the ssaIndex of the Assignable Terms are needed, thats
       why we declare two maps of variables and functions. One for
       the calculation of the SSAIndex, the other to save the references
       to the objects we want to store in the concrete State, so we can avoid
       recreating those objects */

    Map<String, ValueAssignment> variableEnvironment = new HashMap<>();
    Map<LeftHandSide, Object> variables = new HashMap<>();
    Multimap<String, ValueAssignment> functionEnvironment = HashMultimap.create();
    //TODO Persistent Map
    Map<String, Map<Address, Object>> memory = new HashMap<>();

    int ssaMapIndex = 0;

    /*We always look at the precise path, with resolved multi edges*/
    PathIterator pathIt = pPath.fullPathIterator();

    while (pathIt.hasNext()) {
      shutdownNotifier.shutdownIfNecessary();
      CFAEdge cfaEdge = pathIt.getOutgoingEdge();

      memory = new HashMap<>(memory);
      variableEnvironment = new HashMap<>(variableEnvironment);
      functionEnvironment = HashMultimap.create(functionEnvironment);
      Collection<ValueAssignment> terms =
          assignableTerms.getAssignableTermsAtPosition().get(ssaMapIndex);

      SSAMap ssaMap = pSSAMaps.get(ssaMapIndex);

      boolean isInsideMultiEdge;

      if (pathIt.hasNext()) {
        pathIt.advance();
        isInsideMultiEdge = !pathIt.isPositionWithState();
        pathIt.rewind();
      } else {
        isInsideMultiEdge = false;
      }

      ConcreteState concreteState =
          createConcreteState(
              ssaMap,
              variableEnvironment,
              variables,
              functionEnvironment,
              memory,
              addressOfVariables,
              terms,
              evaluator);

      final SingleConcreteState singleConcreteState;
      if (isInsideMultiEdge) {
        // we are in a multi-edge
        singleConcreteState = new IntermediateConcreteState(cfaEdge, concreteState);
      } else {
        // we are on a normal position in the ARG (state is available)
        singleConcreteState = new SingleConcreteState(cfaEdge, concreteState);
      }
      pathWithAssignments.add(singleConcreteState);
      ssaMapIndex++;

      pathIt.advance();
    }

    return new ConcreteStatePath(pathWithAssignments);
  }

  private ConcreteExpressionEvaluator createPredicateAnalysisEvaluator(Iterable<ValueAssignment> pModel) {

    Multimap<String, ValueAssignment> uninterpretedFunctions =
        FluentIterable.from(pModel)
            .filter(ValueAssignment::isFunction)
            .index(ValueAssignment::getName);

    return new PredicateAnalysisConcreteExpressionEvaluator(uninterpretedFunctions);
  }

  private static class PredicateAnalysisConcreteExpressionEvaluator implements ConcreteExpressionEvaluator {

    private final Multimap<String, ValueAssignment> uninterpretedFunctions;

    public PredicateAnalysisConcreteExpressionEvaluator(
        Multimap<String, ValueAssignment> pUninterpretedFunction) {
      uninterpretedFunctions = pUninterpretedFunction;
    }

    @Override
    public boolean shouldEvaluateExpressionWithThisEvaluator(AExpression pExp) {

      if (pExp instanceof CExpression) {
        CExpression cExp = (CExpression) pExp;
        if (hasUninterpretedFunctionName(cExp)) {
          String functionName = getUninterpretedFunctionName(cExp);
          return uninterpretedFunctions.containsKey(functionName);
        }
      }

      return false;
    }

    private String getUninterpretedFunctionName(CExpression pCExp) {

      String typeName = getTypeString(pCExp.getExpressionType());

      if (pCExp instanceof CBinaryExpression) {

        CBinaryExpression binExp = (CBinaryExpression) pCExp;
        String opString = binExp.getOperator().getOperator();

        switch (binExp.getOperator()) {
          case MULTIPLY:
            // $FALL-THROUGH$
          case MODULO:
            // $FALL-THROUGH$
          case DIVIDE:
            opString = "_" + opString;
            break;
          default:
            // default
        }

        return typeName + "_" + opString + "_";

      } else if (pCExp instanceof CUnaryExpression) {
        CUnaryExpression unExp = (CUnaryExpression) pCExp;
        String op = unExp.getOperator().getOperator();

        return typeName + "_" + op + "_";
      } else if (pCExp instanceof CCastExpression) {
        CCastExpression castExp = (CCastExpression) pCExp;
        CType type2 = castExp.getOperand().getExpressionType();
        String typeName2 = getTypeString(type2);
        return "__cast_" + typeName2 + "_to_" + typeName + "__";
      }

      return "";
    }

    private String getTypeString(CType pExpressionType) {

      if(pExpressionType instanceof CSimpleType) {

        CSimpleType simpleType = (CSimpleType) pExpressionType;

        switch (simpleType.getType()) {
          case INT:
          case CHAR:
          case BOOL:
            return "Integer";
          case FLOAT:
          case DOUBLE:
            return "Rational";
          default:
            return "";
        }
      }

      return "";
    }

    private boolean hasUninterpretedFunctionName(CExpression pCExp) {
      return pCExp instanceof CBinaryExpression || pCExp instanceof CUnaryExpression || pCExp instanceof CCastExpression;
    }

    @Override
    public Value evaluate(ABinaryExpression pBinExp, Value pOp1, Value pOp2) {

      CBinaryExpression cBinExp = (CBinaryExpression) pBinExp;
      String functionName = getUninterpretedFunctionName(cBinExp);
      Value[] operands = {pOp1, pOp2};

      for (ValueAssignment valueAssignment : uninterpretedFunctions.get(functionName)) {
        if (matchOperands(valueAssignment, operands)) {
          return asValue(valueAssignment.getValue());
        }
      }

      return Value.UnknownValue.getInstance();
    }

    private boolean matchOperands(ValueAssignment pValueAssignment, Value[] operands) {
      ImmutableList<Object> arguments = pValueAssignment.getArgumentsInterpretation();

      if (arguments.size() != operands.length) {
        return false;
      }

      for (int i = 0; i < operands.length; i++) {
        Value operandI = operands[i];
        Value argumentI = asValue(arguments.get(i));

        if (!argumentI.equals(operandI)) {
          return false;
        }
      }

      return true;
    }

    private Value asValue(Object pValue) {

      if (!(pValue instanceof Number)) {
        return Value.UnknownValue.getInstance();
      }

      return new NumericValue((Number) pValue);
    }

    @Override
    public Value evaluate(AUnaryExpression pUnaryExpression, Value pOperand) {

      if (!pOperand.isNumericValue()) {
        return UnknownValue.getInstance();
      }

      CUnaryExpression cUnaryExp = (CUnaryExpression) pUnaryExpression;
      String functionName = getUninterpretedFunctionName(cUnaryExp);
      Value[] operands = {pOperand};

      for (ValueAssignment valueAssignment : uninterpretedFunctions.get(functionName)) {
        if (matchOperands(valueAssignment, operands)) {
          return asValue(valueAssignment.getValue());
        }
      }

      return Value.UnknownValue.getInstance();
    }

    @Override
    public Value evaluate(ACastExpression pCastExpression, Value pOperand) {

      if (!pOperand.isNumericValue()) {
        return UnknownValue.getInstance();
      }

      CCastExpression cUnaryExp = (CCastExpression) pCastExpression;
      String functionName = getUninterpretedFunctionName(cUnaryExp);
      Value[] operands = {pOperand};

      for (ValueAssignment valueAssignment : uninterpretedFunctions.get(functionName)) {
        if (matchOperands(valueAssignment, operands)) {
          return asValue(valueAssignment.getValue());
        }
      }

      return Value.UnknownValue.getInstance();
    }
  }

  private ConcreteState createConcreteState(
      SSAMap ssaMap,
      Map<String, ValueAssignment> variableEnvironment,
      Map<LeftHandSide, Object> variables,
      Multimap<String, ValueAssignment> functionEnvironment,
      Map<String, Map<Address, Object>> memory,
      Map<LeftHandSide, Address> addressOfVariables,
      Collection<ValueAssignment> terms,
      ConcreteExpressionEvaluator pEvaluator) {

    createAssignments(terms, variableEnvironment, variables, functionEnvironment, memory);
    removeDeallocatedVariables(ssaMap, variableEnvironment);
    Map<String, Memory> allocatedMemory = createAllocatedMemory(memory);

    return new ConcreteState(
        variables, allocatedMemory, addressOfVariables, memoryName, pEvaluator);
  }

  private Map<String, Memory> createAllocatedMemory(Map<String, Map<Address, Object>> pMemory) {

    return ImmutableMap.copyOf(
        Maps.transformEntries(pMemory, (name, heap) -> new Memory(name, heap)));
  }

  private LeftHandSide createLeftHandSide(String pTermName) {

    //TODO ugly, refactor (no splitting)

    List<String> references = ImmutableList.copyOf(Splitter.on('$').split(pTermName));
    String nameAndFunctionAsString = references.get(NAME_AND_FUNCTION);

    List<String> nameAndFunction = ImmutableList.copyOf(Splitter.on("::").split(nameAndFunctionAsString));

    String name;
    String function = null;
    boolean isNotGlobal = nameAndFunction.size() == IS_NOT_GLOBAL;
    boolean isReference = references.size() > IS_FIELD_REFERENCE;

    if (isNotGlobal) {
      function = nameAndFunction.get(0);
      name = nameAndFunction.get(1);
    } else {
      name = nameAndFunction.get(0);
    }

    if (isReference) {
      List<String> fieldNames = new ArrayList<>(references.size() - 1);
      Iterator<String> fieldNameIterator = references.iterator();
      int i = 0;
      while (fieldNameIterator.hasNext()) {
        String fieldName = fieldNameIterator.next();
        if (i != NAME_AND_FUNCTION) {
          fieldNames.add(fieldName);
        }
        ++i;
      }

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

  private void removeDeallocatedVariables(
      SSAMap pMap, Map<String, ValueAssignment> variableEnvironment) {
    variableEnvironment.keySet().removeIf(name -> pMap.getIndex(name) < 0);
  }

  /**
   * We need the variableEnvironment and functionEnvironment for their SSAIndeces.
   */
  private void createAssignments(
      Collection<ValueAssignment> terms,
      Map<String, ValueAssignment> variableEnvironment,
      Map<LeftHandSide, Object> pVariables,
      Multimap<String, ValueAssignment> functionEnvironment,
      Map<String, Map<Address, Object>> memory) {

    for (final ValueAssignment term : terms) {
      String fullName = term.getName();
      Pair<String, OptionalInt> pair = FormulaManagerView.parseName(fullName);
      if (pair.getSecond().isPresent()) {
        String canonicalName = pair.getFirst();
        int newIndex = pair.getSecond().orElseThrow();

        if (variableEnvironment.containsKey(canonicalName)) {
          ValueAssignment oldVariable = variableEnvironment.get(canonicalName);

          int oldIndex =
              FormulaManagerView.parseName(oldVariable.getName()).getSecond().orElseThrow();

          if (oldIndex < newIndex) {

            //update variableEnvironment for subsequent calculation
            variableEnvironment.put(canonicalName, term);

            LeftHandSide lhs = createLeftHandSide(canonicalName);
            pVariables.put(lhs, term.getValue());
          }
        } else {
          //update variableEnvironment for subsequent calculation
          variableEnvironment.put(canonicalName, term);

          LeftHandSide lhs = createLeftHandSide(canonicalName);
          pVariables.put(lhs, term.getValue());
        }
      }

      if (!term.getArgumentsInterpretation().isEmpty()) {

        String name = term.getName();

        if (functionEnvironment.containsKey(name)) {
          boolean replaced = false;
          Set<ValueAssignment> assignments = new HashSet<>(functionEnvironment.get(name));
          for (ValueAssignment oldAssignment : assignments) {

            if (isSmallerSSA(oldAssignment, term)) {

              //update functionEnvironment for subsequent calculation
              functionEnvironment.remove(name, oldAssignment);
              functionEnvironment.put(name, term);
              replaced = true;
              addHeapValue(memory, term);

            }
          }

          if (!replaced) {
            functionEnvironment.put(name, term);
            addHeapValue(memory, term);
          }
        } else {
          functionEnvironment.put(name, term);
          addHeapValue(memory, term);
        }
      }
    }
  }
  private void addHeapValue(Map<String, Map<Address, Object>> memory, ValueAssignment pFunctionAssignment) {
    String heapName = getName(pFunctionAssignment);
    Map<Address, Object> heap;

    if (!memory.containsKey(heapName)) {
      memory.put(heapName, new HashMap<>());
    }

    heap = memory.get(heapName);

    Address address =
        Address.valueOf(Iterables.getOnlyElement(pFunctionAssignment.getArgumentsInterpretation()));

    Object value = pFunctionAssignment.getValue();
    heap.put(address, value);
  }

  private Map<LeftHandSide, Address> getVariableAddresses(
      AssignableTermsInPath assignableTerms) {

    Map<LeftHandSide, Address> addressOfVariables = new HashMap<>();

    for (ValueAssignment constant : assignableTerms.getConstants()) {
      String name = constant.getName();
      if (PointerTargetSet.isBaseName(name)) {
        Address address = Address.valueOf(constant.getValue());

        //TODO ugly, refactor?
        String constantName =
            PointerTargetSet.getBase(FormulaManagerView.parseName(name).getFirst());
        LeftHandSide leftHandSide = createLeftHandSide(constantName);
        addressOfVariables.put(leftHandSide, address);
      }
    }

    return ImmutableMap.copyOf(addressOfVariables);
  }

  private boolean isSmallerSSA(ValueAssignment pOldFunction, ValueAssignment pFunction) {

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

    int arity = pFunction.getArgumentsInterpretation().size();

    int oldArity = pOldFunction.getArgumentsInterpretation().size();

    if (arity != oldArity) {
      return false;
    }

    for (int c = 0; c < arity; c++) {
      if (!pOldFunction.getArgumentsInterpretation().get(c).equals(
          pFunction.getArgumentsInterpretation().get(c))) {
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
  private AssignableTermsInPath assignTermsToPathPosition(List<SSAMap> pSsaMaps,
      Iterable<ValueAssignment> pModel) {

    // Create a map that holds all AssignableTerms that occurred
    // in the given path. The referenced path is the precise path, with multi edges resolved.
    Multimap<Integer, ValueAssignment> assignedTermsPosition = HashMultimap.create();

    Set<ValueAssignment> constants = new HashSet<>();
    Set<ValueAssignment> functionsWithoutSSAIndex = new HashSet<>();

    for (ValueAssignment term : pModel) {

      int ssaIdx = getSSAIndex(term);
      if (term.isFunction()) {
        if (ssaIdx == -2) {
          functionsWithoutSSAIndex.add(term);
        } else {
          int index = findFirstOccurrenceOf(term, pSsaMaps);
          if (index >= 0) {
            assignedTermsPosition.put(index, term);
          }
        }
      } else if (ssaIdx != -2) { // Variable.
        int index = findFirstOccurrenceOf(term, pSsaMaps);
        if (index >= 0) {
          assignedTermsPosition.put(index, term);
        }
      }  else {
        constants.add(term);
      }
    }

    return new AssignableTermsInPath(assignedTermsPosition, constants, functionsWithoutSSAIndex);
  }

  private int getSSAIndex(ValueAssignment pTerm) {
    return FormulaManagerView.parseName(pTerm.getName()).getSecond().orElse(-2);
  }

  private String getName(ValueAssignment pTerm) {
    return FormulaManagerView.parseName(pTerm.getName()).getFirst();
  }

  /**
   * Search through an (unordered) list of SSAMaps for the first index where a given variable
   * appears. We do not expect that the SSAMaps are ordered in any way, e.g. SSA-indices might be
   * incrementing and decrementing along the list. This happens for example in case of
   * counterexample paths through a recursive program.
   *
   * @return -1 if the variable with the given SSA-index never occurs, or an index of first pSsaMaps
   *     where the variable occurs.
   */
  int findFirstOccurrenceOf(ValueAssignment pVar, List<SSAMap> pSsaMaps) {
    int result = -1;
    String canonicalName = getName(pVar);
    int varSSAIdx = getSSAIndex(pVar);

    for (SSAMap map : pSsaMaps) {
      result++;
      int ssaIndex = map.getIndex(canonicalName);
      if (ssaIndex == varSSAIdx) {
        return result;
      }
    }

    return result;
  }

  private static final class AssignableTermsInPath {

    private final Multimap<Integer, ValueAssignment> assignableTermsAtPosition;
    private final Set<ValueAssignment> constants;
    private final Set<ValueAssignment> ufFunctionsWithoutSSAIndex;

    public AssignableTermsInPath(
        Multimap<Integer, ValueAssignment> pAssignableTermsAtPosition,
        Set<ValueAssignment> pConstants, Set<ValueAssignment> pUfFunctionsWithoutSSAIndex) {

      assignableTermsAtPosition = ImmutableMultimap.copyOf(pAssignableTermsAtPosition);
      constants = ImmutableSet.copyOf(pConstants);
      ufFunctionsWithoutSSAIndex = ImmutableSet.copyOf(pUfFunctionsWithoutSSAIndex);
    }

    public Multimap<Integer, ValueAssignment> getAssignableTermsAtPosition() {
      return assignableTermsAtPosition;
    }

    public Set<ValueAssignment> getConstants() {
      return constants;
    }

    @SuppressWarnings("unused")
    public Set<ValueAssignment> getUfFunctionsWithoutSSAIndex() {
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
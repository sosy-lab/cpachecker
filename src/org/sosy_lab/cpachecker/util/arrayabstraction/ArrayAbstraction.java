// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMutableNetwork;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.arrayabstraction.ArrayAbstractionResult.Status;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Array abstraction algorithm. */
public class ArrayAbstraction {

  private ArrayAbstraction() {}

  private static BlankEdge createBlankEdge(AFunctionDeclaration pFunction, String pDescription) {
    return new BlankEdge(
        "", FileLocation.DUMMY, new CFANode(pFunction), new CFANode(pFunction), pDescription);
  }

  private static CAssumeEdge createAssumeEdge(
      AFunctionDeclaration pFunction, CExpression pCondition, boolean pTruthAssumption) {
    return new CAssumeEdge(
        "",
        pCondition.getFileLocation(),
        new CFANode(pFunction),
        new CFANode(pFunction),
        pCondition,
        pTruthAssumption);
  }

  private static ImmutableSet<TransformableArray> getLoopTransformableArrays(
      TransformableLoop pLoop,
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap) {

    return pLoop.getInnerLoopEdges().stream()
        .flatMap(edge -> ArrayAccess.findArrayAccesses(edge).stream())
        .map(arrayAccess -> getTransformableArray(arrayAccess, pTransformableArrayMap))
        .flatMap(Optional::stream)
        .collect(ImmutableSet.toImmutableSet());
  }

  private static ImmutableSet<CFAEdge> getTransformableEdges(
      CfaMutableNetwork pGraph, TransformableLoop pLoop) {

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {
      builder.add(edge);
      if (edge instanceof FunctionSummaryEdge) {
        CFANode summaryEdgeNodeU = pGraph.incidentNodes(edge).nodeU();
        for (CFAEdge callEdge : pGraph.outEdges(summaryEdgeNodeU)) {
          if (callEdge instanceof FunctionCallEdge) {
            builder.add(callEdge);
          }
        }
      }
    }

    return builder.build();
  }

  // Index step condition is required to only allow loop body access, if:
  //   ((index) % (index step value)) == ((index initial value) % (index step value))
  private static Optional<CExpression> createIndexStepCondition(TransformableLoop.Index pIndex) {

    BigInteger updateStepValue = pIndex.getUpdateOperation().getStepValue();
    if (!updateStepValue.equals(BigInteger.ONE)) {

      CType indexType = pIndex.getVariableDeclaration().getType();

      CExpression indexExpression =
          new CIdExpression(FileLocation.DUMMY, pIndex.getVariableDeclaration());

      CExpression updateStepExpression =
          new CIntegerLiteralExpression(FileLocation.DUMMY, indexType, updateStepValue);

      CExpression indexRemainderExpression =
          new CBinaryExpression(
              FileLocation.DUMMY,
              indexType,
              indexType,
              indexExpression,
              updateStepExpression,
              CBinaryExpression.BinaryOperator.MODULO);

      BigInteger startValue = pIndex.getInitializeOperation().getValue();
      BigInteger remainder = startValue.remainder(updateStepValue);
      CExpression remainderExpression =
          new CIntegerLiteralExpression(FileLocation.DUMMY, indexType, remainder);

      CExpression condition =
          new CBinaryExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              indexType,
              indexRemainderExpression,
              remainderExpression,
              CBinaryExpression.BinaryOperator.EQUALS);

      return Optional.of(condition);
    } else {
      return Optional.empty();
    }
  }

  // Result: transformable array -> set of subscript expressions for all accesses of the array in
  // the specified loop
  private static ImmutableMultimap<TransformableArray, CExpression>
      getTransformableArraySubscriptExpressions(
          ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap,
          TransformableLoop pLoop) {

    ImmutableMultimap.Builder<TransformableArray, CExpression> builder =
        ImmutableSetMultimap.builder();

    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {
      for (ArrayAccess arrayAccess : ArrayAccess.findArrayAccesses(edge)) {
        Optional<TransformableArray> optTransformableArray =
            getTransformableArray(arrayAccess, pTransformableArrayMap);
        if (optTransformableArray.isPresent()) {
          builder.put(optTransformableArray.orElseThrow(), arrayAccess.getSubscriptExpression());
        }
      }
    }

    return builder.build();
  }

  private static boolean onlySingleUseNothingElse(EdgeDefUseData pEdgeDefUseData) {
    return pEdgeDefUseData.getUses().size() == 1
        && pEdgeDefUseData.getPointeeUses().isEmpty()
        && pEdgeDefUseData.getDefs().isEmpty()
        && pEdgeDefUseData.getPointeeDefs().isEmpty();
  }

  // In order to enter the loop body, the indices of the transformed arrays must fullfil certain
  // conditions that relate the array indices to the loop index (e.g., array_index == loop_index).
  // Assumptions (fullfil -> continue, otherwise -> abort) are inserted for these conditions.
  // Assumptions are used to prevent analyses from skipping some loops by letting these conditions
  // fail which can cause invalid analyses results.
  private static ImmutableMap<TransformableArray, CExpression> insertTransformableArrayAssumes(
      CfaMutableNetwork pGraph,
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap,
      TransformableLoop pLoop,
      ImmutableSet<TransformableArray> pLoopTransformableArrays,
      CFANode pLoopBodyEntryNode) {

    ImmutableMap.Builder<TransformableArray, CExpression> arrayPreciseSubscriptExpressionBuilder =
        ImmutableMap.builder();

    TransformableLoop.Index index = pLoop.getIndex();
    ImmutableMultimap<TransformableArray, CExpression> transformableArraySubscriptExpressions =
        getTransformableArraySubscriptExpressions(pTransformableArrayMap, pLoop);
    EdgeDefUseData.Extractor extractor = EdgeDefUseData.createExtractor(true);

    List<CExpression> conditions = new ArrayList<>();
    for (TransformableArray transformableArray : pLoopTransformableArrays) {

      CIdExpression arrayIndexIdExpression =
          new CIdExpression(FileLocation.DUMMY, transformableArray.getIndexDeclaration());

      // If the array is always accessed by the same subscript expression, we try to use it as the
      // condition operand.
      ImmutableCollection<CExpression> subscriptExpressions =
          transformableArraySubscriptExpressions.get(transformableArray);
      CExpression conditionOperand = null;
      if (subscriptExpressions.size() == 1) {
        CExpression subscriptExpression = subscriptExpressions.stream().findFirst().orElseThrow();
        EdgeDefUseData subscriptDefUseData = extractor.extract(subscriptExpression);
        if (onlySingleUseNothingElse(subscriptDefUseData)) {
          MemoryLocation subscriptUse =
              subscriptDefUseData.getUses().stream().findFirst().orElseThrow();
          if (subscriptUse.equals(MemoryLocation.forDeclaration(index.getVariableDeclaration()))) {
            conditionOperand = subscriptExpression;
          }
        }
      }

      if (conditionOperand == null) {
        conditionOperand = new CIdExpression(FileLocation.DUMMY, index.getVariableDeclaration());
      }

      arrayPreciseSubscriptExpressionBuilder.put(transformableArray, conditionOperand);

      CExpression conditionExpression =
          new CBinaryExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              index.getVariableDeclaration().getType(),
              arrayIndexIdExpression,
              conditionOperand,
              CBinaryExpression.BinaryOperator.EQUALS);

      conditions.add(conditionExpression);
    }

    AFunctionDeclaration function = pLoopBodyEntryNode.getFunction();

    for (CExpression condition : conditions) {

      CAssumeEdge enterBodyEdge = createAssumeEdge(function, condition, true);
      pGraph.insertSuccessor(pLoopBodyEntryNode, new CFANode(function), enterBodyEdge);

      CAssumeEdge falseEdge = createAssumeEdge(function, condition, false);
      pGraph.addEdge(pLoopBodyEntryNode, new CFATerminationNode(function), falseEdge);
    }

    return arrayPreciseSubscriptExpressionBuilder.buildOrThrow();
  }

  // Is this loop even transformable? Yes -> Status.PRECISE, No -> Status.UNCHANGED
  private static Status loopTransformable(
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap,
      TransformableLoop pLoop) {

    ImmutableSet<MemoryLocation> innerLoopDeclarations =
        pLoop.getInnerLoopDeclarations().stream()
            .map(MemoryLocation::forDeclaration)
            .collect(ImmutableSet.toImmutableSet());
    MemoryLocation loopIndexMemLoc =
        MemoryLocation.forDeclaration(pLoop.getIndex().getVariableDeclaration());

    EdgeDefUseData.Extractor defUseDataExtractor = EdgeDefUseData.createExtractor(true);
    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {

      Set<MemoryLocation> arrayMemoryLocations = new HashSet<>();

      // non-transformable arrays in a transformable loop make the loop non-transformable
      for (CSimpleDeclaration arrayDeclaration : ArrayAccess.findArrayOccurences(edge)) {
        if (!pTransformableArrayMap.containsKey(arrayDeclaration)) {
          return Status.UNCHANGED;
        }

        arrayMemoryLocations.add(MemoryLocation.forDeclaration(arrayDeclaration));
      }

      EdgeDefUseData edgeDefUseData = defUseDataExtractor.extract(edge);
      for (MemoryLocation def : edgeDefUseData.getDefs()) {
        if (!innerLoopDeclarations.contains(def)
            && !arrayMemoryLocations.contains(def)
            && !def.equals(loopIndexMemLoc)) {
          return Status.UNCHANGED;
        }
      }

      // any pointee def make a loop non-transformable
      if (!edgeDefUseData.getPointeeDefs().isEmpty()) {
        return Status.UNCHANGED;
      }
    }

    return Status.PRECISE;
  }

  private static void insertLoopConditions(
      CfaMutableNetwork pGraph,
      ImmutableSet<TransformableArray> pLoopTransformableArrays,
      TransformableLoop pLoop,
      CFANode pBodyEntryNode,
      CFANode pBodyExitNode) {

    AFunctionDeclaration function = pLoop.getLoopNode().getFunction();
    TransformableLoop.Index index = pLoop.getIndex();

    TransformableArray anyTransformableArray =
        pLoopTransformableArrays.stream().findFirst().orElseThrow();

    CIdExpression loopIndexIdExpression =
        new CIdExpression(FileLocation.DUMMY, index.getVariableDeclaration());
    CIdExpression anyTransformableArrayIndex =
        new CIdExpression(FileLocation.DUMMY, anyTransformableArray.getIndexDeclaration());
    CStatement indexInitStatement =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, loopIndexIdExpression, anyTransformableArrayIndex);
    CStatementEdge indexInitStatementEdge =
        new CStatementEdge(
            "",
            indexInitStatement,
            FileLocation.DUMMY,
            new CFANode(function),
            new CFANode(function));
    pGraph.insertPredecessor(new CFANode(function), pBodyEntryNode, indexInitStatementEdge);

    List<CExpression> conditions = new ArrayList<>();
    CExpression indexStartValueExpression =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            index.getVariableDeclaration().getType(),
            index.getInitializeOperation().getValue());
    CExpression indexEndValueExpression =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            index.getVariableDeclaration().getType(),
            index.getComparisonOperation().getValue());

    CBinaryExpression.BinaryOperator startBinaryOperator;
    CBinaryExpression.BinaryOperator endBinaryOperator;

    if (index.isIncreasing()) {
      assert index.getComparisonOperation().getOperator()
          == SpecialOperation.ConstantComparison.Operator.LESS_EQUAL;
      startBinaryOperator = CBinaryExpression.BinaryOperator.GREATER_EQUAL;
      endBinaryOperator = CBinaryExpression.BinaryOperator.LESS_EQUAL;
    } else {
      assert index.isDecreasing();
      assert index.getComparisonOperation().getOperator()
          == SpecialOperation.ConstantComparison.Operator.GREATER_EQUAL;
      startBinaryOperator = CBinaryExpression.BinaryOperator.LESS_EQUAL;
      endBinaryOperator = CBinaryExpression.BinaryOperator.GREATER_EQUAL;
    }

    conditions.add(
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            index.getVariableDeclaration().getType(),
            loopIndexIdExpression,
            indexStartValueExpression,
            startBinaryOperator));
    conditions.add(
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            index.getVariableDeclaration().getType(),
            loopIndexIdExpression,
            indexEndValueExpression,
            endBinaryOperator));

    createIndexStepCondition(index).ifPresent(conditions::add);

    // reverse list, because the last edge inserted with insertSuccessor is the first condition edge
    Collections.reverse(conditions);
    for (CExpression condition : conditions) {

      CAssumeEdge enterBodyEdge = createAssumeEdge(function, condition, true);
      pGraph.insertSuccessor(pBodyEntryNode, new CFANode(function), enterBodyEdge);

      CAssumeEdge skipBodyEdge = createAssumeEdge(function, condition, false);
      pGraph.addEdge(pBodyEntryNode, pBodyExitNode, skipBodyEdge);
    }
  }

  private static Status transformLoop(
      CfaMutableNetwork pGraph,
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap,
      CFA pCfa,
      LogManager pLogger,
      TransformableLoop pLoop) {

    Status status = loopTransformable(pTransformableArrayMap, pLoop);

    if (status == Status.UNCHANGED) {
      return status;
    }

    // transform loop into branching
    CFANode loopNode = pLoop.getLoopNode();
    CFAEdge loopIncomingEdge = pLoop.getIncomingEdge();
    CFAEdge loopOutgoingEdge = pLoop.getOutgoingEdge();

    CFAEdge loopBodyFirstEdge = null;
    CFAEdge loopBodyLastEdge = null;
    for (CFAEdge edge : pGraph.incidentEdges(loopNode)) {
      if (pGraph.incidentNodes(edge).nodeU().equals(loopNode) && !edge.equals(loopOutgoingEdge)) {
        loopBodyFirstEdge = edge;
      }
      if (pGraph.incidentNodes(edge).nodeV().equals(loopNode) && !edge.equals(loopIncomingEdge)) {
        loopBodyLastEdge = edge;
      }
    }

    CFANode loopBodyFirstEdgeNodeV = pGraph.incidentNodes(loopBodyFirstEdge).nodeV();
    CFANode loopOutgoingEdgeNodeV = pGraph.incidentNodes(loopOutgoingEdge).nodeV();

    pGraph.removeEdge(loopBodyFirstEdge);
    pGraph.removeEdge(loopOutgoingEdge);

    CFANode loopIncomingEdgeNodeU = pGraph.incidentNodes(loopIncomingEdge).nodeU();
    pGraph.removeEdge(loopIncomingEdge);
    pGraph.addEdge(loopIncomingEdgeNodeU, loopBodyFirstEdgeNodeV, loopIncomingEdge);

    CFANode loopBodyLastEdgeNodeU = pGraph.incidentNodes(loopBodyLastEdge).nodeU();
    pGraph.removeEdge(loopBodyLastEdge);
    pGraph.addEdge(loopBodyLastEdgeNodeU, loopOutgoingEdgeNodeV, loopBodyLastEdge);

    ImmutableSet<TransformableArray> loopTransformableArrays =
        getLoopTransformableArrays(pLoop, pTransformableArrayMap);
    assert loopTransformableArrays.size() >= 1;

    CFANode bodyEntryNode = loopBodyFirstEdgeNodeV;
    CFANode bodyExitNode = loopOutgoingEdgeNodeV;

    ImmutableMap<TransformableArray, CExpression> preciseArraySubscriptExpressions =
        insertTransformableArrayAssumes(
            pGraph, pTransformableArrayMap, pLoop, loopTransformableArrays, bodyEntryNode);

    // transform inner loop edges
    for (CFAEdge edge : getTransformableEdges(pGraph, pLoop)) {
      Status edgeTransformationStatus =
          transformEdge(
              pGraph,
              pTransformableArrayMap,
              pCfa,
              pLogger,
              edge,
              Optional.of(pLoop),
              Optional.of(preciseArraySubscriptExpressions));
      if (edgeTransformationStatus == Status.UNCHANGED) {
        return Status.UNCHANGED;
      } else if (edgeTransformationStatus == Status.IMPRECISE) {
        status = Status.IMPRECISE;
      }
    }

    insertLoopConditions(pGraph, loopTransformableArrays, pLoop, bodyEntryNode, bodyExitNode);

    // replace index update edge with blank placeholder edge
    TransformableLoop.Index index = pLoop.getIndex();
    var indexUpdateEdgeEndpoints = pGraph.incidentNodes(index.getUpdateEdge());
    pGraph.removeEdge(index.getUpdateEdge());
    pGraph.addEdge(
        indexUpdateEdgeEndpoints,
        createBlankEdge(indexUpdateEdgeEndpoints.nodeU().getFunction(), ""));

    return status;
  }

  private static ImmutableMap<CSimpleDeclaration, TransformableArray> createTransformableArrayMap(
      ImmutableSet<TransformableArray> pTransformableArrays) {

    ImmutableMap.Builder<CSimpleDeclaration, TransformableArray> builder =
        ImmutableMap.builderWithExpectedSize(pTransformableArrays.size());

    for (TransformableArray transformableArray : pTransformableArrays) {
      builder.put(transformableArray.getArrayDeclaration(), transformableArray);
    }

    return builder.buildOrThrow();
  }

  private static ImmutableSet<CFAEdge> createInnerLoopEdgeSet(
      ImmutableSet<TransformableLoop> pTransformableLoops) {

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    for (TransformableLoop transformableLoop : pTransformableLoops) {
      builder.addAll(transformableLoop.getInnerLoopEdges());
    }

    return builder.build();
  }

  private static Optional<TransformableArray> getTransformableArray(
      ArrayAccess pArrayAccess,
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap) {

    CExpression arrayExpression = pArrayAccess.getArrayExpression();

    if (arrayExpression instanceof CIdExpression) {
      CSimpleDeclaration declaration = ((CIdExpression) arrayExpression).getDeclaration();
      TransformableArray transformableArray = pTransformableArrayMap.get(declaration);
      return Optional.ofNullable(transformableArray);
    }

    throw new AssertionError("Unknown array expression: " + arrayExpression);
  }

  private static ImmutableSet<TransformableLoop> findRelevantTransformableLoops(
      CFA pCfa, LogManager pLogger) {

    ImmutableMap<CSimpleDeclaration, TransformableArray> transformableArrayMap =
        createTransformableArrayMap(TransformableArray.findTransformableArrays(pCfa));

    return TransformableLoop.findTransformableLoops(pCfa, pLogger).stream()
        .filter(loop -> !getLoopTransformableArrays(loop, transformableArrayMap).isEmpty())
        .collect(ImmutableSet.toImmutableSet());
  }

  private static ImmutableSet<TransformableArray> findRelevantTransformableArrays(
      CFA pCfa, ImmutableSet<TransformableLoop> pTransformableLoops) {

    ImmutableSet<TransformableArray> transformableArrays =
        TransformableArray.findTransformableArrays(pCfa);
    ImmutableMap<CSimpleDeclaration, TransformableArray> transformableArrayMap =
        createTransformableArrayMap(transformableArrays);

    ImmutableSet.Builder<TransformableArray> relevantTransformableArraysBuilder =
        ImmutableSet.builder();

    for (CFAEdge edge : createInnerLoopEdgeSet(pTransformableLoops)) {
      for (ArrayAccess arrayAccess : ArrayAccess.findArrayAccesses(edge)) {
        Optional<TransformableArray> optTransformableArray =
            getTransformableArray(arrayAccess, transformableArrayMap);
        if (optTransformableArray.isPresent()) {
          relevantTransformableArraysBuilder.add(optTransformableArray.orElseThrow());
        }
      }
    }

    return relevantTransformableArraysBuilder.build();
  }

  private static Status transformEdge(
      CfaMutableNetwork pGraph,
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap,
      CFA pCfa,
      LogManager pLogger,
      CFAEdge pEdge,
      Optional<TransformableLoop> pLoop,
      Optional<ImmutableMap<TransformableArray, CExpression>> pPreciseArraySubscriptExpressions) {

    ImmutableSet<ArrayAccess> arrayAccesses = ArrayAccess.findArrayAccesses(pEdge);

    // We can only handle CFAs with a single array access per edge.
    // Prior simplification should already guarantee that.
    assert arrayAccesses.size() <= 1;

    Optional<ArrayAccess> optArrayAccess = arrayAccesses.stream().findFirst();
    if (optArrayAccess.isPresent()) {

      ArrayAccess arrayAccess = optArrayAccess.orElseThrow();

      Optional<TransformableArray> optTransformableArray =
          getTransformableArray(arrayAccess, pTransformableArrayMap);
      if (optTransformableArray.isEmpty()) {
        return pLoop.isEmpty() ? Status.PRECISE : Status.UNCHANGED;
      }

      TransformableArray transformableArray = optTransformableArray.orElseThrow();

      CExpression subscriptExpression = arrayAccess.getSubscriptExpression();

      // check whether access is at a precise subscript expression
      if (pPreciseArraySubscriptExpressions.isPresent() && pLoop.isPresent()) {

        CExpression preciseSubscriptExpression =
            pPreciseArraySubscriptExpressions.orElseThrow().get(transformableArray);

        TransformableLoop loop = pLoop.orElseThrow();
        CFAEdge updateIndexEdge = loop.getIndex().getUpdateEdge();

        CFAEdge postDominatedEdge = pEdge;
        if (pEdge instanceof CFunctionCallEdge) {
          postDominatedEdge = ((CFunctionCallEdge) pEdge).getSummaryEdge();
        }

        if (subscriptExpression.equals(preciseSubscriptExpression)
            && loop.getPostDominatedInnerLoopEdges(updateIndexEdge).contains(postDominatedEdge)) {
          return Status.PRECISE;
        }
      }

      String functionName = pEdge.getSuccessor().getFunctionName();
      MachineModel machineModel = pCfa.getMachineModel();
      Optional<BigInteger> optSubscriptValue =
          SpecialOperation.eval(
              subscriptExpression,
              functionName,
              machineModel,
              pLogger,
              new ValueAnalysisState(machineModel));

      if (arrayAccess.isRead()) {
        if (optSubscriptValue.isPresent()) {
          // TODO: better implementation that does not return Status.UNCHANGED
          return Status.UNCHANGED;
        } else {
          // TODO: better implementation that does not return Status.UNCHANGED
          return Status.UNCHANGED;
        }
      } else {
        assert arrayAccess.isWrite();

        var edgeEndpoints = pGraph.incidentNodes(pEdge);

        if (optSubscriptValue.isPresent()) {

          CIdExpression indexIdExpression =
              new CIdExpression(
                  subscriptExpression.getFileLocation(), transformableArray.getIndexDeclaration());
          CExpression accessCondition =
              new CBinaryExpression(
                  subscriptExpression.getFileLocation(),
                  subscriptExpression.getExpressionType(),
                  subscriptExpression.getExpressionType(),
                  subscriptExpression,
                  indexIdExpression,
                  CBinaryExpression.BinaryOperator.EQUALS);
          AFunctionDeclaration function = edgeEndpoints.nodeU().getFunction();
          CFANode newPredecessor = new CFANode(function);

          pGraph.insertPredecessor(
              newPredecessor,
              edgeEndpoints.nodeU(),
              createAssumeEdge(function, accessCondition, true));
          pGraph.addEdge(
              newPredecessor,
              edgeEndpoints.nodeV(),
              createAssumeEdge(function, accessCondition, false));

          return Status.PRECISE;
        } else {

          pGraph.removeEdge(pEdge);
          pGraph.addEdge(
              edgeEndpoints,
              createBlankEdge(
                  edgeEndpoints.nodeU().getFunction(), "removed: " + pEdge.getRawStatement()));

          return Status.IMPRECISE;
        }
      }
    } else {
      return Status.PRECISE;
    }
  }

  private static CFA createSimplifiedCfa(
      Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    CFA fstCfa =
        CfaSimplifications.simplifyArrayAccesses(
            pConfiguration, pLogger, pCfa, new VariableGenerator("__array_access_variable_"));

    CFA sndCfa = CfaSimplifications.simplifyIncDecLoopEdges(pConfiguration, pLogger, fstCfa);

    return sndCfa;
  }

  private static void transformArrayDeclarations(
      CfaMutableNetwork pGraph, ImmutableSet<TransformableArray> pTransformableArrays) {

    for (TransformableArray transformableArray : pTransformableArrays) {

      CDeclarationEdge arrayDeclarationEdge = transformableArray.getArrayDeclarationEdge();
      var arrayDeclarationEdgeEndpoints = pGraph.incidentNodes(arrayDeclarationEdge);
      CFANode arrayDeclarationEdgeNodeV = arrayDeclarationEdgeEndpoints.nodeV();
      pGraph.removeEdge(arrayDeclarationEdge);
      pGraph.addEdge(
          arrayDeclarationEdgeEndpoints,
          createBlankEdge(arrayDeclarationEdgeNodeV.getFunction(), ""));

      AFunctionDeclaration function = arrayDeclarationEdgeNodeV.getFunction();
      pGraph.insertPredecessor(
          new CFANode(function),
          arrayDeclarationEdgeNodeV,
          transformableArray.getValueDeclarationEdge());
      pGraph.insertPredecessor(
          new CFANode(function),
          arrayDeclarationEdgeNodeV,
          transformableArray.getIndexDeclarationEdge());

      CType arrayIndexType = transformableArray.getIndexDeclaration().getType();
      CIdExpression arrayIndexExpression =
          new CIdExpression(
              arrayDeclarationEdge.getFileLocation(), transformableArray.getIndexDeclaration());

      CExpression minIndexCondition =
          new CBinaryExpression(
              arrayDeclarationEdge.getFileLocation(),
              CNumericTypes.INT,
              arrayIndexType,
              arrayIndexExpression,
              CIntegerLiteralExpression.ZERO,
              CBinaryExpression.BinaryOperator.GREATER_EQUAL);

      pGraph.insertSuccessor(
          arrayDeclarationEdgeNodeV,
          new CFANode(function),
          createAssumeEdge(function, minIndexCondition, true));
      pGraph.addEdge(
          arrayDeclarationEdgeNodeV,
          new CFATerminationNode(function),
          createAssumeEdge(function, minIndexCondition, false));

      CExpression maxIndexCondition =
          new CBinaryExpression(
              arrayDeclarationEdge.getFileLocation(),
              CNumericTypes.INT,
              arrayIndexType,
              arrayIndexExpression,
              transformableArray.getLengthExpression(),
              CBinaryExpression.BinaryOperator.LESS_THAN);

      pGraph.insertSuccessor(
          arrayDeclarationEdgeNodeV,
          new CFANode(function),
          createAssumeEdge(function, maxIndexCondition, true));
      pGraph.addEdge(
          arrayDeclarationEdgeNodeV,
          new CFATerminationNode(function),
          createAssumeEdge(function, maxIndexCondition, false));
    }
  }

  public static ArrayAbstractionResult transformCfa(
      Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    checkNotNull(pConfiguration);
    checkNotNull(pLogger);
    checkNotNull(pCfa);

    CFA simplifiedCfa = createSimplifiedCfa(pConfiguration, pLogger, pCfa);

    ImmutableSet<TransformableLoop> transformableLoops =
        findRelevantTransformableLoops(simplifiedCfa, pLogger);
    ImmutableSet<TransformableArray> transformableArrays =
        findRelevantTransformableArrays(simplifiedCfa, transformableLoops);
    ImmutableMap<CSimpleDeclaration, TransformableArray> transformableArrayMap =
        createTransformableArrayMap(transformableArrays);

    if (transformableLoops.isEmpty() || transformableArrays.isEmpty()) {
      return ArrayAbstractionResult.createUnchanged(pCfa);
    }

    CfaMutableNetwork graph = CfaMutableNetwork.of(simplifiedCfa);

    Status status = Status.PRECISE;

    // transform loops
    for (TransformableLoop transformableLoop : transformableLoops) {
      Status loopTransformationStatus =
          transformLoop(graph, transformableArrayMap, simplifiedCfa, pLogger, transformableLoop);
      if (loopTransformationStatus == Status.UNCHANGED) {
        return ArrayAbstractionResult.createUnchanged(pCfa);
      } else if (loopTransformationStatus == Status.IMPRECISE) {
        status = Status.IMPRECISE;
      }
    }

    // handle array access edges outside transformable loops
    ImmutableSet<CFAEdge> transformedEdges =
        transformableLoops.stream()
            .flatMap(loop -> getTransformableEdges(graph, loop).stream())
            .collect(ImmutableSet.toImmutableSet());
    for (CFAEdge edge : ImmutableSet.copyOf(graph.edges())) {
      if (!transformedEdges.contains(edge)) {
        Status edgeTransformationStatus =
            transformEdge(
                graph,
                transformableArrayMap,
                simplifiedCfa,
                pLogger,
                edge,
                Optional.empty(),
                Optional.empty());
        if (edgeTransformationStatus == Status.UNCHANGED) {
          return ArrayAbstractionResult.createUnchanged(pCfa);
        } else if (edgeTransformationStatus == Status.IMPRECISE) {
          status = Status.IMPRECISE;
        }
      }
    }

    transformArrayDeclarations(graph, transformableArrays);

    // initialize array access substitution
    CAstNodeSubstitution substitution = new CAstNodeSubstitution();
    for (CFAEdge edge : graph.edges()) {
      for (ArrayAccess arrayAccess : ArrayAccess.findArrayAccesses(edge)) {
        Optional<TransformableArray> optTransformableArray =
            getTransformableArray(arrayAccess, transformableArrayMap);
        if (optTransformableArray.isPresent()) {
          TransformableArray transformableArray = optTransformableArray.orElseThrow();
          CExpression arrayAccessExpression = arrayAccess.getExpression();
          CSimpleDeclaration valueDeclaration = transformableArray.getValueDeclaration();
          CIdExpression valueIdExpression =
              new CIdExpression(arrayAccessExpression.getFileLocation(), valueDeclaration);
          substitution.insertSubstitute(edge, arrayAccessExpression, valueIdExpression);
        }
      }
    }

    CFA transformedCfa =
        CCfaTransformer.createCfa(
            pConfiguration,
            pLogger,
            simplifiedCfa,
            graph,
            (edge, originalAstNode) ->
                originalAstNode.accept(
                    new SubstitutingCAstNodeVisitor(
                        node -> substitution.getSubstitute(edge, node))));

    return new ArrayAbstractionResult(
        status, transformedCfa, transformableArrays, transformableLoops);
  }
}

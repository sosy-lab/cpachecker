// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCfaNetwork;
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
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.arrayabstraction.ArrayAbstractionResult.Status;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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

  private static ImmutableSet<TransformableArray> getTransformableArrays(
      TransformableLoop pLoop,
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap) {

    ImmutableSet.Builder<TransformableArray> builder = ImmutableSet.builder();

    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {
      for (ArrayAccess arrayAccess : ArrayAccess.findArrayAccesses(edge)) {
        Optional<TransformableArray> optTransformableArray =
            getTransformableArray(arrayAccess, pTransformableArrayMap);
        if (optTransformableArray.isPresent()) {
          builder.add(optTransformableArray.orElseThrow());
        }
      }
    }

    return builder.build();
  }

  private static ImmutableSet<CFAEdge> getTransformableEdges(
      MutableCfaNetwork pGraph, TransformableLoop pLoop) {

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

  private static void insertTransformableArrayAssumes(
      MutableCfaNetwork pGraph,
      TransformableLoop pLoop,
      ImmutableSet<TransformableArray> pLoopTransformableArrays,
      CFANode pLoopBodyEntryNode) {

    TransformableLoop.Index index = pLoop.getIndex();

    List<CExpression> conditions = new ArrayList<>();
    for (TransformableArray transformableArray : pLoopTransformableArrays) {

      CIdExpression loopIndexIdExpression =
          new CIdExpression(FileLocation.DUMMY, index.getVariableDeclaration());

      CIdExpression arrayIndexIdExpression =
          new CIdExpression(FileLocation.DUMMY, transformableArray.getIndexDeclaration());

      CExpression conditionExpression =
          new CBinaryExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              index.getVariableDeclaration().getType(),
              loopIndexIdExpression,
              arrayIndexIdExpression,
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
  }

  // Is this loop even transformable?
  private static Status loopTransformable(
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap,
      TransformableLoop pLoop) {

    // TODO: better implementation that is not as strict and supports more kinds of imprecision
    ImmutableSet<MemoryLocation> innerLoopDeclarations =
        pLoop.getInnerLoopDeclarations().stream()
            .map(MemoryLocation::forDeclaration)
            .collect(ImmutableSet.toImmutableSet());
    MemoryLocation loopIndexMemLoc =
        MemoryLocation.forDeclaration(pLoop.getIndex().getVariableDeclaration());
    EdgeDefUseData.Extractor defUseDataExtractor = EdgeDefUseData.createExtractor(true);
    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {

      // non-transformable arrays in a transformable loop make the loop non-transformable
      for (CSimpleDeclaration arrayDeclaration : ArrayAccess.findArrayOccurences(edge)) {
        if (!pTransformableArrayMap.containsKey(arrayDeclaration)) {
          return Status.FAILED;
        }
      }

      EdgeDefUseData edgeDefUseData = defUseDataExtractor.extract(edge);
      for (MemoryLocation def : edgeDefUseData.getDefs()) {
        if (!innerLoopDeclarations.contains(def)
            && !def.equals(loopIndexMemLoc)) {
          return Status.FAILED;
        }
      }

      // any pointee def make a loop non-transformable
      if (!edgeDefUseData.getPointeeDefs().isEmpty()) {
        return Status.FAILED;
      }
    }

    return Status.PRECISE;
  }

  private static Status transformLoop(
      MutableCfaNetwork pGraph,
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap,
      CFA pCfa,
      TransformableLoop pLoop) {

    Status status = loopTransformable(pTransformableArrayMap, pLoop);

    if (status == Status.FAILED) {
      return status;
    }

    // transform inner loop edges
    for (CFAEdge edge : getTransformableEdges(pGraph, pLoop)) {
      Status edgeTransformationStatus =
          transformEdge(pGraph, pTransformableArrayMap, pCfa, edge, Optional.of(pLoop));
      if (edgeTransformationStatus == Status.FAILED) {
        return Status.FAILED;
      } else if (edgeTransformationStatus == Status.IMPRECISE) {
        status = Status.IMPRECISE;
      }
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

    // insert index initialization and branch conditions
    ImmutableSet<TransformableArray> loopTransformableArrays =
        getTransformableArrays(pLoop, pTransformableArrayMap);
    assert loopTransformableArrays.size() >= 1;
    TransformableArray anyTransformableArray =
        loopTransformableArrays.stream().findAny().orElseThrow();

    CFANode bodyEntryNode = loopBodyFirstEdgeNodeV;
    CFANode bodyExitNode = loopOutgoingEdgeNodeV;
    AFunctionDeclaration function = loopNode.getFunction();
    TransformableLoop.Index index = pLoop.getIndex();

    insertTransformableArrayAssumes(pGraph, pLoop, loopTransformableArrays, bodyEntryNode);

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
    pGraph.insertPredecessor(new CFANode(function), bodyEntryNode, indexInitStatementEdge);

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
          == SpecialOperation.ComparisonAssume.Operator.LESS_EQUAL;
      startBinaryOperator = CBinaryExpression.BinaryOperator.GREATER_EQUAL;
      endBinaryOperator = CBinaryExpression.BinaryOperator.LESS_EQUAL;
    } else {
      assert index.isDecreasing();
      assert index.getComparisonOperation().getOperator()
          == SpecialOperation.ComparisonAssume.Operator.GREATER_EQUAL;
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

    // reverse list to make the element added to the list first the outermost condition in the CFA
    Collections.reverse(conditions);
    for (CExpression condition : conditions) {

      CAssumeEdge enterBodyEdge = createAssumeEdge(function, condition, true);
      pGraph.insertSuccessor(bodyEntryNode, new CFANode(function), enterBodyEdge);

      CAssumeEdge skipBodyEdge = createAssumeEdge(function, condition, false);
      pGraph.addEdge(bodyEntryNode, bodyExitNode, skipBodyEdge);
    }

    // replace index update edge with blank placeholder
    var indexUpdateEdgeEndpoints = pGraph.incidentNodes(index.getUpdateEdge());
    pGraph.removeEdge(index.getUpdateEdge());
    pGraph.addEdge(
        indexUpdateEdgeEndpoints,
        createBlankEdge(
            indexUpdateEdgeEndpoints.nodeU().getFunction(),
            "removed: " + index.getUpdateEdge().getRawStatement()));

    return status;
  }

  private static ImmutableMap<CSimpleDeclaration, TransformableArray> createTransformableArrayMap(
      ImmutableSet<TransformableArray> pTransformableArrays) {

    ImmutableMap.Builder<CSimpleDeclaration, TransformableArray> builder =
        ImmutableMap.builderWithExpectedSize(pTransformableArrays.size());

    for (TransformableArray transformableArray : pTransformableArrays) {
      builder.put(transformableArray.getArrayDeclaration(), transformableArray);
    }

    return builder.build();
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

  private static ImmutableSet<TransformableLoop> findRelevantTransformableLoops(CFA pCfa) {

    ImmutableSet<TransformableArray> transformableArrays =
        TransformableArray.findTransformableArrays(pCfa);
    ImmutableMap<CSimpleDeclaration, TransformableArray> transformableArrayMap =
        createTransformableArrayMap(transformableArrays);

    ImmutableSet.Builder<TransformableLoop> relevantTransformableLoopsBuilder =
        ImmutableSet.builder();

    outer:
    for (TransformableLoop transformableLoop : TransformableLoop.findTransformableLoops(pCfa)) {
      for (CFAEdge edge : transformableLoop.getInnerLoopEdges()) {
        for (ArrayAccess arrayAccess : ArrayAccess.findArrayAccesses(edge)) {
          if (getTransformableArray(arrayAccess, transformableArrayMap).isPresent()) {
            relevantTransformableLoopsBuilder.add(transformableLoop);
            continue outer;
          }
        }
      }
    }

    return relevantTransformableLoopsBuilder.build();
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
      MutableCfaNetwork pGraph,
      ImmutableMap<CSimpleDeclaration, TransformableArray> pTransformableArrayMap,
      CFA pCfa,
      CFAEdge pEdge,
      Optional<TransformableLoop> pLoop) {

    ImmutableSet<ArrayAccess> arrayAccesses = ArrayAccess.findArrayAccesses(pEdge);

    // We can only handle CFAs with a single array access per edge.
    // Prior simplification should already guarantee that.
    assert arrayAccesses.size() <= 1;

    Optional<ArrayAccess> optArrayAccess = arrayAccesses.stream().findAny();
    if (optArrayAccess.isPresent()) {

      ArrayAccess arrayAccess = optArrayAccess.orElseThrow();

      Optional<TransformableArray> optTransformableArray =
          getTransformableArray(arrayAccess, pTransformableArrayMap);
      if (optTransformableArray.isEmpty()) {
        return pLoop.isEmpty() ? Status.PRECISE : Status.FAILED;
      }

      TransformableArray transformableArray = optTransformableArray.orElseThrow();

      CExpression subscriptExpression = arrayAccess.getSubscriptExpression();

      // check whether access is at loop index and return Status.PRECISE if it is
      if (subscriptExpression instanceof CIdExpression && pLoop.isPresent()) {

        CIdExpression subscriptIdExpression = (CIdExpression) subscriptExpression;
        CSimpleDeclaration subscriptDeclaration = subscriptIdExpression.getDeclaration();

        TransformableLoop loop = pLoop.orElseThrow();
        CSimpleDeclaration indexDeclaration = loop.getIndex().getVariableDeclaration();
        CFAEdge updateIndexEdge = loop.getIndex().getUpdateEdge();

        CFAEdge postDominatedEdge = pEdge;
        if (pEdge instanceof CFunctionCallEdge) {
          postDominatedEdge = ((CFunctionCallEdge) pEdge).getSummaryEdge();
        }

        if (indexDeclaration.equals(subscriptDeclaration)
            && loop.getPostDominatedInnerLoopEdges(updateIndexEdge).contains(postDominatedEdge)) {
          return Status.PRECISE;
        }
      }

      Optional<BigInteger> optSubscriptValue =
          SpecialOperation.eval(subscriptExpression, pCfa.getMachineModel(), ImmutableMap.of());

      if (arrayAccess.isRead()) {
        if (optSubscriptValue.isPresent()) {
          // TODO: better implementation that does not return Status.FAILED
          return Status.FAILED;
        } else {
          // TODO: better implementation that does not return Status.FAILED
          return Status.FAILED;
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
      MutableCfaNetwork pGraph, ImmutableSet<TransformableArray> pTransformableArrays) {

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
        findRelevantTransformableLoops(simplifiedCfa);
    ImmutableSet<TransformableArray> transformableArrays =
        findRelevantTransformableArrays(simplifiedCfa, transformableLoops);
    ImmutableMap<CSimpleDeclaration, TransformableArray> transformableArrayMap =
        createTransformableArrayMap(transformableArrays);

    MutableCfaNetwork graph = MutableCfaNetwork.of(simplifiedCfa);

    Status status = Status.PRECISE;

    // transform loops
    for (TransformableLoop transformableLoop : transformableLoops) {
      Status loopTransformationStatus =
          transformLoop(graph, transformableArrayMap, simplifiedCfa, transformableLoop);
      if (loopTransformationStatus == Status.FAILED) {
        return ArrayAbstractionResult.createFailed(pCfa);
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
            transformEdge(graph, transformableArrayMap, simplifiedCfa, edge, Optional.empty());
        if (edgeTransformationStatus == Status.FAILED) {
          return ArrayAbstractionResult.createFailed(pCfa);
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
        status, transformedCfa, transformableArrays.size(), transformableLoops.size());
  }
}

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
package org.sosy_lab.cpachecker.cpa.arg;

import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.SINK_NODE_ID;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.SourceLocationMapper;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlTag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

@Options(prefix="cpa.arg.witness")
public class ARGPathExport {

  private static final Function<ARGState, ARGState> COVERED_TO_COVERING = new Function<ARGState, ARGState>() {

    @Override
    public ARGState apply(ARGState pChild) {
      ARGState child = pChild;
      // The child might be covered by another state
      // --> switch to the covering state
      if (child.isCovered()) {
        child = child.getCoveringState();
        assert !child.isCovered();
      }
      return child;
    }

  };

  @Option(secure=true, description="Verification witness: Include function calls and function returns?")
  boolean exportFunctionCallsAndReturns = true;

  @Option(secure=true, description="Verification witness: Include assumptions (C statements)?")
  boolean exportAssumptions = true;

  @Option(secure=true, description="Verification witness: Include the considered case of an assume?")
  boolean exportAssumeCaseInfo = true;

  @Option(secure=true, description="Verification witness: Include the (starting) line numbers of the operations on the transitions?")
  boolean exportLineNumbers = true;

  @Option(secure=true, description="Verification witness: Include the sourcecode of the operations?")
  boolean exportSourcecode = true;

  @Option(secure=true, description="Verification witness: Include the offset within the file?")
  boolean exportOffset = true;


  public ARGPathExport(Configuration pConfig) throws InvalidConfigurationException {
    Preconditions.checkNotNull(pConfig);
    pConfig.inject(this);
  }

  private String getStateIdent(ARGState pState) {
    return getStateIdent(pState, "");
  }

  private String getStateIdent(ARGState pState, String pIdentPostfix) {
    return String.format("A%d%s", pState.getStateId(), pIdentPostfix);
  }

  private String getPseudoStateIdent(ARGState pState, int pSubStateNo, int pSubStateCount) {
    return getStateIdent(pState, String.format("_%d_%d", pSubStateNo, pSubStateCount));
  }

  private static class TransitionCondition {
    public final Map<KeyDef, String> keyValues = Maps.newHashMap();

    public void put(final KeyDef pKey, final String pValue) {
      keyValues.put(pKey, pValue);
    }

    @Override
    public boolean equals(Object pOther) {
      if (!(pOther instanceof TransitionCondition)) {
        return false;
      }

      TransitionCondition oT = (TransitionCondition) pOther;

      return this.keyValues.equals(oT.keyValues);
    }

    public boolean hasTransitionRestrictions() {
      return !keyValues.isEmpty();
    }

    @Override
    public int hashCode() {
      return keyValues.hashCode();
    }
  }

  private static class AggregatedEdge {
    public final String targetRepresentedBy;
    public final TransitionCondition condition;
    public final Set<String> aggregatesTargets = Sets.newHashSet();

    public AggregatedEdge(final String pTargetRepresentedBy,
        final TransitionCondition pCondition) {

      this.condition = pCondition;
      this.targetRepresentedBy = pTargetRepresentedBy;
      this.aggregatesTargets.add(pTargetRepresentedBy);
    }
  }

  public void writePath(Appendable pTarget,
      final ARGState pRootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
      final Predicate<? super ARGState> pPathElements,
      final CounterexampleInfo pCounterExample)
      throws IOException {

    String defaultFileName = getInitialFileName(pRootState);
    WitnessWriter writer = new WitnessWriter(defaultFileName);
    writer.writePath(pTarget, pRootState, pSuccessorFunction, pPathElements, pCounterExample);
  }

  private String getInitialFileName(ARGState pRootState) {
    CFANode initialLoc = AbstractStates.extractLocation(pRootState);
    Deque<CFANode> worklist = Queues.newArrayDeque();
    worklist.push(initialLoc);

    while (!worklist.isEmpty()) {
      CFANode l = worklist.pop();
      for (CFAEdge e: CFAUtils.leavingEdges(l)) {
        Set<FileLocation> fileLocations = SourceLocationMapper.getFileLocationsFromCfaEdge(e);
        if (fileLocations.size() > 0) {
          String fileName = fileLocations.iterator().next().getFileName();
          if (fileName != null) {
            return fileName;
          }
        }
        worklist.push(e.getSuccessor());
      }
    }

    throw new RuntimeException("Could not determine file name based on abstract state!");
  }

  private class WitnessWriter {

    private final Map<String, String> mergedNodes = Maps.newHashMap();
    private final Multimap<String, AggregatedEdge> sourceToTargetMap = HashMultimap.create();
    private final Multimap<String, AggregatedEdge> targetToSourceMap = HashMultimap.create();
    private final Multimap<String, NodeFlag> nodeFlags = HashMultimap.create();
    private final Multimap<String, String> violatedProperties = HashMultimap.create();
    private final Map<String, Element> delayedNodes = Maps.newHashMap();
    private final Map<DelayedAssignmentsKey, CFAEdgeWithAssumptions> delayedAssignments = Maps.newHashMap();
    private final SortedSetMultimap<String, Element> confirmedElements =
        TreeMultimap.create(Ordering.natural(), new Comparator<Element>() {

          @Override
          public int compare(Element pO1, Element pO2) {
            String tagName1 = pO1.getTagName();
            String tagName2 = pO2.getTagName();
            if (tagName1.equals(GraphMlTag.NODE) && tagName2.equals(GraphMlTag.EDGE)) {
              return -1;
            } else if (tagName1.equals(GraphMlTag.EDGE.text) && tagName2.equals(GraphMlTag.NODE.text)) {
              return 1;
            }
            return Ordering.arbitrary().compare(pO1, pO2);
          }

        });

    private final String defaultSourcefileName;
    private boolean isFunctionScope = false;

    public WitnessWriter(@Nullable String pDefaultSourcefileName) {
      this.defaultSourcefileName = pDefaultSourcefileName;
    }

    private boolean containsRestrictedEdgeTo(Collection<AggregatedEdge> pTargets, String pNodeId) {
      for (AggregatedEdge t : pTargets) {
        if (t.targetRepresentedBy.equals(pNodeId)) {
          if (t.condition.hasTransitionRestrictions()) {
            return true;
          }
        }
      }
      return false;
    }

    private String getMergedNodeId(String pNodeId) {
      Preconditions.checkNotNull(pNodeId);
      String nodeId = pNodeId;
      String mergedNodeId;
      while ((mergedNodeId = mergedNodes.get(nodeId)) != null) {
        nodeId = mergedNodeId;
      }
      return nodeId;
    }

    private void mergeNodes(String pOldNodeId, String pMergedNodeId) {
      Preconditions.checkNotNull(pOldNodeId);
      Preconditions.checkNotNull(pMergedNodeId);
      String mergedNodeId = getMergedNodeId(pMergedNodeId);
      mergedNodes.put(pOldNodeId, mergedNodeId);
    }

    private boolean shouldMergeNodes(String pFrom, String pTo, TransitionCondition pTransitionCondition) {
      if (nodeFlags.containsKey(pTo)) {
        return false; // Never merge if node has flags
      }
      if (violatedProperties.containsKey(pTo)) {
        return false; // Never merge if node has violated properties
      }
      if (confirmedElements.containsKey(pTo)) {
        return false; // Confirmed elements cannot be merged
      }
      if (!pTransitionCondition.hasTransitionRestrictions()) {
        return true;
      }
      Collection<AggregatedEdge> edgesToTheSourceNode = targetToSourceMap.get(pFrom);
      for (AggregatedEdge aggregationEdge : edgesToTheSourceNode) {
        if (aggregationEdge.condition.equals(pTransitionCondition)) {
          return true;
        }
      }
      return false;
    }

    private void appendNewPathNode(GraphMlBuilder pDoc, String pNodeId) throws IOException {
      Element result = pDoc.createNodeElement(pNodeId, NodeType.ONPATH);
      for (NodeFlag f : nodeFlags.get(pNodeId)) {
        pDoc.addDataElementChild(result, f.key, "true");
      }
      for (String violation : violatedProperties.get(pNodeId)) {
        pDoc.addDataElementChild(result, KeyDef.VIOLATEDPROPERTY, violation);
      }

      // Decide if writing the node should be delayed.
      // Some nodes might not get referenced by edges later.
      Collection<AggregatedEdge> existingEdgesTo = targetToSourceMap.get(pNodeId);
      // -- A node must always be written if it represents a set of aggregated nodes
      if (containsRestrictedEdgeTo(existingEdgesTo, pNodeId)) {
        confirmedElements.put(pNodeId, result);
      } else {
        delayedNodes.put(pNodeId, result);
      }
    }

    private void appendDelayedNode(final GraphMlBuilder pDoc, final String pNodeId) {
      Element e = delayedNodes.get(pNodeId);
      if (e != null) {
        delayedNodes.remove(pNodeId);
        confirmedElements.put(pNodeId, e);
      }
    }

    private void appendNewEdge(final GraphMlBuilder pDoc, String pFrom,
        final String pTo, final CFAEdge pEdge, final ARGState pFromState,
        final Map<ARGState, CFAEdgeWithAssumptions> pValueMap) throws IOException {

      String from = getMergedNodeId(pFrom);
      String to = getMergedNodeId(pTo);

      attemptSwitchToFunctionScope(pEdge);

      TransitionCondition desc = constructTransitionCondition(from, to, pEdge, pFromState, pValueMap);

      if (shouldMergeNodes(from, to, desc)) {
        mergeNodes(to, from);
        return;
      }

      boolean confirmed = false;

      AggregatedEdge t = new AggregatedEdge(to, desc);

      if (desc.hasTransitionRestrictions()) {
        appendDelayedNode(pDoc, from);
        appendDelayedNode(pDoc, to);

        Element result = pDoc.createEdgeElement(from, to);
        for (KeyDef k : desc.keyValues.keySet())  {
          pDoc.addDataElementChild(result, k, desc.keyValues.get(k));
        }
        confirmedElements.put(from, result);
        confirmed = true;

        sourceToTargetMap.put(from, t);
        targetToSourceMap.put(to, t);
      }

      // If the edge is not yet confirmed, remember it for later
      if (!confirmed) {
        for (AggregatedEdge previousEdge : targetToSourceMap.get(from)) {
          mergeNodes(previousEdge.targetRepresentedBy, to);
        }
      }
    }

    private void attemptSwitchToFunctionScope(CFAEdge pEdge) {
      if (isFunctionScope) {
        return;
      }
      if (!(pEdge instanceof BlankEdge)) {
        return;
      }
      BlankEdge edge = (BlankEdge) pEdge;
      if (!edge.getDescription().equals("Function start dummy edge")) {
        return;
      }
      isFunctionScope = true;
    }

    private TransitionCondition constructTransitionCondition(
        final String pFrom,
        final String pTo,
        final CFAEdge pEdge,
        final ARGState pFromState,
        final Map<ARGState, CFAEdgeWithAssumptions> pValueMap) {

      final TransitionCondition result = new TransitionCondition();
      if (AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge)) {
        return result;
      }

      //desc.put(KeyDef.CFAPREDECESSORNODE, edge.getPredecessor().toString());
      //desc.put(KeyDef.CFASUCCESSORNODE, edge.getSuccessor().toString());

      if (exportFunctionCallsAndReturns) {
        if (pEdge.getSuccessor() instanceof FunctionEntryNode) {
          FunctionEntryNode in = (FunctionEntryNode) pEdge.getSuccessor();
          result.put(KeyDef.FUNCTIONENTRY, in.getFunctionName());

        } else if (pEdge.getSuccessor() instanceof FunctionExitNode) {
          FunctionExitNode out = (FunctionExitNode) pEdge.getSuccessor();
          result.put(KeyDef.FUNCTIONEXIT, out.getFunctionName());
        }
      }

      if (exportAssumptions) {
        if (pFromState != null) {
          DelayedAssignmentsKey key = new DelayedAssignmentsKey(pFrom, pEdge, pFromState);
          CFAEdgeWithAssumptions cfaEdgeWithAssignments = delayedAssignments.get(key);

          if (pValueMap != null && pValueMap.containsKey(pFromState)) {
            CFAEdgeWithAssumptions currentEdgeWithAssignments = pValueMap.get(pFromState);
            if (cfaEdgeWithAssignments == null) {
              cfaEdgeWithAssignments = currentEdgeWithAssignments;

            } else {
              Builder<AExpressionStatement> allAssignments = ImmutableList.builder();
              allAssignments.addAll(cfaEdgeWithAssignments.getExpStmts());
              allAssignments.addAll(currentEdgeWithAssignments.getExpStmts());
              cfaEdgeWithAssignments = new CFAEdgeWithAssumptions(pEdge, allAssignments.build(), currentEdgeWithAssignments.getComment());
            }
          }

          if (cfaEdgeWithAssignments != null) {
            boolean isFunctionScope = this.isFunctionScope;

            List<AExpressionStatement> assignments = cfaEdgeWithAssignments.getExpStmts();
            Predicate<AExpressionStatement> assignsParameterOfOtherFunction = new AssignsParameterOfOtherFunction(pEdge);
            List<AExpressionStatement> functionValidAssignments = FluentIterable.from(assignments).filter(assignsParameterOfOtherFunction).toList();

            if (functionValidAssignments.size() < assignments.size()) {
              cfaEdgeWithAssignments = new CFAEdgeWithAssumptions(pEdge, functionValidAssignments, cfaEdgeWithAssignments.getComment());
              FluentIterable<CFAEdge> nextEdges = CFAUtils.leavingEdges(pEdge.getSuccessor());

              if (nextEdges.size() == 1 && pFromState.getChildren().size() == 1) {
                String keyFrom = pTo;
                CFAEdge keyEdge = Iterables.getOnlyElement(nextEdges);
                ARGState keyState = Iterables.getOnlyElement(pFromState.getChildren());
                List<AExpressionStatement> valueAssignments = FluentIterable.from(assignments).filter(Predicates.not(assignsParameterOfOtherFunction)).toList();
                CFAEdgeWithAssumptions valueCFAEdgeWithAssignments =
                    new CFAEdgeWithAssumptions(keyEdge, valueAssignments, "");
                delayedAssignments.put(
                    new DelayedAssignmentsKey(keyFrom, keyEdge, keyState),
                    valueCFAEdgeWithAssignments);
              }
            }

            // Do not export our own temporary variables
            assignments = FluentIterable.from(cfaEdgeWithAssignments.getExpStmts()).filter(new Predicate<AExpressionStatement>() {

              @Override
              public boolean apply(AExpressionStatement statement) {
                if (statement.getExpression() instanceof CExpression) {
                  CExpression expression = (CExpression) statement.getExpression();
                  for (CIdExpression idExpression : expression.accept(new CIdExpressionCollectingVisitor())) {
                    if (idExpression.getDeclaration().getQualifiedName().toUpperCase().contains("__CPACHECKER_TMP")) {
                      return false;
                    }
                  }
                  return true;
                }
                return false;
              }

            }).toList();
            cfaEdgeWithAssignments = new CFAEdgeWithAssumptions(pEdge, assignments, cfaEdgeWithAssignments.getComment());

            String functionName = pEdge.getPredecessor().getFunctionName();

            // Determine the scope for static local variables
            for (AExpressionStatement functionValidAssignment : functionValidAssignments) {
              if (functionValidAssignment instanceof CExpressionStatement) {
                CExpression expression = (CExpression) functionValidAssignment.getExpression();
                for (CIdExpression idExpression : expression.accept(new CIdExpressionCollectingVisitor())) {
                  CSimpleDeclaration declaration = idExpression.getDeclaration();
                  if (declaration.getName().contains("static")
                      && !declaration.getOrigName().contains("static")
                      && declaration.getQualifiedName().contains("::")) {
                    isFunctionScope = true;
                    functionName = declaration.getQualifiedName().substring(0, declaration.getQualifiedName().indexOf("::"));
                  }
                }
              }
            }

            String code = cfaEdgeWithAssignments.getAsCode();

            if (!code.isEmpty()) {
              result.put(KeyDef.ASSUMPTION, code);
              if (isFunctionScope) {
                result.put(KeyDef.ASSUMPTIONSCOPE, functionName);
              }
            }
          }
        }
      }

      if (exportAssumeCaseInfo) {
        if (pEdge instanceof AssumeEdge) {
          AssumeEdge a = (AssumeEdge) pEdge;
          if (!a.getTruthAssumption()) {
            result.put(KeyDef.NEGATIVECASE, "true");
          }
        }
      }

      if (exportLineNumbers) {
        Set<FileLocation> locations = SourceLocationMapper.getFileLocationsFromCfaEdge(pEdge);
        if (locations.size() > 0) {
          FileLocation l = locations.iterator().next();
          if (!l.getFileName().equals(defaultSourcefileName)) {
            result.put(KeyDef.ORIGINFILE, l.getFileName());
          }
          result.put(KeyDef.ORIGINLINE, Integer.toString(l.getStartingLineInOrigin()));
        }
      }

      if (exportOffset) {
        Set<FileLocation> locations = SourceLocationMapper.getFileLocationsFromCfaEdge(pEdge);
        if (locations.size() > 0) {
          FileLocation l = locations.iterator().next();
          if (!l.getFileName().equals(defaultSourcefileName)) {
            result.put(KeyDef.ORIGINFILE, l.getFileName());
          }
          result.put(KeyDef.OFFSET, Integer.toString(l.getNodeOffset()));
        }
      }

      if (exportSourcecode) {
        result.put(KeyDef.SOURCECODE, pEdge.getRawStatement());
      }

      return result;
    }

    private void appendKeyDefinitions(GraphMlBuilder pDoc, GraphType pGraphType) {
      if (pGraphType == GraphType.CONDITION) {
        pDoc.appendNewKeyDef(KeyDef.INVARIANT, null);
        pDoc.appendNewKeyDef(KeyDef.NAMED, null);
      }
      pDoc.appendNewKeyDef(KeyDef.ASSUMPTION, null);
      pDoc.appendNewKeyDef(KeyDef.SOURCECODE, null);
      pDoc.appendNewKeyDef(KeyDef.SOURCECODELANGUAGE, null);
      pDoc.appendNewKeyDef(KeyDef.NEGATIVECASE, "false");
      pDoc.appendNewKeyDef(KeyDef.ORIGINLINE, null);
      pDoc.appendNewKeyDef(KeyDef.ORIGINFILE, defaultSourcefileName);
      pDoc.appendNewKeyDef(KeyDef.NODETYPE, AutomatonGraphmlCommon.defaultNodeType.text);
      for (NodeFlag f : NodeFlag.values()) {
        pDoc.appendNewKeyDef(f.key, "false");
      }

      pDoc.appendNewKeyDef(KeyDef.FUNCTIONENTRY, null);
      pDoc.appendNewKeyDef(KeyDef.FUNCTIONEXIT, null);
    }

    /**
     * Starting from the given initial ARG state, collects that state and all
     * transitive successors (as defined by the successor function) that are
     * children of their direct predecessor and are accepted by the path state
     * predicate.
     *
     * @param pInitialState the initial ARG state.
     * @param pSuccessorFunction the function defining the successors of a
     * state.
     * @param pPathStates a filter on the nodes.
     *
     * @return the parents with their children.
     */
    private Iterable<ARGState> collectPathNodes(
        final ARGState pInitialState,
        final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
        final Predicate<? super ARGState> pPathStates) {
      return FluentIterable
          .from(collectPathEdges(pInitialState, pSuccessorFunction, pPathStates))
          .transform(Pair.<ARGState>getProjectionToFirst());
    }

    /**
     * Starting from the given initial ARG state, collects that state and all
     * transitive successors (as defined by the successor function) that are
     * children of their direct predecessor. Children are only computed for
     * nodes that are accepted by the path state predicate.
     *
     * @param pInitialState the initial ARG state.
     * @param pSuccessorFunction the function defining the successors of a
     * state.
     * @param pPathStates a filter on the parent nodes.
     *
     * @return the parents with their children.
     */
    private Iterable<Pair<ARGState, Iterable<ARGState>>> collectPathEdges(
        final ARGState pInitialState,
        final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
        final Predicate<? super ARGState> pPathStates) {
      return new Iterable<Pair<ARGState, Iterable<ARGState>>>() {

        private final Set<ARGState> visited = new HashSet<>();

        private final Deque<ARGState> waitlist = new ArrayDeque<>();

        {
          waitlist.add(pInitialState);
          visited.add(pInitialState);
        }

        @Override
        public Iterator<Pair<ARGState, Iterable<ARGState>>> iterator() {
          return new Iterator<Pair<ARGState, Iterable<ARGState>>>() {

            @Override
            public boolean hasNext() {
              return !waitlist.isEmpty();
            }

            @Override
            public Pair<ARGState, Iterable<ARGState>> next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              assert !waitlist.isEmpty();
              final ARGState parent = waitlist.poll();

              Predicate<ARGState> childFilter = new Predicate<ARGState>() {

                @Override
                public boolean apply(ARGState pChild) {
                  return parent.getChildren().contains(pChild);
                }

              };

              // Get all children
              FluentIterable<ARGState> children = FluentIterable
                  .from(pSuccessorFunction.apply(parent))
                  .transform(COVERED_TO_COVERING)
                  .filter(childFilter);

              // Only the children on the path become parents themselves
              for (ARGState child : children.filter(pPathStates)) {
                if (visited.add(child)) {
                  waitlist.offer(child);
                }
              }

              return Pair.<ARGState, Iterable<ARGState>>of(parent, children);
            }

            @Override
            public void remove() {
              throw new UnsupportedOperationException("Removal not supported.");
            }

          };
        }
      };
    }

    public void writePath(Appendable pTarget,
        final ARGState pRootState,
        final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
        final Predicate<? super ARGState> pPathStates,
        final CounterexampleInfo pCounterExample)
        throws IOException {

      Map<ARGState, CFAEdgeWithAssumptions> valueMap = null;
      if (pCounterExample != null) {
        Model model = pCounterExample.getTargetPathModel();
        CFAPathWithAssumptions cfaPath = model.getCFAPathWithAssignments();
        if (cfaPath != null) {
          ARGPath targetPath = pCounterExample.getTargetPath();
          valueMap = model.getExactVariableValues(targetPath);
        }
      }

      GraphType graphType = GraphType.PROGRAMPATH;

      GraphMlBuilder doc;
      try {
        doc = new GraphMlBuilder(pTarget);
      } catch (ParserConfigurationException e) {
        throw new IOException(e);
      }

      // TODO: Full schema details
      // Version of format..
      // TODO! (we could use the version of a XML schema)

      // ...
      String entryStateNodeId = getStateIdent(pRootState);

      doc.appendDocHeader();
      appendKeyDefinitions(doc, graphType);
      doc.appendGraphHeader(graphType, "C");

      // Collect node flags in advance
      for (ARGState s : collectPathNodes(pRootState, pSuccessorFunction, pPathStates)) {
        String sourceStateNodeId = getStateIdent(s);
        EnumSet<NodeFlag> sourceNodeFlags = EnumSet.noneOf(NodeFlag.class);
        if (sourceStateNodeId.equals(entryStateNodeId)) {
          sourceNodeFlags = EnumSet.of(NodeFlag.ISENTRY);
        }
        if (s.isTarget()) {
          sourceNodeFlags.add(NodeFlag.ISVIOLATION);
        }
        sourceNodeFlags.addAll(extractNodeFlags(s));
        nodeFlags.putAll(sourceStateNodeId, sourceNodeFlags);
        violatedProperties.putAll(sourceStateNodeId, extractViolatedProperties(s));
      }
      // Write the sink node
      nodeFlags.put(SINK_NODE_ID, NodeFlag.ISSINKNODE);
      appendNewPathNode(doc, SINK_NODE_ID);
      appendDelayedNode(doc, SINK_NODE_ID);

      // Build the actual graph
      int multiEdgeCount = 0;
      for (Pair<ARGState, Iterable<ARGState>> argEdges : collectPathEdges(pRootState, pSuccessorFunction, pPathStates)) {
        ARGState s = argEdges.getFirst();

        // Location of the state
        CFANode loc = AbstractStates.extractLocation(s);

        // Write the state
        String sourceStateNodeId = getStateIdent(s);
        appendNewPathNode(doc, sourceStateNodeId);

        // Process child states
        for (ARGState child : argEdges.getSecond()) {

          String childStateId = getStateIdent(child);
          CFANode childLoc = AbstractStates.extractLocation(child);
          CFAEdge edgeToNextState = loc.getEdgeTo(childLoc);
          String prevStateId = sourceStateNodeId;

          if (edgeToNextState instanceof MultiEdge) {
            // The successor state might have several incoming MultiEdges.
            // In this case the state names like ARG<successor>_0 would occur
            // several times.
            // So we add this counter to the state names to make them unique.
            multiEdgeCount++;

            // Write out a long linear chain of pseudo-states (one state encodes multiple edges)
            // because the AutomatonCPA also iterates through the MultiEdge.
            List<CFAEdge> edges = ((MultiEdge)edgeToNextState).getEdges();

            // inner part (without last edge)
            for (int i = 0; i < edges.size()-1; i++) {
              CFAEdge innerEdge = edges.get(i);
              String pseudoStateId = getPseudoStateIdent(child, i, multiEdgeCount);

              assert (!(innerEdge instanceof AssumeEdge));

              appendNewPathNode(doc, pseudoStateId);
              appendNewEdge(doc, prevStateId, pseudoStateId, innerEdge, i == 0 ? s : null, valueMap);
              prevStateId = pseudoStateId;
            }

            // last edge connecting it with the real successor
            edgeToNextState = edges.get(edges.size()-1);
          }

          // Only proceed with this state if the path states contains the child
          if (pPathStates.apply(child)) {
            // Child belongs to the path!
            appendNewEdge(doc, prevStateId, childStateId, edgeToNextState, s, valueMap);
          } else {
            // Child does not belong to the path --> add a branch to the SINK node!
            appendNewEdge(doc, prevStateId, SINK_NODE_ID, edgeToNextState, s, valueMap);
          }
        }
      }

      // Write all elements
      for (Element element : confirmedElements.values()) {
        // TODO: Instead of fixing the Is here in the XML elements,
        // the XML elements should be built correctly
        if (element.getTagName().equals(GraphMlTag.NODE.text)) {
          String id = element.getAttribute("id");
          if (id != null && mergedNodes.get(id) != null) {
            continue;
          }
        }
        if (element.getTagName().equals(GraphMlTag.EDGE.text)) {
          String source = element.getAttribute("source");
          if (source != null && mergedNodes.get(source) != null) {
            source = getMergedNodeId(source);
            element.setAttribute("source", source);
            continue;
          }
          String target = element.getAttribute("target");
          if (target != null) {
            target = getMergedNodeId(target);
            element.setAttribute("target", target);
          }
        }
        doc.appendToAppendable(element);
      }

      doc.appendFooter();
    }

    private Collection<NodeFlag> extractNodeFlags(ARGState pState) {
      if (pState.isTarget()) {
        return Collections.singleton(NodeFlag.ISVIOLATION);
      }
      return Collections.emptySet();
    }

    private Collection<String> extractViolatedProperties(ARGState pState) {
      if (pState.isTarget()) {
        String violatedPropertyDescription = pState.getViolatedPropertyDescription();
        int pos = violatedPropertyDescription.indexOf(':');
        if (pos >= 0) {
          return Collections.singleton(violatedPropertyDescription.substring(0, pos));
        }
      }
      return Collections.emptySet();
    }
  }

  private static class DelayedAssignmentsKey {

    private final String from;

    private final CFAEdge edge;

    private final ARGState state;

    public DelayedAssignmentsKey(String pFrom, CFAEdge pEdge, ARGState pState) {
      this.from = pFrom;
      this.edge = pEdge;
      this.state = pState;
    }

    @Override
    public int hashCode() {
      return Objects.hash(from, edge, state);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof DelayedAssignmentsKey) {
        DelayedAssignmentsKey other = (DelayedAssignmentsKey) pObj;
        return Objects.equals(from, other.from)
            && Objects.equals(edge, other.edge)
            && Objects.equals(state, other.state);
      }
      return false;
    }

  }

  private static class AssignsParameterOfOtherFunction implements Predicate<AExpressionStatement> {

    private final CFAEdge edge;

    private final String qualifier;

    public AssignsParameterOfOtherFunction(CFAEdge pEdge) {
      edge = pEdge;
      String currentFunctionName = pEdge.getPredecessor().getFunctionName();
      qualifier = Strings.isNullOrEmpty(currentFunctionName) ? "" : currentFunctionName + "::";
    }

    @Override
    public boolean apply(AExpressionStatement pArg0) {
      AExpression exp = pArg0.getExpression();
      if (!(exp instanceof CExpression)) {
        return false;
      }
      CExpression cExp = (CExpression) exp;
      return cExp.accept(new CExpressionVisitor<Boolean, RuntimeException>() {

        @Override
        public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
          return pIastArraySubscriptExpression.getArrayExpression().accept(this)
              && pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
        }

        @Override
        public Boolean visit(CFieldReference pIastFieldReference) {
          return pIastFieldReference.getFieldOwner().accept(this);
        }

        @Override
        public Boolean visit(CIdExpression pIastIdExpression) {
          CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
          if (declaration instanceof CParameterDeclaration && edge instanceof FunctionCallEdge) {
            return declaration.getQualifiedName().startsWith(qualifier);
          }
          return true;
        }

        @Override
        public Boolean visit(CPointerExpression pPointerExpression) {
          return pPointerExpression.getOperand().accept(this);
        }

        @Override
        public Boolean visit(CComplexCastExpression pComplexCastExpression) {
          return pComplexCastExpression.getOperand().accept(this);
        }

        @Override
        public Boolean visit(CBinaryExpression pIastBinaryExpression) {
          return pIastBinaryExpression.getOperand1().accept(this)
              && pIastBinaryExpression.getOperand2().accept(this);
        }

        @Override
        public Boolean visit(CCastExpression pIastCastExpression) {
          return pIastCastExpression.getOperand().accept(this);
        }

        @Override
        public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) {
          return true;
        }

        @Override
        public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
          return true;
        }

        @Override
        public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
          return true;
        }

        @Override
        public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) {
          return true;
        }

        @Override
        public Boolean visit(CTypeIdExpression pIastTypeIdExpression) {
          return true;
        }

        @Override
        public Boolean visit(CUnaryExpression pIastUnaryExpression) {
          return pIastUnaryExpression.getOperand().accept(this);
        }

        @Override
        public Boolean visit(CImaginaryLiteralExpression pIastLiteralExpression) {
          return pIastLiteralExpression.getValue().accept(this);
        }

        @Override
        public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
          return true;
        }
      });
    }

  }


}

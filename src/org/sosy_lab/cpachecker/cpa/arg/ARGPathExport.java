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
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping.NoOriginMappingAvailableException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.Model.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.core.Model.CFAPathWithAssignments.CFAEdgeWithAssignments;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.SourceLocationMapper;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeSet;

@Options(prefix="cpa.arg.witness")
public class ARGPathExport {

  @Option(description="Verification witness: Include function calls and function returns?")
  boolean exportFunctionCallsAndReturns = true;

  @Option(description="Verification witness: Include assumptions (C statements)?")
  boolean exportAssumptions = true;

  @Option(description="Verification witness: Include the considered case of an assume?")
  boolean exportAssumeCaseInfo = true;

  @Option(description="Verification witness: Include the token numbers of the operations on the transitions?")
  boolean exportTokenNumbers = true;

  @Option(description="Verification witness: Include the (starting) line numbers of the operations on the transitions?")
  boolean exportLineNumbers = true;

  @Option(description="Verification witness: Include the sourcecode of the operations?")
  boolean exportSourcecode = true;

  private final CSourceOriginMapping sourceOriginMapping;

  public ARGPathExport(Configuration config, CFA pCfa) throws InvalidConfigurationException {
    Preconditions.checkNotNull(config);
    config.inject(this);

    sourceOriginMapping = pCfa.getSourceOriginMapping();
  }

  private String tokensToText(Set<Integer> tokens) {
    StringBuilder result = new StringBuilder();
    RangeSet<Integer> tokenRanges = TreeRangeSet.create();
    for (Integer token: tokens) {
      tokenRanges.add(Range.closed(token, token));
    }
    for (Range<Integer> range : tokenRanges.asRanges()) {
      if (result.length() > 0) {
        result.append(",");
      }
      Integer from = range.lowerEndpoint();
      Integer to = range.upperEndpoint();
      if (to - from == 0) {
        result.append(from);
      } else {
        result.append(from);
        result.append("-");
        result.append(to);
      }
    }

    return result.toString();
  }

  private String getStateIdent(ARGState state) {
    return getStateIdent(state, "");
  }

  private String getStateIdent(ARGState state, String identPostfix) {
    return String.format("A%d%s", state.getStateId(), identPostfix);
  }

  private String getPseudoStateIdent(ARGState state, int subStateNo, int subStateCount)
  {
    return getStateIdent(state, String.format("_%d_%d", subStateNo, subStateCount));
  }

  private boolean handleAsEpsilonEdge(CFAEdge edge) {
    if (edge instanceof BlankEdge) {
      return true;
    } else if (edge instanceof CFunctionReturnEdge) {
      return true;
    } else if (edge instanceof CDeclarationEdge) {
      CDeclarationEdge declEdge = (CDeclarationEdge) edge;
      CDeclaration decl = declEdge.getDeclaration();
      if (decl instanceof CFunctionDeclaration) {
        return true;
      } else if (decl instanceof CTypeDeclaration) {
        return true;
      } else if (decl instanceof CVariableDeclaration) {
        CVariableDeclaration varDecl = (CVariableDeclaration) decl;
        if (varDecl.getInitializer() == null) {
          return true;
        }
      }
    }

    return false;
  }

  private static class TransitionCondition {
    public final Map<KeyDef, String> keyValues = Maps.newHashMap();

    public void put(final KeyDef key, final String value) {
      keyValues.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof TransitionCondition)) {
        return false;
      }

      TransitionCondition oT = (TransitionCondition) o;

      return this.keyValues.equals(oT.keyValues);
    }

    public boolean hasTransitionRestrictions(){
      return !keyValues.isEmpty();
    }

    @Override
    public int hashCode() {
      return keyValues.hashCode();
    }
  }

  private class AggregatedEdge {
    public final String source;
    public final String targetRepresentedBy;
    public final TransitionCondition condition;
    public final Set<String> aggregatesTargets = Sets.newHashSet();

    public AggregatedEdge(final String source, final String targetRepresentedBy,
        final TransitionCondition condition) {
      this.source = source;
      this.condition = condition;
      this.targetRepresentedBy = targetRepresentedBy;
      this.aggregatesTargets.add(targetRepresentedBy);
    }
  }

  public void writePath(Appendable sb,
      final ARGState rootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      final Predicate<? super ARGState> displayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> pathEdges,
      CounterexampleInfo pCounterExample)
      throws IOException {
    String defaultFileName = getInitialFileName(rootState);
    WitnessWriter writer = new WitnessWriter(defaultFileName);
    writer.writePath(sb, rootState, successorFunction, displayedElements, pathEdges, pCounterExample);
  }

  private String getInitialFileName(ARGState s) {
    CFANode initialLoc = AbstractStates.extractLocation(s);
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
    private final Multimap<String, AggregatedEdge> sourceToTargetMap = HashMultimap.create();
    private final Multimap<String, AggregatedEdge> targetToSourceMap = HashMultimap.create();
    private final Map<String, Element> delayedNodes = Maps.newHashMap();

    private final String defaultSourcefileName;

    public WitnessWriter(@Nullable String defaultSourcefileName) {
      this.defaultSourcefileName = defaultSourcefileName;
    }

    @SuppressWarnings("unused")
    private boolean isNodeRepresentingOneOf(Collection<AggregatedEdge> targets, String nodeId) {
      for (AggregatedEdge t : targets) {
        if (t.targetRepresentedBy.equals(nodeId)) {
          return true;
        }
      }
      return false;
    }

    private boolean containsRestrictedEdgeTo(Collection<AggregatedEdge> targets, String nodeId) {
      for (AggregatedEdge t : targets) {
        if (t.targetRepresentedBy.equals(nodeId)) {
          if (t.condition.hasTransitionRestrictions()) {
            return true;
          }
        }
      }
      return false;
    }

    private void appendNewPathNode(GraphMlBuilder doc, String nodeId, EnumSet<NodeFlag> nodeFlags) throws IOException {
      Element result = doc.createNodeElement(nodeId, NodeType.ONPATH);
      for (NodeFlag f: nodeFlags) {
        doc.addDataElementChild(result, f.key, "true");
      }

      // Decide if writing the node should be delayed.
      // Some nodes might not get referenced by edges later.
      Collection<AggregatedEdge> existingEdgesTo = targetToSourceMap.get(nodeId);
      // -- A node must always be written if it represents a set of aggregated nodes
      if (containsRestrictedEdgeTo(existingEdgesTo, nodeId)) {
        doc.appendToAppendable(result);
      } else {
        delayedNodes.put(nodeId, result);
      }
    }

    private void appendDelayedNode(final GraphMlBuilder doc, final String nodeId) {
      Element e = delayedNodes.get(nodeId);
      if (e != null) {
        delayedNodes.remove(nodeId);
        doc.appendToAppendable(e);
      }
    }

    private void appendNewEdge(final GraphMlBuilder doc, String from,
        final String to, final CFAEdge edge, final ARGState fromState,
        final Map<ARGState, CFAEdgeWithAssignments> valueMap) throws IOException {

      TransitionCondition desc;
      try {
        desc = constructTransitionCondition(from, to, edge, fromState, valueMap);
      } catch (NoOriginMappingAvailableException e) {
        throw new RuntimeException(e);
      }

      if (aggregateToPrevEdge(from, to, desc)) {
        return;
      }

      // Switch the source node (from) to the aggregating one
      // -- only if this is unambiguous
      Collection<AggregatedEdge> edgesToTheSourceNode = targetToSourceMap.get(from);
      if (edgesToTheSourceNode.size() == 1) {
        AggregatedEdge aggregationEdge = edgesToTheSourceNode.iterator().next();
        from = aggregationEdge.targetRepresentedBy;

        // If there is a edge to "from" with an empty transition condition (epsilon)
        //  then switch "from" to the "from" of the epsilon edge
        if (!aggregationEdge.condition.hasTransitionRestrictions()) {
          from = aggregationEdge.source;
        }
        // -- delay writing epsilon edges!
      }

      if (desc.hasTransitionRestrictions()) {
        appendDelayedNode(doc, from);
        appendDelayedNode(doc, to);

        Element result = doc.createEdgeElement(from, to);
        for (KeyDef k : desc.keyValues.keySet())  {
          doc.addDataElementChild(result, k, desc.keyValues.get(k));
        }
        doc.appendToAppendable(result);
      }

      AggregatedEdge t = new AggregatedEdge(from, to, desc);
      sourceToTargetMap.put(from, t);
      targetToSourceMap.put(to, t);
    }

    private boolean aggregateToPrevEdge(String from, final String to, TransitionCondition desc) {
      // What edges to "from" exist?
      //    edgesTo(from) = [e | e = (u,c,v) in E, v = from]

      Collection<AggregatedEdge> edgesToTheSourceNode = targetToSourceMap.get(from);
      if (edgesToTheSourceNode.size() == 1) {
        AggregatedEdge aggregationEdge = edgesToTheSourceNode.iterator().next();

        // Does the transition descriptor of TT match (equals) the descriptor of this transition?
        //  edgesTo(from)[1].c == this.c
        if (aggregationEdge.condition.equals(desc)) {
          // If everything can be answered with "yes":
          //  Instead of adding a new transition, modify the points-to information of TT

          aggregationEdge.aggregatesTargets.add(to);
          targetToSourceMap.put(to, aggregationEdge);

          return true;
        }
      }

      return false;
    }

    private TransitionCondition constructTransitionCondition(String from, String to, CFAEdge edge, ARGState fromState,
        Map<ARGState, CFAEdgeWithAssignments> valueMap) throws NoOriginMappingAvailableException {
      TransitionCondition desc = new TransitionCondition();

      if (exportFunctionCallsAndReturns) {
        if (edge.getSuccessor() instanceof FunctionEntryNode) {
          FunctionEntryNode in = (FunctionEntryNode) edge.getSuccessor();
          desc.put(KeyDef.FUNCTIONENTRY, in.getFunctionName());
        } else if (edge.getSuccessor() instanceof FunctionExitNode) {
          FunctionExitNode out = (FunctionExitNode) edge.getSuccessor();
          desc.put(KeyDef.FUNCTIONEXIT, out.getFunctionName());
        }
      }

      if (exportAssumptions) {
        if (fromState != null && valueMap != null && valueMap.containsKey(fromState)) {
          CFAEdgeWithAssignments cfaEdgeWithAssignments = valueMap.get(fromState);
          String code = cfaEdgeWithAssignments.getAsCode();
          if (code != null) {
            desc.put(KeyDef.ASSUMPTION, code);
          }
        }
      }

      if (!handleAsEpsilonEdge(edge)) {
        if (exportAssumeCaseInfo) {
          if (edge instanceof AssumeEdge) {
            AssumeEdge a = (AssumeEdge) edge;
            if (!a.getTruthAssumption()) {
              desc.put(KeyDef.TOKENSNEGATED, "true");
            }
          }
        }

        if (exportTokenNumbers) {
          if (sourceOriginMapping.getHasOneInputLinePerToken()) {
            Set<Integer> absoluteTokens = SourceLocationMapper.getAbsoluteTokensFromCFAEdge(edge, false);
            desc.put(KeyDef.TOKENS, tokensToText(absoluteTokens));

            Pair<String, Set<Integer>> relativeTokens = sourceOriginMapping.getRelativeTokensFromAbsolute(absoluteTokens);
            desc.put(KeyDef.ORIGINTOKENS, tokensToText(relativeTokens.getSecond()));
          }
        }

        if (exportLineNumbers) {
          Set<FileLocation> locations = SourceLocationMapper.getFileLocationsFromCfaEdge(edge);
          if (locations.size() > 0) {
            FileLocation l = locations.iterator().next();
            if (!l.getFileName().equals(defaultSourcefileName)) {
              desc.put(KeyDef.ORIGINFILE, l.getFileName());
            }
            desc.put(KeyDef.ORIGINLINE, Integer.toString(l.getStartingLineInOrigin()));
          }
        }

        if (exportSourcecode) {
          desc.put(KeyDef.SOURCECODE, edge.getRawStatement());
        }
      }

      return desc;
    }

    private void appendKeyDefinitions(GraphMlBuilder doc, GraphType graphType) {
      if (graphType == GraphType.CONDITION) {
        doc.appendNewKeyDef(KeyDef.INVARIANT, null);
        doc.appendNewKeyDef(KeyDef.NAMED, null);
      }
      doc.appendNewKeyDef(KeyDef.ASSUMPTION, null);
      doc.appendNewKeyDef(KeyDef.SOURCECODE, null);
      doc.appendNewKeyDef(KeyDef.SOURCECODELANGUAGE, null);
      doc.appendNewKeyDef(KeyDef.TOKENS, null);
      doc.appendNewKeyDef(KeyDef.ORIGINTOKENS, null);
      doc.appendNewKeyDef(KeyDef.TOKENSNEGATED, "false");
      doc.appendNewKeyDef(KeyDef.ORIGINLINE, null);
      doc.appendNewKeyDef(KeyDef.ORIGINFILE, defaultSourcefileName);
      doc.appendNewKeyDef(KeyDef.NODETYPE, AutomatonGraphmlCommon.defaultNodeType.text);
      for (NodeFlag f : NodeFlag.values()) {
        doc.appendNewKeyDef(f.key, "false");
      }

      doc.appendNewKeyDef(KeyDef.FUNCTIONENTRY, null);
      doc.appendNewKeyDef(KeyDef.FUNCTIONEXIT, null);
    }

    public void writePath(Appendable sb,
        final ARGState rootState,
        final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
        final Predicate<? super ARGState> displayedElements,
        final Predicate<? super Pair<ARGState, ARGState>> pathEdges,
        CounterexampleInfo pCounterExample)
        throws IOException {

      Map<ARGState, CFAEdgeWithAssignments> valueMap = null;
      if (pCounterExample != null) {
        Model model = pCounterExample.getTargetPathModel();
        if (model != null) {
          CFAPathWithAssignments cfaPath = model.getAssignedTermsPerEdge();
          if (cfaPath != null) {
            ARGPath targetPath = pCounterExample.getTargetPath();
            valueMap = model.getexactVariableValues(targetPath);
          }
        }
      }

      Set<ARGState> processed = new HashSet<>();
      Deque<ARGState> worklist = new ArrayDeque<>();
      worklist.add(rootState);

      GraphType graphType = GraphType.PROGRAMPATH;

      GraphMlBuilder doc;
      try {
        doc = new GraphMlBuilder(sb);
      } catch (ParserConfigurationException e) {
        throw new IOException(e);
      }

      // TODO: Full schema details
      // Version of format..
      // TODO! (we could use the version of a XML schema)

      // ...
      String entryStateNodeId = getStateIdent(rootState);
      boolean sinkNodeWritten = false;
      int multiEdgeCount = 0; // see below

      doc.appendDocHeader();
      appendKeyDefinitions(doc, graphType);
      doc.appendGraphHeader(graphType, "C");

      while (!worklist.isEmpty()) {
        ARGState s = worklist.removeLast();

        if (!displayedElements.apply(s)) {
          continue;
        }
        if (!processed.add(s)) {
          continue;
        }

        // Location of the state
        CFANode loc = AbstractStates.extractLocation(s);

        // Write the state
        String sourceStateNodeId = getStateIdent(s);
        EnumSet<NodeFlag> sourceNodeFlags = EnumSet.noneOf(NodeFlag.class);
        if (sourceStateNodeId.equals(entryStateNodeId)) {
          sourceNodeFlags = EnumSet.of(NodeFlag.ISENTRY);
        }
        if (s.isTarget()) {
          sourceNodeFlags.add(NodeFlag.ISVIOLATION);
        }
        appendNewPathNode(doc, sourceStateNodeId, sourceNodeFlags);

        // Process child states
        for (ARGState child : successorFunction.apply(s)) {
          // The child might be covered by another state
          // --> switch to the covering state
          if (child.isCovered()) {
            child = child.getCoveringState();
            assert !child.isCovered();
          }

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

              assert(!(innerEdge instanceof AssumeEdge));

              appendNewPathNode(doc, pseudoStateId, EnumSet.noneOf(NodeFlag.class));
              appendNewEdge(doc, prevStateId, pseudoStateId, innerEdge, null, valueMap);
              prevStateId = pseudoStateId;
            }

            // last edge connecting it with the real successor
            edgeToNextState = edges.get(edges.size()-1);
          }

          // Only proceed with this state if the path states contains the child
          boolean isEdgeOnPath = pathEdges.apply(Pair.of(s, child));
          if (s.getChildren().contains(child)) {
            if (isEdgeOnPath) {
              // Child belongs to the path!
              appendNewEdge(doc, prevStateId, childStateId, edgeToNextState, s, valueMap);
            } else {
              // Child does not belong to the path --> add a branch to the SINK node!
              if (!sinkNodeWritten) {
                sinkNodeWritten = true;
                EnumSet<NodeFlag> nodeFlags = EnumSet.of(NodeFlag.ISSINKNODE);
                appendNewPathNode(doc, SINK_NODE_ID, nodeFlags);
              }
              appendNewEdge(doc, prevStateId, SINK_NODE_ID, edgeToNextState, s, valueMap);
            }
          }

          worklist.add(child);
        }
      }

      doc.appendFooter();
    }
  }

}

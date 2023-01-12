// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.Traverser;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

/**
 * A simplifier for CFAs that does the following:
 *
 * <ol>
 *   <li>Mark CFA edges that can be removed using the specified predicate. Edges for which the
 *       specified predicate evaluates to {@code true} are marked.
 *   <li>Try to simplify the CFA as much as possible by removing as many marked edges as possible.
 *       There is no guarantee that all marked edges are removed. Nodes that don't have any
 *       connecting edges are also removed. The simplifier may add new blank edges for skipping
 *       removed sections of a CFA.
 * </ol>
 */
public final class MarkingCfaSimplifier implements CfaPostProcessor {

  private final Predicate<CFAEdge> removableEdgePredicate;

  private MarkingCfaSimplifier(Predicate<CFAEdge> pRemovableEdgePredicate) {
    removableEdgePredicate = pRemovableEdgePredicate;
  }

  /**
   * Returns a new {@link MarkingCfaSimplifier} that tries to remove the edges marked by the
   * specified predicate.
   *
   * @param pRemovableEdgePredicate predicate to mark removable edge (edges for which {@code true}
   *     is returned are marked)
   * @return a new {@link MarkingCfaSimplifier} that tries to remove the edges marked by the
   *     specified predicate
   * @throws NullPointerException if {@code pRemovableEdgePredicate == null}
   */
  public static MarkingCfaSimplifier forRemovableEdgeMarker(
      Predicate<CFAEdge> pRemovableEdgePredicate) {
    return new MarkingCfaSimplifier(checkNotNull(pRemovableEdgePredicate));
  }

  private static List<CFAEdge> removeSection(MutableCfaNetwork pCfaNetwork, Section pSection) {

    CFANode sectionEntryNode = pSection.getEntryNode();
    CFANode sectionExitNode = pSection.getExitNode();

    // the nodes and edges to remove
    List<CFANode> removeNodes = new ArrayList<>();
    List<CFAEdge> removeEdges = new ArrayList<>();

    Set<CFANode> waitlisted = new HashSet<>(ImmutableSet.of(sectionEntryNode));
    Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

    while (!waitlist.isEmpty()) {
      CFANode node = waitlist.remove();
      for (CFAEdge outEdge : pCfaNetwork.outEdges(node)) {
        removeEdges.add(outEdge);
        CFANode successor = pCfaNetwork.successor(outEdge);
        if (!successor.equals(sectionExitNode) && waitlisted.add(successor)) {
          removeNodes.add(successor);
          waitlist.add(successor);
        }
      }
    }

    // we don't remove a single blank edge, maybe it's a skip edge
    if (removeEdges.size() == 1 && Iterables.getOnlyElement(removeEdges) instanceof BlankEdge) {
      return ImmutableList.of();
    }

    removeEdges.forEach(pCfaNetwork::removeEdge);
    removeNodes.forEach(pCfaNetwork::removeNode);

    return Collections.unmodifiableList(removeEdges);
  }

  private static CFAEdge createNewSkipEdge(Section pSection, List<CFAEdge> pSkippedEdges) {

    List<FileLocation> skippedFileLocations =
        Collections3.transformedImmutableListCopy(pSkippedEdges, edge -> edge.getFileLocation());

    return new BlankEdge(
        "",
        FileLocation.merge(skippedFileLocations),
        pSection.getEntryNode(),
        pSection.getExitNode(),
        "skipped unnecessary edges");
  }

  private static boolean isRemovableChainNode(
      CfaNetwork pCfaNetwork, Set<CFAEdge> pRemovableEdges, CFANode pNode) {

    Set<CFAEdge> inEdges = pCfaNetwork.inEdges(pNode);
    if (inEdges.size() != 1) {
      return false;
    }

    CFAEdge singleInEdge = Iterables.getOnlyElement(inEdges);
    if (!pRemovableEdges.contains(singleInEdge)) {
      return false;
    }

    Set<CFAEdge> outEdges = pCfaNetwork.outEdges(pNode);
    if (outEdges.size() != 1) {
      return false;
    }

    CFAEdge singleOutEdge = Iterables.getOnlyElement(outEdges);
    if (!pRemovableEdges.contains(singleOutEdge)) {
      return false;
    }

    return true;
  }

  private static boolean removeMarkedChains(
      MutableCfaNetwork pCfaNetwork, Set<CFAEdge> pRemovableEdges) {

    boolean changed = false;

    // contains edges that were removed as part of a chain
    Set<CFAEdge> removeChainEdges = new HashSet<>();
    Iterator<CFAEdge> removableEdgeIterator = pRemovableEdges.iterator();
    while (removableEdgeIterator.hasNext()) {
      CFAEdge removableEdge = removableEdgeIterator.next();

      if (removeChainEdges.contains(removableEdge)) {
        removableEdgeIterator.remove();
        continue;
      }

      CFANode sectionEntryNode = pCfaNetwork.predecessor(removableEdge);
      while (isRemovableChainNode(pCfaNetwork, pRemovableEdges, sectionEntryNode)) {
        CFAEdge inEdge = Iterables.getOnlyElement(pCfaNetwork.inEdges(sectionEntryNode));
        removeChainEdges.add(inEdge);
        sectionEntryNode = pCfaNetwork.predecessor(inEdge);
      }

      CFANode sectionExitNode = pCfaNetwork.successor(removableEdge);
      while (isRemovableChainNode(pCfaNetwork, pRemovableEdges, sectionExitNode)) {
        CFAEdge outEdge = Iterables.getOnlyElement(pCfaNetwork.outEdges(sectionExitNode));
        removeChainEdges.add(outEdge);
        sectionExitNode = pCfaNetwork.successor(outEdge);
      }

      Section chainSection = new Section(sectionEntryNode, sectionExitNode);
      List<CFAEdge> removedEdges = removeSection(pCfaNetwork, chainSection);
      if (!removedEdges.isEmpty()) {
        CFAEdge skipEdge = createNewSkipEdge(chainSection, removedEdges);
        pCfaNetwork.addEdge(skipEdge);
        pRemovableEdges.add(skipEdge);
        changed = true;
      }
    }

    return changed;
  }

  private static boolean canRemoveSection(
      CfaNetwork pCfaNetwork, Set<CFAEdge> pRemovableEdges, Section pSection) {

    CFANode sectionEntryNode = pSection.getEntryNode();
    CFANode sectionExitNode = pSection.getExitNode();

    Set<CFANode> nodes = pCfaNetwork.nodes();
    if (!nodes.contains(sectionEntryNode) || !nodes.contains(sectionExitNode)) {
      return false;
    }

    Set<CFANode> waitlisted = new HashSet<>(ImmutableSet.of(sectionEntryNode));
    Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

    while (!waitlist.isEmpty()) {
      CFANode node = waitlist.remove();
      for (CFAEdge outEdge : pCfaNetwork.outEdges(node)) {
        // we cannot remove a section if it contains an non-removable edge
        if (!pRemovableEdges.contains(outEdge)) {
          return false;
        }
        CFANode successor = pCfaNetwork.successor(outEdge);
        if (waitlisted.add(successor) && !successor.equals(sectionExitNode)) {
          waitlist.add(successor);
        }
      }
    }

    return true;
  }

  private static Iterable<Section> sectionCandidates(
      FunctionEntryNode pEntryNode, FunctionExitNode pExitNode) {

    CfaNetwork function = CfaNetwork.forFunction(pEntryNode);
    ImmutableGraph<CFANode> domTree = DomTree.forGraph(function, pEntryNode).asGraph();
    DomTree<CFANode> postDomTree = DomTree.forGraph(Graphs.transpose(function), pExitNode);

    return () ->
        new AbstractIterator<>() {

          private final Iterator<CFANode> nodeIterator =
              Traverser.forTree(domTree).depthFirstPostOrder(pEntryNode).iterator();

          @Override
          protected @Nullable Section computeNext() {

            while (nodeIterator.hasNext()) {
              CFANode sectionEntryNode = nodeIterator.next();
              if (domTree.outDegree(sectionEntryNode) > 1) {
                @Nullable CFANode sectionExitNode =
                    postDomTree.getParent(sectionEntryNode).orElse(null);
                if (sectionExitNode != null) {
                  return new Section(sectionEntryNode, sectionExitNode);
                }
              }
            }

            return endOfData();
          }
        };
  }

  @Override
  public MutableCFA execute(
      MutableCFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier) {

    // mark removable edges
    MutableCfaNetwork cfaNetwork = MutableCfaNetwork.wrap(pCfa);
    Set<CFAEdge> removableEdges = new LinkedHashSet<>();
    for (CFAEdge edge : cfaNetwork.edges()) {
      if (removableEdgePredicate.test(edge)) {
        removableEdges.add(edge);
      }
    }

    // TODO: improve simplifier when we cannot easily create the post-dominator tree
    // Currently, we only generate section candidates for functions that have a function exit node
    // that post-dominates the function entry node. We should improve the simplifier for cases
    // where such a function exit node doesn't exist.

    // try to remove as many marked edges as possible until a fixpoint is reached
    boolean changed;
    do {
      changed = removeMarkedChains(cfaNetwork, removableEdges);
      for (FunctionEntryNode entryNode : ImmutableList.copyOf(cfaNetwork.entryNodes())) {
        @Nullable FunctionExitNode exitNode = cfaNetwork.functionExitNode(entryNode).orElse(null);
        // TODO: simplify this check when there are no more unreachable function exit nodes
        if (exitNode != null && !cfaNetwork.inEdges(exitNode).isEmpty()) {
          for (Section section : sectionCandidates(entryNode, exitNode)) {
            if (canRemoveSection(cfaNetwork, removableEdges, section)) {
              List<CFAEdge> removedEdges = removeSection(cfaNetwork, section);
              if (!removedEdges.isEmpty()) {
                CFAEdge skipEdge = createNewSkipEdge(section, removedEdges);
                cfaNetwork.addEdge(skipEdge);
                removableEdges.removeAll(removedEdges);
                removableEdges.add(skipEdge);
                changed = true;
              }
            }
          }
        }
      }
    } while (changed);

    return pCfa;
  }

  /**
   * A {@link Section} is a part of a CFA that contains all nodes and edges between a section-entry
   * and section-exit node (excluding section-entry and section-exit nodes).
   */
  private static final class Section {

    private final CFANode entryNode;
    private final CFANode exitNode;

    private Section(CFANode pEntryNode, CFANode pExitNode) {
      entryNode = checkNotNull(pEntryNode);
      exitNode = checkNotNull(pExitNode);
    }

    private CFANode getEntryNode() {
      return entryNode;
    }

    private CFANode getExitNode() {
      return exitNode;
    }

    @Override
    public int hashCode() {
      return Objects.hash(entryNode, exitNode);
    }

    @Override
    public boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (!(pObject instanceof Section)) {
        return false;
      }

      Section other = (Section) pObject;
      return Objects.equals(entryNode, other.entryNode) && Objects.equals(exitNode, other.exitNode);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("entry", entryNode)
          .add("exit", exitNode)
          .toString();
    }
  }
}

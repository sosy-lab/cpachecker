package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * Performs path focusing on the selected policy.
 */
public class PathFocusingManager {
  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("unused")
  public PathFocusingManager(ShutdownNotifier pShutdownNotifier,
      PolicyIterationStatistics pStatistics) {
    shutdownNotifier = pShutdownNotifier;
  }

  /**
   * @param policy Policy.
   * @param focusedNode Loop head we are performing value determination on.
   * Can not be thrown out.
   *
   * @return Focused policy.
   */
  Table<CFANode, LinearExpression, ? extends CFAEdge> pathFocusing(
      Table<CFANode, LinearExpression, ? extends CFAEdge> policy,
      CFANode focusedNode) throws InterruptedException {

    return fixpointFocusing(convert(policy), focusedNode);
  }

  /**
   * Change every edge to multi-edge.
   */
  private Table<CFANode, LinearExpression, MultiEdge> convert(
      Table<CFANode, LinearExpression, ? extends CFAEdge> t
  ) {
    Table<CFANode, LinearExpression, MultiEdge> out = HashBasedTable.create();
    for (Table.Cell<CFANode, LinearExpression, ? extends CFAEdge> cell : t.cellSet()) {
      CFAEdge edge = cell.getValue();
      out.put(
          cell.getRowKey(),
          cell.getColumnKey(),
          new MultiEdge(edge.getPredecessor(), edge.getSuccessor(), ImmutableList
              .of(edge)));
    }
    return out;
  }

  private Table<CFANode, LinearExpression, ? extends CFAEdge> fixpointFocusing(
      Table<CFANode, LinearExpression, MultiEdge> policy,
      final CFANode focusedOn
  ) throws InterruptedException {
    boolean changed = true; // For the initial iteration.
    while (changed) {
      shutdownNotifier.shutdownIfNecessary();


      changed = false;
      Multimap<CFANode, CFANode> incoming = HashMultimap.create();
      Multimap<CFANode, CFANode> outgoing = HashMultimap.create();

      // Step 1: Fill in [incoming] and [outgoing] maps in O(N).
      for (Table.Cell<CFANode, LinearExpression, MultiEdge> cell : policy.cellSet()) {
        CFANode to = cell.getRowKey();
        CFANode from = cell.getValue().getPredecessor();

        outgoing.put(from, to);
        incoming.put(to, from);
      }

      for (Map.Entry<CFANode, Collection<CFANode>> e :  incoming.asMap().entrySet()) {
        final CFANode mid = e.getKey();

        // We don't try to eliminate the node we are focusing on.
        if (mid == focusedOn) continue;

        Collection<CFANode> incomingNodes = e.getValue();
        Collection<CFANode> outgoingNodes = outgoing.get(mid);
        assert (incomingNodes.size() != 0 && outgoingNodes.size() != 0);

        // A mid-node has only one incoming edge and only one
        // outgoing edge, and CAN be eliminated if all edges between <to>
        // and <from> nodes are the same.
        // We only need to update the policy on the to-node after the elimination.
        if (incomingNodes.size() == 1 && outgoingNodes.size() == 1) {
          CFANode from = incomingNodes.iterator().next();
          CFANode to = outgoingNodes.iterator().next();

          Map<LinearExpression, MultiEdge> midRow, toRow;
          midRow = policy.row(mid);
          toRow = policy.row(to);

          // Check that all edges to the mid- row are equal.
          final MultiEdge fromToMid = midRow.values().iterator().next();

          boolean allMidEqual = Iterables.all(
              midRow.values(),
              new Predicate<MultiEdge>() {
                @Override
                public boolean apply(MultiEdge input) {
                  return input.equals(fromToMid);
                }
              });

          // Can't handle disjunctions for now.
          if (!allMidEqual) continue;

          final MultiEdge midToTo = toRow.values().iterator().next();
          boolean allEqual = Iterables.all(toRow.values(), new Predicate<MultiEdge>() {
            @Override
            public boolean apply(MultiEdge input) {
              return input.equals(midToTo);
            }
          });

          // We can only change things if all edges are equal.
          if (!allEqual) continue;

          final MultiEdge fromToTo = new MultiEdge(
              from, to, ImmutableList.<CFAEdge>builder()
              .addAll(fromToMid.getEdges()).addAll(midToTo.getEdges()).build());

          // Remove the med row.
          policy.rowMap().remove(mid);

          // Update to-map.
          for (LinearExpression template : policy.row(to).keySet()) {
            policy.put(to, template, fromToTo);
          }

          changed = true;
          break;
        }
      }
    }
    return policy;
  }

  /**
   * @return the subset of the policy related (over-approximation) to the given
   * node and the set of updates.
   */
  Table<CFANode, LinearExpression, CFAEdge> findRelated(
      Table<CFANode, LinearExpression, ? extends CFAEdge> policy,
      final CFANode valueDeterminationNode,
      Map<LinearExpression, PolicyTemplateBound> updated) throws InterruptedException {
    Table<CFANode, LinearExpression, CFAEdge> out = HashBasedTable.create();
    Set<CFANode> visited = Sets.newHashSet();
    LinkedHashSet<CFANode> queue = new LinkedHashSet<>();
    queue.add(valueDeterminationNode);

    while (!queue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();

      Iterator<CFANode> it = queue.iterator();
      CFANode node = it.next();
      it.remove();

      visited.add(node);

      Map<LinearExpression, ? extends CFAEdge> row = policy.row(node);
      for (Map.Entry<LinearExpression, ? extends CFAEdge> entry : row.entrySet()) {
        LinearExpression template = entry.getKey();

        CFAEdge edge;

        // For the value determination node only track the updated edges.
        if (node == valueDeterminationNode) {
          PolicyTemplateBound bound = updated.get(template);
          if (bound == null) continue;
          edge = bound.edge;
        } else {
          edge = entry.getValue();
        }

        // Put things related to the node.
        out.put(node, template, edge);

        CFANode toVisit = edge.getPredecessor();
        if (!visited.contains(toVisit)) {
          queue.add(toVisit);
        }
      }
    }
    return out;
  }
}

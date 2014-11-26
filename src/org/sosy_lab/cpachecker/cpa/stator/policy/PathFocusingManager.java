package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.Collection;
import java.util.Map;

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
  Table<CFANode, LinearExpression, CFAEdge> pathFocusing(
      Table<CFANode, LinearExpression, CFAEdge> policy,
      CFANode focusedNode) throws InterruptedException {

    return fixpointFocusing(convert(policy), focusedNode);
  }

  /**
   * Change every edge to multi-edge.
   */
  private Table<CFANode, LinearExpression, CFAEdge> convert(
      Table<CFANode, LinearExpression, CFAEdge> t
  ) {
    Table<CFANode, LinearExpression, CFAEdge> out = HashBasedTable.create();
    for (Table.Cell<CFANode, LinearExpression, CFAEdge> cell : t.cellSet()) {
      CFAEdge edge = cell.getValue();
      out.put(
          cell.getRowKey(),
          cell.getColumnKey(),
          new MultiEdge(edge.getPredecessor(), edge.getSuccessor(), ImmutableList
              .of(edge)));
    }
    return out;
  }

  private Table<CFANode, LinearExpression, CFAEdge> fixpointFocusing(
      Table<CFANode, LinearExpression, CFAEdge> policy,
      final CFANode focusedOn
  ) throws InterruptedException {
    boolean changed = true; // For the initial iteration.
    while (changed) {
      shutdownNotifier.shutdownIfNecessary();


      changed = false;
      Multimap<CFANode, CFANode> incoming = HashMultimap.create();
      Multimap<CFANode, CFANode> outgoing = HashMultimap.create();

      // Step 1: Fill in [incoming] and [outgoing] maps in O(N).
      for (Table.Cell<CFANode, LinearExpression, CFAEdge> cell : policy.cellSet()) {
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

          Map<LinearExpression, CFAEdge> midRow, toRow;
          midRow = policy.row(mid);
          toRow = policy.row(to);

          // Check that all edges to the mid- row are equal.
          final MultiEdge fromToMid =
              (MultiEdge)midRow.values().iterator().next();

          boolean allMidEqual = Iterables.all(
              midRow.values(),
              new Predicate<CFAEdge>() {
                @Override
                public boolean apply(CFAEdge input) {
                  return input.equals(fromToMid);
                }
              });

          // Can't handle disjunctions for now.
          if (!allMidEqual) continue;

          final MultiEdge midToTo = (MultiEdge)toRow.values().iterator().next();
          boolean allEqual = Iterables.all(toRow.values(), new Predicate<CFAEdge>() {
            @Override
            public boolean apply(CFAEdge input) {
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

}

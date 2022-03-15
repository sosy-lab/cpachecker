// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.Network;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Represents a {@link CFA} as a {@link Network}.
 *
 * <p>All methods that have nodes and edges as parameters don't check whether they actually belong
 * to the CFA represented by this network. These methods work just fine with nodes and edges that
 * don't belong to the CFA, but using them that way is highly discouraged, as this would look like a
 * bug.
 *
 * <p>All returned sets are unmodifiable views, so attempts to modify such a set will throw an
 * exception, but modifications to the underlying CFA will be reflected in the returned sets. Don't
 * try to modify the underlying CFA while iterating though such a view as correctness of the
 * iteration cannot be guaranteed anymore.
 */
public final class CfaNetwork implements Network<CFANode, CFAEdge> {

  private final CFA cfa;

  private CfaNetwork(CFA pCfa) {
    cfa = pCfa;
  }

  /**
   * Returns a {@link CfaNetwork} that represents the specified {@link CFA} as a {@link Network}.
   *
   * <p>IMPORTANT: The specified CFA must not contain any parallel edges (i.e., edges that connect
   * the same nodes in the same order) and never add them in the future (if the CFA is mutable).
   * Additionally, the set returned by {@link CFA#getAllNodes()} must not contain any duplicates and
   * never add them in the future (if the CFA is mutable). Be aware that these requirements are not
   * enforced, so violating them may lead to unexpected results.
   *
   * @param pCfa the CFA to create a network for
   * @return a {@link CfaNetwork} that represents the specified {@link CFA} as a {@link Network}
   * @throws NullPointerException if {@code pCfa == null}
   */
  public static CfaNetwork of(CFA pCfa) {
    return new CfaNetwork(checkNotNull(pCfa));
  }

  private boolean hasIncidentParallelEdges(CFANode pNode) {
    return CFAUtils.allEnteringEdges(pNode).toList().size()
            == CFAUtils.allEnteringEdges(pNode).toSet().size()
        && CFAUtils.allLeavingEdges(pNode).toList().size()
            == CFAUtils.allLeavingEdges(pNode).toSet().size();
  }

  // in-edges / predecessors

  @Override
  public int inDegree(CFANode pNode) {

    assert !hasIncidentParallelEdges(pNode);

    int inDegree = pNode.getNumEnteringEdges();

    if (pNode.getEnteringSummaryEdge() != null) {
      inDegree++;
    }

    return inDegree;
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {

    checkNotNull(pNode);

    assert !hasIncidentParallelEdges(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new Iterator<>() {

          private int index = 0;

          @Override
          public boolean hasNext() {
            return pNode.getEnteringSummaryEdge() == null
                ? index < pNode.getNumEnteringEdges()
                : index <= pNode.getNumEnteringEdges();
          }

          @Override
          public CFAEdge next() {

            CFAEdge nextEdge;
            if (index < pNode.getNumEnteringEdges()) {
              nextEdge = pNode.getEnteringEdge(index);
            } else if (index == pNode.getNumEnteringEdges()) {
              CFAEdge enteringSummaryEdge = pNode.getEnteringSummaryEdge();
              assert enteringSummaryEdge != null;
              nextEdge = enteringSummaryEdge;
            } else {
              throw new NoSuchElementException();
            }

            index++;

            return nextEdge;
          }
        };
      }

      @Override
      public int size() {
        return inDegree(pNode);
      }
    };
  }

  @Override
  public Set<CFANode> predecessors(CFANode pNode) {

    checkNotNull(pNode);

    assert !hasIncidentParallelEdges(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new Iterator<>() {

          private final Iterator<CFAEdge> inEdgeIterator = inEdges(pNode).iterator();

          @Override
          public boolean hasNext() {
            return inEdgeIterator.hasNext();
          }

          @Override
          public CFANode next() {
            return inEdgeIterator.next().getPredecessor();
          }
        };
      }

      @Override
      public int size() {
        return inDegree(pNode);
      }
    };
  }

  // out-edges / successors

  @Override
  public int outDegree(CFANode pNode) {

    assert !hasIncidentParallelEdges(pNode);

    int outDegree = pNode.getNumLeavingEdges();

    if (pNode.getLeavingSummaryEdge() != null) {
      outDegree++;
    }

    return outDegree;
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {

    checkNotNull(pNode);

    assert !hasIncidentParallelEdges(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new Iterator<>() {

          private int index = 0;

          @Override
          public boolean hasNext() {
            return pNode.getLeavingSummaryEdge() == null
                ? index < pNode.getNumLeavingEdges()
                : index <= pNode.getNumLeavingEdges();
          }

          @Override
          public CFAEdge next() {

            CFAEdge nextEdge;
            if (index < pNode.getNumLeavingEdges()) {
              nextEdge = pNode.getLeavingEdge(index);
            } else if (index == pNode.getNumLeavingEdges()) {
              CFAEdge leavingSummaryEdge = pNode.getLeavingSummaryEdge();
              assert leavingSummaryEdge != null;
              nextEdge = leavingSummaryEdge;
            } else {
              throw new NoSuchElementException();
            }

            index++;

            return nextEdge;
          }
        };
      }

      @Override
      public int size() {
        return outDegree(pNode);
      }
    };
  }

  @Override
  public Set<CFANode> successors(CFANode pNode) {

    checkNotNull(pNode);

    assert !hasIncidentParallelEdges(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new Iterator<>() {

          private final Iterator<CFAEdge> outEdgeIterator = outEdges(pNode).iterator();

          @Override
          public boolean hasNext() {
            return outEdgeIterator.hasNext();
          }

          @Override
          public CFANode next() {
            return outEdgeIterator.next().getSuccessor();
          }
        };
      }

      @Override
      public int size() {
        return outDegree(pNode);
      }
    };
  }

  // incident / adjacent

  @Override
  public int degree(CFANode pNode) {

    checkNotNull(pNode);

    return inDegree(pNode) + outDegree(pNode);
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
  }

  @Override
  public Set<CFAEdge> incidentEdges(CFANode pNode) {

    checkNotNull(pNode);

    assert !hasIncidentParallelEdges(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new PeekIterator<>() {

          private final Iterator<CFAEdge> inEdgeIterator = inEdges(pNode).iterator();
          private final Iterator<CFAEdge> outEdgeIterator = outEdges(pNode).iterator();

          @Override
          protected @Nullable CFAEdge peek() {

            if (inEdgeIterator.hasNext()) {
              return inEdgeIterator.next();
            }

            while (outEdgeIterator.hasNext()) {

              CFAEdge edge = outEdgeIterator.next();

              // don't iterate over self-loop edges twice
              if (!edge.getPredecessor().equals(edge.getSuccessor())) {
                return edge;
              }
            }

            return null;
          }
        };
      }

      @Override
      public int size() {
        return Iterables.size(this);
      }
    };
  }

  @Override
  public Set<CFANode> adjacentNodes(CFANode pNode) {

    checkNotNull(pNode);

    assert !hasIncidentParallelEdges(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new PeekIterator<>() {

          private final Iterator<CFAEdge> inEdgeIterator = inEdges(pNode).iterator();
          private final Iterator<CFAEdge> outEdgeIterator = outEdges(pNode).iterator();

          @Override
          protected @Nullable CFANode peek() {

            // predecessor iteration
            if (inEdgeIterator.hasNext()) {
              return inEdgeIterator.next().getPredecessor();
            }

            // successor iteration
            while (outEdgeIterator.hasNext()) {

              CFAEdge edge = outEdgeIterator.next();
              CFANode successor = edge.getSuccessor();

              // ignore nodes that were already iterated during predecessor iteration
              if (!successor.equals(pNode) && !successors(successor).contains(pNode)) {
                return successor;
              }
            }

            return null;
          }
        };
      }

      @Override
      public int size() {
        return Iterables.size(this);
      }
    };
  }

  @Override
  public Set<CFAEdge> adjacentEdges(CFAEdge pEdge) {

    checkNotNull(pEdge);

    assert !hasIncidentParallelEdges(pEdge.getPredecessor());

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new PeekIterator<>() {

          private final Iterator<CFAEdge> predecessorEdgeIterator =
              incidentEdges(pEdge.getPredecessor()).iterator();
          private final Iterator<CFAEdge> successorEdgeIterator =
              incidentEdges(pEdge.getSuccessor()).iterator();

          @Override
          protected @Nullable CFAEdge peek() {

            while (predecessorEdgeIterator.hasNext()) {

              CFAEdge edge = predecessorEdgeIterator.next();

              // an edge is not considered adjacent to itself
              if (!edge.equals(pEdge)) {
                return edge;
              }
            }

            while (successorEdgeIterator.hasNext()) {

              CFAEdge edge = successorEdgeIterator.next();

              // an edge is not considered adjacent to itself
              if (!edge.equals(pEdge)) {
                return edge;
              }
            }

            return null;
          }
        };
      }

      @Override
      public int size() {
        // an edge is not considered adjacent to itself, so it has to be subtracted
        return (degree(pEdge.getPredecessor()) - 1) + (degree(pEdge.getSuccessor()) - 1);
      }
    };
  }

  // edge-connecting

  @Override
  public @Nullable CFAEdge edgeConnectingOrNull(CFANode pPredecessor, CFANode pSuccessor) {

    checkNotNull(pPredecessor);
    checkNotNull(pSuccessor);

    for (CFAEdge edge : outEdges(pPredecessor)) {
      if (edge.getSuccessor().equals(pSuccessor)) {
        return edge;
      }
    }

    return null;
  }

  @Override
  public @Nullable CFAEdge edgeConnectingOrNull(EndpointPair<CFANode> pEndpoints) {

    checkArgument(pEndpoints.isOrdered(), "endpoints must be ordered");

    return edgeConnectingOrNull(pEndpoints.source(), pEndpoints.target());
  }

  @Override
  public Optional<CFAEdge> edgeConnecting(CFANode pPredecessor, CFANode pSuccessor) {
    return Optional.ofNullable(edgeConnectingOrNull(pPredecessor, pSuccessor));
  }

  @Override
  public Optional<CFAEdge> edgeConnecting(EndpointPair<CFANode> pEndpoints) {
    return Optional.ofNullable(edgeConnectingOrNull(pEndpoints));
  }

  @Override
  public Set<CFAEdge> edgesConnecting(CFANode pPredecessor, CFANode pSuccessor) {

    checkNotNull(pPredecessor);
    checkNotNull(pSuccessor);

    assert !hasIncidentParallelEdges(pPredecessor);

    return new UnmodifiableSetView<>() {

      @Override
      public int size() {
        return edgeConnectingOrNull(pPredecessor, pSuccessor) != null ? 1 : 0;
      }

      @Override
      public boolean contains(Object pObject) {
        return Objects.equals(pObject, edgeConnectingOrNull(pPredecessor, pSuccessor));
      }

      @Override
      public Iterator<CFAEdge> iterator() {
        return new Iterator<>() {

          private boolean finished = false;

          @Override
          public boolean hasNext() {
            return !finished && edgeConnectingOrNull(pPredecessor, pSuccessor) != null;
          }

          @Override
          public CFAEdge next() {
            finished = true;
            return edgeConnectingOrNull(pPredecessor, pSuccessor);
          }
        };
      }
    };
  }

  @Override
  public Set<CFAEdge> edgesConnecting(EndpointPair<CFANode> pEndpoints) {

    assert !hasIncidentParallelEdges(pEndpoints.source());

    checkArgument(pEndpoints.isOrdered(), "endpoints must be ordered");

    return edgesConnecting(pEndpoints.source(), pEndpoints.target());
  }

  @Override
  public boolean hasEdgeConnecting(CFANode pPredecessor, CFANode pSuccessor) {
    return edgeConnectingOrNull(pPredecessor, pSuccessor) != null;
  }

  @Override
  public boolean hasEdgeConnecting(EndpointPair<CFANode> pEndpoints) {

    if (!pEndpoints.isOrdered()) {
      return false; // see documentation of Network#hasEdgeConnecting(EndpointPair)
    }

    return hasEdgeConnecting(pEndpoints.source(), pEndpoints.target());
  }

  // entire network

  @Override
  public boolean isDirected() {
    return true;
  }

  @Override
  public boolean allowsSelfLoops() {
    return true;
  }

  @Override
  public boolean allowsParallelEdges() {
    return false;
  }

  @Override
  public Set<CFANode> nodes() {

    Collection<CFANode> nodes = cfa.getAllNodes();

    if (nodes instanceof Set) {
      return (Set<CFANode>) nodes;
    }

    assert ImmutableSet.copyOf(nodes).size() == ImmutableList.copyOf(nodes).size();

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return nodes.iterator();
      }

      @Override
      public int size() {
        return nodes.size();
      }
    };
  }

  @Override
  public ElementOrder<CFANode> nodeOrder() {
    return ElementOrder.unordered();
  }

  @Override
  public Set<CFAEdge> edges() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new PeekIterator<>() {

          private final Iterator<CFANode> nodeIterator = nodes().iterator();

          private Iterator<CFAEdge> outEdgeIterator = Collections.emptyIterator();

          @Override
          protected @Nullable CFAEdge peek() {

            while (!outEdgeIterator.hasNext()) {
              if (nodeIterator.hasNext()) {
                CFANode node = nodeIterator.next();
                assert !hasIncidentParallelEdges(node);
                outEdgeIterator = outEdges(node).iterator();
              } else {
                return null;
              }
            }

            return outEdgeIterator.next();
          }
        };
      }

      @Override
      public int size() {
        return Iterables.size(this);
      }
    };
  }

  @Override
  public ElementOrder<CFAEdge> edgeOrder() {
    return ElementOrder.unordered();
  }

  @Override
  public Graph<CFANode> asGraph() {
    return new Graph<>() {

      @Override
      public int inDegree(CFANode pNode) {
        return CfaNetwork.this.inDegree(pNode);
      }

      @Override
      public Set<CFANode> predecessors(CFANode pNode) {
        return CfaNetwork.this.predecessors(pNode);
      }

      @Override
      public int outDegree(CFANode pNode) {
        return CfaNetwork.this.outDegree(pNode);
      }

      @Override
      public Set<CFANode> successors(CFANode pNode) {
        return CfaNetwork.this.successors(pNode);
      }

      @Override
      public int degree(CFANode pNode) {
        return CfaNetwork.this.degree(pNode);
      }

      @Override
      public Set<EndpointPair<CFANode>> incidentEdges(CFANode pNode) {

        checkNotNull(pNode);

        assert !hasIncidentParallelEdges(pNode);

        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<EndpointPair<CFANode>> iterator() {
            return new Iterator<>() {

              private final Iterator<CFAEdge> edgeIterator =
                  CfaNetwork.this.incidentEdges(pNode).iterator();

              @Override
              public boolean hasNext() {
                return edgeIterator.hasNext();
              }

              @Override
              public EndpointPair<CFANode> next() {

                CFAEdge nextEdge = edgeIterator.next();

                return EndpointPair.ordered(nextEdge.getPredecessor(), nextEdge.getSuccessor());
              }
            };
          }

          @Override
          public int size() {
            return CfaNetwork.this.degree(pNode);
          }
        };
      }

      @Override
      public ElementOrder<CFANode> incidentEdgeOrder() {
        return ElementOrder.unordered();
      }

      @Override
      public Set<CFANode> adjacentNodes(CFANode pNode) {
        return CfaNetwork.this.adjacentNodes(pNode);
      }

      @Override
      public boolean hasEdgeConnecting(CFANode pPredecessor, CFANode pSuccessor) {
        return CfaNetwork.this.hasEdgeConnecting(pPredecessor, pSuccessor);
      }

      @Override
      public boolean hasEdgeConnecting(EndpointPair<CFANode> pEndpoints) {
        return CfaNetwork.this.hasEdgeConnecting(pEndpoints);
      }

      @Override
      public boolean isDirected() {
        return CfaNetwork.this.isDirected();
      }

      @Override
      public boolean allowsSelfLoops() {
        return CfaNetwork.this.allowsSelfLoops();
      }

      @Override
      public Set<CFANode> nodes() {
        return CfaNetwork.this.nodes();
      }

      @Override
      public ElementOrder<CFANode> nodeOrder() {
        return CfaNetwork.this.nodeOrder();
      }

      @Override
      public Set<EndpointPair<CFANode>> edges() {
        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<EndpointPair<CFANode>> iterator() {
            return new Iterator<>() {

              private final Iterator<CFAEdge> edgeIterator = CfaNetwork.this.edges().iterator();

              @Override
              public boolean hasNext() {
                return edgeIterator.hasNext();
              }

              @Override
              public EndpointPair<CFANode> next() {

                CFAEdge nextEdge = edgeIterator.next();

                return EndpointPair.ordered(nextEdge.getPredecessor(), nextEdge.getSuccessor());
              }
            };
          }

          @Override
          public int size() {
            return CfaNetwork.this.edges().size();
          }
        };
      }
    };
  }

  // abstract classes

  private abstract static class UnmodifiableSetView<E> extends AbstractCollection<E>
      implements Set<E> {

    @Override
    public final boolean add(E pElement) {
      throw new UnsupportedOperationException();
    }

    @Override
    public final boolean remove(Object pObject) {
      throw new UnsupportedOperationException();
    }

    @Override
    public final boolean addAll(Collection<? extends E> pCollection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public final boolean retainAll(Collection<?> pCollection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public final boolean removeAll(Collection<?> pCollection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public final void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private abstract static class PeekIterator<E> implements Iterator<E> {

    private @Nullable E nextElement = null;

    protected abstract @Nullable E peek();

    @Override
    public final boolean hasNext() {

      if (nextElement == null) {
        nextElement = peek();
      }

      return nextElement != null;
    }

    @Override
    public final E next() {

      if (nextElement == null) {
        nextElement = peek();
      }

      if (nextElement == null) {
        throw new NoSuchElementException();
      }

      E element = nextElement;
      nextElement = null;

      return element;
    }
  }
}

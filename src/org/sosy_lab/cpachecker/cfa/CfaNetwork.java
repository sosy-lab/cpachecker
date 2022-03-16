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

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.Network;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
public abstract class CfaNetwork implements Network<CFANode, CFAEdge> {

  protected CfaNetwork() {}

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

    checkNotNull(pCfa);

    return new CfaNetwork() {

      @Override
      public int inDegree(CFANode pNode) {

        int inDegree = pNode.getNumEnteringEdges();

        if (pNode.getEnteringSummaryEdge() != null) {
          inDegree++;
        }

        return inDegree;
      }

      @Override
      public Set<CFAEdge> inEdges(CFANode pNode) {

        checkNotNull(pNode);

        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<CFAEdge> iterator() {
            return CFAUtils.allEnteringEdges(pNode).iterator();
          }

          @Override
          public int size() {
            return inDegree(pNode);
          }
        };
      }

      @Override
      public int outDegree(CFANode pNode) {

        int outDegree = pNode.getNumLeavingEdges();

        if (pNode.getLeavingSummaryEdge() != null) {
          outDegree++;
        }

        return outDegree;
      }

      @Override
      public Set<CFAEdge> outEdges(CFANode pNode) {

        checkNotNull(pNode);

        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<CFAEdge> iterator() {
            return CFAUtils.allLeavingEdges(pNode).iterator();
          }

          @Override
          public int size() {
            return outDegree(pNode);
          }
        };
      }

      @Override
      public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
        return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
      }

      @Override
      public Set<CFANode> nodes() {
        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<CFANode> iterator() {
            return Iterators.unmodifiableIterator(pCfa.getAllNodes().iterator());
          }

          @Override
          public int size() {
            return pCfa.getAllNodes().size();
          }

          @Override
          public boolean contains(Object pObject) {
            return pCfa.getAllNodes().contains(pObject);
          }

          @Override
          public boolean containsAll(Collection<?> pCollection) {
            return pCfa.getAllNodes().containsAll(pCollection);
          }
        };
      }
    };
  }

  public static CfaNetwork of(CfaNetwork pNetwork, Predicate<CFAEdge> pFilter) {

    checkNotNull(pNetwork);
    checkNotNull(pFilter);

    return new CfaNetwork() {

      @Override
      public Set<CFAEdge> inEdges(CFANode pNode) {
        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<CFAEdge> iterator() {
            return Iterators.filter(pNetwork.inEdges(pNode).iterator(), pFilter::test);
          }

          @Override
          public int size() {
            return Iterables.size(this);
          }
        };
      }

      @Override
      public Set<CFAEdge> outEdges(CFANode pNode) {
        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<CFAEdge> iterator() {
            return Iterators.filter(pNetwork.outEdges(pNode).iterator(), pFilter::test);
          }

          @Override
          public int size() {
            return Iterables.size(this);
          }
        };
      }

      @Override
      public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
        return pNetwork.incidentNodes(pEdge);
      }

      @Override
      public Set<CFANode> nodes() {
        return pNetwork.nodes();
      }
    };
  }

  public static CfaNetwork of(CFA pCfa, Predicate<CFAEdge> pFilter) {
    return of(of(pCfa), pFilter);
  }

  public static CfaNetwork of(CFA pCfa, Set<String> pFunctions) {

    checkNotNull(pCfa);
    checkNotNull(pFunctions);

    return new CfaNetwork() {

      @Override
      public Set<CFAEdge> inEdges(CFANode pNode) {
        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<CFAEdge> iterator() {
            return CFAUtils.allEnteringEdges(pNode)
                .filter(node -> pFunctions.contains(node.getPredecessor().getFunctionName()))
                .iterator();
          }

          @Override
          public int size() {
            return Iterables.size(this);
          }
        };
      }

      @Override
      public Set<CFAEdge> outEdges(CFANode pNode) {
        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<CFAEdge> iterator() {
            return CFAUtils.allLeavingEdges(pNode)
                .filter(node -> pFunctions.contains(node.getSuccessor().getFunctionName()))
                .iterator();
          }

          @Override
          public int size() {
            return Iterables.size(this);
          }
        };
      }

      @Override
      public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
        return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
      }

      @Override
      public Set<CFANode> nodes() {
        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<CFANode> iterator() {
            return new PrepareNextIterator<>() {

              private final Set<CFANode> waitlisted =
                  pFunctions.stream()
                      .map(function -> pCfa.getAllFunctions().get(function))
                      .filter(Objects::nonNull)
                      .collect(Collectors.toCollection(HashSet::new));
              private final Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

              @Override
              protected @Nullable CFANode prepareNext() {

                while (!waitlist.isEmpty()) {

                  CFANode node = waitlist.remove();

                  for (CFANode successor : CFAUtils.allSuccessorsOf(node)) {
                    if (pFunctions.contains(successor.getFunctionName())
                        && waitlisted.add(successor)) {
                      waitlist.add(successor);
                    }
                  }

                  return node;
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
    };
  }

  // in-edges / predecessors

  @Override
  public int inDegree(CFANode pNode) {
    return Iterables.size(inEdges(pNode));
  }

  private CFANode predecessor(CFAEdge pEdge) {
    return incidentNodes(pEdge).source();
  }

  @Override
  public Set<CFANode> predecessors(CFANode pNode) {

    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Iterators.transform(inEdges(pNode).iterator(), CfaNetwork.this::predecessor);
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
    return Iterables.size(outEdges(pNode));
  }

  private CFANode successor(CFAEdge pEdge) {
    return incidentNodes(pEdge).target();
  }

  @Override
  public Set<CFANode> successors(CFANode pNode) {

    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Iterators.transform(outEdges(pNode).iterator(), CfaNetwork.this::successor);
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
  public Set<CFAEdge> incidentEdges(CFANode pNode) {

    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new PrepareNextIterator<>() {

          private final Iterator<CFAEdge> inEdges = inEdges(pNode).iterator();
          private final Iterator<CFAEdge> outEdges = outEdges(pNode).iterator();

          @Override
          protected @Nullable CFAEdge prepareNext() {

            if (inEdges.hasNext()) {
              return inEdges.next();
            }

            while (outEdges.hasNext()) {

              CFAEdge edge = outEdges.next();

              // don't iterate over self-loop edges twice
              if (!predecessor(edge).equals(successor(edge))) {
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

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new PrepareNextIterator<>() {

          private final Iterator<CFAEdge> inEdges = inEdges(pNode).iterator();
          private final Iterator<CFAEdge> outEdges = outEdges(pNode).iterator();

          @Override
          protected @Nullable CFANode prepareNext() {

            // predecessor iteration
            if (inEdges.hasNext()) {
              return predecessor(inEdges.next());
            }

            // successor iteration
            while (outEdges.hasNext()) {

              CFAEdge edge = outEdges.next();
              CFANode successor = successor(edge);

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

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {

        Iterator<CFAEdge> edges =
            Iterators.concat(
                incidentEdges(predecessor(pEdge)).iterator(),
                incidentEdges(successor(pEdge)).iterator());

        return Iterators.filter(edges, edge -> !edge.equals(pEdge));
      }

      @Override
      public int size() {
        // an edge is not considered adjacent to itself, so it has to be subtracted
        return (degree(predecessor(pEdge)) - 1) + (degree(successor(pEdge)) - 1);
      }
    };
  }

  // edge-connecting

  @Override
  public @Nullable CFAEdge edgeConnectingOrNull(CFANode pPredecessor, CFANode pSuccessor) {

    checkNotNull(pPredecessor);
    checkNotNull(pSuccessor);

    for (CFAEdge edge : outEdges(pPredecessor)) {
      if (successor(edge).equals(pSuccessor)) {
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

    return new UnmodifiableSetView<>() {

      @Override
      public int size() {
        return edgeConnectingOrNull(pPredecessor, pSuccessor) != null ? 1 : 0;
      }

      @Override
      public boolean contains(Object pObject) {

        if (pObject == null) {
          return false;
        }

        return pObject.equals(edgeConnectingOrNull(pPredecessor, pSuccessor));
      }

      @Override
      public Iterator<CFAEdge> iterator() {
        return new Iterator<>() {

          private @Nullable CFAEdge edge = edgeConnectingOrNull(pPredecessor, pSuccessor);

          @Override
          public boolean hasNext() {
            return edge != null;
          }

          @Override
          public CFAEdge next() {

            CFAEdge nextEdge = edge;
            edge = null;

            return nextEdge;
          }
        };
      }
    };
  }

  @Override
  public Set<CFAEdge> edgesConnecting(EndpointPair<CFANode> pEndpoints) {

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
  public ElementOrder<CFANode> nodeOrder() {
    return ElementOrder.unordered();
  }

  @Override
  public Set<CFAEdge> edges() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new PrepareNextIterator<>() {

          private final Iterator<CFANode> nodeIterator = nodes().iterator();

          private Iterator<CFAEdge> outEdges = Collections.emptyIterator();

          @Override
          protected @Nullable CFAEdge prepareNext() {

            while (!outEdges.hasNext()) {
              if (nodeIterator.hasNext()) {
                CFANode node = nodeIterator.next();
                outEdges = outEdges(node).iterator();
              } else {
                return null;
              }
            }

            return outEdges.next();
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

        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<EndpointPair<CFANode>> iterator() {
            return Iterators.transform(
                CfaNetwork.this.incidentEdges(pNode).iterator(), CfaNetwork.this::incidentNodes);
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
            return Iterators.transform(
                CfaNetwork.this.edges().iterator(), CfaNetwork.this::incidentNodes);
          }

          @Override
          public int size() {
            return CfaNetwork.this.edges().size();
          }
        };
      }
    };
  }

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

  private abstract static class PrepareNextIterator<E> implements Iterator<E> {

    private @Nullable E nextElement = null;

    protected abstract @Nullable E prepareNext();

    @Override
    public final boolean hasNext() {

      if (nextElement == null) {
        nextElement = prepareNext();
      }

      return nextElement != null;
    }

    @Override
    public final E next() {

      if (nextElement == null) {
        nextElement = prepareNext();
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

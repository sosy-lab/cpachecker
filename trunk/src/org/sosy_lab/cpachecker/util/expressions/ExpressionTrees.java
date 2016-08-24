/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;



public final class ExpressionTrees {

  @SuppressWarnings("unchecked")
  public static <LeafType> ExpressionTree<LeafType> getTrue() {
    return (ExpressionTree<LeafType>) TRUE;
  }

  @SuppressWarnings("unchecked")
  public static <LeafType> ExpressionTree<LeafType> getFalse() {
    return (ExpressionTree<LeafType>) FALSE;
  }

  private static final ExpressionTree<Object> TRUE =
      new AbstractExpressionTree<Object>() {

        @Override
        public <T, E extends Throwable> T accept(ExpressionTreeVisitor<Object, T, E> pVisitor)
            throws E {
          return pVisitor.visitTrue();
        }

        @Override
        public int hashCode() {
          return 1;
        }

        @Override
        public boolean equals(Object pObj) {
          return this == pObj;
        }
      };

  private static final ExpressionTree<Object> FALSE =
      new AbstractExpressionTree<Object>() {

        @Override
        public <T, E extends Throwable> T accept(ExpressionTreeVisitor<Object, T, E> pVisitor)
            throws E {
          return pVisitor.visitFalse();
        }

        @Override
        public int hashCode() {
          return 0;
        }

        @Override
        public boolean equals(Object pObj) {
          return this == pObj;
        }
      };

  public static final Predicate<ExpressionTree<?>> IS_CONSTANT =
      new Predicate<ExpressionTree<?>>() {

        @Override
        public boolean apply(ExpressionTree<?> pExpressionTree) {
          return isConstant(pExpressionTree);
        }
      };

  public static final Predicate<ExpressionTree<?>> IS_LEAF =
      new Predicate<ExpressionTree<?>>() {

        @Override
        public boolean apply(ExpressionTree<?> pExpressionTree) {
          return isLeaf(pExpressionTree);
        }
      };

  public static final Predicate<ExpressionTree<?>> IS_AND =
      new Predicate<ExpressionTree<?>>() {

        @Override
        public boolean apply(ExpressionTree<?> pExpressionTree) {
          return isAnd(pExpressionTree);
        }
      };

  public static final Predicate<ExpressionTree<?>> IS_OR =
      new Predicate<ExpressionTree<?>>() {

        @Override
        public boolean apply(ExpressionTree<?> pExpressionTree) {
          return isOr(pExpressionTree);
        }
      };

  private ExpressionTrees() {

  }

  public static <LeafType> boolean isConstant(ExpressionTree<LeafType> pExpressionTree) {
    @SuppressWarnings("unchecked")
    ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> visitor =
        (ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>)
            new DefaultExpressionTreeVisitor<Object, Boolean, RuntimeException>() {

              @Override
              protected Boolean visitDefault(ExpressionTree<Object> pExpressionTree) {
                return false;
              }

              @Override
              public Boolean visitTrue() {
                return true;
              }

              @Override
              public Boolean visitFalse() {
                return true;
              }
            };
    return pExpressionTree.accept(visitor);
  }

  public static <LeafType> boolean isLeaf(ExpressionTree<LeafType> pExpressionTree) {
    @SuppressWarnings("unchecked")
    ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> visitor =
        (ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>)
            new DefaultExpressionTreeVisitor<Object, Boolean, RuntimeException>() {

              @Override
              protected Boolean visitDefault(ExpressionTree<Object> pExpressionTree) {
                return false;
              }

              @Override
              public Boolean visit(LeafExpression<Object> pLeafExpression) {
                return true;
              }

              @Override
              public Boolean visitTrue() {
                return true;
              }

              @Override
              public Boolean visitFalse() {
                return true;
              }
            };
    return pExpressionTree.accept(visitor);
  }

  public static <LeafType> boolean isOr(ExpressionTree<LeafType> pExpressionTree) {
    @SuppressWarnings("unchecked")
    ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> visitor =
        (ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>)
            new DefaultExpressionTreeVisitor<Object, Boolean, RuntimeException>() {

              @Override
              protected Boolean visitDefault(ExpressionTree<Object> pExpressionTree) {
                return false;
              }

              @Override
              public Boolean visit(Or<Object> pOr) {
                return true;
              }
            };
    return pExpressionTree.accept(visitor);
  }

  public static <LeafType> boolean isAnd(ExpressionTree<LeafType> pExpressionTree) {
    @SuppressWarnings("unchecked")
    ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> visitor =
        (ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>)
            new DefaultExpressionTreeVisitor<Object, Boolean, RuntimeException>() {

              @Override
              protected Boolean visitDefault(ExpressionTree<Object> pExpressionTree) {
                return false;
              }

              @Override
              public Boolean visit(And<Object> pAnd) {
                return true;
              }
            };
    return pExpressionTree.accept(visitor);
  }

  public static <LeafType> boolean isInCNF(ExpressionTree<LeafType> pExpressionTree) {
    @SuppressWarnings("unchecked")
    ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> visitor =
        (ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>)
            new ExpressionTreeVisitor<Object, Boolean, RuntimeException>() {

              @Override
              public Boolean visit(And<Object> pAnd) {
                return getChildren(pAnd)
                    .allMatch(
                        new Predicate<ExpressionTree<Object>>() {

                          @Override
                          public boolean apply(ExpressionTree<Object> pClause) {
                            // A clause may be a single literal or a disjunction of literals
                            assert !isAnd(pClause)
                                : "A conjunction must not contain child conjunctions";
                            return isCNFClause(pClause);
                          }
                        });
              }

              @Override
              public Boolean visit(Or<Object> pOr) {
                return getChildren(pOr).allMatch(IS_LEAF);
              }

              @Override
              public Boolean visit(LeafExpression<Object> pLeafExpression) {
                // Check: One clause with one literal
                return true;
              }

              @Override
              public Boolean visitTrue() {
                // Check: One clause with one literal
                return true;
              }

              @Override
              public Boolean visitFalse() {
                // Check: One clause with one literal
                return true;
              }
            };
    return pExpressionTree.accept(visitor);
  }

  public static <LeafType> boolean isInDNF(ExpressionTree<LeafType> pExpressionTree) {
    @SuppressWarnings("unchecked")
    ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> visitor =
        (ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>)
            new ExpressionTreeVisitor<Object, Boolean, RuntimeException>() {

              @Override
              public Boolean visit(And<Object> pAnd) {
                // Check: One clause with more than one literal
                return getChildren(pAnd).allMatch(IS_LEAF);
              }

              @Override
              public Boolean visit(Or<Object> pOr) {
                return getChildren(pOr)
                    .allMatch(
                        new Predicate<ExpressionTree<Object>>() {

                          @Override
                          public boolean apply(ExpressionTree<Object> pClause) {
                            // A clause may be a single literal or a conjunction of literals
                            assert !isOr(pClause)
                                : "A disjunction must not contain child disjunctions";
                            return isDNFClause(pClause);
                          }
                        });
              }

              @Override
              public Boolean visit(LeafExpression<Object> pLeafExpression) {
                // Check: One clause with one literal
                return true;
              }

              @Override
              public Boolean visitTrue() {
                // Check: One clause with one literal
                return true;
              }

              @Override
              public Boolean visitFalse() {
                // Check: One clause with one literal
                return true;
              }
            };
    return pExpressionTree.accept(visitor);
  }

  public static <LeafType> ExpressionTree<LeafType> toDNF(
      ExpressionTree<LeafType> pExpressionTree) {
    if (isInDNF(pExpressionTree)) {
      return pExpressionTree;
    }
    if (isOr(pExpressionTree)) {
      return Or.of(
          getChildren(pExpressionTree)
              .transform(
                  new Function<ExpressionTree<LeafType>, ExpressionTree<LeafType>>() {

                    @Override
                    public ExpressionTree<LeafType> apply(
                        ExpressionTree<LeafType> pExpressionTree) {
                      return toDNF(pExpressionTree);
                    }
                  }));
    }
    assert isAnd(pExpressionTree);
    Iterator<ExpressionTree<LeafType>> elementIterator = getChildren(pExpressionTree).iterator();
    if (!elementIterator.hasNext()) {
      return ExpressionTrees.getTrue();
    }
    ExpressionTree<LeafType> first = elementIterator.next();
    if (!elementIterator.hasNext()) {
      return first;
    }
    Collection<ExpressionTree<LeafType>> rest = new ArrayList<>();
    Iterators.addAll(rest, elementIterator);
    ExpressionTree<LeafType> firstDNF = toDNF(first);
    ExpressionTree<LeafType> restDNF = toDNF(And.of(rest));
    final Iterable<ExpressionTree<LeafType>> combinatorsA;
    if (isLeaf(firstDNF)) {
      combinatorsA = Collections.singleton(firstDNF);
    } else {
      combinatorsA = getChildren(firstDNF);
    }
    final Iterable<ExpressionTree<LeafType>> combinatorsB;
    if (isLeaf(restDNF)) {
      combinatorsB = Collections.singleton(restDNF);
    } else {
      combinatorsB = getChildren(restDNF);
    }
    Collection<ExpressionTree<LeafType>> newClauses = new ArrayList<>();
    for (ExpressionTree<LeafType> combinatorA : combinatorsA) {
      for (ExpressionTree<LeafType> combinatorB : combinatorsB) {
        newClauses.add(
            And.of(ImmutableList.<ExpressionTree<LeafType>>of(combinatorA, combinatorB)));
      }
    }
    return Or.of(newClauses);
  }

  public static <LeafType> ExpressionTree<LeafType> toCNF(
      ExpressionTree<LeafType> pExpressionTree) {
    if (isInCNF(pExpressionTree)) {
      return pExpressionTree;
    }
    if (isAnd(pExpressionTree)) {
      return And.of(
          getChildren(pExpressionTree)
              .transform(
                  new Function<ExpressionTree<LeafType>, ExpressionTree<LeafType>>() {

                    @Override
                    public ExpressionTree<LeafType> apply(
                        ExpressionTree<LeafType> pExpressionTree) {
                      return toCNF(pExpressionTree);
                    }
                  }));
    }
    assert isOr(pExpressionTree);
    Iterator<ExpressionTree<LeafType>> elementIterator = getChildren(pExpressionTree).iterator();
    if (!elementIterator.hasNext()) {
      return ExpressionTrees.getFalse();
    }
    ExpressionTree<LeafType> first = elementIterator.next();
    if (!elementIterator.hasNext()) {
      return first;
    }
    Collection<ExpressionTree<LeafType>> rest = new ArrayList<>();
    Iterators.addAll(rest, elementIterator);
    ExpressionTree<LeafType> firstCNF = toCNF(first);
    ExpressionTree<LeafType> restCNF = toCNF(And.of(rest));
    final Iterable<ExpressionTree<LeafType>> combinatorsA;
    if (isLeaf(firstCNF)) {
      combinatorsA = Collections.singleton(firstCNF);
    } else {
      combinatorsA = getChildren(firstCNF);
    }
    final Iterable<ExpressionTree<LeafType>> combinatorsB;
    if (isLeaf(restCNF)) {
      combinatorsB = Collections.singleton(restCNF);
    } else {
      combinatorsB = getChildren(restCNF);
    }
    Collection<ExpressionTree<LeafType>> newClauses = new ArrayList<>();
    for (ExpressionTree<LeafType> combinatorA : combinatorsA) {
      for (ExpressionTree<LeafType> combinatorB : combinatorsB) {
        newClauses.add(Or.of(ImmutableList.<ExpressionTree<LeafType>>of(combinatorA, combinatorB)));
      }
    }
    return And.of(newClauses);
  }

  public static <LeafType> boolean isCNFClause(ExpressionTree<LeafType> pExpressionTree) {
    return isLeaf(pExpressionTree) || (isOr(pExpressionTree) && getChildren(pExpressionTree).allMatch(IS_LEAF));
  }

  public static <LeafType> boolean isDNFClause(ExpressionTree<LeafType> pExpressionTree) {
    return isLeaf(pExpressionTree) || (isAnd(pExpressionTree) && getChildren(pExpressionTree).allMatch(IS_LEAF));
  }

  public static <LeafType> FluentIterable<ExpressionTree<LeafType>> getChildren(
      ExpressionTree<LeafType> pExpressionTree) {
    return FluentIterable.from(
        pExpressionTree.<Iterable<ExpressionTree<LeafType>>, RuntimeException>accept(
            new DefaultExpressionTreeVisitor<
                LeafType, Iterable<ExpressionTree<LeafType>>, RuntimeException>() {

              @Override
              protected Iterable<ExpressionTree<LeafType>> visitDefault(
                  ExpressionTree<LeafType> pExpressionTree) {
                return Collections.emptySet();
              }

              @Override
              public Iterable<ExpressionTree<LeafType>> visit(And<LeafType> pAnd) {
                return pAnd;
              }

              @Override
              public Iterable<ExpressionTree<LeafType>> visit(Or<LeafType> pOr) {
                return pOr;
              }
            }));
  }

  public static <LeafType> Simplifier<LeafType> newSimplifier() {
    return newSimplifier(ExpressionTrees.<LeafType>newCachingFactory());
  }

  public static <LeafType> Simplifier<LeafType> newSimplifier(
      final ExpressionTreeFactory<LeafType> pFactory) {
    return new Simplifier<LeafType>() {

      private final Map<
              Set<ExpressionTree<LeafType>>,
              ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, RuntimeException>>
          simplificationVisitors = Maps.newHashMap();

      @Override
      public ExpressionTree<LeafType> simplify(ExpressionTree<LeafType> pExpressionTree) {
        return ExpressionTrees.simplify(
            pExpressionTree,
            Collections.<ExpressionTree<LeafType>>emptySet(),
            simplificationVisitors,
            pFactory,
            true);
      }
    };
  }

  public static <LeafType> ExpressionTree<LeafType> simplify(
      ExpressionTree<LeafType> pExpressionTree) {
    return simplify(pExpressionTree, ExpressionTrees.<LeafType>newCachingFactory());
  }

  public static <LeafType> ExpressionTree<LeafType> simplify(
      ExpressionTree<LeafType> pExpressionTree, ExpressionTreeFactory<LeafType> pFactory) {
    return simplify(
        pExpressionTree,
        Collections.<ExpressionTree<LeafType>>emptySet(),
        Maps
            .<Set<ExpressionTree<LeafType>>,
                ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, RuntimeException>>
                newHashMap(),
        pFactory,
        true);
  }

  private static <LeafType> ExpressionTree<LeafType> simplify(
      ExpressionTree<LeafType> pExpressionTree,
      final Set<ExpressionTree<LeafType>> pExternalKnowledge,
      final Map<
              Set<ExpressionTree<LeafType>>,
              ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, RuntimeException>>
          pVisitors,
      final ExpressionTreeFactory<LeafType> pFactory,
      final boolean pThorough) {
    ExpressionTree<LeafType> expressionTree = pExpressionTree;
    if (isConstant(expressionTree)) {
      return pExpressionTree;
    }
    if (pExternalKnowledge.contains(expressionTree)) {
      return getTrue();
    }
    if (expressionTree instanceof LeafExpression) {
      LeafExpression<LeafType> negated = ((LeafExpression<LeafType>) expressionTree).negate();
      if (pExternalKnowledge.contains(negated)) {
        return getFalse();
      }
    }
    ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, RuntimeException> visitor =
        pVisitors.get(pExternalKnowledge);
    if (visitor == null) {
      visitor =
          new CachingVisitor<LeafType, ExpressionTree<LeafType>, RuntimeException>() {

            @Override
            public ExpressionTree<LeafType> cacheMissAnd(And<LeafType> pAnd)
                throws RuntimeException {
              List<ExpressionTree<LeafType>> operands = Lists.newLinkedList(pAnd);
              boolean changedGlobally = false;
              boolean changed = false;
              Set<ExpressionTree<LeafType>> knowledgeBase = pExternalKnowledge;
              do {
                changed = false;
                ListIterator<ExpressionTree<LeafType>> operandIt = operands.listIterator();
                while (operandIt.hasNext()) {
                  ExpressionTree<LeafType> current = operandIt.next();
                  ExpressionTree<LeafType> simplifiedCurrent =
                      simplify(current, knowledgeBase, pVisitors, pFactory, pThorough);
                  if (!simplifiedCurrent.equals(current)) {
                    if (getFalse().equals(simplifiedCurrent)) {
                      return simplifiedCurrent;
                    }
                    changed = true;
                    operandIt.remove();
                    if (!getTrue().equals(simplifiedCurrent)) {
                      operandIt.add(simplifiedCurrent);
                    } else {
                      continue;
                    }
                    current = simplifiedCurrent;
                  }

                  for (ExpressionTree<LeafType> other : operands) {
                    if (other != current) {
                      boolean newFact = !knowledgeBase.contains(other);
                      if (newFact) {
                        if (current instanceof LeafExpression && other instanceof LeafExpression) {
                          if (current.equals(other)) {
                            simplifiedCurrent = getTrue();
                          } else if (((LeafExpression<?>) current).equals(other)) {
                            simplifiedCurrent = getFalse();
                          } else {
                            simplifiedCurrent = current;
                          }
                        } else {
                          simplifiedCurrent =
                              simplify(
                                  current,
                                  Collections.singleton(other),
                                  pVisitors,
                                  pFactory,
                                  false);
                        }
                        if (!simplifiedCurrent.equals(current)) {
                          if (getFalse().equals(simplifiedCurrent)) {
                            return simplifiedCurrent;
                          }
                          changed = true;
                          ExpressionTree<LeafType> prev = operandIt.previous();
                          assert prev == current;
                          operandIt.remove();
                          if (!getTrue().equals(simplifiedCurrent)) {
                            operandIt.add(simplifiedCurrent);
                          }
                          break;
                        }
                      }
                    }
                  }
                }
                changedGlobally |= changed;
              } while (changed);
              if (!changedGlobally) {
                return pAnd;
              }
              return pFactory.and(operands);
            }

            @Override
            public ExpressionTree<LeafType> cacheMissOr(Or<LeafType> pOr) throws RuntimeException {

              Iterator<ExpressionTree<LeafType>> opIt = pOr.iterator();
              if (opIt.hasNext()) {
                int nOperands = 1;
                Set<ExpressionTree<LeafType>> commonFacts = Sets.newHashSet(asFacts(opIt.next()));
                if (opIt.hasNext()) {
                  while (!commonFacts.isEmpty() && opIt.hasNext()) {
                    Iterable<ExpressionTree<LeafType>> facts = asFacts(opIt.next());
                    commonFacts.retainAll(
                        facts instanceof Collection
                            ? (Collection<?>) facts
                            : Lists.newArrayList(facts));
                    ++nOperands;
                  }

                  if (!commonFacts.isEmpty()) {
                    ExpressionTree<LeafType> commonFactsTree = And.of(commonFacts);
                    commonFacts.addAll(pExternalKnowledge);

                    List<ExpressionTree<LeafType>> simplifiedOperands = new ArrayList<>(nOperands);
                    List<ExpressionTree<LeafType>> operands = new ArrayList<>(nOperands);
                    for (ExpressionTree<LeafType> operand : pOr) {
                      ExpressionTree<LeafType> simplified =
                          simplify(operand, commonFacts, pVisitors, pFactory, pThorough);
                      if (!simplified.equals(getFalse())) {
                        operands.add(operand);
                      }
                      simplifiedOperands.add(simplified);
                    }
                    // If an operand was contradictory, remove it and try again
                    if (operands.size() < simplifiedOperands.size()) {
                      return simplify(
                          pFactory.or(operands),
                          pExternalKnowledge,
                          pVisitors,
                          pFactory,
                          pThorough);
                    }

                    return pFactory.and(
                        simplify(
                            commonFactsTree, pExternalKnowledge, pVisitors, pFactory, pThorough),
                        pFactory.or(simplifiedOperands));
                  }
                }
              }

              List<ExpressionTree<LeafType>> operands = Lists.newLinkedList();
              Set<ExpressionTree<LeafType>> changedOps = Collections.emptySet();
              boolean changed = false;

              // Simplify the operands
              for (ExpressionTree<LeafType> operandToAdd : pOr) {
                ExpressionTree<LeafType> simplified =
                    simplify(operandToAdd, pExternalKnowledge, pVisitors, pFactory, pThorough);
                if (getTrue().equals(simplified)) {
                  return simplified;
                }
                if (!getFalse().equals(simplified)) {
                  operands.add(simplified);
                }
                if (simplified != operandToAdd) {
                  changed = true;
                  if (!pThorough) {
                    if (changedOps.isEmpty()) {
                      changedOps = Sets.newHashSet();
                    }
                    changedOps.add(simplified);
                  }
                }
              }

              // Remove operands that imply other operands
              if (changed || pThorough) {
                Iterator<ExpressionTree<LeafType>> operandIt = operands.iterator();
                while (operandIt.hasNext()) {
                  ExpressionTree<LeafType> operand = operandIt.next();
                  boolean operandChanged = pThorough || changedOps.contains(operand);
                  Iterable<ExpressionTree<LeafType>> innerOperands =
                      operandChanged ? operands : changedOps;
                  if (operand instanceof LeafExpression) {
                    LeafExpression<LeafType> negated =
                        ((LeafExpression<LeafType>) operand).negate();
                    if (!pExternalKnowledge.contains(negated)) {
                      for (ExpressionTree<LeafType> op : innerOperands) {
                        if (op == operand) {
                          continue;
                        }
                        if ((operandChanged || changedOps.contains(op))
                            && getTrue()
                                .equals(
                                    simplify(
                                        op,
                                        Collections.<ExpressionTree<LeafType>>singleton(negated),
                                        pVisitors,
                                        pFactory,
                                        false))) {
                          return getTrue();
                        }
                      }
                    }
                  }
                  if (!pExternalKnowledge.contains(operand)) {
                    for (ExpressionTree<LeafType> op : innerOperands) {
                      if (op == operand) {
                        continue;
                      }
                      if ((operandChanged || changedOps.contains(op))
                          && getTrue()
                              .equals(
                                  simplify(
                                      op,
                                      Collections.<ExpressionTree<LeafType>>singleton(operand),
                                      pVisitors,
                                      pFactory,
                                      false))) {
                        changed = true;
                        operandIt.remove();
                        break;
                      }
                    }
                  }
                }
              }

              if (!changed) {
                return pOr;
              }

              return pFactory.or(operands);
            }

            private Iterable<ExpressionTree<LeafType>> asFacts(
                ExpressionTree<LeafType> pExpressionTree) {
              if (isAnd(pExpressionTree)) {
                return getChildren(pExpressionTree);
              }
              return Collections.singleton(pExpressionTree);
            }

            @Override
            public ExpressionTree<LeafType> cacheMissLeaf(LeafExpression<LeafType> pLeafExpression)
                throws RuntimeException {
              return pLeafExpression;
            }

            @Override
            public ExpressionTree<LeafType> cacheMissTrue() throws RuntimeException {
              return getTrue();
            }

            @Override
            public ExpressionTree<LeafType> cacheMissFalse() throws RuntimeException {
              return getFalse();
            }
          };
      pVisitors.put(pExternalKnowledge, visitor);
    }
    return expressionTree.accept(visitor);
  }

  public static <S, T> ExpressionTree<T> convert(
      ExpressionTree<S> pSource, final Function<? super S, ? extends T> pLeafConverter) {
    final Function<ExpressionTree<S>, ExpressionTree<T>> convert =
        new Function<ExpressionTree<S>, ExpressionTree<T>>() {

          @Override
          public ExpressionTree<T> apply(ExpressionTree<S> pTree) {
            return convert(pTree, pLeafConverter);
          }
        };
    ExpressionTreeVisitor<S, ExpressionTree<T>, RuntimeException> converter =
        new CachingVisitor<S, ExpressionTree<T>, RuntimeException>() {

          @Override
          public ExpressionTree<T> cacheMissAnd(And<S> pAnd) {
            return And.of(FluentIterable.from(pAnd).transform(convert));
          }

          @Override
          public ExpressionTree<T> cacheMissOr(Or<S> pOr) {
            return Or.of(FluentIterable.from(pOr).transform(convert));
          }

          @Override
          public ExpressionTree<T> cacheMissLeaf(LeafExpression<S> pLeafExpression) {
            return LeafExpression.of(
                (T) pLeafConverter.apply(pLeafExpression.getExpression()),
                pLeafExpression.assumeTruth());
          }

          @Override
          public ExpressionTree<T> cacheMissTrue() {
            return ExpressionTrees.getTrue();
          }

          @Override
          public ExpressionTree<T> cacheMissFalse() {
            return ExpressionTrees.getFalse();
          }

          @Override
          public ExpressionTree<T> visitTrue() {
            return ExpressionTrees.getTrue();
          }

          @Override
          public ExpressionTree<T> visitFalse() {
            return ExpressionTrees.getFalse();
          }
        };
    return pSource.accept(converter);
  }

  /**
   * Cast an expression tree with a source leaf type
   * to an expression tree with a target leaf type.
   *
   * This unchecked cast is safe if the expression tree is immutable,
   * which every expression tree is required to be by convention.
   *
   * @param pToCast the tree to be casted.
   *
   * @return the casted tree.
   */
  @SuppressWarnings("unchecked")
  public static <LeafTypeS extends LeafTypeT, LeafTypeT> ExpressionTree<LeafTypeT> cast(
      ExpressionTree<LeafTypeS> pToCast) {
    return (ExpressionTree<LeafTypeT>) pToCast;
  }

  public static <LeafType> ExpressionTreeFactory<LeafType> newCachingFactory() {
    return new ExpressionTreeFactory<LeafType>() {

      private final Map<Object, ExpressionTree<LeafType>> leafCache = Maps.newHashMap();

      private final Map<Object, ExpressionTree<LeafType>> andCache = Maps.newHashMap();

      private final Map<Object, ExpressionTree<LeafType>> orCache = Maps.newHashMap();

      @Override
      public ExpressionTree<LeafType> leaf(LeafType pLeafType) {
        return leaf(pLeafType, true);
      }

      @Override
      public ExpressionTree<LeafType> leaf(LeafType pLeafExpression, boolean pAssumeTruth) {
        ExpressionTree<LeafType> potentialResult = LeafExpression.of(pLeafExpression, pAssumeTruth);
        ExpressionTree<LeafType> cachedResult = leafCache.get(potentialResult);
        if (cachedResult == null) {
          leafCache.put(potentialResult, potentialResult);
          return potentialResult;
        }
        return cachedResult;
      }

      @Override
      public ExpressionTree<LeafType> and(
          ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2) {
        return and(ImmutableSet.of(pOp1, pOp2));
      }

      @Override
      public ExpressionTree<LeafType> and(Iterable<ExpressionTree<LeafType>> pOperands) {
        final Set<ExpressionTree<LeafType>> key;
        if (pOperands instanceof Set) {
          key = (Set<ExpressionTree<LeafType>>) pOperands;
        } else {
          Iterator<ExpressionTree<LeafType>> operandIterator = pOperands.iterator();
          if (!operandIterator.hasNext()) {
            return getTrue();
          }
          ExpressionTree<LeafType> first = operandIterator.next();
          if (!operandIterator.hasNext()) {
            return first;
          }
          ImmutableSet.Builder<ExpressionTree<LeafType>> keyBuilder = ImmutableSet.builder();
          keyBuilder.add(first);
          while (operandIterator.hasNext()) {
            keyBuilder.add(operandIterator.next());
          }
          key = keyBuilder.build();
        }
        ExpressionTree<LeafType> result = andCache.get(key);
        if (result != null) {
          return result;
        }
        result = And.of(key);
        andCache.put(key, result);
        return result;
      }

      @Override
      public ExpressionTree<LeafType> or(
          ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2) {
        return or(ImmutableSet.of(pOp1, pOp2));
      }

      @Override
      public ExpressionTree<LeafType> or(Iterable<ExpressionTree<LeafType>> pOperands) {
        final Set<ExpressionTree<LeafType>> key;
        if (pOperands instanceof Set) {
          key = (Set<ExpressionTree<LeafType>>) pOperands;
        } else {
          Iterator<ExpressionTree<LeafType>> operandIterator = pOperands.iterator();
          if (!operandIterator.hasNext()) {
            return getFalse();
          }
          ExpressionTree<LeafType> first = operandIterator.next();
          if (!operandIterator.hasNext()) {
            return first;
          }
          ImmutableSet.Builder<ExpressionTree<LeafType>> keyBuilder = ImmutableSet.builder();
          keyBuilder.add(first);
          while (operandIterator.hasNext()) {
            keyBuilder.add(operandIterator.next());
          }
          key = keyBuilder.build();
        }
        ExpressionTree<LeafType> result = orCache.get(key);
        if (result != null) {
          return result;
        }
        result = Or.of(key);
        orCache.put(key, result);
        return result;
      }
    };

  }

  public static <LeafType> Comparator<ExpressionTree<LeafType>> getComparator() {
    return new ExpressionTreeComparator<>();
  }

  private static class ExpressionTreeComparator<LeafType>
      implements Comparator<ExpressionTree<LeafType>>, Serializable {

    private static final long serialVersionUID = -8004131077972723263L;

    @Override
    public int compare(final ExpressionTree<LeafType> pO1, final ExpressionTree<LeafType> pO2) {
      @SuppressWarnings("unchecked")
      int typeOrder1 =
          pO1.accept(
              (ExpressionTreeVisitor<LeafType, Integer, RuntimeException>) TYPE_ORDER_VISITOR);
      @SuppressWarnings("unchecked")
      int typeOrder2 =
          pO2.accept(
              (ExpressionTreeVisitor<LeafType, Integer, RuntimeException>) TYPE_ORDER_VISITOR);
      final int typeOrderComp = Integer.compare(typeOrder1, typeOrder2);
      final Ordering<Iterable<ExpressionTree<LeafType>>> lexicographicalOrdering =
          Ordering.<ExpressionTree<LeafType>>from(this).lexicographical();
      return pO1.accept(
          new CachingVisitor<LeafType, Integer, RuntimeException>() {

            @Override
            protected Integer cacheMissAnd(And<LeafType> pAnd) {
              if (pO2 instanceof And) {
                And<LeafType> other = (And<LeafType>) pO2;
                return lexicographicalOrdering.compare(pAnd, other);
              }
              return typeOrderComp;
            }

            @Override
            protected Integer cacheMissOr(Or<LeafType> pOr) {
              if (pO2 instanceof Or) {
                Or<LeafType> other = (Or<LeafType>) pO2;
                return lexicographicalOrdering.compare(pOr, other);
              }
              return typeOrderComp;
            }

            @Override
            protected Integer cacheMissLeaf(LeafExpression<LeafType> pLeafExpression) {
              if (pO2 instanceof LeafExpression) {
                LeafExpression<LeafType> other = (LeafExpression<LeafType>) pO2;
                LeafType o1 = pLeafExpression.getExpression();
                LeafType o2 = other.getExpression();
                int leafComp = Ordering.usingToString().compare(o1, o2);
                if (leafComp != 0) {
                  return leafComp;
                }
                return Boolean.compare(pLeafExpression.assumeTruth(), other.assumeTruth());
              }
              return typeOrderComp;
            }

            @Override
            public Integer cacheMissTrue() {
              if (pO2.equals(ExpressionTrees.getTrue())) {
                return 0;
              }
              return typeOrderComp;
            }

            @Override
            public Integer cacheMissFalse() {
              if (pO2.equals(ExpressionTrees.getFalse())) {
                return 0;
              }
              return typeOrderComp;
            }
          });
    }

  }

  private static final ExpressionTreeVisitor<Object, Integer, RuntimeException> TYPE_ORDER_VISITOR =
      new ExpressionTreeVisitor<Object, Integer, RuntimeException>() {

        @Override
        public Integer visitFalse() {
          return 0;
        }

        @Override
        public Integer visitTrue() {
          return 1;
        }

        @Override
        public Integer visit(LeafExpression<Object> pLeafExpression) {
          return 2;
        }

        @Override
        public Integer visit(Or<Object> pOr) {
          return 3;
        }

        @Override
        public Integer visit(And<Object> pAnd) {
          return 4;
        }
      };

}

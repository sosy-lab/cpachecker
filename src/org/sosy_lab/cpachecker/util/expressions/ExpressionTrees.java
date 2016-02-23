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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;



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
        pExpressionTree.accept(
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

  public static <LeafType> boolean implies(
      ExpressionTree<LeafType> pAntecedent, final ExpressionTree<?> pConsequent) {
    if (getTrue().equals(pConsequent)) {
      return true;
    }
    if (getFalse().equals(pConsequent)) {
      return getFalse().equals(pAntecedent);
    }
    return implies(
        pAntecedent,
        pConsequent,
        ExpressionTrees.<LeafType>newImpliesVisitor(pConsequent));
  }

  private static <LeafType> boolean implies(
      final ExpressionTree<LeafType> pAntecedent, final ExpressionTree<LeafType> pConsequent,
      final Map<ExpressionTree<LeafType>, ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>> pImpliesVisitors) {
    if (getTrue().equals(pConsequent)) {
      return true;
    }
    if (getFalse().equals(pConsequent)) {
      return getFalse().equals(pAntecedent);
    }
    if (pAntecedent.equals(pConsequent)) {
      return true;
    }
    return pConsequent.accept(new CachingVisitor<LeafType, Boolean, RuntimeException>() {

      private final ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> getImpliesVisitor(ExpressionTree<LeafType> pConsequent) {
        ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> impliesVisitor = pImpliesVisitors.get(pConsequent);
        if (impliesVisitor == null) {
          impliesVisitor = newImpliesVisitor(pConsequent);
          pImpliesVisitors.put(pConsequent, impliesVisitor);
        }
        return impliesVisitor;
      }

      @Override
      protected Boolean cacheMissAnd(And<LeafType> pAnd) throws RuntimeException {
        for (ExpressionTree<LeafType> operand : pAnd) {
          if (!pAntecedent.accept(getImpliesVisitor(operand))) {
            return false;
          }
        }
        return true;
      }

      @Override
      protected Boolean cacheMissOr(Or<LeafType> pOr) throws RuntimeException {
        for (ExpressionTree<LeafType> operand : pOr) {
          if (pAntecedent.accept(getImpliesVisitor(operand))) {
            return true;
          }
        }
        return false;
      }

      @Override
      protected Boolean cacheMissLeaf(LeafExpression<LeafType> pLeafExpression)
          throws RuntimeException {
        return pAntecedent.accept(getImpliesVisitor(pLeafExpression));
      }

      @Override
      protected Boolean cacheMissTrue() throws RuntimeException {
        return pAntecedent.accept(getImpliesVisitor(ExpressionTrees.<LeafType>getTrue()));
      }

      @Override
      protected Boolean cacheMissFalse() throws RuntimeException {
        return pAntecedent.accept(getImpliesVisitor(ExpressionTrees.<LeafType>getFalse()));
      }

    });
  }

  private static <LeafType> ExpressionTreeVisitor<LeafType, Boolean, RuntimeException> newImpliesVisitor(
      final ExpressionTree<?> pConsequent) {
    return new CachingVisitor<LeafType, Boolean, RuntimeException>() {

      @Override
      protected Boolean cacheMissAnd(And<LeafType> pAnd) throws RuntimeException {
        for (ExpressionTree<LeafType> operand : pAnd) {
          if (operand.accept(this)) {
            return true;
          }
        }
        return false;
      }

      @Override
      protected Boolean cacheMissOr(Or<LeafType> pOr) throws RuntimeException {
        for (ExpressionTree<LeafType> operand : pOr) {
          if (!operand.accept(this)) {
            return false;
          }
        }
        return true;
      }

      @Override
      protected Boolean cacheMissLeaf(LeafExpression<LeafType> pLeafExpression)
          throws RuntimeException {
        return pLeafExpression.equals(pConsequent);
      }

      @Override
      protected Boolean cacheMissTrue() throws RuntimeException {
        return getTrue().equals(pConsequent);
      }

      @Override
      protected Boolean cacheMissFalse() throws RuntimeException {
        return true;
      }
    };
  }

  private static <LeafType, T extends Throwable> boolean implies(
      ExpressionTree<LeafType> pAntecedent,
      ExpressionTree<?> pConsequent,
      ExpressionTreeVisitor<LeafType, Boolean, T> pImpliesVisitor) throws T {
    if (getTrue().equals(pConsequent)) {
      return true;
    }
    if (getFalse().equals(pConsequent)) {
      return getFalse().equals(pAntecedent);
    }
    return pAntecedent.accept(pImpliesVisitor);
  }

  public static <LeafType> Simplifier<LeafType> newSimplifier() {
    return new Simplifier<LeafType>() {

      private final Map<Set<ExpressionTree<LeafType>>, ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, RuntimeException>> simplificationVisitors = Maps.newHashMap();

      private final Map<ExpressionTree<LeafType>, ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>> implicationVisitors = Maps.newHashMap();

      @Override
      public ExpressionTree<LeafType> simplify(ExpressionTree<LeafType> pExpressionTree) {
        return ExpressionTrees.simplify(
            pExpressionTree,
            Collections.<ExpressionTree<LeafType>>emptySet(),
            simplificationVisitors,
            implicationVisitors);
      }

    };
  }

  public static <LeafType> ExpressionTree<LeafType> simplify(
      ExpressionTree<LeafType> pExpressionTree) {
    return simplify(
        pExpressionTree,
        Collections.<ExpressionTree<LeafType>>emptySet(),
        Maps
            .<Set<ExpressionTree<LeafType>>,
                ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, RuntimeException>>
                newHashMap(),
        Maps.<ExpressionTree<LeafType>, ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>>newHashMap());
  }

  private static <LeafType> ExpressionTree<LeafType> simplify(
      ExpressionTree<LeafType> pExpressionTree,
      final Set<ExpressionTree<LeafType>> pExternalKnowledge,
      final Map<
              Set<ExpressionTree<LeafType>>,
              ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, RuntimeException>>
          pVisitors,
      final Map<ExpressionTree<LeafType>, ExpressionTreeVisitor<LeafType, Boolean, RuntimeException>> pImpliesVisitors) {
    if (isConstant(pExpressionTree)) {
      return pExpressionTree;
    }
    if (pExternalKnowledge.contains(pExpressionTree)) {
      return getTrue();
    }
    if (pExpressionTree instanceof LeafExpression) {
      LeafExpression<LeafType> negated = ((LeafExpression<LeafType>) pExpressionTree).negate();
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
              Collection<ExpressionTree<LeafType>> operands = Sets.newHashSet();
              boolean changed = false;
              for (ExpressionTree<LeafType> operandToAdd : pAnd) {
                Set<ExpressionTree<LeafType>> knowledgeBase = Sets.newHashSet(pAnd);
                knowledgeBase.remove(operandToAdd);
                knowledgeBase.addAll(pExternalKnowledge);
                ExpressionTree<LeafType> simplified =
                    simplify(operandToAdd, knowledgeBase, pVisitors, pImpliesVisitors);
                if (getFalse().equals(simplified)) {
                  return simplified;
                }
                if (!getTrue().equals(simplified)) {
                  operands.add(simplified);
                }
                changed |= simplified != operandToAdd;
              }
              if (!changed) {
                return pAnd;
              }
              return And.of(operands);
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
                    commonFacts.retainAll(facts instanceof Collection ? (Collection<?>) facts : Lists.newArrayList(facts));
                    ++nOperands;
                  }

                  if (!commonFacts.isEmpty()) {
                    ExpressionTree<LeafType> commonFactsTree = And.of(commonFacts);
                    commonFacts.addAll(pExternalKnowledge);

                    List<ExpressionTree<LeafType>> simplifiedOperands = new ArrayList<>(nOperands);
                    List<ExpressionTree<LeafType>> operands = new ArrayList<>(nOperands);
                    for (ExpressionTree<LeafType> operand : pOr) {
                      ExpressionTree<LeafType> simplified = simplify(operand, commonFacts, pVisitors, pImpliesVisitors);
                      if (!simplified.equals(getFalse())) {
                        operands.add(operand);
                      }
                      simplifiedOperands.add(simplified);
                    }
                    // If an operand was contradictory, remove it and try again
                    if (operands.size() < simplifiedOperands.size()) {
                      return simplify(Or.of(operands), pExternalKnowledge, pVisitors, pImpliesVisitors);
                    }

                    return And.of(simplify(commonFactsTree, commonFacts, pVisitors, pImpliesVisitors), Or.of(simplifiedOperands));
                  }
                }
              }

              Collection<ExpressionTree<LeafType>> operands = Sets.newHashSet();
              boolean changed = false;

              // Simplify the operands
              for (ExpressionTree<LeafType> operandToAdd : pOr) {
                ExpressionTree<LeafType> simplified =
                    simplify(operandToAdd, pExternalKnowledge, pVisitors, pImpliesVisitors);
                if (getTrue().equals(simplified)) {
                  return simplified;
                }
                if (!getFalse().equals(simplified)) {
                  operands.add(simplified);
                }
                changed |= simplified != operandToAdd;
              }

              // Remove operands that imply other operands
              Collection<ExpressionTree<LeafType>> weakestOperands =
                  new ArrayList<>(operands.size());
              for (ExpressionTree<LeafType> operand : operands) {
                LeafExpression<LeafType> negated = null;
                if (operand instanceof LeafExpression) {
                  negated = ((LeafExpression<LeafType>) operand).negate();
                }
                boolean skip = false;
                for (ExpressionTree<LeafType> op : operands) {
                  if (op == operand) {
                    continue;
                  }
                  if (negated != null && implies(negated, op, pImpliesVisitors)) {
                    return getTrue();
                  }
                  if (!skip && implies(operand, op, pImpliesVisitors)) {
                    skip = true;
                    if (negated == null) {
                      break;
                    }
                  }
                }
                if (!skip) {
                  weakestOperands.add(operand);
                } else {
                  changed = true;
                }
              }

              if (!changed) {
                return pOr;
              }

              return Or.of(weakestOperands);
            }

            private Iterable<ExpressionTree<LeafType>> asFacts(ExpressionTree<LeafType> pExpressionTree) {
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
    return pExpressionTree.accept(visitor);
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

  public static <LeafType> Comparator<ExpressionTree<LeafType>> getComparator() {
    return new ExpressionTreeComparator<>();
  }

  private static class ExpressionTreeComparator<LeafType>
      implements Comparator<ExpressionTree<LeafType>> {

    private final Comparator<LeafType> leafExpressionComparator;

    public ExpressionTreeComparator() {
      leafExpressionComparator =
          new Comparator<LeafType>() {

            @Override
            public int compare(LeafType pO1, LeafType pO2) {
              return pO1.toString().compareTo(pO2.toString());
            }
          };
    }

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
      final int typeOrderComp = typeOrder1 - typeOrder2;
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
                int leafComp = leafExpressionComparator.compare(o1, o2);
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

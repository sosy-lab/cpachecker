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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;



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

  public static <S, T> ExpressionTree<T> convert(
      ExpressionTree<S> pSource, final Function<? super S, ? extends T> pLeafConverter) {
    final Function<ExpressionTree<S>, ExpressionTree<T>> treeConverter =
        new Function<ExpressionTree<S>, ExpressionTree<T>>() {

          @Override
          public ExpressionTree<T> apply(ExpressionTree<S> pTree) {
            return convert(pTree, pLeafConverter);
          }
        };
    return pSource.accept(
        new ExpressionTreeVisitor<S, ExpressionTree<T>, RuntimeException>() {

          @Override
          public ExpressionTree<T> visit(And<S> pAnd) {
            return And.of(FluentIterable.from(pAnd).transform(treeConverter));
          }

          @Override
          public ExpressionTree<T> visit(Or<S> pOr) {
            return Or.of(FluentIterable.from(pOr).transform(treeConverter));
          }

          @Override
          public ExpressionTree<T> visit(LeafExpression<S> pLeafExpression) {
            return LeafExpression.of(
                (T) pLeafConverter.apply(pLeafExpression.getExpression()),
                pLeafExpression.assumeTruth());
          }

          @Override
          public ExpressionTree<T> visitTrue() {
            return ExpressionTrees.getTrue();
          }

          @Override
          public ExpressionTree<T> visitFalse() {
            return ExpressionTrees.getFalse();
          }
        });
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
          new ExpressionTreeVisitor<LeafType, Integer, RuntimeException>() {

            @Override
            public Integer visit(And<LeafType> pAnd) {
              if (pO2 instanceof And) {
                And<LeafType> other = (And<LeafType>) pO2;
                return lexicographicalOrdering.compare(pAnd, other);
              }
              return typeOrderComp;
            }

            @Override
            public Integer visit(Or<LeafType> pOr) {
              if (pO2 instanceof Or) {
                Or<LeafType> other = (Or<LeafType>) pO2;
                return lexicographicalOrdering.compare(pOr, other);
              }
              return typeOrderComp;
            }

            @Override
            public Integer visit(LeafExpression<LeafType> pLeafExpression) {
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
            public Integer visitTrue() {
              if (pO2.equals(ExpressionTrees.getTrue())) {
                return 0;
              }
              return typeOrderComp;
            }

            @Override
            public Integer visitFalse() {
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

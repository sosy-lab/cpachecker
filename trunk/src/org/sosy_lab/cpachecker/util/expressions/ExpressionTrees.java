// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Comparators;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.graph.Traverser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaToCVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** This is a utility class for common operations on {@link ExpressionTree}s */
public final class ExpressionTrees {

  private static final String FUNCTION_DELIMITER = "::";

  @SuppressWarnings("unchecked")
  public static <LeafType> ExpressionTree<LeafType> getTrue() {
    return (ExpressionTree<LeafType>) TRUE;
  }

  @SuppressWarnings("unchecked")
  public static <LeafType> ExpressionTree<LeafType> getFalse() {
    return (ExpressionTree<LeafType>) FALSE;
  }

  private static final ExpressionTree<Object> TRUE =
      new AbstractExpressionTree<>() {

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
      new AbstractExpressionTree<>() {

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

  private ExpressionTrees() {}

  public static <LeafType> boolean isConstant(ExpressionTree<LeafType> pExpressionTree) {
    ExpressionTreeVisitor<LeafType, Boolean, NoException> visitor =
        new DefaultExpressionTreeVisitor<>() {

          @Override
          protected Boolean visitDefault(ExpressionTree<LeafType> pExprTree) {
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
    ExpressionTreeVisitor<LeafType, Boolean, NoException> visitor =
        new DefaultExpressionTreeVisitor<>() {

          @Override
          protected Boolean visitDefault(ExpressionTree<LeafType> pExprTree) {
            return false;
          }

          @Override
          public Boolean visit(LeafExpression<LeafType> pLeafExpression) {
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
    ExpressionTreeVisitor<LeafType, Boolean, NoException> visitor =
        new DefaultExpressionTreeVisitor<>() {

          @Override
          protected Boolean visitDefault(ExpressionTree<LeafType> pExprTree) {
            return false;
          }

          @Override
          public Boolean visit(Or<LeafType> pOr) {
            return true;
          }
        };
    return pExpressionTree.accept(visitor);
  }

  public static <LeafType> boolean isAnd(ExpressionTree<LeafType> pExpressionTree) {
    ExpressionTreeVisitor<LeafType, Boolean, NoException> visitor =
        new DefaultExpressionTreeVisitor<>() {

          @Override
          protected Boolean visitDefault(ExpressionTree<LeafType> pExprTree) {
            return false;
          }

          @Override
          public Boolean visit(And<LeafType> pAnd) {
            return true;
          }
        };
    return pExpressionTree.accept(visitor);
  }

  public static <LeafType> Iterable<ExpressionTree<LeafType>> traverseRecursively(
      ExpressionTree<LeafType> pExpressionTree) {
    return Traverser.<ExpressionTree<LeafType>>forTree(node -> getChildren(node))
        .depthFirstPreOrder(pExpressionTree);
  }

  public static <LeafType> boolean isInCNF(ExpressionTree<LeafType> pExpressionTree) {
    ExpressionTreeVisitor<LeafType, Boolean, NoException> visitor =
        new ExpressionTreeVisitor<>() {

          @Override
          public Boolean visit(And<LeafType> pAnd) {
            // A clause may be a single literal or a disjunction of literals
            assert getChildren(pAnd).allMatch(pClause -> !isAnd(pClause))
                : "A conjunction must not contain child conjunctions";
            return getChildren(pAnd).allMatch(pClause -> isCNFClause(pClause));
          }

          @Override
          public Boolean visit(Or<LeafType> pOr) {
            return getChildren(pOr).allMatch(ExpressionTrees::isLeaf);
          }

          @Override
          public Boolean visit(LeafExpression<LeafType> pLeafExpression) {
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
    ExpressionTreeVisitor<LeafType, Boolean, NoException> visitor =
        new ExpressionTreeVisitor<>() {

          @Override
          public Boolean visit(And<LeafType> pAnd) {
            // Check: One clause with more than one literal
            return getChildren(pAnd).allMatch(ExpressionTrees::isLeaf);
          }

          @Override
          public Boolean visit(Or<LeafType> pOr) {
            // A clause may be a single literal or a conjunction of literals
            assert getChildren(pOr).allMatch(pClause -> !isOr(pClause))
                : "A disjunction must not contain child disjunctions";
            return getChildren(pOr).allMatch(pClause -> isDNFClause(pClause));
          }

          @Override
          public Boolean visit(LeafExpression<LeafType> pLeafExpression) {
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
      return Or.of(getChildren(pExpressionTree).transform(pExprTree -> toDNF(pExprTree)));
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
        newClauses.add(And.of(ImmutableList.of(combinatorA, combinatorB)));
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
      return And.of(getChildren(pExpressionTree).transform(pExprTree -> toCNF(pExprTree)));
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
        newClauses.add(Or.of(ImmutableList.of(combinatorA, combinatorB)));
      }
    }
    return And.of(newClauses);
  }

  public static <LeafType> boolean isCNFClause(ExpressionTree<LeafType> pExpressionTree) {
    return isLeaf(pExpressionTree)
        || (isOr(pExpressionTree)
            && getChildren(pExpressionTree).allMatch(ExpressionTrees::isLeaf));
  }

  public static <LeafType> boolean isDNFClause(ExpressionTree<LeafType> pExpressionTree) {
    return isLeaf(pExpressionTree)
        || (isAnd(pExpressionTree)
            && getChildren(pExpressionTree).allMatch(ExpressionTrees::isLeaf));
  }

  public static <LeafType> FluentIterable<ExpressionTree<LeafType>> getChildren(
      ExpressionTree<LeafType> pExpressionTree) {
    return FluentIterable.from(
        pExpressionTree.accept(
            new DefaultExpressionTreeVisitor<
                LeafType, Iterable<ExpressionTree<LeafType>>, NoException>() {

              @Override
              protected Iterable<ExpressionTree<LeafType>> visitDefault(
                  ExpressionTree<LeafType> pExprTree) {
                return ImmutableSet.of();
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
    return newSimplifier(ExpressionTrees.newFactory());
  }

  public static <LeafType> Simplifier<LeafType> newSimplifier(
      final ExpressionTreeFactory<LeafType> pFactory) {
    return new Simplifier<>() {

      private final Map<
              Set<ExpressionTree<LeafType>>,
              ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, NoException>>
          simplificationVisitors = new HashMap<>();

      @Override
      public ExpressionTree<LeafType> simplify(ExpressionTree<LeafType> pExpressionTree) {
        return ExpressionTrees.simplify(
            pExpressionTree, ImmutableSet.of(), simplificationVisitors, pFactory, true);
      }
    };
  }

  public static <LeafType> ExpressionTree<LeafType> simplify(
      ExpressionTree<LeafType> pExpressionTree) {
    return simplify(pExpressionTree, ExpressionTrees.newFactory());
  }

  private static <LeafType> ExpressionTree<LeafType> simplify(
      ExpressionTree<LeafType> pExpressionTree, ExpressionTreeFactory<LeafType> pFactory) {
    return simplify(pExpressionTree, ImmutableSet.of(), new HashMap<>(), pFactory, true);
  }

  private static <LeafType> ExpressionTree<LeafType> simplify(
      ExpressionTree<LeafType> pExpressionTree,
      final Set<ExpressionTree<LeafType>> pExternalKnowledge,
      final Map<
              Set<ExpressionTree<LeafType>>,
              ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, NoException>>
          pVisitorCache,
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
    ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, NoException> visitor =
        pVisitorCache.get(pExternalKnowledge);
    if (visitor == null) {
      visitor = new SimplificationVisitor<>(pExternalKnowledge, pFactory, pVisitorCache, pThorough);
      pVisitorCache.put(pExternalKnowledge, visitor);
    }
    return expressionTree.accept(visitor);
  }

  public static <S, T> ExpressionTree<T> convert(
      ExpressionTree<S> pSource, final Function<? super S, ? extends T> pLeafConverter) {
    final Function<ExpressionTree<S>, ExpressionTree<T>> convert =
        pTree -> convert(pTree, pLeafConverter);
    ExpressionTreeVisitor<S, ExpressionTree<T>, NoException> converter =
        new CachingVisitor<>() {

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
                checkNotNull((T) pLeafConverter.apply(pLeafExpression.getExpression())),
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
   * Builds an expression tree for the given {@link BooleanFormula}. If the formula is invalid, i.e.
   * a literal/variable from another method is present (not in scope), the expression tree
   * representing true is returned.
   *
   * <p>Hint: This method can be used to get a C-like assumptions from a boolean formula, obtained
   * using the toStrng() method of the expression tree
   *
   * @param formula the formula to transform
   * @param fMgr the formula manger having the formula "in scope"
   * @param location to determine the current method for checking the scope.
   * @return the expression tree representing the formula.
   */
  public static ExpressionTree<Object> fromFormula(
      BooleanFormula formula, FormulaManagerView fMgr, CFANode location)
      throws InterruptedException {

    BooleanFormula inv = formula;
    String prefix = location.getFunctionName() + FUNCTION_DELIMITER;

    inv =
        fMgr.filterLiterals(
            inv,
            e -> {
              for (String name : fMgr.extractVariableNames(e)) {
                if (name.contains(FUNCTION_DELIMITER) && !name.startsWith(prefix)) {
                  return false;
                }
              }
              return true;
            });

    FormulaToCVisitor v = new FormulaToCVisitor(fMgr);
    boolean isValid = fMgr.visit(inv, v);
    if (isValid) {
      return LeafExpression.of(v.getString());
    }
    return ExpressionTrees.getTrue();
  }

  /**
   * Cast an expression tree with a source leaf type to an expression tree with a target leaf type.
   *
   * <p>This unchecked cast is safe if the expression tree is immutable, which every expression tree
   * is required to be by convention.
   *
   * @param pToCast the tree to be casted.
   * @return the casted tree.
   */
  @SuppressWarnings("unchecked")
  public static <LeafTypeS extends LeafTypeT, LeafTypeT> ExpressionTree<LeafTypeT> cast(
      ExpressionTree<LeafTypeS> pToCast) {
    return (ExpressionTree<LeafTypeT>) pToCast;
  }

  public static <LeafType> ExpressionTreeFactory<LeafType> newFactory() {
    return new CachingExpressionTreeFactory<>();
  }

  public static <LeafType> Comparator<ExpressionTree<LeafType>> getComparator() {
    return new ExpressionTreeComparator<>();
  }

  private static class ExpressionTreeComparator<LeafType>
      implements Comparator<ExpressionTree<LeafType>>, Serializable {

    private static final long serialVersionUID = -8004131077972723263L;

    private final Comparator<Iterable<ExpressionTree<LeafType>>> lexicographicalOrdering =
        Comparators.lexicographical(this);

    @Override
    public int compare(final ExpressionTree<LeafType> pO1, final ExpressionTree<LeafType> pO2) {
      @SuppressWarnings("unchecked")
      int typeOrder1 =
          pO1.accept((ExpressionTreeVisitor<LeafType, Integer, NoException>) TYPE_ORDER_VISITOR);
      @SuppressWarnings("unchecked")
      int typeOrder2 =
          pO2.accept((ExpressionTreeVisitor<LeafType, Integer, NoException>) TYPE_ORDER_VISITOR);
      final int typeOrderComp = Integer.compare(typeOrder1, typeOrder2);
      return pO1.accept(
          new CachingVisitor<LeafType, Integer, NoException>() {

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

  private static class SimplificationVisitor<LeafType>
      extends CachingVisitor<LeafType, ExpressionTree<LeafType>, NoException> {
    private final Set<ExpressionTree<LeafType>> externalKnowledge;
    private final ExpressionTreeFactory<LeafType> factory;
    private final Map<
            Set<ExpressionTree<LeafType>>,
            ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, NoException>>
        visitorCache;
    private final boolean thorough;

    private SimplificationVisitor(
        Set<ExpressionTree<LeafType>> pExternalKnowledge,
        ExpressionTreeFactory<LeafType> pFactory,
        Map<
                Set<ExpressionTree<LeafType>>,
                ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, NoException>>
            pVisitorCache,
        boolean pThorough) {
      externalKnowledge = pExternalKnowledge;
      factory = pFactory;
      visitorCache = pVisitorCache;
      thorough = pThorough;
    }

    @Override
    public ExpressionTree<LeafType> cacheMissAnd(And<LeafType> pAnd) {
      List<ExpressionTree<LeafType>> operands = Lists.newLinkedList(pAnd);
      boolean changedGlobally = false;
      boolean changed = false;
      Set<ExpressionTree<LeafType>> knowledgeBase = externalKnowledge;
      do {
        changed = false;
        ListIterator<ExpressionTree<LeafType>> operandIt = operands.listIterator();
        while (operandIt.hasNext()) {
          ExpressionTree<LeafType> current = operandIt.next();
          ExpressionTree<LeafType> simplifiedCurrent =
              simplify(current, knowledgeBase, visitorCache, factory, thorough);
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
                  } else {
                    simplifiedCurrent = current;
                  }
                } else {
                  simplifiedCurrent =
                      simplify(current, Collections.singleton(other), visitorCache, factory, false);
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
      return factory.and(operands);
    }

    @Override
    public ExpressionTree<LeafType> cacheMissOr(Or<LeafType> pOr) {

      // 1. If we can factor out common facts of the operands, we do so and call simplify
      // recursively.
      // Next time we enter, there are no common facts and we continue with 2.
      Iterator<ExpressionTree<LeafType>> opIt = pOr.iterator();
      if (opIt.hasNext()) {
        int nOperands = 1;
        Set<ExpressionTree<LeafType>> commonFacts = Sets.newHashSet(asFacts(opIt.next()));
        if (opIt.hasNext()) {
          while (!commonFacts.isEmpty() && opIt.hasNext()) {
            Iterable<ExpressionTree<LeafType>> facts = asFacts(opIt.next());
            commonFacts.retainAll(
                facts instanceof Collection ? (Collection<?>) facts : Lists.newArrayList(facts));
            ++nOperands;
          }

          if (!commonFacts.isEmpty()) {
            ExpressionTree<LeafType> commonFactsTree = And.of(commonFacts);
            commonFacts.addAll(externalKnowledge);

            List<ExpressionTree<LeafType>> simplifiedOperands = new ArrayList<>(nOperands);
            List<ExpressionTree<LeafType>> operands = new ArrayList<>(nOperands);
            for (ExpressionTree<LeafType> operand : pOr) {
              ExpressionTree<LeafType> simplified =
                  simplify(operand, commonFacts, visitorCache, factory, thorough);
              if (!simplified.equals(getFalse())) {
                operands.add(operand);
              }
              simplifiedOperands.add(simplified);
            }
            // If an operand was contradictory, remove it and try again
            if (operands.size() < simplifiedOperands.size()) {
              return simplify(
                  factory.or(operands), externalKnowledge, visitorCache, factory, thorough);
            }

            return factory.and(
                simplify(commonFactsTree, externalKnowledge, visitorCache, factory, thorough),
                factory.or(simplifiedOperands));
          }
        }
      }

      List<ExpressionTree<LeafType>> operands = new ArrayList<>();
      Set<ExpressionTree<LeafType>> changedOps = new HashSet<>();
      boolean changed = false;

      // 2. Simplify the operands
      for (ExpressionTree<LeafType> operandToAdd : pOr) {
        ExpressionTree<LeafType> simplified =
            simplify(operandToAdd, externalKnowledge, visitorCache, factory, thorough);
        if (getTrue().equals(simplified)) {
          return simplified;
        }
        if (!getFalse().equals(simplified)) {
          operands.add(simplified);
        }
        if (simplified != operandToAdd) {
          changed = true;
          if (!thorough) {
            if (changedOps.isEmpty()) {
              changedOps = new HashSet<>();
            }
            changedOps.add(simplified);
          }
        }
      }

      // 3. Remove operands that imply other operands
      if (changed || thorough) {
        Iterator<ExpressionTree<LeafType>> operandIt = operands.iterator();
        while (operandIt.hasNext()) {
          ExpressionTree<LeafType> operand = operandIt.next();
          Iterable<ExpressionTree<LeafType>> relevantInnerOperands =
              thorough || changedOps.contains(operand) ? operands : changedOps;
          if (operand instanceof LeafExpression) {
            if (isImplied(operand, relevantInnerOperands, true)) {
              // we proved !a -> b, so a v b v ... has to be a tautology:
              return getTrue();
            }
          }
          if (isImplied(operand, relevantInnerOperands, false)) {
            // we proved that a -> b, so we can remove b from a v b v ...
            changed = true;
            operandIt.remove();
          }
        }
      }

      if (!changed) {
        return pOr;
      }

      return factory.or(operands);
    }

    private boolean isImplied(
        ExpressionTree<LeafType> originalOperand,
        Iterable<ExpressionTree<LeafType>> relevantInnerOperands,
        boolean negate) {

      ExpressionTree<LeafType> operandForCheck = originalOperand;
      if (negate) {
        operandForCheck = ((LeafExpression<LeafType>) originalOperand).negate();
      }

      if (!externalKnowledge.contains(operandForCheck)) {
        for (ExpressionTree<LeafType> op : relevantInnerOperands) {
          if (op == originalOperand) {
            continue;
          }
          if (getTrue()
              .equals(
                  simplify(
                      op, Collections.singleton(operandForCheck), visitorCache, factory, false))) {
            return true;
          }
        }
      }
      return false;
    }

    private Iterable<ExpressionTree<LeafType>> asFacts(ExpressionTree<LeafType> pExprTree) {
      if (isAnd(pExprTree)) {
        return getChildren(pExprTree);
      }
      return Collections.singleton(pExprTree);
    }

    @Override
    public ExpressionTree<LeafType> cacheMissLeaf(LeafExpression<LeafType> pLeafExpression) {
      return pLeafExpression;
    }

    @Override
    public ExpressionTree<LeafType> cacheMissTrue() {
      return getTrue();
    }

    @Override
    public ExpressionTree<LeafType> cacheMissFalse() {
      return getFalse();
    }
  }

  private static final ExpressionTreeVisitor<Object, Integer, NoException> TYPE_ORDER_VISITOR =
      new ExpressionTreeVisitor<>() {

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

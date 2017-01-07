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
package org.sosy_lab.cpachecker.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.sosy_lab.common.annotations.FieldsAreNonnullByDefault;
import org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 *<p>
 * The class computes global control-flow and context-insensitive over-approximation of sets of relevant variables and
 * structure/union fields. Relevance is determined by usage of variables/fields in assumption CFA edges.
 * It is transitively closed under data flow dependencies and is approximated over pointer dereferences,
 * i.e. assignment to a pointer dereference always makes all the variables occurring in the RHS relevant.
 *</p>
 *<p>
 * Dependencies on the addresses of variables/fields are treated as dependencies on the variables/fields themselves.
 * So pointer variables/fields transitively depend on all variables/fields possibly addressed by them. In cases when
 * some parts of the LHS may depend on the values of other parts (e.g. {@code a[i]}, {@code g->f->h}), the former
 * are treated as RHSs.
 *</p>
 * The class implements functional interface through a single function {@link #handleEdge(CFAEdge)}. It should be
 * called for all CFA edges needed to be counted in relevance computation (both for data dependencies and variable/field
 * usage). The returned {@link VarFieldDependencies} instances should be merged with
 * {@link VarFieldDependencies#withDependencies(VarFieldDependencies)}. Then the resulting sets can be obtained by
 * invoking {@link VarFieldDependencies#computeRelevantVariablesAndFields()}.
 *
 * The class also collects a set of all explicitly addressed variables that is returned by
 * {@link VarFieldDependencies#computeAddressedVariables()}.
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
@FieldsAreNonnullByDefault
public final class VariableAndFieldRelevancyComputer {
  /** Represents an approximation of a node in dependency graph i.e. variable, field or `top' (unknown location). */
  private abstract static class VariableOrField implements Comparable<VariableOrField> {
    private static final class Unknown extends VariableOrField {
      private Unknown() {}

      @Override
      public String toString() {
        return "<Unknown>";
      }

      @Override
      public boolean equals(final Object o) {
        if (o == this) {
          return true;
        } else if (!(o instanceof Unknown)) {
          return false;
        } else {
          return true;
        }
      }

      @Override
      public int compareTo(final VariableOrField other) {
        if (this == other) {
          return 0;
        } else if (other instanceof Variable) {
          return -1;
        } else if (other instanceof Field) {
          return -1;
        } else {
          throw new AssertionError("Should not happen: all cases are covered above");
        }
      }

      @Override
      public int hashCode() {
        return 7;
      }

      private static final Unknown INSTANCE = new Unknown();
    }

    private static final class Variable extends VariableOrField {
      private Variable(final String scopedName) {
        this.scopedName = scopedName;
      }

      public String getScopedName() {
        return scopedName;
      }

      @Override
      public String toString() {
        return getScopedName();
      }

      @Override
      public boolean equals(final Object o) {
        if (o == this) {
          return true;
        } else if (!(o instanceof Variable)) {
          return false;
        } else {
          final Variable other = (Variable) o;
          return this.scopedName.equals(other.scopedName);
        }
      }

      @Override
      public int compareTo(final VariableOrField other) {
        if (this == other) {
          return 0;
        } else if (other instanceof Unknown) {
          return 1;
        } else if (other instanceof Field) {
          return -1;
        } else if (other instanceof Variable) {
          return scopedName.compareTo(((Variable) other).scopedName);
        } else {
          throw new AssertionError("Should not happen: all cases are covered above");
        }
      }

      @Override
      public int hashCode() {
        return scopedName.hashCode();
      }

      private final String scopedName;
    }

    private static final class Field extends VariableOrField {
      private Field(final CCompositeType composite, final String name) {
        this.composite = composite;
        this.name = name;
      }

      public CCompositeType getCompositeType() {
        return composite;
      }

      public String getName() {
        return name;
      }

      @Override
      public String toString() {
        return composite + SCOPE_SEPARATOR + name;
      }

      @Override
      public boolean equals(final Object o) {
        if (o == this) {
          return true;
        } else if (!(o instanceof Field)) {
          return false;
        } else {
          final Field other = (Field) o;
          return this.composite.equals(other.composite) && this.name.equals(other.name);
        }
      }

      @Override
      public int compareTo(final VariableOrField other) {
        if (this == other) {
          return 0;
        } else if (other instanceof Unknown) {
          return 1;
        } else if (other instanceof Variable) {
          return 1;
        } else if (other instanceof Field) {
          final Field o = (Field) other;
          final int result = composite.getQualifiedName().compareTo(o.composite.getQualifiedName());
          return result != 0 ? result : name.compareTo(o.name);
        } else {
          throw new AssertionError("Should not happen: all cases are covered above");
        }
      }

      @Override
      public int hashCode() {
        final int prime = 67;
        return prime * composite.hashCode() + name.hashCode();
      }

      private final CCompositeType composite;
      private final String name;
    }

    private VariableOrField() {}

    public static Variable newVariable(final String scopedName) {
      return new Variable(scopedName);
    }

    public static Field newField(final CCompositeType composite, final String name) {
      return new Field(composite, name);
    }

    public static Unknown unknown() {
      return Unknown.INSTANCE;
    }

    public boolean isVariable() {
      return this instanceof Variable;
    }

    public boolean isField() {
      return this instanceof Field;
    }

    public boolean isUnknown() {
      return this instanceof Unknown;
    }

    public Variable asVariable() {
      if (this instanceof Variable) {
        return (Variable) this;
      } else {
        throw new ClassCastException(
            "Tried to match " + this.getClass().getName() + " with " + Variable.class.getName());
      }
    }

    public Field asField() {
      if (this instanceof Field) {
        return (Field) this;
      } else {
        throw new ClassCastException(
            "Tried to match " + this.getClass().getName() + " with " + Field.class.getName());
      }
    }

    @Override
    public abstract boolean equals(final Object other);

    @Override
    public abstract int hashCode();

    @Override
    public abstract int compareTo(final VariableOrField other);
  }

  public static final class VarFieldDependencies {
    @SuppressWarnings("unchecked") // Cloning here should work faster than adding all elements
    private static <T> Set<T> copy(final Set<T> source) {
      if (source instanceof HashSet) {
        return (Set<T>) ((HashSet<T>) source).clone();
      } else {
        return new HashSet<>(source);
      }
    }

    private static <T1, T2> Multimap<T1, T2> copy(final Multimap<T1, T2> source) {
      return HashMultimap.create(source);
    }

    private VarFieldDependencies(
        Set<String> relevantVariables,
        Multimap<CCompositeType, String> relevantFields,
        Multimap<CCompositeType, String> addressedFields,
        Set<String> addressedVariables,
        Multimap<VariableOrField, VariableOrField> dependencies,
        PersistentList<VarFieldDependencies> pendingMerges,
        int currentSize,
        int pendingSize,
        final boolean forceSquash) {
      if ((currentSize > 0 && pendingSize > currentSize)
          || (currentSize == 0 && pendingSize >= INITIAL_SIZE)
          || forceSquash) {
        relevantVariables = copy(relevantVariables);
        relevantFields = copy(relevantFields);
        addressedFields = copy(addressedFields);
        addressedVariables = copy(addressedVariables);
        dependencies = copy(dependencies);
        Queue<PersistentList<VarFieldDependencies>> queue = new ArrayDeque<>();
        queue.add(pendingMerges);
        while (!queue.isEmpty()) {
          for (VarFieldDependencies deps : queue.poll()) {
            for (final String e : deps.relevantVariables) {
              relevantVariables.add(e);
            }
            for (final Map.Entry<CCompositeType, String> e : deps.relevantFields.entries()) {
              relevantFields.put(e.getKey(), e.getValue());
            }
            for (final Map.Entry<CCompositeType, String> e : deps.addressedFields.entries()) {
              addressedFields.put(e.getKey(), e.getValue());
            }
            for (final String e : deps.addressedVariables) {
              addressedVariables.add(e);
            }
            for (final Map.Entry<VariableOrField, VariableOrField> e :
                deps.dependencies.entries()) {
              dependencies.put(e.getKey(), e.getValue());
            }
            if (!deps.pendingMerges.isEmpty()) {
              queue.add(deps.pendingMerges);
            }
          }
        }
        pendingMerges = PersistentLinkedList.of();
        currentSize = currentSize + pendingSize;
        pendingSize = 0;
      }
      this.relevantVariables = relevantVariables;
      this.relevantFields = relevantFields;
      this.addressedFields = addressedFields;
      this.addressedVariables = addressedVariables;
      this.dependencies = dependencies;
      this.pendingMerges = pendingMerges;
      this.currentSize = currentSize;
      this.pendingSize = pendingSize;
    }

    private VarFieldDependencies(
        final Set<String> relevantVariables,
        final Multimap<CCompositeType, String> relevantFields,
        final Multimap<CCompositeType, String> addressedFields,
        final Set<String> addressedVariables,
        final Multimap<VariableOrField, VariableOrField> dependencies,
        final PersistentList<VarFieldDependencies> pendingMerges,
        final int currentSize,
        final int pendingSize) {
      this(
          relevantVariables,
          relevantFields,
          addressedFields,
          addressedVariables,
          dependencies,
          pendingMerges,
          currentSize,
          pendingSize,
          false);
    }

    public static VarFieldDependencies emptyDependencies() {
      return EMPTY_DEPENDENCIES;
    }

    public VarFieldDependencies withDependency(
        final VariableOrField lhs, final VariableOrField rhs) {
      if (!lhs.isUnknown()) {
        final VarFieldDependencies singleDependency =
            new VarFieldDependencies(
                ImmutableSet.of(),
                ImmutableMultimap.of(),
                ImmutableMultimap.of(),
                ImmutableSet.of(),
                ImmutableMultimap.of(lhs, rhs),
                PersistentLinkedList.of(),
                1,
                0);
        return new VarFieldDependencies(
            relevantVariables,
            relevantFields,
            addressedFields,
            addressedVariables,
            dependencies,
            pendingMerges.with(singleDependency),
            currentSize,
            pendingSize + 1);
      } else {
        if (rhs.isVariable()) {
          final VarFieldDependencies singleVariable =
              new VarFieldDependencies(
                  ImmutableSet.of(rhs.asVariable().getScopedName()),
                  ImmutableMultimap.of(),
                  ImmutableMultimap.of(),
                  ImmutableSet.of(),
                  ImmutableMultimap.of(),
                  PersistentLinkedList.of(),
                  1,
                  0);
          return new VarFieldDependencies(
              relevantVariables,
              relevantFields,
              addressedFields,
              addressedVariables,
              dependencies,
              pendingMerges.with(singleVariable),
              currentSize,
              pendingSize + 1);
        } else if (rhs.isField()) {
          final VariableOrField.Field field = rhs.asField();
          final VarFieldDependencies singleField =
              new VarFieldDependencies(
                  ImmutableSet.of(),
                  ImmutableMultimap.of(field.getCompositeType(), field.getName()),
                  ImmutableMultimap.of(),
                  ImmutableSet.of(),
                  ImmutableMultimap.of(),
                  PersistentLinkedList.of(),
                  1,
                  0);
          return new VarFieldDependencies(
              relevantVariables,
              relevantFields,
              addressedFields,
              addressedVariables,
              dependencies,
              pendingMerges.with(singleField),
              currentSize,
              pendingSize + 1);
        } else if (rhs.isUnknown()) {
          throw new IllegalArgumentException("Can't handle dependency on Unknown");
        } else {
          throw new AssertionError("Should be unreachable: all possible cases already handled");
        }
      }
    }

    public VarFieldDependencies withAddressedVariable(final VariableOrField.Variable variable) {
      final VarFieldDependencies singleVariable =
          new VarFieldDependencies(
              ImmutableSet.of(),
              ImmutableMultimap.of(),
              ImmutableMultimap.of(),
              ImmutableSet.of(variable.getScopedName()),
              ImmutableMultimap.of(),
              PersistentLinkedList.of(),
              1,
              0);
      return new VarFieldDependencies(
          relevantVariables,
          relevantFields,
          addressedFields,
          addressedVariables,
          dependencies,
          pendingMerges.with(singleVariable),
          currentSize,
          pendingSize + 1);
    }

    public VarFieldDependencies withAddressedField(final VariableOrField.Field field) {
      final VarFieldDependencies singleField =
          new VarFieldDependencies(
              ImmutableSet.of(),
              ImmutableMultimap.of(),
              ImmutableMultimap.of(field.getCompositeType(), field.getName()),
              ImmutableSet.of(),
              ImmutableMultimap.of(),
              PersistentLinkedList.of(),
              1,
              0);
      return new VarFieldDependencies(
          relevantVariables,
          relevantFields,
          addressedFields,
          addressedVariables,
          dependencies,
          pendingMerges.with(singleField),
          currentSize,
          pendingSize + 1);
    }

    public VarFieldDependencies withDependencies(final VarFieldDependencies other) {
      if (currentSize + pendingSize == 0) {
        return other;
      }
      if (other.currentSize + other.pendingSize == 0) {
        return this;
      }
      // This shouldn't matter much as merging has linear complexity anyway
      // But probably we can get slightly faster by cloning the larger hash sets and iterating over smaller ones
      // As we don't have exact hash set sizes this is only a heuristic
      if (currentSize >= other.currentSize) {
        return new VarFieldDependencies(
            relevantVariables,
            relevantFields,
            addressedFields,
            addressedVariables,
            dependencies,
            pendingMerges.with(other),
            currentSize,
            pendingSize + other.currentSize + other.pendingSize);
      } else {
        return new VarFieldDependencies(
            other.relevantVariables,
            other.relevantFields,
            other.addressedFields,
            other.addressedVariables,
            other.dependencies,
            other.pendingMerges.with(this),
            other.currentSize,
            other.pendingSize + currentSize + pendingSize);
      }
    }

    private void ensureSquashed() {
      if (squashed == null) {
        squashed =
            new VarFieldDependencies(
                relevantVariables,
                relevantFields,
                addressedFields,
                addressedVariables,
                dependencies,
                pendingMerges,
                currentSize,
                pendingSize,
                true);
      }
    }

    public ImmutableSet<String> computeAddressedVariables() {
      ensureSquashed();
      return ImmutableSet.copyOf(squashed.addressedVariables);
    }

    public ImmutableMultimap<CCompositeType, String> computeAddressedFields() {
      ensureSquashed();
      return ImmutableMultimap.copyOf(squashed.addressedFields);
    }

    public Pair<ImmutableSet<String>, ImmutableMultimap<CCompositeType, String>>
        computeRelevantVariablesAndFields() {
      ensureSquashed();
      Queue<VariableOrField> queue =
          new ArrayDeque<>(squashed.relevantVariables.size() + squashed.relevantFields.size());
      Set<String> currentRelevantVariables = copy(squashed.relevantVariables);
      Multimap<CCompositeType, String> currentRelevantFields = copy(squashed.relevantFields);
      for (final String relevantVariable : squashed.relevantVariables) {
        queue.add(VariableOrField.newVariable(relevantVariable));
      }
      for (final Map.Entry<CCompositeType, String> relevantField :
          squashed.relevantFields.entries()) {
        queue.add(VariableOrField.newField(relevantField.getKey(), relevantField.getValue()));
      }
      while (!queue.isEmpty()) {
        for (VariableOrField variableOrField : squashed.dependencies.get(queue.poll())) {
          assert variableOrField.isVariable() || variableOrField.isField()
              : "Match failure: neither variable nor field!";
          if (variableOrField.isVariable()) {
            final VariableOrField.Variable variable = variableOrField.asVariable();
            if (currentRelevantVariables.add(variable.getScopedName())) {
              queue.add(variable);
            }
          } else { // Field
            final VariableOrField.Field field = variableOrField.asField();
            if (currentRelevantFields.put(field.getCompositeType(), field.getName())) {
              queue.add(field);
            }
          }
        }
      }

      return Pair.of(
          ImmutableSet.copyOf(currentRelevantVariables),
          ImmutableMultimap.copyOf(currentRelevantFields));
    }

    private final Set<String> relevantVariables;
    private final Multimap<CCompositeType, String> relevantFields;
    private final Multimap<CCompositeType, String> addressedFields;
    private final Set<String> addressedVariables;
    private final Multimap<VariableOrField, VariableOrField> dependencies;
    private final PersistentList<VarFieldDependencies> pendingMerges;
    private final int currentSize, pendingSize;
    private @Nullable VarFieldDependencies squashed = null;

    private static final int INITIAL_SIZE = 500;
    private static final VarFieldDependencies EMPTY_DEPENDENCIES =
        new VarFieldDependencies(
            ImmutableSet.of(),
            ImmutableMultimap.of(),
            ImmutableMultimap.of(),
            ImmutableSet.of(),
            ImmutableMultimap.of(),
            PersistentLinkedList.of(),
            0,
            0);
  }

  private static final class CollectingLHSVisitor
      extends DefaultCExpressionVisitor<
          Pair<VariableOrField, VarFieldDependencies>, RuntimeException> {

    private CollectingLHSVisitor() {}

    public static CollectingLHSVisitor instance() {
      return INSTANCE;
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CArraySubscriptExpression e) {
      final Pair<VariableOrField, VarFieldDependencies> r = e.getArrayExpression().accept(this);
      return Pair.of(
          r.getFirst(),
          r.getSecond()
              .withDependencies(
                  e.getSubscriptExpression().accept(CollectingRHSVisitor.create(r.getFirst()))));
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CFieldReference e) {
      final VariableOrField result =
          VariableOrField.newField(getCanonicalFieldOwnerType(e), e.getFieldName());
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      return Pair.of(
          result,
          e.getFieldOwner()
              .<VarFieldDependencies, RuntimeException>accept(CollectingRHSVisitor.create(result)));
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CPointerExpression e) {
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      return Pair.of(
          VariableOrField.unknown(),
          e.getOperand()
              .<VarFieldDependencies, RuntimeException>accept(
                  CollectingRHSVisitor.create(VariableOrField.unknown())));
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CIdExpression e) {
      return Pair.of(
          VariableOrField.newVariable(e.getDeclaration().getQualifiedName()),
          VarFieldDependencies.emptyDependencies());
    }

    @Override
    protected Pair<VariableOrField, VarFieldDependencies> visitDefault(final CExpression e) {
      if (e instanceof CUnaryExpression && UnaryOperator.AMPER == ((CUnaryExpression)e).getOperator()) {
        // TODO dependency between address and variable?
        return ((CUnaryExpression)e).getOperand().accept(this);
      }

      throw new AssertionError(
          String.format(
              "The expression %s from %s should not occur in the left hand side",
              e, e.getFileLocation()));
    }

    private static final CollectingLHSVisitor INSTANCE = new CollectingLHSVisitor();
  }

  private static final class CollectingRHSVisitor
      extends DefaultCExpressionVisitor<VarFieldDependencies, RuntimeException>
      implements CRightHandSideVisitor<VarFieldDependencies, RuntimeException> {

    private CollectingRHSVisitor(final VariableOrField lhs, final boolean addressed) {
      this.lhs = lhs;
      this.addressed = addressed;
    }

    public static CollectingRHSVisitor create(final VariableOrField lhs) {
      return new CollectingRHSVisitor(lhs, false);
    }

    private CollectingRHSVisitor createAddressed() {
      return new CollectingRHSVisitor(lhs, true);
    }

    @Override
    public VarFieldDependencies visit(final CArraySubscriptExpression e) {
      return e.getSubscriptExpression()
          .accept(this)
          .withDependencies(e.getArrayExpression().accept(this));
    }

    @Override
    public VarFieldDependencies visit(final CFieldReference e) {
      VariableOrField.Field field = VariableOrField.newField(getCanonicalFieldOwnerType(e), e.getFieldName());
      VarFieldDependencies result = e.getFieldOwner()
          .accept(this)
          .withDependency(
              lhs, field);
      if (addressed) {
        return result.withAddressedField(field);
      } else {
        return result;
      }
    }

    @Override
    public VarFieldDependencies visit(final CBinaryExpression e) {
      return e.getOperand1().accept(this).withDependencies(e.getOperand2().accept(this));
    }

    @Override
    public VarFieldDependencies visit(final CUnaryExpression e) {
      if (e.getOperator() != UnaryOperator.AMPER) {
        return e.getOperand().accept(this);
      } else {
        return e.getOperand().accept(createAddressed());
      }
    }

    @Override
    public VarFieldDependencies visit(final CPointerExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public VarFieldDependencies visit(final CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public VarFieldDependencies visit(final CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public VarFieldDependencies visit(final CIdExpression e) {
      final CSimpleDeclaration decl = e.getDeclaration();
      final VariableOrField.Variable variable =
          VariableOrField.newVariable(decl != null ? decl.getQualifiedName() : e.getName());
      final VarFieldDependencies result =
          VarFieldDependencies.emptyDependencies().withDependency(lhs, variable);
      if (addressed) {
        return result.withAddressedVariable(variable);
      }
      return result;
    }

    @Override
    public VarFieldDependencies visit(CFunctionCallExpression e) {
      VarFieldDependencies result = e.getFunctionNameExpression().accept(this);
      for (CExpression param : e.getParameterExpressions()) {
        result = result.withDependencies(param.accept(this));
      }
      return result;
    }

    @Override
    protected VarFieldDependencies visitDefault(final CExpression e) {
      return VarFieldDependencies.emptyDependencies();
    }

    private final VariableOrField lhs;
    private final boolean addressed;
  }

  private static CCompositeType getCanonicalFieldOwnerType(final CFieldReference fieldReference) {
    CType fieldOwnerType = fieldReference.getFieldOwner().getExpressionType().getCanonicalType();

    if (fieldOwnerType instanceof CPointerType) {
      fieldOwnerType = ((CPointerType) fieldOwnerType).getType();
    }
    assert fieldOwnerType instanceof CCompositeType
        : "Field owner should have composite type, but the field-owner type of expression "
            + fieldReference
            + " in "
            + fieldReference.getFileLocation()
            + " is "
            + fieldOwnerType
            + ", which is a "
            + fieldOwnerType.getClass().getSimpleName()
            + ".";
    final CCompositeType compositeType = (CCompositeType) fieldOwnerType;
    // Currently we don't pay attention to possible const and volatile modifiers
    if (compositeType.isConst() || compositeType.isVolatile()) {
      return new CCompositeType(
          false,
          false,
          compositeType.getKind(),
          compositeType.getMembers(),
          compositeType.getName(),
          compositeType.getOrigName());
    } else {
      return compositeType;
    }
  }

  public static VarFieldDependencies handleEdge(CFAEdge edge) throws UnrecognizedCCodeException {
    VarFieldDependencies result = VarFieldDependencies.emptyDependencies();

    switch (edge.getEdgeType()) {
      case AssumeEdge:
        {
          final CExpression exp = ((CAssumeEdge) edge).getExpression();
          result =
              result.withDependencies(
                  exp.accept(CollectingRHSVisitor.create(VariableOrField.unknown())));
          break;
        }

      case DeclarationEdge:
        {
          final CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
          if (!(decl instanceof CVariableDeclaration)) {
            break;
          }
          for (CExpressionAssignmentStatement init :
              CInitializers.convertToAssignments((CVariableDeclaration) decl, edge)) {
            Pair<VariableOrField, VarFieldDependencies> r =
                init.getLeftHandSide().accept(CollectingLHSVisitor.instance());
            result =
                result.withDependencies(
                    r.getSecond()
                        .withDependencies(
                            init.getRightHandSide()
                                .accept(CollectingRHSVisitor.create(r.getFirst()))));
          }
          break;
        }

      case StatementEdge:
        {
          final CStatement statement = ((CStatementEdge) edge).getStatement();
          // Heuristic: for external function calls
          // r = f(a); // r depends on f and a, BUT
          // f(a); // f and a are always relevant
          if (statement instanceof CAssignment) {
            final CAssignment assignment = (CAssignment) statement;
            final CRightHandSide rhs = assignment.getRightHandSide();
            final Pair<VariableOrField, VarFieldDependencies> r =
                assignment.getLeftHandSide().accept(CollectingLHSVisitor.instance());
            if (rhs instanceof CExpression || rhs instanceof CFunctionCallExpression) {
              result =
                  result.withDependencies(
                      r.getSecond()
                          .withDependencies(rhs.accept(CollectingRHSVisitor.create(r.getFirst()))));
            } else {
              throw new UnrecognizedCCodeException("Unhandled assignment", edge, assignment);
            }
          } else if (statement instanceof CFunctionCallStatement) {
            ((CFunctionCallStatement) statement)
                .getFunctionCallExpression()
                .accept(CollectingRHSVisitor.create(VariableOrField.unknown()));
          }
          break;
        }

      case FunctionCallEdge:
        {
          final CFunctionCallEdge call = (CFunctionCallEdge) edge;
          final List<CExpression> args = call.getArguments();
          final List<CParameterDeclaration> params = call.getSuccessor().getFunctionParameters();
          for (int i = 0; i < params.size(); i++) {
            result =
                result.withDependencies(
                    args.get(i)
                        .accept(
                            CollectingRHSVisitor.create(
                                VariableOrField.newVariable(params.get(i).getQualifiedName()))));
          }
          CFunctionCall statement = call.getSummaryEdge().getExpression();
          Optional<CVariableDeclaration> returnVar = call.getSuccessor().getReturnVariable();
          if (returnVar.isPresent()) {
            String scopedRetVal = returnVar.get().getQualifiedName();
            if (statement instanceof CFunctionCallAssignmentStatement) {
              final Pair<VariableOrField, VarFieldDependencies> r =
                  ((CFunctionCallAssignmentStatement) statement)
                      .getLeftHandSide()
                      .accept(CollectingLHSVisitor.instance());
              result =
                  result
                      .withDependencies(r.getSecond())
                      .withDependency(r.getFirst(), VariableOrField.newVariable(scopedRetVal));
            }
          }
          break;
        }

      case FunctionReturnEdge:
        {
          break;
        }

      case ReturnStatementEdge:
        {
          // this is the 'x' from 'return (x);
          // adding a new temporary FUNCTION_RETURN_VARIABLE, that is not global (-> false)
          final CReturnStatementEdge ret = (CReturnStatementEdge) edge;
          if (ret.asAssignment().isPresent()) {
            final Pair<VariableOrField, VarFieldDependencies> r =
                ret.asAssignment().get().getLeftHandSide().accept(CollectingLHSVisitor.instance());
            result =
                result.withDependencies(
                    r.getSecond()
                        .withDependencies(
                            ret.asAssignment()
                                .get()
                                .getRightHandSide()
                                .accept(CollectingRHSVisitor.create(r.getFirst()))));
          }
          break;
        }

      case BlankEdge:
      case CallToReturnEdge:
        {
          break;
        }

      default:
        {
          throw new UnrecognizedCCodeException("Unknown edge type: " + edge.getEdgeType(), edge);
        }
    }

    return result;
  }

  private static final String SCOPE_SEPARATOR = "::";
}

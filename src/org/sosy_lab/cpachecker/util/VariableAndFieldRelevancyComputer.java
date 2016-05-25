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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
public final class VariableAndFieldRelevancyComputer {
  /** Represents an approximation of a node in dependency graph i.e. variable, field or `top' (unknown location). */
  private static class VariableOrField implements Comparable<VariableOrField> {
    private static final class Unknown extends VariableOrField {
      private Unknown() {
      }

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
      private Variable(final @Nonnull String scopedName) {
        this.scopedName = scopedName;
      }

      public @Nonnull String getScopedName() {
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
        } else if (other instanceof Variable){
          return scopedName.compareTo(((Variable) other).scopedName);
        } else {
          throw new AssertionError("Should not happen: all cases are covered above");
        }
      }

      @Override
      public int hashCode() {
        return scopedName.hashCode();
      }

      private final @Nonnull String scopedName;
    }

    private static final class Field extends VariableOrField {
      private Field(final @Nonnull CCompositeType composite, final @Nonnull String name) {
        this.composite = composite;
        this.name = name;
      }

      public CCompositeType getCompositeType() {
        return composite;
      }

      public @Nonnull String getName() {
        return name;
      }

      @Override
      public @Nonnull String toString() {
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
          return  result != 0 ? result :
                  name.compareTo(o.name);
        } else {
          throw new AssertionError("Should not happen: all cases are covered above");
        }
      }

      @Override
      public int hashCode() {
        final int prime = 67;
        return prime * composite.hashCode() + name.hashCode();
      }

      private final @Nonnull CCompositeType composite;
      private final @Nonnull String name;
    }

    private VariableOrField() {
    }

    public static @Nonnull Variable newVariable(final @Nonnull String scopedName) {
      return new Variable(scopedName);
    }

    public static @Nonnull Field newField(final @Nonnull CCompositeType composite, final @Nonnull String name) {
      return new Field(composite, name);
    }

    public static @Nonnull Unknown unknown() {
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

    public @Nonnull Variable asVariable() {
      if (this instanceof Variable) {
        return (Variable) this;
      } else {
        throw new ClassCastException("Tried to match " + this.getClass().getName() + " with " +
                                     Variable.class.getName());
      }
    }

    public @Nonnull Field asField() {
      if (this instanceof Field) {
        return (Field) this;
      } else {
        throw new ClassCastException("Tried to match " + this.getClass().getName() + " with " + Field.class.getName());
      }
    }

    @Override
    public int compareTo(final VariableOrField other) {
      throw new AssertionError("Should not happen: comparison should always be called on an object of a subclass");
    }
  }

  private static final class ComparableCompositeType implements Comparable<ComparableCompositeType> {
    private ComparableCompositeType(final @Nonnull CCompositeType type) {
      this.type = type;
    }

    @Override
    public int compareTo(final ComparableCompositeType other) {
      if (other == this) {
        return 0;
      } else {
        return type.getQualifiedName().compareTo(other.type.getQualifiedName());
      }
    }

    public static ComparableCompositeType of(final CCompositeType type) {
      return new ComparableCompositeType(type);
    }

    public CCompositeType compositeType() {
      return type;
    }

    private final @Nonnull CCompositeType type;
  }

  public static final class VarFieldDependencies {
    private static <T> PersistentList<T> safeWithAll(final @Nullable PersistentList<T> l1,
                                                     final @Nonnull PersistentList<T> l2) {
      if (l1 == null) {
        return l2;
      } else {
        return l1.withAll(l2);
      }
    }

    private VarFieldDependencies(@Nonnull PersistentSortedMap<String, Boolean> relevantVariables,
       @Nonnull PersistentSortedMap<ComparableCompositeType, PersistentList<String>> relevantFields,
       @Nonnull PersistentSortedMap<String, Boolean> addressedVariables,
       @Nonnull PersistentSortedMap<VariableOrField, PersistentList<VariableOrField>> dependencies,
       @Nonnull PersistentList<VarFieldDependencies> pendingMerges,
       final int size,
       final boolean forceSquash) {
         if (forceSquash || pendingMerges.size() > MAX_PENDING_MERGES) {
           for (VarFieldDependencies deps : pendingMerges) {
             for (final Map.Entry<String, Boolean> e : deps.relevantVariables.entrySet()) {
               relevantVariables = relevantVariables.putAndCopy(e.getKey(), e.getValue());
             }
             for (final Map.Entry<ComparableCompositeType, PersistentList<String>> e : deps.relevantFields.entrySet()) {
               relevantFields = relevantFields.putAndCopy(e.getKey(), safeWithAll(relevantFields.get(e.getKey()),
                                                                                  e.getValue()));
             }
             for (final Map.Entry<String, Boolean> e : deps.addressedVariables.entrySet()) {
               addressedVariables = addressedVariables.putAndCopy(e.getKey(), e.getValue());
             }
             for (final Map.Entry<VariableOrField, PersistentList<VariableOrField>> e : deps.dependencies.entrySet()) {
               dependencies = dependencies.putAndCopy(e.getKey(), safeWithAll(dependencies.get(e.getKey()),
                                                                              e.getValue()));
             }
           }
           pendingMerges = PersistentLinkedList.of();
         }
         this.relevantVariables = relevantVariables;
         this.relevantFields = relevantFields;
         this.addressedVariables = addressedVariables;
         this.dependencies = dependencies;
         this.pendingMerges = pendingMerges;
         this.size = size;
     }

    private VarFieldDependencies(final @Nonnull PersistentSortedMap<String, Boolean> relevantVariables,
        final @Nonnull PersistentSortedMap<ComparableCompositeType, PersistentList<String>> relevantFields,
        final @Nonnull PersistentSortedMap<String, Boolean> addressedVariables,
        final @Nonnull PersistentSortedMap<VariableOrField, PersistentList<VariableOrField>> dependencies,
        final @Nonnull PersistentList<VarFieldDependencies> pendingMerges,
        final int size) {
        this(relevantVariables, relevantFields, addressedVariables, dependencies, pendingMerges, size, false);
    }

     public static @Nonnull VarFieldDependencies emptyDependencies() {
       return EMPTY_DEPENDENCIES;
     }

     private static <T> PersistentList<T> safeWith(final @Nullable PersistentList<T> l, T e) {
       if (l == null) {
         return PersistentLinkedList.of(e);
       } else {
         return l.with(e);
       }
     }

     public @Nonnull VarFieldDependencies withDependency(final @Nonnull VariableOrField lhs,
                                                         final @Nonnull VariableOrField rhs) {
       if (!lhs.isUnknown()) {
         return new VarFieldDependencies(relevantVariables, relevantFields, addressedVariables,
                                         dependencies.putAndCopy(lhs, safeWith(dependencies.get(lhs), rhs)),
                                         pendingMerges,
                                         size + 1);
       } else {
         if (rhs.isVariable()) {
           return new VarFieldDependencies(relevantVariables.putAndCopy(rhs.asVariable().getScopedName(),
                                                                        DUMMY_PRESENT),
                                           relevantFields, addressedVariables, dependencies, pendingMerges,
                                           size + 1);
         } else if (rhs.isField()) {
           final VariableOrField.Field field = rhs.asField();
           final ComparableCompositeType key = ComparableCompositeType.of(field.getCompositeType());
           return new VarFieldDependencies(relevantVariables,
                                           relevantFields.putAndCopy(key, safeWith(relevantFields.get(key),
                                                                                   field.getName())),
                                           addressedVariables, dependencies, pendingMerges,
                                           size + 1);
         } else if (rhs.isUnknown()) {
           throw new IllegalArgumentException("Can't handle dependency on Unknown");
         } else {
           throw new AssertionError("Should be unreachable: all possible cases already handled");
         }
       }
     }

     public @Nonnull VarFieldDependencies withAddressedVariable(final @Nonnull VariableOrField.Variable variable) {
       return new VarFieldDependencies(relevantVariables, relevantFields,
                                       addressedVariables.putAndCopy(variable.getScopedName(), DUMMY_PRESENT),
                                       dependencies, pendingMerges,
                                       size + 1);
     }

     public @Nonnull VarFieldDependencies withDependencies(final @Nonnull VarFieldDependencies other) {
       if (size == 0) {
         return other;
       }
       if (other.size == 0) {
         return this;
       }
       if (size >= other.size) {
         return new VarFieldDependencies(relevantVariables, relevantFields, addressedVariables, dependencies,
                                         pendingMerges.withAll(other.pendingMerges).with(other),
                                         size + other.size);
       } else {
         return new VarFieldDependencies(other.relevantVariables, other.relevantFields, other.addressedVariables,
                                         other.dependencies,
                                         other.pendingMerges.withAll(pendingMerges).with(this),
                                         size + other.size);
       }
     }

     private void ensureSquashed() {
       if (squashed == null) {
         squashed = new VarFieldDependencies(relevantVariables,
                                             relevantFields, addressedVariables, dependencies, pendingMerges,
                                             size, true);
       }
     }

     public ImmutableSet<String> computeAddressedVariables() {
       ensureSquashed();
       return ImmutableSet.copyOf(squashed.addressedVariables.keySet());
     }

     public Pair<ImmutableSet<String>, ImmutableMultimap<CCompositeType, String>> computeRelevantVariablesAndFields() {
       ensureSquashed();
       Queue<VariableOrField> queue = new ArrayDeque<>(squashed.relevantVariables.size() +
                                                       squashed.relevantFields.size());
       Set<String> currentRelevantVariables = new HashSet<>();
       Multimap<CCompositeType, String> currentRelevantFields = LinkedHashMultimap.create();
       for (final String relevantVariable : squashed.relevantVariables.keySet()) {
         queue.add(VariableOrField.newVariable(relevantVariable));
         currentRelevantVariables.add(relevantVariable);
       }
       for (final Map.Entry<ComparableCompositeType, PersistentList<String>> relevantField :
            squashed.relevantFields.entrySet()) {
         for (final String s : relevantField.getValue()) {
           queue.add(VariableOrField.newField(relevantField.getKey().compositeType(), s));
           currentRelevantFields.put(relevantField.getKey().compositeType(), s);
         }
       }
       while (!queue.isEmpty()) {
         final VariableOrField relevantVariableOrField = queue.poll();
         final PersistentList<VariableOrField> variableOrFieldList = squashed.dependencies.get(relevantVariableOrField);
         if (variableOrFieldList != null) {
           for (VariableOrField variableOrField : variableOrFieldList) {
             assert variableOrField.isVariable() || variableOrField.isField() :
               "Match failure: neither variable nor field!";
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
       }

       return Pair.of(ImmutableSet.copyOf(currentRelevantVariables), ImmutableMultimap.copyOf(currentRelevantFields));
     }

     private final PersistentSortedMap<String, Boolean> relevantVariables;
     private final PersistentSortedMap<ComparableCompositeType, PersistentList<String>> relevantFields;
     private final PersistentSortedMap<String, Boolean> addressedVariables;
     private final PersistentSortedMap<VariableOrField, PersistentList<VariableOrField>> dependencies;
     private final PersistentList<VarFieldDependencies> pendingMerges;
     private final int size;
     private VarFieldDependencies squashed = null;

     private static final Boolean DUMMY_PRESENT = true;
     private static final int MAX_PENDING_MERGES = 100;
     private static final VarFieldDependencies EMPTY_DEPENDENCIES =
         new VarFieldDependencies(PathCopyingPersistentTreeMap.<String, Boolean>of(),
                                  PathCopyingPersistentTreeMap.<ComparableCompositeType, PersistentList<String>>of(),
                                  PathCopyingPersistentTreeMap.<String, Boolean>of(),
                                  PathCopyingPersistentTreeMap.<VariableOrField, PersistentList<VariableOrField>>of(),
                                  PersistentLinkedList.<VarFieldDependencies>of(),
                                  0);
  }

  private static final class CollectingLHSVisitor
    extends DefaultCExpressionVisitor<Pair<VariableOrField, VarFieldDependencies>, RuntimeException> {

    private CollectingLHSVisitor () {

    }

    public static CollectingLHSVisitor instance() {
      return INSTANCE;
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CArraySubscriptExpression e) {
      final Pair<VariableOrField, VarFieldDependencies> r = e.getArrayExpression().accept(this);
      return Pair.of(r.getFirst(), r.getSecond().withDependencies(
                                                         e.getSubscriptExpression()
                                                          .accept(CollectingRHSVisitor.create(r.getFirst()))));
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CFieldReference e) {
      final VariableOrField result = VariableOrField.newField(getCanonicalFieldOwnerType(e), e.getFieldName());
      return Pair.of(result, e.getFieldOwner().accept(CollectingRHSVisitor.create(result)));
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CPointerExpression e) {
      return Pair.of(VariableOrField.unknown(),
                     e.getOperand().accept(CollectingRHSVisitor.create(VariableOrField.unknown())));
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies>visit(final CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Pair<VariableOrField, VarFieldDependencies> visit(final CIdExpression e) {
      return Pair.of(VariableOrField.newVariable(e.getDeclaration().getQualifiedName()),
                     VarFieldDependencies.emptyDependencies());
    }

    @Override
    protected Pair<VariableOrField, VarFieldDependencies> visitDefault(final CExpression e)  {
      throw new AssertionError("The expression should not occur in the left hand side");
    }

    private static final CollectingLHSVisitor INSTANCE = new CollectingLHSVisitor();
  }

  private static final class CollectingRHSVisitor
    extends DefaultCExpressionVisitor<VarFieldDependencies, RuntimeException>
    implements CRightHandSideVisitor<VarFieldDependencies, RuntimeException> {

    private CollectingRHSVisitor(final @Nonnull VariableOrField lhs, final boolean addressed) {
      this.lhs = lhs;
      this.addressed = addressed;
    }

    public static CollectingRHSVisitor create(final @Nonnull VariableOrField lhs) {
      return new CollectingRHSVisitor(lhs, false);
    }

    private CollectingRHSVisitor createAddressed() {
      return new CollectingRHSVisitor(lhs, true);
    }

    @Override
    public VarFieldDependencies visit(final CArraySubscriptExpression e) {
      return e.getSubscriptExpression().accept(this).withDependencies(e.getArrayExpression().accept(this));
    }

    @Override
    public VarFieldDependencies visit(final CFieldReference e) {
      return e.getFieldOwner().accept(this).withDependency(lhs,
                                                           VariableOrField.newField(getCanonicalFieldOwnerType(e),
                                                                                    e.getFieldName()));
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
      final VariableOrField.Variable variable = VariableOrField.newVariable(decl != null ? decl.getQualifiedName() :
                                                                                           e.getName());
      final VarFieldDependencies result = VarFieldDependencies.emptyDependencies().withDependency(lhs, variable);
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
    protected VarFieldDependencies visitDefault(final CExpression e)  {
      return VarFieldDependencies.emptyDependencies();
    }

    private final @Nonnull VariableOrField lhs;
    private final boolean addressed;
  }

  private static CCompositeType getCanonicalFieldOwnerType(final @Nonnull CFieldReference fieldReference) {
    CType fieldOwnerType = fieldReference.getFieldOwner().getExpressionType().getCanonicalType();

    if (fieldOwnerType instanceof CPointerType) {
      fieldOwnerType = ((CPointerType) fieldOwnerType).getType();
    }
    assert fieldOwnerType instanceof CCompositeType
        : "Field owner should have composite type, but the field-owner type of expression " + fieldReference
          + " in " + fieldReference.getFileLocation()
          + " is " + fieldOwnerType + ", which is a " + fieldOwnerType.getClass().getSimpleName() + ".";
    final CCompositeType compositeType = (CCompositeType) fieldOwnerType;
    // Currently we don't pay attention to possible const and volatile modifiers
    if (compositeType.isConst() || compositeType.isVolatile()) {
      return new CCompositeType(false,
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

    case AssumeEdge: {
      final CExpression exp = ((CAssumeEdge) edge).getExpression();
      result = result.withDependencies(exp.accept(CollectingRHSVisitor.create(VariableOrField.unknown())));
      break;
    }

    case DeclarationEdge: {
      final CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
      if (!(decl instanceof CVariableDeclaration)) {
        break;
      }
      for (CExpressionAssignmentStatement init :
           CInitializers.convertToAssignments((CVariableDeclaration) decl, edge)) {
        Pair<VariableOrField, VarFieldDependencies> r = init.getLeftHandSide()
                                                            .accept(CollectingLHSVisitor.instance());
        result = result.withDependencies(r.getSecond().withDependencies(init.getRightHandSide().accept(
                                                                          CollectingRHSVisitor.create(r.getFirst()))));
      }
      break;
    }

    case StatementEdge: {
      final CStatement statement = ((CStatementEdge) edge).getStatement();
      // Heuristic: for external function calls
      // r = f(a); // r depends on f and a, BUT
      // f(a); // f and a are always relevant
      if (statement instanceof CAssignment) {
        final CAssignment assignment = (CAssignment) statement;
        final CRightHandSide rhs = assignment.getRightHandSide();
        final Pair<VariableOrField, VarFieldDependencies> r = assignment.getLeftHandSide().accept(
                                                                                      CollectingLHSVisitor.instance());
        if (rhs instanceof CExpression || rhs instanceof CFunctionCallExpression) {
          result = result.withDependencies(r.getSecond().withDependencies(rhs.accept(CollectingRHSVisitor
                                                                                              .create(r.getFirst()))));
        } else {
          throw new UnrecognizedCCodeException("Unhandled assignment", edge, assignment);
        }
      } else if (statement instanceof CFunctionCallStatement) {
        ((CFunctionCallStatement) statement).getFunctionCallExpression().accept(CollectingRHSVisitor.create(
                                                                                           VariableOrField.unknown()));
      }
      break;
    }

    case FunctionCallEdge: {
      final CFunctionCallEdge call = (CFunctionCallEdge) edge;
      final List<CExpression> args = call.getArguments();
      final List<CParameterDeclaration> params = call.getSuccessor().getFunctionParameters();
      for (int i = 0; i < params.size(); i++) {
        result = result.withDependencies(args.get(i).accept(
            CollectingRHSVisitor.create(VariableOrField.newVariable(params.get(i).getQualifiedName()))));
      }
      CFunctionCall statement = call.getSummaryEdge().getExpression();
      Optional<CVariableDeclaration> returnVar = call.getSuccessor().getReturnVariable();
      if (returnVar.isPresent()) {
        String scopedRetVal = returnVar.get().getQualifiedName();
        if (statement instanceof CFunctionCallAssignmentStatement) {
          final Pair<VariableOrField, VarFieldDependencies> r =
                                                      ((CFunctionCallAssignmentStatement) statement)
                                                        .getLeftHandSide().accept(CollectingLHSVisitor.instance());
          result = result.withDependencies(r.getSecond()).withDependency(r.getFirst(),
                                                                         VariableOrField.newVariable(scopedRetVal));
        }
      }
      break;
    }

    case FunctionReturnEdge: {
      break;
    }

    case ReturnStatementEdge: {
      // this is the 'x' from 'return (x);
      // adding a new temporary FUNCTION_RETURN_VARIABLE, that is not global (-> false)
      final CReturnStatementEdge ret = (CReturnStatementEdge) edge;
      if (ret.asAssignment().isPresent()) {
        final Pair<VariableOrField, VarFieldDependencies> r = ret.asAssignment()
                                                                 .get().getLeftHandSide()
                                                                 .accept(CollectingLHSVisitor.instance());
        result = result.withDependencies(r.getSecond().withDependencies(ret.asAssignment()
                                                                           .get().getRightHandSide()
                                                                           .accept(CollectingRHSVisitor.create(
                                                                                                       r.getFirst()))));
      }
      break;
    }

    case BlankEdge:
    case CallToReturnEdge: {
      break;
    }

    default: {
      throw new UnrecognizedCCodeException("Unknown edge type: " + edge.getEdgeType(), edge);
    }
    }

    return result;
  }

  private static final String SCOPE_SEPARATOR = "::";
}

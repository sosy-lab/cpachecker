// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.functionpointer;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class FunctionPointerTransferRelationTest {

  private FunctionPointerTransferRelation relation;
  private final Precision precision = SingletonPrecision.getInstance();
  private final FunctionPointerState initialState = FunctionPointerState.createEmptyState();

  private final CElaboratedType struct =
      new CElaboratedType(
          CTypeQualifiers.CONST,
          ComplexTypeKind.STRUCT,
          "allocator",
          "allocator",
          new CCompositeType(
              CTypeQualifiers.NONE,
              ComplexTypeKind.STRUCT,
              ImmutableList.of(
                  new CCompositeTypeMemberDeclaration(
                      new CPointerType(CTypeQualifiers.NONE, CVoidType.VOID), "die")),
              "allocator",
              "allocator"));

  private final CInitializerExpression initializer =
      new CInitializerExpression(
          FileLocation.DUMMY,
          new CCastExpression(
              FileLocation.DUMMY,
              new CPointerType(CTypeQualifiers.NONE, CVoidType.VOID),
              new CIntegerLiteralExpression(
                  FileLocation.DUMMY, CNumericTypes.SIGNED_INT, BigInteger.ZERO)));

  private final CVariableDeclaration voidPointerDecl =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          struct,
          "stdlib_allocator",
          "stdlib_allocator",
          "stdlib_allocator",
          new CInitializerList(FileLocation.DUMMY, ImmutableList.of(initializer)));

  private final CIdExpression libAllocator =
      new CIdExpression(FileLocation.DUMMY, struct, "stdlib_allocator", voidPointerDecl);

  private final CTypedefType sizeT =
      new CTypedefType(
          CTypeQualifiers.NONE,
          "size_t",
          new CTypedefType(CTypeQualifiers.NONE, "size_t", CNumericTypes.UNSIGNED_LONG_INT));

  private final CFunctionTypeWithNames functionType =
      new CFunctionTypeWithNames(
          CVoidType.VOID,
          ImmutableList.of(new CParameterDeclaration(FileLocation.DUMMY, sizeT, "")),
          false);

  private final CFieldReference fieldReference =
      new CFieldReference(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, functionType),
          "die",
          libAllocator,
          false);

  private final CIntegerLiteralExpression zeroLiteral =
      new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.SIGNED_INT, BigInteger.ZERO);

  private final CIntegerLiteralExpression oneLiteral =
      new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.SIGNED_INT, BigInteger.ONE);

  @Before
  public void init() throws InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();
    LogManager manager = LogManager.createNullLogManager();
    relation = new FunctionPointerTransferRelation(manager, config);
  }

  private CAssumeEdge createAssumeEdge(
      BinaryOperator operator, boolean truthAssumption, CExpression lhs, CExpression rhs) {
    return new CAssumeEdge(
        "stdlib_allocator.die == 0",
        FileLocation.DUMMY,
        CFANode.newDummyCFANode("main"),
        CFANode.newDummyCFANode("main"),
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.SIGNED_INT,
            new CPointerType(CTypeQualifiers.NONE, functionType),
            lhs,
            rhs,
            operator),
        truthAssumption);
  }

  @Test
  public void equalsThenFieldReferenceConstantTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, true, fieldReference, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void equalsElseFieldReferenceConstantTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, false, fieldReference, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsThenFieldReferenceConstantTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, true, fieldReference, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsElseFieldReferenceConstantTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, false, fieldReference, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void lessThenFieldReferenceConstantTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.LESS_EQUAL, true, fieldReference, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void lessElseFieldReferenceConstantTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.LESS_EQUAL, false, fieldReference, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void equalsThenBranchSameConstantsTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, true, zeroLiteral, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void equalsElseBranchSameConstantsTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, false, zeroLiteral, zeroLiteral));
    assertThat(successors).isEmpty();
  }

  @Test
  public void equalsThenBranchDifferentConstantsTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, true, zeroLiteral, oneLiteral));
    assertThat(successors).isEmpty();
  }

  @Test
  public void equalsElseBranchDifferentConstantsTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, false, oneLiteral, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsThenBranchSameConstantsTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, true, zeroLiteral, zeroLiteral));
    assertThat(successors).isEmpty();
  }

  @Test
  public void unequalsElseBranchSameConstantsTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, false, zeroLiteral, zeroLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsThenBranchDifferentConstantsTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, true, zeroLiteral, oneLiteral));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsElseBranchDifferentConstantsTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, false, oneLiteral, zeroLiteral));
    assertThat(successors).isEmpty();
  }

  @Test
  public void equalsThenConstantFieldReferenceTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, true, oneLiteral, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void equalsElseConstantFieldReferenceTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, false, oneLiteral, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsThenConstantFieldReferenceTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, true, oneLiteral, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsElseConstantFieldReferenceTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, false, oneLiteral, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void lessThenConstantFieldReferenceTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.LESS_EQUAL, true, oneLiteral, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void lessElseConstantFieldReferenceTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.LESS_EQUAL, false, oneLiteral, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void equalsThenSameUnknownFieldReferencesTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, true, fieldReference, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsThenSameUnknownFieldReferencesTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, true, fieldReference, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void equalsElseSameUnknownFieldReferencesTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.EQUALS, false, fieldReference, fieldReference));
    assertThat(successors).hasSize(1);
  }

  @Test
  public void unequalsElseSameUnknownFieldReferencesTest() throws CPATransferException {
    Collection<? extends AbstractState> successors =
        relation.getAbstractSuccessorsForEdge(
            initialState,
            precision,
            createAssumeEdge(BinaryOperator.NOT_EQUALS, false, fieldReference, fieldReference));
    assertThat(successors).hasSize(1);
  }
}

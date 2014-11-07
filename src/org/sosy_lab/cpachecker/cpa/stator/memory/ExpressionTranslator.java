package org.sosy_lab.cpachecker.cpa.stator.memory;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import java.util.*;
import java.util.logging.Level;

public final class ExpressionTranslator {
  private final AliasState prevAbstractValue; // doesn't it hold an address?
  private final MachineModel machineModel;
  private final CFAEdge edge;
  private final LogManager logger;

  @SuppressWarnings("unused")
  public ExpressionTranslator(
      AliasState pPrevAbstractValue,
      MachineModel pMachineModel,
      CFAEdge pEdge,
      Precision pPrecision,
      CFANode pFromNode,
      LogManager pLogger
  ) {
    // Create-assignment-on shouldn't propagate.
    prevAbstractValue = pPrevAbstractValue.withCreateAssignmentOn(AbstractMemoryAddress.BOTTOM);
    machineModel = pMachineModel;
    edge = pEdge;
    logger = pLogger;
  }

  public Collection<? extends AliasState> getAbstractSuccessors() {
    logger.log(Level.FINE,
        "# Processing edge: ", edge.getRawStatement());

    if (edge instanceof CStatementEdge) {
      return Collections.singleton(handleStatement());
    } else if (edge instanceof CAssumeEdge) {

      // TODO: we should be able to handle things like
      // if (x == &a)
      return Collections.singleton(prevAbstractValue);
    } else if (edge instanceof CFunctionCallEdge) {
      throw new UnsupportedOperationException("CFunctionCallEdge is not supported yet!");
    } else if (edge instanceof CReturnStatementEdge) {

      // For now we just ignore return statements.
      return Collections.singleton(prevAbstractValue);
    } else if (edge instanceof CFunctionReturnEdge) {
      throw new UnsupportedOperationException("CFunctionReturnEdge is not supported yet!");
    } else if (edge instanceof CDeclarationEdge) {

      return Collections.singleton(handleDeclaration());
    } else if (edge instanceof BlankEdge) {
      return Collections.singleton(prevAbstractValue);
    } else if (edge instanceof MultiEdge) {
      throw new UnsupportedOperationException("MultiEdge is not supported!");
    } else if (edge instanceof CFunctionSummaryEdge) {
      throw new UnsupportedOperationException("CFunctionSummaryEdge is not supported!");
    } else {
      throw new IllegalArgumentException("Unknown edge type!");
    }
  }

  private AliasState handleDeclaration() {
    AliasState outState = prevAbstractValue;

    CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;

    if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return outState;
    }

    // We're only interested in variable declarations, skipping functions
    // for now.
    CVariableDeclaration declaration = (CVariableDeclaration)
        declarationEdge.getDeclaration();

    outState = outState.setAddress(
        declaration.getQualifiedName(),
        MemorySegment.ofDeclaration(declaration)
    );

    if (!(declaration.getType().getCanonicalType() instanceof CPointerType)) {
      logger.log(Level.FINE, "Not a pointer declaration, skipping");
      return outState;
    }

    if (!(declaration.getInitializer() instanceof CInitializerExpression)) {
      logger.log(Level.FINE, "Not expression initializer, skipping");
      return outState;
    }

    CInitializerExpression expression = (CInitializerExpression)
        declaration.getInitializer();
    CExpression initializer = expression.getExpression();

    String varName = declaration.getQualifiedName();

    MemorySegment address = prevAbstractValue.getAddress(varName);

    AbstractMemoryAddress varAdr =  AbstractMemoryAddress.ofAddresses(
        Collections.singleton(address));

    AbstractMemoryAddress value = getPointerExpressionValue(initializer);
    return transformByAssignment(outState, varAdr, value);
  }

  private AliasState handleStatement() {
    CStatementEdge statementEdge = (CStatementEdge) edge;
    CStatement stmt = statementEdge.getRawAST().get();
    logger.log(Level.FINE, "# Handling statement ", stmt);
    if (stmt instanceof CExpressionStatement) {

      // TODO
      logger.log(Level.WARNING, "Aliasing sees CExpression statement, skipping");
      return prevAbstractValue;
    } else if (stmt instanceof CExpressionAssignmentStatement) {

      CExpressionAssignmentStatement assignmentStatement = ((CExpressionAssignmentStatement) stmt);

      CExpression lvalueExpr = assignmentStatement.getLeftHandSide();
      CExpression rvalueExpr = ((CExpressionAssignmentStatement) stmt).getRightHandSide();

      if (lvalueExpr instanceof CPointerExpression) {
        CPointerExpression ptrExpression = (CPointerExpression) lvalueExpr;
        if (ptrExpression.getOperand() instanceof CIdExpression) {
          AbstractMemoryAddress aliasFrom = AbstractMemoryAddress.ofAddresses(
              Collections.singleton(
                  idExpressionToAddress((CIdExpression) ptrExpression.getOperand())));
          return prevAbstractValue.withCreateAssignmentOn(aliasFrom);
        }
      }

      AbstractMemoryAddress lValue = getLValue(lvalueExpr);

      // We do not care about writing into things which are not pointers.
      // TODO: is it the check we want to perform?..
      if (!lValue.isPointerType()) {
        return prevAbstractValue;
      }

      AbstractMemoryAddress rValue = getPointerExpressionValue(rvalueExpr);

      return transformByAssignment(prevAbstractValue, lValue, rValue);

    } else if (stmt instanceof CFunctionCallStatement) {
      // TODO. For now ignore.
      return prevAbstractValue;

    } else if (stmt instanceof CFunctionCallAssignmentStatement) {
      throw new UnsupportedOperationException(
          "CFunctionCallAssignmentStatement is not supported yet!");
    } else {
      throw new IllegalArgumentException("Unknown statement edge type!");

    }
  }

  /**
   * Update the abstract value to reflect the effect of the assignment statement.
   *
   * @param lValue L-value of the assignment. Set of possible addresses we can assign to.
   * @param rValue R-value of the assignment, what are we writing into the memory addresses.
   * @return Updated abstract value.
   */
  public AliasState transformByAssignment(
      AliasState outState,
      AbstractMemoryAddress lValue,
      AbstractMemoryAddress rValue) {

    if (lValue == AbstractMemoryAddress.TOP) {
      return AliasState.TOP;
    }

    AliasState newState = outState;

    // The assignment is certain, unique l-value.
    if (lValue.size() == 1) {
      MemorySegment address = lValue.iterator().next();
      return newState.setBlockValue(address, rValue);
    }

    // Otherwise: multiple possible assignments.
    for (MemorySegment address : lValue) {
      newState = newState.addValueToBlock(address, rValue);
    }

    return newState;
  }

  /**
   * Process left-hand-side of the assignment expression and return the
   * memory address.
   * Currently only assignments to variables and assignments to pointers are
   * handled.
   *
   * @param expr Expression applied to the l-value.
   * @return Resulting l-value after the application of the expression.
   *
   * NOTE: this is general enough to be there for all analysis.
   */
  private AbstractMemoryAddress getLValue(CExpression expr) {
    if (expr instanceof CIdExpression) {

      // Base case: get the identifier for the given variable.
      return AbstractMemoryAddress.ofAddresses(
       Collections.singleton(idExpressionToAddress((CIdExpression)expr))
      );

    } else if (expr instanceof CPointerExpression) {
      CPointerExpression pointerExpression = (CPointerExpression) expr;

      return getPointerExpressionValue(pointerExpression);
    } else if (expr instanceof CArraySubscriptExpression) {

      // TODO: no reason not to support arrays, copy the code over
      // TODO: from the new version of the Marek's prototype.
      throw new UnsupportedOperationException("Array write not implemented yet.");
    } else if (expr instanceof CFieldReference) {

      throw new UnsupportedOperationException("CFieldReference NOT IMPLEMENTED YET!");
    }

    throw new IllegalArgumentException(
        "Illegal operation on l-value of the assignment.");
  }

  /**
   * From the C semantics we assume that the returned expression must
   * contain an address.
   *
   * @param expr Expression applied to the r-value
   * @return Resulting r-value
   */
  private AbstractMemoryAddress getPointerExpressionValue(CExpression expr) {

    if (expr instanceof CBinaryExpression) {
      return transformValueByBinaryExpression((CBinaryExpression) expr);

    } else if (expr instanceof CUnaryExpression) {
      return transformValueByUnaryExpression((CUnaryExpression) expr);

    } else if (expr instanceof CComplexCastExpression) {
      throw new UnsupportedOperationException("CComplexCastExpression NOT IMPLEMENTED YET!");

    } else if (expr instanceof CCastExpression) {

      // TODO: currently let's not handle casts.
      return getPointerExpressionValue(((CCastExpression)expr).getOperand());

    } else if (expr instanceof CArraySubscriptExpression) {

      // TODO: port things over from the other prototype.
      throw new UnsupportedOperationException("CArraySubscriptExpression NOT IMPLEMENTED YET!");

    } else if (expr instanceof CPointerExpression) {

      // If it's a pointer expression, the value inside must be an address.
      AbstractMemoryAddress lValue = getPointerExpressionValue(
          ((CPointerExpression)expr).getOperand());

      if (lValue == AbstractMemoryAddress.TOP) {
        return AbstractMemoryAddress.TOP;
      }

      // Sum of all things referenced inside.
      AbstractMemoryAddress result = AbstractMemoryAddress.BOTTOM;
      for (MemorySegment adr : lValue) {
        result = result.join(prevAbstractValue.getValuesStored(adr));
      }
      return result;

    } else if (expr instanceof CFieldReference) {
      throw new UnsupportedOperationException("CFieldReference NOT IMPLEMENTED YET!");

    } else if (expr instanceof CIdExpression) {
      MemorySegment segment = idExpressionToAddress((CIdExpression) expr);
      return prevAbstractValue.getValuesStored(segment);

    } else if (expr instanceof CTypeIdExpression) {
      throw new UnsupportedOperationException("CTypeIdExpression NOT IMPLEMENTED YET!");

    } else if (expr instanceof CCharLiteralExpression) {
      throw new UnsupportedOperationException("CCharLiteralExpression NOT IMPLEMENTED YET!");

    } else if (expr instanceof CIntegerLiteralExpression) {

      // Can't convert integers back to addresses.
      return AbstractMemoryAddress.TOP;

    } else if (expr instanceof CFloatLiteralExpression) {
      throw new UnsupportedOperationException("CFloatLiteralExpression NOT IMPLEMENTED YET!");

    } else if (expr instanceof CStringLiteralExpression) {
      throw new UnsupportedOperationException("CStringLiteralExpression NOT IMPLEMENTED YET!");

    } else if (expr instanceof CImaginaryLiteralExpression) {
      throw new UnsupportedOperationException("CImaginaryLiteralExpression NOT IMPLEMENTED YET!");

    } else {
      throw new IllegalArgumentException("Unknown expression type!");
    }
  }

  /**
   * Get address for the CIdExpression.
   */
  private MemorySegment idExpressionToAddress(CIdExpression var) {
    String qualifiedIdentifier = var.getDeclaration().getQualifiedName();
    return prevAbstractValue.getAddress(qualifiedIdentifier);
  }

  private AbstractMemoryAddress transformValueByBinaryExpression(CBinaryExpression expr)
      throws UnsupportedOperationException {

    CExpression arg1 = expr.getOperand1();
    CExpression arg2 = expr.getOperand2();
    CType commonType;

    // Adding implicit casts.
    if (!arg1.getExpressionType().equals(arg2.getExpressionType())
        && !(arg1.getExpressionType() instanceof CPointerType)
        && !(arg2.getExpressionType() instanceof CPointerType)) {

      commonType = computeCommonType(arg1.getExpressionType(), arg2.getExpressionType(), machineModel);
      arg1 = new CCastExpression(arg1.getFileLocation(), commonType, arg1);
      arg2 = new CCastExpression(arg2.getFileLocation(), commonType, arg2);
    }

    if (arg1.getExpressionType() instanceof CSimpleType
        && arg2.getExpressionType() instanceof CSimpleType) {

      logger.log(Level.WARNING,
          "Trying to convert simple type to pointer, returning TOP");
      return AbstractMemoryAddress.TOP;
    }

    if (!(arg1.getExpressionType() instanceof CPointerType)
          && arg2.getExpressionType() instanceof CPointerType) {
      CExpression tmp = arg1;
      arg1 = arg2;
      arg2 = tmp;
    }

    if (arg1.getExpressionType() instanceof CPointerType) {
      if (arg2.getExpressionType() instanceof CPointerType) {

        logger.log(Level.FINE, "# Pointer arithmetic not supported, skipping");
        return AbstractMemoryAddress.TOP;

      } else if (arg2.getExpressionType() instanceof CSimpleType) {

        // TODO: query the other analysis for the value of the numerical type
        // TODO: in order to give the good output.
        logger.log(Level.WARNING, "Pointer seek, skipping");
        return AbstractMemoryAddress.TOP;
      } else {
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET!");
      }
    }
    throw new UnsupportedOperationException("NOT IMPLEMENTED YET!");
  }

  private AbstractMemoryAddress transformValueByUnaryExpression(CUnaryExpression expr)
      throws UnsupportedOperationException {

    switch (expr.getOperator()) {
      case AMPER:

        // Semantics of the ampersand is equivalent to the semantics of the
        // left-hand-side of the expression.
        return getLValue(expr.getOperand());
      case MINUS:
      case TILDE:
      case SIZEOF:
      case ALIGNOF:
      default:
        throw new UnsupportedOperationException("UnaryExpression Unknown unary operator type!");
    }
  }

  public static CType computeCommonType(
      final CType type1,
      final CType type2,
      MachineModel machineModel) {
    if (type1.equals(type2))
      return type1;
    if (type1 instanceof CSimpleType) {
      if (type2 instanceof CSimpleType) {
        int sz1 = computeSizeOf(type1, machineModel);
        int sz2 = computeSizeOf(type2, machineModel);
        if (sz1 > sz2)
          return type1;
        if (sz1 < sz2)
          return type2;
        if (((CSimpleType) type1).isUnsigned())
          return type1;
        return type2;
      } else if (type2 instanceof CPointerType) {
        return type2;
      } else {
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET!");
      }
    } else if (type1 instanceof CPointerType) {
      if (type2 instanceof CSimpleType) {
        return type1;
      } else if (type2 instanceof CPointerType) {
        return type1;
      } else {
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET!");
      }
    } else {
      throw new UnsupportedOperationException("NOT IMPLEMENTED YET!");
    }
  }

  public static int computeSizeOf(CType type, MachineModel machineModel) {
    return type.accept(new MachineModel.BaseSizeofVisitor(machineModel));
  }
}

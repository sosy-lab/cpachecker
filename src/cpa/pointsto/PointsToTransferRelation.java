/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 *
 */
package cpa.pointsto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IArrayType;
import org.eclipse.cdt.core.dom.ast.IPointerType;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.StatementEdge;
import cpa.common.CPAchecker;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.pointsto.PointsToElement.InMemoryObject;
import cpa.pointsto.PointsToRelation.Address;
import cpa.pointsto.PointsToRelation.AddressOfObject;
import cpa.pointsto.PointsToRelation.InvalidPointer;
import exceptions.CPATransferException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToTransferRelation implements TransferRelation {

  private class DeclarationVisitor extends ASTVisitor {

    private final PointsToElement pointsToElement;
    private PointsToRelation relation;

    public DeclarationVisitor (PointsToElement pointsToElement) {
      this.pointsToElement = pointsToElement;
      relation = null;

      shouldVisitParameterDeclarations = true;
      shouldVisitDeclarators = true;
      shouldVisitExpressions = true;
    }

    @Override
    public int visit (IASTDeclarator declarator) {
      // CPAMain.logManager.log(Level.ALL, "DEBUG_1", "Got into IASTDeclarator");
      if (0 != declarator.getPointerOperators().length) {
        relation = pointsToElement.addVariable(declarator);
        IASTInitializer initializer = declarator.getInitializer ();
        if (initializer != null) initializer.accept(this);
      }
      // TODO we need to handle arrays here
      return PROCESS_ABORT;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTExpression)
     */
    @Override
    public int visit(IASTExpression expression) {
      // CPAMain.logManager.log(Level.ALL, "DEBUG_1", "Got into IASTExpression with " + expression.toString());
      assert (null != relation);

      if (expression.getRawSignature().equals("NULL") || expression.toString().equals("0")) {
        relation.makeNull();
      } else {
        // any other constant is invalid
        relation.makeInvalid();
      }
      return super.visit(expression);
    }
  }

  private class StatementVisitor extends ASTVisitor {

    private final PointsToElement pointsToElement;
    private final HashMap<IASTNode,PointsToRelation> relations;

    // TODO instead of this hack we must properly map IType to ints
    private final static int MAGIC_SIZE = 4;

    public StatementVisitor (PointsToElement pointsToElement) {
      this.pointsToElement = pointsToElement;
      this.relations = new HashMap<IASTNode,PointsToRelation>();

      shouldVisitExpressions = true;
      shouldVisitStatements = true;
    }

    private void handle(IASTBinaryExpression binaryExpression) {

      CPAchecker.logger.log(Level.ALL, "DEBUG_1", "Got into IASTBinaryExpression with " + binaryExpression.toString());

      switch (binaryExpression.getOperator ())
      {
      // X = Y
      case IASTBinaryExpression.op_assign:
      {
        IASTExpression op1 = binaryExpression.getOperand1();
        PointsToRelation entryLhs = relations.get(op1);
        assert (entryLhs != null);
        IASTExpression rhs = binaryExpression.getOperand2();
        PointsToRelation entryRhs = relations.get(rhs);
        if (null == entryRhs) {
          if (rhs.getRawSignature().equals("NULL") || rhs.toString().equals("0")) {
            entryLhs.makeNull();
          } else {
            // TODO handle malloc here -- hmm, no, malloc should yield an appropriate object
            // this is likely invalid
            entryLhs.makeInvalid();
          }
        } else {
          entryLhs.makeAlias(entryRhs);
        }
        // if X is *x or x[y] then add an entry to the memory map
        // TODO, hmm, how does that interfer with the temporaries that we write to mem for *x?
        if (op1 instanceof IASTUnaryExpression && IASTUnaryExpression.op_star == ((IASTUnaryExpression)op1).getOperator()) {
          IASTExpression op = ((IASTUnaryExpression)op1).getOperand();
          PointsToRelation r = relations.get(op);
          assert (r != null);
          for (Address a : r.getValues()) {
            pointsToElement.writeToMem(a, entryRhs);
          }
        } else if (op1 instanceof IASTArraySubscriptExpression) {
          // TODO handle assignments to arrays
        }
        relations.put(binaryExpression, entryLhs);
        break;
      }
      // X + Y, X - Y
      case IASTBinaryExpression.op_plus:
      case IASTBinaryExpression.op_minus:
      {
        PointsToRelation result = relations.get(binaryExpression.getOperand1()).clone();
        assert (relations.get(binaryExpression.getOperand2()) == null);
        try {
          int shift = Integer.valueOf(binaryExpression.getOperand2().getRawSignature());
          if (binaryExpression.getOperator() == IASTBinaryExpression.op_minus) shift *= -1;
          result.shift(shift * MAGIC_SIZE);
        } catch (NumberFormatException e) {
          result.makeInvalid();
        }
        relations.put(binaryExpression.getOperand1(), result);
        break;
      }
      // X += Y, X -= Y
      case IASTBinaryExpression.op_plusAssign:
      case IASTBinaryExpression.op_minusAssign:
      {
        PointsToRelation entryLhs = relations.get(binaryExpression.getOperand1());
        assert (entryLhs != null);
        IASTExpression rhs = binaryExpression.getOperand2();
        PointsToRelation entryRhs = relations.get(rhs);
        assert (entryRhs == null);
        try {
          int shift = Integer.valueOf(binaryExpression.getOperand2().getRawSignature());
          if (binaryExpression.getOperator() == IASTBinaryExpression.op_minus) shift *= -1;
          entryLhs.shift(shift * MAGIC_SIZE);
        } catch (NumberFormatException e) {
          entryLhs.makeInvalid();
        }
        relations.put(binaryExpression, entryLhs);
        break;
      }
      /*case IASTBinaryExpression.op_pmarrow:
      case IASTBinaryExpression.op_pmdot:*/
      default:
      {
        CPAchecker.logger.log(Level.WARNING, "Unhandled expression " + binaryExpression.getRawSignature());
      }
      }
    }

    private void handle(IASTUnaryExpression unaryExpression) {

      CPAchecker.logger.log(Level.ALL, "DEBUG_1", "Got into IASTUnaryExpression with " + unaryExpression.toString());

      PointsToRelation entry = relations.get(unaryExpression.getOperand());

      switch (unaryExpression.getOperator()) {
      // X--
      case IASTUnaryExpression.op_postFixDecr:
      {
        assert (entry != null);
        // clone before modifying the state
        relations.put(unaryExpression, entry.clone());
        entry.shift(-1 * MAGIC_SIZE);
        break;
      }
      // --X
      case IASTUnaryExpression.op_prefixDecr:
      {
        assert (entry != null);
        entry.shift(-1 * MAGIC_SIZE);
        relations.put(unaryExpression, entry);
        break;
      }
      // X++
      case IASTUnaryExpression.op_postFixIncr:
      {
        assert (entry != null);
        // clone before modifying the state
        relations.put(unaryExpression, entry.clone());
        entry.shift(1 * MAGIC_SIZE);
        break;
      }
      // ++X
      case IASTUnaryExpression.op_prefixIncr:
      {
        assert (entry != null);
        entry.shift(1 * MAGIC_SIZE);
        relations.put(unaryExpression, entry);
        break;
      }
      // *X
      case IASTUnaryExpression.op_star:
      {
        assert (entry != null);
        // for all addresses in entry do a lookup in memoryMap
        PointsToRelation r = new PointsToRelation(entry.getVariable(), new String("*") + entry.getName());
        for (Address a : entry.getValues()) {
          // get the objects in memory
          InMemoryObject deref = pointsToElement.deref(a);
          // we can only make use of pointers here
          if (null == deref || !(deref instanceof PointsToRelation)) {
            r.makeInvalid();
            break;
          } else {
            // ok, it's a pointers, properly update r
            // TODO update functions in PointsToRelation should get improved, this is a cruel hack
            if (r.isInvalid()) {
              r.makeAlias((PointsToRelation)deref);
            } else {
              for (Address addr : ((PointsToRelation)deref).getValues()) {
                r.addAddress(addr);
              }
            }
          }
        }
        relations.put(unaryExpression, r);
        // store the temporary in the memory map
        for (Address a : entry.getValues()) {
          pointsToElement.writeToMem(a, r);
        }
        break;
      }
      // &X
      case IASTUnaryExpression.op_amper:
      {
        if (null != entry) {
          // X is actually *Y
          // for all addresses in entry do a _reverse_ lookup in memoryMap
          Set<Address> addresses = pointsToElement.addressOf(entry);
          PointsToRelation r = new PointsToRelation(entry.getVariable(),
              (entry.getName().substring(0,1).equals("*") ? entry.getName().substring(1) : new String("&") + entry.getName()));
          for (Address a : addresses) {
            if (a instanceof InvalidPointer) {
              r.makeInvalid();
              break;
            } else {
              if (r.isInvalid()) {
                r.setAddress(a);
              } else {
                r.addAddress(a);
              }
            }
          }
          relations.put(unaryExpression, r);
        } else {
          PointsToRelation r = new PointsToRelation(null, new String("&") + unaryExpression.getOperand().getRawSignature());
          r.setAddress(new AddressOfObject(unaryExpression.getOperand()));
          relations.put(unaryExpression, r);
        }
      }
      default:
      {
        CPAchecker.logger.log(Level.WARNING, "Unhandled expression " + unaryExpression.getRawSignature());
      }
      }
    }

    private void handle(IASTArraySubscriptExpression arrayExpression) {
      CPAchecker.logger.log(Level.ALL, "DEBUG_1", "Got into IASTArraySubscriptExpression with " + arrayExpression.toString());
      CPAchecker.logger.log(Level.ALL, "DEBUG_1", "Is composed of " + arrayExpression.getArrayExpression() + " " + arrayExpression.getSubscriptExpression());
      PointsToRelation var = relations.get(arrayExpression.getArrayExpression());
      assert (var != null);
      PointsToRelation r = new PointsToRelation(var.getVariable(),
          var.getName() + "[" + arrayExpression.getSubscriptExpression().getRawSignature() + "]");
      try {
        int shift = Integer.valueOf(arrayExpression.getSubscriptExpression().getRawSignature());
        for (Address a : var.getValues()) {
          InMemoryObject deref = pointsToElement.deref(a);
          if (null == deref || !(deref instanceof PointsToRelation)) {
            r.makeInvalid();
          } else {
            // ok, it's a pointers, properly update r
            PointsToRelation ptr = (PointsToRelation)deref;
            ptr.shift(shift * MAGIC_SIZE);
            // TODO update functions in PointsToRelation should get improved, this is a cruel hack
            if (r.isInvalid()) {
              r.makeAlias(ptr);
            } else {
              for (Address addr : ptr.getValues()) {
                r.addAddress(addr);
              }
            }
          }
        }
      } catch (NumberFormatException e) {
        r.makeInvalid();
      }
      relations.put(arrayExpression, r);
      // store the temporary in the memory map
      //for (Address a : var.getValues()) {
        // TODO hmm, not really as easy, we need to handle the involved shift
        // pointsToElement.writeToMem(a, r);
      //}
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#leave(org.eclipse.cdt.core.dom.ast.IASTExpression)
     */
    @Override
    public int leave(IASTExpression expression) {
      CPAchecker.logger.log(Level.ALL, "DEBUG_1", "Got into IASTExpression with " + expression.toString());

      if (!(expression.getExpressionType() instanceof IPointerType) &&
          !(expression.getExpressionType() instanceof IArrayType)) return PROCESS_CONTINUE;

      // happy casting *ARRRGH***
      if (expression instanceof IASTBinaryExpression) {
        handle((IASTBinaryExpression)expression);
      } else if (expression instanceof IASTUnaryExpression) {
        handle((IASTUnaryExpression)expression);
      } else if (expression instanceof IASTArraySubscriptExpression) {
        handle((IASTArraySubscriptExpression)expression);
      } else if (expression instanceof IASTIdExpression) {
        CPAchecker.logger.log(Level.ALL, "DEBUG_1", "Got into IASTName");
        relations.put(expression, pointsToElement.lookup(((IASTIdExpression)expression).getName()));
        assert (relations.get(expression) != null);
      } else {
        CPAchecker.logger.log(Level.WARNING, "Unhandled expression " + expression);
        assert (false);
      }
      return PROCESS_CONTINUE;
    }
  }

  /*
  private class XVisitor extends ASTVisitor {

    private final PointsToElement pointsToElement;
    private final PointsToRelation relation;

    public XVisitor (PointsToElement pointsToElement) {
      this.pointsToElement = pointsToElement;
      relation = null;

      shouldVisitNames = true;
      shouldVisitDeclarations = true;
      shouldVisitInitializers = true;
      shouldVisitParameterDeclarations = true;
      shouldVisitDeclarators = true;
      shouldVisitDeclSpecifiers = true;
      shouldVisitExpressions = true;
      shouldVisitStatements = true;
      shouldVisitTypeIds = true;
      shouldVisitEnumerators = true;
      shouldVisitTranslationUnit = true;
      shouldVisitProblems = true;
    }
  }
  */

  private AbstractElement getAbstractSuccessor(AbstractElement element,
      CFAEdge cfaEdge, Precision prec) throws CPATransferException {
    PointsToElement pointsToElement = (PointsToElement) element;
    CPAchecker.logger.log(Level.INFO, "Input: " + pointsToElement.toString());

    switch (cfaEdge.getEdgeType ())
    {
    case StatementEdge:
    {
      pointsToElement = pointsToElement.clone ();
      StatementVisitor visitor = new StatementVisitor(pointsToElement);
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      IASTExpression expression = statementEdge.getExpression ();
      CPAchecker.logger.log(Level.INFO, "Statement Edge = " + expression.getRawSignature());
      expression.accept(visitor);
      break;
    }
    case MultiStatementEdge:
    {
      pointsToElement = pointsToElement.clone ();
      StatementVisitor visitor = new StatementVisitor(pointsToElement);
      MultiStatementEdge multiStatementEdge = (MultiStatementEdge) cfaEdge;
      for (IASTExpression expression : multiStatementEdge.getExpressions ())
        expression.accept(visitor);

      break;
    }
    case DeclarationEdge:
    {
      pointsToElement = pointsToElement.clone();
      DeclarationVisitor visitor = new DeclarationVisitor(pointsToElement);
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      IASTDeclarator [] declarators = declarationEdge.getDeclarators ();
      CPAchecker.logger.log(Level.INFO, "Decleration Edge = " + declarationEdge.getRawStatement());
      for (IASTDeclarator declarator : declarators) {
        declarator.accept(visitor);
      }
      break;
    }
    case MultiDeclarationEdge:
    {
      pointsToElement = pointsToElement.clone ();
      DeclarationVisitor visitor = new DeclarationVisitor(pointsToElement);
      MultiDeclarationEdge multiDeclarationEdge = (MultiDeclarationEdge) cfaEdge;
      for (IASTDeclarator [] declarators : multiDeclarationEdge.getDeclarators ())
        for (IASTDeclarator declarator : declarators) {
          declarator.accept(visitor);
        }

      break;
    }
    case AssumeEdge:
    case FunctionCallEdge:
    case ReturnEdge:
    default:
    {
      CPAchecker.logger.log(Level.WARNING, "Edge " + cfaEdge + " not handled in points-to transfer relation");
    }
    }

    CPAchecker.logger.log(Level.INFO, "Output: " + pointsToElement.toString());
    return pointsToElement;
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision prec, CFAEdge cfaEdge) throws CPATransferException {
    return Collections.singleton(getAbstractSuccessor(element, cfaEdge, prec));
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
    return null;
  }
}

/**
 *
 */
package cpa.pointsto;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToCPA implements ConfigurableProgramAnalysis {

  private final AbstractDomain abstractDomain;
  private final PrecisionDomain precisionDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;

  public PointsToCPA (String mergeType, String stopType) throws CPAException {
    abstractDomain = new PointsToDomain();
    precisionDomain = new PointsToPrecisionDomain();
    transferRelation = new PointsToTransferRelation();
    mergeOperator = new PointsToMerge(abstractDomain);
    stopOperator = new PointsToStop(abstractDomain);
    precisionAdjustment = new PointsToPrecisionAdjustment();
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getAbstractDomain()
   */
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  public PrecisionDomain getPrecisionDomain() {
    return precisionDomain;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getTransferRelation()
   */
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getMergeOperator()
   */
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getStopOperator()
   */
  public StopOperator getStopOperator() {
    return stopOperator;
  }


  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    PointsToElement initial = new PointsToElement();

    if (node instanceof FunctionDefinitionNode) {
      List<IASTParameterDeclaration> parameters = ((FunctionDefinitionNode)node).getFunctionParameters ();
      for (IASTParameterDeclaration parameter : parameters) {
        if (0 != parameter.getDeclarator().getPointerOperators().length) {
          if (parameter.getDeclarator().getNestedDeclarator() != null) {
            initial.addVariable(parameter.getDeclarator().getNestedDeclarator());
          } else {
            initial.addVariable(parameter.getDeclarator());
          }
        }
      }
    }

    return initial;
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    // TODO Auto-generated method stub
    return null;
  }
}

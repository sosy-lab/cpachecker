/**
 * 
 */
package cpaplugin.cpa.cpas.pointsto;

import java.util.List;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToCPA implements ConfigurableProgramAnalysis {

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	public PointsToCPA (String mergeType, String stopType) throws CPAException {
		abstractDomain = new PointsToDomain();
		mergeOperator = new PointsToMerge(abstractDomain);
		stopOperator = new PointsToStop(abstractDomain);
		transferRelation = new PointsToTransferRelation(abstractDomain);
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis#getAbstractDomain()
	 */
	public AbstractDomain getAbstractDomain() {
		return abstractDomain;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis#getInitialElement(cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode)
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

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis#getMergeOperator()
	 */
	public MergeOperator getMergeOperator() {
		return mergeOperator;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis#getStopOperator()
	 */
	public StopOperator getStopOperator() {
		return stopOperator;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis#getTransferRelation()
	 */
	public TransferRelation getTransferRelation() {
		return transferRelation;
	}

}

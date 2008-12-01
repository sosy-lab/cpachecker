package cpa.symbpredabs.summary;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;


import cfa.objectmodel.c.FunctionDefinitionNode;


/**
 * InnerCFANode for function definitions
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class InnerFunctionDefinitionNode extends FunctionDefinitionNode
    implements InnerCFANode {

    private SummaryCFANode summary;

    public InnerFunctionDefinitionNode(int lineNumber,
                                       IASTFunctionDefinition def) {
        super(lineNumber, def);
        summary = null;
    }

    public SummaryCFANode getSummaryNode() { return summary; }

    public void setSummaryNode(SummaryCFANode s) {
        summary = s;
    }
}

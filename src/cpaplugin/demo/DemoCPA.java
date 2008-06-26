package cpaplugin.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.common.CPAException;
import cpaplugin.cpa.common.CompositeDomain;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.SimpleCPA;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.domains.defuse.DefUseDefinition;
import cpaplugin.cpa.domains.defuse.DefUseDomain;
import cpaplugin.cpa.domains.defuse.DefUseElement;
import cpaplugin.cpa.domains.defuse.DefUseMergeJoin;
import cpaplugin.cpa.domains.defuse.DefUseMergeSep;
import cpaplugin.cpa.domains.defuse.DefUseStopSep;
import cpaplugin.cpa.domains.defuse.DefUseTransferRelation;
import cpaplugin.cpa.domains.location.LocationDomain;
import cpaplugin.cpa.domains.location.LocationElement;
import cpaplugin.cpa.domains.location.LocationMergeSep;
import cpaplugin.cpa.domains.location.LocationStopSep;
import cpaplugin.cpa.domains.location.LocationTransferRelation;

public class DemoCPA
{
    private static class CompositeMergeOperator implements MergeOperator
    {
        private CompositeDomain compositeDomain;
        private List<MergeOperator> mergeOperators;
        
        public CompositeMergeOperator (CompositeDomain compositeDomain, List<MergeOperator> mergeOperators)
        {
            this.compositeDomain = compositeDomain;
            this.mergeOperators = mergeOperators;
        }
        
        public AbstractDomain getAbstractDomain ()
        {
            return compositeDomain;
        }

        public AbstractElement merge (AbstractElement element1, AbstractElement element2)
        {
            // Merge Sep Code
            CompositeElement comp1 = (CompositeElement) element1;
            CompositeElement comp2 = (CompositeElement) element2;
            
            LocationElement locationElement1 = (LocationElement) comp1.getElements ().get (0);
            LocationElement locationElement2 = (LocationElement) comp2.getElements ().get (0);
            
            DefUseElement defUseElement1 = (DefUseElement) comp1.getElements ().get (1);
            DefUseElement defUseElement2 = (DefUseElement) comp2.getElements ().get (1);
            
            if (!locationElement1.equals (locationElement2))
                return element2;
            
            List<AbstractElement> mergedElements = new ArrayList<AbstractElement> ();
            mergedElements.add (mergeOperators.get (0).merge (locationElement1, locationElement2));
            mergedElements.add (mergeOperators.get (1).merge (defUseElement1, defUseElement2));
            
            return new CompositeElement (mergedElements);
        }
    }
    
    private static class CompositeStopOperator implements StopOperator
    {
        private CompositeDomain compositeDomain;
        private List<StopOperator> stopOperators;
        
        public CompositeStopOperator (CompositeDomain compositeDomain, List<StopOperator> stopOperators)
        {
            this.compositeDomain = compositeDomain;
            this.stopOperators = stopOperators;
        }
        
        public AbstractDomain getAbstractDomain ()
        {
            return compositeDomain;
        }

        public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
        {
            PreOrder preOrder = compositeDomain.getPreOrder ();
            for (AbstractElement testElement : reached)
            {
                if (preOrder.satisfiesPreOrder (element, testElement))
                    return true;
            }
            
            return false;
        }
    }
    
    private static class CompositeTransferRelation implements TransferRelation
    {
        private CompositeDomain compositeDomain;
        private List<TransferRelation> transferRelations;
        
        private LocationTransferRelation locationTransferRelation;
        
        public CompositeTransferRelation (CompositeDomain compositeDomain, List<TransferRelation> transferRelations)
        {
            this.compositeDomain = compositeDomain;
            this.transferRelations = transferRelations;
            
            TransferRelation first = transferRelations.get (0);
            if (first instanceof LocationTransferRelation)
            {
                locationTransferRelation = (LocationTransferRelation) first;
            }
        }
        
        public AbstractDomain getAbstractDomain ()
        {
            return compositeDomain;
        }

        public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge)
        {
            CompositeElement compositeElement = (CompositeElement) element;
            List<AbstractElement> inputElements = compositeElement.getElements ();
            List<AbstractElement> resultingElements = new ArrayList<AbstractElement> ();
            
            for (int idx = 0; idx < transferRelations.size (); idx++)
            {
                TransferRelation transfer = transferRelations.get (idx);
                AbstractElement subElement = inputElements.get (idx);
                
                AbstractElement successor = transfer.getAbstractSuccessor (subElement, cfaEdge);
                resultingElements.add (successor);
            }
            
            return new CompositeElement (resultingElements);
        }
        
        public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException
        {
            if (locationTransferRelation == null)
                throw new CPAException ("First abstract domain must be a location domain to call getAllAbstractSuccessors()");
            
            CompositeElement compositeElement = (CompositeElement) element;
            List<AbstractElement> abstractElements = compositeElement.getElements (); 
            LocationElement locationElement = (LocationElement) abstractElements.get (0);
            
            List<AbstractElement> results = new ArrayList<AbstractElement> ();
            
            CFANode node = locationElement.getLocationNode ();
            for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges (); edgeIdx++)
            {
                CFAEdge edge = node.getLeavingEdge (edgeIdx);
                results.add (getAbstractSuccessor (element, edge));
            }
            
            return results;
        }   
    }
    
    public static String getFileName ()
    {
        return "TestCApp/Test1.c";
    }
        
    public static ConfigurableProblemAnalysis getDemoCPA ()
    {
        LocationDomain locationDomain = new LocationDomain ();
        MergeOperator locationMergeOp = new LocationMergeSep (locationDomain);
        StopOperator locationStopOp = new LocationStopSep (locationDomain);
        LocationTransferRelation locationTransferRelation = new LocationTransferRelation (locationDomain);

        DefUseDomain defUseDomain = new DefUseDomain ();
        MergeOperator defUseMergeOp = new DefUseMergeSep (defUseDomain);
        //MergeOperator defUseMergeOp = new DefUseMergeJoin (defUseDomain);
        StopOperator defUseStopOp = new DefUseStopSep (defUseDomain);
        TransferRelation defUseTransferRelation = new DefUseTransferRelation (defUseDomain);
        
        List<AbstractDomain> domains = new ArrayList<AbstractDomain> ();
        domains.add (locationDomain);
        domains.add (defUseDomain);
        
        List<MergeOperator> mergeOperators = new ArrayList<MergeOperator> ();
        mergeOperators.add (locationMergeOp);
        mergeOperators.add (defUseMergeOp);
        
        List<StopOperator> stopOperators = new ArrayList<StopOperator> ();
        stopOperators.add (locationStopOp);
        stopOperators.add (defUseStopOp);
        
        List<TransferRelation> transferRelations = new ArrayList<TransferRelation> ();
        transferRelations.add (locationTransferRelation);
        transferRelations.add (defUseTransferRelation);
        
        CompositeDomain compositeDomain = new CompositeDomain (domains);
        CompositeMergeOperator compositeMerge = new CompositeMergeOperator (compositeDomain, mergeOperators);
        CompositeStopOperator compositeStop = new CompositeStopOperator (compositeDomain, stopOperators);
        CompositeTransferRelation compositeTransfer = new CompositeTransferRelation (compositeDomain, transferRelations);
        
        ConfigurableProblemAnalysis cpa = SimpleCPA.createSimpleCPA (compositeDomain, compositeMerge, compositeStop, compositeTransfer);
        return cpa;
    }
    
    public static AbstractElement getInitialElement (CFAFunctionDefinitionNode node)
    {
        List<DefUseDefinition> defUseDefinitions = null;
        if (node instanceof FunctionDefinitionNode)
        {
            List<String> parameterNames = ((FunctionDefinitionNode)node).getFunctionParameterNames ();
            defUseDefinitions = new ArrayList<DefUseDefinition> ();
            
            for (String parameterName : parameterNames)
            {
                DefUseDefinition newDef = new DefUseDefinition (parameterName, null);
                defUseDefinitions.add (newDef);
            }
        }

        LocationElement initialLocationElement = new LocationElement (node);
        DefUseElement defUseInitialElement = new DefUseElement (defUseDefinitions);
        
        List<AbstractElement> initialElements = new ArrayList<AbstractElement> ();
        initialElements.add (initialLocationElement);
        initialElements.add (defUseInitialElement);
                
        CompositeElement initialElement = new CompositeElement (initialElements);
        
        return initialElement;
    }
}

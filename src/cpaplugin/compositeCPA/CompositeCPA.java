package cpaplugin.compositeCPA;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cpa.common.CompositeDomain;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.defuse.DefUseCPA;
import cpaplugin.cpa.cpas.interprocedural.InterProceduralCPA;
import cpaplugin.cpa.cpas.location.LocationCPA;
import cpaplugin.cpa.cpas.octagon.OctagonCPA;
import cpaplugin.cpa.cpas.predicateabstraction.PredicateAbstractionCPA;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class CompositeCPA implements ConfigurableProblemAnalysis
{
	//public final static CompositeCPA CompositeCPA_INSTANCE = new CompositeCPA();
	
	private AbstractDomain abstractDomain;
    private MergeOperator mergeOperator;
    private StopOperator stopOperator;
    private TransferRelation transferRelation;
    private AbstractElement initialElement;

	private CompositeCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation,
			AbstractElement initialElement)
	{
		this.abstractDomain = abstractDomain;
		this.mergeOperator = mergeOperator;
		this.stopOperator = stopOperator;
		this.transferRelation = transferRelation;
		this.initialElement = initialElement;
	}

	private static ConfigurableProblemAnalysis createNewCompositeCPA(List<ConfigurableProblemAnalysis> cpas, CFAFunctionDefinitionNode node) {

		int sizeOfCompositeDomain = cpas.size();
		List<AbstractDomain> domains = new ArrayList<AbstractDomain> ();
		List<MergeOperator> mergeOperators = new ArrayList<MergeOperator> ();
		List<StopOperator> stopOperators = new ArrayList<StopOperator> ();
		List<TransferRelation> transferRelations = new ArrayList<TransferRelation> ();
		List<AbstractElement> initialElements = new ArrayList<AbstractElement> ();

		for(int i=0; i<sizeOfCompositeDomain; i++){
			ConfigurableProblemAnalysis sp = cpas.get(i);
			AbstractDomain domain = sp.getAbstractDomain();
			domains.add(domain);

			MergeOperator mergeOperator = sp.getMergeOperator();
			mergeOperators.add(mergeOperator);

			StopOperator stopOperator = sp.getStopOperator();
			stopOperators.add(stopOperator);

			TransferRelation transferRelation = sp.getTransferRelation();
			transferRelations.add(transferRelation);
			
			AbstractElement initialElement = sp.getInitialElement(node);
			initialElements.add(initialElement);
		}

		CompositeDomain compositeDomain = new CompositeDomain (domains);
		CompositeMergeOperator compositeMerge = new CompositeMergeOperator (compositeDomain, mergeOperators);
		CompositeStopOperator compositeStop = new CompositeStopOperator (compositeDomain, stopOperators);
		CompositeTransferRelation compositeTransfer = new CompositeTransferRelation (compositeDomain, transferRelations);
		CompositeElement initialElement = new CompositeElement (initialElements);

		return createCompositeCPA(compositeDomain, compositeMerge, compositeStop, compositeTransfer, initialElement);

	}

	public static CompositeCPA createCompositeCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation,
			AbstractElement initialElement)
	{
		if (abstractDomain == null || mergeOperator == null ||
				stopOperator == null || transferRelation == null ||
				initialElement == null)
			return null;

		if (mergeOperator.getAbstractDomain () != abstractDomain ||
				stopOperator.getAbstractDomain () != abstractDomain ||
				transferRelation.getAbstractDomain () != abstractDomain)
			return null;

		return new CompositeCPA (abstractDomain, mergeOperator, stopOperator, transferRelation, initialElement);
	}
	
	public static ConfigurableProblemAnalysis getCompositeCPA (CPAType[] cpaNamesArray, CFAFunctionDefinitionNode node) throws CPAException
	{
		// The list to keep all cpas
		List<ConfigurableProblemAnalysis> cpas = new ArrayList<ConfigurableProblemAnalysis> ();

		// Create new instances of domains and operators when you add a new cpa
		
		LocationCPA locationCpa;
		InterProceduralCPA interProceduralCpa;

		DefUseCPA defUseCpa;
		MergeType defUseMergeType;
		StopType defUseStopType;

		OctagonCPA octagonCpa;
		MergeType octagonMergeType;
		StopType octagonStopType;

		PredicateAbstractionCPA predicateAbstractionCpa;
		MergeType predicateAbstractionMergeType;
		StopType predicateAbstractionStopType;

		int sizeOfCompositeCPA = cpaNamesArray.length;

		for(int i=0; i<sizeOfCompositeCPA; i++){
			CPAType typeOfCPA = cpaNamesArray[i];
			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, typeOfCPA + " is added to the list of CPAs ");

			if(typeOfCPA == CPAType.LocationCPA){
				locationCpa = LocationCPA.createNewLocationCPA(MergeType.MergeSep);
				cpas.add(locationCpa);
			}
			else if(typeOfCPA == CPAType.InterProceduralCPA){
				interProceduralCpa = InterProceduralCPA.createNewInterProceduralCPA(MergeType.MergeSep);
				cpas.add(interProceduralCpa);
			}
			else if(typeOfCPA == CPAType.DefUseCPA){
				// TODO read from file
				defUseMergeType = MergeType.MergeSep;
				defUseStopType = StopType.StopSep;
				defUseCpa = DefUseCPA.createNewDefUseCPA (defUseMergeType, defUseStopType);
				cpas.add(defUseCpa);
			}
			else if(typeOfCPA == CPAType.OctagonCPA){
				// TODO read from file
				octagonMergeType = MergeType.MergeSep;
				octagonStopType = StopType.StopSep;
				octagonCpa = OctagonCPA.createNewDefUseCPA (octagonMergeType, octagonStopType);
				cpas.add(octagonCpa);
			}
			else if(typeOfCPA == CPAType.PredicateAbstractionCPA){
				// TODO read from file
				predicateAbstractionMergeType = MergeType.MergeSep;
				predicateAbstractionStopType = StopType.StopSep;
				predicateAbstractionCpa = PredicateAbstractionCPA.createNewPredicateAbstractionCPA (predicateAbstractionMergeType, predicateAbstractionStopType);
				cpas.add(predicateAbstractionCpa);
			}
		}
		CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "CompositeCPA is built using the list of CPAs");
		ConfigurableProblemAnalysis cpa = CompositeCPA.createNewCompositeCPA (cpas, node);
		return cpa;
	}

	public AbstractDomain getAbstractDomain() {
		return abstractDomain;
	}

	public MergeOperator getMergeOperator() {
		return mergeOperator;
	}

	public StopOperator getStopOperator() {
		return stopOperator;
	}

	public TransferRelation getTransferRelation() {
		return transferRelation;
	}
	
    public AbstractElement getInitialElement (CFAFunctionDefinitionNode node)
    {
        return initialElement;
    }
}


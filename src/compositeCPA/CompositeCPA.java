package compositeCPA;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import logging.CustomLogLevel;
import logging.LazyLogger;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.CompositeDomain;
import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpaplugin.CPAStatistics;
import exceptions.CPAException;

public class CompositeCPA implements ConfigurableProgramAnalysis
{
	private final AbstractDomain abstractDomain;
	private final MergeOperator mergeOperator;
	private final StopOperator stopOperator;
	private final TransferRelation transferRelation;
	private final AbstractElement initialElement;

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

	public static ConfigurableProgramAnalysis createNewCompositeCPA(List<ConfigurableProgramAnalysis> cpas, CFAFunctionDefinitionNode node) {

		int sizeOfCompositeDomain = cpas.size();
		List<AbstractDomain> domains = new ArrayList<AbstractDomain> ();
		List<MergeOperator> mergeOperators = new ArrayList<MergeOperator> ();
		List<StopOperator> stopOperators = new ArrayList<StopOperator> ();
		List<TransferRelation> transferRelations = new ArrayList<TransferRelation> ();
		List<AbstractElement> initialElements = new ArrayList<AbstractElement> ();

		for(int i=0; i<sizeOfCompositeDomain; i++){
			ConfigurableProgramAnalysis sp = cpas.get(i);
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
		CompositeElement initialElement = new CompositeElement (initialElements, null);
		// set call stack
		CallStack initialCallStack = new CallStack();
		CallElement initialCallElement = new CallElement(node.getFunctionName(), node, initialElement);
		initialCallStack.push(initialCallElement);
		initialElement.setCallStack(initialCallStack);

		return createCompositeCPA(compositeDomain, compositeMerge, compositeStop, compositeTransfer, initialElement);

	}

	public static CompositeCPA createCompositeCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation,
			AbstractElement initialElement)
	{
		// TODO Michael: this should throw something
		if (abstractDomain == null || mergeOperator == null ||
				stopOperator == null || transferRelation == null ||
				initialElement == null)
			return null;

		/*if (mergeOperator.getAbstractDomain () != abstractDomain ||
				stopOperator.getAbstractDomain () != abstractDomain ||
				transferRelation.getAbstractDomain () != abstractDomain)
			return null;*/

		return new CompositeCPA (abstractDomain, mergeOperator, stopOperator, transferRelation, initialElement);
	}

	@SuppressWarnings("unchecked")
	public static ConfigurableProgramAnalysis getCompositeCPA (CFAFunctionDefinitionNode node) throws CPAException
	{
		String[] cpaNamesArray = CPAMain.cpaConfig.getPropertiesArray("analysis.cpas");
		String[] mergeTypesArray = CPAMain.cpaConfig.getPropertiesArray("analysis.mergeOperators");
		String[] stopTypesArray = CPAMain.cpaConfig.getPropertiesArray("analysis.stopOperators");

		// The list to keep all cpas
		List<ConfigurableProgramAnalysis> cpas = new ArrayList<ConfigurableProgramAnalysis> ();

		int sizeOfCompositeCPA = cpaNamesArray.length;

		for(int i=0; i<sizeOfCompositeCPA; i++){
			// TODO make sure that the first CPA carries location information
			// otherwise the analysis will have efficiency problems

			// get name of the cpa, we are getting the explicit
			// path of the representing class of this cpa
			String cpaName = cpaNamesArray[i];

			Class cls;
			try {
				cls = Class.forName(cpaName);
				Class parameterTypes[] = {String.class, String.class};
				Constructor ct = cls.getConstructor(parameterTypes);
				Object argumentlist[] = {mergeTypesArray[i], stopTypesArray[i]};
				Object obj = ct.newInstance(argumentlist);
				// Convert object to CPA
				ConfigurableProgramAnalysis newCPA = (ConfigurableProgramAnalysis)obj;
				cpas.add(newCPA);

				// AG - check if this cpa defines its own
				// statistics, and if so add them to the
				// main ones
				try {
				    Method meth =
				        cls.getDeclaredMethod("getStatistics");
				    CPAStatistics s =
				        (CPAStatistics)meth.invoke(newCPA);
				    CPAMain.cpaStats.addSubStatistics(s);
				} catch (Exception e) {
				    // ignore, this is not an error
				}

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		ConfigurableProgramAnalysis cpa = null;
		// TODO this was for efficiency but I modified the condition for it to work only with
		// summary nodes
		if (cpas.size() == 1 &&
		        CPAMain.cpaConfig.getBooleanValue(
		                "analysis.noCompositeCPA")) {
		    LazyLogger.log(CustomLogLevel.MainApplicationLevel,
		            "Only one analyis active, ",
		            "no need of a composite CPA");
		    cpa = cpas.get(0);
		} else {
		    LazyLogger.log(CustomLogLevel.MainApplicationLevel,
                                   "CompositeCPA is built using the list " +
                                   "of CPAs");
		    cpa = CompositeCPA.createNewCompositeCPA (cpas, node);
		 }
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


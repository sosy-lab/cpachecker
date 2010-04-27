package org.sosy_lab.cpachecker.plugin.eclipse;

import org.eclipse.cdt.core.model.ICElement;

public class TestRunner {

	public void run(ICElement selected) {
		// TODO Auto-generated method stub
		
		// use:
		CPAcheckerPlugin.getPlugin().fireTestsStarted(10);
		CPAcheckerPlugin.getPlugin().fireTestStarted("someid");
		CPAcheckerPlugin.getPlugin().fireTestFailed("someid");
		CPAcheckerPlugin.getPlugin().fireTestsFinished();
		
		
		
	}

}

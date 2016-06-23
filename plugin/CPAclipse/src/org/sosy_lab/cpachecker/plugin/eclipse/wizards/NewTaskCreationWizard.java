/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.sosy_lab.cpachecker.plugin.eclipse.wizards;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;


public class NewTaskCreationWizard extends Wizard implements IWorkbenchWizard{
	private NewTaskCreationWizardPage firstPage;
	//private IWorkbench workbench;
	//private IStructuredSelection selection;
	
	public NewTaskCreationWizard() {
		super();
		firstPage = new NewTaskCreationWizardPage("First Page", this);
		addPage(firstPage);
	}

	@Override
	public boolean performFinish() {
	    ITranslationUnit source = firstPage.getITranslationUnit();
	    if (source == null) {
	    	return false;
	    }
	    
		Task t;
		t = new Task(firstPage.getTaskName(),firstPage.getConfigFile() , source, firstPage.getSpecificationFile());
		CPAclipse.getPlugin().addTask(t);
		return true;
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		//this.workbench = workbench;
		//this.selection = selection;
		setWindowTitle("new Task");
		//setDefaultPageImageDescriptor(ReadmeImages.README_WIZARD_BANNER);
	}
}

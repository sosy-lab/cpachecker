package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner;

public class DefaultCheckAction implements IObjectActionDelegate {

	private Shell shell;
	private ISelection selection;
	
	public DefaultCheckAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection structured = (IStructuredSelection) selection;
		ICElement selected = (ITranslationUnit) structured.getFirstElement();
		// safe cast because plugin.xml states that only ITranslationUnits can be subject of this action
		MessageDialog.openInformation(
			shell,
			"CPAcheckerPlugin",
			"C File Type + " + selected.getClass().toString() + " Resource: " + selected);
		
		Map<String, String> emptyMap = Collections.emptyMap();
		//TODO: provide real configuration
		Configuration config = new Configuration(emptyMap);
		String filename = selected.toString();		
		
		CPAcheckerPlugin.runTest(Collections.singletonList(new TaskRunner.Task("anonymous Task", config, filename)));
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
}

package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

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
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;

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
		CPAcheckerPlugin.runTest(selected);
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
}

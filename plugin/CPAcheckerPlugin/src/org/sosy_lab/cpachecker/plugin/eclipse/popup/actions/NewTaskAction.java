package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.List;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;

public class NewTaskAction implements IObjectActionDelegate {

	private Shell shell;
	private ISelection selection;
	
	public NewTaskAction() {
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
	@SuppressWarnings("unchecked") // List is without generics
	public void run(IAction action) {
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection structured = (IStructuredSelection) selection;
		ITranslationUnit selectedSource = null;
		IFile selectedConfig = null;
		List selectedElements = structured.toList();
		//assert selectedElements.size() == 2;
		for (Object element : selectedElements) {
			if (element instanceof ITranslationUnit) {
				selectedSource = (ITranslationUnit)element;
			} else if (element instanceof IFile) {
				selectedConfig = (IFile)element;
			}
		}
		
		if (selectedConfig != null && ! selectedConfig.getFileExtension().equals("properties")) {
			MessageDialog.openError(shell, "Unable to perform", "Configuration file must be a \".properties\" File.");
		} else {
			Task t = new Task("generated", selectedConfig, selectedSource);
			// at this time config does not need to be a correct configuration file
			CPAcheckerPlugin.getPlugin().addTask(t);
			System.out.println("NewTaskAction: Task added");
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@SuppressWarnings("unchecked")
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
		IStructuredSelection structured = (IStructuredSelection) selection;
		ITranslationUnit selectedSource = null;
		IFile selectedConfig = null;
		List selectedElements = structured.toList();
		//assert selectedElements.size() == 2;
		for (Object element : selectedElements) {
			if (element instanceof ITranslationUnit) {
				selectedSource = (ITranslationUnit)element;
			} else if (element instanceof IFile) {
				selectedConfig = (IFile)element;
			}
		}
		if (selectedConfig != null && ! selectedConfig.getFileExtension().equals("properties")) {
			action.setEnabled(false);
		} else if (selectedConfig != null || selectedSource != null) {
			action.setEnabled(true);
		} else {
			action.setEnabled(false);
		}
	}
}

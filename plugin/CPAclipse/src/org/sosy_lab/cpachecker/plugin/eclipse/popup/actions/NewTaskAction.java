package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

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
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
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
	public void run(IAction action) {
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection structured = (IStructuredSelection) selection;
		ITranslationUnit selectedSource = null;
		IFile selectedConfig = null;
		IFile selectedSpecification = null;
		for (Object element : structured.toList()) {
			if (element instanceof ITranslationUnit) {
				selectedSource = (ITranslationUnit)element;
			} else if (element instanceof IFile) {
				IFile file = (IFile)element;
				if (file.getFileExtension() != null) {
					if (file.getFileExtension().equals("spc")) {
						selectedSpecification = file;
					} else if(file.getFileExtension().equals("properties")) {
						selectedConfig = file;
					} else {
						MessageDialog.openError(shell, "Unable to perform", "Configuration file must have a \".properties\"-Ending. Specification Files must have \".spc\"-Ending");
						return;
					}
				} else {
					MessageDialog.openError(shell, "Unable to perform", "Configuration file must have a \".properties\"-Ending. Specification Files must have \".spc\"-Ending");
					return;
				}
			}
		}
		{
			Task t = new Task("generated", selectedConfig, selectedSource, selectedSpecification);
			// at this time config does not need to be a correct configuration file
			CPAclipse.getPlugin().addTask(t);
			System.out.println("NewTaskAction: Task added: " +  t.getName());
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
		IStructuredSelection structured = (IStructuredSelection) selection;
		ITranslationUnit selectedSource = null;
		IFile selectedConfig = null;
		IFile selectedSpecification = null;
		for (Object element : structured.toList()) {
			if (element instanceof ITranslationUnit) {
				selectedSource = (ITranslationUnit)element;
			} else if (element instanceof IFile) {
				IFile file = (IFile)element;
				if (file.getFileExtension() != null) {
					if (file.getFileExtension().equals("spc")) {
						selectedSpecification = file;
					} else if(file.getFileExtension().equals("properties")) {
						selectedConfig = file;
					}
				}
			}
		}
		if (selectedConfig != null && ! selectedConfig.getFileExtension().equals("properties")) {
			action.setEnabled(false);
		} else if (selectedSpecification != null && ! selectedSpecification.getFileExtension().equals("spc")) {
			action.setEnabled(false);
		} else if (selectedConfig != null || selectedSource != null || selectedSpecification != null) {
			action.setEnabled(true);
		} else {
			action.setEnabled(false);
		}
	}
}

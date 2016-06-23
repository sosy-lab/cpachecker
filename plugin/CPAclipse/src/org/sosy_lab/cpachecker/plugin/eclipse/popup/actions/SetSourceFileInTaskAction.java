package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.Collections;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;

public class SetSourceFileInTaskAction extends Action {
	private Shell shell;
	private Task task;
	
	public SetSourceFileInTaskAction(Shell shell, Task task) {
		super();
		this.shell = shell;
		this.task = task;
		this.setText("set source File");
	}

	@Override
	public void run() {
		ITranslationUnit newTransUnit;
		if (task.getTranslationUnit() != null) {
			newTransUnit = CPAclipse.askForSourceFile(shell, task.getTranslationUnit());
		} else {
			newTransUnit = CPAclipse.askForSourceFile(shell, null);
		}
		if (newTransUnit != null) {
			task.setTranslationUnit(newTransUnit);
			task.setDirty(true);
			CPAclipse.getPlugin().fireTasksChanged(Collections.singletonList(task));
		}
	}
		/*
		String currentPath = "";
		if (task.getTranslationUnit() != null) {
			currentPath = task.getTranslationUnit().getResource().getFullPath().toPortableString();
		}
		
		InputDialog inputdialog = new InputDialog(
				shell, "enter new source File", "Enter the path to the new SourceFile " +  task.getName(), currentPath, new Vaildator());
		inputdialog.setBlockOnOpen(true);
		inputdialog.open();
		String newName =  inputdialog.getValue();
		IPath path = Path.fromPortableString(newName);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource member = root.findMember(path);
		if (!(member == null || !member.exists() || !(member.getType() == IResource.FILE))) {
			ITranslationUnit tu = CoreModelUtil.findTranslationUnit((IFile) member);
			if (tu != null) {
				task.setTranslationUnit(tu);
				task.setDirty(true);
				CPAcheckerPlugin.getPlugin().fireTasksChanged(Collections.singletonList(task));
			}
		}
	}*/
	/*
	private static class Vaildator implements IInputValidator {
		@Override
		public String isValid(String newText) {
			IPath path = Path.fromPortableString(newText);
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource member = root.findMember(path);
			if (member == null || !member.exists() || !(member.getType() == IResource.FILE)) {
				return("Failed to locate the file " + newText);
			} else {
				ITranslationUnit tu = CoreModelUtil.findTranslationUnit((IFile) member);
				if (tu == null) {
					return "Failed to locate c TranslationUnit " +newText;
				} else {
					return null;
				}
			}
		}
	}*/
}

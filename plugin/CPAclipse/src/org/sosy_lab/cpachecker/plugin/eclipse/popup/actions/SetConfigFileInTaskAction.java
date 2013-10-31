package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;

public class SetConfigFileInTaskAction extends Action {
	private Shell shell;
	private Task task;
	
	public SetConfigFileInTaskAction(Shell shell, Task task) {
		super();
		this.shell = shell;
		this.task = task;
		this.setText("set configuration File");
	}

	@Override
	public void run() {
		IFile result;
		if (task.hasConfigurationFile()) {
			result = CPAclipse.askForConfigFile(shell, task.getConfigFile());
		} else {
			result = CPAclipse.askForConfigFile(shell, null);
		}
		if (result != null) {
			task.setConfigFile( result);
			task.setDirty(true);
			CPAclipse.getPlugin().fireTasksChanged(Collections.singletonList(task));
		}
		
		/* 
		InputDialog inputdialog = new InputDialog(
				shell, "enter new config File", "Enter the path to the new ConfigurationFile " +  task.getName(), currentPath, new Vaildator());
		inputdialog.setBlockOnOpen(true);
		inputdialog.open();
		String newName =  inputdialog.getValue();
		
		IWorkspaceRoot fWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IResource member = fWorkspaceRoot.findMember(newName);
		if (member == null || !member.exists() || !(member.getType() == IResource.FILE)) {
			task.setConfigFile((IFile) member);
			task.setDirty(true);
			CPAcheckerPlugin.getPlugin().fireTasksChanged(Collections.singletonList(task));
		}*/
	}
	/*
	private static class Vaildator implements IInputValidator {
		@Override
		public String isValid(String newText) {
			IWorkspaceRoot fWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IResource member = fWorkspaceRoot.findMember(newText);
			if (member == null || !member.exists() || !(member.getType() == IResource.FILE)) {
				return ("Failed to locate the file " + newText);
			} else {
				return null;
			}
		}
	}*/
}

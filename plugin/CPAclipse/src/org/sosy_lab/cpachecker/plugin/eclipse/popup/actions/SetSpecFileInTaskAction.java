package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.swt.widgets.Shell;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;

public class SetSpecFileInTaskAction extends Action {
	private Shell shell;
	private Task task;
	
	public SetSpecFileInTaskAction(Shell shell, Task task) {
		super();
		this.shell = shell;
		this.task = task;
		this.setText("set specification File");
	}

	@Override
	public void run() {
		IFile result;
		if (task.hasConfigurationFile()) {
			result = CPAclipse.askForSpecFile(shell, task.getConfigFile());
		} else {
			result = CPAclipse.askForSpecFile(shell, null);
		}
		if (result != null) {
			task.setSpecificationFile( result);
			task.setDirty(true);
			CPAclipse.getPlugin().fireTasksChanged(Collections.singletonList(task));
		}
	}
	
	@SuppressWarnings("unused")
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
	}
}

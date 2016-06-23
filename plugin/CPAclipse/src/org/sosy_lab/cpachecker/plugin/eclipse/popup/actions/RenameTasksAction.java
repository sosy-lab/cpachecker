package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Shell;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;

public class RenameTasksAction extends Action {
	private Shell shell;
	private List<Task> toRename;
	
	public RenameTasksAction(Shell shell, List<Task> toRename) {
		super();
		this.shell = shell;
		this.toRename = toRename;
		this.setText("rename selected tasks");
	}

	@Override
	public void run() {
		for (Task t : toRename) {
			// perhaps verify that the name does not exist so far? use the verifier
			InputDialog inputdialog = new InputDialog(shell, "enter new name", "Enter the new Name for the Task " +  t.getName(), t.getName(), null);
			inputdialog.setBlockOnOpen(true);
			inputdialog.open();
			String newName =  inputdialog.getValue();
			t.setName(newName);
			CPAclipse.getPlugin().fireTasksChanged();
		}
	}
}

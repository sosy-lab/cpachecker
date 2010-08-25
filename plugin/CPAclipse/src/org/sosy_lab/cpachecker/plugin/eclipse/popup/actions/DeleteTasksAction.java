package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;
import org.sosy_lab.cpachecker.plugin.eclipse.TasksIO;

public class DeleteTasksAction extends Action {
	private Shell shell;
	private List<Task> toDelete;
	
	public DeleteTasksAction(Shell shell, List<Task> toDelete) {
		super();
		this.shell = shell;
		this.toDelete = toDelete;
		this.setText("delete selected tasks");
	}

	@Override
	public void run() {
		if (MessageDialog.openQuestion(shell, "Deletion Confirmation", "Sure? \nTask cannot be restored, all Tasks have to be saved \n(we dont support undo)")) {
			CPAclipse.getPlugin().removeTasks(toDelete);
			for (Task t : toDelete) {
				try {
					t.getOutputDirectory(false).delete(true, null);
				} catch (CoreException e) {
					CPAclipse.log(e.getStatus());
				}
			}
			TasksIO.saveTasks(CPAclipse.getPlugin().getTasks());
			CPAclipse.getPlugin().fireTasksChanged();
		}
	}
	@SuppressWarnings("unused")
	private boolean deleteDir(File dir) {
		if (!dir.exists()) return true;
		boolean success = true;
		File[] subFiles = dir.listFiles();
		for (int i = 0; i < subFiles.length; i++) {
			if (subFiles[i].exists()) {
				if (subFiles[i].isDirectory()) {
					if (!deleteDir(subFiles[i])) {
						success = false;
					}
				}  else {
					if (!subFiles[i].delete()) {
						success = false;
					}
				}
			}
		}
		if (!dir.delete()) {
			success = false;
		}
		return success;
	}
}

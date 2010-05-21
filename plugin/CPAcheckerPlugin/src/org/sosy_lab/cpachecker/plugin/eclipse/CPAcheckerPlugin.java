package org.sosy_lab.cpachecker.plugin.eclipse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;

public class CPAcheckerPlugin extends AbstractUIPlugin {
	
	// The plug-in ID
	public static final String PLUGIN_ID = "org.sosy_lab.cpachecker.plugin.eclipse";
	
	private static final String listenerId = "org.sosy_lab.cpachecker.plugin.eclipse.listeners";

	private List<ITestListener> listeners;
	private List<Task> tasks = new ArrayList<Task>();
	private static CPAcheckerPlugin instance = null;
	
	public CPAcheckerPlugin() {
		super();
		// instance will be overwritten each time, but this seems to be intended by eclipse people
		instance = this;
		this.addTestListener(new ITestListener() {
			@Override
			public void tasksStarted(int taskCount) {
				System.out.println(taskCount + " Tasks started");
			}
			@Override
			public void tasksFinished() {
				System.out.println("all Tasks finished");
			}
			@Override
			public void tasksChanged(List<Task> t) {
				System.out.println("tasks Changed");
			}
			@Override
			public void taskStarted(Task id) {
				System.out.println("started Task \"" + id.getName() + "\"");
			}
			@Override
			public void taskFinished(Task id,CPAcheckerResult result) {
				String resStr = (result.getResult().equals(CPAcheckerResult.Result.SAFE) 
						? "succeded " : "failed ");
				System.out.println(resStr + " Task \"" + id.getName() + "\"");
			}
			@Override
			public void taskHasPreRunError(Task id, String errorMessage) {
				System.out.println("Task \"" + id.getName() + "\" could not be started: " + errorMessage);
			}
			@Override
			public void tasksChanged() {
				System.out.println("tasks Changed");
			}
		});
	}

	public static CPAcheckerPlugin getPlugin() {
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;
		this.tasks.addAll(TasksIO.loadTasks());
		fireTasksChanged();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		instance = null;
		super.stop(context);
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	public static void runTasks(List<Task> tasks) {
		TaskRunner.run(tasks);
	}

	public List<ITestListener> getListeners() {
		if (listeners == null) {
			// compute the listeners. This is lazy loading (not all plugins have to be loaded at eclipse startup)
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint = registry.getExtensionPoint(listenerId);
			IExtension[] extensions = extensionPoint.getExtensions();
			listeners = new ArrayList<ITestListener>();
			for (int i = 0; i< extensions.length; i++) {
				IConfigurationElement[] elements =
					extensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					try {
						Object listener = elements[j].createExecutableExtension("class");
						if (listener instanceof ITestListener) {
							listeners.add((ITestListener) listener);
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return listeners;
	}
	
	public void addTestListener(ITestListener listener) {
		getListeners().add(listener);
	}
	public void removeTestListener(ITestListener listener) {
		getListeners().remove(listener);
	}
	
	public void fireTasksStarted(final int count) {
		for (final Iterator<ITestListener> iter = getListeners().iterator(); iter.hasNext();) {
			final ITestListener current = iter.next();
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					current.tasksStarted(count);
				}		
				@Override
				public void handleException(Throwable exception) {
					iter.remove(); // listener is likely in some error-state, ignore it
				}
			};
			SafeRunner.run(runnable);
		}
	}
	public void fireTasksFinished() {
		for (final Iterator<ITestListener> iter = getListeners().iterator(); iter.hasNext();) {
			final ITestListener current = iter.next();
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					current.tasksFinished();
				}		
				@Override
				public void handleException(Throwable exception) {
					iter.remove(); // listener is likely in some error-state, ignore it
				}
			};
			SafeRunner.run(runnable);
		}
	}
	public void fireTaskStarted(final Task taskID) {
		for (final Iterator<ITestListener> iter = getListeners().iterator(); iter.hasNext();) {
			final ITestListener current = iter.next();
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					current.taskStarted(taskID);
				}		
				@Override
				public void handleException(Throwable exception) {
					iter.remove(); // listener is likely in some error-state, ignore it
				}
			};
			SafeRunner.run(runnable);
		}
	}
	public void fireTaskFinished(final Task taskID, final CPAcheckerResult results) {
		for (final Iterator<ITestListener> iter = getListeners().iterator(); iter.hasNext();) {
			final ITestListener current = iter.next();
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					current.taskFinished(taskID, results);
				}		
				@Override
				public void handleException(Throwable exception) {
					iter.remove(); // listener is likely in some error-state, ignore it
				}
			};
			SafeRunner.run(runnable);
		}
	}
	public void fireTasksChanged() {
		for (final Iterator<ITestListener> iter = getListeners().iterator(); iter.hasNext();) {
			final ITestListener current = iter.next();
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					current.tasksChanged();
				}		
				@Override
				public void handleException(Throwable exception) {
					iter.remove(); // listener is likely in some error-state, ignore it
				}
			};
			SafeRunner.run(runnable);
		}
	}
	public void fireTasksChanged(final List<Task> changed) {
		for (final Iterator<ITestListener> iter = getListeners().iterator(); iter.hasNext();) {
			final ITestListener current = iter.next();
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					current.tasksChanged(changed);
				}		
				@Override
				public void handleException(Throwable exception) {
					iter.remove(); // listener is likely in some error-state, ignore it
				}
			};
			SafeRunner.run(runnable);
		}
	}
	public List<Task> getTasks() {
		return tasks;
	}
	public void removeTasks(List<Task> t) {
		tasks.removeAll(t);
		fireTasksChanged();
	}

	public void addTask(Task t) {
		this.tasks.add(t);
		fireTasksChanged();
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public void firePreRunError(final Task t, final String errorMessage) {
		for (final Iterator<ITestListener> iter = getListeners().iterator(); iter.hasNext();) {
			final ITestListener current = iter.next();
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					current.taskHasPreRunError(t, errorMessage);
				}		
				@Override
				public void handleException(Throwable exception) {
					iter.remove(); // listener is likely in some error-state, ignore it
				}
			};
			SafeRunner.run(runnable);
		}
	}
	public static ITranslationUnit askForSourceFile(Shell shell, ITranslationUnit initial) {
		String[] extension = {"*.c"};
		// TODO: should we accept other extensions than .c?
        FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.SHEET);
        
        if (initial != null) {
			dialog.setFileName(initial.getResource().getLocation().toPortableString());
		} else {
			dialog.setFileName(CPAcheckerPlugin.getWorkspace().getRoot().getLocation().toPortableString());
		}
        
		dialog.setFilterExtensions(extension);
        String file = dialog.open();
        if (file != null) {
            file = file.trim();
            if (file.length() > 0) {
            	Path p = new Path(file);
            	IFile member = CPAcheckerPlugin.getWorkspace().getRoot().getFileForLocation(p);
            	if (member != null && member.exists()) {
            		ITranslationUnit tu = CoreModelUtil.findTranslationUnit((IFile) member);
        			if (tu != null) {
        				return tu;
        			}
            	}
			}
            // translationUnit could not be found.
            MessageDialog.openError(shell, "Error opening the Source file", "Could not find the c Source file " + file);
            return null;            
        } else {
        	return null;
        }
	}
	public static IFile askForConfigFile(Shell shell, IFile initial) {
		String extension = "*.properties";
        FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.SHEET);
        
        if (initial != null) {
			dialog.setFileName(initial.getLocation().toPortableString());
		} else {
			dialog.setFileName(CPAcheckerPlugin.getWorkspace().getRoot().getLocation().toPortableString());
		}
        
		dialog.setFilterExtensions(new String[] {extension});
        String file = dialog.open();
        if (file != null) {
            file = file.trim();
            if (file.length() > 0) {
            	Path p = new Path(file);
            	IFile member = CPAcheckerPlugin.getWorkspace().getRoot().getFileForLocation(p);
            	if (member != null) {
            		return member;
            	} else {
            		MessageDialog.openError(shell, "Error opening the file", "Could not locate the file " + file +" in the workspace");
            	}
			}
        }
        return null;
	}
}

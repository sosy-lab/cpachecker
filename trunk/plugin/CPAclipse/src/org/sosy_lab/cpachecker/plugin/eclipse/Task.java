/**
 * 
 */
package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

public class Task {
	private static final String DEFAULT_OUTPUT_DIR = "test/output/";
	private String name;
	private ITranslationUnit sourceTranslationUnit = null;
	private IFile configFile = null;
	private IFile specificationFile = null;
	private boolean isDirty = true;
	private Result lastResult = Result.UNKNOWN;

	/**
	 * One of the parameters configFile and source may be null.
	 * 
	 * @param taskName
	 * @param configFile
	 * @param source
	 */
	public Task(String taskName, IFile configFile, ITranslationUnit source, IFile specificationFile) {
		this.name = createUniqueName(taskName);
		this.configFile = configFile;
		this.sourceTranslationUnit = source;
		this.specificationFile = specificationFile;
	}

	public void setLastResult(Result result) {
		this.lastResult = result;
	}

	public Result getLastResult() {
		return this.lastResult;
	}

	public Task(String taskName, IFile configFile) {
		this.name = createUniqueName(taskName);
		this.configFile = configFile;
	}

	public Task(String taskName, ITranslationUnit source) {
		this.name = createUniqueName(taskName);
		this.sourceTranslationUnit = source;
	}

	private static String createUniqueName(String preferredName) {
		List<Task> tasks = CPAclipse.getPlugin().getTasks();
		Set<String> takenNames = new HashSet<String>();
		for (Task t : tasks) {
			takenNames.add(t.getName());
		}
		if (takenNames.contains(preferredName)) {
			int i = 1;
			while (true) {
				if (takenNames.contains(preferredName + " (" + i + ")")) {
					i++;
				} else {
					return preferredName + " (" + i + ")";
				}
			}
		} else {
			return preferredName;
		}
	}
	
	public Configuration createConfig() throws IOException, CoreException, InvalidConfigurationException {
		Configuration.Builder config = Configuration.builder();
		config.loadFromStream(configFile.getContents());
		
		config.setOption("output.path", getOutputDirectory(false).getProjectRelativePath().toOSString());
		config.setOption("output.disable", "false");
		config.setOption("specification", specificationFile.getProjectRelativePath().toOSString());
		config.setOption("rootDirectory", configFile.getProject().getLocation().toOSString());
		
		return config.build();
	}

	public String getName() {
		return name;
	}

	public IFile getConfigFile() {
		return this.configFile;
	}
	public IFile getSpecFile() {
		return this.specificationFile;
	}

	/**
	 * PreRunError == an error that does not allow the task to be run.
	 * 
	 * @return
	 */
	public boolean hasPreRunError() {
		try {
			if (configFile == null || createConfig() == null) {
				return true;
			} else if (specificationFile == null) {
				return true;
			} else if (this.getTranslationUnit() == null) {
				return true;
			}
		} catch (IOException e) {
			return true;
		} catch (CoreException e) {
			return true;
		} catch (InvalidConfigurationException e) {
			return true;
		}
		return false;
	}

	public String getErrorMessage() {
		if (this.configFile == null) {
			return "No configuration file was associated with this task!";
		} else if (specificationFile == null) {
			return "No specification file was associated with this task!";
		} else if (this.getTranslationUnit() == null) {
			return "No Source file was associated with this Task!";
		} else {
			return "";
		}
	}

	public ITranslationUnit getTranslationUnit() {
		return this.sourceTranslationUnit;
	}

	public boolean hasConfigurationFile() {
		return this.configFile != null;
	}
	public boolean hasSpecificationFile() {
		return this.specificationFile != null;
	}

	public void setName(String newName) {
		this.name = createUniqueName(newName);
	}

	public boolean isDirty() {
		return this.isDirty;
	}

	public void setDirty(boolean b) {
		this.isDirty = b;
	}

	public void setConfigFile(IFile member) {
		this.configFile = member;
	}

	public void setTranslationUnit(ITranslationUnit tu) {
		this.sourceTranslationUnit = tu;
	}
	
	/**Returns the File (a Directory) where the results of this Task should be saved.
	 * @return
	 */
	public IFolder getOutputDirectory(boolean create) {
		Properties properties = new Properties();
		if (hasConfigurationFile()) {
		    try {
				properties.load(this.configFile.getContents());
			} catch (IOException e1) {
				CPAclipse.logError("Could not load the configFile", e1);
			} catch (CoreException e1) {
				CPAclipse.logError("Could not load the configFile", e1);
			}
		}
		String projectRelativePath = properties.getProperty("output.path", DEFAULT_OUTPUT_DIR);
		
		IFolder outDir = ResourcesPlugin.getWorkspace().getRoot().getFolder(
				this.configFile.getProject().getFullPath().append(projectRelativePath).append(this.getName()));

		if (create && !outDir.exists()) {
			try {
				outDir.create(true, true, null);
			} catch (CoreException e) {
				CPAclipse.logError("Could not create Result Directory at path " 
						+ outDir.getFullPath().toPortableString(), e);
			}
		}
		return outDir;
	}

	public void setSpecificationFile(IFile file) {
		this.specificationFile = file;
	}
	
}
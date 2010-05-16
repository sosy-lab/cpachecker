package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TasksIO {
	private final static String DEFAULT_TASKS_SAVE_LOCATION = "/CPAcheckerTasks.xml";
	private final static String TOP_NODE_NAME = "CPAcheckerTasks";
	private final static String TASK_NODE_NAME = "Task";
	private final static String TASKNAME_ATTR_NAME = "Name";
	private final static String CONFIG_NODE_NAME = "ConfigFile";
	private final static String SOURCE_NODE_NAME = "SourceFile";
	private final static String PATH_ATTR_NAME = "WorkspaceRelativePath";
	private static final String LOADER_GENERATED_NAME = "loader_generated_name";
	
	private static File getFile(String location) throws URISyntaxException, MalformedURLException {
		IResource root = ResourcesPlugin.getWorkspace().getRoot();
		String newPath = root.getLocation().toPortableString() + location;
		System.out.println(newPath);
		return new File(newPath);
	}
	
	public static List<Task> loadTasks() {
		List<Task> returnList = new ArrayList<Task>();
		File tasksInputFile = null;
		try {
			tasksInputFile = getFile(DEFAULT_TASKS_SAVE_LOCATION);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return Collections.<Task>emptyList();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			return Collections.<Task>emptyList();
		}
		
		if (tasksInputFile == null || !tasksInputFile.exists()) {
			return Collections.<Task>emptyList();
		}
		DocumentBuilder docBuild;
		try {
			docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			Document doc = docBuild.parse(tasksInputFile);
			Node rootNode = doc.getFirstChild();
			assert rootNode.getNodeName().equals(TOP_NODE_NAME);
			// this NodeList contains the Tasks in XML
			NodeList rootChilds = rootNode.getChildNodes();
			for (int i = 0; i < rootChilds.getLength(); i++) {
				Node nd = rootChilds.item(i);
				if (nd.getNodeName() == null || !nd.getNodeName().equals(TASK_NODE_NAME)) {
					continue; // skip this element
				}
				// determine task name
				String taskName = null;
				Node taskNameNode = nd.getAttributes().getNamedItem(TASKNAME_ATTR_NAME);
				if (taskNameNode != null) {
					taskName = taskNameNode.getNodeValue();
				}
				if (taskName == null) taskName = LOADER_GENERATED_NAME;
				// determine config and source file (one of them should be specified)
				NodeList taskChilds = nd.getChildNodes();
				IFile config = null;
				ITranslationUnit source = null;
				for (int j = 0; j < taskChilds.getLength(); j++) {
					Node child = taskChilds.item(j);
					if (child.getNodeName().equals(CONFIG_NODE_NAME)) {
						config = loadConfigFile(child, taskName);
					} else if (child.getNodeName().equals(SOURCE_NODE_NAME)) {
						source = loadSourceFile(child, taskName);
					}
				}
				assert (config != null || source != null) : "Task had neither a configFile nor a sourceFile defined";
				Task t = new TaskRunner.Task(taskName, config, source);
				t.setDirty(false);
				returnList.add(t);
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnList;
	}
	private static ITranslationUnit loadSourceFile(Node sourceNode, String taskName) {
		NamedNodeMap attr = sourceNode.getAttributes();
		Node pathStr = attr.getNamedItem(PATH_ATTR_NAME);
		if (pathStr != null) {
			IPath path = Path.fromPortableString(pathStr.getNodeValue());
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource member = root.findMember(path);
			if (member == null || !member.exists() || !(member.getType() == IResource.FILE)) {
				System.out.println("Failed to locate the file " + pathStr);
				return null;
			} else {
				//ICModel fInput = CoreModel.create(root);
				return CoreModelUtil.findTranslationUnit((IFile) member);
			}
		} else {
			System.out.println("error during load of configFile for Task " + taskName);
			return null;
		}
	}
	private static IFile loadConfigFile(Node configNode, String taskName) {
		NamedNodeMap attr = configNode.getAttributes();
		Node pathStr = attr.getNamedItem(PATH_ATTR_NAME);
		if (pathStr != null) {
			IPath path = Path.fromPortableString(pathStr.getNodeValue());
			IResource member = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (member == null || !member.exists() || !(member.getType() == IResource.FILE)) {
				System.out.println("Failed to locate the file " + pathStr);
				return null;
			} else {
				return (IFile)member;
			}
		} else {
			System.out.println("error during load of configFile for Task " + taskName);
			return null;
		}
	}
	
	public static void saveTasks(List<Task> toSave) {
		try {
		File tasksInputFile;
		tasksInputFile = getFile(DEFAULT_TASKS_SAVE_LOCATION);
		
		if (!tasksInputFile.exists()) {
				tasksInputFile.createNewFile();
			
		}
		// Create a XMLOutputFactory
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		// Create XMLEventWriter
		XMLEventWriter eventWriter = outputFactory
				.createXMLEventWriter(new FileOutputStream(tasksInputFile));
		// Create a EventFactory
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();
		XMLEvent newline = eventFactory.createDTD("\n");
		XMLEvent tab = eventFactory.createDTD("\t");
		// Create and write Start Tag
		eventWriter.add(eventFactory.createStartDocument());
		eventWriter.add(newline);
		// Create config open tag
		StartElement topStartElement = eventFactory.createStartElement("", "", TOP_NODE_NAME);
		eventWriter.add(tab);
		eventWriter.add(topStartElement);
		eventWriter.add(newline);
		// Write the different nodes
		for (Task t: toSave) {
			//rootNode.appendChild(getNodeFromTask(t, doc));
			addNodeFromTask(eventWriter, t);
		}
		eventWriter.add(tab);
		eventWriter.add(eventFactory.createEndElement("", "", TOP_NODE_NAME));
		eventWriter.add(newline);
		eventWriter.add(eventFactory.createEndDocument());
		eventWriter.close();

		
		DocumentBuilder docBuild;
			
			docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			Document doc = docBuild.parse(tasksInputFile);
			Element rootNode = doc.createElement(TOP_NODE_NAME);
			doc.adoptNode(rootNode);
			for (Task t: toSave) {
				rootNode.appendChild(getNodeFromTask(t, doc));
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return;
		} catch (SAXException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		} catch (XMLStreamException e) {
			e.printStackTrace();
			return;
		}
		for (Task t : toSave) {
			t.setDirty(false);
		}
	}
	private static Node getNodeFromTask(Task task, Document doc) {
		 Element taskNode = doc.createElement(TASK_NODE_NAME);
		 taskNode.setAttribute(TASKNAME_ATTR_NAME, task.getName());
		 if (task.hasConfigurationFile()) {
			 String path = task.getConfigFile().getFullPath().toPortableString();
			 Element configNode = doc.createElement(CONFIG_NODE_NAME);
			 configNode.setAttribute(PATH_ATTR_NAME, path);
			 taskNode.appendChild(configNode);
		 }
		 if (task.getTranslationUnit() != null) {
			 IResource resource = task.getTranslationUnit().getResource();
			 if (resource != null) {
				 String path = resource.getFullPath().toPortableString();
				 Element sourceNode = doc.createElement(SOURCE_NODE_NAME);
				 sourceNode.setAttribute(PATH_ATTR_NAME, path);
				 taskNode.appendChild(sourceNode);
			 }
		 }
		 return taskNode;	
	}
	private static void addNodeFromTask(XMLEventWriter eventWriter, Task task) throws XMLStreamException {

		XMLEventFactory eventFactory = XMLEventFactory.newInstance();
		XMLEvent newline = eventFactory.createDTD("\n");
		XMLEvent tab = eventFactory.createDTD("\t");
		// Create Start node
		eventWriter.add(tab);eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "", TASK_NODE_NAME));
		eventWriter.add(eventFactory.createAttribute(TASKNAME_ATTR_NAME, task.getName()));
		eventWriter.add(newline);
		// Create Content
		if (task.hasConfigurationFile()) {
			 String path = task.getConfigFile().getFullPath().toPortableString();
			 eventWriter.add(tab);eventWriter.add(tab);eventWriter.add(tab);
			 eventWriter.add(eventFactory.createStartElement("", "", CONFIG_NODE_NAME));
			 eventWriter.add(eventFactory.createAttribute(PATH_ATTR_NAME, path));
			 eventWriter.add(newline);
			 eventWriter.add(tab);eventWriter.add(tab);eventWriter.add(tab);
			 eventWriter.add(eventFactory.createEndElement("", "", CONFIG_NODE_NAME));
			 eventWriter.add(newline);
		}
		if (task.getTranslationUnit() != null) {
			 IResource resource = task.getTranslationUnit().getResource();
			 if (resource != null) {
				 String path = resource.getFullPath().toPortableString();
				 eventWriter.add(tab);eventWriter.add(tab);eventWriter.add(tab);
				 eventWriter.add(eventFactory.createStartElement("", "", SOURCE_NODE_NAME));
				 eventWriter.add(eventFactory.createAttribute(PATH_ATTR_NAME, path));
				 eventWriter.add(newline);
				 eventWriter.add(tab);eventWriter.add(tab);eventWriter.add(tab);
				 eventWriter.add(eventFactory.createEndElement("", "", SOURCE_NODE_NAME));
				 eventWriter.add(newline);
			 }
		}
		eventWriter.add(tab);eventWriter.add(tab);
		eventWriter.add(eventFactory.createEndElement("", "", TASK_NODE_NAME));
		eventWriter.add(newline);
	}

}

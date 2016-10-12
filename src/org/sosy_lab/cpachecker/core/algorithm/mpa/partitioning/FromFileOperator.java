/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.InfinitePropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning.PartitioningStatus;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

@Options(prefix = "analysis.mpa.partition")
@SuppressWarnings("unused")
public class FromFileOperator extends PartitioningBudgetOperator {

  private final ImmutableList<ImmutableSet<String>> parsedProperties;
  @Option(secure = true,
      description = "XML File which describes a static partitioning to be used with "
          + "FromFileOperator. Look into FromFileOperator.java for an example.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path partitioningFile = null;

  public FromFileOperator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    pConfig.inject(this);

    try {
      parsedProperties = parseXML();
    } catch (ParserConfigurationException | IOException | SAXException
        | XPathExpressionException e) {
      throw new IllegalStateException("Unable to parse partitioning XML file!", e);
    }
  }

  private ImmutableList<ImmutableSet<String>> parseXML()
      throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

    checkNotNull(partitioningFile, "You must specify a partitioningFile to use the "
        + "FromFileOperator");

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setIgnoringComments(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(partitioningFile.toFile());
    XPath xPath = XPathFactory.newInstance().newXPath();

    Builder<ImmutableSet<String>> listBuilder = ImmutableList.builder();

    String partitionsXpath = "/partitioning/partition";
    NodeList partitionNodes = (NodeList) xPath.compile(partitionsXpath)
        .evaluate(doc, XPathConstants.NODESET);

    for (int i = 0; i < partitionNodes.getLength(); i++) {
      Node partitionNode = partitionNodes.item(i);
      String propertiesXpath = "./property";
      NodeList propertyNodes = (NodeList) xPath.compile(propertiesXpath)
          .evaluate(partitionNode, XPathConstants.NODESET);
      ImmutableSet.Builder<String> setBuilder = ImmutableSet.builder();
      for (int j = 0; j < propertyNodes.getLength(); j++) {
        Node propertyNode = propertyNodes.item(j);
        setBuilder.add(propertyNode.getTextContent());
      }
      listBuilder.add(setBuilder.build());
    }

    return listBuilder.build();
  }

  private ImmutableList<ImmutableSet<Property>> partitionMatchingFile(Set<Property> pToCheck) {
    Builder<ImmutableSet<Property>> listBuilder = ImmutableList.builder();
    Set<Property> addedProps = new LinkedHashSet<>();

    for (ImmutableSet<String> propset : parsedProperties) {
      ImmutableSet.Builder<Property> setBuilder = ImmutableSet.builder();
      for (String propstr : propset) {
        Optional<Property> property = pToCheck.stream()
            .filter(prop -> propstr.equals(prop.toString()))
            .findFirst();
        if (property.isPresent()) {
          setBuilder.add(property.get());
          addedProps.add(property.get());
        } else {
          logger.log(Level.WARNING, "Property %s specified in file not found, skipping!", propstr);
        }
      }
      ImmutableSet<Property> propSet = setBuilder.build();
      if (!propSet.isEmpty()) {
        listBuilder.add(propSet);
      }
    }

    SetView<Property> unPartitionedProps = Sets.difference(pToCheck, addedProps);
    if (!unPartitionedProps.isEmpty()) {
      logger.log(Level.WARNING, "Properties %s are missing a corresponding file entry, "
          + "placing them into an extra partition!", unPartitionedProps);
      listBuilder.add(ImmutableSet.copyOf(unPartitionedProps));
    }

    return listBuilder.build();
  }

  @Override
  public Partitioning partition(
      Partitioning pLastCheckedPartitioning,
      Set<Property> pToCheck, Set<Property> pExpensiveProperties,
      Comparator<Property> pPropertyExpenseComparator)
      throws PartitioningException {

    ImmutableList<ImmutableSet<Property>> partitionedProps = partitionMatchingFile(pToCheck);

    return pLastCheckedPartitioning.getStatus() == PartitioningStatus.NONE
           ? create(PartitioningStatus.PREDEFINED, getPropertyBudgetingOperator(),
        getPartitionBudgetingOperator(), partitionedProps)
           : create(PartitioningStatus.BREAK, InfinitePropertyBudgeting.INSTANCE,
               getPartitionBudgetingOperator(),
               ImmutableList.of());
  }

}

/* Example XML:

<?xml version="1.0"?>

<partitioning>
    <partition>
        <property>foo.spc</property>
        <property>bar.spc</property>
    </partition>
    <partition>
        <property>baz.spc</property>
    </partition>
</partitioning>

*/
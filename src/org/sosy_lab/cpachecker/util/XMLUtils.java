// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XMLUtils {

  public static DocumentBuilderFactory getSecureDocumentBuilderFactory(boolean disableDTD)
      throws ParserConfigurationException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    // protect against XML eXternal Entity injection (XXE) following the recommendations on
    // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
    // see https://xerces.apache.org/xerces-j/features.html
    // and http://xerces.apache.org/xerces2-j/features.html for features

    // disable DTD, only works for Xerces2
    if (disableDTD) {
      docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    } else {
      docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    }

    // ignore the external DTD completely
    docFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    // Do not include external entitites
    docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    // Do not include external parameter entities or the external DTD subset.
    docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    // Add these as per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
    docFactory.setXIncludeAware(false);
    docFactory.setExpandEntityReferences(false);
    // ensure that central mechanism for safeguarding XML processing
    // "Feature for Secure Processing (FSP)" is enabled
    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

    return docFactory;
  }

  public static SAXParserFactory getSecureSaxParserFactory()
      throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
    SAXParserFactory saxFactory = SAXParserFactory.newInstance();

    // protect against XML eXternal Entity injection (XXE) following the recommendations on
    // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
    // see https://xerces.apache.org/xerces-j/features.html
    // and http://xerces.apache.org/xerces2-j/features.html for features

    // disable DTD, only works for Xerces2
    saxFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

    // ignore the external DTD completely
    saxFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    // Do not include external entitites
    saxFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    // Do not include external parameter entities or the external DTD subset.
    saxFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    // Add these as per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
    saxFactory.setXIncludeAware(false);
    // ensure that central mechanism for safeguarding XML processing
    // "Feature for Secure Processing (FSP)" is enabled
    saxFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

    return saxFactory;
  }

  public static TransformerFactory getSecureTransformerFactory() {
    TransformerFactory tf = TransformerFactory.newInstance();

    // protect against XML eXternal Entity injection (XXE) following the recommendations on
    // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
    tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

    return tf;
  }
}

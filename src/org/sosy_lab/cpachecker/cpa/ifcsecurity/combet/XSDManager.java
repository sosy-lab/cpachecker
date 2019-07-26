package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

public class XSDManager<S> {
  private Schema schema;

  public XSDManager(File schema, File extension) {
    try {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      if (extension != null) {
        this.schema =
            schemaFactory
                .newSchema(new Source[] {new StreamSource(schema), new StreamSource(extension)});
      } else {
        this.schema = schemaFactory.newSchema(schema);
      }
    } catch (SAXException e) {

    }
  }

  @SuppressWarnings("unchecked")
  public S readXML(File xml) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Case.class);

      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      unmarshaller.setSchema(schema);
      S temp = (S) unmarshaller.unmarshal(xml);
      return temp;
    } catch (UnmarshalException e) {

    } catch (JAXBException e) {

    }
    return null;
  }

  public void writeXML(S tree, File xml) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Case.class);

      Marshaller marschaller = jaxbContext.createMarshaller();
      marschaller.marshal(tree, xml);

      return;
    } catch (UnmarshalException e) {

    } catch (JAXBException e) {

    }
  }

}

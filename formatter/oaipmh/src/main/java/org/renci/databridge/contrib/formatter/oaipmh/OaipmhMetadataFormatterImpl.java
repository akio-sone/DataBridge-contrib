package org.renci.databridge.contrib.formatter.oaipmh;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.StringReader;
import java.io.Serializable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.bind.JAXBElement;

import org.renci.databridge.formatter.JaxbMetadataFormatter;
import org.renci.databridge.formatter.FormatterException;

import org.renci.databridge.persistence.metadata.MetadataObject;
import org.renci.databridge.persistence.metadata.CollectionTransferObject;
import org.renci.databridge.persistence.metadata.FileTransferObject;
import org.renci.databridge.persistence.metadata.VariableTransferObject;

import org.renci.databridge.contrib.formatter.codebook.CodeBook;
import org.renci.databridge.contrib.formatter.codebook.CodeBookMetadataFormatterImpl;

/**
 * Implementation for OAI-PMH document. An OAI-PMH document is a container that holds a variable number of metadata objects. Currently supports only embeded CodeBook record type.
 *
 * This formatter depends on CodeBookMetadataFormatterImpl for its CodeBook unmarshalling and processing.
 *
 * @author mrshoffn
 */
public class OaipmhMetadataFormatterImpl extends JaxbMetadataFormatter {

  public OaipmhMetadataFormatterImpl () {
    // logger may be replaced later if parent calls superclass setLogger method
    setLogger (Logger.getLogger ("org.renci.databridge.contrib.formatter.oaipmh"));
  }

  @Override
  public List<MetadataObject> format (byte [] bytes) throws FormatterException {
    String metadataString = new String (bytes);
    if (this.logger.isLoggable (Level.FINER) && metadataString != null) {
      String stringToLog = metadataString;
      // truncate log string if too long
      if (stringToLog.length () > 2048) {
        stringToLog = metadataString.substring (2048) + " [truncated to log]";
      }
      this.logger.log (Level.FINE, "bytes: '" + stringToLog + "'");
    }

    OAIPMHtype ot = unmarshal (metadataString, OAIPMHtype.class, OAIPMHtype.class, CodeBook.class);

    this.logger.log (Level.FINE, "Processing an OAIPMHtype.");
    List<MetadataObject> metadataObjects = new ArrayList<MetadataObject> ();
    extractOAIPMHtype (ot, metadataObjects);

    return metadataObjects;

  }
 
  /**
   * @param metadataObjects is modified by this method.
   */
  protected void extractOAIPMHtype (OAIPMHtype oaipmhType, List<MetadataObject> metadataObjects) throws FormatterException {

    ListRecordsType lrt = oaipmhType.getListRecords ();

    // @todo A bit gross, but JAXB returns an "empty" if asked to parse by specific type and the type does not exist in the input.
    List<RecordType> rtList = null;
    try {     
      rtList = lrt.getRecord ();
    } catch (NullPointerException npe) {
      this.logger.log (Level.FINE, "Got an NPE, so there is probably no OAIPMH content object in the input document.");
      return;
    }

    // for delegating codebook extration
    CodeBookMetadataFormatterImpl cbmfi = new CodeBookMetadataFormatterImpl ();
    cbmfi.setLogger (this.logger);

    Iterator<RecordType> i = rtList.iterator ();
    while (i.hasNext ()) {

      RecordType r = i.next ();
      HeaderType h = r.getHeader ();

      MetadataType mt = r.getMetadata ();
      Object any = mt.getAny ();
      if (!(any instanceof CodeBook)) {
        throw new FormatterException (any.getClass ().getName () + " is unknown ANY element for OAI-PMH. A schema for this element probably needs to added to the build for this formatter.");
      }

      CodeBook cb = (CodeBook) any;
      MetadataObject mo = cbmfi.extractCodeBook (cb); 
      metadataObjects.add (mo);

    }

  }

}

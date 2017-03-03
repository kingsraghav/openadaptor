/*
 Copyright (C) 2001 - 2010 The Software Conservancy as Trustee. All rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in the
 Software without restriction, including without limitation the rights to use, copy,
 modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 and to permit persons to whom the Software is furnished to do so, subject to the
 following conditions:

 The above copyright notice and this permission notice shall be included in all 
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Nothing in this notice shall be deemed to grant any rights to trademarks, copyrights,
 patents, trade secrets or any other intellectual property of the licensor or any
 contributor except as expressly stated herein. No patent license is granted separate
 from the Software, for code that you delete from the Software, or for combinations
 of the Software with other software or hardware.
*/

package org.openadaptor.auxil.connector.jndi;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openadaptor.auxil.orderedmap.IOrderedMap;
import org.openadaptor.auxil.orderedmap.OrderedHashMap;
import org.openadaptor.core.Component;
import org.openadaptor.core.IReadConnector;
import org.openadaptor.core.IEnrichmentReadConnector;
import org.openadaptor.core.exception.RecordException;
import org.openadaptor.core.exception.ValidationException;

/**
 * Abstract base class for connectors which use JNDI searches.
 * 
 * Holds the mandatory JNDISearch property (validates it is set).
 * 
 * Also contains the IEnhancementProcessor logic for JNDI data sources.
 * 
 * @author Eddy Higgins, Andrew Shire, Kris Lachor
 * @see JNDIReadConnector
 * @see JNDIConnection
 * @see JNDISearch
 */

/* 
These comments were copied from legacy JNDIEnhancementProcessor (of which most of the functionality 
has been moved here).
Done: Needed to enrich JndiEnhancementProcessor:
+ search root driven by dn in incomingKeyMap (when present in values)
- issue warning if overriding explicit JNDISearch searchBases;
+ filter uses values in incomingKeyMap
- derive filter using actual incoming values and AND it with any explicit JNDISearch filter;
+ explicitly ask for just the attributes in outgoingKeyMap keys (performance and operational attributes)
- merge outgoingKeyMap keys with list of attributes directly specified on JNDISearch;
+ to get dn to appear in results it must be set on JNDISearch.returnedDNAttributeName,
and
+ refactor: merge JNDIExistenceProcessor into it.
+ refactor: make it cleaner to subclass with just a different AbstractJNDIReader.
*/

/* TODO: Need to enrich JNDISearch:
+ schema: allow explicit statement of which attributes are multi-valued (if missing treat all as multi-valued).
*/

public abstract class AbstractJNDIReadConnector extends Component implements IReadConnector, IEnrichmentReadConnector
{
  private static final Log log = LogFactory.getLog(AbstractJNDIReadConnector.class);

  // internal state:
  /**
   * Flag to indicate whether or not the search has already run.
   */
  protected boolean _searchHasExecuted = false;
  
  protected boolean enrichmentProcessorMode = false;
  
  /** To store internal state when used as IEnrichmentReadConnector */
  protected IOrderedMap inputParameters = null;


  // bean properties
  
  protected JNDISearch search;

  protected String recordKeyUsedAsSearchBase = null;

  protected String recordKeySetByExistence = null;

  protected String recordKeySetToMatchCount = null;

  protected Map incomingMap;
  
  protected Map outgoingMap;
  
  protected String[] outgoingKeys; // derived from outgoingMap bean property
  
  protected String[] configDefinedSearchAttributes; // derived from attributes property of embedded search property
  
  protected String configDefinedSearchFilter; // derived from filter property of embedded search property
  
  protected String valueIfExists = "true";

  protected String valueIfDoesNotExist = "false";
  

  /**
   * Constructor.
   */
  protected AbstractJNDIReadConnector() {
  }

  /**
   * Constructor.
   */
  protected AbstractJNDIReadConnector(String id) {
    super(id);
  }
 
  /**
   * @return the <code>search</code>.
   */
  public JNDISearch getSearch() {
    return search;
  }

  /**
   * Sets the <code>search</code>.
   * 
   * @param search the <code>search</code>.
   */
  public void setSearch(JNDISearch search) {
    this.search = search;
  }

  /**
   * Always returns null.
   * 
   * @return null
   * @see org.openadaptor.core.IReadConnector#getReaderContext()
   */
  public Object getReaderContext() {
    return null;
  }
  
  /**
   * Takes no action.
   * 
   * @see org.openadaptor.core.IReadConnector#setReaderContext(Object)
   */
  public void setReaderContext(Object context) {
  }

  public void setRecordKeyUsedAsSearchBase(String recordKeyUsedAsSearchBase) {
    this.recordKeyUsedAsSearchBase = recordKeyUsedAsSearchBase;
    this.enrichmentProcessorMode = true;
  }

  public String getRecordKeyUsedAsSearchBase() {
    return recordKeyUsedAsSearchBase;
  }
    
  public void setRecordKeySetByExistence(String recordKeySetByExistence) {
    this.recordKeySetByExistence = recordKeySetByExistence;
    this.enrichmentProcessorMode = true;
  }

  public String getRecordKeySetByExistence() {
    return recordKeySetByExistence;
  }
  
  public void setRecordKeySetToMatchCount(String recordKeySetToMatchCount) {
    this.recordKeySetToMatchCount = recordKeySetToMatchCount;
    this.enrichmentProcessorMode = true;
  }

  public String getRecordKeySetToMatchCount() {
    return recordKeySetToMatchCount;
  }
  
  public void setIncomingMap(Map incomingMap) {
    this.incomingMap = incomingMap;
    this.enrichmentProcessorMode = true;
  }

  public Map getIncomingMap() {
    return incomingMap;
  }
   
  public void setOutgoingMap(Map outgoingMap) {
    this.outgoingMap = outgoingMap;
  }

  public Map getOutgoingMap() {
    return outgoingMap;
  }
  
  public void setEnrichmentProcessorMode(boolean enhancementProcessorMode) {
    this.enrichmentProcessorMode = enhancementProcessorMode;
  }


  /**
   * Checks that the mandatory properties have been set
   *
   * @param exceptions list of exceptions that any validation errors will be appended to
   */
  public void validate(List exceptions) {
    if (search == null) {
      exceptions.add(new ValidationException("search property not set", this));
    }

    if(enrichmentProcessorMode){
      // relied on to allow this class to be subclassed by code that repeats the following with a different reader:

      // Enforce preconditions:
      if ((incomingMap == null || incomingMap.size() < 1) && (recordKeyUsedAsSearchBase == null)) {
        log.warn("Must provide an incomingKeyMap and/or set recordKeyUsedAsSearchBase.");
        exceptions.add(new ValidationException("Must provide an incomingKeyMap and/or set recordKeyUsedAsSearchBase.",
            this));
      }
      if ((outgoingMap == null || outgoingMap.size() < 1) && (recordKeySetByExistence == null) && (recordKeySetToMatchCount == null)) {
        log.warn("Must provide an outgoingKeyMap and/or set recordKeyUsedForExistence or recordKeySetToMatchCount.");
        exceptions.add(new ValidationException("Must provide an outgoingKeyMap and/or set recordKeyUsedForExistence.",
            this));
      }

      String[] bases = search.getSearchBases();
      if (recordKeyUsedAsSearchBase == null) {
        // Must provide a searchBase in the embedded JNDISearch:
        if (bases == null || bases.length < 1) {
          log.warn("Must provide a non-empty search.searchBases (or provide recordKeyUsedAsSearchBase).");
          exceptions.add(new ValidationException(
              "Must provide a non-empty search.searchBases (or provide recordKeyUsedAsSearchBase).", this));
        }
      } else {
        // Must not provide a searchBase in the embedded JNDISearch as well:
        if (bases != null && bases.length > 0) {
          log.warn("Must provide either a search.searchBases or a recordKeyUsedAsSearchBase (not both!).");
          exceptions.add(new ValidationException(
              "Must provide either a search.searchBases or a recordKeyUsedAsSearchBase (not both!).", this));
        }
        // Must provide an incomingMap and/or a search filter in the embedded JNDISearch (eg. "(objectclass=*"))
        String filter = search.getFilter();
        if ((incomingMap == null || incomingMap.size() < 1) && (filter == null || filter.length() == 0)) {
          log.warn("Must provide an incomingMap and/or a search.filter.");
          exceptions.add(new ValidationException("Must provide an incomingMap and/or a search.filter.", this));
        }
      }

      // Initialise derived member variables:
      if (outgoingMap == null || outgoingMap.size() < 1) {
        outgoingKeys = new String[] {};
      } else {
        outgoingKeys = (String[]) outgoingMap.keySet().toArray(new String[] {});
      }

      configDefinedSearchAttributes = search.getAttributes();
      if (configDefinedSearchAttributes == null) {
        configDefinedSearchAttributes = new String[] {};
      }

      configDefinedSearchFilter = search.getFilter();
      if (configDefinedSearchFilter != null && !configDefinedSearchFilter.startsWith("(")
          && !configDefinedSearchFilter.endsWith(")")) {
        configDefinedSearchFilter = "(" + configDefinedSearchFilter + ")";
      }

      // Setup the attributes we're interested in:
      // outgoingMap keys combined with any config defined search attributes
      int attribsSize = outgoingKeys.length + configDefinedSearchAttributes.length;

      String[] attributeNames = new String[attribsSize];

      for (int i = 0; i < outgoingKeys.length; i++) {
        attributeNames[i] = outgoingKeys[i];
      }

      for (int i = 0; i < configDefinedSearchAttributes.length; i++) {
        attributeNames[i + outgoingKeys.length] = configDefinedSearchAttributes[i];
      }

      search.setAttributes(attributeNames);
    }
  }
  
  /**
   * Sets parameters for customisation of the query.
   * 
   * @see IEnrichmentReadConnector#next(IOrderedMap, long)
   */
  public Object[] next(IOrderedMap inputParameters, long timeout) {
    this.inputParameters = inputParameters;
    return next(timeout);
  }


  /**
   * Ask the enrichment connection for the enrichment data that matches
   * the incoming record (i.e. perform the enrichment lookup).
   * 
   * @return enrichment data for the current incoming record
   * @throws Exception for example if there was a connectivity problem
   */
  protected abstract IOrderedMap[] getMatches() throws Exception;
  

  public Object[] processOrderedMap(IOrderedMap orderedMap) throws RecordException {
    Object[] result = null;

    tailorSearchToThisRecord(orderedMap);

    try {
      IOrderedMap[] matches = getMatches(); //This should now have an array of IOrderedMaps to work with
      if (matches == null) {
        log.debug("Enrichment search returned no results");

        // So simply pass original data through un-enhanced:
        result = new IOrderedMap[1];
        result[0] = new OrderedHashMap();

        // And set existence flag to does not exist:
        if (recordKeySetByExistence != null) {
          ((IOrderedMap) result[0]).put(recordKeySetByExistence, valueIfDoesNotExist);
        }
        
        // And set match count flag to zero:
        if (recordKeySetToMatchCount != null) {
        	((IOrderedMap) result[0]).put(recordKeySetToMatchCount, "0");
        }
      } else {
        int size = matches.length;
        log.debug("Enrichment search returned " + size + " results");
        result = new IOrderedMap[size];
        for (int i = 0; i < size; i++) {
          IOrderedMap outgoing = new OrderedHashMap();

          // Enrich outgoing record according to outgoingMap:
          if (outgoingMap != null && outgoingMap.size() > 0) {
            Iterator outgoingMapIterator = outgoingMap.entrySet().iterator();
            while (outgoingMapIterator.hasNext()) {
              Map.Entry entry = (Map.Entry) outgoingMapIterator.next();
              Object outKeyValue = matches[i].get(entry.getKey());
              if (outKeyValue == null) {
                // attribute has a null value, only write it if that is because attribute is present:
                if (matches[i].containsKey(entry.getKey())) {
                  outgoing.put(entry.getValue(), outKeyValue);
                }
              } else {
                // attribute has a real value, so use it (may be an array of strings):
                if (outKeyValue instanceof Object[]) {
                  // If it is an array of objects, convert it to an array of strings:
                  Object[] outKeyValueArray = (Object[]) outKeyValue;
                  String[] outKeyStringArray = new String[outKeyValueArray.length];
                  for (int k=0; k<outKeyValueArray.length; k++) {
                    outKeyStringArray[k] = outKeyValueArray[k].toString(); 
                  }
                  outgoing.put(entry.getValue(), outKeyStringArray);
                } else {
                  // Otherwise simply convert object to a string:
                  outgoing.put(entry.getValue(), outKeyValue.toString());
                }
              }
            }
          }

          // And set existence flag to exists:
          if (recordKeySetByExistence != null) {
            outgoing.put(recordKeySetByExistence, valueIfExists);
          }

          // And set match count flag to number of matches (set in each matching record):
          if (recordKeySetToMatchCount != null) {
            outgoing.put(recordKeySetToMatchCount, Integer.toString(size));
          }

          log.debug("OutputMap: " + outgoing);
          result[i] = outgoing;
        }
      }
    } catch (Exception e) {
      log.fatal(e);
      log.info("RecordException of " + e.getMessage());
      if (log.isDebugEnabled())
        e.printStackTrace();
      throw new RecordException(e.getMessage(), e);
    }
    return result;
  }
  
  
  public void tailorSearchToThisRecord(IOrderedMap orderedMapRecord) throws RecordException {
    // Use a dynamic search base from the incoming record?
    if (recordKeyUsedAsSearchBase != null) {
      Object incomingBase = orderedMapRecord.get(recordKeyUsedAsSearchBase);
      if ((incomingBase == null) || !(incomingBase instanceof CharSequence)) {
        log.warn("Empty search base produced: recordKeyUsedAsSearchBase missing from this record: " + orderedMapRecord);
        throw new RecordException("Empty search base produced: recordKeyUsedAsSearchBase missing from this record.");
      }
      if (!(incomingBase instanceof String)) {
        incomingBase = incomingBase.toString();
      }
      search.setSearchBases(new String[] { (String) incomingBase });
    }

    // Set up the search filter to use all incomingMap values (GDS attribute names) with
    // any corresponding record values (if null then use "*").
    StringBuffer searchFilter = new StringBuffer();
    if (incomingMap != null) {
      if (incomingMap.size() > 1) {
        searchFilter.append("(&");
      }
      Iterator incomingMapIterator = incomingMap.entrySet().iterator();
      while (incomingMapIterator.hasNext()) {
        Map.Entry entry = (Map.Entry) incomingMapIterator.next();
        Object recordValue = orderedMapRecord.get(entry.getKey());
        if (recordValue != null) {
          searchFilter.append("(").append(entry.getValue()).append("=").append(recordValue).append(")");
        }
      }
      if (incomingMap.size() > 1) {
        searchFilter.append(")");
      }
    }
    // Combine it with any config defined search filter (e.g. it might restrict objectclass)
    if (configDefinedSearchFilter != null && configDefinedSearchFilter.length() > 0) {
      if (incomingMap != null) {
        searchFilter.insert(0, "(&");
      }
      searchFilter.append(configDefinedSearchFilter);
      if (incomingMap != null) {
        searchFilter.append(")");
      }
    }
    // Sanity check (don't want to do unconstrained searches):
    if (searchFilter.length() == 0) {
      log.warn("Empty search filter produced: probably missing incomingMap keys in record: " + orderedMapRecord);
      throw new RecordException("Empty search filter produced: probably missing incomingMap keys in tbis record.");
    }
    // Set this updated filter:
    search.setFilter(searchFilter.toString());
  }
}

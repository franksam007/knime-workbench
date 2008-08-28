/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 */
package org.knime.workbench.ui.metanodes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.KNIMEUIPlugin;
/**
 * Loads all meta node template repository items from plugins store 
 * and provides them as input.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public final class MetaNodeTemplateRepositoryManager {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MetaNodeTemplateRepositoryManager.class); 
    
    private List<MetaNodeTemplateRepositoryItem>m_items;
    
    private static final String CFG_ITEM = "MetaNodeTemplate";
    private static final String CFG_TYPE = "MetaNodeTemplates";

    // the key for the preference store
    private static final String METANODE_TEMPLATE_REPOSITORY_KEY 
        = "org.knime.metanode.template.repository";
    
    // TODO: store in user workspace
    // the default location for the meta node template repository
    private static final String DEFAULT_REPOSITORY_DIR 
        = KNIMEConstants.getKNIMEHomeDir() + "/.metanodetemplates";
    
    
    // the actual file
    private static final String METANODE_TEMPLATE_REPOSITORY;
        
    static {
        KNIMEUIPlugin.getDefault().getPreferenceStore().setDefault(
                METANODE_TEMPLATE_REPOSITORY_KEY, DEFAULT_REPOSITORY_DIR);
        METANODE_TEMPLATE_REPOSITORY = KNIMEUIPlugin.getDefault()
            .getPreferenceStore().getString(METANODE_TEMPLATE_REPOSITORY_KEY);
         
    }
    
    
    private static MetaNodeTemplateRepositoryManager instance;
    
    private static WorkflowManager workflowmanager;
    
    private MetaNodeTemplateRepositoryManager() {
        workflowmanager = WorkflowManager.ROOT.createAndAddProject();
        load();
        instance = this;
    }

    /**
     * 
     * @return the singleton instance of this manager (already loaded).
     */
    public static MetaNodeTemplateRepositoryManager getInstance() {
        if (instance == null) {
            instance = new MetaNodeTemplateRepositoryManager();
            
        }
        return instance;
    }
    
    /**
     * 
     * @return the {@link MetaNodeTemplateRepositoryItem}s
     */
    public List<MetaNodeTemplateRepositoryItem> getTemplates() {
        return m_items;
    }

    /**
     * 
     * @param item adds the item to the list of items
     */
    public void addItem(final MetaNodeTemplateRepositoryItem item) {
        m_items.add(item);
    }

    /**
     * 
     * @return the workflow manager to which all meta nodes are added
     */
    public WorkflowManager getWorkflowManager() {
        return workflowmanager;
    }

    /**
     * 
     * @param item to be removed from the list
     */
    public void removeItem(final MetaNodeTemplateRepositoryItem item) {
        m_items.remove(item);
    }
    
    /**
     * Creates a new meta node template by copying the meta node from the source
     * {@link WorkflowManager} to the this {@link WorkflowManager}. 
     * @param name name of the template
     * @param source WorkflowManager from which the MetaNode is copied
     * @param nodeID the {@link NodeID} of the meta node to be copied
     */
    public void createMetaNodeTemplate(final String name, 
            final WorkflowManager source, final NodeID nodeID) {
        NodeID newNode = workflowmanager.copy(source, new NodeID[] {nodeID})[0];
        MetaNodeTemplateRepositoryItem item 
            = new MetaNodeTemplateRepositoryItem(
                name, newNode);
        m_items.add(item);
    }
    
    /**
     * 
     * @param item the template
     * @return the meta node represented by the template 
     *  (referenced by the NodeID)
     */
    public WorkflowManager getSubworkflow(
            final MetaNodeTemplateRepositoryItem item) {
        return (WorkflowManager)workflowmanager.getNodeContainer(
                item.getNodeID());
        
    }
    
    
    
    private void load() {
        m_items = new ArrayList<MetaNodeTemplateRepositoryItem>();
        try {
            FileReader fileReader = new FileReader(getMetaNodeTemplateStore());
            XMLMemento memento = XMLMemento.createReadRoot(fileReader);
            workflowmanager = loadWorkflowManager();
            // if file is not there create new one
            if (workflowmanager == null) {
                workflowmanager = WorkflowManager.ROOT.createAndAddProject();
                return;
            }
            // load WorkflowManager from file
            IMemento[] templates = memento.getChildren(CFG_ITEM);
            for (IMemento template : templates) {
                MetaNodeTemplateRepositoryItem item 
                    = new MetaNodeTemplateRepositoryItem();
                item.loadFrom((XMLMemento)template);
                item.updateNodeID(workflowmanager.getID());
                m_items.add(item);
            }
        } catch (FileNotFoundException fnfe) {
            // file not found -> no meta nodes defined yet 
            // -> no problem (ignoring that)
            // LOGGER.error("Error during load of meta node templates: ", fnfe);
            LOGGER.debug("No meta node templates found at " 
                    +  getMetaNodeTemplateStore().getPath());
        } catch (WorkbenchException we) {
            LOGGER.error("Error during load of meta node templates: ", we);
        } 
    }
    
    /**
     * Saves the workflowManager to the directory specified by 
     *  {@link #METANODE_TEMPLATE_REPOSITORY} and all 
     *  {@link MetaNodeTemplateRepositoryItem}s to the preference store 
     *  (.plugins/org.knime.workbench.ui).
     */
    public void save() {
        // save workflow manager to dedicated dir
        try {
            workflowmanager.save(new File(METANODE_TEMPLATE_REPOSITORY), 
                    new ExecutionMonitor(), false);
            XMLMemento memento = XMLMemento.createWriteRoot(CFG_TYPE);
            for (MetaNodeTemplateRepositoryItem item : m_items) {
                item.saveTo((XMLMemento)memento.createChild(CFG_ITEM));   
            }
            FileWriter writer = new FileWriter(getMetaNodeTemplateStore());
            memento.save(writer);
        } catch (IOException e) {
            LOGGER.error("Couldn't save meta node templates. " 
                    + "Templates will be lost", e);
        } catch (CanceledExecutionException e) {
            LOGGER.error("Couldn't save meta node templates. " 
                    + "Templates will be lost", e);
        }
    }

    
    private WorkflowManager loadWorkflowManager() {
        try {
            WorkflowManager wfm = WorkflowManager.load(
                    new File(METANODE_TEMPLATE_REPOSITORY), 
                    new ExecutionMonitor()).getWorkflowManager();
            return wfm;
        } catch (IOException e) {
            LOGGER.error("Couldn't load WorkflowManager. " 
                    + "Old templates will be lost!", e);
        } catch (InvalidSettingsException e) {
            LOGGER.error("Couldn't load WorkflowManager. " 
                    + "Old templates will be lost!", e);
        } catch (CanceledExecutionException e) {
            LOGGER.error("Couldn't load WorkflowManager. " 
                    + "Old templates will be lost!", e);
        }
        return null;
    }
    
    
    private File getMetaNodeTemplateStore() {
        return KNIMEUIPlugin.getDefault()
            .getStateLocation().append("MetaNodeTemplates.xml").toFile();
    }
    
    
}
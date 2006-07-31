/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   16.03.2005 (georg): created
 */
package de.unikn.knime.workbench.repository.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Realizes a root node. This has no parent (<code>null</code>) and can't be
 * added as a child to other containers.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class Root extends AbstractContainerObject {
    public Root() {
    }

    /**
     * This returns the '/' as ID for the root repository element.
     * 
     * @see de.unikn.knime.workbench.repository.model.IRepositoryObject#getID()
     */
    @Override
    public String getID() {
        return "/";
    }

    /**
     * @return always <code>null</code>
     * @see de.unikn.knime.workbench.repository.model.AbstractRepositoryObject#
     *      getParent()
     */
    @Override
    public IContainerObject getParent() {
        return null;
    }

    /**
     * Throws a <code>UnsupportedOperationException</code>.
     * 
     * @see de.unikn.knime.workbench.repository.model.AbstractRepositoryObject#
     *      setParent
     *      (de.unikn.knime.workbench.repository.model.IContainerObject)
     */
    @Override
    public void setParent(final IContainerObject parent) {
        throw new UnsupportedOperationException(
                "Can't set parent of a root object");
    }

    /**
     * Locates a sub-container given by the supplied name.
     * 
     * @param path The path that is made up of ID segments, seperated by a slash
     *            "/"
     * @return The container, or <code>null</code> if not found
     */
    public IContainerObject findContainer(final String path) {
        String[] segments = path.split("/");
        IContainerObject parent = this;
        for (int i = 0; i < segments.length; i++) {
            IRepositoryObject obj = parent.getChildByID(segments[i], false);

            // not found ?
            if (obj == null) {
                return null;
            }
            // object is no container ? skip it
            if (!(obj instanceof IContainerObject)) {
                continue;
            }
            parent = (IContainerObject)obj;

        }

        assert parent != null;

        return parent;
    }

    /**
     * @return a list with categories that have wrong after-relationship
     *         information.
     */
    public List<Category> getProblemCategories() {
        List<Category> problemCategories = new ArrayList<Category>();
        appendProblemCategories(problemCategories);
        
        return problemCategories;
    }
}

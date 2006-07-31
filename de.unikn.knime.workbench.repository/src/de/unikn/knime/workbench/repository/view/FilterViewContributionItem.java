/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2004
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
 *   11.01.2006 (Florian Georg): created
 */
package de.unikn.knime.workbench.repository.view;

import java.util.Arrays;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Contribution Item in der RepositoryView. This registers a
 * <code>RepositoryViewFilter</code> on the viewer, that is able to filter
 * rquested nodes.
 * 
 * @see RepositoryViewFilter
 * 
 * @author Florian Georg, University of Konstanz
 */
public class FilterViewContributionItem extends ControlContribution implements
        KeyListener {
    private TreeViewer m_viewer;

    private Combo m_combo;

    private RepositoryViewFilter m_filter;

    /**
     * Creates the contribution item.
     * 
     * @param viewer The viewer.
     */
    public FilterViewContributionItem(final TreeViewer viewer) {
        super("@knime.repository.view.filter");

        m_viewer = viewer;
        m_filter = new RepositoryViewFilter();
        m_viewer.addFilter(m_filter);
    }

    /**
     * 
     * @see org.eclipse.jface.action.ControlContribution
     *      #createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createControl(final Composite parent) {
        m_combo = new Combo(parent, SWT.DROP_DOWN);
        m_combo.addKeyListener(this);
        m_combo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_filter.setQueryString(m_combo.getText());
                m_viewer.refresh();
                if (m_combo.getText().length() > 0) {
                    m_viewer.expandAll();
                }

            }

        });
        
        return m_combo;
    }
    
    /**
     * @see org.eclipse.jface.action.ControlContribution
     *  #computeWidth(org.eclipse.swt.widgets.Control)
     */
    @Override
    protected int computeWidth(final Control control) {
        return Math.max(super.computeWidth(control), 150);
    }

    /**
     * @see org.eclipse.swt.events.KeyListener
     *      #keyPressed(org.eclipse.swt.events.KeyEvent)
     */
    public void keyPressed(final KeyEvent e) {

    }

    /**
     * @see org.eclipse.swt.events.KeyListener
     *      #keyReleased(org.eclipse.swt.events.KeyEvent)
     */
    public void keyReleased(final KeyEvent e) {
        String str = m_combo.getText();
        boolean shouldExpand = false;

        if (e.character == SWT.CR) {
            shouldExpand = true;
            if ((str.length() > 0)
                    && (!Arrays.asList(m_combo.getItems()).contains(str))) {
                m_combo.add(str, 0);
                m_combo.select(0);

            } else if (str.length() == 0) {
                m_viewer.collapseAll();
                shouldExpand = false;
            }

        } else if (e.character == SWT.ESC) {
            m_combo.setText("");
            str = "";
            m_viewer.collapseAll();
        }

        m_filter.setQueryString(str);

        m_viewer.refresh();

        if (shouldExpand) {
            m_viewer.expandAll();
        }
    }
}

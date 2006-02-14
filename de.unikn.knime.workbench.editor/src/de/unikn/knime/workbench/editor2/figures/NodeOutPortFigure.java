/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   31.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Figure for displaying a <code>NodeOutPort</code> inside a node.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeOutPortFigure extends AbstractNodePortFigure {

    private int m_id;

    private boolean m_isModelPort;

    /**
     * 
     * @param id The id of the port, needed to determine the position inside the
     *            surrounding node visual
     * @param numPorts max number of out ports for this node
     * @param tooltip The tooltip text for this port
     * @param isModelPort indicates a model inPort, displayed with a diff. shape
     */
    public NodeOutPortFigure(final int id, final int numPorts,
            final String tooltip, final boolean isModelPort) {
        super(numPorts);
        m_id = id;
        m_isModelPort = isModelPort;
        setOpaque(false);
        setToolTip(new NewToolTipFigure(tooltip));
        setFill(true);
        setOutline(true);
    }

    /**
     * 
     * @see de.unikn.knime.workbench.editor2.figures.AbstractNodePortFigure#isModelPort()
     */
    protected boolean isModelPort() {
        return m_isModelPort;
    }

    /**
     * Create a point list for the triangular figure (a polygon).
     * 
     * @param r The bounds
     * @return the pointlist (size=3)
     */
    protected PointList createShapePoints(final Rectangle r) {
        if (!m_isModelPort) {
            PointList points = new PointList(3);
            points.addPoint(r.getLeft().getCopy().translate(WIDTH,
                    -(HEIGHT / 2)));
            points.addPoint(r.getLeft().getCopy().translate(WIDTH * 2, 0));
            points.addPoint(r.getLeft().getCopy()
                    .translate(WIDTH, (HEIGHT / 2)));
            return points;
        } else {
            PointList points = new PointList(4);
            points.addPoint(r.getLeft().getCopy().translate(WIDTH,
                    -(HEIGHT / 2)));
            points.addPoint(r.getLeft().getCopy().translate(WIDTH * 2,
                    -(HEIGHT / 2)));
            points.addPoint(r.getLeft().getCopy().translate(WIDTH * 2,
                    (HEIGHT / 2)));
            points.addPoint(r.getLeft().getCopy()
                    .translate(WIDTH, (HEIGHT / 2)));
            return points;

        }
    }

    /**
     * Returns the preffered size of a port. A port is streched in length,
     * depending on the number of ports. Always try to fill up as much height as
     * possible.
     * 
     * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
     */
    public Dimension getPreferredSize(final int wHint, final int hHint) {
        Dimension d = new Dimension();

        d.height = (getParent().getBounds().height) / getNumPorts();
        d.width = NodeContainerFigure.WIDTH / 4;
        return d;

    }

    /**
     * @return The <code>RelativeLocator</code> that places this figure on the
     *         right side (y offset corresponds to the number of the port).
     */
    public Locator getLocator() {
        return new PortLocator((NodeContainerFigure)getParent().getParent(),
                PortLocator.TYPE_OUTPORT, getNumPorts(), m_id);
    }

}

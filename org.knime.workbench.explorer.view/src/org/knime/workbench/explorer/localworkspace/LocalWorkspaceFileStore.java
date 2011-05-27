/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: Apr 14, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.localworkspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;

/**
 * Wraps the Eclipse LocalFile. Provides a file interface to the workspace.
 * Returns all files (doesn't stop at nodes and doesn't hide workflow files or
 * meta files, etc.).
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceFileStore extends ExplorerFileStore {

    private final IFileStore m_file;

    /**
     * @param mountID the id of the mount
     * @param fullPath the full path of the file store
     */
    public LocalWorkspaceFileStore(final String mountID,
            final String fullPath) {
        super(mountID, fullPath);
        IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        IPath filePath = rootPath.append(new Path(fullPath));
        m_file = EFS.getLocalFileSystem().getStore(filePath);
    }

    /**
     * Call this only with a local file!
     *
     * @param mountID
     * @param file the underlying {@link IFileStore}
     * @param fullPath the path relative to the root!
     */
    private LocalWorkspaceFileStore(final String mountID,
            final IFileStore file, final String fullPath) {
        super(mountID, fullPath);
        m_file = file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] childNames(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return m_file.childNames(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileInfo fetchInfo(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return m_file.fetchInfo(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalWorkspaceFileStore getChild(final String name) {
        return new LocalWorkspaceFileStore(getMountID(), m_file.getChild(name),
                new Path(getFullName()).append(name).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalWorkspaceFileStore getParent() {
        IFileStore p = m_file.getParent();
        if (p == null) {
            return null;
        }
        return new LocalWorkspaceFileStore(getMountID(), p, new Path(
                getFullName()).removeLastSegments(1).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return m_file.openInputStream(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toLocalFile(final int options, final IProgressMonitor monitor)
            throws CoreException {
        return m_file.toLocalFile(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        File srcFile = toLocalFile(options, monitor);
        File dstFile = destination.toLocalFile(options, monitor);
        if (!dstFile.isDirectory()) {
            throw new UnsupportedOperationException("The local workspace "
                    + "filestore only allows copying to directories but the "
                    + "destination \"" + dstFile.getAbsolutePath() + "\" is not"
                    + " a directory.");
        }
        File targetDir = new File(dstFile, srcFile.getName());
        if (targetDir.exists()) {
            throw new CoreException(new Status(Status.ERROR,
                    ExplorerActivator.PLUGIN_ID, "A resource with the name "
                    + srcFile.getName() + " already exists in "
                    + dstFile.getName()));
        }
        try {
            if (srcFile.isDirectory()) {
                FileUtils.copyDirectory(srcFile, targetDir);
            } else if (srcFile.isFile()) {
                FileUtils.copyFileToDirectory(srcFile, dstFile);
            }
        } catch (IOException e) {
            String message = "Could not copy \"" + srcFile.getAbsolutePath()
                    + "\" to \"" + dstFile.getAbsolutePath() + "\".";
            throw new CoreException(new Status(Status.ERROR,
                    ExplorerActivator.PLUGIN_ID, message, e));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int options, final IProgressMonitor monitor)
            throws CoreException {
        File srcFile = toLocalFile(options, monitor);
        try {
            if (srcFile.isDirectory()) {
                FileUtils.deleteDirectory(srcFile);
            } else if (srcFile.isFile()) {
                srcFile.delete();
            }
        } catch (IOException e) {
            String message = "Could not delete \"" + srcFile.getAbsolutePath()
            + "\".";
            throw new CoreException(new Status(Status.ERROR,
                    ExplorerActivator.PLUGIN_ID, message, e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        File srcFile = toLocalFile(options, monitor);
        File dstFile = destination.toLocalFile(options, monitor);

        try {
            srcFile.renameTo(dstFile);
        } catch (SecurityException e) {
            String message = "Could not rename file \""
                + srcFile.getAbsolutePath() + "\" to \""
                + dstFile.getAbsolutePath()
                + "\" due to missing access rights.";
            throw new CoreException(new Status(Status.ERROR,
                    ExplorerActivator.PLUGIN_ID, message, e));
        }
    }
}

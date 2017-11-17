/*
 * Copyright (c) 1999-2003, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice",
 *    nor may "Alice" appear in their name, without prior written
 *    permission of Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    "This product includes software developed by Carnegie Mellon University"
 */

package edu.cmu.cs.stage3.alice.authoringtool.galleryviewer;

/**
 * @author David Culyba
 *
 */

public class DirectoryBarButton extends javax.swing.JButton {
	/**
	 *
	 */
	private static final long serialVersionUID = 5260594571519437277L;
	GalleryViewer.DirectoryStructure directoryData;
	GalleryViewer mainViewer;

	public DirectoryBarButton(final GalleryViewer.DirectoryStructure dirData, final GalleryViewer viewer) {
		super();
		setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		if (dirData == null) {
			super.setText("Home");
		} else {
			super.setText(dirData.name);
		}
		setBorder(null);
		setOpaque(false);
		setForeground(GalleryViewer.linkColor);
		directoryData = dirData;
		mainViewer = viewer;
		addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent e) {
				if (mainViewer != null) {
					mainViewer.changeDirectory(directoryData);
				}
			}
		});
	}
}
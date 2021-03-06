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

package edu.cmu.cs.stage3.alice.authoringtool.editors.scripteditor;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

/**
 * @author Jason Pratt
 */
public class ScriptEditor extends javax.swing.JPanel implements edu.cmu.cs.stage3.alice.authoringtool.Editor {
	/**
	 *
	 */
	private static final long serialVersionUID = 3579137398392545588L;

	public String editorName = "Script Editor";

	protected edu.cmu.cs.stage3.alice.core.property.ScriptProperty scriptProperty;
	protected edu.cmu.cs.stage3.alice.authoringtool.AuthoringTool authoringTool;
	protected edu.cmu.cs.stage3.alice.authoringtool.util.ScriptEditorPane scriptEditorPane = new edu.cmu.cs.stage3.alice.authoringtool.util.ScriptEditorPane();
	protected javax.swing.event.CaretListener caretListener = new javax.swing.event.CaretListener() {
		@Override
		public void caretUpdate(final javax.swing.event.CaretEvent e) {
			ScriptEditor.this.updateLineNumber();
		}
	};
	protected javax.swing.event.DocumentListener documentListener = new javax.swing.event.DocumentListener() {
		// TODO: more efficient updating; this is going to be really costly when
		// the script is large...
		@Override
		public void changedUpdate(final javax.swing.event.DocumentEvent e) {
			scriptProperty.set(scriptEditorPane.getText());
		}

		@Override
		public void insertUpdate(final javax.swing.event.DocumentEvent e) {
			scriptProperty.set(scriptEditorPane.getText());
		}

		@Override
		public void removeUpdate(final javax.swing.event.DocumentEvent e) {
			scriptProperty.set(scriptEditorPane.getText());
		}
	};

	public ScriptEditor() {
		jbInit();
		guiInit();
	}

	private void guiInit() {
		scriptScrollPane.setViewportView(scriptEditorPane);
		scriptEditorPane.addCaretListener(caretListener);
		scriptEditorPane.performAllAction.setEnabled(false);
		scriptEditorPane.performSelectedAction.setEnabled(false);
	}

	@Override
	public javax.swing.JComponent getJComponent() {
		return this;
	}

	@Override
	public Object getObject() {
		return scriptProperty;
	}

	public void setObject(final edu.cmu.cs.stage3.alice.core.property.ScriptProperty scriptProperty) {
		scriptEditorPane.getDocument().removeDocumentListener(documentListener);
		this.scriptProperty = scriptProperty;

		if (this.scriptProperty != null) {
			if (scriptProperty.getStringValue() == null) {
				scriptProperty.set("");
			}
			scriptEditorPane.setText(scriptProperty.getStringValue());

			scriptEditorPane.getDocument().addDocumentListener(documentListener);

			scriptEditorPane.resetUndoManager();
			scriptEditorPane.setSandbox(scriptProperty.getOwner().getSandbox());
		} else {
			scriptEditorPane.resetUndoManager();
			scriptEditorPane.setSandbox(null);
		}
	}

	@Override
	public void setAuthoringTool(final edu.cmu.cs.stage3.alice.authoringtool.AuthoringTool authoringTool) {
		this.authoringTool = authoringTool;
	}

	public void updateLineNumber() {
		// TODO: better formatting
		lineNumberLabel.setText("  line number: " + (scriptEditorPane.getCurrentLineNumber() + 1) + "     ");
	}

	// /////////////////////////////////////////////
	// AuthoringToolStateListener interface
	// /////////////////////////////////////////////

	@Override
	public void stateChanging(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldLoading(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldUnLoading(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldStarting(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldStopping(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldPausing(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldSaving(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void stateChanged(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldLoaded(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldUnLoaded(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldStarted(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldStopped(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldPaused(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	@Override
	public void worldSaved(final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent ev) {
	}

	// ////////////////////
	// Autogenerated
	// ////////////////////

	BorderLayout borderLayout1 = new BorderLayout();
	JPanel southPanel = new JPanel();
	JScrollPane scriptScrollPane = new JScrollPane();
	JLabel lineNumberLabel = new JLabel();
	BoxLayout boxLayout1 = new BoxLayout(southPanel, BoxLayout.X_AXIS);
	Border border1;
	Border border2;
	Border border3;
	JPanel bogusPanel = new JPanel();
	Border border4;

	private void jbInit() {
		border1 = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.lightGray,
				new Color(142, 142, 142), new Color(99, 99, 99));
		border2 = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.gray,
				new Color(142, 142, 142), new Color(99, 99, 99));
		border3 = BorderFactory.createEmptyBorder(1, 1, 1, 1);
		border4 = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.lightGray,
				new Color(99, 99, 99), new Color(142, 142, 142));
		setLayout(borderLayout1);
		lineNumberLabel.setBorder(border1);
		lineNumberLabel.setText("  line number:     ");
		southPanel.setLayout(boxLayout1);
		southPanel.setBorder(border3);
		bogusPanel.setBorder(border4);
		this.add(southPanel, BorderLayout.SOUTH);
		southPanel.add(bogusPanel, null);
		southPanel.add(lineNumberLabel, null);
		this.add(scriptScrollPane, BorderLayout.CENTER);
	}
}
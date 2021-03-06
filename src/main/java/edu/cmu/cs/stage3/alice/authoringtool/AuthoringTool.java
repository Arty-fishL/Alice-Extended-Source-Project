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

package edu.cmu.cs.stage3.alice.authoringtool;

import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

import com.artyfishl.alicex.ui.JSystemFileChooser;

import edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateChangedEvent;
import edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateListener;
import edu.cmu.cs.stage3.alice.authoringtool.event.ElementSelectionListener;
import edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionFileFilter;
import edu.cmu.cs.stage3.alice.authoringtool.util.WorldDifferencesCapsule;
import edu.cmu.cs.stage3.alice.core.Element;
import edu.cmu.cs.stage3.alice.core.Variable;
import edu.cmu.cs.stage3.alice.core.property.ObjectProperty;
import edu.cmu.cs.stage3.alice.core.question.userdefined.CallToUserDefinedQuestion;
import edu.cmu.cs.stage3.alice.core.question.userdefined.UserDefinedQuestion;
import edu.cmu.cs.stage3.alice.core.response.CallToUserDefinedResponse;
import edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse;
import edu.cmu.cs.stage3.alice.scenegraph.renderer.DefaultRenderTargetFactory;
import edu.cmu.cs.stage3.media.Player;
import edu.cmu.cs.stage3.progress.ProgressPane;

/**
 * @author Jason Pratt
 */
public class AuthoringTool implements java.awt.datatransfer.ClipboardOwner,
		edu.cmu.cs.stage3.caitlin.stencilhelp.application.StencilApplication {
	// file extensions
	public static final String CHARACTER_EXTENSION = "a2c";
	public static final String WORLD_EXTENSION = "a2w";

	// python standard out/err
	private static org.python.core.PyFile pyStdOut;
	private static org.python.core.PyFile pyStdErr;

	// core components
	private edu.cmu.cs.stage3.alice.scenegraph.renderer.RenderTargetFactory renderTargetFactory;

	private edu.cmu.cs.stage3.alice.core.World world;
	private java.io.File defaultWorld;
	private JAliceFrame jAliceFrame;
	private edu.cmu.cs.stage3.alice.authoringtool.EditorManager editorManager;

	private edu.cmu.cs.stage3.alice.authoringtool.util.DefaultScheduler scheduler;
	private edu.cmu.cs.stage3.alice.authoringtool.util.OneShotScheduler oneShotScheduler;

	private Runnable worldScheduleRunnable;
	private edu.cmu.cs.stage3.alice.core.clock.DefaultClock worldClock;

	private MainUndoRedoStack undoRedoStack;
	private Actions actions;
	private Importing importing;
	public edu.cmu.cs.stage3.alice.authoringtool.dialog.OutputComponent stdOutOutputComponent;
	public edu.cmu.cs.stage3.alice.authoringtool.dialog.OutputComponent stdErrOutputComponent;
	private edu.cmu.cs.stage3.alice.authoringtool.util.WatcherPanel watcherPanel;

	private javax.swing.JFileChooser importFileChooser;
	private javax.swing.JFileChooser addCharacterFileChooser;
	private javax.swing.JFileChooser browseFileChooser;

	private javax.swing.JFileChooser saveWorldFileChooser;
	private javax.swing.JFileChooser saveCharacterFileDialog; // java.awt.FileDialog

	private edu.cmu.cs.stage3.alice.authoringtool.dialog.LoadElementProgressPane worldLoadProgressPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.StoreElementProgressPane worldStoreProgressPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.LoadElementProgressPane characterLoadProgressPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.StoreElementProgressPane characterStoreProgressPane;

	private edu.cmu.cs.stage3.alice.authoringtool.dialog.PreferencesContentPane preferencesContentPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.AboutContentPane aboutContentPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.WorldInfoContentPane worldInfoContentPane;
	public edu.cmu.cs.stage3.alice.authoringtool.dialog.StdErrOutContentPane stdErrOutContentPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.ExportCodeForPrintingContentPane exportCodeForPrintingContentPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.StartUpContentPane startUpContentPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.SaveForWebContentPane saveForWebContentPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.NewVariableContentPane newVariableContentPane;

	private edu.cmu.cs.stage3.alice.authoringtool.dialog.RenderContentPane renderContentPane;
	private edu.cmu.cs.stage3.alice.authoringtool.dialog.CaptureContentPane captureContentPane;
	private javax.swing.JPanel renderPanel;

	// file filters
	private javax.swing.filechooser.FileFilter worldFileFilter;
	private javax.swing.filechooser.FileFilter characterFileFilter;

	// misc
	private java.io.File currentWorldLocation;
	private boolean worldHasBeenModified = false;
	private long lastSaveTime;
	private movieMaker.SoundStorage soundStorage = null;
	@SuppressWarnings("unused")
	private java.io.File worldDirectory; // only needed for saving backup files
	private java.util.HashMap<String, FileFilter> extensionStringsToFileFilterMap;
	private edu.cmu.cs.stage3.alice.authoringtool.util.Configuration authoringToolConfig;
	private edu.cmu.cs.stage3.alice.core.RenderTarget renderTarget;
	private edu.cmu.cs.stage3.alice.scripting.ScriptingFactory scriptingFactory;

	private java.awt.event.WindowListener jAliceFrameWindowListener;

	private boolean saveTabsEnabled = false;
	private long worldLoadedTime;
	// private double speedMultiplier = 1.0;

	private edu.cmu.cs.stage3.alice.authoringtool.util.RectangleAnimator rectangleAnimator;

	private boolean stdOutToConsole;
	private boolean stdErrToConsole;
	public int numEncoded = 0; // Madeleine added

	private static List<Player> sound = new ArrayList<Player>();

	private final edu.cmu.cs.stage3.alice.core.util.WorldListener userDefinedParameterListener = new edu.cmu.cs.stage3.alice.core.util.WorldListener() {
		private Object m_previousPropertyValue = null;

		private CallToUserDefinedResponse[] getCallsTo(final UserDefinedResponse userDefined) {
			final Vector<CallToUserDefinedResponse> v = new Vector<CallToUserDefinedResponse>();
			getWorld().internalSearch(new edu.cmu.cs.stage3.util.Criterion() {
				@Override
				public boolean accept(final Object o) {
					if (o instanceof edu.cmu.cs.stage3.alice.core.response.CallToUserDefinedResponse) {
						final edu.cmu.cs.stage3.alice.core.response.CallToUserDefinedResponse call = (edu.cmu.cs.stage3.alice.core.response.CallToUserDefinedResponse) o;
						if (call.userDefinedResponse.getUserDefinedResponseValue() == userDefined) {
							return true;
						}
					}
					return false;
				}
			}, edu.cmu.cs.stage3.util.HowMuch.INSTANCE_AND_ALL_DESCENDANTS, v);
			final CallToUserDefinedResponse[] calls = new CallToUserDefinedResponse[v.size()];
			v.copyInto(calls);
			return calls;
		}

		private CallToUserDefinedQuestion[] getCallsTo(final UserDefinedQuestion userDefined) {
			final Vector<CallToUserDefinedQuestion> v = new Vector<CallToUserDefinedQuestion>();
			getWorld().internalSearch(new edu.cmu.cs.stage3.util.Criterion() {
				@Override
				public boolean accept(final Object o) {
					if (o instanceof edu.cmu.cs.stage3.alice.core.question.userdefined.CallToUserDefinedQuestion) {
						final edu.cmu.cs.stage3.alice.core.question.userdefined.CallToUserDefinedQuestion call = (edu.cmu.cs.stage3.alice.core.question.userdefined.CallToUserDefinedQuestion) o;
						if (call.userDefinedQuestion.getUserDefinedQuestionValue() == userDefined) {
							return true;
						}
					}
					return false;
				}
			}, edu.cmu.cs.stage3.util.HowMuch.INSTANCE_AND_ALL_DESCENDANTS, v);
			final edu.cmu.cs.stage3.alice.core.question.userdefined.CallToUserDefinedQuestion[] calls = new edu.cmu.cs.stage3.alice.core.question.userdefined.CallToUserDefinedQuestion[v
					.size()];
			v.copyInto(calls);
			return calls;
		}

		@Override
		protected void handleChildrenChanging(final edu.cmu.cs.stage3.alice.core.event.ChildrenEvent e) {
		}

		@Override
		protected void handleChildrenChanged(final edu.cmu.cs.stage3.alice.core.event.ChildrenEvent e) {
		}

		@Override
		protected void handlePropertyChanging(final edu.cmu.cs.stage3.alice.core.event.PropertyEvent e) {
			m_previousPropertyValue = e.getProperty().get();
		}

		@Override
		protected void handlePropertyChanged(final edu.cmu.cs.stage3.alice.core.event.PropertyEvent e) {
			final edu.cmu.cs.stage3.alice.core.Property property = e.getProperty();
			final edu.cmu.cs.stage3.alice.core.Element owner = property.getOwner();
			if (owner instanceof edu.cmu.cs.stage3.alice.core.Variable) {
				final edu.cmu.cs.stage3.alice.core.Variable variable = (edu.cmu.cs.stage3.alice.core.Variable) owner;
				if (property.getName().equals("name")) {
					final edu.cmu.cs.stage3.alice.core.Element parent = variable.getParent();
					if (parent instanceof edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse) {
						final edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse userDefined = (edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse) parent;
						final edu.cmu.cs.stage3.alice.core.response.CallToUserDefinedResponse[] calls = getCallsTo(
								userDefined);
						for (final CallToUserDefinedResponse call : calls) {
							for (int j = 0; j < call.requiredActualParameters.size(); j++) {
								final edu.cmu.cs.stage3.alice.core.Variable actualParameterJ = (edu.cmu.cs.stage3.alice.core.Variable) call.requiredActualParameters
										.get(j);
								final String nameJ = actualParameterJ.name.getStringValue();
								if (nameJ != null && nameJ.equals(m_previousPropertyValue)) {
									actualParameterJ.name.set(e.getValue());
								}
							}
						}
					} else if (parent instanceof edu.cmu.cs.stage3.alice.core.question.userdefined.UserDefinedQuestion) {
						final edu.cmu.cs.stage3.alice.core.question.userdefined.UserDefinedQuestion userDefined = (edu.cmu.cs.stage3.alice.core.question.userdefined.UserDefinedQuestion) parent;
						final edu.cmu.cs.stage3.alice.core.question.userdefined.CallToUserDefinedQuestion[] calls = getCallsTo(
								userDefined);
						for (final CallToUserDefinedQuestion call : calls) {
							for (int j = 0; j < call.requiredActualParameters.size(); j++) {
								final edu.cmu.cs.stage3.alice.core.Variable actualParameterJ = (edu.cmu.cs.stage3.alice.core.Variable) call.requiredActualParameters
										.get(j);
								final String nameJ = actualParameterJ.name.getStringValue();
								if (nameJ != null && nameJ.equals(m_previousPropertyValue)) {
									actualParameterJ.name.set(e.getValue());
								}
							}
						}
					}
				}
			}
		}

		@Override
		protected void handleObjectArrayPropertyChanging(
				final edu.cmu.cs.stage3.alice.core.event.ObjectArrayPropertyEvent e) {
		}

		@Override
		protected void handleObjectArrayPropertyChanged(
				final edu.cmu.cs.stage3.alice.core.event.ObjectArrayPropertyEvent e) {
			final edu.cmu.cs.stage3.alice.core.property.ObjectArrayProperty oap = e.getObjectArrayProperty();
			final edu.cmu.cs.stage3.alice.core.Element owner = oap.getOwner();
			if (owner instanceof edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse) {
				final edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse userDefined = (edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse) owner;
				if (oap.getName().equals("requiredFormalParameters")) {
					final Object item = e.getItem();
					if (item instanceof edu.cmu.cs.stage3.alice.core.Variable) {
						final edu.cmu.cs.stage3.alice.core.Variable formalParameter = (edu.cmu.cs.stage3.alice.core.Variable) item;
						final String formalParameterName = formalParameter.name.getStringValue();
						final edu.cmu.cs.stage3.alice.core.response.CallToUserDefinedResponse[] calls = getCallsTo(
								userDefined);
						switch (e.getChangeType()) {
						case edu.cmu.cs.stage3.alice.core.event.ObjectArrayPropertyEvent.ITEM_INSERTED:
							for (final CallToUserDefinedResponse call : calls) {
								final edu.cmu.cs.stage3.alice.core.Variable actualParameter = new edu.cmu.cs.stage3.alice.core.Variable();
								actualParameter.name.set(formalParameter.name.get());
								final Class<?> cls = formalParameter.valueClass.getClassValue();
								actualParameter.valueClass.set(cls);
								actualParameter.value.set(edu.cmu.cs.stage3.alice.authoringtool.AuthoringToolResources
										.getDefaultValueForClass(cls));
								final boolean tempListening = AuthoringTool.this.getUndoRedoStack().getIsListening();
								AuthoringTool.this.getUndoRedoStack().setIsListening(false);
								call.addChild(actualParameter);
								call.requiredActualParameters.add(actualParameter);
								AuthoringTool.this.getUndoRedoStack().setIsListening(tempListening);
							}
							break;
						case edu.cmu.cs.stage3.alice.core.event.ObjectArrayPropertyEvent.ITEM_REMOVED:
							for (final CallToUserDefinedResponse call : calls) {
								for (int j = 0; j < call.requiredActualParameters.size(); j++) {
									final edu.cmu.cs.stage3.alice.core.Variable actualParameterJ = (edu.cmu.cs.stage3.alice.core.Variable) call.requiredActualParameters
											.get(j);
									final String nameJ = actualParameterJ.name.getStringValue();
									if (nameJ != null && nameJ.equals(formalParameterName)) {
										final boolean tempListening = AuthoringTool.this.getUndoRedoStack()
												.getIsListening();
										AuthoringTool.this.getUndoRedoStack().setIsListening(false);
										actualParameterJ.removeFromParent();
										AuthoringTool.this.getUndoRedoStack().setIsListening(tempListening);
									}
								}
							}
							break;
						case edu.cmu.cs.stage3.alice.core.event.ObjectArrayPropertyEvent.ITEM_SHIFTED:
							for (final CallToUserDefinedResponse call : calls) {
								call.requiredActualParameters.shift(e.getOldIndex(), e.getNewIndex());
							}
							break;
						}
					}
				}
			} else if (owner instanceof edu.cmu.cs.stage3.alice.core.question.userdefined.UserDefinedQuestion) {
				final edu.cmu.cs.stage3.alice.core.question.userdefined.UserDefinedQuestion userDefined = (edu.cmu.cs.stage3.alice.core.question.userdefined.UserDefinedQuestion) owner;
				if (oap.getName().equals("requiredFormalParameters")) {
					final Object item = e.getItem();
					if (item instanceof edu.cmu.cs.stage3.alice.core.Variable) {
						final edu.cmu.cs.stage3.alice.core.Variable formalParameter = (edu.cmu.cs.stage3.alice.core.Variable) item;
						final String formalParameterName = formalParameter.name.getStringValue();
						final edu.cmu.cs.stage3.alice.core.question.userdefined.CallToUserDefinedQuestion[] calls = getCallsTo(
								userDefined);
						switch (e.getChangeType()) {
						case edu.cmu.cs.stage3.alice.core.event.ObjectArrayPropertyEvent.ITEM_INSERTED:
							for (final CallToUserDefinedQuestion call : calls) {
								final edu.cmu.cs.stage3.alice.core.Variable actualParameter = new edu.cmu.cs.stage3.alice.core.Variable();
								actualParameter.name.set(formalParameter.name.get());
								final Class<?> cls = formalParameter.valueClass.getClassValue();
								actualParameter.valueClass.set(cls);
								actualParameter.value.set(edu.cmu.cs.stage3.alice.authoringtool.AuthoringToolResources
										.getDefaultValueForClass(cls));
								call.addChild(actualParameter);
								call.requiredActualParameters.add(actualParameter);
							}
							break;
						case edu.cmu.cs.stage3.alice.core.event.ObjectArrayPropertyEvent.ITEM_REMOVED:
							for (final CallToUserDefinedQuestion call : calls) {
								for (int j = 0; j < call.requiredActualParameters.size(); j++) {
									final edu.cmu.cs.stage3.alice.core.Variable actualParameterJ = (edu.cmu.cs.stage3.alice.core.Variable) call.requiredActualParameters
											.get(j);
									final String nameJ = actualParameterJ.name.getStringValue();
									if (nameJ != null && nameJ.equals(formalParameterName)) {
										actualParameterJ.removeFromParent();
									}
								}
							}
							break;
						case edu.cmu.cs.stage3.alice.core.event.ObjectArrayPropertyEvent.ITEM_SHIFTED:
							for (final CallToUserDefinedQuestion call : calls) {
								call.requiredActualParameters.shift(e.getOldIndex(), e.getNewIndex());
							}
							break;
						}
					}
				}
			}
		}

		@Override
		protected boolean isPropertyListeningRequired(final edu.cmu.cs.stage3.alice.core.Property property) {
			return true;
		}

		@Override
		protected boolean isObjectArrayPropertyListeningRequired(
				final edu.cmu.cs.stage3.alice.core.property.ObjectArrayProperty oap) {
			return true;
		}
	};

	// selected element
	private edu.cmu.cs.stage3.alice.core.Element selectedElement;
	private final java.util.HashSet<ElementSelectionListener> selectionListeners = new java.util.HashSet<ElementSelectionListener>();

	// AuthoringTool state listening
	private final java.util.HashSet<AuthoringToolStateListener> stateListeners = new java.util.HashSet<AuthoringToolStateListener>();

	public static org.python.core.PyFile getPyStdOut() {
		return pyStdOut;
	}

	public static org.python.core.PyFile getPyStdErr() {
		return pyStdErr;
	}

	private static AuthoringTool hack;

	public static AuthoringTool getHack() {
		return hack;
	}

	// constructor
	public AuthoringTool(final java.io.File defaultWorld, final java.io.File worldToLoad, final boolean stdOutToConsole,
			final boolean stdErrToConsole) {
		final String font = "SansSerif"; // "Tahoma";
		try {
			javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

			// javax.swing.UIManager.put( "Button.focus", new java.awt.Color(
			// 255, 255, 255, 0 ) ); // don't show focus // makes printing slow,
			// unfortunately

			class CustomButtonBorder extends javax.swing.border.AbstractBorder implements javax.swing.plaf.UIResource {
				private static final long serialVersionUID = 3455651414378617637L;
				
				
				protected java.awt.Insets insets = new java.awt.Insets(3, 3, 3, 3);
				protected javax.swing.border.Border line = javax.swing.BorderFactory
						.createLineBorder(java.awt.Color.black, 1);
				protected javax.swing.border.Border spacer = javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4);
				protected javax.swing.border.Border raisedBevel = javax.swing.BorderFactory
						.createBevelBorder(javax.swing.border.BevelBorder.RAISED);
				protected javax.swing.border.Border loweredBevel = javax.swing.BorderFactory
						.createBevelBorder(javax.swing.border.BevelBorder.LOWERED);
				protected javax.swing.border.Border raisedBorder = javax.swing.BorderFactory.createCompoundBorder(
						javax.swing.BorderFactory.createCompoundBorder(line, raisedBevel), spacer);
				protected javax.swing.border.Border loweredBorder = javax.swing.BorderFactory.createCompoundBorder(
						javax.swing.BorderFactory.createCompoundBorder(line, loweredBevel), spacer);
				// protected javax.swing.border.Border raisedBorder =
				// javax.swing.BorderFactory.createCompoundBorder( raisedBevel,
				// spacer );
				// protected javax.swing.border.Border loweredBorder =
				// javax.swing.BorderFactory.createCompoundBorder( loweredBevel,
				// spacer );

				@Override
				public void paintBorder(final java.awt.Component c, final java.awt.Graphics g, final int x, final int y,
						final int w, final int h) {
					final javax.swing.JButton button = (javax.swing.JButton) c;
					final javax.swing.ButtonModel model = button.getModel();

					if (model.isEnabled()) {
						if (model.isPressed() && model.isArmed()) {
							loweredBorder.paintBorder(button, g, x, y, w, h);
						} else {
							raisedBorder.paintBorder(button, g, x, y, w, h);
						}
					} else {
						raisedBorder.paintBorder(button, g, x, y, w, h);
					}
				}

				@Override
				public java.awt.Insets getBorderInsets(final java.awt.Component c) {
					return insets;
				}
			}
			javax.swing.UIManager.put("Button.border", new javax.swing.plaf.BorderUIResource.CompoundBorderUIResource(
					new CustomButtonBorder(), new javax.swing.plaf.basic.BasicBorders.MarginBorder()));

			// necessary for 1.4

			javax.swing.UIManager.put("Label.font", new java.awt.Font(font, java.awt.Font.BOLD, 12));
			javax.swing.UIManager.put("Label.foreground",
					edu.cmu.cs.stage3.alice.authoringtool.AuthoringToolResources.getColor("mainFontColor"));
			javax.swing.UIManager.put("TabbedPane.selected", new java.awt.Color(255, 255, 255, 0));
			javax.swing.UIManager.put("TabbedPane.tabInsets", new java.awt.Insets(1, 4, 1, 3));

			if (System.getProperty("os.name") != null && System.getProperty("os.name").startsWith("Windows")) {
				// Can't do this in Java 7
				//javax.swing.UIManager.put("FileChooserUI", "com.sun.java.swing.plaf.windows.WindowsFileChooserUI");
			}
		} catch (final Exception e) {
			showErrorDialog("Error configuring Look and Feel.", e);
		}

		AuthoringTool.hack = this;
		this.defaultWorld = defaultWorld;
		if (!(defaultWorld.exists() && defaultWorld.canRead())) {
			this.defaultWorld = null;
			edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
					defaultWorld.getAbsolutePath()
							+ " does not exist or cannot be read!  No starting world will be available.",
					"Warning", javax.swing.JOptionPane.WARNING_MESSAGE);
		}

		filterInit();
		configInit();
		try {
			AikMin.setFontSize(Integer.parseInt(authoringToolConfig.getValue("fontSize")));
			if (authoringToolConfig.getValue("enableHighContrastMode").equalsIgnoreCase("true")) {
				javax.swing.UIManager.put("Label.foreground", java.awt.Color.black);
			}
		} catch (final Exception e) {
		}
		mainInit();
		this.stdOutToConsole = stdOutToConsole;
		this.stdErrToConsole = stdErrToConsole;
		initializeOutput(stdOutToConsole, stdErrToConsole);
		pyInit();
		// AikMin.setFontSize(12);
		dialogInit();
		// AikMin.setFontSize(Integer.parseInt( authoringToolConfig.getValue(
		// "fontSize" )));
		undoRedoInit();
		miscInit();
		importInit();
		// preCacheCommonEditors();
		worldInit(null);
		stencilInit();
		final edu.cmu.cs.stage3.scheduler.Scheduler s = new edu.cmu.cs.stage3.scheduler.AbstractScheduler() {
			@Override
			protected void handleCaughtThowable(final Runnable source, final Throwable t) {
				markEachFrameRunnableForRemoval(source);
				showErrorDialog(source.toString(), t);

				// todo?
				// addEachFrameRunnable( source );
			}
		};
		s.addEachFrameRunnable(scheduler);
		s.addEachFrameRunnable(oneShotScheduler);

		final edu.cmu.cs.stage3.scheduler.SchedulerThread schedulerThread = new edu.cmu.cs.stage3.scheduler.SchedulerThread(
				s);
		// schedulerThread.setSleepMillis( 1 );
		// schedulerThread.setPriority( Thread.MAX_PRIORITY );
		schedulerThread.start();

		jAliceFrame.setVisible(true);
		worldInit(worldToLoad);
		if (worldToLoad == null) {
			if (authoringToolConfig.getValue("showStartUpDialog").equalsIgnoreCase("true")) {
				showStartUpDialog(edu.cmu.cs.stage3.alice.authoringtool.dialog.StartUpContentPane.DO_NOT_CHANGE_TAB_ID);
			}
		}
	}

	private void filterInit() {
		worldFileFilter = new edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionFileFilter(WORLD_EXTENSION,
				WORLD_EXTENSION.toUpperCase() + " (Alice World Files)");
		characterFileFilter = new edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionFileFilter(CHARACTER_EXTENSION,
				CHARACTER_EXTENSION.toUpperCase() + " (Alice Object Files)");
	}

	private void mainInit() {
		editorManager = new edu.cmu.cs.stage3.alice.authoringtool.EditorManager(this);
		scheduler = new edu.cmu.cs.stage3.alice.authoringtool.util.DefaultScheduler();
		undoRedoStack = new MainUndoRedoStack(this);
		oneShotScheduler = new edu.cmu.cs.stage3.alice.authoringtool.util.OneShotScheduler();
		jAliceFrame = new JAliceFrame(this);
		actions = new Actions(this, jAliceFrame);
		jAliceFrame.actionInit(actions);
		edu.cmu.cs.stage3.swing.DialogManager.initialize(jAliceFrame);
		importing = new Importing();
		stdOutOutputComponent = new edu.cmu.cs.stage3.alice.authoringtool.dialog.OutputComponent();
		stdErrOutputComponent = new edu.cmu.cs.stage3.alice.authoringtool.dialog.OutputComponent();
		watcherPanel = new edu.cmu.cs.stage3.alice.authoringtool.util.WatcherPanel();
	}

	private void pyInit() {
		scriptingFactory = new edu.cmu.cs.stage3.alice.scripting.jython.ScriptingFactory();
		scriptingFactory.setStdOut(System.out);
		scriptingFactory.setStdErr(System.err);
	}

	private void worldsDirectoryChanged() {
		final String worldsDirPath = authoringToolConfig.getValue("directories.worldsDirectory");
		if (worldsDirPath != null) {
			final java.io.File worldsDir = new java.io.File(worldsDirPath);
			if (worldsDir != null && worldsDir.exists() && worldsDir.isDirectory()) {
				try {
					saveWorldFileChooser.setCurrentDirectory(worldsDir);
				} catch (final IndexOutOfBoundsException ioobe) {
					// for some reason this can potentially fail in jdk1.4.2_04
				}
			} else {
				// TODO: ?
			}
		} else {
			// TODO: what to do when the directory is null?
		}
	}

	private void importDirectoryChanged() {
		final String importDirPath = authoringToolConfig.getValue("directories.importDirectory");
		if (importDirPath != null) {
			final java.io.File importDir = new java.io.File(importDirPath);
			if (importDir != null && importDir.exists() && importDir.isDirectory()) {
				try {
					importFileChooser.setCurrentDirectory(importDir);
				} catch (final IndexOutOfBoundsException aioobe) {
					// for some reason this can potentially fail in jdk1.4.2_04
				}
			} else {
				// TODO: ?
			}
		} else {
			// TODO: what to do when the directory is null?
		}
	}

	private void charactersDirectoryChanged() {
		final String charactersDirPath = authoringToolConfig.getValue("directories.charactersDirectory");
		if (charactersDirPath != null) {
			final java.io.File charactersDir = new java.io.File(charactersDirPath);
			if (charactersDir != null && charactersDir.exists() && charactersDir.isDirectory()) {
				try {
					addCharacterFileChooser.setCurrentDirectory(charactersDir);
					saveCharacterFileDialog.setCurrentDirectory(charactersDir);// .getAbsolutePath());
				} catch (final IndexOutOfBoundsException aioobe) {
					// for some reason this can potentially fail in jdk1.4.2_04
				}
			} else {
				// TODO: ?
			}
		} else {
			// TODO: what to do when the directory is null?
		}
	}

	private void configInit() {
		authoringToolConfig = edu.cmu.cs.stage3.alice.authoringtool.util.Configuration
				.getLocalConfiguration(AuthoringTool.class.getPackage());
		edu.cmu.cs.stage3.alice.authoringtool.util.Configuration
				.addConfigurationListener(new edu.cmu.cs.stage3.alice.authoringtool.util.event.ConfigurationListener() {
					@Override
					public void changing(final edu.cmu.cs.stage3.alice.authoringtool.util.event.ConfigurationEvent ev) {
					}

					@Override
					public void changed(final edu.cmu.cs.stage3.alice.authoringtool.util.event.ConfigurationEvent ev) {
						if (ev.getKeyName().equals("edu.cmu.cs.stage3.alice.authoringtool.recentWorlds.maxWorlds")) {
							jAliceFrame.updateRecentWorlds();
						} else if (ev.getKeyName().equals("edu.cmu.cs.stage3.alice.authoringtool.numberOfClipboards")) {
							jAliceFrame.updateClipboards();
						} else if (ev.getKeyName().equals("edu.cmu.cs.stage3.alice.authoringtool.showWorldStats")) {
							jAliceFrame.showStatusPanel(ev.getNewValue().equalsIgnoreCase("true"));
						} else if (ev.getKeyName()
								.equals("edu.cmu.cs.stage3.alice.authoringtool.directories.worldsDirectory")) {
							AuthoringTool.this.worldsDirectoryChanged();
						} else if (ev.getKeyName()
								.equals("edu.cmu.cs.stage3.alice.authoringtool.directories.importDirectory")) {
							AuthoringTool.this.importDirectoryChanged();
						} else if (ev.getKeyName()
								.equals("edu.cmu.cs.stage3.alice.authoringtool.directories.charactersDirectory")) {
							AuthoringTool.this.charactersDirectoryChanged();
						}
					}
				});
	}

	private void dialogInit() {
		importFileChooser = new JSystemFileChooser();
		saveWorldFileChooser = new JSystemFileChooser() {
			private static final long serialVersionUID = 7900019006340546025L;

			@Override
			public void approveSelection() {
				final java.io.File desiredFile = getSelectedFile();
				if (currentWorldLocation == null || currentWorldLocation.equals(desiredFile) || !desiredFile.exists()) {
					if (shouldAllowOverwrite(desiredFile)) {
						super.approveSelection();
					} else {
						edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
								"That is protected Alice file and you can not overwrite it. Please choose another file.");
					}
				} else if (desiredFile.exists()) {
					if (shouldAllowOverwrite(desiredFile)) {
						final int n = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(
								"You are about to save over an existing file. Are you sure you want to?",
								"Save Over Warning", javax.swing.JOptionPane.YES_NO_OPTION);
						if (n == javax.swing.JOptionPane.YES_OPTION) {
							super.approveSelection();
						}
					} else {
						edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
								"That is protected Alice file and you can not overwrite it. Please choose another file.");
					}

				}
			}
		};
		addCharacterFileChooser = new JSystemFileChooser();
		saveCharacterFileDialog = new JSystemFileChooser();

		browseFileChooser = new JSystemFileChooser();
		// browseFileChooser.setApproveButtonText( "Set Directory" );
		// browseFileChooser.setDialogTitle( "Choose Directory..." );
		// browseFileChooser.setDialogType( javax.swing.JFileChooser.OPEN_DIALOG
		// );
		// browseFileChooser.setFileSelectionMode(
		// javax.swing.JFileChooser.DIRECTORIES_ONLY );

		/*
		 * Removed - variable not used. java.io.FilenameFilter
		 * worldFilenameFilter = new java.io.FilenameFilter() { public boolean
		 * accept(java.io.File dir, String name) { return
		 * name.toLowerCase().endsWith("." + WORLD_EXTENSION); } };
		 *
		 * java.io.FilenameFilter characterFilenameFilter = new
		 * java.io.FilenameFilter() { public boolean accept(java.io.File dir,
		 * String name) { return name.toLowerCase().endsWith("." +
		 * CHARACTER_EXTENSION); } };
		 */
		final int fontSize = Integer.parseInt(authoringToolConfig.getValue("fontSize"));
		final int x = 700 + (fontSize - 12) * 25;
		final int y = 405 + fontSize - 12;

		saveCharacterFileDialog.setDialogTitle("Save Object...");
		saveCharacterFileDialog.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
		saveCharacterFileDialog.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
		saveCharacterFileDialog.setFileFilter(characterFileFilter);
		saveCharacterFileDialog.setPreferredSize(new java.awt.Dimension(x, y));

		worldLoadProgressPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.LoadElementProgressPane(
				"Loading World...", "Loading: ");
		worldStoreProgressPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.StoreElementProgressPane(
				"Saving World...", "Saving: ");
		characterLoadProgressPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.LoadElementProgressPane(
				"Loading Object...", "Loading: ");
		characterStoreProgressPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.StoreElementProgressPane(
				"Saving Object...", "Saving: ");

		preferencesContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.PreferencesContentPane();
		preferencesContentPane.setAuthoringTool(this);
		captureContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.CaptureContentPane(this);
		renderContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.RenderContentPane(this);
		renderPanel = new javax.swing.JPanel();

		aboutContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.AboutContentPane();

		renderPanel.setLayout(new java.awt.BorderLayout());

		importFileChooser.setApproveButtonText("Import");
		importFileChooser.setDialogTitle("Import...");
		importFileChooser.setDialogType(javax.swing.JFileChooser.OPEN_DIALOG);
		importFileChooser.setPreferredSize(new java.awt.Dimension(x, y));

		saveWorldFileChooser.setApproveButtonText("Save World As");
		saveWorldFileChooser.setDialogTitle("Save World As...");
		saveWorldFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
		saveWorldFileChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
		saveWorldFileChooser.setFileFilter(worldFileFilter);
		saveWorldFileChooser.setPreferredSize(new java.awt.Dimension(x, y));

		addCharacterFileChooser.setApproveButtonText("Add Object");
		addCharacterFileChooser.setDialogTitle("Add Object...");
		addCharacterFileChooser.setDialogType(javax.swing.JFileChooser.OPEN_DIALOG);
		addCharacterFileChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
		addCharacterFileChooser.setFileFilter(characterFileFilter);
		addCharacterFileChooser.setPreferredSize(new java.awt.Dimension(x, y));

		worldsDirectoryChanged();
		importDirectoryChanged();
		charactersDirectoryChanged();

		worldInfoContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.WorldInfoContentPane();

		stdErrOutContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.StdErrOutContentPane(this);

		exportCodeForPrintingContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.ExportCodeForPrintingContentPane(
				this);

		saveForWebContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.SaveForWebContentPane(this);

		newVariableContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.NewVariableContentPane();

		startUpContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.StartUpContentPane(this);
	}

	private void undoRedoInit() {
		addAuthoringToolStateListener(undoRedoStack);

		undoRedoStack.addUndoRedoListener(new edu.cmu.cs.stage3.alice.authoringtool.event.UndoRedoListener() {
			@Override
			public void onChange() {
				final int currentIndex = undoRedoStack.getCurrentUndoableRedoableIndex();
				if (currentIndex == -1) {
					actions.undoAction.setEnabled(false);
				} else {
					actions.undoAction.setEnabled(true);
				}

				if (currentIndex == undoRedoStack.size() - 1) {
					actions.redoAction.setEnabled(false);
				} else {
					actions.redoAction.setEnabled(true);
				}

				worldHasBeenModified = currentIndex != undoRedoStack.getUnmodifiedIndex()
						|| undoRedoStack.isScriptDirty();
				AuthoringTool.this.updateTitle();
			}
		});
	}

	private void miscInit() {
		extensionStringsToFileFilterMap = new java.util.HashMap<String, FileFilter>();
		scheduler.addEachFrameRunnable(oneShotScheduler);

		// try to quit on window close
		jAliceFrameWindowListener = new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(final java.awt.event.WindowEvent e) {
				AuthoringTool.this.quit();
			}
		};
		jAliceFrame.addWindowListener(jAliceFrameWindowListener);

		worldClock = new edu.cmu.cs.stage3.alice.core.clock.DefaultClock();

		worldScheduleRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					worldClock.schedule();
					// double time = AuthoringToolResources.getCurrentTime() -
					// AuthoringTool.this.timeDifferential;
					// AuthoringTool.this.world.schedule( time );
				} catch (final Throwable t) {
					stopWorldAndShowDialog(t);
				}
			}
		};

		// for running the world
		// worldScheduleRunnable = new Runnable() {
		// public void run() {
		// try {
		// AuthoringTool.this.world.schedule(
		// AuthoringToolResources.getCurrentTime() -
		// AuthoringTool.this.timeDifferential );
		// } catch( final edu.cmu.cs.stage3.alice.core.SimulationException e ) {
		// javax.swing.SwingUtilities.invokeLater( new Runnable() {
		// public void run() {
		// AuthoringTool.showRuntimeErrorDialog( e, AuthoringTool.this );
		// }
		// } );
		// } catch( final edu.cmu.cs.stage3.alice.core.ExceptionWrapper e ) {
		// javax.swing.SwingUtilities.invokeLater( new Runnable() {
		// public void run() {
		// Exception wrappedException = e.getWrappedException();
		// if( wrappedException instanceof
		// edu.cmu.cs.stage3.alice.core.SimulationException ) {
		// AuthoringTool.showRuntimeErrorDialog(
		// (edu.cmu.cs.stage3.alice.core.SimulationException)wrappedException,
		// AuthoringTool.this );
		// } else {
		// AuthoringTool.showErrorDialog( "Error during simulation.",
		// wrappedException );
		// }
		// }
		// } );
		// } catch( final org.python.core.PyException e ) {
		// javax.swing.SwingUtilities.invokeLater( new Runnable() {
		// public void run() {
		// if( org.python.core.Py.matchException( e,
		// org.python.core.Py.SystemExit ) ) {
		// //just quit
		// } else {
		// AuthoringTool.showErrorDialog( "Jython error during world run.", e,
		// false );
		// }
		// }
		// } );
		// } catch( final Throwable t ) {
		// javax.swing.SwingUtilities.invokeLater( new Runnable() {
		// public void run() {
		// // renderWindowListener.windowClosing( null ); // somewhat hackish
		// AuthoringTool.showErrorDialog( "Error during simulation.", t );
		// }
		// } );
		// }
		// }
		// };

		// track framerate
		final javax.swing.Timer fpsTimer = new javax.swing.Timer(500, new java.awt.event.ActionListener() {
			java.text.DecimalFormat formater = new java.text.DecimalFormat("#0.00");

			@Override
			public void actionPerformed(final java.awt.event.ActionEvent ev) {
				final String fps = formater.format(scheduler.getSimulationFPS()) + " fps";
				if (authoringToolConfig.getValue("rendering.showFPS").equalsIgnoreCase("true")) {
					renderContentPane.setTitle("World Running...  " + fps);
				}
			}
		});
		fpsTimer.start();

		// prompt to save
		lastSaveTime = System.currentTimeMillis();
		final javax.swing.Timer promptToSaveTimer = new javax.swing.Timer(60000, new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent ev) {
				// this check is a little hackish. the idea is to not throw up
				// the save dialog if a modal dialog is already showing.
				boolean modalShowing = false;
				final java.awt.Window[] ownedWindows = jAliceFrame.getOwnedWindows();
				for (final Window ownedWindow : ownedWindows) {
					// System.out.println(ownedWindows[i]+",
					// "+ownedWindows[i].isShowing());
					if (ownedWindow.isShowing()) {
						modalShowing = true;
						break;
					}
				}

				// skip tutorial worlds
				boolean skipThisWorld = false;
				if (currentWorldLocation != null) {
					if (currentWorldLocation.getAbsolutePath().startsWith(getTutorialDirectory().getAbsolutePath())) {
						skipThisWorld = true;
					}
				}

				// skip unmodified worlds
				if (!worldHasBeenModified) {
					skipThisWorld = true;
				}

				if (!modalShowing && !skipThisWorld) {
					final long time = System.currentTimeMillis();
					final long dt = time - lastSaveTime;
					final int interval = Integer.parseInt(authoringToolConfig.getValue("promptToSaveInterval"));
					final long intervalMillis = (long) interval * (long) 60000;
					if (dt > intervalMillis) {
						// edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
						// "You have not saved in more than" + interval +
						// " minutes.\nIt is recommended that you save early and
						// often to avoid losing work."
						// );
						final int result = edu.cmu.cs.stage3.swing.DialogManager.showOptionDialog(
								"You have not saved in more than" + interval
										+ " minutes.\nIt is recommended that you save early and often to avoid losing work.",
								"Save?", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
								null, new Object[] { "Save right now", "Remind me later" }, "Save right now");
						if (result == javax.swing.JOptionPane.YES_OPTION) {
							AuthoringTool.this.getActions().saveWorldAction.actionPerformed(null);
						}
						lastSaveTime = System.currentTimeMillis();
					}
				}
			}
		});
		promptToSaveTimer.start();
		addAuthoringToolStateListener(new edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateAdapter() {
			@Override
			public void worldLoaded(final AuthoringToolStateChangedEvent ev) {
				lastSaveTime = System.currentTimeMillis();
			}

			@Override
			public void worldSaved(final AuthoringToolStateChangedEvent ev) {
				lastSaveTime = System.currentTimeMillis();
			}
		});

		// global mouse listening
		if (edu.cmu.cs.stage3.awt.AWTUtilities.mouseListenersAreSupported()
				|| edu.cmu.cs.stage3.awt.AWTUtilities.mouseMotionListenersAreSupported()) {
			scheduler.addEachFrameRunnable(new Runnable() {
				@Override
				public void run() {
					edu.cmu.cs.stage3.awt.AWTUtilities.fireMouseAndMouseMotionListenersIfNecessary();
				}
			});
		}

		// for animating ui changes
		rectangleAnimator = new edu.cmu.cs.stage3.alice.authoringtool.util.RectangleAnimator(this);

		watcherPanel.setMinimumSize(new java.awt.Dimension(0, 0));

		// tooltips
		javax.swing.ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		javax.swing.ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		// sound format TEST
		// System.out.println( AuthoringToolResources.formatTime( .123 ) );
		// System.out.println( AuthoringToolResources.formatTime( 45.123 ) );
		// System.out.println( AuthoringToolResources.formatTime( 54.0 ) );
		// System.out.println( AuthoringToolResources.formatTime( 63.123 ) );
		// System.out.println( AuthoringToolResources.formatTime( 927.123 ) );
		// System.out.println( AuthoringToolResources.formatTime( 32729.123 ) );
	}

	private void importInit() {
		final List<Importer> importers = importing.getImporters();
		final edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionGroupFileFilter imageFiles = new edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionGroupFileFilter(
				"Image Files");
		extensionStringsToFileFilterMap.put("Image Files", imageFiles);
		final edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionGroupFileFilter soundFiles = new edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionGroupFileFilter(
				"Sound Files");
		extensionStringsToFileFilterMap.put("Sound Files", soundFiles);
		final java.util.TreeSet<ExtensionFileFilter> extensions = new java.util.TreeSet<ExtensionFileFilter>();
		for (final Importer importer : importers) {
			final Map<String, String> map = importer.getExtensionMap();
			for (final Map.Entry<String, String> entry : map.entrySet()) {
				final String extension = entry.getKey();
				final String description = extension + " (" + entry.getValue() + ")";
				final edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionFileFilter ext = new edu.cmu.cs.stage3.alice.authoringtool.util.ExtensionFileFilter(
						extension, description);
				extensions.add(ext);
				extensionStringsToFileFilterMap.put(extension, ext);
				if (importer instanceof edu.cmu.cs.stage3.alice.authoringtool.importers.ImageImporter) {
					imageFiles.addExtensionFileFilter(ext);
				} else if (importer instanceof edu.cmu.cs.stage3.alice.authoringtool.importers.MediaImporter) {
					soundFiles.addExtensionFileFilter(ext);
				}
			}
		}
		importFileChooser.addChoosableFileFilter(characterFileFilter);
		importFileChooser.addChoosableFileFilter(imageFiles);
		importFileChooser.addChoosableFileFilter(soundFiles);
		for (final ExtensionFileFilter ext : extensions) {
			importFileChooser.addChoosableFileFilter(ext);
		}
		importFileChooser.setFileFilter(importFileChooser.getAcceptAllFileFilter());
	}

	// private void preCacheCommonEditors() {
	// editorManager.preloadEditor(
	// edu.cmu.cs.stage3.alice.authoringtool.editors.evaluationgroupeditor.EvaluationGroupEditor.class
	// );
	// editorManager.preloadEditor(
	// edu.cmu.cs.stage3.alice.authoringtool.editors.miscgroupeditor.MiscGroupEditor.class
	// );
	// editorManager.preloadEditor(
	// edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.SceneEditor.class
	// );
	// editorManager.preloadEditor(
	// edu.cmu.cs.stage3.alice.authoringtool.editors.soundgroupeditor.SoundGroupEditor.class
	// );
	// editorManager.preloadEditor(
	// edu.cmu.cs.stage3.alice.authoringtool.editors.texturemapgroupeditor.TextureMapGroupEditor.class
	// );
	// editorManager.preloadEditor(
	// edu.cmu.cs.stage3.alice.authoringtool.editors.variablegroupeditor.VariableGroupEditor.class
	// );
	// }

	private void worldInit(final java.io.File worldToLoad) {
		if (worldToLoad != null) {
			if (worldToLoad.exists()) {
				if (worldToLoad.canRead()) {
					final int retVal = loadWorld(worldToLoad, false);
					if (retVal == Constants.SUCCEEDED) {
						return;
					}
				} else {
					AuthoringTool.showErrorDialog("cannot read world: " + worldToLoad, null, false);
				}
			} else {
				AuthoringTool.showErrorDialog("world doesn't exist: " + worldToLoad, null, false);
			}
		}

		// if that fails
		loadWorld(defaultWorld, false);
	}

	private void initializeOutput(final boolean stdOutToConsole, final boolean stdErrToConsole) {
		if (stdOutToConsole) {
			AuthoringTool.pyStdOut = new org.python.core.PyFile(System.out);
		} else {
			final java.io.PrintStream stdOutStream = stdOutOutputComponent.getStdOutStream();
			System.setOut(stdOutStream);
			AuthoringTool.pyStdOut = new org.python.core.PyFile(stdOutStream);
		}
		if (stdErrToConsole) {
			AuthoringTool.pyStdErr = new org.python.core.PyFile(System.err);
		} else {
			final java.io.PrintStream stdErrStream = stdErrOutputComponent.getStdErrStream();
			System.setErr(stdErrStream);
			AuthoringTool.pyStdErr = new org.python.core.PyFile(stdErrStream);
		}
	}

	public JAliceFrame getJAliceFrame() {
		return jAliceFrame;
	}

	public edu.cmu.cs.stage3.alice.authoringtool.util.DefaultScheduler getScheduler() {
		return scheduler;
	}

	public edu.cmu.cs.stage3.alice.authoringtool.util.OneShotScheduler getOneShotScheduler() {
		return oneShotScheduler;
	}

	public MainUndoRedoStack getUndoRedoStack() {
		return undoRedoStack;
	}

	public Actions getActions() {
		return actions;
	}

	public edu.cmu.cs.stage3.alice.authoringtool.util.Configuration getConfig() {
		return authoringToolConfig;
	}

	public edu.cmu.cs.stage3.alice.scenegraph.renderer.RenderTargetFactory getRenderTargetFactory() {
		if (renderTargetFactory == null) {
			Class<?> rendererClass = null;
			boolean isSoftwareEmulationForced = false;
			try {
				final String[] renderers = authoringToolConfig.getValueList("rendering.orderedRendererList");
				rendererClass = Class.forName(renderers[0]);
			} catch (final Throwable t) {
				// todo: inform user of configuration problem?
				// pass
			}
			try {
				final String s = authoringToolConfig.getValue("rendering.forceSoftwareRendering");
				if (s != null) {
					isSoftwareEmulationForced = s.equals("true");
				}
			} catch (final Throwable t) {
				// todo: inform user of configuration problem?
				// pass
			}
			final String commandLineOption = System.getProperty("alice.forceSoftwareRendering");
			if (commandLineOption != null && commandLineOption.equalsIgnoreCase("true")) {
				isSoftwareEmulationForced = true;
			}
			renderTargetFactory = new DefaultRenderTargetFactory(rendererClass);
			renderTargetFactory.setIsSoftwareEmulationForced(isSoftwareEmulationForced);
		}
		return renderTargetFactory;
	}

	public Object getContext() {
		// TODO
		return null;
	}

	public void setContext(final Object context) {
		// TODO
	}

	public edu.cmu.cs.stage3.alice.authoringtool.dialog.OutputComponent getStdOutOutputComponent() {
		return stdOutOutputComponent;
	}

	public edu.cmu.cs.stage3.alice.authoringtool.dialog.OutputComponent getStdErrOutputComponent() {
		return stdErrOutputComponent;
	}

	public boolean isStdOutToConsole() {
		return stdOutToConsole;
	}

	public boolean isStdErrToConsole() {
		return stdErrToConsole;
	}

	public edu.cmu.cs.stage3.alice.authoringtool.util.WatcherPanel getWatcherPanel() {
		return watcherPanel;
	}

	public EditorManager getEditorManager() {
		return editorManager;
	}

	// /////////////
	// Selection
	// /////////////

	public void setSelectedElement(edu.cmu.cs.stage3.alice.core.Element element) {
		if (element == null) { // is this too much of a hack?
			element = getWorld();
		}
		if (selectedElement != element) {
			selectedElement = element;
			fireElementSelected(element);
		}
	}

	public edu.cmu.cs.stage3.alice.core.Element getSelectedElement() {
		return selectedElement;
	}

	public void addElementSelectionListener(
			final edu.cmu.cs.stage3.alice.authoringtool.event.ElementSelectionListener listener) {
		selectionListeners.add(listener);
	}

	public void removeElementSelectionListener(
			final edu.cmu.cs.stage3.alice.authoringtool.event.ElementSelectionListener listener) {
		selectionListeners.remove(listener);
	}

	protected void fireElementSelected(final edu.cmu.cs.stage3.alice.core.Element element) {
		for (final ElementSelectionListener elementSelectionListener : selectionListeners) {
			elementSelectionListener.elementSelected(element);
		}
	}

	// /////////////////////
	// State listening
	// /////////////////////

	public void addAuthoringToolStateListener(
			final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateListener listener) {
		stateListeners.add(listener);
	}

	public void removeAuthoringToolStateListener(
			final edu.cmu.cs.stage3.alice.authoringtool.event.AuthoringToolStateListener listener) {
		stateListeners.remove(listener);
	}

	protected void fireStateChanging(final int previousState, final int currentState) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(previousState, currentState,
				world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.stateChanging(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to an authoring tool state change.", t);
			}
		}
	}

	protected void fireWorldLoading(final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(
				AuthoringToolStateChangedEvent.AUTHORING_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE, world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldLoading(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world load.", t);
			}
		}
	}

	protected void fireWorldUnLoading(final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(
				AuthoringToolStateChangedEvent.AUTHORING_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE, world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldUnLoading(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world unload.", t);
			}
		}
	}

	protected void fireWorldStarting(final int previousState, final int currentState,
			final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(previousState, currentState,
				world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldStarting(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world starting.", t);
			}
		}
	}

	protected void fireWorldStopping(final int previousState, final int currentState,
			final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(previousState, currentState,
				world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldStopping(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world stopping.", t);
			}
		}
	}

	protected void fireWorldPausing(final int previousState, final int currentState,
			final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(previousState, currentState,
				world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldPausing(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world pausing.", t);
			}
		}
	}

	protected void fireWorldSaving(final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(
				AuthoringToolStateChangedEvent.AUTHORING_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE, world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldSaving(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world saving.", t);
			}
		}
	}

	protected void fireStateChanged(final int previousState, final int currentState) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(previousState, currentState,
				world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.stateChanged(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to authoring tool state changed.", t);
			}
		}
	}

	protected void fireWorldLoaded(final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(
				AuthoringToolStateChangedEvent.AUTHORING_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE, world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldLoaded(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world loaded.", t);
			}
		}
	}

	protected void fireWorldUnLoaded(final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(
				AuthoringToolStateChangedEvent.AUTHORING_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE, world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldUnLoaded(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world unloaded.", t);
			}
		}
	}

	protected void fireWorldStarted(final int previousState, final int currentState,
			final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(previousState, currentState,
				world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldStarted(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world started.", t);
			}
		}
	}

	protected void fireWorldStopped(final int previousState, final int currentState,
			final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(previousState, currentState,
				world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldStopped(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world stopped.", t);
			}
		}
	}

	protected void fireWorldPaused(final int previousState, final int currentState,
			final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(previousState, currentState,
				world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldPaused(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world paused.", t);
			}
		}
	}

	protected void fireWorldSaved(final edu.cmu.cs.stage3.alice.core.World world) {
		final AuthoringToolStateChangedEvent ev = new AuthoringToolStateChangedEvent(
				AuthoringToolStateChangedEvent.AUTHORING_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE, world);
		for (final AuthoringToolStateListener listener : stateListeners) {
			try {
				listener.worldSaved(ev);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error in listener responding to world saved.", t);
			}
		}
	}

	// ////////////////////////////
	// Editors
	// ////////////////////////////

	public void editObject(final Object object) {
		editObject(object, true);
	}

	public void editObject(final Object object, final boolean switchToNewTab) {
		Class<? extends Editor> editorClass = null;
		if (object != null) {
			editorClass = edu.cmu.cs.stage3.alice.authoringtool.util.EditorUtilities.getBestEditor(object.getClass());
		}
		editObject(object, editorClass, switchToNewTab);
	}

	public void editObject(final Object object, final Class<? extends Editor> editorClass, final boolean switchToNewTab) {
		jAliceFrame.getTabbedEditorComponent().editObject(object, editorClass, switchToNewTab);
		saveTabs();
		if (switchToNewTab && getJAliceFrame().getGuiMode() != JAliceFrame.SCENE_EDITOR_SMALL_MODE) {
			getJAliceFrame().setGuiMode(JAliceFrame.SCENE_EDITOR_SMALL_MODE);
		}
	}

	public void editObject(final Object object, final javax.swing.JComponent componentToAnimateFrom) {
		if (!isObjectBeingEdited(object)) {
			animateEditOpen(componentToAnimateFrom);
		}
		editObject(object);
		// edu.cmu.cs.stage3.alice.authoringtool.util.SwingWorker worker = new
		// edu.cmu.cs.stage3.alice.authoringtool.util.SwingWorker() {
		// public Object construct() {
		// editObject( object );
		// return null;
		// }
		// };
		// worker.start();
	}

	public void editObject(final Object object, final boolean switchToNewTab,
			final javax.swing.JComponent componentToAnimateFrom) {
		if (!isObjectBeingEdited(object)) {
			animateEditOpen(componentToAnimateFrom);
		}
		editObject(object, switchToNewTab);
		// edu.cmu.cs.stage3.alice.authoringtool.util.SwingWorker worker = new
		// edu.cmu.cs.stage3.alice.authoringtool.util.SwingWorker() {
		// public Object construct() {
		// editObject( object, switchToNewTab );
		// return null;
		// }
		// };
		// worker.start();
	}

	public void editObject(final Object object, final Class<? extends Editor> editorClass, final boolean switchToNewTab,
			final javax.swing.JComponent componentToAnimateFrom) {
		if (!isObjectBeingEdited(object)) {
			animateEditOpen(componentToAnimateFrom);
		}
		editObject(object, editorClass, switchToNewTab);
		// edu.cmu.cs.stage3.alice.authoringtool.util.SwingWorker worker = new
		// edu.cmu.cs.stage3.alice.authoringtool.util.SwingWorker() {
		// public Object construct() {
		// editObject( object, editorClass, switchToNewTab );
		// return null;
		// }
		// };
		// worker.start();
	}

	protected void animateEditOpen(final javax.swing.JComponent componentToAnimateFrom) {
		final java.awt.Rectangle sourceBounds = componentToAnimateFrom.getBounds();
		final java.awt.Point sourceLocation = sourceBounds.getLocation();
		javax.swing.SwingUtilities.convertPointToScreen(sourceLocation, componentToAnimateFrom);
		sourceBounds.setLocation(sourceLocation);
		final java.awt.Rectangle targetBounds = jAliceFrame.getTabbedEditorComponent().getBounds();
		final java.awt.Point targetLocation = targetBounds.getLocation();
		javax.swing.SwingUtilities.convertPointToScreen(targetLocation, jAliceFrame.getTabbedEditorComponent());
		targetBounds.setLocation(targetLocation);
		final java.awt.Color color = componentToAnimateFrom.getBackground();
		rectangleAnimator.animate(sourceBounds, targetBounds, color);
	}

	public Object getObjectBeingEdited() {
		return jAliceFrame.getTabbedEditorComponent().getObjectBeingEdited();
	}

	public Object[] getObjectsBeingEdited() {
		return jAliceFrame.getTabbedEditorComponent().getObjectsBeingEdited();
	}

	public boolean isObjectBeingEdited(final Object object) {
		return jAliceFrame.getTabbedEditorComponent().isObjectBeingEdited(object);
	}

	// /////////////////////////
	// General Functionality
	// /////////////////////////

	private int showStartUpDialog(final int tabID) {
		final int retVal = askForSaveIfNecessary();
		if (retVal != Constants.CANCELED) {
			startUpContentPane.setTabID(tabID);
			if (edu.cmu.cs.stage3.swing.DialogManager
					.showDialog(startUpContentPane) == edu.cmu.cs.stage3.swing.ContentPane.OK_OPTION) {
				final java.io.File file = startUpContentPane.getFile();
				if (startUpContentPane.isTutorial()) {
					launchTutorialFile(file);
				} else {
					// loadWorld(file, startUpContentPane.isSaveNeeded());
					loadWorld(file, false);
				}
			}
			resetClipboards();
			return Constants.SUCCEEDED;
		} else {
			return retVal;
		}
	}

	public void resetClipboards() {
		try {
			final int numClipboards = Integer.parseInt(authoringToolConfig.getValue("numberOfClipboards"));
			for (int i = 0; i < numClipboards; i++) {
				jAliceFrame.clipboardPanel.remove(0);
			}
			for (int i = 0; i < numClipboards; i++) {
				jAliceFrame.clipboardPanel.add(new edu.cmu.cs.stage3.alice.authoringtool.util.DnDClipboard());
			}
			jAliceFrame.clipboardPanel.revalidate();
			jAliceFrame.clipboardPanel.repaint();
		} catch (final NumberFormatException e) {
			AuthoringTool.showErrorDialog(
					"illegal number of clipboards: " + authoringToolConfig.getValue("numberOfClipboards"), null);
		}
	}

	public int newWorld() {
		watcherPanel.clear();
		return showStartUpDialog(edu.cmu.cs.stage3.alice.authoringtool.dialog.StartUpContentPane.TEMPLATE_TAB_ID);
	}

	public int openWorld() {
		watcherPanel.clear();
		return showStartUpDialog(edu.cmu.cs.stage3.alice.authoringtool.dialog.StartUpContentPane.OPEN_TAB_ID);
	}

	public int openExampleWorld() {
		watcherPanel.clear();
		return showStartUpDialog(edu.cmu.cs.stage3.alice.authoringtool.dialog.StartUpContentPane.EXAMPLE_TAB_ID);
	}

	public int openTutorialWorld() {
		watcherPanel.clear();
		return showStartUpDialog(edu.cmu.cs.stage3.alice.authoringtool.dialog.StartUpContentPane.TUTORIAL_TAB_ID);
	}

	public int saveWorld() {
		if (currentWorldLocation != null && shouldAllowOverwrite(currentWorldLocation)
				&& currentWorldLocation.canWrite()) {
			try {
				return saveWorldToFile(currentWorldLocation, false);
			} catch (final java.io.IOException e) {
				AuthoringTool.showErrorDialog("Unable to save world: " + currentWorldLocation.getAbsolutePath(), e);
				return Constants.FAILED;
			}
		} else {
			return saveWorldAs();
		}
	}

	public int saveWorldAs() {
		final int returnVal = edu.cmu.cs.stage3.swing.DialogManager.showSaveDialog(saveWorldFileChooser);
		if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
			java.io.File file = saveWorldFileChooser.getSelectedFile();
			/*
			 * if (!saveWorldFileChooser.getCurrentDirectory().canWrite()){
			 * AuthoringTool.showErrorDialog("Unable to save world" ,
			 * "The folder " + saveWorldFileChooser.getCurrentDirectory() +
			 * " isn't accessible."); file.delete(); return Constants.FAILED; }
			 */
			if (!file.getName().endsWith("." + WORLD_EXTENSION)) {
				file = new java.io.File(file.getParent(), file.getName() + "." + WORLD_EXTENSION);
			}
			try {
				return saveWorldToFile(file, false);
			} catch (final java.io.IOException e) {
				AuthoringTool.showErrorDialog("Unable to save world: " + file, e);
				file.delete();
				return Constants.FAILED;
			}
		} else if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
			return Constants.CANCELED;
		} else {
			return Constants.FAILED;
		}
	}

	private void finalCleanUp() {
		try {
			final java.awt.Rectangle bounds = jAliceFrame.getBounds();
			authoringToolConfig.setValue("mainWindowBounds",
					bounds.x + ", " + bounds.y + ", " + bounds.width + ", " + bounds.height);
			edu.cmu.cs.stage3.alice.authoringtool.util.Configuration.storeConfig();
			renderTargetFactory.release();
		} catch (final Throwable t) {
			AuthoringTool.showErrorDialog("Error encountered during final cleanup.", t);
		}
	}

	public int quit() {
		try {
			final int retVal = leaveWorld(true);
			if (retVal == Constants.SUCCEEDED) {
				finalCleanUp();
				final java.io.File aliceHasNotExitedFile = new java.io.File(
						edu.cmu.cs.stage3.alice.authoringtool.JAlice.getAliceUserDirectory(), "aliceHasNotExited.txt");
				aliceHasNotExitedFile.delete();
				java.io.BufferedInputStream bis = null;
				java.io.BufferedOutputStream bos = null;
				final java.io.File aliceJAR = new java.io.File(
						JAlice.getAliceHomeDirectory().toString() + "\\lib\\aliceupdate.jar");
				try {
					bis = new java.io.BufferedInputStream(new java.io.FileInputStream(aliceJAR));
					bos = new java.io.BufferedOutputStream(new java.io.FileOutputStream(
							new java.io.File(JAlice.getAliceHomeDirectory().toString() + "\\lib\\alice.jar")));
					int i;
					while ((i = bis.read()) != -1) {
						bos.write(i);
					}
				} catch (final Exception e1) {

				} finally {
					if (bis != null) {
						try {
							bis.close();
							aliceJAR.delete();
						} catch (final java.io.IOException ioe) {
							ioe.printStackTrace();
						}
					}
					if (bos != null) {
						try {
							bos.close();
						} catch (final java.io.IOException ioe) {
							ioe.printStackTrace();
						}
					}
				}
				System.exit(0);
				return Constants.SUCCEEDED; // never reached
			} else if (retVal == Constants.CANCELED) {
				return Constants.CANCELED;
			} else if (retVal == Constants.FAILED) {
				final int result = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(
						"Alice failed to correctly save and/or close the current world.  Would you still like to quit?");
				if (result == javax.swing.JOptionPane.YES_OPTION) {
					finalCleanUp();
					System.exit(1);
				}
			}
		} catch (final Throwable t) {
			try {
				final int result = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(
						"Error encountered while attempting to close Alice.  Would you like to force the close?"); // TODO:
				// give
				// information
				// about
				// the
				// error
				if (result == javax.swing.JOptionPane.YES_OPTION) {
					finalCleanUp();
					System.exit(1);
				}
			} catch (final Throwable t2) {
				finalCleanUp();
				System.exit(1);
			}
		}
		return Constants.FAILED;
	}

	public int askForSaveIfNecessary() {
		if (worldHasBeenModified) {
			if (currentWorldLocation != null
					&& currentWorldLocation.getAbsolutePath().startsWith(getTutorialDirectory().getAbsolutePath())) { // skip
																														// tutorial
																														// worlds
				return Constants.SUCCEEDED;
			} else if (currentWorldLocation == null || !shouldAllowOverwrite(currentWorldLocation)) {
				final String question = "The world has not been saved.  Would you like to save it?";
				final int n = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(question, "Save World?",
						javax.swing.JOptionPane.YES_NO_CANCEL_OPTION);
				if (n == javax.swing.JOptionPane.YES_OPTION) {
					final int retVal = saveWorldAs();
					if (retVal == Constants.CANCELED) {
						return askForSaveIfNecessary();
					} else if (retVal == Constants.FAILED) {
						return Constants.FAILED;
					}
				} else if (n == javax.swing.JOptionPane.NO_OPTION) {
					return Constants.SUCCEEDED;
				} else if (n == javax.swing.JOptionPane.CANCEL_OPTION) {
					return Constants.CANCELED;
				} else if (n == javax.swing.JOptionPane.CLOSED_OPTION) {
					return Constants.CANCELED;
				}
			} else {
				final String question = "The world has been modified.  Would you like to save it?";
				final int n = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(question, "Save World?",
						javax.swing.JOptionPane.YES_NO_CANCEL_OPTION);
				if (n == javax.swing.JOptionPane.YES_OPTION) {
					final int retVal = saveWorld();
					if (retVal == Constants.CANCELED) {
						return Constants.CANCELED;
					} else if (retVal == Constants.FAILED) {
						return Constants.FAILED;
					}
				} else if (n == javax.swing.JOptionPane.NO_OPTION) {
					return Constants.SUCCEEDED;
				} else if (n == javax.swing.JOptionPane.CANCEL_OPTION) {
					return Constants.CANCELED;
				} else if (n == javax.swing.JOptionPane.CLOSED_OPTION) {
					return Constants.CANCELED;
				}
			}
			worldHasBeenModified = false;
			undoRedoStack.setUnmodified();
		}
		return Constants.SUCCEEDED;
	}

	// public int askForSaveIfNecessaryForMovie() {
	// if (worldHasBeenModified) {
	// if ((currentWorldLocation != null) &&
	// currentWorldLocation.getAbsolutePath().startsWith(getTutorialDirectory().getAbsolutePath()))
	// { // skip tutorial worlds
	// return Constants.SUCCEEDED;
	// } else if ((currentWorldLocation == null) ||
	// (!shouldAllowOverwrite(currentWorldLocation))) {
	// String question =
	// "The world has not been saved. You need to save it before making a
	// movie.";
	// int n = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(question,
	// "Save World?", javax.swing.JOptionPane.OK_CANCEL_OPTION);
	// if (n == javax.swing.JOptionPane.YES_OPTION) {
	// int retVal = saveWorldAs();
	// if (retVal == Constants.CANCELED) {
	// return askForSaveIfNecessaryForMovie();
	// } else if (retVal == Constants.FAILED) {
	// return Constants.FAILED;
	// }
	// } else if (n == javax.swing.JOptionPane.CANCEL_OPTION) {
	// return Constants.CANCELED;
	// } else if (n == javax.swing.JOptionPane.CLOSED_OPTION) {
	// return Constants.CANCELED;
	// }
	// } else {
	// String question =
	// "The world has been modified. You need to save it before making a
	// movie.";
	// int n = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(question,
	// "Save World?", javax.swing.JOptionPane.OK_CANCEL_OPTION);
	// if (n == javax.swing.JOptionPane.YES_OPTION) {
	// int retVal = saveWorld();
	// if (retVal == Constants.CANCELED) {
	// return Constants.CANCELED;
	// } else if (retVal == Constants.FAILED) {
	// return Constants.FAILED;
	// }
	// } else if (n == javax.swing.JOptionPane.CANCEL_OPTION) {
	// return Constants.CANCELED;
	// } else if (n == javax.swing.JOptionPane.CLOSED_OPTION) {
	// return Constants.CANCELED;
	// }
	// }
	// worldHasBeenModified = false;
	// undoRedoStack.setUnmodified();
	// }
	// return Constants.SUCCEEDED;
	// }

	public int leaveWorld(final boolean askForSaveIfNecessary) {
		try {
			if (askForSaveIfNecessary) {
				final int retVal = askForSaveIfNecessary();
				if (retVal == Constants.CANCELED) {
					return Constants.CANCELED;
				} else if (retVal == Constants.FAILED) {
					return Constants.FAILED;
				}
			}

			fireWorldUnLoading(world);

			saveTabsEnabled = false;
			undoRedoStack.clear();
			jAliceFrame.setWorld(null);
			userDefinedParameterListener.setWorld(null);
			setCurrentWorldLocation(null);
			editObject(null);
			if (world != null) {
				world.release();
				fireWorldUnLoaded(world);
			}

			world = null;

			return Constants.SUCCEEDED;
		} catch (final Exception e) {
			AuthoringTool.showErrorDialog("Error encountered while leaving current world.", e);
			return Constants.FAILED;
		}
	}

	// private edu.cmu.cs.stage3.progress.ProgressObserver
	// m_backupProgressObserver = null;
	public void waitForBackupToFinishIfNecessary(final java.io.File file) {
	}

	public void backupWorld(final java.io.File src, final int maxBackups) {
		if (edu.cmu.cs.stage3.io.FileUtilities.isFileCopySupported()) {
			new Thread() {
				@Override
				public void run() {
					final java.io.File parentDir = src.getParentFile();
					String name = src.getName();
					if (name.endsWith(".a2w")) {
						name = name.substring(0, name.length() - 4);
					}
					final java.io.File dstDir = new java.io.File(parentDir, "Backups of " + name);

					final StringBuffer sb = new StringBuffer();
					final java.util.Calendar calendar = java.util.Calendar.getInstance();
					sb.append(name);
					sb.append(" backed up on ");
					switch (calendar.get(java.util.Calendar.MONTH)) {
					case java.util.Calendar.JANUARY:
						sb.append("Jan ");
						break;
					case java.util.Calendar.FEBRUARY:
						sb.append("Feb ");
						break;
					case java.util.Calendar.MARCH:
						sb.append("Mar ");
						break;
					case java.util.Calendar.APRIL:
						sb.append("Apr ");
						break;
					case java.util.Calendar.MAY:
						sb.append("May ");
						break;
					case java.util.Calendar.JUNE:
						sb.append("Jun ");
						break;
					case java.util.Calendar.JULY:
						sb.append("Jul ");
						break;
					case java.util.Calendar.AUGUST:
						sb.append("Aug ");
						break;
					case java.util.Calendar.SEPTEMBER:
						sb.append("Sep ");
						break;
					case java.util.Calendar.OCTOBER:
						sb.append("Oct ");
						break;
					case java.util.Calendar.NOVEMBER:
						sb.append("Noc ");
						break;
					case java.util.Calendar.DECEMBER:
						sb.append("Dec ");
						break;
					}
					sb.append(calendar.get(java.util.Calendar.DAY_OF_MONTH));
					sb.append(" ");
					sb.append(calendar.get(java.util.Calendar.YEAR));
					sb.append(" at ");
					sb.append(calendar.get(java.util.Calendar.HOUR));
					sb.append("h");
					sb.append(calendar.get(java.util.Calendar.MINUTE));
					sb.append("m");
					sb.append(calendar.get(java.util.Calendar.SECOND));
					switch (calendar.get(java.util.Calendar.AM_PM)) {
					case java.util.Calendar.AM:
						sb.append("s AM.a2w");
						break;
					case java.util.Calendar.PM:
						sb.append("s PM.a2w");
						break;
					}

					final java.io.File dst = new java.io.File(dstDir, sb.toString());
					// m_backupProgressObserver = new
					// edu.cmu.cs.stage3.progress.ProgressObserver();
					try {
						edu.cmu.cs.stage3.io.FileUtilities.copy(src, dst, true);
						final java.io.File[] siblings = dstDir.listFiles(new java.io.FilenameFilter() {
							@Override
							public boolean accept(final java.io.File dir, final String name) {
								return name.endsWith(".a2w");
							}
						});
						if (siblings.length > maxBackups) {
							java.io.File fileToDelete = siblings[0];
							long fileToDeleteLastModified = fileToDelete.lastModified();
							for (int i = 1; i < siblings.length; i++) {
								final long lastModified = siblings[i].lastModified();
								if (lastModified < fileToDeleteLastModified) {
									fileToDelete = siblings[i];
									fileToDeleteLastModified = lastModified;
								}
							}
							fileToDelete.delete();
						}
					} finally {
						// m_backupProgressObserver = null;
					}
				}
			}.start();
		}
	}

	public int saveWorldToFile(final java.io.File file, final boolean saveForWeb) throws java.io.IOException {
		if (file.exists() && !file.canWrite()) {
			edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
					"Cannot save world.  " + file.getAbsolutePath() + " is read-only.", "Cannot Save World",
					javax.swing.JOptionPane.ERROR_MESSAGE);
			return Constants.FAILED;
		}
		if (saveForWeb && file.exists()) {
			file.delete();
		}
		if (file.exists()) {
			worldStoreProgressPane.setIsCancelEnabled(false);
		} else {
			worldStoreProgressPane.setIsCancelEnabled(true);
			waitForBackupToFinishIfNecessary(file);
		}
		fireWorldSaving(world);

		worldDirectory = null;
		final boolean tempListening = AuthoringTool.this.getUndoRedoStack().getIsListening();
		undoRedoStack.setIsListening(false);
		try {
			// save which tabs are open
			saveTabsEnabled = true;
			saveTabs();

			// save count
			countSomething("edu.cmu.cs.stage3.alice.authoringtool.saveCount");

			// world open time
			updateWorldOpenTime();

			// store the world
			final java.util.Dictionary<String, byte[]> map = new java.util.Hashtable<String, byte[]>();
			if (authoringToolConfig.getValue("saveThumbnailWithWorld").equalsIgnoreCase("true")) {
				try {
					final edu.cmu.cs.stage3.alice.core.Camera[] cameras = (edu.cmu.cs.stage3.alice.core.Camera[]) world
							.getDescendants(edu.cmu.cs.stage3.alice.core.Camera.class);
					if (cameras.length > 0) {
						final edu.cmu.cs.stage3.alice.scenegraph.renderer.OffscreenRenderTarget rt = getRenderTargetFactory()
								.createOffscreenRenderTarget();
						rt.setSize(120, 90);
						rt.addCamera(cameras[0].getSceneGraphCamera());
						rt.clearAndRenderOffscreen();
						final java.awt.Image image = rt.getOffscreenImage();
						if (image != null) {
							final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
							edu.cmu.cs.stage3.image.ImageIO.store("png", baos, image);
							map.put("thumbnail.png", baos.toByteArray());
						}
						rt.release();
					}
				} catch (final Throwable t) {
					t.printStackTrace();
				}
			}
			worldStoreProgressPane.setElement(world);
			worldStoreProgressPane.setFile(file);
			worldStoreProgressPane.setFilnameToByteArrayMap(map);
			final int result = edu.cmu.cs.stage3.swing.DialogManager.showDialog(worldStoreProgressPane);
			if (result == edu.cmu.cs.stage3.swing.ContentPane.OK_OPTION) {
				if (worldStoreProgressPane.wasSuccessful()) {
					jAliceFrame.HACK_standDownFromRedAlert();
					worldHasBeenModified = false;
					undoRedoStack.setUnmodified();
					if (!file.equals(defaultWorld) && !isTemplateWorld(file.getAbsolutePath())) {
						setCurrentWorldLocation(file);

						if (file.isDirectory()) {
							worldDirectory = file;
						}
						if (saveForWeb) {
							// pass
						} else {
							jAliceFrame.updateRecentWorlds(file.getAbsolutePath());
							int backupCount = 0;
							try {
								backupCount = Integer.parseInt(authoringToolConfig.getValue("maximumWorldBackupCount"));
							} catch (final Throwable t) {
								t.printStackTrace();
							}
							if (backupCount > 0) {
								backupWorld(file, backupCount);
							}
						}
					} else {
						setCurrentWorldLocation(null);
					}
					fireWorldSaved(world);
					return Constants.SUCCEEDED;
				} else {
					return Constants.FAILED;
				}
			} else {
				file.delete();
				return Constants.CANCELED;
			}
		} catch (final Throwable t) {
			AuthoringTool.showErrorDialog("Unable to store world to file: " + file, t);
			// file.delete();
			return Constants.FAILED;
		} finally {
			undoRedoStack.setIsListening(tempListening);
		}
	}

	public void saveForWeb() {
		if (edu.cmu.cs.stage3.swing.DialogManager
				.showDialog(saveForWebContentPane) == edu.cmu.cs.stage3.swing.ContentPane.OK_OPTION) {
			final java.io.File directory = saveForWebContentPane.getExportDirectory();
			if (!directory.exists()) {
				directory.mkdir();
			}
			final String fileName = saveForWebContentPane.getExportFileName();
			final java.io.File file = new java.io.File(directory, fileName);
			final int width = saveForWebContentPane.getExportWidth();
			final int height = saveForWebContentPane.getExportHeight();
			final String authorName = saveForWebContentPane.getExportAuthorName();

			try {
				String htmlCode = null;
				if (saveForWebContentPane.isCodeToBeExported()) {
					final StringBuffer buffer = new StringBuffer();
					exportCodeForPrintingContentPane.initialize(authorName);
					exportCodeForPrintingContentPane.getHTML(buffer, file, false, true, null);
					htmlCode = buffer.toString();
				}
				saveWorldForWeb(file, width, height, authorName, htmlCode);
			} catch (final Throwable t) {
				AuthoringTool.showErrorDialog("Error saving for the web.", t);
			}
		}
	}

	public int saveWorldForWeb(final java.io.File htmlFile, final int width, final int height, String authorName,
			String code) throws java.io.IOException {
		String baseName = htmlFile.getName();
		final int dotIndex = htmlFile.getName().lastIndexOf(".");
		if (dotIndex > 0) {
			baseName = htmlFile.getName().substring(0, dotIndex);
		}

		final java.io.File worldFile = new java.io.File(htmlFile.getParentFile(), baseName + "." + WORLD_EXTENSION);

		if (htmlFile.exists() && !htmlFile.canWrite()) {
			edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
					"Cannot save web page.  " + htmlFile.getAbsolutePath() + " is read-only", "Cannot Save",
					javax.swing.JOptionPane.ERROR_MESSAGE);
			return Constants.FAILED;
		}
		if (worldFile.exists() && !worldFile.canWrite()) {
			edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
					"Cannot save world.  " + worldFile.getAbsolutePath() + " is read-only", "Cannot Save",
					javax.swing.JOptionPane.ERROR_MESSAGE);
			return Constants.FAILED;
		}
		if (authorName != null) {
			authorName = "<h2>Created by " + authorName + "</h2>\n";
		} else {
			authorName = " ";
		}
		if (code == null) {
			code = " ";
		}
		final java.util.HashMap<String, String> replacements = new java.util.HashMap<String, String>();
		replacements.put("__worldname__", baseName);
		replacements.put("__code__", code);
		replacements.put("__authorname__", authorName);
		replacements.put("__worldfile__", worldFile.getName());
		replacements.put("__width__", Integer.toString(width));
		replacements.put("__height__", Integer.toString(height));

		// write web page
		final java.io.File templateFile = new java.io.File(JAlice.getAliceHomeDirectory(), "etc/appletTemplate.html");
		if (!templateFile.exists()) {
			templateFile.createNewFile();
		}
		final java.io.BufferedReader templateReader = new java.io.BufferedReader(new java.io.FileReader(templateFile));
		final java.io.PrintWriter webPageWriter = new java.io.PrintWriter(
				new java.io.BufferedWriter(new java.io.FileWriter(htmlFile)));

		String line = templateReader.readLine();
		while (line != null) {
			for (final String from : replacements.keySet()) {
				final String to = replacements.get(from);
				while (line.indexOf(from) > 0) {
					line = line.substring(0, line.indexOf(from)) + to
							+ line.substring(line.indexOf(from) + from.length());
				}
			}
			webPageWriter.println(line);
			line = templateReader.readLine();
		}
		templateReader.close();
		webPageWriter.flush();
		webPageWriter.close();

		// write world
		final int saveWorldResult = saveWorldToFile(worldFile, true);
		if (saveWorldResult != Constants.SUCCEEDED) {
			return saveWorldResult;
		}

		// write applet
		final java.io.File appletSourceFile = new java.io.File(JAlice.getAliceHomeDirectory(), "etc/aliceapplet.jar");
		final java.io.File appletDestinationFile = new java.io.File(htmlFile.getParentFile(), "aliceapplet.jar");
		AuthoringToolResources.copyFile(appletSourceFile, appletDestinationFile);

		return Constants.SUCCEEDED;
	}

	public int loadWorld(final String filename, final boolean askForSaveIfNecessary) {
		return loadWorld(new java.io.File(filename), askForSaveIfNecessary);
	}

	public int loadWorld(final java.io.File file, final boolean askForSaveIfNecessary) {
		int result;
		if (file.isFile()) {
			result = loadWorld(new edu.cmu.cs.stage3.io.ZipFileTreeLoader(), file, askForSaveIfNecessary);
		} else if (file.isDirectory()) {
			result = loadWorld(new edu.cmu.cs.stage3.io.FileSystemTreeLoader(), file, askForSaveIfNecessary);
		} else {
			AuthoringTool.showErrorDialog("The file or directory is not valid: " + file, null);
			result = Constants.FAILED;
		}
		return result;
	}

	public boolean isTemplateWorld(final String filename) {
		return filename.startsWith(getTemplateWorldsDirectory().getAbsolutePath());
	}

	public int loadWorld(final edu.cmu.cs.stage3.io.DirectoryTreeLoader loader, final Object path,
			final boolean askForSaveIfNecessary) {
		if (askForSaveIfNecessary) {
			final int retVal = askForSaveIfNecessary();
			if (retVal == Constants.CANCELED) {
				return Constants.CANCELED;
			} else if (retVal == Constants.FAILED) {
				final int result = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(
						"Alice failed to correctly save the current world.  Would you still like to load a new world?");
				if (result != javax.swing.JOptionPane.YES_OPTION) {
					return Constants.CANCELED;
				}
			}
		}

		fireWorldLoading(null);

		worldDirectory = null;

		edu.cmu.cs.stage3.alice.core.World tempWorld = null;
		try {
			loader.open(path);
			try {
				if (path.equals(defaultWorld)) {
					tempWorld = (edu.cmu.cs.stage3.alice.core.World) edu.cmu.cs.stage3.alice.core.Element.load(loader,
							null, null);
				} else {
					worldLoadProgressPane.setLoader(loader);
					worldLoadProgressPane.setExternalRoot(null);
					edu.cmu.cs.stage3.swing.DialogManager.showDialog(worldLoadProgressPane);
					tempWorld = (edu.cmu.cs.stage3.alice.core.World) worldLoadProgressPane.getLoadedElement();
				}
			} finally {
				loader.close();
			}
		} catch (final Throwable t) {
			AuthoringTool.showErrorDialog("Unable to load world: " + path, t);
		}
		//
		// try {
		// loader.open(path);
		// } catch (java.io.IOException ioe) {
		// AuthoringTool.showErrorDialog("Unable to open world: " + path, ioe);
		// }
		//
		// try {
		// tempWorld = (edu.cmu.cs.stage3.alice.core.World)
		// edu.cmu.cs.stage3.alice.core.Element.load(loader, null,
		// progressPane);
		// } catch
		// (edu.cmu.cs.stage3.alice.core.UnresolvablePropertyReferencesException
		// upre) {
		// edu.cmu.cs.stage3.alice.core.reference.PropertyReference[]
		// propertyReferences = upre.getPropertyReferences();
		// System.err.println("Unable to load world: " + path +
		// ". Couldn't resolve the following references:");
		// for (int i = 0; i < propertyReferences.length; i++) {
		// System.err.println("\t" + propertyReferences[i]);
		// }
		// tempWorld = (edu.cmu.cs.stage3.alice.core.World) upre.getElement();
		// } catch (edu.cmu.cs.stage3.progress.ProgressCancelException pce) {
		// return Constants.CANCELED;
		// } catch (Throwable t) {
		// AuthoringTool.showErrorDialog("Unable to load world: " + path, t);
		// } finally {
		// try {
		// loader.close();
		// } catch (java.io.IOException ioe) {
		// AuthoringTool.showErrorDialog("Unable to close world: " + path, ioe);
		// }
		// }

		if (tempWorld != null) {
			final java.awt.Cursor prevCursor = getJAliceFrame().getCursor();
			getJAliceFrame().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)); // Aik
																											// Min
			try {
				leaveWorld(false);
				world = tempWorld;
				worldClock.setWorld(world);
				world.setClock(worldClock);
				world.setScriptingFactory(scriptingFactory);
				worldLoadedTime = System.currentTimeMillis();
				jAliceFrame.setWorld(world);
				userDefinedParameterListener.setWorld(world);
				world.setRenderTargetFactory(getRenderTargetFactory());

				// edu.cmu.cs.stage3.alice.core.Element[] elements =
				// world.search(new
				// edu.cmu.cs.stage3.util.criterion.InstanceOfCriterion(edu.cmu.cs.stage3.alice.core.RenderTarget.class));
				final edu.cmu.cs.stage3.alice.core.Element[] elements = world
						.getDescendants(edu.cmu.cs.stage3.alice.core.RenderTarget.class);
				if (elements.length > 0) {
					renderPanel.removeAll();
					renderTarget = (edu.cmu.cs.stage3.alice.core.RenderTarget) elements[0];
					renderPanel.add(renderTarget.getAWTComponent(), java.awt.BorderLayout.CENTER);
					renderPanel.revalidate();
					renderPanel.repaint();
				}

				setSelectedElement(world);

				loadTabs();
				if (!world.responses.isEmpty()) {
					editObject(world.responses.get(0), true);
				}

				if (!path.equals(defaultWorld) && path instanceof java.io.File
						&& !isTemplateWorld(((java.io.File) path).getAbsolutePath())) {
					setCurrentWorldLocation((java.io.File) path);

					if (((java.io.File) path).isDirectory()) {
						worldDirectory = (java.io.File) path;
					}
					jAliceFrame.updateRecentWorlds(((java.io.File) path).getAbsolutePath());
				} else {
					setCurrentWorldLocation(null);
				}

				undoRedoStack.setUnmodified();
				fireWorldLoaded(world);
			} finally {
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						getJAliceFrame().setCursor(prevCursor);
					}
				});
			}
			return Constants.SUCCEEDED;
		} else {
			return Constants.FAILED;
		}
	}

	protected boolean shouldAllowOverwrite(final java.io.File file) {
		if (file != null) {
			if (file.getAbsolutePath().startsWith(getTutorialDirectory().getAbsolutePath())) {
				return false;
			} else if (file.getAbsolutePath().startsWith(getExampleWorldsDirectory().getAbsolutePath())) {
				return false;
			} else if (isTemplateWorld(file.getAbsolutePath())) {
				return false;
			}

		}

		return true;
	}

	public edu.cmu.cs.stage3.alice.core.Element loadAndAddCharacter() {
		// addCharacterFileDialog.setVisible( true );
		// AuthoringToolResources.centerComponentOnScreen(
		// addCharacterFileDialog );
		// if( addCharacterFileDialog.getFile() != null ) {
		// String filename = addCharacterFileDialog.getFile();
		// java.io.File openFile = new java.io.File(
		// addCharacterFileDialog.getDirectory(), filename );
		// return loadCharacter( openFile );
		// } else {
		// return Constants.CANCELED;
		// }

		edu.cmu.cs.stage3.alice.core.Element character = null;

		addCharacterFileChooser.rescanCurrentDirectory();
		final int returnVal = edu.cmu.cs.stage3.swing.DialogManager.showDialog(addCharacterFileChooser, null);

		if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
			final java.io.File file = addCharacterFileChooser.getSelectedFile();
			character = loadAndAddCharacter(file);
		}

		return character;
	}

	public int add3DText() {
		final edu.cmu.cs.stage3.alice.authoringtool.dialog.Add3DTextPanel add3DTextPanel = new edu.cmu.cs.stage3.alice.authoringtool.dialog.Add3DTextPanel();
		// this.setTitle("Add 3D Text");

		if (edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(add3DTextPanel, "Add 3D Text",
				javax.swing.JOptionPane.OK_CANCEL_OPTION,
				javax.swing.JOptionPane.PLAIN_MESSAGE) == javax.swing.JOptionPane.OK_OPTION) {
			final edu.cmu.cs.stage3.alice.core.Text3D text3D = add3DTextPanel.createText3D();
			if (text3D != null) {
				undoRedoStack.startCompound();

				text3D.name.set(AuthoringToolResources.getNameForNewChild(text3D.name.getStringValue(), world));

				world.addChild(text3D);
				world.sandboxes.add(text3D);

				if (getCurrentCamera() instanceof edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) {
					animateAddModel(text3D, world,
							(edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) getCurrentCamera());
				} else {
					text3D.vehicle.set(world);
				}

				undoRedoStack.stopCompound();

				return Constants.SUCCEEDED;
			} else {
				return Constants.CANCELED;
			}
		} else {
			return Constants.CANCELED;
		}

	}

	public int exportMovie() {

		// Get Dimensions and Location of Movie
		/*
		 * int boundsX=0, boundsY=0, boundsWidth=0, boundsHeight = 0;
		 *
		 * Package authoringToolPackage = Package.getPackage(
		 * "edu.cmu.cs.stage3.alice.authoringtool" ); String dimensions =
		 * Configuration.getValue( authoringToolPackage,
		 * "rendering.renderWindowBounds" );
		 *
		 * java.util.StringTokenizer st = new java.util.StringTokenizer(
		 * dimensions, " \t," ); if( st.countTokens() == 4 ) { boundsX =
		 * Integer.parseInt(st.nextToken())+5; boundsY =
		 * Integer.parseInt(st.nextToken())+65; boundsWidth =
		 * Integer.parseInt(st.nextToken()); boundsHeight =
		 * Integer.parseInt(st.nextToken()); }
		 */
		// if(currentWorldLocation==null)
		if (saveWorldAs() != Constants.SUCCEEDED) {
			return Constants.CANCELED;
		}
		String directory = currentWorldLocation.getParent();
		directory = directory.replace('\\', '/');
		final java.io.File dir = new java.io.File(directory + "/frames");
		dir.mkdir();

		soundStorage = new movieMaker.SoundStorage();
		playWhileEncoding(directory);
		soundStorage = null;
		if (authoringToolConfig.getValue("rendering.deleteFiles").equalsIgnoreCase("true") == true) {
			captureContentPane.removeFiles(directory + "/frames/");
			dir.delete();
			if (dir.exists()) {
				showErrorDialog("Error removing temporary folder.",
						"Cannot delete the frames folder. \nOne or more files in the folder is being used.\n"
								+ "Try deleting the " + dir + "folder manually after Alice closes.");
			}
		}
		return Constants.SUCCEEDED;
	}

	public edu.cmu.cs.stage3.alice.core.Element loadAndAddCharacter(final java.net.URL url) {
		return loadAndAddCharacter(new edu.cmu.cs.stage3.io.ZipTreeLoader(), url, null);
	}

	public edu.cmu.cs.stage3.alice.core.Element loadAndAddCharacter(final java.io.File file) {
		edu.cmu.cs.stage3.alice.core.Element character = null;
		if (file.isFile()) {
			character = loadAndAddCharacter(new edu.cmu.cs.stage3.io.ZipFileTreeLoader(), file, null);
		} else if (file.isDirectory()) {
			character = loadAndAddCharacter(new edu.cmu.cs.stage3.io.FileSystemTreeLoader(), file, null);
		} else {
			AuthoringTool.showErrorDialog("The file or directory is not valid: " + file, null);
		}
		return character;
	}

	public edu.cmu.cs.stage3.alice.core.Element loadAndAddCharacter(final java.net.URL url,
			final edu.cmu.cs.stage3.math.Matrix44 targetTransformation) {
		return loadAndAddCharacter(new edu.cmu.cs.stage3.io.ZipTreeLoader(), url, targetTransformation);
	}

	public edu.cmu.cs.stage3.alice.core.Element loadAndAddCharacter(final java.io.File file,
			final edu.cmu.cs.stage3.math.Matrix44 targetTransformation) {
		edu.cmu.cs.stage3.alice.core.Element character = null;
		if (file.isFile()) {
			character = loadAndAddCharacter(new edu.cmu.cs.stage3.io.ZipFileTreeLoader(), file, targetTransformation);
		} else if (file.isDirectory()) {
			character = loadAndAddCharacter(new edu.cmu.cs.stage3.io.FileSystemTreeLoader(), file,
					targetTransformation);
		} else {
			AuthoringTool.showErrorDialog("The file or directory is not valid: " + file, null);
		}
		return character;
	}

	public edu.cmu.cs.stage3.alice.core.Element loadAndAddCharacter(
			final edu.cmu.cs.stage3.io.DirectoryTreeLoader loader, final Object pathname,
			final edu.cmu.cs.stage3.math.Matrix44 targetTransformation) {
		undoRedoStack.startCompound();

		edu.cmu.cs.stage3.alice.core.Element character = null;

		try {
			loader.open(pathname);
			try {
				characterLoadProgressPane.setLoader(loader);
				characterLoadProgressPane.setExternalRoot(world);
				edu.cmu.cs.stage3.swing.DialogManager.showDialog(characterLoadProgressPane);
				character = characterLoadProgressPane.getLoadedElement();
			} finally {
				loader.close();
			}
			if (character != null) {
				addCharacter(character, targetTransformation);
			}
		} catch (final java.util.zip.ZipException e) {
			AuthoringTool.showErrorDialog("File is not a valid " + CHARACTER_EXTENSION + ": " + pathname, e, false);
		} catch (final Exception e) {
			AuthoringTool.showErrorDialog("Unable to load object: " + pathname, e);
		} finally {
			undoRedoStack.stopCompound();
		}
		return character;
	}

	public void addCharacter(final edu.cmu.cs.stage3.alice.core.Element element,
			final edu.cmu.cs.stage3.math.Matrix44 targetTransformation,
			final edu.cmu.cs.stage3.alice.core.ReferenceFrame asSeenBy) {
		if (element != null) {
			element.name.set(AuthoringToolResources.getNameForNewChild(element.name.getStringValue(), world));

			world.addChild(element);
			world.sandboxes.add(element);

			if (element instanceof edu.cmu.cs.stage3.alice.core.Transformable) {
				int animateStyle = 0;
				final edu.cmu.cs.stage3.alice.core.Transformable model = (edu.cmu.cs.stage3.alice.core.Transformable) element;
				if (targetTransformation != null) {
					try {
						final boolean tempListening = AuthoringTool.this.getUndoRedoStack().getIsListening();
						AuthoringTool.this.getUndoRedoStack().setIsListening(false);
						model.vehicle.set(world);
						final edu.cmu.cs.stage3.math.Box boundingBox = model.getBoundingBox();
						@SuppressWarnings("unused")
						final javax.vecmath.Vector3d insertionPoint = boundingBox.getCenterOfBottomFace();
						model.setAbsoluteTransformationRightNow(targetTransformation);
						model.moveRightNow(
								edu.cmu.cs.stage3.math.MathUtilities.negate(boundingBox.getCenterOfBottomFace()));
						// model.vehicle.set(null);
						animateStyle = 2;
						AuthoringTool.this.getUndoRedoStack().setIsListening(tempListening);
					} catch (final Exception e) {
						animateStyle = 1;
					}
				} else {
					animateStyle = 1;
				}

				promptForVisualizationInfo(element);
				if (animateStyle > 0) {
					if (animateStyle == 1
							&& getCurrentCamera() instanceof edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) {
						animateAddModel(model, world,
								(edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) getCurrentCamera());
					} else if (animateStyle == 2
							|| !(getCurrentCamera() instanceof edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera)) {
						animateAddModel(model, world, null);
					}
				}
			}
		} else {
			AuthoringTool.showErrorDialog("null Element encountered", null);
		}
	}

	public void addCharacter(final edu.cmu.cs.stage3.alice.core.Element element,
			final edu.cmu.cs.stage3.math.Matrix44 targetTransformation) {
		addCharacter(element, targetTransformation, null);
	}

	public void addCharacter(final edu.cmu.cs.stage3.alice.core.Element element) {
		addCharacter(element, null, null);
	}

	public void promptForVisualizationInfo(final edu.cmu.cs.stage3.alice.core.Element element) {
		if (element instanceof edu.cmu.cs.stage3.alice.core.visualization.CollectionOfModelsVisualization) {
			String typeString = "array";
			if (element instanceof edu.cmu.cs.stage3.alice.core.visualization.ListOfModelsVisualization) {
				typeString = "list";
			} else if (element instanceof edu.cmu.cs.stage3.alice.core.visualization.ArrayOfModelsVisualization) {
				typeString = "array";
			}
			final edu.cmu.cs.stage3.alice.core.visualization.CollectionOfModelsVisualization visualization = (edu.cmu.cs.stage3.alice.core.visualization.CollectionOfModelsVisualization) element;

			final edu.cmu.cs.stage3.alice.authoringtool.util.CollectionEditorPanel collectionEditorPanel = edu.cmu.cs.stage3.alice.authoringtool.util.GUIFactory
					.getCollectionEditorPanel();
			collectionEditorPanel.setCollection(visualization.getItemsCollection());
			edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(collectionEditorPanel, "Initialize " + typeString,
					javax.swing.JOptionPane.PLAIN_MESSAGE);
		}
	}

	public void saveCharacter(final edu.cmu.cs.stage3.alice.core.Element element) {
		String characterFilename = element.name.getStringValue() + "." + CHARACTER_EXTENSION;
		characterFilename = characterFilename.substring(0, 1).toUpperCase() + characterFilename.substring(1);
		saveCharacterFileDialog.setSelectedFile(new java.io.File(characterFilename));
		saveCharacterFileDialog.setVisible(true);
		AuthoringToolResources.centerComponentOnScreen(saveCharacterFileDialog);
		final int returnVal = edu.cmu.cs.stage3.swing.DialogManager.showDialog(saveCharacterFileDialog, null);

		if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
			final java.io.File file = saveCharacterFileDialog.getSelectedFile();
			saveCharacter(element, file);
		}
	}

	public void saveCharacter(final edu.cmu.cs.stage3.alice.core.Element element, final java.io.File file) {
		characterStoreProgressPane.setElement(element);
		characterStoreProgressPane.setFile(file);
		characterStoreProgressPane.setFilnameToByteArrayMap(null);
		edu.cmu.cs.stage3.swing.DialogManager.showDialog(characterStoreProgressPane);
	}

	public edu.cmu.cs.stage3.alice.core.Element importElement() {
		return importElement(null);
	}

	public edu.cmu.cs.stage3.alice.core.Element importElement(final Object path) {
		return importElement(path, null);
	}

	public edu.cmu.cs.stage3.alice.core.Element importElement(final Object path,
			final edu.cmu.cs.stage3.alice.core.Element parent) {
		return importElement(path, parent, null);
	}

	public edu.cmu.cs.stage3.alice.core.Element importElement(final Object path,
			final edu.cmu.cs.stage3.alice.core.Element parent,
			final edu.cmu.cs.stage3.alice.authoringtool.util.PostImportRunnable postImportRunnable) {
		return importElement(path, parent, postImportRunnable, true);
	}

	public edu.cmu.cs.stage3.alice.core.Element importElement(Object path, edu.cmu.cs.stage3.alice.core.Element parent,
			final edu.cmu.cs.stage3.alice.authoringtool.util.PostImportRunnable postImportRunnable,
			final boolean animateOnAdd) {
		edu.cmu.cs.stage3.alice.core.Element element = null;

		if (path == null) {
			importFileChooser.rescanCurrentDirectory();
			final int returnVal = edu.cmu.cs.stage3.swing.DialogManager.showDialog(importFileChooser, null);

			if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
				final java.io.File file = importFileChooser.getSelectedFile();
				if (file.getAbsolutePath().toLowerCase().endsWith(AuthoringTool.CHARACTER_EXTENSION)) { // special
																										// case
																										// Alice
																										// objects
					return loadAndAddCharacter(file);
				} else {
					path = file;
				}
			}
		}
		if (path != null) {
			String ext = null, s;
			if (path instanceof String) {
				s = (String) path;
				ext = s.substring(s.lastIndexOf('.') + 1).toUpperCase();
			} else if (path instanceof java.io.File) {
				s = ((java.io.File) path).getAbsolutePath();
				ext = s.substring(s.lastIndexOf('.') + 1).toUpperCase();
				path = ((java.io.File) path).getAbsoluteFile();
			} else if (path instanceof java.net.URL) {
				s = ((java.net.URL) path).getPath();
				ext = s.substring(s.lastIndexOf('.') + 1).toUpperCase();
			} else {
				throw new IllegalArgumentException("path must be a String, java.io.File, or java.net.URL");
			}

			if (parent == null) {
				parent = world;
			}

			Importer importerToUse = null;
			for (final Importer importer : importing.getImporters()) {
				if (importer.getExtensionMap().get(ext) != null) {
					importerToUse = importer;
					break;
				}
			}

			if (importerToUse != null) {
				AuthoringTool.this.undoRedoStack.startCompound();

				try {
					if (path instanceof String) {
						element = importerToUse.load((String) path);
					} else if (path instanceof java.io.File) {
						element = importerToUse.load((java.io.File) path);
					} else if (path instanceof java.net.URL) {
						element = importerToUse.load((java.net.URL) path);
					}

					if (element != null) {
						if (element.getParent() != parent) {
							final String name = element.name.getStringValue();
							element.name.set(AuthoringToolResources.getNameForNewChild(name, parent));
							if (parent != null) {
								parent.addChild(element);
								AuthoringToolResources.addElementToAppropriateProperty(element, parent);
							}
							if (animateOnAdd) {
								animateAddModelIfPossible(element, parent);
							} else {
								// makeIDVisible();
							}
							if (postImportRunnable != null) {
								postImportRunnable.setImportedElement(element);
								postImportRunnable.run();
							}
						}
					} else {
						AuthoringTool.showErrorDialog("Corrupted file or incorrect file type.", null, false);
					}
				} catch (final java.io.IOException e) {
					AuthoringTool.showErrorDialog("Error while importing object.", e);
				}
				AuthoringTool.this.undoRedoStack.stopCompound();
			} else {
				edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog("No importer found to load given file type.",
						"Import error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
		} else {
			return null;
		}

		return element;
	}

	private void animateAddModelIfPossible(final edu.cmu.cs.stage3.alice.core.Element element,
			final edu.cmu.cs.stage3.alice.core.Element parent) {
		if (element instanceof edu.cmu.cs.stage3.alice.core.Transformable) {
			if (parent instanceof edu.cmu.cs.stage3.alice.core.ReferenceFrame) {
				if (AuthoringTool.this
						.getCurrentCamera() instanceof edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) {
					animateAddModel((edu.cmu.cs.stage3.alice.core.Transformable) element,
							(edu.cmu.cs.stage3.alice.core.ReferenceFrame) parent,
							(edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) AuthoringTool.this
									.getCurrentCamera());
				} else {
					((edu.cmu.cs.stage3.alice.core.Transformable) element).vehicle.set(parent);
				}
			} else {
				if (AuthoringTool.this
						.getCurrentCamera() instanceof edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) {
					animateAddModel((edu.cmu.cs.stage3.alice.core.Transformable) element, world,
							(edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) AuthoringTool.this
									.getCurrentCamera());
				} else {
					((edu.cmu.cs.stage3.alice.core.Transformable) element).vehicle.set(world);
				}
			}
		}
	}

	public void animateAddModel(final edu.cmu.cs.stage3.alice.core.Transformable transformable,
			final edu.cmu.cs.stage3.alice.core.ReferenceFrame vehicle,
			final edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera camera) {
		if (transformable instanceof edu.cmu.cs.stage3.alice.core.Model) {
			final boolean shouldAnimateCamera = camera != null;
			final edu.cmu.cs.stage3.alice.core.Model model = (edu.cmu.cs.stage3.alice.core.Model) transformable;

			final java.util.HashMap<Element, Object> opacityMap = new java.util.HashMap<Element, Object>();
			final java.util.Vector<ObjectProperty> properties = new java.util.Vector<ObjectProperty>();
			if (shouldAnimateCamera) {
				properties.add(camera.localTransformation);
				properties.add(camera.farClippingPlaneDistance);
			}
			properties.add(model.vehicle);
			final edu.cmu.cs.stage3.alice.core.Element[] descendants = model.getDescendants(
					edu.cmu.cs.stage3.alice.core.Model.class,
					edu.cmu.cs.stage3.util.HowMuch.INSTANCE_AND_ALL_DESCENDANTS);
			for (final Element descendant : descendants) {
				opacityMap.put(descendant, ((edu.cmu.cs.stage3.alice.core.Model) descendant).opacity.get());
				properties.add(((edu.cmu.cs.stage3.alice.core.Model) descendant).opacity);
			}
			final edu.cmu.cs.stage3.alice.core.Property[] affectedProperties = properties
					.toArray(new edu.cmu.cs.stage3.alice.core.Property[0]);
			final boolean tempListening = AuthoringTool.this.getUndoRedoStack().getIsListening();
			undoRedoStack.setIsListening(false);
			model.opacity.set(new Double(0.0), edu.cmu.cs.stage3.util.HowMuch.INSTANCE_AND_ALL_DESCENDANTS);
			// javax.vecmath.Matrix4d goodLook =
			// AuthoringToolResources.getAGoodLookAtMatrix( model, camera );
			javax.vecmath.Matrix4d goodLook = null;
			if (shouldAnimateCamera) {
				model.vehicle.set(vehicle);
				goodLook = camera.calculateGoodLookAt(model);
			}
			model.vehicle.set(null);
			undoRedoStack.setIsListening(tempListening);

			double distanceToBackOfObject = 0.0;
			boolean needToChangeFarClipping = false;
			if (shouldAnimateCamera) {
				distanceToBackOfObject = AuthoringToolResources.distanceToBackAfterGetAGoodLookAt(model, camera);
				needToChangeFarClipping = distanceToBackOfObject > camera.farClippingPlaneDistance.doubleValue();
			}

			// getAGoodLook response
			final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation setupOpacity = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
			setupOpacity.element.set(model);
			setupOpacity.propertyName.set("opacity");
			setupOpacity.value.set(new Double(0.0));
			setupOpacity.duration.set(new Double(0.0));
			setupOpacity.howMuch.set(edu.cmu.cs.stage3.util.HowMuch.INSTANCE_AND_ALL_DESCENDANTS);
			final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation vehicleAnimation = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
			vehicleAnimation.element.set(model);
			vehicleAnimation.propertyName.set("vehicle");
			vehicleAnimation.value.set(vehicle);
			vehicleAnimation.duration.set(new Double(0.0));
			vehicleAnimation.howMuch.set(edu.cmu.cs.stage3.util.HowMuch.INSTANCE);
			final edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation getAGoodLook = new edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation();
			if (shouldAnimateCamera) {
				getAGoodLook.subject.set(camera);
				getAGoodLook.pointOfView.set(goodLook);
			}

			final edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation cameraGoBack = new edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation();
			if (shouldAnimateCamera) {
				cameraGoBack.subject.set(camera);
				cameraGoBack.pointOfView.set(camera.getLocalTransformation());
				cameraGoBack.duration.set(new Double(.5));
			}
			final edu.cmu.cs.stage3.alice.core.response.Wait wait = new edu.cmu.cs.stage3.alice.core.response.Wait();
			wait.duration.set(new Double(.7));
			final edu.cmu.cs.stage3.alice.core.response.Wait wait2 = new edu.cmu.cs.stage3.alice.core.response.Wait();
			wait2.duration.set(new Double(.2));
			final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation farClipping = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
			if (shouldAnimateCamera) {
				farClipping.element.set(camera);
				farClipping.propertyName.set("farClippingPlaneDistance");
				farClipping.value.set(new Double(distanceToBackOfObject));
			}
			final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation farClipping2 = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
			if (shouldAnimateCamera) {
				farClipping2.element.set(camera);
				farClipping2.propertyName.set("farClippingPlaneDistance");
				farClipping2.value.set(camera.farClippingPlaneDistance.get());
			}
			final edu.cmu.cs.stage3.alice.core.response.DoTogether opacityDoTogether = new edu.cmu.cs.stage3.alice.core.response.DoTogether();
			for (final Element element : opacityMap.keySet()) {
				final edu.cmu.cs.stage3.alice.core.Model m = (edu.cmu.cs.stage3.alice.core.Model) element;
				final Object opacity = opacityMap.get(m);
				final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation opacityAnimation = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
				opacityAnimation.element.set(m);
				opacityAnimation.propertyName.set("opacity");
				opacityAnimation.value.set(opacity);
				opacityAnimation.howMuch.set(edu.cmu.cs.stage3.util.HowMuch.INSTANCE);
				opacityDoTogether.componentResponses.add(opacityAnimation);
			}
			final edu.cmu.cs.stage3.alice.core.response.DoInOrder waitOpacityDoInOrder = new edu.cmu.cs.stage3.alice.core.response.DoInOrder();
			waitOpacityDoInOrder.componentResponses.add(wait);
			waitOpacityDoInOrder.componentResponses.add(opacityDoTogether);
			waitOpacityDoInOrder.componentResponses.add(wait2);
			final edu.cmu.cs.stage3.alice.core.response.DoTogether cameraOpacityDoTogether = new edu.cmu.cs.stage3.alice.core.response.DoTogether();
			if (needToChangeFarClipping) {
				cameraOpacityDoTogether.componentResponses.add(farClipping);
			}
			cameraOpacityDoTogether.componentResponses.add(getAGoodLook);
			cameraOpacityDoTogether.componentResponses.add(waitOpacityDoInOrder);
			final edu.cmu.cs.stage3.alice.core.response.DoInOrder response = new edu.cmu.cs.stage3.alice.core.response.DoInOrder();
			response.componentResponses.add(setupOpacity);
			response.componentResponses.add(vehicleAnimation);
			if (shouldAnimateCamera) {
				response.componentResponses.add(cameraOpacityDoTogether);
				response.componentResponses.add(cameraGoBack);
				response.componentResponses.add(farClipping2);
			} else {
				response.componentResponses.add(opacityDoTogether);
			}
			// getAGoodLook undoResponse
			final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation undoVehicleAnimation = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
			undoVehicleAnimation.element.set(model);
			undoVehicleAnimation.propertyName.set("vehicle");
			undoVehicleAnimation.value.set(null);
			undoVehicleAnimation.duration.set(new Double(0.0));
			undoVehicleAnimation.howMuch.set(edu.cmu.cs.stage3.util.HowMuch.INSTANCE);
			final edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation undoGetAGoodLook = new edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation();
			if (shouldAnimateCamera) {
				undoGetAGoodLook.subject.set(camera);
				undoGetAGoodLook.pointOfView.set(goodLook);
				undoGetAGoodLook.duration.set(new Double(.5));
			}
			final edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation undoCameraGoBack = new edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation();
			if (shouldAnimateCamera) {
				undoCameraGoBack.subject.set(camera);
				undoCameraGoBack.pointOfView.set(camera.getLocalTransformation());
			}
			final edu.cmu.cs.stage3.alice.core.response.Wait undoWait = new edu.cmu.cs.stage3.alice.core.response.Wait();
			undoWait.duration.set(new Double(.9));
			final edu.cmu.cs.stage3.alice.core.response.Wait undoWait2 = new edu.cmu.cs.stage3.alice.core.response.Wait();
			undoWait2.duration.set(new Double(.2));
			final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation undoFarClipping = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
			if (shouldAnimateCamera) {
				undoFarClipping.element.set(camera);
				undoFarClipping.propertyName.set("farClippingPlaneDistance");
				undoFarClipping.value.set(new Double(distanceToBackOfObject));
				undoFarClipping.duration.set(new Double(.2));
			}
			final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation undoFarClipping2 = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
			if (shouldAnimateCamera) {
				undoFarClipping2.element.set(camera);
				undoFarClipping2.propertyName.set("farClippingPlaneDistance");
				undoFarClipping2.value.set(camera.farClippingPlaneDistance.get());
				undoFarClipping2.duration.set(new Double(.2));
			}
			final edu.cmu.cs.stage3.alice.core.response.DoInOrder undoCameraGoBackWaitDoInOrder = new edu.cmu.cs.stage3.alice.core.response.DoInOrder();
			undoCameraGoBackWaitDoInOrder.componentResponses.add(undoWait);
			undoCameraGoBackWaitDoInOrder.componentResponses.add(undoCameraGoBack);
			final edu.cmu.cs.stage3.alice.core.response.DoTogether undoOpacityDoTogether = new edu.cmu.cs.stage3.alice.core.response.DoTogether();
			for (final Element element : opacityMap.keySet()) {
				final edu.cmu.cs.stage3.alice.core.Model m = (edu.cmu.cs.stage3.alice.core.Model) element;
				final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation opacityAnimation = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
				opacityAnimation.element.set(m);
				opacityAnimation.propertyName.set("opacity");
				opacityAnimation.value.set(new Double(0.0));
				opacityAnimation.howMuch.set(edu.cmu.cs.stage3.util.HowMuch.INSTANCE);
				undoOpacityDoTogether.componentResponses.add(opacityAnimation);
			}
			final edu.cmu.cs.stage3.alice.core.response.DoInOrder undoOpacityWaitDoInOrder = new edu.cmu.cs.stage3.alice.core.response.DoInOrder();
			undoOpacityWaitDoInOrder.componentResponses.add(undoWait2);
			undoOpacityWaitDoInOrder.componentResponses.add(undoOpacityDoTogether);
			final edu.cmu.cs.stage3.alice.core.response.DoTogether undoCameraOpacityDoTogether = new edu.cmu.cs.stage3.alice.core.response.DoTogether();
			undoCameraOpacityDoTogether.componentResponses.add(undoOpacityWaitDoInOrder);
			undoCameraOpacityDoTogether.componentResponses.add(undoCameraGoBackWaitDoInOrder);
			if (needToChangeFarClipping) {
				undoCameraOpacityDoTogether.componentResponses.add(undoFarClipping);
			}
			final edu.cmu.cs.stage3.alice.core.response.DoInOrder undoResponse = new edu.cmu.cs.stage3.alice.core.response.DoInOrder();
			if (shouldAnimateCamera) {
				undoResponse.componentResponses.add(undoGetAGoodLook);
				undoResponse.componentResponses.add(undoCameraOpacityDoTogether);
				if (needToChangeFarClipping) {
					undoResponse.componentResponses.add(undoFarClipping2);
				}
			} else {
				undoResponse.componentResponses.add(undoOpacityWaitDoInOrder);
			}
			undoResponse.componentResponses.add(undoVehicleAnimation);
			// displayDurations(undoResponse);
			performOneShot(response, undoResponse, affectedProperties);
		} else {
			transformable.vehicle.set(vehicle);
		}
	}

	@SuppressWarnings("unused")
	private void displayDurations(final edu.cmu.cs.stage3.alice.core.Response r) {
		if (r instanceof edu.cmu.cs.stage3.alice.core.response.CompositeResponse) {
			final edu.cmu.cs.stage3.alice.core.response.CompositeResponse c = (edu.cmu.cs.stage3.alice.core.response.CompositeResponse) r;
			System.out.println("COMPOSITE(");
			for (int i = 0; i < c.componentResponses.size(); i++) {
				displayDurations((edu.cmu.cs.stage3.alice.core.Response) c.componentResponses.get(i));
			}
			System.out.println(")");
		} else {
			System.out.println(r.duration.doubleValue());
		}
	}

	public void getAGoodLookAt(final edu.cmu.cs.stage3.alice.core.Transformable transformable,
			final edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera camera) {
		final edu.cmu.cs.stage3.alice.core.Property[] affectedProperties = { camera.localTransformation };

		// javax.vecmath.Matrix4d goodLook =
		// AuthoringToolResources.getAGoodLookAtMatrix( transformable, camera );
		final javax.vecmath.Matrix4d goodLook = camera.calculateGoodLookAt(transformable);

		final edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation getAGoodLook = new edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation();
		getAGoodLook.subject.set(camera);
		getAGoodLook.pointOfView.set(goodLook);

		final edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation undoResponse = new edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation();
		undoResponse.subject.set(camera);
		undoResponse.pointOfView.set(camera.getLocalTransformation());

		performOneShot(getAGoodLook, undoResponse, affectedProperties);

		final double distanceToBackOfObject = AuthoringToolResources.distanceToBackAfterGetAGoodLookAt(transformable,
				camera);

		if (distanceToBackOfObject > camera.farClippingPlaneDistance.doubleValue()) {
			final Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000);
					} catch (final InterruptedException e) {
						AuthoringTool.showErrorDialog("Interrupted during clipping plane operation.", e);
					}
					final int result = edu.cmu.cs.stage3.swing.DialogManager.showConfirmDialog(
							"The camera's far clipping plane is too close to see all of the object.  Would you like to move it back?",
							"Alter camera's far clipping plane?", javax.swing.JOptionPane.YES_NO_OPTION,
							javax.swing.JOptionPane.INFORMATION_MESSAGE);
					if (result == javax.swing.JOptionPane.YES_OPTION) {
						final edu.cmu.cs.stage3.alice.core.Property[] affectedProperties = {
								camera.farClippingPlaneDistance };

						final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation farClippingAnimation = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
						farClippingAnimation.element.set(camera);
						farClippingAnimation.propertyName.set("farClippingPlaneDistance");
						farClippingAnimation.value.set(new Double(distanceToBackOfObject));

						final edu.cmu.cs.stage3.alice.core.response.PropertyAnimation undoResponse = new edu.cmu.cs.stage3.alice.core.response.PropertyAnimation();
						undoResponse.element.set(camera);
						undoResponse.propertyName.set("farClippingPlaneDistance");
						undoResponse.value.set(camera.farClippingPlaneDistance.get());

						performOneShot(farClippingAnimation, undoResponse, affectedProperties);
					}
				}
			};
			javax.swing.SwingUtilities.invokeLater(runnable);
		}
	}

	public void saveTabs() {
		if (saveTabsEnabled && world != null) {
			final Object[] objects = jAliceFrame.getTabbedEditorComponent().getObjectsBeingEdited();
			String tabObjectsString = "";
			for (final Object object : objects) {
				if (object instanceof edu.cmu.cs.stage3.alice.core.Element) { // TODO:
																				// handle
																				// non
																				// Elements?
					tabObjectsString += ((edu.cmu.cs.stage3.alice.core.Element) object).getKey() + ":";
				}
			}
			world.data.put("edu.cmu.cs.stage3.alice.authoringtool.tabObjects", tabObjectsString);
		}
	}

	public void loadTabs() {
		if (authoringToolConfig.getValue("loadSavedTabs").equalsIgnoreCase("true")) {
			if (world != null) {
				final String tabObjectsString = (String) world.data
						.get("edu.cmu.cs.stage3.alice.authoringtool.tabObjects");
				if (tabObjectsString != null) {
					final java.util.StringTokenizer st = new java.util.StringTokenizer(tabObjectsString, ":");
					getJAliceFrame().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
					while (st.hasMoreTokens()) {
						String key = st.nextToken();
						key = key.substring(world.getKey().length() + (key.equals(world.getKey()) ? 0 : 1));
						final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(key);
						if (element != null) {
							editObject(element, false); // TODO: handle
														// different types of
														// editors
						}
					}
					getJAliceFrame().setCursor(java.awt.Cursor.getDefaultCursor());
				}
			}
		}
	}

	public boolean isImportable(final String extension) {
		for (final Importer importer : importing.getImporters()) {
			final Map<String, String> map = importer.getExtensionMap();
			if (map.get(extension.toUpperCase()) != null) {
				return true;
			}
		}

		return false;
	}

	public void setImportFileFilter(final String extensionString) {
		final FileFilter filter = extensionStringsToFileFilterMap.get(extensionString);
		if (filter != null) {
			importFileChooser.setFileFilter(filter);
		}
	}

	public void showWorldInfoDialog() {
		updateWorldOpenTime();
		worldInfoContentPane.setWorld(world);
		edu.cmu.cs.stage3.swing.DialogManager.showDialog(worldInfoContentPane);
	}

	private final java.text.DecimalFormat captureFormatter = new java.text.DecimalFormat();

	public void storeCapturedImage(final java.awt.Image image) {
		java.io.File dir = new java.io.File(authoringToolConfig.getValue("screenCapture.directory"));

		final int numDigits = Integer.parseInt(authoringToolConfig.getValue("screenCapture.numDigits"));
		final StringBuffer pattern = new StringBuffer(authoringToolConfig.getValue("screenCapture.baseName"));
		final String codec = authoringToolConfig.getValue("screenCapture.codec");
		pattern.append("#");
		for (int i = 0; i < numDigits; i++) {
			pattern.append("0");
		}
		pattern.append(".");
		pattern.append(codec);
		captureFormatter.applyPattern(pattern.toString());

		int i = 0;
		java.io.File file = new java.io.File(dir, captureFormatter.format(i));
		boolean writable;
		if (file.exists()) {
			writable = file.canWrite();
		} else {
			try {
				final boolean success = file.createNewFile();
				writable = success;
				if (success) {
					file.delete();
				}
			} catch (final Throwable e) {
				writable = false;
			}
		}

		if (!writable) {
			final Object[] options = { "Yes, let me select a directory", "No, I don't want to take a picture anymore" };
			final int dialogVal = edu.cmu.cs.stage3.swing.DialogManager.showOptionDialog(
					"Alice can not save the captured image. Do you want to select a new directory?",
					"Alice can't save the file", javax.swing.JOptionPane.YES_NO_OPTION,
					javax.swing.JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (dialogVal == javax.swing.JOptionPane.YES_OPTION) {
				final java.io.File parent = dir.getParentFile();
				try {
					browseFileChooser.setCurrentDirectory(parent);
				} catch (final ArrayIndexOutOfBoundsException aioobe) {
					// for some reason this can potentially fail in jdk1.4.2_04
				}
				boolean done = false;
				while (!done) {
					final int returnVal = edu.cmu.cs.stage3.swing.DialogManager.showOpenDialog(browseFileChooser);
					if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
						final java.io.File selectedFile = browseFileChooser.getSelectedFile();
						final java.io.File testCaptureFile = new java.io.File(selectedFile, "test.jpg");
						if (testCaptureFile.exists()) {
							writable = testCaptureFile.canWrite();
						} else {
							try {
								final boolean success = testCaptureFile.createNewFile();
								writable = success;
								if (success) {
									testCaptureFile.delete();
								}
							} catch (final Exception e) {
								writable = false;
							}
						}
						if (!writable) {
							edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
									"The capture directory specified can not be written to. Please choose another directory.");
						} else {
							done = true;
							dir = selectedFile;
						}
					} else {
						edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
								"You have not selected a writable directory to save pictures to.\n"
										+ "Alice will not be able to take pictures until you do so. You can go to Preferences->Screen Grab to set a directory.",
								"No directory set", javax.swing.JOptionPane.WARNING_MESSAGE);
						return;
					}
				}
			} else {
				edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
						"You have not selected a writable directory to save pictures to.\n"
								+ "Alice will not be able to take pictures until you do so. You can go to Preferences->Screen Grab to set a directory.",
						"No directory set", javax.swing.JOptionPane.WARNING_MESSAGE);
				return;
			}
			file = new java.io.File(dir, captureFormatter.format(i));
		}
		while (file.exists()) {
			i++;
			file = new java.io.File(dir, captureFormatter.format(i));
		}
		try {
			file.createNewFile();
			final java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
			final java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos);
			final java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
			edu.cmu.cs.stage3.image.ImageIO.store(codec, dos, image);
			dos.flush();
			fos.close();

			if (authoringToolConfig.getValue("screenCapture.informUser").equalsIgnoreCase("true")) {
				final java.awt.image.BufferedImage scaledImage = edu.cmu.cs.stage3.alice.authoringtool.util.GUIEffects
						.getImageScaledToLongestDimension(image, 128);
				final edu.cmu.cs.stage3.alice.authoringtool.dialog.CapturedImageContentPane capturedImageContentPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.CapturedImageContentPane();
				capturedImageContentPane.setStoreLocation(file.getCanonicalPath());
				capturedImageContentPane.setImage(scaledImage);
				edu.cmu.cs.stage3.swing.DialogManager.showDialog(capturedImageContentPane);
			}
			// } catch( InterruptedException ie ) {
			// } catch( java.io.IOException ioe ) {
		} catch (final Throwable t) {
			AuthoringTool.showErrorDialog("Error while storing screen capture.", t);
		}
	}

	public void performPointOfViewOneShot(final edu.cmu.cs.stage3.alice.core.Transformable transformable,
			final edu.cmu.cs.stage3.math.Matrix44 newTransformation) {
		final edu.cmu.cs.stage3.math.Matrix44 oldTransformation = transformable.localTransformation.getMatrix44Value();

		final edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation povAnimation = new edu.cmu.cs.stage3.alice.core.response.PointOfViewAnimation();
		povAnimation.subject.set(transformable);
		povAnimation.pointOfView.set(newTransformation);

		final edu.cmu.cs.stage3.alice.authoringtool.util.OneShotSimpleBehavior oneShotBehavior = new edu.cmu.cs.stage3.alice.authoringtool.util.OneShotSimpleBehavior();
		oneShotBehavior.setResponse(povAnimation);
		oneShotBehavior.setAffectedProperties(
				new edu.cmu.cs.stage3.alice.core.Property[] { transformable.localTransformation });
		oneShotBehavior.start(oneShotScheduler);

		final edu.cmu.cs.stage3.alice.authoringtool.util.PointOfViewUndoableRedoable undo = new edu.cmu.cs.stage3.alice.authoringtool.util.PointOfViewUndoableRedoable(
				transformable, oldTransformation, newTransformation, oneShotScheduler);
		undoRedoStack.push(undo);
	}

	public void performOneShot(final edu.cmu.cs.stage3.alice.core.Response response,
			final edu.cmu.cs.stage3.alice.core.Response undoResponse,
			final edu.cmu.cs.stage3.alice.core.Property[] affectedProperties) {
		final edu.cmu.cs.stage3.alice.authoringtool.util.OneShotSimpleBehavior oneShotBehavior = new edu.cmu.cs.stage3.alice.authoringtool.util.OneShotSimpleBehavior();
		oneShotBehavior.setResponse(response);
		oneShotBehavior.setAffectedProperties(affectedProperties);
		oneShotBehavior.start(oneShotScheduler);

		final edu.cmu.cs.stage3.alice.authoringtool.util.OneShotUndoableRedoable undo = new edu.cmu.cs.stage3.alice.authoringtool.util.OneShotUndoableRedoable(
				response, undoResponse, oneShotBehavior, oneShotScheduler);
		undoRedoStack.push(undo);
	}

	@Override
	public void lostOwnership(final java.awt.datatransfer.Clipboard clipboard,
			final java.awt.datatransfer.Transferable contents) {
		// TODO: store a reference to CastMembers until ownership is lost, then
		// put the whole thing in the clipboard
	}

	/*
	 * public void editCharacter( edu.cmu.cs.stage3.alice.core.Transformable
	 * character ) { characterEditorDialog.setCharacter( character ); if( !
	 * characterEditorDialog.isShowing() ) { characterEditorDialog.show(); } }
	 */

	public void makeBillboard() {
		setImportFileFilter("Image Files");
		importElement(null, null, new edu.cmu.cs.stage3.alice.authoringtool.util.PostImportRunnable() {
			@Override
			public void run() {
				final edu.cmu.cs.stage3.alice.core.TextureMap textureMap = (edu.cmu.cs.stage3.alice.core.TextureMap) getImportedElement();
				if (textureMap != null) {
					textureMap.removeFromParent();
					textureMap.name
							.set(AuthoringToolResources.getNameForNewChild(textureMap.name.getStringValue(), world));
					final edu.cmu.cs.stage3.alice.core.Billboard billboard = AuthoringToolResources
							.makeBillboard(textureMap, true);
					animateAddModelIfPossible(billboard, world);
					world.addChild(billboard);
					billboard.vehicle.set(world);
					world.sandboxes.add(billboard);
				}
			}
		}, true);
		// importObject( null, new
		// edu.cmu.cs.stage3.alice.authoringtool.util.BillboardPostWorker( world
		// ) );
	}

	public void setElementScope(final edu.cmu.cs.stage3.alice.core.Element element) {
		jAliceFrame.getWorldTreeComponent().setCurrentScope(element);
		if (element != null) {
			if (selectedElement != null) {
				if (!selectedElement.isDescendantOf(element)) {
					setSelectedElement(element);
				}
			} else {
				setSelectedElement(element);
			}
		}
	}

	// TODO: handle this correctly
	public edu.cmu.cs.stage3.alice.core.Camera getCurrentCamera() {
		// edu.cmu.cs.stage3.alice.core.Element[] e = world.search(new
		// edu.cmu.cs.stage3.util.criterion.InstanceOfCriterion(edu.cmu.cs.stage3.alice.core.Camera.class));
		final edu.cmu.cs.stage3.alice.core.Element[] e = world
				.getDescendants(edu.cmu.cs.stage3.alice.core.Camera.class);
		if (e != null) {
			return (edu.cmu.cs.stage3.alice.core.Camera) e[0];
		} else {
			return null;
		}
	}

	public void showAbout() {
		edu.cmu.cs.stage3.swing.DialogManager.showDialog(aboutContentPane);
	}

	public void showPreferences() {
		final int result = edu.cmu.cs.stage3.swing.DialogManager.showDialog(preferencesContentPane);
		if (result == edu.cmu.cs.stage3.swing.ContentPane.OK_OPTION) {
			preferencesContentPane.finalizeSelections();
		}
	}

	/*
	 * public static void showSaveErrorDialog(String message, Throwable t) {
	 * getHack().jAliceFrame.HACK_goToRedAlert();
	 * edu.cmu.cs.stage3.alice.authoringtool.dialog.SaveErrorContentPane
	 * errorPane = new
	 * edu.cmu.cs.stage3.alice.authoringtool.dialog.SaveErrorContentPane();
	 * errorPane.setMessage(message); errorPane.setSubmitBugButtonEnabled( true
	 * ); errorPane.setThrowable(t); int result =
	 * edu.cmu.cs.stage3.swing.DialogManager.showDialog(errorPane); if
	 * (getHack().isStdErrToConsole() && (t != null)) {
	 * t.printStackTrace(System.err); } }
	 */
	public static void showErrorDialog(final String message, final Object t) {
		showErrorDialog(message, t, false);
	}

	public static void showErrorDialog(final String message, final Object t, final boolean showSubmitBugButton) {
		final edu.cmu.cs.stage3.alice.authoringtool.dialog.ErrorContentPane errorPane = new edu.cmu.cs.stage3.alice.authoringtool.dialog.ErrorContentPane();
		errorPane.setMessage(message);
		// errorPane.setSubmitBugButtonEnabled(showSubmitBugButton);
		if (t instanceof Throwable) {
			errorPane.setThrowable((Throwable) t);
		} else {
			errorPane.setDetails((String) t);
		}
		edu.cmu.cs.stage3.swing.DialogManager.showDialog(errorPane);
		if (t instanceof Throwable) {
			if (getHack().isStdErrToConsole() && t != null) {
				final Throwable tt = (Throwable) t;
				tt.printStackTrace(System.err);
			}
		}
	}

	/*
	 * public static void showErrorDialogWithDetails(String message, String
	 * details) { showErrorDialogWithDetails(message, details, true); }
	 *
	 * public static void showErrorDialogWithDetails(String message, String
	 * details, boolean showSubmitBugButton) {
	 * edu.cmu.cs.stage3.alice.authoringtool.dialog.ErrorContentPane errorPane =
	 * new edu.cmu.cs.stage3.alice.authoringtool.dialog.ErrorContentPane();
	 * errorPane.setMessage(message);
	 * errorPane.setSubmitBugButtonEnabled(showSubmitBugButton); int result =
	 * edu.cmu.cs.stage3.swing.DialogManager.showDialog(errorPane); // if(
	 * getHack().isStdErrToConsole() && (t != null) ) { // t.printStackTrace(
	 * System.err ); // } }
	 */
	private void stopWorldAndShowDialog(Throwable throwable) {
		stopWorld();
		if (throwable instanceof edu.cmu.cs.stage3.alice.core.ExceptionWrapper) {
			throwable = ((edu.cmu.cs.stage3.alice.core.ExceptionWrapper) throwable).getWrappedException();
		}
		final Throwable t = throwable;
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (t instanceof edu.cmu.cs.stage3.alice.core.SimulationException) {
					showSimulationExceptionDialog((edu.cmu.cs.stage3.alice.core.SimulationException) t);
				} else {
					showErrorDialog("Error during simulation.", t);
				}
			}
		});
	}

	public void showSimulationExceptionDialog(
			final edu.cmu.cs.stage3.alice.core.SimulationException simulationException) {
		final edu.cmu.cs.stage3.alice.authoringtool.dialog.SimulationExceptionPanel simulationExceptionPanel = new edu.cmu.cs.stage3.alice.authoringtool.dialog.SimulationExceptionPanel(
				this);
		simulationExceptionPanel.setSimulationException(simulationException);
		simulationExceptionPanel.setErrorHighlightingEnabled(true);
		edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(simulationExceptionPanel, "Problem Detected",
				javax.swing.JOptionPane.ERROR_MESSAGE, AuthoringToolResources.getAliceSystemIcon());
		simulationExceptionPanel.setErrorHighlightingEnabled(false);
	}

	public javax.swing.JFileChooser getImportFileChooser() {
		return importFileChooser;
	}

	public edu.cmu.cs.stage3.alice.core.World getWorld() {
		return world;
	}

	public void showOnScreenHelp() {
		showStencils();
	}

	public void showStdErrOutDialog() {
		stdErrOutContentPane.setMode(edu.cmu.cs.stage3.alice.authoringtool.dialog.StdErrOutContentPane.HISTORY_MODE);
		stdErrOutContentPane.showStdErrOutDialog();
	}

	public void updateWorldOpenTime() {
		if (world != null) {
			final String worldOpenTimeString = (String) world.data
					.get("edu.cmu.cs.stage3.alice.authoringtool.worldOpenTime");
			long worldOpenTime = 0;
			if (worldOpenTimeString != null) {
				worldOpenTime = Long.parseLong(worldOpenTimeString);
			}
			worldOpenTime += System.currentTimeMillis() - worldLoadedTime;
			worldLoadedTime = System.currentTimeMillis();
			world.data.put("edu.cmu.cs.stage3.alice.authoringtool.worldOpenTime", Long.toString(worldOpenTime));
		}
	}

	public void countSomething(final String dataKey) {
		final String countString = (String) world.data.get(dataKey);
		int count = 0;
		if (countString != null) {
			count = Integer.parseInt(countString);
		}
		count++;
		world.data.put(dataKey, Integer.toString(count));
	}

	public void showPrintDialog() {
		final int result = edu.cmu.cs.stage3.swing.DialogManager.showDialog(exportCodeForPrintingContentPane);
		if (result == edu.cmu.cs.stage3.swing.ContentPane.OK_OPTION) {
			final java.io.File fileToExportTo = exportCodeForPrintingContentPane.getFileToExportTo();
			final edu.cmu.cs.stage3.progress.ProgressPane progressPane = new ProgressPane("Saving HTML...",
					"Saving: ") {
				private static final long serialVersionUID = 840287082339530378L;

				@Override
				protected void construct() throws edu.cmu.cs.stage3.progress.ProgressCancelException {
					try {
						final StringBuffer htmlOutput = new StringBuffer();
						exportCodeForPrintingContentPane.getHTML(htmlOutput, fileToExportTo, true, true, this);
						final java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
								new java.io.FileOutputStream(fileToExportTo));
						writer.write(htmlOutput.toString());
						writer.flush();
						writer.close();
					} catch (final edu.cmu.cs.stage3.progress.ProgressCancelException pce) {
						fileToExportTo.delete();
						throw pce;
					} catch (final Throwable t) {
						AuthoringTool.showErrorDialog("Unable to store world to file: " + fileToExportTo, t);
						fileToExportTo.delete();
					}
				}
			};
			edu.cmu.cs.stage3.swing.DialogManager.showDialog(progressPane);
		}
	}

	// /////////////////////////////
	// Private methods
	// /////////////////////////////

	private void setCurrentWorldLocation(final java.io.File file) {
		currentWorldLocation = file;
		updateTitle();
	}

	private void updateTitle() {
		String path = "";
		if (currentWorldLocation != null) {
			try {
				path = currentWorldLocation.getCanonicalPath();
			} catch (final java.io.IOException e) {
				path = currentWorldLocation.getAbsolutePath();
			}
		}

		String modifiedStatus = "";
		if (worldHasBeenModified) {
			modifiedStatus = "-  [Modified]";
		}

		jAliceFrame.setTitle("Alice (" + JAlice.getVersion() + ") " + path + modifiedStatus);
	}

	public java.awt.Component getEditorForElement(final edu.cmu.cs.stage3.alice.core.Element elementToEdit) {
		final int index = jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementToEdit);
		if (index > -1) {
			return jAliceFrame.tabbedEditorComponent.getEditorAt(index).getJComponent();
		} else {
			final Class<? extends Editor> editorClass = edu.cmu.cs.stage3.alice.authoringtool.util.EditorUtilities
					.getBestEditor(elementToEdit.getClass());
			final Editor editor = editorManager.getEditorInstance(editorClass);
			edu.cmu.cs.stage3.alice.authoringtool.util.EditorUtilities.editObject(editor, elementToEdit);
			return editor.getJComponent();
		}
	}

	// /////////////
	// Stencils
	// /////////////
	protected java.util.HashMap<String, JComponent> componentMap = new java.util.HashMap<String, JComponent>();
	protected edu.cmu.cs.stage3.caitlin.stencilhelp.client.StencilManager stencilManager;
	protected java.util.HashSet<Class<?>> classesToStopOn = new java.util.HashSet<Class<?>>();
	protected javax.swing.Timer updateTimer;
	protected boolean stencilDragging = false;
	protected java.awt.Component dragStartSource;
	protected java.io.File tutorialOne;
	protected java.io.File tutorialDirectory = new java.io.File(JAlice.getAliceHomeDirectory(), "tutorial")
			.getAbsoluteFile();
	protected java.util.ArrayList<WorldDifferencesCapsule> wayPoints = new java.util.ArrayList<WorldDifferencesCapsule>();

	private void stencilInit() {
		stencilManager = new edu.cmu.cs.stage3.caitlin.stencilhelp.client.StencilManager(this);
		jAliceFrame.setGlassPane(stencilManager.getStencilComponent());
		((javax.swing.JComponent) stencilManager.getStencilComponent()).setOpaque(false); // TODO:
																							// remove

		classesToStopOn.add(javax.swing.JMenu.class);
		classesToStopOn.add(javax.swing.AbstractButton.class);
		classesToStopOn.add(javax.swing.JComboBox.class);
		classesToStopOn.add(javax.swing.JList.class);
		classesToStopOn.add(javax.swing.JMenu.class);
		classesToStopOn.add(javax.swing.JSlider.class);
		classesToStopOn.add(javax.swing.text.JTextComponent.class);
		classesToStopOn.add(javax.swing.JTabbedPane.class);
		classesToStopOn.add(javax.swing.JTree.class);
		classesToStopOn.add(javax.swing.JTable.class);
		classesToStopOn.add(edu.cmu.cs.stage3.alice.authoringtool.DragFromComponent.class);
		classesToStopOn.add(edu.cmu.cs.stage3.alice.authoringtool.util.DnDGroupingPanel.class);
		classesToStopOn.add(edu.cmu.cs.stage3.alice.authoringtool.util.TrashComponent.class);
		classesToStopOn.add(edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.PropertyViewController.class);
		classesToStopOn
				.add(edu.cmu.cs.stage3.alice.authoringtool.editors.behaviorgroupseditor.BehaviorGroupsEditor.class);
		classesToStopOn.add(edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.SceneEditor.class);
		classesToStopOn.add(edu.cmu.cs.stage3.alice.authoringtool.galleryviewer.GalleryViewer.class);
		classesToStopOn.add(edu.cmu.cs.stage3.alice.authoringtool.galleryviewer.GalleryObject.class);
		// classesToStopOn.add(
		// edu.cmu.cs.stage3.alice.authoringtool.util.GuiNavigator.class );

		componentMap.put("fileMenu", jAliceFrame.fileMenu);
		componentMap.put("editMenu", jAliceFrame.editMenu);
		componentMap.put("toolsMenu", jAliceFrame.toolsMenu);
		componentMap.put("helpMenu", jAliceFrame.helpMenu);
		componentMap.put("playButton", jAliceFrame.playButton);
		componentMap.put("addObjectButton", jAliceFrame.addObjectButton);
		componentMap.put("undoButton", jAliceFrame.undoButton);
		componentMap.put("redoButton", jAliceFrame.redoButton);
		componentMap.put("trashComponent", jAliceFrame.trashComponent);
		componentMap.put("clipboardPanel", jAliceFrame.clipboardPanel);
		componentMap.put("objectTree", jAliceFrame.worldTreeComponent);
		componentMap.put("sceneEditor", jAliceFrame.sceneEditor);
		componentMap.put("details", jAliceFrame.dragFromComponent);
		componentMap.put("behaviors", jAliceFrame.behaviorGroupsEditor);
		componentMap.put("editors", jAliceFrame.tabbedEditorComponent);

		updateTimer = new javax.swing.Timer(100, new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent ev) {
				stencilManager.update();
				// System.out.println( "update: " + System.currentTimeMillis()
				// );
			}
		});
		updateTimer.setRepeats(false);
		final java.awt.event.AWTEventListener updateListener = new java.awt.event.AWTEventListener() {
			@Override
			public void eventDispatched(final java.awt.AWTEvent ev) {
				if (ev.getSource() instanceof java.awt.Component) {
					if (ev.getSource() == jAliceFrame
							|| jAliceFrame.isAncestorOf((java.awt.Component) ev.getSource())) {
						updateTimer.restart();
					}
				}
			}
		};
		java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
				updateListener, /*
								 * java . awt . AWTEvent . PAINT_EVENT_MASK |
								 */
				java.awt.AWTEvent.CONTAINER_EVENT_MASK | java.awt.AWTEvent.HIERARCHY_EVENT_MASK
						| java.awt.AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK);

		// special case updates
		jAliceFrame.worldTreeComponent.worldTree
				.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
					@Override
					public void treeCollapsed(final javax.swing.event.TreeExpansionEvent ev) {
						updateTimer.restart();
					}

					@Override
					public void treeExpanded(final javax.swing.event.TreeExpansionEvent ev) {
						updateTimer.restart();
					}
				});

		tutorialOne = new java.io.File(JAlice.getAliceHomeDirectory(),
				"tutorial" + java.io.File.separator + "Tutorial1.stl");
	}

	public void hackStencilUpdate() { // used in specific places in
										// authoringtool code to notify stencils
										// of updates not otherwise caught
		if (updateTimer != null) {
			updateTimer.restart();
		}
	}

	protected java.awt.Dimension oldDimension;
	protected java.awt.Point oldPosition;
	protected int oldLeftRightSplitPaneLocation;
	protected int oldWorldTreeDragFromSplitPaneLocation;
	protected int oldEditorBehaviorSplitPaneLocation;
	protected int oldSmallSceneBehaviorSplitPaneLocation;
	protected java.awt.Dimension oldRenderWindowSize;
	protected java.awt.Point oldRenderWindowPosition;
	protected boolean oldShouldConstrain;
	protected java.awt.Rectangle oldRenderBounds;

	public void showStencils() {
		setLayout();
		jAliceFrame.dragFromComponent.selectTab(edu.cmu.cs.stage3.alice.authoringtool.DragFromComponent.ANIMATIONS_TAB);
		jAliceFrame.removeKeyListener(stencilManager);
		jAliceFrame.addKeyListener(stencilManager);
		jAliceFrame.requestFocus();
		stencilManager.showStencils(true);
		authoringToolConfig.setValue("doNotShowUnhookedMethodWarning", "true");
	}

	protected void restoreLayout() {
		jAliceFrame.setResizable(true);
		jAliceFrame.leftRightSplitPane.setDividerLocation(oldLeftRightSplitPaneLocation);
		jAliceFrame.worldTreeDragFromSplitPane.setDividerLocation(oldWorldTreeDragFromSplitPaneLocation);
		jAliceFrame.editorBehaviorSplitPane.setDividerLocation(oldEditorBehaviorSplitPaneLocation);
		jAliceFrame.smallSceneBehaviorSplitPane.setDividerLocation(oldSmallSceneBehaviorSplitPaneLocation);
		jAliceFrame.doLayout();
		if (oldShouldConstrain) {
			authoringToolConfig.setValue("rendering.constrainRenderDialogAspectRatio", "true");
		} else {
			authoringToolConfig.setValue("rendering.constrainRenderDialogAspectRatio", "false");
		}
		renderContentPane.saveRenderBounds(oldRenderBounds);
		jAliceFrame.setSize(oldDimension);
		jAliceFrame.setLocation(oldPosition);

	}

	protected void setLayout() {
		final int screenWidth = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		final int screenHeight = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		// int height = screenHeight - 28;

		final java.awt.Dimension d = getScreenSize();
		oldDimension = new java.awt.Dimension(d.width, d.height);
		oldPosition = jAliceFrame.getLocation();
		final java.awt.Point newPosition = new java.awt.Point(oldPosition.x, oldPosition.y);
		int newWidth = d.width;
		int newHeight = d.height;
		if (d.width > 1032) {
			newWidth = 1032;
		} else if (d.width < 1024 && screenWidth >= 1024) {
			newWidth = 1032;
		}
		if (d.height > 776) {
			newHeight = 740;
		} else if (d.height < 740 && screenWidth >= 768) {
			newHeight = 740;
		}
		if (oldPosition.x + newWidth > screenWidth + 4) {
			newPosition.x = (screenWidth - newWidth) / 2;
		}
		if (oldPosition.y + newHeight > screenHeight + 4) {
			newPosition.y = (screenHeight - 28 - newHeight) / 2;
		}
		boolean shouldModifyStuff = true;
		if (screenWidth < 1024 || screenHeight < 740) {
			edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
					"Your screen resolution is lower than what we recommend for running the tutorial.\n"
							+ "Alice will still run the tutorial, but some of the objects may not line up well.",
					"Low Resolution Warning", javax.swing.JOptionPane.WARNING_MESSAGE, null);
			shouldModifyStuff = false;
		}
		oldLeftRightSplitPaneLocation = jAliceFrame.leftRightSplitPane.getDividerLocation();
		oldWorldTreeDragFromSplitPaneLocation = jAliceFrame.worldTreeDragFromSplitPane.getDividerLocation();
		oldEditorBehaviorSplitPaneLocation = jAliceFrame.editorBehaviorSplitPane.getDividerLocation();
		oldSmallSceneBehaviorSplitPaneLocation = jAliceFrame.smallSceneBehaviorSplitPane.getDividerLocation();
		oldRenderWindowSize = renderContentPane.getSize();
		oldRenderWindowPosition = renderContentPane.getLocation();

		oldShouldConstrain = authoringToolConfig.getValue("rendering.constrainRenderDialogAspectRatio")
				.equalsIgnoreCase("true");
		authoringToolConfig.setValue("rendering.constrainRenderDialogAspectRatio", "false");

		oldRenderBounds = renderContentPane.getRenderBounds();
		renderContentPane.saveRenderBounds(new java.awt.Rectangle(191, 199, 400, 300));

		if ((newHeight != oldDimension.height || newWidth != oldDimension.width) && shouldModifyStuff) {
			edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
					"Alice is going to adjust your screen size to make the tutorial fit on the screen better.\n"
							+ "When you exit the tutorial your original screen size will be restored.",
					"Different Resolution Warning", javax.swing.JOptionPane.WARNING_MESSAGE, null);

		}
		if (shouldModifyStuff) {
			jAliceFrame.leftRightSplitPane.setDividerLocation(250);
			jAliceFrame.worldTreeDragFromSplitPane.setDividerLocation(237);
			jAliceFrame.editorBehaviorSplitPane.setDividerLocation(204);
			jAliceFrame.smallSceneBehaviorSplitPane.setDividerLocation(224);
			try {
				Thread.sleep(100);
			} catch (final Exception e) {
			}
			jAliceFrame.setSize(newWidth, newHeight);
			jAliceFrame.setLocation(newPosition);
		}

		// java.awt.Dimension targetDimension = new java.awt.Dimension(newWidth,
		// newHeight);
		jAliceFrame.doLayout();
		jAliceFrame.setResizable(false);
	}

	public void hideStencils() {
		restoreLayout();
		stencilManager.showStencils(false);
		authoringToolConfig.setValue("doNotShowUnhookedMethodWarning", "false");
		jAliceFrame.removeKeyListener(stencilManager);
		jAliceFrame.requestFocus();
	}

	@Override
	public void setGlassPane(final java.awt.Component c) {
		jAliceFrame.setGlassPane(c);
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible) {
			setLayout();
			authoringToolConfig.setValue("doNotShowUnhookedMethodWarning", "true");
			jAliceFrame.removeKeyListener(stencilManager);
			jAliceFrame.addKeyListener(stencilManager);
			jAliceFrame.requestFocus();
		} else {
			restoreLayout();
			authoringToolConfig.setValue("doNotShowUnhookedMethodWarning", "false");
			jAliceFrame.removeKeyListener(stencilManager);
			jAliceFrame.requestFocus();
		}
	}

	// Caitlin's code

	@Override
	public edu.cmu.cs.stage3.caitlin.stencilhelp.application.StateCapsule getStateCapsuleFromString(
			final String capsuleString) {
		final edu.cmu.cs.stage3.alice.authoringtool.util.StencilStateCapsule sc = new edu.cmu.cs.stage3.alice.authoringtool.util.StencilStateCapsule();
		sc.parse(capsuleString);
		return sc;
	}

	@Override
	public edu.cmu.cs.stage3.caitlin.stencilhelp.application.StateCapsule getCurrentState() {
		if (wayPoints.size() > 0) {
			final edu.cmu.cs.stage3.alice.authoringtool.util.WorldDifferencesCapsule currentWayPoint = wayPoints.get(0);
			final edu.cmu.cs.stage3.alice.authoringtool.util.StencilStateCapsule capsule = currentWayPoint
					.getStateCapsule();

			// this.doesStateMatch(capsule);
			return capsule;
		}
		return null;
	}

	// public void newSlide() {}
	// public void clearSlide() {}

	protected java.awt.Component getValidComponent(java.awt.Component c) {
		while (c != null) {
			// special cases
			if (c instanceof javax.swing.JButton && c
					.getParent() instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.PropertyViewController) {
				c = c.getParent();
			} else if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.ElementNamePropertyViewController) {
				c = c.getParent();
			} else if (c instanceof javax.swing.AbstractButton && c.getParent() instanceof javax.swing.JComboBox) {
				c = c.getParent();
			} else if (c instanceof javax.swing.JTextField && c.getParent() instanceof javax.swing.JComboBox) {
				c = c.getParent();
			} else if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.util.ImagePanel
					&& c.getParent() instanceof edu.cmu.cs.stage3.alice.authoringtool.util.GuiNavigator) {
				return c;
			} else if (c instanceof javax.swing.JLabel && "more...".equals(((javax.swing.JLabel) c).getText())) {
				return c;
			} else if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.SceneEditor) {
				return ((edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.SceneEditor) c).getRenderPanel();
				// } else if( (c instanceof javax.swing.JScrollPane) &&
				// (c.getParent() instanceof
				// edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)
				// && (c ==
				// ((edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)c.getParent()).getWorkSpace())
				// ) {
				// return c;
				// } else if( (c instanceof javax.swing.JPanel) &&
				// (c.getParent() != null) && (c.getParent().getParent()
				// instanceof
				// edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)
				// && (c ==
				// ((edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)c.getParent().getParent()).getParameterPanel())
				// ) {
				// return c;
				// } else if( (c instanceof javax.swing.JPanel) &&
				// (c.getParent() != null) && (c.getParent().getParent()
				// instanceof
				// edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)
				// && (c ==
				// ((edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)c.getParent().getParent()).getVariablePanel())
				// ) {
				// return c;
				// } else if( (c instanceof javax.swing.JPanel) &&
				// (c.getParent() != null) && (c.getParent().getParent()
				// instanceof
				// edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)
				// && (c ==
				// ((edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)c.getParent().getParent()).getDoNothingPanel())
				// ) {
				// return c;
			}

			// default cases
			for (final Class<?> stopClass : classesToStopOn) {
				if (stopClass.isAssignableFrom(c.getClass())) {
					return c;
				}
			}

			// look up one level
			c = c.getParent();
		}

		return null;
	}

	@Override
	public String getIDForPoint(java.awt.Point p, final boolean dropSite) {
		p = javax.swing.SwingUtilities.convertPoint(jAliceFrame.getGlassPane(), p, jAliceFrame.getLayeredPane()); // shouldn't
																													// be
																													// necessary;
																													// just
																													// being
																													// careful
		java.awt.Component c = jAliceFrame.getRootPane().getLayeredPane().findComponentAt(p);
		c = getValidComponent(c);
		if (c == null) {
			return null;
		}
		final java.awt.Point localPoint = javax.swing.SwingUtilities
				.convertPoint(jAliceFrame.getRootPane().getLayeredPane(), p, c);

		String key = null;
		if (c instanceof javax.swing.JTree && jAliceFrame.worldTreeComponent.isAncestorOf(c)) {
			final javax.swing.JTree tree = (javax.swing.JTree) c;
			final javax.swing.tree.TreePath treePath = tree.getClosestPathForLocation(localPoint.x, localPoint.y);
			final java.awt.Rectangle bounds = tree.getPathBounds(treePath);
			key = "objectTree";
			if (bounds.contains(localPoint)) {
				final String elementKey = ((edu.cmu.cs.stage3.alice.core.Element) treePath.getLastPathComponent())
						.getKey(world);
				key += "<" + elementKey + ">";
			}
		} else if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.util.DnDClipboard) {
			key = "clipboard";
			final java.awt.Component[] components = c.getParent().getComponents();
			int index = -1;
			for (int i = 0; i < components.length; i++) {
				if (components[i] == c) {
					index = i;
				}
			}
			key += "<" + Integer.toString(index) + ">";

		} else if (jAliceFrame.sceneEditor.isAncestorOf(c) || c == jAliceFrame.sceneEditor) {
			if (jAliceFrame.sceneEditor
					.getGuiMode() == edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.SceneEditor.LARGE_MODE) {
				key = "sceneEditor<large>";
			} else {
				key = "sceneEditor<small>";
			}
			if (jAliceFrame.sceneEditor
					.getViewMode() == edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.CameraViewPanel.QUAD_VIEW_MODE) {
				key += ":quadView";
			} else {
				key += ":singleView";
			}

			if (jAliceFrame.sceneEditor.getGalleryViewer().isAncestorOf(c)
					|| c == jAliceFrame.sceneEditor.getGalleryViewer()) {
				key += ":galleryViewer<" + jAliceFrame.sceneEditor.getGalleryViewer().getDirectory() + ">";

				if (c instanceof javax.swing.JButton) {
					key += ":button<" + ((javax.swing.JButton) c).getText() + ">";
				} else if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.galleryviewer.GalleryObject) {
					key += ":galleryObject<" + ((edu.cmu.cs.stage3.alice.authoringtool.galleryviewer.GalleryObject) c)
							.getUniqueIdentifier() + ">";
				} else if (c != jAliceFrame.sceneEditor.getGalleryViewer()) {
					key = null;
				}
			} else {
				final String id = jAliceFrame.sceneEditor.getIdForComponent(c);
				// System.out.println( c + " --> " + id );
				if (id != null) {
					key += ":" + id;
				} else if (c != jAliceFrame.sceneEditor) {
					key = null;
				}
			}
		} else if (jAliceFrame.dragFromComponent.isAncestorOf(c) || c == jAliceFrame.dragFromComponent) {
			if (jAliceFrame.dragFromComponent.getElement() == null) {
				key = "details";
			} else {
				key = "details<" + jAliceFrame.dragFromComponent.getElement().getKey(world) + ">";
				if (c instanceof javax.swing.JTabbedPane) {
					final int whichTab = ((javax.swing.JTabbedPane) c).getUI()
							.tabForCoordinate((javax.swing.JTabbedPane) c, localPoint.x, localPoint.y);
					if (whichTab > -1) {
						key += ":tab<" + Integer.toString(whichTab) + ">";
					}
				} else if (c != jAliceFrame.dragFromComponent) {
					key += ":" + jAliceFrame.dragFromComponent.getKeyForComponent(c);
				}
			}
		} else if (jAliceFrame.behaviorGroupsEditor.isAncestorOf(c)) {
			key = "behaviors";
			if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.util.DnDGroupingPanel) {
				try {
					final java.awt.datatransfer.Transferable transferable = ((edu.cmu.cs.stage3.alice.authoringtool.util.DnDGroupingPanel) c)
							.getTransferable();
					if (AuthoringToolResources.safeIsDataFlavorSupported(transferable,
							edu.cmu.cs.stage3.alice.authoringtool.datatransfer.ElementReferenceTransferable.elementReferenceFlavor)) {
						final edu.cmu.cs.stage3.alice.core.Element e = (edu.cmu.cs.stage3.alice.core.Element) transferable
								.getTransferData(
										edu.cmu.cs.stage3.alice.authoringtool.datatransfer.ElementReferenceTransferable.elementReferenceFlavor);
						key += ":elementTile<" + e.getKey(world) + ">";
					} else {
						key = null;
					}
				} catch (final Exception e) {
					AuthoringTool.showErrorDialog("Error while examining DnDGroupingPanel.", e);
					key = null;
				}
			} else if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.PropertyViewController) {
				final edu.cmu.cs.stage3.alice.core.Property property = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.PropertyViewController) c)
						.getProperty();
				final edu.cmu.cs.stage3.alice.core.Element element = property.getOwner();
				key += ":elementTile<" + element.getKey(world) + ">";
				key += ":property<" + property.getName() + ">";
				// TODO: handle user-defined parameters
			} else if (c instanceof javax.swing.JLabel && ((javax.swing.JLabel) c).getText().equals("more...")) {
				if (c.getParent().getParent()
						.getParent() instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
					final edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController vc = (edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) c
							.getParent().getParent().getParent();
					final edu.cmu.cs.stage3.alice.core.Element element = vc.getElement();
					key += ":elementTile<" + element.getKey(world) + ">";
					key += ":more";
				} else {
					key = null;
				}
			} else if (c instanceof javax.swing.JButton) {
				if (((javax.swing.JButton) c).getText().equals("create new event")) { // HACK
					key += ":createNewEventButton";
				} else {
					key = null;
				}
			}
		} else if (jAliceFrame.tabbedEditorComponent.isAncestorOf(c)) {
			key = "editors";
			if (c instanceof javax.swing.JTabbedPane) {
				final int whichTab = ((javax.swing.JTabbedPane) c).getUI().tabForCoordinate((javax.swing.JTabbedPane) c,
						localPoint.x, localPoint.y);
				if (whichTab > -1) {
					final Object o = jAliceFrame.tabbedEditorComponent.getObjectBeingEditedAt(whichTab);
					if (o instanceof edu.cmu.cs.stage3.alice.core.Element) {
						key += ":element<" + ((edu.cmu.cs.stage3.alice.core.Element) o).getKey(world) + ">";
					} else {
						key = null;
					}
				}
			} else {
				final Object o = jAliceFrame.tabbedEditorComponent.getObjectBeingEdited();
				if (o instanceof edu.cmu.cs.stage3.alice.core.Element) {
					key += ":element<" + ((edu.cmu.cs.stage3.alice.core.Element) o).getKey(world) + ">";
					// edu.cmu.cs.stage3.alice.authoringtool.editors.responseeditor.ResponseEditor
					// responseEditor = null;
					// if( jAliceFrame.tabbedEditorComponent.getCurrentEditor()
					// instanceof
					// edu.cmu.cs.stage3.alice.authoringtool.editors.responseeditor.ResponseEditor
					// ) {
					// responseEditor =
					// (edu.cmu.cs.stage3.alice.authoringtool.editors.responseeditor.ResponseEditor)jAliceFrame.tabbedEditorComponent.getCurrentEditor();
					// }
					// if( (c instanceof javax.swing.JScrollPane) &&
					// (c.getParent() instanceof
					// edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)
					// && (c ==
					// ((edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)c.getParent()).getWorkSpace())
					// ) {
					// key += ":compositeEditorWorkSpace";
					// } else if( (c instanceof javax.swing.JPanel) &&
					// (c.getParent() != null) && (c.getParent().getParent()
					// instanceof
					// edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)
					// && (c ==
					// ((edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)c.getParent().getParent()).getParameterPanel())
					// ) {
					// key += ":compositeEditorParameterPanel";
					// } else if( (c instanceof javax.swing.JPanel) &&
					// (c.getParent() != null) && (c.getParent().getParent()
					// instanceof
					// edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)
					// && (c ==
					// ((edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)c.getParent().getParent()).getVariablePanel())
					// ) {
					// key += ":compositeEditorVariablePanel";
					// } else if( (c instanceof javax.swing.JPanel) &&
					// (c.getParent() != null) && (c.getParent().getParent()
					// instanceof
					// edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)
					// && (c ==
					// ((edu.cmu.cs.stage3.alice.authoringtool.editors.compositeeditor.MainCompositeElementPanel)c.getParent().getParent()).getDoNothingPanel())
					// ) {
					// key += ":compositeEditorDoNothingPanel";
					if (c instanceof javax.swing.JButton) {
						key += ":button<" + ((javax.swing.JButton) c).getText() + ">";
					} else if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.util.DnDGroupingPanel) {
						try {
							final java.awt.datatransfer.Transferable transferable = ((edu.cmu.cs.stage3.alice.authoringtool.util.DnDGroupingPanel) c)
									.getTransferable();
							if (AuthoringToolResources.safeIsDataFlavorSupported(transferable,
									edu.cmu.cs.stage3.alice.authoringtool.datatransfer.ElementReferenceTransferable.elementReferenceFlavor)) {
								final edu.cmu.cs.stage3.alice.core.Element e = (edu.cmu.cs.stage3.alice.core.Element) transferable
										.getTransferData(
												edu.cmu.cs.stage3.alice.authoringtool.datatransfer.ElementReferenceTransferable.elementReferenceFlavor);
								key += ":elementTile<" + e.getKey(world) + ">";
							} else if (AuthoringToolResources.safeIsDataFlavorSupported(transferable,
									edu.cmu.cs.stage3.alice.authoringtool.datatransfer.ElementPrototypeReferenceTransferable.elementPrototypeReferenceFlavor)) {
								final edu.cmu.cs.stage3.alice.authoringtool.util.ElementPrototype ep = (edu.cmu.cs.stage3.alice.authoringtool.util.ElementPrototype) transferable
										.getTransferData(
												edu.cmu.cs.stage3.alice.authoringtool.datatransfer.ElementPrototypeReferenceTransferable.elementPrototypeReferenceFlavor);
								key += ":elementPrototypeTile<" + ep.getElementClass().getName() + ">";
							} else {
								key = null;
							}
							// TODO: handle other DnDGroupingPanels
						} catch (final Exception e) {
							AuthoringTool.showErrorDialog("Error while examining DnDGroupingPanel.", e);
							key = null;
						}
					} else if (c instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.PropertyViewController) {
						final edu.cmu.cs.stage3.alice.core.Property property = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.PropertyViewController) c)
								.getProperty();
						final edu.cmu.cs.stage3.alice.core.Element element = property.getOwner();
						key += ":elementTile<" + element.getKey(world) + ">";
						key += ":property<" + property.getName() + ">";
						// TODO: handle user-defined parameters
					} else if (c instanceof javax.swing.JLabel
							&& ((javax.swing.JLabel) c).getText().equals("more...")) {
						if (c.getParent().getParent()
								.getParent() instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
							final edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController vc = (edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) c
									.getParent().getParent().getParent();
							final edu.cmu.cs.stage3.alice.core.Element element = vc.getElement();
							key += ":elementTile<" + element.getKey(world) + ">";
							key += ":more";
						} else {
							key = null;
						}
					}
				} else {
					key = null;
				}
			}
		} else if (componentMap.containsValue(c)) {
			for (final String k : componentMap.keySet()) {
				if (c.equals(componentMap.get(k))) {
					key = k;
					break;
				}
			}
		}
		// System.out.println( key );
		return key;
	}

	private java.awt.Image getComponentImage(final java.awt.Component c) {
		final java.awt.Rectangle bounds = c.getBounds();
		final java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(bounds.width, bounds.height,
				java.awt.image.BufferedImage.TYPE_INT_ARGB);
		final java.awt.Graphics2D g = image.createGraphics();
		c.paintAll(g);
		return image;
	}

	public java.awt.Image getImageForID(final String id) {
		java.awt.Rectangle r = null;
		java.awt.Image image = null;
		final java.util.StringTokenizer st = new java.util.StringTokenizer(id, ":", false);
		if (st.hasMoreTokens()) {
			String token = st.nextToken();
			String prefix = AuthoringToolResources.getPrefix(token);
			String spec = AuthoringToolResources.getSpecifier(token);
			if (prefix.equals("objectTree")) {
				if (spec != null) {
					final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
					if (element != null) {
						final javax.swing.JTree tree = jAliceFrame.worldTreeComponent.worldTree;
						final edu.cmu.cs.stage3.alice.authoringtool.util.WorldTreeModel worldTreeModel = (edu.cmu.cs.stage3.alice.authoringtool.util.WorldTreeModel) tree
								.getModel();
						r = tree.getPathBounds(new javax.swing.tree.TreePath(worldTreeModel.getPath(element)));
						if (r != null && !worldTreeModel.isLeaf(element)) { // HACK
																			// to
																			// include
																			// expand
																			// handle
							r.x -= 15;
							r.width += 15;
						}
						if (r != null) {
							r = javax.swing.SwingUtilities.convertRectangle(tree, r, jAliceFrame.getGlassPane());
						}
					}
				} else {
					r = jAliceFrame.worldTreeComponent.getBounds();
					r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.worldTreeComponent.getParent(), r,
							jAliceFrame.getGlassPane());
				}
			} else if (prefix.equals("clipboard")) {
				if (spec != null) {
					try {
						final int index = Integer.parseInt(spec);
						if (index > -1) {
							final java.awt.Component c = jAliceFrame.clipboardPanel.getComponent(index);
							if (c != null) {
								image = getComponentImage(c);
							}
						}
					} catch (final Exception e) {
					}
				}
			} else if (prefix.equals("sceneEditor")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken(); // pull off singleView/quadView,
											// assume we're in the right mode
					if (st.hasMoreTokens()) {
						token = st.nextToken();
						prefix = AuthoringToolResources.getPrefix(token);
						spec = AuthoringToolResources.getSpecifier(token);
						if (prefix.equals("galleryViewer")) {
							jAliceFrame.sceneEditor.getGalleryViewer().setDirectory(spec);
							if (st.hasMoreTokens()) {
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("button")) {
									final java.awt.Component c = AuthoringToolResources
											.findButton(jAliceFrame.sceneEditor.getGalleryViewer(), spec);
									if (c != null) {
										image = getComponentImage(c);
									}
								} else if (prefix.equals("galleryObject")) {
									final java.awt.Component c = AuthoringToolResources
											.findGalleryObject(jAliceFrame.sceneEditor.getGalleryViewer(), spec);
									if (c != null) {
										image = getComponentImage(c);
									}
								}
							}
						} else {
							final java.awt.Component c = jAliceFrame.sceneEditor.getComponentForId(token);
							if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
								image = getComponentImage(c);
							}
						}
					}
				}
			} else if (prefix.equals("details")) {
				if (spec != null) {
					final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
					if (jAliceFrame.dragFromComponent.getElement().equals(element)) {
						if (st.hasMoreTokens()) {
							token = st.nextToken();
							prefix = AuthoringToolResources.getPrefix(token);
							spec = AuthoringToolResources.getSpecifier(token);
							if (prefix.equals("viewController")) {
								if (st.hasMoreTokens()) {
									token = st.nextToken();
									prefix = AuthoringToolResources.getPrefix(token);
									spec = AuthoringToolResources.getSpecifier(token);
									final java.awt.Component c = jAliceFrame.dragFromComponent
											.getPropertyViewComponentForKey(token);
									if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
										image = getComponentImage(c);
									}
								}
							} else {
								final java.awt.Component c = jAliceFrame.dragFromComponent.getComponentForKey(token);
								if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
									image = getComponentImage(c);
								}
							}
						} else {
							image = getComponentImage(jAliceFrame.dragFromComponent);
						}
					}
				} else {
					image = getComponentImage(jAliceFrame.dragFromComponent);
				}
			} else if (prefix.equals("behaviors")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken();
					prefix = AuthoringToolResources.getPrefix(token);
					spec = AuthoringToolResources.getSpecifier(token);
					if (prefix.equals("createNewEventButton")) {
						final java.awt.Component c = AuthoringToolResources.findButton(jAliceFrame.behaviorGroupsEditor,
								"create new event");
						if (c != null) {
							image = getComponentImage(c);
						}
					} else if (prefix.equals("elementTile") && spec != null) {
						final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
						if (element != null) {
							if (st.hasMoreTokens()) {
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("property") && spec != null) {
									final java.awt.Component c = AuthoringToolResources.findPropertyViewController(
											jAliceFrame.behaviorGroupsEditor, element, spec);
									if (c != null) {
										image = getComponentImage(c);
									}
								} else if (prefix.equals("more")) {
									final java.awt.Component dndPanel = AuthoringToolResources
											.findElementDnDPanel(jAliceFrame.behaviorGroupsEditor, element);
									if (dndPanel instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
										final java.awt.Component moreTile = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) dndPanel)
												.getMoreTile();
										if (moreTile != null && moreTile.isShowing()) {
											image = getComponentImage(moreTile);
										}
									}
								}
							} else {
								final java.awt.Component c = AuthoringToolResources
										.findElementDnDPanel(jAliceFrame.behaviorGroupsEditor, element);
								if (c != null) {
									image = getComponentImage(c);
								}
							}
						}
					}
				}
			} else if (prefix.equals("editors")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken();
					prefix = AuthoringToolResources.getPrefix(token);
					spec = AuthoringToolResources.getSpecifier(token);
					if (prefix.equals("element")) {
						final edu.cmu.cs.stage3.alice.core.Element elementBeingEdited = world.getDescendantKeyed(spec);
						if (st.hasMoreTokens()) {
							if (jAliceFrame.tabbedEditorComponent.getObjectBeingEdited() != null
									&& jAliceFrame.tabbedEditorComponent.getObjectBeingEdited()
											.equals(elementBeingEdited)) {
								// Editor editor =
								// jAliceFrame.tabbedEditorComponent.getEditorAt(
								// jAliceFrame.tabbedEditorComponent.getIndexOfObject(
								// elementBeingEdited ) );
								final java.awt.Container container = (java.awt.Container) jAliceFrame.tabbedEditorComponent.tabbedPane
										.getComponentAt(
												jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementBeingEdited));
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("button")) {
									final java.awt.Component c = AuthoringToolResources.findButton(container, spec);
									if (c != null) {
										image = getComponentImage(c);
									}
								} else if (prefix.equals("elementTile") && spec != null) {
									final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
									if (element != null) {
										if (st.hasMoreTokens()) {
											token = st.nextToken();
											prefix = AuthoringToolResources.getPrefix(token);
											spec = AuthoringToolResources.getSpecifier(token);
											if (prefix.equals("property") && spec != null) {
												final java.awt.Component c = AuthoringToolResources
														.findPropertyViewController(container, element, spec);
												if (c != null) {
													image = getComponentImage(c);
												}
											} else if (prefix.equals("more")) {
												final java.awt.Component dndPanel = AuthoringToolResources
														.findElementDnDPanel(container, element);
												if (dndPanel instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
													final java.awt.Component moreTile = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) dndPanel)
															.getMoreTile();
													if (moreTile != null && moreTile.isShowing()) {
														image = getComponentImage(moreTile);
													}
												}
											}
										} else {
											final java.awt.Component c = AuthoringToolResources
													.findElementDnDPanel(container, element);
											if (c != null) {
												image = getComponentImage(c);
											}
										}
									}
								} else if (prefix.equals("elementPrototypeTile") && spec != null) {
									try {
										final Class<?> elementClass = Class.forName(spec);
										if (elementClass != null) {
											final java.awt.Component c = AuthoringToolResources
													.findPrototypeDnDPanel(container, elementClass);
											if (c != null) {
												image = getComponentImage(c);
											}
										}
									} catch (final Exception e) {
										AuthoringTool.showErrorDialog(
												"Error while looking for ProtoypeDnDPanel using class " + spec, e);
									}
								}
							}
						} else {
							final int tabIndex = jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementBeingEdited);
							if (tabIndex >= 0
									&& tabIndex < jAliceFrame.tabbedEditorComponent.tabbedPane.getComponentCount()) {
								r = jAliceFrame.tabbedEditorComponent.tabbedPane.getUI()
										.getTabBounds(jAliceFrame.tabbedEditorComponent.tabbedPane, tabIndex);
								r = javax.swing.SwingUtilities.convertRectangle(
										jAliceFrame.tabbedEditorComponent.tabbedPane, r, jAliceFrame.getGlassPane());
							}
						}
					}
				} else {
					r = jAliceFrame.tabbedEditorComponent.getBounds();
					r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.tabbedEditorComponent.getParent(), r,
							jAliceFrame.getGlassPane());
				}
			} else if (componentMap.containsKey(prefix)) {
				final java.awt.Component c = componentMap.get(prefix);
				if (c != null) {
					image = getComponentImage(c);
				}
			}
		}
		return image;
	}

	@Override
	public java.awt.Rectangle getBoxForID(final String id)
			throws edu.cmu.cs.stage3.caitlin.stencilhelp.application.IDDoesNotExistException {
		java.awt.Rectangle r = null;
		final java.util.StringTokenizer st = new java.util.StringTokenizer(id, ":", false);
		if (st.hasMoreTokens()) {
			String token = st.nextToken();
			String prefix = AuthoringToolResources.getPrefix(token);
			String spec = AuthoringToolResources.getSpecifier(token);
			if (prefix.equals("objectTree")) {
				if (spec != null) {
					final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
					if (element != null) {
						final javax.swing.JTree tree = jAliceFrame.worldTreeComponent.worldTree;
						final edu.cmu.cs.stage3.alice.authoringtool.util.WorldTreeModel worldTreeModel = (edu.cmu.cs.stage3.alice.authoringtool.util.WorldTreeModel) tree
								.getModel();
						r = tree.getPathBounds(new javax.swing.tree.TreePath(worldTreeModel.getPath(element)));
						if (r != null && !worldTreeModel.isLeaf(element)) { // HACK
																			// to
																			// include
																			// expand
																			// handle
							r.x -= 15;
							r.width += 15;
						}
						if (r != null) {
							r = javax.swing.SwingUtilities.convertRectangle(tree, r, jAliceFrame.getGlassPane());
						}
					}
				} else {
					r = jAliceFrame.worldTreeComponent.getBounds();
					r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.worldTreeComponent.getParent(), r,
							jAliceFrame.getGlassPane());
				}
			} else if (prefix.equals("clipboard")) {
				if (spec != null) {
					try {
						final int index = Integer.parseInt(spec);
						if (index > -1) {
							final java.awt.Component c = jAliceFrame.clipboardPanel.getComponent(index);
							if (c != null) {
								r = c.getBounds();
								r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
										jAliceFrame.getGlassPane());
							}
						}
					} catch (final Exception e) {
					}
				}

			} else if (prefix.equals("sceneEditor")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken(); // pull off singleView/quadView,
											// assume we're in the right mode
					if (st.hasMoreTokens()) {
						token = st.nextToken();
						prefix = AuthoringToolResources.getPrefix(token);
						spec = AuthoringToolResources.getSpecifier(token);
						if (prefix.equals("galleryViewer")) {
							jAliceFrame.sceneEditor.getGalleryViewer().setDirectory(spec);
							if (st.hasMoreTokens()) {
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("button")) {
									final java.awt.Component c = AuthoringToolResources
											.findButton(jAliceFrame.sceneEditor.getGalleryViewer(), spec);
									if (c != null) {
										r = c.getBounds();
										r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
												jAliceFrame.getGlassPane());
									}
								} else if (prefix.equals("galleryObject")) {
									final java.awt.Component c = AuthoringToolResources
											.findGalleryObject(jAliceFrame.sceneEditor.getGalleryViewer(), spec);
									if (c != null) {
										r = c.getBounds();
										r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
												jAliceFrame.getGlassPane());
									}
								}
							} else {
								r = jAliceFrame.sceneEditor.getGalleryViewer().getBounds();
								r = javax.swing.SwingUtilities.convertRectangle(
										jAliceFrame.sceneEditor.getGalleryViewer().getParent(), r,
										jAliceFrame.getGlassPane());
							}
						} else {
							final java.awt.Component c = jAliceFrame.sceneEditor.getComponentForId(token);
							if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
								r = c.getBounds();
								r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
										jAliceFrame.getGlassPane());
							}
						}
					} else {
						r = jAliceFrame.sceneEditor.getBounds();
						r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.sceneEditor.getParent(), r,
								jAliceFrame.getGlassPane());
					}
				} else {
					r = jAliceFrame.sceneEditor.getBounds();
					r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.sceneEditor.getParent(), r,
							jAliceFrame.getGlassPane());
				}
			} else if (prefix.equals("details")) {
				if (spec != null) {
					final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
					if (jAliceFrame.dragFromComponent.getElement().equals(element)) {
						if (st.hasMoreTokens()) {
							token = st.nextToken();
							prefix = AuthoringToolResources.getPrefix(token);
							spec = AuthoringToolResources.getSpecifier(token);
							if (prefix.equals("tab")) {
								final int tabIndex = Integer.parseInt(spec);
								r = jAliceFrame.dragFromComponent.tabbedPane.getUI()
										.getTabBounds(jAliceFrame.dragFromComponent.tabbedPane, tabIndex);
								r = javax.swing.SwingUtilities.convertRectangle(
										jAliceFrame.dragFromComponent.tabbedPane, r, jAliceFrame.getGlassPane());
								return r;
							} else if (prefix.equals("viewController")) {
								if (st.hasMoreTokens()) {
									token = st.nextToken();
									prefix = AuthoringToolResources.getPrefix(token);
									spec = AuthoringToolResources.getSpecifier(token);
									final java.awt.Component c = jAliceFrame.dragFromComponent
											.getPropertyViewComponentForKey(token);
									if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
										r = c.getBounds();
										r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
												jAliceFrame.getGlassPane());
									}
								}
							} else {
								final java.awt.Component c = jAliceFrame.dragFromComponent.getComponentForKey(token);
								if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
									r = c.getBounds();
									r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
											jAliceFrame.getGlassPane());
								}
							}
						} else {
							r = jAliceFrame.dragFromComponent.getBounds();
							r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.dragFromComponent.getParent(),
									r, jAliceFrame.getGlassPane());
						}
					}
				} else {
					r = jAliceFrame.dragFromComponent.getBounds();
					r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.dragFromComponent.getParent(), r,
							jAliceFrame.getGlassPane());
				}
			} else if (prefix.equals("behaviors")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken();
					prefix = AuthoringToolResources.getPrefix(token);
					spec = AuthoringToolResources.getSpecifier(token);
					if (prefix.equals("createNewEventButton")) {
						final java.awt.Component c = AuthoringToolResources.findButton(jAliceFrame.behaviorGroupsEditor,
								"create new event");
						if (c != null) {
							r = c.getBounds();
							r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
									jAliceFrame.getGlassPane());
						}
					} else if (prefix.equals("elementTile") && spec != null) {
						final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
						if (element != null) {
							if (st.hasMoreTokens()) {
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("property") && spec != null) {
									final java.awt.Component c = AuthoringToolResources.findPropertyViewController(
											jAliceFrame.behaviorGroupsEditor, element, spec);
									if (c != null) {
										r = c.getBounds();
										r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
												jAliceFrame.getGlassPane());
									}
								} else if (prefix.equals("more")) {
									final java.awt.Component dndPanel = AuthoringToolResources
											.findElementDnDPanel(jAliceFrame.behaviorGroupsEditor, element);
									if (dndPanel instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
										final java.awt.Component moreTile = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) dndPanel)
												.getMoreTile();
										if (moreTile != null && moreTile.isShowing()) {
											r = moreTile.getBounds();
											r = javax.swing.SwingUtilities.convertRectangle(moreTile.getParent(), r,
													jAliceFrame.getGlassPane());
										}
									}
								}
							} else {
								final java.awt.Component c = AuthoringToolResources
										.findElementDnDPanel(jAliceFrame.behaviorGroupsEditor, element);
								if (c != null) {
									r = c.getBounds();
									r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
											jAliceFrame.getGlassPane());
								}
							}
						}
					}
				} else {
					r = jAliceFrame.behaviorGroupsEditor.getBounds();
					r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.behaviorGroupsEditor.getParent(), r,
							jAliceFrame.getGlassPane());
				}
			} else if (prefix.equals("editors")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken();
					prefix = AuthoringToolResources.getPrefix(token);
					spec = AuthoringToolResources.getSpecifier(token);
					if (prefix.equals("element")) {
						final edu.cmu.cs.stage3.alice.core.Element elementBeingEdited = world.getDescendantKeyed(spec);
						if (st.hasMoreTokens()) {
							if (jAliceFrame.tabbedEditorComponent.getObjectBeingEdited() != null
									&& jAliceFrame.tabbedEditorComponent.getObjectBeingEdited()
											.equals(elementBeingEdited)) {
								// Editor editor =
								// jAliceFrame.tabbedEditorComponent.getEditorAt(
								// jAliceFrame.tabbedEditorComponent.getIndexOfObject(
								// elementBeingEdited ) );
								final java.awt.Container container = (java.awt.Container) jAliceFrame.tabbedEditorComponent.tabbedPane
										.getComponentAt(
												jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementBeingEdited));
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("button")) {
									final java.awt.Component c = AuthoringToolResources.findButton(container, spec);
									if (c != null) {
										r = c.getBounds();
										r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
												jAliceFrame.getGlassPane());
									}
								} else if (prefix.equals("elementTile") && spec != null) {
									final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
									if (element != null) {
										if (st.hasMoreTokens()) {
											token = st.nextToken();
											prefix = AuthoringToolResources.getPrefix(token);
											spec = AuthoringToolResources.getSpecifier(token);
											if (prefix.equals("property") && spec != null) {
												final java.awt.Component c = AuthoringToolResources
														.findPropertyViewController(container, element, spec);
												if (c != null) {
													r = c.getBounds();
													r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
															jAliceFrame.getGlassPane());
												}
											} else if (prefix.equals("more")) {
												final java.awt.Component dndPanel = AuthoringToolResources
														.findElementDnDPanel(container, element);
												if (dndPanel instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
													final java.awt.Component moreTile = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) dndPanel)
															.getMoreTile();
													if (moreTile != null && moreTile.isShowing()) {
														r = moreTile.getBounds();
														r = javax.swing.SwingUtilities.convertRectangle(
																moreTile.getParent(), r, jAliceFrame.getGlassPane());
													}
												}
											}
										} else {
											final java.awt.Component c = AuthoringToolResources
													.findElementDnDPanel(container, element);
											if (c != null) {
												r = c.getBounds();
												r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
														jAliceFrame.getGlassPane());
											}
										}
									}
								} else if (prefix.equals("elementPrototypeTile") && spec != null) {
									try {
										final Class<?> elementClass = Class.forName(spec);
										if (elementClass != null) {
											final java.awt.Component c = AuthoringToolResources
													.findPrototypeDnDPanel(container, elementClass);
											if (c != null) {
												r = c.getBounds();
												r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r,
														jAliceFrame.getGlassPane());
											}
										}
									} catch (final Exception e) {
										AuthoringTool.showErrorDialog(
												"Error while looking for ProtoypeDnDPanel using class " + spec, e);
									}
								}
							}
						} else {
							final int tabIndex = jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementBeingEdited);
							if (tabIndex >= 0
									&& tabIndex < jAliceFrame.tabbedEditorComponent.tabbedPane.getComponentCount()) {
								r = jAliceFrame.tabbedEditorComponent.tabbedPane.getUI()
										.getTabBounds(jAliceFrame.tabbedEditorComponent.tabbedPane, tabIndex);
								r = javax.swing.SwingUtilities.convertRectangle(
										jAliceFrame.tabbedEditorComponent.tabbedPane, r, jAliceFrame.getGlassPane());
							}
						}
					}
				} else {
					r = jAliceFrame.tabbedEditorComponent.getBounds();
					r = javax.swing.SwingUtilities.convertRectangle(jAliceFrame.tabbedEditorComponent.getParent(), r,
							jAliceFrame.getGlassPane());
				}
			} else if (componentMap.containsKey(prefix)) {
				final java.awt.Component c = componentMap.get(prefix);
				if (c != null) {
					r = c.getBounds();
					r = javax.swing.SwingUtilities.convertRectangle(c.getParent(), r, jAliceFrame.getGlassPane());
				}
			}
		}

		if (r == null) {
			throw new edu.cmu.cs.stage3.caitlin.stencilhelp.application.IDDoesNotExistException(id);
		}

		return r;
	}

	public boolean isComponentVisible(final javax.swing.JComponent c) {
		if (c == null) {
			return false;
		}
		final java.awt.Rectangle visibleR = c.getVisibleRect();
		final java.awt.Rectangle ourRect = c.getBounds();
		return ourRect.width == visibleR.width && ourRect.height == visibleR.height;
		// java.awt.Component parent = c.getParent();
		// while (parent != null && !(parent instanceof
		// javax.swing.JScrollPane)){
		// parent = parent.getParent();
		// }
		// if (parent == null){
		// return c.isVisible();
		// } else{
		// javax.swing.JScrollPane parentScrollPane =
		// (javax.swing.JScrollPane)parent;
		// return false;
		// }
	}

	@Override
	public boolean isIDVisible(final String id)
			throws edu.cmu.cs.stage3.caitlin.stencilhelp.application.IDDoesNotExistException {
		final java.util.StringTokenizer st = new java.util.StringTokenizer(id, ":", false);

		if (st.hasMoreTokens()) {
			String token = st.nextToken();
			String prefix = AuthoringToolResources.getPrefix(token);
			String spec = AuthoringToolResources.getSpecifier(token);
			if (prefix.equals("objectTree")) {
				if (spec != null) {
					final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
					if (element != null) {
						final javax.swing.JTree tree = jAliceFrame.worldTreeComponent.worldTree;
						final edu.cmu.cs.stage3.alice.authoringtool.util.WorldTreeModel worldTreeModel = (edu.cmu.cs.stage3.alice.authoringtool.util.WorldTreeModel) tree
								.getModel();
						final java.awt.Rectangle r = tree
								.getPathBounds(new javax.swing.tree.TreePath(worldTreeModel.getPath(element)));
						if (r != null && tree.getVisibleRect().contains(r)) {
							return true;
						} else {
							return false;
						}
					}
				}
			} else if (prefix.equals("clipboard")) {
				if (spec != null) {
					try {
						final int index = Integer.parseInt(spec);
						if (index > -1) {
							final java.awt.Component c = jAliceFrame.clipboardPanel.getComponent(index);
							if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
								return true;
							} else {
								return false;
							}
						} else {
							return false;
						}
					} catch (final Exception e) {
						return false;
					}
				} else {
					return false;
				}

			} else if (prefix.equals("sceneEditor")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken(); // pull off singleView/quadView
					if (st.hasMoreTokens()) {
						token = st.nextToken();
						prefix = AuthoringToolResources.getPrefix(token);
						spec = AuthoringToolResources.getSpecifier(token);
						if (prefix.equals("galleryViewer")) {
							if (jAliceFrame.sceneEditor.getGalleryViewer().getDirectory().equals(spec)) {
								if (st.hasMoreTokens()) {
									token = st.nextToken();
									prefix = AuthoringToolResources.getPrefix(token);
									spec = AuthoringToolResources.getSpecifier(token);
									if (prefix.equals("button")) {
										final java.awt.Component c = AuthoringToolResources
												.findButton(jAliceFrame.sceneEditor.getGalleryViewer(), spec);
										if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
											return true;
										} else {
											return false;
										}
									} else if (prefix.equals("galleryObject")) {
										final java.awt.Component c = AuthoringToolResources
												.findGalleryObject(jAliceFrame.sceneEditor.getGalleryViewer(), spec);
										if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
											return true;
										} else {
											return false;
										}
									}
								} else {
									if (jAliceFrame.sceneEditor.getGalleryViewer().isShowing()) {
										return true;
									} else {
										return false;
									}
								}
							} else {
								return false;
							}
						} else {
							final java.awt.Component c = jAliceFrame.sceneEditor.getComponentForId(token);
							if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
								return true;
							} else {
								return false;
							}
						}
					}
				}
			} else if (prefix.equals("details")) {
				if (spec == null) {
					spec = "";
				}
				final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
				if (jAliceFrame.dragFromComponent.getElement().equals(element)) {
					if (st.hasMoreTokens()) {
						token = st.nextToken();
						prefix = AuthoringToolResources.getPrefix(token);
						spec = AuthoringToolResources.getSpecifier(token);
						if (prefix.equals("tab")) {
							return true;
						} else {
							final java.awt.Component c = jAliceFrame.dragFromComponent.getComponentForKey(token);
							// System.out.println(c+",
							// "+isComponentVisible((javax.swing.JComponent)c)+",
							// "+c.isVisible());
							if (c != null) {
								java.awt.Rectangle boundss = c.getBounds();
								boundss = javax.swing.SwingUtilities.convertRectangle(c.getParent(), boundss,
										jAliceFrame);
							}
							// System.out.println(boundss);
							// System.out.println(jAliceFrame.getBounds());
							if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
								final java.awt.Rectangle bounds = c.getBounds();
								javax.swing.SwingUtilities.convertRectangle(c.getParent(), bounds,
										jAliceFrame.dragFromComponent);
								if (jAliceFrame.dragFromComponent.getVisibleRect().contains(bounds)) {
									// System.out.println("returning true");
									return true;
								} else {
									return false;
								}
							} else {
								return false;
							}
						}
					} else {
						return false;
					}
				} else {
					// TODO is this safe?
					return false;
				}
			} else if (prefix.equals("behaviors")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken();
					prefix = AuthoringToolResources.getPrefix(token);
					spec = AuthoringToolResources.getSpecifier(token);
					if (prefix.equals("createNewBehaviorButton")) {
						final java.awt.Component c = AuthoringToolResources.findButton(jAliceFrame.behaviorGroupsEditor,
								"create new behavior");
						if (c != null && c.isShowing()) {
							return true;
						} else {
							return false;
						}
					} else if (prefix.equals("elementTile") && spec != null) {
						final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
						if (element != null) {
							if (st.hasMoreTokens()) {
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("property") && spec != null) {
									final java.awt.Component c = AuthoringToolResources.findPropertyViewController(
											jAliceFrame.behaviorGroupsEditor, element, spec);
									if (c != null && c.isShowing()) {
										java.awt.Rectangle bounds = c.getBounds();
										bounds = javax.swing.SwingUtilities.convertRectangle(c.getParent(), bounds,
												jAliceFrame.behaviorGroupsEditor.getScrollPane());
										if (jAliceFrame.behaviorGroupsEditor.getScrollPaneVisibleRect()
												.contains(bounds)) {
											return true;
										} else {
											return false;
										}
										// return true;
										// } else {
										// return false;
									} else {
										return false;
									}
								} else if (prefix.equals("more")) {
									final java.awt.Component dndPanel = AuthoringToolResources
											.findElementDnDPanel(jAliceFrame.behaviorGroupsEditor, element);
									if (dndPanel instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
										final java.awt.Component moreTile = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) dndPanel)
												.getMoreTile();
										if (moreTile != null && moreTile.isShowing()) {
											final java.awt.Rectangle bounds = moreTile.getBounds();
											javax.swing.SwingUtilities.convertRectangle(moreTile.getParent(), bounds,
													jAliceFrame.behaviorGroupsEditor.getScrollPane());
											if (jAliceFrame.behaviorGroupsEditor.getScrollPaneVisibleRect()
													.contains(bounds)) {
												return true;
											} else {
												return false;
											}
											// return true;
											// } else {
											// return false;
										} else {
											return false;
										}
									}
								}
							} else {
								final java.awt.Component c = AuthoringToolResources
										.findElementDnDPanel(jAliceFrame.behaviorGroupsEditor, element);
								boolean visibleRectNotEmpty = true;
								if (c instanceof javax.swing.JComponent) {
									visibleRectNotEmpty = !((javax.swing.JComponent) c).getVisibleRect().isEmpty();
								}
								if (c != null && c.isShowing() && visibleRectNotEmpty) {
									final java.awt.Rectangle bounds = c.getBounds();
									javax.swing.SwingUtilities.convertRectangle(c.getParent(), bounds,
											jAliceFrame.behaviorGroupsEditor.getScrollPane());
									if (jAliceFrame.behaviorGroupsEditor.getScrollPaneVisibleRect().contains(bounds)) {
										return true;
									} else {
										return false;
									}
									// return true;
								} else {
									return false;
								}
							}
						}
					}
				}
			} else if (prefix.equals("editors")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken();
					prefix = AuthoringToolResources.getPrefix(token);
					spec = AuthoringToolResources.getSpecifier(token);
					if (prefix.equals("element")) {
						final edu.cmu.cs.stage3.alice.core.Element elementBeingEdited = world.getDescendantKeyed(spec);
						if (st.hasMoreTokens()) {
							if (jAliceFrame.tabbedEditorComponent.getObjectBeingEdited() != null
									&& jAliceFrame.tabbedEditorComponent.getObjectBeingEdited()
											.equals(elementBeingEdited)) {
								// Editor editor =
								// jAliceFrame.tabbedEditorComponent.getEditorAt(
								// jAliceFrame.tabbedEditorComponent.getIndexOfObject(
								// elementBeingEdited ) );
								final java.awt.Container container = (java.awt.Container) jAliceFrame.tabbedEditorComponent.tabbedPane
										.getComponentAt(
												jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementBeingEdited));
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("button")) {
									final java.awt.Component c = AuthoringToolResources.findButton(container, spec);
									if (c != null && c.isShowing()) {
										return true;
									} else {
										return false;
									}
								} else if (prefix.equals("elementTile") && spec != null) {
									final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
									if (element != null) {
										if (st.hasMoreTokens()) {
											token = st.nextToken();
											prefix = AuthoringToolResources.getPrefix(token);
											spec = AuthoringToolResources.getSpecifier(token);
											if (prefix.equals("property") && spec != null) {
												final java.awt.Component c = AuthoringToolResources
														.findPropertyViewController(container, element, spec);
												if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
													return true;
												} else {
													return false;
												}
											} else if (prefix.equals("more")) {
												final java.awt.Component dndPanel = AuthoringToolResources
														.findElementDnDPanel(container, element);
												if (dndPanel instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
													final java.awt.Component moreTile = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) dndPanel)
															.getMoreTile();
													if (moreTile != null && moreTile.isShowing()) {
														return true;
													} else {
														return false;
													}
												}
											}
										} else {
											final java.awt.Component c = AuthoringToolResources
													.findElementDnDPanel(container, element);
											if (c != null && isComponentVisible((javax.swing.JComponent) c)) {
												return true;
											} else {
												return false;
											}
										}
									}
								} else if (prefix.equals("elementPrototypeTile") && spec != null) {
									try {
										final Class<?> elementClass = Class.forName(spec);
										if (elementClass != null) {
											final java.awt.Component c = AuthoringToolResources
													.findPrototypeDnDPanel(container, elementClass);
											if (c != null && c.isShowing()) {
												return true;
											} else {
												return false;
											}
										}
									} catch (final Exception e) {
										AuthoringTool.showErrorDialog(
												"Error while looking for ProtoypeDnDPanel using class " + spec, e);
									}
								}
							} else {
								return false;
							}
						} else {
							final int tabIndex = jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementBeingEdited);
							if (tabIndex >= 0
									&& tabIndex < jAliceFrame.tabbedEditorComponent.tabbedPane.getComponentCount()) {
								return true;
							} else {
								return false;
							}
						}
					}
				}
			} else if (componentMap.containsKey(prefix)) {
				final java.awt.Component c = componentMap.get(prefix);
				if (c != null && c.isShowing()) {
					return true;
				} else {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void makeIDVisible(final String id)
			throws edu.cmu.cs.stage3.caitlin.stencilhelp.application.IDDoesNotExistException {
		final java.util.StringTokenizer st = new java.util.StringTokenizer(id, ":", false);

		if (st.hasMoreTokens()) {
			String token = st.nextToken();
			String prefix = AuthoringToolResources.getPrefix(token);
			String spec = AuthoringToolResources.getSpecifier(token);

			if (prefix.equals("objectTree")) {
				if (spec != null) {
					final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
					if (element != null) {
						final javax.swing.JTree tree = jAliceFrame.worldTreeComponent.worldTree;
						final edu.cmu.cs.stage3.alice.authoringtool.util.WorldTreeModel worldTreeModel = (edu.cmu.cs.stage3.alice.authoringtool.util.WorldTreeModel) tree
								.getModel();
						tree.scrollPathToVisible(new javax.swing.tree.TreePath(worldTreeModel.getPath(element)));
					}
				}
			} else if (prefix.equals("clipboard")) {
				// Do nothing

			} else if (prefix.equals("sceneEditor")) {
				if (spec.equals("large")) {
					jAliceFrame.sceneEditor.setGuiMode(
							edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.SceneEditor.LARGE_MODE);
					// System.out.println("just made scene editor large");
				} else if (spec.equals("small")) {
					jAliceFrame.sceneEditor.setGuiMode(
							edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.SceneEditor.SMALL_MODE);
					// System.out.println("just made scene editor small");
				}
				if (st.hasMoreTokens()) {
					token = st.nextToken(); // pull off singleView/quadView
					if (token.equals("singleView")) {
						jAliceFrame.sceneEditor.setViewMode(
								edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.CameraViewPanel.SINGLE_VIEW_MODE);
						// System.out.println("just set single view");
					} else if (token.equals("quadView")) {
						jAliceFrame.sceneEditor.setViewMode(
								edu.cmu.cs.stage3.alice.authoringtool.editors.sceneeditor.CameraViewPanel.QUAD_VIEW_MODE);
						// System.out.println("just made set quad view");
					}

					if (st.hasMoreTokens()) {
						token = st.nextToken();
						prefix = AuthoringToolResources.getPrefix(token);
						spec = AuthoringToolResources.getSpecifier(token);
						if (prefix.equals("galleryViewer")) {
							// jAliceFrame.sceneEditor.getGalleryViewer().setDirectory(
							// spec );
							// System.out.println("trying to show gallery
							// viewer");
							if (st.hasMoreTokens()) {
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("button")) {
									final java.awt.Component c = AuthoringToolResources
											.findButton(jAliceFrame.sceneEditor.getGalleryViewer(), spec);
									if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
										((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
									}
								} else if (prefix.equals("galleryObject")) {
									final java.awt.Component c = AuthoringToolResources
											.findGalleryObject(jAliceFrame.sceneEditor.getGalleryViewer(), spec);
									if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
										((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
									}
								}
							} else {
								// System.out.println("no more tokens");
							}
						} else {
							final java.awt.Component c = jAliceFrame.sceneEditor.getComponentForId(token);
							if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
								((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
							}
						}
					}
				}
			} else if (prefix.equals("details")) {
				if (spec == null) {
					spec = "";
				}
				final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
				if (!jAliceFrame.dragFromComponent.getElement().equals(element)) {
					jAliceFrame.dragFromComponent.setElement(element);
				}
				if (jAliceFrame.dragFromComponent.getElement().equals(element)) {
					if (st.hasMoreTokens()) {
						token = st.nextToken();
						prefix = AuthoringToolResources.getPrefix(token);
						spec = AuthoringToolResources.getSpecifier(token);

						if (!prefix.equals("tab")) {
							boolean isViewController = false;
							if (prefix.equals("viewController")) {
								if (st.hasMoreTokens()) {
									token = st.nextToken();
									prefix = AuthoringToolResources.getPrefix(token);
									spec = AuthoringToolResources.getSpecifier(token);
									isViewController = true;
								}
							}
							if (prefix.equals("property") || prefix.equals("variable") || prefix.equals("textureMap")
									|| prefix.equals("sound") || prefix.equals("other")) {
								jAliceFrame.dragFromComponent.selectTab(
										edu.cmu.cs.stage3.alice.authoringtool.DragFromComponent.PROPERTIES_TAB);
							}
							if (prefix.equals("userDefinedResponse") || prefix.equals("responsePrototype")) {
								jAliceFrame.dragFromComponent.selectTab(
										edu.cmu.cs.stage3.alice.authoringtool.DragFromComponent.ANIMATIONS_TAB);
							}
							if (prefix.equals("userDefinedQuestion") || prefix.equals("questionPrototype")) {
								jAliceFrame.dragFromComponent.selectTab(
										edu.cmu.cs.stage3.alice.authoringtool.DragFromComponent.QUESTIONS_TAB);
							}
							java.awt.Component c = null;
							if (isViewController) {
								c = jAliceFrame.dragFromComponent.getPropertyViewComponentForKey(token);
							} else {
								c = jAliceFrame.dragFromComponent.getComponentForKey(token);
							}
							if (c != null && c
									.getParent() instanceof edu.cmu.cs.stage3.alice.authoringtool.util.ExpandablePanel) {
								final edu.cmu.cs.stage3.alice.authoringtool.util.ExpandablePanel ep = (edu.cmu.cs.stage3.alice.authoringtool.util.ExpandablePanel) c
										.getParent();
								ep.setExpanded(true);
							}
							if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
								((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
							}
						}
					}
				}
			} else if (prefix.equals("behaviors")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken();
					prefix = AuthoringToolResources.getPrefix(token);
					spec = AuthoringToolResources.getSpecifier(token);
					if (prefix.equals("elementTile") && spec != null) {
						final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
						if (element != null) {
							if (st.hasMoreTokens()) {
								token = st.nextToken();
								prefix = AuthoringToolResources.getPrefix(token);
								spec = AuthoringToolResources.getSpecifier(token);
								if (prefix.equals("property") && spec != null) {
									final java.awt.Component c = AuthoringToolResources.findPropertyViewController(
											jAliceFrame.behaviorGroupsEditor, element, spec);
									if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
										((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
									}
								} else if (prefix.equals("more")) {
									final java.awt.Component dndPanel = AuthoringToolResources
											.findElementDnDPanel(jAliceFrame.behaviorGroupsEditor, element);
									if (dndPanel instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
										final java.awt.Component moreTile = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) dndPanel)
												.getMoreTile();
										if (moreTile != null && moreTile.isShowing()
												&& moreTile instanceof javax.swing.JComponent) {
											((javax.swing.JComponent) moreTile)
													.scrollRectToVisible(moreTile.getBounds());
										}
									}
								}
							} else {
								final java.awt.Component c = AuthoringToolResources
										.findElementDnDPanel(jAliceFrame.behaviorGroupsEditor, element);
								if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
									// ((javax.swing.JComponent)c).scrollRectToVisible(
									// c.getBounds() );
									((javax.swing.JComponent) c).scrollRectToVisible(
											new java.awt.Rectangle(0, 0, c.getWidth(), c.getHeight()));
								}
							}
						}
					}
				}
			} else if (prefix.equals("editors")) {
				if (st.hasMoreTokens()) {
					token = st.nextToken();
					prefix = AuthoringToolResources.getPrefix(token);
					spec = AuthoringToolResources.getSpecifier(token);
					if (prefix.equals("element")) {
						final edu.cmu.cs.stage3.alice.core.Element elementBeingEdited = world.getDescendantKeyed(spec);
						if (elementBeingEdited == null) {
							throw new edu.cmu.cs.stage3.caitlin.stencilhelp.application.IDDoesNotExistException(spec);
						}
						if (st.hasMoreTokens()) {
							if (jAliceFrame.tabbedEditorComponent.getObjectBeingEdited() != elementBeingEdited) {
								editObject(elementBeingEdited);
							}

							// Editor editor =
							// jAliceFrame.tabbedEditorComponent.getEditorAt(
							// jAliceFrame.tabbedEditorComponent.getIndexOfObject(
							// elementBeingEdited ) );
							final java.awt.Container container = (java.awt.Container) jAliceFrame.tabbedEditorComponent.tabbedPane
									.getComponentAt(
											jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementBeingEdited));
							token = st.nextToken();
							prefix = AuthoringToolResources.getPrefix(token);
							spec = AuthoringToolResources.getSpecifier(token);
							if (prefix.equals("button")) {
								final java.awt.Component c = AuthoringToolResources.findButton(container, spec);
								if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
									((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
								}
							} else if (prefix.equals("elementTile") && spec != null) {
								final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(spec);
								if (element != null) {
									if (st.hasMoreTokens()) {
										token = st.nextToken();
										prefix = AuthoringToolResources.getPrefix(token);
										spec = AuthoringToolResources.getSpecifier(token);
										if (prefix.equals("property") && spec != null) {
											final java.awt.Component c = AuthoringToolResources
													.findPropertyViewController(container, element, spec);
											if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
												((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
											}
										} else if (prefix.equals("more")) {
											final java.awt.Component dndPanel = AuthoringToolResources
													.findElementDnDPanel(container, element);
											if (dndPanel instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) {
												final java.awt.Component moreTile = ((edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.FormattedElementViewController) dndPanel)
														.getMoreTile();
												if (moreTile != null && moreTile.isShowing()
														&& moreTile instanceof javax.swing.JComponent) {
													((javax.swing.JComponent) moreTile)
															.scrollRectToVisible(moreTile.getBounds());
												}
											}
										}
									} else {
										final java.awt.Component c = AuthoringToolResources
												.findElementDnDPanel(container, element);
										if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
											((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
										}
									}
								}
							} else if (prefix.equals("elementPrototypeTile") && spec != null) {
								try {
									final Class<?> elementClass = Class.forName(spec);
									if (elementClass != null) {
										final java.awt.Component c = AuthoringToolResources
												.findPrototypeDnDPanel(container, elementClass);
										if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
											((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
										}
									}
								} catch (final Exception e) {
									AuthoringTool.showErrorDialog(
											"Error while looking for ProtoypeDnDPanel using class " + spec, e);
								}
							}
						} else {
							final int tabIndex = jAliceFrame.tabbedEditorComponent.getIndexOfObject(elementBeingEdited);
							if (tabIndex < 0
									|| tabIndex >= jAliceFrame.tabbedEditorComponent.tabbedPane.getComponentCount()) {
								editObject(elementBeingEdited);
							}
						}
					}
				}
			} else if (componentMap.containsKey(prefix)) {
				final java.awt.Component c = componentMap.get(prefix);
				if (c != null && c.isShowing() && c instanceof javax.swing.JComponent) {
					((javax.swing.JComponent) c).scrollRectToVisible(c.getBounds());
				}
			}
		}
	}

	@Override
	synchronized public void makeWayPoint() {
		if (wayPoints.size() > 0) {
			final edu.cmu.cs.stage3.alice.authoringtool.util.WorldDifferencesCapsule currentWayPoint = wayPoints.get(0);
			currentWayPoint.stopListening();
		}

		final edu.cmu.cs.stage3.alice.authoringtool.util.WorldDifferencesCapsule wayPoint = new edu.cmu.cs.stage3.alice.authoringtool.util.WorldDifferencesCapsule(
				this, world);
		wayPoints.add(0, wayPoint);
	}

	@Override
	synchronized public void goToPreviousWayPoint() {
		if (wayPoints.size() > 0) {
			final edu.cmu.cs.stage3.alice.authoringtool.util.WorldDifferencesCapsule currentWayPoint = wayPoints.get(0);
			currentWayPoint.restoreWorld();
			currentWayPoint.dispose();
			wayPoints.remove(0);
		}

		if (wayPoints.size() > 0) {
			final edu.cmu.cs.stage3.alice.authoringtool.util.WorldDifferencesCapsule previousWayPoint = wayPoints
					.get(0);
			previousWayPoint.restoreWorld();
			previousWayPoint.startListening();
		}
	}

	@Override
	synchronized public void clearWayPoints() {
		for (final WorldDifferencesCapsule wayPoint : wayPoints) {
			wayPoint.dispose();
		}
		wayPoints.clear();
	}

	@Override
	public boolean doesStateMatch(final edu.cmu.cs.stage3.caitlin.stencilhelp.application.StateCapsule capsule) {
		if (capsule instanceof edu.cmu.cs.stage3.alice.authoringtool.util.StencilStateCapsule) {
			final edu.cmu.cs.stage3.alice.authoringtool.util.StencilStateCapsule stencilStateCapsule = (edu.cmu.cs.stage3.alice.authoringtool.util.StencilStateCapsule) capsule;

			final String[] existantElements = stencilStateCapsule.getExistantElements();
			final String[] nonExistantElements = stencilStateCapsule.getNonExistantElements();
			final Set<String> propertyValueKeys = stencilStateCapsule.getPropertyValueKeySet();
			final Set<String> elementPositions = stencilStateCapsule.getElementPositionKeySet();

			// check for all the elements that need to exist
			for (final String existantElement : existantElements) {
				if (world.getDescendantKeyed(existantElement) == null) {
					return false;
				}
			}

			// System.out.println("elements exist ok");

			// make sure all the elements that shouldn't exist don't
			for (final String nonExistantElement : nonExistantElements) {
				if (world.getDescendantKeyed(nonExistantElement) != null) {
					return false;
				}
			}

			// System.out.println("elements don't exist ok");

			// check that elements are in the right positions
			for (final String elementKey : elementPositions) {
				final int position = stencilStateCapsule.getElementPosition(elementKey);

				final edu.cmu.cs.stage3.alice.core.Element element = world.getDescendantKeyed(elementKey);
				if (element != null) {
					int actualPosition = element.getParent().getIndexOfChild(element);
					if (element instanceof edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse) {
						final edu.cmu.cs.stage3.alice.core.Property resp = element.getParent()
								.getPropertyNamed("responses");
						if (resp instanceof edu.cmu.cs.stage3.alice.core.property.ObjectArrayProperty) {
							actualPosition = ((edu.cmu.cs.stage3.alice.core.property.ObjectArrayProperty) resp)
									.indexOf(element);
							// System.out.println("index in responses: " +
							// actualPosition);
						}
					} else if (element instanceof edu.cmu.cs.stage3.alice.core.Response) {
						final edu.cmu.cs.stage3.alice.core.Property resp = element.getParent()
								.getPropertyNamed("componentResponses");
						if (resp instanceof edu.cmu.cs.stage3.alice.core.property.ObjectArrayProperty) {
							actualPosition = ((edu.cmu.cs.stage3.alice.core.property.ObjectArrayProperty) resp)
									.indexOf(element);
							// System.out.println("index in componentResponses:
							// "
							// + actualPosition);
						}
					} else if (element instanceof edu.cmu.cs.stage3.alice.core.Behavior) {
						final edu.cmu.cs.stage3.alice.core.Property resp = element.getParent()
								.getPropertyNamed("behaviors");
						if (resp instanceof edu.cmu.cs.stage3.alice.core.property.ObjectArrayProperty) {
							actualPosition = ((edu.cmu.cs.stage3.alice.core.property.ObjectArrayProperty) resp)
									.indexOf(element);
							// System.out.println("index in responses: " +
							// actualPosition);
						}
					}

					// element isn't in the right place wrt to its parent
					if (position != actualPosition) {
						// System.out.println("actual position: " +
						// actualPosition + " correct position: " + position);
						return false;
					}
				} else {
					// element doesn't exist and it should
					return false;
				}
			}

			// System.out.println("elements in correct positions ok");

			for (final String propertyKey : propertyValueKeys) {
				final String valueRepr = stencilStateCapsule.getPropertyValue(propertyKey);
				final int dotIndex = propertyKey.lastIndexOf(".");
				final String elementKey = propertyKey.substring(0, dotIndex);
				final String propertyName = propertyKey.substring(dotIndex + 1);

				final edu.cmu.cs.stage3.alice.core.Element propertyOwner = world.getDescendantKeyed(elementKey);
				if (propertyOwner != null) {

					// getting "properties" of a call to a user defined response
					if (propertyOwner instanceof edu.cmu.cs.stage3.alice.core.response.CallToUserDefinedResponse) {
						final edu.cmu.cs.stage3.alice.core.Property requiredParams = propertyOwner
								.getPropertyNamed("requiredActualParameters");

						final Object udobj = requiredParams.getValue();
						if (udobj instanceof edu.cmu.cs.stage3.alice.core.Variable[]) {
							final edu.cmu.cs.stage3.alice.core.Variable vars[] = (edu.cmu.cs.stage3.alice.core.Variable[]) udobj;
							if (vars != null) {
								for (final Variable var : vars) {
									if (var.getKey(world).equals(propertyKey)) {
										final String actualValueRepr = AuthoringToolResources
												.getReprForValue(var.getValue(), true);
										if (!actualValueRepr.equals(valueRepr)) {
											return false;
										}
									}
								}
							}
						}
					} else {
						final Object value = propertyOwner.getPropertyNamed(propertyName).get();
						final String actualValueRepr = AuthoringToolResources.getReprForValue(value, true);
						if (actualValueRepr != null) {
							if (!actualValueRepr.equals(valueRepr)) {
								return false;
							}
						} else {
							return false;
						}
					}
				} else {
					return false;
				}
			}

			// System.out.println("property value checks ok");

			// if we've arrived here, it means that all the new conditions have
			// been met. check to make sure user hasn't done
			// anything else that's strange.

			final edu.cmu.cs.stage3.alice.authoringtool.util.WorldDifferencesCapsule currentWayPoint = wayPoints.get(0);

			if (currentWayPoint.otherPropertyChangesMade(propertyValueKeys)
					|| currentWayPoint.otherElementsInsertedOrDeleted(existantElements, nonExistantElements)
					|| currentWayPoint.otherElementsShifted(elementPositions)) {
				return false;
			}

			return true;

		}
		return true;
	}

	@Override
	public void handleMouseEvent(final java.awt.event.MouseEvent ev) {
		java.awt.Point p = ev.getPoint();
		p = javax.swing.SwingUtilities.convertPoint((java.awt.Component) ev.getSource(), p,
				jAliceFrame.getLayeredPane());
		java.awt.Component newSource = jAliceFrame.getLayeredPane().findComponentAt(p);

		if (newSource instanceof javax.swing.JLabel
				|| newSource instanceof edu.cmu.cs.stage3.alice.authoringtool.viewcontroller.ElementPrototypeDnDPanel.Tile) {
			newSource = newSource.getParent(); // is this the right way to
												// handle this?
		}

		switch (ev.getID()) {
		case java.awt.event.MouseEvent.MOUSE_DRAGGED:
			if (stencilDragging) {
				newSource = dragStartSource;
			}
			break;
		case java.awt.event.MouseEvent.MOUSE_PRESSED:
			stencilDragging = true;
			dragStartSource = newSource;
			break;
		case java.awt.event.MouseEvent.MOUSE_RELEASED:
			if (stencilDragging) {
				newSource = dragStartSource;
				dragStartSource = null;
			}
			stencilDragging = false;
			break;
		case java.awt.event.MouseEvent.MOUSE_CLICKED:
		case java.awt.event.MouseEvent.MOUSE_ENTERED:
		case java.awt.event.MouseEvent.MOUSE_EXITED:
		case java.awt.event.MouseEvent.MOUSE_MOVED:
		default:
			break;
		}
		if (newSource != null) {
			p = javax.swing.SwingUtilities.convertPoint(jAliceFrame.getLayeredPane(), p, newSource);
			final java.awt.event.MouseEvent newEv = new java.awt.event.MouseEvent(newSource, ev.getID(), ev.getWhen(),
					ev.getModifiers(), p.x, p.y, ev.getClickCount(), ev.isPopupTrigger());
			ev.consume();
			newSource.dispatchEvent(newEv);
			// }
		}
	}

	@Override
	public void deFocus() {
		jAliceFrame.getContentPane().requestFocus();
	}

	@Override
	public void performTask(final String task) {
		final String prefix = AuthoringToolResources.getPrefix(task);
		final String spec = AuthoringToolResources.getSpecifier(task);

		if (prefix.equals("loadWorld")) {
			// boolean askForSave = !((currentWorldLocation != null) &&
			// currentWorldLocation.getAbsolutePath().startsWith(tutorialDirectory.getAbsolutePath()));
			final boolean askForSave = false;
			loadWorld(spec, askForSave);
		}
	}

	@Override
	public java.awt.Dimension getScreenSize() {
		return jAliceFrame.getSize();
	}

	public void launchTutorial() {
		launchTutorialFile(null);
	}

	public void launchTutorialFile(java.io.File tutorialFile) {
		if (Integer.parseInt(authoringToolConfig.getValue("fontSize")) > 12) {
			edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
					"Alice is not able to load the tutorial.\n"
							+ "Your font size is too large. You can go to Edit->Preferences to change the font size.",
					"Font size too large", javax.swing.JOptionPane.WARNING_MESSAGE);
		} else {
			if (tutorialFile == null) {
				tutorialFile = tutorialOne;
			}
			tutorialFile = tutorialFile.getAbsoluteFile(); // BIG HACK
			showStencils();
			stencilManager.loadStencilTutorial(tutorialFile);
		}
	}

	public java.io.File getTutorialDirectory() {
		return tutorialDirectory;
	}

	public java.io.File getExampleWorldsDirectory() {
		return new java.io.File(authoringToolConfig.getValue("directories.examplesDirectory")).getAbsoluteFile();
	}

	public java.io.File getTemplateWorldsDirectory() {
		return new java.io.File(authoringToolConfig.getValue("directories.templatesDirectory")).getAbsoluteFile();
	}

	public javax.swing.filechooser.FileFilter getWorldFileFilter() {
		return worldFileFilter;
	}

	public javax.swing.filechooser.FileFilter getCharacterFileFilter() {
		return characterFileFilter;
	}

	// Dialog Handling

	public edu.cmu.cs.stage3.alice.core.Variable showNewVariableDialog(final String title,
			final edu.cmu.cs.stage3.alice.core.Element context) {
		newVariableContentPane.reset(context);
		newVariableContentPane.setTitle(title);
		newVariableContentPane.setListsOnly(false);
		newVariableContentPane.setShowValue(true);
		return showNewVariableDialog(newVariableContentPane, context);
	}

	public edu.cmu.cs.stage3.alice.core.Variable showNewVariableDialog(final String title,
			final edu.cmu.cs.stage3.alice.core.Element context, final boolean listsOnly, final boolean showValue) {
		newVariableContentPane.reset(context);
		newVariableContentPane.setListsOnly(listsOnly);
		newVariableContentPane.setShowValue(showValue);
		newVariableContentPane.setTitle(title);
		return showNewVariableDialog(newVariableContentPane, context);
	}

	protected edu.cmu.cs.stage3.alice.core.Variable showNewVariableDialog(
			final edu.cmu.cs.stage3.alice.authoringtool.dialog.NewVariableContentPane newVariablePaneToShow,
			final edu.cmu.cs.stage3.alice.core.Element context) {
		final int result = edu.cmu.cs.stage3.swing.DialogManager.showDialog(newVariablePaneToShow);
		switch (result) {
		case edu.cmu.cs.stage3.swing.ContentPane.OK_OPTION:
			return newVariablePaneToShow.getVariable();
		case edu.cmu.cs.stage3.swing.ContentPane.CANCEL_OPTION:
			return null;
		default:
			return null;
		}
	}

	public edu.cmu.cs.stage3.alice.core.Sound promptUserForRecordedSound(
			final edu.cmu.cs.stage3.alice.core.Sandbox parent) {
		final edu.cmu.cs.stage3.alice.authoringtool.dialog.SoundRecorder soundRecorder = new edu.cmu.cs.stage3.alice.authoringtool.dialog.SoundRecorder();
		soundRecorder.setParentToCheckForNameValidity(parent);

		final int result = edu.cmu.cs.stage3.swing.DialogManager.showDialog(soundRecorder);
		edu.cmu.cs.stage3.alice.core.Sound sound = null;

		switch (result) {
		case edu.cmu.cs.stage3.swing.ContentPane.OK_OPTION:
			sound = soundRecorder.getSound();
			if (sound != null) {
				getUndoRedoStack().startCompound();
				try {
					parent.addChild(sound);
					parent.sounds.add(sound);
				} finally {
					getUndoRedoStack().stopCompound();
				}
			}
			break;
		}
		return sound;
	}

	public java.io.File getCurrentWorldLocation() {
		return currentWorldLocation;
	}

	public String getCurrentRendererText() {
		return renderTargetFactory.toString();
	}

	private boolean worldRun() {
		undoRedoStack.setIsListening(false);

		fireStateChanging(AuthoringToolStateChangedEvent.AUTHORING_STATE, AuthoringToolStateChangedEvent.RUNTIME_STATE);
		fireWorldStarting(AuthoringToolStateChangedEvent.AUTHORING_STATE, AuthoringToolStateChangedEvent.RUNTIME_STATE,
				world);

		// if( authoringToolConfig.getValue( "reloadWorldScriptOnRun"
		// ).equalsIgnoreCase( "true" ) ) {
		// try {
		// if( world.script.isAssociatedWithFile() ) {
		// world.script.loadFromAssociatedFile();
		// }
		// } catch( java.io.IOException e ) {
		// AuthoringTool.showErrorDialog(
		// "Error while loading script from associated file.", e );
		// }
		// }

		// play count
		countSomething("edu.cmu.cs.stage3.alice.authoringtool.playCount");

		world.preserve();
		try {
			// scheduler.addDoOnceRunnable(new Runnable() {
			// public void run() {
			// double t = AuthoringToolResources.getCurrentTime();
			// world.start(t);
			// //System.out.println( "world.start( " + t + " )" );
			// }
			// });

			worldClock.start();
			scheduler.addEachFrameRunnable(worldScheduleRunnable);
			actions.pauseWorldAction.setEnabled(true);
			actions.resumeWorldAction.setEnabled(false);
			fireStateChanged(AuthoringToolStateChangedEvent.AUTHORING_STATE,
					AuthoringToolStateChangedEvent.RUNTIME_STATE);
			fireWorldStarted(AuthoringToolStateChangedEvent.AUTHORING_STATE,
					AuthoringToolStateChangedEvent.RUNTIME_STATE, world);
		} catch (final org.python.core.PyException e) {
			world.restore();
			AuthoringTool.showErrorDialog("Error during world start.", null);
			if (org.python.core.Py.matchException(e, org.python.core.Py.SystemExit)) {
				// TODO
			} else {
				org.python.core.Py.printException(e, null, pyStdErr);
			}
			return false;
		} catch (final edu.cmu.cs.stage3.alice.core.SimulationException e) {
			world.restore();
			showSimulationExceptionDialog(e);
			return false;
		} catch (final edu.cmu.cs.stage3.alice.core.ExceptionWrapper e) {
			world.restore();
			final Exception wrappedException = e.getWrappedException();
			if (wrappedException instanceof edu.cmu.cs.stage3.alice.core.SimulationException) {
				showSimulationExceptionDialog((edu.cmu.cs.stage3.alice.core.SimulationException) wrappedException);
			} else {
				AuthoringTool.showErrorDialog("Error during world start.", wrappedException);
			}
			return false;
		} catch (final Throwable t) {
			world.restore();
			showErrorDialog("Error during world start.", t);
			return false;
		}
		return true;
	}

	public void worldStopRunning() {
		fireStateChanging(AuthoringToolStateChangedEvent.RUNTIME_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE);
		fireWorldStopping(AuthoringToolStateChangedEvent.RUNTIME_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE,
				world);
		// Thread.dumpStack();
		scheduler.removeEachFrameRunnable(worldScheduleRunnable);
		try {
			// world.stop(AuthoringToolResources.getCurrentTime());
			worldClock.stop();
		} catch (final org.python.core.PyException e) {
			AuthoringTool.showErrorDialog("Error during world stop.", null);
			if (org.python.core.Py.matchException(e, org.python.core.Py.SystemExit)) {
				// TODO
			} else {
				org.python.core.Py.printException(e, null, pyStdErr);
			}
		} catch (final Throwable t) {
			AuthoringTool.showErrorDialog("Error during world stop.", t);
		}
		world.restore();
		undoRedoStack.setIsListening(true);
		fireStateChanged(AuthoringToolStateChangedEvent.RUNTIME_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE);
		fireWorldStopped(AuthoringToolStateChangedEvent.RUNTIME_STATE, AuthoringToolStateChangedEvent.AUTHORING_STATE,
				world);
	}

	public double getAspectRatio() {
		double aspectRatio = 0.0;
		if (getCurrentCamera() instanceof edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) {
			final edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera cam = (edu.cmu.cs.stage3.alice.core.camera.SymmetricPerspectiveCamera) getCurrentCamera();
			final Number hAngle = cam.horizontalViewingAngle.getNumberValue();
			final Number vAngle = cam.verticalViewingAngle.getNumberValue();
			if (hAngle != null && vAngle != null) {
				aspectRatio = hAngle.doubleValue() / vAngle.doubleValue();
			}
		}
		return aspectRatio;
	}

	private void checkForUnreferencedCurrentMethod() {
		final Object object = getObjectBeingEdited();
		if (object instanceof edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse) {
			if (!AuthoringToolResources
					.isMethodHookedUp((edu.cmu.cs.stage3.alice.core.response.UserDefinedResponse) object, world)
					&& !authoringToolConfig.getValue("doNotShowUnhookedMethodWarning").equalsIgnoreCase("true")) {
				final String objectRepr = AuthoringToolResources.getReprForValue(object, true);
				edu.cmu.cs.stage3.swing.DialogManager.showMessageDialog(
						"The current method (" + objectRepr
								+ ") is not called by any events or by any other methods which might be called by any events.",
						"Warning: Current method will not be called.", javax.swing.JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	public void play() {
		jAliceFrame.playButton.setEnabled(false);

		checkForUnreferencedCurrentMethod();

		if (worldRun()) {
			final double aspectRatio = getAspectRatio();
			stdErrOutContentPane.stopReactingToPrint();
			renderContentPane.setAspectRatio(aspectRatio);
			renderContentPane.getRenderPanel().add(renderPanel, java.awt.BorderLayout.CENTER);

			edu.cmu.cs.stage3.swing.DialogManager.showDialog(renderContentPane);
			stdErrOutContentPane.startReactingToPrint();
		}
		jAliceFrame.playButton.setEnabled(true);
	}

	public static boolean isresizable = true;

	public javax.swing.JPanel playWhileEncoding(final String directory) {
		jAliceFrame.playButton.setEnabled(false);

		checkForUnreferencedCurrentMethod();

		if (worldRun()) {
			final double aspectRatio = getAspectRatio();
			stdErrOutContentPane.stopReactingToPrint();
			captureContentPane.setExportDirectory(directory);
			captureContentPane.captureInit();
			captureContentPane.setAspectRatio(aspectRatio);
			captureContentPane.getRenderPanel().add(renderPanel, java.awt.BorderLayout.CENTER);
			jAliceFrame.sceneEditor.makeDirty();
			isresizable = false;
			edu.cmu.cs.stage3.swing.DialogManager.showDialog(captureContentPane);
			stdErrOutContentPane.startReactingToPrint();
		}
		jAliceFrame.playButton.setEnabled(true);
		isresizable = true;
		return captureContentPane;
	}

	public void pause() {
		new Thread(new PauseSound(true)).start();
		worldClock.pause();
		actions.pauseWorldAction.setEnabled(false);
		actions.resumeWorldAction.setEnabled(true);
		renderTarget.getAWTComponent().requestFocus();
	}

	public edu.cmu.cs.stage3.alice.core.clock.DefaultClock getWorldClock() {
		return worldClock;
	}

	public void resume() {
		new Thread(new PauseSound(false)).start();
		worldClock.resume();
		actions.pauseWorldAction.setEnabled(true);
		actions.resumeWorldAction.setEnabled(false);
		renderTarget.getAWTComponent().requestFocus();
	}

	public void restartWorld() {
		try {
			worldClock.stop();
			sound.clear();
			world.restore();
			actions.pauseWorldAction.setEnabled(true);
			actions.resumeWorldAction.setEnabled(false);
			// renderContentPane.setSpeedSliderValue(0);
			Thread.sleep(100); // Need this pause so the world starts from the
								// beginning.
			worldClock.start();
		} catch (final org.python.core.PyException e) {
			AuthoringTool.showErrorDialog("Error while restarting world.", null);
			if (org.python.core.Py.matchException(e, org.python.core.Py.SystemExit)) {
				// TODO
			} else {
				org.python.core.Py.printException(e, null, pyStdErr);
			}
		} catch (final edu.cmu.cs.stage3.alice.core.SimulationException e) {
			showSimulationExceptionDialog(e);
		} catch (final edu.cmu.cs.stage3.alice.core.ExceptionWrapper e) {
			final Exception wrappedException = e.getWrappedException();
			if (wrappedException instanceof edu.cmu.cs.stage3.alice.core.SimulationException) {
				showSimulationExceptionDialog((edu.cmu.cs.stage3.alice.core.SimulationException) wrappedException);
			} else {
				AuthoringTool.showErrorDialog("Error while restarting world.", wrappedException);
			}
		} catch (final Throwable t) {
			AuthoringTool.showErrorDialog("Error while restarting world.", t);
		}
		renderTarget.getAWTComponent().requestFocus();
	}

	public void stopWorld() {
		sound.clear();
		renderContentPane.stopWorld();
	}

	public void setWorldSpeed(final double newSpeed) {
		worldClock.setSpeed(newSpeed);
	}

	public void takePicture() {
		try {
			storeCapturedImage(renderTarget.getOffscreenImage());
		} catch (final Throwable t) {
			AuthoringTool.showErrorDialog("Error capturing image.", t);
		}
		renderTarget.getAWTComponent().requestFocus();
	}

	public movieMaker.SoundStorage getSoundStorage() {
		return soundStorage;
	}

	public void setSoundStorage(final movieMaker.SoundStorage myS) {
		soundStorage = myS;
	}

	public static void pauseSound(final Player m_player) {
		sound.add(m_player);
		m_player.startFromBeginning();
	}

	private class PauseSound implements Runnable {
		boolean pause = false;

		public PauseSound(final boolean pause) {
			this.pause = pause;
		}

		@Override
		public void run() {
			if (pause) {
				for (int i = 0; i < sound.size(); i++) {
					sound.get(i).stop();
				}
			} else {
				for (int i = 0; i < sound.size(); i++) {
					sound.get(i).start();
				}
			}
		}

	}

	// Aik Min
	private static boolean pulsing = true;
	private static JLabel statusLabel = new JLabel("Ready");
	private static JFrame statusFrame;

	public void updateAlice() {
		createStatusFrame();
		pulsing = true;
		final Thread pulse = new Thread(new StatusPulsing(statusLabel, " Updating "));
		statusFrame.setVisible(true);
		pulse.start();
		new Thread(new StartUpdating()).start();
	}

	public static void createStatusFrame() {
		final Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		statusFrame = new JFrame("Updating Alice 2.2");
		statusFrame.setPreferredSize(new Dimension(250, 100));
		statusFrame.setLocation(dim.getSize().width / 2 - 150, dim.getSize().height / 2 - 50);
		statusFrame.getContentPane().add(statusLabel);
		statusFrame.setVisible(false);
		statusFrame.setAlwaysOnTop(true);
		statusFrame.pack();
	}

	public class StartUpdating implements Runnable {

		@Override
		public void run() {
			final StringBuffer sb = new StringBuffer("http://alicedownloads.org/alice.jar");
			java.io.BufferedInputStream bis = null;
			java.io.BufferedOutputStream bos = null;
			try {
				final java.net.URL url = new java.net.URL(sb.toString());
				final java.net.URLConnection urlc = url.openConnection();

				final java.io.File aliceHome = new java.io.File(
						JAlice.getAliceHomeDirectory().toString() + "\\lib\\aliceupdate.jar");

				bis = new java.io.BufferedInputStream(urlc.getInputStream());
				bos = new java.io.BufferedOutputStream(new java.io.FileOutputStream(aliceHome));

				int i;
				while ((i = bis.read()) != -1) {
					bos.write(i);
				}
				pulsing = false;
				statusFrame.dispose();
				javax.swing.JOptionPane.showMessageDialog(null,
						" You must restart Alice for the updates to take effect. ", "Update completed",
						javax.swing.JOptionPane.INFORMATION_MESSAGE);
			} catch (final Exception e1) {
				pulsing = false;
				statusFrame.dispose();
				javax.swing.JOptionPane.showMessageDialog(null,
						" Update failed. Please check your internet connection. ", "Update failed",
						javax.swing.JOptionPane.ERROR_MESSAGE);
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (final java.io.IOException ioe) {
						ioe.printStackTrace();
					}
				}
				if (bos != null) {
					try {
						bos.close();
					} catch (final java.io.IOException ioe) {
						ioe.printStackTrace();
					}
				}

			}
		}
	}

	public class StatusPulsing implements Runnable {
		private final JLabel label;

		private final String text;

		public StatusPulsing(final JLabel l, final String s) {
			label = l;
			text = s;
		}

		@Override
		public void run() {

			final long time = 300;
			while (AuthoringTool.pulsing) {
				label.setText(text);
				try {
					Thread.sleep(time);
				} catch (final InterruptedException e) {
				}
				label.setText(text + ".");
				try {
					Thread.sleep(time);
				} catch (final InterruptedException e) {
				}
				label.setText(text + ". .");
				try {
					Thread.sleep(time);
				} catch (final InterruptedException e) {
				}
				label.setText(text + ". . .");
				try {
					Thread.sleep(time);
				} catch (final InterruptedException e) {
				}
			}
		}

	}

}
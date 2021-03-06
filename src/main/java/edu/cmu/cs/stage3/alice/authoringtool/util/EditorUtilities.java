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

package edu.cmu.cs.stage3.alice.authoringtool.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Vector;

import edu.cmu.cs.stage3.alice.authoringtool.Editor;

/**
 * Utilities for Editors. This Class keeps the master list of which Editors are
 * available, as well as providing a bunch of static methods for manipulating
 * and getting information from Editors.
 *
 * @author Jason Pratt
 * @see edu.cmu.cs.stage3.alice.authoringtool.Editor
 */
@SuppressWarnings("unchecked")
public final class EditorUtilities {
	private static Class<? extends Editor>[] allEditors = null;

	static {
		allEditors = edu.cmu.cs.stage3.alice.authoringtool.AuthoringToolResources.getEditorClasses();
		if (allEditors == null) {
			edu.cmu.cs.stage3.alice.authoringtool.AuthoringTool.showErrorDialog("no editors found!", null);
			allEditors = new Class[0];
		}
		// TODO: auto-find more editors?
	}

	/**
	 * Returns the current list of Editors that the system knows about. You must
	 * call findAllEditors if you want to find new Editors that have been added
	 * since initialization.
	 */
	public static Class<? extends Editor>[] getAllEditors() {
		return allEditors;
	}

	/**
	 * Returns all of the Editors that are able to view the specified Object
	 * type.
	 *
	 * TODO: CACHING...
	 */
	public static Class<? extends Editor>[] getEditorsForClass(final Class<?> objectClass) {
		final Vector<Class<? extends Editor>> editors = new Vector<>();
		if (!Object.class.isAssignableFrom(objectClass)) {
			return null;
		}
		if (allEditors == null) {
			return null;
		}
		for (final Class<? extends Editor> allEditor : allEditors) {
			final Class<?> acceptedClass = getObjectParameter(allEditor);
			if (acceptedClass.isAssignableFrom(objectClass)) {
				editors.addElement(allEditor);
			}
		}

		sort(editors, objectClass);

		final Class<? extends Editor>[] cvs = new Class[editors.size()];
		for (int i = 0; i < cvs.length; i++) {
			cvs[i] = (Class<? extends Editor>) editors.elementAt(i);
		}
		return cvs;
	}

	/**
	 * returns true if potentialEditor is in the current list of all known
	 * Editors.
	 */
	public static boolean isInAllEditors(final Class<? extends Editor> potentialEditor) {
		if (allEditors == null) {
			return false;
		}
		for (final Class<? extends Editor> allEditor : allEditors) {
			if (potentialEditor == allEditor) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a new Editor instance of the specified Class
	 */
	public static edu.cmu.cs.stage3.alice.authoringtool.Editor getEditorFromClass(final Class<? extends Editor> editorClass) {
		try {
			return (edu.cmu.cs.stage3.alice.authoringtool.Editor) editorClass.newInstance();
		} catch (final Throwable t) {
			edu.cmu.cs.stage3.alice.authoringtool.AuthoringTool
					.showErrorDialog("Error creating new editor of type " + editorClass, t);
		}
		return null;
	}

	public static java.lang.reflect.Method getSetMethodFromClass(final Class<? extends Editor> editorClass) {
		final java.lang.reflect.Method[] methods = editorClass.getMethods();
		for (final Method potentialMethod : methods) {
			if (potentialMethod.getName().equals("setObject")) {
				final Class<?>[] parameterTypes = potentialMethod.getParameterTypes();
				if (parameterTypes.length == 1) {
					if (Object.class.isAssignableFrom(parameterTypes[0])) {
						return potentialMethod;
					}
				}
			}
		}

		return null;
	}

	public static Class<?> getObjectParameter(final Class<? extends Editor> editorClass) {
		final java.lang.reflect.Method setObject = getSetMethodFromClass(editorClass);
		if (setObject != null) {
			return setObject.getParameterTypes()[0];
		}

		return null;
	}

	/**
	 * Returns the Editor type can most suitably view the given objectClass.
	 * Suitability is determined by getting the Class hierarchical distance
	 * between the given objectClass and the actual type that each Editor's
	 * setObject method accepts.
	 */
	public static Class<? extends Editor> getBestEditor(final Class<?> objectClass) {
		// DEBUG System.out.println( "objectClass: " + objectClass );
		Class<? extends Editor> bestEditor = null;
		int bestDepth = Integer.MAX_VALUE;
		for (final Class<? extends Editor> editorClass : allEditors) {
			// DEBUG System.out.println( "editorClass: " + editorClass );
			final java.lang.reflect.Method setObject = getSetMethodFromClass(editorClass);
			if (setObject != null) {
				final Class<?>[] parameterTypes = setObject.getParameterTypes();
				if (parameterTypes.length == 1) {
					final int depth = getObjectClassDepth(parameterTypes[0], objectClass);
					// DEBUG System.out.println( "getObjectClassDepth( " +
					// parameterTypes[0] + ", " + objectClass + " ): " + depth
					// );
					if (depth < bestDepth && depth >= 0) {
						bestDepth = depth;
						bestEditor = editorClass;
						// DEBUG System.out.println( "setting bestEditor: " +
						// bestEditor );
					}
				}

			}
		}
		// DEBUG System.out.println( "bestEditor: " + bestEditor );
		return bestEditor;
	}

	public static void editObject(final edu.cmu.cs.stage3.alice.authoringtool.Editor editor, final Object object) {
		final java.lang.reflect.Method setObject = getSetMethodFromClass(editor.getClass());
		try {
			setObject.invoke(editor, new Object[] { object });
		} catch (final Exception e) {
			edu.cmu.cs.stage3.alice.authoringtool.AuthoringTool.showErrorDialog("Error editing object: " + object, e);
		}
	}

	// Unused ??
	/**
	 * This method <bold>defines</bold> what it means to be a valid Editor.
	 *
	 * To be valid, a Class must: -implement Editor -have an constructor that
	 * takes no arguments -have a method called setObject that takes a single
	 * argument
	 */
	@SuppressWarnings("unused")
	private static boolean isValidEditor(final Class<? extends Editor> editorClass) {
		if (!edu.cmu.cs.stage3.alice.authoringtool.Editor.class.isAssignableFrom(editorClass)) {
			return false;
		}

		boolean constructorFound = false;
		final java.lang.reflect.Constructor<?>[] editorConstructors = editorClass.getConstructors();
		for (final Constructor<?> editorConstructor : editorConstructors) {
			final Class<?>[] parameterTypes = editorConstructor.getParameterTypes();
			if (parameterTypes.length == 0) {
				constructorFound = true;
				break;
			}
		}

		if (getSetMethodFromClass(editorClass) != null && constructorFound) {
			return true;
		}

		return false;
	}

	/**
	 * Determines how close a subclass is to a superclass
	 *
	 * @returns the depth of the class hierarchy between the given superclass
	 *          and subclass
	 */
	private static int getObjectClassDepth(final Class<?> superclass, final Class<?> subclass) {
		if (!superclass.isAssignableFrom(subclass)) {
			return -1;
		}

		Class<?> temp = subclass;
		int i = 0;
		while (temp != superclass && superclass.isAssignableFrom(temp)) {
			i++;
			temp = temp.getSuperclass();
		}

		return i;
	}

	/**
	 * Swaps elements a and b in Vector v
	 */
	private static void swap(final Vector<Class<? extends Editor>> v, final int a, final int b) {
		final Class<? extends Editor> t = v.elementAt(a);
		v.setElementAt(v.elementAt(b), a);
		v.setElementAt(t, b);
	}

	/**
	 * Compares two EditorClasses based on how close their setObject's Object
	 * parameter is to the given objectClass.
	 *
	 * returns -1 if a is closer returns 1 if b is closer returns 0 if they are
	 * equally close
	 */
	private static int compare(final Class<? extends Editor> a, final Class<? extends Editor> b, final Class<?> objectClass) {
		final int aDist = getObjectClassDepth(getObjectParameter(a), objectClass);
		final int bDist = getObjectClassDepth(getObjectParameter(b), objectClass);
		if (aDist < bDist) {
			return -1;
		} else if (bDist < aDist) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Sorts a Vector of Editor Classes based on how fit they are to view the
	 * given objectClass
	 *
	 * This is going to be used on small vectors, so I'm just using Insertion
	 * sort.
	 */
	private static void sort(final Vector<Class<? extends Editor>> v, final Class<?> objectClass) {
		for (int i = 0; i < v.size(); i++) {
			for (int j = i; j > 0
					&& compare((Class<? extends Editor>) v.elementAt(j - 1), (Class<? extends Editor>) v.elementAt(j), objectClass) > 0; j--) {
				swap(v, j, j - 1);
			}
		}
	}
}

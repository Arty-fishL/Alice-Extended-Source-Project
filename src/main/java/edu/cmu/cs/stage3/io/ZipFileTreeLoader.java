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

package edu.cmu.cs.stage3.io;

import java.util.Enumeration;
import java.util.zip.ZipEntry;

/**
 * @author Dennis Cosgrove
 */
public class ZipFileTreeLoader implements DirectoryTreeLoader {
	private static String getCanonicalPathname(String pathname) {
		pathname = pathname.replace('\\', '/');

		// remove double separators
		int index;
		while ((index = pathname.indexOf("//")) != -1) {
			pathname = pathname.substring(0, index + 1) + pathname.substring(index + 2);
		}

		if (pathname.charAt(0) == '/') {
			pathname = pathname.substring(1);
		}

		return pathname;
	}

	private final java.util.Hashtable<String, ZipEntry> m_pathnameToZipEntryMap = new java.util.Hashtable<String, ZipEntry>();
	private java.io.File m_rootFile = null;
	private java.util.zip.ZipFile m_zipFile = null;
	private String m_currentDirectory = null;
	private java.io.InputStream m_currentlyOpenStream = null;

	@Override
	public void open(final Object pathname) throws IllegalArgumentException, java.io.IOException {
		if (m_zipFile != null) {
			close();
		}

		if (pathname instanceof String) {
			m_rootFile = new java.io.File((String) pathname);
		} else if (pathname instanceof java.io.File) {
			m_rootFile = (java.io.File) pathname;
		} else if (pathname == null) {
			throw new IllegalArgumentException("pathname is null");
		} else {
			throw new IllegalArgumentException("pathname must be an instance of String or java.io.File");
		}
		m_zipFile = new java.util.zip.ZipFile(m_rootFile);
		m_currentDirectory = "";
		m_currentlyOpenStream = null;
		m_pathnameToZipEntryMap.clear();
		final Enumeration<? extends ZipEntry> enum0 = m_zipFile.entries();
		while (enum0.hasMoreElements()) {
			final java.util.zip.ZipEntry zipEntry = (java.util.zip.ZipEntry) enum0.nextElement();
			m_pathnameToZipEntryMap.put(getCanonicalPathname(zipEntry.getName()), zipEntry);
		}
	}

	@Override
	public void close() throws java.io.IOException {
		if (m_zipFile != null) {
			closeCurrentFile();
			m_zipFile.close();
			m_zipFile = null;
		}
	}

	@Override
	public void setCurrentDirectory(String pathname) throws IllegalArgumentException {
		if (pathname == null) {
			pathname = "";
		} else if (pathname.length() > 0) {
			if (!(pathname.charAt(0) == '/' || pathname.charAt(0) == '\\')) {
				pathname = m_currentDirectory + pathname;
			}

			pathname = getCanonicalPathname(pathname);

			if (!pathname.endsWith("/")) {
				pathname = pathname + "/";
			}

			if (!pathname.startsWith("/")) {
				pathname = "/" + pathname;
			}
		}

		m_currentDirectory = pathname;
	}

	@Override
	public String getCurrentDirectory() {
		return m_currentDirectory;
	}

	@Override
	public java.io.InputStream readFile(final String filename) throws IllegalArgumentException, java.io.IOException {
		closeCurrentFile();
		final String pathname = getCanonicalPathname(m_currentDirectory + filename);
		final java.util.zip.ZipEntry zipEntry = m_pathnameToZipEntryMap.get(pathname);
		if (zipEntry != null) {
			m_currentlyOpenStream = m_zipFile.getInputStream(zipEntry);
			return m_currentlyOpenStream;
		} else {
			throw new java.io.FileNotFoundException("Not Found: " + pathname);
		}
	}

	@Override
	public void closeCurrentFile() throws java.io.IOException {
		if (m_currentlyOpenStream != null) {
			m_currentlyOpenStream.close();
			m_currentlyOpenStream = null;
		}
	}

	@Override
	public String[] getFilesInCurrentDirectory() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public String[] getDirectoriesInCurrentDirectory() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public boolean isKeepFileSupported() {
		return true;
	}

	static Object getKeepKey(final java.io.File file, final String currentDirectory, final String filename) {
		return file.getAbsolutePath() + "____" + currentDirectory + filename;
	}

	@Override
	public Object getKeepKey(final String filename) {
		return getKeepKey(m_rootFile, m_currentDirectory, filename);
	}
}

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

/**
 * @author Jason Pratt
 */
public class ZipTreeStorer implements DirectoryTreeStorer {
	protected java.util.zip.ZipOutputStream zipOut = null;
	protected String currentDirectory = null;
	protected java.util.zip.ZipEntry currentEntry = null;

	protected boolean isCompressed() {
		return true;
	}

	/**
	 * pathname can be a String (representing a file on disk), a java.io.File,
	 * or a java.io.OutputStream
	 */
	@Override
	public void open(final Object pathname) throws IllegalArgumentException, java.io.IOException {
		if (zipOut != null) {
			close();
		}

		java.io.OutputStream out = null;
		if (pathname instanceof String) {
			out = new java.io.FileOutputStream((String) pathname);
		} else if (pathname instanceof java.io.File) {
			out = new java.io.FileOutputStream((java.io.File) pathname);
		} else if (pathname instanceof java.io.OutputStream) {
			out = (java.io.OutputStream) pathname;
		} else {
			throw new IllegalArgumentException(
					"pathname must be an instance of String, java.io.File, or java.io.OutputStream");
		}

		zipOut = new java.util.zip.ZipOutputStream(new java.io.BufferedOutputStream(out));
		if (isCompressed()) {
			// pass
		} else {
			zipOut.setMethod(java.util.zip.ZipOutputStream.STORED);
		}
		currentDirectory = "";
	}

	@Override
	public void close() throws java.io.IOException {
		if (zipOut != null) {
			closeCurrentFile();
			zipOut.flush();
			zipOut.finish();
			zipOut.close();
			zipOut = null;
		}
	}

	@Override
	public void createDirectory(final String pathname) throws IllegalArgumentException, java.io.IOException {
		if (pathname.indexOf('/') != -1 || pathname.indexOf('\\') != -1) {
			throw new IllegalArgumentException("pathname cannot contain path separators");
		}
		if (pathname.length() <= 0) {
			throw new IllegalArgumentException("pathname has no length");
		}

		final java.util.zip.ZipEntry newEntry = new java.util.zip.ZipEntry(currentDirectory + pathname + "/");
		if (zipOut != null) {
			zipOut.putNextEntry(newEntry);
			zipOut.closeEntry();
		} else {
			throw new java.io.IOException("No zip file currently open");
		}
	}

	@Override
	public void setCurrentDirectory(String pathname) throws IllegalArgumentException {
		if (pathname == null) {
			pathname = "";
		} else if (pathname.length() > 0) {
			pathname = pathname.replace('\\', '/');

			// remove double separators
			int index;
			while ((index = pathname.indexOf("//")) != -1) {
				pathname = pathname.substring(0, index + 1) + pathname.substring(index + 2);
			}

			if (pathname.charAt(0) == '/') {
				pathname = pathname.substring(1);
			} else {
				pathname = currentDirectory + pathname;
			}

			if (!pathname.endsWith("/")) {
				pathname = pathname + "/";
			}
			if (!pathname.startsWith("/")) {
				pathname = "/" + pathname;
			}
		}

		currentDirectory = pathname;
	}

	@Override
	public String getCurrentDirectory() {
		return currentDirectory;
	}

	@Override
	public java.io.OutputStream createFile(final String filename, final boolean compressItIfYouGotIt)
			throws IllegalArgumentException, java.io.IOException {
		// TODO: respect compressItIfYouGotIt
		if (zipOut != null) {
			currentEntry = new java.util.zip.ZipEntry(currentDirectory + filename);
			if (isCompressed()) {
				// pass
			} else {
				currentEntry.setMethod(java.util.zip.ZipEntry.STORED);
			}
			zipOut.putNextEntry(currentEntry);
		} else {
			throw new java.io.IOException("No zip file currently open");
		}

		return zipOut;
	}

	@Override
	public void closeCurrentFile() throws java.io.IOException {
		if (currentEntry != null) {
			zipOut.flush();
			zipOut.closeEntry();
			currentEntry = null;
		}
	}

	@Override
	public Object getKeepKey(final String filename) {
		return null;
	}

	@Override
	public boolean isKeepFileSupported() {
		return false;
	}

	@Override
	public void keepFile(final String filename) throws KeepFileNotSupportedException, KeepFileDoesNotExistException {
		throw new KeepFileNotSupportedException();
	}
}
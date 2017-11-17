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

package edu.cmu.cs.stage3.image.codec;

/*
 * The contents of this file are subject to the  JAVA ADVANCED IMAGING
 * SAMPLE INPUT-OUTPUT CODECS AND WIDGET HANDLING SOURCE CODE  License
 * Version 1.0 (the "License"); You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.sun.com/software/imaging/JAI/index.html
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is JAVA ADVANCED IMAGING SAMPLE INPUT-OUTPUT CODECS
 * AND WIDGET HANDLING SOURCE CODE.
 * The Initial Developer of the Original Code is: Sun Microsystems, Inc..
 * Portions created by: _______________________________________
 * are Copyright (C): _______________________________________
 * All Rights Reserved.
 * Contributor(s): _______________________________________
 */

import java.io.IOException;
import java.util.StringTokenizer;

//
// NOTE -- all 'long' variables are really at most 32 bits,
// corresponding to Microsoft 'ULONG' variables.
//

// Temporary assumptions:
//
// All streams, including the ministream, are shorter than 2GB (size < 2GB)
//
// There are < 2^31 directory entries (#streams < 2^31)
//

class SSDirectoryEntry {

	int index;
	String name;
	long size;
	long startSector;
	long SIDLeftSibling;
	long SIDRightSibling;
	long SIDChild;

	public SSDirectoryEntry(final int index, final String name, final long size, final long startSector,
			final long SIDLeftSibling, final long SIDRightSibling, final long SIDChild) {
		this.name = name;
		this.index = index;
		this.size = size;
		this.startSector = startSector;
		this.SIDLeftSibling = SIDLeftSibling;
		this.SIDRightSibling = SIDRightSibling;
		this.SIDChild = SIDChild;

		// System.out.println("Got a directory entry named " + name +
		// " (index " + index + ")");
		// System.out.println("Start sector = " + startSector);
	}

	public String getName() {
		return name;
	}

	public long getSize() {
		return size;
	}

	public long getStartSector() {
		return startSector;
	}

	public long getSIDLeftSibling() {
		return SIDLeftSibling;
	}

	public long getSIDRightSibling() {
		return SIDRightSibling;
	}

	public long getSIDChild() {
		return SIDChild;
	}
}

public class StructuredStorage {

	SeekableStream file;

	// Header fields
	private int sectorShift; // ULONG -- must be between 1 and 31
	private int miniSectorShift;
	private long csectFat;
	private long sectDirStart;
	private long miniSectorCutoff;
	private long sectMiniFatStart;
	private long csectMiniFat;
	private long sectDifStart;
	private long csectDif;
	private long[] sectFat;

	// FAT, MiniFAT, and ministream in unrolled format
	// private long[] FAT; // ULONG -- only 2G entries max
	private long[] MINIFAT; // ULONG -- only 2G entries max
	private SSDirectoryEntry[] DIR;

	private SeekableStream miniStream;
	private SeekableStream FATStream;

	// The index of the current directory
	long cwdIndex = -1L;

	public StructuredStorage(final SeekableStream file) throws IOException {
		this.file = file;

		// Read fields from the header
		getHeader();

		// Read the FAT
		getFat();

		// Read the MiniFAT
		getMiniFat();

		// Read the directory
		getDirectory();

		// Read the MiniStream
		getMiniStream();
	}

	private void getHeader() throws IOException {
		file.seek(0x1e);
		sectorShift = file.readUnsignedShortLE();

		file.seek(0x20);
		miniSectorShift = file.readUnsignedShortLE();

		file.seek(0x2c);
		csectFat = file.readUnsignedIntLE();

		file.seek(0x30);
		sectDirStart = file.readUnsignedIntLE();

		file.seek(0x38);
		miniSectorCutoff = file.readUnsignedIntLE();

		file.seek(0x3c);
		sectMiniFatStart = file.readUnsignedIntLE();

		file.seek(0x40);
		csectMiniFat = file.readUnsignedIntLE();

		file.seek(0x44);
		sectDifStart = file.readUnsignedIntLE();

		file.seek(0x48);
		csectDif = file.readUnsignedIntLE();

		sectFat = new long[109];
		file.seek(0x4c);
		for (int i = 0; i < 109; i++) {
			sectFat[i] = file.readUnsignedIntLE();
		}
	}

	private void getFat() throws IOException {
		final int size = getSectorSize();
		final int sectsPerFat = size / 4;
		final int fatsPerDif = size / 4 - 1;

		final int numFATSectors = (int) (csectFat + csectDif * fatsPerDif);
		final long[] FATSectors = new long[numFATSectors];
		int count = 0;

		for (int i = 0; i < 109; i++) {
			final long sector = sectFat[i];
			if (sector == 0xFFFFFFFFL) {
				break;
			}

			FATSectors[count++] = getOffsetOfSector(sectFat[i]);
		}

		if (csectDif > 0) {
			long dif = sectDifStart;
			final byte[] difBuf = new byte[size];

			for (int i = 0; i < csectDif; i++) {
				readSector(dif, difBuf, 0);
				for (int j = 0; j < fatsPerDif; j++) {
					final int sec = FPXUtils.getIntLE(difBuf, 4 * j);
					FATSectors[count++] = getOffsetOfSector(sec);
				}

				dif = FPXUtils.getIntLE(difBuf, size - 4);
			}
		}

		FATStream = new SegmentedSeekableStream(file, FATSectors, size, numFATSectors * size, true);
	}

	private void getMiniFat() throws IOException {
		final int size = getSectorSize();
		final int sectsPerFat = size / 4;
		int index = 0;

		MINIFAT = new long[(int) (csectMiniFat * sectsPerFat)];

		long sector = sectMiniFatStart;
		final byte[] buf = new byte[size];
		while (sector != 0xFFFFFFFEL) {
			readSector(sector, buf, 0);
			for (int j = 0; j < sectsPerFat; j++) {
				MINIFAT[index++] = FPXUtils.getIntLE(buf, 4 * j);
			}
			sector = getFATSector(sector);
		}
	}

	private void getDirectory() throws IOException {
		final int size = getSectorSize();
		long sector = sectDirStart;

		// Count the length of the directory in sectors
		int numDirectorySectors = 0;
		while (sector != 0xFFFFFFFEL) {
			sector = getFATSector(sector);
			++numDirectorySectors;
		}

		final int directoryEntries = 4 * numDirectorySectors;
		DIR = new SSDirectoryEntry[directoryEntries];

		sector = sectDirStart;
		final byte[] buf = new byte[size];
		int index = 0;
		while (sector != 0xFFFFFFFEL) {
			readSector(sector, buf, 0);

			int offset = 0;
			for (int i = 0; i < 4; i++) { // 4 dirents per sector
				// We divide the length by 2 for now even though
				// the spec says not to...
				final int length = FPXUtils.getShortLE(buf, offset + 0x40);

				final String name = FPXUtils.getString(buf, offset + 0x00, length);
				final long SIDLeftSibling = FPXUtils.getUnsignedIntLE(buf, offset + 0x44);
				final long SIDRightSibling = FPXUtils.getUnsignedIntLE(buf, offset + 0x48);
				final long SIDChild = FPXUtils.getUnsignedIntLE(buf, offset + 0x4c);
				final long startSector = FPXUtils.getUnsignedIntLE(buf, offset + 0x74);
				final long streamSize = FPXUtils.getUnsignedIntLE(buf, offset + 0x78);

				DIR[index] = new SSDirectoryEntry(index, name, streamSize, startSector, SIDLeftSibling, SIDRightSibling,
						SIDChild);
				++index;
				offset += 128;
			}

			sector = getFATSector(sector);
		}
	}

	private void getMiniStream() throws IOException {
		final int length = getLength(0L);
		final int sectorSize = getSectorSize();
		final int sectors = (length + sectorSize - 1) / sectorSize;

		final long[] segmentPositions = new long[sectors];

		long sector = getStartSector(0);

		for (int i = 0; i < sectors - 1; i++) {
			segmentPositions[i] = getOffsetOfSector(sector);
			sector = getFATSector(sector);
		}
		segmentPositions[sectors - 1] = getOffsetOfSector(sector);

		miniStream = new SegmentedSeekableStream(file, segmentPositions, sectorSize, length, true);
	}

	private int getSectorSize() {
		return 1 << sectorShift;
	}

	private long getOffsetOfSector(final long sector) {
		return sector * getSectorSize() + 512;
	}

	private int getMiniSectorSize() {
		return 1 << miniSectorShift;
	}

	private long getOffsetOfMiniSector(final long sector) {
		return sector * getMiniSectorSize();
	}

	private void readMiniSector(final long sector, final byte[] buf, final int offset, final int length)
			throws IOException {
		miniStream.seek(getOffsetOfMiniSector(sector));
		miniStream.read(buf, offset, length);
	}

	private void readMiniSector(final long sector, final byte[] buf, final int offset) throws IOException {
		readMiniSector(sector, buf, offset, getMiniSectorSize());
	}

	private void readSector(final long sector, final byte[] buf, final int offset, final int length)
			throws IOException {
		file.seek(getOffsetOfSector(sector));
		file.read(buf, offset, length);
	}

	private void readSector(final long sector, final byte[] buf, final int offset) throws IOException {
		readSector(sector, buf, offset, getSectorSize());
	}

	private SSDirectoryEntry getDirectoryEntry(final long index) {
		// Assume #streams < 2^31
		return DIR[(int) index];
	}

	private long getStartSector(final long index) {
		// Assume #streams < 2^31
		return DIR[(int) index].getStartSector();
	}

	private int getLength(final long index) {
		// Assume #streams < 2^31
		// Assume size < 2GB
		return (int) DIR[(int) index].getSize();
	}

	private long getFATSector(final long sector) throws IOException {
		FATStream.seek(4 * sector);
		return FATStream.readUnsignedIntLE();
	}

	private long getMiniFATSector(final long sector) {
		return MINIFAT[(int) sector];
	}

	private int getCurrentIndex() {
		return -1;
	}

	private int getIndex(final String name, final int index) {
		return -1;
	}

	private long searchDirectory(final String name, final long index) {
		if (index == 0xFFFFFFFFL) {
			return -1L;
		}

		final SSDirectoryEntry dirent = getDirectoryEntry(index);

		if (name.equals(dirent.getName())) {
			return index;
		} else {
			final long lindex = searchDirectory(name, dirent.getSIDLeftSibling());
			if (lindex != -1L) {
				return lindex;
			}

			final long rindex = searchDirectory(name, dirent.getSIDRightSibling());
			if (rindex != -1L) {
				return rindex;
			}
		}

		return -1L;
	}

	// Public methods

	public void changeDirectoryToRoot() {
		cwdIndex = getDirectoryEntry(0L).getSIDChild();
	}

	public boolean changeDirectory(final String name) {
		final long index = searchDirectory(name, cwdIndex);
		if (index != -1L) {
			cwdIndex = getDirectoryEntry(index).getSIDChild();
			return true;
		} else {
			return false;
		}
	}

	private long getStreamIndex(final String name) {
		// Move down the directory hierarchy
		long index = cwdIndex;

		final StringTokenizer st = new StringTokenizer(name, "/");
		boolean firstTime = true;
		while (st.hasMoreTokens()) {
			final String tok = st.nextToken();

			if (!firstTime) {
				index = getDirectoryEntry(index).getSIDChild();
			} else {
				firstTime = false;
			}
			index = searchDirectory(tok, index);
		}

		return index;
	}

	public byte[] getStreamAsBytes(final String name) throws IOException {
		final long index = getStreamIndex(name);
		if (index == -1L) {
			return null;
		}

		// Cast index to int (streams < 2^31) and cast stream size to an
		// int (size < 2GB)
		final int length = getLength(index);
		final byte[] buf = new byte[length];

		if (length > miniSectorCutoff) {
			final int sectorSize = getSectorSize();
			final int sectors = (length + sectorSize - 1) / sectorSize;

			long sector = getStartSector(index);
			int offset = 0;
			for (int i = 0; i < sectors - 1; i++) {
				readSector(sector, buf, offset, sectorSize);
				offset += sectorSize;
				sector = getFATSector(sector);
			}

			readSector(sector, buf, offset, length - offset);
		} else {
			final int sectorSize = getMiniSectorSize();
			final int sectors = (length + sectorSize - 1) / sectorSize;

			long sector = getStartSector(index);

			// Assume ministream size < 2GB
			int offset = 0;
			for (int i = 0; i < sectors - 1; i++) {
				final long miniSectorOffset = getOffsetOfMiniSector(sector);
				readMiniSector(sector, buf, offset, sectorSize);
				offset += sectorSize;
				sector = getMiniFATSector(sector);
			}
			readMiniSector(sector, buf, offset, length - offset);
		}

		return buf;
	}

	public SeekableStream getStream(final String name) throws IOException {
		final long index = getStreamIndex(name);
		if (index == -1L) {
			return null;
		}

		// Cast index to int (streams < 2^31) and cast stream size to an
		// int (size < 2GB)
		final int length = getLength(index);

		long[] segmentPositions;
		int sectorSize, sectors;

		if (length > miniSectorCutoff) {
			sectorSize = getSectorSize();
			sectors = (length + sectorSize - 1) / sectorSize;
			segmentPositions = new long[sectors];

			long sector = getStartSector(index);
			for (int i = 0; i < sectors - 1; i++) {
				segmentPositions[i] = getOffsetOfSector(sector);
				sector = getFATSector(sector);
			}
			segmentPositions[sectors - 1] = getOffsetOfSector(sector);

			return new SegmentedSeekableStream(file, segmentPositions, sectorSize, length, true);
		} else {
			sectorSize = getMiniSectorSize();
			sectors = (length + sectorSize - 1) / sectorSize;
			segmentPositions = new long[sectors];

			long sector = getStartSector(index);
			for (int i = 0; i < sectors - 1; i++) {
				segmentPositions[i] = getOffsetOfMiniSector(sector);
				sector = getMiniFATSector(sector);
			}
			segmentPositions[sectors - 1] = getOffsetOfMiniSector(sector);

			return new SegmentedSeekableStream(miniStream, segmentPositions, sectorSize, length, true);
		}
	}
	/*
	 * public static void main(String[] args) { try { RandomAccessFile f = new
	 * RandomAccessFile(args[0], "r"); SeekableStream sis = new
	 * FileSeekableStream(f); StructuredStorage ss = new StructuredStorage(sis);
	 *
	 * ss.changeDirectoryToRoot();
	 *
	 * byte[] s = ss.getStreamAsBytes("SummaryInformation");
	 *
	 * PropertySet ps = new PropertySet(new ByteArraySeekableStream(s));
	 *
	 * // Get the thumbnail property byte[] thumb = ps.getBlob(17);
	 *
	 * // Emit it as a BMP file System.out.print("BM"); int fs = (thumb.length -
	 * 8) + 14 + 40; System.out.print((char)(fs & 0xff));
	 * System.out.print((char)((fs >> 8) & 0xff)); System.out.print((char)((fs
	 * >> 16) & 0xff)); System.out.print((char)((fs >> 24) & 0xff));
	 * System.out.print((char)0); System.out.print((char)0);
	 * System.out.print((char)0); System.out.print((char)0);
	 * System.out.print('6'); System.out.print((char)0);
	 * System.out.print((char)0); System.out.print((char)0); for (int i = 8; i <
	 * thumb.length; i++) { System.out.print((char)(thumb[i] & 0xff)); }
	 *
	 * } catch (Exception e) { e.printStackTrace(); } }
	 */
}

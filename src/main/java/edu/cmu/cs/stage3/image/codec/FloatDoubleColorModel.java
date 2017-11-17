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

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/**
 * A <code>ColorModel</code> class that works with pixel values that represent
 * color and alpha information as separate samples, using float or double
 * elements. This class can be used with an arbitrary <code>ColorSpace</code>.
 * The number of color samples in the pixel values must be same as the number of
 * color components in the <code>ColorSpace</code>. There may be a single alpha
 * sample.
 *
 * <p>
 * Sample values are taken as ranging from 0.0 to 1.0; that is, when converting
 * to 8-bit RGB, a multiplication by 255 is performed and values outside of the
 * range 0-255 are clamped at the closest endpoint.
 *
 * <p>
 * For maximum efficiency, pixel data being interpreted by this class should be
 * in the sRGB color space. This will result in only the trivial conversion
 * (scaling by 255 and dividing by any premultiplied alpha) to be performed.
 * Other color spaces require more general conversions.
 *
 * <p>
 * For those methods that use a primitive array pixel representation of type
 * <code>transferType</code>, the array length is the same as the number of
 * color and alpha samples. Color samples are stored first in the array followed
 * by the alpha sample, if present. The order of the color samples is specified
 * by the <code>ColorSpace</code>. Typically, this order reflects the name of
 * the color space type. For example, for <code>TYPE_RGB</code>, index 0
 * corresponds to red, index 1 to green, and index 2 to blue. The transfer types
 * supported are <code>DataBuffer.TYPE_FLOAT</code>,
 * <code>DataBuffer.TYPE_DOUBLE</code>.
 *
 * <p>
 * The translation from pixel values to color/alpha components for display or
 * processing purposes is a one-to-one correspondence of samples to components.
 *
 * <p>
 * Methods that use a single int pixel representation throw an
 * <code>IllegalArgumentException</code>.
 *
 * <p>
 * A <code>FloatDoubleColorModel</code> can be used in conjunction with a
 * <code>ComponentSampleModelJAI</code>.
 *
 * @see ColorModel
 * @see ColorSpace
 * @see ComponentSampleModel
 * @see ComponentSampleModelJAI
 */
public class FloatDoubleColorModel extends ComponentColorModel {

	ColorSpace colorSpace;
	int colorSpaceType;
	int numColorComponents;
	int numComponents;
	int transparency;
	boolean hasAlpha;
	boolean isAlphaPremultiplied;

	private static int[] bitsHelper(final int transferType, final ColorSpace colorSpace, final boolean hasAlpha) {
		final int numBits = transferType == DataBuffer.TYPE_FLOAT ? 32 : 64;
		int numComponents = colorSpace.getNumComponents();
		if (hasAlpha) {
			++numComponents;
		}
		final int[] bits = new int[numComponents];
		for (int i = 0; i < numComponents; i++) {
			bits[i] = numBits;
		}

		return bits;
	}

	/**
	 * Constructs a <code>ComponentColorModel</code> from the specified
	 * parameters. Color components will be in the specified
	 * <code>ColorSpace</code>. <code>hasAlpha</code> indicates whether alpha
	 * information is present. If <code>hasAlpha</code> is true, then the
	 * boolean <code>isAlphaPremultiplied</code> specifies how to interpret
	 * color and alpha samples in pixel values. If the boolean is
	 * <code>true</code>, color samples are assumed to have been multiplied by
	 * the alpha sample. The <code>transparency</code> specifies what alpha
	 * values can be represented by this color model. The
	 * <code>transferType</code> is the type of primitive array used to
	 * represent pixel values.
	 *
	 * @param colorSpace
	 *            The <code>ColorSpace</code> associated with this color model.
	 * @param hasAlpha
	 *            If true, this color model supports alpha.
	 * @param isAlphaPremultiplied
	 *            If true, alpha is premultiplied.
	 * @param transparency
	 *            Specifies what alpha values can be represented by this color
	 *            model.
	 * @param transferType
	 *            Specifies the type of primitive array used to represent pixel
	 *            values, one of DataBuffer.TYPE_FLOAT or TYPE_DOUBLE.
	 * @throws IllegalArgumentException
	 *             If the transfer type is not DataBuffer.TYPE_FLOAT or
	 *             TYPE_DOUBLE.
	 *
	 * @see ColorSpace
	 * @see java.awt.Transparency
	 */
	public FloatDoubleColorModel(final ColorSpace colorSpace, final boolean hasAlpha,
			final boolean isAlphaPremultiplied, final int transparency, final int transferType) {
		super(colorSpace, bitsHelper(transferType, colorSpace, hasAlpha), hasAlpha, isAlphaPremultiplied, transparency,
				transferType);

		if (transferType != DataBuffer.TYPE_FLOAT && transferType != DataBuffer.TYPE_DOUBLE) {
			throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel0"));
		}

		this.colorSpace = colorSpace;
		colorSpaceType = colorSpace.getType();
		numComponents = numColorComponents = colorSpace.getNumComponents();
		if (hasAlpha) {
			++numComponents;
		}
		this.transparency = transparency;
		this.hasAlpha = hasAlpha;
		this.isAlphaPremultiplied = isAlphaPremultiplied;
	}

	/**
	 * Throws an <code>IllegalArgumentException</code>, since pixel values for
	 * this <code>ColorModel</code> are not conveniently representable as a
	 * single <code>int</code>.
	 */

	@Override
	public int getRed(final int pixel) {
		throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel1"));
	}

	/**
	 * Throws an <code>IllegalArgumentException</code>, since pixel values for
	 * this <code>ColorModel</code> are not conveniently representable as a
	 * single <code>int</code>.
	 */

	@Override
	public int getGreen(final int pixel) {
		throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel2"));
	}

	/**
	 * Throws an <code>IllegalArgumentException</code>, since pixel values for
	 * this <code>ColorModel</code> are not conveniently representable as a
	 * single <code>int</code>.
	 */

	@Override
	public int getBlue(final int pixel) {
		throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel3"));
	}

	/**
	 * Throws an <code>IllegalArgumentException</code>, since pixel values for
	 * this <code>ColorModel</code> are not conveniently representable as a
	 * single <code>int</code>.
	 */

	@Override
	public int getAlpha(final int pixel) {
		throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel4"));
	}

	/**
	 * Throws an <code>IllegalArgumentException</code>, since pixel values for
	 * this <code>ColorModel</code> are not conveniently representable as a
	 * single <code>int</code>.
	 */

	@Override
	public int getRGB(final int pixel) {
		throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel5"));
	}

	private int clamp(final float value) {
		// Ensure NaN maps to 0
		return value >= 0.0F ? value > 255.0F ? 255 : (int) value : 0;
	}

	private int clamp(final double value) {
		// Ensure NaN maps to 0
		return value >= 0.0 ? value > 255.0 ? 255 : (int) value : 0;
	}

	private int getSample(final Object inData, int sample) {
		final boolean needAlpha = hasAlpha && isAlphaPremultiplied;
		final int type = colorSpaceType;

		boolean is_sRGB = colorSpace.isCS_sRGB();

		if (type == ColorSpace.TYPE_GRAY) {
			sample = 0;
			is_sRGB = true;
		}

		if (is_sRGB) {
			if (transferType == DataBuffer.TYPE_FLOAT) {
				final float[] fdata = (float[]) inData;
				final float fsample = fdata[sample] * 255;
				if (needAlpha) {
					final float falp = fdata[numColorComponents];
					return clamp(fsample / falp);
				} else {
					return clamp(fsample);
				}
			} else {
				final double[] ddata = (double[]) inData;
				final double dsample = ddata[sample] * 255.0;
				if (needAlpha) {
					final double dalp = ddata[numColorComponents];
					return clamp(dsample / dalp);
				} else {
					return clamp(dsample);
				}
			}
		}

		// Not TYPE_GRAY or TYPE_RGB ColorSpace
		float[] norm;
		float[] rgb;
		if (transferType == DataBuffer.TYPE_FLOAT) {
			final float[] fdata = (float[]) inData;
			if (needAlpha) {
				final float falp = fdata[numColorComponents];
				norm = new float[numColorComponents];
				for (int i = 0; i < numColorComponents; i++) {
					norm[i] = fdata[i] / falp;
				}
				rgb = colorSpace.toRGB(norm);
				return (int) (rgb[sample] * falp * 255); // Why multiply by
															// falp?
			} else {
				rgb = colorSpace.toRGB(fdata);
				return (int) (rgb[sample] * 255);
			}
		} else {
			final double[] ddata = (double[]) inData;
			norm = new float[numColorComponents];
			if (needAlpha) {
				final double dalp = ddata[numColorComponents];
				for (int i = 0; i < numColorComponents; i++) {
					norm[i] = (float) (ddata[i] / dalp);
				}
				rgb = colorSpace.toRGB(norm);
				return (int) (rgb[sample] * dalp * 255);
			} else {
				for (int i = 0; i < numColorComponents; i++) {
					norm[i] = (float) ddata[i];
				}
				rgb = colorSpace.toRGB(norm);
				return (int) (rgb[sample] * 255);
			}
		}
	}

	/**
	 * Returns the red color component for the specified pixel, scaled from 0 to
	 * 255 in the default RGB ColorSpace, sRGB. A color conversion is done if
	 * necessary. The <code>pixel</code> value is specified by an array of data
	 * elements of type <code>transferType</code> passed in as an object
	 * reference. The returned value will be a non pre-multiplied value. If the
	 * alpha is premultiplied, this method divides it out before returning the
	 * value (if the alpha value is 0, the red value will be 0).
	 *
	 * @param inData
	 *            The pixel from which you want to get the red color component,
	 *            specified by an array of data elements of type
	 *            <code>transferType</code>.
	 *
	 * @return The red color component for the specified pixel, as an int.
	 *
	 * @throws ClassCastException
	 *             If <code>inData</code> is not a primitive array of type
	 *             <code>transferType</code>.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if <code>inData</code> is not large enough to hold a pixel
	 *             value for this <code>ColorModel</code>.
	 */

	@Override
	public int getRed(final Object inData) {
		return getSample(inData, 0);
	}

	/**
	 * Returns the green color component for the specified pixel, scaled from 0
	 * to 255 in the default RGB ColorSpace, sRGB. A color conversion is done if
	 * necessary. The <code>pixel</code> value is specified by an array of data
	 * elements of type <code>transferType</code> passed in as an object
	 * reference. The returned value will be a non pre-multiplied value. If the
	 * alpha is premultiplied, this method divides it out before returning the
	 * value (if the alpha value is 0, the green value will be 0).
	 *
	 * @param inData
	 *            The pixel from which you want to get the green color
	 *            component, specified by an array of data elements of type
	 *            <code>transferType</code>.
	 *
	 * @return The green color component for the specified pixel, as an int.
	 *
	 * @throws ClassCastException
	 *             If <code>inData</code> is not a primitive array of type
	 *             <code>transferType</code>.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if <code>inData</code> is not large enough to hold a pixel
	 *             value for this <code>ColorModel</code>.
	 */

	@Override
	public int getGreen(final Object inData) {
		return getSample(inData, 1);
	}

	/**
	 * Returns the blue color component for the specified pixel, scaled from 0
	 * to 255 in the default RGB ColorSpace, sRGB. A color conversion is done if
	 * necessary. The <code>pixel</code> value is specified by an array of data
	 * elements of type <code>transferType</code> passed in as an object
	 * reference. The returned value will be a non pre-multiplied value. If the
	 * alpha is premultiplied, this method divides it out before returning the
	 * value (if the alpha value is 0, the blue value will be 0).
	 *
	 * @param inData
	 *            The pixel from which you want to get the blue color component,
	 *            specified by an array of data elements of type
	 *            <code>transferType</code>.
	 *
	 * @return The blue color component for the specified pixel, as an int.
	 *
	 * @throws ClassCastException
	 *             If <code>inData</code> is not a primitive array of type
	 *             <code>transferType</code>.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if <code>inData</code> is not large enough to hold a pixel
	 *             value for this <code>ColorModel</code>.
	 */

	@Override
	public int getBlue(final Object inData) {
		return getSample(inData, 2);
	}

	/**
	 * Returns the alpha component for the specified pixel, scaled from 0 to
	 * 255. The pixel value is specified by an array of data elements of type
	 * <code>transferType</code> passed in as an object reference. If the
	 * <code>ColorModel</code> does not have alpha, 255 is returned.
	 *
	 * @param inData
	 *            The pixel from which you want to get the alpha component,
	 *            specified by an array of data elements of type
	 *            <code>transferType</code>.
	 *
	 * @return The alpha component for the specified pixel, as an int.
	 *
	 * @throws NullPointerException
	 *             if <code>inData</code> is <code>null</code> and the
	 *             <code>colorModel</code> has alpha.
	 * @throws ClassCastException
	 *             If <code>inData</code> is not a primitive array of type
	 *             <code>transferType</code> and the <code>ColorModel</code> has
	 *             alpha.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if <code>inData</code> is not large enough to hold a pixel
	 *             value for this <code>ColorModel</code> and the
	 *             <code>ColorModel</code> has alpha.
	 */

	@Override
	public int getAlpha(final Object inData) {
		if (hasAlpha == false) {
			return 255;
		}

		if (transferType == DataBuffer.TYPE_FLOAT) {
			final float[] fdata = (float[]) inData;
			return (int) (fdata[numColorComponents] * 255.0F);
		} else {
			final double[] ddata = (double[]) inData;
			return (int) (ddata[numColorComponents] * 255.0);
		}
	}

	/**
	 * Returns the color/alpha components for the specified pixel in the default
	 * RGB color model format. A color conversion is done if necessary. The
	 * pixel value is specified by an array of data elements of type
	 * <code>transferType</code> passed in as an object reference. The returned
	 * value is in a non pre-multiplied format. If the alpha is premultiplied,
	 * this method divides it out of the color components (if the alpha value is
	 * 0, the color values will be 0).
	 *
	 * @param inData
	 *            The pixel from which you want to get the color/alpha
	 *            components, specified by an array of data elements of type
	 *            <code>transferType</code>.
	 *
	 * @return The color/alpha components for the specified pixel, as an int.
	 *
	 * @throws ClassCastException
	 *             If <code>inData</code> is not a primitive array of type
	 *             <code>transferType</code>.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if <code>inData</code> is not large enough to hold a pixel
	 *             value for this <code>ColorModel</code>.
	 */

	@Override
	public int getRGB(final Object inData) {
		final boolean needAlpha = hasAlpha && isAlphaPremultiplied;
		int alpha = 255;
		int red, green, blue;

		if (colorSpace.isCS_sRGB()) {
			if (transferType == DataBuffer.TYPE_FLOAT) {
				final float[] fdata = (float[]) inData;
				final float fred = fdata[0];
				final float fgreen = fdata[1];
				final float fblue = fdata[2];
				float fscale = 255.0F;
				if (needAlpha) {
					final float falpha = fdata[3];
					fscale /= falpha;
					alpha = clamp(255.0F * falpha);
				}

				red = clamp(fred * fscale);
				green = clamp(fgreen * fscale);
				blue = clamp(fblue * fscale);
			} else {
				final double[] ddata = (double[]) inData;
				final double dred = ddata[0];
				final double dgreen = ddata[1];
				final double dblue = ddata[2];
				double dscale = 255.0;
				if (needAlpha) {
					final double dalpha = ddata[3];
					dscale /= dalpha;
					alpha = clamp(255.0 * dalpha);
				}

				red = clamp(dred * dscale);
				green = clamp(dgreen * dscale);
				blue = clamp(dblue * dscale);
			}
		} else if (colorSpaceType == ColorSpace.TYPE_GRAY) {
			if (transferType == DataBuffer.TYPE_FLOAT) {
				final float[] fdata = (float[]) inData;
				final float fgray = fdata[0];
				if (needAlpha) {
					final float falp = fdata[1];
					red = green = blue = clamp(fgray * 255.0F / falp);
					alpha = clamp(255.0F * falp);
				} else {
					red = green = blue = clamp(fgray * 255.0F);
				}
			} else {
				final double[] ddata = (double[]) inData;
				final double dgray = ddata[0];
				if (needAlpha) {
					final double dalp = ddata[1];
					red = green = blue = clamp(dgray * 255.0 / dalp);
					alpha = clamp(255.0 * dalp);
				} else {
					red = green = blue = clamp(dgray * 255.0);
				}
			}
		} else {
			// Not Gray or sRGB
			float[] norm;
			float[] rgb;
			if (transferType == DataBuffer.TYPE_FLOAT) {
				final float[] fdata = (float[]) inData;
				if (needAlpha) {
					final float falp = fdata[numColorComponents];
					final float invfalp = 1.0F / falp;
					norm = new float[numColorComponents];
					for (int i = 0; i < numColorComponents; i++) {
						norm[i] = fdata[i] * invfalp;
					}
					alpha = clamp(255.0F * falp);
				} else {
					norm = fdata;
				}
			} else {
				final double[] ddata = (double[]) inData;
				norm = new float[numColorComponents];
				if (needAlpha) {
					final double dalp = ddata[numColorComponents];
					final double invdalp = 1.0 / dalp;
					for (int i = 0; i < numColorComponents; i++) {
						norm[i] = (float) (ddata[i] * invdalp);
					}
					alpha = clamp(255.0 * dalp);
				} else {
					for (int i = 0; i < numColorComponents; i++) {
						norm[i] = (float) ddata[i];
					}
				}
			}

			// Perform color conversion
			rgb = colorSpace.toRGB(norm);

			red = clamp(rgb[0] * 255.0F);
			green = clamp(rgb[1] * 255.0F);
			blue = clamp(rgb[2] * 255.0F);
		}

		return alpha << 24 | red << 16 | green << 8 | blue;
	}

	/**
	 * Returns a data element array representation of a pixel in this
	 * <code>ColorModel</code>, given an integer pixel representation in the
	 * default RGB color model. This array can then be passed to the
	 * <code>setDataElements</code> method of a <code>WritableRaster</code>
	 * object. If the <code>pixel</code> parameter is null, a new array is
	 * allocated.
	 *
	 * @param rgb
	 *            An ARGB value packed into an int.
	 * @param pixel
	 *            The float or double array representation of the pixel.
	 *
	 * @throws ClassCastException
	 *             If <code>pixel</code> is not null and is not a primitive
	 *             array of type <code>transferType</code>.
	 *
	 * @throws ArrayIndexOutOfBoundsException
	 *             If <code>pixel</code> is not large enough to hold a pixel
	 *             value for this <code>ColorModel</code>.
	 */

	@Override
	public Object getDataElements(final int rgb, final Object pixel) {
		if (transferType == DataBuffer.TYPE_FLOAT) {
			float[] floatPixel;

			if (pixel == null) {
				floatPixel = new float[numComponents];
			} else {
				if (!(pixel instanceof float[])) {
					throw new ClassCastException(JaiI18N.getString("FloatDoubleColorModel7"));
				}
				floatPixel = (float[]) pixel;
				if (floatPixel.length < numComponents) {
					throw new ArrayIndexOutOfBoundsException(JaiI18N.getString("FloatDoubleColorModel8"));
				}
			}

			final float inv255 = 1.0F / 255.0F;
			if (colorSpace.isCS_sRGB()) {
				final int alp = rgb >> 24 & 0xff;
				final int red = rgb >> 16 & 0xff;
				final int grn = rgb >> 8 & 0xff;
				final int blu = rgb & 0xff;
				float norm = inv255;
				if (isAlphaPremultiplied) {
					norm *= alp;
				}
				floatPixel[0] = red * norm;
				floatPixel[1] = grn * norm;
				floatPixel[2] = blu * norm;
				if (hasAlpha) {
					floatPixel[3] = alp * inv255;
				}
			} else if (colorSpaceType == ColorSpace.TYPE_GRAY) {
				final float gray = (rgb >> 16 & 0xff) * (.299F * inv255) + (rgb >> 8 & 0xff) * (.587F * inv255)
						+ (rgb & 0xff) * (.114F * inv255);

				floatPixel[0] = gray;

				if (hasAlpha) {
					final int alpha = rgb >> 24 & 0xff;
					floatPixel[1] = alpha * inv255;
				}
			} else {
				// Need to convert the color
				float[] norm = new float[3];
				norm[0] = (rgb >> 16 & 0xff) * inv255;
				norm[1] = (rgb >> 8 & 0xff) * inv255;
				norm[2] = (rgb & 0xff) * inv255;

				norm = colorSpace.fromRGB(norm);
				for (int i = 0; i < numColorComponents; i++) {
					floatPixel[i] = norm[i];
				}
				if (hasAlpha) {
					final int alpha = rgb >> 24 & 0xff;
					floatPixel[numColorComponents] = alpha * inv255;
				}
			}

			return floatPixel;
		} else { // transferType == DataBuffer.TYPE_DOUBLE
			double[] doublePixel;

			if (pixel == null) {
				doublePixel = new double[numComponents];
			} else {
				if (!(pixel instanceof double[])) {
					throw new ClassCastException(JaiI18N.getString("FloatDoubleColorModel7"));
				}
				doublePixel = (double[]) pixel;
				if (doublePixel.length < numComponents) {
					throw new ArrayIndexOutOfBoundsException(JaiI18N.getString("FloatDoubleColorModel8"));
				}
			}

			final double inv255 = 1.0 / 255.0;
			if (colorSpace.isCS_sRGB()) {
				final int alp = rgb >> 24 & 0xff;
				final int red = rgb >> 16 & 0xff;
				final int grn = rgb >> 8 & 0xff;
				final int blu = rgb & 0xff;
				double norm = inv255;
				if (isAlphaPremultiplied) {
					norm *= alp;
				}
				doublePixel[0] = red * norm;
				doublePixel[1] = grn * norm;
				doublePixel[2] = blu * norm;
				if (hasAlpha) {
					doublePixel[3] = alp * inv255;
				}
			} else if (colorSpaceType == ColorSpace.TYPE_GRAY) {
				final double gray = (rgb >> 16 & 0xff) * (.299 * inv255) + (rgb >> 8 & 0xff) * (.587 * inv255)
						+ (rgb & 0xff) * (.114 * inv255);

				doublePixel[0] = gray;

				if (hasAlpha) {
					final int alpha = rgb >> 24 & 0xff;
					doublePixel[1] = alpha * inv255;
				}
			} else {
				final float inv255F = 1.0F / 255.0F;

				// Need to convert the color, need data in float form
				float[] norm = new float[3];
				norm[0] = (rgb >> 16 & 0xff) * inv255F;
				norm[1] = (rgb >> 8 & 0xff) * inv255F;
				norm[2] = (rgb & 0xff) * inv255F;

				norm = colorSpace.fromRGB(norm);
				for (int i = 0; i < numColorComponents; i++) {
					doublePixel[i] = norm[i];
				}
				if (hasAlpha) {
					final int alpha = rgb >> 24 & 0xff;
					doublePixel[numColorComponents] = alpha * inv255;
				}
			}

			return doublePixel;
		}
	}

	/**
	 * Throws an <code>IllegalArgumentException</code>, since pixel values for
	 * this <code>ColorModel</code> are not conveniently representable as a
	 * single <code>int</code>.
	 */

	@Override
	public int[] getComponents(final int pixel, final int[] components, final int offset) {
		throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel9"));
	}

	/**
	 * Throws an <code>IllegalArgumentException</code> since the pixel values
	 * cannot be placed into an <code>int</code> array.
	 */

	@Override
	public int[] getComponents(final Object pixel, final int[] components, final int offset) {
		throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel9"));
	}

	/**
	 * Throws an <code>IllegalArgumentException</code>, since pixel values for
	 * this <code>ColorModel</code> are not conveniently representable as a
	 * single <code>int</code>.
	 */

	@Override
	public int getDataElement(final int[] components, final int offset) {
		throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel9"));
	}

	/**
	 * Returns a data element array representation of a pixel in this
	 * <code>ColorModel</code>, given an array of unnormalized color/alpha
	 * components. This array can then be passed to the
	 * <code>setDataElements</code> method of a <code>WritableRaster</code>
	 * object.
	 *
	 * @param components
	 *            An array of unnormalized color/alpha components.
	 * @param offset
	 *            The integer offset into the <code>components</code> array.
	 * @param obj
	 *            The object in which to store the data element array
	 *            representation of the pixel. If <code>obj</code> variable is
	 *            null, a new array is allocated. If <code>obj</code> is not
	 *            null, it must be a primitive array of type
	 *            <code>transferType</code>. An
	 *            <code>ArrayIndexOutOfBoundsException</code> is thrown if
	 *            <code>obj</code> is not large enough to hold a pixel value for
	 *            this <code>ColorModel</code>.
	 *
	 * @return The data element array representation of a pixel in this
	 *         <code>ColorModel</code>.
	 *
	 * @throws IllegalArgumentException
	 *             If the components array is not large enough to hold all the
	 *             color and alpha components (starting at offset).
	 * @throws ClassCastException
	 *             If <code>obj</code> is not null and is not a primitive array
	 *             of type <code>transferType</code>.
	 * @throws ArrayIndexOutOfBoundsException
	 *             If <code>obj</code> is not large enough to hold a pixel value
	 *             for this <code>ColorModel</code>.
	 */

	@Override
	public Object getDataElements(final int[] components, final int offset, final Object obj) {
		if (components.length - offset < numComponents) {
			throw new IllegalArgumentException(numComponents + " " + JaiI18N.getString("FloatDoubleColorModel10"));
		}
		if (transferType == DataBuffer.TYPE_FLOAT) {
			float[] pixel;
			if (obj == null) {
				pixel = new float[components.length];
			} else {
				pixel = (float[]) obj;
			}
			for (int i = 0; i < numComponents; i++) {
				pixel[i] = components[offset + i];
			}

			return pixel;
		} else {
			double[] pixel;
			if (obj == null) {
				pixel = new double[components.length];
			} else {
				pixel = (double[]) obj;
			}
			for (int i = 0; i < numComponents; i++) {
				pixel[i] = components[offset + i];
			}

			return pixel;
		}
	}

	/**
	 * Forces the raster data to match the state specified in the
	 * <code>isAlphaPremultiplied</code> variable, assuming the data is
	 * currently correctly described by this <code>ColorModel</code>. It may
	 * multiply or divide the color raster data by alpha, or do nothing if the
	 * data is in the correct state. If the data needs to be coerced, this
	 * method also returns an instance of <code>FloatDoubleColorModel</code>
	 * with the <code>isAlphaPremultiplied</code> flag set appropriately.
	 *
	 * @throws IllegalArgumentException
	 *             if transfer type of <code>raster</code> is not the same as
	 *             that of this <code>FloatDoubleColorModel</code>.
	 */

	@Override
	public ColorModel coerceData(final WritableRaster raster, final boolean isAlphaPremultiplied) {
		if (hasAlpha == false || this.isAlphaPremultiplied == isAlphaPremultiplied) {
			// Nothing to do
			return this;
		}

		final int w = raster.getWidth();
		final int h = raster.getHeight();
		final int aIdx = raster.getNumBands() - 1;
		final int rminX = raster.getMinX();
		int rY = raster.getMinY();
		int rX;

		if (raster.getTransferType() != transferType) {
			throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel6"));
		}

		if (isAlphaPremultiplied) {
			switch (transferType) {
			case DataBuffer.TYPE_FLOAT: {
				float pixel[] = null;
				for (int y = 0; y < h; y++, rY++) {
					rX = rminX;
					for (int x = 0; x < w; x++, rX++) {
						pixel = (float[]) raster.getDataElements(rX, rY, pixel);
						final float fAlpha = pixel[aIdx];
						if (fAlpha != 0) {
							for (int c = 0; c < aIdx; c++) {
								pixel[c] *= fAlpha;
							}
							raster.setDataElements(rX, rY, pixel);
						}
					}
				}
			}
				break;
			case DataBuffer.TYPE_DOUBLE: {
				double pixel[] = null;
				for (int y = 0; y < h; y++, rY++) {
					rX = rminX;
					for (int x = 0; x < w; x++, rX++) {
						pixel = (double[]) raster.getDataElements(rX, rY, pixel);
						final double dAlpha = pixel[aIdx];
						if (dAlpha != 0) {
							for (int c = 0; c < aIdx; c++) {
								pixel[c] *= dAlpha;
							}
							raster.setDataElements(rX, rY, pixel);
						}
					}
				}
			}
				break;
			}
		} else {
			// We are premultiplied and want to divide it out
			switch (transferType) {
			case DataBuffer.TYPE_FLOAT: {
				for (int y = 0; y < h; y++, rY++) {
					rX = rminX;
					for (int x = 0; x < w; x++, rX++) {
						float pixel[] = null;
						pixel = (float[]) raster.getDataElements(rX, rY, pixel);
						final float fAlpha = pixel[aIdx];
						if (fAlpha != 0) {
							final float invFAlpha = 1.0F / fAlpha;
							for (int c = 0; c < aIdx; c++) {
								pixel[c] *= invFAlpha;
							}
						}
						raster.setDataElements(rX, rY, pixel);
					}
				}
			}
				break;
			case DataBuffer.TYPE_DOUBLE: {
				for (int y = 0; y < h; y++, rY++) {
					rX = rminX;
					for (int x = 0; x < w; x++, rX++) {
						double pixel[] = null;
						pixel = (double[]) raster.getDataElements(rX, rY, pixel);
						final double dAlpha = pixel[aIdx];
						if (dAlpha != 0) {
							final double invDAlpha = 1.0 / dAlpha;
							for (int c = 0; c < aIdx; c++) {
								pixel[c] *= invDAlpha;
							}
						}
						raster.setDataElements(rX, rY, pixel);
					}
				}
			}
				break;
			}
		}

		// Return a new color model
		return new FloatDoubleColorModel(colorSpace, hasAlpha, isAlphaPremultiplied, transparency, transferType);
	}

	/**
	 * Returns <code>true</code> if the supplied <code>Raster</code>'s
	 * <code>SampleModel</code> is compatible with this
	 * <code>FloatDoubleColorModel</code>.
	 *
	 * @param raster
	 *            a <code>Raster</code>to be checked for compatibility.
	 */

	@Override
	public boolean isCompatibleRaster(final Raster raster) {
		final SampleModel sm = raster.getSampleModel();
		return isCompatibleSampleModel(sm);
	}

	/**
	 * Creates a <code>WritableRaster</code> with the specified width and
	 * height, that has a data layout (<code>SampleModel</code>) compatible with
	 * this <code>ColorModel</code>. The returned <code>WritableRaster</code>'s
	 * <code>SampleModel</code> will be an instance of
	 * <code>ComponentSampleModel</code>.
	 *
	 * @param w
	 *            The width of the <code>WritableRaster</code> you want to
	 *            create.
	 * @param h
	 *            The height of the <code>WritableRaster</code> you want to
	 *            create.
	 *
	 * @return A <code>WritableRaster</code> that is compatible with this
	 *         <code>ColorModel</code>.
	 *
	 * @see WritableRaster
	 * @see SampleModel
	 */

	@Override
	public WritableRaster createCompatibleWritableRaster(final int w, final int h) {
		final SampleModel sm = createCompatibleSampleModel(w, h);
		return RasterFactory.createWritableRaster(sm, new Point(0, 0));
	}

	/**
	 * Creates a <code>SampleModel</code> with the specified width and height
	 * that has a data layout compatible with this <code>ColorModel</code>. The
	 * returned <code>SampleModel</code> will be an instance of
	 * <code>ComponentSampleModel</code>.
	 *
	 * @param w
	 *            The width of the <code>SampleModel</code> you want to create.
	 * @param h
	 *            The height of the <code>SampleModel</code> you want to create.
	 *
	 * @return A <code>SampleModel</code> that is compatible with this
	 *         <code>ColorModel</code>.
	 *
	 * @see SampleModel
	 * @see ComponentSampleModel
	 */

	@Override
	public SampleModel createCompatibleSampleModel(final int w, final int h) {
		final int[] bandOffsets = new int[numComponents];
		for (int i = 0; i < numComponents; i++) {
			bandOffsets[i] = i;
		}
		return new ComponentSampleModelJAI(transferType, w, h, numComponents, w * numComponents, bandOffsets);
	}

	/**
	 * Checks whether or not the specified <code>SampleModel</code> is
	 * compatible with this <code>ColorModel</code>. A <code>SampleModel</code>
	 * is compatible if it is an instance of <code>ComponentSampleModel</code>,
	 * has the sample number of bands as the total nomber of components
	 * (including alpha) in the <code>ColorSpace</code> used by this
	 * <code>ColorModel</code>, and has the same data type (float or double) as
	 * this <code>ColorModel</code>.
	 *
	 * @param sm
	 *            The <code>SampleModel</code> to test for compatibility.
	 *
	 * @return <code>true</code> if the <code>SampleModel</code> is compatible
	 *         with this <code>ColorModel</code>, <code>false</code> if it is
	 *         not.
	 *
	 * @see SampleModel
	 * @see ComponentSampleModel
	 */

	@Override
	public boolean isCompatibleSampleModel(final SampleModel sm) {
		if (sm instanceof ComponentSampleModel) {
			if (sm.getNumBands() != getNumComponents()) {
				return false;
			}
			if (sm.getDataType() != transferType) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}

	/** Returns a String containing the values of all valid fields. */

	@Override
	public String toString() {
		return "FloatDoubleColorModel: " + super.toString();
	}
}

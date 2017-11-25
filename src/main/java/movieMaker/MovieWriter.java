package movieMaker;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class to write out an AVI or Quicktime movie from a series of JPEG (jpg)
 * frames in a directory
 *
 * @author Barb Ericson ericson@cc.gatech.edu
 */
public class MovieWriter {
	// /////////////// fields ///////////////////////////

	/** the directory to read the frames from */
	private String framesDir = null;
	/** the number of frames per second */
	private int frameRate = 16;
	/** the name of the movie file */
	private String movieName = null;
	/** the name of the movie file */
	private String movieDir = null;
	/** the output url for the movie */
	private String outputURL = null;

	// //////////////// constructors //////////////////////

	/**
	 * No arg constructor
	 */
	public MovieWriter() {
		framesDir = FileChooser.pickADirectory();
		movieDir = framesDir;
		movieName = getMovieName();
		outputURL = getOutputURL();
	}

	/**
	 * Constructor that takes the directory that has the frames
	 *
	 * @param dirPath
	 *            the full path for the directory that has the movie frames
	 */
	public MovieWriter(final String dirPath) {
		framesDir = dirPath;
		movieName = getMovieName();
		movieDir = framesDir;
		outputURL = getOutputURL();

	}

	/**
	 * Constructor that takes the frame rate
	 *
	 * @param theFrameRate
	 *            the number of frames per second
	 */
	public MovieWriter(final int theFrameRate) {
		framesDir = FileChooser.pickADirectory();
		frameRate = theFrameRate;
		movieDir = framesDir;
		movieName = getMovieName();
		outputURL = getOutputURL();

	}

	/**
	 * Constructor that takes the frame rate and the directory that the frames
	 * are stored in
	 *
	 * @param theFrameRate
	 *            the number of frames per second
	 * @param theFramesDir
	 *            the directory where the frames are
	 */
	public MovieWriter(final int theFrameRate, final String theFramesDir) {
		framesDir = theFramesDir;
		frameRate = theFrameRate;
		movieDir = framesDir;
		movieName = getMovieName();
		outputURL = getOutputURL();

	}

	/**
	 * Constructor that takes the frame rate and the directory that the frames
	 * are stored in
	 *
	 * @param theFrameRate
	 *            the number of frames per second
	 * @param theFramesDir
	 *            the directory where the frames are
	 */
	public MovieWriter(final int theFrameRate, final String theFramesDir, final String theMovieName) {
		framesDir = theFramesDir;
		frameRate = theFrameRate;
		movieDir = framesDir;
		movieName = theMovieName;
		outputURL = getOutputURL();

	}

	/**
	 * Constructor that takes the frame rate and the directory that the frames
	 * are stored in
	 *
	 * @param theFrameRate
	 *            the number of frames per second
	 * @param theFramesDir
	 *            the directory where the frames are
	 */
	public MovieWriter(final int theFrameRate, final String theFramesDir, final String theMovieName,
			final String theMovieDir) {
		framesDir = theFramesDir;
		frameRate = theFrameRate;
		movieDir = theMovieDir;
		movieName = theMovieName;
		outputURL = getOutputURL();
	}

	/**
	 * Constructor that takes the directory with the frames the frame rate, and
	 * the output url (dir,name, and extendsion)
	 *
	 * @param theFramesDir
	 *            the directory that holds the frame
	 * @param theFrameRate
	 *            the number of frames per second
	 * @param theOutputURL
	 *            the complete path name for the output movie
	 */
	public MovieWriter(final String theFramesDir, final int theFrameRate, final String theOutputURL) {
		framesDir = theFramesDir;
		frameRate = theFrameRate;
		outputURL = theOutputURL;
		movieDir = theFramesDir;
	}

	// ///////////////// methods //////////////////////////

	/**
	 * Method to get the movie name from the directory where the frames are
	 * stored
	 *
	 * @return the name of the movie (like movie1)
	 */
	private String getMovieName() {
		final File dir = new File(framesDir);
		return dir.getName();
	}

	/**
	 * Method to create the output URL from the directory the frames are stored
	 * in.
	 *
	 * @return the URL for the output movie file
	 */
	private String getOutputURL() {
		File dir = null;
		URL myURL = null;
		if (framesDir != null) {
			try {
				dir = new File(movieDir + movieName);
				myURL = dir.toURI().toURL();
			} catch (final Exception ex) {
			}
		}
		return myURL.toString();
	}

	/**
	 * Method to get the list of jpeg frames
	 *
	 * @return a list of full path names for the frames of the movie
	 */
	public List<String> getFrameNames() {
		final File dir = new File(framesDir);
		final String[] filesArray = dir.list();
		final List<String> files = new ArrayList<>();
		long lenFirst = 0;
		for (final String fileName : filesArray) {
			// only continue if jpg picture
			if (fileName.indexOf(".jpg") >= 0) {
				final File f = new File(framesDir + fileName);
				// check for incomplete image
				if (lenFirst == 0 || f.length() > lenFirst / 2) {
					// image okay so far
					try {
						/*final BufferedImage i =*/ ImageIO.read(f);
						files.add(framesDir + fileName);
					} catch (final Exception ex) {
						// if problem reading don't add it
					}
				}
				if (lenFirst == 0) {
					lenFirst = f.length();
				}
			}
		}
		return files;
	}

	/**
	 * Method to write the movie frames in AVI format
	 */
	public void writeAVI() {
		final JpegImagesToMovie imageToMovie = new JpegImagesToMovie();
		final List<String> frameNames = getFrameNames();
		final Picture p = new Picture((String) frameNames.get(0));
		imageToMovie.doItAVI(p.getWidth(), p.getHeight(), frameRate, frameNames, outputURL + ".avi");
	}

	/**
	 * Method to write the movie frames as quicktime
	 */
	public boolean writeQuicktime() {
		final JpegImagesToMovie imageToMovie = new JpegImagesToMovie();
		final List<String> frameNames = getFrameNames();
		final Picture p = new Picture((String) frameNames.get(0));
		return imageToMovie.doItQuicktime(p.getWidth(), p.getHeight(), frameRate, frameNames, outputURL + ".mov");
	}

	public static void main(final String[] args) {
		final MovieWriter writer = new MovieWriter("c:/Temp/tr1/");
		writer.writeQuicktime();
		writer.writeAVI();
	}
}

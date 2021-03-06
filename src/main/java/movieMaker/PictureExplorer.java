package movieMaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

/**
 * Displays a picture and lets you explore the picture by displaying the x, y,
 * red, green, and blue values of the pixel at the cursor when you click a mouse
 * button or press and hold a mouse button while moving the cursor. It also lets
 * you zoom in or out. You can also type in a x and y value to see the color at
 * that location.
 *
 * Originally created for the Jython Environment for Students (JES). Modified to
 * work with DrJava by Barbara Ericson
 *
 * Copyright Georgia Institute of Technology 2004
 *
 * @author Keith McDermottt, gte047w@cc.gatech.edu
 * @author Barb Ericson ericson@cc.gatech.edu
 */
public class PictureExplorer implements MouseMotionListener, ActionListener, MouseListener {

	// current x and y index
	private int xIndex = 0;
	private int yIndex = 0;

	// Main gui variables
	private JFrame pictureFrame;
	private JScrollPane scrollPane;

	// information bar variables
	private JLabel xLabel;
	private JButton xPrevButton;
	private JButton yPrevButton;
	private JButton xNextButton;
	private JButton yNextButton;
	private JLabel yLabel;
	private JTextField xValue;
	private JTextField yValue;
	private JLabel rValue;
	private JLabel gValue;
	private JLabel bValue;
	private JLabel colorLabel;
	private JPanel colorPanel;

	// menu components
	private JMenuBar menuBar;
	private JMenu zoomMenu;
	private JMenuItem twentyFive;
	private JMenuItem fifty;
	private JMenuItem seventyFive;
	private JMenuItem hundred;
	private JMenuItem hundredFifty;
	private JMenuItem twoHundred;
	private JMenuItem fiveHundred;

	/** The picture being explored */
	private final DigitalPicture picture;

	/** The image icon used to display the picture */
	// Unused ?? private ImageIcon scrollImageIcon;

	/** The image display */
	private ImageDisplay imageDisplay;

	/** the zoom factor (amount to zoom) */
	private double zoomFactor;

	/**
	 * the number system to use, 0 means starting at 0, 1 means starting at 1
	 */
	private int numberBase = 0;

	/**
	 * Public constructor
	 *
	 * @param picture
	 *            the picture to explore
	 */
	public PictureExplorer(final DigitalPicture picture) {
		// set the fields
		this.picture = picture;
		zoomFactor = 1;

		// create the window and set things up
		createWindow();
	}

	/**
	 * Changes the number system to start at one
	 */
	public void changeToBaseOne() {
		numberBase = 1;
	}

	/**
	 * Set the title of the frame
	 *
	 * @param title
	 *            the title to use in the JFrame
	 */
	public void setTitle(final String title) {
		pictureFrame.setTitle(title);
	}

	/**
	 * Method to create and initialize the picture frame
	 */
	private void createAndInitPictureFrame() {
		pictureFrame = new JFrame(); // create the JFrame
		pictureFrame.setResizable(true); // allow the user to resize it
		pictureFrame.getContentPane().setLayout(new BorderLayout()); // use
																		// border
																		// layout
		pictureFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // when
																					// close
																					// stop
		pictureFrame.setTitle(picture.getTitle());
		final PictureExplorerFocusTraversalPolicy newPolicy = new PictureExplorerFocusTraversalPolicy();
		pictureFrame.setFocusTraversalPolicy(newPolicy);

	}

	/**
	 * Method to create the menu bar, menus, and menu items
	 */
	private void setUpMenuBar() {
		// create menu
		menuBar = new JMenuBar();
		zoomMenu = new JMenu("Zoom");
		twentyFive = new JMenuItem("25%");
		fifty = new JMenuItem("50%");
		seventyFive = new JMenuItem("75%");
		hundred = new JMenuItem("100%");
		hundred.setEnabled(false);
		hundredFifty = new JMenuItem("150%");
		twoHundred = new JMenuItem("200%");
		fiveHundred = new JMenuItem("500%");

		// add the action listeners
		twentyFive.addActionListener(this);
		fifty.addActionListener(this);
		seventyFive.addActionListener(this);
		hundred.addActionListener(this);
		hundredFifty.addActionListener(this);
		twoHundred.addActionListener(this);
		fiveHundred.addActionListener(this);

		// add the menu items to the menus
		zoomMenu.add(twentyFive);
		zoomMenu.add(fifty);
		zoomMenu.add(seventyFive);
		zoomMenu.add(hundred);
		zoomMenu.add(hundredFifty);
		zoomMenu.add(twoHundred);
		zoomMenu.add(fiveHundred);
		menuBar.add(zoomMenu);

		// set the menu bar to this menu
		pictureFrame.setJMenuBar(menuBar);
	}

	/**
	 * Create and initialize the scrolling image
	 */
	private void createAndInitScrollingImage() {
		scrollPane = new JScrollPane();

		final BufferedImage bimg = picture.getBufferedImage();
		imageDisplay = new ImageDisplay(bimg);
		imageDisplay.addMouseMotionListener(this);
		imageDisplay.addMouseListener(this);
		imageDisplay.setToolTipText("Click a mouse button on a pixel to see the pixel information");
		scrollPane.setViewportView(imageDisplay);
		pictureFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Creates the JFrame and sets everything up
	 */
	private void createWindow() {
		// create the picture frame and initialize it
		createAndInitPictureFrame();

		// set up the menu bar
		setUpMenuBar();

		// create the information panel
		createInfoPanel();

		// creates the scrollpane for the picture
		createAndInitScrollingImage();

		// show the picture in the frame at the size it needs to be
		pictureFrame.pack();
		pictureFrame.setVisible(true);
	}

	/**
	 * Method to set up the next and previous buttons for the pixel location
	 * information
	 */
	private void setUpNextAndPreviousButtons() {
		// create the image icons for the buttons
		final Icon prevIcon = new ImageIcon(SoundExplorer.class.getResource("leftArrow.gif"), "previous index");
		final Icon nextIcon = new ImageIcon(SoundExplorer.class.getResource("rightArrow.gif"), "next index");
		// create the arrow buttons
		xPrevButton = new JButton(prevIcon);
		xNextButton = new JButton(nextIcon);
		yPrevButton = new JButton(prevIcon);
		yNextButton = new JButton(nextIcon);

		// set the tool tip text
		xNextButton.setToolTipText("Click to go to the next x value");
		xPrevButton.setToolTipText("Click to go to the previous x value");
		yNextButton.setToolTipText("Click to go to the next y value");
		yPrevButton.setToolTipText("Click to go to the previous y value");

		// set the sizes of the buttons
		final int prevWidth = prevIcon.getIconWidth() + 2;
		final int nextWidth = nextIcon.getIconWidth() + 2;
		final int prevHeight = prevIcon.getIconHeight() + 2;
		final int nextHeight = nextIcon.getIconHeight() + 2;
		final Dimension prevDimension = new Dimension(prevWidth, prevHeight);
		final Dimension nextDimension = new Dimension(nextWidth, nextHeight);
		xPrevButton.setPreferredSize(prevDimension);
		yPrevButton.setPreferredSize(prevDimension);
		xNextButton.setPreferredSize(nextDimension);
		yNextButton.setPreferredSize(nextDimension);

		// handle previous x button press
		xPrevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent evt) {
				xIndex--;
				if (xIndex < 0) {
					xIndex = 0;
				}
				displayPixelInformation(xIndex, yIndex);
			}
		});

		// handle previous y button press
		yPrevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent evt) {
				yIndex--;
				if (yIndex < 0) {
					yIndex = 0;
				}
				displayPixelInformation(xIndex, yIndex);
			}
		});

		// handle next x button press
		xNextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent evt) {
				xIndex++;
				if (xIndex >= picture.getWidth()) {
					xIndex = picture.getWidth() - 1;
				}
				displayPixelInformation(xIndex, yIndex);
			}
		});

		// handle next y button press
		yNextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent evt) {
				yIndex++;
				if (yIndex >= picture.getHeight()) {
					yIndex = picture.getHeight() - 1;
				}
				displayPixelInformation(xIndex, yIndex);
			}
		});
	}

	/**
	 * Create the pixel location panel
	 *
	 * @param labelFont
	 *            the font for the labels
	 * @return the location panel
	 */
	public JPanel createLocationPanel(final Font labelFont) {

		// create a location panel
		final JPanel locationPanel = new JPanel();
		locationPanel.setLayout(new FlowLayout());
		final Box hBox = Box.createHorizontalBox();

		// create the labels
		xLabel = new JLabel("X:");
		yLabel = new JLabel("Y:");

		// create the text fields
		xValue = new JTextField(Integer.toString(xIndex + numberBase), 6);
		xValue.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				displayPixelInformation(xValue.getText(), yValue.getText());
			}
		});
		yValue = new JTextField(Integer.toString(yIndex + numberBase), 6);
		yValue.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				displayPixelInformation(xValue.getText(), yValue.getText());
			}
		});

		// set up the next and previous buttons
		setUpNextAndPreviousButtons();

		// set up the font for the labels
		xLabel.setFont(labelFont);
		yLabel.setFont(labelFont);
		xValue.setFont(labelFont);
		yValue.setFont(labelFont);

		// add the items to the vertical box and the box to the panel
		hBox.add(Box.createHorizontalGlue());
		hBox.add(xLabel);
		hBox.add(xPrevButton);
		hBox.add(xValue);
		hBox.add(xNextButton);
		hBox.add(Box.createHorizontalStrut(10));
		hBox.add(yLabel);
		hBox.add(yPrevButton);
		hBox.add(yValue);
		hBox.add(yNextButton);
		locationPanel.add(hBox);
		hBox.add(Box.createHorizontalGlue());

		return locationPanel;
	}

	/**
	 * Create the color information panel
	 *
	 * @param labelFont
	 *            the font to use for labels
	 * @return the color information panel
	 */
	private JPanel createColorInfoPanel(final Font labelFont) {
		// create a color info panel
		final JPanel colorInfoPanel = new JPanel();
		colorInfoPanel.setLayout(new FlowLayout());

		// get the pixel at the x and y
		final Pixel pixel = new Pixel(picture, xIndex, yIndex);

		// create the labels
		rValue = new JLabel("R: " + pixel.getRed());
		gValue = new JLabel("G: " + pixel.getGreen());
		bValue = new JLabel("B: " + pixel.getBlue());

		// create the sample color panel and label
		colorLabel = new JLabel("Color at location: ");
		colorPanel = new JPanel();
		colorPanel.setBorder(new LineBorder(Color.black, 1));

		// set the color sample to the pixel color
		colorPanel.setBackground(pixel.getColor());

		// set the font
		rValue.setFont(labelFont);
		gValue.setFont(labelFont);
		bValue.setFont(labelFont);
		colorLabel.setFont(labelFont);
		colorPanel.setPreferredSize(new Dimension(25, 25));

		// add items to the color information panel
		colorInfoPanel.add(rValue);
		colorInfoPanel.add(gValue);
		colorInfoPanel.add(bValue);
		colorInfoPanel.add(colorLabel);
		colorInfoPanel.add(colorPanel);

		return colorInfoPanel;
	}

	/**
	 * Creates the North JPanel with all the pixel location and color
	 * information
	 */
	private void createInfoPanel() {
		// create the info panel and set the layout
		final JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());

		// create the font
		final Font largerFont = new Font(infoPanel.getFont().getName(), infoPanel.getFont().getStyle(), 14);

		// create the pixel location panel
		final JPanel locationPanel = createLocationPanel(largerFont);

		// create the color informaiton panel
		final JPanel colorInfoPanel = createColorInfoPanel(largerFont);

		// add the panels to the info panel
		infoPanel.add(BorderLayout.NORTH, locationPanel);
		infoPanel.add(BorderLayout.SOUTH, colorInfoPanel);

		// add the info panel
		pictureFrame.getContentPane().add(BorderLayout.NORTH, infoPanel);
	}

	/**
	 * Method to check that the current position is in the viewing area and if
	 * not scroll to center the current position if possible
	 */
	public void checkScroll() {
		// get the x and y position in pixels
		final int xPos = (int) (xIndex * zoomFactor);
		final int yPos = (int) (yIndex * zoomFactor);

		// only do this if the image is larger than normal
		if (zoomFactor > 1) {

			// get the rectangle that defines the current view
			final JViewport viewport = scrollPane.getViewport();
			final Rectangle rect = viewport.getViewRect();
			// Unused ?? final int rectMinX = (int) rect.getX();
			final int rectWidth = (int) rect.getWidth();
			// Unused ?? final int rectMaxX = rectMinX + rectWidth - 1;
			// Unused ?? final int rectMinY = (int) rect.getY();
			final int rectHeight = (int) rect.getHeight();
			// Unused ?? final int rectMaxY = rectMinY + rectHeight - 1;

			// get the maximum possible x and y index
			final int maxIndexX = (int) (picture.getWidth() * zoomFactor) - rectWidth - 1;
			final int maxIndexY = (int) (picture.getHeight() * zoomFactor) - rectHeight - 1;

			// calculate how to position the current position in the middle of
			// the viewing
			// area
			int viewX = xPos - rectWidth / 2;
			int viewY = yPos - rectHeight / 2;

			// reposition the viewX and viewY if outside allowed values
			if (viewX < 0) {
				viewX = 0;
			} else if (viewX > maxIndexX) {
				viewX = maxIndexX;
			}
			if (viewY < 0) {
				viewY = 0;
			} else if (viewY > maxIndexY) {
				viewY = maxIndexY;
			}

			// move the viewport upper left point
			viewport.scrollRectToVisible(new Rectangle(viewX, viewY, rectWidth, rectHeight));
		}
	}

	/**
	 * Zooms in the on picture by scaling the image. It is extremely memory
	 * intensive.
	 *
	 * @param factor
	 *            the amount to zoom by
	 */
	public void zoom(final double factor) {
		// save the current zoom factor
		zoomFactor = factor;

		// calculate the new width and height and get an image that size
		final int width = (int) (picture.getWidth() * zoomFactor);
		final int height = (int) (picture.getHeight() * zoomFactor);
		final BufferedImage bimg = picture.getBufferedImage();

		// set the scroll image icon to the new image
		imageDisplay.setImage(bimg.getScaledInstance(width, height, Image.SCALE_DEFAULT));
		imageDisplay.setCurrentX((int) (xIndex * zoomFactor));
		imageDisplay.setCurrentY((int) (yIndex * zoomFactor));
		imageDisplay.revalidate();
		checkScroll(); // check if need to reposition scroll
	}

	/**
	 * Repaints the image on the scrollpane.
	 */
	public void repaint() {
		pictureFrame.repaint();
	}

	// ****************************************//
	// Event Listeners //
	// ****************************************//

	/**
	 * Called when the mouse is dragged (button held down and moved)
	 *
	 * @param e
	 *            the mouse event
	 */
	@Override
	public void mouseDragged(final MouseEvent e) {
		displayPixelInformation(e);
	}

	/**
	 * Method to check if the given x and y are in the picture
	 *
	 * @param x
	 *            the horiztonal value
	 * @param y
	 *            the vertical value
	 * @return true if the x and y are in the picture and false otherwise
	 */
	private boolean isLocationInPicture(final int x, final int y) {
		boolean result = false; // the default is false
		if (x >= 0 && x < picture.getWidth() && y >= 0 && y < picture.getHeight()) {
			result = true;
		}

		return result;
	}

	/**
	 * Method to display the pixel information from the passed x and y but also
	 * converts x and y from strings
	 *
	 * @param xString
	 *            the x value as a string from the user
	 * @param yString
	 *            the y value as a string from the user
	 */
	public void displayPixelInformation(final String xString, final String yString) {
		int x = -1;
		int y = -1;
		try {
			x = Integer.parseInt(xString);
			x = x - numberBase;
			y = Integer.parseInt(yString);
			y = y - numberBase;
		} catch (final Exception ex) {
		}

		if (x >= 0 && y >= 0) {
			displayPixelInformation(x, y);
		}
	}

	/**
	 * Method to display pixel information for the passed x and y
	 *
	 * @param pictureX
	 *            the x value in the picture
	 * @param pictureY
	 *            the y value in the picture
	 */
	private void displayPixelInformation(final int pictureX, final int pictureY) {
		// check that this x and y is in range
		if (isLocationInPicture(pictureX, pictureY)) {
			// save the current x and y index
			xIndex = pictureX;
			yIndex = pictureY;

			// get the pixel at the x and y
			final Pixel pixel = new Pixel(picture, xIndex, yIndex);

			// set the values based on the pixel
			xValue.setText(Integer.toString(xIndex + numberBase));
			yValue.setText(Integer.toString(yIndex + numberBase));
			rValue.setText("R: " + pixel.getRed());
			gValue.setText("G: " + pixel.getGreen());
			bValue.setText("B: " + pixel.getBlue());
			colorPanel.setBackground(new Color(pixel.getRed(), pixel.getGreen(), pixel.getBlue()));

		} else {
			clearInformation();
		}

		// notify the image display of the current x and y
		imageDisplay.setCurrentX((int) (xIndex * zoomFactor));
		imageDisplay.setCurrentY((int) (yIndex * zoomFactor));
	}

	/**
	 * Method to display pixel information based on a mouse event
	 *
	 * @param e
	 *            a mouse event
	 */
	private void displayPixelInformation(final MouseEvent e) {

		// get the cursor x and y
		final int cursorX = e.getX();
		final int cursorY = e.getY();

		// get the x and y in the original (not scaled image)
		final int pictureX = (int) (cursorX / zoomFactor + numberBase);
		final int pictureY = (int) (cursorY / zoomFactor + numberBase);

		// display the information for this x and y
		displayPixelInformation(pictureX, pictureY);

	}

	/**
	 * Method to clear the labels and current color and reset the current index
	 * to -1
	 */
	private void clearInformation() {
		xValue.setText("N/A");
		yValue.setText("N/A");
		rValue.setText("R: N/A");
		gValue.setText("G: N/A");
		bValue.setText("B: N/A");
		colorPanel.setBackground(Color.black);
		xIndex = -1;
		yIndex = -1;
	}

	/**
	 * Method called when the mouse is moved with no buttons down
	 *
	 * @param e
	 *            the mouse event
	 */
	@Override
	public void mouseMoved(final MouseEvent e) {
	}

	/**
	 * Method called when the mouse is clicked
	 *
	 * @param e
	 *            the mouse event
	 */
	@Override
	public void mouseClicked(final MouseEvent e) {
		displayPixelInformation(e);
	}

	/**
	 * Method called when the mouse button is pushed down
	 *
	 * @param e
	 *            the mouse event
	 */
	@Override
	public void mousePressed(final MouseEvent e) {
		displayPixelInformation(e);
	}

	/**
	 * Method called when the mouse button is released
	 *
	 * @param e
	 *            the mouse event
	 */
	@Override
	public void mouseReleased(final MouseEvent e) {
	}

	/**
	 * Method called when the component is entered (mouse moves over it)
	 *
	 * @param e
	 *            the mouse event
	 */
	@Override
	public void mouseEntered(final MouseEvent e) {
	}

	/**
	 * Method called when the mouse moves over the component
	 *
	 * @param e
	 *            the mouse event
	 */
	@Override
	public void mouseExited(final MouseEvent e) {
	}

	/**
	 * Method to enable all menu commands
	 */
	private void enableZoomItems() {
		twentyFive.setEnabled(true);
		fifty.setEnabled(true);
		seventyFive.setEnabled(true);
		hundred.setEnabled(true);
		hundredFifty.setEnabled(true);
		twoHundred.setEnabled(true);
		fiveHundred.setEnabled(true);
	}

	/**
	 * Controls the zoom menu bar
	 *
	 * @param a
	 *            the ActionEvent
	 */
	@Override
	public void actionPerformed(final ActionEvent a) {

		if (a.getActionCommand().equals("Update")) {
			repaint();
		}

		if (a.getActionCommand().equals("25%")) {
			zoom(.25);
			enableZoomItems();
			twentyFive.setEnabled(false);
		}

		if (a.getActionCommand().equals("50%")) {
			zoom(.50);
			enableZoomItems();
			fifty.setEnabled(false);
		}

		if (a.getActionCommand().equals("75%")) {
			zoom(.75);
			enableZoomItems();
			seventyFive.setEnabled(false);
		}

		if (a.getActionCommand().equals("100%")) {
			zoom(1.0);
			enableZoomItems();
			hundred.setEnabled(false);
		}

		if (a.getActionCommand().equals("150%")) {
			zoom(1.5);
			enableZoomItems();
			hundredFifty.setEnabled(false);
		}

		if (a.getActionCommand().equals("200%")) {
			zoom(2.0);
			enableZoomItems();
			twoHundred.setEnabled(false);
		}

		if (a.getActionCommand().equals("500%")) {
			zoom(5.0);
			enableZoomItems();
			fiveHundred.setEnabled(false);
		}
	}

	/**
	 * Test Main. It will ask you to pick a file and then show it
	 */
	public static void main(final String args[]) {
		final Picture p = new Picture(FileChooser.pickAFile());
		/* Unused ?? final PictureExplorer test = */ new PictureExplorer(p);

	}

	/**
	 * Class for establishing the focus for the textfields
	 */
	private class PictureExplorerFocusTraversalPolicy extends FocusTraversalPolicy {

		/**
		 * Method to get the next component for focus
		 */

		@Override
		public Component getComponentAfter(final Container focusCycleRoot, final Component aComponent) {
			if (aComponent.equals(xValue)) {
				return yValue;
			} else {
				return xValue;
			}
		}

		/**
		 * Method to get the previous component for focus
		 */

		@Override
		public Component getComponentBefore(final Container focusCycleRoot, final Component aComponent) {
			if (aComponent.equals(xValue)) {
				return yValue;
			} else {
				return xValue;
			}
		}

		@Override
		public Component getDefaultComponent(final Container focusCycleRoot) {
			return xValue;
		}

		@Override
		public Component getLastComponent(final Container focusCycleRoot) {
			return yValue;
		}

		@Override
		public Component getFirstComponent(final Container focusCycleRoot) {
			return xValue;
		}
	}

}
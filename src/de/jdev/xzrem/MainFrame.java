/*
 * This file is part of the XZrem project.
 *
 * Copyright (C) 2011 Stefan Wendler <sw@kaltpost.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.jdev.xzrem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeTimeoutException;

/**
 * @brief Main frame listening to key events, sending commands to remote XBee
 */
public class MainFrame extends JFrame implements KeyListener {

	private static final long serialVersionUID = 1L;

	/**
     * Port to which coordinator is connected to 
     */
	public static String 	port 		= null;
	
	/**
 	 * Speed at which coordinator operates in bauds
 	 */
	public static int    	speed		= 9600;

	/**
 	 * String representing the remote XBees 64bit address
 	 */
	public static String 	addr		= null;

 	/**
	 * Operate the motors in exclusive mode
	 * If set only one motor at a time is operated to save power
	 */
	public static boolean 	mExclMode 	= true;

	/**
  	 * Main text area showing what was sent to remote XBee and other status messages
  	 */
	JTextArea displayArea;

	/**
 	 * XBeeDo instance
 	 */
	private XBeeDo xdo;

	/**
 	 * Internal representation of remote XBees 64bit address
 	 */
	private XBeeAddress64 addr64;
	
	/**
 	 * Motor OFF
 	 */
	private static int M_OFF	= 0;

	/**
 	 * Motor ClockWise 
 	 */
	private static int M_CW		= 1;

	/**
 	 * Motor CounterClockWise
 	 */
	private static int M_CCW	= 2;
	
	/**
 	 * State of moter 1 (M_OFF|M_CW|M_CCW)
 	 */
	private int m1 = 0;

	/**
 	 * State of moter 2 (M_OFF|M_CW|M_CCW)
 	 */
	private int m2 = 0;

	/**
 	 * State of moter 3 (M_OFF|M_CW|M_CCW)
 	 */
	private int m3 = 0;

	/**
	 * Character to send for new-line on this platform
	 */
	static final String newline = System.getProperty("line.separator");

	/**
 	 * Constructor for the main frame
 	 *
 	 * @param	name	Window title to display
 	 */
	public MainFrame(String name) {
		super(name);

		// addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40, 0x61, 0x30, 0x09);
		addr64 = new XBeeAddress64(addr);
		xdo = new XBeeDo(port, 9600);
	}

	/**
	 * Create and show the GUI
	 */
	private static void createAndShowGUI() {

		MainFrame frame = new MainFrame("xzrem");

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addComponentsToPane();
		frame.pack();
		frame.setVisible(true);
	}

	/**
 	 * Add the needed GUI components to the pane
 	 */
	private void addComponentsToPane() {

		displayArea = new JTextArea();
		displayArea.setEditable(false);
		displayArea.addKeyListener(this);

		JScrollPane scrollPane = new JScrollPane(displayArea);
		scrollPane.setPreferredSize(new Dimension(640, 480));
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		displayArea.setBackground(Color.BLACK);
		displayArea.setForeground(Color.GREEN);

		msg("* Press CTRL+C to exit *");
	}

	/**
 	 * Write new message to the text area
 	 *
 	 * @param	m	the message string to write
 	 */
	public void msg(String m) {
		displayArea.append(m + newline);
		displayArea.setCaretPosition(displayArea.getDocument().getLength());
	}

	/**
 	 * Callback handler for key typed event - NOT USED
 	 */
	public void keyTyped(KeyEvent e) {
		// ignore
	}

	/**
 	 * Callback handler for key released event - NOT USED
 	 */
	public void keyReleased(KeyEvent e) {
		// ignore
	}

	/**
 	 * Callback handler for key pressed event
 	 */
	public void keyPressed(KeyEvent e) {
		try {
			if (e.getKeyCode() == 116 && !xdo.isConnected()) {
				msg("Connecting ...");
				xdo.open();
				mAllOff();
				msg("Connected to Coordinator!");				
			} else if (e.getKeyCode() == 67 && e.getModifiersEx() == 128) {
				try {
					mAllOff();
					xdo.close();
				} catch (Exception x) {
					// ignore
				}
				System.exit(0);
			} else if (e.getKeyCode() == 87) {
				m1Ctrl(M_CW);
				msg("-> forward");
			} else if (m1 != 2 && e.getKeyCode() == 83) {
				m1Ctrl(M_CCW);
				msg("-> backward");
			} else if (m2 != 1 && (e.getKeyCode() == 65 || e.getKeyCode() == 37)) {
				m2Ctrl(M_CW);
				msg("-> left");
			} else if (m2 != 2 && e.getKeyCode() == 68 || e.getKeyCode() == 39) {
				m2Ctrl(M_CCW);
				msg("-> right");
			} else if (m3 != 1 && e.getKeyCode() == 38) {
				m3Ctrl(M_CW);
				msg("-> up");
			} else if (m3 != 2 && e.getKeyCode() == 40) {
				m3Ctrl(M_CCW);
				msg("-> down");
			} else if (e.getKeyCode() == 32) {
				mAllOff();
				msg("-> OFF");
			}
		} catch (Exception x) {
			if(xdo.isConnected()) {
				if(x.getMessage() != null) {
					msg("ERROR: unable to send message to remote (" + x.getMessage() + ")");
				}
				else {
					msg("ERROR: unable to send message to remote");
				}
			}
			else {
				msg("ERROR: not connected");
			}				
		}
	}

	/**
	 * Set operation mode for motor 2
	 *
	 * @param	mode	one of M_OFF, M_CW or M_CCW
	 */
	private void m1Ctrl(int mode) throws XBeeTimeoutException, XBeeException {
	
		if(mode == m1) {
			return;
		}
		
		if(mExclMode) {
			mExclMode = false;
			try {
				mAllOff();
			}
			catch(Exception e) {
				// ignore
			}
			mExclMode = true;
		}
		
		m1 = mode;
		
		if(m1 == M_CW) {
			xdo.setDo(addr64, "D3", true);
			xdo.setDo(addr64, "D2", false);
		}
		else if(m1 == M_CCW) {
			xdo.setDo(addr64, "D2", true);
			xdo.setDo(addr64, "D3", false);
		}
		else {
			xdo.setDo(addr64, "D2", true);
			xdo.setDo(addr64, "D3", true);
		}
	}

	/**
	 * Set operation mode for motor 2
	 *
	 * @param	mode	one of M_OFF, M_CW or M_CCW
	 */
	private void m2Ctrl(int mode) throws XBeeTimeoutException, XBeeException {
	
		if(mode == m2) {
			return;
		}

		if(mExclMode) {
			mExclMode = false;
			try {
				mAllOff();
			}
			catch(Exception e) {
				// ignore
			}
			mExclMode = true;
		}
	
		m2 = mode;
				
		if(m2 == M_CW) {
			xdo.setDo(addr64, "D5", true);
			xdo.setDo(addr64, "D4", false);
		}
		else if(m2 == M_CCW) {
			xdo.setDo(addr64, "D4", true);
			xdo.setDo(addr64, "D5", false);
		}
		else {
			xdo.setDo(addr64, "D4", true);
			xdo.setDo(addr64, "D5", true);
		}
	}

	/**
	 * Set operation mode for motor 3
	 *
	 * @param	mode	one of M_OFF, M_CW or M_CCW
	 */
	private void m3Ctrl(int mode) throws XBeeTimeoutException, XBeeException {
		if(mode == m3) {
			return;
		}

		if(mExclMode) {
			mExclMode = false;
			try {
				mAllOff();
			}
			catch(Exception e) {
				// ignore
			}
			mExclMode = true;
		}

		m3 = mode;
		
		if(m3 == M_CW) {
			xdo.setDo(addr64, "D1", true);
			xdo.setDo(addr64, "D0", false);
		}
		else if(m3 == M_CCW) {
			xdo.setDo(addr64, "D0", true);
			xdo.setDo(addr64, "D1", false);
		}
		else {
			xdo.setDo(addr64, "D0", true);
			xdo.setDo(addr64, "D1", true);
		}
	}
	
	/**
 	 * Turn all motors OFF
 	 */
	private void mAllOff() throws XBeeTimeoutException, XBeeException {
		m1Ctrl(M_OFF);
		m2Ctrl(M_OFF);
		m3Ctrl(M_OFF);	
	}

	/**
 	 * Main method. Takes the following arguments:
 	 * <br/>
 	 * <br/>serial port	: mandatory, port to which coordinator is connected to
 	 * <br/>speed	  	: mandatory, speed in bauds at which coordinator operates
 	 * <br/>address		: mandatory, 64bit address of remote in the form aa:bb:cc:dd:ee:ff:gg:hh
 	 * <br/>motor mode	: optional, give "excel" to operate each motor eclusively
 	 *
 	 * @param	args[]	commandline arguments
 	 */
	public static void main(String[] args) {

		if (args.length < 3) {
			System.err.println("Specify all mandatory arguments: <serial port> <speed> <address> [excl]");
			System.err.println("- give <address> in the form: aa:bb:cc:dd:ee:ff:gg:hh");
			System.err.println("- if optional parameter [excl] is given, only one motor is driven at time");
			System.exit(1);
		}

		port  = args[0];
		speed = Integer.parseInt(args[1]);
		addr  = args[2].replace(":", " ");
		
		if(args.length > 3 && args[3].equals("excl")) {
			mExclMode = true;
		}
		else {
			mExclMode = false;
		}
		
		System.out.println("Using " + port + "@" + speed + ", sending to " + 
						   addr + " mExclMode is " + mExclMode);

		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			// UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}

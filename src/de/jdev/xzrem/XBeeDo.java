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

import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.RemoteAtResponse;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeTimeoutException;

/**
 * @brief Set digital outputs of remote XBee to High/Low.
 */
public class XBeeDo {

	/**
 	 * XBee instance to use for sending
 	 */
	private XBee xbee;

	/**
	 * Serial port the XBee coordinator is connected to 
	 */
	private String port;
	
	/**
     * Speed the coordinator is connected with (in bauds)
     */
	private int speed; 
	
	/**
 	 * Flag indicating if connectino to coordinator is established
 	 */
	private boolean connected = false;

	/**
 	 * New XBeeDo instance using coordinator at given port and speed.
 	 *
 	 * @param	port	the port to which the coordinator is connected to
 	 * @param	speed	the speed in baus at wich the coordinator is connected
 	 */
	public XBeeDo(String port, int speed) {
		super();
		this.port = port;
		this.speed = speed;
	}

	/**
 	 * Open serial connection to coordinator.
 	 */
	public void open() throws XBeeException {
		xbee = new XBee();
		xbee.open(port, speed);		
		connected = true;
	}
	
	/**
 	 * Close serial connection to coordinator.
 	 */
	public void close() {
		xbee.close();
		connected = false;
	}

	/**
 	 * Set digital output of remote XBee to HIGH/LOW.
 	 *
 	 * @param addr64	64 bit address of remote XBee
 	 * @param io		name of IO (e.g. "D0", "D1", ...)
 	 * @param high		true to set output to high, false for low
 	 * @return			true on success, false otherwise
 	 */
	public boolean setDo(XBeeAddress64 addr64, String io, boolean high) 
			throws XBeeTimeoutException, XBeeException {
		
		RemoteAtRequest request 	= new RemoteAtRequest(addr64, io, 
											(high ? new int[] {5} : new int[] {4}) );			
		RemoteAtResponse response 	= (RemoteAtResponse) xbee.sendSynchronous(request, 10000);
		
		if (response.isOk()) {
			System.out.println("Remote XBee turn " + (high ? "ON  " : "OFF ") + io);	
		} else {
			return false;
		}
		
		return true;
	}

	/**
     * Check if connection to coordinator is established.
     *
     * @return	true if connection is established, false otherwise
     */
	public boolean isConnected() {
		return connected;
	}
}

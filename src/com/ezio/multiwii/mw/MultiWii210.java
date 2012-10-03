/*  MultiWii EZ-GUI
    Copyright (C) <2012>  Bartosz Szczygiel (eziosoft)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ezio.multiwii.mw;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ezio.multiwii.Waypoint;

import android.util.Log;

public class MultiWii210 extends MultirotorData {

	public MultiWii210(BT b) {
		bt = b;

		// changes from 2.0//
		PIDITEMS = 10;
		CHECKBOXITEMS = 14;
		byteP = new int[PIDITEMS];
		byteI = new int[PIDITEMS];
		byteD = new int[PIDITEMS];

		frame_size_read = 108 + 3 * PIDITEMS + 2 * CHECKBOXITEMS; // maybe not
																	// used in
																	// 2.1
		activation1 = new int[CHECKBOXITEMS]; // not used in 2.1
		activation2 = new int[CHECKBOXITEMS]; // not used in 2.1
		activation = new int[CHECKBOXITEMS];
		ActiveModes = new boolean[CHECKBOXITEMS];
		buttonCheckboxLabel = new String[] { "LEVEL", "BARO", "MAG", "CAMSTAB", "CAMTRIG", "ARM", "GPS HOME", "GPS HOLD", "PASSTHRU", "HEADFREE", "BEEPER", "LEDMAX", "LLIGHTS",
				"HEADADJ" };

		Checkbox = new Boolean[CHECKBOXITEMS][12];
		ResetAllChexboxes();
		// ////end changes/////////////
	}

	void ResetAllChexboxes() {
		for (int i = 0; i < buttonCheckboxLabel.length; i++) {
			for (int j = 0; j < 12; j++) {
				Checkbox[i][j] = false;
			}
		}
	}

	// send msp without payload
	private List<Byte> requestMSP(int msp) {
		return requestMSP(msp, null);
	}

	// send multiple msp without payload
	private List<Byte> requestMSP(int[] msps) {
		List<Byte> s = new LinkedList<Byte>();
		for (int m : msps) {
			s.addAll(requestMSP(m, null));
		}
		return s;
	}

	// send msp with payload
	private List<Byte> requestMSP(int msp, Character[] payload) {
		if (msp < 0) {
			return null;
		}
		List<Byte> bf = new LinkedList<Byte>();
		for (byte c : MSP_HEADER.getBytes()) {
			bf.add(c);
		}

		byte checksum = 0;
		// byte pl_size = (byte)((payload != null ? int(payload.length) :
		// 0)&0xFF);
		byte pl_size = (byte) ((payload != null ? (int) (payload.length) : 0) & 0xFF);
		bf.add(pl_size);
		checksum ^= (pl_size & 0xFF);

		bf.add((byte) (msp & 0xFF));
		checksum ^= (msp & 0xFF);

		if (payload != null) {
			for (char c : payload) {
				bf.add((byte) (c & 0xFF));
				checksum ^= (c & 0xFF);
			}
		}
		bf.add(checksum);
		return (bf);
	}

	void sendRequestMSP(List<Byte> msp) {
		byte[] arr = new byte[msp.size()];
		int i = 0;
		for (byte b : msp) {
			arr[i++] = b;
		}
		bt.Write(arr); // send the complete byte sequence in one go
	}

	public void evaluateCommand(byte cmd, int dataSize) {
		int i;
		int icmd = (int) (cmd & 0xFF);
		switch (icmd) {
			case MSP_IDENT:
				version = read8();
				multiType = read8();
				read8(); // MSP version
				read32();// capability
				break;
			case MSP_STATUS:
				cycleTime = read16();
				i2cError = read16();
				present = read16();
				mode = read32();

				if ((present & 1) > 0)
					AccPresent = 1;
				else AccPresent = 0;

				if ((present & 2) > 0)
					BaroPresent = 1;
				else BaroPresent = 0;

				if ((present & 4) > 0)
					MagPresent = 1;
				else MagPresent = 0;

				if ((present & 8) > 0)
					GPSPresent = 1;
				else GPSPresent = 0;

				if ((present & 16) > 0)
					SonarPresent = 1;
				else SonarPresent = 0;

				for (i = 0; i < CHECKBOXITEMS; i++) {
					if ((mode & (1 << i)) > 0)
						ActiveModes[i] = true;
					else ActiveModes[i] = false;
				}
				break;
			case MSP_RAW_IMU:
				ax = read16();
				ay = read16();
				az = read16();
				gx = read16() / 8;
				gy = read16() / 8;
				gz = read16() / 8;
				magx = read16() / 3;
				magy = read16() / 3;
				magz = read16() / 3;
				break;
			case MSP_SERVO:
				for (i = 0; i < 8; i++)
					servo[i] = read16();
				break;
			case MSP_MOTOR:
				for (i = 0; i < 8; i++)
					mot[i] = read16();
				break;
			case MSP_RC:
				rcRoll = read16();
				rcPitch = read16();
				rcYaw = read16();
				rcThrottle = read16();
				rcAUX1 = read16();
				rcAUX2 = read16();
				rcAUX3 = read16();
				rcAUX4 = read16();
				break;
			case MSP_RAW_GPS:
				GPS_fix = read8();
				// println(dataSize);
				GPS_numSat = read8();
				GPS_latitude = read32();
				GPS_longitude = read32();
				GPS_altitude = read16();
				GPS_speed = read16();
				break;
			case MSP_COMP_GPS:
				GPS_distanceToHome = read16();
				GPS_directionToHome = read16();
				GPS_update = read16();
				break;
			case MSP_ATTITUDE:
				angx = read16() / 10;
				angy = read16() / 10;
				head = read16();
				break;
			case MSP_ALTITUDE:
				baro = alt = (float) read32() / 100;
				break;
			case MSP_BAT:
				bytevbat = read8();
				pMeterSum = read16();
				break;
			case MSP_RC_TUNING:
				byteRC_RATE = read8();
				byteRC_EXPO = read8();
				byteRollPitchRate = read8();
				byteYawRate = read8();
				byteDynThrPID = read8();
				byteThrottle_MID = read8();
				byteThrottle_EXPO = read8();
				break;
			case MSP_ACC_CALIBRATION:
				break;
			case MSP_MAG_CALIBRATION:
				break;
			case MSP_PID:
				for (i = 0; i < PIDITEMS; i++) {
					byteP[i] = read8();
					byteI[i] = read8();
					byteD[i] = read8();
				}
				break;
			case MSP_BOX:

				for (i = 0; i < CHECKBOXITEMS; i++) {
					activation[i] = read16();
					for (int aa = 0; aa < 12; aa++) {
						if ((activation[i] & (1 << aa)) > 0)
							Checkbox[i][aa] = true;
						else Checkbox[i][aa] = false;
					}
				}
				break;

			case MSP_BOXNAMES:
				buttonCheckboxLabel = new String(inBuf, 0, dataSize).split(";");
				break;
			case MSP_PIDNAMES:

				/* TODO create GUI elements from this message */
				// System.out.println("Got PIDNAMES: "+new String(inBuf, 0,
				// dataSize));
				break;
		case MSP_MISC:
			intPowerTrigger = read16();
			break;
			case MSP_MOTOR_PINS:
				for (i = 0; i < 8; i++) {
					byteMP[i] = read8();
				}
				break;
			case MSP_DEBUG:
				debug1 = read16();
				debug2 = read16();
				debug3 = read16();
				debug4 = read16();
				break;

			case MSP_WP:
				Waypoint WP0 = new Waypoint();
				WP0.No = read8();
				WP0.Lat = read32();
				WP0.Lon = read32();
				WP0.Alt = read16();
				WP0.Nav = read8();
				Log.d("aaa", String.valueOf(WP0.No));
				Log.d("aaa", String.valueOf(WP0.Lat));
				Log.d("aaa", String.valueOf(WP0.Lon));
				break;
			default:

		}
	}

	int		c_state		= IDLE;
	byte	c;
	boolean	err_rcvd	= false;
	int		offset		= 0, dataSize = 0;
	byte	checksum	= 0;
	byte	cmd;
	byte[]	inBuf		= new byte[256];
	int		i			= 0;
	int		p			= 0;

	int read32() {
		return (inBuf[p++] & 0xff) + ((inBuf[p++] & 0xff) << 8) + ((inBuf[p++] & 0xff) << 16) + ((inBuf[p++] & 0xff) << 24);
	}

	int read16() {
		return (inBuf[p++] & 0xff) + ((inBuf[p++]) << 8);
	}

	int read8() {
		return inBuf[p++] & 0xff;
	}

	public void ReadFrame() {
		while (bt.available() > 0) {
			c = (bt.Read());
			// Log.d("21",String.valueOf(c));
			if (c_state == IDLE) {
				c_state = (c == '$') ? HEADER_START : IDLE;
			}
			else if (c_state == HEADER_START) {
				c_state = (c == 'M') ? HEADER_M : IDLE;
			}
			else if (c_state == HEADER_M) {
				if (c == '>') {
					c_state = HEADER_ARROW;
				}
				else if (c == '!') {
					c_state = HEADER_ERR;
				}
				else {
					c_state = IDLE;
				}
			}
			else if (c_state == HEADER_ARROW || c_state == HEADER_ERR) {
				/* is this an error message? */
				err_rcvd = (c_state == HEADER_ERR); /*
													 * now we are expecting the
													 * payload size
													 */
				dataSize = (c & 0xFF);
				/* reset index variables */
				p = 0;
				offset = 0;
				checksum = 0;
				checksum ^= (c & 0xFF);
				/* the command is to follow */
				c_state = HEADER_SIZE;
			}
			else if (c_state == HEADER_SIZE) {
				cmd = (byte) (c & 0xFF);
				checksum ^= (c & 0xFF);
				c_state = HEADER_CMD;
			}
			else if (c_state == HEADER_CMD && offset < dataSize) {
				checksum ^= (c & 0xFF);
				inBuf[offset++] = (byte) (c & 0xFF);
			}
			else if (c_state == HEADER_CMD && offset >= dataSize) {
				/* compare calculated and transferred checksum */
				if ((checksum & 0xFF) == (c & 0xFF)) {
					if (err_rcvd) {
						// System.err.println("Copter did not understand request type "+c);
					}
					else {
						/* we got a valid response packet, evaluate it */
						evaluateCommand(cmd, (int) dataSize);
					}
				}
				else {
					System.out.println("invalid checksum for command " + ((int) (cmd & 0xFF)) + ": " + (checksum & 0xFF) + " expected, got " + (int) (c & 0xFF));
					System.out.print("<" + (cmd & 0xFF) + " " + (dataSize & 0xFF) + "> {");
					for (i = 0; i < dataSize; i++) {
						if (i != 0) {
							System.err.print(' ');
						}
						System.out.print((inBuf[i] & 0xFF));
					}
					System.out.println("} [" + c + "]");
					System.out.println(new String(inBuf, 0, dataSize));
				}
				c_state = IDLE;
			}
		}
	}

	int	timer1	= 0;

	@Override
	public void SendRequest() {
		if (bt.Connected) {
			int[] requests;

			requests = new int[] { MSP_STATUS, MSP_RAW_IMU, MSP_SERVO, MSP_MOTOR, MSP_RC, MSP_RAW_GPS, MSP_COMP_GPS, MSP_ALTITUDE, MSP_ATTITUDE, MSP_DEBUG };
			sendRequestMSP(requestMSP(requests));

			timer1++;
			if (timer1 > 10) {
				requests = new int[] { MSP_BAT, MSP_IDENT, MSP_MISC, MSP_RC_TUNING };
				sendRequestMSP(requestMSP(requests));
				timer1 = 0;
			}

		}
	}

	@Override
	public void SendRequestGetPID() {
		if (bt.Connected) {
			int[] requests = { MSP_PID, MSP_RC_TUNING };
			sendRequestMSP(requestMSP(requests));

		}

	}

	@Override
	public void SendRequestSetandSaveMISC(int confPowerTrigger) {
		// MSP_SET_MISC
		payload = new ArrayList<Character>();
		intPowerTrigger = (Math.round(confPowerTrigger));
		payload.add((char) (intPowerTrigger % 256));
		payload.add((char) (intPowerTrigger / 256));

		sendRequestMSP(requestMSP(MSP_SET_MISC, payload.toArray(new Character[payload.size()])));

		// MSP_EEPROM_WRITE
		SendRequestWriteToEEprom();
	}

	@Override
	public void ProcessSerialData(boolean appLogging) {
		if (bt.Connected) {
			ReadFrame();
			// ProcessSerialData(appLogging);
			baro = alt = baro - AltCorrection;
			if (appLogging)
				Logging();
		}
	}

	@Override
	public void SendRequestAccCalibration() {
		sendRequestMSP(requestMSP(MSP_ACC_CALIBRATION));
	}

	@Override
	public void SendRequestMagCalibration() {
		sendRequestMSP(requestMSP(MSP_MAG_CALIBRATION));
	}

	ArrayList<Character>	payload	= new ArrayList<Character>();

	@Override
	public void SendRequestSetPID(float confRC_RATE, float confRC_EXPO, float rollPitchRate, float yawRate, float dynamic_THR_PID, float throttle_MID, float throttle_EXPO,
			float[] confP, float[] confI, float[] confD) {

		// MSP_SET_RC_TUNING
		payload = new ArrayList<Character>();
		payload.add((char) (Math.round(confRC_RATE * 100)));
		payload.add((char) (Math.round(confRC_EXPO * 100)));
		payload.add((char) (Math.round(rollPitchRate * 100)));
		payload.add((char) (Math.round(yawRate * 100)));
		payload.add((char) (Math.round(dynamic_THR_PID * 100)));
		payload.add((char) (Math.round(throttle_MID * 100)));
		payload.add((char) (Math.round(throttle_EXPO * 100)));
		sendRequestMSP(requestMSP(MSP_SET_RC_TUNING, payload.toArray(new Character[payload.size()])));

		// MSP_SET_PID
		payload = new ArrayList<Character>();
		for (int i = 0; i < PIDITEMS; i++) {
			byteP[i] = (int) (Math.round(confP[i] * 10));
			byteI[i] = (int) (Math.round(confI[i] * 1000));
			byteD[i] = (int) (Math.round(confD[i]));
		}

		// POS-4 POSR-5 NAVR-6 use different dividers
		byteP[4] = (int) (Math.round(confP[4] * 100.0));
		byteI[4] = (int) (Math.round(confI[4] * 100.0));

		byteP[5] = (int) (Math.round(confP[5] * 10.0));
		byteI[5] = (int) (Math.round(confI[5] * 100.0));
		byteD[5] = (int) ((Math.round(confD[5] * 10000.0)) / 10);

		byteP[6] = (int) (Math.round(confP[6] * 10.0));
		byteI[6] = (int) (Math.round(confI[6] * 100.0));
		byteD[6] = (int) ((Math.round(confD[6] * 10000.0)) / 10);

		for (i = 0; i < PIDITEMS; i++) {
			payload.add((char) (byteP[i]));
			payload.add((char) (byteI[i]));
			payload.add((char) (byteD[i]));
		}
		sendRequestMSP(requestMSP(MSP_SET_PID, payload.toArray(new Character[payload.size()])));

	}

	@Override
	public void SendRequestResetSettings() {
		sendRequestMSP(requestMSP(MSP_RESET_CONF));

	}

	@Override
	public void SendRequestGetMisc() {
		sendRequestMSP(requestMSP(MSP_MISC));

	}

	@Override
	public void SendRequestGPSinject21(byte GPS_FIX, byte numSat, int coordLAT, int coordLON, int altitude, int speed) {
		ArrayList<Character> payload = new ArrayList<Character>();
		payload.add((char) GPS_FIX);
		payload.add((char) numSat);
		payload.add((char) (coordLAT & 0xFF));
		payload.add((char) ((coordLAT >> 8) & 0xFF));
		payload.add((char) ((coordLAT >> 16) & 0xFF));
		payload.add((char) ((coordLAT >> 24) & 0xFF));

		payload.add((char) (coordLON & 0xFF));
		payload.add((char) ((coordLON >> 8) & 0xFF));
		payload.add((char) ((coordLON >> 16) & 0xFF));
		payload.add((char) ((coordLON >> 24) & 0xFF));

		payload.add((char) (altitude & 0xFF));
		payload.add((char) ((altitude >> 8) & 0xFF));

		payload.add((char) (speed & 0xFF));
		payload.add((char) ((speed >> 8) & 0xFF));

		sendRequestMSP(requestMSP(MSP_SET_RAW_GPS, payload.toArray(new Character[payload.size()])));
	}

	@Override
	public void SendRequestGetWayPoints() {
		ArrayList<Character> payload = new ArrayList<Character>();
		payload.add((char) 0);

		sendRequestMSP(requestMSP(MSP_WP, payload.toArray(new Character[payload.size()])));

		// TODO
	}

	/**
	 * 0rcRoll 1rcPitch 2rcYaw 3rcThrottle 4rcAUX1 5rcAUX2 6rcAUX3 7rcAUX4
	 */
	@Override
	public void SendRequestSetRawRC(int[] channels8) {
		ArrayList<Character> payload = new ArrayList<Character>();
		for (int i = 0; i < 8; i++) {
			payload.add((char) (channels8[i] & 0xFF));
			payload.add((char) ((channels8[i] >> 8) & 0xFF));

			sendRequestMSP(requestMSP(MSP_SET_RAW_RC, payload.toArray(new Character[payload.size()])));

			sendRequestMSP(requestMSP(new int[] { MSP_RC, MSP_STATUS }));
		}

	}

	@Override
	public void SendRequestGetCheckboxes() {
		sendRequestMSP(requestMSP(MSP_BOX));
	}

	@Override
	public void SendRequestSetCheckboxes() {
		// MSP_SET_BOX
		payload = new ArrayList<Character>();
		for (i = 0; i < CHECKBOXITEMS; i++) {
			activation[i] = 0;
			for (int aa = 0; aa < 12; aa++) {
				activation[i] += (int) (((int) (Checkbox[i][aa] ? 1 : 0)) * (1 << aa));
				// activation[i] += (int) (checkbox[i].arrayValue()[aa] * (1 <<
				// aa));

			}
			payload.add((char) (activation[i] % 256));
			payload.add((char) (activation[i] / 256));
		}
		sendRequestMSP(requestMSP(MSP_SET_BOX, payload.toArray(new Character[payload.size()])));

	}

	@Override
	public void SendRequestWriteToEEprom() {
		// MSP_EEPROM_WRITE
		sendRequestMSP(requestMSP(MSP_EEPROM_WRITE));
	}

}

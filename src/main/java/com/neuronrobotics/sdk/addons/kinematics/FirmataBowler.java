package com.neuronrobotics.sdk.addons.kinematics;

import java.io.IOException;
import java.util.ArrayList;

import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;

import com.neuronrobotics.sdk.common.NonBowlerDevice;

public class FirmataBowler extends NonBowlerDevice {

	private FirmataDevice device;
	public FirmataBowler(String port){
		setFirmataDevice(new FirmataDevice("/dev/ttyUSB0"));
		
	}
		
	@Override
	public boolean connectDeviceImp() {
		
		try {
			getFirmataDevice().start(); // initiate communication to the device
			getFirmataDevice().ensureInitializationIsDone();
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} // wait for initialization is done
		return true;
	}

	@Override
	public void disconnectDeviceImp() {
		try {
			getFirmataDevice().stop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // stop communication to the device
	}

	@Override
	public ArrayList<String> getNamespacesImp() {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	public FirmataDevice getFirmataDevice() {
		return device;
	}

	public void setFirmataDevice(FirmataDevice device) {
		this.device = device;
	}

}

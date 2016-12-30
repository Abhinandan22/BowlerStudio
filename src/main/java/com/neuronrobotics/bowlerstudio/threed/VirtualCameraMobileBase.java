package com.neuronrobotics.bowlerstudio.threed;
import java.io.FileInputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;

import com.neuronrobotics.bowlerstudio.assets.AssetFactory;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.sdk.addons.kinematics.DrivingType;
import com.neuronrobotics.sdk.addons.kinematics.IDriveEngine;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import com.neuronrobotics.sdk.addons.kinematics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

public class VirtualCameraMobileBase extends MobileBase {
	
	private final static class IDriveEngineImplementation implements IDriveEngine {
		@Override
		public void DriveVelocityStraight(MobileBase source, double cmPerSecond) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void DriveVelocityArc(MobileBase source, double degreesPerSecond, double cmRadius) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void DriveArc(MobileBase source, TransformNR newPose, double seconds) {
			try{
				TransformNR pureTrans = newPose.copy();
				pureTrans.setRotation(new RotationNR());
				TransformNR global= source.getFiducialToGlobalTransform().times(pureTrans);
				
				double az = Math.toDegrees(newPose.getRotation().getRotationAzimuth()+global.getRotation().getRotationAzimuth());
				double el = Math.toDegrees(newPose.getRotation().getRotationElevation()+global.getRotation().getRotationElevation());
				double tl = Math.toDegrees(newPose.getRotation().getRotationTilt()+global.getRotation().getRotationTilt());
				TransformNR globalRot=new TransformNR(0,
						0,
						0,
						global.getRotation());
				
				RotationNR finalRot = newPose.times(globalRot).getRotation();
				RotationNR 	HandMadeRot =new RotationNR(	tl,
						az, 
						el//el
						);
				if(el>=89.0){
					el=89.0;
				}if(el<=-89.0){
					el=-89.0;
				}
				//System.out.println("Azumuth = "+az+" elevation = "+el+" tilt = "+tl);
				global = new TransformNR(global.getX(),
						global.getY(),
						global.getZ(),
						finalRot);
				//System.err.println("Camera = "+global.getRotation());
				// New target calculated appliaed to global offset
				source.setGlobalToFiducialTransform(global);
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
	}
	private static IDriveEngine de = new IDriveEngineImplementation();
	private static ArrayList<VirtualCameraMobileBase> bases= new ArrayList<VirtualCameraMobileBase>(); 
	public  VirtualCameraMobileBase() throws Exception{
		//super (IOUtils.toInputStream(ScriptingEngine.codeFromGistID("bfa504cdfba41b132c5d","flyingCamera.xml")[0], "UTF-8"));
		super (new FileInputStream( AssetFactory.loadFile("layout/flyingCamera.xml")));
		//setDriveType(DrivingType.WALKING);
		
		setWalkingDriveEngine(getDriveEngine());
		bases.add(this);
	}
	public static IDriveEngine getDriveEngine() {
		return de;
	}
	public static void setDriveEngine(IDriveEngine de) {
		VirtualCameraMobileBase.de = de;
		for(VirtualCameraMobileBase base:bases){
			base.setWalkingDriveEngine(getDriveEngine());
		}
	}

}
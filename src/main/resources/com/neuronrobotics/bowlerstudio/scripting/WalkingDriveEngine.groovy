
import java.util.ArrayList;

import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.util.ThreadUtil;
import com.neuronrobotics.sdk.addons.kinematics.IDriveEngine;

IDriveEngine engine =  new IDriveEngine (){
	double stepOverHeight=5;
	boolean takingStep = false;
	private Double zLock=null;
	@Override
	public void DriveArc(MobileBase source, TransformNR newPose, double seconds) {
		
		if(takingStep)
			return;
		takingStep = true;
		try{
				int numlegs = source.getLegs().size();
				TransformNR [] feetLocations = new TransformNR[numlegs];
				TransformNR [] home = new TransformNR[numlegs];
				ArrayList<DHParameterKinematics> legs = source.getLegs();
				
				// Load in the locations of the tips of each of the feet.
				for(int i=0;i<numlegs;i++){
					feetLocations[i]=legs.get(i).getCurrentTaskSpaceTransform();
					if(zLock==null){
						//sets a standard plane at the z location of the first leg. 
						zLock=feetLocations[i].getZ();
						println "ZLock level set to "+zLock
					}
					home[i] = legs.get(i).calcHome();
					//feetLocations[i].setZ(home[i].getZ());
				}
				//zLock =zLock+newPose.getZ();
				
				//Apply transform to each dimention of current pose
				TransformNR global= source.getFiducialToGlobalTransform();
				global.translateX(newPose.getX());
				global.translateY(newPose.getY());
				global.translateZ(newPose.getZ());
				double rotz = newPose.getRotation().getRotationZ() +global.getRotation().getRotationZ() ;
				double rotx = newPose.getRotation().getRotationX() ;
				double roty = newPose.getRotation().getRotationY() ;
				RotationNR neRot = new RotationNR(	Math.toDegrees(rotx),
													Math.toDegrees(roty),
													Math.toDegrees(rotz));//RotationNR.getRotationZ(Math.toDegrees(rotz));

				global.setRotation(neRot );
				// New target calculated appliaed to global offset
				source.setGlobalToFiducialTransform(global);
				for(int i=0;i<numlegs;i++){
					double footx,footy;
					TransformNR startLocation = legs.get(i).getCurrentTaskSpaceTransform();
					// start by storing where the feet are
					footx = startLocation.getX() - feetLocations[i].getX() ;
					footy = startLocation.getY() - feetLocations[i].getY() ;
					if(!legs.get(i).checkTaskSpaceTransform(feetLocations[i])){
						
						feetLocations[i].setX(home[i].getX()-footx);
						feetLocations[i].setY(home[i].getY()-footy);
						int j=0;
						while(legs.get(i).checkTaskSpaceTransform(feetLocations[i]) && j<20){
							//increment by the xy unit vectors
							feetLocations[i].translateX(footx/2);
							feetLocations[i].translateY(footy/2);
							j++;
							
						}
						//step back one unit vector
						feetLocations[i].translateX(-footx);
						feetLocations[i].translateY(-footy);
						
						//perform the step over
						home[i].setZ(stepOverHeight+zLock+newPose.getZ());
						//println "Leg "+i+" setep over to x="+feetLocations[i].getX()+" y="+feetLocations[i].getY()
						try {
							// lift leg above home
							legs.get(i).setDesiredTaskSpaceTransform(home[i], seconds);
							ThreadUtil.wait((int) (seconds*1000));
							//step to new target
							legs.get(i).setDesiredTaskSpaceTransform(feetLocations[i], seconds);
							ThreadUtil.wait((int) (seconds*1000));
							//set new target for the coordinated motion step at the end
							feetLocations[i].translateX(newPose.getX());
							feetLocations[i].translateY(newPose.getY());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
		
				}
				//all legs have a valid target set, perform coordinated motion
				for(int i=0;i<numlegs;i++){
					feetLocations[i].setZ(zLock.doubleValue()+newPose.getZ());
					try {
						legs.get(i).setDesiredTaskSpaceTransform(feetLocations[i], seconds);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		takingStep = false;
		
	}

	@Override
	public void DriveVelocityStraight(MobileBase source, double cmPerSecond) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void DriveVelocityArc(MobileBase source, double degreesPerSecond,
			double cmRadius) {
		// TODO Auto-generated method stub
		
	}



}

return engine;
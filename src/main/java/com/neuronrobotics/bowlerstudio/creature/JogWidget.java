package com.neuronrobotics.bowlerstudio.creature;

import java.time.Duration;

import net.java.games.input.Component;

import org.reactfx.util.FxTimer;

import com.neuronrobotics.bowlerstudio.ConnectionManager;
import com.neuronrobotics.bowlerstudio.assets.AssetFactory;
import com.neuronrobotics.sdk.addons.gamepad.BowlerJInputDevice;
import com.neuronrobotics.sdk.addons.gamepad.IJInputEventListener;
import com.neuronrobotics.sdk.addons.kinematics.AbstractKinematicsNR;
import com.neuronrobotics.sdk.addons.kinematics.ITaskSpaceUpdateListenerNR;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.DeviceManager;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.util.ThreadUtil;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

public class JogWidget extends GridPane implements ITaskSpaceUpdateListenerNR, IOnTransformChange,IJInputEventListener {
	double defauletSpeed=0.40;
	private AbstractKinematicsNR kin;
	private MobileBase mobilebase=null;
	Button px = new Button("",AssetFactory.loadIcon("Plus-X.png"));
	Button nx = new Button("",AssetFactory.loadIcon("Minus-X.png"));
	Button py = new Button("",AssetFactory.loadIcon("Plus-Y.png"));
	Button ny = new Button("",AssetFactory.loadIcon("Minus-Y.png"));
	Button pz = new Button("",AssetFactory.loadIcon("Plus-Z.png"));
	Button nz = new Button("",AssetFactory.loadIcon("Minus-Z.png"));
	Button home = new Button("",AssetFactory.loadIcon("Home.png"));
	Button game = new Button("Add Game Controller",AssetFactory.loadIcon("Add-Game-Controller.png"));
	TextField increment=new TextField(new Double(defauletSpeed).toString());
	TextField sec=new TextField(".01");
	private TransformWidget transform;
	BowlerJInputDevice gameController=null;
	double x,y,rz,slider=0;
	private boolean stop=true;
	private jogThread jogTHreadHandle;
	
	public JogWidget(AbstractKinematicsNR kinimatics){
		this.setKin(kinimatics);
		
		if(MobileBase.class.isInstance(kinimatics)){
			py = new Button("",AssetFactory.loadIcon("Rotation-Z.png"));
			ny = new Button("",AssetFactory.loadIcon("Rotation-Neg-Z.png"));
			
		}


		getKin().addPoseUpdateListener(this);

		
		px.setOnMousePressed(	event -> handle( (Button)event.getSource() ));
		nx.setOnMousePressed(	event -> handle( (Button)event.getSource() ));
		py.setOnMousePressed(	event -> handle( (Button)event.getSource() ));
		ny.setOnMousePressed(	event -> handle( (Button)event.getSource() ));
		pz.setOnMousePressed(	event -> handle( (Button)event.getSource() ));
		nz.setOnMousePressed(	event -> handle( (Button)event.getSource() ));
		home.setOnMousePressed(	event -> handle( (Button)event.getSource() ));
		
		px.setOnMouseReleased(	event -> handle( (Button)event.getSource() ));
		nx.setOnMouseReleased(	event -> handle( (Button)event.getSource() ));
		py.setOnMouseReleased(	event -> handle( (Button)event.getSource() ));
		ny.setOnMouseReleased(	event -> handle( (Button)event.getSource() ));
		pz.setOnMouseReleased(	event -> handle( (Button)event.getSource() ));
		nz.setOnMouseReleased(	event -> handle( (Button)event.getSource() ));
		home.setOnMouseReleased(	event -> handle( (Button)event.getSource() ));
		game.setOnAction(event -> {
			if(gameController == null){
				gameController = (BowlerJInputDevice) DeviceManager.getSpecificDevice(BowlerJInputDevice.class, "jogController");
				if(gameController==null){
					ConnectionManager.onConnectGamePad("jogController");
					gameController = (BowlerJInputDevice) DeviceManager.getSpecificDevice(BowlerJInputDevice.class, "jogController");
				}
				if(gameController!=null){
					gameController.addListeners(this);
					game.setText("Remove Game Controller");
					controllerLoop();
					//TODO open a configuration panel here
				}else{
					//the controller must not be availible, bailing
				}
				
			}else{
				gameController.removeListeners(this);
				game.setText("Add Game Controller");
				gameController=null;
			}
		});
		
		
		GridPane buttons = new GridPane();
		buttons.getColumnConstraints().add(new ColumnConstraints(80)); // column 1 is 75 wide
		buttons.getColumnConstraints().add(new ColumnConstraints(80)); // column 2 is 300 wide
		buttons.getColumnConstraints().add(new ColumnConstraints(80)); // column 2 is 100 wide
		buttons. getColumnConstraints().add(new ColumnConstraints(50)); // column 2 is 100 wide
	    
		buttons.getRowConstraints().add(new RowConstraints(40)); // 
		buttons. getRowConstraints().add(new RowConstraints(40)); // 
		buttons. getRowConstraints().add(new RowConstraints(40)); // 
		buttons.getRowConstraints().add(new RowConstraints(40)); // 
		
		buttons.add(	py, 
				0, 
				1);
		buttons.add(	home, 
				1, 
				1);
		buttons.add(	ny, 
				2, 
				1);
		
		
		buttons.add(	px, 
				1, 
				0);
		
		buttons.add(	nx, 
				1, 
				2);
		buttons.add(	increment, 
				0, 
				3);
		buttons.add(	new Label("m/s"), 
				1, 
				3);
		
		buttons.add(	sec, 
				2, 
				3);
		buttons.add(	new Label("sec"), 
				3, 
				3);
		if(!MobileBase.class.isInstance(kinimatics)){
			buttons.add(	pz, 
					3, 
					0);
			buttons.add(	nz, 
					3, 
					1);
		}
		buttons.add(	game, 
				4, 
				0);
		add(	buttons, 
				0, 
				0);
		transform = new TransformWidget("Current Pose", getKin().getCurrentPoseTarget(), this);
		Accordion advancedPanel = new Accordion();
		advancedPanel.getPanes().add(new TitledPane("Current Position", transform));
		add(	advancedPanel, 
				0, 
				1);
		jogTHreadHandle = new jogThread();
		jogTHreadHandle.start();
		controllerLoop();
	}
	
	private void handle(final Button button ){

		if(!button.isPressed()){
			// button released
			Log.warning(button.getText()+" Button released ");
			try {
				TransformNR t = getKin().getCurrentTaskSpaceTransform();
				getKin().setDesiredTaskSpaceTransform(t,  0);
			} catch (Exception e) {}
			if(button == px){
				x=0;
			}
			if(button == nx){
				x=0;
			}
			if(mobilebase==null){
				if(button == py){
					y=0;
				}
				if(button == ny){
					y=0;
				}
			}else{
				if(button == py){
					rz=0;
				}
				if(button == ny){
					rz=0;
				}
			}
			if(button == pz){
				slider=0;
			}
			if(button == nz){
				slider=0;
			}
			stop=true;
			return;
		}else{
			Log.warning(button.getText()+" Button pressed ");
		}
		if(button == px){
			x=1;
		}
		if(button == nx){
			x=-1;
		}
		if(mobilebase==null){
			if(button == py){
				y=1;
			}
			if(button == ny){
				y=-1;
			}
		}else{
			if(button == py){
				rz=1;
			}
			if(button == ny){
				rz=-1;
			}
		}
		if(button == pz){
			slider=1;
		}
		if(button == nz){
			slider=-1;
		}
		if(button == home){
			home();
			stop=true;
			return;
		}
		stop=false;
		controllerLoop();
	}
	
	public void home(){
		if(getMobilebase()!=null){
			getMobilebase().setGlobalToFiducialTransform(new TransformNR());
		}
		for(int i=0;i<getKin().getNumberOfLinks();i++){
			try {
				getKin().setDesiredJointAxisValue(i, 0, Double.parseDouble(sec.getText()));
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			kin.setDesiredTaskSpaceTransform( kin.calcHome(),0);
		} catch (Exception e) {}
	}

	@Override
	public void onTaskSpaceUpdate(AbstractKinematicsNR source, TransformNR pose) {
		// TODO Auto-generated method stub
		Platform.runLater(() -> {
			transform.updatePose(pose);
		});
	}

	@Override
	public void onTargetTaskSpaceUpdate(AbstractKinematicsNR source,
			TransformNR pose) {
		Platform.runLater(() -> {
			transform.updatePose(pose);
		});
	}
	
	@Override
	public void onTransformChaging(TransformNR newTrans) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onTransformFinished(TransformNR newTrans) {
		try {
			getKin().setDesiredTaskSpaceTransform(newTrans,  Double.parseDouble(sec.getText()));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public AbstractKinematicsNR getKin() {
		return kin;
	}
	public void setKin(AbstractKinematicsNR kin) {
		if(MobileBase.class.isInstance(kin))
			setMobilebase((MobileBase)kin);
		this.kin = kin;
		try {
			kin.setDesiredTaskSpaceTransform( kin.calcHome(),0);
		} catch (Exception e) {}
	}
	
	
	private void controllerLoop(){
		double seconds=.1;
		if(gameController!=null || stop==false){
			try{
				seconds =Double.parseDouble(sec.getText());
				if(!stop){ 
					
					
					double inc;
					try{
						inc = Double.parseDouble(increment.getText())*1000*seconds;//convert to mm
						
					}catch(Exception e){
						inc=defauletSpeed;
						Platform.runLater(() -> {
							increment.setText(new Double(defauletSpeed).toString());
						});
					}
					//double rxl=0;
					double ryl=inc/20*slider;
					double rzl=inc/2*rz;
					TransformNR current = new TransformNR(0,0,0,new RotationNR( 0,0, rzl));
					current.translateX(inc*x);
					current.translateY(inc*y);
					current.translateZ(inc*slider);
					
					try {
						if(getMobilebase()==null){
							current = getKin().getCurrentPoseTarget();
							current.translateX(inc*x);
							current.translateY(inc*y);
							current.translateZ(inc*slider);
							current.setRotation(new RotationNR());
							TransformNR toSet = current.copy();
							double toSeconds=seconds;
							jogTHreadHandle.setToSet(toSet, toSeconds);
							
						}else{
							TransformNR toSet = current.copy();
							double toSeconds=seconds;
							jogTHreadHandle.setToSet(toSet, toSeconds);
						}
							
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}catch(Exception e){
				
			}
			if(seconds<.01){
				seconds=.01;
				sec.setText(".01");
			}
			FxTimer.runLater(
					Duration.ofMillis((int)(seconds*1000.0)) ,() -> {
						controllerLoop();
					});
		}
	}
	
	private class jogThread extends Thread{
		private boolean controlThreadRunning=false;
		private TransformNR toSet;
		private double toSeconds=.016;
		public void run(){
			setName("Jog Widget Set Drive Arc Command");
			while(kin.isAvailable()){
				if(controlThreadRunning){
					if(getMobilebase()==null){
						try {
							Log.enableDebugPrint();
							//System.out.println("Jogging to: "+toSet);
							getKin().setDesiredTaskSpaceTransform(toSet,  toSeconds);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}else{
						toSet.setZ(0);
						try {
							getMobilebase().DriveArc(toSet, toSeconds);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					controlThreadRunning=false;
				}
				ThreadUtil.wait((int) (toSeconds*1000));
			}
		}

		public void setToSet(TransformNR toSet,double toSeconds) {
			this.toSet = toSet.copy();
			this.toSeconds = toSeconds;
			controlThreadRunning=true;
		}
		
	}
	

	@Override
	public void onEvent(Component comp, net.java.games.input.Event event,
			float value, String eventString) {

		if(comp.getName().toLowerCase().contentEquals("y"))
			x=value;
		if(comp.getName().toLowerCase().contentEquals("rz"))
			y=value;
		if(comp.getName().toLowerCase().contentEquals("x"))
			rz=-value;
		if(comp.getName().toLowerCase().contentEquals("slider"))
			slider=-value;
		if(Math.abs(x)<.01)
			x=0;
		if(Math.abs(y)<.01)
			y=0;
		if(Math.abs(rz)<.01)
			rz=0;
		if(Math.abs(slider)<.01)
			slider=0;
		if(x==0.0&&y==0.0 &&rz==0.0&&slider==0) {
			//System.out.println("Stoping on="+comp.getName());
			stop=true;
			try {
				getKin().setDesiredTaskSpaceTransform(getKin().getCurrentTaskSpaceTransform(),  0);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else
			stop=false;
		
		
	}

	public MobileBase getMobilebase() {
		return mobilebase;
	}

	public void setMobilebase(MobileBase mobilebase) {
		this.mobilebase = mobilebase;
	}

}

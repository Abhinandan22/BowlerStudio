package com.neuronrobotics.nrconsole.plugin.cartesian;

import java.time.Duration;

import org.reactfx.util.FxTimer;

import com.neuronrobotics.sdk.addons.kinematics.AbstractKinematicsNR;
import com.neuronrobotics.sdk.addons.kinematics.ITaskSpaceUpdateListenerNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

public class JogWidget extends GridPane implements ITaskSpaceUpdateListenerNR, IOnTransformChange {
	
	private AbstractKinematicsNR kin;
	Button px = new Button("+X");
	Button nx = new Button("-X");
	Button py = new Button("+Y");
	Button ny = new Button("-Y");
	Button pz = new Button("+Z");
	Button nz = new Button("-Z");
	Button home = new Button("home");
	TextField increment=new TextField("10");
	TextField sec=new TextField("1");
	private TransformWidget transform;

	public JogWidget(AbstractKinematicsNR kinimatics){
		this.setKin(kinimatics);
		

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
		
		
		
		
		
		
		GridPane buttons = new GridPane();
		buttons.getColumnConstraints().add(new ColumnConstraints(50)); // column 1 is 75 wide
		buttons.getColumnConstraints().add(new ColumnConstraints(60)); // column 2 is 300 wide
		buttons.getColumnConstraints().add(new ColumnConstraints(50)); // column 2 is 100 wide
		buttons. getColumnConstraints().add(new ColumnConstraints(50)); // column 2 is 100 wide
	    
		buttons.getRowConstraints().add(new RowConstraints(30)); // 
		buttons. getRowConstraints().add(new RowConstraints(30)); // 
		buttons. getRowConstraints().add(new RowConstraints(30)); // 
		buttons.getRowConstraints().add(new RowConstraints(30)); // 
		
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
		buttons.add(	new Label("mm"), 
				1, 
				3);
		
		buttons.add(	sec, 
				2, 
				3);
		buttons.add(	new Label("sec"), 
				3, 
				3);
		
		buttons.add(	pz, 
				3, 
				0);
		buttons.add(	nz, 
				3, 
				1);
		add(	buttons, 
				0, 
				0);
		transform = new TransformWidget("Current Pose", getKin().getCurrentPoseTarget(), this);
		Accordion advancedPanel = new Accordion();
		advancedPanel.getPanes().add(new TitledPane("Advanced Positioning", transform));
		add(	advancedPanel, 
				0, 
				1);
	}
	
	private void handle(final Button button ){
		TransformNR current = getKin().getCurrentTaskSpaceTransform();
		double inc;
		try{
			inc = Double.parseDouble(increment.getText());
		}catch(Exception e){
			Platform.runLater(() -> {
				increment.setText("10");
			});
			inc=100;
		}
		if(!button.isPressed()){
			// button released
			Log.warning(button.getText()+" Button released ");
			try {
				getKin().setDesiredTaskSpaceTransform(current,  0);
			} catch (Exception e) {}
			return;
		}else{
			Log.warning(button.getText()+" Button pressed ");
		}
		if(button == px){
			current.translateX(inc);
		}
		if(button == nx){
			current.translateX(-inc);
		}
		if(button == py){
			current.translateY(inc);
		}
		if(button == ny){
			current.translateY(-inc);
		}
		if(button == pz){
			current.translateZ(inc);
		}
		if(button == nz){
			current.translateZ(-inc);
		}
		if(button == home){
			home();
		}else{
			try {
				double seconds =Double.parseDouble(sec.getText());
				getKin().setDesiredTaskSpaceTransform(current,  seconds);
				
				FxTimer.runLater(
						Duration.ofMillis((int)(seconds*1000.0)) ,() -> {
							Log.warning(button.getText()+" Completion handler");
							if(button.isPressed()){
								handle( button);
								return;
							}
							Log.warning(button.getText()+" Still pressed");
						});
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void home(){
		for(int i=0;i<getKin().getNumberOfLinks();i++){
			try {
				getKin().setDesiredJointAxisValue(i, 0, Double.parseDouble(sec.getText()));
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
		this.kin = kin;
	}

}
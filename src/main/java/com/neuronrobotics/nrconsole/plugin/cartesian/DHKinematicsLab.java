package com.neuronrobotics.nrconsole.plugin.cartesian;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import sun.security.action.GetLongAction;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.neuronrobotics.bowlerstudio.BowlerStudioController;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.tabs.AbstractBowlerStudioTab;
import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import com.neuronrobotics.nrconsole.util.GroovyFilter;
import com.neuronrobotics.nrconsole.util.XmlFilter;
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink;
import com.neuronrobotics.sdk.addons.kinematics.DHChain;
import com.neuronrobotics.sdk.addons.kinematics.DHLink;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.LinkConfiguration;
import com.neuronrobotics.sdk.addons.kinematics.LinkFactory;
import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Transform;

public class DHKinematicsLab extends AbstractBowlerStudioTab {
	DHParameterKinematics device;
	private File currentFile=null;
	private VBox links;
	private HBox controls;
	JogWidget jog = null;
	@Override
	public void onTabClosing() {
		// TODO Auto-generated method stub

	}

	@Override
	public String[] getMyNameSpaces() {
		// TODO Auto-generated method stub
		return new String[0];
	}

	@Override
	public void initializeUI(BowlerAbstractDevice pm) {
		// TODO Auto-generated method stub
		setText("DH Lab");
		device=(DHParameterKinematics)pm;
		Log.debug("Loading xml: "+device.getXml());
		links = new VBox(20);
		controls = new HBox(10);
		jog = new JogWidget(device);
		controls.getChildren().add(jog);
		Button save = new Button("Save Configuration");
		Button add = new Button("Add Link");
		Button refresh = new Button("RefreshModel");
		save.setOnAction(event -> {
			new Thread(){
				public void run(){
					File last = FileSelectionFactory.GetFile(currentFile==null?
										ScriptingEngine.getWorkspace():
										new File(ScriptingEngine.getWorkspace().getAbsolutePath()+"/"+currentFile.getName()),
							new XmlFilter());
					if (last != null) {
						try {
							Files.write(Paths.get(last.getAbsolutePath()),device.getXml().getBytes() );
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}.start();
		});
		add.setOnAction(event -> {
			LinkConfiguration newLink = new LinkConfiguration();
			LinkFactory factory  =device.getFactory();
			//remove the link listener while the number of links could chnage
			factory.removeLinkListener(device);
			AbstractLink link = factory.getLink(newLink);
			DHChain chain =  device.getDhChain() ;
			chain.addLink(new DHLink(0, 0, 0, 0));
			//set the modified kinematics chain
			device.setChain(chain);
			//once the new link configuration is set up, re add the listener
			factory.addLinkListener(device);
			onTabReOpening();
		});
		refresh.setOnAction(event -> {
			onTabReOpening();
		});
		
		
		controls.getChildren().add(save);
		controls.getChildren().add(add);
		controls.getChildren().add(refresh);
		onTabReOpening();

		setContent(new ScrollPane(links));
	}


	@Override
	public void onTabReOpening() {
		links.getChildren().clear();
		ArrayList<DHLink> dhLinks = device.getChain().getLinks();
		links.getChildren().add(controls);
		ArrayList<CSG> csg = new ArrayList<CSG>();
		
		for(int i=0;i<dhLinks.size();i++){
			DHLink dh  =dhLinks.get(i);
			links.getChildren().add(
									new DHLinkWidget(i,
													dh,
													device
													)
									);
			
			// Create an axis to represent the link
			double y = dh.getD()>0?dh.getD():2;
			double  x= dh.getRadius()>0?dh.getRadius():2;

			CSG cube = new Cube(x,y,2).toCSG();
			cube=cube.transformed(new Transform().translateX(-x/2));
			cube=cube.transformed(new Transform().translateY(y/2));
			//add listner to axis
			cube.setManipulator(dh.getListener());
			cube.setColor(Color.GOLD);
			// add ax to list of objects to be returned
			csg.add(cube);
		}
		BowlerStudioController.setCsg(csg);
		jog.home();
	}

}

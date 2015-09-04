package com.neuronrobotics.bowlerstudio.creature;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.ArrayList;

import org.python.core.exceptions;

import javafx.application.Platform;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.FileChooser.ExtensionFilter;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.BowlerStudioController;
import com.neuronrobotics.bowlerstudio.ConnectionManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngineWidget;
import com.neuronrobotics.bowlerstudio.scripting.ShellType;
import com.neuronrobotics.bowlerstudio.tabs.AbstractBowlerStudioTab;
import com.neuronrobotics.nrconsole.util.DirectoryFilter;
import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import com.neuronrobotics.nrconsole.util.GroovyFilter;
import com.neuronrobotics.nrconsole.util.XmlFilter;
import com.neuronrobotics.sdk.addons.kinematics.AbstractKinematicsNR;
import com.neuronrobotics.sdk.addons.kinematics.DHLink;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.DhInverseSolver;
import com.neuronrobotics.sdk.addons.kinematics.DrivingType;
import com.neuronrobotics.sdk.addons.kinematics.IDriveEngine;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.IDeviceConnectionEventListener;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.util.FileChangeWatcher;
import com.neuronrobotics.sdk.util.IFileChangeListener;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.STL;
import eu.mihosoft.vrl.v3d.Transform;

public class CreatureLab extends AbstractBowlerStudioTab implements ICadGenerator, IOnEngineeringUnitsChange {

	private ICadGenerator cadEngine;
	private BowlerAbstractDevice pm;
	private File openMobileBaseConfiguration;
	private File cadScript;
	private FileChangeWatcher watcher;
	private IDriveEngine defaultDriveEngine;
	private DhInverseSolver defaultDHSolver;
	private Menu localMenue;

	@Override
	public void onTabClosing() {
		// TODO Auto-generated method stub
		if (watcher != null) {
			watcher.close();
		}
	}

	@Override
	public String[] getMyNameSpaces() {
		// TODO Auto-generated method stub
		return new String[0];
	}

	@Override
	public void initializeUI(BowlerAbstractDevice pm) {
		this.pm = pm;
		// TODO Auto-generated method stub
		setText(pm.getScriptingName());

		GridPane dhlabTopLevel=new GridPane();
		
		


		if(DHParameterKinematics.class.isInstance(pm)){
			DHParameterKinematics device=(DHParameterKinematics)pm;
        	try {
        		setDefaultDhParameterKinematics(device);
        		
			} catch (Exception e) {
				  StringWriter sw = new StringWriter();
			      PrintWriter pw = new PrintWriter(sw);
			      e.printStackTrace(pw);
			      System.out.println(sw.toString());
			}
			Log.debug("Loading xml: "+device.getXml());
			dhlabTopLevel.add(new DhChainWidget(device, null), 0, 0);
		}else if(MobileBase.class.isInstance(pm)) {
			MobileBase device=(MobileBase)pm;
			Menu CreaturLabMenue =BowlerStudio.getCreatureLabMenue();
			localMenue = new Menu(pm.getScriptingName());
			MenuItem printable = new MenuItem("Generate Printable CAD");
			printable.setOnAction(event -> {
				File defaultStlDir =new File(System.getProperty("user.home")+"/bowler-workspace/STL/");
				if(!defaultStlDir.exists()){
					defaultStlDir.mkdirs();
				}
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("Select Output Directory For .STL files");
			
				chooser.setInitialDirectory(defaultStlDir);
    	    	File baseDirForFiles = chooser.showDialog(BowlerStudio.getPrimaryStage());

    	        if (baseDirForFiles == null) {
    	            return;
    	        }
		    	new Thread(){

					public void run(){
						
		    	        generateCad();
		    	        ArrayList<File> files = cadEngine.generateStls((MobileBase) pm, baseDirForFiles);
		    	        Platform.runLater(()->{
		    				Alert alert = new Alert(AlertType.INFORMATION);
		    				alert.setTitle("Stl Export Success!");
		    				alert.setHeaderText("Stl Export Success");
		    				alert.setContentText("All SLT's for the Creature Generated at\n"+files.get(0).getAbsolutePath());
		    				alert.setWidth(500);
		    				alert .initModality(Modality.APPLICATION_MODAL);
		    				alert.show();
		    	        });
		    		}
		    	}.start();
				generateCad();
			});
			
			
			MenuItem saveConfig = new MenuItem("Save Configuration");
			saveConfig.setOnAction(event -> {
		    	new Thread(){

					public void run(){
						if(openMobileBaseConfiguration==null)
							openMobileBaseConfiguration=ScriptingEngineWidget.getLastFile();
		    	    	openMobileBaseConfiguration = FileSelectionFactory.GetFile(openMobileBaseConfiguration,
		    	    			new ExtensionFilter("MobileBase XML","*.xml","*.XML"));

		    	        if (openMobileBaseConfiguration == null) {
		    	            return;
		    	        }
		    	        try {
							PrintWriter out = new PrintWriter(openMobileBaseConfiguration.getAbsoluteFile());
							out.println(device.getXml());
							out.flush();
							out.close();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    	        
		    		}
		    	}.start();
			});
			
			
			MenuItem setCadScript = new MenuItem("Set Cad Generation Script");
			
			setCadScript.setOnAction(event -> {
		    	new Thread(){

					public void run(){
						setName("Cad generation thread");
						if(getCadScript()==null)
							setCadScript(ScriptingEngineWidget.getLastFile());
		    	    	setCadScript(FileSelectionFactory.GetFile(getCadScript(),
		    	    			new ExtensionFilter("Kinematics Script","*.groovy","*.java","*.txt")));

		    	        if (getCadScript() == null) {
		    	            return;
		    	        }
		    	        generateCad();
		    	        
		    		}
		    	}.start();
			});
			MenuItem updateRobotScripts = new MenuItem("Pull Scripts from Server");
			updateRobotScripts.setOnAction(event -> {
		    	new Thread(){

					public void run(){
						setName("Cad generation thread");
						cadEngine=null;
						defaultDriveEngine=null;
						try {
							setDefaultLinkLevelCadEngine();
							setDefaultWalkingEngine(device);
			    	        generateCad();
						} catch (Exception e) {
							  StringWriter sw = new StringWriter();
						      PrintWriter pw = new PrintWriter(sw);
						      e.printStackTrace(pw);
						      System.out.println(sw.toString());
						}
						
		    	        
		    		}
		    	}.start();
			});
			localMenue.getItems().addAll(printable, saveConfig, setCadScript, updateRobotScripts);
			
			
			CreaturLabMenue.getItems().add(localMenue);
			CreaturLabMenue.setDisable(false);
			pm.addConnectionEventListener(new IDeviceConnectionEventListener() {
				@Override
				public void onDisconnect(BowlerAbstractDevice source) {
					// cleanup menues after add
					CreaturLabMenue.getItems().remove(localMenue);
					if(CreaturLabMenue.getItems().size()==0)
						CreaturLabMenue.setDisable(true);
					BowlerStudioController.setCsg(null);
				}
				
				@Override
				public void onConnect(BowlerAbstractDevice source) {}
			});
			
	
			//Button save = new Button("Save Configuration");
			
			
			try {
				setDefaultWalkingEngine(device);
			} catch (Exception e) {
				  StringWriter sw = new StringWriter();
			      PrintWriter pw = new PrintWriter(sw);
			      e.printStackTrace(pw);
			      System.out.println(sw.toString());
			}
			
			GridPane mobileBaseControls=new GridPane();
			dhlabTopLevel.add(mobileBaseControls, 0, 0);
			
			Accordion advancedPanel = new Accordion();
			TitledPane rp =new TitledPane("Multi-Appendage Cordinated Motion", new DhChainWidget(device, this));
			advancedPanel.getPanes().add(rp);


			addAppendagePanel(device.getLegs(),"Legs",advancedPanel);
			addAppendagePanel(device.getAppendages(),"Appandges",advancedPanel);
			addAppendagePanel(device.getSteerable(),"Steerable",advancedPanel);
			addAppendagePanel(device.getDrivable(),"Drivable",advancedPanel);
			
			dhlabTopLevel.add(advancedPanel, 0, 2);
			
			if(device.getDriveType() != DrivingType.NONE){
				advancedPanel.setExpandedPane(rp);
			}
			
		}else if(AbstractKinematicsNR.class.isInstance(pm)) {
			AbstractKinematicsNR device=(AbstractKinematicsNR)pm;
			dhlabTopLevel.add(new DhChainWidget(device,null), 0, 0);
		}
		generateCad();
		
		setContent(new ScrollPane(dhlabTopLevel));
		
	}

	private void setDefaultLinkLevelCadEngine() throws Exception {
		String [] cad =null;
		if(MobileBase.class.isInstance(pm)) {
			cad = ((MobileBase)pm).getCadEngine();
		}else if(DHParameterKinematics.class.isInstance(pm)){
			DHParameterKinematics device=(DHParameterKinematics)pm;
			cad = device.getCadEngine();
		}
		if(cadEngine==null){
			String code = ScriptingEngineWidget.codeFromGistID(cad[0],cad[1])[0];
			cadEngine = (ICadGenerator) ScriptingEngine.inlineScriptRun(code, null,ShellType.GROOVY);
		}
	}
	private void setDefaultDhParameterKinematics(DHParameterKinematics device) throws Exception {
		String code = ScriptingEngineWidget.codeFromGistID(device.getDhEngine()[0],device.getDhEngine()[1])[0];
		defaultDHSolver = (DhInverseSolver) ScriptingEngine.inlineScriptRun(code, null,ShellType.GROOVY);
		
		device.setInverseSolver(defaultDHSolver);
	}

	private void setDefaultWalkingEngine(MobileBase device) throws Exception {
		if(defaultDriveEngine==null){
			String code = ScriptingEngineWidget.codeFromGistID(device.getWalkingEngine()[0],device.getWalkingEngine()[1])[0];
			defaultDriveEngine = (IDriveEngine) ScriptingEngine.inlineScriptRun(code, null,ShellType.GROOVY);
		}
		device.setWalkingDriveEngine( defaultDriveEngine);
		for(DHParameterKinematics dh : device.getAllDHChains()){
			setDefaultDhParameterKinematics(dh);
		}
	}
	
	private void addAppendagePanel(ArrayList<DHParameterKinematics> apps,String title,Accordion advancedPanel){
		if(apps.size()>0){
			for(DHParameterKinematics l:apps){
				TitledPane rp =new TitledPane(title+" - "+l.getScriptingName(),new DhChainWidget(l, this));
				rp.setMaxWidth(200);
				rp.setMaxHeight(200);
				advancedPanel.getPanes().add(rp);
				advancedPanel.setExpandedPane(rp);
			}
		}
		
	}
	private void generateCad(){
		new Thread(){
			public void run(){
				Log.warning("Generating cad");
				//new Exception().printStackTrace();
				ArrayList<CSG> allCad=new ArrayList<>();
				if(MobileBase.class.isInstance(pm)) {
					MobileBase device=(MobileBase)pm;
					if (getCadScript() != null) {
						try{
							cadEngine = (ICadGenerator) ScriptingEngine.inlineFileScriptRun(getCadScript(), null);
						}catch(Exception e){
						      StringWriter sw = new StringWriter();
						      PrintWriter pw = new PrintWriter(sw);
						      e.printStackTrace(pw);
						      System.out.println(sw.toString());
						}
			        }
					if(cadEngine==null){
						try {
							setDefaultLinkLevelCadEngine();
						} catch (Exception e) {
							  StringWriter sw = new StringWriter();
						      PrintWriter pw = new PrintWriter(sw);
						      e.printStackTrace(pw);
						      System.out.println(sw.toString());
						}
					}
					try {
						allCad= cadEngine.generateBody(device);
					} catch (Exception e) {
						  StringWriter sw = new StringWriter();
					      PrintWriter pw = new PrintWriter(sw);
					      e.printStackTrace(pw);
					      System.out.println(sw.toString());
					}
					
				}else if(DHParameterKinematics.class.isInstance(pm)){
					for(CSG csg:generateCad(((DHParameterKinematics)pm).getChain().getLinks())){
						allCad.add(csg);
						
					}
				}
				BowlerStudioController.setCsg(allCad);
			}


		}.start();
	}
	
	

//	private void generateCad(){
//		Log.warning("Generating cad");
//		//new Exception().printStackTrace();
//		ArrayList<CSG> allCad=new ArrayList<>();
//		if(MobileBase.class.isInstance(pm)) {
//			MobileBase device=(MobileBase)pm;
//			for(DHParameterKinematics l:device.getAllDHChains()){
//				for(CSG csg:generateCad(l.getChain().getLinks())){
//					allCad.add(csg);
//				}
//			}
//			
//		}else if(DHParameterKinematics.class.isInstance(pm)){
//			for(CSG csg:generateCad(((DHParameterKinematics)pm).getChain().getLinks())){
//				allCad.add(csg);
////				new Thread(){
////					public void run(){
////						BowlerStudioController.setCsg(allCad);
////					}
////				}.start();
//				
//			}
//		}
//		new Thread(){
//			public void run(){
//				
//				BowlerStudioController.setCsg(allCad);
//			}
//		}.start();
//	}
	
	public ArrayList<CSG> generateCad(ArrayList<DHLink> dhLinks ){
		if (getCadScript() != null) {
			try{
			cadEngine = (ICadGenerator) ScriptingEngine.inlineFileScriptRun(getCadScript(), null);
			}catch(Exception e){
			      StringWriter sw = new StringWriter();
			      PrintWriter pw = new PrintWriter(sw);
			      e.printStackTrace(pw);
			      System.out.println(sw.toString());
			}
        }
		if(cadEngine==null){
			try {
				setDefaultLinkLevelCadEngine();
				} catch (Exception e) {
				  StringWriter sw = new StringWriter();
			      PrintWriter pw = new PrintWriter(sw);
			      e.printStackTrace(pw);
			      System.out.println(sw.toString());
			}
		}
		try {
			return cadEngine.generateCad(dhLinks);
		} catch (Exception e) {
			  StringWriter sw = new StringWriter();
		      PrintWriter pw = new PrintWriter(sw);
		      e.printStackTrace(pw);
		      System.out.println(sw.toString());
		}
		return null;
		
	}


	@Override
	public void onTabReOpening() {
		setCadScript(getCadScript());
		try{
			generateCad();
		}catch(Exception ex){
			
		}
	}
	
	public static String getFormatted(double value){
	    return String.format("%4.3f%n", (double)value);
	}

	public File getCadScript() {
		return cadScript;
	}

	public void setCadScript(File cadScript) {
		if (watcher != null) {
		
			watcher.close();
		}
		 try {
		 watcher = new FileChangeWatcher(cadScript);
		 watcher.addIFileChangeListener(new IFileChangeListener() {
			
			@Override
			public void onFileChange(File fileThatChanged, WatchEvent event) {
				try{
					generateCad();
				}catch(Exception ex){
					
				}
				
			}
		});
		 watcher.start();
		 } catch (IOException e) {
		 // TODO Auto-generated catch block
		 e.printStackTrace();
		 }
		this.cadScript = cadScript;
		
	}

	@Override
	public ArrayList<CSG> generateBody(MobileBase base) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<File> generateStls(MobileBase base, File baseDirForFiles) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onSliderMoving(EngineeringUnitsSliderWidget source, double newAngleDegrees) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSliderDoneMoving(EngineeringUnitsSliderWidget source,
			double newAngleDegrees) {
		generateCad();
	}

}

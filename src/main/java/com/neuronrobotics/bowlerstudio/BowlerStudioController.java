package com.neuronrobotics.bowlerstudio;

import eu.mihosoft.vrl.v3d.CSG;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.text.BadLocationException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jgit.api.errors.GitAPIException;

//import org.bytedeco.javacpp.DoublePointer;












import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import com.neuronrobotics.bowlerstudio.assets.AssetFactory;
import com.neuronrobotics.bowlerstudio.scripting.IScriptEventListener;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingFileWidget;
import com.neuronrobotics.bowlerstudio.tabs.LocalFileScriptTab;
import com.neuronrobotics.bowlerstudio.tabs.WebTab;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.MobileBaseCadManager;
import com.neuronrobotics.imageprovider.AbstractImageProvider;
import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.util.ThreadUtil;
import com.sun.javafx.scene.control.behavior.TabPaneBehavior;
import com.sun.javafx.scene.control.skin.TabPaneSkin;

public class BowlerStudioController extends TabPane implements
		IScriptEventListener {

	private static final int WEBSERVER_PORT = 8065;
	private static String HOME_URL = "http://neuronrobotics.com/BowlerStudio/Welcome-To-BowlerStudio/";
	private static String HOME_Local_URL = "http://localhost:"+WEBSERVER_PORT+"/BowlerStudio/Welcome-To-BowlerStudio/";
	/**
	 * 
	 */
	private static final long serialVersionUID = -2686618188618431477L;
	private ConnectionManager connectionManager;
	private BowlerStudio3dEngine jfx3dmanager;
	private MainController mainController;
	private AbstractImageProvider vrCamera;
	private static BowlerStudioController bowlerStudioControllerStaticReference=null;
	private boolean doneLoadingTutorials = false;
	public BowlerStudioController(BowlerStudio3dEngine jfx3dmanager,
			MainController mainController) {
		if(getBowlerStudio()!=null)
			throw new RuntimeException("There can be only one Bowler Studio controller");
		bowlerStudioControllerStaticReference=this;
		this.setJfx3dmanager(jfx3dmanager);
		this.mainController = mainController;
		createScene();
		
	}
	private HashMap<String,Tab> openFiles = new HashMap<>();
	private HashMap<String,LocalFileScriptTab> widgets = new HashMap<>();
	private HashMap<String,WebTab> webTabs = new HashMap<>();
	// Custom function for creation of New Tabs.
	public ScriptingFileWidget createFileTab(File file) {
		if(openFiles.get(file.getAbsolutePath())!=null && widgets.get(file.getAbsolutePath())!=null){
			setSelectedTab(openFiles.get(file.getAbsolutePath()));
			return widgets.get(file.getAbsolutePath()).getScripting();
		}

		Tab fileTab =new Tab(file.getName());
		openFiles.put(file.getAbsolutePath(), fileTab);
		try {
			Log.warning("Loading local file from: "+file.getAbsolutePath());
			LocalFileScriptTab t  =new LocalFileScriptTab( file);
			fileTab.setContent(t);
			fileTab.setGraphic(AssetFactory.loadIcon("Script-Tab-"+ScriptingEngine.getShellType(file.getName())+".png"));
			addTab(fileTab, true);
			widgets.put(file.getAbsolutePath(),  t);
			fileTab.setOnCloseRequest(event->{
				widgets.remove(file.getAbsolutePath());
				openFiles.remove(file.getAbsolutePath());
			});
			return t.getScripting();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	public void clearHighlits(){
		for(Entry<String, LocalFileScriptTab> set: widgets.entrySet()){
			set.getValue().clearHighlits();
		}
	}
	
	public void setHighlight(File fileEngineRunByName, int lineNumber, Color color) {
		System.out.println("Highlighting line "+lineNumber+" in "+fileEngineRunByName);
		if(openFiles.get(fileEngineRunByName.getAbsolutePath())==null){
			createFileTab(fileEngineRunByName);
		}
		setSelectedTab(openFiles.get(fileEngineRunByName.getAbsolutePath()));
		//System.out.println("Highlighting "+fileEngineRunByName+" at line "+lineNumber+" to color "+color);
		try {
			widgets.get(fileEngineRunByName.getAbsolutePath()).setHighlight(lineNumber,color);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	public static void highlightException(File fileEngineRunByName, Exception ex){
		bowlerStudioControllerStaticReference.highlightExceptionLocal(fileEngineRunByName, ex);
	}
	public static void clearHighlight(){
		bowlerStudioControllerStaticReference.clearHighlits();
	}
	private void highlightExceptionLocal(File fileEngineRunByName, Exception ex) {
		new Thread(){
			public void run(){
				setName("Highlighter thread");
				if(fileEngineRunByName!=null){
					if(openFiles.get(fileEngineRunByName.getAbsolutePath())==null){
						createFileTab(fileEngineRunByName);
					}
					setSelectedTab(openFiles.get(fileEngineRunByName.getAbsolutePath()));
					widgets.get(fileEngineRunByName.getAbsolutePath()).clearHighlits();
					//System.out.println("Highlighting "+fileEngineRunByName+" at line "+lineNumber+" to color "+color);
					for(StackTraceElement el:ex.getStackTrace()){
						try {
							//System.out.println("Compairing "+fileEngineRunByName.getName()+" to "+el.getFileName());
							if(el.getFileName().contentEquals(fileEngineRunByName.getName())){
								widgets.get(fileEngineRunByName.getAbsolutePath()).setHighlight(el.getLineNumber(),Color. CYAN);
							}
						} catch (Exception e) {
//							StringWriter sw = new StringWriter();
//							PrintWriter pw = new PrintWriter(sw);
//							e.printStackTrace(pw);
//							System.out.println(sw.toString());
						}
					}
					
				}
				if(widgets.get(fileEngineRunByName.getAbsolutePath())!=null){
					String message = ex.getMessage();
					//System.out.println(message);
					if(message!=null)
						if(message.contains(fileEngineRunByName.getName())){
							
							try {
								int indexOfFile = message.lastIndexOf(fileEngineRunByName.getName());
								String fileSub=message.substring(indexOfFile);
								String [] fileAndNum =fileSub .split(":");
								String FileNum = fileAndNum[1];
								int linNum =  Integer.parseInt(FileNum.trim());
								widgets.get(fileEngineRunByName.getAbsolutePath()).setHighlight(linNum,Color.CYAN);
							} catch (Exception e) {
								StringWriter sw = new StringWriter();
								PrintWriter pw = new PrintWriter(sw);
								e.printStackTrace(pw);
								System.out.println(sw.toString());
							}
						}
				}
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);
				System.out.println(sw.toString());
			}
		}.start();

		
	}
	

//	// Custom function for creation of New Tabs.
//	private void createAndSelectNewTab(final BowlerStudioController tabPane,
//			final String title) {
//
//
//			Platform.runLater(() -> {
//				try {
//					if(ScriptingEngine.getLoginID() != null)
//						
//						addTab(new ScriptingGistTab(title,getHomeUrl(), true), false);
//				} catch (IOException | InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			});
//
//
//	}
	
	public void openUrlInNewTab(URL url){
		String urlstr=url.toExternalForm();
		
		if(webTabs.get(urlstr)!=null){
			setSelectedTab(webTabs.get(urlstr));
		}else
			Platform.runLater(() -> {
				try {
					if(ScriptingEngine.getLoginID() != null){
						WebTab newTab = new WebTab("Web",url.toExternalForm(), false);
						newTab.setOnCloseRequest(event -> {
							webTabs.remove(urlstr );
						});
						webTabs.put(urlstr,newTab );
						addTab(newTab, true);
					}
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
	}

	private Tab createTab() throws IOException, InterruptedException {
		final WebTab tab = new WebTab(null,
				 null);

		return tab;
	}

	public void addTab(Tab tab, boolean closable) {

		//new RuntimeException().printStackTrace();

		Platform.runLater(() -> {
			final ObservableList<Tab> tabs = getTabs();
			tab.setClosable(closable);
			int index = tabs.size() - 1;
			//new RuntimeException().printStackTrace();
			tabs.add(index, tab);
			setSelectedTab(tab);
		});
		
	}

	public void createScene() {

		// BorderPane borderPane = new BorderPane();

		// Placement of TabPane.
		setSide(Side.TOP);

		/*
		 * To disable closing of tabs.
		 * tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		 */

		final Tab newtab = new Tab();
		newtab.setText("");
		newtab.setClosable(false);
		newtab.setGraphic(AssetFactory.loadIcon("New-Web-Tab.png"));
		
		// Addnewtabition of New Tab to the tabpane.
		getTabs().addAll(newtab);
		new Thread(){
			public void run(){
				try {
					
					File indexOfTutorial = ScriptingEngine.fileFromGit(
							"https://github.com/NeuronRobotics/NeuronRobotics.github.io.git", 
							"index.html");
					
					//HOME_Local_URL = indexOfTutorial.toURI().toString().replace("file:/", "file:///");
					Server server = new Server(WEBSERVER_PORT);

					ResourceHandler resource_handler = new ResourceHandler();
					resource_handler.setDirectoriesListed(true);
					resource_handler.setWelcomeFiles(new String[] { "index.html" });

					resource_handler.setResourceBase(indexOfTutorial.getParent());

					HandlerList handlers = new HandlerList();
					handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
					server.setHandler(handlers);
					doneLoadingTutorials = true;
					try {
						server.start();
						server.join();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} catch (GitAPIException | IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
			}
		}.start();
		
		long start = System.currentTimeMillis();
		// wait up to 30 seconds for menue to load, then fail over to the web version
		while(! doneLoadingTutorials && (System.currentTimeMillis()-start<3000)){
			ThreadUtil.wait(100);
		}
		if(doneLoadingTutorials && !ScriptingEngine.isAutoupdate())
				HOME_URL = HOME_Local_URL;
		Platform.runLater(() -> {
			Tab t=new Tab();
			try {
				
				
				t = new WebTab("Tutorial",getHomeUrl(), true);
			} catch (Exception  e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Tab tab =t;
			final ObservableList<Tab> tabs = getTabs();
			ConnectionManager.getConnectionManager().setClosable(false);
			int index = tabs.size() - 1;
			//new RuntimeException().printStackTrace();
			tabs.add(index, ConnectionManager.getConnectionManager());
			setSelectedTab(ConnectionManager.getConnectionManager());

			tab.setClosable(false);
			index = tabs.size() - 1;
			//new RuntimeException().printStackTrace();
			tabs.add(index, tab);
			setSelectedTab(tab);

		});

		// Function to add and display new tabs with default URL display.
		getSelectionModel().selectedItemProperty().addListener(
				new ChangeListener<Tab>() {
					@Override
					public void changed(
							ObservableValue<? extends Tab> observable,
							Tab oldSelectedTab, Tab newSelectedTab) {
						if (newSelectedTab == newtab) {

							try {
								addTab(createTab(), true);

							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					}
				});
	}

	public static String getHomeUrl() {

		return HOME_URL;
	}



	private void removeObject(Object p) {
		if (CSG.class.isInstance(p)) {
			Platform.runLater(() -> {
				getJfx3dmanager().removeObjects();
			});
		} else if (Tab.class.isInstance(p)) {
			Platform.runLater(() -> {
				// new RuntimeException().printStackTrace();
				getTabs().remove(p);
				Tab t = (Tab) p;
				TabPaneBehavior behavior = ((TabPaneSkin) getSkin())
						.getBehavior();
				if (behavior.canCloseTab(t)) {
					behavior.closeTab(t);
				}
			});
		}
	}
	
	public static void setCsg(List<CSG> toadd, File source){
		Platform.runLater(() -> {
			getBowlerStudio().getJfx3dmanager().removeObjects();
			if(toadd!=null)
			for(CSG c:toadd){
				Platform.runLater(() ->getBowlerStudio().getJfx3dmanager().addObject(c,source));
			}
		});
	}
	public static void setCsg(List<CSG> toadd){
		setCsg(toadd,null);
	}
	public static void addCsg(CSG toadd){
		addCsg(toadd,null);
	}
	public static void addCsg(CSG toadd, File source){
		Platform.runLater(() -> {
			if(toadd!=null)
				getBowlerStudio().getJfx3dmanager().addObject(toadd,source);
			
		});
	}
	private void addObject(Object o, File source) {
		if (CSG.class.isInstance(o)) {
			CSG csg = (CSG) o;
			Platform.runLater(() -> {
				// new RuntimeException().printStackTrace();
				getJfx3dmanager().addObject(csg,source);
			});
		} else if (Tab.class.isInstance(o)) {

			addTab((Tab) o, true);

		}if (BowlerAbstractDevice.class.isInstance(o)) {
			BowlerAbstractDevice bad = (BowlerAbstractDevice) o;
			ConnectionManager.addConnection((BowlerAbstractDevice) o,
					bad.getScriptingName());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onScriptFinished(Object result, Object Previous,File source) {
		Log.warning("Loading script results " + result + " previous "
				+ Previous);
		// this is added in the script engine when the connection manager is
		// loaded

		ThreadUtil.wait(20);
		if (ArrayList.class.isInstance(Previous)) {
			ArrayList<Object> c = (ArrayList<Object>) Previous;
			for (int i = 0; i < c.size(); i++) {
				removeObject(c.get(i));
			}
		} else {
			removeObject(Previous);
		}
		//Check if a CSG is coming in and clear the screen first
		if (ArrayList.class.isInstance(result)) {
			ArrayList<Object> c = (ArrayList<Object>) result;
			for (int i = 0; i < c.size(); i++) {
				if (CSG.class.isInstance(c.get(i))){
					Platform.runLater(() -> {
						getJfx3dmanager().removeObjects();
					});
					break;
				}
			}
		} else {
			if (CSG.class.isInstance(result)){
				Platform.runLater(() -> {
					getJfx3dmanager().removeObjects();
				});
			}
		}
		if (ArrayList.class.isInstance(result)) {
			ArrayList<Object> c = (ArrayList<Object>) result;
			for (int i = 0; i < c.size(); i++) {
				Log.warning("Loading array Lists with removals " + c.get(i));
				addObject(c.get(i),  source);
			}
		} else {
			addObject(result,  source);
		}
	}

	@Override
	public void onScriptChanged(String previous, String current,File source) {

	}

	@Override
	public void onScriptError(Exception except,File source) {
		// TODO Auto-generated method stub

	}

	public void disconnect() {
		ConnectionManager.disconnectAll();
	}

	public Stage getPrimaryStage() {
		// TODO Auto-generated method stub
		return BowlerStudio.getPrimaryStage();
	}

	public void setSelectedTab(Tab tab) {
		if(getSelectionModel().getSelectedItem()!=tab){
			Platform.runLater(() -> {
				//System.out.println("Selecting "+tab.getText());
				getSelectionModel().select(tab);
			});
		}
	}

	public AbstractImageProvider getVrCamera() {
		return vrCamera;
	}

	public void setVrCamera(AbstractImageProvider vrCamera) {
		this.vrCamera = vrCamera;
	}

	public static BowlerStudioController getBowlerStudio() {
		return bowlerStudioControllerStaticReference;
	}


	public static void setup() {
		// TODO Auto-generated method stub
		
	}
	
	public static void clearCSG() {
		Platform.runLater(() -> {
			getBowlerStudio().getJfx3dmanager().removeObjects();
		});
	}

	public static void setCsg(CSG legAssembly, File cadScript) {
		Platform.runLater(() -> {
			getBowlerStudio().getJfx3dmanager().removeObjects();
			if(legAssembly!=null)
	
				Platform.runLater(() ->getBowlerStudio().getJfx3dmanager().addObject(legAssembly,cadScript));
			
		});
	}


	public static void setCsg(MobileBaseCadManager thread, File cadScript) {
		setCsg(thread.getAllCad(), cadScript);
	}


	public BowlerStudio3dEngine getJfx3dmanager() {
		return jfx3dmanager;
	}


	public void setJfx3dmanager(BowlerStudio3dEngine jfx3dmanager) {
		this.jfx3dmanager = jfx3dmanager;
	}

	

}
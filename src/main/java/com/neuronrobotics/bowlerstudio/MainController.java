/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.neuronrobotics.bowlerstudio;

import com.neuronrobotics.bowlerstudio.assets.AssetFactory;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.scripting.CommandLineWidget;
import com.neuronrobotics.bowlerstudio.scripting.IGithubLoginListener;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingFileWidget;
//import com.neuronrobotics.bowlerstudio.scripting.*;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.twod.TwoDCad;
import com.neuronrobotics.bowlerstudio.twod.TwoDCadFactory;
import com.neuronrobotics.imageprovider.CHDKImageProvider;
import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import com.neuronrobotics.nrconsole.util.PromptForGit;
import com.neuronrobotics.pidsim.LinearPhysicsEngine;
import com.neuronrobotics.replicator.driver.NRPrinter;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.pid.VirtualGenericPIDDevice;
import com.neuronrobotics.sdk.util.ThreadUtil;

import eu.mihosoft.vrl.v3d.Polygon;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GHGist;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.reactfx.util.FxTimer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;

//import javafx.scene.control.ScrollPane;

/**
 * FXML Controller class
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * @author Kevin Harrington madhephaestus:github mad.hephaestus@gmail.com
 */
public class MainController implements Initializable {

	private SubScene subScene;
	private BowlerStudio3dEngine jfx3dmanager;
	private File openFile;
	private BowlerStudioController application;
	private MainController mainControllerRef;
	protected EventHandler<? super KeyEvent> normalKeyPessHandle = null;
	protected static String currentGistID = ""; // Is there a better solution to
												// pass data into a controller
												// than using
												// a static global?
	// private CommandLineWidget cmdLine;
	// protected EventHandler<? super KeyEvent> normalKeyPessHandle;

	/**
	 * FXML Widgets
	 */

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="BowlerStudioMenue"
	private MenuBar BowlerStudioMenue; // Value injected by FXMLLoader

	@FXML // fx:id="CadControlsAnchor"
	private AnchorPane CadControlsAnchor; // Value injected by FXMLLoader

	@FXML // fx:id="CadTextSplit"
	private SplitPane CadTextSplit; // Value injected by FXMLLoader

	@FXML // fx:id="CommandLine"
	private AnchorPane CommandLine; // Value injected by FXMLLoader

	@FXML // fx:id="CreaturesMenu"
	private Menu CreaturesMenu; // Value injected by FXMLLoader

	@FXML // fx:id="DriveControlsAnchor"
	private AnchorPane DriveControlsAnchor; // Value injected by FXMLLoader

	@FXML // fx:id="GitHubRoot"
	private Menu GitHubRoot; // Value injected by FXMLLoader

	@FXML // fx:id="TempControlsAnchor"
	private AnchorPane TempControlsAnchor; // Value injected by FXMLLoader

	@FXML // fx:id="addMarlinGCODEDevice"
	private MenuItem addMarlinGCODEDevice; // Value injected by FXMLLoader

	@FXML // fx:id="clearCache"
	private MenuItem clearCache; // Value injected by FXMLLoader

	@FXML // fx:id="commandLineTitledPane"
	private TitledPane commandLineTitledPane; // Value injected by FXMLLoader

	@FXML // fx:id="createNewGist"
	private MenuItem createNewGist; // Value injected by FXMLLoader

	@FXML // fx:id="editorContainer"
	private AnchorPane editorContainer; // Value injected by FXMLLoader

	@FXML // fx:id="jfx3dControls"
	private AnchorPane jfx3dControls; // Value injected by FXMLLoader

	@FXML // fx:id="logView"
	private AnchorPane logView; // Value injected by FXMLLoader

	@FXML // fx:id="logViewRef"
	private TextArea logViewRef; // Value injected by FXMLLoader

	@FXML // fx:id="logoutGithub"
	private MenuItem logoutGithub; // Value injected by FXMLLoader

	@FXML // fx:id="myGists"
	private Menu myGists; // Value injected by FXMLLoader

	@FXML // fx:id="myOrganizations"
	private Menu myOrganizations; // Value injected by FXMLLoader

	@FXML // fx:id="myRepos"
	private Menu myRepos; // Value injected by FXMLLoader

	@FXML // fx:id="overlayScrollPanel"
	private ScrollPane overlayScrollPanel; // Value injected by FXMLLoader

	@FXML // fx:id="viewContainer"
	private AnchorPane viewContainer; // Value injected by FXMLLoader

	@FXML // fx:id="watchingRepos"
	private Menu watchingRepos; // Value injected by FXMLLoader

	public void setCadSplit(double value) {
		Platform.runLater(() -> {
			CadTextSplit.setDividerPosition(0, value);
		});
	}

	public void setOverlayLeft(TreeView<String> content) {
		Platform.runLater(() -> {

			overlayScrollPanel.setFitToHeight(true);
			overlayScrollPanel.setContent(content);
			content.setOpacity(1);
			overlayScrollPanel.viewportBoundsProperty()
					.addListener((ObservableValue<? extends Bounds> arg0, Bounds arg1, Bounds arg2) -> {
				// Node content = overlayScrollPanel.getContent();
				//
				// System.out.println("Resizing " + arg2);
				Platform.runLater(() -> {
					overlayScrollPanel.setFitToHeight(true);
					/// content.seth
					overlayScrollPanel.setContent(content);

				});
			});
			overlayScrollPanel.setVisible(true);
		});
	}

	public void clearOverlayLeft() {
		Platform.runLater(() -> {
			overlayScrollPanel.setContent(null);
			overlayScrollPanel.setVisible(false);
		});
	}

	public void setOverlayTop(Group content) {
		Platform.runLater(() -> {
			CadControlsAnchor.getChildren().clear();
			CadControlsAnchor.getChildren().add(content);

			AnchorPane.setTopAnchor(content, 0.0);
			AnchorPane.setRightAnchor(content, 0.0);
			AnchorPane.setLeftAnchor(content, 0.0);
			AnchorPane.setBottomAnchor(content, 0.0);
			CadControlsAnchor.setVisible(true);
		});
	}

	public void clearOverlayTop() {
		Platform.runLater(() -> {
			CadControlsAnchor.getChildren().clear();
			CadControlsAnchor.setVisible(false);
		});
	}

	public void setOverlayTopRight(Group content) {
		Platform.runLater(() -> {
			DriveControlsAnchor.getChildren().clear();
			DriveControlsAnchor.getChildren().add(content);
			AnchorPane.setTopAnchor(content, 0.0);
			AnchorPane.setRightAnchor(content, 0.0);
			AnchorPane.setLeftAnchor(content, 0.0);
			AnchorPane.setBottomAnchor(content, 0.0);
			DriveControlsAnchor.setVisible(true);
		});
	}

	public void clearOverlayTopRight() {
		Platform.runLater(() -> {
			DriveControlsAnchor.getChildren().clear();
			DriveControlsAnchor.setVisible(false);
		});
	}

	public void setOverlayBottomRight(Group content) {
		Platform.runLater(() -> {
			TempControlsAnchor.getChildren().clear();
			TempControlsAnchor.getChildren().add(content);
			AnchorPane.setTopAnchor(content, 0.0);
			AnchorPane.setRightAnchor(content, 0.0);
			AnchorPane.setLeftAnchor(content, 0.0);
			AnchorPane.setBottomAnchor(content, 0.0);
			TempControlsAnchor.setVisible(true);
		});
	}

	public void clearOverlayBottomRight() {
		Platform.runLater(() -> {
			TempControlsAnchor.getChildren().clear();
			TempControlsAnchor.setVisible(false);
		});
	}

	// private final CodeArea codeArea = new CodeArea();

	/**
	 * Initializes the controller class.
	 *
	 * @param url
	 * @param rb
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		assert BowlerStudioMenue != null : "fx:id=\"BowlerStudioMenue\" was not injected: check your FXML file 'Main.fxml'.";
		assert CadControlsAnchor != null : "fx:id=\"CadControlsAnchor\" was not injected: check your FXML file 'Main.fxml'.";
		assert CommandLine != null : "fx:id=\"CommandLine\" was not injected: check your FXML file 'Main.fxml'.";
		assert CreaturesMenu != null : "fx:id=\"CreaturesMenu\" was not injected: check your FXML file 'Main.fxml'.";
		assert DriveControlsAnchor != null : "fx:id=\"DriveControlsAnchor\" was not injected: check your FXML file 'Main.fxml'.";
		assert GitHubRoot != null : "fx:id=\"GitHubRoot\" was not injected: check your FXML file 'Main.fxml'.";
		assert TempControlsAnchor != null : "fx:id=\"TempControlsAnchor\" was not injected: check your FXML file 'Main.fxml'.";
		assert clearCache != null : "fx:id=\"clearCache\" was not injected: check your FXML file 'Main.fxml'.";
		assert commandLineTitledPane != null : "fx:id=\"commandLineTitledPane\" was not injected: check your FXML file 'Main.fxml'.";
		assert createNewGist != null : "fx:id=\"createNewGist\" was not injected: check your FXML file 'Main.fxml'.";
		assert editorContainer != null : "fx:id=\"editorContainer\" was not injected: check your FXML file 'Main.fxml'.";
		assert jfx3dControls != null : "fx:id=\"jfx3dControls\" was not injected: check your FXML file 'Main.fxml'.";
		assert logView != null : "fx:id=\"logView\" was not injected: check your FXML file 'Main.fxml'.";
		assert logViewRef != null : "fx:id=\"logViewRef\" was not injected: check your FXML file 'Main.fxml'.";
		assert logoutGithub != null : "fx:id=\"logoutGithub\" was not injected: check your FXML file 'Main.fxml'.";
		assert myGists != null : "fx:id=\"myGists\" was not injected: check your FXML file 'Main.fxml'.";
		assert myOrganizations != null : "fx:id=\"myOrganizations\" was not injected: check your FXML file 'Main.fxml'.";
		assert myRepos != null : "fx:id=\"myRepos\" was not injected: check your FXML file 'Main.fxml'.";
		assert overlayScrollPanel != null : "fx:id=\"overlayScrollPanel\" was not injected: check your FXML file 'Main.fxml'.";
		assert viewContainer != null : "fx:id=\"viewContainer\" was not injected: check your FXML file 'Main.fxml'.";
		assert watchingRepos != null : "fx:id=\"watchingRepos\" was not injected: check your FXML file 'Main.fxml'.";
		clearOverlayLeft();
		BowlerStudio.setLogViewRefStatic(logViewRef);
		System.out.println("Main controller inializing");
		mainControllerRef = this;
		addMarlinGCODEDevice.setOnAction(event->{
			Platform.runLater(() -> ConnectionManager.onMarlinGCODE());
		});
		new Thread(new Runnable() {

			@Override
			public void run() {
				ThreadUtil.wait(200);

				// ScriptingEngine.getGithub().getMyself().getGravatarId()
				// System.out.println("Loading 3d engine");
				jfx3dmanager = new BowlerStudio3dEngine();

				setApplication(new BowlerStudioController(jfx3dmanager, mainControllerRef));
				Platform.runLater(() -> {
					editorContainer.getChildren().add(getApplication());
					AnchorPane.setTopAnchor(getApplication(), 0.0);
					AnchorPane.setRightAnchor(getApplication(), 0.0);
					AnchorPane.setLeftAnchor(getApplication(), 0.0);
					AnchorPane.setBottomAnchor(getApplication(), 0.0);

					subScene = jfx3dmanager.getSubScene();
					subScene.setFocusTraversable(false);
					subScene.setOnMouseEntered(mouseEvent -> {
						// System.err.println("3d window requesting focus");
						Scene topScene = BowlerStudio.getScene();
						normalKeyPessHandle = topScene.getOnKeyPressed();
						// jfx3dmanager.handleKeyboard(topScene);
					});

					subScene.setOnMouseExited(mouseEvent -> {
						// System.err.println("3d window dropping focus");
						Scene topScene = BowlerStudio.getScene();
						if (normalKeyPessHandle != null)
							topScene.setOnKeyPressed(normalKeyPessHandle);
					});

					subScene.widthProperty().bind(viewContainer.widthProperty());
					subScene.heightProperty().bind(viewContainer.heightProperty());
				});

				Platform.runLater(() -> {
					jfx3dControls.getChildren().add(jfx3dmanager.getControlsBox());
					viewContainer.getChildren().add(subScene);
				});

				FxTimer.runLater(Duration.ofMillis(100), () -> {
					if (ScriptingEngine.getLoginID() != null) {
						setToLoggedIn(ScriptingEngine.getLoginID());
					} else {
						setToLoggedOut();
					}

				});

				ScriptingEngine.addIGithubLoginListener(new IGithubLoginListener() {

					@Override
					public void onLogout(String oldUsername) {
						setToLoggedOut();
					}

					@Override
					public void onLogin(String newUsername) {
						setToLoggedIn(newUsername);

					}
				});
				// System.out.println("Laoding ommand line widget");
				CommandLineWidget cmdLine = new CommandLineWidget();

				Platform.runLater(() -> {
					// CadDebugger.getChildren().add(jfx3dmanager.getDebuggerBox());
					AnchorPane.setTopAnchor(jfx3dmanager.getDebuggerBox(), 0.0);
					AnchorPane.setRightAnchor(jfx3dmanager.getDebuggerBox(), 0.0);
					AnchorPane.setLeftAnchor(jfx3dmanager.getDebuggerBox(), 0.0);
					AnchorPane.setBottomAnchor(jfx3dmanager.getDebuggerBox(), 0.0);
					CommandLine.getChildren().add(cmdLine);
					AnchorPane.setTopAnchor(cmdLine, 0.0);
					AnchorPane.setRightAnchor(cmdLine, 0.0);
					AnchorPane.setLeftAnchor(cmdLine, 0.0);
					AnchorPane.setBottomAnchor(cmdLine, 0.0);
				});
				try {
					ScriptingEngine.setAutoupdate(true);
					File f = ScriptingEngine.fileFromGit(
							"https://github.com/madhephaestus/BowlerStudioExampleRobots.git", // git
																								// repo,
																								// change
																								// this
																								// if
																								// you
																								// fork
																								// this
																								// demo
							"exampleRobots.json"// File from within the Git repo
					);

					@SuppressWarnings("unchecked")
					HashMap<String, HashMap<String, Object>> map = (HashMap<String, HashMap<String, Object>>) ScriptingEngine
							.inlineFileScriptRun(f, null);
					for (Map.Entry<String, HashMap<String, Object>> entry : map.entrySet()) {
						HashMap<String, Object> script = entry.getValue();
						MenuItem item = new MenuItem(entry.getKey());
						item.setOnAction(event -> {
							loadMobilebaseFromGit((String) script.get("scriptGit"), (String) script.get("scriptFile"));
						});
						Platform.runLater(() -> {
							CreaturesMenu.getItems().add(item);
						});
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				HashMap<String, Object> openGits = ConfigurationDatabase.getParamMap("studio-open-git");
				for (String s : openGits.keySet()) {
					ArrayList<String> repoFile = (ArrayList<String>) openGits.get(s);
					try {
						File f = ScriptingEngine.fileFromGit(repoFile.get(0), repoFile.get(1));
						getApplication().createFileTab(f);
					} catch (GitAPIException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				

			}
		}).start();
		Platform.runLater(() -> {
			commandLineTitledPane.setGraphic(AssetFactory.loadIcon("Command-Line.png"));
		});

	}

	private void setToLoggedIn(final String name) {
		// new Exception().printStackTrace();
		FxTimer.runLater(Duration.ofMillis(100), () -> {
			logoutGithub.disableProperty().set(false);
			logoutGithub.setText("Log out " + name);
			new Thread() {
				public void run() {
					try {
						ScriptingEngine.setAutoupdate(false);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					GitHub github = ScriptingEngine.getGithub();
					while (github == null) {
						github = ScriptingEngine.getGithub();
						ThreadUtil.wait(20);
					}
					try {
						GHMyself myself = github.getMyself();
						PagedIterable<GHGist> gists = myself.listGists();
						Platform.runLater(() -> {
							myGists.getItems().clear();
						});
						ThreadUtil.wait(20);
						for (GHGist gist : gists) {
							String desc = gist.getDescription();
							if (desc == null || desc.length() == 0) {
								desc = gist.getFiles().keySet().toArray()[0].toString();
							}
							Menu tmpGist = new Menu(desc);
							String description = desc;
							MenuItem loadWebGist = new MenuItem("Show Web Gist...");
							loadWebGist.setOnAction(event -> {
								String webURL = gist.getHtmlUrl();
								try {
									BowlerStudio.openUrlInNewTab(new URL(webURL));
								} catch (MalformedURLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							});
							MenuItem addFile = new MenuItem("Add file to Gist...");
							addFile.setOnAction(event -> new Thread() {
								public void run() {
									Platform.runLater(() -> {
										Stage s = new Stage();
										currentGistID = gist.getHtmlUrl().substring(24);
										AddFileToGistController controller = new AddFileToGistController();
										try {
											controller.start(s);
										} catch (Exception e) {
											e.printStackTrace();
										}
									});
								}
							}.start());
							Platform.runLater(() -> {
								// tmpGist.getItems().addAll(addFile,
								// loadWebGist);
								tmpGist.getItems().add(loadWebGist);
							});
							EventHandler<Event> loadFiles = new EventHandler<Event>() {
								boolean gistFlag = false;

								@Override
								public void handle(Event ev) {
									if (gistFlag)
										return;// another thread is servicing
												// this gist
									// for(ScriptingEngine.)
									new Thread() {
										public void run() {

											ThreadUtil.wait(500);
											if (!tmpGist.isShowing())
												return;
											if (gistFlag)
												return;// another thread is
														// servicing this gist
											gistFlag = true;
											System.out.println("Loading files for " + description);
											ArrayList<String> listofFiles;
											try {
												listofFiles = ScriptingEngine.filesInGit(gist.getGitPushUrl(), "master",
														null);

											} catch (Exception e1) {
												e1.printStackTrace();
												return;
											}
											if (tmpGist.getItems().size() != 1)
												return;// menue populated by
														// another thread
											for (String s : listofFiles) {
												MenuItem tmp = new MenuItem(s);
												tmp.setOnAction(event -> {
													new Thread() {
														public void run() {
															try {
																File fileSelected = ScriptingEngine
																		.fileFromGit(gist.getGitPushUrl(), s);
																BowlerStudio.createFileTab(fileSelected);
															} catch (Exception e) {
																// TODO
																// Auto-generated
																// catch block
																e.printStackTrace();
															}
														}
													}.start();

												});
												Platform.runLater(() -> {
													tmpGist.getItems().add(tmp);
													// removing this listener
													// after menue is activated
													// for the first time
													tmpGist.setOnShowing(null);

												});
											}
											Platform.runLater(() -> {
												tmpGist.hide();
												Platform.runLater(() -> {
													tmpGist.show();
												});
											});
										}
									}.start();
								}
							};

							tmpGist.setOnShowing(loadFiles);
							Platform.runLater(() -> {
								myGists.getItems().add(tmpGist);
							});

						}
						// Now load the users GIT repositories
						// github.getMyOrganizations();
						Map<String, GHOrganization> orgs = github.getMyOrganizations();
						for (Map.Entry<String, GHOrganization> entry : orgs.entrySet()) {
							// System.out.println("Org: "+org);
							Menu OrgItem = new Menu(entry.getKey());
							GHOrganization ghorg = entry.getValue();
							Map<String, GHRepository> repos = ghorg.getRepositories();
							for (Map.Entry<String, GHRepository> entry1 : repos.entrySet()) {
								setUpRepoMenue(OrgItem, entry1.getValue());
							}
							Platform.runLater(() -> {
								myOrganizations.getItems().add(OrgItem);
							});
						}
						GHMyself self = github.getMyself();
						// Repos I own
						Map<String, GHRepository> myPublic = self.getAllRepositories();
						HashMap<String, Menu> myownerMenue = new HashMap<>();
						for (Map.Entry<String, GHRepository> entry : myPublic.entrySet()) {
							GHRepository g = entry.getValue();
							if (myownerMenue.get(g.getOwnerName()) == null) {
								myownerMenue.put(g.getOwnerName(), new Menu(g.getOwnerName()));
								Platform.runLater(() -> {
									myRepos.getItems().add(myownerMenue.get(g.getOwnerName()));
								});
							}
							setUpRepoMenue(myownerMenue.get(g.getOwnerName()), g);
						}
						// Watched repos
						PagedIterable<GHRepository> watching = self.listSubscriptions();
						HashMap<String, Menu> ownerMenue = new HashMap<>();
						for (GHRepository g : watching) {
							if (ownerMenue.get(g.getOwnerName()) == null) {
								ownerMenue.put(g.getOwnerName(), new Menu(g.getOwnerName()));
								Platform.runLater(() -> {
									watchingRepos.getItems().add(ownerMenue.get(g.getOwnerName()));
								});
							}
							setUpRepoMenue(ownerMenue.get(g.getOwnerName()), g);
						}

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}.start();

		});
	}

	private void setUpRepoMenue(Menu repoMenue, GHRepository repo) {
		new Thread() {
			public void run() {

				Menu orgRepo = new Menu(repo.getFullName());
				Menu orgFiles = new Menu("Files");
				MenuItem loading = new MenuItem("Loading...");
				MenuItem addFile = new MenuItem("Add file to Git Repo...");
				addFile.setOnAction(event -> {
					new Thread() {
						public void run() {
							// TODO add the implementation of add file, make
							// sure its modular to be reused elsewhere

						}
					}.start();
				});

				Platform.runLater(() -> {
					orgFiles.getItems().add(loading);
					orgRepo.getItems().addAll(addFile, orgFiles);
				});

				String url = repo.getGitTransportUrl().replace("git://", "https://");
				EventHandler<Event> loadFiles = new EventHandler<Event>() {
					boolean gistFlag = false;

					@Override
					public void handle(Event ev) {

						// for(ScriptingEngine.)
						new Thread() {
							public void run() {

								ThreadUtil.wait(500);
								if (!orgFiles.isShowing())
									return;
								if (gistFlag)
									return;// another thread is
											// servicing this gist
								gistFlag = true;
								System.out.println(
										"Loading files for " + repo.getFullName() + " " + repo.getDescription());
								ArrayList<String> listofFiles;
								try {
									listofFiles = ScriptingEngine.filesInGit(url, "master", null);
									System.out.println("Clone Done for " + url + listofFiles.size() + " files");
								} catch (Exception e1) {
									e1.printStackTrace();
									return;
								}
								if (orgFiles.getItems().size() != 1) {
									Log.warning("Bailing out of loading thread");
									return;// menue populated by
											// another thread
								}

								for (String s : listofFiles) {
									// System.out.println("Adding file: "+s);
									MenuItem tmp = new MenuItem(s);
									tmp.setOnAction(event -> {
										new Thread() {
											public void run() {
												try {
													File fileSelected = ScriptingEngine.fileFromGit(url, s);
													BowlerStudio.createFileTab(fileSelected);
												} catch (Exception e) {
													// TODO
													// Auto-generated
													// catch block
													e.printStackTrace();
												}
											}
										}.start();

									});
									Platform.runLater(() -> {
										orgFiles.getItems().add(tmp);
										// removing this listener
										// after menue is activated
										// for the first time
										orgFiles.setOnShowing(null);

									});

								}
								System.out.println("Refreshing menu");
								Platform.runLater(() -> {
									orgFiles.hide();
									orgFiles.getItems().remove(loading);
									Platform.runLater(() -> {
										orgFiles.show();
									});
								});
							}
						}.start();
					}
				};
				orgFiles.setOnShowing(loadFiles);
				Platform.runLater(() -> {
					repoMenue.getItems().add(orgRepo);
				});

			}
		}.start();
	}

	public void setToLoggedOut() {
		Platform.runLater(() -> {
			myGists.getItems().clear();
			logoutGithub.disableProperty().set(true);
			logoutGithub.setText("Anonymous");
		});
	}

	// /**
	// * Returns the location of the Jar archive or .class file the specified
	// * class has been loaded from. <b>Note:</b> this only works if the class
	// is
	// * loaded from a jar archive or a .class file on the locale file system.
	// *
	// * @param cls
	// * class to locate
	// * @return the location of the Jar archive the specified class comes from
	// */
	// public static File getClassLocation(Class<?> cls) {
	//
	// // VParamUtil.throwIfNull(cls);
	// String className = cls.getName();
	// ClassLoader cl = cls.getClassLoader();
	// URL url = cl.getResource(className.replace(".", "/") + ".class");
	//
	// String urlString = url.toString().replace("jar:", "");
	//
	// if (!urlString.startsWith("file:")) {
	// throw new IllegalArgumentException("The specified class\"" +
	// cls.getName()
	// + "\" has not been loaded from a location" + "on the local filesystem.");
	// }
	//
	// urlString = urlString.replace("file:", "");
	// urlString = urlString.replace("%20", " ");
	//
	// int location = urlString.indexOf(".jar!");
	//
	// if (location > 0) {
	// urlString = urlString.substring(0, location) + ".jar";
	// } else {
	// // System.err.println("No Jar File found: " + cls.getName());
	// }
	//
	// return new File(urlString);
	// }

	@FXML
	public void onLoadFile(ActionEvent e) {
		new Thread() {
			public void run() {
				setName("Load File Thread");
				openFile = FileSelectionFactory.GetFile(ScriptingEngine.getLastFile(),
						new ExtensionFilter("Groovy Scripts", "*.groovy", "*.java", "*.txt"),
						new ExtensionFilter("Clojure", "*.cloj", "*.clj", "*.txt", "*.clojure"),
						new ExtensionFilter("Python", "*.py", "*.python", "*.txt"),
						new ExtensionFilter("DXF", "*.dxf", "*.DXF"),
						new ExtensionFilter("GCODE", "*.gcode", "*.nc", "*.ncg", "*.txt"),
						new ExtensionFilter("Image", "*.jpg", "*.jpeg", "*.JPG", "*.png", "*.PNG"),
						new ExtensionFilter("STL", "*.stl", "*.STL", "*.Stl"), new ExtensionFilter("All", "*.*"));
				if (openFile == null) {
					return;
				}
				ArrayList<Polygon> points = TwoDCadFactory.pointsFromFile(openFile);
				if (null != points) {
					getApplication().addTab(new TwoDCad(points), true);
					return;
				}
				getApplication().createFileTab(openFile);
			}
		}.start();
	}

	@FXML
	public void onConnect(ActionEvent e) {
		new Thread() {
			public void run() {
				setName("Load BowlerDevice Dialog Thread");
				ConnectionManager.addConnection();
			}
		}.start();
	}

	@FXML
	public void onConnectVirtual(ActionEvent e) {

		ConnectionManager.addConnection(new VirtualGenericPIDDevice(10000), "virtual");
	}

	@FXML
	public void onClose(ActionEvent e) {
		System.exit(0);
	}

	public TextArea getLogView() {
		return logViewRef;
	}

	public void disconnect() {
		try {
			getApplication().disconnect();
		} catch (NullPointerException ex) {

		}
	}

	public void openUrlInNewTab(URL url) {
		getApplication().openUrlInNewTab(url);
	}

	@FXML
	public void onConnectCHDKCamera(ActionEvent event) {
		Platform.runLater(() -> {
			try {
				ConnectionManager.addConnection(new CHDKImageProvider(), "cameraCHDK");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	@FXML
	public void onConnectCVCamera(ActionEvent event) {

		Platform.runLater(() -> ConnectionManager.onConnectCVCamera());

	}

	//
	// public void onConnectJavaCVCamera() {
	//
	// Platform.runLater(() -> ConnectionManager.onConnectJavaCVCamera());
	//
	// }

	@FXML
	public void onConnectFileSourceCamera(ActionEvent event) {
		Platform.runLater(() -> ConnectionManager.onConnectFileSourceCamera());

	}

	@FXML
	public void onConnectURLSourceCamera(ActionEvent event) {

		Platform.runLater(() -> ConnectionManager.onConnectURLSourceCamera());

	}

	@FXML
	public void onConnectHokuyoURG(ActionEvent event) {
		Platform.runLater(() -> ConnectionManager.onConnectHokuyoURG());

	}

	@FXML
	public void onConnectGamePad(ActionEvent event) {
		Platform.runLater(() -> ConnectionManager.onConnectGamePad("gamepad"));

	}

	// public CheckMenuItem getAddVRCamera() {
	// return AddVRCamera;
	// }
	//
	//
	// public void setAddVRCamera(CheckMenuItem addVRCamera) {
	// AddVRCamera = addVRCamera;
	// }
	//
	//
	// public CheckMenuItem getAddDefaultRightArm() {
	// return AddDefaultRightArm;
	// }
	//
	//
	// public void setAddDefaultRightArm(CheckMenuItem addDefaultRightArm) {
	// AddDefaultRightArm = addDefaultRightArm;
	// }

	@FXML
	public void onLogin(ActionEvent event) {
		new Thread() {
			public void run() {
				ScriptingEngine.setLoginManager(new GitHubLoginManager());
				setName("Login Gist Thread");
				try {
					ScriptingEngine.login();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();

	}

	@FXML
	public void onLogout(ActionEvent event) {
		try {
			ScriptingEngine.logout();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	public void onConnectPidSim(ActionEvent event) {
		LinearPhysicsEngine eng = new LinearPhysicsEngine();
		eng.connect();
		ConnectionManager.addConnection(eng, "engine");
	}

	@FXML
	public void onPrint(ActionEvent event) {
		NRPrinter printer = (NRPrinter) ConnectionManager.pickConnectedDevice(NRPrinter.class);
		if (printer != null) {
			// run a print here
		}

	}

	@FXML
	public void onMobileBaseFromFile(ActionEvent event) {
		new Thread() {
			public void run() {
				setName("Load Mobile Base Thread");
				openFile = FileSelectionFactory.GetFile(ScriptingEngine.getLastFile(),
						new ExtensionFilter("MobileBase XML", "*.xml", "*.XML"));

				if (openFile == null) {
					return;
				}
				Platform.runLater(() -> {
					try {
						MobileBase mb = new MobileBase(new FileInputStream(openFile));
						ConnectionManager.addConnection(mb, mb.getScriptingName());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			}
		}.start();

	}

	// public Menu getCreatureLabMenue() {
	// return CreatureLabMenue;
	// }
	//
	// public void setCreatureLabMenue(Menu creatureLabMenue) {
	// CreatureLabMenue = creatureLabMenue;
	// }

	public void loadMobilebaseFromGist(String id, String file) {
		loadMobilebaseFromGit("https://gist.github.com/" + id + ".git", file);
	}

	public void loadMobilebaseFromGit(String id, String file) {
		new Thread() {
			public void run() {
				try {
					// BowlerStudio.openUrlInNewTab(new
					// URL("https://gist.github.com/" + id));
					String xmlContent = ScriptingEngine.codeFromGit(id, file)[0];
					MobileBase mb = new MobileBase(IOUtils.toInputStream(xmlContent, "UTF-8"));

					mb.setGitSelfSource(new String[] { id, file });
					ConnectionManager.addConnection(mb, mb.getScriptingName());

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();

	}

	@FXML
	public void onMobileBaseFromGist(ActionEvent event) {

		PromptForGit.prompt("Select a Creature From a Gist", "bcb4760a449190206170", (gitsId, file) -> {
			loadMobilebaseFromGist(gitsId, file);
		});
	}

	public ScriptingFileWidget createFileTab(File file) {
		return getApplication().createFileTab(file);
	}

	public BowlerStudioController getApplication() {
		return application;
	}

	public void setApplication(BowlerStudioController application) {
		this.application = application;
	}

	@FXML
	public void onCreatenewGist(ActionEvent event) {
		Stage s = new Stage();
		new Thread() {
			public void run() {
				NewGistController controller = new NewGistController();
				try {
					controller.start(s);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	@FXML
	public void onOpenGitter(ActionEvent event) {
		String url = "https://gitter.im";
		try {
			BowlerStudio.openUrlInNewTab(new URL(url));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	public void clearScriptCache(ActionEvent event) {
		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Are you sure you have published all your work?");
			alert.setHeaderText("This will wipe out the local cache");
			alert.setContentText("All files that are not published will be deleted");

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				new Thread() {
					public void run() {
						File cache = new File(ScriptingEngine.getWorkspace().getAbsolutePath() + "/gistcache/");
						deleteFolder(cache);
					}
				}.start();
			} else {
				System.out.println("Nothing was deleted");
			}
		});

	}

	private static void deleteFolder(File folder) {

		System.out.println("Deleting " + folder.getAbsolutePath());
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

	@FXML
	public void onMobileBaseFromGit(ActionEvent event) {
		PromptForGit.prompt("Select a Creature From a Git", "https://gist.github.com/bcb4760a449190206170.git",
				(gitsId, file) -> {
					loadMobilebaseFromGit(gitsId, file);
				});
	}

	@FXML
	void onSaveConfiguration(ActionEvent event) {
		System.err.println("Saving database");
		new Thread() {
			public void run() {

				ConfigurationDatabase.save();
			}
		}.start();
	}

}

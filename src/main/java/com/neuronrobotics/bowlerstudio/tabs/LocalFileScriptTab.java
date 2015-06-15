package com.neuronrobotics.bowlerstudio.tabs;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.Change;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;

import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.fxmisc.richtext.StyleSpansBuilder;

import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.dyio.DyIO;
import com.neuronrobotics.bowlerstudio.ConnectionManager;
import com.neuronrobotics.bowlerstudio.PluginManager;
import com.neuronrobotics.bowlerstudio.scripting.IScriptEventListener;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngineWidget;

import javafx.scene.control.Tab;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class LocalFileScriptTab extends Stage implements IScriptEventListener, EventHandler<WindowEvent> {
	
	private ScriptingEngineWidget scripting;
    private static final String[] KEYWORDS = new String[]{
        "def", "in", "as", "abstract", "assert", "boolean", "break", "byte",
        "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else",
        "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import",
        "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws",
        "transient", "try", "void", "volatile", "while"
    };
    IScriptEventListener l=null;

    private static final Pattern KEYWORD_PATTERN
            = Pattern.compile("\\b(" + String.join("|", KEYWORDS) + ")\\b");
    
    
    //private final CodeArea codeArea = new CodeArea();
	private VBox vBox;
	private RSyntaxTextArea textArea;

    
	public LocalFileScriptTab(ConnectionManager connectionManager, File file) throws IOException {
		scripting = new ScriptingEngineWidget( file );
		setOnCloseRequest(this);
		setTitle(file.getName());
		l=this;


		scripting.addIScriptEventListener(l);
		
		textArea = new RSyntaxTextArea(20, 60);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
		textArea.setCodeFoldingEnabled(true);
		textArea.setText(scripting.getCode());
		textArea.getDocument().addDocumentListener(new DocumentListener() {

	        @Override
	        public void removeUpdate(DocumentEvent e) {

	        }

	        @Override
	        public void insertUpdate(DocumentEvent e) {
	        	
	        }

	        @Override
	        public void changedUpdate(DocumentEvent arg0) {
            	scripting.removeIScriptEventListener(l);
            	scripting.setCode(textArea.getText());
            	scripting.addIScriptEventListener(l);
	        }
	    });
		RTextScrollPane sp = new RTextScrollPane(textArea);
		SwingNode sn = new SwingNode();
		SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	sn.setContent(sp);
            	
            }
        });

		HBox hBox = new HBox(5);
		hBox.getChildren().setAll(sn);
		HBox.setHgrow(sn, Priority.ALWAYS);

		vBox = new VBox(5);
		vBox.getChildren().setAll(hBox, scripting);
		VBox.setVgrow(sn, Priority.ALWAYS);
		Group g = new Group(vBox);
		g.setAutoSizeChildren(true);
		Scene scene = new Scene(g);
		setScene(scene);
		
		widthProperty().addListener((w,o,n)->{
			sp.setSize((int)getWidth(), (int)getHeight());
			sn.resize((int)getWidth(), (int)getHeight());
			textArea.setSize((int)getWidth(), (int)getHeight());
			//System.err.println("Resize width");

		});
		
		heightProperty().addListener((w,o,n)->{
			sp.setSize((int)getWidth(), (int)getHeight());
			sn.resize((int)getWidth(), (int)getHeight());
			textArea.setSize((int)getWidth(), (int)getHeight());
			//System.err.println("Resize height");
		});
		setHeight(480);
		setWidth(640);
		show();
	      
	}
	
	


	@Override
	public void onGroovyScriptFinished(	Object result,Object previous) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onGroovyScriptChanged(String previous, String current) {
//		 Cursor place = codeArea.getCursor();
//		 codeArea.replaceText(current);
//		 codeArea.setCursor(place);
		Platform.runLater(()->{
			textArea.setText(current);
		});
		
	}


	@Override
	public void onGroovyScriptError(Exception except) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handle(WindowEvent event) {
		// TODO Auto-generated method stub
		scripting.stop();
	}
}

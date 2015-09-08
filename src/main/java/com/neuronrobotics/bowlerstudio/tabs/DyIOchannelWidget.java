package com.neuronrobotics.bowlerstudio.tabs;

import java.io.PrintWriter;
import java.io.StringWriter;



import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;



import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;



import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.ShellType;
import com.neuronrobotics.nrconsole.plugin.DyIO.DyIOConsole;
import com.neuronrobotics.sdk.dyio.DyIOChannel;
import com.neuronrobotics.sdk.dyio.DyIOChannelEvent;
import com.neuronrobotics.sdk.dyio.DyIOChannelMode;
import com.neuronrobotics.sdk.dyio.IChannelEventListener;
import com.neuronrobotics.sdk.dyio.peripherals.ServoChannel;



import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class DyIOchannelWidget {

	private final class ChangeListenerImplementation implements
			ChangeListener<Number> {
		public void changed(ObservableValue<? extends Number> ov,
				Number old_val, Number new_val) {
			int newVal = new_val.intValue();
			chanValue.setText(new Integer(newVal).toString());
			if(currentMode==DyIOChannelMode.SERVO_OUT && timeSlider.getValue()>.1){
				//servo should only set on release when time is defined
				return;
			}
			channel.setValue(newVal);
		}
	}

	@FXML Button setListenerButton;
	@FXML AnchorPane listenerCodeBox;
	@FXML LineChart<Integer,Integer> channelGraph;
	@FXML Slider timeSlider;
	@FXML Slider positionSlider;
	@FXML Label chanValue;
	@FXML Label deviceType;
	@FXML Label deviceNumber;
	@FXML ImageView deviceModeIcon;
	private DyIOChannel channel;
	private  XYChart.Series<Integer, Integer> series = new XYChart.Series<Integer, Integer>();
	private long startTime=0;
	@FXML Label secondsLabel;
	private IChannelEventListener myLocalListener=null;
	private RSyntaxTextArea textArea;
	private SwingNode sn;
	private RTextScrollPane sp;
	private ServoChannel srv= null;
	private DyIOChannelMode currentMode;
	private ChangeListenerImplementation imp = new ChangeListenerImplementation();
	
	public void setChannel(DyIOChannel chan){
		this.channel = chan;
		startTime=System.currentTimeMillis();
		setMode( chan.getMode());
		deviceNumber.setText(new Integer(chan.getChannelNumber()).toString());
		chanValue.setText(new Integer(chan.getValue()).toString());
		secondsLabel.setText(String.format("%.2f", 0.0));
		positionSlider.setValue(chan.getValue());
	
		positionSlider.valueProperty().addListener(imp);
		
		positionSlider.valueChangingProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {

			chanValue.setText(new Integer((int) positionSlider.getValue()).toString());
			if(currentMode==DyIOChannelMode.SERVO_OUT && timeSlider.getValue()>.1){
				srv.SetPosition((int) positionSlider.getValue(), timeSlider.getValue());
			}
			
		});
		timeSlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				secondsLabel.setText(String.format("%.2f", new_val));
			}
		});
		channel.addChannelModeChangeListener(newMode -> {
			setMode( newMode);
		});
		
		
		channel.addChannelEventListener(new IChannelEventListener() {
			@Override
			public void onChannelEvent(DyIOChannelEvent dyioEvent) {
				Platform.runLater(()->{
					chanValue.setText(new Integer(dyioEvent.getValue()).toString());
					positionSlider.valueProperty().removeListener(imp);
					positionSlider.setValue(dyioEvent.getValue());
					positionSlider.valueProperty().addListener(imp);
					if(series.getData().size()>200){
						series.getData().remove(0);
					}
			        series.getData().add(new XYChart.Data<Integer, Integer>(
			        		(int) (System.currentTimeMillis()-startTime),
			        		dyioEvent.getValue())
			        		);
			   
				});
				
			}
		});
		Platform.runLater(()->
		channelGraph.getData().add(series));

		setUpListenerPanel();
		Platform.runLater(()->setListenerButton.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY))));
		
	}
	
	private void setMode(DyIOChannelMode newMode){
		Image image;
		currentMode = newMode;
		try {
			image = new Image(
					DyIOConsole.class
							.getResourceAsStream("images/icon-"
									+ currentMode.toSlug()+ ".png"));
		} catch (NullPointerException e) {
			image = new Image(
					DyIOConsole.class
							.getResourceAsStream("images/icon-off.png"));
		}
		Image i = image;
		Platform.runLater(()->{

			deviceModeIcon.setImage(i);	
			series.setName(currentMode.toSlug()+" values");
			deviceType.setText(currentMode.toSlug());
			//set slider bounds
			switch(currentMode){
			case ANALOG_IN:
				positionSlider.setMin(0);
				positionSlider.setMax(1024);
				break;
			case COUNT_IN_DIR:
			case COUNT_IN_HOME:
			case COUNT_IN_INT:
			case COUNT_OUT_DIR:
			case COUNT_OUT_HOME:
			case COUNT_OUT_INT:
				positionSlider.setMin(-5000);
				positionSlider.setMax(-5000);
				break;
			case DC_MOTOR_DIR:			
			case DC_MOTOR_VEL:
			case PWM_OUT:
			case SERVO_OUT:
				positionSlider.setMin(0);
				positionSlider.setMax(255);
				break;
			case DIGITAL_IN:
			case DIGITAL_OUT:
				positionSlider.setMin(0);
				positionSlider.setMax(1);
				break;
			default:
				break;
			
			}
			// allow slider to be disabled for inputs
			switch(currentMode){
			case ANALOG_IN:
			case COUNT_IN_DIR:
			case COUNT_IN_HOME:
			case COUNT_IN_INT:
			case DIGITAL_IN:
				timeSlider.setDisable(true);
				positionSlider.setDisable(true);
				break;
	
			default:
				timeSlider.setDisable(false);
				positionSlider.setDisable(false);
				break;
			
			}
			
			if(currentMode==DyIOChannelMode.SERVO_OUT && srv==null){
				srv = new ServoChannel(channel);
			}
		});
		
	}

	@FXML public void onListenerButtonClicked(ActionEvent event) {
		try{
			if(myLocalListener==null){
				Platform.runLater(()->{
					sn.setDisable(true);
					textArea.setEditable(false);
				});
				myLocalListener=(IChannelEventListener) ScriptingEngine.inlineScriptRun(textArea.getText(), null, ShellType.GROOVY);
				channel.addChannelEventListener(myLocalListener);
				Platform.runLater(()->{
					setListenerButton.setText("Kill Listener");
					setListenerButton.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
				});
			}else{
				Platform.runLater(()->{
					sn.setDisable(false);
					textArea.setEditable(true);
					channel.removeChannelEventListener(myLocalListener);
					myLocalListener=null;
					setListenerButton.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
					setListenerButton.setText("Set Listener");
				});
			}
		}catch(Exception e){
			  StringWriter sw = new StringWriter();
		      PrintWriter pw = new PrintWriter(sw);
		      e.printStackTrace(pw);
		      System.out.println(sw.toString());
		}
		
	}
	
	private void setUpListenerPanel(){
		Platform.runLater(()->{
			textArea = new RSyntaxTextArea(15, 80);
			textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
			textArea.setCodeFoldingEnabled(true);
			textArea.setText("return new IChannelEventListener() { \n"+
				"\tpublic \n"
				+ "\tvoid onChannelEvent(DyIOChannelEvent dyioEvent){\n"+
				"\t\tprintln \"From Listener=\"+dyioEvent.getValue();\n"+
				"\t}\n"+
			"}"
				);
			sp = new RTextScrollPane(textArea);
			
			sn = new SwingNode();
			SwingUtilities.invokeLater(new Runnable() {
	            @Override
	            public void run() {
	            	sn.setContent(sp);
	            }
	        });
			
			listenerCodeBox.getChildren().setAll(sn);
			sn.setOnMouseEntered(mouseEvent -> {
				sn.requestFocus();
				SwingUtilities.invokeLater(new Runnable() {
		            @Override
		            public void run() {
		            	textArea.requestFocusInWindow();
		            }
		        });
			});
		});
	}

}

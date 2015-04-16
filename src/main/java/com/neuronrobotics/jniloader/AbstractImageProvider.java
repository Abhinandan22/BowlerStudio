package com.neuronrobotics.jniloader;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;

public abstract class AbstractImageProvider {
	private BufferedImage image = null;
	private MatOfByte mb = new MatOfByte();
	/**
	 * This method should capture a new image and load it into the Mat datatype
	 * @param imageData
	 * @return
	 */
	protected abstract boolean captureNewImage(Mat imageData);
	
	
	public BufferedImage getLatestImage(Mat inputImage, Mat displayImage){
		captureNewImage(inputImage);
//		try {
//		    // retrieve image
//		    BufferedImage bi = matToBufferedImage(inputImage);
//		    File outputfile = new File("Robot_Log_Image"+System.currentTimeMillis()+".png");
//		    ImageIO.write(bi, "png", outputfile);
//		    
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		inputImage.copyTo(displayImage);
		
		Highgui.imencode(".jpg", inputImage, mb);
		try {
			image = ImageIO.read(new ByteArrayInputStream(mb.toArray()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return image;
	}
	
	public BufferedImage getLatestImage(){
		return image;
	}
	
	/**
	 * Converts/writes a Mat into a BufferedImage.
	 * 
	 * @param matrix
	 *            Mat of type CV_8UC3 or CV_8UC1
	 * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
	 */
	public static BufferedImage matToBufferedImage(Mat matrix) {
		int cols = matrix.cols();
		int rows = matrix.rows();
		int elemSize = (int) matrix.elemSize();
		byte[] data = new byte[cols * rows * elemSize];
		int type;
		matrix.get(0, 0, data);
		switch (matrix.channels()) {
		case 1:
			type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		case 3:
			type = BufferedImage.TYPE_3BYTE_BGR;
			// bgr to rgb
			byte b;
			for (int i = 0; i < data.length; i = i + 3) {
				b = data[i];
				data[i] = data[i + 2];
				data[i + 2] = b;
			}
			break;
		default:
			return null;
		}
		BufferedImage image2 = new BufferedImage(cols, rows, type);
		image2.getRaster().setDataElements(0, 0, cols, rows, data);
		return image2;
	}
	
	public static void bufferedImageToMat(BufferedImage input, Mat output){
		MatOfByte mb;

		try {
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(input, "jpg", baos);
			baos.flush();
			byte[] tmpByteArray = baos.toByteArray();
			baos.close();
			mb = new MatOfByte(tmpByteArray);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	
		Mat matImageLocal =Highgui.imdecode(mb, 0);
		matImageLocal.copyTo(output);
	}

	public static  BufferedImage toGrayScale(BufferedImage in, int w, int h) {
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = bi.createGraphics();
		g.drawImage(in, 0, 0, w, h, null);
		return bi;
	}

	public  static BufferedImage toGrayScale(BufferedImage in, double scale) {
		int w = (int) (in.getWidth() * scale);
		int h = (int) (in.getHeight() * scale);
		return toGrayScale(in, w, h);
	}
}

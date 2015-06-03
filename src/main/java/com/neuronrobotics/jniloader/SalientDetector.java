// How to boxes in case more stuff goes bad http://stackoverflow.com/questions/26814069/how-to-set-region-of-interest-in-opencv-java
// ssdasd
package com.neuronrobotics.jniloader;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

public class SalientDetector implements IObjectDetector {

    @Override
    public List<Detection> getObjects(BufferedImage inImg, BufferedImage disp){
	
	int Horizon = 100;
	int minArea = 100;
	int maxArea = 700;
		
	Scalar RedBox = new Scalar(0, 0, 255);
			
	int threshMin = 50;
	int threshMax = 255;

	int MS_SR = 10; // spacial radius
	int MS_CR = 50; // color radius

	int Erode_Max = 2;
	int Erode_Min = 2;

	int Dilate_Max = 5;
	int Dilate_Min = 5;

	Mat erodeElement  = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(Erode_Min,Erode_Max));
	Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(Dilate_Min, Dilate_Max));

	int PyrSize = 3; // how many times to downsample

	ArrayList<Detection> ReturnedArea = new ArrayList<Detection>(); // areas
	ArrayList<Mat> RegionsOfInterest  = new ArrayList<Mat>();

	Mat inputImage  =  new Mat(); // original webcam image
	Mat MeanShift   =  new Mat(); // MeanShifted image to reduce noise
	Mat ObjFound    =  new Mat(); // Where stuff is found and red boxes drawn
	Mat Saliency    =  new Mat(); // Saliency of image

	Mat top = new Mat();          // top level of pyramid
	Mat bot = new Mat();          // meanshifted image to compare to

	Mat DS = new Mat();
	Mat UP = new Mat();

	AbstractImageProvider.bufferedImageToMat(inImg, inputImage);// ACCESS
			
	ObjFound = inputImage.clone();
	MeanShift = inputImage.clone();

	Imgproc.pyrMeanShiftFiltering(MeanShift, MeanShift, MS_SR, MS_CR);
	Imgproc.cvtColor(MeanShift, MeanShift, Imgproc.COLOR_BGR2GRAY);

	// Perform Pyramid Function ****************************************
	MeanShift.convertTo(MeanShift, CvType.CV_32F);
	DS = MeanShift.clone();

	for (int i = 0; i < PyrSize; i++) {
	    Mat a = new Mat();
	    Imgproc.pyrDown(DS, a);
	    DS = a.clone();
	}

	UP = DS.clone();

	for (int i = 0; i < PyrSize; i++) {
	    Mat a = new Mat();
	    Imgproc.pyrUp(UP, a);
	    UP = a.clone();
	}

	top = UP.clone();             // most downsampled then upsampled image
	bot = MeanShift.clone();
	Saliency = MeanShift.clone();
	// *******************************************************************

	
	// AbstractImageProvider.deepCopy(AbstractImageProvider.matToBufferedImage(top),disp);
	// http://answers.opencv.org/question/5/how-to-get-and-modify-the-pixel-of-mat-in-java/
	// http://www.tutorialspoint.com/java_dip/applying_weighted_average_filter.htm		
		
	int channelNumb = top.channels();

	float[] top_temp = new float[channelNumb];
	float[] bot_temp = new float[channelNumb];
	float[] sal_temp = new float[channelNumb];

	float top_min = (float) 1.0;
	float bot_min = (float) 1.0;

	for (int i = 0; i < top.rows(); i++) { // find the smallest value in
	    for (int j = 0; j < top.cols(); j++) { // this is so you can sub out
		top.get(i, j, top_temp);
		bot.get(i, j, bot_temp);
				
		float a = top_temp[0]; // Currently I'm using grayscale which is 1 channel therefore 0 is the only position you use.
		float b = bot_temp[0]; // If you are using multiple colorspaces go and add another for loop that is 0-channelNumb.
				
		if (a < top_min && a > 0) {top_min = a;} 
		if (b < bot_min && b > 0) {bot_min = b;}
	    }
	}

	for (int i = 0; i < top.rows(); i++){
	    for (int j = 0; j < top.cols(); j++){
		    	
		top.get(i, j, top_temp);
		bot.get(i, j, bot_temp);
						
		float a = top_temp[0];
		float b = bot_temp[0];
	
		if (a == 0) {a = top_min;}
		if (b == 0) {b = bot_min;}
	
		if (a <= b) {sal_temp[0] = (float) (1.0 - a / b);} 
		else        {sal_temp[0] = (float) (1.0 - b / a);}
					
		Saliency.put(i, j, sal_temp);
	    }
	}       // *******************************************************************************************************************

	Mat thr = new Mat(Saliency.rows(), Saliency.cols(), CvType.CV_8UC1);
	Saliency.convertTo(thr, CvType.CV_8UC1, 255); // change float to 1-255
	Imgproc.threshold(thr, Saliency, threshMin, threshMax, Imgproc.THRESH_BINARY); // turn to black and white

	Imgproc.erode(Saliency, Saliency, erodeElement);
	Imgproc.erode(Saliency, Saliency, erodeElement);
	Imgproc.dilate(Saliency, Saliency, dilateElement);
	Imgproc.dilate(Saliency, Saliency, dilateElement);

	ArrayList<MatOfPoint> contours     = new ArrayList<MatOfPoint>();           // all the shapes aka contours in image stored here
	ArrayList<MatOfPoint> contourFinal = new ArrayList<MatOfPoint>();
	ArrayList<Rect> boundRect          = new ArrayList<Rect>();

	if(contours.size() < 100){

	    while(true){            // sort by size
		int sortCount = 0;
		for(int i = 0; i < contours.size()-1; i++){
				    
		    MatOfPoint contourHold1, contourHold2;
		    double area1 = Imgproc.contourArea(contours.get(i), false);
		    double area2 = Imgproc.contourArea(contours.get(i+1), false);
				    
		    if (area1 < area2){
			contourHold1   =  contours.get(i);
			contourHold2   = contours.get(i+1);
			contours.set(i, contourHold2);
			contours.set(i+1, contourHold1);
			sortCount++;
		    }
		}
		if(sortCount == 0){break;}
	    }

	    for (int i = 0; i < contours.size(); i++){
		Rect rectHold = new Rect();
		rectHold = Imgproc.boundingRect(new MatOfPoint(contours.get(i)));
	
		if      (i == 0)                    {contourFinal.add(contours.get(i));}
		else if (rectHold.tl().y > Horizon) {contourFinal.add(contours.get(i));}
	    }

	    int FinalSize = contourFinal.size();
	    if (FinalSize > 15) {FinalSize = 15;} // if there's 16 objects, look for 15, otherwise look for 1-15

	    for(int i = -1; i < FinalSize; i++){
			
		Rect test = new Rect();
		double area;
		int newX = 100;
		int newY = 100;
	
		if (i == -1){
		    newY = 250;
		    newX = 250;
		    area = Imgproc.contourArea(contourFinal.get(0), false);             // found contour area
		    test = Imgproc.boundingRect(new MatOfPoint(contourFinal.get(0)));
		}
		else{
		    area = Imgproc.contourArea(contourFinal.get(i), false);             // found contour area
		    test = Imgproc.boundingRect(new MatOfPoint(contourFinal.get(i)));
		}

		if (area >= minArea){ // size check
				    
		    Boolean a = true;
				    
		    int oldX = (int) (test.br().x - (test.width/2));
		    int oldY = (int) (test.br().y - (test.height/2));
				    
		    if (test.br().y > Horizon && test.width < newX && test.height < newY){
					
			test.width = newX;
			test.height = newY;
						
			int nX = (int) (test.br().x - (test.width/2));
			int nY = (int) (test.br().y - (test.height/2));
						
			int shiftX = nX - oldX;
			int shiftY = nY - oldY;

			test.x -= shiftX; 
			test.y -= shiftY;
						
			if (test.x <= 0 || test.y <= 0 || test.br().x >= newX || test.br().y >= newY){a = false;}
			if (a == true) {boundRect.add(test);}			
		    }		  
		}
	    }

	    for (int i = 0; i < boundRect.size(); i++) {
		Mat holder = new Mat();                        // hold the cropped 100x100
		Core.rectangle(ObjFound, boundRect.get(i).tl(), boundRect.get(i).br(), RedBox, 1, 8, 0); // make box in ObjFound
		holder = inputImage.submat(boundRect.get(i));  // put cropped 100x100 in holder
		RegionsOfInterest.add(holder);                 // put holder in array
	    }
	    AbstractImageProvider.deepCopy(AbstractImageProvider.matToBufferedImage(ObjFound), disp); // display input image + red boxes
	}
	 	
	for (int a = 0; a < RegionsOfInterest.size(); a++){ // process those small areas
			
	    Mat Interesting = new Mat();
	    Interesting     = RegionsOfInterest.get(a);
			
	    int ObjWidth    =  Interesting.cols();   // the first time it runs 250x250 
	    int ObjHeight   =  Interesting.rows();
	    int edgeOffset  =  5;                  
	    int contMinArea =  300;
	    int numbOfObj   =  8;
	    int ObjCent     =  125;
	    int centoffset  =  80;
			
	    if (ObjWidth == 100 && ObjHeight == 100){ // 100x100 squares
		contMinArea = 50;
		numbOfObj   = 4;
		centoffset  = 20;
		edgeOffset  = 20;
		ObjCent     = 50;
	    }
			
	    ArrayList <MatOfPoint> resultCont;
	    ArrayList <MatOfPoint> FinalContours;
						  
	    Mat colorResult;
	    Mat hsv1 = Interesting.clone();  // Range of color
	    
	    Scalar white_min1 = new Scalar(0,0,100);
	    Scalar white_max1 = new Scalar(180,100,256);
	
	    Scalar pink_min1 = new Scalar(120,0,0);  // very very very distinct
	    Scalar pink_max1 = new Scalar(180,0,256);

	    Scalar red_min1 = new Scalar(0,0,0);
	    Scalar red_max1 = new Scalar(5,256,256);

	    Scalar red_min2 = new Scalar(0,0,0);
	    Scalar red_max2 = new Scalar(10,256,256);
	    
	    Scalar yellow_min1 = new Scalar(0,0,0);
	    Scalar yellow_max1 = new Scalar(30,256,256);

	    Scalar grey_min1 = new Scalar(0,0,200);
	    Scalar grey_max1 = new Scalar(0,0, 256);
	
	    ArrayList<Scalar> colorRanges = new ArrayList<Scalar>(); // array of scalar min,max for confirmation

	    Imgproc.cvtColor(hsv1, colorResult, Imgproc.COLOR_BGR2HSV);
	    	    
	    Core.inRange(white_min1, white_max1, colorResult);
	    erode  (colorResult, colorResult,  erodeElement);
	    dilate (colorResult, colorResult, dilateElement);

	    Imgproc.findContours(colorResult, resultCont, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

	    if(!resultCont.isEmpty() && resultCont.size() <= numbOfObj){

	    }
	  	
	}
		
	return ReturnedArea;
    }

}

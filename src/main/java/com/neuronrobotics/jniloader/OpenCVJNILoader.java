package com.neuronrobotics.jniloader;

import javax.management.RuntimeErrorException;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class OpenCVJNILoader {
	static NativeResource resource=null;
	public static void load() {
		if( resource!=null)
			return;
		resource= new NativeResource();
		
		if(NativeResource.isLinux()){
			try{
				System.load("/usr/local/share/OpenCV/java/lib"+Core.NATIVE_LIBRARY_NAME+".so");
				
			}catch(Error e){
				System.load("/usr/lib/jni/lib"+Core.NATIVE_LIBRARY_NAME+".so");
			}
		}else
		if(NativeResource.isOSX())
			resource.load("lib"+Core.NATIVE_LIBRARY_NAME);
		else if(NativeResource.isWindows()){
			String dir = "OpenCV-"+Core.VERSION.split(".0")[0];
			if(NativeResource.is64Bit()){
				System.load("C:\\"+dir+"\\build\\java\\x64\\"+Core.NATIVE_LIBRARY_NAME+".dll");
			}else{
				System.load("C:\\"+dir+"\\build\\java\\x86\\"+Core.NATIVE_LIBRARY_NAME+".dll");
			}
		}else{
			throw new RuntimeErrorException(null);
		}
		
		Mat m  = Mat.eye(3, 3, CvType.CV_8UC1);
        //System.out.println("m = " + m.dump());
		
	}

}

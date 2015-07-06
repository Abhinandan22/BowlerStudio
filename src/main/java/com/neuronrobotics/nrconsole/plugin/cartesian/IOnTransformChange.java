package com.neuronrobotics.nrconsole.plugin.cartesian;

import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

public interface IOnTransformChange {
	public abstract void onTransformChaging(TransformNR newTrans);
	public abstract void onTransformFinished(TransformNR newTrans);
}

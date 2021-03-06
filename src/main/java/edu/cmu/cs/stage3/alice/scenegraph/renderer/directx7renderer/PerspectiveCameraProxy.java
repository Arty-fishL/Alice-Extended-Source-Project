package edu.cmu.cs.stage3.alice.scenegraph.renderer.directx7renderer;

class PerspectiveCameraProxy extends edu.cmu.cs.stage3.alice.scenegraph.renderer.nativerenderer.PerspectiveCameraProxy {
	// from ElementProxy

	@Override
	protected native void createNativeInstance();

	@Override
	protected native void releaseNativeInstance();
	// from ComponentProxy

	@Override
	protected native void onAbsoluteTransformationChange(javax.vecmath.Matrix4d m);

	@Override
	protected native void addToScene(edu.cmu.cs.stage3.alice.scenegraph.renderer.nativerenderer.SceneProxy scene);

	@Override
	protected native void removeFromScene(edu.cmu.cs.stage3.alice.scenegraph.renderer.nativerenderer.SceneProxy scene);
	// from CameraProxy

	@Override
	protected native void onNearClippingPlaneDistanceChange(double value);

	@Override
	protected native void onFarClippingPlaneDistanceChange(double value);

	@Override
	protected native void onBackgroundChange(
			edu.cmu.cs.stage3.alice.scenegraph.renderer.nativerenderer.BackgroundProxy value);
	// from PerspectiveCameraProxy

	@Override
	protected native void onPlaneChange(double left, double top, double right, double bottom);
}

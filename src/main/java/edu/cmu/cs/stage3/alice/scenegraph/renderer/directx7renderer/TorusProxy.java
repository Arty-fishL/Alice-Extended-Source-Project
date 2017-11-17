package edu.cmu.cs.stage3.alice.scenegraph.renderer.directx7renderer;

class TorusProxy extends edu.cmu.cs.stage3.alice.scenegraph.renderer.nativerenderer.TorusProxy {
	// from ElementProxy

	@Override
	protected native void createNativeInstance();

	@Override
	protected native void releaseNativeInstance();
	// from GeometryProxy

	@Override
	protected native void onBoundChange(double x, double y, double z, double radius);
	// from TorusProxy

	@Override
	protected native void onInnerRadiusChange(double value);

	@Override
	protected native void onOuterRadiusChange(double value);
}
package org.opencv.samples.colorblobdetect;

public interface IUsbConnectionHandler {

	void onUsbStopped();

	void onErrorLooperRunningAlready();

	void onDeviceNotFound();
}

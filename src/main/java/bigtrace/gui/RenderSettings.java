package bigtrace.gui;


import ij.Prefs;

public class RenderSettings {
	
	/** screen render width for BVV **/
	public int nRenderW = (int) Prefs.get("BigTrace.nRenderW",800.0);
	
	/** screen render height for BVV **/
	public int nRenderH = (int) Prefs.get("BigTrace.nRenderH",600.0);
	
	/** dither width BVV **/
	public int ditherWidth = (int) Prefs.get("BigTrace.ditherWidth",3);
	
	public int numDitherSamples = (int) Prefs.get("BigTrace.numDitherSamples",8);

	public int cacheBlockSize = (int) Prefs.get("BigTrace.cacheBlockSize",32);

	public int maxCacheSizeInMB = (int) Prefs.get("BigTrace.maxCacheSizeInMB",300);
	
	public RenderSettings()
	{
		
	}
}

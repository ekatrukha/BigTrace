package bigtrace;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.Prefs;


@Plugin(type = Command.class, menuPath = "Plugins>BigTrace 0.3.2>Render Settings")
public class BigTraceRenderSettings implements Command {

	@Parameter( label = "Render width" )
	private int renderWidth = (int) Prefs.get("BigTrace.nRenderW",800.0);

	@Parameter( label = "Render height" )
	private int renderHeight = (int) Prefs.get("BigTrace.nRenderH",600.0);

	@Parameter( label = "Dither window size",
			choices = { "none (always render full resolution)", "2x2", "3x3", "4x4", "5x5", "6x6", "7x7", "8x8" } )
	private String dithering = "3x3";

	@Parameter( label = "Number of dither samples",
			description = "Pixels are interpolated from this many nearest neighbors when dithering. This is not very expensive, it's fine to turn it up to 8.",
			min="1",
			max="8",
			style="slider")
	private int numDitherSamples = 8;

	@Parameter( label = "GPU cache tile size" )
	private int cacheBlockSize = 32;

	@Parameter( label = "GPU cache size (in MB)",
				description = "The size of the GPU cache texture will match this as close as possible with the given tile size." )
	private int maxCacheSizeInMB = 300;
	@Override
	public void run() {
		
		
		final int ditherWidth;
		switch ( dithering )
		{
		case "none (always render full resolution)":
		default:
			ditherWidth = 1;
			break;
		case "2x2":
			ditherWidth = 2;
			break;
		case "3x3":
			ditherWidth = 3;
			break;
		case "4x4":
			ditherWidth = 4;
			break;
		case "5x5":
			ditherWidth = 5;
			break;
		case "6x6":
			ditherWidth = 6;
			break;
		case "7x7":
			ditherWidth = 7;
			break;
		case "8x8":
			ditherWidth = 8;
			break;
		}
		
		Prefs.set("BigTrace.nRenderW", renderWidth);
		Prefs.set("BigTrace.nRenderH", renderHeight);
		Prefs.set("BigTrace.ditherWidth", ditherWidth);
		Prefs.set("BigTrace.numDitherSamples", numDitherSamples);
		Prefs.set("BigTrace.cacheBlockSize", cacheBlockSize);
		Prefs.set("BigTrace.maxCacheSizeInMB", maxCacheSizeInMB);
	}

}

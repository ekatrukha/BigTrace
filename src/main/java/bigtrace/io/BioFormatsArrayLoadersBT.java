package bigtrace.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import bdv.img.cache.CacheArrayLoader;
import ch.epfl.biop.bdv.img.ResourcePool;
import loci.formats.IFormatReader;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;

public class BioFormatsArrayLoadersBT {

	/**
	 * Generic class with the necessary elements to read and load pixels
	 */
	abstract static class BioformatsArrayLoader {

		final protected ResourcePool<IFormatReader> readerPool;
		final protected int channel;
		final protected int iSeries;

		private BioformatsArrayLoader(ResourcePool<IFormatReader> readerPool, int channel, int iSeries)
		{
			this.readerPool = readerPool;
			this.channel = channel;
			this.iSeries = iSeries;
		}

	}

	/**
	 * Class explaining how to read and load pixels of type : Unsigned Byte (8 bits)
	 */
	protected static class BioFormatsUnsignedByteArrayLoader extends
		BioformatsArrayLoader implements CacheArrayLoader<VolatileByteArray>
	{

		protected BioFormatsUnsignedByteArrayLoader(ResourcePool<IFormatReader> readerPool,
			int channel, int iSeries)
		{
			super(readerPool, channel, iSeries);
		}

		@Override
		public VolatileByteArray loadArray(int timepoint, int setup, int level,
										   int[] dimensions, long[] min) throws InterruptedException
		{
			try {
				// get the reader
				IFormatReader reader = readerPool.acquire();
				reader.setSeries(iSeries);
				reader.setResolution(level);
				int minX = (int) min[0];
				int minY = (int) min[1];
				int minZ = (int) min[2];
				int maxX = Math.min(minX + dimensions[0], reader.getSizeX());
				int maxY = Math.min(minY + dimensions[1], reader.getSizeY());
				int maxZ = Math.min(minZ + dimensions[2], reader.getSizeZ());
				int w = maxX - minX;
				int h = maxY - minY;
				int d = maxZ - minZ;
				int nElements = (w * h * d);

				// read pixels
				ByteBuffer buffer = ByteBuffer.allocate(nElements);
				for (int z = minZ; z < maxZ; z++) {
					byte[] bytesCurrentPlane = reader.openBytes(reader.getIndex(z, channel,
							timepoint), minX, minY, w, h);
					buffer.put(bytesCurrentPlane);
				}

				// release the reader
				readerPool.recycle(reader);
				return new VolatileByteArray(buffer.array(), true);
			}
			catch (Exception e) {
				throw new InterruptedException(e.getMessage());
			}
		}

		@Override
		public int getBytesPerElement() {
			return 1;
		}
	}
	
	/**
	 * Class explaining how to read and load pixels of type : Unsigned Byte (8 bits)
	 */
	protected static class BioFormatsUnsignedByteArrayToShortLoader extends
		BioformatsArrayLoader implements CacheArrayLoader<VolatileShortArray>
	{

		protected BioFormatsUnsignedByteArrayToShortLoader(ResourcePool<IFormatReader> readerPool,
			int channel, int iSeries)
		{
			super(readerPool, channel, iSeries);
		}

		@Override
		public VolatileShortArray loadArray(int timepoint, int setup, int level,
										   int[] dimensions, long[] min) throws InterruptedException
		{
			try {
				// get the reader
				IFormatReader reader = readerPool.acquire();
				reader.setSeries(iSeries);
				reader.setResolution(level);
				int minX = (int) min[0];
				int minY = (int) min[1];
				int minZ = (int) min[2];
				int maxX = Math.min(minX + dimensions[0], reader.getSizeX());
				int maxY = Math.min(minY + dimensions[1], reader.getSizeY());
				int maxZ = Math.min(minZ + dimensions[2], reader.getSizeZ());
				int w = maxX - minX;
				int h = maxY - minY;
				int d = maxZ - minZ;
				int nElements = (w * h * d);

				/*
				// read pixels
				ByteBuffer buffer = ByteBuffer.allocate(nElements);
				for (int z = minZ; z < maxZ; z++) {
					byte[] bytesCurrentPlane = reader.openBytes(reader.getIndex(z, channel,
							timepoint), minX, minY, w, h);
					buffer.put(bytesCurrentPlane);
				}

				// release the reader
				readerPool.recycle(reader);
				// unsigned short specific transform
				short[] shorts = new short[nElements];
				byte[] bytes = buffer.array();
				for(int i = 0; i<bytes.length;i++)
				{
					shorts[i] = (short)Byte.toUnsignedInt(bytes[i]);
				}
				return new VolatileShortArray(shorts, true);

				 *
				 */
				//ShortBuffer buffer = ShortBuffer.allocate(nElements);
				//short[] shortsCurrentPlane = new short[nElements];
				// read pixels
				/*
				ByteBuffer buffer = ByteBuffer.allocate(nElements*2);
				for (int z = minZ; z < maxZ; z++) 
				{
					byte[] bytesCurrentPlane = reader.openBytes(reader.getIndex(z, channel,
							timepoint), minX, minY, w, h);
					byte[] shortsCurrentPlane = new byte[2*bytesCurrentPlane.length];
					for(int i = 0; i<bytesCurrentPlane.length;i++)
					{
						shortsCurrentPlane[2*i] = 0;	
						shortsCurrentPlane[2*i+1] = bytesCurrentPlane[i];						
					}
					buffer.put(shortsCurrentPlane);
				}
				// release the reader
				readerPool.recycle(reader);
				
				return new VolatileShortArray(buffer.asShortBuffer().array(), true);
				*/
				ShortBuffer buffer = ShortBuffer.allocate(nElements);
				for (int z = minZ; z < maxZ; z++) 
				{
					byte[] bytesCurrentPlane = reader.openBytes(reader.getIndex(z, channel,
							timepoint), minX, minY, w, h);
					short[] shortsCurrentPlane = new short[bytesCurrentPlane.length];
					for(int i = 0; i<bytesCurrentPlane.length;i++)
					{
						shortsCurrentPlane[i] = (short)Byte.toUnsignedInt(bytesCurrentPlane[i]);	
												
					}
					buffer.put(shortsCurrentPlane);
				}
				// release the reader
				readerPool.recycle(reader);
				return new VolatileShortArray(buffer.array(), true);
			}
			catch (Exception e) {
				throw new InterruptedException(e.getMessage());
			}
		}

		@Override
		public int getBytesPerElement() {
			return 2;
		}
	}

	/**
	 * Class explaining how to read and load pixels of type : unsigned short (16 bits)
	 */
	public static class BioFormatsUnsignedShortArrayLoader extends
		BioformatsArrayLoader implements CacheArrayLoader<VolatileShortArray>
	{

		final ByteOrder byteOrder;

		protected BioFormatsUnsignedShortArrayLoader(ResourcePool<IFormatReader> readerPool,
			int channel, int iSeries, boolean littleEndian)
		{
			super(readerPool, channel, iSeries);
			if (littleEndian) {
				byteOrder = ByteOrder.LITTLE_ENDIAN;
			}
			else {
				byteOrder = ByteOrder.BIG_ENDIAN;
			}
		}

		@Override
		public VolatileShortArray loadArray(int timepoint, int setup, int level,
			int[] dimensions, long[] min) throws InterruptedException
		{
			try {
				// get the reader
				IFormatReader reader = readerPool.acquire();
				reader.setSeries(iSeries);
				reader.setResolution(level);
				int minX = (int) min[0];
				int minY = (int) min[1];
				int minZ = (int) min[2];
				int maxX = Math.min(minX + dimensions[0], reader.getSizeX());
				int maxY = Math.min(minY + dimensions[1], reader.getSizeY());
				int maxZ = Math.min(minZ + dimensions[2], reader.getSizeZ());
				int w = maxX - minX;
				int h = maxY - minY;
				int d = maxZ - minZ;
				int nElements = (w * h * d);

				// read pixels
				ByteBuffer buffer = ByteBuffer.allocate(nElements * 2);
				for (int z = minZ; z < maxZ; z++) {
					byte[] bytes = reader.openBytes(reader.getIndex(z, channel, timepoint), minX, minY,
						w, h);
					buffer.put(bytes);
				}

				// release the reader
				readerPool.recycle(reader);

				// unsigned short specific transform
				short[] shorts = new short[nElements];
				buffer.flip();
				buffer.order(byteOrder).asShortBuffer().get(shorts);
				return new VolatileShortArray(shorts, true);
			}
			catch (Exception e) {
				throw new InterruptedException(e.getMessage());
			}
		}

		@Override
		public int getBytesPerElement() {
			return 2;
		}
	}

	/**
	 * Class explaining how to read and load pixels of type : float (32 bits)
	 */
	public static class BioFormatsFloatArrayLoader extends BioformatsArrayLoader
		implements CacheArrayLoader<VolatileFloatArray>
	{

		final ByteOrder byteOrder;

		protected BioFormatsFloatArrayLoader(ResourcePool<IFormatReader> readerPool,
										  int channel, int iSeries, boolean littleEndian)
		{
			super(readerPool, channel, iSeries);
			if (littleEndian) {
				byteOrder = ByteOrder.LITTLE_ENDIAN;
			}
			else {
				byteOrder = ByteOrder.BIG_ENDIAN;
			}
		}

		@Override
		public VolatileFloatArray loadArray(int timepoint, int setup, int level,
			int[] dimensions, long[] min) throws InterruptedException
		{
			try {
				// get the reader
				IFormatReader reader = readerPool.acquire();
				reader.setSeries(iSeries);
				reader.setResolution(level);
				int minX = (int) min[0];
				int minY = (int) min[1];
				int minZ = (int) min[2];
				int maxX = Math.min(minX + dimensions[0], reader.getSizeX());
				int maxY = Math.min(minY + dimensions[1], reader.getSizeY());
				int maxZ = Math.min(minZ + dimensions[2], reader.getSizeZ());
				int w = maxX - minX;
				int h = maxY - minY;
				int d = maxZ - minZ;
				int nElements = (w * h * d);

				// read pixels
				ByteBuffer buffer = ByteBuffer.allocate(nElements * 4);
				for (int z = minZ; z < maxZ; z++) {
					byte[] bytes = reader.openBytes(reader.getIndex(z, channel, timepoint), minX, minY,
						w, h);
					buffer.put(bytes);
				}

				// release the reader
				readerPool.recycle(reader);

				// float specific transform
				float[] floats = new float[nElements];
				buffer.flip();
				buffer.order(byteOrder).asFloatBuffer().get(floats);
				return new VolatileFloatArray(floats, true);
			}
			catch (Exception e) {
				throw new InterruptedException(e.getMessage());
			}
		}

		@Override
		public int getBytesPerElement() {
			return 4;
		}
	}

	/**
	 * Class explaining how to read and load pixels of type : RGB (3 * 8 bits)
	 */
	public static class BioFormatsRGBArrayLoader extends BioformatsArrayLoader
		implements CacheArrayLoader<VolatileIntArray>
	{

		protected BioFormatsRGBArrayLoader(ResourcePool<IFormatReader> readerPool,
			int channel, int iSeries)
		{
			super(readerPool, channel, iSeries);
		}

		// Annoying because bioformats returns 3 bytes, while imglib2 requires ARGB,
		// so 4 bytes
		@Override
		public VolatileIntArray loadArray(int timepoint, int setup, int level,
										  int[] dimensions, long[] min) throws InterruptedException
		{
			try {
				// get the reader
				IFormatReader reader = readerPool.acquire();
				reader.setSeries(iSeries);
				reader.setResolution(level);
				int minX = (int) min[0];
				int minY = (int) min[1];
				int minZ = (int) min[2];
				int maxX = Math.min(minX + dimensions[0], reader.getSizeX());
				int maxY = Math.min(minY + dimensions[1], reader.getSizeY());
				int maxZ = Math.min(minZ + dimensions[2], reader.getSizeZ());
				int w = maxX - minX;
				int h = maxY - minY;
				int d = maxZ - minZ;
				int nElements = (w * h * d);

				// read pixels
				byte[] bytes;
				if (d == 1) {
					bytes = reader.openBytes(reader.getIndex(minZ, channel, timepoint), minX, minY,
							w, h);
				}
				else {
					int nBytesPerPlane = nElements * 3;
					bytes = new byte[nBytesPerPlane];
					int offset = 0;
					for (int z = minZ; z < maxZ; z++) {
						byte[] bytesCurrentPlane = reader.openBytes(reader.getIndex(z, channel,
								timepoint), minX, minY, w, h);
						System.arraycopy(bytesCurrentPlane, 0, bytes, offset,
								nBytesPerPlane);
						offset += nBytesPerPlane;
					}
				}
				boolean interleaved = reader.isInterleaved();

				// release the reader
				readerPool.recycle(reader);

				// RGB specific transform
				int[] ints = new int[nElements];
				int idxPx = 0;
				if (interleaved) {
					for (int i = 0; i < nElements; i++) {
						ints[i] = ((0xff) << 24) | ((bytes[idxPx] & 0xff) << 16) |
								((bytes[idxPx + 1] & 0xff) << 8) | (bytes[idxPx + 2] & 0xff);
						idxPx += 3;
					}
				} else {
					int bOffset = 2*nElements;
					for (int i = 0; i < nElements; i++) {
						ints[i] = ((bytes[idxPx] & 0xff) << 16 ) | ((bytes[idxPx+nElements] & 0xff) << 8) | (bytes[idxPx+bOffset] & 0xff);
						idxPx += 1;
					}
				}
				return new VolatileIntArray(ints, true);
			}
			catch (Exception e) {
				throw new InterruptedException(e.getMessage());
			}
		}

		@Override
		public int getBytesPerElement() {
			return 4;
		}
	}

	/**
	 * Class explaining how to read and load pixels of type : signed int (32 bits)
	 */
	public static class BioFormatsIntArrayLoader extends BioformatsArrayLoader
		implements CacheArrayLoader<VolatileIntArray>
	{

		final ByteOrder byteOrder;

		protected BioFormatsIntArrayLoader(ResourcePool<IFormatReader> readerPool,
										int channel, int iSeries, boolean littleEndian)
		{
			super(readerPool, channel, iSeries);
			if (littleEndian) {
				byteOrder = ByteOrder.LITTLE_ENDIAN;
			}
			else {
				byteOrder = ByteOrder.BIG_ENDIAN;
			}
		}

		@Override
		public VolatileIntArray loadArray(int timepoint, int setup, int level,
			int[] dimensions, long[] min) throws InterruptedException
		{
			try {
				// get the reader
				IFormatReader reader = readerPool.acquire();
				reader.setSeries(iSeries);
				reader.setResolution(level);
				int minX = (int) min[0];
				int minY = (int) min[1];
				int minZ = (int) min[2];
				int maxX = Math.min(minX + dimensions[0], reader.getSizeX());
				int maxY = Math.min(minY + dimensions[1], reader.getSizeY());
				int maxZ = Math.min(minZ + dimensions[2], reader.getSizeZ());
				int w = maxX - minX;
				int h = maxY - minY;
				int d = maxZ - minZ;
				int nElements = (w * h * d);

				// read pixels
				ByteBuffer buffer = ByteBuffer.allocate(nElements * 4);
				for (int z = minZ; z < maxZ; z++) {
					byte[] bytes = reader.openBytes(reader.getIndex(z, channel, timepoint), minX, minY,
						w, h);
					buffer.put(bytes);
				}

				// release the reader
				readerPool.recycle(reader);

				// int specific transform
				int[] ints = new int[nElements];
				buffer.flip();
				buffer.order(byteOrder).asIntBuffer().get(ints);
				return new VolatileIntArray(ints, true);
			}
			catch (Exception e) {
				throw new InterruptedException(e.getMessage());
			}
		}

		@Override
		public int getBytesPerElement() {
			return 4;
		}
	}
}

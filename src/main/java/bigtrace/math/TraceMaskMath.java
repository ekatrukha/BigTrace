package bigtrace.math;

import java.util.ArrayList;

import bigtrace.BigTrace;
import bigtrace.rois.AbstractCurve3D;
import bigtrace.rois.Roi3D;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class TraceMaskMath < T extends RealType< T > & NativeType< T > >
{
    public BigTrace<T> bt;

    public TraceMaskMath(final BigTrace<T> bt_)
	{
		bt = bt_;
    }

    public void generateTraceMask(){
        
        // Get the minimum coordinates of the trace interval
        System.out.println("Regen trace mask");
        IntervalView<T> traceIV = bt.getTraceInterval(false);
        long[] minV = traceIV.minAsLongArray();
        long[] dim = bt.btData.getDataCurrentSourceClipped().dimensionsAsLongArray();
        ArrayImg<FloatType, FloatArray> sW = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ]);
        IntervalView<FloatType> salWeights =  Views.translate(sW, minV);
        ArrayList<Roi3D> rois = bt.roiManager.rois;

        for (Roi3D roi : rois) {
            if (roi.getTimePoint() == bt.btData.nCurrTimepoint && roi instanceof AbstractCurve3D) {
                AbstractCurve3D curve = (AbstractCurve3D) roi; 
                if (curve.vertices.size() > 1)
                {
                    for (RealPoint point : curve.getJointSegmentResampled()) { 
                        point = Roi3D.scaleGlobInv(point, bt.btData.globCal);
                        final RandomAccess< FloatType > r = salWeights.randomAccess();
                        r.setPosition( (int) point.getDoublePosition(0), 0 );
                        r.setPosition( (int) point.getDoublePosition(1), 1 );
                        r.setPosition( (int) point.getDoublePosition(2), 2 );
                        final FloatType t = r.get();
                        t.set( 1.0f );
                    }
                }
            } 
        }
        double[] sigma = { 2.0, 2.0, 2.0 }; // x, y, z
        // Apply Gaussian blur to the weights
        ArrayImg<FloatType, FloatArray> blurredSW = ArrayImgs.floats(dim[0], dim[1], dim[2]);
        IntervalView<FloatType> blurredSalWeights = Views.translate(blurredSW, minV);
        Gauss3.gauss(sigma, Views.extendBorder(salWeights), blurredSalWeights);
        bt.btData.flTraceMask = blurredSalWeights;
        System.out.println("Regen trace mask success");
        // bt.btData.trace_mask = VolumeMisc.convertFloatToUnsignedByte(blurredSalWeights, false);
    }

}

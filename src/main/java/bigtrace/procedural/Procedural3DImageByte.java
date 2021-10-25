package bigtrace.procedural;

import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.numeric.integer.UnsignedByteType;


import java.util.function.ToIntFunction;

public class Procedural3DImageByte extends RealPoint implements RealRandomAccess<UnsignedByteType> {
    final UnsignedByteType t;

    ToIntFunction<double[]> evalFunction;

    public Procedural3DImageByte(ToIntFunction<double[]> evalFunction)
    {
        super( 3 ); // number of dimensions is 3
        t = new UnsignedByteType();
        this.evalFunction=evalFunction;
    }

    public Procedural3DImageByte(UnsignedByteType t) {
        this.t = t;
    }

    @Override
    public RealRandomAccess<UnsignedByteType> copyRealRandomAccess() {
        return copy();
    }

    @Override
    public UnsignedByteType get() {
        t.set(
                evalFunction.applyAsInt(position)
        );
        return t;
    }

    @Override
    public Procedural3DImageByte copy() {
        Procedural3DImageByte a = new Procedural3DImageByte(evalFunction);
        a.setPosition( this );
        return a;
    }

    public RealRandomAccessible<UnsignedByteType> getRRA() {

        RealRandomAccessible<UnsignedByteType> rra = new RealRandomAccessible<UnsignedByteType>() {
            @Override
            public RealRandomAccess<UnsignedByteType> realRandomAccess() {
                return copy();
            }

            @Override
            public RealRandomAccess<UnsignedByteType> realRandomAccess(RealInterval realInterval) {
                return copy();
            }

            @Override
            public int numDimensions() {
                return 3;
            }
        };

        return rra;
    }


}
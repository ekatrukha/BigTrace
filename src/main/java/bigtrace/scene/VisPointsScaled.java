package bigtrace.scene;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import bigtrace.BigTraceData;
import net.imglib2.RealPoint;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import bvvbigtrace.backend.jogl.JoglGpuContext;
import bvvbigtrace.shadergen.DefaultShader;
import bvvbigtrace.shadergen.Shader;
import bvvbigtrace.shadergen.generate.Segment;
import bvvbigtrace.shadergen.generate.SegmentTemplate;


import static com.jogamp.opengl.GL.GL_FLOAT;


public class VisPointsScaled
{

	private final Shader prog;

	private int vao;
	
	private Vector4f l_color;
	
	private float fPointSize;
	
	float vertices[]; 
	
	private int nPointsN;
	
	private boolean initialized;

	public VisPointsScaled()
	{
		final Segment pointVp = new SegmentTemplate( VisPointsScaled.class, "/scene/scaled_point_color.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisPointsScaled.class, "/scene/scaled_point_color.fp" ).instantiate();		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	
	}
	/** constructor with one point **/
	public VisPointsScaled(final RealPoint point, final float fPointSize_,final Color color_in)
	{		
		this();
		
		fPointSize = fPointSize_;
		
		l_color = new Vector4f(color_in.getComponents(null));
		
		nPointsN = 1;
		
		vertices = new float [nPointsN*3];//assume 3D
		
		point.localize(vertices);	

	}
	/** constructor with multiple vertices **/
	public VisPointsScaled(final ArrayList< RealPoint > points,final double scalexyz_[], final float fPointSize_,final Color color_in)
	{
		this();
		
		int i,j;
		
		fPointSize= fPointSize_;
		
		l_color = new Vector4f(color_in.getComponents(null));
		
		nPointsN = points.size();
		
		vertices = new float [nPointsN*3];//assume 3D

		for (i=0;i<nPointsN; i++)
		{
			for (j=0;j<3; j++)
			{				
				vertices[i*3+j]=points.get(i).getFloatPosition(j);
			}			
		}

	}
	
	public void setVertices( ArrayList< RealPoint > points)
	{
		int i,j;	
		
		nPointsN = points.size();
		
		vertices = new float [nPointsN*3];//assume 3D
		
		for (i=0;i<nPointsN; i++)
		{
			for (j=0;j<3; j++)
			{
				vertices[i*3+j]=points.get(i).getFloatPosition(j);
			}			
		}
		
		initialized = false;
	}
	public void setColor(Color pointColor) {
		
		l_color = new Vector4f(pointColor.getComponents(null));
		
	}
	public void setSize(float fPointSize_)
	{
		fPointSize= fPointSize_;
	}

	private void init( GL3 gl )
	{
		initialized = true;

		// ..:: VERTEX BUFFER ::..

		final int[] tmp = new int[ 2 ];
		gl.glGenBuffers( 1, tmp, 0 );
		final int vbo = tmp[ 0 ];
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, vbo );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, vertices.length * Float.BYTES, FloatBuffer.wrap( vertices ), GL.GL_STATIC_DRAW );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );


		// ..:: VERTEX ARRAY OBJECT ::..

		gl.glGenVertexArrays( 1, tmp, 0 );
		vao = tmp[ 0 ];
		gl.glBindVertexArray( vao );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, vbo );
		gl.glVertexAttribPointer( 0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0 );
		gl.glEnableVertexAttribArray( 0 );
		gl.glBindVertexArray( 0 );
	}

	public void draw(final GL3 gl,final Matrix4fc pvm,final int [] screen_size )
	{
		if ( !initialized )
			init( gl );
		Vector2f window_sizef;
		Vector2f ellipse_axes;

		JoglGpuContext context = JoglGpuContext.get( gl );
		
		//scale disk with viewport transform
		window_sizef =  new Vector2f ((float)(screen_size[0]), (float)(screen_size[1]));
		
		//The whole story behind the code below is that
		//the size of the OpenGL sprite corresponding to a point is
		//changing depending on the actual window size and the render window size parameters.
		//Basically it scales with coefficient screen_size[0]/renderParams.nRenderW (in each dimension).
		//To compensate for that, we have to enlarge (shrink) effective point size
		//(it is done in the vertex shader, we enabled gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE))
		//and then render the point as nice circle by painting it as an ellipse (in the fragment shader)
		//that will scale into the circle %)
		//
		
		ellipse_axes = new Vector2f((float)screen_size[0]/(float)BigTraceData.renderParams.nRenderW, (float)screen_size[1]/(float)BigTraceData.renderParams.nRenderH);
		//scale of viewport vs render
		//we enlarge/shrink to minimum dimension scale
		//and in the ellipse the other dimension will be cropped
		//(maybe this part can be moved to GPU? seems not critical right now)
		float fPointScale = Math.min(ellipse_axes.x,ellipse_axes.y);
		ellipse_axes.mul(1.0f/fPointScale);
		//actually it is not true ellipse axes,
		//but rather inverse squared values
		ellipse_axes.x = ellipse_axes.x*ellipse_axes.x;
		ellipse_axes.y = ellipse_axes.y*ellipse_axes.y;
				
		//voxel size
		Vector3f globCalForw = new Vector3f((float)BigTraceData.globCal[0], (float)BigTraceData.globCal[1], (float)BigTraceData.globCal[2]);

		prog.getUniform1f( "pointSizeReal" ).set( (float) (fPointSize*BigTraceData.dMinVoxelSize) );
		prog.getUniform1f( "pointScale" ).set( fPointScale );
		prog.getUniformMatrix4f( "pvm" ).set( pvm );
		prog.getUniform3f( "voxelScale" ).set( globCalForw );
		prog.getUniform4f("colorin").set(l_color);
		prog.getUniform2f("windowSize").set(window_sizef);
		prog.getUniform2f("ellipseAxes").set(ellipse_axes);
		prog.setUniforms( context );
		prog.use( context );


		gl.glBindVertexArray( vao );
		gl.glDrawArrays( GL.GL_POINTS, 0, nPointsN);
		gl.glBindVertexArray( 0 );
	}

}

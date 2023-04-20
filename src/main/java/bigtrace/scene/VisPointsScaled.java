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
	
	//private final ArrayList< Point > points = new ArrayList<>();
	
	float vertices[]; 
	private int nPointsN;
	private boolean initialized;

	public VisPointsScaled()
	{
		final Segment pointVp = new SegmentTemplate( VisPointsScaled.class, "/scene/point_color.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisPointsScaled.class, "/scene/point_color.fp" ).instantiate();
	
		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	
	}
	public VisPointsScaled(final RealPoint point, final float fPointSize_,final Color color_in)
	{		
		this();

		int j;
		fPointSize= fPointSize_;
		
		l_color = new Vector4f(color_in.getComponents(null));
		
		nPointsN=1;
		vertices = new float [nPointsN*3];//assume 3D
		

		for (j=0;j<3; j++)
		{
			vertices[j]=point.getFloatPosition(j);
		}					
	

	}
	public VisPointsScaled(final ArrayList< RealPoint > points,final double scalexyz_[], final float fPointSize_,final Color color_in)
	{
		this();
		int i,j;
		
		fPointSize= fPointSize_;
		
		l_color = new Vector4f(color_in.getComponents(null));
		
		nPointsN=points.size();
		vertices = new float [nPointsN*3];//assume 3D

		for (i=0;i<nPointsN; i++)
		{
			for (j=0;j<3; j++)
			{
				
				vertices[i*3+j]=(float) (points.get(i).getFloatPosition(j));
			}
			
		}

	}
	public void setVertices( ArrayList< RealPoint > points)
	{
		int i,j;
		
		
		nPointsN=points.size();
		vertices = new float [nPointsN*3];//assime 3D
		
		for (i=0;i<nPointsN; i++)
		{
			for (j=0;j<3; j++)
			{
				vertices[i*3+j]=points.get(i).getFloatPosition(j);
			}
			
		}
		
		initialized=false;
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

	public void draw( GL3 gl, Matrix4fc pvm, int [] screen_size )
	{
		if ( !initialized )
			init( gl );
		//Vector2f screen_sizef;

		JoglGpuContext context = JoglGpuContext.get( gl );
		
		//for disk scaling with viewport aspect ratio
		//screen_sizef =  new Vector2f ((float)(screen_size[0]), (float)(screen_size[1]));
		//screen_sizef =  new Vector2f ((float)(1.0/screen_size[0]), (float)(1.0/screen_size[1]));
		//screen_sizef.mul((float)Math.min(screen_size[0], screen_size[1]));
		//screen_sizef.x=screen_sizef.x*screen_sizef.x;
		//screen_sizef.y=screen_sizef.y*screen_sizef.y;
		prog.getUniformMatrix4f( "pvm" ).set( pvm );

		prog.getUniform4f("colorin").set(l_color);
		//prog.getUniform2f("screenSize").set(screen_sizef);

		prog.setUniforms( context );
		prog.use( context );
		final int [] viewport  = new int[] { 0, 0, screen_size[0], screen_size[1] };
		//gl.glGetIntegerv( GL.GL_VIEWPORT, viewport, 0 );
		
		//voxel calibration
		Vector3f globCalForw = new Vector3f((float)BigTraceData.globCal[0], (float)BigTraceData.globCal[1], (float)BigTraceData.globCal[2]);
		Vector3f globCalInv = new Vector3f((float)(1.0/BigTraceData.globCal[0]), (float)(1.0/BigTraceData.globCal[1]), (float)(1.0/BigTraceData.globCal[2]));
		double dMin = Math.min(Math.min(BigTraceData.globCal[0], BigTraceData.globCal[1]),BigTraceData.globCal[2]);
		
		int i,j;
		
		gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
		
		gl.glBindVertexArray( vao );
		
		Vector3f curr_RAI =new  Vector3f();
		//current point in screen coordinates
		Vector3f curr = new Vector3f(); 
		Vector3f shift = new Vector3f(); 
		Vector3f curr_scale = new Vector3f();
		for (i=0;i<nPointsN; i++)
		{
	
			//current point in VOXEL space
			curr_RAI = new Vector3f(vertices[i*3],vertices[i*3+1],vertices[i*3+2]);
			
			//current point in screen coordinates
			//calculate it
			pvm.project(curr_RAI, viewport, curr);
			//let's shift it in a plane perpendicular to the view 
			shift = new Vector3f(curr);
			shift.x+=100.0f;
			
	
			//Vector3f shift_RAI = new Vector3f();
			//project it back to VOXEL space
			//pvm.unproject(shift, new int[] { 0, 0, screen_size[0], screen_size[1] }, shift_RAI);
			pvm.unproject(shift, viewport, shift);
			
			//move to SPACE units
			
			//Vector3f shift_scale = new Vector3f(); 
			shift.mul( globCalForw, shift);		
			curr_RAI.mul( globCalForw, curr_scale);
			//correct the length to specified size
			shift.sub(curr_scale);
			shift.normalize();
			shift.mul((float)(dMin*fPointSize));
			shift.add(curr_scale);
			//go back to VOXEL units
			shift.mul( globCalInv, shift);
			//project on the screen to get dimensions in pixels
			pvm.project(shift, viewport, shift);
			
			//gl.glEnable(0x8861);
			//just in case it is too small
			float dPointSize = (float)Math.max(Math.abs(curr.x-shift.x),1.0);
			dPointSize *= Math.max(screen_size[0], screen_size[1])/Math.min(screen_size[0], screen_size[1]); 
			gl.glPointSize(dPointSize);				
			//gl.glDrawArrays( GL.GL_POINTS, 0, nPointsN);
			gl.glDrawArrays( GL.GL_POINTS, i, i+1);
		}
		
		
		gl.glBindVertexArray( 0 );
	}

}

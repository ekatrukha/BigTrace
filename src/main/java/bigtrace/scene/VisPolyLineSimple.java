package bigtrace.scene;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import net.imglib2.RealPoint;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector4f;

import btbvv.core.backend.jogl.JoglGpuContext;
import btbvv.core.shadergen.DefaultShader;
import btbvv.core.shadergen.Shader;
import btbvv.core.shadergen.generate.Segment;
import btbvv.core.shadergen.generate.SegmentTemplate;


import static com.jogamp.opengl.GL.GL_FLOAT;


public class VisPolyLineSimple
{
	//private final String imageFilename;

	private final Shader prog;

	private int vao;
	
	private Vector4f l_color;
	

	public float fLineThickness;
	
	//private final ArrayList< Point > points = new ArrayList<>();
	
	float vertices[]; 
	private int nPointsN;

	private boolean initialized;

	public VisPolyLineSimple()
	{
		//final Segment pointVp = new SegmentTemplate( VisPolyLineSimple.class, "/scene/antialiased_line.vp" ).instantiate();
		//final Segment pointFp = new SegmentTemplate( VisPolyLineSimple.class, "/scene/antialiased_line.fp" ).instantiate();
		final Segment pointVp = new SegmentTemplate( VisPolyLineSimple.class, "/scene/simple_color.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisPolyLineSimple.class, "/scene/simple_color.fp" ).instantiate();
	
		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	}
	
	
	public VisPolyLineSimple(final ArrayList< RealPoint > points, final float fLineThickness_,final Color color_in)
	{
		this();
		fLineThickness = fLineThickness_;		
		l_color = new Vector4f(color_in.getComponents(null));		
		setVertices(points);
		
	}
	
	public void setThickness(float fLineThickness_)
	{
		fLineThickness= fLineThickness_;
	}
	
	public void setColor(Color color_in)
	{
		l_color = new Vector4f(color_in.getComponents(null));
	}
	
	public void setParams(final ArrayList< RealPoint > points, final float fLineThickness_, final Color color_in)
	{

		fLineThickness= fLineThickness_;
		l_color = new Vector4f(color_in.getComponents(null));		
		setVertices(points);
	}
	public void setVertices( ArrayList< RealPoint > points)
	{
		int i,j;
		
		
		nPointsN=points.size();
		vertices = new float [nPointsN*3];//assume 3D	

		for (i=0;i<nPointsN; i++)
		{
			for (j=0;j<3; j++)
			{
				vertices[i*3+j]=points.get(i).getFloatPosition(j);
			}
			
		}
		initialized=false;
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

	public void draw( GL3 gl, Matrix4fc pvm, Matrix4fc vm )
	{
		if ( !initialized )
			init( gl );


		JoglGpuContext context = JoglGpuContext.get( gl );
		
		prog.getUniformMatrix4f( "pvm" ).set( pvm );
		
		prog.getUniformMatrix4f( "pvm" ).set( pvm );
		prog.getUniform4f("colorin").set(l_color);
		prog.setUniforms( context );
		prog.use( context );


		gl.glBindVertexArray( vao );
		gl.glLineWidth(fLineThickness);
		gl.glDrawArrays( GL.GL_LINE_STRIP, 0, nPointsN);
		gl.glBindVertexArray( 0 );
		
//		prog.getUniformMatrix4f( "vm" ).set( pvm );
//		prog.getUniform4f("colorin").set(l_color);
//		int noffset = 0;
//		int[] sizeVP = new int[4];
//		gl.glGetIntegerv( GL.GL_VIEWPORT, sizeVP, noffset );
//		Vector2f viewPort =  new Vector2f(sizeVP[2],sizeVP[3]);
//		prog.getUniform2f("uViewPort").set(viewPort);
//		prog.setUniforms( context );
//		prog.use( context );
//
//		
//		gl.glBindVertexArray( vao );
//
//		gl.glEnable( GL.GL_BLEND );
//		gl.glEnable(GL.GL_DEPTH_TEST);
//		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); 
//
//		gl.glDepthFunc( GL.GL_ALWAYS);
//		gl.glLineWidth(10);
//		gl.glDrawArrays( GL.GL_LINE_STRIP, 0, nPointsN);
//
//		gl.glBindVertexArray( 0 );
	}
}

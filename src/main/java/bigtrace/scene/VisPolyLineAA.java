package bigtrace.scene;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import net.imglib2.RealPoint;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;


import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import bigtrace.BigTraceData;
import bvvpg.core.backend.jogl.JoglGpuContext;
import bvvpg.core.shadergen.DefaultShader;
import bvvpg.core.shadergen.Shader;
import bvvpg.core.shadergen.generate.Segment;
import bvvpg.core.shadergen.generate.SegmentTemplate;


import static com.jogamp.opengl.GL.GL_FLOAT;


public class VisPolyLineAA
{

	private final Shader prog;

	private int vao;
	
	private Vector4f l_color;

	public float fLineThickness;
	
	float lineLength = 0.0f;
	
	public boolean bIncludeClip = false;
		
	float vertices[]; 
	
	private int nPointsN;
	
	int nTotVert = 0 ;

	private boolean initialized;

	public VisPolyLineAA()
	{
		final Segment lineVp = new SegmentTemplate( VisPolyLineAA.class, "/scene/aa_line.vp" ).instantiate();
		final Segment lineFp = new SegmentTemplate( VisPolyLineAA.class, "/scene/aa_line.fp" ).instantiate();
		
		prog = new DefaultShader( lineVp.getCode(), lineFp.getCode() );
	}
	
	
	public VisPolyLineAA(final ArrayList< RealPoint > points, final float fLineThickness_,final Color color_in)
	{
		this();
		fLineThickness = fLineThickness_;		
		l_color = new Vector4f(color_in.getComponents(null));		
		setVertices(points);
		
	}
	
	public VisPolyLineAA(final ArrayList< RealPoint > points, final float fLineThickness_, final Vector4f l_color_)
	{
		this();
		fLineThickness = fLineThickness_;		
		l_color = new Vector4f(l_color_);		
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
	
	public void setColor(final Vector4f l_color_)
	{
		l_color = new Vector4f(l_color_);
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

	private void init( GL3 gl )
	{
		initialized = true;
		
		// drawing of antialiased 3D lines
		// taken and adapted from 
		// https://www.labri.fr/perso/nrougier/python-opengl/#d-lines
		// Python & OpenGL for Scientific Visualization
		// Copyright (c) 2018 - Nicolas P. Rougier <Nicolas.Rougier@inria.fr>
		
		int nTotLength = (nPointsN)*3*2;
		nTotVert = nPointsN*2;
		float [] nCumLength = new float [nPointsN];
		float [] UV = new float [nPointsN*2*2];
		//calculate new arrays
		float [] vertAll = new float [nTotLength+2*3*2];
		float [] vertCurr = new float [nTotLength];
		float [] vertPrev = new float [nTotLength];
		float [] vertNext = new float [nTotLength];
		
	
		for(int nV = 1; nV<nPointsN+1; nV++)
		{
			for(int nDup = 0; nDup<2; nDup++)
			{
				for (int d=0;d<3; d++)
				{	
					vertAll[nV*6+nDup*3+d] = vertices[(nV-1)*3+d];
				}
			}
		}
		for(int nDup = 0; nDup<2; nDup++)
		{
			for (int d=0;d<3; d++)
			{	
				vertAll[nDup*3+d] = vertices[d];
			}
		}
		for(int nDup = 0; nDup<2; nDup++)
		{
			for (int d=0;d<3; d++)
			{	
				vertAll[(nPointsN+1)*6+nDup*3+d] = vertices[(nPointsN-1)*3+d];
			}
		}
		for(int i = 0; i<nTotLength; i++)
		{
			vertCurr[i] =vertAll[i+6];
			vertPrev[i] =vertAll[i];
			vertNext[i] =vertAll[i+12];
		}
		
		//cumulative length
		for(int i = 1; i<nPointsN;i++)
		{
			double dLen = 0;
			for(int d=0; d<3; d++)
			{
				dLen += Math.pow( vertices[i*3+d]-vertices[(i-1)*3+d], 2 );
			}
			nCumLength[i] = ( float ) Math.sqrt(dLen)+nCumLength[i]-1;
		}
		
		lineLength = nCumLength[nPointsN-1];
		for(int nV = 0; nV<nPointsN; nV++)
		{
				UV[nV*4] = nCumLength[nV];
				UV[nV*4+1] = 1.0f;
				UV[nV*4+2] = nCumLength[nV];
				UV[nV*4+3] = -1.0f;
		}


		// ..:: VERTEX BUFFER ::..

		final int[] tmp = new int[ 4 ];
		gl.glGenBuffers( 4, tmp, 0 );
		final int currVbo = tmp[ 0 ];
		final int prevVbo = tmp[ 1 ];
		final int nextVbo = tmp[ 2 ];
		final int uvVbo   = tmp[ 3 ];

		
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, currVbo );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, vertCurr.length * Float.BYTES, FloatBuffer.wrap( vertCurr ), GL.GL_STATIC_DRAW );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );

		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, prevVbo );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, vertPrev.length * Float.BYTES, FloatBuffer.wrap( vertPrev ), GL.GL_STATIC_DRAW );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );
		
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, nextVbo );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, vertNext.length * Float.BYTES, FloatBuffer.wrap( vertNext ), GL.GL_STATIC_DRAW );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );
		
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, uvVbo );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, UV.length * Float.BYTES, FloatBuffer.wrap( UV ), GL.GL_STATIC_DRAW );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );
		
		
		// ..:: VERTEX ARRAY OBJECT ::..

		
		gl.glGenVertexArrays( 1, tmp, 0 );
		vao = tmp[ 0 ];
		gl.glBindVertexArray( vao );
		
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, currVbo );
		gl.glVertexAttribPointer( 0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0 );
		gl.glEnableVertexAttribArray( 0 );
		
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, prevVbo );
		gl.glVertexAttribPointer( 1, 3, GL_FLOAT, false, 3 * Float.BYTES, 0 );
		gl.glEnableVertexAttribArray( 1 );

		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, nextVbo );
		gl.glVertexAttribPointer( 2, 3, GL_FLOAT, false, 3 * Float.BYTES, 0 );
		gl.glEnableVertexAttribArray( 2 );

		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, uvVbo );
		gl.glVertexAttribPointer( 3, 2, GL_FLOAT, false, 2 * Float.BYTES, 0 );
		gl.glEnableVertexAttribArray( 3 );
		
		gl.glBindVertexArray( 0 );

		
	}

	public void draw( GL3 gl, Matrix4fc pvm)
	{
		if ( !initialized )
			init( gl );

		JoglGpuContext context = JoglGpuContext.get( gl );
		
		
		int noffset = 0;
		int[] sizeVP = new int[4];
		gl.glGetIntegerv( GL.GL_VIEWPORT, sizeVP, noffset );
		Vector2f viewPort =  new Vector2f(sizeVP[2],sizeVP[3]);
		prog.getUniform2f("viewport").set(viewPort);
		prog.getUniformMatrix4f( "pvm" ).set( pvm );	
		prog.getUniform4f("color").set(l_color);
		prog.getUniform1f( "linelength" ).set( lineLength );
		//prog.getUniform1f( "thickness" ).set(3 );
		prog.getUniform1f( "thickness" ).set( fLineThickness );
		prog.getUniform1f( "antialias" ).set( 1.5f);
		if(bIncludeClip)
		{
			prog.getUniform1i("clipactive").set(BigTraceData.nClipROI);
			prog.getUniform3f("clipmin").set(new Vector3f(BigTraceData.nDimCurr[0][0],BigTraceData.nDimCurr[0][1],BigTraceData.nDimCurr[0][2]));
			prog.getUniform3f("clipmax").set(new Vector3f(BigTraceData.nDimCurr[1][0],BigTraceData.nDimCurr[1][1],BigTraceData.nDimCurr[1][2]));

		}
		else
		{
			prog.getUniform1i("clipactive").set(0);
		}
		
		prog.setUniforms( context );
		prog.use( context );
		
		
		
		gl.glDepthFunc( GL.GL_ALWAYS);
		//gl.glDepthFunc( GL.GL_LESS);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); 
		gl.glBindVertexArray( vao );
		gl.glDepthMask(false);
		
		gl.glDrawArrays( GL.GL_TRIANGLE_STRIP, 0, nTotVert);
		
		gl.glDepthMask(true);
		gl.glBindVertexArray( 0 );
		
	}
}

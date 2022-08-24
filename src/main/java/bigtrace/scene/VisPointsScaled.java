package bigtrace.scene;
/*-
 * #%L
 * Volume rendering of bdv datasets
 * %%
 * Copyright (C) 2018 - 2021 Tobias Pietzsch
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import net.imglib2.RealPoint;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector4f;

import bvvbigtrace.backend.jogl.JoglGpuContext;
import bvvbigtrace.shadergen.DefaultShader;
import bvvbigtrace.shadergen.Shader;
import bvvbigtrace.shadergen.generate.Segment;
import bvvbigtrace.shadergen.generate.SegmentTemplate;


import static com.jogamp.opengl.GL.GL_FLOAT;


public class VisPointsScaled
{
	//private final String imageFilename;
	private static final float fMaxRenderSpotSize = 600.0f;
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
		final Segment pointVp = new SegmentTemplate( VisPointsScaled.class, "/scene/scaled_point_color.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisPointsScaled.class, "/scene/scaled_point_color.fp" ).instantiate();
	
		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	
	}
	public VisPointsScaled(final RealPoint point, final float fPointSize_,final Color color_in)
	{		
		this();

		int j;
		fPointSize= fPointSize_;
		
		l_color = new Vector4f(color_in.getComponents(null));
		
		nPointsN=1;
		vertices = new float [nPointsN*3];//assime 3D
		

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
		Vector2f screen_sizef;

		JoglGpuContext context = JoglGpuContext.get( gl );
		
		//scale disk with viewport transform
		screen_sizef =  new Vector2f ((float)(screen_size[0]), (float)(screen_size[1]));

		prog.getUniform1f( "pointSizeReal" ).set( fPointSize );
		prog.getUniform1f( "pointSizeMaxRender" ).set( fMaxRenderSpotSize);
		prog.getUniformMatrix4f( "pvm" ).set( pvm );
		//prog.getUniformMatrix4f( "projection" ).set( projection );
		prog.getUniform4f("colorin").set(l_color);
		prog.getUniform2f("screenSize").set(screen_sizef);

		prog.setUniforms( context );
		prog.use( context );


		gl.glBindVertexArray( vao );
		gl.glPointSize(fMaxRenderSpotSize);
		gl.glDrawArrays( GL.GL_POINTS, 0, nPointsN);
		gl.glBindVertexArray( 0 );
	}

}

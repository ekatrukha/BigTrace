package BigTrace.scene;
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
import java.nio.FloatBuffer;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;

import tpietzsch.backend.jogl.JoglGpuContext;
import tpietzsch.shadergen.DefaultShader;
import tpietzsch.shadergen.Shader;
import tpietzsch.shadergen.generate.Segment;
import tpietzsch.shadergen.generate.SegmentTemplate;

import static com.jogamp.opengl.GL.GL_FLOAT;


public class VisTestCubeDiskRed
{


	private final Shader prog;

	private int vao;


	public VisTestCubeDiskRed()
	{
		//this.imageFilename = imageFilename;
		final Segment pointVp = new SegmentTemplate( VisTestCubeDiskRed.class, "/scene/simple_red_point.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisTestCubeDiskRed.class, "/scene/simple_red_point.fp" ).instantiate();
		
		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	}

	private boolean initialized;

	private void init( GL3 gl )
	{
		initialized = true;

		// ..:: VERTEX BUFFER ::..

		final float vertices[] = {
				// 3 pos
				-0.5f, -0.5f, -0.5f,
				-0.5f, -0.5f, 0.5f,
				-0.5f, 0.5f, -0.5f,
				-0.5f, 0.5f, 0.5f,
				0.5f, -0.5f, -0.5f,
				0.5f, -0.5f, 0.5f,
				0.5f, 0.5f, -0.5f,
				0.5f, 0.5f, 0.5f
				

		};
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

	public void draw( GL3 gl, Matrix4fc pvm,  double [] screen_size)
	{
		if ( !initialized )
			init( gl );
		Vector2f screen_sizef;//= new Vector2f ((float)screen_size[0], (float)screen_size[1]);

		JoglGpuContext context = JoglGpuContext.get( gl );

		if(screen_size[0]>screen_size[1])
		{
			screen_sizef =  new Vector2f ((float)(Math.pow(0.5*screen_size[1]/screen_size[0],2)), 0.25f);
		}
		else
		{
			screen_sizef =  new Vector2f ((float)(Math.pow(0.5*screen_size[0]/screen_size[1],2)), 0.25f);
		}
		prog.getUniformMatrix4f( "pvm" ).set( pvm );
		prog.getUniform2f("scrnsize").set(screen_sizef);
		prog.setUniforms( context );
		prog.use( context );


		gl.glBindVertexArray( vao );
		gl.glPointSize(10);
		gl.glDrawArrays( GL.GL_POINTS, 0, 8 );
		gl.glBindVertexArray( 0 );
	}
}

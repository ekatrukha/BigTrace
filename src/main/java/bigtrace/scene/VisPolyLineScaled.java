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

import bigtrace.BigTraceData;
import bigtrace.geometry.Pipe3D;
import bigtrace.geometry.ShapeInterpolation;
import bigtrace.rois.Roi3D;
import bigtrace.volume.VolumeMisc;
import net.imglib2.RealPoint;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Matrix4fc;
import org.joml.Vector4f;

import bvvbigtrace.backend.jogl.JoglGpuContext;
import bvvbigtrace.shadergen.DefaultShader;
import bvvbigtrace.shadergen.Shader;
import bvvbigtrace.shadergen.generate.Segment;
import bvvbigtrace.shadergen.generate.SegmentTemplate;

import static com.jogamp.opengl.GL.GL_FLOAT;



public class VisPolyLineScaled
{
	
	
	private final Shader prog;

	private int vao;
	
	
	private float vertices[]; 
	private int nPointsN=0;

	public int renderType = Roi3D.WIRE;
	private Vector4f l_color;	
	public float fLineThickness;

	/** number of segments in the cylinder cross-section (stack count),
	 *  3 = prism, 4 = cuboid, etc.
	 *  The more the number, more smooth is surface **/
	//public final int nSectorN = BigTraceData.sectorN;

	private boolean initialized;
	
	/** whether or not use smoothing in the render **/
	public boolean bSmooth = true;


	public VisPolyLineScaled()
	{
		final Segment pointVp = new SegmentTemplate( VisPolyLineScaled.class, "/scene/simple_color.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisPolyLineScaled.class, "/scene/simple_color.fp" ).instantiate();
	
		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	}
	
	
	public VisPolyLineScaled(final ArrayList< RealPoint > points, final float fLineThickness_, final Color color_in, final int nRenderType)
	{
		this();
		
		fLineThickness= fLineThickness_;	
		l_color = new Vector4f(color_in.getComponents(null));		
		renderType = nRenderType;
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


	public void setParams(final ArrayList< RealPoint > points, final float fLineThickness_, final int nSectorN_, final Color color_in)
	{
		
		fLineThickness= fLineThickness_;		
		l_color = new Vector4f(color_in.getComponents(null));		
		setVertices(points);
	}
	
	public void setRenderType(int nRenderType_)
	{
		renderType = nRenderType_;
		
	}
	public int getRenderType()
	{
		return renderType;
		
	}
	public void setVertices( ArrayList< RealPoint > points_)
	{
		
		ArrayList< RealPoint > points;
		
		//smoothing, if necessary
		if(bSmooth && BigTraceData.shapeInterpolation==BigTraceData.SHAPE_Subvoxel)
		{
			points= ShapeInterpolation.getSmoothVals(points_);
		}
		else
		{
			points = points_;
		}	
		
		if(renderType == Roi3D.OUTLINE)
		{
			setVerticesCenterLine(points);
		}
		else
		{
			//move to scaled space
			points = Roi3D.scaleGlob(points, BigTraceData.globCal);
			//min voxel dimension
			double dMin = Math.min(Math.min(BigTraceData.globCal[0], BigTraceData.globCal[1]),BigTraceData.globCal[2]);
			//build a pipe in scaled space
			ArrayList<ArrayList< RealPoint >> point_contours = Pipe3D.getCountours(points, BigTraceData.sectorN, 0.5*fLineThickness*dMin);
			//return to voxel space			
			for(int i=0;i<point_contours.size();i++)
			{
				point_contours.set(i, Roi3D.scaleGlobInv(point_contours.get(i), BigTraceData.globCal));
			}
				
			if(renderType == Roi3D.WIRE)
			{
				setVerticesWire(point_contours);
			}
			else
			//(renderType == Roi3D.SURFACE)
			{
				setVerticesSurface(point_contours);
			}
		}
		initialized=false;
	}
	
	
	/** simple polyline, not cylindrical **/
	public void setVerticesCenterLine(final ArrayList< RealPoint > points)
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
		
	}
	
	/** generates triangulated surface mesh of a pipe around provided points **/
	public void setVerticesSurface(final ArrayList<ArrayList< RealPoint >> allContours)
	{
		int i,j, iPoint;
		int vertShift;

		//ArrayList<ArrayList< RealPoint >> allContours = Pipe3D.getCountours(points, BigTraceData.sectorN, 0.5*fLineThickness);
		final int nSectorN = BigTraceData.sectorN;
		nPointsN = allContours.size();
		if(nPointsN>1)
		{
			//all vertices
			vertices = new float [2*(nSectorN+1)*3*nPointsN];
			for (iPoint=1;iPoint<nPointsN;iPoint++)
			{
				//add to drawing vertices triangles
				vertShift = (iPoint-1)*(nSectorN+1)*2*3;
				for (i=0;i<nSectorN; i++)
				{
					for (j=0;j<3; j++)
					{
						vertices[vertShift+i*6+j]=allContours.get(iPoint-1).get(i).getFloatPosition(j);
						vertices[vertShift+i*6+3+j]=allContours.get(iPoint).get(i).getFloatPosition(j);

					}				
				}
				//last one closing circles
				for (j=0;j<3; j++)
				{
					vertices[vertShift+i*6+j]=allContours.get(iPoint-1).get(0).getFloatPosition(j);
					vertices[vertShift+i*6+3+j]=allContours.get(iPoint).get(0).getFloatPosition(j);

				}
			}
		}

	
	}
	
	/** generates a wireframe mesh of a pipe around provided points **/
	public void setVerticesWire( final ArrayList<ArrayList< RealPoint >> allContours)
	{
		int i,j, iPoint;
		int vertShift;

		//ArrayList<ArrayList< RealPoint >> allContours = Pipe3D.getCountours(points, BigTraceData.sectorN, 0.5*fLineThickness);
		final int nSectorN = BigTraceData.sectorN;
		nPointsN = allContours.size();
		if(nPointsN>1)
		{
			//all vertices
			vertices = new float [2*nSectorN*3*nPointsN];
			for (iPoint=0;iPoint<nPointsN;iPoint++)
			{
				//add to drawing vertices triangles
				vertShift = iPoint*nSectorN*3;
				for (i=0;i<nSectorN; i++)
				{
					for (j=0;j<3; j++)
					{
						vertices[vertShift+i*3+j]=allContours.get(iPoint).get(i).getFloatPosition(j);
					}				
				}
			}
			addLinesAlong();
		}
	}
	
	/** for the WIRE mode, generates a set of lines running along the main path,
	 * assuming that transverse contours already generated and put to vertices **/
	private void addLinesAlong()
	{
		final int nSectorN = BigTraceData.sectorN;
		int nShift = nSectorN*3*nPointsN;
		int i,j,k;
		for (i=0;i<nSectorN;i++)
			for(j=0;j<nPointsN;j++)
				for(k=0;k<3;k++)
				{
					vertices[nShift+3*i*nPointsN+j*3+k]=vertices[i*3+3*j*nSectorN+k];
				}
		
	}

	/** given a set of polyline nodes in points pointsPL,
	 * using Bresenham algorithm, generates a set of continuous lines among them
	 * and calls main method "setVertices.
	 * For now, it is kind of stub for PolyLine3D **/
	public void setVerticesBresenham(ArrayList< RealPoint > pointsPL)
	{
		ArrayList< RealPoint > points = new ArrayList< RealPoint >();
		ArrayList< RealPoint > curr_segment; 
		int nPLsize = pointsPL.size();
		int iPoint,i;
		if(nPLsize > 1)
		{
			for(iPoint = 0;iPoint<(nPLsize-1);iPoint++)
			{
				curr_segment=VolumeMisc.BresenhamWrap(pointsPL.get(iPoint),pointsPL.get(iPoint+1));
				if(iPoint==0)
				{
					points.add(curr_segment.get(0));
				}
				for (i=1;i<curr_segment.size();i++)
				{
					points.add(curr_segment.get(i));
				}
			}
			
		}
		setVertices(points);
		//setVertices(pointsPL);
	}
	
	/** OpenGL buffer binding, etc thing **/
	private void init( GL3 gl )
	{
		initialized = true;
		if(nPointsN>1)
		{

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
	}

	public void draw( GL3 gl, Matrix4fc pvm )
	{
		int nPointIt;
		if ( !initialized )
			init( gl );

		if(nPointsN>1)
		{
			final int nSectorN = BigTraceData.sectorN;
			JoglGpuContext context = JoglGpuContext.get( gl );
	
			prog.getUniformMatrix4f( "pvm" ).set( pvm );
			prog.getUniform4f("colorin").set(l_color);
			prog.setUniforms( context );
			prog.use( context );

	
			gl.glBindVertexArray( vao );
			

			if(renderType == Roi3D.OUTLINE)
			{
				gl.glLineWidth(fLineThickness);
				gl.glDrawArrays( GL.GL_LINE_STRIP, 0, nPointsN);
			}
			
			if(renderType == Roi3D.WIRE)
			{
				gl.glLineWidth(1.0f);
				for(nPointIt=0;nPointIt<nPointsN;nPointIt+=1)
				{
					gl.glDrawArrays( GL.GL_LINE_LOOP, nPointIt*nSectorN, nSectorN);
					//gl.glDrawArrays( GL.GL_LINE_LOOP, nSectorN, nSectorN);
				}
				int nShift = nSectorN*nPointsN;
				for(nPointIt=0;nPointIt<nSectorN;nPointIt+=1)
				{
					gl.glDrawArrays( GL.GL_LINE_STRIP, nShift+nPointIt*nPointsN, nPointsN);
					//gl.glDrawArrays( GL.GL_LINE_LOOP, nSectorN, nSectorN);
				}
			}

			if(renderType == Roi3D.SURFACE)
			{
				gl.glLineWidth(1.0f);
				for(nPointIt=0;nPointIt<(nPointsN-1);nPointIt+=1)
				{
					gl.glDrawArrays( GL.GL_TRIANGLE_STRIP, nPointIt*(nSectorN+1)*2, (nSectorN+1)*2);
				}
			}
			gl.glBindVertexArray( 0 );
		}
	}
	

}

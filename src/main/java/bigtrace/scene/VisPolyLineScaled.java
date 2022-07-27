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

import bigtrace.geometry.Intersections3D;
import bigtrace.geometry.Line3D;
import bigtrace.geometry.Plane3D;
import bigtrace.geometry.Smoothing;
import bigtrace.volume.VolumeMisc;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Matrix4fc;
import org.joml.Vector4f;

import tpietzsch.backend.jogl.JoglGpuContext;
import tpietzsch.shadergen.DefaultShader;
import tpietzsch.shadergen.Shader;
import tpietzsch.shadergen.generate.Segment;
import tpietzsch.shadergen.generate.SegmentTemplate;

import static com.jogamp.opengl.GL.GL_BLEND;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_ONE_MINUS_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_SRC_ALPHA;


public class VisPolyLineScaled
{
	
	public static final int CENTER_LINE=0, WIRE=1, SURFACE=2;
	private final Shader prog;

	private int vao;
	
	
	private float vertices[]; 
	private int nPointsN=0;

	public int renderType = WIRE;
	private Vector4f l_color;	
	public float fLineThickness;

	/** number of segments in the cylinder cross-section (stack count),
	 *  3 = prism, 4 = cuboid, etc.
	 *  The more the number, more smooth is surface **/
	public int nSectorN;

	private boolean initialized;
	


	public VisPolyLineScaled()
	{
		final Segment pointVp = new SegmentTemplate( VisPolyLineScaled.class, "/scene/simple_color.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisPolyLineScaled.class, "/scene/simple_color.fp" ).instantiate();
	
		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	}
	
	
	public VisPolyLineScaled(final ArrayList< RealPoint > points, final float fLineThickness_, final Color color_in, final int nSectorN_, final int nRenderType)
	{
		this();
		
		fLineThickness= fLineThickness_;	
		nSectorN= nSectorN_;
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

	public void setSectorN(int nSectorN_)
	{
		nSectorN= nSectorN_;
	}

	public void setParams(final ArrayList< RealPoint > points, final float fLineThickness_, final int nSectorN_, final Color color_in)
	{
		
		fLineThickness= fLineThickness_;		
		nSectorN= nSectorN_;
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
	public void setVertices( ArrayList< RealPoint > points)
	{
		if(renderType == VisPolyLineScaled.CENTER_LINE)
		{
			setVerticesCenterLine(points);
		}

		if(renderType == VisPolyLineScaled.WIRE)
		{
			setVerticesWire(points);
		}
		if(renderType == VisPolyLineScaled.SURFACE)
		{
			setVerticesSurface(points);
		}
	}
	
	public void setVerticesCenterLine( ArrayList< RealPoint > points_)
	{
		ArrayList< RealPoint > points= Smoothing.getSmoothVals(points_);
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
	
	/** generates triangulated surface mesh of a pipe around provided points **/
	public void setVerticesSurface(ArrayList< RealPoint > points_)
	{
		ArrayList< RealPoint > points= Smoothing.getSmoothVals(points_);
		int i,j, iPoint;
        int vertShift;
		double [][] path = new double [3][3];
		double [] prev_segment = new double [3];
		double [] next_segment = new double [3];
		double [] plane_norm = new double[3];
		double [] cont_point = new double [3];
		Plane3D planeBetween =new Plane3D();
		ArrayList<Line3D> extrudeLines;
		Line3D contLine;
		ArrayList< RealPoint > contour_curr;
		ArrayList< RealPoint > contour_next;
		
		nPointsN=points.size();
		if(nPointsN>1)
		{
			vertices = new float [2*(nSectorN+1)*3*nPointsN];
	
			//first contour around first line
			for (i=0;i<2;i++)
			{
				points.get(i).localize(path[i]);
			}
			
			LinAlgHelpers.subtract(path[1], path[0], prev_segment );
			LinAlgHelpers.normalize(prev_segment);
			RealPoint iniVec = new RealPoint(prev_segment );			
			RealPoint zVec = new RealPoint(0.0,0.0,1.0);
			
			AffineTransform3D ini_rot = Intersections3D.alignVectors( iniVec,zVec);
			ini_rot.translate(path[0]);
			contour_curr = iniSectorContour();
			for(i=0;i<nSectorN;i++)
			{
				ini_rot.apply(contour_curr.get(i), contour_curr.get(i));
			}
			ini_rot.apply(zVec, zVec);
			
			//other contours, inbetween
			for (iPoint=1;iPoint<(points.size()-1);iPoint++)
			{
				//find a vector that is inbetween two next segments
				//1) orientation of the segment
				points.get(iPoint+1).localize(path[2]);
				LinAlgHelpers.subtract(path[2], path[1], next_segment);
				LinAlgHelpers.normalize(next_segment);
				//2) average angle/vector between segments
				LinAlgHelpers.add(prev_segment, next_segment, plane_norm);
				LinAlgHelpers.scale(plane_norm, 0.5, plane_norm);
		
				//calculate intersection
				// also there is a special case: reverse, basically do nothing
				// use the same contour
				if(LinAlgHelpers.length(plane_norm)>0.0000001)
				{					
					//3) plane of segments cross-section
					planeBetween.setVectors(path[1], plane_norm);
					//4) make a set of lines emanating from the current contour
					extrudeLines = new ArrayList<Line3D>();
					for(i=0;i<nSectorN;i++)
					{
						contLine=new Line3D();
						contour_curr.get(i).localize(cont_point);
						contLine.setVectors(cont_point, prev_segment);
						extrudeLines.add(contLine);
					}
					
					//intersections
					contour_next = Intersections3D.planeLinesIntersect(planeBetween, extrudeLines);
				}
				else
				{
					//reversed direction
					contour_next=contour_curr;
				}
				
				//add to drawing vertices triangles
				vertShift = (iPoint-1)*(nSectorN+1)*2*3;
				for (i=0;i<nSectorN; i++)
				{
					for (j=0;j<3; j++)
					{
						vertices[vertShift+i*6+j]=contour_curr.get(i).getFloatPosition(j);
						vertices[vertShift+i*6+3+j]=contour_next.get(i).getFloatPosition(j);

					}				
				}
				//last one closing circles
				for (j=0;j<3; j++)
				{
					vertices[vertShift+i*6+j]=contour_curr.get(0).getFloatPosition(j);
					vertices[vertShift+i*6+3+j]=contour_next.get(0).getFloatPosition(j);

				}
				//prepare to move forward
				for(i=0;i<3;i++)
				{
					prev_segment[i]=next_segment[i];
					path[1][i]=path[2][i];
				}
				contour_curr=contour_next;
			}
			//last point
			points.get(nPointsN-2).localize(path[0]);
			points.get(nPointsN-1).localize(path[1]);
			LinAlgHelpers.subtract(path[0],path[1],plane_norm);
			planeBetween.setVectors(path[1], plane_norm);
			
			extrudeLines = new ArrayList<Line3D>();
			for(i=0;i<nSectorN;i++)
			{
				contLine=new Line3D();
				contour_curr.get(i).localize(cont_point);
				contLine.setVectors(cont_point, plane_norm);
				extrudeLines.add(contLine);
			}
			//intersections
			contour_next = Intersections3D.planeLinesIntersect(planeBetween, extrudeLines);
			
			//add to drawing vertices
			vertShift=(nPointsN-2)*(nSectorN+1)*2*3;
			//vertShift = (nPointsN-1)*nSectorN*3;
			for (i=0;i<nSectorN; i++)
			{
				for (j=0;j<3; j++)
				{
					//vertices[vertShift+i*3+j]=contour.get(i).getFloatPosition(j);
					vertices[vertShift+i*6+j]=contour_curr.get(i).getFloatPosition(j);
					vertices[vertShift+i*6+3+j]=contour_next.get(i).getFloatPosition(j);
				}				
			}
			//last one closing circles
			for (j=0;j<3; j++)
			{
				vertices[vertShift+i*6+j]=contour_curr.get(0).getFloatPosition(j);
				vertices[vertShift+i*6+3+j]=contour_next.get(0).getFloatPosition(j);

			}
		}
		//addLinesAlong();
		initialized=false;
	}
	
	
	/** generates a wireframe mesh of a pipe around provided points **/
	public void setVerticesWire( ArrayList< RealPoint > points_)
	//public void setVerticesWire( ArrayList< RealPoint > points)
	{
		
		ArrayList< RealPoint > points= Smoothing.getSmoothVals(points_);
		int i,j, iPoint;
        int vertShift;
		double [][] path = new double [3][3];
		double [] prev_segment = new double [3];
		double [] next_segment = new double [3];
		double [] plane_norm = new double[3];
		double [] cont_point = new double [3];
		Plane3D planeBetween =new Plane3D();
		ArrayList<Line3D> extrudeLines;
		Line3D contLine;
		ArrayList< RealPoint > contour;
		
		
		
		nPointsN=points.size();
		if(nPointsN>1)
		{
			vertices = new float [2*nSectorN*3*nPointsN];
	
			//first contour around first line
			for (i=0;i<2;i++)
			{
				points.get(i).localize(path[i]);
			}
			
			LinAlgHelpers.subtract(path[1], path[0], prev_segment );
			LinAlgHelpers.normalize(prev_segment);
			RealPoint iniVec = new RealPoint(prev_segment );
			RealPoint zVec = new RealPoint(0.0,0.0,1.0);
			
			AffineTransform3D ini_rot = Intersections3D.alignVectors( iniVec,zVec);
			ini_rot.translate(path[0]);
			contour = iniSectorContour();
			for(i=0;i<nSectorN;i++)
			{
				ini_rot.apply(contour.get(i), contour.get(i));
			}
			ini_rot.apply(zVec, zVec);
			for (i=0;i<nSectorN; i++)
			{
				for (j=0;j<3; j++)
				{
					vertices[i*3+j]=contour.get(i).getFloatPosition(j);
				}			
			}
			
			//other contours, inbetween
			for (iPoint=1;iPoint<(points.size()-1);iPoint++)
			{
				//find a vector that is inbetween two next segments
				//1) orientation of the segment
				points.get(iPoint+1).localize(path[2]);
				LinAlgHelpers.subtract(path[2], path[1], next_segment);
				
				LinAlgHelpers.normalize(next_segment);
				//2) average angle/vector between segments
				LinAlgHelpers.add(prev_segment, next_segment, plane_norm);
				//calculate intersection
				// also there is a special case: reverse, basically do nothing
				//use the same contour
				if(LinAlgHelpers.length(plane_norm)>0.0000001)
				{					
					//3) plane of segments cross-section
					planeBetween.setVectors(path[1], plane_norm);
					//4) make a set of lines emanating from the current contour
					extrudeLines = new ArrayList<Line3D>();
					for(i=0;i<nSectorN;i++)
					{
						contLine=new Line3D();
						contour.get(i).localize(cont_point);
						contLine.setVectors(cont_point, prev_segment);
						extrudeLines.add(contLine);
					}
					
					//intersections
					contour = Intersections3D.planeLinesIntersect(planeBetween, extrudeLines);
				}
				
				//add to drawing vertices
				vertShift = iPoint*nSectorN*3;
				for (i=0;i<nSectorN; i++)
				{
					for (j=0;j<3; j++)
					{
						vertices[vertShift+i*3+j]=contour.get(i).getFloatPosition(j);
					}				
				}
				
				//prepare to move forward
				for(i=0;i<3;i++)
				{
					prev_segment[i]=next_segment[i];
					path[1][i]=path[2][i];
				}
			
			}
			//last point
			points.get(nPointsN-2).localize(path[0]);
			points.get(nPointsN-1).localize(path[1]);
			LinAlgHelpers.subtract(path[0],path[1],plane_norm);
			planeBetween.setVectors(path[1], plane_norm);
			
			extrudeLines = new ArrayList<Line3D>();
			for(i=0;i<nSectorN;i++)
			{
				contLine=new Line3D();
				contour.get(i).localize(cont_point);
				contLine.setVectors(cont_point, plane_norm);
				extrudeLines.add(contLine);
			}
			//intersections
			contour = Intersections3D.planeLinesIntersect(planeBetween, extrudeLines);
			
			//add to drawing vertices
			vertShift = (nPointsN-1)*nSectorN*3;
			for (i=0;i<nSectorN; i++)
			{
				for (j=0;j<3; j++)
				{
					vertices[vertShift+i*3+j]=contour.get(i).getFloatPosition(j);
				}				
			}
			addLinesAlong();
		}
		
		initialized=false;
	}
	
	/** for the WIRE mode, generates a set of lines running along the main path,
	 * assuming that transverse contours alreade generated and put to vertices **/
	private void addLinesAlong()
	{
		int nShift = nSectorN*3*nPointsN;
		int i,j,k;
		for (i=0;i<nSectorN;i++)
			for(j=0;j<nPointsN;j++)
				for(k=0;k<3;k++)
				{
					vertices[nShift+3*i*nPointsN+j*3+k]=vertices[i*3+3*j*nSectorN+k];
				}
		
	}
	/** generates initial (first point) contour around path in XY plane **/
	private ArrayList< RealPoint > iniSectorContour()
	{
		 ArrayList< RealPoint > contourXY = new  ArrayList< RealPoint > ();
		 
		 double dAngleInc = 2.0*Math.PI/(nSectorN);
		 double dAngle = 0.0;
		 
		 for (int i=0;i<nSectorN;i++)
		 {
			 contourXY.add(new RealPoint(fLineThickness*0.5*Math.cos(dAngle),fLineThickness*0.5*Math.sin(dAngle),0.0));
			 dAngle+=dAngleInc;
		 }
		 
		 return contourXY;
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

			JoglGpuContext context = JoglGpuContext.get( gl );
	
			prog.getUniformMatrix4f( "pvm" ).set( pvm );
			prog.getUniform4f("colorin").set(l_color);
			prog.setUniforms( context );
			prog.use( context );

	
			gl.glBindVertexArray( vao );
			

			if(renderType == VisPolyLineScaled.CENTER_LINE)
			{
				gl.glLineWidth(fLineThickness);
				gl.glDrawArrays( GL.GL_LINE_STRIP, 0, nPointsN);
			}
			
			if(renderType == VisPolyLineScaled.WIRE)
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

			if(renderType == VisPolyLineScaled.SURFACE)
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

package bigtrace.scene;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import bigtrace.BigTraceData;
import bigtrace.geometry.Pipe3D;
import bigtrace.rois.Roi3D;
import net.imglib2.RealPoint;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Matrix4fc;
import org.joml.Vector4f;

import btbvv.core.backend.jogl.JoglGpuContext;
import btbvv.core.shadergen.DefaultShader;
import btbvv.core.shadergen.Shader;
import btbvv.core.shadergen.generate.Segment;
import btbvv.core.shadergen.generate.SegmentTemplate;


import static com.jogamp.opengl.GL.GL_FLOAT;


public class VisPolyLineScaled
{	
	
	private final Shader prog;

	private int vao;
		
	private float vertices[]; 
	
	public int nPointsN = 0;

	public int renderType = Roi3D.WIRE;
	
	private Vector4f l_color;	
	
	public float fLineThickness;

	private boolean initialized;	

	public VisPolyLineScaled()
	{
		final Segment pointVp = new SegmentTemplate( VisPolyLineScaled.class, "/scene/simple_color.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisPolyLineScaled.class, "/scene/simple_color.fp" ).instantiate();
	
		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	}
	
	public VisPolyLineScaled(final ArrayList< RealPoint > points, final ArrayList< double [] > tangents, final float fLineThickness_, final Color color_in, final int nRenderType)
	{
		this();
		
		fLineThickness= fLineThickness_;	
		l_color = new Vector4f(color_in.getComponents(null));		
		renderType = nRenderType;
		setVertices(points, tangents);
		
	}
	
	public void setThickness(float fLineThickness_)
	{
		fLineThickness= fLineThickness_;
	}
	
	public void setColor(Color color_in)
	{
		l_color = new Vector4f(color_in.getComponents(null));
	}


	public void setParams(final ArrayList< RealPoint > points, final ArrayList< double [] > tangents, final float fLineThickness_, final int nSectorN_, final Color color_in)
	{
		
		fLineThickness= fLineThickness_;		
		l_color = new Vector4f(color_in.getComponents(null));		
		setVertices(points, tangents);
	}
	
	public void setRenderType(int nRenderType_)
	{
		renderType = nRenderType_;
		
	}
	public int getRenderType()
	{
		return renderType;
		
	}
	
	public void setVertices( final ArrayList< RealPoint > points_, final ArrayList<double[]> tangents_)
	{
		
		//ArrayList< RealPoint > points;
		
		
		if(renderType == Roi3D.OUTLINE)
		{
			setVerticesCenterLine(Roi3D.scaleGlobInv(points_, BigTraceData.globCal));
		}
		else
		{

			//build a pipe in scaled space
			ArrayList<ArrayList< RealPoint >> point_contours = Pipe3D.getCountours(points_, tangents_, BigTraceData.sectorN, 0.5*fLineThickness*BigTraceData.dMinVoxelSize);
			//return to voxel space	for the render		
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

		final int nSectorN = BigTraceData.sectorN;
		nPointsN = allContours.size();
		if(nPointsN>1)
		{
			//all vertices
			vertices = new float [2*nSectorN*3*nPointsN];
			for (iPoint=0;iPoint<nPointsN;iPoint++)
			{
				//drawing contours around each point
				vertShift = iPoint*nSectorN*3;
				for (i=0;i<nSectorN; i++)
				{
					for (j=0;j<3; j++)
					{
						vertices[vertShift+i*3+j]=allContours.get(iPoint).get(i).getFloatPosition(j);
					}				
				}
			}
			//lines along the pipe
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
	
	/** OpenGL buffer binding, etc thing **/
	private void init( final GL3 gl )
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

	public void draw( final GL3 gl, final Matrix4fc pvm )
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
	

//			gl.glEnable(GL3.GL_LINE_SMOOTH_HINT);
//			gl.glEnable(GL3.GL_POLYGON_SMOOTH);
//			gl.glHint(GL3.GL_LINE_SMOOTH_HINT, GL3.GL_NICEST );
//			gl.glHint(GL3.GL_POLYGON_SMOOTH_HINT, GL3.GL_NICEST );
//			gl.glEnable(GL3.GL_BLEND);
//			gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
//			gl.glDepthFunc(GL3.GL_ALWAYS);
			if(renderType == Roi3D.OUTLINE)
			{
				gl.glLineWidth(fLineThickness);
				gl.glDrawArrays( GL.GL_LINE_STRIP, 0, nPointsN);
			}
			
			if(renderType == Roi3D.WIRE)
			{

				gl.glLineWidth(1.0f);
				//contours
				for(nPointIt=0;nPointIt<nPointsN;nPointIt+=BigTraceData.wireCountourStep)
				{
					gl.glDrawArrays( GL.GL_LINE_LOOP, nPointIt*nSectorN, nSectorN);
					//gl.glDrawArrays( GL.GL_LINE_LOOP, nSectorN, nSectorN);
				}
				//the last contour
				if((nPointIt-BigTraceData.wireCountourStep)!=(nPointsN-1))
				{
					gl.glDrawArrays( GL.GL_LINE_LOOP, (nPointsN-1)*nSectorN, nSectorN);
				}
				//lines along the pipe
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

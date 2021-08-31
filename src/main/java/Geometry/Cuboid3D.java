package Geometry;

import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;
/**
 *  3D shape that has 8 vertices and 6 faces,
 *  not necessarily box, but it is not verified 
 *  and supposed to be checked by the user
 * **/
public class Cuboid3D {

	public double[][] vertices;
	public double [][] dMinMax;
	public ArrayList<Plane3D> faces;
	public boolean faces_init = false;
	
	public Cuboid3D()
	{
		vertices = new double [8][3];
		faces = new ArrayList<Plane3D>(); 
	}
	/**
	 *    Creates a cube with min coordinates at nMinMax[0]
	 *    and max coordinates at nMinMax[1].
	 *    Does not initializes "faces", use iniFaces()
	 * **/
	public Cuboid3D(final double [][] dMinMaxin)
	{
		vertices = new double [8][3];
		dMinMax = new double [2][3];
		
		for(int i=0;i<3;i++)
		{
			dMinMax[0][i]=dMinMaxin[0][i];
			dMinMax[1][i]=dMinMaxin[1][i];
		}
		double [] temp = new double[3];
		double [] diff = new double[3];
		int i;
		
		//range
		for(i=0;i<3;i++)
			diff[i]=dMinMax[1][i]-dMinMax[0][i];
		
		LinAlgHelpers.scale(dMinMax[0],1.0, vertices[0]);
		LinAlgHelpers.scale(dMinMax[1],1.0, vertices[6]);
		temp[1]=diff[1]; 
		LinAlgHelpers.add(vertices[0],temp,vertices[1]);
		temp[0]=diff[0]; 
		LinAlgHelpers.add(vertices[0],temp,vertices[2]);
		temp[1]=0.0;
		LinAlgHelpers.add(vertices[0],temp,vertices[3]);
		temp[0]=0.0;
		temp[2]=diff[2];
		LinAlgHelpers.add(vertices[0],temp,vertices[4]);
		LinAlgHelpers.add(vertices[1],temp,vertices[5]);
		LinAlgHelpers.add(vertices[3],temp,vertices[7]);
	}
	
	/**
	 *    Creates a cube with min coordinates at nMinMax[0]
	 *    and max coordinates at nMinMax[1].
	 *    Does not initializes "faces", use iniFaces()
	 * **/
	public Cuboid3D(final long [][] nMinMax)
	{
		dMinMax = new double [2][3];
		
		for(int i=0;i<3;i++)
		{
			dMinMax[0][i]=(long)Math.round(nMinMax[0][i]);
			dMinMax[1][i]=(long)Math.round(nMinMax[1][i]);
		}
		vertices = new double [8][3];
		double [] temp = new double[3];
		double [] diff = new double[3];
		int i;
		
		//range
		for(i=0;i<3;i++)
			diff[i]=dMinMax[1][i]-dMinMax[0][i];
		
		LinAlgHelpers.scale(dMinMax[0],1.0, vertices[0]);
		LinAlgHelpers.scale(dMinMax[1],1.0, vertices[6]);
		temp[1]=diff[1]; 
		LinAlgHelpers.add(vertices[0],temp,vertices[1]);
		temp[0]=diff[0]; 
		LinAlgHelpers.add(vertices[0],temp,vertices[2]);
		temp[1]=0.0;
		LinAlgHelpers.add(vertices[0],temp,vertices[3]);
		temp[0]=0.0;
		temp[2]=diff[2];
		LinAlgHelpers.add(vertices[0],temp,vertices[4]);
		LinAlgHelpers.add(vertices[1],temp,vertices[5]);
		LinAlgHelpers.add(vertices[3],temp,vertices[7]);
	}	
	public void iniFaces()
	{		
		faces = new ArrayList<Plane3D>(); 
		int i,j;
		//front
		faces.add(new Plane3D(vertices[3],vertices[0],vertices[1]));
		//bottom
		faces.add(new Plane3D(vertices[7],vertices[4],vertices[0]));
		//left
		faces.add(new Plane3D(vertices[0],vertices[4],vertices[5]));
		//put vertex [0] as a vector for the first three planes
		// j==1 is already 0
		for(j=1;j<3;j++)
		{
			for(i=0;i<3;i++)
			{
				faces.get(j).p0[i]=vertices[0][i];
			}
		}
		//back
		faces.add(new Plane3D(vertices[6],vertices[5],vertices[4]));
		//top
		faces.add(new Plane3D(vertices[2],vertices[1],vertices[5]));
		//right
		faces.add(new Plane3D(vertices[7],vertices[3],vertices[2]));
		//put vertex [6] as a vector for the last three planes
		for(j=3;j<6;j++)
		{
			for(i=0;i<3;i++)
			{
				faces.get(j).p0[i]=vertices[6][i];
			}
		}
		
		faces_init = true;
	}
	/** Verifies if point is inside the bounding box of cuboid. 
	 *  Faster version. For proper non-rectangular shape boundaries' check  
	 *  use slower isPointInsideShape **/
	public boolean isPointInsideMinMax(final double [] pPoint)
	{
		for(int i=0;i<3;i++)
		{
			if(pPoint[i]<dMinMax[0][i])
				return false;
			if(pPoint[i]>dMinMax[1][i])
				return false;
		}
		return true;
	}
	/** Verifies if point is inside the bounding box of cuboid. 
	 *  Faster version. For proper non-rectangular shape boundaries' check  
	 *  use slower isPointInsideShape **/
	public boolean isPointInsideMinMax(final RealPoint pPoint)
	{
		for(int i=0;i<3;i++)
		{
			if(pPoint.getDoublePosition(i)<dMinMax[0][i])
				return false;
			if(pPoint.getDoublePosition(i)>dMinMax[1][i])
				return false;
		}
		return true;
	}
	
	/** Verifies if point is inside the bounding box of cuboid. 
	 *  Slower version, for quick check of minmax bounding box boundaries  
	 *  use faster isPointInsideMinMax **/
	public boolean isPointInsideShape(final double [] pPoint)
	{
		double [] diff = new double [3];
		int i;
		//first three planes:front left bottom
		LinAlgHelpers.subtract(pPoint, vertices[0], diff);
		for(i=0;i<3;i++)
		{
			if(LinAlgHelpers.dot(diff, faces.get(i).n) <0)
			{
				return false;
			}
		}
		//last three planes:back right top
		LinAlgHelpers.subtract(pPoint, vertices[6], diff);
		for(i=3;i<6;i++)
		{
			if(LinAlgHelpers.dot(diff, faces.get(i).n) <0)
			{
				return false;
			}
		}
		return true;
	}
}

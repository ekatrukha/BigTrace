package shapes3D;

import org.joml.Vector3f;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

/** 
 * 	Basic conical frustum, specified by two points p1 and p2 and
 *  two radii r1 and r2;
 *  Helpful member is p = p2-p1, normalized
 *  and len = distance between p1 and p2
 * **/
public class ConicalFrustum {
	public RealPoint p1 = new RealPoint(3);
	public RealPoint p2 =  new RealPoint(3);
	public RealPoint p =  new RealPoint(3);
	public double r1;
	public double r2;
	public double len;
	
	public ConicalFrustum(RealPoint p1_, RealPoint p2_, float r1_, float r2_)
	{
		p1.setPosition(p1_);
		p2.setPosition(p2_);
		r1 = r1_;
		r2 = r2_;
		double [] pos1 = new double[3];
		double [] pos2 = new double[3];
		  		
		p1.localize(pos1);
		p2.localize(pos2);
		
		LinAlgHelpers.subtract(pos2, pos1, pos2);
		//distance between p1 and p2
		len = LinAlgHelpers.length(pos2);
		LinAlgHelpers.normalize(pos2);
		
		p.setPosition(pos2);
		
	}
	/**
	 * constructor that takes centers of cone's cross-sections p1_ and p2_
	 * and some vectors at the outer circle of each corresponding crossection p1r1_ and p2r2_   
	 * **/
	
	public ConicalFrustum(Vector3f p1_, Vector3f p2_, Vector3f p1r1_, Vector3f p2r2_)
	{
		double [] val1 = new double [] {p1_.x,p1_.y,p1_.z};
		double [] val2 = new double [] {p2_.x,p2_.y,p2_.z};
		Vector3f subx= new Vector3f ();
		p1.setPosition(val1);
		p2.setPosition(val2);
		subx.set(p1_);
		r1 = (subx.sub(p1r1_).length());
		subx.set(p2_);
		r2 = (subx.sub(p2r2_).length());
		double [] pos1 = new double[3];
		double [] pos2 = new double[3];
		  		
		p1.localize(pos1);
		p2.localize(pos2);
		
		LinAlgHelpers.subtract(pos2, pos1, pos2);
		//distance between p1 and p2
		len = LinAlgHelpers.length(pos2);
		LinAlgHelpers.normalize(pos2);
		
		p.setPosition(pos2);
		
	}
	
	public boolean isPointInside(RealPoint px_)
	{

		final double [] pos1 = new double[3];
		final double [] pos2 = new double[3];
		final double [] pos = new double[3];
		final double [] posx = new double[3];
		final double [] ptemp = new double[3];
		double dist;
		double proj_length;
		 

		p1.localize(pos1);
		p2.localize(pos2);
		p.localize(pos);
		px_.localize(posx);

		//is point on the proper side of one cross-section?
		LinAlgHelpers.subtract(posx, pos2, ptemp);
		if(LinAlgHelpers.dot(ptemp, pos)>0)
			return false;
		//is point on the proper side of another cross-section?
		LinAlgHelpers.subtract(posx, pos1, ptemp);
		if(LinAlgHelpers.dot(ptemp, pos)<0)
			return false;
		//distance to the central line cone 
		LinAlgHelpers.cross(ptemp,pos,pos2);
		dist=LinAlgHelpers.length(pos2);
		//projection
		proj_length = LinAlgHelpers.dot(ptemp, pos);
		//corresponding R at this point:
		proj_length = r1 +(proj_length/len)*(r2-r1); 
		if(dist<=proj_length)
			return true;
		else
			return false;
	}
	
	public void applyTransform(AffineTransform3D transform)
	{
		final double [] pos1 = new double[3];
		final double [] pos2 = new double[3];		 
		final double [] pos = new double[3];
		final double [] pos_perp = new double[3];
		final double [] rad1 = new double[3];
		final double [] rad2 = new double[3];

		p1.localize(pos1);
		p2.localize(pos2);
		p.localize(pos);
		
		//make two perpendicular vectors to see how radii must change
		
		//first let's make a normal vector to the vector of cone's axis
		ConicalFrustum.getPerpendicularVector3D(pos, pos_perp);

		//got it, now make vectors in the plane of radius		
		LinAlgHelpers.scale(pos_perp, r1, rad1);
		LinAlgHelpers.add(rad1, pos1, rad1);
		LinAlgHelpers.scale(pos_perp, r2, rad2);
		LinAlgHelpers.add(rad2, pos2, rad2);
		
		//let's transform all 4 vectors
		transform.apply(pos1, pos1);
		transform.apply(pos2, pos2);
		transform.apply(rad1, rad1);
		transform.apply(rad2, rad2);
		
		//reinitialize object		
		p1.setPosition(pos1);
		p2.setPosition(pos2);
		r1 = LinAlgHelpers.distance(pos1, rad1);
		r2 = LinAlgHelpers.distance(pos2, rad2);
		LinAlgHelpers.subtract(pos2, pos1, pos2);
		
		len = LinAlgHelpers.length(pos2);
		LinAlgHelpers.normalize(pos2);
		
		p.setPosition(pos2);
	}
	
	/**  Given a p_in vector in 3D, returns 
	 *   normalized perpendicular vector @p_out 
	 *   (taking care of special cases)  
	 * **/
	public static void getPerpendicularVector3D (final double [] p_in, final double [] p_out)
	{

		final double s = LinAlgHelpers.length(p_in);
		final double g;
		final double h;
		if(p_in[2]==0.0)
			{g=1.0;}
		else
			{g=Math.copySign(s, p_in[2]);}
		
		h=g+p_in[2];
		
		p_out[0] = g*h - p_in[0]*p_in[0];
		p_out[1] = (-1.0)*p_in[0]*p_in[1];
		p_out[2] = (-1.0)*p_in[0]*h;
		LinAlgHelpers.normalize(p_out);

	}

}

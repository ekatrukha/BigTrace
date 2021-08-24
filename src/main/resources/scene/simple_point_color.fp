out vec4 fragColor;

in float dScale;

uniform vec3 colorin;
uniform vec2 scrnsize;

void main()
{
    
    vec2 coord = gl_PointCoord - vec2(0.5,0.5);  //from [0,1] to [-0.5,0.5]
	
	
	float t = 0.;
	
	//scaled ellipse	
	float norm = (coord.x*coord.x/(dScale*scrnsize.x))+(coord.y*coord.y/(dScale*scrnsize.y));	
	
	if ( norm >1)
	{    
		discard;
	}
	//else
	//{
		//1) square root intensity decay
		//t = sqrt(1.0-sqrt(norm));
		//2) gaussian intensity decay
		//t=exp(-(norm*norm)/(2*0.3*0.3));
	//}
	//diffuse spot
    //vec4 ccolor = vec4(colorin, t);
    //just round spot
    vec4 ccolor = vec4(colorin, 1.0);


    fragColor = ccolor; 
}

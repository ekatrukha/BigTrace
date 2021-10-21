out vec4 fragColor;

in float dScale;

uniform vec4 colorin;
uniform vec2 screenSize;

void main()
{
    
    //transform coordinates
	vec2 coord = 2.0*gl_PointCoord - 1.0;
	
	//screen size scaling
	//accounting for the stretched spot render
	vec2 scrnsize=screenSize/(max(screenSize.x,screenSize.y));
	
	//ellipse taking into account stretched render window	
	// and depth scaling of the point
	float norm = (coord.x*coord.x/(dScale*scrnsize.y*scrnsize.y))+(coord.y*coord.y/(dScale*scrnsize.x*scrnsize.x));	
	
	//cut off everything outside the ellipse
	if ( norm >1)
	{    
		discard;
	}

    //just color in
    //vec4 ccolor = vec4(colorin, 1.0);

    fragColor = colorin; 
}

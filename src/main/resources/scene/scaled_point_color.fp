out vec4 fragColor;

uniform vec4 colorin;
uniform vec2 ellipseAxes;

void main()
{
    
    //transform coordinates to NDC
	
	vec2 coord = 2.0 * gl_PointCoord - 1.0;
	
	
	//ellipse taking into account stretched render window	
	float norm = (coord.x*coord.x*ellipseAxes.x)+(coord.y*coord.y*ellipseAxes.y);	
	
	//cut off everything outside the ellipse
	if ( norm >1) discard;

    fragColor = colorin; 
}

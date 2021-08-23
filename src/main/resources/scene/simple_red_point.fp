out vec4 fragColor;

uniform vec2 scrnsize;

void main()
{
    
    vec2 coord = gl_PointCoord - vec2(0.5,0.5);  //from [0,1] to [-0.5,0.5]
	
	float len=length(coord);
	float t = 0.;
	
	//ellipse
	float norm = (coord.x*coord.x/scrnsize.x)+(coord.y*coord.y/scrnsize.y);	
	
	norm=(1.0-gl_FragCoord.w)*norm;
	if ( norm > 1)
	{    
		discard;
	}
	else
	{
		t= sqrt(1.0-sqrt(norm));
	}
    vec4 ccolor = vec4(1.0, 0.0, 0.0, t);


    fragColor = ccolor; 
}

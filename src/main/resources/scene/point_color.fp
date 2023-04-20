out vec4 fragColor;
uniform vec4 colorin;

//uniform vec2 screenSize;

void main()
{
    

	vec2 coord = 2.0*gl_PointCoord - 1.0;
	//vec2 coord = 2.0*gl_FragCoord/screenSize - 1.0;

	//vec2 scrnsize=screenSize/(min(screenSize.x,screenSize.y));

	//float norm = (coord.x*coord.x*scrnsize.x*scrnsize.x)+(coord.y*coord.y*scrnsize.y*scrnsize.y);	
	//float norm = coord.x*coord.x/screenSize.x+coord.y*coord.y/screenSize.y;	
	float norm = (coord.x*coord.x)+(coord.y*coord.y);	
    if (norm > 1.0) discard; 

    fragColor = colorin; 
}

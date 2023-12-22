uniform vec4 colorin;
out vec4 fragColor;
uniform int surfaceRender;

void main()
{
	//"silhouette" surface
	if(surfaceRender==1)
	{
		gl_FragDepth = 1.0;										
	}
	else
	{
		gl_FragDepth = gl_FragCoord.z;
	}
	fragColor = colorin;
    
}

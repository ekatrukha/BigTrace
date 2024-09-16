uniform vec4 color;
uniform float antialias;
uniform float thickness;
uniform float linelength;
in vec2 v_uv;
out vec4 fragColor;

void main()
{
    float d = 0;
    float w = thickness/2.0 - antialias;

    vec4 colorOut = vec4(color);

    // Cap at start
    if (v_uv.x < 0)
    {      
       d = length(v_uv) - w;
    }
    // Cap at end
    else if (v_uv.x >= linelength)
    {
		d = length(v_uv - vec2(linelength,0)) - w;
    }
    // Body
    else
    {
        d = abs(v_uv.y) - w;
    }
        
    if( d < 0) 
    {
       fragColor = vec4(color.xyz, 1.0);     
    } 
    else 
    {
        d /= antialias;
        fragColor = vec4(color.xyz, exp(-d*d));
    }
	
}

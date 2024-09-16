uniform vec4 colorin;
out vec4 fragColor;
in vec2 vLineCenter;
in vec3 FragPos;

void main()
{
	  double w = 4;
	  float uBlendFactor = 1.5;
	  
  	  vec4 col = colorin;
  	  
  	    vec4 ndc = vec4(
        (gl_FragCoord.x / 800.0 - 0.5) * 2.0,
        (gl_FragCoord.y / 600.0 - 0.5) * 2.0,
        (gl_FragCoord.z - 0.5) * 2.0,
        1.0);
  	  
      double d = length(vLineCenter.xy -gl_FragCoord.xy);	

      if (d>w)
      	discard;
        //col.a = 0;
      else
        col.a *= pow(float((w-d)/w), uBlendFactor);
        
      fragColor = col;
     // gl_FragDepth = gl_FragCoord.z;
      gl_FragDepth = 1.0;

}

out vec4 fragColor;

uniform sampler2D texture1;

void main()
{
	//highp vec4 texture = texture2D(texture1, gl_PointCoord);
	//vec4 ccolor = vec4(1.0, 1.0, 1.0, 1.0);
    fragColor = texture2D(texture1, gl_PointCoord) ;//* ccolor;
}

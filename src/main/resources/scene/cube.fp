out vec4 fragColor;

in vec2 texCoord;

uniform sampler2D texture1;

void main()
{
    fragColor = texture( texture1, texCoord );
}

uniform mat4 uMVPMatrix;
uniform float uAlpha;
uniform float uMask;
attribute vec3 vPosition;
attribute vec2 inputTextureCoordinate;
varying vec2 textureCoordinate;
varying float vAlpha;
varying float vMask;
void main()
{
	gl_Position = uMVPMatrix * vec4(vPosition, 1);
	textureCoordinate = inputTextureCoordinate;
	vAlpha = uAlpha;
	vMask = uMask;
}
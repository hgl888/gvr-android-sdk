precision mediump float;
varying vec2 textureCoordinate;
uniform sampler2D s_texture;
varying float vAlpha;
varying float vMask;
void main() {
	vec4 color = texture2D( s_texture, textureCoordinate );
	gl_FragColor = vec4(color.r * vMask, color.g * vMask, color.b * vMask, color.a * vAlpha);
}
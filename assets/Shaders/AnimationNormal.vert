// The SSAO filter's normal pass (PreNormalPass) of the DkLighting materials:
// the drawn position, animated as in AnimationShadow.vert, and the view space
// normal, for AnimationNormal.frag. Explicit locations (GLSL 330, #540)
#extension GL_ARB_separate_shader_objects : enable
#import "Common/ShaderLib/Instancing.glsllib"
#import "Shaders/AnimationFrame.glsllib"
#import "Shaders/VertexAnimation.glsllib"
layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTexCoord;
layout(location = 0) out vec2 texCoord;
layout(location = 1) out vec3 viewNormal;
#ifdef ANIMATED_TILES
uniform float g_Time;
uniform int m_NumberOfTiles;
uniform int m_Speed;
#endif
void main() {
    gl_Position = TransformWorldViewProjection(vec4(vertexAnimationPosition(inPosition), 1.0));
    viewNormal = TransformNormal(vertexAnimationNormal(inNormal));
    texCoord = inTexCoord;
#ifdef ANIMATED_TILES
    texCoord.x = (texCoord.x + mod(float(animationFrame(g_Time,m_Speed)),float(m_NumberOfTiles))) / float(m_NumberOfTiles);
#endif
}

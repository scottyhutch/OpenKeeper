#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Shaders/AnimationFrame.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Shaders/DkLighting/DkFalloff.glsllib"
#import "Shaders/VertexAnimation.glsllib"
#import "Shaders/DkLighting/VertexLighting.glsllib"

#ifdef ANIMATED_TILES
uniform float g_Time;
uniform int m_NumberOfTiles;
uniform int m_Speed;
#endif

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying vec3 vVertexColor;
varying vec3 vOverlay;
varying vec3 vGlow;

void main() {
    vec4 modelSpacePos = vec4(vertexAnimationPosition(inPosition), 1.0);
    gl_Position = TransformWorldViewProjection(modelSpacePos);
    texCoord = inTexCoord;
#ifdef ANIMATED_TILES
    float tiles = float(m_NumberOfTiles);
    float selectedTile = float(animationFrame(g_Time,m_Speed));
    texCoord.x = (texCoord.x + mod(selectedTile, tiles)) / tiles;
#endif

    vec3 worldPos = TransformWorld(modelSpacePos).xyz;
    vec3 worldNormal = normalize(TransformWorldNormal(inNormal));

    vVertexColor = dkVertexLight(worldPos, worldNormal);
    vOverlay = m_TintOverlay.rgb;
    vGlow = vec3(0.0);
}

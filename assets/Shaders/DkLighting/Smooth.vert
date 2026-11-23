// Mesh smoothing, vertex stage of every technique of DkLighting/Smooth: only
// animates the vertex (VertexAnimation.glsllib, with the key frames' normals),
// the tessellation stages make and light the smoothed surface.
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Shaders/AnimationFrame.glsllib"
#import "Shaders/VertexAnimation.glsllib"

#ifdef ANIMATED_TILES
uniform float g_Time;
uniform int m_NumberOfTiles;
uniform int m_Speed;
#endif

in vec3 inPosition;
in vec3 inNormal;
in vec2 inTexCoord;

out vec3 vsPosition;
out vec3 vsNormal;
out vec2 vsTexCoord;

void main() {
    vsPosition = vertexAnimationPosition(inPosition);
    vsNormal = normalize(vertexAnimationNormal(inNormal));
    vsTexCoord = inTexCoord;
#ifdef ANIMATED_TILES
    float tiles = float(m_NumberOfTiles);
    float selectedTile = float(animationFrame(g_Time, m_Speed));
    vsTexCoord.x = (vsTexCoord.x + mod(selectedTile, tiles)) / tiles;
#endif
    gl_Position = vec4(vsPosition, 1.0);
}

#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Shaders/AnimationFrame.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Shaders/DkLighting/DkFalloff.glsllib"
#import "Shaders/VertexAnimation.glsllib"
#import "Shaders/DkLighting/VertexLighting.glsllib"

#if defined(ANIMATED_TILES) || defined(LAVA_WAVES)
uniform float g_Time;
#endif
#ifdef ANIMATED_TILES
uniform int m_NumberOfTiles;
uniform int m_Speed;
#endif
#ifdef LAVA_WAVES
uniform vec4 m_LavaWaveParameters;
#endif

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying vec3 vVertexColor;
varying vec3 vOverlay;
varying vec3 vGlow;
varying vec2 liquidTexCoord;
varying vec2 embmEnvironmentTexCoord;
varying float vSurfaceVisibility;

void main() {
    vec4 modelSpacePos = vec4(vertexAnimationPosition(inPosition), 1.0);
    vec3 localNormal = inNormal;
#ifdef LAVA_WAVES
    float waveNumber = 6.28318530718 / max(m_LavaWaveParameters.z, 0.001);
    float phaseA = (inPosition.x + inPosition.z - g_Time * m_LavaWaveParameters.w) * waveNumber;
    float phaseB = (inPosition.x - inPosition.z - g_Time * m_LavaWaveParameters.w) * waveNumber;
    float amplitudeA = m_LavaWaveParameters.x;
    float amplitudeB = m_LavaWaveParameters.y;
    modelSpacePos.y += amplitudeA * sin(phaseA) + amplitudeB * sin(phaseB);
    float slopeX = waveNumber * (amplitudeA * cos(phaseA) + amplitudeB * cos(phaseB));
    float slopeZ = waveNumber * (amplitudeA * cos(phaseA) - amplitudeB * cos(phaseB));
    localNormal = normalize(vec3(-slopeX, 1.0, -slopeZ));
#endif
    gl_Position = TransformWorldViewProjection(modelSpacePos);
    texCoord = inTexCoord;
    liquidTexCoord = inTexCoord;
#ifdef ANIMATED_TILES
    float tiles = float(m_NumberOfTiles);
    float selectedTile = float(animationFrame(g_Time,m_Speed));
    texCoord.x = (texCoord.x + mod(selectedTile, tiles)) / tiles;
#endif

    vec3 worldPos = TransformWorld(modelSpacePos).xyz;
    vec3 worldNormal = normalize(TransformWorldNormal(localNormal));
#ifdef EMBM_ENABLED
    // The original G400 path perturbs precomputed environment coordinates.
    // Sphere-map the reflected view vector so the reflection is continuous
    // across the merged tile mesh instead of restarting at every tile UV.
    vec3 viewPosition = (g_ViewMatrix * vec4(worldPos, 1.0)).xyz;
    vec3 viewNormal = normalize((g_ViewMatrix * vec4(worldNormal, 0.0)).xyz);
    vec3 viewReflection = reflect(normalize(viewPosition), viewNormal);
    float sphereDenominator = 2.0 * sqrt(dot(viewReflection.xy, viewReflection.xy)
            + (viewReflection.z + 1.0) * (viewReflection.z + 1.0));
    embmEnvironmentTexCoord = sphereDenominator > 0.0001
            ? viewReflection.xy / sphereDenominator + vec2(0.5)
            : vec2(0.5);
#endif

    float explored;
    vVertexColor = dkVertexLight(worldPos, worldNormal, explored);
    vSurfaceVisibility = explored;
    vOverlay = m_TintOverlay.rgb;
    vGlow = vec3(0.0);
}

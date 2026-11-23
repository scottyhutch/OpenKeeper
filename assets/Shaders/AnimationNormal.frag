// The SSAO filter's normal pass (PreNormalPass) of the DkLighting materials:
// the same cut-out as LightingSprite.frag, and the view space normal encoded
// as jME's Common/MatDefs/SSAO/normal.frag does, which ssao.frag decodes.
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_uniform_location : enable
#import "Shaders/AnimationCoverage.glsllib"

precision highp float;
precision highp sampler2D;

layout(location = 7) uniform sampler2D m_DiffuseMap;
layout(location = 8) uniform float m_AlphaDiscardThreshold;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec3 viewNormal;
layout(location = 0) out vec4 fragColor;

void main()
{
  #ifdef DISCARD_ALPHA
      if (!animationTexelCovered(texture(m_DiffuseMap, texCoord), m_AlphaDiscardThreshold))
          discard;
  #endif

  // A mesh without normals faces the camera rather than giving NaN
  vec3 normal = dot(viewNormal, viewNormal) > 1e-8 ? normalize(viewNormal) : vec3(0.0, 0.0, 1.0);
  fragColor = vec4(normal.xy * 0.5 + 0.5, -normal.z * 0.5 + 0.5, 1.0);
}
